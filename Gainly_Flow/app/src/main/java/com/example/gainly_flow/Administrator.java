package com.example.gainly_flow;

public class Administrator extends User {
    private static Administrator instance; // static reference

    public Administrator(String id, Profile profile) {
        super(id, profile);
        instance = this; // store self as singleton
    }

    public static Administrator get() {
        if (instance == null) {
            // fallback placeholder instance
            instance = new Administrator("admin_id", new Profile("admin_id", "Admin", "admin@example.com"));
        }
        return instance;
    }

    @Override public String getRole() { return "Administrator"; }

    public void removeEvent(String eventId) {
        Database.get().removeEvent(eventId);
    }

    public void removeProfile(String userId) {
        Database.get().removeProfile(userId);
    }

    public void removeImage(String imageId) {}
    public void removeOrganizer(String organizerId) {}
    public void reviewNotificationLogs() {}
}
