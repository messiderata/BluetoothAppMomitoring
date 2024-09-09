package com.example.bluetoothmonitoring.util;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

public class TimeGet {

    // Method to get the current formatted time
    // Ensure you're using 24-hour format in TimeGet
    public String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());  // 24-hour format
        return sdf.format(new Date());
    }


    public String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    // Method to get the current formatted time
    public String getCurrentTimeFormatted() {
        LocalDateTime currentTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a");
        return currentTime.format(formatter);
    }

}
