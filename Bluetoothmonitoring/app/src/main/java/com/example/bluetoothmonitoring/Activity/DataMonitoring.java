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
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bluetoothmonitoring.R;
import com.example.bluetoothmonitoring.util.DatabaseHelper;
import com.example.bluetoothmonitoring.util.DateAverageData;
import com.example.bluetoothmonitoring.util.TimeGet;
import com.github.lzyzsd.circleprogress.DonutProgress;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.List;

public class DataMonitoring extends AppCompatActivity {
    private static final String TAG = "DataMonitoring";
    private static final String ACTION_BLUETOOTH_DATA = "BluetoothData";

    private TextView currentTimeText, dateDisplay;
    private DonutProgress donutProgress;
    private BarChart barChart;
    private ImageView backButton;

    // Handlers for time updates and graph updates
    private final Handler timeHandler = new Handler(Looper.getMainLooper());
    private final Handler graphHandler = new Handler(Looper.getMainLooper());

    private DatabaseHelper databaseHelper;
    private TimeGet timeGet;
    private String currentDate;  // Track the current date

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
        backButton = findViewById(R.id.back_button);
        dateDisplay = findViewById(R.id.text_date);  // Corrected this line
        currentTimeText = findViewById(R.id.time);
        donutProgress = findViewById(R.id.donut_progress);
        donutProgress.setMax(4095); // Set the maximum value for the DonutProgress
        barChart = findViewById(R.id.bar_chart);



        // Initialize TimeGet and DatabaseHelper
        timeGet = new TimeGet();
        databaseHelper = new DatabaseHelper(this);

        // Get the current date
        currentDate = timeGet.getCurrentDate();

        // Register the receiver for Bluetooth data
        IntentFilter filter = new IntentFilter(ACTION_BLUETOOTH_DATA);
        registerReceiver(bluetoothDataReceiver, filter);

        backButton.setOnClickListener(v -> {
            finish();
        });

        // Start both time and graph updates simultaneously
        startRealTimeUpdates();
        startGraphUpdates();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister the receiver
        unregisterReceiver(bluetoothDataReceiver);
        // Remove any pending posts of the runnables to avoid memory leaks
        timeHandler.removeCallbacksAndMessages(null);
        graphHandler.removeCallbacksAndMessages(null);
    }

    // Method to update the current time
    private void startRealTimeUpdates() {
        timeHandler.post(new Runnable() {
            @Override
            public void run() {
                // Get the current time
                String currentTime = timeGet.getCurrentTime();

                setTextViewTime(currentTime);
                setTexViewDate(currentDate);

                // Update the time every minute (60,000 milliseconds)
                timeHandler.postDelayed(this, 1000);  // Corrected delay to 1 minute
            }
        });
    }

    // Method to update the graph periodically or based on Bluetooth data
    private void startGraphUpdates() {
        graphHandler.post(new Runnable() {
            @Override
            public void run() {
                // Plot the data for the last 7 days
                plotDataForLastNDays(7, false);

                // Replot the data every 5 minutes (300,000 milliseconds)
                graphHandler.postDelayed(this, 300000); // 5 minutes
            }
        });
    }

    // Method to set the text of the TextView for time
    private void setTextViewTime(String time) {
        currentTimeText.setText(time);
    }
    private void setTexViewDate(String dateToday) {
        dateDisplay.setText(dateToday);
    }

    private void updateUI(String data) {
        timeHandler.post(() -> {
            try {
                int progressValue = Integer.parseInt(data);
                Log.d(TAG, "Bluetooth Data Received: " + progressValue);  // Log received Bluetooth data
                donutProgress.setProgress(progressValue);

                // Insert the data into the database with the current date
                insertData(progressValue, currentDate);

                // Replot the chart with the updated data
                plotDataForLastNDays(7, false);  // Replot the last 7 days including today's new data
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error parsing data: " + data, e);
            }
        });
    }

    private void insertData(int data, String date) {
        Log.d(TAG, "Inserting Data: " + data + " on Date: " + date);  // Log data insertion
        databaseHelper.insertData(data, date);
    }

    private void plotDataForLastNDays(int days, boolean showMonth) {
        // Get the average data along with formatted dates (either month or day of week)
        List<DateAverageData> averageDataWithDates = databaseHelper.getAverageDataWithDatesForDays(days, showMonth);

        // Log the data to check if it's being retrieved correctly
        Log.d(TAG, "Data: " + averageDataWithDates.toString());

        // Create a list of BarEntries for the graph and a list of labels for the x-axis
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < averageDataWithDates.size(); i++) {
            DateAverageData data = averageDataWithDates.get(i);
            entries.add(new BarEntry(i, data.getAverage()));  // Each day's average as a bar entry
            labels.add(data.getDate());  // Add the corresponding formatted label (month or day) to the labels list
        }

        // If no data, show a log message
        if (entries.isEmpty()) {
            Log.e(TAG, "No data to plot.");
            return;  // Stop execution if no data
        }

        // Set up the BarDataSet and BarData
        BarDataSet dataSet = new BarDataSet(entries, "Average Data for the Last " + days + " Days");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        BarData barData = new BarData(dataSet);

        // Set the data and refresh the chart
        barChart.setData(barData);

        // Customize the x-axis to show formatted date labels
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));  // Ensure labels are set
        xAxis.setGranularity(1f);  // Ensure each label is displayed
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);  // Place labels at the bottom of the chart

        // Refresh the chart to show changes
        barChart.notifyDataSetChanged();
        barChart.invalidate();
    }
}
