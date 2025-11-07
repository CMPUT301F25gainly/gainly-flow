package com.example.gainly_flow;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.provider.Settings;

public class EventDetailActivity extends AppCompatActivity {

    private TextView tvEntrants, tvAvailable, titleEvent, eventStatus, locationEvent;
    private TextView eventDescription, eventLocationDetail, eventPrice, geolocationInfo;
    private TextView eventDuration, registrationOpen, registrationClose, eventTime;
    private ImageView backButton, qrCodeImage;
    private LinearLayout qrSection;
    private Button btnJoin, btnLeave, btnShareQr, btnViewWaitingList;


    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

    private WaitingList waitingList;
    private String eventId;
    private int eventCapacity;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        initializeViews();

        // Get current user ID (assuming Firebase Auth is used)
        currentUserId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        // Get event data from intent
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            displayEventData(extras);
        } else {
            Toast.makeText(this, "Event data not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Load waiting list data
        loadWaitingList();

        setupButtonListeners();
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

        btnJoin = findViewById(R.id.btnJoin);
        btnLeave = findViewById(R.id.btnLeave);
        backButton = findViewById(R.id.back_button_event_detail);

        qrSection = findViewById(R.id.qr_section);
        qrCodeImage = findViewById(R.id.qr_code_image);
        btnShareQr = findViewById(R.id.btn_share_qr);
        btnViewWaitingList = findViewById(R.id.btnViewWaitingList);

    }

    private void displayEventData(Bundle extras) {
        eventId = extras.getString("event_id");
        String eventName = extras.getString("event_name");
        String eventDesc = extras.getString("event_description");
        eventCapacity = extras.getInt("event_capacity", 0);
        long eventDateMillis = extras.getLong("event_date", 0);
        long regOpenMillis = extras.getLong("registration_open", 0);
        long regCloseMillis = extras.getLong("registration_close", 0);
        boolean geoRequired = extras.getBoolean("geo_required", false);
        String eventLocation = extras.getString("event_location");
        String eventTimeString = extras.getString("event_time_string");

        titleEvent.setText(eventName != null ? eventName : "Event Name");
        tvAvailable.setText(String.valueOf(eventCapacity));

        if (eventDescription != null && eventDesc != null) {
            eventDescription.setText(eventDesc);
        }

        if (eventLocation != null && !eventLocation.isEmpty()) {
            locationEvent.setText(eventLocation);
            eventLocationDetail.setText(eventLocation);
        } else {
            locationEvent.setText("Location not specified");
            eventLocationDetail.setText("Location not specified");
        }

        if (geoRequired) {
            geolocationInfo.setVisibility(View.VISIBLE);
            geolocationInfo.setText("📍 Geolocation required for check-in");
        } else {
            geolocationInfo.setVisibility(View.GONE);
        }

        updateRegistrationStatus(regOpenMillis, regCloseMillis);
        updateScheduleSection(eventDateMillis, regOpenMillis, regCloseMillis, eventTimeString);
        updateQrCode(eventId);
    }

    private void loadWaitingList() {
        if (eventId == null) return;

        waitingList = new WaitingList(eventId);
        waitingList.load(loaded -> {
            updateWaitingListUI();
        });
    }

    private void updateWaitingListUI() {
        if (waitingList == null) return;

        int count = waitingList.getCount();
        tvEntrants.setText(String.valueOf(count));

        boolean isUserInList = waitingList.getEntrants().contains(currentUserId);

        // Default to show Join button if list hasn't loaded yet
        btnJoin.setVisibility(isUserInList ? View.GONE : View.VISIBLE);
        btnLeave.setVisibility(isUserInList ? View.VISIBLE : View.GONE);
    }


    private void updateRegistrationStatus(long regOpenMillis, long regCloseMillis) {
        Date now = new Date();
        Date regOpen = regOpenMillis > 0 ? new Date(regOpenMillis) : null;
        Date regClose = regCloseMillis > 0 ? new Date(regCloseMillis) : null;

        boolean isOpen = false;
        if (regOpen != null && regClose != null) {
            isOpen = now.after(regOpen) && now.before(regClose);
        }

        if (isOpen) {
            eventStatus.setText("OPEN");
            eventStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            btnJoin.setEnabled(true);
            btnJoin.setAlpha(1.0f);
        } else {
            eventStatus.setText("CLOSED");
            eventStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            btnJoin.setEnabled(false);
            btnJoin.setAlpha(0.5f);
        }
    }

    private void updateScheduleSection(long eventDateMillis, long regOpenMillis, long regCloseMillis, String eventTimeString) {
        if (eventDateMillis > 0) {
            eventDuration.setText("Event Date: " + dateFormat.format(new Date(eventDateMillis)));
        } else {
            eventDuration.setText("Event Date: Not specified");
        }

        if (regOpenMillis > 0) {
            String openDate = dateFormat.format(new Date(regOpenMillis));
            String openTime = timeFormat.format(new Date(regOpenMillis));
            registrationOpen.setText("Registration Opens: " + openDate + " at " + openTime);
        } else {
            registrationOpen.setText("Registration Opens: Not specified");
        }

        if (regCloseMillis > 0) {
            String closeDate = dateFormat.format(new Date(regCloseMillis));
            String closeTime = timeFormat.format(new Date(regCloseMillis));
            registrationClose.setText("Registration Closes: " + closeDate + " at " + closeTime);
        } else {
            registrationClose.setText("Registration Closes: Not specified");
        }

        if (eventTimeString != null && !eventTimeString.isEmpty()) {
            eventTime.setText("Event Time: " + eventTimeString);
        } else {
            eventTime.setText("Event Time: Not specified");
        }
    }

    private void updateQrCode(String eventId) {
        if (eventId != null && !eventId.isEmpty()) {
            // QR logic if needed
        }
    }

    private void setupButtonListeners() {
        backButton.setOnClickListener(v -> onBackPressed());

        btnJoin.setOnClickListener(v -> {
            if (waitingList != null) {
                waitingList.addEntrant(currentUserId, eventCapacity);
                Toast.makeText(this, "Joined waiting list!", Toast.LENGTH_SHORT).show();
                updateWaitingListUI();
            }
        });

        btnLeave.setOnClickListener(v -> {
            if (waitingList != null) {
                waitingList.removeEntrant(currentUserId);
                Toast.makeText(this, "Left waiting list!", Toast.LENGTH_SHORT).show();
                updateWaitingListUI();
            }
        });

        btnShareQr.setOnClickListener(v -> {
            Toast.makeText(this, "Share QR feature coming soon", Toast.LENGTH_SHORT).show();
        });

        btnViewWaitingList.setOnClickListener(v -> {
            String eventId = getIntent().getStringExtra("event_id");
            if (eventId == null || eventId.isEmpty()) {
                Toast.makeText(this, "Event ID not found.", Toast.LENGTH_SHORT).show();
                return;
            }

            WaitingList waitingList = new WaitingList(eventId);
            waitingList.load(loadedList -> {
                java.util.List<String> entrants = loadedList.getEntrants();
                if (entrants.isEmpty()) {
                    runOnUiThread(() ->
                            Toast.makeText(this, "No entrants in waiting list.", Toast.LENGTH_SHORT).show());
                } else {
                    // Build readable list for dialog
                    StringBuilder message = new StringBuilder();
                    for (int i = 0; i < entrants.size(); i++) {
                        message.append(i + 1).append(". ").append(entrants.get(i)).append("\n");
                    }

                    runOnUiThread(() -> {
                        new androidx.appcompat.app.AlertDialog.Builder(this)
                                .setTitle("Waiting List (" + entrants.size() + " Entrants)")
                                .setMessage(message.toString())
                                .setPositiveButton("OK", null)
                                .show();
                    });
                }
            });
        });

    }
}
