package com.MythiCode.camerakit

import io.flutter.plugin.common.MethodChannel

interface FlutterMethodListener {
    fun onBarcodesRead(barcodes: List<String?>?)
    fun onTakePicture(result: MethodChannel.Result?, filePath: String?)
    fun onTakePictureFailed(
        result: MethodChannel.Result?,
        errorCode: String?,
        errorMessage: String?
    )
}