package com.firedragon.app.engine

import koma.*
import koma.extensions.forEachIndexed
import koma.extensions.mapIndexed
import koma.matrix.Matrix
import org.opencv.core.*
import org.opencv.features2d.DescriptorExtractor
import org.opencv.features2d.DescriptorMatcher
import org.opencv.features2d.FeatureDetector
import kotlin.collections.ArrayList

class Status(
    val state: Int,
    val dx: Double,
    val dy: Double,
    val angleInDegrees: Double,
    val numMatches: Int
)

class VisualOdometer2D(
    private val featureDetector: FeatureDetector,
    private val descriptorExtractor: DescriptorExtractor,
    private val descriptorMatcher: DescriptorMatcher,
    private val ANCHOR_FRAME_MATCHES_THRESHOLD: Int,
    private val ANCHOR_TO_NEW_FRAME_MATCHES_THRESHOLD: Int,
    private val NN_DIST_RATIO: Double,
    private val IMAGE_WIDTH: Int,
    private val IMAGE_HEIGHT: Int
) {
    companion object {
        const val NEW_FRAME_MATCHED = 0
        const val FOUND_ANCHOR = 1
        const val ANCHOR_NOT_FOUND = 2
        const val NEW_FRAME_NOT_MATCHED = 3
    }

    private var isFirstFrame = true
    private var anchorFrame = Mat()
    private var anchorFrameKeyPoints = MatOfKeyPoint()
    private var anchorFrameDescriptors = MatOfKeyPoint()
    private var prevStatus = Status(ANCHOR_NOT_FOUND, 0.0, 0.0, 0.0, 0)

    init {
        assert(ANCHOR_FRAME_MATCHES_THRESHOLD > 0)
        assert(ANCHOR_TO_NEW_FRAME_MATCHES_THRESHOLD > 0)
        assert(ANCHOR_FRAME_MATCHES_THRESHOLD > ANCHOR_TO_NEW_FRAME_MATCHES_THRESHOLD)
        assert(NN_DIST_RATIO >= 0.0)
        assert(NN_DIST_RATIO <= 1.0)
    }

    public fun feed(frame: Mat): Status {
        if (isFirstFrame) {
            val status = setAnchorFrame(frame)
            if (status.state == FOUND_ANCHOR) {
                isFirstFrame = false
            }
            return status
        } else {
            return matchNewFrame(frame)
        }
    }

    public fun reset() {
        isFirstFrame = true
    }

    private fun setAnchorFrame(frame1: Mat): Status {
        // Anchor frame
        featureDetector.detect(frame1, anchorFrameKeyPoints)
        val numKPs = anchorFrameKeyPoints.toList().size
        if (numKPs < ANCHOR_FRAME_MATCHES_THRESHOLD) {
            return Status(ANCHOR_NOT_FOUND, 0.0, 0.0, 0.0, 0)
        } else {
            descriptorExtractor.compute(frame1, anchorFrameKeyPoints, anchorFrameDescriptors)
            this.anchorFrame = frame1.clone()
            return Status(FOUND_ANCHOR, 0.0, 0.0, 0.0, numKPs)
        }
    }

    private fun matchNewFrame(newFrame: Mat): Status {
        // New frame
        val newFrameKeyPoints = MatOfKeyPoint()
        featureDetector.detect(newFrame, newFrameKeyPoints)
        val newFrameDescriptors = MatOfKeyPoint()
        descriptorExtractor.compute(newFrame, newFrameKeyPoints, newFrameDescriptors)
        // Matching and filtering good matches
        val anchorToNewFramematches = ArrayList<MatOfDMatch>()
        descriptorMatcher.knnMatch(
            anchorFrameDescriptors,
            newFrameDescriptors,
            anchorToNewFramematches,
            2
        )
        val goodMatchesList = ArrayList<DMatch>()
        for (i in 0 until anchorToNewFramematches.size) {
            // Compare first best two neighbours
            val dMatchArray = anchorToNewFramematches[i].toArray()
            if (dMatchArray.size < 2) {
                continue
            }
            val m1 = dMatchArray[0]
            val m2 = dMatchArray[1]
            // If best neighbour and second best are far apart then it is a good match
            if (m1.distance <= m2.distance * NN_DIST_RATIO) {
                goodMatchesList.add(m1)
            }
        }
        if (goodMatchesList.size < ANCHOR_TO_NEW_FRAME_MATCHES_THRESHOLD) {
            // If good matches are too less => anchor image found
            return Status(NEW_FRAME_NOT_MATCHED, 0.0, 0.0, 0.0, 0)
        } else {
            // Get good keypoints from good matches
            val anchorFrameKeyPointsList = anchorFrameKeyPoints.toList()
            val newFrameKeyPointsList = newFrameKeyPoints.toList()
            val dxs = DoubleArray(goodMatchesList.size)
            val dys = DoubleArray(goodMatchesList.size)
            val A = arrayListOf<Double>()
            val B = arrayListOf<Double>()
            for (i in 0 until goodMatchesList.size) {
                val anchorFrameGoodKeyPoint =
                    anchorFrameKeyPointsList[goodMatchesList[i].queryIdx].pt
                val newFrameGoodKeyPoint = newFrameKeyPointsList[goodMatchesList[i].trainIdx].pt
                dxs[i] = newFrameGoodKeyPoint.x - anchorFrameGoodKeyPoint.x
                dys[i] = newFrameGoodKeyPoint.y - anchorFrameGoodKeyPoint.y
                A.add(anchorFrameGoodKeyPoint.x)
                A.add(anchorFrameGoodKeyPoint.y)
                B.add(newFrameGoodKeyPoint.x)
                B.add(newFrameGoodKeyPoint.y)
            }
            // Calculate rotation and translation
            val matA = create(A.toDoubleArray(), goodMatchesList.size, 2)
            val matB = create(B.toDoubleArray(), goodMatchesList.size, 2)
            val RT = getRotationInDegrees(matA, matB)
            val angleInDegrees = getAngleInDegreesFromR(RT.first)
            var dx = RT.second.getDouble(0, 0)
            var dy = RT.second.getDouble(0, 1)
            if (abs(angleInDegrees - prevStatus.angleInDegrees) > 0.1
                && pow(dx - prevStatus.dx, 2) + pow(dy - prevStatus.dy, 2) > 100
            ) {
                // High possibility of unwanted jerk => decrease the change in position
                dx = prevStatus.dx
                dy = prevStatus.dy
            }
            // Update prev status
            val newStatus = Status(NEW_FRAME_MATCHED, dx, dy, angleInDegrees, goodMatchesList.size)
            prevStatus = newStatus
            return newStatus
        }
    }

    private fun meanByColumns(mat: Matrix<Double>): Pair<Double, Double> {
        var cx = 0.0
        var cy = 0.0
        mat.forEachIndexed { row, col, element ->
            if (col == 0) {
                cx += element
            } else if (col == 1) {
                cy += element
            }
        }
        return Pair(
            cx / mat.numRows(),
            cy / mat.numRows()
        )
    }

    private fun subtract(
        mat: Matrix<Double>,
        centroid: Pair<Double, Double>
    ): Matrix<Double> {
        val matMinusCentroid = mat.mapIndexed { row, col, element ->
            if (col == 0) {
                element - centroid.first
            } else {
                element - centroid.second
            }
        }
        return matMinusCentroid
    }

    private fun getRotationInDegrees(
        endCoordinatedA: Matrix<Double>,
        endCoordinatedB: Matrix<Double>
    ): Pair<Matrix<Double>, Matrix<Double>> {
        // Centroid
        val actualOrigin = Pair(IMAGE_WIDTH.toDouble() / 2, IMAGE_HEIGHT.toDouble() / 2)
        val A = subtract(endCoordinatedA, actualOrigin)
        val B = subtract(endCoordinatedB, actualOrigin)
        val ACentroid = meanByColumns(A)
        val BCentroid = meanByColumns(B)
        // H, SVD
        val H = subtract(A, ACentroid).T * subtract(B, BCentroid)
        val USV = H.SVD()
        val U = USV.first
        val V = USV.third
        // R, T calculation
        val R = V * U.T
        val rotatedA = (R * A.T).T
        val TAsPair = meanByColumns(B - rotatedA)
        val T = mat[TAsPair.first, TAsPair.second]
        return Pair(R, T)
    }

    private fun getAngleInDegreesFromR(R: Matrix<Double>): Double {
        val cosTheta = R.getDouble(0, 0)
        val sinTheta = R.getDouble(1, 0)
        val theta = atan2(sinTheta, cosTheta)
        val thetaInDegrees = theta * 180 / PI
        return thetaInDegrees
    }

    private fun median(numArray: DoubleArray): Double {
        return if (numArray.size % 2 == 0) {
            (numArray[numArray.size / 2] + numArray[numArray.size / 2 - 1]) / 2
        } else {
            numArray[numArray.size / 2]
        }
    }
}
