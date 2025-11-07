package com.example.gainly_flow;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AdminBrowseEventsActivity extends AppCompatActivity {

    private LinearLayout eventListContainer;
    private EditText searchBox;
    private ImageButton backButton;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_browse_events);

        eventListContainer = findViewById(R.id.eventListContainer);
        searchBox = findViewById(R.id.etSearch);
        backButton = findViewById(R.id.btnBack);

        backButton.setOnClickListener(v -> finish());

        loadEventsFromFirebase();
    }

    private void loadEventsFromFirebase() {
        FirebaseFirestore.getInstance()
                .collection("events")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    eventListContainer.removeAllViews();

                    if (querySnapshot.isEmpty()) {
                        TextView emptyText = new TextView(this);
                        emptyText.setText("No events available");
                        emptyText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                        emptyText.setPadding(0, 50, 0, 50);
                        eventListContainer.addView(emptyText);
                        return;
                    }

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String id = doc.getString("id");
                        if (id == null) continue;

                        Event e = new Event(id);
                        e.setName(doc.getString("name"));
                        e.setDescription(doc.getString("description"));

                        Long capacity = null;
                        try {
                            Number capNum = (Number) doc.get("capacity");
                            if (capNum != null) capacity = capNum.longValue();
                        } catch (Exception ignored) {}
                        if (capacity != null) e.setCapacity(capacity.intValue());

                        e.setGeolocationRequired(Boolean.TRUE.equals(doc.getBoolean("geolocationRequired")));
                        e.setPosterImage(doc.getString("posterUri"));

                        // registration dates
                        Long regOpen = doc.getLong("registrationOpen");
                        Long regClose = doc.getLong("registrationClose");
                        if (regOpen != null || regClose != null) {
                            e.setRegistrationPeriod(
                                    regOpen == null ? null : new Date(regOpen),
                                    regClose == null ? null : new Date(regClose)
                            );
                        }

                        addEventCard(e);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load events: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void addEventCard(Event event) {
        CardView eventCard = (CardView) LayoutInflater.from(this)
                .inflate(R.layout.item_event_admin, eventListContainer, false);

        TextView title = eventCard.findViewById(R.id.eventTitle);
        TextView date = eventCard.findViewById(R.id.eventDate);
        TextView spots = eventCard.findViewById(R.id.eventSpots);
        TextView waiting = eventCard.findViewById(R.id.eventWaiting);
        TextView status = eventCard.findViewById(R.id.eventStatus);
        ImageButton deleteButton = eventCard.findViewById(R.id.btnDelete);

        title.setText(event.getName());
        if (event.getEventDate() != null)
            date.setText(dateFormat.format(event.getEventDate()));

        spots.setText(event.getCapacity() + " spots");
        waiting.setText("45 waiting"); // Placeholder, replace later with real data

        if (event.isRegistrationOpen()) {
            status.setText("Open");
            status.setTextColor(getResources().getColor(R.color.green_500));
            status.setBackgroundResource(R.drawable.status_open_bg);
        } else {
            status.setText("Closed");
            status.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            status.setBackgroundResource(R.drawable.status_closed_bg);
        }


        // On delete click → confirm and delete
        deleteButton.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete \"" + event.getName() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    FirebaseFirestore.getInstance().collection("events")
                            .document(event.getId())
                            .delete()
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Event deleted", Toast.LENGTH_SHORT).show();
                                loadEventsFromFirebase();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
                })
                .setNegativeButton("Cancel", null)
                .show());

        eventListContainer.addView(eventCard);
    }
}
