package com.MythiCode.camerakit

import android.app.Activity
import android.graphics.Color
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import io.flutter.plugin.platform.PlatformView
import java.util.*

class CameraBaseView(
    private val activity: Activity,
    private val flutterMethodListener: FlutterMethodListener
) : PlatformView {
    private val linearLayout: FrameLayout = FrameLayout(activity)
    private var cameraViewInterface: CameraViewInterface? = null

    fun initCamera(
        hasBarcodeReader: Boolean,
        flashMode: Char,
        isFillScale: Boolean,
        restrictFormat: ArrayList<Int?>?,
        cameraSelector: Int
    ) {
        cameraViewInterface = CameraViewX(activity, flutterMethodListener).apply {
            initCamera(
                linearLayout,
                hasBarcodeReader,
                flashMode,
                isFillScale,
                restrictFormat,
                cameraSelector
            )
        }
    }

    fun setCameraVisible(isCameraVisible: Boolean) {
        if (cameraViewInterface != null) cameraViewInterface!!.setCameraVisible(isCameraVisible)
    }

    fun changeFlashMode(captureFlashMode: Char) {
        cameraViewInterface!!.changeFlashMode(captureFlashMode)
    }

    fun pauseCamera() {
        cameraViewInterface!!.pauseCamera()
    }

    fun resumeCamera() {
        cameraViewInterface!!.resumeCamera()
    }

    override fun getView(): View {
        return linearLayout
    }

    override fun dispose() {
        if (cameraViewInterface != null) cameraViewInterface!!.dispose()
    }

    init {
        linearLayout.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        linearLayout.setBackgroundColor(Color.argb(0,0,0,0))
    }
}