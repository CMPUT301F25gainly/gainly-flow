package com.example.gainly_flow;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * LotterySystem:
 * - Randomly select entrants from waiting_lists/{eventId}.entrantIds
 * - Append to waiting_lists/{eventId}.selectedIds
 * - Remove from entrantIds
 * - Log to notification_logs/{eventId}/draws/{autoId}
 * - Enqueue notifications in notifications/
 */
public class LotterySystem {
    private static final String TAG = "LotterySystem";

    public interface DrawCallback {
        void onSuccess(List<String> winners, int capacityLeftBefore, int capacityLeftAfter);
        void onFailure(Exception e);
    }

    /**
     * Run a draw in a Firestore transaction.
     * @param eventId   Event id
     * @param requested Requested winners (will be clamped to min(requested, capacityLeft, waitingSize))
     * @param cb        Callback
     */
    public static void runLottery(@NonNull String eventId, int requested, @NonNull DrawCallback cb) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference eventRef = db.collection("events").document(eventId);
        DocumentReference wlRef    = db.collection("waiting_lists").document(eventId);

        db.runTransaction((Transaction.Function<Map<String, Object>>) transaction -> {
            DocumentSnapshot eventSnap = transaction.get(eventRef);
            DocumentSnapshot wlSnap    = transaction.get(wlRef);

            if (!eventSnap.exists()) {
                throw new IllegalStateException("Event not found: " + eventId);
            }
            if (!wlSnap.exists()) {
                throw new IllegalStateException("Waiting list not found: " + eventId);
            }

            // Read capacity
            Long capacityLong = safeLong(eventSnap.get("capacity"));
            int capacity = capacityLong == null ? 0 : capacityLong.intValue();

            // Read arrays
            List<String> entrantIds = toStringList(wlSnap.get("entrantIds"));
            List<String> selectedIds = toStringList(wlSnap.get("selectedIds"));

            // Sanitize (remove null/blank/dup)
            entrantIds  = sanitizeIds(entrantIds);
            selectedIds = sanitizeIds(selectedIds);

            // Exclude already selected from the candidate pool
            Set<String> alreadySelected = new LinkedHashSet<>(selectedIds);
            List<String> pool = new ArrayList<>();
            for (String id : entrantIds) if (!alreadySelected.contains(id)) pool.add(id);

            int already = selectedIds.size();
            int capacityLeft = Math.max(0, capacity - already);
            int waitingSize  = pool.size();

            if (requested <= 0)          throw new IllegalArgumentException("Requested must be > 0");
            if (capacityLeft <= 0)       throw new IllegalStateException("No capacity left");
            if (waitingSize <= 0)        throw new IllegalStateException("No entrants waiting");

            int k = Math.min(requested, Math.min(capacityLeft, waitingSize));
            Collections.shuffle(pool);
            List<String> winners = new ArrayList<>(pool.subList(0, k));

            // Transactional updates
            Map<String, Object> updates = new HashMap<>();
            // remove winners from entrantIds
            List<Object> toRemove = new ArrayList<>(winners);
            updates.put("entrantIds", FieldValue.arrayRemove(toRemove.toArray()));
            // add winners to selectedIds
            List<Object> toAdd = new ArrayList<>(winners);
            updates.put("selectedIds", FieldValue.arrayUnion(toAdd.toArray()));

            transaction.update(wlRef, updates);

            // draw log (write within transaction for atomicity of the record)
            Map<String, Object> log = new HashMap<>();
            log.put("eventId", eventId);
            log.put("requested", requested);
            log.put("winners", winners);
            log.put("waitingBefore", entrantIds.size());
            log.put("waitingAfter", entrantIds.size() - winners.size());
            log.put("selectedBefore", selectedIds.size());
            log.put("selectedAfter", selectedIds.size() + winners.size());
            log.put("createdAt", FieldValue.serverTimestamp());
            DocumentReference logRef = db.collection("notification_logs")
                    .document(eventId)
                    .collection("draws")
                    .document(); // auto id
            transaction.set(logRef, log);

            Map<String, Object> result = new HashMap<>();
            result.put("winners", winners);
            result.put("capacityLeftBefore", capacityLeft);
            result.put("capacityLeftAfter", capacityLeft - winners.size());
            return result;
        }).addOnSuccessListener(result -> {
            @SuppressWarnings("unchecked")
            List<String> winners = (List<String>) result.get("winners");
            int before = (int) result.get("capacityLeftBefore");
            int after  = (int) result.get("capacityLeftAfter");

            // Enqueue notifications (outside the transaction)
            enqueueNotifications(eventId, winners);

            Log.d(TAG, "Draw complete. Winners=" + winners.size());
            cb.onSuccess(winners, before, after);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Draw failed: " + e.getMessage(), e);
            cb.onFailure(e);
        });
    }

    /** Write a notification doc per winner; your backend can deliver FCM. */
    private static void enqueueNotifications(String eventId, List<String> winners) {
        if (winners == null || winners.isEmpty()) return;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String title = "You're selected!";
        String body  = "You’ve been selected for the event.";

        for (String uid : winners) {
            if (uid == null || uid.trim().isEmpty()) continue;
            Map<String, Object> n = new HashMap<>();
            n.put("userId", uid);
            n.put("eventId", eventId);
            n.put("title", title);
            n.put("body", body);
            n.put("type", "WINNER");
            n.put("read", false);
            n.put("createdAt", FieldValue.serverTimestamp());
            db.collection("notifications").add(n);
        }
    }

    // ---------- helpers ----------

    private static Long safeLong(Object o) {
        if (o instanceof Long) return (Long) o;
        if (o instanceof Integer) return ((Integer) o).longValue();
        if (o instanceof Double)  return ((Double) o).longValue();
        return null;
    }

    private static List<String> toStringList(Object o) {
        List<String> out = new ArrayList<>();
        if (o instanceof List<?>) {
            for (Object x : (List<?>) o) {
                if (x != null) out.add(String.valueOf(x));
            }
        }
        return out;
    }

    private static List<String> sanitizeIds(List<String> in) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (in != null) {
            for (String s : in) {
                if (s != null) {
                    String t = s.trim();
                    if (!t.isEmpty()) set.add(t);
                }
            }
        }
        return new ArrayList<>(set);
    }
}
