package com.example.gainly_flow;

import android.util.Log;

import androidx.annotation.NonNull;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.EventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WaitingList {

    private static final String TAG = "WaitingList";
    private static final String COLLECTION_NAME = "waiting_lists";

    private String eventId;
    private List<Entrant> entrants;

    public WaitingList() {
        this.entrants = new ArrayList<>();
    }

    public WaitingList(String eventId) {
        this();
        this.eventId = eventId;
    }

    // --- Setters / Getters ---
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public List<Entrant> getEntrants() {
        return new ArrayList<>(entrants);
    }

    public int getCount() { return entrants.size(); }

    // --- Add / Remove Entrants ---
    public void addEntrant(@NonNull Entrant entrant, int capacity) {
        if (entrant == null || entrant.getId() == null || entrant.getId().trim().isEmpty()) return;

        for (Entrant e : entrants) {
            if (e.getId().equals(entrant.getId())) return;
        }

        if (capacity > 0 && entrants.size() >= capacity) return;

        entrants.add(entrant);
        save(() -> {});
    }

    public void removeEntrant(@NonNull Entrant entrant) {
        if (entrant == null) return;
        entrants.removeIf(e -> e.getId().equals(entrant.getId()));
        save(() -> {});
    }

    // --- Firestore Integration ---
    public void fromDocument(@NonNull DocumentSnapshot doc) {
        this.eventId = doc.getId();
        List<String> ids = (List<String>) doc.get("entrantIds");
        this.entrants = new ArrayList<>();
        if (ids != null) {
            for (String id : ids) {
                this.entrants.add(new Entrant(id));
            }
        }
    }

    public void save(@NonNull Runnable onComplete) {
        if (eventId == null || eventId.trim().isEmpty()) return;

        Map<String, Object> data = new HashMap<>();
        List<String> ids = new ArrayList<>();
        for (Entrant e : entrants) ids.add(e.getId());
        data.put("entrantIds", ids);
        data.put("size", ids.size());

        FirebaseFirestore.getInstance()
                .collection("waiting_lists")
                .document(eventId)
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Waiting list saved for event " + eventId);
                    onComplete.run();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save waiting list for event " + eventId, e);
                    onComplete.run();
                });
    }

    // Optional: keep old save() for internal calls
    public void save() { save(() -> {}); }




    /**
     * Load the waiting list from Firebase asynchronously.
     * @param callback A listener that returns the loaded WaitingList.
     */
    public void loadFromFirebase(@NonNull WaitingListCallback callback) {
        if (eventId == null || eventId.trim().isEmpty()) {
            callback.onComplete(new WaitingList());
            return;
        }

        FirebaseFirestore.getInstance()
                .collection(COLLECTION_NAME)
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        fromDocument(doc);
                    } else {
                        entrants = new ArrayList<>();
                    }
                    callback.onComplete(this);
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    callback.onComplete(new WaitingList());
                });
    }

    @NonNull
    @Override
    public String toString() {
        return "WaitingList{" +
                "eventId='" + eventId + '\'' +
                ", entrants=" + entrants.size() +
                '}';
    }

    // --- Callback Interface ---
    public interface WaitingListCallback {
        void onComplete(WaitingList waitingList);
    }
}
