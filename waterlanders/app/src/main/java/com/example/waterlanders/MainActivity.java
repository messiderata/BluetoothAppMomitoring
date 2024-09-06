package com.example.waterlanders;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler; // Import Handler class
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private Handler handler; // Declare Handler variable

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new Handler(); // Initialize Handler

        // Post delayed execution
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(MainActivity.this, login.class);
                startActivity(intent);
                finish();
            }
        }, 3000); // Changed delay to 3000 milliseconds// Corrected delayMillis to 3000 (milliseconds)

    }
}
