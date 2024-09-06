package com.example.bluetoothmonitoring.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import java.io.IOException;
import java.io.InputStream;

public class BluetoothListener {

    private static final String TAG = "BluetoothMonitoring";
    private final InputStream inputStream;
    private final Handler handler;
    private final TextView statusTextView;
    private final DatabaseHelper databaseHelper; // Correct class name should be used here

    public BluetoothListener(Context context, InputStream inputStream, TextView statusTextView) {
        this.inputStream = inputStream;
        this.handler = new Handler(Looper.getMainLooper());
        this.statusTextView = statusTextView;
        this.databaseHelper = new DatabaseHelper(context); // Ensure correct class name
    }

    public void startListening() {
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            int bytes;
            while (true) {
                try {
                    if (inputStream != null) {
                        bytes = inputStream.read(buffer);
                        String message = new String(buffer, 0, bytes);
                        handler.post(() -> statusTextView.append("\nReceived: " + message));

                        // Save the message to the SQLite database
                        databaseHelper.insertData(message);

                    } else {
                        Log.e(TAG, "InputStream is null, stopping listener.");
                        break;
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error reading from InputStream", e);
                    break;
                }
            }
        }).start();
    }
}
