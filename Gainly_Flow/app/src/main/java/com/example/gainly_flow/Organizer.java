package com.example.gainly_flow;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an Organizer.
 * Organizers can create events and manage entrants.
 */
public class Organizer extends Profile {

    /** List of event IDs created by this organizer. */
    private List<String> createdEvents;

    /** Required empty constructor for Firebase. */
    public Organizer() {
        super();
        this.setRole("Organizer");
        this.createdEvents = new ArrayList<>();
    }

    public Organizer(String id, String name, String email, String phone) {
        super(id, name, email);
        this.setPhone(phone);
        this.setRole("Organizer");
        this.createdEvents = new ArrayList<>();
    }

    public List<String> getCreatedEvents() {
        return createdEvents;
    }

    public void addCreatedEvent(String eventId) {
        createdEvents.add(eventId);
    }
}
