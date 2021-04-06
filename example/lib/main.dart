import 'package:camerakit/camerakit.dart';
import 'package:flutter/material.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  CameraKitView cameraKitView;
  CameraKitController cameraKitController;

  @override
  void initState() {
    super.initState();
    cameraKitController = CameraKitController();
    print("cameraKitController" + cameraKitController.toString());
    cameraKitView = CameraKitView(
      hasBarcodeReader: true,
      onBarcodeRead: (barcode) {
        print("Flutter read barcode: " + barcode);
      },
      previewFlashMode: CameraFlashMode.auto,
      cameraKitController: cameraKitController,
    );
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        body: Container(
          height: 2000,
          child: Column(
            children: <Widget>[
              Expanded(
                  child: CameraKitView(
                hasBarcodeReader: false,
                barcodeFormat: BarcodeFormats.FORMAT_ALL_FORMATS,
                scaleType: ScaleTypeMode.fill,
                onBarcodeRead: (barcode) {
                  print("Flutter read barcode: " + barcode);
                },
                previewFlashMode: CameraFlashMode.auto,
                cameraKitController: cameraKitController,
                androidCameraMode: AndroidCameraMode.API_X,
                cameraSelector: CameraSelector.back,
              )),
              Row(
                children: <Widget>[
                  ElevatedButton(
                    child: Text("Flash OFF"),
                    onPressed: () {
                      setState(() {
                        cameraKitController
                            .changeFlashMode(CameraFlashMode.off);
                      });
                    },
                  ),
                  ElevatedButton(
                    child: Text("Capture"),
                    onPressed: () {
                      cameraKitController.takePicture().then((value) =>
                          print("flutter take picture result: " + value));
                    },
                  ),
                  ElevatedButton(
                    child: Text("Flash On"),
                    onPressed: () {
                      setState(() {
                        cameraKitController.changeFlashMode(CameraFlashMode.on);
                      });
                    },
                  ),
                ],
              ),
              Builder(
                builder: (context) => ElevatedButton(
                  child: Text("GO"),
                  onPressed: () {
                    Navigator.push(
                        context,
                        MaterialPageRoute(
                            builder: (context) => Scaffold(
                                  body: Text("Go is Here"),
                                )));
                  },
                ),
              )
            ],
          ),
        ),
      ),
    );
  }
}
