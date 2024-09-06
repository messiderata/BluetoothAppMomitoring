package com.example.bluetoothmonitoring.util;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "BluetoothData.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "data";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_DATA = "data";
    private static final String COLUMN_DATE = "date";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE " + TABLE_NAME + " (" +
                        COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_DATA + " REAL, " +
                        COLUMN_DATE + " TEXT" + // Added date column
                        ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    // Method to insert data with the current date
    public void insertData(int data, String date) {
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COLUMN_DATA, data);
            values.put(COLUMN_DATE, date);
            db.insert(TABLE_NAME, null, values);
        } catch (Exception e) {
            e.printStackTrace();  // Log any database errors
        } finally {
            if (db != null) db.close();
        }
    }

    // Method to retrieve all data for today and calculate the average
    @SuppressLint("Range")
    public float getAverageDataForToday(String todayDate) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        float sum = 0;
        int count = 0;

        try {
            // Query to get all data where the date matches today's date
            String query = "SELECT " + COLUMN_DATA + " FROM " + TABLE_NAME + " WHERE " + COLUMN_DATE + " = ?";
            cursor = db.rawQuery(query, new String[]{todayDate});

            // Loop through the results to calculate the sum and count of today's data
            if (cursor.moveToFirst()) {
                do {
                    float data = cursor.getFloat(cursor.getColumnIndex(COLUMN_DATA));
                    sum += data;
                    count++;
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }

        // Return the average (if there are no entries, return 0)
        return count == 0 ? 0 : sum / count;
    }

    public float getAverageDataForToday() {
        String todayDate = new TimeGet().getCurrentDate(); // Automatically get today's date
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        float sum = 0;
        int count = 0;

        try {
            // Query to get all data where the date matches today's date
            String query = "SELECT " + COLUMN_DATA + " FROM " + TABLE_NAME + " WHERE " + COLUMN_DATE + " = ?";
            cursor = db.rawQuery(query, new String[]{todayDate});

            // Loop through the results to calculate the sum and count of today's data
            if (cursor.moveToFirst()) {
                do {
                    @SuppressLint("Range") float data = cursor.getFloat(cursor.getColumnIndex(COLUMN_DATA));
                    sum += data;
                    count++;
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }

        // Return the average (if there are no entries, return 0)
        return count == 0 ? 0 : sum / count;
    }
}

