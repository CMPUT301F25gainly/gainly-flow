package com.example.gainly_flow;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class AdminMainActivity extends AppCompatActivity {

    private LinearLayout rowBrowseEvents;
    private LinearLayout rowBrowseProfiles; // optional: hook up if you want
    private LinearLayout rowBrowseImages;   // optional
    private LinearLayout rowNotificationLogs; // optional

    private TextView tvTotalEvents;
    private TextView tvTotalUsers;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        // ---- bind views (IDs come from activity_admin_main.xml) ----
        rowBrowseEvents = findViewById(R.id.rowBrowseEvents);
        rowBrowseProfiles = findViewById(R.id.rowBrowseProfiles);
        rowBrowseImages = findViewById(R.id.rowBrowseImages);
        rowNotificationLogs = findViewById(R.id.rowNotificationLogs);

        tvTotalEvents = findViewById(R.id.tvTotalEvents);
        tvTotalUsers  = findViewById(R.id.tvTotalUsers);

        // ---- wire up navigation ----
        rowBrowseEvents.setOnClickListener(v ->
                startActivity(new Intent(AdminMainActivity.this, AdminBrowseEventsActivity.class))
        );

        // (Optional) enable these if you’ve created the screens
        rowBrowseProfiles.setOnClickListener(v ->
                startActivity(new Intent(AdminMainActivity.this, AdminBrowseProfilesActivity.class))
        );

        // TODO: when you create activities for Images and Logs, point these to them.
        rowBrowseImages.setOnClickListener(v ->
                toastNotImplemented(v)
        );
        rowNotificationLogs.setOnClickListener(v ->
                toastNotImplemented(v)
        );

        // ---- populate dashboard stats (stubbed; replace with real data source) ----
        // TODO: Replace with counts from your database / repository.
        tvTotalEvents.setText(String.valueOf(getFakeEventCount()));
        tvTotalUsers.setText(String.valueOf(getFakeUserCount()));
    }

    private void toastNotImplemented(View v) {
        // You can swap this for a Snackbar/Toast to indicate the screen is coming soon.
        // Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show();
    }

    // --------- stub data (remove once wired to real repo) ----------
    private int getFakeEventCount() { return 3; }
    private int getFakeUserCount()  { return 3; }
}
