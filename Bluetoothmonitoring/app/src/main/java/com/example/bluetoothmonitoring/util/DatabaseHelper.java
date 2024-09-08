package com.example.bluetoothmonitoring.util;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "BluetoothData.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "data";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_DATA = "data";
    private static final String COLUMN_DATE = "date";  // Store date in yyyy-MM-dd format

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE " + TABLE_NAME + " (" +
                        COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_DATA + " REAL, " +
                        COLUMN_DATE + " TEXT" + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    // Method to insert data, store date in yyyy-MM-dd format
    public void insertData(int data, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_DATA, data);
        values.put(COLUMN_DATE, date);  // Store date in yyyy-MM-dd format
        db.insert(TABLE_NAME, null, values);
        db.close();
    }

    // Method to get the average data for a specific date
    @SuppressLint("Range")
    public float getAverageDataForDate(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_DATA + " FROM " + TABLE_NAME + " WHERE " + COLUMN_DATE + " = ?", new String[]{date});
        float sum = 0;
        int count = 0;
        if (cursor.moveToFirst()) {
            do {
                float data = cursor.getFloat(cursor.getColumnIndex(COLUMN_DATA));
                sum += data;
                count++;
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return count == 0 ? 0 : sum / count;
    }

    // Method to get the average data for multiple consecutive days
    @SuppressLint("Range")
    public List<DateAverageData> getAverageDataWithDatesForDays(int days, boolean showMonth) {
        List<DateAverageData> averagesWithDates = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat querySdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());  // Query using full date
        SimpleDateFormat labelSdf;  // To format labels as either month or day of the week

        // Set the label format depending on whether you want to show the month or the day of the week
        if (showMonth) {
            labelSdf = new SimpleDateFormat("MMM", Locale.getDefault());  // Month abbreviation
        } else {
            labelSdf = new SimpleDateFormat("E", Locale.getDefault());  // Day of the week abbreviation (M, T, etc.)
        }

        // Loop through the last 'days' number of days
        try {
            for (int i = 0; i < days; i++) {
                String queryDate = querySdf.format(calendar.getTime());  // Get full date format for querying
                String labelDate = labelSdf.format(calendar.getTime());  // Get formatted label (month or day of week)
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

                // Add Date and its average to the list (with formatted label)
                float average = count == 0 ? 0 : sum / count;
                averagesWithDates.add(new DateAverageData(labelDate, average));

                // Close the cursor before moving to the next date
                if (cursor != null) cursor.close();

                // Move to the previous day
                calendar.add(Calendar.DATE, -1);
            }
        } finally {
            db.close();  // Ensure database is closed
        }

        return averagesWithDates;
    }
}
