package com.MythiCode.camerakit

import android.content.Context
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory

class CameraKitFactory(
    private val messenger: BinaryMessenger,
    private val pluginBinding: ActivityPluginBinding
) : PlatformViewFactory(StandardMessageCodec.INSTANCE) {
    override fun create(context: Context?, viewId: Int, args: Any?): PlatformView {
        return CameraKitFlutterView(pluginBinding, messenger, viewId)
    }
}