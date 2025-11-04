package com.j.background_sms;

import android.app.Activity;
import android.content.Context;
import android.telephony.SmsManager;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/**
 * BackgroundSmsPlugin
 *
 * Migrated to V2 Android embedding (implements FlutterPlugin and ActivityAware)
 */
public class BackgroundSmsPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {
    private MethodChannel channel;
    private Context context;
    private Activity activity;

    // Called when plugin is attached to the Flutter engine.
    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        this.context = binding.getApplicationContext();
        channel = new MethodChannel(binding.getBinaryMessenger(), "background_sms");
        channel.setMethodCallHandler(this);
    }

    // Called when plugin is detached from the Flutter engine.
    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        if (channel != null) {
            channel.setMethodCallHandler(null);
            channel = null;
        }
        this.context = null;
    }

    // ActivityAware callbacks (optional — only used if plugin needs Activity)
    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        this.activity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        this.activity = null;
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        this.activity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivity() {
        this.activity = null;
    }

    // Handle method calls from Dart
    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        if ("send".equals(call.method) || "sendSMS".equals(call.method)) {
            // Accept either "send" or "sendSMS" method name to be forgiving.
            Object args = call.arguments;
            String phone = null;
            String message = null;

            if (args instanceof Map) {
                Map<?,?> map = (Map<?,?>) args;
                Object p = map.get("phone");
                Object m = map.get("message");
                if (p != null) phone = p.toString();
                if (m != null) message = m.toString();
            } else if (call.argument("phone") != null || call.argument("message") != null) {
                phone = call.argument("phone");
                message = call.argument("message");
            }

            if (phone == null || message == null) {
                result.error("INVALID_ARGUMENTS", "Expected arguments: {phone: String, message: String}", null);
                return;
            }

            sendSms(phone, message, result);
        } else {
            result.notImplemented();
        }
    }

    // Perform SMS send using SmsManager (requires SEND_SMS permission)
    private void sendSms(String phone, String message, Result result) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<String> parts = smsManager.divideMessage(message);
            if (parts.size() > 1) {
                smsManager.sendMultipartTextMessage(phone, null, parts, null, null);
            } else {
                smsManager.sendTextMessage(phone, null, message, null, null);
            }
            result.success(true);
        } catch (SecurityException se) {
            // Likely missing runtime SEND_SMS permission
            result.error("PERMISSION_ERROR", "Missing SEND_SMS permission or denied by user: " + se.getMessage(), null);
        } catch (Exception e) {
            result.error("SMS_ERROR", e.getMessage(), null);
        }
    }
}