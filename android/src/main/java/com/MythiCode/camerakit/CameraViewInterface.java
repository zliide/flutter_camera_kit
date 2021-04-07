package com.MythiCode.camerakit;

import android.widget.FrameLayout;

import java.util.ArrayList;

import io.flutter.plugin.common.MethodChannel;

public interface CameraViewInterface {

    void initCamera(FrameLayout frameLayout, boolean hasBarcodeReader, char flashMode,
                    boolean isFillScale, ArrayList<Integer> restrictFormat, int cameraSelector);

    void setCameraVisible(boolean isCameraVisible);

    void changeFlashMode(char captureFlashMode);

    void takePicture(String path, final MethodChannel.Result result);

    void pauseCamera();

    void resumeCamera();

    void dispose();

}
