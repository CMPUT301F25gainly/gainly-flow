package com.example.gainly_flow;

public class Organizer extends User {
    public Organizer(String id, Profile profile) { super(id, profile); }

    @Override public String getRole() { return "Organizer"; }

    public String createEvent() { return ""; }
    public void updateEvent(String eventId) {}
    public void sendNotificationToWaitingList(String eventId, String message) {}
    public void setGeolocationRequired(String eventId, boolean required) {}
}
