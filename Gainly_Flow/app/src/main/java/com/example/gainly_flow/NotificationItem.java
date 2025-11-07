package com.example.gainly_flow;

import com.google.firebase.firestore.DocumentSnapshot;
import java.util.Date;

/**
 * {@code NotificationItem} represents a single notification entity in the Gainly Flow app.
 * Each notification includes a title, description, associated event ID, timestamp, and
 * information about whether user action (such as accept or decline) is required.
 *
 * <p>This class is designed to be easily constructed from a Firebase Firestore
 * {@link DocumentSnapshot}, allowing seamless integration between the Firestore database
 * and the app's UI components (e.g., {@link NotificationAdapter}).</p>
 */
public class NotificationItem {

    /** The unique identifier of the notification document in Firestore. */
    private String id;

    /** The title or heading of the notification. */
    private String title;

    /** A descriptive message providing more context about the notification. */
    private String description;

    /** The ID of the related event, if applicable. */
    private String eventId;

    /** The timestamp indicating when the notification was created or sent. */
    private Date timestamp;

    /** Indicates whether the notification requires user action (e.g., accept/decline). */
    private boolean actionRequired;

    /**
     * Default no-argument constructor required by Firebase for data deserialization.
     */
    public NotificationItem() {}

    /**
     * Constructs a {@code NotificationItem} object from a Firestore {@link DocumentSnapshot}.
     *
     * <p>This constructor safely extracts known fields such as "title", "message",
     * "eventId", "timestamp", and "type" from the Firestore document. Missing fields
     * are handled gracefully by assigning default values.</p>
     *
     * @param doc the Firestore {@link DocumentSnapshot} containing the notification data
     */
    public NotificationItem(DocumentSnapshot doc) {
        this.id = doc.getId();
        this.title = doc.contains("title") ? doc.getString("title") : "No Title";
        this.description = doc.contains("message") ? doc.getString("message") : "";
        this.eventId = doc.contains("eventId") ? doc.getString("eventId") : null;

        Long ts = doc.contains("timestamp") ? doc.getLong("timestamp") : null;
        this.timestamp = ts != null ? new Date(ts) : new Date();

        String type = doc.contains("type") ? doc.getString("type") : "";
        this.actionRequired = "WIN".equals(type); // Only "WIN" notifications require user action
    }

    /**
     * Returns the unique ID of the notification.
     *
     * @return the notification ID
     */
    public String getId() { return id; }

    /**
     * Returns the title of the notification.
     *
     * @return the notification title
     */
    public String getTitle() { return title; }

    /**
     * Returns the descriptive message of the notification.
     *
     * @return the notification description
     */
    public String getDescription() { return description; }

    /**
     * Returns the event ID associated with this notification, if any.
     *
     * @return the event ID, or {@code null} if not associated with any event
     */
    public String getEventId() { return eventId; }

    /**
     * Returns the timestamp representing when the notification was created or sent.
     *
     * @return the notification timestamp
     */
    public Date getTimestamp() { return timestamp; }

    /**
     * Returns whether this notification requires user interaction (e.g., Accept or Decline).
     *
     * @return {@code true} if the notification requires an action; {@code false} otherwise
     */
    public boolean isActionRequired() { return actionRequired; }

    /**
     * Sets the unique ID of the notification.
     *
     * @param id the new notification ID
     */
    public void setId(String id) { this.id = id; }

    /**
     * Sets the title of the notification.
     *
     * @param title the new title
     */
    public void setTitle(String title) { this.title = title; }

    /**
     * Sets the description or message of the notification.
     *
     * @param description the new description
     */
    public void setDescription(String description) { this.description = description; }

    /**
     * Sets the event ID associated with this notification.
     *
     * @param eventId the event ID
     */
    public void setEventId(String eventId) { this.eventId = eventId; }

    /**
     * Sets the timestamp of the notification.
     *
     * @param timestamp the timestamp as a {@link Date} object
     */
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    /**
     * Sets whether this notification requires user interaction.
     *
     * @param actionRequired {@code true} if user action is required; {@code false} otherwise
     */
    public void setActionRequired(boolean actionRequired) { this.actionRequired = actionRequired; }
}
