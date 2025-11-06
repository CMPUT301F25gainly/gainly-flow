package com.example.gainly_flow;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the waiting list for a specific event.
 * Each waiting list is stored in Firestore under collection "waiting_lists"
 * with document ID equal to the eventId.
 */
public class WaitingList {
    private static final String TAG = "waiting_list";

    private String eventId;
    private List<String> entrantList = new ArrayList<>();

    public WaitingList(String eventId) {
        this.eventId = eventId;
    }

    /**
     * Load the waiting list for an event from Firestore asynchronously.
     */
    public void load(com.google.android.gms.tasks.OnSuccessListener<WaitingList> listener) {
        if (eventId == null || eventId.trim().isEmpty()) {
            android.util.Log.e(TAG, "WaitingList.load called with null/empty eventId");
            listener.onSuccess(this);
            return;
        }
        Database.get("waiting_lists", eventId, document -> {
            if (document.exists()) {
                java.util.List<String> list = (java.util.List<String>) document.get("entrants");
                entrantList = (list != null) ? new java.util.ArrayList<>(list) : new java.util.ArrayList<>();
            }
            listener.onSuccess(this);
        });
    }


    /**
     * Add a new entrant to the waiting list.
     */
    public void addEntrant(String entrantId, int capacity) {
        if (entrantList.contains(entrantId)) {
            Log.w(TAG, "Entrant already exists in waiting list: " + entrantId);
            return;
        }

        if (capacity > 0 && entrantList.size() >= capacity) {
            Log.w(TAG, "Waiting list full for event " + eventId + " (maxCapacity: " + capacity + ")");
            return;
        }

        entrantList.add(entrantId);
        save();
        Log.d(TAG, "Added entrant " + entrantId + " to waiting list for " + eventId);
    }

    /**
     * Remove an entrant from the waiting list.
     */
    public void removeEntrant(String entrantId) {
        if (!entrantList.contains(entrantId)) {
            Log.w(TAG, "Entrant not found in waiting list: " + entrantId);
            return;
        }
        entrantList.remove(entrantId);
        save();
        Log.d(TAG, "Removed entrant " + entrantId + " from waiting list for " + eventId);
    }

    /**
     * Get the list of entrant IDs.
     */
    public List<String> getEntrants() {
        return new ArrayList<>(entrantList);
    }

    /**
     * Get count of entrants.
     */
    public int getCount() {
        return entrantList.size();
    }

    /**
     * Persist the waiting list to Firestore.
     */
    private void save() {
        Map<String, Object> data = new HashMap<>();
        data.put("entrants", entrantList);
        Database.save("waiting_lists", eventId, data);
    }
}
