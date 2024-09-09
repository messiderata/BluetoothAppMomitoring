package com.example.bluetoothmonitoring.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;

public class BluetoothListener {
    private static final String ACTION_BLUETOOTH_DATA = "BluetoothData";
    private static final String TAG = "BluetoothMonitoring";
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 101;
    private final InputStream inputStream;
    private final Handler handler;
    private final TextView statusTextView;
    private final DatabaseHelper databaseHelper; // Ensure correct class name is used
    private final Context context;

    public BluetoothListener(Context context, InputStream inputStream, TextView statusTextView) {
        this.inputStream = inputStream;
        this.handler = new Handler(Looper.getMainLooper());
        this.statusTextView = statusTextView;
        this.databaseHelper = new DatabaseHelper(context); // Ensure correct class name
        this.context = context;
    }

    // Call this method when you have new data to broadcast
    public void onDataReceived(String data) {
        Intent intent = new Intent(ACTION_BLUETOOTH_DATA);
        intent.putExtra("data", data);
        context.sendBroadcast(intent);
    }

    public void startListening() {
        // Check Bluetooth permissions for Android 12 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                if (context instanceof Activity) {
                    ActivityCompat.requestPermissions((Activity) context,
                            new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN},
                            REQUEST_BLUETOOTH_PERMISSIONS);
                }
                return; // Exit if permissions are not granted
            }
        }

        new Thread(() -> {
            byte[] buffer = new byte[1024];
            int bytes;
            while (true) {
                try {
                    if (inputStream != null) {
                        bytes = inputStream.read(buffer);
                        if (bytes > 0) {
                            String message = new String(buffer, 0, bytes).trim();
                            Log.d(TAG, "Received: " + message);
                            // Only broadcast if the message is valid
                            if (!message.isEmpty()) {
                                onDataReceived(message);
                            }
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error reading from InputStream", e);
                    break; // Stop listening when there's an error
                }
            }
        }).start();
    }
}
