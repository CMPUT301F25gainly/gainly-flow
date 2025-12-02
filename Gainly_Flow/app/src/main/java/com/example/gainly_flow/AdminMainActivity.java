package com.example.gainly_flow;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Main dashboard activity for administrators.
 * This activity provides quick navigation to administrative management screens,
 * including browsing events, profiles, images, and notification logs.
 * It also displays basic statistics such as the total number of events and users.
 *
 * Features:
 * Navigation to different admin management activities.
 * Displays total counts of events and users.
 * Includes a back button to return to the previous activity.
 *
 * @author Gainly Flow Team
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
                tvTotalUsers = findViewById(R.id.tvTotalUsers);

                backButton = findViewById(R.id.backButton_admin);
                backButton.setOnClickListener(v -> onBackPressed());

                // ---- Wire up navigation ----
                rowBrowseEvents.setOnClickListener(v -> startActivity(
                                new Intent(AdminMainActivity.this, AdminBrowseEventsActivity.class)));

                // Navigate to profile management screen
                rowBrowseProfiles.setOnClickListener(v -> startActivity(
                                new Intent(AdminMainActivity.this, AdminBrowseProfilesActivity.class)));

                // Navigate to image management screen
                rowBrowseImages.setOnClickListener(v -> startActivity(
                                new Intent(AdminMainActivity.this, AdminBrowseImagesActivity.class)));

                // Navigate to notification logs screen
                rowNotificationLogs.setOnClickListener(v -> startActivity(
                                new Intent(AdminMainActivity.this, AdminNotificationLogsActivity.class)));

                // ---- Populate dashboard stats ----
                fetchDashboardStats();
        }

        /**
         * Fetches and displays the total number of events and users from Firestore.
         */
        private void fetchDashboardStats() {
                FirebaseFirestore db = FirebaseFirestore.getInstance();

                // Count events
                db.collection("events").count().get(AggregateSource.SERVER).addOnSuccessListener(snapshot -> {
                        tvTotalEvents.setText(String.valueOf(snapshot.getCount()));
                }).addOnFailureListener(e -> {
                        tvTotalEvents.setText("-");
                });

                // Count users (profiles)
                db.collection("profiles").count().get(AggregateSource.SERVER).addOnSuccessListener(snapshot -> {
                        tvTotalUsers.setText(String.valueOf(snapshot.getCount()));
                }).addOnFailureListener(e -> {
                        tvTotalUsers.setText("-");
                });
        }

        /**
         * Displays a placeholder message (e.g., "Coming soon") for unimplemented
         * features.
         *
         * @param v the view that triggered the click event
         */
        private void toastNotImplemented(View v) {
                // You can swap this for a Snackbar/Toast to indicate the screen is coming soon.
                // Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show();
        }

}
