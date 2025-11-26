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
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Combined Entrant + Organizer home screen with a mode switch.
 */
public class EntrantOrganizerViewMain extends AppCompatActivity {

    private static final String TAG = "EntrantOrganizerViewMain";

    // Common UI
    private MaterialButton browseEventsButton, lotteryGuidelinesButton, eventHistoryButton;
    private BottomNavigationView bottomNav;
    private LinearLayout eventListContainer, headerLayout;
    private ImageButton backButton;

    // Mode switch UI
    private MaterialSwitch roleSwitch;
    private TextView entrantLabel, organizerLabel;

    // Entrant/profile
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private Profile currentProfile;
    private Entrant currentEntrant;

    // Organizer
    private FirebaseFirestore db;
    private String currentOrganizerId;

    // Mode flag: false = Entrant, true = Organizer
    private boolean isOrganizerMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrant_organizer_home);

        // Initialize profile/entrant data (same logic as EntrantViewMain)
        initializeProfileData();

        // Organizer side: init Firestore + organizer id
        db = FirebaseFirestore.getInstance();
        currentOrganizerId = getCurrentOrganizerId();
        Log.d(TAG, "Current Organizer ID: " + currentOrganizerId);

        // Bind views
        browseEventsButton = findViewById(R.id.browseEventsButton);
        lotteryGuidelinesButton = findViewById(R.id.lotteryGuidelinesButton);
        bottomNav = findViewById(R.id.bottomNav);
        eventListContainer = findViewById(R.id.eventListContainer);
        backButton = findViewById(R.id.backButton);
        headerLayout = findViewById(R.id.headerLayout);
        eventHistoryButton = findViewById(R.id.eventHistoryButton);
        roleSwitch = findViewById(R.id.roleSwitch);
        entrantLabel = findViewById(R.id.entrantLabel);
        organizerLabel = findViewById(R.id.organizerLabel);

        // Clear container
        eventListContainer.removeAllViews();

        // Back button
        backButton.setOnClickListener(v -> onBackPressed());

        // Entrant buttons
        browseEventsButton.setOnClickListener(v -> {
            if (!isOrganizerMode) {
                // Entrant: scan QR
                Intent intent = new Intent(this, QRCodeScanner.class);
                if (currentEntrant != null) {
                    intent.putExtra("entrant", currentEntrant);
                } else if (currentProfile != null) {
                    intent.putExtra("profile", currentProfile);
                }
                startActivity(intent);
            } else {
                // Organizer mode – just reload all events for now
                loadAllEvents();
            }
        });

        lotteryGuidelinesButton.setOnClickListener(v -> {
            if (!isOrganizerMode) {
                showLotteryGuidelines();
            } else {
                Toast.makeText(this, "Lottery guidelines apply to entrants.", Toast.LENGTH_SHORT).show();
            }
        });

        // Event history (Entrant only; hidden in organizer mode)
        eventHistoryButton.setOnClickListener(v ->
                Toast.makeText(this, "Event history not implemented yet.", Toast.LENGTH_SHORT).show()
        );

        // Bottom nav
        bottomNav.setOnItemSelectedListener(this::onNavItemSelected);

        // Role switch
        roleSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isOrganizerMode = isChecked;
            updateModeUI();
        });

        // Initial mode (Entrant by default)
        isOrganizerMode = roleSwitch.isChecked();
        updateModeUI();
    }

    // -----------------------
    // MODE HANDLING
    // -----------------------

    private void updateModeUI() {
        if (isOrganizerMode) {
            // Organizer mode
            headerLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.purple_500)); // <-- pick organizer color here

            eventHistoryButton.setVisibility(View.GONE);
            organizerLabel.setAlpha(1f);
            entrantLabel.setAlpha(0.7f);

            // Load organizer-style event list
            loadAllEvents();
        } else {
            // Entrant mode
            headerLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.blue_500)); // <-- original entrant blue

            eventHistoryButton.setVisibility(View.VISIBLE);
            organizerLabel.setAlpha(0.7f);
            entrantLabel.setAlpha(1f);

            // Load entrant-style event list
            loadEventsFromFirebase();
        }
    }

    // -----------------------
    // ENTRANT LOGIC (from EntrantViewMain)
    // -----------------------

    private void initializeProfileData() {
        // Try to get Entrant first
        currentEntrant = (Entrant) getIntent().getSerializableExtra("entrant");
        if (currentEntrant != null) {
            currentProfile = currentEntrant; // Entrant extends Profile
            Log.d(TAG, "Loaded Entrant: " + currentEntrant.getDisplayName());
            return;
        }

        // Try to get Profile
        currentProfile = (Profile) getIntent().getSerializableExtra("profile");
        if (currentProfile != null) {
            Log.d(TAG, "Loaded Profile: " + currentProfile.getDisplayName());

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
            Log.e(TAG, "No profile data found in intent");

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

                    Log.d(TAG, "Found " + querySnapshot.size() + " events (entrant view)");

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Log.d(TAG, "Doc " + doc.getId() + ": " + doc.getData());

                        Event event = createEventFromDocument(doc);
                        addEventToView(event);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load events: " + e.getMessage());
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
        event.setWaitingList((List<String>) doc.get("waitingList"));
        event.setSelected((List<String>) doc.get("selected"));
        event.setEnrolled((List<String>) doc.get("enrolled"));
        event.setCancelled((List<String>) doc.get("cancelled"));

        return event;
    }

    private void showEmptyState(String message) {
        eventListContainer.removeAllViews();
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
            Intent intent = new Intent(EntrantOrganizerViewMain.this, EventDetailActivity.class);
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
            // Reload according to mode
            updateModeUI();
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
            // You can later change this based on isOrganizerMode if needed
            intent.putExtra("userType", isOrganizerMode ? "Organizer" : "Entrant");
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

    // -----------------------
    // ORGANIZER LOGIC (from OrganizerViewMain)
    // -----------------------

    private String getCurrentOrganizerId() {
        // TODO: Replace with your actual organizer ID retrieval logic
        String organizerId = "org_default";

        // If you need to get from Intent extras:
        // if (getIntent() != null && getIntent().hasExtra("organizer_id")) {
        //     organizerId = getIntent().getStringExtra("organizer_id");
        // }

        return organizerId;
    }

    private void loadAllEvents() {
        showLoadingState();

        Log.d(TAG, "Loading ALL events from database (organizer view)");

        db.collection("events")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d(TAG, "Total events found: " + querySnapshot.size());

                    List<Event> events = new ArrayList<>();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        try {
                            Event event = new Event();
                            event.fromDocument(doc);
                            events.add(event);

                            String eventOrganizerId = doc.getString("organizerId");
                            Log.d(TAG, "Loaded event: " + event.getName() +
                                    " (Organizer: " + eventOrganizerId +
                                    ", Current Organizer: " + currentOrganizerId +
                                    ", Is Mine: " + currentOrganizerId.equals(eventOrganizerId) + ")");
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing event document: " + e.getMessage());
                        }
                    }

                    populateEventList(events);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load events: " + e.getMessage());
                    Toast.makeText(this, "Failed to load events: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    showEmptyStateOrganizer();
                });
    }

    private void populateEventList(@NonNull List<Event> events) {
        eventListContainer.removeAllViews();

        Log.d(TAG, "Populating event list with " + events.size() + " events (organizer view)");

        if (events.isEmpty()) {
            showEmptyStateOrganizer();
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);

        for (Event event : events) {
            View itemView = inflater.inflate(R.layout.item_organizer_event, eventListContainer, false);

            TextView eventTitle = itemView.findViewById(R.id.eventTitle);
            TextView eventWaiting = itemView.findViewById(R.id.eventWaiting);
            TextView eventSelected = itemView.findViewById(R.id.eventSelected);
            TextView eventCancelled = itemView.findViewById(R.id.eventCancelled);
            TextView eventStatus = itemView.findViewById(R.id.eventStatus);

            if (eventTitle != null) {
                String eventName = event.getName() != null ? event.getName() : "Unnamed Event";

                if (currentOrganizerId.equals(event.getOrganizerId())) {
                    eventName += " (My Event)";
                }
                eventTitle.setText(eventName);
            }

            if (eventWaiting != null) {
                int waitingCount = event.getWaitingList() != null ? event.getWaitingList().size() : 0;
                eventWaiting.setText(String.valueOf(waitingCount));
            }

            if (eventSelected != null) {
                int selectedCount = event.getSelected() != null ? event.getSelected().size() : 0;
                eventSelected.setText(String.valueOf(selectedCount));
            }

            if (eventCancelled != null) {
                int cancelledCount = event.getCancelled() != null ? event.getCancelled().size() : 0;
                eventCancelled.setText(String.valueOf(cancelledCount));
            }

            if (eventStatus != null) {
                String status = event.getRegistrationStatus();
                eventStatus.setText(status);
                applyStatusStyle(eventStatus, status);
            }

            itemView.setOnClickListener(v -> openEventDetails(event));

            eventListContainer.addView(itemView);
        }
    }

    private void applyStatusStyle(TextView statusView, String status) {
        int backgroundDrawable;
        int textColor;

        switch (status) {
            case "OPEN":
                backgroundDrawable = R.drawable.status_open_bg;
                textColor = R.color.green_500;
                break;
            case "FULL":
                backgroundDrawable = R.drawable.box_waiting;
                textColor = R.color.yellow_500;
                break;
            case "CLOSED":
                backgroundDrawable = R.drawable.status_closed_bg;
                textColor = R.color.red_500;
                break;
            case "CANCELLED":
                backgroundDrawable = R.drawable.box_cancelled;
                textColor = R.color.grey_500;
                break;
            default:
                backgroundDrawable = R.drawable.status_closed_bg;
                textColor = R.color.grey_500;
        }

        try {
            statusView.setBackgroundResource(backgroundDrawable);
            statusView.setTextColor(getResources().getColor(textColor));
        } catch (Exception e) {
            Log.e(TAG, "Error applying status style: " + e.getMessage());
        }
    }

    private void openEventDetails(Event event) {
        Intent intent = new Intent(this, OrganizerEventActivity.class);
        intent.putExtra("event_id", event.getId());
        intent.putExtra("event_name", event.getName());
        intent.putExtra("event_description", event.getDescription());
        intent.putExtra("event_capacity", event.getCapacity());
        intent.putExtra("current_participants", event.getCurrentParticipants());
        intent.putExtra("event_date", event.getEventDate() != null ? event.getEventDate().getTime() : 0);
        intent.putExtra("registration_open", event.getRegistrationOpen() != null ? event.getRegistrationOpen().getTime() : 0);
        intent.putExtra("registration_close", event.getRegistrationClose() != null ? event.getRegistrationClose().getTime() : 0);
        intent.putExtra("geo_required", event.isGeolocationRequired());
        intent.putExtra("event_location", event.getLocation());
        intent.putExtra("event_time_string", event.getTimeString());
        intent.putExtra("organizer_id", event.getOrganizerId());
        intent.putExtra("is_my_event", currentOrganizerId.equals(event.getOrganizerId()));
        startActivity(intent);
    }

    private void showLoadingState() {
        eventListContainer.removeAllViews();
        TextView loadingText = new TextView(this);
        loadingText.setText("Loading all events...");
        loadingText.setTextSize(16);
        loadingText.setPadding(32, 32, 32, 32);
        eventListContainer.addView(loadingText);
    }

    private void showEmptyStateOrganizer() {
        eventListContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(this);
        View emptyView = inflater.inflate(R.layout.empty_organizer_events, eventListContainer, false);

        MaterialButton createFirstEvent = emptyView.findViewById(R.id.btnCreateFirstEvent);
        if (createFirstEvent != null) {
            createFirstEvent.setOnClickListener(v -> {
                Intent toCreateEvent = new Intent(EntrantOrganizerViewMain.this, CreateEvent.class);
                startActivity(toCreateEvent);
            });
        }

        eventListContainer.addView(emptyView);
        Log.d(TAG, "Showing empty state - no events found in database (organizer view)");
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNav.setSelectedItemId(R.id.menu_events);
        // Refresh content based on current mode
        updateModeUI();
    }
}
