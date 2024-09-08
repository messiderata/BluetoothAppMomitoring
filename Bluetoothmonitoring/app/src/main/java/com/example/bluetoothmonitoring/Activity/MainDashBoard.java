package com.example.bluetoothmonitoring.Activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.bluetoothmonitoring.R;
import com.example.bluetoothmonitoring.util.BluetoothDeviceAdapter;
import com.example.bluetoothmonitoring.util.BluetoothListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainDashBoard extends AppCompatActivity {

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
    private boolean switchIsOff = false;

    // Cache views
    private TextView statusTextView;
    private TextView textViewStatus;
    private SwitchCompat enableBluetoothSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_dash_board);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // Initialize views and cache them for later use
        enableBluetoothSwitch = findViewById(R.id.button_enable_bluetooth);
        CardView discoverDevicesButton = findViewById(R.id.button_discover_devices);
        ListView devicesListView = findViewById(R.id.list_view_devices);
        statusTextView = findViewById(R.id.bluetooth_status_text);
        textViewStatus = findViewById(R.id.text_status);

        // Create a list to hold Bluetooth devices
        List<BluetoothDevice> bluetoothDevices = new ArrayList<>();

        // Set status based on the switch
        if (!switchIsOff) {
            textViewStatus.setText("ON");
        } else {
            textViewStatus.setText("OFF");
        }

        // Initialize the custom adapter using your custom layout
        BluetoothDeviceAdapter adapter = new BluetoothDeviceAdapter(this, bluetoothDevices);
        devicesListView.setAdapter(adapter);

        // Set initial state of Bluetooth switch
        enableBluetoothSwitch.setChecked(bluetoothAdapter.isEnabled());

        // Toggle Bluetooth on/off using the switch
        enableBluetoothSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (bluetoothAdapter == null) {
                statusTextView.setText("Bluetooth not supported.");
            } else {
                if (isChecked) {
                    // Turn Bluetooth ON
                    if (!bluetoothAdapter.isEnabled()) {
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    } else {
                        statusTextView.setText("Bluetooth is already enabled.");
                    }
                } else {
                    // Turn Bluetooth OFF
                    if (bluetoothAdapter.isEnabled()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                                    != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(this,
                                        new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_ENABLE_BT);
                            } else {
                                bluetoothAdapter.disable();
                                statusTextView.setText("Bluetooth is disabled.");
                            }
                        } else {
                            // For older versions of Android, just disable Bluetooth
                            bluetoothAdapter.disable();
                            statusTextView.setText("Bluetooth is disabled.");
                        }
                    } else {
                        statusTextView.setText("Bluetooth is already disabled.");
                    }
                }
            }
        });

        // Discover Devices
        discoverDevicesButton.setOnClickListener(v -> {
            if (bluetoothAdapter != null) {
                // Request location permission if not granted
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            REQUEST_FINE_LOCATION);
                } else {
                    // Clear the previous list of devices
                    bluetoothDevices.clear();

                    // Add paired devices
                    Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                    if (pairedDevices != null && !pairedDevices.isEmpty()) {
                        bluetoothDevices.addAll(pairedDevices);
                    }

                    // Notify adapter of data change
                    adapter.notifyDataSetChanged();

                    // Start discovery
                    bluetoothAdapter.startDiscovery();
                    statusTextView.setText("Discovering devices...");
                }
            }
        });

        // Set up item click listener for connecting to a device
        devicesListView.setOnItemClickListener((parent, view1, position, id) -> {
            BluetoothDevice selectedDevice = bluetoothDevices.get(position);
            new Thread(() -> connectToDevice(selectedDevice)).start();
        });

        // Register a receiver for Bluetooth state changes
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister Bluetooth state receiver
        unregisterReceiver(bluetoothReceiver);
    }

    @SuppressLint("MissingPermission")
    private void connectToDevice(BluetoothDevice device) {
        BluetoothSocket tmp = null;
        try {
            // Create a socket to connect
            tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            bluetoothAdapter.cancelDiscovery();  // Always cancel discovery because it will slow down a connection
            tmp.connect();  // Attempt to connect to the remote device. This call blocks until it succeeds or throws an exception

            bluetoothSocket = tmp;
            outputStream = bluetoothSocket.getOutputStream();
            inputStream = bluetoothSocket.getInputStream();

            // Check if the socket is connected before starting the listener
            if (bluetoothSocket.isConnected()) {
                handler.post(() -> {
                    statusTextView.setText("Connected to:\n " + device.getName());
                    // Start listening for data using BluetoothDataListener
                    BluetoothListener dataListener = new BluetoothListener(this, inputStream, statusTextView);
                    dataListener.startListening();

                    Intent intent = new Intent(MainDashBoard.this, DataMonitoring.class);
                    startActivity(intent);
                });
            }

        } catch (IOException e) {
            Log.e(TAG, "Connection failed", e);
            handler.post(() -> statusTextView.setText("Connection failed."));
            if (tmp != null) {
                try {
                    tmp.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Error closing socket", closeException);
                }
            }
        }
    }

    // BroadcastReceiver to detect Bluetooth state changes
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        enableBluetoothSwitch.setChecked(false);
                        statusTextView.setText("Bluetooth is disabled.");
                        textViewStatus.setTextColor(Color.parseColor("#FF0000"));
                        switchIsOff = true;
                        textViewStatus.setText("OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        enableBluetoothSwitch.setChecked(true);
                        statusTextView.setText("Bluetooth is enabled.");
                        textViewStatus.setTextColor(Color.parseColor("#00FF00"));
                        switchIsOff = false;
                        textViewStatus.setText("ON");
                        break;
                }
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_ENABLE_BT || requestCode == REQUEST_FINE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Handle the granted permission here, if needed
            } else {
                // Permission was denied. Show a message or handle accordingly
                statusTextView.setText("Permission denied.");
            }
        }
    }
}