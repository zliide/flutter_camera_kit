package com.MythiCode.camerakit;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.StandardMessageCodec;
import io.flutter.plugin.platform.PlatformView;
import io.flutter.plugin.platform.PlatformViewFactory;

public class CameraKitFactory extends PlatformViewFactory {

    @NonNull
    private final BinaryMessenger messenger;
    @NonNull
    private final ActivityPluginBinding pluginBinding;

    public CameraKitFactory(@NonNull BinaryMessenger messenger, @NonNull ActivityPluginBinding pluginBinding) {
        super(StandardMessageCodec.INSTANCE);
        this.messenger = messenger;
        this.pluginBinding = pluginBinding;
    }

    @NonNull
    @Override
    public PlatformView create(@NonNull Context context, int id, @Nullable Object args) {
        return new CameraKitFlutterView(pluginBinding, messenger, id);
    }
}
