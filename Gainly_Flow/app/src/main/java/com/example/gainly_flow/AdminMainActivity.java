package com.example.gainly_flow;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * {@code AdminMainActivity} serves as the main dashboard for administrators.
 * <p>
 * This activity provides quick navigation to administrative management screens,
 * including browsing events, profiles, images, and notification logs.
 * It also displays basic statistics such as the total number of events and users.
 * </p>
 *
 * <p><b>Features:</b></p>
 * <ul>
 *     <li>Navigation to different admin management activities.</li>
 *     <li>Displays total counts of events and users (currently stubbed with placeholder data).</li>
 *     <li>Includes a back button to return to the previous activity.</li>
 * </ul>
 *
 * <p><b>Associated Layout:</b></p>
 * <ul>
 *     <li>{@code activity_admin_main.xml} — Defines the main layout for the admin dashboard.</li>
 * </ul>
 *
 * @author
 * @version 1.0
 */
public class AdminMainActivity extends AppCompatActivity {

    /** Row that navigates to the event browsing screen. */
    private LinearLayout rowBrowseEvents;

    /** Row that navigates to the profile browsing screen. */
    private LinearLayout rowBrowseProfiles;

    /** Row for image management (currently not implemented). */
    private LinearLayout rowBrowseImages;

    /** Row for viewing notification logs (currently not implemented). */
    private LinearLayout rowNotificationLogs;

    /** TextView displaying the total number of events. */
    private TextView tvTotalEvents;

    /** TextView displaying the total number of users. */
    private TextView tvTotalUsers;

    /** Back button to return to the previous screen. */
    private ImageButton backButton;

    /**
     * Initializes the admin dashboard and sets up UI components,
     * click listeners, and placeholder data.
     *
     * @param savedInstanceState the previously saved instance state, if available
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        // ---- Bind views (IDs come from activity_admin_main.xml) ----
        rowBrowseEvents = findViewById(R.id.rowBrowseEvents);
        rowBrowseProfiles = findViewById(R.id.rowBrowseProfiles);
        rowBrowseImages = findViewById(R.id.rowBrowseImages);
        rowNotificationLogs = findViewById(R.id.rowNotificationLogs);

        tvTotalEvents = findViewById(R.id.tvTotalEvents);
        tvTotalUsers  = findViewById(R.id.tvTotalUsers);

        backButton = findViewById(R.id.backButton_admin);
        backButton.setOnClickListener(v -> onBackPressed());

        // ---- Wire up navigation ----
        rowBrowseEvents.setOnClickListener(v ->
                startActivity(new Intent(AdminMainActivity.this, AdminBrowseEventsActivity.class))
        );

        // Navigate to profile management screen
        rowBrowseProfiles.setOnClickListener(v ->
                startActivity(new Intent(AdminMainActivity.this, AdminBrowseProfilesActivity.class))
        );

        // Navigate to image management screen
        rowBrowseImages.setOnClickListener(v ->
                startActivity(new Intent(AdminMainActivity.this, AdminBrowseImagesActivity.class))
        );

        // Navigate to notification logs screen
        rowNotificationLogs.setOnClickListener(v ->
                startActivity(new Intent(AdminMainActivity.this, AdminNotificationLogsActivity.class))
        );

        // ---- Populate dashboard stats (stubbed; replace with real data source) ----
        tvTotalEvents.setText(String.valueOf(getFakeEventCount()));
        tvTotalUsers.setText(String.valueOf(getFakeUserCount()));
    }

    /**
     * Displays a placeholder message (e.g., "Coming soon") for unimplemented features.
     *
     * @param v the view that triggered the click event
     */
    private void toastNotImplemented(View v) {
        // You can swap this for a Snackbar/Toast to indicate the screen is coming soon.
        // Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show();
    }

    /**
     * Returns a stubbed count of events.
     * <p>
     * This method currently returns a hardcoded value and should be replaced
     * with a real data source connection once implemented.
     * </p>
     *
     * @return a fake total event count
     */
    private int getFakeEventCount() { return 3; }

    /**
     * Returns a stubbed count of users.
     * <p>
     * This method currently returns a hardcoded value and should be replaced
     * with a real data source connection once implemented.
     * </p>
     *
     * @return a fake total user count
     */
    private int getFakeUserCount()  { return 3; }
}
