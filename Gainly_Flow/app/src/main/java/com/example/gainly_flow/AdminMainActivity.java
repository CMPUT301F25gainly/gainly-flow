package com.example.gainly_flow;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Arrays;

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
 *     <li>Displays total counts of events and users fetched from Firestore.</li>
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

    private FirebaseFirestore db;

    /**
     * Initializes the admin dashboard and sets up UI components,
     * click listeners, and loads real data from Firestore.
     *
     * @param savedInstanceState the previously saved instance state, if available
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

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

        // ---- Load real data from Firestore ----
        loadEventCount();
        loadUserCount();
    }

    /**
     * Loads the total number of events from Firestore.
     */
    private void loadEventCount() {
        db.collection("events")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int eventCount = querySnapshot.size();
                    tvTotalEvents.setText(String.valueOf(eventCount));
                    Log.d("AdminMain", "Loaded " + eventCount + " events");
                })
                .addOnFailureListener(e -> {
                    Log.e("AdminMain", "Error loading event count: " + e.getMessage());
                    tvTotalEvents.setText("0");
                });
    }

    /**
     * Loads the total number of users from Firestore.
     * Assumes users are stored in a "profiles" collection.
     */
    private void loadUserCount() {
        db.collection("profiles")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int userCount = querySnapshot.size();
                    tvTotalUsers.setText(String.valueOf(userCount));
                    Log.d("AdminMain", "Loaded " + userCount + " users");
                })
                .addOnFailureListener(e -> {
                    Log.e("AdminMain", "Error loading user count: " + e.getMessage());
                    tvTotalUsers.setText("0");
                });
    }

    /**
     * Alternative method to load user count with role filtering.
     * Use this if you want to count only specific roles (e.g., exclude admins).
     */
    private void loadUserCountByRole() {
        // If you want to count only entrants and organizers (exclude admins)
        db.collection("profiles")
                .whereIn("role", Arrays.asList("Entrant", "Organizer"))
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int userCount = querySnapshot.size();
                    tvTotalUsers.setText(String.valueOf(userCount));
                })
                .addOnFailureListener(e -> {
                    Log.e("AdminMain", "Error loading user count: " + e.getMessage());
                    tvTotalUsers.setText("0");
                });
    }

    /**
     * Loads both counts simultaneously for better performance.
     */
    private void loadAllCounts() {
        // Load events count
        db.collection("events").get().addOnSuccessListener(eventSnapshot -> {
            int eventCount = eventSnapshot.size();
            tvTotalEvents.setText(String.valueOf(eventCount));
        }).addOnFailureListener(e -> {
            Log.e("AdminMain", "Error loading event count: " + e.getMessage());
            tvTotalEvents.setText("0");
        });

        // Load users count
        db.collection("profiles").get().addOnSuccessListener(userSnapshot -> {
            int userCount = userSnapshot.size();
            tvTotalUsers.setText(String.valueOf(userCount));
        }).addOnFailureListener(e -> {
            Log.e("AdminMain", "Error loading user count: " + e.getMessage());
            tvTotalUsers.setText("0");
        });
    }

    /**
     * Refreshes the counts when the activity resumes.
     */
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh counts when returning to this activity
        loadEventCount();
        loadUserCount();
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
}