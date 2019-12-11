package com.firefly.app.engine

import org.opencv.core.*
import org.opencv.features2d.DescriptorExtractor
import org.opencv.features2d.DescriptorMatcher
import org.opencv.features2d.FeatureDetector
import java.util.*
import kotlin.collections.ArrayList

class Status(
    val state: Int,
    val dx: Double,
    val dy: Double,
    val numMatches: Int
)

class VisualOdometer2D(
    private val featureDetector: FeatureDetector,
    private val descriptorExtractor: DescriptorExtractor,
    private val descriptorMatcher: DescriptorMatcher,
    private val ANCHOR_FRAME_MATCHES_THRESHOLD: Int,
    private val ANCHOR_TO_NEW_FRAME_MATCHES_THRESHOLD: Int,
    private val NN_DIST_RATIO: Double
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
            return Status(ANCHOR_NOT_FOUND, 0.0, 0.0, 0)
        } else {
            descriptorExtractor.compute(frame1, anchorFrameKeyPoints, anchorFrameDescriptors)
            this.anchorFrame = frame1.clone()
            return Status(FOUND_ANCHOR, 0.0, 0.0, numKPs)
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
            return Status(NEW_FRAME_NOT_MATCHED, 0.0, 0.0, 0)
        } else {
            // Get good keypoints from good matches
            val anchorFrameKeyPointsList = anchorFrameKeyPoints.toList()
            val newFrameKeyPointsList = newFrameKeyPoints.toList()
            val dxs = DoubleArray(goodMatchesList.size)
            val dys = DoubleArray(goodMatchesList.size)
            for (i in 0 until goodMatchesList.size) {
                val f1GoodKeyPoint = anchorFrameKeyPointsList[goodMatchesList[i].queryIdx].pt
                val f2GoodKeyPoint = newFrameKeyPointsList[goodMatchesList[i].trainIdx].pt
                dxs[i] = f2GoodKeyPoint.x - f1GoodKeyPoint.x
                dys[i] = f2GoodKeyPoint.y - f1GoodKeyPoint.y
            }
            Arrays.sort(dxs)
            Arrays.sort(dys)
            val dx = -median(dxs)
            val dy = median(dys)
            return Status(NEW_FRAME_MATCHED, dx, dy, goodMatchesList.size)
        }
    }

    private fun median(numArray: DoubleArray): Double {
        return if (numArray.size % 2 == 0) {
            (numArray[numArray.size / 2] + numArray[numArray.size / 2 - 1]) / 2
        } else {
            numArray[numArray.size / 2]
        }
    }
}
