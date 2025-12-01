package com.example.gainly_flow;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Activity for administrators to review logs of all notifications sent to
 * entrants.
 * Implements US 03.08.01: "As an administrator, I want to review logs of all
 * notifications
 * sent to entrants by organizers."
 */
public class AdminNotificationLogsActivity extends AppCompatActivity {

    private static final String TAG = "AdminNotificationLogs";

    private RecyclerView recyclerView;
    private LogAdapter logAdapter;
    private List<NotificationItem> logList;
    private TextView tvLogCount;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_notification_logs);

        logList = new ArrayList<>();

        initializeViews();
        setupRecyclerView();
        loadLogs();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recycler_logs);
        tvLogCount = findViewById(R.id.tv_log_count);
        btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        logAdapter = new LogAdapter(logList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(logAdapter);
    }

    /**
     * Loads all notification logs from Firestore 'notifications' collection.
     */
    private void loadLogs() {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("notifications")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    logList.clear();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        try {
                            NotificationItem item = new NotificationItem(doc);
                            logList.add(item);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing notification: " + e.getMessage());
                        }
                    }

                    // Sort by timestamp (most recent first)
                    Collections.sort(logList, (a, b) -> {
                        if (a.getTimestamp() == null)
                            return 1;
                        if (b.getTimestamp() == null)
                            return -1;
                        return b.getTimestamp().compareTo(a.getTimestamp());
                    });

                    updateUI();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching notifications: " + e.getMessage());
                    Toast.makeText(this, "Error loading logs: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUI() {
        tvLogCount.setText("Total Logs: " + logList.size());
        logAdapter.notifyDataSetChanged();

        if (logList.isEmpty()) {
            Toast.makeText(this, "No notification logs found", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * RecyclerView adapter for displaying notification logs.
     */
    private class LogAdapter extends RecyclerView.Adapter<LogAdapter.LogViewHolder> {
        private final List<NotificationItem> logs;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

        public LogAdapter(List<NotificationItem> logs) {
            this.logs = logs;
        }

        @NonNull
        @Override
        public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_notification_log, parent, false);
            return new LogViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
            NotificationItem entry = logs.get(position);
            holder.bind(entry);
        }

        @Override
        public int getItemCount() {
            return logs.size();
        }

        class LogViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvRecipient;
            private final TextView tvEvent;
            private final TextView tvMessage;
            private final TextView tvTimestamp;
            private final TextView tvStatus;
            private final View statusIndicator;

            public LogViewHolder(@NonNull View itemView) {
                super(itemView);
                tvRecipient = itemView.findViewById(R.id.tv_recipient);
                tvEvent = itemView.findViewById(R.id.tv_event);
                tvMessage = itemView.findViewById(R.id.tv_message);
                tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
                tvStatus = itemView.findViewById(R.id.tv_status);
                statusIndicator = itemView.findViewById(R.id.status_indicator);
            }

            public void bind(NotificationItem entry) {
                // Set recipient
                String recipientText = "Recipient: "
                        + (entry.getRecipientId() != null ? entry.getRecipientId() : "Unknown");
                if (recipientText.length() > 40) {
                    recipientText = recipientText.substring(0, 37) + "...";
                }
                tvRecipient.setText(recipientText);

                // Set event
                String eventText = "Event: " + (entry.getEventId() != null ? entry.getEventId() : "N/A");
                if (eventText.length() > 50) {
                    eventText = eventText.substring(0, 47) + "...";
                }
                tvEvent.setText(eventText);

                // Set message
                String message = entry.getMessage();
                if (message == null || message.isEmpty()) {
                    message = entry.getTitle();
                }
                tvMessage.setText(message != null ? message : "No content");

                // Set timestamp
                if (entry.getTimestamp() != null) {
                    tvTimestamp.setText(getRelativeTime(entry.getTimestamp().getTime()));
                } else {
                    tvTimestamp.setText("Unknown time");
                }

                // Set status - Assuming SENT for all existing notifications in this collection
                // We could also check if it's read or not, but for "Log" purposes, "SENT" is
                // appropriate
                // unless we want to show "READ" vs "UNREAD".
                // Let's stick to "SENT" as per the plan, or maybe "DELIVERED".
                String status = "SENT";
                tvStatus.setText(status);

                // Set status color (Green for SENT)
                tvStatus.setBackgroundTintList(
                        getResources().getColorStateList(android.R.color.holo_green_light, null));
                tvStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark, null));
                statusIndicator
                        .setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_green_dark, null));
            }

            /**
             * Converts timestamp to relative time string (e.g., "2 hours ago").
             */
            private String getRelativeTime(long timestamp) {
                long now = System.currentTimeMillis();
                long diff = now - timestamp;

                long seconds = diff / 1000;
                long minutes = seconds / 60;
                long hours = minutes / 60;
                long days = hours / 24;

                if (seconds < 60) {
                    return "Just now";
                } else if (minutes < 60) {
                    return minutes + " min ago";
                } else if (hours < 24) {
                    return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
                } else if (days < 7) {
                    return days + " day" + (days > 1 ? "s" : "") + " ago";
                } else {
                    return dateFormat.format(timestamp);
                }
            }
        }
    }
}