package com.example.gainly_flow;

import android.util.Log;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages sending and logging notifications to entrants in the Gainly Flow system.
 * <p>
 * This class is responsible for:
 * <ul>
 *     <li>Sending notifications to selected entrants for a specific event.</li>
 *     <li>Creating Firestore entries under the <b>"notifications"</b> collection.</li>
 *     <li>Recording each sent notification in the <b>"notification_logs"</b> collection.</li>
 * </ul>
 * <p>
 * Notifications are associated with events by their {@code eventId} and stored using
 * a deterministic notification document ID.
 *
 * @see NotificationLog
 * @see Database
 */
public class NotificationManager {
    private static final String TAG = "NotificationManager";

    /** Used to log notification entries in Firestore under "notification_logs". */
    private final NotificationLog log = new NotificationLog();

    /**
     * Sends a notification message to a list of entrants who were selected for a given event.
     * <p>
     * Each entrant receives a message indicating they have been selected, and the
     * notification is logged to Firestore.
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
     * <p>
     * The notification is stored under the <b>"notifications"</b> Firestore collection, and
     * an entry is created in the <b>"notification_logs"</b> collection for tracking.
     *
     * @param entrantId the recipient’s unique identifier (e.g., device ID or email)
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
     * <p>
     * The ID is formatted as:
     * <pre>{@code
     * {eventId}_{entrantId}_{timestamp}
     * }</pre>
     *
     * @param eventId   the event identifier
     * @param entrantId the entrant identifier
     * @return a unique Firestore document ID string for this notification
     */
    private String generateNotificationId(String eventId, String entrantId) {
        return eventId + "_" + entrantId + "_" + System.currentTimeMillis();
    }
}
