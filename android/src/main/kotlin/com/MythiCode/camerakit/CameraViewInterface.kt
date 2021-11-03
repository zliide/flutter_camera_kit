package com.MythiCode.camerakit

import android.widget.FrameLayout
import java.util.*

interface CameraViewInterface {
    fun initCamera(
        frameLayout: FrameLayout?, hasBarcodeReader: Boolean, flashMode: Char,
        isFillScale: Boolean, restrictFormat: ArrayList<Int?>?, cameraSelector: Int
    )

    fun setCameraVisible(isCameraVisible: Boolean)
    fun changeFlashMode(captureFlashMode: Char)
    fun pauseCamera()
    fun resumeCamera()
    fun dispose()
}