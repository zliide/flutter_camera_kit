import 'package:camerakit/src/view.dart';
import 'package:flutter/foundation.dart';

class _CameraState {
  final bool active;
  final CameraFlashMode flashMode;

  _CameraState({required this.active, required this.flashMode});

  _CameraState copyWith({
    bool? paused,
    CameraFlashMode? flashMode,
  }) =>
      _CameraState(
        active: paused ?? this.active,
        flashMode: flashMode ?? this.flashMode,
      );
}

///This controller is used to control CameraKiView.dart
class CameraKitController extends ValueNotifier<_CameraState> {
  CameraKitController(
      {bool active = true, CameraFlashMode flashMode = CameraFlashMode.auto})
      : super(_CameraState(active: active, flashMode: flashMode));

  ///pause camera while stop camera preview.
  ///Plugin manage automatically pause camera based android, iOS lifecycle and widget visibility
  void pauseCamera() {
    value = value.copyWith(paused: true);
  }

  ///resume camera while resume camera preview.
  ///Plugin manage automatically resume camera based android, iOS lifecycle and widget visibility
  void resumeCamera() {
    value = value.copyWith(paused: false);
  }

  ///Change flash mode between auto, on and off
  void changeFlashMode(CameraFlashMode captureFlashMode) {
    value = value.copyWith(flashMode: captureFlashMode);
  }
}
