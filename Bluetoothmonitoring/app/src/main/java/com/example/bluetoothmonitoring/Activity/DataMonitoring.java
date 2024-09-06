package com.example.bluetoothmonitoring.Activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bluetoothmonitoring.R;
import com.example.bluetoothmonitoring.util.DatabaseHelper;
import com.example.bluetoothmonitoring.util.TimeGet;
import com.github.lzyzsd.circleprogress.DonutProgress;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.List;

public class DataMonitoring extends AppCompatActivity {
    private static final String TAG = "DataMonitoring";
    private static final String ACTION_BLUETOOTH_DATA = "BluetoothData";
    private TextView currentTimeText;
    private DonutProgress donutProgress;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private DatabaseHelper databaseHelper;
    private TimeGet timeGet;
    private BarChart barChart;

    private final BroadcastReceiver bluetoothDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_BLUETOOTH_DATA.equals(intent.getAction())) {
                String data = intent.getStringExtra("data");
                if (data != null) {
                    // Update the UI with the received data
                    updateUI(data);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.data_monitoring);

        currentTimeText = findViewById(R.id.time);
        donutProgress = findViewById(R.id.donut_progress);
        donutProgress.setMax(4095); // Set the maximum value for the DonutProgress
        barChart = findViewById(R.id.bar_chart);

        // Initialize TimeGet and DatabaseHelper
        timeGet = new TimeGet();
        databaseHelper = new DatabaseHelper(this);

        // Start real-time time updates
        startRealTimeUpdates();

        // Register the receiver for Bluetooth data
        IntentFilter filter = new IntentFilter(ACTION_BLUETOOTH_DATA);
        registerReceiver(bluetoothDataReceiver, filter);

        // Initialize the chart with default or historical data before Bluetooth sends data
        initializeChart();
    }


    private void startRealTimeUpdates() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                // Update the current time every second
                String currentTime = timeGet.getCurrentTime();
                setTextViewTime(currentTime);
                handler.postDelayed(this, 1000); // Update every second
            }
        });
    }

    // Method to set the text of the TextView
    private void setTextViewTime(String time) {
        currentTimeText.setText(time);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister the receiver
        unregisterReceiver(bluetoothDataReceiver);
        // Remove any pending posts of the runnable to avoid memory leaks
        handler.removeCallbacksAndMessages(null);
    }

    private void updateUI(String data) {
        handler.post(() -> {
            try {
                int progressValue = Integer.parseInt(data);
                donutProgress.setProgress(progressValue);

                // Insert the data into the database with the current date
                String currentDate = timeGet.getCurrentDate();
                insertData(progressValue, currentDate);

                // Update the chart by re-calculating the average of today's data
                plotAverageData(databaseHelper.getAverageDataForToday());  // Recalculate the average
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error parsing data: " + data, e);
            }
        });
    }

    private void initializeChart() {
        // Option 1: Start with default value (e.g., 0)
        float initialAverage = 0;

        // Option 2: Use historical data (e.g., last saved average)
        // Uncomment the line below to use last known average from the database for today's date
         initialAverage = databaseHelper.getAverageDataForToday();

        // Plot the initial value on the chart
        plotAverageData(initialAverage);
    }

    private void insertData(int data, String date) {
        databaseHelper.insertData(data, date);
    }

    // Method to retrieve all data for today and calculate the average
    @SuppressLint("Range")
    private void plotAverageData(float averageValue) {
        // Create a list to hold a single BarEntry (the average)
        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, averageValue));  // Single bar at index 0 with the average value

        // Set up the BarDataSet and BarData
        BarDataSet dataSet = new BarDataSet(entries, "Average of Today's Data");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        BarData barData = new BarData(dataSet);

        // Set the data and refresh the chart
        barChart.setData(barData);
        barChart.invalidate();  // Refresh chart to show changes
    }
}
