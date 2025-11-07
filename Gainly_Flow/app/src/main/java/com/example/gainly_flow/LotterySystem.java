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
 * A Firestore-backed lottery (draw) utility for events.
 *
 * <p>This class selects a random subset of entrants from a per-event waiting list and promotes
 * them to {@code selectedIds}. The selection and list updates happen inside a single
 * Firestore transaction to ensure atomicity. A draw log entry is also written during the
 * transaction. After the transaction commits, per-winner notification documents are created
 * (outside the transaction) so a backend/Cloud Function can deliver push notifications.</p>
 *
 * <h2>Data model (collections/documents)</h2>
 * <ul>
 *   <li>{@code events/{eventId}} — must contain a numeric {@code capacity} field.</li>
 *   <li>{@code waiting_lists/{eventId}} — contains arrays:
 *     <ul>
 *       <li>{@code entrantIds}: users waiting to be drawn</li>
 *       <li>{@code selectedIds}: users already drawn/selected</li>
 *     </ul>
 *   </li>
 *   <li>{@code notification_logs/{eventId}/draws/{autoId}} — a record of each draw,
 *       including before/after counts and the list of winner user IDs.</li>
 *   <li>{@code notifications/{autoId}} — one document per winner, enqueued after the
 *       transaction so another service can deliver notifications.</li>
 * </ul>
 *
 * <h2>Concurrency and idempotency</h2>
 * <p>Because selection and list mutations happen in a single transaction, concurrent
 * draws on the same event will serialize or fail with a retry-able error. Callers
 * should invoke {@link #runLottery(String, int, DrawCallback)} once per draw.</p>
 *
 * <h2>Failure modes</h2>
 * <ul>
 *   <li>Missing {@code events/{eventId}} or {@code waiting_lists/{eventId}} documents</li>
 *   <li>Non-positive requested winners</li>
 *   <li>No remaining capacity or no waiting entrants</li>
 *   <li>General Firestore errors (permission, connectivity, etc.)</li>
 * </ul>
 *
 * <p>All failures surface via {@link DrawCallback#onFailure(Exception)}.</p>
 */
public class LotterySystem {
    private static final String TAG = "LotterySystem";

    /**
     * Callback for the asynchronous result of a lottery draw.
     */
    public interface DrawCallback {
        /**
         * Invoked when the transaction commits successfully.
         *
         * @param winners             ordered random list of winner user IDs (size ≤ requested)
         * @param capacityLeftBefore  remaining capacity before applying the draw
         * @param capacityLeftAfter   remaining capacity after applying the draw
         */
        void onSuccess(List<String> winners, int capacityLeftBefore, int capacityLeftAfter);

        /**
         * Invoked when the draw fails (validation, missing data, Firestore error, etc.).
         *
         * @param e the underlying exception describing the failure
         */
        void onFailure(Exception e);
    }

    /**
     * Runs a lottery (random draw) inside a Firestore transaction.
     *
     * <p>Steps performed atomically in the transaction:</p>
     * <ol>
     *   <li>Read {@code events/{eventId}} (capacity) and {@code waiting_lists/{eventId}}
     *       (entrantIds, selectedIds).</li>
     *   <li>Compute remaining capacity and clamp the number of winners to
     *       {@code min(requested, capacityLeft, waitingSize)}.</li>
     *   <li>Randomly select {@code k} winners from {@code entrantIds \ selectedIds}.</li>
     *   <li>Remove winners from {@code entrantIds} and add them to {@code selectedIds} using
     *       {@link FieldValue#arrayRemove(Object...)} and {@link FieldValue#arrayUnion(Object...)}.</li>
     *   <li>Write a draw log in {@code notification_logs/{eventId}/draws/{autoId}}.</li>
     * </ol>
     *
     * <p>After the transaction commits, this method enqueues one notification document per winner
     * in {@code notifications/} (non-transactional side effect).</p>
     *
     * @param eventId   Firestore document ID of the event
     * @param requested requested number of winners; must be {@code > 0}
     * @param cb        callback receiving success or failure
     *
     * @throws IllegalArgumentException if {@code requested <= 0}
     * @throws IllegalStateException    if no capacity remains or there are no waiting entrants,
     *                                  or if the required documents do not exist
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

    /**
     * Enqueues one notification document per winner in the {@code notifications} collection.
     *
     * <p>This method is intentionally executed outside the draw transaction. Creating
     * notification docs is a side effect that should not block the primary state change
     * (the selection itself). A separate backend process (e.g., Cloud Functions) can watch
     * this collection and deliver FCM push notifications.</p>
     *
     * @param eventId the event identifier associated with the draw
     * @param winners list of user IDs to notify; empty lists are ignored
     */
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

    /**
     * Safely converts a Firestore numeric field to a {@link Long}, accepting
     * {@link Long}, {@link Integer}, or {@link Double} inputs.
     *
     * @param o the value read from Firestore
     * @return a {@link Long} representation, or {@code null} if the type is unsupported or {@code null}
     */
    private static Long safeLong(Object o) {
        if (o instanceof Long) return (Long) o;
        if (o instanceof Integer) return ((Integer) o).longValue();
        if (o instanceof Double)  return ((Double) o).longValue();
        return null;
    }

    /**
     * Converts a Firestore field to a list of strings.
     *
     * <p>Non-null elements are converted with {@link String#valueOf(Object)}.</p>
     *
     * @param o a value expected to be a {@code List<?>}
     * @return a new mutable {@link List} of strings; empty if input is {@code null} or not a list
     */
    private static List<String> toStringList(Object o) {
        List<String> out = new ArrayList<>();
        if (o instanceof List<?>) {
            for (Object x : (List<?>) o) {
                if (x != null) out.add(String.valueOf(x));
            }
        }
        return out;
    }

    /**
     * Produces a cleaned, de-duplicated list of IDs.
     *
     * <p>Rules:</p>
     * <ul>
     *   <li>Ignore {@code null} values</li>
     *   <li>Trim whitespace from each element</li>
     *   <li>Drop blanks after trim</li>
     *   <li>Preserve insertion order while removing duplicates</li>
     * </ul>
     *
     * @param in raw list (possibly {@code null})
     * @return a new {@link ArrayList} with sanitized IDs in deterministic order
     */
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
