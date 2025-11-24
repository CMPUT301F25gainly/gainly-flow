package com.example.gainly_flow;

import androidx.annotation.NonNull;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import java.io.Serializable;
import java.util.Date;

/**
 * Represents a user profile within the Gainly Flow system.
 * This class can represent an Entrant, Organizer, or Administrator user.
 * Uses @IgnoreExtraProperties for Firestore to ignore unknown fields during deserialization.
 */
@IgnoreExtraProperties
public class Profile implements Serializable {

    private String id;
    private String displayName;
    private String email;
    private String phone;
    private String role;
    private Date createdAt;
    private Date lastLoginAt;
    private String deviceId;

    /** User preference: whether the user wants to receive notifications. */
    private boolean receiveNotifications = true;

    /** User preference: whether the user allows location services. */
    private boolean enableLocationService = false;

    /** Default constructor required for Firebase. */
    public Profile() {
        this.createdAt = new Date();
        this.lastLoginAt = new Date();
    }

    /** Minimal constructor for new profiles */
    public Profile(String id, String displayName, String email) {
        this.id = id;
        this.displayName = displayName;
        this.email = email;
        this.createdAt = new Date();
        this.lastLoginAt = new Date();
        this.deviceId = id; // For new profiles, device ID is the profile ID
    }

    /** Full constructor */
    public Profile(String id, String displayName, String email, String phone, String role, Date createdAt) {
        this.id = id;
        this.displayName = displayName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.createdAt = createdAt != null ? createdAt : new Date();
        this.lastLoginAt = new Date();
        this.deviceId = id;
    }

    /** Constructor with device ID */
    public Profile(String id, String displayName, String email, String phone, String role, Date createdAt, String deviceId) {
        this.id = id;
        this.displayName = displayName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.createdAt = createdAt != null ? createdAt : new Date();
        this.lastLoginAt = new Date();
        this.deviceId = deviceId;
    }

    // --- Getters ---

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getRole() { return role; }
    public Date getCreatedAt() { return createdAt; }
    public Date getLastLoginAt() { return lastLoginAt; }
    public String getDeviceId() { return deviceId; }

    /** Returns whether the user wants to receive notifications. */
    public boolean isReceiveNotifications() { return receiveNotifications; }

    /** Returns whether the user has enabled location services. */
    public boolean isEnableLocationService() { return enableLocationService; }

    // --- Setters ---

    public void setId(String id) { this.id = id; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setRole(String role) { this.role = role; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public void setLastLoginAt(Date lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    /** Sets whether the user wants to receive notifications. */
    public void setReceiveNotifications(boolean receiveNotifications) {
        this.receiveNotifications = receiveNotifications;
    }

    /** Sets whether the user allows location services. */
    public void setEnableLocationService(boolean enableLocationService) {
        this.enableLocationService = enableLocationService;
    }

    // --- Firestore Helper Methods ---

    /**
     * Gets the Firestore document ID for this profile.
     * Uses deviceId as the document ID for easy lookup.
     */
    @Exclude
    public String getDocumentId() {
        return deviceId != null ? deviceId : id;
    }

    /**
     * Updates the last login timestamp to current time.
     */
    public void updateLastLogin() {
        this.lastLoginAt = new Date();
    }

    /**
     * Checks if this profile is complete with essential information.
     */
    @Exclude
    public boolean isProfileComplete() {
        return displayName != null && !displayName.trim().isEmpty() &&
                email != null && !email.trim().isEmpty() &&
                role != null && !role.trim().isEmpty();
    }

    /**
     * Returns a display-friendly version of the role.
     */
    @Exclude
    public String getDisplayRole() {
        if (role == null) return "User";
        switch (role.toLowerCase()) {
            case "entrant": return "Entrant";
            case "organizer": return "Organizer";
            case "admin": return "Administrator";
            default: return role;
        }
    }

    @Exclude
    public boolean hasRole(String role) {
        if (role == null || this.role == null) return false;
        return this.role.equalsIgnoreCase(role);
    }

    /**
     * Returns true if this profile can act as organizer
     */
    @Exclude
    public boolean isOrganizer() {
        return hasRole("Organizer") || hasRole("Admin");
    }

    /**
     * Returns true if this profile can act as entrant
     */
    @Exclude
    public boolean isEntrant() {
        return hasRole("Entrant") || hasRole("Organizer") || hasRole("Admin");
    }

    /**
     * Creates a device-specific profile ID combining device ID and role
     */
    @Exclude
    public static String generateProfileId(String deviceId, String role) {
        return deviceId + "_" + role.toLowerCase();
    }

    @NonNull
    @Override
    public String toString() {
        String line1 = displayName == null ? "(No name)" : displayName;
        String line2 = (email == null || email.isEmpty()) ? "" : " • " + email;
        String line3 = (role == null || role.isEmpty()) ? "" : " (" + getDisplayRole() + ")";
        return line1 + line2 + line3;
    }

    /**
     * Creates a copy of this profile.
     */
    @Exclude
    public Profile copy() {
        Profile copy = new Profile();
        copy.id = this.id;
        copy.displayName = this.displayName;
        copy.email = this.email;
        copy.phone = this.phone;
        copy.role = this.role;
        copy.createdAt = this.createdAt != null ? new Date(this.createdAt.getTime()) : null;
        copy.lastLoginAt = this.lastLoginAt != null ? new Date(this.lastLoginAt.getTime()) : null;
        copy.deviceId = this.deviceId;
        copy.receiveNotifications = this.receiveNotifications;
        copy.enableLocationService = this.enableLocationService;
        return copy;
    }

    /**
     * Compares two profiles for equality based on ID and device ID.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Profile profile = (Profile) o;

        if (id != null ? !id.equals(profile.id) : profile.id != null) return false;
        return deviceId != null ? deviceId.equals(profile.deviceId) : profile.deviceId == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (deviceId != null ? deviceId.hashCode() : 0);
        return result;
    }

}