package com.example.gainly_flow;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.bumptech.glide.Glide;
import com.google.firebase.storage.StorageReference;

public class EventDetailActivity extends AppCompatActivity {

    private static final String TAG = "EventDetailActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private TextView tvEntrants, tvAvailable, titleEvent, eventStatus, locationEvent;
    private TextView eventDuration, registrationOpen, registrationClose, eventTime;
    private TextView eventDescription, eventLocationDetail, eventPrice, geolocationInfo;
    private ImageView backButton, qrCodeImage, eventPosterImage;
    private LinearLayout qrSection;
    private Button btnJoin, btnLeave, btnShareQr, btnViewWaitingList;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

    private Event event;
    private List<String> waitingList; // Changed to List<String> for entrant IDs
    private String eventId;
    private int eventCapacity;
    private String currentUserId;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Get current user ID
        currentUserId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        // Get event ID from intent
        eventId = getIntent().getStringExtra("event_id");
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Event ID not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupButtonListeners();
        loadEvent(eventId);
        loadEvent(eventId);
    }

    private void initializeViews() {
        tvEntrants = findViewById(R.id.tvEntrants);
        tvAvailable = findViewById(R.id.tvAvailable);
        titleEvent = findViewById(R.id.title_event);
        eventStatus = findViewById(R.id.event_status);
        locationEvent = findViewById(R.id.location_event);

        eventDuration = findViewById(R.id.event_duration);
        registrationOpen = findViewById(R.id.registration_open);
        registrationClose = findViewById(R.id.registration_close);
        eventTime = findViewById(R.id.event_time);

        eventDescription = findViewById(R.id.event_description);
        eventLocationDetail = findViewById(R.id.event_location_detail);
        eventPrice = findViewById(R.id.event_price);
        geolocationInfo = findViewById(R.id.geolocation_info);

        backButton = findViewById(R.id.back_button_event_detail);
        qrSection = findViewById(R.id.qr_section);
        qrCodeImage = findViewById(R.id.qr_code_image);
        eventPosterImage = findViewById(R.id.event_poster_image);

        btnJoin = findViewById(R.id.btnJoin);
        btnLeave = findViewById(R.id.btnLeave);
        btnShareQr = findViewById(R.id.btn_share_qr);
        btnViewWaitingList = findViewById(R.id.btnViewWaitingList);
    }

    public void loadEvent(String eventId) {
        if (eventId == null || eventId.isEmpty()) return;

        db.collection("events").document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        event = new Event();
                        event.fromDocument(documentSnapshot);

                        // Ensure waitingList is never null and is List<String>
                        if (event.getWaitingList() == null) {
                            event.setWaitingList(new ArrayList<>());
                        }

                        waitingList = event.getWaitingList();
                        updateUIWithEventData();
                        updateWaitingListUI();
                    } else {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading event: " + e.getMessage());
                    Toast.makeText(this, "Error loading event", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void updateUIWithEventData() {
        if (event == null) return;

        // Basic event info
        titleEvent.setText(event.getName() != null ? event.getName() : "Event Name");
        eventDescription.setText(event.getDescription() != null ? event.getDescription() : "No description");

        // Capacity and participants
        eventCapacity = event.getCapacity();
        int currentParticipants = event.getCurrentParticipants();
        int waitingCount = waitingList != null ? waitingList.size() : 0;

        tvEntrants.setText(String.valueOf(waitingCount));
        tvAvailable.setText(String.valueOf(eventCapacity - currentParticipants));

        // Location
        String location = event.getLocation();
        locationEvent.setText(location != null && !location.isEmpty() ? location : "Location not specified");
        eventLocationDetail.setText(locationEvent.getText());

        // Price
        double price = event.getPrice();
        eventPrice.setText(price == 0 ? "Free" : String.format("$%.2f", price));

        // Geolocation
        boolean geoRequired = event.isGeolocationRequired();
        geolocationInfo.setVisibility(geoRequired ? View.VISIBLE : View.GONE);
        if (geoRequired) {
            geolocationInfo.setText("📍 Geolocation required for check-in");
        }

        // Registration status
        updateRegistrationStatus();

        // Schedule information
        updateScheduleSection();

        // QR Code
        updateQrCode();

        // Poster
        updatePoster();
    }

    private void updatePoster() {
        if (event == null) {
            eventPosterImage.setVisibility(View.GONE);
            return;
        }

        String posterId = event.getPosterImageId();
        if (posterId == null || posterId.trim().isEmpty()) {
            eventPosterImage.setVisibility(View.GONE);
            return;
        }

        posterId = posterId.trim();
        eventPosterImage.setVisibility(View.VISIBLE);

        // Support both raw download URLs and storage IDs
        if (posterId.startsWith("http")) {
            Glide.with(this)
                    .load(posterId)
                    .centerCrop()
                    .placeholder(R.drawable.blue_gradient_bg)
                    .into(eventPosterImage);
            return;
        }

        ImageManager imageManager = new ImageManager();
        StorageReference posterRef = imageManager.getPosterReference(posterId);

        if (posterRef != null) {
            Glide.with(this)
                    .load(posterRef)
                    .centerCrop()
                    .placeholder(R.drawable.blue_gradient_bg)
                    .into(eventPosterImage);
        } else {
            eventPosterImage.setVisibility(View.GONE);
        }
    }

    private void updateRegistrationStatus() {
        if (event == null) return;

        String status = event.getRegistrationStatus();
        boolean isOpen = "OPEN".equals(status);

        eventStatus.setText(status);
        eventStatus.setTextColor(getResources().getColor(
                isOpen ? android.R.color.holo_green_dark : android.R.color.holo_red_dark
        ));

        btnJoin.setEnabled(isOpen);
        btnJoin.setAlpha(isOpen ? 1f : 0.5f);
    }

    private void updateScheduleSection() {
        if (event == null) return;

        // Event date
        Date eventDate = event.getEventDate();
        eventDuration.setText(eventDate != null ?
                "Event Date: " + dateFormat.format(eventDate) : "Event Date: Not specified");

        // Registration dates
        Date regOpen = event.getRegistrationOpen();
        Date regClose = event.getRegistrationClose();

        registrationOpen.setText(regOpen != null ?
                "Registration Opens: " + dateFormat.format(regOpen) + " at " + timeFormat.format(regOpen) :
                "Registration Opens: Not specified");

        registrationClose.setText(regClose != null ?
                "Registration Closes: " + dateFormat.format(regClose) + " at " + timeFormat.format(regClose) :
                "Registration Closes: Not specified");

        // Event time
        String eventTimeString = event.getTimeString();
        eventTime.setText(eventTimeString != null && !eventTimeString.isEmpty() ?
                "Event Time: " + eventTimeString : "Event Time: Not specified");
    }

    private void updateQrCode() {
        if (event == null) {
            qrSection.setVisibility(View.GONE);
            return;
        }

        String qrUrl = event.getQrUrl();
        if (qrUrl == null || qrUrl.trim().isEmpty()) {
            qrSection.setVisibility(View.GONE);
            return;
        }

        Bitmap qrBitmap = QRImage.bitmapFromUrl(qrUrl, 512);
        if (qrBitmap != null) {
            qrSection.setVisibility(View.VISIBLE);
            qrCodeImage.setImageBitmap(qrBitmap);
        } else {
            qrSection.setVisibility(View.GONE);
        }
    }

    private void updateWaitingListUI() {
        if (event == null || waitingList == null) {
            tvEntrants.setText("0");
            btnJoin.setVisibility(View.VISIBLE);
            btnLeave.setVisibility(View.GONE);
            return;
        }

        int count = waitingList.size();
        tvEntrants.setText(String.valueOf(count));

        boolean isOrganizer = isCurrentUserOrganizer();
        boolean isUserInList = waitingList.contains(currentUserId);

        if (isOrganizer) {
            // Organizers cannot join their own waiting lists; allow leave only if legacy data has them enrolled
            btnJoin.setVisibility(View.GONE);
            btnLeave.setVisibility(isUserInList ? View.VISIBLE : View.GONE);
        } else {
            btnJoin.setVisibility(isUserInList ? View.GONE : View.VISIBLE);
            btnLeave.setVisibility(isUserInList ? View.VISIBLE : View.GONE);
        }

        // Update available spots
        int currentParticipants = event.getCurrentParticipants();
        tvAvailable.setText(String.valueOf(event.getCapacity() - currentParticipants));
    }

    private void setupButtonListeners() {
        backButton.setOnClickListener(v -> onBackPressed());

        btnJoin.setOnClickListener(v -> joinWaitingList());
        btnLeave.setOnClickListener(v -> leaveWaitingList());

        btnShareQr.setOnClickListener(v -> {
            if (event != null && event.getQrUrl() != null) {
                shareQrCode();
            } else {
                Toast.makeText(this, "QR code not available", Toast.LENGTH_SHORT).show();
            }
        });

        btnViewWaitingList.setOnClickListener(v -> showWaitingListDialog());
    }

    private void joinWaitingList() {
        if (event == null || currentUserId == null) return;

        if (isCurrentUserOrganizer()) {
            Toast.makeText(this, "Organizers cannot join their own waiting list.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if user is already in waiting list
        if (waitingList.contains(currentUserId)) {
            Toast.makeText(this, "You are already on the waiting list.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if waiting list is full (using capacity as limit for simplicity)
        if (waitingList.size() >= event.getCapacity()) {
            Toast.makeText(this, "Waiting list is full.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if registration is open
        if (!"OPEN".equals(event.getRegistrationStatus())) {
            Toast.makeText(this, "Registration is closed for this event.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add user to waiting list in Firestore
        db.collection("events").document(eventId)
                .update("waitingList", FieldValue.arrayUnion(currentUserId))
                .addOnSuccessListener(aVoid -> {
                    // Update local list
                    waitingList.add(currentUserId);
                    event.setWaitingList(waitingList);

                    // Update UI
                    updateWaitingListUI();
                    Toast.makeText(this, "Successfully joined waiting list!", Toast.LENGTH_SHORT).show();

                    // Add event to user's history
                    addEventToUserHistory();

                    // Save location if geolocation is enabled for this event
                    if (event.isGeolocationRequired()) {
                        saveJoinLocation();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to join waiting list: " + e.getMessage());
                    Toast.makeText(this, "Failed to join waiting list.", Toast.LENGTH_SHORT).show();
                });
    }

    private void leaveWaitingList() {
        if (event == null || currentUserId == null || waitingList == null) {
            Toast.makeText(this, "Error leaving waiting list", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!waitingList.contains(currentUserId)) {
            Toast.makeText(this, "You are not on the waiting list.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Remove user from waiting list in Firestore
        db.collection("events").document(eventId)
                .update("waitingList", FieldValue.arrayRemove(currentUserId))
                .addOnSuccessListener(aVoid -> {
                    // Update local list
                    waitingList.remove(currentUserId);
                    event.setWaitingList(waitingList);

                    // Update UI
                    updateWaitingListUI();
                    Toast.makeText(this, "Successfully left waiting list.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to leave waiting list: " + e.getMessage());
                    Toast.makeText(this, "Failed to leave waiting list.", Toast.LENGTH_SHORT).show();
                });
    }

    private void addEventToUserHistory() {
        if (currentUserId == null || eventId == null) return;

        // Add event to user's event history
        db.collection("profiles").document(currentUserId)
                .update("eventHistory", FieldValue.arrayUnion(eventId))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update user history: " + e.getMessage());
                });
    }

    private void showWaitingListDialog() {
        if (waitingList == null || waitingList.isEmpty()) {
            Toast.makeText(this, "No entrants in waiting list.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Load entrant details for display
        loadWaitingListEntrantsDetails(waitingList);
    }

    private void loadWaitingListEntrantsDetails(List<String> entrantIds) {
        if (entrantIds.isEmpty()) return;

        StringBuilder message = new StringBuilder();
        final int[] loadedCount = {0};

        for (int i = 0; i < entrantIds.size(); i++) {
            final int position = i;
            String entrantId = entrantIds.get(i);

            db.collection("profiles").document(entrantId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        String name;
                        if (documentSnapshot.exists()) {
                            Profile profile = documentSnapshot.toObject(Profile.class);
                            name = profile != null && profile.getDisplayName() != null ?
                                    profile.getDisplayName() : "User " + entrantId;
                        } else {
                            name = "User " + entrantId;
                        }

                        message.append(position + 1).append(". ").append(name).append("\n");
                        loadedCount[0]++;

                        // Show dialog when all names are loaded
                        if (loadedCount[0] == entrantIds.size()) {
                            showWaitingListDialog(message.toString());
                        }
                    })
                    .addOnFailureListener(e -> {
                        // If we can't load the name, just show the ID
                        message.append(position + 1).append(". User ").append(entrantId).append("\n");
                        loadedCount[0]++;

                        if (loadedCount[0] == entrantIds.size()) {
                            showWaitingListDialog(message.toString());
                        }
                    });
        }
    }

    private void showWaitingListDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Waiting List (" + waitingList.size() + " Entrants)")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void shareQrCode() {
        // TODO: Implement QR code sharing
        // This would use Android's share intent to share the QR code image or event link
        Toast.makeText(this, "Share QR feature coming soon", Toast.LENGTH_SHORT).show();
    }

    /**
     * Saves the user's current location when they join the waiting list.
     * This location data will be displayed on a map for the organizer.
     * Only called if geolocation is enabled for this event.
     */
    private void saveJoinLocation() {
        // Fetch user's display name from their profile
        db.collection("profiles").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String entrantName = "Unknown User";
                    if (documentSnapshot.exists()) {
                        Profile profile = documentSnapshot.toObject(Profile.class);
                        if (profile != null && profile.getDisplayName() != null) {
                            entrantName = profile.getDisplayName();
                        }
                    }

                    // Save location using LocationHelper
                    LocationHelper.saveJoinLocation(
                            EventDetailActivity.this,
                            eventId,
                            currentUserId,
                            entrantName
                    );
                })
                .addOnFailureListener(e -> {
                    // Even if we can't get the name, save the location with user ID
                    LocationHelper.saveJoinLocation(
                            EventDetailActivity.this,
                            eventId,
                            currentUserId,
                            "User " + currentUserId
                    );
                });
    }

    private boolean isCurrentUserOrganizer() {
        return event != null &&
                event.getOrganizerId() != null &&
                event.getOrganizerId().equals(currentUserId);
    }
}
