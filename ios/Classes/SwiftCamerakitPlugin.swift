import Flutter
import UIKit

public class SwiftCamerakitPlugin: NSObject, FlutterPlugin {
  public static func register(with registrar: FlutterPluginRegistrar) {
    let factory = CameraKitFactory(registrar: registrar)
    registrar.register(factory, withId: "plugins/camera_kit")
  }
}
