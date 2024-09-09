package com.example.bluetoothmonitoring.Activity;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

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
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DataMonitoring extends AppCompatActivity {
    private static final String TAG = "DataMonitoring";
    private static final String ACTION_BLUETOOTH_DATA = "BluetoothData";

    private TextView currentTimeText, dateDisplay;
    private DonutProgress donutProgress;
    private BarChart dailyBarChart, hourlyBarChart;
    private long lastDialogTime = 0;

    private final Handler timeHandler = new Handler(Looper.getMainLooper());
    private final Handler graphHandler = new Handler(Looper.getMainLooper());

    private List<Float> currentHourData = new ArrayList<>();

    private DatabaseHelper databaseHelper;
    private TimeGet timeGet;
    private String currentDate;

    private final BroadcastReceiver bluetoothDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_BLUETOOTH_DATA.equals(intent.getAction())) {
                String data = intent.getStringExtra("data");
                if (data != null) {
                    updateUI(data);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.data_monitoring);

        initializeUI();
        timeGet = new TimeGet();
        databaseHelper = new DatabaseHelper(this);

        currentDate = timeGet.getCurrentDate();

        // Register the receiver with the appropriate flag
        IntentFilter filter = new IntentFilter(ACTION_BLUETOOTH_DATA);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 12 and above, use the flag to declare that the receiver is not exported
            registerReceiver(bluetoothDataReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            // For older versions, register without the flag
            registerReceiver(bluetoothDataReceiver, filter);
        }

        startRealTimeUpdates();
        startGraphUpdates();
        startHourlyCheck();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(bluetoothDataReceiver);  // Unregister the receiver
        timeHandler.removeCallbacksAndMessages(null);
        graphHandler.removeCallbacksAndMessages(null);
    }


    private void initializeUI() {
        ImageView backButton = findViewById(R.id.back_button);
        dateDisplay = findViewById(R.id.text_date);
        currentTimeText = findViewById(R.id.time);
        donutProgress = findViewById(R.id.donut_progress);
        donutProgress.setMax(4095);
        dailyBarChart = findViewById(R.id.bar_chart);
        hourlyBarChart = findViewById(R.id.bar_chart_hourly);

        backButton.setOnClickListener(v -> finish());
    }

    // Update UI with new data
    private void updateUI(String data) {
        try {
            int progressValue = Integer.parseInt(data);
            donutProgress.setProgress(progressValue);
            insertData(progressValue, currentDate);

            // Add the current data to the list for this hour
            currentHourData.add((float) progressValue);

            // Update graphs
            plotDataForLastNDays(7, false);
            plotHourlyAverages(currentDate);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing data: " + data, e);
        }
    }

    // Insert new data into the database
    private void insertData(int data, String date) {
        String currentTime = timeGet.getCurrentTime();
        databaseHelper.insertData(data, date, currentTime);
    }

    // Plot data for the last N days
    private void plotDataForLastNDays(int days, boolean showMonth) {
        List<DateAverageData> averageDataWithDates = databaseHelper.getAverageDataWithDatesForDays(days, showMonth);
        if (averageDataWithDates.isEmpty()) return;

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < averageDataWithDates.size(); i++) {
            entries.add(new BarEntry(i, averageDataWithDates.get(i).getAverage()));
            labels.add(averageDataWithDates.get(i).getDate());
        }

        BarDataSet dataSet = new BarDataSet(entries, "Average Data for the Last " + days + " Days");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);

        BarData barData = new BarData(dataSet);
        dailyBarChart.setData(barData);

        // Set up x-axis to show proper labels
        setupXAxis(dailyBarChart.getXAxis(), labels);

        // Notify chart of data change
        dailyBarChart.notifyDataSetChanged();

        // Invalidate to force a redraw
        dailyBarChart.invalidate();
    }


    // Plot hourly averages for the current date
    private void plotHourlyAverages(String date) {
        List<DateAverageData> averageDataWithHours = databaseHelper.getAverageDataFor24Hours(date);
        if (averageDataWithHours.isEmpty()) return;

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < averageDataWithHours.size(); i++) {
            DateAverageData data = averageDataWithHours.get(i);
            entries.add(new BarEntry(i, data.getAverage()));

            // Use 24-hour format (00 to 23) for the labels
            labels.add(String.format(Locale.getDefault(), "%02d", i));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Hourly Data for " + date);
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        hourlyBarChart.setData(new BarData(dataSet));

        // Set up x-axis to show 24-hour format
        setupXAxis(hourlyBarChart.getXAxis(), labels);
    }

    private void setupXAxis(XAxis xAxis, List<String> labels) {
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        hourlyBarChart.notifyDataSetChanged();
        hourlyBarChart.invalidate();
    }


    // Show CO2 level dialog if necessary
    private void showCO2DialogIfNecessary(float averageForHour) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastDialogTime >= 3600000) {  // Show dialog only once per hour
            if (averageForHour <= 400) {
                showCO2LevelDialog("Good", "CO2 Levels are Good", R.color.green);
            } else if (averageForHour > 400 && averageForHour <= 1200) {
                showCO2LevelDialog("Fair", "CO2 Levels are Fair", R.color.yellow);
            } else if (averageForHour > 1200) {
                showCO2LevelDialog("Bad", "CO2 Levels are Bad", R.color.red);
            }
            lastDialogTime = currentTime;
        }
    }


    // Display CO2 level dialog
    private void showCO2LevelDialog(String co2Level, String message, int colorResId) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.good_notification);
        TextView titleText = dialog.findViewById(R.id.textView3);
        TextView messageText = dialog.findViewById(R.id.textView);
        CardView cardView = dialog.findViewById(R.id.notif_bg);
        Button okayButton = dialog.findViewById(R.id.okay_button);

        int color = getResources().getColor(colorResId);
        titleText.setText(co2Level);
        titleText.setTextColor(color);
        messageText.setText(message);
        cardView.setCardBackgroundColor(color);

        okayButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }



    // Hourly check for displaying dialog
    private void startHourlyCheck() {
        timeHandler.post(new Runnable() {
            @Override
            public void run() {
                checkFor59Minutes();
                timeHandler.postDelayed(this, 1000);  // Run every minute
            }
        });
    }

    private void checkFor59Minutes() {
        Calendar calendar = Calendar.getInstance();
        if (calendar.get(Calendar.MINUTE) == 59 && calendar.get(Calendar.SECOND)== 59) {
            // Calculate the average for the data collected this hour
            calculateAndShowDialogForHour(currentHourData);

            // Clear the data for the next hour's collection
            currentHourData.clear();
        }
    }

    private void calculateAndShowDialogForHour(List<Float> hourData) {
        if (hourData.isEmpty()) return;

        // Calculate the average for the current hour's data
        float sum = 0;
        for (float value : hourData) {
            sum += value;
        }

        float averageForHour = sum / hourData.size();
        showCO2DialogIfNecessary(averageForHour);
    }


    // Real-time updates to show the current time
    private void startRealTimeUpdates() {
        timeHandler.post(new Runnable() {
            @Override
            public void run() {
                String currentTime = timeGet.getCurrentTimeFormatted();
                String currentDate = timeGet.getCurrentDate();
                setTextViewTime(currentTime);
                setTextViewDate(currentDate);
                timeHandler.postDelayed(this, 1000);  // Update every second
            }
        });
    }



    // Graph updates to refresh the chart data periodically
    private void startGraphUpdates() {
        graphHandler.post(new Runnable() {
            @Override
            public void run() {
                plotDataForLastNDays(7, false);  // Update the 7-day chart
                plotHourlyAverages(currentDate);  // Update the hourly chart
                graphHandler.postDelayed(this, 1000);  // Refresh every 5 minutes
            }
        });
    }

    // Helper method to set the current time text view
    private void setTextViewTime(String time) {
        currentTimeText.setText(time);
    }

    // Helper method to set the current time text view
    private void setTextViewDate(String date) {
        dateDisplay.setText(date);
    }
}
