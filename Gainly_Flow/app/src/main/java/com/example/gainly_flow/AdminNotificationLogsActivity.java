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
 * Activity for administrators to review logs of all notifications sent to entrants.
 * Implements US 03.08.01: "As an administrator, I want to review logs of all notifications
 * sent to entrants by organizers."
 */
public class AdminNotificationLogsActivity extends AppCompatActivity {

    private static final String TAG = "AdminNotificationLogs";

    private RecyclerView recyclerView;
    private LogAdapter logAdapter;
    private List<NotificationLog.Entry> logList;
    private TextView tvLogCount;
    private ImageButton btnBack;
    private NotificationLog notificationLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_notification_logs);

        notificationLog = new NotificationLog();
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
     * Loads all notification logs from Firestore.
     */
    private void loadLogs() {
        notificationLog.listAll(logs -> {
            logList.clear();
            logList.addAll(logs);

            // Sort by timestamp (most recent first)
            Collections.sort(logList, (a, b) -> {
                if (a.timestamp == null) return 1;
                if (b.timestamp == null) return -1;
                return b.timestamp.compareTo(a.timestamp);
            });

            updateUI();
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
        private final List<NotificationLog.Entry> logs;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

        public LogAdapter(List<NotificationLog.Entry> logs) {
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
            NotificationLog.Entry entry = logs.get(position);
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

            public void bind(NotificationLog.Entry entry) {
                // Set recipient
                String recipientText = "Recipient: " + (entry.to != null ? entry.to : "Unknown");
                if (recipientText.length() > 40) {
                    recipientText = recipientText.substring(0, 37) + "...";
                }
                tvRecipient.setText(recipientText);

                // Set event
                String eventText = "Event: " + (entry.eventId != null ? entry.eventId : "Unknown");
                if (eventText.length() > 50) {
                    eventText = eventText.substring(0, 47) + "...";
                }
                tvEvent.setText(eventText);

                // Set message
                tvMessage.setText(entry.message != null ? entry.message : "No message");

                // Set timestamp
                if (entry.timestamp != null) {
                    tvTimestamp.setText(getRelativeTime(entry.timestamp.getTime()));
                } else {
                    tvTimestamp.setText("Unknown time");
                }

                // Set status
                String status = entry.status != null ? entry.status.toUpperCase() : "UNKNOWN";
                tvStatus.setText(status);

                // Set status color
                if ("SENT".equals(status)) {
                    tvStatus.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_green_light, null));
                    tvStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark, null));
                    statusIndicator.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_green_dark, null));
                } else if ("FAILED".equals(status)) {
                    tvStatus.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_red_light, null));
                    tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark, null));
                    statusIndicator.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_red_dark, null));
                } else {
                    tvStatus.setBackgroundTintList(getResources().getColorStateList(android.R.color.darker_gray, null));
                    tvStatus.setTextColor(getResources().getColor(android.R.color.black, null));
                    statusIndicator.setBackgroundTintList(getResources().getColorStateList(android.R.color.darker_gray, null));
                }
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