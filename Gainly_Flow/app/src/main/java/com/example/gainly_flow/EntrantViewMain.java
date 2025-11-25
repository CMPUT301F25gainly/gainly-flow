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
    private Entrant currentEntrant;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrant_home);

        // Get profile from intent - handle both Profile and Entrant objects
        initializeProfileData();

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
            if (currentEntrant != null) {
                intent.putExtra("entrant", currentEntrant);
            } else if (currentProfile != null) {
                intent.putExtra("profile", currentProfile);
            }
            startActivity(intent);
        });
        lotteryGuidelinesButton.setOnClickListener(v -> showLotteryGuidelines());
        backButton.setOnClickListener(v -> onBackPressed());
        bottomNav.setOnItemSelectedListener(this::onNavItemSelected);

        // Load events
        loadEventsFromFirebase();
    }

    private void initializeProfileData() {
        // Try to get Entrant first
        currentEntrant = (Entrant) getIntent().getSerializableExtra("entrant");
        if (currentEntrant != null) {
            currentProfile = currentEntrant; // Entrant extends Profile
            Log.d("EntrantViewMain", "Loaded Entrant: " + currentEntrant.getDisplayName());
            return;
        }

        // Try to get Profile
        currentProfile = (Profile) getIntent().getSerializableExtra("profile");
        if (currentProfile != null) {
            Log.d("EntrantViewMain", "Loaded Profile: " + currentProfile.getDisplayName());

            // If we have a Profile but not an Entrant, try to convert it
            if (currentProfile instanceof Entrant) {
                currentEntrant = (Entrant) currentProfile;
            } else if ("Entrant".equals(currentProfile.getRole())) {
                // Convert Profile to Entrant
                currentEntrant = convertProfileToEntrant(currentProfile);
            }
        } else {
            // No profile data found
            Toast.makeText(this, "Profile data not available. Please log in again.", Toast.LENGTH_LONG).show();
            Log.e("EntrantViewMain", "No profile data found in intent");

            // Redirect to MainActivity
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private Entrant convertProfileToEntrant(Profile profile) {
        Entrant entrant = new Entrant(profile.getId());
        entrant.setDisplayName(profile.getDisplayName());
        entrant.setEmail(profile.getEmail());
        entrant.setPhone(profile.getPhone());
        entrant.setRole("Entrant");
        entrant.setDeviceId(profile.getDeviceId());
        entrant.setCreatedAt(profile.getCreatedAt());
        entrant.setLastLoginAt(profile.getLastLoginAt());
        entrant.setReceiveNotifications(profile.isReceiveNotifications());
        entrant.setEnableLocationService(profile.isEnableLocationService());
        return entrant;
    }

    private void loadEventsFromFirebase() {
        FirebaseFirestore.getInstance()
                .collection("events")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    eventListContainer.removeAllViews();

                    if (querySnapshot.isEmpty()) {
                        showEmptyState("No events available");
                        return;
                    }

                    Log.d("EntrantViewMain", "Found " + querySnapshot.size() + " events");

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Log.d("EntrantViewMain", "Doc " + doc.getId() + ": " + doc.getData());

                        Event event = createEventFromDocument(doc);
                        addEventToView(event);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("EntrantViewMain", "Failed to load events: " + e.getMessage());
                    Toast.makeText(this, "Failed to load events: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    showEmptyState("Failed to load events");
                });
    }

    private Event createEventFromDocument(DocumentSnapshot doc) {
        Event event = new Event(doc.getId());

        // Basic event info
        event.setName(doc.getString("name"));
        event.setDescription(doc.getString("description"));
        event.setPosterImageId(doc.getString("posterImageId"));
        event.setLocation(doc.getString("location"));
        event.setTimeString(doc.getString("timeString"));

        // Capacity and participants
        Long capLong = doc.getLong("capacity");
        event.setCapacity(capLong != null ? capLong.intValue() : 20);

        Long currentLong = doc.getLong("currentParticipants");
        event.setCurrentParticipants(currentLong != null ? currentLong.intValue() : 0);

        // Geolocation
        Boolean geo = doc.getBoolean("geolocationRequired");
        event.setGeolocationRequired(Boolean.TRUE.equals(geo));

        // Event date
        Object eventDateObj = doc.get("eventDate");
        if (eventDateObj instanceof Date) {
            event.setEventDate((Date) eventDateObj);
        } else if (eventDateObj instanceof Long) {
            event.setEventDate(new Date((Long) eventDateObj));
        }

        // Registration period
        Object openObj = doc.get("registrationOpen");
        if (openObj instanceof Date) {
            event.setRegistrationOpen((Date) openObj);
        } else if (openObj instanceof Long) {
            event.setRegistrationOpen(new Date((Long) openObj));
        }

        Object closeObj = doc.get("registrationClose");
        if (closeObj instanceof Date) {
            event.setRegistrationClose((Date) closeObj);
        } else if (closeObj instanceof Long) {
            event.setRegistrationClose(new Date((Long) closeObj));
        }

        // Price
        Double price = doc.getDouble("price");
        if (price != null) {
            event.setPrice(price);
        }

        // Category
        String category = doc.getString("category");
        if (category != null) {
            try {
                event.setCategory(Event.Category.valueOf(category));
            } catch (IllegalArgumentException e) {
                event.setCategory(Event.Category.ALL);
            }
        }

        // Lists
        event.setWaitingList((java.util.List<String>) doc.get("waitingList"));
        event.setSelected((java.util.List<String>) doc.get("selected"));
        event.setEnrolled((java.util.List<String>) doc.get("enrolled"));
        event.setCancelled((java.util.List<String>) doc.get("cancelled"));

        return event;
    }

    private void showEmptyState(String message) {
        TextView emptyText = new TextView(this);
        emptyText.setText(message);
        emptyText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        emptyText.setPadding(0, 50, 0, 50);
        emptyText.setTextSize(16f);
        eventListContainer.addView(emptyText);
    }

    private void addEventToView(Event event) {
        CardView eventView = (CardView) LayoutInflater.from(this)
                .inflate(R.layout.item_event, eventListContainer, false);

        TextView eventTitle = eventView.findViewById(R.id.eventTitle);
        TextView eventDate = eventView.findViewById(R.id.eventDate);
        TextView eventSpots = eventView.findViewById(R.id.eventSpots);
        TextView eventWaiting = eventView.findViewById(R.id.eventWaiting);
        TextView eventStatus = eventView.findViewById(R.id.eventStatus);

        eventTitle.setText(event.getName() != null ? event.getName() : "Unnamed Event");

        if (event.getEventDate() != null) {
            eventDate.setText(dateFormat.format(event.getEventDate()));
        } else {
            eventDate.setText("Date TBD");
        }

        eventSpots.setText(event.getCapacity() + " spots");
        eventWaiting.setText(event.getWaitingListSize() + " waiting");

        // Status with better logic
        String status = event.getRegistrationStatus();
        eventStatus.setText(status);

        switch (status) {
            case "OPEN":
                eventStatus.setTextColor(getResources().getColor(R.color.green_500));
                eventStatus.setBackgroundResource(R.drawable.status_open_bg);
                break;
            case "FULL":
                eventStatus.setTextColor(getResources().getColor(R.color.yellow_500));
                eventStatus.setBackgroundResource(R.drawable.box_waiting);
                break;
            case "CLOSED":
                eventStatus.setTextColor(getResources().getColor(R.color.grey_500));
                eventStatus.setBackgroundResource(R.drawable.status_closed_bg);
                break;
            case "CANCELLED":
                eventStatus.setTextColor(getResources().getColor(R.color.grey_500));
                eventStatus.setBackgroundResource(R.drawable.box_cancelled);
                break;
            default:
                eventStatus.setTextColor(getResources().getColor(android.R.color.darker_gray));
                eventStatus.setBackgroundResource(R.drawable.status_closed_bg);
        }

        eventView.setOnClickListener(v -> {
            Intent intent = new Intent(EntrantViewMain.this, EventDetailActivity.class);
            intent.putExtra("event_id", event.getId());
            intent.putExtra("event_name", event.getName());
            intent.putExtra("event_description", event.getDescription());
            intent.putExtra("event_capacity", event.getCapacity());
            intent.putExtra("current_participants", event.getCurrentParticipants());

            if (event.getEventDate() != null) {
                intent.putExtra("event_date", event.getEventDate().getTime());
            }
            if (event.getRegistrationOpen() != null) {
                intent.putExtra("registration_open", event.getRegistrationOpen().getTime());
            }
            if (event.getRegistrationClose() != null) {
                intent.putExtra("registration_close", event.getRegistrationClose().getTime());
            }

            intent.putExtra("event_location", event.getLocation());
            intent.putExtra("event_time_string", event.getTimeString());
            intent.putExtra("geo_required", event.isGeolocationRequired());
            intent.putExtra("event_price", event.getPrice());

            // Pass the appropriate profile/entrant object
            if (currentEntrant != null) {
                intent.putExtra("entrant", currentEntrant);
            } else if (currentProfile != null) {
                intent.putExtra("profile", currentProfile);
            }

            startActivity(intent);
        });

        eventListContainer.addView(eventView);
    }

    private boolean onNavItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Intent intent;

        if (id == R.id.menu_events) {
            // Already on events page, refresh
            loadEventsFromFirebase();
        } else if (id == R.id.menu_notifications) {
            intent = new Intent(this, NotificationsActivity.class);
            if (currentEntrant != null) {
                intent.putExtra("entrant", currentEntrant);
            } else if (currentProfile != null) {
                intent.putExtra("profile", currentProfile);
            }
            startActivity(intent);
        } else if (id == R.id.menu_profile) {
            intent = new Intent(this, ProfileActivity.class);
            if (currentEntrant != null) {
                intent.putExtra("profile", currentEntrant);
            } else if (currentProfile != null) {
                intent.putExtra("profile", currentProfile);
            }
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
        // Refresh events when returning to this activity
        loadEventsFromFirebase();
    }
}