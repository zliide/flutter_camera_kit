import 'package:camerakit/src/view.dart';

///This controller is used to control CameraKiView.dart
class CameraKitController {
  late CameraKitView cameraKitView;
  bool _initialized = false;

  ///pause camera while stop camera preview.
  ///Plugin manage automatically pause camera based android, iOS lifecycle and widget visibility
  void pauseCamera() {
    cameraKitView.viewState.controller?.setCameraVisible(false);
  }

  ///Closing camera and dispose all resource
  void dispose() {
    cameraKitView.viewState.controller?.dispose();
  }

  ///resume camera while resume camera preview.
  ///Plugin manage automatically resume camera based android, iOS lifecycle and widget visibility
  void resumeCamera() {
    cameraKitView.viewState.controller?.setCameraVisible(true);
  }

  ///Use this method for taking picture in take picture mode
  ///This method return path of image
  Future<String?> takePicture({String path = ''}) {
    return cameraKitView.viewState.controller!.takePicture(path);
  }

  ///Change flash mode between auto, on and off
  void changeFlashMode(CameraFlashMode captureFlashMode) {
    cameraKitView.viewState.controller?.changeFlashMode(captureFlashMode);
  }

  ///Connect view to this controller
  void setView(CameraKitView cameraKitView) {
    if (_initialized) {
      dispose();
    }
    this.cameraKitView = cameraKitView;
    _initialized = true;
  }
}
