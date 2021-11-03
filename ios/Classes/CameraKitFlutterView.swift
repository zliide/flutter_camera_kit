import Flutter
import AVFoundation
import MLKitBarcodeScanning
import MLKitCommon
import MLKitVision


class CameraKitFlutterView: NSObject, FlutterPlatformView, AVCaptureVideoDataOutputSampleBufferDelegate {
  let channel: FlutterMethodChannel
  let frame: CGRect

  var hasBarcodeReader: Bool!
  var imageSavePath: String!
  var isCameraVisible: Bool! = true
  var initCameraFinished: Bool! = false
  var isFillScale: Bool!
  var flashMode: AVCaptureDevice.FlashMode!
  var cameraPosition: AVCaptureDevice.Position!

  var previewView: UIView!
  var videoDataOutput: AVCaptureVideoDataOutput!
  var videoDataOutputQueue: DispatchQueue!

  var photoOutput: AVCapturePhotoOutput?
  var previewLayer: AVCaptureVideoPreviewLayer!
  var captureDevice: AVCaptureDevice!
  let session = AVCaptureSession()
  var discoverySession: AVCaptureDevice.DiscoverySession!
  var barcodeScanner: BarcodeScanner!

  init(registrar: FlutterPluginRegistrar, viewId: Int64, frame: CGRect) {
    channel = FlutterMethodChannel(name: "plugins/camera_kit_" + String(viewId), binaryMessenger: registrar.messenger())
    self.frame = frame
  }

  func requestPermission(flutterResult: @escaping FlutterResult) {
    if AVCaptureDevice.authorizationStatus(for: .video) == .authorized {
      //already authorized
      flutterResult(true)
    } else {
      DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
        AVCaptureDevice.requestAccess(for: .video, completionHandler: { (granted: Bool) in
          if granted {
            //access allowed
            flutterResult(true)
          } else {
            //access denied
            flutterResult(false)
          }
        })
      }
    }
  }


  public func setMethodHandler() {
    channel.setMethodCallHandler({ (FlutterMethodCall, FlutterResult) in
      let args = FlutterMethodCall.arguments
      let myArgs = args as? [String: Any]
      if FlutterMethodCall.method == "requestPermission" {
        self.requestPermission(flutterResult: FlutterResult)
      } else if FlutterMethodCall.method == "initCamera" {
        self.initCameraFinished = false
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
          self.initCamera(hasBarcodeReader: (myArgs?["hasBarcodeReader"] as! Bool),
              flashMode: (myArgs?["flashMode"]) as! String, isFillScale:
          (myArgs?["isFillScale"]) as! Bool
              , restrictFormat: (myArgs?["restrictFormat"]) as! [Int]
              , cameraSelector: (myArgs?["cameraSelector"]) as! Int
          )
          self.startOrientationListener()
        }
      } else if FlutterMethodCall.method == "resumeCamera" {
        if self.initCameraFinished == true {
          self.session.startRunning()
          self.isCameraVisible = true
        }
      } else if FlutterMethodCall.method == "pauseCamera" {
        if self.initCameraFinished == true {
          self.stopCamera()
          self.isCameraVisible = false
        }
      } else if FlutterMethodCall.method == "changeFlashMode" {
        self.setFlashMode(flashMode: (myArgs?["flashMode"]) as! String)
        self.changeFlashMode()
      } else if FlutterMethodCall.method == "setCameraVisible" {
        let cameraVisibility = (myArgs?["isCameraVisible"] as! Bool)
        if cameraVisibility == true {
          if self.isCameraVisible == false {
            self.session.startRunning()
            self.isCameraVisible = true
          }
        } else {
          if self.isCameraVisible == true {
            self.stopCamera()
            self.isCameraVisible = false
          }
        }

      } else if FlutterMethodCall.method == "dispose" {
        if self.isCameraVisible == true {
          self.stopCamera()
          self.isCameraVisible = false
        }
      }
    })
  }

  func startOrientationListener() {
    UIDevice.current.beginGeneratingDeviceOrientationNotifications()
    NotificationCenter.default.addObserver(self, selector: #selector(orientationChanged), name: UIDevice.orientationDidChangeNotification, object: nil)
  }

  @objc func orientationChanged() {
    let videoOrientation = { () -> AVCaptureVideoOrientation in
      switch UIApplication.shared.statusBarOrientation {
      case .landscapeLeft:
        return .landscapeLeft
      case .landscapeRight:
        return .landscapeRight
      case .portraitUpsideDown:
        return .portraitUpsideDown
      default:
        return .portrait
      }
    }
    DispatchQueue.main.async {
      self.previewLayer.connection?.videoOrientation = videoOrientation()
    }
  }

  func changeFlashMode() {

    if (hasBarcodeReader) {
      do {
        if (captureDevice.hasFlash) {
          try captureDevice.lockForConfiguration()
          captureDevice.torchMode = (flashMode == .auto) ? (.auto) : (self.flashMode == .on ? (.on) : (.off))
          captureDevice.unlockForConfiguration()
        }
      } catch {
        //DISABEL FLASH BUTTON HERE IF ERROR
        print("Device tourch Flash Error ");
      }

    }
  }

  func setFlashMode(flashMode: String) {
    if flashMode == "A" {
      self.flashMode = .auto
    } else if flashMode == "O" {
      self.flashMode = .on
    } else if flashMode == "F" {
      self.flashMode = .off
    }
  }

  func view() -> UIView {
    if previewView == nil {
      previewView = UIView(frame: frame)
    }
    return previewView
  }

  func initCamera(hasBarcodeReader: Bool, flashMode: String, isFillScale: Bool, restrictFormat: [Int], cameraSelector: Int) {
    self.hasBarcodeReader = hasBarcodeReader
    self.isFillScale = isFillScale
    cameraPosition = cameraSelector == 0 ? .back : .front
    setFlashMode(flashMode: flashMode)
    if hasBarcodeReader {
      var format: BarcodeFormat
      if restrictFormat.isEmpty || restrictFormat.contains(0) {
        format = .all
      } else {
        format = BarcodeFormat()
        restrictFormat.forEach {
          format.insert(BarcodeFormat.init(rawValue: $0))
        }
      }
      let barcodeOptions = BarcodeScannerOptions(formats: format)
      barcodeScanner = BarcodeScanner.barcodeScanner(options: barcodeOptions)
    }
    setupAVCapture()
  }

  func setupAVCapture() {
    session.sessionPreset = AVCaptureSession.Preset.hd1920x1080
    if #available(iOS 11.1, *) {
      discoverySession = AVCaptureDevice.DiscoverySession(deviceTypes:
                                                            [.builtInTrueDepthCamera, .builtInDualCamera, .builtInWideAngleCamera],
                                                          mediaType: .video, position: .unspecified)
    } else {
      discoverySession = AVCaptureDevice.DiscoverySession(deviceTypes:
                                                            [.builtInDualCamera, .builtInWideAngleCamera],
                                                          mediaType: .video, position: .unspecified)
    }
    guard let device = bestDevice(in: cameraPosition) else {
      return
    }
    captureDevice = device


    beginSession()
    changeFlashMode()
  }
  
  func bestDevice(in position: AVCaptureDevice.Position) -> AVCaptureDevice? {
      let devices = self.discoverySession.devices
      guard !devices.isEmpty else { return nil }

      return devices.first(where: { device in device.position == position })!
  }


  func beginSession(isFirst: Bool = true) {
    var deviceInput: AVCaptureDeviceInput!


    do {
      deviceInput = try AVCaptureDeviceInput(device: captureDevice)
      guard deviceInput != nil else {
        print("error: cant get deviceInput")
        return
      }

      if session.canAddInput(deviceInput) {
        session.addInput(deviceInput)
      }

      if (hasBarcodeReader) {
        videoDataOutput = AVCaptureVideoDataOutput()
        videoDataOutput.alwaysDiscardsLateVideoFrames = true

        videoDataOutputQueue = DispatchQueue(label: "VideoDataOutputQueue")
        videoDataOutput.setSampleBufferDelegate(self, queue: videoDataOutputQueue)
        if session.canAddOutput(videoDataOutput!) {
          session.addOutput(videoDataOutput!)
        }
        videoDataOutput.connection(with: .video)?.isEnabled = true

      } else {
        photoOutput = AVCapturePhotoOutput()
        photoOutput?.setPreparedPhotoSettingsArray([AVCapturePhotoSettings(format: [AVVideoCodecKey: AVVideoCodecType.jpeg])], completionHandler: nil)
        if session.canAddOutput(photoOutput!) {
          session.addOutput(photoOutput!)
        }
      }


      previewLayer = AVCaptureVideoPreviewLayer(session: session)
      if isFillScale {
        previewLayer.videoGravity = AVLayerVideoGravity.resizeAspectFill
      } else {
        previewLayer.videoGravity = AVLayerVideoGravity.resizeAspect
      }
      
      orientationChanged()

      startSession(isFirst: isFirst)


    } catch let error as NSError {
      deviceInput = nil
      print("error: \(error.localizedDescription)")
    }
  }

  func startSession(isFirst: Bool) {
    DispatchQueue.main.async {
      let rootLayer: CALayer = self.previewView.layer
      rootLayer.masksToBounds = true
      if (rootLayer.bounds.size.width != 0 && rootLayer.bounds.size.width != 0) {
        self.previewLayer.frame = rootLayer.bounds
        rootLayer.addSublayer(self.previewLayer)
        self.session.startRunning()
        if isFirst == true {
          DispatchQueue.global().asyncAfter(deadline: .now() + 0.2) {
            self.initCameraFinished = true
          }
        }
      } else {
        DispatchQueue.global().asyncAfter(deadline: .now() + 1.0) {
          self.startSession(isFirst: isFirst)
        }
      }
    }
  }

  func stopCamera() {
    if session.isRunning {
      session.stopRunning()
    }
  }


  private func currentUIOrientation() -> UIDeviceOrientation {
    let deviceOrientation = { () -> UIDeviceOrientation in
      switch UIApplication.shared.statusBarOrientation {
      case .landscapeLeft:
        return .landscapeRight
      case .landscapeRight:
        return .landscapeLeft
      case .portraitUpsideDown:
        return .portraitUpsideDown
      case .portrait, .unknown:
        return .portrait
      @unknown default:
        fatalError()
      }
    }
    guard Thread.isMainThread else {
      var currentOrientation: UIDeviceOrientation = .portrait
      DispatchQueue.main.sync {
        currentOrientation = deviceOrientation()
      }
      return currentOrientation
    }
    return deviceOrientation()
  }


  public func imageOrientation(
      fromDevicePosition devicePosition: AVCaptureDevice.Position = .back
  ) -> UIImage.Orientation {
    var deviceOrientation = UIDevice.current.orientation
    if deviceOrientation == .faceDown || deviceOrientation == .faceUp
           || deviceOrientation
           == .unknown {
      deviceOrientation = currentUIOrientation()
    }
    switch deviceOrientation {
    case .portrait:
      return devicePosition == .front ? .leftMirrored : .right
    case .landscapeLeft:
      return devicePosition == .front ? .downMirrored : .up
    case .portraitUpsideDown:
      return devicePosition == .front ? .rightMirrored : .left
    case .landscapeRight:
      return devicePosition == .front ? .upMirrored : .down
    case .faceDown, .faceUp, .unknown:
      return .up
    @unknown default:
      fatalError()
    }
  }


  func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
    guard barcodeScanner != nil else {
      return
    }

    let visionImage = VisionImage(buffer: sampleBuffer)
    let orientation = imageOrientation(
        fromDevicePosition: cameraPosition
    )
    visionImage.orientation = orientation
    var barcodes: [Barcode]
    do {
      barcodes = try barcodeScanner.results(in: visionImage)
    } catch let error {
      print("Failed to scan barcodes with error: \(error.localizedDescription).")
      return
    }

    guard !barcodes.isEmpty else {
      return
    }

    barcodesRead(barcodes: barcodes.map {
      $0.rawValue!
    })
  }

  func barcodesRead(barcodes: [String]) {
    channel.invokeMethod("onBarcodesRead", arguments: barcodes)
  }

}
