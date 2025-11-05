package com.example.gainly_flow; // ← Replace with your actual package name

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gainly_flow.AdminBrowseProfilesActivity;
import com.example.gainly_flow.Database;

/**
 * AdminMainActivity
 *
 * This Activity serves as the administrator dashboard.
 * It displays total counts of events and users, and provides
 * navigation to admin tools for browsing/removing events and profiles.
 *
 * Implements:
 *  - US 03.01.01 (Remove Event)
 *  - US 03.02.01 (Remove Profile)
 *  - US 03.04.01 (Browse Events)
 *  - US 03.05.01 (Browse Profiles)
 */
public class AdminMainActivity extends AppCompatActivity {

    // Tool row containers
    private LinearLayout rowBrowseEvents;
    private LinearLayout rowBrowseProfiles;
    private LinearLayout rowBrowseImages;      // Placeholder (not implemented)
    private LinearLayout rowNotificationLogs;  // Placeholder (not implemented)

    // Stat counters
    private TextView tvTotalEvents;
    private TextView tvTotalUsers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        // Initialize stat views
        tvTotalEvents = findViewById(R.id.tvTotalEvents);
        tvTotalUsers = findViewById(R.id.tvTotalUsers);

        // Initialize tool rows by ID
        rowBrowseEvents = findViewById(R.id.rowBrowseEvents);
        rowBrowseProfiles = findViewById(R.id.rowBrowseProfiles);
        rowBrowseImages = findViewById(R.id.rowBrowseImages);
        rowNotificationLogs = findViewById(R.id.rowNotificationLogs);

        // --- Click listeners ---
        // Browse Events
        rowBrowseEvents.setOnClickListener(v ->
                startActivity(new Intent(this, AdminBrowseEventsActivity.class)));

        // Browse Profiles
        rowBrowseProfiles.setOnClickListener(v ->
                startActivity(new Intent(this, AdminBrowseProfilesActivity.class)));

        // Unimplemented placeholders
        rowBrowseImages.setOnClickListener(v ->
                android.widget.Toast.makeText(this, "Browse Images not implemented yet", android.widget.Toast.LENGTH_SHORT).show());

        rowNotificationLogs.setOnClickListener(v ->
                android.widget.Toast.makeText(this, "Notification Logs not implemented yet", android.widget.Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Update live statistics
        if (tvTotalEvents != null)
            tvTotalEvents.setText(String.valueOf(Database.get().totalEvents()));

        if (tvTotalUsers != null)
            tvTotalUsers.setText(String.valueOf(Database.get().totalUsers()));
    }
}
