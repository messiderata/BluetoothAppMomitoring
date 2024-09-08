package com.example.bluetoothmonitoring.util;

public class DateAverageData {
    private String date;
    private float average;

    public DateAverageData(String date, float average) {
        this.date = date;
        this.average = average;
    }

    public String getDate() {
        return date;
    }

    public float getAverage() {
        return average;
    }
}
