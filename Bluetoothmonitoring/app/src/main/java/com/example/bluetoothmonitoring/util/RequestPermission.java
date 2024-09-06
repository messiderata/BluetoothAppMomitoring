package com.example.bluetoothmonitoring.util;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class RequestPermission {

    public static final int REQUEST_FINE_LOCATION = 2;
    private final Activity activity;

    // Constructor to pass the Activity context
    public RequestPermission(Activity activity) {
        this.activity = activity;
    }



    // **Remove static** from here:
    public void handleRequestPermissionsResult(int requestCode, int[] grantResults, CardView discoverDevicesButton, TextView statusTextView) {
        if (requestCode == REQUEST_FINE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, discover devices
                discoverDevicesButton.performClick();
            } else {
                // Permission denied
                statusTextView.setText("Location permission is required to discover Bluetooth devices.");
            }
        }
    }
}