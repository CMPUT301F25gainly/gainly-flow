package com.example.gainly_flow;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an Entrant in the system.
 * Entrants can join waiting lists, accept/decline invitations,
 * receive notifications, and view history.
 */
@IgnoreExtraProperties
public class Entrant extends Profile {

    /** History of events the entrant has interacted with (event IDs). */
    private List<String> eventHistory;

    /** List of notifications received by this entrant. */
    private List<NotificationItem> notifications;

    /** Current events where entrant is on waiting list */
    private List<String> currentWaitingLists;

    /** Events where entrant has been invited */
    private List<String> pendingInvitations;

    /** Required empty constructor for Firebase. */
    public Entrant() {
        super();
        this.setRole("Entrant");
        this.eventHistory = new ArrayList<>();
        this.notifications = new ArrayList<>();
        this.currentWaitingLists = new ArrayList<>();
        this.pendingInvitations = new ArrayList<>();
    }

    public Entrant(String id, String name, String email, String phone) {
        super(id, name, email, phone, "Entrant", null);
        this.eventHistory = new ArrayList<>();
        this.notifications = new ArrayList<>();
        this.currentWaitingLists = new ArrayList<>();
        this.pendingInvitations = new ArrayList<>();
    }

    /** Minimal constructor */
    public Entrant(String id) {
        super(id, "User_" + id.substring(0, 8), "");
        this.setRole("Entrant");
        this.eventHistory = new ArrayList<>();
        this.notifications = new ArrayList<>();
        this.currentWaitingLists = new ArrayList<>();
        this.pendingInvitations = new ArrayList<>();
    }

    // -----------------------------
    // Event history management
    // -----------------------------
    public List<String> getEventHistory() {
        return eventHistory;
    }

    public void setEventHistory(List<String> eventHistory) {
        this.eventHistory = eventHistory != null ? eventHistory : new ArrayList<>();
    }

    public void addEventHistory(String eventId) {
        if (eventId != null && !eventHistory.contains(eventId)) {
            eventHistory.add(eventId);
        }
    }

    @Exclude
    public boolean hasEventInHistory(String eventId) {
        return eventHistory.contains(eventId);
    }

    // -----------------------------
    // Notification handling
    // -----------------------------
    public List<NotificationItem> getNotifications() {
        return notifications;
    }

    public void setNotifications(List<NotificationItem> notifications) {
        this.notifications = notifications != null ? notifications : new ArrayList<>();
    }

    public void addNotification(NotificationItem notification) {
        if (notification != null) {
            notifications.add(notification);
        }
    }

    public void clearNotifications() {
        notifications.clear();
    }

    @Exclude
    public int getUnreadNotificationCount() {
        int count = 0;
        for (NotificationItem notification : notifications) {
            if (!notification.isRead()) {
                count++;
            }
        }
        return count;
    }

    // -----------------------------
    // Waiting list management
    // -----------------------------
    public List<String> getCurrentWaitingLists() {
        return currentWaitingLists;
    }

    public void setCurrentWaitingLists(List<String> currentWaitingLists) {
        this.currentWaitingLists = currentWaitingLists != null ? currentWaitingLists : new ArrayList<>();
    }

    public void joinWaitingList(String eventId) {
        if (eventId != null && !currentWaitingLists.contains(eventId)) {
            currentWaitingLists.add(eventId);
        }
    }

    public void leaveWaitingList(String eventId) {
        currentWaitingLists.remove(eventId);
    }

    @Exclude
    public boolean isOnWaitingList(String eventId) {
        return currentWaitingLists.contains(eventId);
    }

    // -----------------------------
    // Invitation management
    // -----------------------------
    public List<String> getPendingInvitations() {
        return pendingInvitations;
    }

    public void setPendingInvitations(List<String> pendingInvitations) {
        this.pendingInvitations = pendingInvitations != null ? pendingInvitations : new ArrayList<>();
    }

    public void addPendingInvitation(String eventId) {
        if (eventId != null && !pendingInvitations.contains(eventId)) {
            pendingInvitations.add(eventId);
        }
    }

    public void removePendingInvitation(String eventId) {
        pendingInvitations.remove(eventId);
    }

    @Exclude
    public boolean hasPendingInvitation(String eventId) {
        return pendingInvitations.contains(eventId);
    }
}