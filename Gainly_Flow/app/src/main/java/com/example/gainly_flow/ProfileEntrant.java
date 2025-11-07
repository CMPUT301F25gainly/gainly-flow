package com.example.gainly_flow;

public class ProfileEntrant {  // Make sure it says ProfileEntrant, not Profile
    private String id;
    private String displayName;
    private String email;
    private String phoneNumber;

    public ProfileEntrant() {
    }

    public ProfileEntrant(String id, String displayName, String email) {
        this.id = id;
        this.displayName = displayName;
        this.email = email;
    }

    public ProfileEntrant(String id, String displayName, String email, String phoneNumber) {
        this.id = id;
        this.displayName = displayName;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    // All your getters and setters stay the same
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }

    public void setId(String id) { this.id = id; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setEmail(String email) { this.email = email; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    @Override
    public String toString() {
        return displayName + (email == null ? "" : " • " + email);
    }
}