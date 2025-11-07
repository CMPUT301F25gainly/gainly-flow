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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        recyclerView = findViewById(R.id.recyclerViewNotifications);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        backButton = findViewById(R.id.backButton_notification);

        backButton.setOnClickListener(v -> {
            onBackPressed();
        });

        adapter = new NotificationAdapter(notificationList, new NotificationAdapter.OnActionClickListener() {
            @Override
            public void onAcceptClicked(NotificationItem item) {
                Toast.makeText(NotificationsActivity.this, "Accepted: " + item.getTitle(), Toast.LENGTH_SHORT).show();

                // 🔹 Optionally update Firestore (e.g., mark delivered or accepted)
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("notifications")
                        .document(item.getId())
                        .update("delivered", true)  // or any field you want
                        .addOnSuccessListener(aVoid -> {
                            // 🔹 Hide buttons locally
                            item.setActionRequired(false);
                            adapter.notifyDataSetChanged();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(NotificationsActivity.this, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                        );
            }

            @Override
            public void onDeclineClicked(NotificationItem item) {
                Toast.makeText(NotificationsActivity.this, "Declined: " + item.getTitle(), Toast.LENGTH_SHORT).show();

                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("notifications")
                        .document(item.getId())
                        .update("delivered", true) // same logic as above
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

        loadNotifications();
    }

    private void loadNotifications() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String currentUserId = "22ae419f5bed11cd"; // Replace with actual logged-in user ID

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
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load notifications: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

}
