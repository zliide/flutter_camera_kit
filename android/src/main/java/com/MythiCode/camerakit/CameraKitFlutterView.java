package com.MythiCode.camerakit;

import android.Manifest;
import android.content.pm.PackageManager;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.platform.PlatformView;

public class CameraKitFlutterView implements PlatformView, MethodChannel.MethodCallHandler, FlutterMethodListener {

    private static final int REQUEST_CAMERA_PERMISSION = 10001;
    private final MethodChannel channel;
    private final ActivityPluginBinding activityPluginBinding;
    private CameraBaseView cameraView;

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull final MethodChannel.Result result) {
        switch (call.method) {
            case "requestPermission":
                if (ActivityCompat.checkSelfPermission(activityPluginBinding.getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(activityPluginBinding.getActivity(), new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                    activityPluginBinding.addRequestPermissionsResultListener(new PluginRegistry.RequestPermissionsResultListener() {
                        @Override
                        public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
                            for (int i :
                                    grantResults) {
                                if (i == PackageManager.PERMISSION_DENIED) {
                                    try {
                                        result.success(false);
                                    } catch (Exception ignored) {

                                    }
                                    return false;
                                }
                            }
                            try {
                                result.success(true);
                            } catch (Exception ignored) {

                            }
                            return false;
                        }
                    });
                    return;
                } else {
                    try {
                        result.success(true);
                    } catch (Exception ignored) {

                    }
                }
                break;
            case "initCamera":
                boolean hasBarcodeReader = call.argument("hasBarcodeReader");
                char flashMode = call.argument("flashMode").toString().charAt(0);
                boolean isFillScale = call.argument("isFillScale");
                int barcodeMode = call.argument("barcodeMode");
                int androidCameraMode = call.argument("androidCameraMode");
                int cameraSelector = call.argument("cameraSelector");
                getCameraView().initCamera(hasBarcodeReader, flashMode, isFillScale, barcodeMode, androidCameraMode, cameraSelector);
                break;
            case "resumeCamera":
                getCameraView().resumeCamera();

                break;
            case "pauseCamera":
                getCameraView().pauseCamera();
                break;
            case "takePicture":
                String path = call.argument("path").toString();
                getCameraView().takePicture(path, result);
                break;
            case "changeFlashMode":
                char captureFlashMode = call.argument("flashMode").toString().charAt(0);
                getCameraView().changeFlashMode(captureFlashMode);
                break;
            case "dispose":
                dispose();
                break;
            case "setCameraVisible":
                boolean isCameraVisible = call.argument("isCameraVisible");
                getCameraView().setCameraVisible(isCameraVisible);
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    private CameraBaseView getCameraView() {
        return cameraView;
    }

    public CameraKitFlutterView(ActivityPluginBinding activityPluginBinding, @NonNull BinaryMessenger messenger, int id) {
        this.channel = new MethodChannel(messenger, "plugins/camera_kit_" + id);
        this.activityPluginBinding = activityPluginBinding;
        this.channel.setMethodCallHandler(this);
        if (getCameraView() == null) {
            cameraView = new CameraBaseView(activityPluginBinding.getActivity(), this);
        }
    }

    @Override
    public View getView() {
        return getCameraView().getView();
    }

    @Override
    public void dispose() {
        if (getCameraView() != null) {
            getCameraView().dispose();
        }
    }

    @Override
    public void onBarcodeRead(String barcode) {
        channel.invokeMethod("onBarcodeRead", barcode);
    }

    @Override
    public void onTakePicture(final MethodChannel.Result result, final String filePath) {
        activityPluginBinding.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                result.success(filePath);
            }
        });
    }

    @Override
    public void onTakePictureFailed(final MethodChannel.Result result, final String errorCode, final String errorMessage) {
        activityPluginBinding.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                result.error(errorCode, errorMessage, null);
            }
        });
    }
}
