package com.example.gainly_flow;

import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles random selection ("lottery") of entrants for events.
 * Persists results in Firestore under collection "selected_invites".
 */
public class LotterySystem {
    private static final String TAG = "LotterySystem";

    /**
     * Draw a random selection of entrants from a waiting list.
     *
     * @param eventId Event to draw from.
     * @param count   Number of entrants to select.
     * @param listener Callback with list of selected entrant IDs.
     */
    public static void drawSelected(String eventId, int count, OnSuccessListener<List<String>> listener) {
        if (eventId == null || eventId.trim().isEmpty()) {
            android.util.Log.e(TAG, "drawSelected: null/empty eventId");
            listener.onSuccess(new java.util.ArrayList<>());
            return;
        }
        // Load waiting list first
        Database.get("waiting_lists", eventId, document -> {
            if (!document.exists()) {
                Log.w(TAG, "No waiting list found for event " + eventId);
                listener.onSuccess(new ArrayList<>());
                return;
            }

            List<String> entrants = (List<String>) document.get("entrants");
            if (entrants == null || entrants.isEmpty()) {
                Log.w(TAG, "No entrants to draw for event " + eventId);
                listener.onSuccess(new ArrayList<>());
                return;
            }

            // Shuffle and pick up to count entrants
            Collections.shuffle(entrants);
            List<String> selected = new ArrayList<>(entrants.subList(0, Math.min(count, entrants.size())));

            Log.d(TAG, "Drawn " + selected.size() + " selected entrants for event " + eventId);
            recordOutcome(eventId, selected);
            listener.onSuccess(selected);
        });
    }

    /**
     * Save the drawn entrants to Firestore.
     */
    public static void recordOutcome(String eventId, List<String> selected) {
        Map<String, Object> data = new HashMap<>();
        data.put("selected", selected);
        Database.save("selected_invites", eventId, data);
        Log.d(TAG, "Recorded selection outcome for event " + eventId);
    }

    /**
     * Load the list of previously selected entrants.
     */
    public static void loadSelected(String eventId, OnSuccessListener<List<String>> listener) {
        Database.get("selected_invites", eventId, document -> {
            if (eventId == null || eventId.trim().isEmpty()) {
                android.util.Log.e(TAG, "loadSelected: null/empty eventId");
                listener.onSuccess(new java.util.ArrayList<>());
                return;
            }

            if (document.exists()) {
                List<String> selected = (List<String>) document.get("selected");
                if (selected == null) selected = new ArrayList<>();
                listener.onSuccess(selected);
                Log.d(TAG, "Loaded selected entrants for event " + eventId + ": " + selected.size());
            } else {
                listener.onSuccess(new ArrayList<>());
                Log.d(TAG, "No selected entrants found for event " + eventId);
            }
        });
    }
}
