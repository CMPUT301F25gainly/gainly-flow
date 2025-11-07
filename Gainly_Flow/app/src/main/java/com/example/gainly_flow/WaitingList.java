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
 //add
    /**
     * Load the waiting list for an event from Firestore asynchronously.
     */
    public void load(com.google.android.gms.tasks.OnSuccessListener<WaitingList> listener) {
        if (eventId == null || eventId.trim().isEmpty()) {
            android.util.Log.e(TAG, "WaitingList.load called with null/empty eventId");
            listener.onSuccess(this);
            return;
        }

        // First try docId == eventId (your current convention)
        Database.get("waiting_lists", eventId, document -> {
            if (document != null && document.exists()) {
                applyFromDocument(document);
                listener.onSuccess(this);
                return;
            }

            // Fallback: random docId with eventId as a FIELD (your dummy doc shape)
            Database.findOne("waiting_lists", "eventId", eventId, altDoc -> {
                if (altDoc != null && altDoc.exists()) {
                    applyFromDocument(altDoc);
                } else {
                    android.util.Log.d(TAG, "No waiting_list found for eventId=" + eventId);
                    this.entrantList = new java.util.ArrayList<>();
                }
                listener.onSuccess(this);
            });
        });
    }

    // Keep this small parsing helper private to avoid API churn
    private void applyFromDocument(@androidx.annotation.NonNull com.google.firebase.firestore.DocumentSnapshot doc) {
        java.util.List<String> ids = (java.util.List<String>) doc.get("entrantIds");
        if (ids == null) ids = new java.util.ArrayList<>();
        this.entrantList = new java.util.ArrayList<>(ids);
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
    // WaitingList.java -> save()
    private void save() {
        Map<String, Object> data = new HashMap<>();
        data.put("entrantIds", entrantList);     // <-- match your Firestore docs
        data.put("size", entrantList.size());    // optional but consistent with your docs
        Database.save("waiting_lists", eventId, data);
    }


}
