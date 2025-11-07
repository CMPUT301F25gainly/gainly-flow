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
 * Logs all notifications sent to entrants.
 * Stored under collection "notification_logs" in Firestore.
 */
public class NotificationLog {
    private static final String TAG = "NotificationLog";

    /**
     * Represents one log entry for a notification sent.
     */
    public static class Entry {
        public String to;
        public String eventId;
        public String message;
        public Date timestamp;
        public String status; // e.g., "sent", "failed"

        public Entry() {}

        public Entry(String to, String eventId, String message, Date timestamp, String status) {
            this.to = to;
            this.eventId = eventId;
            this.message = message;
            this.timestamp = timestamp;
            this.status = status;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("to", to);
            map.put("eventId", eventId);
            map.put("message", message);
            map.put("timestamp", new Timestamp(timestamp));
            map.put("status", status);
            return map;
        }

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
     * Record a new log entry to Firestore.
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
     * Generate deterministic log ID.
     */
    private String generateLogId(Entry entry) {
        return entry.eventId + "_" + entry.to + "_" + System.currentTimeMillis();
    }

    /**
     * (Optional) List all logs from Firestore.
     * This can be called by admin panels or debugging tools.
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
