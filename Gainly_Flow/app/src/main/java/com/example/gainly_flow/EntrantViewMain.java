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

/**
 * Entrant home screen that lists available events, provides quick access to QR scanning,
 * lottery guidelines, and bottom navigation to notifications and profile.
 * <p>
 * Events are fetched from the {@code events} collection in Cloud Firestore and rendered
 * as cards in a vertical list. Tapping a card opens {@link EventDetailActivity}.
 * </p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Fetch and display events ordered by {@code createdAt} descending.</li>
 *   <li>Navigate to QR code scanner, notifications, and profile screens.</li>
 *   <li>Display a modal dialog describing the lottery selection process.</li>
 * </ul>
 *
 * <h3>Notes</h3>
 * This activity reads all events once at creation time (no live snapshot). Consider
 * switching to a real-time listener if you need automatic updates.
 */
public class EntrantViewMain extends AppCompatActivity {

    /** Button that navigates to the QR code scanner screen. */
    private MaterialButton browseEventsButton;
    /** Bottom navigation for switching between main sections. */
    private BottomNavigationView bottomNav;
    /** Container that holds event item views. */
    private LinearLayout eventListContainer;
    /** Back arrow in the app bar. */
    private ImageButton backButton;
    /** Button that shows the lottery guidelines dialog. */
    private MaterialButton lotteryGuidelinesButton;
    /** Date formatter for event dates (e.g., "Jan 05, 2025"). */
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    /**
     * Initializes the UI, wires click listeners, and loads events from Firestore.
     *
     * @param savedInstanceState previously saved state, if any
     */
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

    /**
     * Loads event documents from Firestore in descending {@code createdAt} order and renders
     * them in {@link #eventListContainer}. If no events exist, displays a placeholder message.
     * <p>
     * Expected schema (subset) for each document:
     * <ul>
     *   <li>{@code id} (String)</li>
     *   <li>{@code name} (String)</li>
     *   <li>{@code description} (String, optional)</li>
     *   <li>{@code capacity} (Number, optional)</li>
     *   <li>{@code geolocationRequired} (Boolean, optional)</li>
     *   <li>{@code posterUri} (String, optional)</li>
     *   <li>{@code registrationOpen}, {@code registrationClose} (epoch ms, optional)</li>
     * </ul>
     * </p>
     */
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

    /**
     * Inflates an event card from {@code R.layout.item_event}, binds event fields,
     * sets up click behavior to open {@link EventDetailActivity}, and adds the card
     * to the container.
     *
     * @param event the event to render
     */
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

    /**
     * Handles bottom navigation selections and launches the corresponding activity.
     *
     * @param item the selected menu item
     * @return {@code true} to indicate the selection was handled
     */
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

    /**
     * Displays a simple informational dialog that outlines the lottery selection process.
     * The dialog is dismissible via the "OK" button.
     */
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
