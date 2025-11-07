package com.example.gainly_flow;

/**
 * Represents an entrant's profile in the Gainly Flow system.
 * <p>
 * This class stores essential user information such as ID, display name,
 * email, and phone number. It is primarily used for entrant-specific
 * profile handling and display purposes.
 * </p>
 */
public class ProfileEntrant {

    /** Unique identifier for the entrant. */
    private String id;

    /** Display name of the entrant (e.g., full name or username). */
    private String displayName;

    /** Email address associated with the entrant. */
    private String email;

    /** Phone number of the entrant. */
    private String phoneNumber;

    /**
     * Default no-argument constructor.
     * <p>
     * Required for deserialization and frameworks like Firebase.
     * </p>
     */
    public ProfileEntrant() {
    }

    /**
     * Constructs a {@code ProfileEntrant} with basic information.
     *
     * @param id           unique identifier for the entrant
     * @param displayName  display name of the entrant
     * @param email        email address of the entrant
     */
    public ProfileEntrant(String id, String displayName, String email) {
        this.id = id;
        this.displayName = displayName;
        this.email = email;
    }

    /**
     * Constructs a {@code ProfileEntrant} with full information including phone number.
     *
     * @param id           unique identifier for the entrant
     * @param displayName  display name of the entrant
     * @param email        email address of the entrant
     * @param phoneNumber  phone number of the entrant
     */
    public ProfileEntrant(String id, String displayName, String email, String phoneNumber) {
        this.id = id;
        this.displayName = displayName;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    /**
     * Returns the entrant's unique identifier.
     *
     * @return entrant ID
     */
    public String getId() { return id; }

    /**
     * Returns the entrant's display name.
     *
     * @return display name
     */
    public String getDisplayName() { return displayName; }

    /**
     * Returns the entrant's email address.
     *
     * @return email address
     */
    public String getEmail() { return email; }

    /**
     * Returns the entrant's phone number.
     *
     * @return phone number
     */
    public String getPhoneNumber() { return phoneNumber; }

    /**
     * Sets the entrant's unique identifier.
     *
     * @param id new entrant ID
     */
    public void setId(String id) { this.id = id; }

    /**
     * Sets the entrant's display name.
     *
     * @param displayName new display name
     */
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    /**
     * Sets the entrant's email address.
     *
     * @param email new email address
     */
    public void setEmail(String email) { this.email = email; }

    /**
     * Sets the entrant's phone number.
     *
     * @param phoneNumber new phone number
     */
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    /**
     * Returns a string representation of the entrant's profile.
     * <p>
     * The format is: {@code displayName • email} if email is available,
     * or just {@code displayName} otherwise.
     * </p>
     *
     * @return formatted string containing display name and email
     */
    @Override
    public String toString() {
        return displayName + (email == null ? "" : " • " + email);
    }
}
