package com.MythiCode.camerakit;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.platform.PlatformViewRegistry;

/**
 * CamerakitPlugin
 */
public class CamerakitPlugin implements FlutterPlugin, ActivityAware {
    private PlatformViewRegistry registry;
    private BinaryMessenger messenger;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        registry = flutterPluginBinding.getPlatformViewRegistry();
        messenger = flutterPluginBinding.getBinaryMessenger();
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {

    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        if (binding.getActivity() != null) {
            registry.registerViewFactory("plugins/camera_kit", new CameraKitFactory(messenger, binding));
        }
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {

    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {

    }

    @Override
    public void onDetachedFromActivity() {

    }
}
