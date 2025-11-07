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

/**
 * {@code AdminBrowseEventsActivity} allows administrators to browse, view, and delete events
 * stored in Firebase Firestore.
 * <p>
 * The activity displays all events in descending order of their creation time.
 * Each event is represented by a card containing details such as title, date, capacity,
 * and registration status. Administrators can also delete an event after a confirmation dialog.
 * </p>
 *
 * <h3>Features:</h3>
 * <ul>
 *     <li>Displays a list of all events fetched from Firestore.</li>
 *     <li>Supports reloading the list dynamically after event deletion.</li>
 *     <li>Provides a back button to return to the previous screen.</li>
 *     <li>Shows a placeholder message if no events are available.</li>
 * </ul>
 *
 * <h3>Firestore Structure:</h3>
 * <pre>
 * Collection: events
 * ├── id: String
 * ├── name: String
 * ├── description: String
 * ├── capacity: Long
 * ├── geolocationRequired: Boolean
 * ├── posterUri: String
 * ├── registrationOpen: Long (timestamp)
 * ├── registrationClose: Long (timestamp)
 * ├── createdAt: Long (timestamp)
 * </pre>
 *
 * @author
 *     Gainly Flow Development Team
 * @version
 *     1.0, November 2025
 */
public class AdminBrowseEventsActivity extends AppCompatActivity {

    /** Container layout holding dynamically generated event cards. */
    private LinearLayout eventListContainer;

    /** Search box for filtering events (future implementation). */
    private EditText searchBox;

    /** Button that navigates back to the previous activity. */
    private ImageButton backButton;

    /** Formatter for displaying event dates in user-friendly format. */
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    /**
     * Called when the activity is created.
     * Initializes UI elements, sets listeners, and loads event data from Firestore.
     *
     * @param savedInstanceState the saved instance state bundle.
     */
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

    /**
     * Loads all events from the Firebase Firestore database.
     * <p>
     * The events are ordered by creation time (newest first).
     * If no events are found, a placeholder message is displayed.
     * Each event document is mapped into an {@link Event} object and displayed
     * via {@link #addEventCard(Event)}.
     * </p>
     */
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

    /**
     * Dynamically adds a new event card to the list container.
     * Each card displays event information such as title, date, spots, waiting count, and status.
     * It also provides a delete button for removing the event from Firestore.
     *
     * @param event the {@link Event} object containing event details.
     */
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
        waiting.setText("45 waiting"); // Placeholder value; to be replaced with dynamic data.

        if (event.isRegistrationOpen()) {
            status.setText("Open");
            status.setTextColor(getResources().getColor(R.color.green_500));
            status.setBackgroundResource(R.drawable.status_open_bg);
        } else {
            status.setText("Closed");
            status.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            status.setBackgroundResource(R.drawable.status_closed_bg);
        }

        // Handle delete confirmation and action.
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
