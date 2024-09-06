package com.example.bluetoothmonitoring;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.bluetoothmonitoring.Fragment.DeviceConnectionFragment;
import com.example.bluetoothmonitoring.Fragment.MonitoringFragment;
import com.google.android.material.navigation.NavigationView;

public class MainDashBoard extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private int lastSelectedItemId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_dash_board);

        // Initialize views
        ImageView menuButton = findViewById(R.id.menu_button);
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        // Set the status bar color
        setStatusBarColor();

        // Set default fragment as MonitoringFragment if no saved instance exists
        if (savedInstanceState == null) {
            // Automatically select the Monitoring item in the navigation drawer
            navigationView.setCheckedItem(R.id.Monitoring);
            replaceFragment(new MonitoringFragment());
        }

        // Handle menu button click to open/close the navigation drawer
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            }
        });

        // Handle navigation item clicks
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.Monitoring) {
                    replaceFragment(new MonitoringFragment());
                    lastSelectedItemId = R.id.Monitoring;  // Update last selected
                } else if (id == R.id.device) {
                    replaceFragment(new DeviceConnectionFragment());
                    lastSelectedItemId = R.id.device;  // Update last selected
                } else if (id == R.id.exit) {
                    showExitConfirmationDialog();  // Show exit confirmation dialog
                }

                // Close the drawer when an item is selected
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.addToBackStack(null);  // Allows back navigation
        fragmentTransaction.commit();
    }

    private void showExitConfirmationDialog() {
        // Inflate the custom layout/view
        android.view.LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.exit_dialog, null); // Make sure the layout file is correctly named

        // Create the dialog and set the custom view
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        // Get reference to the Yes and No buttons
        Button yesButton = dialogView.findViewById(R.id.yes_button); // Make sure to set an id for the buttons in XML
        Button noButton = dialogView.findViewById(R.id.no_button);   // Set id in your XML as well

        // Create the dialog
        AlertDialog dialog = builder.create();
        dialog.show();

        // Set click listener for the Yes button
        yesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Exit the app
                finishAffinity();  // This will close all activities and exit the app

            }
        });

        // Set click listener for the No button
        noButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();  // Dismiss the dialog
                // Return to the previous fragment
                NavigationView navigationView = findViewById(R.id.nav_view);
                navigationView.setCheckedItem(lastSelectedItemId);  // Highlight last selected item

                // Reload the last fragment based on lastSelectedItemId
                if (lastSelectedItemId == R.id.Monitoring) {
                    replaceFragment(new MonitoringFragment());
                } else if (lastSelectedItemId == R.id.device) {
                    replaceFragment(new DeviceConnectionFragment());
                }

            }
        });
    }

    @SuppressLint("ObsoleteSdkInt")
    private void setStatusBarColor() {
        // Check if the Android version supports status bar customization
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.main_bg));
        }
    }

    @Override
    public void onBackPressed() {
        // Close drawer if open when back is pressed
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
