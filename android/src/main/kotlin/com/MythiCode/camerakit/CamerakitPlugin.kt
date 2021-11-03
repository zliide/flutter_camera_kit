package com.MythiCode.camerakit

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.platform.PlatformViewRegistry

/**
 * CamerakitPlugin
 */
class CamerakitPlugin : FlutterPlugin, ActivityAware {
    private var registry: PlatformViewRegistry? = null
    private var messenger: BinaryMessenger? = null
    override fun onAttachedToEngine(flutterPluginBinding: FlutterPluginBinding) {
        registry = flutterPluginBinding.platformViewRegistry
        messenger = flutterPluginBinding.binaryMessenger
    }

    override fun onDetachedFromEngine(binding: FlutterPluginBinding) {}
    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        binding.activity
        registry!!.registerViewFactory("plugins/camera_kit", CameraKitFactory(messenger!!, binding))
    }

    override fun onDetachedFromActivityForConfigChanges() {}
    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {}
    override fun onDetachedFromActivity() {}
}