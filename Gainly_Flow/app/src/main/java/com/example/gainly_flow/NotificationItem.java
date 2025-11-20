package com.example.gainly_flow;

import com.google.firebase.firestore.DocumentSnapshot;
import java.util.Date;

/**
 * Represents a notification for the user.
 */
public class NotificationItem {
    private String id;
    private String title;
    private String description;
    private String eventId;
    private Date timestamp;
    private boolean actionRequired;

    public NotificationItem() {}

    public NotificationItem(DocumentSnapshot doc) {
        this.id = doc.getId();
        this.title = doc.contains("title") ? doc.getString("title") : "No Title";
        this.description = doc.contains("message") ? doc.getString("message") : "";
        this.eventId = doc.contains("eventId") ? doc.getString("eventId") : null;

        Long ts = doc.contains("timestamp") ? doc.getLong("timestamp") : null;
        this.timestamp = ts != null ? new Date(ts) : new Date();

        String type = doc.contains("type") ? doc.getString("type") : "";
        this.actionRequired = "WIN".equals(type);
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getEventId() { return eventId; }
    public Date getTimestamp() { return timestamp; }
    public boolean isActionRequired() { return actionRequired; }

    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    public void setActionRequired(boolean actionRequired) { this.actionRequired = actionRequired; }

    public String getMessage() {
        // return description if your notification text is stored in 'description'
        return description != null ? description : title;
    }

    @Override
    public String toString() {
        return "NotificationItem{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", actionRequired=" + actionRequired +
                '}';
    }
}
