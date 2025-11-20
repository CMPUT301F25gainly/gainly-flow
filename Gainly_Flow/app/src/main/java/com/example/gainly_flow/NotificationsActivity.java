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

public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<NotificationItem> notificationList = new ArrayList<>();
    private ImageButton backButton;

    private String currentUserId = "22ae419f5bed11cd";
    // TODO: Replace with real logged-in entrant ID

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        recyclerView = findViewById(R.id.recyclerViewNotifications);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        backButton = findViewById(R.id.backButton_notification);

        backButton.setOnClickListener(v -> onBackPressed());

        adapter = new NotificationAdapter(notificationList,
                new NotificationAdapter.OnActionClickListener() {

                    @Override
                    public void onAcceptClicked(NotificationItem item) {
                        handleAction(item, "Accepted");
                    }

                    @Override
                    public void onDeclineClicked(NotificationItem item) {
                        handleAction(item, "Declined");
                    }
                });

        recyclerView.setAdapter(adapter);

        loadNotifications();
    }

    /**
     * Accept / Decline handler
     */
    private void handleAction(NotificationItem item, String action) {
        Toast.makeText(this, action + ": " + item.getMessage(), Toast.LENGTH_SHORT).show();

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("entrants")
                .document(currentUserId)
                .collection("notifications")
                .document(item.getId())
                .update("delivered", true)
                .addOnSuccessListener(aVoid -> {
                    item.setActionRequired(false);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Loads notifications from:
     * entrants/{currentUserId}/notifications
     */
    private void loadNotifications() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("entrants")
                .document(currentUserId)
                .collection("notifications")
                .orderBy("timestamp")
                .get()
                .addOnSuccessListener(snapshot -> {
                    notificationList.clear();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        NotificationItem item = doc.toObject(NotificationItem.class);
                        if (item != null) {
                            item.setId(doc.getId()); // IMPORTANT: Save Firestore doc ID
                            notificationList.add(item);
                        }
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load notifications: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
