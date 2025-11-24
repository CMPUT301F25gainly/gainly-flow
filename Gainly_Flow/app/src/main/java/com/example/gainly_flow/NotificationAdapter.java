package com.example.gainly_flow;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private final List<NotificationItem> notifications;
    private final OnActionClickListener actionClickListener;

    public NotificationAdapter(List<NotificationItem> notifications, OnActionClickListener listener) {
        this.notifications = notifications;
        this.actionClickListener = listener;
    }

    public interface OnActionClickListener {
        void onAcceptClicked(NotificationItem item);
        void onDeclineClicked(NotificationItem item);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationItem item = notifications.get(position);

        // Set notification data
        holder.tvTitle.setText(item.getDisplayTitle());
        holder.tvDescription.setText(item.getDisplayMessage());

        // Format and set timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        holder.tvTime.setText(sdf.format(item.getTimestamp()));

        // Set indicator bar color based on notification type
        setIndicatorColor(holder, item.getType());

        // Show/hide action buttons based on whether action is required
        if (item.isActionRequired() && item.getType() != null &&
                item.getType().equals(NotificationItem.NotificationType.WIN.name())) {
            holder.layoutActions.setVisibility(View.VISIBLE);

            // Set click listeners
            holder.btnAccept.setOnClickListener(v -> {
                if (actionClickListener != null) {
                    actionClickListener.onAcceptClicked(item);
                }
            });

            holder.btnDecline.setOnClickListener(v -> {
                if (actionClickListener != null) {
                    actionClickListener.onDeclineClicked(item);
                }
            });
        } else {
            holder.layoutActions.setVisibility(View.GONE);
        }

        // Visual feedback for read/unread
        if (item.isRead()) {
            holder.itemView.setAlpha(0.6f);
        } else {
            holder.itemView.setAlpha(1.0f);
        }
    }

    private void setIndicatorColor(ViewHolder holder, String type) {
        int color;

        if (type != null) {
            switch (type) {
                case "WIN":
                    color = holder.itemView.getContext().getResources().getColor(android.R.color.holo_green_dark);
                    break;
                case "INFO":
                    color = holder.itemView.getContext().getResources().getColor(android.R.color.holo_blue_dark);
                    break;
                case "REMINDER":
                    color = holder.itemView.getContext().getResources().getColor(android.R.color.holo_orange_dark);
                    break;
                case "UPDATE":
                    color = holder.itemView.getContext().getResources().getColor(android.R.color.holo_purple);
                    break;
                case "CANCELLATION":
                    color = holder.itemView.getContext().getResources().getColor(android.R.color.holo_red_dark);
                    break;
                case "INVITATION":
                    color = holder.itemView.getContext().getResources().getColor(android.R.color.holo_purple);
                    break;
                default:
                    color = holder.itemView.getContext().getResources().getColor(android.R.color.darker_gray);
            }
        } else {
            color = holder.itemView.getContext().getResources().getColor(android.R.color.darker_gray);
        }

        holder.indicatorBar.setBackgroundColor(color);
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvDescription;
        TextView tvTime;
        LinearLayout layoutActions;
        Button btnAccept;
        Button btnDecline;
        View indicatorBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvDescription = itemView.findViewById(R.id.tv_description);
            tvTime = itemView.findViewById(R.id.tv_time);
            layoutActions = itemView.findViewById(R.id.layout_actions);
            btnAccept = itemView.findViewById(R.id.btn_accept);
            btnDecline = itemView.findViewById(R.id.btn_decline);
            indicatorBar = itemView.findViewById(R.id.indicator_bar);
        }
    }
}