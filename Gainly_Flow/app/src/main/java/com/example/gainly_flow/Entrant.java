package com.example.gainly_flow;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an Entrant in the system.
 * Entrants can join waiting lists, accept/decline invitations,
 * receive notifications, and view history.
 */
public class Entrant extends Profile {

    /** History of events the entrant has interacted with (event IDs). */
    private List<String> eventHistory;

    /** List of notifications received by this entrant. */
    private List<NotificationItem> notifications;

    /** Required empty constructor for Firebase. */
    public Entrant() {
        super();
        this.setRole("Entrant");
        this.eventHistory = new ArrayList<>();
        this.notifications = new ArrayList<>();
    }

    public Entrant(String id, String name, String email, String phone) {
        super(id, name, email);
        this.setPhone(phone);
        this.setRole("Entrant");
        this.eventHistory = new ArrayList<>();
        this.notifications = new ArrayList<>();
    }

    /** Minimal constructor */
    public Entrant(String id) {
        super(id, "", "");
        this.setRole("Entrant");
        this.eventHistory = new ArrayList<>();
        this.notifications = new ArrayList<>();
    }

    // -----------------------------
    // Event history management
    // -----------------------------
    public List<String> getEventHistory() {
        return eventHistory;
    }

    public void addEventHistory(String eventId) {
        if (!eventHistory.contains(eventId)) {
            eventHistory.add(eventId);
        }
    }

    // -----------------------------
    // Notification handling
    // -----------------------------
    public List<NotificationItem> getNotifications() {
        return notifications;
    }

    public void addNotification(NotificationItem notification) {
        if (notification != null) {
            notifications.add(notification);
        }
    }

    public void clearNotifications() {
        notifications.clear();
    }
}
