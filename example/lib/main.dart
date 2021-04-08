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
                hasBarcodeReader: true,
                scaleType: ScaleTypeMode.fill,
                onBarcodesRead: (barcodes) {
                  print("Flutter read barcode: $barcodes");
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
