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

import java.util.HashMap;
import java.util.Map;

public class EntrantViewMain extends AppCompatActivity {

    // Header
    private View returnButton;

    // Top CTA
    private MaterialButton browseEventsButton;

    // Filters (present in XML, not fully used yet)
    private EditText searchInput;
    private View categoryButton;
    private View dateButton;

    // List container for event cards
    private LinearLayout eventListContainer;

    // Bottom nav
    private BottomNavigationView bottomNav;

    // --- Firebase wrapper you already integrated ---
    private final Database db = new Database();
    private static final String COL_EVENTS = "events";

    // Field names used by EventDetailActivity too
    private static final String F_NAME      = "name";
    private static final String F_LOCATION  = "location";        // optional to show in row
    private static final String F_STATUS    = "status";          // "Open"/"Full"/"Closed"
    private static final String F_WAITING   = "waitingCount";
    private static final String F_AVAILABLE = "availableCount";

    // Keep a handle from eventId -> inflated row, so we can update on snapshots
    private final Map<String, View> rowById = new HashMap<>();

    // For now: a few known IDs (swap to your real list later)
    private static final String[] EVENT_IDS = new String[] { "evt-001", "evt-002", "evt-003" };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Use your existing layout that defines eventListContainer, etc.
        setContentView(R.layout.activity_entrant_home);

        // ---- Find views ----
        returnButton        = findViewById(R.id.returnButton);
        browseEventsButton  = findViewById(R.id.browseEventsButton);
        searchInput         = findViewById(R.id.searchInput);
        categoryButton      = findViewById(R.id.categoryButton);
        dateButton          = findViewById(R.id.dateButton);
        eventListContainer  = findViewById(R.id.eventListContainer);
        bottomNav           = findViewById(R.id.bottomNav);

        if (returnButton != null) returnButton.setOnClickListener(v -> finish());

        if (browseEventsButton != null) {
            browseEventsButton.setOnClickListener(v -> {
                // Demo: open first event detail if available
                Intent intent = new Intent(this, EventDetailActivity.class);
                intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, EVENT_IDS[0]);
                startActivity(intent);
            });
        }

        // 🔥 Live list from Firestore via your Database wrapper
        populateFromFirestore();

        if (bottomNav != null) bottomNav.setOnItemSelectedListener(this::onNavItemSelected);
    }

    /** Subscribe to each known eventId and keep the UI row live. */
    private void populateFromFirestore() {
        eventListContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (String eventId : EVENT_IDS) {
            // 1) Inflate a row now (so the list shows quickly)
            View card = inflater.inflate(R.layout.item_event, eventListContainer, false);
            bindPlaceholders(card); // optional: show placeholders before data arrives
            eventListContainer.addView(card);
            rowById.put(eventId, card);

            // 2) Initial render from cache (may be null on first run)
            renderRow(eventId, db.get(COL_EVENTS, eventId));

            // 3) Subscribe for live updates
            db.subscribe(COL_EVENTS, eventId, () -> {
                Map<String, Object> ev = db.get(COL_EVENTS, eventId);
                renderRow(eventId, ev);
            });

            // 4) Click → open details with eventId only
            ((CardView) card).setOnClickListener(v -> {
                Intent intent = new Intent(this, EventDetailActivity.class);
                intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, eventId);
                startActivity(intent);
            });
        }
    }

    private void bindPlaceholders(View card) {
        TextView title   = card.findViewById(R.id.eventTitle);
        TextView date    = card.findViewById(R.id.eventDate);
        TextView spots   = card.findViewById(R.id.eventSpots);
        TextView waiting = card.findViewById(R.id.eventWaiting);
        TextView status  = card.findViewById(R.id.eventStatus);

        title.setText("Loading…");
        date.setText("");
        spots.setText("");
        waiting.setText("");
        status.setText("");
    }

    /** Render one row from the Firestore map. */
    private void renderRow(String eventId, Map<String, Object> ev) {
        View card = rowById.get(eventId);
        if (card == null) return;

        TextView title   = card.findViewById(R.id.eventTitle);
        TextView date    = card.findViewById(R.id.eventDate);
        TextView spots   = card.findViewById(R.id.eventSpots);
        TextView waiting = card.findViewById(R.id.eventWaiting);
        TextView status  = card.findViewById(R.id.eventStatus);

        if (ev == null) {
            title.setText("Unavailable");
            date.setText("");
            spots.setText("");
            waiting.setText("");
            status.setText("");
            status.setTextColor(ContextCompat.getColor(this, R.color.gray_700));
            return;
        }

        String name  = s(ev.get(F_NAME));
        String stat  = s(ev.get(F_STATUS));
        int capLeft  = i(ev.get(F_AVAILABLE));
        int waitCnt  = i(ev.get(F_WAITING));

        title.setText(name.isEmpty() ? "Untitled Event" : name);
        // If you store dates in docs later, map them to text here. For now, omit or show location.
        date.setText(s(ev.get(F_LOCATION)));
        spots.setText(capLeft + " spots");
        waiting.setText(waitCnt + " waiting");
        status.setText(stat);

        status.setTextColor(ContextCompat.getColor(this,
                "Open".equalsIgnoreCase(stat) ? R.color.green_500 : R.color.gray_700));
    }

    private boolean onNavItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_events) {
            Toast.makeText(this, "Events", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.menu_notifications) {
            Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.menu_profile) {
            Toast.makeText(this, "Profile", Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    private static String s(Object o) { return o == null ? "" : String.valueOf(o); }
    private static int i(Object o) {
        if (o instanceof Number) return ((Number) o).intValue();
        if (o instanceof String) try { return Integer.parseInt((String) o); } catch (Exception ignored) {}
        return 0;
    }
}
