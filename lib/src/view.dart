import 'dart:async';
import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:visibility_detector/visibility_detector.dart';

import 'controller.dart';

typedef BarcodesReadCallback = void Function(List<String> barcodes);

enum CameraFlashMode { on, off, auto }
enum ScaleTypeMode { fit, fill }
enum CameraSelector { front, back }
enum BarcodeFormat {
  aztec,
  codaBar,
  code128,
  code39,
  code93,
  dataMatrix,
  ean13,
  ean8,
  itf,
  pdf417,
  qrCode,
  upcA,
  upcE,
}

// ignore: must_be_immutable
class CameraKitView extends StatefulWidget {
  /// In barcodeReader mode, while camera preview detect barcodes, This method is called.
  final BarcodesReadCallback? onBarcodesRead;

  ///After android and iOS user deny run time permission, this method is called.
  final VoidCallback? onPermissionDenied;

  ///There are 2 modes `ScaleTypeMode.fill` and `ScaleTypeMode.fit` for this parameter.
  ///If you want camera preview fill your widget area, use `fill` mode. In this mode, camera preview may be croped for filling widget area.
  ///If you want camera preview to show entire lens preview, use `fit` mode. In this mode, camera preview may be shows blank areas.
  final ScaleTypeMode scaleType;

  ///True means scan barcode mode and false means take picture mode
  ///Because of performance reasons, you can't use barcode reader mode and take picture mode simultaneously.
  final bool hasBarcodeReader;

  ///This parameter accepts 3 values. `CameraFlashMode.auto`, `CameraFlashMode.on` and `CameraFlashMode.off`.
  /// For changing value after initial use `changeFlashMode` method in controller.
  final CameraFlashMode previewFlashMode;

  ///Set barcode format from available values, default value is FORMAT_ALL_FORMATS
  final List<BarcodeFormat> restrictFormat;

  ///Controller for this widget
  final CameraKitController? cameraKitController;

  ///Set front and back camera
  final CameraSelector cameraSelector;

  late _BarcodeScannerViewState viewState;

  CameraKitView(
      {Key? key,
      this.hasBarcodeReader = false,
      this.scaleType = ScaleTypeMode.fill,
      this.onBarcodesRead,
      this.restrictFormat = const [],
      this.previewFlashMode = CameraFlashMode.auto,
      this.cameraKitController,
      this.onPermissionDenied,
      this.cameraSelector = CameraSelector.back})
      : super(key: key);

  void dispose() {
    viewState.disposeView();
  }

  @override
  State<StatefulWidget> createState() {
    if (cameraKitController != null) cameraKitController!.setView(this);
    viewState = _BarcodeScannerViewState();
    return viewState;
  }
}

class _BarcodeScannerViewState extends State<CameraKitView>
    with WidgetsBindingObserver {
  NativeCameraKitController? controller;
  late VisibilityDetector visibilityDetector;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance!.addObserver(this);
    visibilityDetector = VisibilityDetector(
      key: const Key('visible-camerakit-key-1'),
      onVisibilityChanged: (visibilityInfo) {
        controller?.setCameraVisible(visibilityInfo.visibleFraction != 0);
      },
      child: defaultTargetPlatform == TargetPlatform.android
          ? AndroidView(
              viewType: 'plugins/camera_kit',
              onPlatformViewCreated: _onPlatformViewCreated,
            )
          : UiKitView(
              viewType: 'plugins/camera_kit',
              onPlatformViewCreated: _onPlatformViewCreated,
            ),
    );
  }

  @override
  Widget build(BuildContext context) => visibilityDetector;

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    switch (state) {
      case AppLifecycleState.resumed:
        print('Flutter Life Cycle: resumed');
        if (controller != null) {
          controller!.resumeCamera();
        }
        break;
      case AppLifecycleState.inactive:
        print('Flutter Life Cycle: inactive');
        if (Platform.isIOS) {
          controller!.pauseCamera();
        }
        break;
      case AppLifecycleState.paused:
        print('Flutter Life Cycle: paused');
        controller!.pauseCamera();
        break;
      default:
        break;
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance!.removeObserver(this);
    super.dispose();
  }

  void _onPlatformViewCreated(int id) {
    controller = NativeCameraKitController._(id, context, widget);
    controller!.initCamera();
  }

  void disposeView() {
    controller!.dispose();
  }
}

///View State controller. User works with CameraKitController
///and CameraKitController Works with this controller.
class NativeCameraKitController {
  BuildContext context;
  CameraKitView widget;

  NativeCameraKitController._(int id, this.context, this.widget)
      : _channel = MethodChannel('plugins/camera_kit_' + id.toString());

  final MethodChannel _channel;

  Future<dynamic> nativeMethodCallHandler(MethodCall methodCall) async {
    if (methodCall.method == 'onBarcodesRead') {
      if (widget.onBarcodesRead != null)
        widget.onBarcodesRead!(List.from(methodCall.arguments));
    }

    return null;
  }

  bool _getScaleTypeMode(ScaleTypeMode scaleType) {
    if (scaleType == ScaleTypeMode.fill)
      return true;
    else
      return false;
  }

  String _getCharFlashMode(CameraFlashMode cameraFlashMode) {
    switch (cameraFlashMode) {
      case CameraFlashMode.auto:
        return 'A';
      case CameraFlashMode.on:
        return 'O';
      case CameraFlashMode.off:
        return 'F';
    }
  }

  int _getCameraSelector(CameraSelector cameraSelector) {
    switch (cameraSelector) {
      case CameraSelector.back:
        return 0;
      case CameraSelector.front:
        return 1;
    }
  }

  Future<void> initCamera() async {
    _channel.setMethodCallHandler(nativeMethodCallHandler);
    _channel.invokeMethod<bool>('requestPermission').then((value) {
      if (value ?? false) {
        final formats = widget.restrictFormat
            .map((f) => _getBarcodeFormatValue(f))
            .toList();
        if (Platform.isAndroid) {
          _channel.invokeMethod<void>('initCamera', {
            'hasBarcodeReader': widget.hasBarcodeReader,
            'flashMode': _getCharFlashMode(widget.previewFlashMode),
            'isFillScale': _getScaleTypeMode(widget.scaleType),
            'restrictFormat': formats,
            'cameraSelector': _getCameraSelector(widget.cameraSelector)
          });
        } else {
          _channel.invokeMethod<void>('initCamera', {
            'hasBarcodeReader': widget.hasBarcodeReader,
            'flashMode': _getCharFlashMode(widget.previewFlashMode),
            'isFillScale': _getScaleTypeMode(widget.scaleType),
            'restrictFormat': formats,
            'cameraSelector': _getCameraSelector(widget.cameraSelector)
          });
        }
      } else {
        widget.onPermissionDenied!();
      }
    });
  }

  ///Call resume camera in Native API
  Future<void> resumeCamera() => _channel.invokeMethod('resumeCamera');

  ///Call pause camera in Native API
  Future<void> pauseCamera() => _channel.invokeMethod('pauseCamera');

  ///Call take picture in Native API
  Future<String?> takePicture(String path) =>
      _channel.invokeMethod('takePicture', {'path': path});

  ///Call change flash mode in Native API
  Future<void> changeFlashMode(CameraFlashMode captureFlashMode) =>
      _channel.invokeMethod('changeFlashMode',
          {'flashMode': _getCharFlashMode(captureFlashMode)});

  ///Call dispose in Native API
  Future<void> dispose() => _channel.invokeMethod('dispose');

  ///Call set camera visible in Native API.
  ///This API is used to automatically manage pause and resume camera
  Future<void> setCameraVisible(bool isCameraVisible) => _channel
      .invokeMethod('setCameraVisible', {'isCameraVisible': isCameraVisible});

  int _getBarcodeFormatValue(BarcodeFormat format) {
    switch (format) {
      case BarcodeFormat.code128:
        return 1;
      case BarcodeFormat.code39:
        return 2;
      case BarcodeFormat.code93:
        return 4;
      case BarcodeFormat.codaBar:
        return 8;
      case BarcodeFormat.dataMatrix:
        return 16;
      case BarcodeFormat.ean13:
        return 32;
      case BarcodeFormat.ean8:
        return 64;
      case BarcodeFormat.itf:
        return 128;
      case BarcodeFormat.qrCode:
        return 256;
      case BarcodeFormat.upcA:
        return 512;
      case BarcodeFormat.upcE:
        return 1024;
      case BarcodeFormat.pdf417:
        return 2048;
      case BarcodeFormat.aztec:
        return 4096;

      default:
        return 0;
    }
  }
}
