package com.MythiCode.camerakit

import android.Manifest
import android.content.pm.PackageManager
import android.view.View
import androidx.core.app.ActivityCompat
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.platform.PlatformView
import java.util.*

class CameraKitFlutterView(
    private val activityPluginBinding: ActivityPluginBinding,
    messenger: BinaryMessenger,
    id: Int
) : PlatformView, MethodCallHandler, FlutterMethodListener {
    private val channel: MethodChannel = MethodChannel(messenger, "plugins/camera_kit_$id")
    private var cameraView: CameraBaseView? = null
    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "requestPermission" -> if (ActivityCompat.checkSelfPermission(
                    activityPluginBinding.activity,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    activityPluginBinding.activity,
                    arrayOf(Manifest.permission.CAMERA),
                    REQUEST_CAMERA_PERMISSION
                )
                activityPluginBinding.addRequestPermissionsResultListener { _: Int, _: Array<String?>?, grantResults: IntArray ->
                    for (i in grantResults) {
                        if (i == PackageManager.PERMISSION_DENIED) {
                            try {
                                result.success(false)
                            } catch (ignored: Exception) {
                            }
                            return@addRequestPermissionsResultListener false
                        }
                    }
                    try {
                        result.success(true)
                    } catch (ignored: Exception) {
                    }
                    false
                }
                return
            } else {
                try {
                    result.success(true)
                } catch (ignored: Exception) {
                }
            }
            "initCamera" -> {
                val hasBarcodeReader = call.argument<Boolean>("hasBarcodeReader")!!
                val flashMode = call.argument<Any>("flashMode").toString()[0]
                val isFillScale = call.argument<Boolean>("isFillScale")!!
                val restrictFormat = call.argument<ArrayList<Int?>>("restrictFormat")!!
                val cameraSelector = call.argument<Int>("cameraSelector")!!
                cameraView!!.initCamera(
                    hasBarcodeReader,
                    flashMode,
                    isFillScale,
                    restrictFormat,
                    cameraSelector
                )
            }
            "resumeCamera" -> cameraView!!.resumeCamera()
            "pauseCamera" -> cameraView!!.pauseCamera()
            "changeFlashMode" -> {
                val captureFlashMode = call.argument<Any>("flashMode").toString()[0]
                cameraView!!.changeFlashMode(captureFlashMode)
            }
            "dispose" -> dispose()
            "setCameraVisible" -> {
                val isCameraVisible = call.argument<Boolean>("isCameraVisible")!!
                cameraView!!.setCameraVisible(isCameraVisible)
            }
            else -> result.notImplemented()
        }
    }

    override fun getView(): View {
        return cameraView!!.view
    }

    override fun dispose() {
        cameraView?.dispose()
    }

    override fun onBarcodesRead(barcodes: List<String?>?) {
        channel.invokeMethod("onBarcodesRead", barcodes)
    }

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 10001
    }

    init {
        channel.setMethodCallHandler(this)
        if (cameraView == null) {
            cameraView = CameraBaseView(activityPluginBinding.activity, this)
        }
    }
}