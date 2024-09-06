package com.example.bluetoothmonitoring.util;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.bluetoothmonitoring.R;

import java.util.List;

public class BluetoothDeviceAdapter extends ArrayAdapter<BluetoothDevice> {
    private final Context context;
    private final List<BluetoothDevice> devices;

    // Constructor
    public BluetoothDeviceAdapter(Context context, List<BluetoothDevice> devices) {
        super(context, R.layout.device_list_view, devices); // Use your custom layout
        this.context = context;
        this.devices = devices;
    }

    @SuppressLint("MissingPermission")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            // Inflate the custom layout for each item in the list
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.device_list_view, parent, false);
            holder = new ViewHolder();
            holder.deviceName = convertView.findViewById(R.id.device_name); // The TextView in your custom layout
            holder.deviceAddress = convertView.findViewById(R.id.device_address); // The TextView in your custom layout
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // Get the current Bluetooth device
        BluetoothDevice device = devices.get(position);

        // Set the device name (or use "Unknown Device" if the name is null)
        holder.deviceName.setText(device.getName() != null ? device.getName() : "Unknown Device");

        // Set the MAC address
        holder.deviceAddress.setText(device.getAddress());

        return convertView;
    }

    // ViewHolder pattern to optimize performance
    private static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }
}