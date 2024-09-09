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
    private final InputStream inputStream;
    private final Handler handler;
    private final TextView statusTextView;
    private final Context context;
    private boolean listening = true;

    public BluetoothListener(Context context, InputStream inputStream, TextView statusTextView) {
        this.inputStream = inputStream;
        this.handler = new Handler(Looper.getMainLooper());
        this.statusTextView = statusTextView;
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
            byte[] buffer = new byte[1024];  // Buffer for incoming data
            int bytes;

            while (listening) {
                try {
                    // Check if the socket is closed or there is no data to read
                    if (inputStream == null) {
                        Log.e(TAG, "InputStream is null. Socket might be closed.");
                        break;  // Exit the loop if the socket is closed
                    }

                    // Blocking call to read data from the InputStream
                    bytes = inputStream.read(buffer);

                    // If the read returns -1, the socket is closed
                    if (bytes == -1) {
                        Log.e(TAG, "Bluetooth socket closed, stopping listener.");
                        break;  // Exit the loop if the socket is closed
                    }

                    // Convert the byte array to string
                    String data = new String(buffer, 0, bytes);
                    Log.d(TAG, "Data received: " + data);

                    // Broadcast the received data
                    onDataReceived(data);

                    // Update the UI with the received data
                    handler.post(() -> statusTextView.setText("Received: " + data));

                } catch (IOException e) {
                    Log.e(TAG, "Error reading from InputStream", e);
                    break;  // Exit the loop on IO error
                }
            }

            // Clean up and close the input stream
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error closing InputStream", e);
            }

        }).start();
    }

    public void stopListening() {
        listening = false;  // Stop the while loop
    }
}