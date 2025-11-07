package com.example.gainly_flow;

import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WaitingList {
    private static final String TAG = "waiting_list";

    // ---- UNIT-TEST SWITCHES ----
    private static volatile boolean PERSISTENCE_ENABLED = true;
    private static volatile boolean LOGGING_ENABLED = true;

    /** Call from JVM unit tests to avoid Firebase *and* android.util.Log. */
    public static void disablePersistenceForUnitTests() { PERSISTENCE_ENABLED = false; }
    public static void disableLoggingForUnitTests()     { LOGGING_ENABLED = false;   }
    public static void enablePersistence()              { PERSISTENCE_ENABLED = true; }
    public static void enableLogging()                  { LOGGING_ENABLED = true;     }

    private String eventId;
    private List<String> entrantList = new ArrayList<>();

    public WaitingList(String eventId) { this.eventId = eventId; }

    // ---- SAFE LOG HELPERS (never call android.util.Log directly in tests) ----
    private static void logD(String tag, String msg) {
        if (!LOGGING_ENABLED) return;
        try { android.util.Log.d(tag, msg); } catch (Throwable ignored) { /* JVM tests */ }
    }
    private static void logW(String tag, String msg) {
        if (!LOGGING_ENABLED) return;
        try { android.util.Log.w(tag, msg); } catch (Throwable ignored) { /* JVM tests */ }
    }
    private static void logE(String tag, String msg) {
        if (!LOGGING_ENABLED) return;
        try { android.util.Log.e(tag, msg); } catch (Throwable ignored) { /* JVM tests */ }
    }

    /** Load from Firestore asynchronously. Do not call in local unit tests. */
    public void load(com.google.android.gms.tasks.OnSuccessListener<WaitingList> listener) {
        if (eventId == null || eventId.trim().isEmpty()) {
            logE(TAG, "WaitingList.load called with null/empty eventId");
            listener.onSuccess(this);
            return;
        }
        if (!PERSISTENCE_ENABLED) { // JVM tests: no DB
            listener.onSuccess(this);
            return;
        }
        Database.get("waiting_lists", eventId, document -> {
            if (document != null && document.exists()) {
                applyFromDocument(document);
                listener.onSuccess(this);
                return;
            }
            Database.findOne("waiting_lists", "eventId", eventId, altDoc -> {
                if (altDoc != null && altDoc.exists()) {
                    applyFromDocument(altDoc);
                } else {
                    logD(TAG, "No waiting_list found for eventId=" + eventId);
                    this.entrantList = new ArrayList<>();
                }
                listener.onSuccess(this);
            });
        });
    }

    private void applyFromDocument(@NonNull com.google.firebase.firestore.DocumentSnapshot doc) {
        List<String> ids = (List<String>) doc.get("entrantIds");
        if (ids == null) ids = new ArrayList<>();
        this.entrantList = new ArrayList<>(ids);
    }

    /** Add entrant (duplicate = no-op; capacity respected). */
    public void addEntrant(String entrantId, int capacity) {
        if (entrantId == null || entrantId.trim().isEmpty()) {
            logW(TAG, "Ignoring blank/null entrantId");
            return;
        }
        if (entrantList.contains(entrantId)) {
            logW(TAG, "Entrant already exists in waiting list: " + entrantId);
            return;
        }
        if (capacity > 0 && entrantList.size() >= capacity) {
            logW(TAG, "Waiting list full for event " + eventId + " (maxCapacity: " + capacity + ")");
            return;
        }
        entrantList.add(entrantId);
        save();
        logD(TAG, "Added entrant " + entrantId + " to waiting list for " + eventId);
    }

    /** Remove entrant (missing = no-op). */
    public void removeEntrant(String entrantId) {
        if (!entrantList.contains(entrantId)) {
            logW(TAG, "Entrant not found in waiting list: " + entrantId);
            return;
        }
        entrantList.remove(entrantId);
        save();
        logD(TAG, "Removed entrant " + entrantId + " from waiting list for " + eventId);
    }

    public List<String> getEntrants() { return new ArrayList<>(entrantList); }
    public int getCount() { return entrantList.size(); }

    /** Persist (skips during unit tests). */
    private void save() {
        if (!PERSISTENCE_ENABLED) return; // JVM unit tests
        Map<String, Object> data = new HashMap<>();
        data.put("entrantIds", entrantList);
        data.put("size", entrantList.size());
        try {
            Database.save("waiting_lists", eventId, data);
        } catch (Throwable t) {
            // Never crash unit tests because of DB
            logW(TAG, "Persistence unavailable; skipping save");
        }
    }
}
