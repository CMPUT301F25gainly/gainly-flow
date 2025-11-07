package com.example.gainly_flow;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

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

public class OrganizerLanding extends AppCompatActivity {

    private static final String TAG = "OrganizerLanding";
    private FirebaseFirestore db;
    private LinearLayout eventListContainer;

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_organizer_landing);

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

    // Handles multiple possible Firestore types
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

    private String formatDate(long millis) {
        SimpleDateFormat f = new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault());
        f.setTimeZone(TimeZone.getTimeZone("America/Edmonton"));
        return f.format(new Date(millis));
    }
}