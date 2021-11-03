package com.MythiCode.camerakit;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import io.flutter.plugin.common.MethodChannel;

public class CameraViewX implements CameraViewInterface {

    private static final int MAX_PREVIEW_WIDTH = 1920;
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    private final Activity activity;
    private final FlutterMethodListener flutterMethodListener;
    private ImageCapture imageCapture;
    private boolean hasBarcodeReader;
    private char previewFlashMode;
    private int userCameraSelector;
    private BarcodeScanner scanner;
    private Point displaySize;
    private BarcodeScannerOptions options;
    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageAnalysis imageAnalyzer;
    private boolean isCameraVisible = true;
    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private CameraSelector cameraSelector;
    private Preview preview;
    private Size optimalPreviewSize;


    public CameraViewX(Activity activity, FlutterMethodListener flutterMethodListener) {
        this.activity = activity;
        this.flutterMethodListener = flutterMethodListener;
    }

    @Override
    public void initCamera(FrameLayout linearLayout, boolean hasBarcodeReader, char flashMode,
                           boolean isFillScale, ArrayList<Integer> restrictFormat, int cameraSelector) {
        this.hasBarcodeReader = hasBarcodeReader;
        this.previewFlashMode = flashMode;
        userCameraSelector = cameraSelector;
        if (hasBarcodeReader) {
            int format = 0;
            for (int f : restrictFormat) {
                format |= f;
            }
            options = new BarcodeScannerOptions.Builder().setBarcodeFormats(format).build();
            scanner = BarcodeScanning.getClient(options);
        }
        displaySize = new Point();
        activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
        if (isFillScale) {
            linearLayout.setLayoutParams(new FrameLayout.LayoutParams(
                    displaySize.x,
                    displaySize.y));
        }

        previewView = new PreviewView(activity);
        previewView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        linearLayout.addView(previewView);
        startCamera();

    }


    private int getFlashMode() {
        switch (previewFlashMode) {
            case 'O':
                return ImageCapture.FLASH_MODE_ON;
            case 'F':
                return ImageCapture.FLASH_MODE_OFF;
            default:
                return ImageCapture.FLASH_MODE_AUTO;
        }
    }


    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth, int maxHeight) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();

        int w = 16;
        int h = 9;
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }


    private void prepareOptimalSize() {
        int width = previewView.getWidth();
        int height = previewView.getHeight();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

                // We don't use a front facing camera in this sample.
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    if (userCameraSelector == 0)
                        continue;
                } else {
                    if (userCameraSelector == 1)
                        continue;
                }
                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }


                // Find out if we need to swap dimension to get the preview size relative to sensor
                // coordinate.
                int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
                Integer sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                boolean swappedDimensions = false;
                switch (displayRotation) {
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if (sensorOrientation == 90 || sensorOrientation == 270) {
                            swappedDimensions = true;
                        }
                        break;
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if (sensorOrientation == 0 || sensorOrientation == 180) {
                            swappedDimensions = true;
                        }
                        break;
                    default:
                        Log.e(TAG, "Display rotation is invalid: " + displayRotation);
                }


                int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;

                if (swappedDimensions) {
                    rotatedPreviewWidth = height;
                    rotatedPreviewHeight = width;
                    maxPreviewWidth = displaySize.y;
                    maxPreviewHeight = displaySize.x;
                }

                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH;
                }

                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                }

                // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.
                Size previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                        rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                        maxPreviewHeight);
                int orientation = activity.getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    optimalPreviewSize = new Size(previewSize.getWidth(), previewSize.getHeight());
                } else {
                    optimalPreviewSize = new Size(previewSize.getHeight(), previewSize.getWidth());
                }

                return;
            }
        } catch (CameraAccessException | NullPointerException e) {
            e.printStackTrace();
        }

    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(activity);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                prepareOptimalSize();
                preview = new Preview.Builder()
                        .setTargetResolution(new Size(optimalPreviewSize.getWidth(), optimalPreviewSize.getHeight()))
                        .build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());


                imageCapture = new ImageCapture.Builder()
                        .setFlashMode(getFlashMode())
                        .setTargetResolution(new Size(optimalPreviewSize.getWidth(), optimalPreviewSize.getHeight()))
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                if (hasBarcodeReader) {
                    imageAnalyzer = new ImageAnalysis.Builder()
                            .build();
                    imageAnalyzer.setAnalyzer(Runnable::run, new BarcodeAnalyzer());
                }


                if (userCameraSelector == 0)
                    cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                else cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

                bindCamera();


            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(activity));
    }


    void setFlashBarcodeReader() {
        if (camera != null) {
            camera.getCameraControl().enableTorch(previewFlashMode == 'O');
        }
    }

    private void bindCamera() {
        cameraProvider.unbindAll();
        if (hasBarcodeReader) {
            camera = cameraProvider.bindToLifecycle((LifecycleOwner) activity, cameraSelector
                    , preview, imageCapture, imageAnalyzer);
            setFlashBarcodeReader();
        } else {
            cameraProvider.bindToLifecycle((LifecycleOwner) activity, cameraSelector
                    , preview, imageCapture);
        }
    }


    @Override
    public void setCameraVisible(boolean isCameraVisible) {
        if (isCameraVisible != this.isCameraVisible) {
            this.isCameraVisible = isCameraVisible;
            if (isCameraVisible) resumeCamera2();
            else pauseCamera2();
        }
    }

    @Override
    public void changeFlashMode(char newPreviewFlashMode) {
        previewFlashMode = newPreviewFlashMode;
        imageCapture.setFlashMode(getFlashMode());
        if (hasBarcodeReader) {
            setFlashBarcodeReader();
        }
    }


    @Override
    public void takePicture(String path, final MethodChannel.Result result) {
        final File file = getPictureFile(path);
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(file).build();


        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
                outputOptions, ContextCompat.getMainExecutor(activity), new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        flutterMethodListener.onTakePicture(result, file + "");
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        flutterMethodListener.onTakePictureFailed(result, "-1", exception.getMessage());
                    }
                });
    }

    @Override
    public void pauseCamera() {

    }

    @Override
    public void resumeCamera() {
        if (hasBarcodeReader && isCameraVisible) {
            setFlashBarcodeReader();
        }
    }

    @Override
    public void dispose() {

    }

    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }


    public void pauseCamera2() {
        cameraProvider.unbindAll();
        if (scanner != null) {
            scanner.close();
            scanner = null;
        }
    }

    public void resumeCamera2() {
        if (isCameraVisible) {
            if (scanner == null && hasBarcodeReader) scanner = BarcodeScanning.getClient(options);
            startCamera();
        }
    }

    private File getPictureFile(String path) {
        if (path.equals(""))
            return new File(activity.getCacheDir(), "pic.jpg");
        else return new File(path);
    }


    private class BarcodeAnalyzer implements ImageAnalysis.Analyzer {


        @Override
        public void analyze(@NonNull final ImageProxy imageProxy) {
            @SuppressLint("UnsafeOptInUsageError") Image mediaImage = imageProxy.getImage();
            if (mediaImage != null) {
                InputImage image =
                        InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

                scanner.process(image)
                        .addOnSuccessListener(barcodes -> {
                            if (barcodes.size() > 0) {
                                List<String> barcodesList = new ArrayList<>();
                                for (Barcode barcode : barcodes) {
                                    barcodesList.add(barcode.getRawValue());
                                }
                                flutterMethodListener.onBarcodesRead(barcodesList);
                            }
                        })
                        .addOnFailureListener(e -> System.out.println("Error in reading barcode: " + e.getMessage()))
                        .addOnCompleteListener(task -> imageProxy.close());
            }
        }
    }


}
