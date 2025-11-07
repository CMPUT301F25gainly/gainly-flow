package com.example.gainly_flow;

/**
 * Represents an abstract base class for all user types in the Gainly Flow system.
 * <p>
 * Each user is identified by a unique ID and has an associated {@link Profile} object
 * that stores their personal details such as name, email, and role.
 * Concrete subclasses (e.g., Entrant, Organizer, or Admin) must implement the
 * {@link #getRole()} method to specify their user role.
 * </p>
 */
public abstract class User {

    /** Unique identifier for the user (e.g., Firebase UID). */
    protected String id;

    /** The profile containing the user's personal and role-related information. */
    protected Profile profile;

    /**
     * Constructs a new {@code User} with the specified ID and profile.
     *
     * @param id the unique identifier of the user
     * @param profile the {@link Profile} object containing user information
     */
    public User(String id, Profile profile) {
        this.id = id;
        this.profile = profile;
    }

    /**
     * Returns the unique identifier of the user.
     *
     * @return the user ID
     */
    public String getId() { return id; }

    /**
     * Returns the profile associated with this user.
     *
     * @return the {@link Profile} object containing user details
     */
    public Profile getProfile() { return profile; }

    /**
     * Returns the role of the user (e.g., "Entrant", "Organizer", or "Admin").
     * <p>
     * Each subclass must override this method to define its specific role.
     * </p>
     *
     * @return a string representing the user's role
     */
    public abstract String getRole();
}
