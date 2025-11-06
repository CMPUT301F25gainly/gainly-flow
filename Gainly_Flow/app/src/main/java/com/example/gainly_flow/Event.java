package com.example.gainly_flow;

import android.util.Log;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an event organized in the system.
 * Stored under collection "events" in Firestore.
 */
public class Event {
    private static final String TAG = "Event";

    private String eventId;
    private String name;
    private String description;
    private String organizerId;
    private int capacity; // 0 = unlimited
    private long registrationOpen;
    private long registrationClose;

    public Event() {}

    public Event(String eventId, String name, String description, String organizerId,
                 int capacity, long registrationOpen, long registrationClose) {
        this.eventId = eventId;
        this.name = name;
        this.description = description;
        this.organizerId = organizerId;
        this.capacity = capacity;
        this.registrationOpen = registrationOpen;
        this.registrationClose = registrationClose;
    }

    public Event(String eventId, String name, String description, String organizerId) {
        this.eventId = eventId;
        this.name = name;
        this.description = description;
        this.organizerId = organizerId;
    }

    // -------------------------------
    // Firestore Operations
    // -------------------------------

    /**
     * Load this event from Firestore asynchronously.
     */
    public void load(String eventId, OnSuccessListener<Event> listener) {
        Database.get("events", eventId, document -> {
            if (document.exists()) {
                fromDocument(document);
                Log.d(TAG, "Loaded event " + eventId);
            } else {
                Log.w(TAG, "Event not found: " + eventId);
                this.eventId = eventId;   // <-- keep requested id to avoid nulls
            }
            listener.onSuccess(this);
        });
    }

    /**
     * Save (create or update) this event in Firestore.
     */
    public void save() {
        Database.save("events", eventId, toMap());
    }

    /**
     * Update capacity of the event.
     * 0 = unlimited waiting list size
     */
    public void setCapacity(int capacity) {
        this.capacity = Math.max(0, capacity);
        Map<String, Object> update = new HashMap<>();
        update.put("capacity", this.capacity);
        Database.update("events", eventId, update);
        Log.d(TAG, "Set capacity for event " + eventId + " to " + capacity);
    }

    // -------------------------------
    // Getters and Helpers
    // -------------------------------

    public int getCapacity() {
        return capacity;
    }

    public String getEventId() {
        return eventId;
    }

    public String getId() {
        return eventId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isRegistrationOpen() {
        long now = System.currentTimeMillis();
        return now >= registrationOpen && now <= registrationClose;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("description", description);
        map.put("organizerId", organizerId);
        map.put("capacity", capacity);
        map.put("registrationOpen", registrationOpen);
        map.put("registrationClose", registrationClose);
        return map;
    }

    private void fromDocument(DocumentSnapshot doc) {
        this.eventId = doc.getId();
        this.name = doc.getString("name");
        this.description = doc.getString("description");
        this.organizerId = doc.getString("organizerId");
        Long cap = doc.getLong("capacity");
        this.capacity = cap != null ? cap.intValue() : 0;
        this.registrationOpen = doc.getLong("registrationOpen") != null ? doc.getLong("registrationOpen") : 0;
        this.registrationClose = doc.getLong("registrationClose") != null ? doc.getLong("registrationClose") : 0;
    }
}
