package com.MythiCode.camerakit

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.res.Configuration
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.*
import java.util.concurrent.ExecutionException

class CameraViewX(
    private val activity: Activity,
    private val flutterMethodListener: FlutterMethodListener
) : CameraViewInterface {
    private var hasBarcodeReader = false
    private var previewFlashMode = 0.toChar()
    private var userCameraSelector = 0
    private var scanner: BarcodeScanner? = null
    private var displaySize: Point? = null
    private var options: BarcodeScannerOptions? = null
    private var previewView: PreviewView? = null
    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var isCameraVisible = true
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var cameraSelector: CameraSelector? = null
    private var preview: Preview? = null
    private var optimalPreviewSize: Size? = null
    override fun initCamera(
        frameLayout: FrameLayout?, hasBarcodeReader: Boolean, flashMode: Char,
        isFillScale: Boolean, restrictFormat: ArrayList<Int?>?, cameraSelector: Int
    ) {
        this.hasBarcodeReader = hasBarcodeReader
        previewFlashMode = flashMode
        userCameraSelector = cameraSelector
        if (hasBarcodeReader) {
            var format = 0
            for (f in restrictFormat!!) {
                format = format or f!!
            }
            options = BarcodeScannerOptions.Builder().setBarcodeFormats(format).build()
            scanner = BarcodeScanning.getClient(options!!)
        }
        displaySize = Point()

        activity.windowManager.defaultDisplay.getSize(displaySize)
        if (isFillScale) {
            frameLayout!!.layoutParams = FrameLayout.LayoutParams(
                displaySize!!.x,
                displaySize!!.y
            )
        }
        previewView = PreviewView(activity)
        previewView!!.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        frameLayout!!.addView(previewView)
        startCamera()
    }

    private fun prepareOptimalSize() {
        val width = previewView!!.width
        val height = previewView!!.height
        val manager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            for (cameraId in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(cameraId)

                // We don't use a front facing camera in this sample.
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    if (userCameraSelector == 0) continue
                } else {
                    if (userCameraSelector == 1) continue
                }
                val map = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
                ) ?: continue


                // Find out if we need to swap dimension to get the preview size relative to sensor
                // coordinate.
                val displayRotation = activity.windowManager.defaultDisplay.rotation
                val sensorOrientation =
                    characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)
                var swappedDimensions = false
                when (displayRotation) {
                    Surface.ROTATION_0, Surface.ROTATION_180 -> if (sensorOrientation == 90 || sensorOrientation == 270) {
                        swappedDimensions = true
                    }
                    Surface.ROTATION_90, Surface.ROTATION_270 -> if (sensorOrientation == 0 || sensorOrientation == 180) {
                        swappedDimensions = true
                    }
                    else -> Log.e(
                        ContentValues.TAG,
                        "Display rotation is invalid: $displayRotation"
                    )
                }
                var rotatedPreviewWidth = width
                var rotatedPreviewHeight = height
                var maxPreviewWidth = displaySize!!.x
                var maxPreviewHeight = displaySize!!.y
                if (swappedDimensions) {
                    rotatedPreviewWidth = height
                    rotatedPreviewHeight = width
                    maxPreviewWidth = displaySize!!.y
                    maxPreviewHeight = displaySize!!.x
                }
                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH
                }
                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT
                }

                // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.
                val previewSize = chooseOptimalSize(
                    map.getOutputSizes(
                        SurfaceTexture::class.java
                    ),
                    rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                    maxPreviewHeight
                )
                val orientation = activity.resources.configuration.orientation
                optimalPreviewSize = if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    Size(previewSize.width, previewSize.height)
                } else {
                    Size(previewSize.height, previewSize.width)
                }
                return
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
    }

    private fun startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(activity)
        cameraProviderFuture!!.addListener({
            try {
                cameraProvider = cameraProviderFuture!!.get()
                prepareOptimalSize()
                preview = Preview.Builder()
                    .setTargetResolution(
                        Size(
                            optimalPreviewSize!!.width,
                            optimalPreviewSize!!.height
                        )
                    )
                    .build()
                preview!!.setSurfaceProvider(previewView!!.surfaceProvider)
                if (hasBarcodeReader) {
                    imageAnalyzer = ImageAnalysis.Builder()
                        .build()
                    imageAnalyzer!!.setAnalyzer({ obj: Runnable -> obj.run() }, BarcodeAnalyzer())
                }
                cameraSelector =
                    if (userCameraSelector == 0) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
                bindCamera()
            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(activity))
    }

    private fun setFlashBarcodeReader() {
        if (camera != null) {
            camera!!.cameraControl.enableTorch(previewFlashMode == 'O')
        }
    }

    private fun bindCamera() {
        cameraProvider!!.unbindAll()
        if (hasBarcodeReader) {
            camera = cameraProvider!!.bindToLifecycle(
                (activity as LifecycleOwner), cameraSelector!!, preview, imageAnalyzer
            )
            setFlashBarcodeReader()
        } else {
            cameraProvider!!.bindToLifecycle(
                (activity as LifecycleOwner), cameraSelector!!, preview
            )
        }
    }

    override fun setCameraVisible(isCameraVisible: Boolean) {
        if (isCameraVisible != this.isCameraVisible) {
            this.isCameraVisible = isCameraVisible
            if (isCameraVisible) resumeCamera2() else pauseCamera2()
        }
    }

    override fun changeFlashMode(captureFlashMode: Char) {
        previewFlashMode = captureFlashMode
        if (hasBarcodeReader) {
            setFlashBarcodeReader()
        }
    }

    override fun pauseCamera() {}
    override fun resumeCamera() {
        if (hasBarcodeReader && isCameraVisible) {
            setFlashBarcodeReader()
        }
    }

    override fun dispose() {}
    internal class CompareSizesByArea : Comparator<Size> {
        override fun compare(lhs: Size, rhs: Size): Int {
            // We cast here to ensure the multiplications won't overflow
            return java.lang.Long.signum(
                lhs.width.toLong() * lhs.height -
                        rhs.width.toLong() * rhs.height
            )
        }
    }

    private fun pauseCamera2() {
        cameraProvider!!.unbindAll()
        if (scanner != null) {
            scanner!!.close()
            scanner = null
        }
    }

    private fun resumeCamera2() {
        if (isCameraVisible) {
            if (scanner == null && hasBarcodeReader) scanner = BarcodeScanning.getClient(options!!)
            startCamera()
        }
    }

    private inner class BarcodeAnalyzer : ImageAnalysis.Analyzer {
        override fun analyze(imageProxy: ImageProxy) {
            @SuppressLint("UnsafeOptInUsageError") val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image =
                    InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                scanner!!.process(image)
                    .addOnSuccessListener {
                        if (it.isNotEmpty()) {
                            val barcodesList: MutableList<String?> = ArrayList()
                            for (barcode in it) {
                                barcodesList.add(barcode.rawValue)
                            }
                            flutterMethodListener.onBarcodesRead(barcodesList)
                        }
                    }
                    .addOnFailureListener { e: Exception -> println("Error in reading barcode: " + e.message) }
                    .addOnCompleteListener { imageProxy.close() }
            }
        }
    }

    companion object {
        private const val MAX_PREVIEW_WIDTH = 1920
        private const val MAX_PREVIEW_HEIGHT = 1080
        private fun chooseOptimalSize(
            choices: Array<Size>, textureViewWidth: Int,
            textureViewHeight: Int, maxWidth: Int, maxHeight: Int
        ): Size {

            // Collect the supported resolutions that are at least as big as the preview Surface
            val bigEnough: MutableList<Size> = ArrayList()
            // Collect the supported resolutions that are smaller than the preview Surface
            val notBigEnough: MutableList<Size> = ArrayList()
            val w = 16
            val h = 9
            for (option in choices) {
                if (option.width <= maxWidth && option.height <= maxHeight && option.height == option.width * h / w) {
                    if (option.width >= textureViewWidth &&
                        option.height >= textureViewHeight
                    ) {
                        bigEnough.add(option)
                    } else {
                        notBigEnough.add(option)
                    }
                }
            }

            // Pick the smallest of those big enough. If there is no one big enough, pick the
            // largest of those not big enough.
            return when {
                bigEnough.size > 0 -> {
                    Collections.min(bigEnough, CompareSizesByArea())
                }
                notBigEnough.size > 0 -> {
                    Collections.max(notBigEnough, CompareSizesByArea())
                }
                else -> {
                    Log.e(ContentValues.TAG, "Couldn't find any suitable preview size")
                    choices[0]
                }
            }
        }
    }
}