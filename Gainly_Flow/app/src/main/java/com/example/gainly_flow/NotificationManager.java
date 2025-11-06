package com.example.gainly_flow;

import android.util.Log;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles sending notifications to entrants.
 * Each notification is stored in Firestore under "notifications",
 * and an entry is logged in "notification_logs".
 */
public class NotificationManager {
    private static final String TAG = "NotificationManager";

    private final NotificationLog log = new NotificationLog();

    /**
     * Send notification to a list of entrants who were selected.
     *
     * @param entrantIds List of entrant IDs (device IDs, emails, etc.)
     * @param eventId    Event associated with the notification
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
     * Internal helper to send and log one notification.
     */
    private void sendNotification(String entrantId, String eventId, String message) {
        Map<String, Object> data = new HashMap<>();
        data.put("to", entrantId);
        data.put("eventId", eventId);
        data.put("message", message);
        data.put("timestamp", new java.util.Date());

        // Write to Firestore notifications collection
        Database.save("notifications", generateNotificationId(eventId, entrantId), data);

        // Log it in the notification log
        NotificationLog.Entry entry = new NotificationLog.Entry(
                entrantId, eventId, message, new java.util.Date(), "sent"
        );
        log.record(entry);

        Log.d(TAG, "Notification sent to " + entrantId + " for event " + eventId);
    }

    /**
     * Generate a deterministic ID for the notification document.
     */
    private String generateNotificationId(String eventId, String entrantId) {
        return eventId + "_" + entrantId + "_" + System.currentTimeMillis();
    }
}
