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

/**
 * Displays the detail page for a single event, including title, location, schedule,
 * registration window, availability, and waiting list status.
 * <p>
 * This activity:
 * <ul>
 *   <li>Reads event data from {@link android.content.Intent} extras.</li>
 *   <li>Shows registration OPEN/CLOSED state based on current time and registration window.</li>
 *   <li>Integrates with {@link WaitingList} to join/leave and to view entrants.</li>
 *   <li>Optionally shows geolocation requirement and a placeholder QR section.</li>
 * </ul>
 *
 * <h3>Expected Intent Extras</h3>
 * <ul>
 *   <li><b>"event_id"</b> {@code String}</li>
 *   <li><b>"event_name"</b> {@code String}</li>
 *   <li><b>"event_description"</b> {@code String}</li>
 *   <li><b>"event_capacity"</b> {@code int}</li>
 *   <li><b>"event_date"</b> {@code long} (epoch millis)</li>
 *   <li><b>"registration_open"</b> {@code long} (epoch millis)</li>
 *   <li><b>"registration_close"</b> {@code long} (epoch millis)</li>
 *   <li><b>"geo_required"</b> {@code boolean}</li>
 *   <li><b>"event_location"</b> {@code String}</li>
 *   <li><b>"event_time_string"</b> {@code String} (e.g., "6:00 PM–8:00 PM")</li>
 * </ul>
 *
 * <p><b>Note:</b> The current user ID is derived from {@link Settings.Secure#ANDROID_ID}.
 * Replace with authenticated user IDs if you later tie this activity to {@link FirebaseAuth}.</p>
 */
public class EventDetailActivity extends AppCompatActivity {

    /** TextView showing the current number of entrants in the waiting list. */
    private TextView tvEntrants;
    /** TextView showing the available capacity (max entrants). */
    private TextView tvAvailable;
    /** TextView for the event title at the top of the screen. */
    private TextView titleEvent;
    /** TextView badge for "OPEN"/"CLOSED" registration state. */
    private TextView eventStatus;
    /** Compact location title line under the header. */
    private TextView locationEvent;

    /** Human-readable event date label. */
    private TextView eventDuration;
    /** Registration window open time label. */
    private TextView registrationOpen;
    /** Registration window close time label. */
    private TextView registrationClose;
    /** Human-readable event time-of-day label. */
    private TextView eventTime;

    /** Paragraph description of the event. */
    private TextView eventDescription;
    /** Detailed location line in the info section. */
    private TextView eventLocationDetail;
    /** Price label (if you later populate it). */
    private TextView eventPrice;
    /** Message shown if geolocation is required for check-in. */
    private TextView geolocationInfo;

    /** Back arrow in the toolbar/header. */
    private ImageView backButton;
    /** Optional QR image preview (placeholder). */
    private ImageView qrCodeImage;

    /** Container for QR section to show/hide the block. */
    private LinearLayout qrSection;

    /** Button to join the waiting list for this event. */
    private Button btnJoin;
    /** Button to leave the waiting list for this event. */
    private Button btnLeave;
    /** Button to share or export the QR (placeholder behavior). */
    private Button btnShareQr;
    /** Button to view the full waiting list (dialog). */
    private Button btnViewWaitingList;

    /** Date-only formatter, e.g., "Nov 07, 2025". */
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    /** Time-only formatter, e.g., "6:04 PM". */
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

    /** Backing waiting list model for this event. */
    private WaitingList waitingList;
    /** Identifier of the event being displayed. */
    private String eventId;
    /** Maximum number of entrants allowed (capacity). */
    private int eventCapacity;
    /** Device-scoped current user identifier; replace with real auth user if needed. */
    private String currentUserId;

    /**
     * Android lifecycle entry point. Initializes the UI, parses intent extras,
     * and loads the waiting list before wiring up listeners.
     *
     * @param savedInstanceState previous state, if the Activity is being re-created
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        initializeViews();

        // Get current user ID (using ANDROID_ID as a stand-in for authenticated user)
        currentUserId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        // Read event payload from intent
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            displayEventData(extras);
        } else {
            Toast.makeText(this, "Event data not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Load waiting list data and reflect it in the UI
        loadWaitingList();

        setupButtonListeners();
    }

    /**
     * Finds and caches references to all views used by this screen.
     * <p>Call once after {@link #setContentView(int)}.</p>
     */
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

    /**
     * Populates the UI with event details parsed from the given {@code extras}.
     *
     * @param extras the Intent extras bundle containing event fields; see class Javadoc for keys
     */
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

    /**
     * Creates and loads a {@link WaitingList} model for the current {@link #eventId},
     * then updates the UI once the data is available.
     * <p>Safe to call multiple times; if {@code eventId} is null, it no-ops.</p>
     */
    private void loadWaitingList() {
        if (eventId == null) return;

        waitingList = new WaitingList(eventId);
        waitingList.load(loaded -> {
            updateWaitingListUI();
        });
    }

    /**
     * Reflects the waiting list state in the UI: shows entrant count and toggles
     * Join/Leave buttons based on whether the current user is already in the list.
     * <p>Safe to call before data is loaded; shows Join by default.</p>
     */
    private void updateWaitingListUI() {
        if (waitingList == null) return;

        int count = waitingList.getCount();
        tvEntrants.setText(String.valueOf(count));

        boolean isUserInList = waitingList.getEntrants().contains(currentUserId);

        // Default to show Join button if list hasn't loaded yet
        btnJoin.setVisibility(isUserInList ? View.GONE : View.VISIBLE);
        btnLeave.setVisibility(isUserInList ? View.VISIBLE : View.GONE);
    }

    /**
     * Computes and displays the registration state ("OPEN" or "CLOSED") and enables/disables
     * the Join button accordingly.
     *
     * @param regOpenMillis  registration open time in epoch millis (0 if unspecified)
     * @param regCloseMillis registration close time in epoch millis (0 if unspecified)
     */
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

    /**
     * Updates the schedule section: event date, registration open/close timestamps, and
     * a free-form event time string.
     *
     * @param eventDateMillis   event date in epoch millis (0 if unspecified)
     * @param regOpenMillis     registration open in epoch millis (0 if unspecified)
     * @param regCloseMillis    registration close in epoch millis (0 if unspecified)
     * @param eventTimeString   human-readable time string (e.g., "6:00 PM–8:00 PM"); may be null/empty
     */
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

    /**
     * Placeholder hook to populate a QR code for the given event; currently no-ops.
     * <p>Integrate with a QR library and set {@link #qrCodeImage} when available.</p>
     *
     * @param eventId the event identifier; ignored if null/empty
     */
    private void updateQrCode(String eventId) {
        if (eventId != null && !eventId.isEmpty()) {
            // QR logic if needed
        }
    }

    /**
     * Wires up click listeners for the back button, Join/Leave actions, placeholder QR share,
     * and viewing the full waiting list (dialog).
     * <p>Join/Leave operations mutate the {@link WaitingList} model and refresh the UI.</p>
     */
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
