package com.example.gainly_flow;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import java.io.Serializable;
import java.util.Date;

/**
 * Represents a notification for the User.
 */
@IgnoreExtraProperties
public class NotificationItem implements Serializable { // Add Serializable here
    private static final long serialVersionUID = 1L; // Add serialVersionUID

    private String id;
    private String title;
    private String description;
    private String message;
    private String eventId;
    private String eventName;
    private Date timestamp;
    private boolean actionRequired;
    private boolean isRead;
    private String type; // WIN, INFO, REMINDER, etc.
    private String recipientId; // User/device ID who should receive this notification

    // Notification types
    public enum NotificationType {
        WIN, INFO, REMINDER, UPDATE, CANCELLATION, INVITATION, LOSE
    }

    /** Default constructor required for Firebase */
    public NotificationItem() {
        this.timestamp = new Date();
        this.isRead = false;
        this.actionRequired = false;
    }

    /** Constructor from Firestore document */
    public NotificationItem(DocumentSnapshot doc) {
        this();
        this.id = doc.getId();
        this.title = doc.contains("title") ? doc.getString("title") : "No Title";
        this.description = doc.contains("description") ? doc.getString("description") : "";
        this.message = doc.contains("message") ? doc.getString("message") : "";
        this.eventId = doc.contains("eventId") ? doc.getString("eventId") : null;
        this.eventName = doc.contains("eventName") ? doc.getString("eventName") : null;
        this.recipientId = doc.contains("recipientId") ? doc.getString("recipientId") : null;
        this.type = doc.contains("type") ? doc.getString("type") : NotificationType.INFO.name();

        // Handle timestamp - can be Date object or Long timestamp
        if (doc.contains("timestamp")) {
            Object timestampObj = doc.get("timestamp");
            if (timestampObj instanceof Date) {
                this.timestamp = (Date) timestampObj;
            } else if (timestampObj instanceof Long) {
                this.timestamp = new Date((Long) timestampObj);
            }
        }

        // Determine if action is required based on type
        String notificationType = doc.contains("type") ? doc.getString("type") : "";
        this.actionRequired = NotificationType.WIN.name().equals(notificationType) ||
                NotificationType.INVITATION.name().equals(notificationType);

        this.isRead = doc.contains("isRead") && Boolean.TRUE.equals(doc.getBoolean("isRead"));
    }

    /** Constructor for creating new notifications */
    public NotificationItem(String title, String message, String type, String recipientId) {
        this();
        this.title = title;
        this.message = message;
        this.type = type;
        this.recipientId = recipientId;
        this.actionRequired = NotificationType.WIN.name().equals(type) ||
                NotificationType.INVITATION.name().equals(type);
    }

    /** Constructor for event-related notifications */
    public NotificationItem(String title, String message, String type, String recipientId, String eventId,
            String eventName) {
        this(title, message, type, recipientId);
        this.eventId = eventId;
        this.eventName = eventName;
    }

    // ... rest of your getters and setters remain the same
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getMessage() {
        return message;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public boolean isActionRequired() {
        return actionRequired;
    }

    public boolean isRead() {
        return isRead;
    }

    public String getType() {
        return type;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public void setActionRequired(boolean actionRequired) {
        this.actionRequired = actionRequired;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public void setType(String type) {
        this.type = type;
        this.actionRequired = NotificationType.WIN.name().equals(type) ||
                NotificationType.INVITATION.name().equals(type);
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    // ... rest of your helper methods remain the same
    @Exclude
    public String getDisplayMessage() {
        if (message != null && !message.isEmpty())
            return message;
        if (description != null && !description.isEmpty())
            return description;
        return title != null ? title : "No message";
    }

    @Exclude
    public String getDisplayTitle() {
        if (title != null && !title.isEmpty())
            return title;
        if (type != null) {
            switch (type) {
                case "WIN":
                    return "You Won!";
                case "REMINDER":
                    return "Event Reminder";
                case "UPDATE":
                    return "Event Update";
                case "CANCELLATION":
                    return "Event Cancelled";
                case "INVITATION":
                    return "New Invitation";
                case "LOSE":
                    return "Lottery Result";
                default:
                    return "Notification";
            }
        }
        return "Notification";
    }

    @Exclude
    public void markAsRead() {
        this.isRead = true;
    }

    @Exclude
    public boolean isEventNotification() {
        return eventId != null && !eventId.isEmpty();
    }

    @Exclude
    public String getFormattedTimestamp() {
        if (timestamp == null)
            return "";
        long diff = System.currentTimeMillis() - timestamp.getTime();
        long minutes = diff / (60 * 1000);
        long hours = diff / (60 * 60 * 1000);
        long days = diff / (24 * 60 * 60 * 1000);
        if (minutes < 1)
            return "Just now";
        if (minutes < 60)
            return minutes + " min ago";
        if (hours < 24)
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        if (days < 7)
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        return timestamp.toString();
    }

    @Exclude
    public String getDocumentId() {
        return id;
    }

    @Exclude
    public NotificationItem copy() {
        NotificationItem copy = new NotificationItem();
        copy.id = this.id;
        copy.title = this.title;
        copy.description = this.description;
        copy.message = this.message;
        copy.eventId = this.eventId;
        copy.eventName = this.eventName;
        copy.timestamp = this.timestamp != null ? new Date(this.timestamp.getTime()) : null;
        copy.actionRequired = this.actionRequired;
        copy.isRead = this.isRead;
        copy.type = this.type;
        copy.recipientId = this.recipientId;
        return copy;
    }

    @Override
    public String toString() {
        return "NotificationItem{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", type='" + type + '\'' +
                ", actionRequired=" + actionRequired +
                ", isRead=" + isRead +
                ", eventId=" + eventId +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        NotificationItem that = (NotificationItem) o;
        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}