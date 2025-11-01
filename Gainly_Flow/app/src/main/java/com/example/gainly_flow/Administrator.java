package com.example.gainly_flow;

public class Administrator extends User {
    public Administrator(String id, Profile profile) { super(id, profile); }

    @Override public String getRole() { return "Administrator"; }

    public void removeEvent(String eventId) {}
    public void removeProfile(String userId) {}
    public void removeImage(String imageId) {}
    public void removeOrganizer(String organizerId) {}
    public void reviewNotificationLogs() {}
}

