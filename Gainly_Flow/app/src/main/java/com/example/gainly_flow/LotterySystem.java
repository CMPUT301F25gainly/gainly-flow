package com.example.gainly_flow;

import androidx.annotation.NonNull;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import android.util.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * LotterySystem - Handles lottery draws and replacement draws for events
 * Uses Firebase Firestore directly for database operations
 */
public class LotterySystem {
    private static final String TAG = "LotterySystem";
    private static final Random random = new Random();

    private Event event;
    private FirebaseFirestore db;
    private NotificationManager notificationManager;

    static class ReplacementSelection {
        private final List<String> replacements;
        private final List<String> remainingWaiting;

        ReplacementSelection(List<String> replacements, List<String> remainingWaiting) {
            this.replacements = replacements;
            this.remainingWaiting = remainingWaiting;
        }

        List<String> getReplacements() {
            return replacements;
        }

        List<String> getRemainingWaiting() {
            return remainingWaiting;
        }
    }

    /**
     * Helper used by replacement draws to select entrants deterministically for
     * testing.
     */
    static ReplacementSelection selectReplacements(List<String> waitingList, int numberToSelect, int availableSpots,
            Random rng) {
        List<String> safeWaiting = waitingList == null ? Collections.emptyList() : waitingList;

        if (numberToSelect <= 0 || availableSpots <= 0 || safeWaiting.isEmpty()) {
            return new ReplacementSelection(Collections.emptyList(), new ArrayList<>(safeWaiting));
        }

        int replacementsToDraw = Math.min(numberToSelect, safeWaiting.size());
        replacementsToDraw = Math.min(replacementsToDraw, availableSpots);

        List<String> shuffledWaiting = new ArrayList<>(safeWaiting);
        Collections.shuffle(shuffledWaiting, rng);

        List<String> replacements = new ArrayList<>(shuffledWaiting.subList(0, replacementsToDraw));
        List<String> remainingWaiting = new ArrayList<>(
                shuffledWaiting.subList(replacementsToDraw, shuffledWaiting.size()));

        return new ReplacementSelection(replacements, remainingWaiting);
    }

    public LotterySystem(Event event) {
        this.event = event;
        this.db = FirebaseFirestore.getInstance();
        this.notificationManager = new NotificationManager();
    }

    public LotterySystem(String eventId) {
        this.event = new Event(eventId);
        this.db = FirebaseFirestore.getInstance();
        this.notificationManager = new NotificationManager();
    }

    /**
     * US 02.05.02 - Draw initial lottery winners
     */
    public void drawInitialLottery(int numberOfWinners, final LotteryDrawCallback callback) {
        if (event == null || event.getId() == null) {
            callback.onError("Event not loaded properly");
            return;
        }

        // Load event directly from Firestore
        db.collection("events").document(event.getId())
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            event.fromDocument(documentSnapshot);
                            performInitialDraw(numberOfWinners, callback);
                        } else {
                            callback.onError("Event not found in Firestore");
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Failed to load event from Firestore: " + e.getMessage());
                        callback.onError("Failed to load event: " + e.getMessage());
                    }
                });
    }

    private void performInitialDraw(int numberOfWinners, LotteryDrawCallback callback) {
        List<String> waitingList = event.getWaitingList();

        if (waitingList == null || waitingList.isEmpty()) {
            callback.onError("No entrants in waiting list");
            return;
        }

        if (numberOfWinners <= 0) {
            callback.onError("Invalid number of winners specified");
            return;
        }

        if (isRegistrationOpen()) {
            callback.onError("Registration period is still open. Cannot draw lottery yet.");
            return;
        }

        Log.d(TAG, "performInitialDraw: Requested=" + numberOfWinners + ", WaitingCount=" + waitingList.size());
        Log.d(TAG, "performInitialDraw: WaitingList=" + waitingList);

        List<String> shuffledList = new ArrayList<>(waitingList);
        Collections.shuffle(shuffledList, random);

        int actualWinners = Math.min(numberOfWinners, shuffledList.size());
        Log.d(TAG, "performInitialDraw: Actual=" + actualWinners);

        List<String> winners = shuffledList.subList(0, actualWinners);
        List<String> remainingWaitingList = new ArrayList<>(shuffledList.subList(actualWinners, shuffledList.size()));

        updateEventAfterDraw(winners, remainingWaitingList, callback);
    }

    /**
     * US 02.05.03 - Draw replacement when a selected entrant declines.
     * Default single replacement for backward compatibility.
     */
    public void drawReplacement(final LotteryDrawCallback callback) {
        drawReplacements(1, callback);
    }

    /**
     * Draw the requested number of replacements, capped by available spots and
     * waiting list size.
     */
    public void drawReplacements(final int numberToSelect, final LotteryDrawCallback callback) {
        if (event == null || event.getId() == null) {
            callback.onError("Event not loaded properly");
            return;
        }

        // Load latest event data from Firestore
        db.collection("events").document(event.getId())
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            event.fromDocument(documentSnapshot);
                            performReplacementDraw(numberToSelect, callback);
                        } else {
                            callback.onError("Event not found in Firestore");
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Failed to load event from Firestore: " + e.getMessage());
                        callback.onError("Failed to load event: " + e.getMessage());
                    }
                });
    }

    private void performReplacementDraw(int numberToSelect, LotteryDrawCallback callback) {
        List<String> waitingList = event.getWaitingList();

        if (waitingList == null || waitingList.isEmpty()) {
            callback.onError("No entrants available for replacement draw");
            return;
        }

        if (event.isFull()) {
            callback.onError("Event is already full");
            return;
        }

        if (numberToSelect <= 0) {
            callback.onError("Invalid number of replacements requested");
            return;
        }

        int availableSpots = event.getAvailableSpots();
        if (availableSpots <= 0) {
            callback.onError("No available spots to fill");
            return;
        }

        int replacementsToDraw = Math.min(numberToSelect, waitingList.size());
        replacementsToDraw = Math.min(replacementsToDraw, availableSpots);

        ReplacementSelection selection = selectReplacements(waitingList, replacementsToDraw, availableSpots, random);
        List<String> replacements = selection.getReplacements();
        List<String> remainingWaiting = selection.getRemainingWaiting();

        Map<String, Object> updates = new HashMap<>();
        updates.put("selected", FieldValue.arrayUnion(replacements.toArray()));
        updates.put("waitingList", remainingWaiting);

        // Update directly in Firestore
        db.collection("events").document(event.getId())
                .update(updates)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Replacement draw completed successfully. Winners: " + replacements.size());

                        // Update local event object
                        event.getSelected().addAll(replacements);
                        event.setWaitingList(remainingWaiting);

                        // Send notifications
                        for (String replacement : replacements) {
                            sendWinnerNotification(replacement, true);
                        }
                        // Notify everyone still waiting they were not selected this round
                        notifyWaitingListNotSelected(remainingWaiting, true);

                        callback.onSuccess(replacements);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Failed to draw replacement: " + e.getMessage());
                        callback.onError("Failed to draw replacement: " + e.getMessage());
                    }
                });
    }

    /**
     * Process multiple declines and draw replacements in batch
     */
    public void processDeclinesAndDrawReplacements(final List<String> declinedEntrants,
            final LotteryDrawCallback callback) {
        if (event == null || event.getId() == null) {
            callback.onError("Event not loaded properly");
            return;
        }

        // Load latest event data from Firestore
        db.collection("events").document(event.getId())
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            event.fromDocument(documentSnapshot);
                            performBatchReplacementDraw(declinedEntrants, callback);
                        } else {
                            callback.onError("Event not found in Firestore");
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Failed to load event from Firestore: " + e.getMessage());
                        callback.onError("Failed to load event: " + e.getMessage());
                    }
                });
    }

    private void performBatchReplacementDraw(List<String> declinedEntrants, LotteryDrawCallback callback) {
        List<String> waitingList = event.getWaitingList();

        if (waitingList == null || waitingList.isEmpty()) {
            removeDeclinedEntrants(declinedEntrants, callback);
            return;
        }

        int availableSpots = event.getAvailableSpots();
        int replacementsNeeded = Math.min(declinedEntrants.size(), availableSpots);
        replacementsNeeded = Math.min(replacementsNeeded, waitingList.size());

        List<String> shuffledWaiting = new ArrayList<>(waitingList);
        Collections.shuffle(shuffledWaiting, random);
        List<String> replacements = shuffledWaiting.subList(0, replacementsNeeded);
        List<String> remainingWaiting = new ArrayList<>(
                shuffledWaiting.subList(replacementsNeeded, shuffledWaiting.size()));

        Map<String, Object> updates = new HashMap<>();

        for (String declined : declinedEntrants) {
            updates.put("selected", FieldValue.arrayRemove(declined));
            updates.put("cancelled", FieldValue.arrayUnion(declined));
        }

        for (String replacement : replacements) {
            updates.put("selected", FieldValue.arrayUnion(replacement));
            updates.put("waitingList", FieldValue.arrayRemove(replacement));
        }

        if (replacementsNeeded < shuffledWaiting.size()) {
            updates.put("waitingList", remainingWaiting);
        }

        // Update directly in Firestore
        db.collection("events").document(event.getId())
                .update(updates)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Batch replacement draw completed successfully");

                        // Update local event object
                        event.getSelected().removeAll(declinedEntrants);
                        event.getCancelled().addAll(declinedEntrants);
                        event.getSelected().addAll(replacements);
                        event.getWaitingList().removeAll(replacements);

                        // Notify replacements
                        for (String replacement : replacements) {
                            sendWinnerNotification(replacement, true);
                        }
                        // Notify remaining waiting list they were not selected in this draw
                        notifyWaitingListNotSelected(event.getWaitingList(), true);

                        callback.onSuccess(replacements);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Failed to process batch replacements: " + e.getMessage());
                        callback.onError("Failed to process replacements: " + e.getMessage());
                    }
                });
    }

    private void removeDeclinedEntrants(List<String> declinedEntrants, LotteryDrawCallback callback) {
        Map<String, Object> updates = new HashMap<>();

        for (String declined : declinedEntrants) {
            updates.put("selected", FieldValue.arrayRemove(declined));
            updates.put("cancelled", FieldValue.arrayUnion(declined));
        }

        // Update directly in Firestore
        db.collection("events").document(event.getId())
                .update(updates)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Declined entrants removed successfully");
                        event.getSelected().removeAll(declinedEntrants);
                        event.getCancelled().addAll(declinedEntrants);
                        callback.onSuccess(new ArrayList<String>());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Failed to remove declined entrants: " + e.getMessage());
                        callback.onError("Failed to remove declined entrants: " + e.getMessage());
                    }
                });
    }

    private void updateEventAfterDraw(List<String> winners, List<String> remainingWaitingList,
            LotteryDrawCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("selected", FieldValue.arrayUnion(winners.toArray()));
        updates.put("waitingList", remainingWaitingList);

        // Update directly in Firestore
        db.collection("events").document(event.getId())
                .update(updates)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Lottery draw completed successfully. Winners: " + winners.size());

                        event.getSelected().addAll(winners);
                        event.setWaitingList(remainingWaitingList);

                        // Notify winners and losers
                        notifyWinnersAndLosers(winners, remainingWaitingList);

                        callback.onSuccess(winners);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Failed to update event after lottery draw: " + e.getMessage());
                        callback.onError("Failed to complete lottery draw: " + e.getMessage());
                    }
                });
    }

    /**
     * US 01.04.01 & US 01.04.02 - Notify winners and losers
     */
    private void notifyWinnersAndLosers(List<String> winners, List<String> remainingWaitingList) {
        Log.d(TAG, "notifyWinnersAndLosers: Winners=" + winners.size() + ", Losers=" + remainingWaitingList.size());

        // Notify winners (US 01.04.01)
        for (String winnerId : winners) {
            Log.d(TAG, "Sending WIN notification to: " + winnerId);
            sendWinnerNotification(winnerId, false);
        }

        // Notify losers (US 01.04.02)
        for (String loserId : remainingWaitingList) {
            Log.d(TAG, "Sending LOSE notification to: " + loserId);
            sendLoserNotification(loserId);
        }

        Log.d(TAG, "Notifications sent: " + winners.size() + " winners, " +
                remainingWaitingList.size() + " losers notified");
    }

    /**
     * Send winner notification
     */
    private void sendWinnerNotification(String entrantId, boolean isReplacement) {
        checkAndSendNotification(entrantId, new NotificationCreator() {
            @Override
            public NotificationItem createNotification(String entrantId, String entrantName) {
                String title = isReplacement ? "Great News! A Spot Opened Up" : "Congratulations! You've Been Selected";

                String message = isReplacement ? String.format(
                        "Hello %s! A spot has opened up for %s and you've been selected! Please respond quickly.",
                        entrantName != null ? entrantName : "there", event.getName())
                        : String.format(
                                "Hello %s! You've been selected for %s. Please accept your spot within the specified time.",
                                entrantName != null ? entrantName : "there", event.getName());

                return new NotificationItem(
                        title,
                        message,
                        NotificationItem.NotificationType.WIN.name(),
                        entrantId,
                        event.getId(),
                        event.getName());
            }
        });
    }

    /**
     * Send loser notification
     */
    private void sendLoserNotification(String entrantId) {
        checkAndSendNotification(entrantId, new NotificationCreator() {
            @Override
            public NotificationItem createNotification(String entrantId, String entrantName) {
                String title = String.format("Not Selected • %s", event.getName());
                String message = String.format(
                        "You have not been selected for %s. You remain on the waiting list and will be notified if a replacement spot opens.",
                        event.getName());

                return new NotificationItem(
                        title,
                        message,
                        NotificationItem.NotificationType.LOSE.name(),
                        entrantId,
                        event.getId(),
                        event.getName());
            }
        });
    }

    /**
     * Notify entrants who remain on the waiting list that they were not selected in
     * this draw.
     */
    private void notifyWaitingListNotSelected(List<String> waitingList, boolean isReplacementDraw) {
        if (waitingList == null || waitingList.isEmpty()) {
            return;
        }

        for (String waitingId : waitingList) {
            checkAndSendNotification(waitingId, new NotificationCreator() {
                @Override
                public NotificationItem createNotification(String entrantId, String entrantName) {
                    String title = isReplacementDraw
                            ? String.format("Still Waiting • %s", event.getName())
                            : String.format("Not Selected • %s", event.getName());
                    String message = isReplacementDraw
                            ? String.format(
                                    "You were not selected in the latest draw for %s, but you remain on the waiting list for the next available spot.",
                                    event.getName())
                            : String.format(
                                    "You have not been selected for %s. You remain on the waiting list and will be notified if a replacement spot opens.",
                                    event.getName());

                    return new NotificationItem(
                            title,
                            message,
                            NotificationItem.NotificationType.LOSE.name(),
                            entrantId,
                            event.getId(),
                            event.getName());
                }
            });
        }
    }

    /**
     * Check notification preferences and send notification if allowed
     */
    private void checkAndSendNotification(String entrantId, NotificationCreator creator) {
        // Check user profile for notification preferences
        db.collection("profiles").document(entrantId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot document) {
                        if (document.exists()) {
                            // Check if entrant has opted out of notifications (US 01.04.03)
                            Boolean notificationsEnabled = document.getBoolean("notificationsEnabled");
                            if (notificationsEnabled != null && !notificationsEnabled) {
                                Log.d(TAG, "Entrant " + entrantId + " has notifications disabled");
                                return;
                            }

                            // Get entrant's name for personalized message
                            String entrantName = document.getString("name");

                            // Create and save notification
                            NotificationItem notification = creator.createNotification(entrantId, entrantName);
                            Log.d(TAG, "Saving notification for " + entrantId + " (Profile exists)");
                            saveNotification(notification);
                        } else {
                            // Create notification even if profile doesn't exist (for device ID users)
                            Log.d(TAG, "Saving notification for " + entrantId + " (Profile not found)");
                            NotificationItem notification = creator.createNotification(entrantId, null);
                            saveNotification(notification);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Failed to load profile for " + entrantId + ": " + e.getMessage());
                        // Create notification anyway
                        NotificationItem notification = creator.createNotification(entrantId, null);
                        saveNotification(notification);
                    }
                });
    }

    /**
     * Save notification to Firestore using your existing structure
     */
    private void saveNotification(NotificationItem notification) {
        // Save to global notifications collection (if you still want this)
        String notificationId = db.collection("notifications").document().getId();

        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("title", notification.getTitle());
        notificationData.put("message", notification.getMessage());
        notificationData.put("type", notification.getType());
        notificationData.put("recipientId", notification.getRecipientId());
        notificationData.put("eventId", notification.getEventId());
        notificationData.put("eventName", notification.getEventName());
        notificationData.put("timestamp", new Date());
        notificationData.put("actionRequired", notification.isActionRequired());
        notificationData.put("isRead", notification.isRead());

        db.collection("notifications").document(notificationId)
                .set(notificationData)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save notification to global collection: " + e.getMessage());
                });

        // ALSO save to entrant's document
        saveNotificationToEntrant(notification);
    }

    /**
     * Save notification to entrant's document
     */
    private void saveNotificationToEntrant(NotificationItem notification) {
        String entrantId = notification.getRecipientId();

        // Add notification to entrant's notifications list
        db.collection("entrants").document(entrantId)
                .update("notifications", FieldValue.arrayUnion(notification))
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Notification saved to entrant: " + entrantId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save notification to entrant: " + e.getMessage());
                });
    }

    /**
     * Get all events that are ready for lottery draw (registration closed)
     */
    public void getEventsReadyForDraw(final EventsReadyCallback callback) {
        Date now = new Date();

        db.collection("events")
                .whereLessThan("registrationClose", now)
                .whereEqualTo("isActive", true)
                .whereGreaterThan("capacity", 0)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        List<Event> events = new ArrayList<>();
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            Event event = new Event();
                            event.fromDocument(doc);
                            events.add(event);
                        }
                        callback.onSuccess(events);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Failed to get events ready for draw: " + e.getMessage());
                        callback.onError("Failed to load events: " + e.getMessage());
                    }
                });
    }

    private boolean isRegistrationOpen() {
        Date now = new Date();
        return event.getRegistrationClose() != null && now.before(event.getRegistrationClose());
    }

    // Getters and setters
    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    /**
     * Callback interface for lottery draw operations
     */
    public interface LotteryDrawCallback {
        void onSuccess(List<String> winners);

        void onError(String errorMessage);
    }

    /**
     * Callback interface for getting events ready for draw
     */
    public interface EventsReadyCallback {
        void onSuccess(List<Event> events);

        void onError(String errorMessage);
    }

    /**
     * Interface for creating different types of notifications
     */
    private interface NotificationCreator {
        NotificationItem createNotification(String entrantId, String entrantName);
    }
}
