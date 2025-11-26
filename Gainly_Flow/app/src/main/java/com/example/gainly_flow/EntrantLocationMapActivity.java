package com.example.gainly_flow;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Activity that displays a map showing the locations where entrants joined
 * an event's waiting list. Implements US 02.02.02.
 *
 * As an organizer, I want to see on a map where entrants joined my event
 * waiting list from.
 */
public class EntrantLocationMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "EntrantLocationMap";

    private GoogleMap mMap;
    private FirebaseFirestore db;
    private String eventId;
    private String eventName;
    private TextView tvTitle;
    private TextView tvEntrantCount;
    private ImageButton btnBack;

    private List<EntrantLocation> entrantLocations = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrant_location_map);

        // Get event details from intent
        eventId = getIntent().getStringExtra("event_id");
        eventName = getIntent().getStringExtra("event_name");

        if (eventId == null) {
            Toast.makeText(this, "Error: No event ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Initialize views
        tvTitle = findViewById(R.id.tv_map_title);
        tvEntrantCount = findViewById(R.id.tv_entrant_count);
        btnBack = findViewById(R.id.btn_back);

        tvTitle.setText(eventName != null ? eventName : "Event Waiting List Map");
        btnBack.setOnClickListener(v -> finish());

        // Initialize map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(true);

        // Load entrant locations from Firestore
        loadEntrantLocations();
    }

    /**
     * Loads entrant location data from Firestore for the event's waiting list.
     * Looks for location data stored in a subcollection called "entrantLocations"
     * under each event document.
     */
    private void loadEntrantLocations() {
        db.collection("events")
                .document(eventId)
                .collection("entrantLocations")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    entrantLocations.clear();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        try {
                            String entrantId = doc.getString("entrantId");
                            String entrantName = doc.getString("entrantName");
                            Double latitude = doc.getDouble("latitude");
                            Double longitude = doc.getDouble("longitude");
                            Date joinedAt = doc.getDate("joinedAt");

                            if (latitude != null && longitude != null) {
                                EntrantLocation location = new EntrantLocation(
                                        entrantId,
                                        entrantName != null ? entrantName : "Unknown",
                                        latitude,
                                        longitude,
                                        joinedAt
                                );
                                entrantLocations.add(location);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing entrant location: " + e.getMessage());
                        }
                    }

                    displayLocationsOnMap();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load entrant locations: " + e.getMessage());
                    Toast.makeText(this, "Failed to load locations", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Displays all entrant locations as markers on the map and adjusts
     * the camera to show all markers.
     */
    private void displayLocationsOnMap() {
        if (mMap == null) return;

        // Update count
        tvEntrantCount.setText("Showing " + entrantLocations.size() + " entrant location(s)");

        if (entrantLocations.isEmpty()) {
            Toast.makeText(this, "No location data available for this event", Toast.LENGTH_LONG).show();
            // Default to a world view
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(0, 0), 2));
            return;
        }

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

        for (EntrantLocation location : entrantLocations) {
            LatLng position = new LatLng(location.getLatitude(), location.getLongitude());

            // Add marker for this entrant
            mMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(location.getEntrantName())
                    .snippet("Joined: " + (location.getJoinedAt() != null ?
                            location.getJoinedAt().toString() : "Unknown")));

            boundsBuilder.include(position);
        }

        // Adjust camera to show all markers
        try {
            LatLngBounds bounds = boundsBuilder.build();
            int padding = 100; // padding in pixels
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
        } catch (IllegalStateException e) {
            // If there's only one marker, zoom to it
            if (entrantLocations.size() == 1) {
                EntrantLocation location = entrantLocations.get(0);
                LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 12));
            }
        }
    }
}