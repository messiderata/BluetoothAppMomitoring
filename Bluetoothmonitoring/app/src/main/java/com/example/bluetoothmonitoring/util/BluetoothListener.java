package com.example.bluetoothmonitoring.util;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;

public class BluetoothListener {
    private static final String ACTION_BLUETOOTH_DATA = "BluetoothData";
    private static final String TAG = "BluetoothMonitoring";
    private final InputStream inputStream;
    private final Handler handler;
    private final TextView statusTextView;
    private final DatabaseHelper databaseHelper; // Correct class name should be used here
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
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            int bytes;
            while (true) {
                try {
                    if (inputStream != null) {
                        bytes = inputStream.read(buffer);
                        if (bytes > 0) {
                            String message = new String(buffer, 0, bytes).trim();
                            // Log the received message for debugging
                            Log.d(TAG, "Received: " + message);
                            // Send the data to another activity

                            Intent intent = new Intent("BluetoothData");
                            intent.putExtra("data", message);
                            context.sendBroadcast(intent);
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error reading from InputStream", e);
                    break;
                }
            }
        }).start();
    }
}
