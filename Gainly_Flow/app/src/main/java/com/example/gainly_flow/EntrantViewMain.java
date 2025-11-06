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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrant_home);

        // ---- Find views ----
        returnButton        = findViewById(R.id.returnButton);      // from your updated header
        browseEventsButton  = findViewById(R.id.browseEventsButton);
        searchInput         = findViewById(R.id.searchInput);
        categoryButton      = findViewById(R.id.categoryButton);
        dateButton          = findViewById(R.id.dateButton);
        eventListContainer  = findViewById(R.id.eventListContainer);
        bottomNav           = findViewById(R.id.bottomNav);

        // ---- Back / Return (top-left) ----
        if (returnButton != null) {
            returnButton.setOnClickListener(v -> finish());
        }

        // ---- Browse Events CTA ----
        if (browseEventsButton != null) {
            browseEventsButton.setOnClickListener(v -> {
                // For now, just open the detail page as a demo
                Intent intent = new Intent(this, EventDetailActivity.class);
                startActivity(intent);
            });
        }

        // ---- Populate list with a few sample events using your item_event.xml ----
        populateMockEvents();

        // ---- Bottom nav handling ----
        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(this::onNavItemSelected);
        }
    }

    private void populateMockEvents() {
        // Clear any existing children (in case of recreation)
        eventListContainer.removeAllViews();

        // Simple mock data for now; replace with real list later
        MockEvent[] data = new MockEvent[] {
                new MockEvent(
                        "Swimming Lessons for Beginners",
                        "Jan 15–Mar 15, 2025",
                        20, 45, "Open"),
                new MockEvent(
                        "Test 2",
                        "Apr 4, 2025",
                        200, 120, "Open"),
                new MockEvent(
                        "Test 3",
                        "Feb 2–Feb 3, 2025",
                        12, 60, "Full")
        };

        LayoutInflater inflater = LayoutInflater.from(this);

        for (MockEvent e : data) {
            View card = inflater.inflate(R.layout.item_event, eventListContainer, false);

            TextView title     = card.findViewById(R.id.eventTitle);
            TextView date      = card.findViewById(R.id.eventDate);
            TextView spots     = card.findViewById(R.id.eventSpots);
            TextView waiting   = card.findViewById(R.id.eventWaiting);
            TextView status    = card.findViewById(R.id.eventStatus);

            title.setText(e.title);
            date.setText(e.date);
            spots.setText(e.capacity + " spots");
            waiting.setText(e.waiting + " waiting");
            status.setText(e.status);

            // Color/status badge cue
            if ("Open".equalsIgnoreCase(e.status)) {
                status.setTextColor(ContextCompat.getColor(this, R.color.green_500));
            } else {
                status.setTextColor(ContextCompat.getColor(this, R.color.gray_700));
            }

            // Click → open detail screen (you can pass extras later)
            CardView root = (CardView) card;
            root.setOnClickListener(v -> {
                Intent intent = new Intent(this, EventDetailActivity.class);
                // Example extras if you want to wire them in EventDetailActivity later:
                intent.putExtra("title", e.title);
                intent.putExtra("date", e.date);
                intent.putExtra("capacity", e.capacity);
                intent.putExtra("waiting", e.waiting);
                intent.putExtra("status", e.status);
                startActivity(intent);
            });

            eventListContainer.addView(card);
        }
    }

    private boolean onNavItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_events) {
            Toast.makeText(this, "Events", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.menu_notifications) {
            Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show();
            // TODO: startActivity(new Intent(this, EntrantNotificationsActivity.class));
            return true;
        } else if (id == R.id.menu_profile) {
            Toast.makeText(this, "Profile", Toast.LENGTH_SHORT).show();
            // TODO: startActivity(new Intent(this, EntrantProfileActivity.class));
            return true;
        }
        return false;
    }

    /** Tiny struct for the mock list; you can delete this when you hook real data. */
    private static class MockEvent {
        final String title, date, status;
        final int capacity, waiting;
        MockEvent(String title, String date, int capacity, int waiting, String status) {
            this.title = title;
            this.date = date;
            this.capacity = capacity;
            this.waiting = waiting;
            this.status = status;
        }
    }
}
