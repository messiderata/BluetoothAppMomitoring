package com.example.bluetoothmonitoring;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_FINE_LOCATION = 2;
    private static final String TAG = "BluetoothMonitoring";
    private BluetoothAdapter bluetoothAdapter;
    private ArrayAdapter<String> devicesAdapter;
    private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothSocket bluetoothSocket;
    private BluetoothDevice selectedDevice;
    private OutputStream outputStream;
    private InputStream inputStream;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        Button enableBluetoothButton = findViewById(R.id.button_enable_bluetooth);
        Button discoverDevicesButton = findViewById(R.id.button_discover_devices);
        ListView devicesListView = findViewById(R.id.list_view_devices);
        TextView statusTextView = findViewById(R.id.text_view_status);

        devicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        devicesListView.setAdapter(devicesAdapter);

        enableBluetoothButton.setOnClickListener(v -> {
            if (bluetoothAdapter == null) {
                statusTextView.setText("Bluetooth not supported.");
            } else {
                if (bluetoothAdapter.isEnabled()) {
                    statusTextView.setText("Bluetooth is already enabled.");
                } else {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
            }
        });

        discoverDevicesButton.setOnClickListener(v -> {
            if (bluetoothAdapter != null) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            REQUEST_FINE_LOCATION);
                } else {
                    devicesAdapter.clear();
                    Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                    for (BluetoothDevice device : pairedDevices) {
                        devicesAdapter.add(device.getName() + "\n" + device.getAddress());
                    }
                    bluetoothAdapter.startDiscovery();
                }
            }
        });

        devicesListView.setOnItemClickListener((parent, view, position, id) -> {
            String item = devicesAdapter.getItem(position);
            if (item != null) {
                String address = item.substring(item.indexOf("\n") + 1);
                selectedDevice = bluetoothAdapter.getRemoteDevice(address);
                new Thread(() -> connectToDevice(selectedDevice)).start();
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void connectToDevice(BluetoothDevice device) {
        BluetoothSocket tmp = null;
        try {
            tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            bluetoothAdapter.cancelDiscovery();
            Log.d(TAG, "Connecting to device...");
            tmp.connect();
            bluetoothSocket = tmp;
            outputStream = bluetoothSocket.getOutputStream();
            inputStream = bluetoothSocket.getInputStream();

            handler.post(() -> {
                TextView statusTextView = findViewById(R.id.text_view_status);
                statusTextView.setText("Connected to " + device.getName());
                startListeningForData();
            });

        } catch (IOException e) {
            Log.e(TAG, "Connection failed", e);
            handler.post(() -> {
                TextView statusTextView = findViewById(R.id.text_view_status);
                statusTextView.setText("Connection failed.");
            });
            try {
                if (tmp != null) tmp.close();
            } catch (IOException closeException) {
                Log.e(TAG, "Error closing socket", closeException);
            }
        }
    }

    private void startListeningForData() {
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            int bytes;
            while (true) {
                try {
                    if (inputStream != null) {
                        bytes = inputStream.read(buffer);
                        String message = new String(buffer, 0, bytes);
                        handler.post(() -> {
                            TextView statusTextView = findViewById(R.id.text_view_status);
                            statusTextView.append("\nReceived: " + message);
                        });
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Input stream disconnected", e);
                    break;
                }
            }
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_FINE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can start discovering devices
                Button discoverDevicesButton = findViewById(R.id.button_discover_devices);
                discoverDevicesButton.performClick();
            } else {
                // Permission denied, show a message to the user
                TextView statusTextView = findViewById(R.id.text_view_status);
                statusTextView.setText("Location permission is required to discover Bluetooth devices.");
            }
        }
    }
}
