package com.example.gainly_flow;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class to manage storing entrant location data when they join
 * an event's waiting list.
 *
 * This supports US 02.02.02: "As an organizer I want to see on a map where
 * entrants joined my event waiting list from."
 */
public class LocationHelper {
    private static final String TAG = "LocationHelper";

    /**
     * Saves the current device location to Firestore when an entrant joins
     * an event's waiting list.
     *
     * @param context The application context
     * @param eventId The ID of the event being joined
     * @param entrantId The ID of the entrant joining
     * @param entrantName The display name of the entrant
     */
    public static void saveJoinLocation(Context context, String eventId, String entrantId, String entrantName) {
        // Check if we have location permission
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Location permission not granted. Cannot save join location.");
            return;
        }

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        saveLocationToFirestore(eventId, entrantId, entrantName, location);
                    } else {
                        Log.w(TAG, "Location is null. Cannot save join location.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get location: " + e.getMessage());
                });
    }

    /**
     * Saves location data to Firestore in the event's entrantLocations subcollection.
     *
     * @param eventId The event ID
     * @param entrantId The entrant ID
     * @param entrantName The entrant's display name
     * @param location The Location object containing lat/lng
     */
    private static void saveLocationToFirestore(String eventId, String entrantId, String entrantName, Location location) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> locationData = new HashMap<>();
        locationData.put("entrantId", entrantId);
        locationData.put("entrantName", entrantName);
        locationData.put("latitude", location.getLatitude());
        locationData.put("longitude", location.getLongitude());
        locationData.put("joinedAt", new Date());

        db.collection("events")
                .document(eventId)
                .collection("entrantLocations")
                .document(entrantId)
                .set(locationData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Location saved for entrant " + entrantId + " at event " + eventId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save location: " + e.getMessage());
                });
    }
}