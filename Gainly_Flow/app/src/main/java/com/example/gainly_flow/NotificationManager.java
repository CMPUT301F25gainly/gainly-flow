package com.example.gainly_flow;

import android.util.Log;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Coordinates sending entrant notifications for events and records audit entries in Firestore.
 * Creates the user-facing notification documents alongside notification log entries so delivery
 * history can be traced by organizers and administrators.
 *
 * @author Gainly Flow Team
 * @version 1.0
 * @see NotificationLog
 * @see Database
 */
public class NotificationManager {
    private static final String TAG = "NotificationManager";

    /** Used to log notification entries in Firestore under "notification_logs". */
    private final NotificationLog log = new NotificationLog();

    /**
     * Sends a notification message to entrants selected for a given event and logs each delivery.
     *
     * @param entrantIds a non-null list of entrant identifiers (e.g., device IDs or user emails)
     * @param eventId    the unique identifier of the event associated with this notification
     */
    public void notifySelected(@NonNull List<String> entrantIds, @NonNull String eventId) {
        if (entrantIds == null || entrantIds.isEmpty()) {
            Log.w(TAG, "No entrants to notify for event " + eventId);
            return;
        }

        for (String entrantId : entrantIds) {
            sendNotification(entrantId, eventId, "🎉 You have been selected for event " + eventId + "!");
        }
    }

    /**
     * Sends a single notification to an entrant and records it in the notification log.
     * The notification is stored under the "notifications" Firestore collection, and
     * an entry is created in the "notification_logs" collection for tracking.
     *
     * @param entrantId the recipient's unique identifier (e.g., device ID or email)
     * @param eventId   the ID of the event associated with this notification
     * @param message   the message content to send to the entrant
     */
    private void sendNotification(String entrantId, String eventId, String message) {
        Map<String, Object> data = new HashMap<>();
        data.put("to", entrantId);
        data.put("eventId", eventId);
        data.put("message", message);
        data.put("timestamp", new java.util.Date());

        // Write to Firestore "notifications" collection
        Database.save("notifications", generateNotificationId(eventId, entrantId), data);

        // Log to "notification_logs"
        NotificationLog.Entry entry = new NotificationLog.Entry(
                entrantId, eventId, message, new java.util.Date(), "sent"
        );
        log.record(entry);

        Log.d(TAG, "Notification sent to " + entrantId + " for event " + eventId);
    }

    /**
     * Generates a deterministic, timestamp-based notification document ID for Firestore.
     * The ID is formatted as: eventId_entrantId_timestamp
     *
     * @param eventId   the event identifier
     * @param entrantId the entrant identifier
     * @return a unique Firestore document ID string for this notification
     */
    private String generateNotificationId(String eventId, String entrantId) {
        return eventId + "_" + entrantId + "_" + System.currentTimeMillis();
    }
}
