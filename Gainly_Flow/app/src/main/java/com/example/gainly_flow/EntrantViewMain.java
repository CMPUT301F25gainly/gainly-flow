package com.example.gainly_flow;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import java.util.Locale;

/**
 * Entrant home screen that lists available events.
 * Fixed to match Firestore field names and optional fields.
 */
public class EntrantViewMain extends AppCompatActivity {

    private MaterialButton browseEventsButton, lotteryGuidelinesButton;
    private BottomNavigationView bottomNav;
    private LinearLayout eventListContainer;
    private ImageButton backButton;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private Profile currentProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrant_home);

        // Get profile from intent
        currentProfile = (Profile) getIntent().getSerializableExtra("profile");
        if (currentProfile == null) {
            Toast.makeText(this, "Profile data not available", Toast.LENGTH_SHORT).show();
            // You might want to redirect to MainActivity or handle this case
        }

        browseEventsButton = findViewById(R.id.browseEventsButton);
        lotteryGuidelinesButton = findViewById(R.id.lotteryGuidelinesButton);
        bottomNav = findViewById(R.id.bottomNav);
        eventListContainer = findViewById(R.id.eventListContainer);
        backButton = findViewById(R.id.backButton);

        // Clear container
        eventListContainer.removeAllViews();

        // Buttons
        browseEventsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, QRCodeScanner.class);
            intent.putExtra("profile", currentProfile);
            startActivity(intent);
        });
        lotteryGuidelinesButton.setOnClickListener(v -> showLotteryGuidelines());
        backButton.setOnClickListener(v -> onBackPressed());
        bottomNav.setOnItemSelectedListener(this::onNavItemSelected);

        // Load events
        loadEventsFromFirebase();
    }

    private void loadEventsFromFirebase() {
        FirebaseFirestore.getInstance()
                .collection("events")
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

                    Log.d("EntrantViewMain", "Found " + querySnapshot.size() + " events");

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Log.d("EntrantViewMain", "Doc " + doc.getId() + ": " + doc.getData());

                        Event e = new Event(doc.getId());

                        e.setName(doc.getString("name"));
                        e.setDescription(doc.getString("description"));
                        e.setPosterImageId(doc.getString("posterImageId"));

                        Long capLong = doc.getLong("capacity");
                        e.setCapacity(capLong != null ? capLong.intValue() : 20);

                        Long currentLong = doc.getLong("currentParticipants");
                        e.setCurrentParticipants(currentLong != null ? currentLong.intValue() : 0);

                        // Geolocation
                        Boolean geo = doc.getBoolean("geolocationRequired"); // FIXED FIELD
                        e.setGeolocationRequired(Boolean.TRUE.equals(geo));

                        // Event date
                        Long eventDateMs = doc.getLong("eventDateUtc");
                        if (eventDateMs != null) e.setEventDate(new Date(eventDateMs));

                        // Registration period
                        Long openMs = doc.getLong("registrationOpenUtc");
                        Long closeMs = doc.getLong("registrationCloseUtc");
                        e.setRegistrationPeriod(
                                openMs != null ? new Date(openMs) : null,
                                closeMs != null ? new Date(closeMs) : null
                        );

                        addEventToView(e);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load events: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void addEventToView(Event event) {
        CardView eventView = (CardView) LayoutInflater.from(this)
                .inflate(R.layout.item_event, eventListContainer, false);

        TextView eventTitle = eventView.findViewById(R.id.eventTitle);
        TextView eventDate = eventView.findViewById(R.id.eventDate);
        TextView eventSpots = eventView.findViewById(R.id.eventSpots);
        TextView eventWaiting = eventView.findViewById(R.id.eventWaiting);
        TextView eventStatus = eventView.findViewById(R.id.eventStatus);

        eventTitle.setText(event.getName());
        if (event.getEventDate() != null) eventDate.setText(dateFormat.format(event.getEventDate()));
        eventSpots.setText(event.getCapacity() + " spots");
        eventWaiting.setText("0 waiting"); // Placeholder

        // Status
        if (event.isRegistrationOpen()) {
            eventStatus.setText("Open");
            eventStatus.setTextColor(getResources().getColor(R.color.green_500));
            eventStatus.setBackgroundResource(R.drawable.status_open_bg);
        } else {
            eventStatus.setText("Closed");
            eventStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            eventStatus.setBackgroundResource(R.drawable.status_closed_bg);
        }

        eventView.setOnClickListener(v -> {
            Intent intent = new Intent(EntrantViewMain.this, EventDetailActivity.class);
            intent.putExtra("event_id", event.getId());
            intent.putExtra("event_name", event.getName());
            intent.putExtra("event_description", event.getDescription());
            intent.putExtra("event_capacity", event.getCapacity());
            intent.putExtra("event_date", event.getEventDate() != null ? event.getEventDate().getTime() : 0);
            intent.putExtra("registration_open", event.getRegistrationOpen() != null ? event.getRegistrationOpen().getTime() : 0);
            intent.putExtra("registration_close", event.getRegistrationClose() != null ? event.getRegistrationClose().getTime() : 0);
            intent.putExtra("profile", currentProfile);
            startActivity(intent);
        });

        eventListContainer.addView(eventView);
    }

    private boolean onNavItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_events) {
            // Already on events page, just refresh or do nothing
            // startActivity(new Intent(this, EntrantViewMain.class));
            // finish();
        } else if (id == R.id.menu_notifications) {
            Intent intent = new Intent(this, NotificationsActivity.class);
            intent.putExtra("profile", currentProfile);
            startActivity(intent);
        } else if (id == R.id.menu_profile) {
            Intent intent = new Intent(this, ProfileActivity.class);
            intent.putExtra("profile", currentProfile);
            intent.putExtra("userType", "Entrant");
            startActivity(intent);
        }
        return true;
    }

    private void showLotteryGuidelines() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Lottery Selection Process")
                .setMessage("1. All entrants must register before the deadline.\n" +
                        "2. Lottery is random and fair.\n" +
                        "3. Only registered entrants are eligible.\n" +
                        "4. Winners will be notified via email or app notification.\n" +
                        "5. Each entrant can only win one spot per event.\n\nFor more details, contact support.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update the selected item in bottom navigation
        bottomNav.setSelectedItemId(R.id.menu_events);
    }
}