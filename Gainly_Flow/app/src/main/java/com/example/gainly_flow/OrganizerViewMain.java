package com.example.gainly_flow;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class OrganizerViewMain extends AppCompatActivity {

    private LinearLayout eventListContainer;
    private FirebaseFirestore db;
    private ImageButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_home);

        eventListContainer = findViewById(R.id.eventListContainer);
        db = FirebaseFirestore.getInstance();

        backButton = findViewById(R.id.backButton_organizer);
        backButton.setOnClickListener(v -> onBackPressed());

        Button createEventButton = findViewById(R.id.createEventButton);
        createEventButton.setOnClickListener(v -> {
            Intent toCreateEvent = new Intent(OrganizerViewMain.this, CreateEvent.class);
            startActivity(toCreateEvent);
        });

        loadOrganizerEvents();
    }

    private void loadOrganizerEvents() {
        db.collection("events")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Event> events = new ArrayList<>();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Event e = new Event();

                        // ID + basic text fields
                        e.setId(doc.getId());
                        e.setName(doc.getString("name"));
                        e.setDescription(doc.getString("description"));
                        e.setPosterImageId(doc.getString("posterUrl"));
                        e.setQrUrl(doc.getString("qrUrl"));

                        // Capacity + current participants
                        e.setCapacity(getIntSafely(doc.get("capacity")));
                        e.setCurrentParticipants(getIntSafely(doc.get("currentParticipants")));

                        // Date fields (epoch millis)
                        e.setEventDate(parseEpochMillis(doc.get("eventDateUtc")));
                        e.setRegistrationPeriod(
                                parseEpochMillis(doc.get("registrationOpenUtc")),
                                parseEpochMillis(doc.get("registrationCloseUtc"))
                        );

                        // Time of day
                        e.setTimeString(parseTimeOfDay(doc.get("eventTimeOfDayMs")));

                        // Geolocation boolean stored as string?
                        e.setGeolocationRequired(getBooleanSafely(doc.get("geolocationEnabled")));

                        // Organizer (optional)
                        if (doc.contains("organizer")) {
                            Object orgObj = doc.get("organizer");
                            if (orgObj instanceof Map) {
                                Map<String, Object> orgMap = (Map<String, Object>) orgObj;
                                Organizer org = new Organizer();
                                org.setId((String) orgMap.get("id"));
                                org.setDisplayName((String) orgMap.get("name"));
                                e.setOrganizer(org);
                            }
                        }

                        // Entrant lists — safe deserialization
                        e.setWaitingList(doc.contains("waitingList") ? doc.get("waitingList", WaitingList.class) : new WaitingList());
                        e.setSelected(getListSafely(doc.get("selected")));
                        e.setCancelled(getListSafely(doc.get("cancelled")));
                        e.setEnrolled(getListSafely(doc.get("enrolled")));

                        // Add event — no organizer filter
                        events.add(e);
                    }

                    populateEventList(events);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load events: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    // --- Helper: parse epoch millis to Date ---
    private Date parseEpochMillis(Object obj) {
        if (obj == null) return null;

        try {
            if (obj instanceof Number) return new Date(((Number) obj).longValue());
            if (obj instanceof String) return new Date(Long.parseLong((String) obj));
        } catch (Exception ignored) {
        }
        return null;
    }

    // --- Helper: parse timeOfDayMs into HH:mm ---
    private String parseTimeOfDay(Object obj) {
        try {
            long ms;
            if (obj instanceof Number) ms = ((Number) obj).longValue();
            else if (obj instanceof String) ms = Long.parseLong((String) obj);
            else return "Time not specified";

            int totalSeconds = (int) (ms / 1000);
            int hour = totalSeconds / 3600;
            int minute = (totalSeconds % 3600) / 60;
            return String.format("%02d:%02d", hour, minute);

        } catch (Exception e) {
            return "Time not specified";
        }
    }

    // --- Helper: safe integer ---
    private int getIntSafely(Object obj) {
        if (obj instanceof Number) return ((Number) obj).intValue();
        if (obj instanceof String) {
            try { return Integer.parseInt((String) obj); }
            catch (Exception ignored) {}
        }
        return 0;
    }

    // --- Helper: safe boolean ---
    private boolean getBooleanSafely(Object obj) {
        if (obj instanceof Boolean) return (Boolean) obj;
        if (obj instanceof String) return Boolean.parseBoolean((String) obj);
        return false;
    }

    // --- Helper: safe List<Entrant> deserialization ---
    @SuppressWarnings("unchecked")
    private List<Entrant> getListSafely(Object obj) {
        if (obj instanceof List) {
            List<?> rawList = (List<?>) obj;
            List<Entrant> result = new ArrayList<>();

            for (Object item : rawList) {
                if (item instanceof Map) {
                    Map<String, Object> map = (Map<String, Object>) item;
                    Entrant entrant = new Entrant();
                    entrant.setId((String) map.get("id"));
                    entrant.setDisplayName((String) map.get("name"));
                    result.add(entrant);
                }
            }
            return result;
        }
        return new ArrayList<>();
    }

    // --- Populate UI ---
    private void populateEventList(@NonNull List<Event> events) {
        eventListContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (Event e : events) {
            View itemView = inflater.inflate(R.layout.item_organizer_event, eventListContainer, false);

            TextView title = itemView.findViewById(R.id.eventTitle);
            TextView waiting = itemView.findViewById(R.id.eventWaiting);
            TextView selected = itemView.findViewById(R.id.eventSelected);
            TextView cancelled = itemView.findViewById(R.id.eventCancelled);
            TextView status = itemView.findViewById(R.id.eventStatus);

            title.setText(e.getName() != null ? e.getName() : "(No Name)");
            waiting.setText(String.valueOf(e.getWaitingList().getCount()));
            selected.setText(String.valueOf(e.getSelected().size()));
            cancelled.setText(String.valueOf(e.getCancelled().size()));

            String regStatus = e.getRegistrationStatus();
            status.setText(regStatus);

            switch (regStatus) {
                case "OPEN":
                    status.setTextColor(getResources().getColor(R.color.green_500));
                    status.setBackgroundResource(R.drawable.status_open_bg);
                    break;
                case "FULL":
                    status.setTextColor(getResources().getColor(R.color.yellow_500));
                    status.setBackgroundResource(R.drawable.box_waiting);
                    break;
                case "CANCELLED":
                    status.setTextColor(getResources().getColor(R.color.red_500));
                    status.setBackgroundResource(R.drawable.box_cancelled);
                    break;
                default:
                    status.setTextColor(getResources().getColor(R.color.grey_500));
                    status.setBackgroundResource(R.drawable.status_closed_bg);
            }

            eventListContainer.addView(itemView);
        }

        if (events.isEmpty()) {
            Toast.makeText(this, "No events found", Toast.LENGTH_SHORT).show();
        }
    }
}
