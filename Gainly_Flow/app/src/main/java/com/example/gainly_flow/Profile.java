package com.example.gainly_flow;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.Date;

/**
 * Represents a user profile within the Gainly Flow system.
 * Can be used for Entrants, Organizers, or Administrators.
 */
public class Profile implements Serializable {
    private String id;
    private String displayName;
    private String email;
    private String phone;
    private String role; // e.g. "Entrant", "Organizer", "Admin"
    private Date createdAt;

    // Empty constructor required for Firebase deserialization
    public Profile() {}

    public Profile(String id, String displayName, String email) {
        this.id = id;
        this.displayName = displayName;
        this.email = email;
    }

    public Profile(String id, String displayName, String email, String phone, String role, Date createdAt) {
        this.id = id;
        this.displayName = displayName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.createdAt = createdAt;
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getRole() { return role; }
    public Date getCreatedAt() { return createdAt; }

    // --- Setters (needed if loaded from Firebase) ---
    public void setId(String id) { this.id = id; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setRole(String role) { this.role = role; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    @NonNull
    @Override
    public String toString() {
        String line1 = displayName == null ? "(No name)" : displayName;
        String line2 = (email == null || email.isEmpty()) ? "" : " • " + email;
        String line3 = (role == null || role.isEmpty()) ? "" : " (" + role + ")";
        return line1 + line2 + line3;
    }
}
