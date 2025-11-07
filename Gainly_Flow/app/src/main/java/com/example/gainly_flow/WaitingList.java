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
 * Represents and manages a waiting list for a specific event in the Gainly Flow application.
 * <p>
 * Each waiting list corresponds to an event and is stored in the Firestore database
 * under the collection {@code "waiting_lists"} with a document ID matching the event's ID.
 * The waiting list maintains a list of entrant IDs that represent users
 * currently waiting to participate in the event.
 * </p>
 */
public class WaitingList {
    private static final String TAG = "waiting_list";

    /** Unique identifier for the event associated with this waiting list. */
    private String eventId;

    /** List of entrant IDs currently on the waiting list. */
    private List<String> entrantList = new ArrayList<>();

    /**
     * Constructs a {@code WaitingList} instance for the specified event.
     *
     * @param eventId the unique identifier of the event
     */
    public WaitingList(String eventId) {
        this.eventId = eventId;
    }

    /**
     * Loads the waiting list for the given event asynchronously from Firestore.
     * <p>
     * This method first attempts to retrieve the document using the event ID as the document ID.
     * If not found, it searches for a document where the {@code eventId} field matches.
     * Once the data is loaded (or determined to be empty), the provided listener is invoked.
     * </p>
     *
     * @param listener callback to receive the loaded {@code WaitingList} instance
     */
    public void load(OnSuccessListener<WaitingList> listener) {
        if (eventId == null || eventId.trim().isEmpty()) {
            Log.e(TAG, "WaitingList.load called with null/empty eventId");
            listener.onSuccess(this);
            return;
        }

        // Attempt to fetch the document with ID == eventId
        Database.get("waiting_lists", eventId, document -> {
            if (document != null && document.exists()) {
                applyFromDocument(document);
                listener.onSuccess(this);
                return;
            }

            // Fallback: search for a document with matching eventId field
            Database.findOne("waiting_lists", "eventId", eventId, altDoc -> {
                if (altDoc != null && altDoc.exists()) {
                    applyFromDocument(altDoc);
                } else {
                    Log.d(TAG, "No waiting_list found for eventId=" + eventId);
                    this.entrantList = new ArrayList<>();
                }
                listener.onSuccess(this);
            });
        });
    }

    /**
     * Parses Firestore document data and updates the local entrant list accordingly.
     *
     * @param doc the Firestore document containing waiting list data
     */
    private void applyFromDocument(@NonNull DocumentSnapshot doc) {
        List<String> ids = (List<String>) doc.get("entrantIds");
        if (ids == null) ids = new ArrayList<>();
        this.entrantList = new ArrayList<>(ids);
    }

    /**
     * Adds a new entrant to the waiting list and persists the change to Firestore.
     * <p>
     * If the entrant already exists in the list or the capacity is full, the method logs a warning
     * and does not modify the list.
     * </p>
     *
     * @param entrantId the ID of the entrant to be added
     * @param capacity  the maximum capacity of the waiting list (0 means unlimited)
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
     * Removes an entrant from the waiting list and updates Firestore accordingly.
     * <p>
     * If the entrant is not found, no changes are made and a warning is logged.
     * </p>
     *
     * @param entrantId the ID of the entrant to be removed
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
     * Retrieves a copy of the list of entrant IDs currently on the waiting list.
     *
     * @return a new {@link List} containing all entrant IDs
     */
    public List<String> getEntrants() {
        return new ArrayList<>(entrantList);
    }

    /**
     * Returns the number of entrants currently in the waiting list.
     *
     * @return the count of entrants
     */
    public int getCount() {
        return entrantList.size();
    }

    /**
     * Persists the current waiting list state to Firestore.
     * <p>
     * The document fields include:
     * <ul>
     *   <li>{@code entrantIds} — list of entrant IDs</li>
     *   <li>{@code size} — number of entrants</li>
     * </ul>
     * </p>
     */
    private void save() {
        Map<String, Object> data = new HashMap<>();
        data.put("entrantIds", entrantList);
        data.put("size", entrantList.size());
        Database.save("waiting_lists", eventId, data);
    }
}
