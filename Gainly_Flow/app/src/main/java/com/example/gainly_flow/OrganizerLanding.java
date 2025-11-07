package com.example.gainly_flow;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.Intent;
import androidx.activity.OnBackPressedCallback;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * OrganizerLanding
 * <p>
 * Landing screen for organizers. Displays a reverse-chronological list of events pulled from the
 * {@code events} collection in Firestore, with per-card status (open/closed), capacity, date/time,
 * and waiting list size. Each card is clickable and navigates to
 * {@link OrganizerEntrantListActivity} for detailed waiting list management.
 * </p>
 *
 * <h3>Key behaviors</h3>
 * <ul>
 *   <li>Enables edge-to-edge UI and applies system bar insets.</li>
 *   <li>Provides both an up-button and physical back gesture that return to {@link MainActivity}.</li>
 *   <li>Supports heterogeneous Firestore schemas for numeric and timestamp-like fields via
 *       {@link #getLongFlexible(DocumentSnapshot, String)} and {@link #getMillis(DocumentSnapshot, String)}.</li>
 *   <li>Shows a "Create Event" button that opens {@link CreateEvent}.</li>
 * </ul>
 *
 * <h3>Firestore schema expectations</h3>
 * <ul>
 *   <li>{@code events}: documents with fields such as {@code id}, {@code name}, {@code capacity},
 *       {@code eventDateTimeUtc} (preferred) or {@code eventDate}, {@code registrationOpen},
 *       {@code registrationClose}, and {@code createdAt} (for ordering).</li>
 *   <li>{@code waiting_lists/{eventId}}: document containing an array field {@code entrants}
 *       whose size is used to display "X waiting".</li>
 * </ul>
 */
public class OrganizerLanding extends AppCompatActivity {

    /** Tag used for logcat messages originating from this class. */
    private static final String TAG = "OrganizerLanding";

    /** Shared Firestore instance for loading events and waiting list sizes. */
    private FirebaseFirestore db;

    /** Container into which event "card" views (inflated from {@code R.layout.item_event}) are added. */
    private LinearLayout eventListContainer;

    /**
     * Parses a Firestore field as a {@link Long} in a schema-tolerant manner.
     * <p>
     * Accepts {@link Number} (returns {@code longValue()}), numeric {@link String}, or {@code null}.
     * Any other type or unparsable value returns {@code null}.
     * </p>
     *
     * @param d   The Firestore document snapshot to read from.
     * @param key The field name to parse.
     * @return A {@link Long} value if present and parsable; otherwise {@code null}.
     */
    private @Nullable Long getLongFlexible(DocumentSnapshot d, String key) {
        Object v = d.get(key);
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        if (v instanceof String) {
            try {
                String s = ((String) v).trim();
                if (s.isEmpty()) return null;
                return Long.parseLong(s);
            } catch (NumberFormatException ignore) { return null; }
        }
        return null;
    }

    /**
     * Navigates back to {@link MainActivity} and clears intermediate activities if present.
     * <p>
     * Uses {@link Intent#FLAG_ACTIVITY_CLEAR_TOP} and {@link Intent#FLAG_ACTIVITY_SINGLE_TOP}
     * so that an existing {@code MainActivity} is reused instead of creating a new instance.
     * </p>
     */
    private void goHome() {
        Intent i = new Intent(this, MainActivity.class);
        // Clear anything above MainActivity if it already exists in the stack
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }

    /**
     * Handles toolbar "up" navigation by routing to {@link #goHome()}.
     *
     * @return {@code true} after performing navigation.
     */
    @Override
    public boolean onSupportNavigateUp() {
        goHome();
        return true;
    }

    /**
     * Initializes the UI: edge-to-edge layout, action bar "up" button, back-press callback,
     * insets handling, Firestore instance, and click listeners. Triggers the initial load of events.
     *
     * @param savedInstanceState Previously saved state, or {@code null}.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_organizer_landing);

        // Show the action bar back arrow (Up)
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(""); // optional
        }

        // Make the physical/gesture back go to MainActivity
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { goHome(); }
        });

        findViewById(R.id.btn_home).setOnClickListener(v -> goHome());

        // Apply system bar insets to the root container
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        eventListContainer = findViewById(R.id.eventListContainer);

        Button createEventButton = findViewById(R.id.btn_create_event);
        createEventButton.setOnClickListener(v -> {
            Intent toCreateEvent = new Intent(OrganizerLanding.this, CreateEvent.class);
            startActivity(toCreateEvent);
        });

        loadEvents();
    }

    /**
     * Loads events from Firestore and renders one card per document inside {@link #eventListContainer}.
     * <p>
     * Query: {@code events} ordered by {@code createdAt} descending. For each event:
     * </p>
     * <ul>
     *   <li>Title from {@code name} (fallback "Untitled Event").</li>
     *   <li>Capacity from {@code capacity} via {@link #getLongFlexible(DocumentSnapshot, String)}.</li>
     *   <li>Date/time from {@code eventDateTimeUtc} or {@code eventDate} via {@link #getMillis(DocumentSnapshot, String)}.</li>
     *   <li>Status derived from {@code registrationOpen}/{@code registrationClose} vs current time.</li>
     *   <li>Waiting count fetched from {@code waiting_lists/{eventId}} (size of {@code entrants}).</li>
     *   <li>Clicking a card opens {@link OrganizerEntrantListActivity} with {@code eventId} and {@code eventName} extras.</li>
     * </ul>
     * <p>
     * Any read failure is logged and leaves the current contents unchanged.
     * </p>
     */
    private void loadEvents() {
        db.collection("events")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(qs -> {
                    eventListContainer.removeAllViews();
                    long now = System.currentTimeMillis();

                    for (QueryDocumentSnapshot doc : qs) {
                        View card = getLayoutInflater().inflate(R.layout.item_event, eventListContainer, false);

                        TextView title   = card.findViewById(R.id.eventTitle);
                        TextView date    = card.findViewById(R.id.eventDate);
                        TextView spots   = card.findViewById(R.id.eventSpots);
                        TextView waiting = card.findViewById(R.id.eventWaiting);
                        TextView status  = card.findViewById(R.id.eventStatus);

                        String name = doc.getString("name");
                        Long capacity = getLongFlexible(doc, "capacity"); // instead of document.getLong("capacity")
                        long capValue = capacity != null ? capacity : 0L;      // choose a sensible default
                        String eventId = doc.getString("id");

                        Long dtMs = getMillis(doc, "eventDateTimeUtc");
                        if (dtMs == null) dtMs = getMillis(doc, "eventDate");

                        title.setText(name != null ? name : "Untitled Event");
                        spots.setText(capacity != null ? (capacity + " spots") : "—");
                        date.setText(dtMs != null ? formatDate(dtMs) : "TBA");

                        Long openMs  = getMillis(doc, "registrationOpen");
                        Long closeMs = getMillis(doc, "registrationClose");
                        boolean isOpen = openMs != null && closeMs != null && now >= openMs && now <= closeMs;
                        status.setText(isOpen ? "Open" : "Closed");
                        status.setBackgroundResource(isOpen ? R.drawable.status_open_bg : R.drawable.status_closed_bg);

                        waiting.setText("… waiting");
                        if (eventId != null) {
                            db.collection("waiting_lists").document(eventId).get()
                                    .addOnSuccessListener(wd -> {
                                        @SuppressWarnings("unchecked")
                                        List<String> entrants = (List<String>) wd.get("entrants");
                                        int n = (entrants != null) ? entrants.size() : 0;
                                        waiting.setText(n + " waiting");
                                    })
                                    .addOnFailureListener(e -> waiting.setText("0 waiting"));
                        }

                        // ✅ Make each card clickable → open waiting list screen
                        card.setOnClickListener(v -> {
                            Intent i = new Intent(this, OrganizerEntrantListActivity.class);
                            i.putExtra("eventId", eventId);
                            i.putExtra("eventName", name);
                            startActivity(i);
                        });

                        eventListContainer.addView(card);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to load events", e));
    }

    /**
     * Retrieves a millisecond timestamp for a field that may be stored in multiple Firestore types.
     * <p>
     * Supported types:
     * </p>
     * <ul>
     *   <li>{@link Number}: returns {@code longValue()}.</li>
     *   <li>{@link Timestamp}: returns {@code toDate().getTime()}.</li>
     *   <li>{@link String}: parsed via {@link Long#parseLong(String)}.</li>
     * </ul>
     * Unrecognized or unparsable values return {@code null}.
     *
     * @param doc   The document snapshot.
     * @param field The field name to read.
     * @return Milliseconds since epoch if present and parsable; otherwise {@code null}.
     */
    @Nullable
    private Long getMillis(DocumentSnapshot doc, String field) {
        Object v = doc.get(field);
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        if (v instanceof Timestamp) return ((Timestamp) v).toDate().getTime();
        if (v instanceof String) {
            try { return Long.parseLong((String) v); } catch (Exception ignored) {}
        }
        return null;
    }

    /**
     * Formats a UTC millisecond instant into a human-readable date/time string in the
     * {@code America/Edmonton} time zone, using pattern {@code "MMM d, yyyy h:mm a"}.
     *
     * @param millis Epoch time in milliseconds.
     * @return A formatted date/time string (e.g., {@code "Nov 7, 2025 3:30 PM"}).
     */
    private String formatDate(long millis) {
        SimpleDateFormat f = new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault());
        f.setTimeZone(TimeZone.getTimeZone("America/Edmonton"));
        return f.format(new Date(millis));
    }
}
