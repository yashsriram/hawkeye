package com.firefly.app

import android.app.Activity
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import com.firefly.app.engine.VisualOdometer2D
import kotlinx.android.synthetic.main.activity_visual_odometer.*
import org.opencv.android.*
import org.opencv.core.Mat
import org.opencv.features2d.DescriptorExtractor
import org.opencv.features2d.DescriptorMatcher
import org.opencv.features2d.FeatureDetector
import kotlin.math.roundToInt

class VisualOdometerActivity : Activity(), CameraBridgeViewBase.CvCameraViewListener2 {
    private val MAX_WIDTH = 600
    private val MAX_HEIGHT = 400
    private val ANCHOR_FRAME_MATCHES_THRESHOLD = 20
    private val ANCHOR_TO_NEW_FRAME_MATCHES_THRESHOLD = 10
    private val NN_DIST_RATIO = 0.7
    private val FEATURE_DETECTOR_TYPE = FeatureDetector.AKAZE
    private val DESCRIPTOR_EXTRACTOR_TYPE = DescriptorExtractor.AKAZE
    private val DESCRIPTOR_MATCHER_TYPE = DescriptorMatcher.BRUTEFORCE_HAMMING

    private var visualOdometer2D: VisualOdometer2D? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_visual_odometer)
        cameraView.setCvCameraViewListener(this)
        anchorResetButton.setOnClickListener { visualOdometer2D!!.reset() }
    }

    private val loaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                SUCCESS -> {
                    Log.d("dontbugme", "OpenCV loaded successfully")
                    cameraView.setMaxFrameSize(MAX_WIDTH, MAX_HEIGHT)
                    cameraView.enableView()
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }

    public override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            Log.d(
                "dontbugme",
                "Internal OpenCV library not found. Using OpenCV Manager for initialization"
            )
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, loaderCallback)
        } else {
            Log.d("dontbugme", "OpenCV library found inside package. Using it!")
            loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        Log.d("dontbugme", "onCameraViewStarted")
        visualOdometer2D = VisualOdometer2D(
            FeatureDetector.create(FEATURE_DETECTOR_TYPE),
            DescriptorExtractor.create(DESCRIPTOR_EXTRACTOR_TYPE),
            DescriptorMatcher.create(DESCRIPTOR_MATCHER_TYPE),
            ANCHOR_FRAME_MATCHES_THRESHOLD,
            ANCHOR_TO_NEW_FRAME_MATCHES_THRESHOLD,
            NN_DIST_RATIO
        )
    }


    public override fun onPause() {
        super.onPause()
        cameraView.disableView()
    }

    override fun onCameraViewStopped() {
        Log.d("dontbugme", "onCameraViewStopped")
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        val inputImg = inputFrame!!.rgba()
        val status = visualOdometer2D!!.feed(inputImg)
        when (status.state) {
            VisualOdometer2D.NEW_FRAME_MATCHED -> {
                SocketHolder.send("${status.dx.roundToInt()} ${status.dy.roundToInt()}")
                statusView.post {
                    statusView.text =
                        "dx = ${status.dx.roundToInt()}\n dy = ${status.dy.roundToInt()}\n angle = ${status.angleInDegrees.roundToInt()}\n #matches = ${status.numMatches}"
                }
            }
            VisualOdometer2D.FOUND_ANCHOR -> {
                val anchorBitMap = Bitmap.createBitmap(
                    inputImg.cols(),
                    inputImg.rows(),
                    Bitmap.Config.ARGB_8888
                )
                Utils.matToBitmap(inputImg, anchorBitMap)
                anchorImageView.post { anchorImageView.setImageBitmap(anchorBitMap) }
                SocketHolder.send("reset")
                statusView.post { statusView.text = "Found Anchor Image" }
            }
            VisualOdometer2D.ANCHOR_NOT_FOUND -> {
                statusView.post { statusView.text = "Finding Anchor Image" }
            }
            VisualOdometer2D.NEW_FRAME_NOT_MATCHED -> {
                statusView.post { statusView.text = "Anchor image out of sight" }
            }
        }

        return inputImg
    }

}
