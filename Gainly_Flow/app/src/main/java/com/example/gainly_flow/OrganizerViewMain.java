package com.example.gainly_flow;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class OrganizerViewMain extends AppCompatActivity {

    private static final String TAG = "OrganizerViewMain";

    private LinearLayout eventListContainer;
    private FirebaseFirestore db;
    private ImageButton backButton;
    private String currentOrganizerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_home);

        // Initialize views
        initializeViews();

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Get current organizer ID
        currentOrganizerId = getCurrentOrganizerId();
        Log.d(TAG, "Current Organizer ID: " + currentOrganizerId);

        // Setup button listeners
        setupButtonListeners();

        // Load ALL events (not just current organizer's events)
        loadAllEvents();
    }

    private void initializeViews() {
        eventListContainer = findViewById(R.id.eventListContainer);
        backButton = findViewById(R.id.backButton_organizer);
    }

    private String getCurrentOrganizerId() {
        // TODO: Replace with your actual organizer ID retrieval logic
        String organizerId = "org_default"; // Change this to match your actual organizer IDs

        // If you need to get from Intent extras:
        // if (getIntent() != null && getIntent().hasExtra("organizer_id")) {
        //     organizerId = getIntent().getStringExtra("organizer_id");
        // }

        return organizerId;
    }

    private void setupButtonListeners() {
        backButton.setOnClickListener(v -> onBackPressed());

        Button createEventButton = findViewById(R.id.createEventButton);
        createEventButton.setOnClickListener(v -> {
            Intent toCreateEvent = new Intent(OrganizerViewMain.this, CreateEvent.class);
            startActivity(toCreateEvent);
        });
    }

    private void loadAllEvents() {
        // Show loading state
        showLoadingState();

        Log.d(TAG, "Loading ALL events from database");

        // Query ALL events without filtering by organizerId
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

                            // Log event details including which organizer created it
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
                    showEmptyState();
                });
    }

    private void populateEventList(@NonNull List<Event> events) {
        eventListContainer.removeAllViews();

        Log.d(TAG, "Populating event list with " + events.size() + " events");

        if (events.isEmpty()) {
            showEmptyState();
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);

        for (Event event : events) {
            View itemView = inflater.inflate(R.layout.item_organizer_event, eventListContainer, false);

            // Find views in the item layout
            TextView eventTitle = itemView.findViewById(R.id.eventTitle);
            TextView eventWaiting = itemView.findViewById(R.id.eventWaiting);
            TextView eventSelected = itemView.findViewById(R.id.eventSelected);
            TextView eventCancelled = itemView.findViewById(R.id.eventCancelled);
            TextView eventStatus = itemView.findViewById(R.id.eventStatus);

            // Set event data
            if (eventTitle != null) {
                String eventName = event.getName() != null ? event.getName() : "Unnamed Event";

                // Add indicator if this event belongs to current organizer
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

            // Make the item clickable
            itemView.setOnClickListener(v -> openEventDetails(event));

            // Optional: Change background color for events that don't belong to current organizer
            /*if (!currentOrganizerId.equals(event.getOrganizerId())) {
                itemView.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
            }*/

            eventListContainer.addView(itemView);
        }
    }

    private void applyStatusStyle(TextView statusView, String status) {
        int backgroundColor;
        int textColor;

        switch (status) {
            case "OPEN":
                backgroundColor = R.drawable.status_open_bg;
                textColor = R.color.green_500;
                break;
            case "FULL":
                backgroundColor = R.drawable.box_waiting;
                textColor = R.color.yellow_500;
                break;
            case "CLOSED":
                backgroundColor = R.drawable.status_closed_bg;
                textColor = R.color.red_500;
                break;
            case "CANCELLED":
                backgroundColor = R.drawable.box_cancelled;
                textColor = R.color.grey_500;
                break;
            default:
                backgroundColor = R.drawable.status_closed_bg;
                textColor = R.color.grey_500;
        }

        try {
            statusView.setBackgroundResource(backgroundColor);
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

    private void showEmptyState() {
        eventListContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(this);
        View emptyView = inflater.inflate(R.layout.empty_organizer_events, eventListContainer, false);

        Button createFirstEvent = emptyView.findViewById(R.id.btnCreateFirstEvent);
        if (createFirstEvent != null) {
            createFirstEvent.setOnClickListener(v -> {
                Intent toCreateEvent = new Intent(OrganizerViewMain.this, CreateEvent.class);
                startActivity(toCreateEvent);
            });
        }

        eventListContainer.addView(emptyView);
        Log.d(TAG, "Showing empty state - no events found in database");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh all events when returning from other activities
        loadAllEvents();
    }
}