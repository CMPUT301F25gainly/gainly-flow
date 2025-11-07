package com.example.gainly_flow;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EventDetailActivity extends AppCompatActivity {

    private TextView tvEntrants, tvAvailable, titleEvent, eventStatus, locationEvent;
    private TextView eventDescription, eventLocationDetail, eventPrice, geolocationInfo;
    private TextView eventDuration, registrationOpen, registrationClose, eventTime;
    private Button btnJoin, btnLeave, btnShareQr;
    private ImageView backButton, qrCodeImage;
    private LinearLayout qrSection;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        // Initialize all views
        initializeViews();

        // Get event data from intent
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            displayEventData(extras);
        } else {
            Toast.makeText(this, "Event data not available", Toast.LENGTH_SHORT).show();
            finish();
        }

        setupButtonListeners();
    }

    private void initializeViews() {
        // Main info views
        tvEntrants = findViewById(R.id.tvEntrants);
        tvAvailable = findViewById(R.id.tvAvailable);
        titleEvent = findViewById(R.id.title_event);
        eventStatus = findViewById(R.id.event_status);
        locationEvent = findViewById(R.id.location_event);

        // Schedule views
        eventDuration = findViewById(R.id.event_duration);
        registrationOpen = findViewById(R.id.registration_open);
        registrationClose = findViewById(R.id.registration_close);
        eventTime = findViewById(R.id.event_time);

        // About section views
        eventDescription = findViewById(R.id.event_description);
        eventLocationDetail = findViewById(R.id.event_location_detail);
        eventPrice = findViewById(R.id.event_price);
        geolocationInfo = findViewById(R.id.geolocation_info);

        // Button views
        btnJoin = findViewById(R.id.btnJoin);
        btnLeave = findViewById(R.id.btnLeave);
        backButton = findViewById(R.id.back_button_event_detail);

        // QR section views
        qrSection = findViewById(R.id.qr_section);
        qrCodeImage = findViewById(R.id.qr_code_image);
        btnShareQr = findViewById(R.id.btn_share_qr);
    }

    private void displayEventData(Bundle extras) {
        String eventId = extras.getString("event_id");
        String eventName = extras.getString("event_name");
        String eventDesc = extras.getString("event_description");
        int eventCapacity = extras.getInt("event_capacity", 0);
        long eventDateMillis = extras.getLong("event_date", 0);
        long regOpenMillis = extras.getLong("registration_open", 0);
        long regCloseMillis = extras.getLong("registration_close", 0);
        boolean geoRequired = extras.getBoolean("geo_required", false);

        // Set basic event info
        titleEvent.setText(eventName != null ? eventName : "Event Name");
        tvAvailable.setText(String.valueOf(eventCapacity));

        // Set description
        if (eventDescription != null && eventDesc != null) {
            eventDescription.setText(eventDesc);
        }

        // Set location (you might want to add this to your Event class)
        locationEvent.setText("Community Recreation Center");
        eventLocationDetail.setText("🏊 Indoor Pool, Community Rec Center");

        // Set geolocation info
        if (geoRequired) {
            geolocationInfo.setVisibility(View.VISIBLE);
        } else {
            geolocationInfo.setVisibility(View.GONE);
        }

        // Set registration status
        updateRegistrationStatus(regOpenMillis, regCloseMillis);

        // Update schedule section
        updateScheduleSection(eventDateMillis, regOpenMillis, regCloseMillis);

        // Update QR code if available
        updateQrCode(eventId);
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

    private void updateScheduleSection(long eventDateMillis, long regOpenMillis, long regCloseMillis) {
        // Event date
        if (eventDateMillis > 0) {
            String eventDate = dateFormat.format(new Date(eventDateMillis));
            eventDuration.setText("Event Date: " + eventDate);
        } else {
            eventDuration.setText("Event Date: Not specified");
        }

        // Registration open
        if (regOpenMillis > 0) {
            String openDate = dateFormat.format(new Date(regOpenMillis));
            registrationOpen.setText("Registration Opens: " + openDate);
        } else {
            registrationOpen.setText("Registration Opens: Not specified");
        }

        // Registration close
        if (regCloseMillis > 0) {
            String closeDate = dateFormat.format(new Date(regCloseMillis));
            registrationClose.setText("Registration Closes: " + closeDate);
        } else {
            registrationClose.setText("Registration Closes: Not specified");
        }

        // Event time (you might want to add this to your Event class)
        eventTime.setText("Event Time: 6:00 PM - 7:00 PM");
    }

    private void updateQrCode(String eventId) {
        if (eventId != null && !eventId.isEmpty()) {
            // Show QR section if you have QR codes
            // qrSection.setVisibility(View.VISIBLE);
            // Load QR code image here
        }
    }

    private void setupButtonListeners() {
        backButton.setOnClickListener(v -> {
            onBackPressed();
        });

        btnJoin.setOnClickListener(v -> {
            String eventName = titleEvent.getText().toString();
            Toast.makeText(this, "Joined waiting list for " + eventName + "!", Toast.LENGTH_SHORT).show();
            // Add your join logic here
        });

        btnLeave.setOnClickListener(v -> {
            String eventName = titleEvent.getText().toString();
            Toast.makeText(this, "Left waiting list for " + eventName + "!", Toast.LENGTH_SHORT).show();
            // Add your leave logic here
        });

        btnShareQr.setOnClickListener(v -> {
            Toast.makeText(this, "Share QR functionality", Toast.LENGTH_SHORT).show();
            // Add your share QR logic here
        });
    }
}