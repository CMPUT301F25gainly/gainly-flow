package com.example.gainly_flow;

public class Profile {
    private final String role;
    private final String id;
    private final String displayName;
    private final String email;

    public Profile(String id, String displayName, String email, String role) {
        this.id = id; this.displayName = displayName; this.email = email; this.role = role;
    }

    public String getId() { return id; }
    public String getRole() { return role; }

    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }

    @Override public String toString() {
        return displayName + (email == null ? "" : " • " + email);
    }
}

