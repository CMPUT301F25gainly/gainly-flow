package com.example.gainly_flow;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Records and retrieves notification history for entrants so administrators can audit deliveries.
 * Entries are stored in the Firestore {@code notification_logs} collection with recipient, event,
 * message, timestamp, and delivery status metadata.
 *
 * @author Gainly Flow Team
 * @version 1.0
 */
public class NotificationLog {
    private static final String TAG = "NotificationLog";

    /**
     * Represents a single notification log entry with recipient, event, message, timestamp,
     * and status fields that serialize to and from Firestore documents.
     */
    public static class Entry {
        /** The ID of the recipient who received the notification. */
        public String to;

        /** The ID of the event associated with the notification. */
        public String eventId;

        /** The message content of the notification. */
        public String message;

        /** The timestamp representing when the notification was sent. */
        public Date timestamp;

        /** The status of the notification, such as "sent" or "failed". */
        public String status;

        /**
         * Default constructor required for Firestore deserialization.
         */
        public Entry() {}

        /**
         * Constructs a new {@code Entry} with the specified parameters.
         *
         * @param to        the recipient ID
         * @param eventId   the associated event ID
         * @param message   the message content
         * @param timestamp the timestamp of when the notification was sent
         * @param status    the delivery status of the notification
         */
        public Entry(String to, String eventId, String message, Date timestamp, String status) {
            this.to = to;
            this.eventId = eventId;
            this.message = message;
            this.timestamp = timestamp;
            this.status = status;
        }

        /**
         * Converts this {@code Entry} into a Firestore-compatible map for storage.
         *
         * @return a {@code Map} containing the entry's fields for Firestore
         */
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("to", to);
            map.put("eventId", eventId);
            map.put("message", message);
            map.put("timestamp", new Timestamp(timestamp));
            map.put("status", status);
            return map;
        }

        /**
         * Creates a {@code NotificationLog.Entry} instance from a Firestore document.
         *
         * @param doc the {@code DocumentSnapshot} representing a Firestore document
         * @return a populated {@code Entry} object
         */
        public static Entry fromDocument(DocumentSnapshot doc) {
            Entry e = new Entry();
            e.to = doc.getString("to");
            e.eventId = doc.getString("eventId");
            e.message = doc.getString("message");
            Timestamp ts = doc.getTimestamp("timestamp");
            e.timestamp = ts != null ? ts.toDate() : new Date();
            e.status = doc.getString("status");
            return e;
        }
    }

    /**
     * Records a new notification log entry into the Firestore collection.
     *
     * @param entry the {@code NotificationLog.Entry} object to record
     */
    public void record(Entry entry) {
        if (entry == null) {
            Log.w(TAG, "Attempted to record null notification entry");
            return;
        }

        String logId = generateLogId(entry);
        Database.save("notification_logs", logId, entry.toMap());
        Log.d(TAG, "Recorded notification log: " + logId);
    }

    /**
     * Generates a deterministic log ID based on the event ID, recipient ID, and the current
     * timestamp to avoid duplicate records for the same user-event pair.
     *
     * @param entry the {@code NotificationLog.Entry} used to build the ID
     * @return a unique string identifier for the log
     */
    private String generateLogId(Entry entry) {
        return entry.eventId + "_" + entry.to + "_" + System.currentTimeMillis();
    }

    /**
     * Retrieves all notification logs from Firestore for administrative listing or debugging.
     *
     * @param listener a {@code OnSuccessListener} that receives a list of {@code Entry} objects
     */
    public void listAll(com.google.android.gms.tasks.OnSuccessListener<List<Entry>> listener) {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("notification_logs")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Entry> logs = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        logs.add(Entry.fromDocument(doc));
                    }
                    listener.onSuccess(logs);
                    Log.d(TAG, "Loaded " + logs.size() + " notification logs");
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching notification logs: " + e.getMessage(), e));
    }
}
