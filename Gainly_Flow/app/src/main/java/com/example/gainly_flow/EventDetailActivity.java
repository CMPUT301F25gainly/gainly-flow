package com.example.gainly_flow;

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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import com.google.firebase.firestore.FirebaseFirestore;


public class EventDetailActivity extends AppCompatActivity {

    private TextView tvEntrants, tvAvailable, titleEvent, eventStatus, locationEvent;
    private TextView eventDuration, registrationOpen, registrationClose, eventTime;
    private TextView eventDescription, eventLocationDetail, eventPrice, geolocationInfo;
    private ImageView backButton, qrCodeImage;
    private LinearLayout qrSection;
    private Button btnJoin, btnLeave, btnShareQr, btnViewWaitingList;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

    private Event event;
    private List<Entrant> waitingList;
    private String eventId;
    private int eventCapacity;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        eventId = getIntent().getStringExtra("event_id");
        loadEvent(eventId);


        initializeViews();

        currentUserId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        if (getIntent() == null || getIntent().getExtras() == null) {
            Toast.makeText(this, "Event data not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        displayEventData(getIntent().getExtras());
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

        backButton = findViewById(R.id.back_button_event_detail);
        qrSection = findViewById(R.id.qr_section);
        qrCodeImage = findViewById(R.id.qr_code_image);

        btnJoin = findViewById(R.id.btnJoin);
        btnLeave = findViewById(R.id.btnLeave);
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
        eventDescription.setText(eventDesc != null ? eventDesc : "No description");
        locationEvent.setText(eventLocation != null && !eventLocation.isEmpty() ? eventLocation : "Location not specified");
        eventLocationDetail.setText(locationEvent.getText());

        geolocationInfo.setVisibility(geoRequired ? View.VISIBLE : View.GONE);
        if (geoRequired) geolocationInfo.setText("📍 Geolocation required for check-in");

        updateRegistrationStatus(regOpenMillis, regCloseMillis);
        updateScheduleSection(eventDateMillis, regOpenMillis, regCloseMillis, eventTimeString);
        updateQrCode(eventId);
    }

    public void loadEvent(String eventId) {
        if (eventId == null || eventId.isEmpty()) return;

        Database.get("events", eventId, doc -> {
            if (doc.exists()) {
                event = new Event();
                // Let Event handle Firestore → POJO mapping, including waitingList
                event.fromDocument(doc);

                // Ensure waitingList is never null
                if (event.getWaitingList() == null) {
                    event.setWaitingList(new ArrayList<Entrant>());
                }

                int waitingCount = event.getWaitingList().size();
                tvAvailable.setText(String.valueOf(event.getCapacity() - waitingCount));
                updateWaitingListUI();
            }
        });
    }




    private void updateWaitingListUI() {
        if (event == null) return;

        List<Entrant> list = event.getWaitingList();
        if (list == null) {
            tvEntrants.setText("0");
            btnJoin.setVisibility(View.VISIBLE);
            btnLeave.setVisibility(View.GONE);
            return;
        }

        int count = list.size();
        tvEntrants.setText(String.valueOf(count));

        boolean isUserInList = false;
        for (Entrant e : list) {
            if (currentUserId != null && currentUserId.equals(e.getId())) {
                isUserInList = true;
                break;
            }
        }

        btnJoin.setVisibility(isUserInList ? View.GONE : View.VISIBLE);
        btnLeave.setVisibility(isUserInList ? View.VISIBLE : View.GONE);
    }



    private void updateRegistrationStatus(long regOpenMillis, long regCloseMillis) {
        Date now = new Date();
        Date regOpen = regOpenMillis > 0 ? new Date(regOpenMillis) : null;
        Date regClose = regCloseMillis > 0 ? new Date(regCloseMillis) : null;

        boolean isOpen = regOpen != null && regClose != null && now.after(regOpen) && now.before(regClose);

        eventStatus.setText(isOpen ? "OPEN" : "CLOSED");
        eventStatus.setTextColor(getResources().getColor(isOpen ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
        btnJoin.setEnabled(isOpen);
        btnJoin.setAlpha(isOpen ? 1f : 0.5f);
    }

    private void updateScheduleSection(long eventDateMillis, long regOpenMillis, long regCloseMillis, String eventTimeString) {
        eventDuration.setText(eventDateMillis > 0 ? "Event Date: " + dateFormat.format(new Date(eventDateMillis)) : "Event Date: Not specified");
        registrationOpen.setText(regOpenMillis > 0 ? "Registration Opens: " + dateFormat.format(new Date(regOpenMillis)) + " at " + timeFormat.format(new Date(regOpenMillis)) : "Registration Opens: Not specified");
        registrationClose.setText(regCloseMillis > 0 ? "Registration Closes: " + dateFormat.format(new Date(regCloseMillis)) + " at " + timeFormat.format(new Date(regCloseMillis)) : "Registration Closes: Not specified");
        eventTime.setText(eventTimeString != null && !eventTimeString.isEmpty() ? "Event Time: " + eventTimeString : "Event Time: Not specified");
    }

    private void updateQrCode(String eventId) {
        // Placeholder; integrate QR logic if needed
    }

    private void setupButtonListeners() {
        backButton.setOnClickListener(v -> onBackPressed());

        btnJoin.setOnClickListener(v -> joinWaitingList());
        btnLeave.setOnClickListener(v -> leaveWaitingList());

        btnShareQr.setOnClickListener(v ->
                Toast.makeText(this, "Share QR feature coming soon", Toast.LENGTH_SHORT).show()
        );

        btnViewWaitingList.setOnClickListener(v -> showWaitingListDialog());
    }
    private String getEventDocumentId() {
        if (event != null && event.getId() != null) {
            return event.getId();
        }
        return eventId; // fallback to the id passed via Intent
    }

    private void joinWaitingList() {
        if (event == null) return;

        if (event.getWaitingList() == null) {
            event.setWaitingList(new ArrayList<Entrant>());
        }

        List<Entrant> list = event.getWaitingList();

        // Already in list?
        for (Entrant e : list) {
            if (currentUserId != null && currentUserId.equals(e.getId())) {
                Toast.makeText(this, "You are already on the waiting list.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Capacity check (optional: here capacity is max entrants)
        if (list.size() >= event.getCapacity()) {
            Toast.makeText(this, "Waiting list is full.", Toast.LENGTH_SHORT).show();
            return;
        }

        Entrant entrant = new Entrant(currentUserId);
        list.add(entrant);

        String docId = getEventDocumentId();
        if (docId == null) {
            updateWaitingListUI();
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("events")
                .document(docId)
                .update("waitingList", list)
                .addOnSuccessListener(aVoid -> {
                    int waitingCount = list.size();
                    tvAvailable.setText(String.valueOf(event.getCapacity() - waitingCount));
                    updateWaitingListUI();
                })
                .addOnFailureListener(e -> {
                    list.remove(entrant); // rollback
                    Toast.makeText(this, "Failed to join waiting list.", Toast.LENGTH_SHORT).show();
                });
    }

    private void leaveWaitingList() {
        if (event == null || event.getWaitingList() == null) {
            Toast.makeText(this, "No event or waiting list.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Entrant> list = event.getWaitingList();
        Log.d("LeaveWL", "Current userId = " + currentUserId);
        for (Entrant e : list) {
            Log.d("LeaveWL", "Entrant in list: id=" + e.getId() + ", name=" + e.getDisplayName());
        }

        Entrant toRemove = null;
        for (Entrant e : list) {
            if (currentUserId != null && currentUserId.equals(e.getId())) {
                toRemove = e;
                break;
            }
        }

        if (toRemove == null) {
            Toast.makeText(this, "You are not on the waiting list.", Toast.LENGTH_SHORT).show();
            return;
        }

        list.remove(toRemove);
        final Entrant removed = toRemove;  // <- effectively final
        String docId = getEventDocumentId();
        if (docId == null) {
            Log.e("LeaveWL", "docId is null, cannot update Firestore");
            updateWaitingListUI();
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("events")
                .document(docId)
                .update("waitingList", list)
                .addOnSuccessListener(aVoid -> {
                    Log.d("LeaveWL", "Firestore update successful");
                    int waitingCount = list.size();
                    tvAvailable.setText(String.valueOf(event.getCapacity() - waitingCount));
                    updateWaitingListUI();
                })
                .addOnFailureListener(e -> {
                    Log.e("LeaveWL", "Firestore update FAILED", e);
                    list.add(removed); // rollback
                    Toast.makeText(this, "Failed to leave waiting list.", Toast.LENGTH_SHORT).show();
                });
    }

    private void showWaitingListDialog() {
        if (event == null || event.getWaitingList() == null || event.getWaitingList().isEmpty()) {
            Toast.makeText(this, "No entrants in waiting list.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Entrant> list = event.getWaitingList();

        StringBuilder message = new StringBuilder();
        int i = 1;
        for (Entrant entrant : list) {
            String name = entrant.getDisplayName() != null
                    ? entrant.getDisplayName()
                    : "User " + entrant.getId();
            message.append(i++).append(". ").append(name).append("\n");
        }

        new AlertDialog.Builder(this)
                .setTitle("Waiting List (" + list.size() + " Entrants)")
                .setMessage(message.toString())
                .setPositiveButton("OK", null)
                .show();
    }



}
