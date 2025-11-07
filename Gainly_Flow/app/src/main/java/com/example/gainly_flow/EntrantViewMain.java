package com.example.gainly_flow;

import android.content.Intent;
import android.os.Bundle;
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
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EntrantViewMain extends AppCompatActivity {

    // UI (these ids exist in activity_entrant_home.xml)
    private MaterialButton browseEventsButton;
    private EditText searchInput;
    private View categoryButton, dateButton;
    private LinearLayout eventListContainer;
    private BottomNavigationView bottomNav;

    // Firestore
    private FirebaseFirestore fs;
    private ListenerRegistration eventsReg;

    private final SimpleDateFormat DF = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrant_home);

        // bind
        browseEventsButton = findViewById(R.id.browseEventsButton);
        searchInput        = findViewById(R.id.searchInput);
        categoryButton     = findViewById(R.id.categoryButton);
        dateButton         = findViewById(R.id.dateButton);
        eventListContainer = findViewById(R.id.eventListContainer);
        bottomNav          = findViewById(R.id.bottomNav);

        fs = FirebaseFirestore.getInstance();

        if (browseEventsButton != null) {
            browseEventsButton.setOnClickListener(v -> {
                // optional: scroll to top or just toast
                Toast.makeText(this, "Browse all events", Toast.LENGTH_SHORT).show();
            });
        }
        if (bottomNav != null) bottomNav.setOnItemSelectedListener(this::onNavItemSelected);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Live list direct from Firestore (no Database.java changes)
        eventsReg = fs.collection("events")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, err) -> {
                    if (err != null || snap == null) return;
                    List<EventRow> rows = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        String id   = getStr(d.get("id"));        // your schema stores its own id
                        if (id.isEmpty()) id = d.getId();          // fallback to doc id
                        String name = getStr(d.get("name"));
                        String desc = getStr(d.get("description"));

                        Long regOpen  = getLong(d.get("registrationOpen"));
                        Long regClose = getLong(d.get("registrationClose"));
                        Integer cap   = getInt(d.get("capacity"));

                        rows.add(new EventRow(id, name, desc, regOpen, regClose, cap));
                    }
                    renderList(rows);
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (eventsReg != null) { eventsReg.remove(); eventsReg = null; }
    }

    /* ---------- UI render ---------- */

    private void renderList(List<EventRow> rows) {
        if (eventListContainer == null) return;
        LayoutInflater inflater = LayoutInflater.from(this);
        eventListContainer.removeAllViews();

        for (EventRow r : rows) {
            View card = inflater.inflate(R.layout.item_event, eventListContainer, false);

            TextView title   = card.findViewById(R.id.eventTitle);
            TextView date    = card.findViewById(R.id.eventDate);
            TextView spots   = card.findViewById(R.id.eventSpots);
            TextView waiting = card.findViewById(R.id.eventWaiting);
            TextView status  = card.findViewById(R.id.eventStatus);

            title.setText(r.name.isEmpty() ? "Untitled Event" : r.name);
            date.setText(formatRegRange(r.regOpenMs, r.regCloseMs));

            int capacity = Math.max(0, r.capacity == null ? 0 : r.capacity);
            spots.setText(capacity + " spots");

            waiting.setText(""); // not tracked yet in your schema; leave blank or "—"

            // Basic open/closed based on reg window
            boolean isOpen = isNowWithin(r.regOpenMs, r.regCloseMs);
            status.setText(isOpen ? "Open" : "Closed");
            status.setTextColor(ContextCompat.getColor(this,
                    isOpen ? android.R.color.holo_green_dark : android.R.color.darker_gray));

            ((CardView) card).setOnClickListener(v -> {
                Intent intent = new Intent(this, EventDetailActivity.class);
                intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, r.id);
                startActivity(intent);
            });

            eventListContainer.addView(card);
        }
    }

    private String formatRegRange(Long openMs, Long closeMs) {
        if (openMs == null && closeMs == null) return "";
        if (openMs != null && closeMs == null) return "Opens " + DF.format(new java.util.Date(openMs));
        if (openMs == null) return "Closes " + DF.format(new java.util.Date(closeMs));

        java.util.Calendar cs = java.util.Calendar.getInstance(); cs.setTimeInMillis(openMs);
        java.util.Calendar ce = java.util.Calendar.getInstance(); ce.setTimeInMillis(closeMs);
        if (cs.get(java.util.Calendar.YEAR) == ce.get(java.util.Calendar.YEAR)) {
            SimpleDateFormat shortDF = new SimpleDateFormat("MMM d", Locale.getDefault());
            return shortDF.format(new java.util.Date(openMs)) + " – " + DF.format(new java.util.Date(closeMs));
        }
        return DF.format(new java.util.Date(openMs)) + " – " + DF.format(new java.util.Date(closeMs));
    }

    private boolean isNowWithin(Long openMs, Long closeMs) {
        long now = System.currentTimeMillis();
        if (openMs == null && closeMs == null) return true; // default open if no windows set
        if (openMs != null && now < openMs) return false;
        if (closeMs != null && now > closeMs) return false;
        return true;
    }

    /* ---------- Nav ---------- */
    private boolean onNavItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_events) {
            return true;
        } else if (id == R.id.menu_notifications) {
            Intent i = new Intent(this, EntrantNotificationsActivity.class);
            startActivity(i);
            return true;
        } else if (id == R.id.menu_profile) {
            Toast.makeText(this, "Profile", Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    /* ---------- tiny utils ---------- */
    private static String getStr(Object o) { return o == null ? "" : String.valueOf(o); }
    private static Integer getInt(Object o) {
        if (o instanceof Number) return ((Number) o).intValue();
        if (o instanceof String) try { return Integer.parseInt((String) o); } catch (Exception ignored) {}
        return null;
    }
    private static Long getLong(Object o) {
        if (o instanceof Number) return ((Number) o).longValue();
        if (o instanceof String) try { return Long.parseLong((String) o); } catch (Exception ignored) {}
        return null;
    }

    /* ---------- light row model ---------- */
    private static class EventRow {
        final String id, name, desc;
        final Long regOpenMs, regCloseMs;
        final Integer capacity;
        EventRow(String id, String name, String desc, Long regOpenMs, Long regCloseMs, Integer capacity) {
            this.id = id;
            this.name = name == null ? "" : name;
            this.desc = desc == null ? "" : desc;
            this.regOpenMs = regOpenMs;
            this.regCloseMs = regCloseMs;
            this.capacity = capacity;
        }
    }
}
