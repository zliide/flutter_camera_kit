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
  void dispose() {
    cameraKitController.dispose();
    super.dispose();
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
                  child:
                      CameraWidget(cameraKitController: cameraKitController)),
              Row(
                children: <Widget>[
                  ElevatedButton(
                    child: const Text('Flash OFF'),
                    onPressed: () {
                      setState(() {
                        cameraKitController
                            .changeFlashMode(CameraFlashMode.off);
                      });
                    },
                  ),
                  ElevatedButton(
                    child: const Text('Flash On'),
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
                  child: const Text('GO'),
                  onPressed: () {
                    Navigator.push<void>(
                      context,
                      MaterialPageRoute(
                        builder: (context) => const Scaffold(
                          body: Text('Go is Here'),
                        ),
                      ),
                    );
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

class CameraWidget extends StatefulWidget {
  const CameraWidget({
    Key key,
    @required this.cameraKitController,
  }) : super(key: key);

  final CameraKitController cameraKitController;

  @override
  State<CameraWidget> createState() => _CameraWidgetState();
}

class _CameraWidgetState extends State<CameraWidget> {
  bool _visible = false;

  @override
  Widget build(BuildContext context) => Column(
        children: [
          Expanded(
            child: _visible
                ? CameraKitView(
                    hasBarcodeReader: true,
                    scaleType: ScaleTypeMode.fit,
                    onBarcodesRead: (barcodes) {
                      print('Flutter read barcode: $barcodes');
                    },
                    controller: widget.cameraKitController,
                    cameraSelector: CameraSelector.back,
                  )
                : const SizedBox(),
          ),
          ElevatedButton(
            onPressed: () {
              setState(() {
                _visible = !_visible;
              });
            },
            child: Text('Toggle visible'),
          ),
        ],
      );
}
