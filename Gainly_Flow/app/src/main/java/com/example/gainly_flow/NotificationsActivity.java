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

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private static final String TAG = "NotificationsActivity";

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<NotificationItem> notificationList = new ArrayList<>();
    private ImageButton backButton;
    private TextView emptyStateText;

    private FirebaseFirestore db;
    private String currentUserId;
    private String currentDeviceId;
    private ListenerRegistration profileListener;
    private Profile currentProfile;
    private Entrant currentEntrant;

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

        // Create empty state text programmatically
        createEmptyStateView();

        db = FirebaseFirestore.getInstance();

        // Get current user ID from intent or use device ID
        initializeUserData();

        backButton.setOnClickListener(v -> onBackPressed());
    }

    private void initializeUserData() {
        // Try to get profile/entrant from intent first
        currentEntrant = (Entrant) getIntent().getSerializableExtra("entrant");
        if (currentEntrant != null) {
            currentProfile = currentEntrant;
            currentUserId = currentEntrant.getId();
            currentDeviceId = currentEntrant.getDeviceId();
            Log.d(TAG, "Loaded Entrant from intent: " + currentEntrant.getDisplayName());
            return;
        }

        currentProfile = (Profile) getIntent().getSerializableExtra("profile");
        if (currentProfile != null) {
            currentUserId = currentProfile.getId();
            currentDeviceId = currentProfile.getDeviceId();
            Log.d(TAG, "Loaded Profile from intent: " + currentProfile.getDisplayName());

            // Convert to Entrant if needed
            if (currentProfile instanceof Entrant) {
                currentEntrant = (Entrant) currentProfile;
            } else if ("Entrant".equals(currentProfile.getRole())) {
                currentEntrant = convertProfileToEntrant(currentProfile);
            }
        } else {
            // Fallback to device ID
            currentDeviceId = getCurrentDeviceId();
            currentUserId = currentDeviceId; // Use device ID as user ID
            Log.d(TAG, "Using device ID as user ID: " + currentDeviceId);
        }

        Log.d(TAG, "Current user ID: " + currentUserId + ", Device ID: " + currentDeviceId);
    }

    // Renamed to avoid conflict with ContextWrapper's getDeviceId()
    private String getCurrentDeviceId() {
        // TODO: Replace with your actual device ID retrieval logic
        // This should match what you use in MainActivity
        return Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ANDROID_ID
        );
    }

    private Entrant convertProfileToEntrant(Profile profile) {
        Entrant entrant = new Entrant(profile.getId());
        entrant.setDisplayName(profile.getDisplayName());
        entrant.setEmail(profile.getEmail());
        entrant.setPhone(profile.getPhone());
        entrant.setRole("Entrant");
        entrant.setDeviceId(profile.getDeviceId());
        entrant.setCreatedAt(profile.getCreatedAt());
        entrant.setLastLoginAt(profile.getLastLoginAt());
        entrant.setReceiveNotifications(profile.isReceiveNotifications());
        entrant.setEnableLocationService(profile.isEnableLocationService());
        return entrant;
    }

    private void createEmptyStateView() {
        // Create empty state view programmatically
        emptyStateText = new TextView(this);
        emptyStateText.setId(View.generateViewId());
        emptyStateText.setLayoutParams(new androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT,
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT
        ));
        emptyStateText.setText("No notifications yet");
        emptyStateText.setTextSize(16f);
        emptyStateText.setTextColor(getResources().getColor(android.R.color.darker_gray));
        emptyStateText.setGravity(android.view.Gravity.CENTER);
        emptyStateText.setPadding(0, 32, 0, 32);
        emptyStateText.setVisibility(View.GONE);

        // Add to constraint layout
        androidx.constraintlayout.widget.ConstraintLayout layout = findViewById(R.id.notificationLayout);
        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams params =
                (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) emptyStateText.getLayoutParams();
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
     * Load notifications from the profile's document in the profiles collection
     */
    private void loadNotifications() {
        showLoading(true);

        // Use device ID as the document ID in the profiles collection
        String documentId = currentDeviceId != null ? currentDeviceId : currentUserId;

        if (documentId == null || documentId.isEmpty()) {
            Toast.makeText(this, "Cannot load notifications: No user ID available", Toast.LENGTH_LONG).show();
            showLoading(false);
            return;
        }

        Log.d(TAG, "Loading notifications from profiles/" + documentId);

        // Listen to the profile document for real-time updates
        profileListener = db.collection("profiles").document(documentId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    showLoading(false);

                    if (error != null) {
                        Log.e(TAG, "Error loading profile data: " + error.getMessage());
                        Toast.makeText(this, "Failed to load notifications", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        processProfileData(documentSnapshot);
                    } else {
                        Log.w(TAG, "Profile document not found: " + documentId);
                        Toast.makeText(this, "User profile not found. Please complete your profile first.", Toast.LENGTH_LONG).show();
                        showCustomEmptyState("Complete your profile to receive notifications");
                    }
                });
    }

    private void processProfileData(DocumentSnapshot documentSnapshot) {
        try {
            // Convert the document to appropriate class
            Profile profile = DeviceIdManager.convertToRoleClass(documentSnapshot, "Entrant");
            if (profile != null) {
                this.currentProfile = profile;

                // Get notifications from the profile
                List<NotificationItem> profileNotifications = new ArrayList<>();

                if (profile instanceof Entrant) {
                    currentEntrant = (Entrant) profile;
                    profileNotifications = currentEntrant.getNotifications();
                    Log.d(TAG, "Loaded Entrant with " + profileNotifications.size() + " notifications");
                } else {
                    // For base Profile class, we need to handle notifications differently
                    // You might want to add notifications field to base Profile class
                    Log.d(TAG, "Loaded base Profile, no notifications available");
                    Toast.makeText(this, "Notification feature requires Entrant profile", Toast.LENGTH_SHORT).show();
                }

                notificationList.clear();
                if (profileNotifications != null) {
                    notificationList.addAll(profileNotifications);

                    // Sort by timestamp (newest first)
                    notificationList.sort((n1, n2) -> n2.getTimestamp().compareTo(n1.getTimestamp()));
                }

                Log.d(TAG, "Loaded " + notificationList.size() + " notifications");
                updateEmptyState();
                adapter.notifyDataSetChanged();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing profile data: " + e.getMessage());
            Toast.makeText(this, "Error loading notifications", Toast.LENGTH_SHORT).show();
        }
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

    // Renamed method to avoid conflict
    private void showCustomEmptyState(String message) {
        if (emptyStateText != null) {
            emptyStateText.setVisibility(View.VISIBLE);
            emptyStateText.setText(message);
            recyclerView.setVisibility(View.GONE);
        }
    }

    private void showLoading(boolean show) {
        if (emptyStateText != null) {
            if (show) {
                emptyStateText.setVisibility(View.VISIBLE);
                emptyStateText.setText("Loading notifications...");
                recyclerView.setVisibility(View.GONE);
            } else {
                updateEmptyState(); // Let updateEmptyState handle the final state
            }
        }
    }

    /**
     * Handle accept action for lottery win notifications
     */
    private void handleAcceptAction(NotificationItem item) {
        Log.d(TAG, "Accepting notification: " + item.getId());

        if (item.getType() != null && item.getType().equals(NotificationItem.NotificationType.WIN.name())) {
            // Handle lottery win acceptance
            acceptLotteryWin(item);
        } else {
            // Handle other types of accept actions
            markNotificationAsRead(item);
            Toast.makeText(this, "Accepted: " + item.getTitle(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handle decline action for lottery win notifications
     */
    private void handleDeclineAction(NotificationItem item) {
        Log.d(TAG, "Declining notification: " + item.getId());

        if (item.getType() != null && item.getType().equals(NotificationItem.NotificationType.WIN.name())) {
            // Handle lottery win decline
            declineLotteryWin(item);
        } else {
            // Handle other types of decline actions
            markNotificationAsRead(item);
            Toast.makeText(this, "Declined: " + item.getTitle(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Accept a lottery win - move user from selected to enrolled
     */
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

        // Update event in Firestore - move user from selected to enrolled
        db.collection("events").document(item.getEventId())
                .update(
                        "selected", com.google.firebase.firestore.FieldValue.arrayRemove(userId),
                        "enrolled", com.google.firebase.firestore.FieldValue.arrayUnion(userId)
                )
                .addOnSuccessListener(aVoid -> {
                    markNotificationAsRead(item);
                    Toast.makeText(this, "Successfully enrolled in " + item.getEventName(), Toast.LENGTH_LONG).show();
                    Log.d(TAG, "User enrolled in event: " + item.getEventId());

                    // Remove from pending invitations if this is an Entrant
                    if (currentEntrant != null) {
                        removeFromPendingInvitations(item.getEventId());
                    }

                    // Remove action buttons since decision is made
                    item.setActionRequired(false);
                    adapter.notifyItemChanged(notificationList.indexOf(item));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to accept lottery win: " + e.getMessage());
                    Toast.makeText(this, "Failed to accept: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Decline a lottery win - move user from selected to cancelled
     */
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

        // Update event in Firestore - move user from selected to cancelled
        db.collection("events").document(item.getEventId())
                .update(
                        "selected", com.google.firebase.firestore.FieldValue.arrayRemove(userId),
                        "cancelled", com.google.firebase.firestore.FieldValue.arrayUnion(userId)
                )
                .addOnSuccessListener(aVoid -> {
                    markNotificationAsRead(item);
                    Toast.makeText(this, "Declined spot for " + item.getEventName(), Toast.LENGTH_LONG).show();
                    Log.d(TAG, "User declined event: " + item.getEventId());

                    // Remove from pending invitations if this is an Entrant
                    if (currentEntrant != null) {
                        removeFromPendingInvitations(item.getEventId());
                    }

                    // Remove action buttons since decision is made
                    item.setActionRequired(false);
                    adapter.notifyItemChanged(notificationList.indexOf(item));

                    // The LotterySystem will automatically draw a replacement
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to decline lottery win: " + e.getMessage());
                    Toast.makeText(this, "Failed to decline: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Remove event from entrant's pending invitations
     */
    private void removeFromPendingInvitations(String eventId) {
        if (currentDeviceId == null) return;

        db.collection("profiles").document(currentDeviceId)
                .update("pendingInvitations", com.google.firebase.firestore.FieldValue.arrayRemove(eventId))
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Removed from pending invitations: " + eventId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to remove from pending invitations: " + e.getMessage());
                });
    }

    /**
     * Mark notification as read in the profile's document
     */
    private void markNotificationAsRead(NotificationItem item) {
        if (currentDeviceId == null) return;

        // Update the specific notification in the list
        item.setRead(true);
        item.setActionRequired(false);

        // Update the entire notifications list in Firestore
        db.collection("profiles").document(currentDeviceId)
                .update("notifications", notificationList)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Notifications marked as read and updated");
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update notifications: " + e.getMessage());
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove the Firestore listener to prevent memory leaks
        if (profileListener != null) {
            profileListener.remove();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh notifications when activity resumes
        if (currentDeviceId != null || currentUserId != null) {
            loadNotifications();
        }
    }
}