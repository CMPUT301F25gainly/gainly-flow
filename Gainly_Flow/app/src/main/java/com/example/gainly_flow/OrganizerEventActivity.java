package com.example.gainly_flow;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.*;

public class OrganizerEventActivity extends AppCompatActivity {

    // UI Components
    private TextView textEventName;
    private TextView textWaitingListCount;
    private TextView textTargetSpots;

    // Quick Actions Buttons
    private Button btnViewWaitingList;
    private Button btnExportCsv;

    // Lottery Draw Section
    private EditText etNumberToSelect;
    private CheckBox cbAutoNotify;
    private Button btnRunLottery;
    private Button btnDrawReplacement;

    // Notification Section
    private RadioGroup radioGroupRecipient;
    private RadioButton radioAllWaitingList;
    private RadioButton radioAllSelected;
    private RadioButton radioAllCancelled;
    private EditText etMessage;
    private Button btnSendNotification;

    // Geolocation (already in XML)
    private TextView textGeolocationStatus;

    // Firebase
    private FirebaseFirestore db;
    private Event currentEvent;
    private String eventId;

    // Lottery System
    private LotterySystem lotterySystem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_event);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Get event ID from intent
        Intent intent = getIntent();
        eventId = intent.getStringExtra("event_id");

        if (eventId == null) {
            Toast.makeText(this, "Error: No event ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupEventListeners();
        loadEventData();
    }

    private void initializeViews() {
        // Event Header
        textEventName = findViewById(R.id.text_event_name);
        textWaitingListCount = findViewById(R.id.text_waiting_list_count);
        textTargetSpots = findViewById(R.id.text_target_spots);

        // Quick Actions
        btnViewWaitingList = findViewById(R.id.btn_view_waiting_list);
        btnExportCsv = findViewById(R.id.btn_export_csv);

        // Lottery Draw Section
        etNumberToSelect = findViewById(R.id.et_number_to_select);
        cbAutoNotify = findViewById(R.id.cb_auto_notify);
        btnRunLottery = findViewById(R.id.btn_run_lottery);
        btnDrawReplacement = findViewById(R.id.btn_draw_replacement);

        // Notification Section
        radioGroupRecipient = findViewById(R.id.radio_group_recipient);
        radioAllWaitingList = findViewById(R.id.radio_all_waitinglist);
        radioAllSelected = findViewById(R.id.radio_all_selected);
        radioAllCancelled = findViewById(R.id.radio_all_canceled);
        etMessage = findViewById(R.id.et_message);
        btnSendNotification = findViewById(R.id.btn_send_notification);

        // Initialize LotterySystem
        lotterySystem = new LotterySystem(eventId);
    }

    private void setupEventListeners() {
        // Quick Actions
        btnViewWaitingList.setOnClickListener(v -> viewWaitingList());
        btnExportCsv.setOnClickListener(v -> exportToCsv());

        // Lottery Draw
        btnRunLottery.setOnClickListener(v -> runLotteryDraw());
        btnDrawReplacement.setOnClickListener(v -> drawReplacement());

        // Send Notification
        btnSendNotification.setOnClickListener(v -> sendNotification());

        // Number validation
        etNumberToSelect.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validateNumberInput();
            }
        });

        // Recipient group change listener
        radioGroupRecipient.setOnCheckedChangeListener((group, checkedId) -> {
            updateMessageHintBasedOnRecipient();
        });
    }

    private void loadEventData() {
        showLoading(true);

        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentEvent = new Event();
                        currentEvent.fromDocument(documentSnapshot);
                        updateUIWithEventData();
                        showLoading(false);
                    } else {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void updateUIWithEventData() {
        if (currentEvent == null) return;

        // Basic event info
        textEventName.setText(currentEvent.getName());
        textWaitingListCount.setText(String.valueOf(currentEvent.getWaitingListSize()));
        textTargetSpots.setText(String.valueOf(currentEvent.getCapacity()));

        // Set initial number to select
        int suggestedNumber = Math.min(currentEvent.getAvailableSpots(), currentEvent.getWaitingListSize());
        etNumberToSelect.setText(String.valueOf(suggestedNumber));

        // Update lottery button state based on registration status
        updateLotteryButtonState();

        // Update message hint
        updateMessageHintBasedOnRecipient();
    }

    private void updateLotteryButtonState() {
        if (currentEvent == null) return;

        boolean canRunLottery = !currentEvent.isRegistrationOpen() &&
                currentEvent.getWaitingListSize() > 0 &&
                currentEvent.getAvailableSpots() > 0;

        btnRunLottery.setEnabled(canRunLottery);
        btnRunLottery.setAlpha(canRunLottery ? 1.0f : 0.5f);

        boolean canDrawReplacement = currentEvent.getWaitingListSize() > 0 &&
                currentEvent.getAvailableSpots() > 0;

        btnDrawReplacement.setEnabled(canDrawReplacement);
        btnDrawReplacement.setAlpha(canDrawReplacement ? 1.0f : 0.5f);

        // Update button text based on state
        if (currentEvent.isRegistrationOpen()) {
            btnRunLottery.setText("Registration Still Open");
        } else {
            btnRunLottery.setText("Run Lottery Draw");
        }
    }

    // Quick Actions Methods
    private void viewWaitingList() {
        Intent intent = new Intent(this, WaitingListActivity.class);
        intent.putExtra("event_id", eventId);
        intent.putExtra("event_name", currentEvent.getName());
        startActivity(intent);
    }

    private void drawReplacement() {
        showLoading(true);
        lotterySystem.drawReplacement(new LotterySystem.LotteryDrawCallback() {
            @Override
            public void onSuccess(List<String> winners) {
                showLoading(false);
                if (!winners.isEmpty()) {
                    String message = "Replacement drawn successfully!";
                    Toast.makeText(OrganizerEventActivity.this, message, Toast.LENGTH_LONG).show();
                    // Reload event data to update counts
                    loadEventData();
                } else {
                    Toast.makeText(OrganizerEventActivity.this, "No replacements available", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);
                Toast.makeText(OrganizerEventActivity.this, "Failed to draw replacement: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void exportToCsv() {
        if (currentEvent == null) return;

        // Simple CSV generation - in real app, you'd fetch user details
        StringBuilder csv = new StringBuilder();
        csv.append("User ID,Status\n");

        // Add waiting list users
        for (String userId : currentEvent.getWaitingList()) {
            csv.append(userId).append(",WAITING\n");
        }

        // Add selected users
        for (String userId : currentEvent.getSelected()) {
            csv.append(userId).append(",SELECTED\n");
        }

        // Add enrolled users
        for (String userId : currentEvent.getEnrolled()) {
            csv.append(userId).append(",ENROLLED\n");
        }

        // Add cancelled users
        for (String userId : currentEvent.getCancelled()) {
            csv.append(userId).append(",CANCELLED\n");
        }

        // In real implementation, save to file or share
        String csvData = csv.toString();
        Toast.makeText(this, "CSV data ready for " +
                (currentEvent.getWaitingListSize() + currentEvent.getSelectedCount() +
                        currentEvent.getEnrolledCount() + currentEvent.getCancelled().size()) +
                " users", Toast.LENGTH_LONG).show();

        // You could implement file sharing here
    }

    // Lottery Methods
    private void runLotteryDraw() {
        if (!validateNumberInput()) {
            return;
        }

        int numberToSelect = Integer.parseInt(etNumberToSelect.getText().toString());
        boolean autoNotify = cbAutoNotify.isChecked();

        showLoading(true);
        lotterySystem.drawInitialLottery(numberToSelect, new LotterySystem.LotteryDrawCallback() {
            @Override
            public void onSuccess(List<String> winners) {
                showLoading(false);
                String message = String.format("Lottery completed! %d entrants selected.", winners.size());
                Toast.makeText(OrganizerEventActivity.this, message, Toast.LENGTH_LONG).show();

                if (autoNotify) {
                    // Notifications are automatically sent by LotterySystem
                    Toast.makeText(OrganizerEventActivity.this, "Notifications sent to winners", Toast.LENGTH_SHORT).show();
                }

                // Reload event data to update counts
                loadEventData();
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);
                Toast.makeText(OrganizerEventActivity.this, "Lottery failed: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    // Notification Methods
    private void sendNotification() {
        String recipientGroup = getSelectedRecipientGroup();
        String message = etMessage.getText().toString().trim();

        if (message.isEmpty()) {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> recipientIds = getRecipientIdsForGroup(recipientGroup);

        if (recipientIds.isEmpty()) {
            Toast.makeText(this, "No recipients in selected group", Toast.LENGTH_SHORT).show();
            return;
        }

        sendNotificationsToUsers(recipientIds, message, recipientGroup);
        etMessage.setText("");
    }

    private List<String> getRecipientIdsForGroup(String recipientGroup) {
        if (currentEvent == null) return new ArrayList<>();

        switch (recipientGroup) {
            case "All Waiting List":
                return currentEvent.getWaitingList();
            case "All Selected":
                return currentEvent.getSelected();
            case "All Canceled":
                return currentEvent.getCancelled();
            default:
                return new ArrayList<>();
        }
    }

    private void sendNotificationsToUsers(List<String> userIds, String message, String recipientGroup) {
        for (String userId : userIds) {
            // Save to global notifications collection
            saveNotificationToGlobalCollection(userId, message);

            // Also save to user's profile if needed
            saveNotificationToUserProfile(userId, message);
        }

        String toastMessage = String.format("Notification sent to %s (%d users)", recipientGroup, userIds.size());
        Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
    }

    private void saveNotificationToGlobalCollection(String userId, String message) {
        String notificationId = db.collection("notifications").document().getId();

        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("id", notificationId);
        notificationData.put("title", "Message from " + currentEvent.getName());
        notificationData.put("message", message);
        notificationData.put("type", NotificationItem.NotificationType.INFO.name());
        notificationData.put("recipientId", userId);
        notificationData.put("eventId", currentEvent.getId());
        notificationData.put("eventName", currentEvent.getName());
        notificationData.put("timestamp", com.google.firebase.Timestamp.now());
        notificationData.put("actionRequired", false);
        notificationData.put("isRead", false);

        db.collection("notifications").document(notificationId)
                .set(notificationData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Notification saved to global collection"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save notification: " + e.getMessage()));
    }

    private void saveNotificationToUserProfile(String userId, String message) {
        // This adds the notification to the user's profile document
        NotificationItem notification = new NotificationItem(
                "Message from " + currentEvent.getName(),
                message,
                NotificationItem.NotificationType.INFO.name(),
                userId,
                currentEvent.getId(),
                currentEvent.getName()
        );

        // Convert to map
        Map<String, Object> notificationMap = new HashMap<>();
        notificationMap.put("id", notification.getId());
        notificationMap.put("title", notification.getTitle());
        notificationMap.put("message", notification.getMessage());
        notificationMap.put("type", notification.getType());
        notificationMap.put("recipientId", notification.getRecipientId());
        notificationMap.put("eventId", notification.getEventId());
        notificationMap.put("eventName", notification.getEventName());
        notificationMap.put("timestamp", com.google.firebase.Timestamp.now());
        notificationMap.put("actionRequired", notification.isActionRequired());
        notificationMap.put("isRead", notification.isRead());

        // Add to user's profile notifications array
        db.collection("profiles").document(userId) // Using userId as document ID
                .update("notifications", com.google.firebase.firestore.FieldValue.arrayUnion(notificationMap))
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Notification added to user profile"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to add notification to profile: " + e.getMessage()));
    }

    private void saveNotificationToFirestore(NotificationItem notification) {
        String notificationId = db.collection("notifications").document().getId();

        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("title", notification.getTitle());
        notificationData.put("message", notification.getMessage());
        notificationData.put("type", notification.getType());
        notificationData.put("recipientId", notification.getRecipientId());
        notificationData.put("eventId", notification.getEventId());
        notificationData.put("eventName", notification.getEventName());
        notificationData.put("timestamp", new Date());
        notificationData.put("actionRequired", false);
        notificationData.put("isRead", false);

        db.collection("notifications").document(notificationId)
                .set(notificationData)
                .addOnFailureListener(e -> {
                    Log.e("OrganizerEventActivity", "Failed to save notification: " + e.getMessage());
                });
    }

    private String getSelectedRecipientGroup() {
        int selectedId = radioGroupRecipient.getCheckedRadioButtonId();

        if (selectedId == R.id.radio_all_waitinglist) {
            return "All Waiting List";
        } else if (selectedId == R.id.radio_all_selected) {
            return "All Selected";
        } else if (selectedId == R.id.radio_all_canceled) {
            return "All Canceled";
        } else {
            return "All Waiting List";
        }
    }

    private void updateMessageHintBasedOnRecipient() {
        if (currentEvent == null) return;

        String recipientGroup = getSelectedRecipientGroup();
        int count = getRecipientIdsForGroup(recipientGroup).size();
        String hint = String.format("Message for %s (%d users)...", recipientGroup, count);
        etMessage.setHint(hint);
    }

    // Utility Methods
    private boolean validateNumberInput() {
        String input = etNumberToSelect.getText().toString();
        if (input.isEmpty()) {
            etNumberToSelect.setError("Please enter a number");
            return false;
        }

        try {
            int number = Integer.parseInt(input);

            if (number <= 0) {
                etNumberToSelect.setError("Number must be greater than 0");
                return false;
            }

            if (currentEvent != null) {
                if (number > currentEvent.getWaitingListSize()) {
                    etNumberToSelect.setError("Cannot select more than waiting list count");
                    return false;
                }

                if (number > currentEvent.getAvailableSpots()) {
                    etNumberToSelect.setError("Cannot select more than available spots");
                    return false;
                }
            }

            etNumberToSelect.setError(null);
            return true;

        } catch (NumberFormatException e) {
            etNumberToSelect.setError("Please enter a valid number");
            return false;
        }
    }

    private void showLoading(boolean show) {
        // Disable interactive elements while loading
        btnRunLottery.setEnabled(!show);
        btnDrawReplacement.setEnabled(!show);
        btnSendNotification.setEnabled(!show);
        btnViewWaitingList.setEnabled(!show);
        btnExportCsv.setEnabled(!show);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh event data when returning to this activity
        if (eventId != null) {
            loadEventData();
        }
    }
}