package com.example.bluetoothmonitoring.util;

public class HourlyAverageData {
    private String hour;
    private float average;

    public HourlyAverageData(String hour, float average) {
        this.hour = hour;
        this.average = average;
    }

    public String getHour() {
        return hour;
    }

    public float getAverage() {
        return average;
    }
}
