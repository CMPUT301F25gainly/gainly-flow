package com.example.gainly_flow;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Entrant event history screen.
 * Shows events related to this entrant and filters by
 * All / Waiting / Won / Lost.
 */
public class EntrantEventHistory extends AppCompatActivity {

    private static final String TAG = "EntrantEventHistory";

    // UI
    private BottomNavigationView bottomNav;
    private LinearLayout eventListContainer;
    private MaterialButtonToggleGroup historyFilterGroup;
    private MaterialButton filterAllButton, filterWaitingButton, filterWonButton, filterLostButton;
    private EditText searchInput; // not wired yet, but ready for later

    // Date format reused from entrant view
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    // Profile / entrant
    private Profile currentProfile;
    private Entrant currentEntrant;

    // Firestore
    private FirebaseFirestore db;

    // Filter mode
    private enum HistoryFilter {
        ALL, WAITING, WON, LOST
    }

    private HistoryFilter currentFilter = HistoryFilter.ALL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrant_event_history);

        // Init Firestore
        db = FirebaseFirestore.getInstance();

        // Load profile / entrant from intent (same logic style as EntrantOrganizerViewMain)
        initializeProfileData();

        // Bind views
        bottomNav = findViewById(R.id.bottomNav);
        eventListContainer = findViewById(R.id.eventListContainer);
        historyFilterGroup = findViewById(R.id.historyFilterGroup);
        filterAllButton = findViewById(R.id.filterAllButton);
        filterWaitingButton = findViewById(R.id.filterWaitingButton);
        filterWonButton = findViewById(R.id.filterWonButton);
        filterLostButton = findViewById(R.id.filterLostButton);
        searchInput = findViewById(R.id.searchInput);

        // Bottom nav behavior
        bottomNav.setOnItemSelectedListener(this::onNavItemSelected);

        // Toggle group for All / Waiting / Won / Lost
        historyFilterGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }

            if (checkedId == R.id.filterAllButton) {
                currentFilter = HistoryFilter.ALL;
            } else if (checkedId == R.id.filterWaitingButton) {
                currentFilter = HistoryFilter.WAITING;
            } else if (checkedId == R.id.filterWonButton) {
                currentFilter = HistoryFilter.WON;
            } else if (checkedId == R.id.filterLostButton) {
                currentFilter = HistoryFilter.LOST;
            }

            loadHistoryEvents();
        });

        // Default selection already set in XML, but make sure
        historyFilterGroup.check(R.id.filterAllButton);

        // Initial load
        loadHistoryEvents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNav.setSelectedItemId(R.id.menu_events);
        loadHistoryEvents();
    }

    // ----------------------------------------------------
    // Profile / Entrant initialization (same pattern)
    // ----------------------------------------------------

    private void initializeProfileData() {
        // Try Entrant first
        currentEntrant = (Entrant) getIntent().getSerializableExtra("entrant");
        if (currentEntrant != null) {
            currentProfile = currentEntrant;
            Log.d(TAG, "Loaded Entrant: " + currentEntrant.getDisplayName());
            return;
        }

        // Fallback to Profile
        currentProfile = (Profile) getIntent().getSerializableExtra("profile");
        if (currentProfile != null) {
            Log.d(TAG, "Loaded Profile: " + currentProfile.getDisplayName());

            if (currentProfile instanceof Entrant) {
                currentEntrant = (Entrant) currentProfile;
            } else if ("Entrant".equals(currentProfile.getRole())) {
                currentEntrant = convertProfileToEntrant(currentProfile);
            }
        } else {
            Toast.makeText(this, "Profile data not available. Please log in again.", Toast.LENGTH_LONG).show();
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

    // ----------------------------------------------------
    // Loading and filtering history
    // ----------------------------------------------------

    private void loadHistoryEvents() {
        if (db == null) {
            db = FirebaseFirestore.getInstance();
        }

        db.collection("events")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    eventListContainer.removeAllViews();

                    if (querySnapshot.isEmpty()) {
                        showEmptyState("No events found.");
                        return;
                    }

                    String entrantId = getEntrantId();
                    String entrantDeviceId = getEntrantDeviceId();

                    List<Event> results = new ArrayList<>();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Event event = createEventFromDocument(doc);

                        List<String> waitingList = event.getWaitingList();
                        List<String> selectedList = event.getSelected();
                        List<String> enrolledList = event.getEnrolled();
                        List<String> cancelledList = event.getCancelled();

                        boolean isWaiting = containsEntrant(waitingList, entrantId, entrantDeviceId);
                        boolean isWon = containsEntrant(selectedList, entrantId, entrantDeviceId)
                                || containsEntrant(enrolledList, entrantId, entrantDeviceId);
                        boolean isLost = containsEntrant(cancelledList, entrantId, entrantDeviceId);

                        boolean involved = isWaiting || isWon || isLost;

                        if (!involved) {
                            // This event has nothing to do with this entrant
                            continue;
                        }

                        boolean include = false;
                        switch (currentFilter) {
                            case ALL:
                                include = true;
                                break;
                            case WAITING:
                                include = isWaiting;
                                break;
                            case WON:
                                include = isWon;
                                break;
                            case LOST:
                                include = isLost;
                                break;
                        }

                        if (include) {
                            results.add(event);
                        }
                    }

                    if (results.isEmpty()) {
                        showEmptyState("No events match this filter.");
                    } else {
                        for (Event e : results) {
                            addEventToView(e);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load history: " + e.getMessage());
                    Toast.makeText(this, "Failed to load history: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    showEmptyState("Failed to load events.");
                });
    }

    private String getEntrantId() {
        if (currentEntrant != null && currentEntrant.getId() != null) {
            return currentEntrant.getId();
        }
        if (currentProfile != null && currentProfile.getId() != null) {
            return currentProfile.getId();
        }
        return null;
    }

    private String getEntrantDeviceId() {
        if (currentEntrant != null && currentEntrant.getDeviceId() != null) {
            return currentEntrant.getDeviceId();
        }
        if (currentProfile != null && currentProfile.getDeviceId() != null) {
            return currentProfile.getDeviceId();
        }
        return null;
    }

    private boolean containsEntrant(List<String> list, String id, String deviceId) {
        if (list == null || list.isEmpty()) return false;
        if (id != null && list.contains(id)) return true;
        if (deviceId != null && list.contains(deviceId)) return true;
        return false;
    }

    // ----------------------------------------------------
    // Helpers copied from entrant list
    // ----------------------------------------------------

    private Event createEventFromDocument(DocumentSnapshot doc) {
        Event event = new Event(doc.getId());

        event.setName(doc.getString("name"));
        event.setDescription(doc.getString("description"));
        event.setPosterImageId(doc.getString("posterImageId"));
        event.setLocation(doc.getString("location"));
        event.setTimeString(doc.getString("timeString"));

        Long capLong = doc.getLong("capacity");
        event.setCapacity(capLong != null ? capLong.intValue() : 20);

        Long currentLong = doc.getLong("currentParticipants");
        event.setCurrentParticipants(currentLong != null ? currentLong.intValue() : 0);

        Boolean geo = doc.getBoolean("geolocationRequired");
        event.setGeolocationRequired(Boolean.TRUE.equals(geo));

        Object eventDateObj = doc.get("eventDate");
        if (eventDateObj instanceof Date) {
            event.setEventDate((Date) eventDateObj);
        } else if (eventDateObj instanceof Long) {
            event.setEventDate(new Date((Long) eventDateObj));
        }

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

        Double price = doc.getDouble("price");
        if (price != null) {
            event.setPrice(price);
        }

        String category = doc.getString("category");
        if (category != null) {
            try {
                event.setCategory(Event.Category.valueOf(category));
            } catch (IllegalArgumentException e) {
                event.setCategory(Event.Category.ALL);
            }
        }

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
            Intent intent = new Intent(EntrantEventHistory.this, EventDetailActivity.class);
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

    // ----------------------------------------------------
    // Bottom navigation
    // ----------------------------------------------------

    private boolean onNavItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Intent intent;

        if (id == R.id.menu_events) {
            // Back to main entrant/organizer events screen
            intent = new Intent(this, EntrantOrganizerViewMain.class);
            if (currentEntrant != null) {
                intent.putExtra("entrant", currentEntrant);
            } else if (currentProfile != null) {
                intent.putExtra("profile", currentProfile);
            }
            startActivity(intent);
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
                intent.putExtra("userType", "Entrant");
            } else if (currentProfile != null) {
                intent.putExtra("profile", currentProfile);
                intent.putExtra("userType", currentProfile.getRole());
            }
            startActivity(intent);
        }
        return true;
    }
}
