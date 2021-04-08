package com.MythiCode.camerakit;

import java.util.List;

import io.flutter.plugin.common.MethodChannel;

public interface FlutterMethodListener {

    void onBarcodesRead(List<String> barcodes);

    void onTakePicture(MethodChannel.Result result, String filePath);

    void onTakePictureFailed(MethodChannel.Result result, String errorCode, String errorMessage);
}
