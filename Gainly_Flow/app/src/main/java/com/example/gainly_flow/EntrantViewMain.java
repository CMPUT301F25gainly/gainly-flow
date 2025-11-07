package com.example.gainly_flow;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EntrantViewMain extends AppCompatActivity {

    private MaterialButton browseEventsButton;
    private BottomNavigationView bottomNav;
    private LinearLayout eventListContainer;
    private ImageButton backButton;
    private MaterialButton lotteryGuidelinesButton;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrant_home);

        browseEventsButton = findViewById(R.id.browseEventsButton);
        bottomNav = findViewById(R.id.bottomNav);
        eventListContainer = findViewById(R.id.eventListContainer);
        backButton = findViewById(R.id.backButton);
        lotteryGuidelinesButton = findViewById(R.id.lotteryGuidelinesButton);

        // Clear existing static events
        eventListContainer.removeAllViews();

        // Button to QR Page
        browseEventsButton.setOnClickListener(v -> {
            Intent toQR = new Intent(EntrantViewMain.this, QRCodeScanner.class);
            startActivity(toQR);
        });

        lotteryGuidelinesButton.setOnClickListener(v -> showLotteryGuidelines());

        backButton.setOnClickListener(v -> {
            onBackPressed();
        });

        // Load events from Firebase
        loadEventsFromFirebase();

        // Handle Bottom Navigation
        bottomNav.setOnItemSelectedListener(this::onNavItemSelected);
    }

    private void loadEventsFromFirebase() {
        FirebaseFirestore.getInstance()
                .collection("events")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    eventListContainer.removeAllViews();

                    if (querySnapshot.isEmpty()) {
                        TextView emptyText = new TextView(this);
                        emptyText.setText("No events available");
                        emptyText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                        emptyText.setPadding(0, 50, 0, 50);
                        eventListContainer.addView(emptyText);
                        return;
                    }

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String id = doc.getString("id");
                        if (id == null) continue;

                        Event e = new Event(id);
                        e.setName(doc.getString("name"));
                        e.setDescription(doc.getString("description"));

                        Long capacity = null;
                        try {
                            Number capNum = (Number) doc.get("capacity");
                            if (capNum != null) capacity = capNum.longValue();
                        } catch (Exception ignored) {}
                        if (capacity != null) e.setCapacity(capacity.intValue());

                        e.setGeolocationRequired(Boolean.TRUE.equals(doc.getBoolean("geolocationRequired")));
                        e.setPosterImage(doc.getString("posterUri"));

                        // registration dates
                        Long regOpen = doc.getLong("registrationOpen");
                        Long regClose = doc.getLong("registrationClose");
                        if (regOpen != null || regClose != null) {
                            e.setRegistrationPeriod(
                                    regOpen == null ? null : new Date(regOpen),
                                    regClose == null ? null : new Date(regClose)
                            );
                        }

                        addEventToView(e);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load events: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }


    private void addEventToView(Event event) {
        // Inflate the event item layout
        CardView eventView = (CardView) LayoutInflater.from(this).inflate(R.layout.item_event, eventListContainer, false);

        // Find views in the event item layout
        TextView eventTitle = eventView.findViewById(R.id.eventTitle);
        TextView eventDate = eventView.findViewById(R.id.eventDate);
        TextView eventSpots = eventView.findViewById(R.id.eventSpots);
        TextView eventWaiting = eventView.findViewById(R.id.eventWaiting);
        TextView eventStatus = eventView.findViewById(R.id.eventStatus);

        // Set event data
        if (eventTitle != null) {
            eventTitle.setText(event.getName());
        }

        if (eventDate != null && event.getEventDate() != null) {
            String dateString = dateFormat.format(event.getEventDate());
            eventDate.setText(dateString);
        }

        if (eventSpots != null) {
            // You might want to add actual spots data to your Event class
            eventSpots.setText(event.getCapacity() + " spots");
        }

        if (eventWaiting != null) {
            // You might want to add waiting list count to your Event class
            eventWaiting.setText("45 waiting"); // Replace with actual waiting count
        }

        if (eventStatus != null) {
            // Determine status based on registration period
            if (event.isRegistrationOpen()) {
                eventStatus.setText("Open");
                eventStatus.setTextColor(getResources().getColor(R.color.green_500));
                eventStatus.setBackgroundResource(R.drawable.status_open_bg);
            } else {
                eventStatus.setText("Closed");
                eventStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                eventStatus.setBackgroundResource(R.drawable.status_closed_bg); // Create this drawable
            }
        }

        // Set click listener
        eventView.setOnClickListener(v -> {
            Toast.makeText(EntrantViewMain.this, event.getName() + " clicked!", Toast.LENGTH_SHORT).show();

            // Pass event data to EventDetailActivity
            Intent intent = new Intent(EntrantViewMain.this, EventDetailActivity.class);
            intent.putExtra("event_id", event.getId());
            intent.putExtra("event_name", event.getName());
            intent.putExtra("event_description", event.getDescription());
            intent.putExtra("event_capacity", event.getCapacity());
            intent.putExtra("event_date", event.getEventDate() != null ? event.getEventDate().getTime() : 0);
            intent.putExtra("registration_open", event.getRegistrationOpen() != null ? event.getRegistrationOpen().getTime() : 0);
            intent.putExtra("registration_close", event.getRegistrationClose() != null ? event.getRegistrationClose().getTime() : 0);
            startActivity(intent);
        });

        // Add the event view to container
        eventListContainer.addView(eventView);
    }

    private boolean onNavItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_events) {
            Intent toEvent = new Intent(EntrantViewMain.this, EntrantViewMain.class);
            startActivity(toEvent);
        } else if (id == R.id.menu_notifications) {
            Intent toNotification = new Intent(EntrantViewMain.this, NotificationsActivity.class);
            startActivity(toNotification);
        } else if (id == R.id.menu_profile) {
            Intent toProfile = new Intent(EntrantViewMain.this, ProfileActivity.class);
            startActivity(toProfile);
        }
        return true;
    }

    // Method to show the popup dialog
    private void showLotteryGuidelines() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Lottery Selection Process");

        builder.setMessage("Here are the guidelines for the lottery selection process:\n\n" +
                "1. All entrants must register before the deadline.\n" +
                "2. Lottery is random and fair.\n" +
                "3. Only registered entrants are eligible.\n" +
                "4. Winners will be notified via email or app notification.\n" +
                "5. Each entrant can only win one spot per event.\n" +
                "\nFor more details, please contact support.");

        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}