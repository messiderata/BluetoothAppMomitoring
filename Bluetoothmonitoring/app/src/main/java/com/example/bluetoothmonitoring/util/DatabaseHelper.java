package com.example.bluetoothmonitoring.util;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "BluetoothData.db";
    private static final int DATABASE_VERSION = 2;
    private static final String TABLE_NAME = "data";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_DATA = "data";
    private static final String COLUMN_DATE = "date";  // Store date in yyyy-MM-dd format
    private static final String COLUMN_TIME = "time";  // Store time in HH:mm:ss format

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE data (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "data REAL, " +
                        "date TEXT, " +  // Date in yyyy-MM-dd format
                        "time TEXT)"  // Time in HH:mm:ss format
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {  // Upgrade to add 'time' column if version is updated
            db.execSQL("ALTER TABLE data ADD COLUMN time TEXT");
        }
    }

    // Insert data into the database
    public void insertData(int data, String date, String time) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_DATA, data);
        values.put(COLUMN_DATE, date);
        values.put(COLUMN_TIME, time);
        db.insert(TABLE_NAME, null, values);
        db.close();  // Safely close the database after the transaction
    }

    // Get the average data for a specific date
    @SuppressLint("Range")
    public float getAverageDataForDate(String date) {
        float sum = 0;
        int count = 0;
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = this.getReadableDatabase();
            cursor = db.rawQuery("SELECT " + COLUMN_DATA + " FROM " + TABLE_NAME + " WHERE " + COLUMN_DATE + " = ?", new String[]{date});

            if (cursor.moveToFirst()) {
                do {
                    float data = cursor.getFloat(cursor.getColumnIndex(COLUMN_DATA));
                    sum += data;
                    count++;
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("DatabaseError", "Error querying data for date: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }

        return count == 0 ? 0 : sum / count;
    }

    // Get the average data for a specific number of days
    @SuppressLint("Range")
    public List<DateAverageData> getAverageDataWithDatesForDays(int days, boolean showMonth) {
        List<DateAverageData> averagesWithDates = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat querySdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat labelSdf = showMonth ? new SimpleDateFormat("MMM", Locale.getDefault()) : new SimpleDateFormat("E", Locale.getDefault());

        try {
            db = this.getReadableDatabase();
            for (int i = 0; i < days; i++) {
                String queryDate = querySdf.format(calendar.getTime());
                String labelDate = labelSdf.format(calendar.getTime());

                cursor = db.rawQuery("SELECT " + COLUMN_DATA + " FROM " + TABLE_NAME + " WHERE " + COLUMN_DATE + " = ?", new String[]{queryDate});

                float sum = 0;
                int count = 0;

                if (cursor.moveToFirst()) {
                    do {
                        float data = cursor.getFloat(cursor.getColumnIndex(COLUMN_DATA));
                        sum += data;
                        count++;
                    } while (cursor.moveToNext());
                }

                float average = count == 0 ? 0 : sum / count;
                averagesWithDates.add(new DateAverageData(labelDate, average));

                calendar.add(Calendar.DATE, -1);  // Move to the previous day
            }
        } catch (Exception e) {
            Log.e("DatabaseError", "Error querying data for days: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }

        return averagesWithDates;
    }

    // Get the average data for the last 24 hours
    @SuppressLint("Range")
    public List<DateAverageData> getAverageDataFor24Hours(String date) {
        List<DateAverageData> averagesWithHours = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            for (int hour = 0; hour < 24; hour++) {
                String hourString = String.format(Locale.getDefault(), "%02d", hour);  // Always use 24-hour format

                cursor = db.rawQuery(
                        "SELECT data FROM data WHERE date = ? AND strftime('%H', time) = ?",
                        new String[]{date, hourString}
                );

                float sum = 0;
                int count = 0;

                if (cursor.moveToFirst()) {
                    do {
                        float data = cursor.getFloat(cursor.getColumnIndex("data"));
                        sum += data;
                        count++;
                    } while (cursor.moveToNext());
                }

                float average = count == 0 ? 0 : sum / count;
                averagesWithHours.add(new DateAverageData(hourString, average));

                if (cursor != null) {
                    cursor.close();
                }
            }
        } catch (Exception e) {
            Log.e("DatabaseError", "Error querying data: " + e.getMessage());
        } finally {
            if (db != null) {
                db.close();
            }
        }
        return averagesWithHours;
    }


    // Get the average data for a specific hour
    public List<DateAverageData> getAverageDataForHour(String date, String hour) {
        List<DateAverageData> hourlyData = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = this.getReadableDatabase();
            cursor = db.rawQuery(
                    "SELECT data FROM " + TABLE_NAME + " WHERE date = ? AND strftime('%H', time) = ?",
                    new String[]{date, hour}
            );

            if (cursor.moveToFirst()) {
                do {
                    float data = cursor.getFloat(cursor.getColumnIndex(COLUMN_DATA));
                    hourlyData.add(new DateAverageData(hour, data));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("DatabaseError", "Error querying data for hour: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }

        return hourlyData;
    }
}
