package com.example.gainly_flow;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays all notifications sent to the currently logged-in entrant.
 * <p>
 * This activity retrieves notification documents from Firestore's
 * <b>"notifications"</b> collection and displays them in a {@link RecyclerView}
 * using {@link NotificationAdapter}. Users can view their notifications
 * and respond with actions such as "Accept" or "Decline".
 * <p>
 * Each action updates the corresponding Firestore document to mark
 * the notification as delivered or handled.
 *
 * @see NotificationAdapter
 * @see NotificationItem
 * @see FirebaseFirestore
 */
public class NotificationsActivity extends AppCompatActivity {

    /** RecyclerView displaying the list of notifications. */
    private RecyclerView recyclerView;

    /** Adapter handling notification item display and user actions. */
    private NotificationAdapter adapter;

    /** List of notifications fetched from Firestore. */
    private List<NotificationItem> notificationList = new ArrayList<>();

    /** Back button for returning to the previous screen. */
    private ImageButton backButton;

    /**
     * Called when the activity is created.
     * <p>
     * Initializes the layout, sets up the RecyclerView and adapter,
     * and loads notifications from Firestore for the current user.
     *
     * @param savedInstanceState the previously saved state of the activity, or {@code null} if none
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        recyclerView = findViewById(R.id.recyclerViewNotifications);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        backButton = findViewById(R.id.backButton_notification);

        // Navigate back to previous screen
        backButton.setOnClickListener(v -> onBackPressed());

        // Set up RecyclerView adapter and handle accept/decline actions
        adapter = new NotificationAdapter(notificationList, new NotificationAdapter.OnActionClickListener() {
            /**
             * Handles the Accept button click event for a notification item.
             * <p>
             * Displays a confirmation toast, updates the Firestore document to mark
             * the notification as delivered, and hides the action buttons locally.
             *
             * @param item the {@link NotificationItem} that was accepted
             */
            @Override
            public void onAcceptClicked(NotificationItem item) {
                Toast.makeText(NotificationsActivity.this, "Accepted: " + item.getTitle(), Toast.LENGTH_SHORT).show();

                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("notifications")
                        .document(item.getId())
                        .update("delivered", true)
                        .addOnSuccessListener(aVoid -> {
                            item.setActionRequired(false);
                            adapter.notifyDataSetChanged();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(NotificationsActivity.this, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                        );
            }

            /**
             * Handles the Decline button click event for a notification item.
             * <p>
             * Similar to {@link #onAcceptClicked(NotificationItem)}, this method
             * updates the Firestore record and hides the buttons locally after decline.
             *
             * @param item the {@link NotificationItem} that was declined
             */
            @Override
            public void onDeclineClicked(NotificationItem item) {
                Toast.makeText(NotificationsActivity.this, "Declined: " + item.getTitle(), Toast.LENGTH_SHORT).show();

                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("notifications")
                        .document(item.getId())
                        .update("delivered", true)
                        .addOnSuccessListener(aVoid -> {
                            item.setActionRequired(false);
                            adapter.notifyDataSetChanged();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(NotificationsActivity.this, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                        );
            }
        });

        recyclerView.setAdapter(adapter);

        // Load notifications from Firestore
        loadNotifications();
    }

    /**
     * Fetches notifications for the current user from Firestore.
     * <p>
     * Retrieves all documents in the <b>"notifications"</b> collection
     * where the {@code recipientId} field matches the logged-in user's ID.
     * On success, populates the RecyclerView; on failure, displays an error toast.
     */
    private void loadNotifications() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String currentUserId = "22ae419f5bed11cd"; // TODO: Replace with actual logged-in user ID

        db.collection("notifications")
                .whereEqualTo("recipientId", currentUserId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    notificationList.clear();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        notificationList.add(new NotificationItem(doc));
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load notifications: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
