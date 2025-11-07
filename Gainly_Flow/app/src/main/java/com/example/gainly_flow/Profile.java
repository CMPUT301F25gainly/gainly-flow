package com.example.gainly_flow;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.Date;

/**
 * Represents a user profile within the Gainly Flow system.
 * This class can represent an Entrant, Organizer, or Administrator user.
 * <p>
 * Each profile stores essential identification and contact information,
 * including a unique ID, display name, email, phone number, role, and creation timestamp.
 * </p>
 *
 * <p>
 * This class implements {@link Serializable} so it can be passed between activities
 * via Intents or saved in persistent storage.
 * </p>
 */
public class Profile implements Serializable {

    /** Unique identifier for the user (e.g., Firebase Auth UID). */
    private String id;

    /** The display name of the user. */
    private String displayName;

    /** The email address associated with the user. */
    private String email;

    /** The user's phone number (optional). */
    private String phone;

    /** The role of the user, such as "Entrant", "Organizer", or "Admin". */
    private String role;

    /** The date when the profile was created. */
    private Date createdAt;

    /**
     * Default constructor required for Firebase deserialization.
     */
    public Profile() {}

    /**
     * Constructs a profile with basic user information.
     *
     * @param id           the unique user ID
     * @param displayName  the display name of the user
     * @param email        the user's email address
     */
    public Profile(String id, String displayName, String email) {
        this.id = id;
        this.displayName = displayName;
        this.email = email;
    }

    /**
     * Constructs a complete user profile with all details.
     *
     * @param id           the unique user ID
     * @param displayName  the display name of the user
     * @param email        the user's email address
     * @param phone        the user's phone number
     * @param role         the user's role (e.g., "Entrant", "Organizer", "Admin")
     * @param createdAt    the timestamp indicating when the profile was created
     */
    public Profile(String id, String displayName, String email, String phone, String role, Date createdAt) {
        this.id = id;
        this.displayName = displayName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.createdAt = createdAt;
    }

    // --- Getters ---

    /**
     * Returns the unique identifier of the user.
     *
     * @return the user ID
     */
    public String getId() { return id; }

    /**
     * Returns the display name of the user.
     *
     * @return the user's display name
     */
    public String getDisplayName() { return displayName; }

    /**
     * Returns the email address of the user.
     *
     * @return the user's email address
     */
    public String getEmail() { return email; }

    /**
     * Returns the user's phone number.
     *
     * @return the phone number, or {@code null} if not provided
     */
    public String getPhone() { return phone; }

    /**
     * Returns the user's role within the system.
     *
     * @return the user's role (e.g., "Entrant", "Organizer", "Admin")
     */
    public String getRole() { return role; }

    /**
     * Returns the date when the profile was created.
     *
     * @return the creation date
     */
    public Date getCreatedAt() { return createdAt; }

    // --- Setters ---

    /**
     * Sets the unique identifier of the user.
     *
     * @param id the new user ID
     */
    public void setId(String id) { this.id = id; }

    /**
     * Sets the display name of the user.
     *
     * @param displayName the new display name
     */
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    /**
     * Sets the email address of the user.
     *
     * @param email the new email address
     */
    public void setEmail(String email) { this.email = email; }

    /**
     * Sets the phone number of the user.
     *
     * @param phone the new phone number
     */
    public void setPhone(String phone) { this.phone = phone; }

    /**
     * Sets the role of the user.
     *
     * @param role the new role (e.g., "Entrant", "Organizer", "Admin")
     */
    public void setRole(String role) { this.role = role; }

    /**
     * Sets the creation date of the profile.
     *
     * @param createdAt the new creation date
     */
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    /**
     * Returns a string representation of the profile in a readable format.
     * The result includes the display name, email, and role when available.
     *
     * @return a human-readable string describing the user
     */
    @NonNull
    @Override
    public String toString() {
        String line1 = displayName == null ? "(No name)" : displayName;
        String line2 = (email == null || email.isEmpty()) ? "" : " • " + email;
        String line3 = (role == null || role.isEmpty()) ? "" : " (" + role + ")";
        return line1 + line2 + line3;
    }
}
