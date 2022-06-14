// ignore_for_file: library_private_types_in_public_api, avoid_print

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

class CameraKitView extends StatefulWidget {
  /// In barcodeReader mode, while camera preview detect barcodes, This method is called.
  final BarcodesReadCallback? onBarcodesRead;

  ///After android and iOS user deny run time permission, this method is called.
  final VoidCallback? onPermissionDenied;

  ///There are 2 modes `ScaleTypeMode.fill` and `ScaleTypeMode.fit` for this parameter.
  ///If you want camera preview fill your widget area, use `fill` mode. In this mode, camera preview may be cropped for filling widget area.
  ///If you want camera preview to show entire lens preview, use `fit` mode. In this mode, camera preview may be shows blank areas.
  final ScaleTypeMode scaleType;

  ///True means scan barcode mode and false means take picture mode
  ///Because of performance reasons, you can't use barcode reader mode and take picture mode simultaneously.
  final bool hasBarcodeReader;

  ///True means that the view is initially obscured.
  final bool initiallyVisible;

  ///This parameter accepts 3 values. `CameraFlashMode.auto`, `CameraFlashMode.on` and `CameraFlashMode.off`.
  /// For changing value after initial use `changeFlashMode` method in controller.
  final CameraFlashMode initialFlashMode;

  ///Set barcode format from available values, default value is FORMAT_ALL_FORMATS
  final List<BarcodeFormat> restrictFormat;

  ///Controller for this widget
  final CameraKitController? controller;

  ///Set front and back camera
  final CameraSelector cameraSelector;

  CameraKitView({
    Key? key,
    this.initiallyVisible = true,
    this.hasBarcodeReader = false,
    this.scaleType = ScaleTypeMode.fill,
    this.onBarcodesRead,
    this.restrictFormat = const [],
    this.initialFlashMode = CameraFlashMode.auto,
    this.controller,
    this.onPermissionDenied,
    this.cameraSelector = CameraSelector.back,
  }) : super(key: key);

  @override
  State<StatefulWidget> createState() => _BarcodeScannerViewState();
}

class _BarcodeScannerViewState extends State<CameraKitView>
    with WidgetsBindingObserver {
  _NativeCameraKitController? _nativeController;
  CameraKitController? _localController;
  CameraKitController? get _effectiveController =>
      widget.controller ?? _localController;

  bool _lifeCyclePaused = false;
  bool _visible = true;
  CameraFlashMode _flashMode = CameraFlashMode.auto;

  @override
  void initState() {
    super.initState();
    if (widget.controller == null) {
      _localController = CameraKitController(
          active: widget.initiallyVisible, flashMode: widget.initialFlashMode);
    }
    _visible = !_effectiveController!.value.active;
    _flashMode = _effectiveController!.value.flashMode;
    _effectiveController!.addListener(_onControllerValueChanged);
    _binding().addObserver(this);
  }

  @override
  void didUpdateWidget(covariant CameraKitView oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.controller != oldWidget.controller) {
      oldWidget.controller?.removeListener(_onControllerValueChanged);
      if (widget.controller == null) {
        _localController = CameraKitController(
          active: oldWidget.controller!.value.active,
          flashMode: oldWidget.controller!.value.flashMode,
        );
      }
      _effectiveController!.addListener(_onControllerValueChanged);
    }
  }

  @override
  void dispose() {
    _binding().removeObserver(this);
    _nativeController
        ?.setCameraVisible(false)
        .then((_) => _nativeController?.dispose());
    _nativeController = null;
    _effectiveController?.removeListener(_onControllerValueChanged);
    _localController?.dispose();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    switch (state) {
      case AppLifecycleState.resumed:
        _lifeCyclePaused = false;
        _nativeController?.resumeCamera();
        break;
      case AppLifecycleState.inactive:
        if (Platform.isIOS) {
          _lifeCyclePaused = true;
          _nativeController?.pauseCamera();
        }
        break;
      case AppLifecycleState.paused:
        _lifeCyclePaused = true;
        _nativeController?.pauseCamera();
        break;
      default:
        break;
    }
  }

  void _onControllerValueChanged() {
    final controller = _nativeController;
    if (controller == null) {
      return;
    }
    final value = _effectiveController!.value;
    if (value.active != _visible) {
      controller.setCameraVisible(value.active);
      _visible = value.active;
    }
    if (value.flashMode != _flashMode) {
      controller.changeFlashMode(value.flashMode);
      _flashMode = value.flashMode;
    }
  }

  @override
  Widget build(BuildContext context) => VisibilityDetector(
        key: const Key('visible-camera-kit-key-1'),
        onVisibilityChanged: (visibilityInfo) {
          _visible = visibilityInfo.visibleFraction != 0;
          _nativeController?.setCameraVisible(_visible);
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

  void _onPlatformViewCreated(int id) {
    final controller =
        _nativeController = _NativeCameraKitController._(id, context, widget);
    if (_lifeCyclePaused) {
      controller.pauseCamera();
    }
    controller.setCameraVisible(_visible);
    controller.initCamera(_flashMode);
  }
}

WidgetsBinding _binding() => _ambiguate(WidgetsBinding.instance)!;

T? _ambiguate<T>(T? value) => value;

///View State controller. User works with CameraKitController
///and CameraKitController Works with this controller.
class _NativeCameraKitController {
  BuildContext context;
  CameraKitView widget;
  MethodChannel? _channel;

  _NativeCameraKitController._(int id, this.context, this.widget)
      : _channel = MethodChannel('plugins/camera_kit_$id');

  Future<dynamic> nativeMethodCallHandler(MethodCall methodCall) async {
    if (methodCall.method == 'onBarcodesRead') {
      if (widget.onBarcodesRead != null) {
        widget.onBarcodesRead!(List.from(methodCall.arguments));
      }
    }
    return null;
  }

  bool _getScaleTypeMode(ScaleTypeMode scaleType) =>
      scaleType == ScaleTypeMode.fill;

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

  Future<void> initCamera(CameraFlashMode flashMode) async {
    final channel = _channel;
    if (channel == null) {
      return;
    }
    channel.setMethodCallHandler(nativeMethodCallHandler);
    final permissionGranted =
        await channel.invokeMethod<bool>('requestPermission');
    if (permissionGranted != true) {
      widget.onPermissionDenied!();
      return;
    }
    final formats =
        widget.restrictFormat.map((f) => _getBarcodeFormatValue(f)).toList();
    await channel.invokeMethod<void>('initCamera', {
      'hasBarcodeReader': widget.hasBarcodeReader,
      'flashMode': _getCharFlashMode(flashMode),
      'isFillScale': _getScaleTypeMode(widget.scaleType),
      'restrictFormat': formats,
      'cameraSelector': _getCameraSelector(widget.cameraSelector)
    });
  }

  ///Call resume camera in Native API
  Future<void> resumeCamera() async =>
      await _channel?.invokeMethod('resumeCamera');

  ///Call pause camera in Native API
  Future<void> pauseCamera() async =>
      await _channel?.invokeMethod('pauseCamera');

  ///Call change flash mode in Native API
  Future<void> changeFlashMode(CameraFlashMode captureFlashMode) async =>
      await _channel?.invokeMethod('changeFlashMode',
          {'flashMode': _getCharFlashMode(captureFlashMode)});

  ///Call dispose in Native API
  Future<void> dispose() {
    final channel = _channel;
    _channel = null;
    if (channel == null) {
      return Future<void>.value();
    }
    return channel.invokeMethod('dispose');
  }

  ///Call set camera visible in Native API.
  ///This API is used to automatically manage pause and resume camera
  Future<void> setCameraVisible(bool isCameraVisible) async => await _channel
      ?.invokeMethod('setCameraVisible', {'isCameraVisible': isCameraVisible});

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
