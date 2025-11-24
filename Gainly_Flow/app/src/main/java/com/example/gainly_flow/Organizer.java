package com.example.gainly_flow;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an Organizer.
 * Organizers can create events and manage entrants.
 */
@IgnoreExtraProperties
public class Organizer extends Profile {

    /** List of event IDs created by this organizer. */
    private List<String> createdEvents;

    /** List of active events */
    private List<String> activeEvents;

    /** Required empty constructor for Firebase. */
    public Organizer() {
        super();
        this.setRole("Organizer");
        this.createdEvents = new ArrayList<>();
        this.activeEvents = new ArrayList<>();
    }

    public Organizer(String id, String name, String email, String phone) {
        super(id, name, email, phone, "Organizer", null);
        this.createdEvents = new ArrayList<>();
        this.activeEvents = new ArrayList<>();
    }

    /** Minimal constructor */
    public Organizer(String id) {
        super(id, "Organizer_" + id.substring(0, 8), "");
        this.setRole("Organizer");
        this.createdEvents = new ArrayList<>();
        this.activeEvents = new ArrayList<>();
    }

    public List<String> getCreatedEvents() {
        return createdEvents;
    }

    public void setCreatedEvents(List<String> createdEvents) {
        this.createdEvents = createdEvents != null ? createdEvents : new ArrayList<>();
    }

    public List<String> getActiveEvents() {
        return activeEvents;
    }

    public void setActiveEvents(List<String> activeEvents) {
        this.activeEvents = activeEvents != null ? activeEvents : new ArrayList<>();
    }

    public void addCreatedEvent(String eventId) {
        if (eventId != null && !createdEvents.contains(eventId)) {
            createdEvents.add(eventId);
        }
    }

    public void removeCreatedEvent(String eventId) {
        createdEvents.remove(eventId);
        activeEvents.remove(eventId);
    }

    @Exclude
    public int getEventCount() {
        return createdEvents.size();
    }

    @Exclude
    public int getActiveEventCount() {
        return activeEvents.size();
    }

    public void addActiveEvent(String eventId) {
        if (eventId != null && !activeEvents.contains(eventId)) {
            activeEvents.add(eventId);
        }
    }

    public void removeActiveEvent(String eventId) {
        activeEvents.remove(eventId);
    }

    @Exclude
    public boolean hasEvent(String eventId) {
        return createdEvents.contains(eventId);
    }

    @Exclude
    public boolean hasActiveEvent(String eventId) {
        return activeEvents.contains(eventId);
    }
}