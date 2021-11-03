package com.MythiCode.camerakit

import android.widget.FrameLayout
import io.flutter.plugin.common.MethodChannel
import java.util.*

interface CameraViewInterface {
    fun initCamera(
        frameLayout: FrameLayout?, hasBarcodeReader: Boolean, flashMode: Char,
        isFillScale: Boolean, restrictFormat: ArrayList<Int?>?, cameraSelector: Int
    )

    fun setCameraVisible(isCameraVisible: Boolean)
    fun changeFlashMode(captureFlashMode: Char)
    fun takePicture(path: String?, result: MethodChannel.Result?)
    fun pauseCamera()
    fun resumeCamera()
    fun dispose()
}