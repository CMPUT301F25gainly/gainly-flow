package com.example.gainly_flow;

import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class NotificationsActivity extends AppCompatActivity {

    private static final String TAG = "NotificationsActivity";

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<NotificationItem> notificationList = new ArrayList<>();
    private ImageButton backButton;
    private TextView emptyStateText;
    private BottomNavigationView bottomNav;

    private FirebaseFirestore db;
    private String currentUserId;
    private String currentDeviceId;
    private ListenerRegistration notificationsListener;

    private Entrant currentEntrant;
    private Profile currentProfile;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        initializeViews();
        setupRecyclerView();
        loadNotifications();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewNotifications);
        backButton = findViewById(R.id.backButton_notification);
        bottomNav = findViewById(R.id.bottomNav);

        createEmptyStateView();
        db = FirebaseFirestore.getInstance();
        initializeUserData();

        backButton.setOnClickListener(v -> onBackPressed());

        BottomNavHelper.setupBottomNav(this, bottomNav, currentEntrant, currentProfile);
    }

    private void initializeUserData() {
        // Get device ID first
        currentDeviceId = getCurrentDeviceId();
        Log.d(TAG, "Device ID: " + currentDeviceId);

        // Try to get user data from intent
        currentUserId = getIntent().getStringExtra("userId");

        currentProfile = (Profile) getIntent().getSerializableExtra("profile");
        currentEntrant = (Entrant) getIntent().getSerializableExtra("entrant");

        if (currentUserId == null) {
            if (currentEntrant != null) {
                currentUserId = currentEntrant.getId();
                Log.d(TAG, "Got user ID from entrant: " + currentUserId);
            } else if (currentProfile != null) {
                currentUserId = currentProfile.getId();
                Log.d(TAG, "Got user ID from profile: " + currentUserId);
            } else {
                // Fallback: use device ID as user ID
                currentUserId = currentDeviceId;
                Log.d(TAG, "Using device ID as user ID: " + currentUserId);
            }
        }

        Log.d(TAG, "Final User ID: " + currentUserId + ", Device ID: " + currentDeviceId);
    }

    private String getCurrentDeviceId() {
        return Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ANDROID_ID);
    }

    private void createEmptyStateView() {
        emptyStateText = new TextView(this);
        emptyStateText.setId(View.generateViewId());
        emptyStateText.setLayoutParams(new androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT,
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT));
        emptyStateText.setText("No notifications yet");
        emptyStateText.setTextSize(16f);
        emptyStateText.setTextColor(getResources().getColor(android.R.color.darker_gray));
        emptyStateText.setGravity(android.view.Gravity.CENTER);
        emptyStateText.setPadding(0, 32, 0, 32);
        emptyStateText.setVisibility(View.GONE);

        androidx.constraintlayout.widget.ConstraintLayout layout = findViewById(R.id.notificationLayout);
        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams params = (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) emptyStateText
                .getLayoutParams();
        params.topToBottom = R.id.headerLayout;
        params.bottomToBottom = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
        params.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
        params.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;

        layout.addView(emptyStateText);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new NotificationAdapter(notificationList,
                new NotificationAdapter.OnActionClickListener() {
                    @Override
                    public void onAcceptClicked(NotificationItem item) {
                        handleAcceptAction(item);
                    }

                    @Override
                    public void onDeclineClicked(NotificationItem item) {
                        handleDeclineAction(item);
                    }
                });

        recyclerView.setAdapter(adapter);
    }

    /**
     * Load notifications from ALL possible sources
     */
    private void loadNotifications() {
        showLoading(true);
        notificationList.clear();

        Log.d(TAG, "Loading notifications for user: " + currentUserId);

        // Method 1: Load from global notifications collection
        loadGlobalNotifications();

        // Method 2: Load from user's profile document (using user ID)
        // loadUserProfileNotifications();

        // Method 3: Load from device profile (using device ID) - for backward
        // compatibility
        // if (currentDeviceId != null && !currentDeviceId.equals(currentUserId)) {
        // loadDeviceProfileNotifications();
        // }
    }

    /**
     * Load from global notifications collection where recipientId matches
     */
    private void loadGlobalNotifications() {
        Log.d(TAG, "Loading from global notifications collection...");

        db.collection("notifications")
                .whereEqualTo("recipientId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " global notifications");

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        try {
                            NotificationItem notification = new NotificationItem(doc);
                            if (!containsNotification(notificationList, notification)) {
                                notificationList.add(notification);
                                Log.d(TAG, "Added global notification: " + notification.getTitle());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing global notification: " + e.getMessage());
                        }
                    }
                    updateNotificationsUI();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading global notifications: " + e.getMessage());
                    updateNotificationsUI();
                });
    }

    /**
     * Load from user's profile document (using user ID as document ID)
     */
    private void loadUserProfileNotifications() {
        if (currentUserId == null) {
            Log.e(TAG, "Cannot load user profile notifications: user ID is null");
            return;
        }

        Log.d(TAG, "Loading from user profile: " + currentUserId);

        db.collection("profiles").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        processProfileNotifications(documentSnapshot, "user profile");
                    } else {
                        Log.d(TAG, "User profile document not found: " + currentUserId);
                    }
                    updateNotificationsUI();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user profile: " + e.getMessage());
                    updateNotificationsUI();
                });
    }

    /**
     * Load from device profile (using device ID as document ID) - for backward
     * compatibility
     */
    private void loadDeviceProfileNotifications() {
        Log.d(TAG, "Loading from device profile: " + currentDeviceId);

        db.collection("profiles").document(currentDeviceId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        processProfileNotifications(documentSnapshot, "device profile");
                    } else {
                        Log.d(TAG, "Device profile document not found: " + currentDeviceId);
                    }
                    updateNotificationsUI();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading device profile: " + e.getMessage());
                    updateNotificationsUI();
                });
    }

    private void processProfileNotifications(DocumentSnapshot documentSnapshot, String source) {
        try {
            if (documentSnapshot.contains("notifications")) {
                Object notificationsObj = documentSnapshot.get("notifications");

                if (notificationsObj instanceof List) {
                    List<Map<String, Object>> notificationsData = (List<Map<String, Object>>) notificationsObj;
                    Log.d(TAG, "Found " + notificationsData.size() + " notifications in " + source);

                    for (Map<String, Object> notificationData : notificationsData) {
                        try {
                            NotificationItem notification = convertMapToNotification(notificationData);
                            if (notification != null && !containsNotification(notificationList, notification)) {
                                notificationList.add(notification);
                                Log.d(TAG,
                                        "Added profile notification from " + source + ": " + notification.getTitle());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error converting notification data: " + e.getMessage());
                        }
                    }
                }
            } else {
                Log.d(TAG, "No notifications field in " + source);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing " + source + " notifications: " + e.getMessage());
        }
    }

    private boolean containsNotification(List<NotificationItem> list, NotificationItem notification) {
        for (NotificationItem item : list) {
            if (item.getId() != null && item.getId().equals(notification.getId())) {
                return true;
            }
            if (item.getTitle() != null && item.getTitle().equals(notification.getTitle()) &&
                    item.getMessage() != null && item.getMessage().equals(notification.getMessage()) &&
                    item.getTimestamp() != null && item.getTimestamp().equals(notification.getTimestamp())) {
                return true;
            }
        }
        return false;
    }

    private NotificationItem convertMapToNotification(Map<String, Object> data) {
        try {
            NotificationItem notification = new NotificationItem();

            if (data.containsKey("id"))
                notification.setId((String) data.get("id"));
            if (data.containsKey("title"))
                notification.setTitle((String) data.get("title"));
            if (data.containsKey("message"))
                notification.setMessage((String) data.get("message"));
            if (data.containsKey("description"))
                notification.setDescription((String) data.get("description"));
            if (data.containsKey("eventId"))
                notification.setEventId((String) data.get("eventId"));
            if (data.containsKey("eventName"))
                notification.setEventName((String) data.get("eventName"));
            if (data.containsKey("type"))
                notification.setType((String) data.get("type"));
            if (data.containsKey("recipientId"))
                notification.setRecipientId((String) data.get("recipientId"));

            // Handle timestamp
            if (data.containsKey("timestamp")) {
                Object timestamp = data.get("timestamp");
                if (timestamp instanceof com.google.firebase.Timestamp) {
                    notification.setTimestamp(((com.google.firebase.Timestamp) timestamp).toDate());
                } else if (timestamp instanceof Date) {
                    notification.setTimestamp((Date) timestamp);
                } else if (timestamp instanceof Long) {
                    notification.setTimestamp(new Date((Long) timestamp));
                }
            }

            // Handle boolean fields
            if (data.containsKey("actionRequired")) {
                Object actionReq = data.get("actionRequired");
                if (actionReq instanceof Boolean) {
                    notification.setActionRequired((Boolean) actionReq);
                }
            }

            if (data.containsKey("isRead")) {
                Object isRead = data.get("isRead");
                if (isRead instanceof Boolean) {
                    notification.setRead((Boolean) isRead);
                }
            }

            return notification;

        } catch (Exception e) {
            Log.e(TAG, "Error converting map to notification: " + e.getMessage());
            return null;
        }
    }

    private void updateNotificationsUI() {
        // Remove this check to allow UI updates from multiple sources
        runOnUiThread(() -> {
            // Sort by timestamp (newest first)
            notificationList.sort((n1, n2) -> {
                if (n1.getTimestamp() == null && n2.getTimestamp() == null)
                    return 0;
                if (n1.getTimestamp() == null)
                    return 1;
                if (n2.getTimestamp() == null)
                    return -1;
                return n2.getTimestamp().compareTo(n1.getTimestamp());
            });

            Log.d(TAG, "Total notifications loaded: " + notificationList.size());

            updateEmptyState();
            adapter.notifyDataSetChanged();
            showLoading(false);
        });
    }

    private void updateEmptyState() {
        if (emptyStateText != null) {
            if (notificationList.isEmpty()) {
                emptyStateText.setVisibility(View.VISIBLE);
                emptyStateText.setText("No notifications yet");
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyStateText.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        }
    }

    private void showLoading(boolean show) {
        if (emptyStateText != null) {
            if (show) {
                emptyStateText.setVisibility(View.VISIBLE);
                emptyStateText.setText("Loading notifications...");
                recyclerView.setVisibility(View.GONE);
            } else {
                updateEmptyState();
            }
        }
    }

    // ... rest of your methods (handleAcceptAction, handleDeclineAction, etc.)
    // remain the same
    private void handleAcceptAction(NotificationItem item) {
        Log.d(TAG, "Accepting notification: " + item.getId());
        if (item.getType() != null && item.getType().equals(NotificationItem.NotificationType.WIN.name())) {
            acceptLotteryWin(item);
        } else {
            markNotificationAsRead(item);
            Toast.makeText(this, "Accepted: " + item.getTitle(), Toast.LENGTH_SHORT).show();
        }
    }

    private void handleDeclineAction(NotificationItem item) {
        Log.d(TAG, "Declining notification: " + item.getId());
        if (item.getType() != null && item.getType().equals(NotificationItem.NotificationType.WIN.name())) {
            declineLotteryWin(item);
        } else {
            markNotificationAsRead(item);
            Toast.makeText(this, "Declined: " + item.getTitle(), Toast.LENGTH_SHORT).show();
        }
    }

    private void acceptLotteryWin(NotificationItem item) {
        if (item.getEventId() == null) {
            Toast.makeText(this, "Error: No event associated with this notification", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUserId;
        if (userId == null) {
            Toast.makeText(this, "Error: User not identified", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("events").document(item.getEventId())
                .update(
                        "selected", com.google.firebase.firestore.FieldValue.arrayRemove(userId),
                        "enrolled", com.google.firebase.firestore.FieldValue.arrayUnion(userId))
                .addOnSuccessListener(aVoid -> {
                    markNotificationAsRead(item);
                    Toast.makeText(this, "Successfully enrolled in " + item.getEventName(), Toast.LENGTH_LONG).show();
                    item.setActionRequired(false);
                    adapter.notifyItemChanged(notificationList.indexOf(item));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to accept lottery win: " + e.getMessage());
                    Toast.makeText(this, "Failed to accept: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void declineLotteryWin(NotificationItem item) {
        if (item.getEventId() == null) {
            Toast.makeText(this, "Error: No event associated with this notification", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUserId;
        if (userId == null) {
            Toast.makeText(this, "Error: User not identified", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("events").document(item.getEventId())
                .update(
                        "selected", com.google.firebase.firestore.FieldValue.arrayRemove(userId),
                        "cancelled", com.google.firebase.firestore.FieldValue.arrayUnion(userId))
                .addOnSuccessListener(aVoid -> {
                    markNotificationAsRead(item);
                    Toast.makeText(this, "Declined spot for " + item.getEventName(), Toast.LENGTH_LONG).show();
                    item.setActionRequired(false);
                    adapter.notifyItemChanged(notificationList.indexOf(item));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to decline lottery win: " + e.getMessage());
                    Toast.makeText(this, "Failed to decline: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void markNotificationAsRead(NotificationItem item) {
        item.setRead(true);
        item.setActionRequired(false);
        adapter.notifyDataSetChanged();
        Log.d(TAG, "Notification marked as read: " + item.getId());

        persistNotificationUpdate(item);
    }

    private void persistNotificationUpdate(NotificationItem item) {
        // 1. Update global notification
        if (item.getId() != null) {
            db.collection("notifications").document(item.getId())
                    .update(
                            "isRead", true,
                            "actionRequired", false)
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to update global notification: " + e.getMessage()));
        }

        // 2. Update user profile notification
        if (currentUserId != null) {
            db.collection("profiles").document(currentUserId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Object notificationsObj = documentSnapshot.get("notifications");
                            if (notificationsObj instanceof List) {
                                List<Map<String, Object>> notifications = (List<Map<String, Object>>) notificationsObj;
                                boolean changed = false;
                                for (Map<String, Object> notifMap : notifications) {
                                    String id = (String) notifMap.get("id");
                                    if (id != null && id.equals(item.getId())) {
                                        notifMap.put("isRead", true);
                                        notifMap.put("actionRequired", false);
                                        changed = true;
                                        break;
                                    }
                                }
                                if (changed) {
                                    db.collection("profiles").document(currentUserId)
                                            .update("notifications", notifications)
                                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Updated profile notification"))
                                            .addOnFailureListener(e -> Log.e(TAG,
                                                    "Failed to update profile notification: " + e.getMessage()));
                                }
                            }
                        }
                    });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotifications();
        BottomNavHelper.setSelectedItem(this, bottomNav);
    }
}