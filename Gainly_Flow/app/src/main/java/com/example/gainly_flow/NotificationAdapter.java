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

/**
 * RecyclerView adapter that renders entrant notifications with type-based styling,
 * action buttons for lottery invitations, and visual read/unread feedback.
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private static final int VIEW_TYPE_DEFAULT = 0;
    private static final int VIEW_TYPE_NOT_SELECTED = 1;

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
        int layoutRes = viewType == VIEW_TYPE_NOT_SELECTED ? R.layout.item_notification_not_selected
                : R.layout.item_notification;

        View view = LayoutInflater.from(parent.getContext())
                .inflate(layoutRes, parent, false);
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
            switch (type.toUpperCase(Locale.ROOT)) {
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
                case "LOSE":
                    color = holder.itemView.getContext().getResources().getColor(android.R.color.holo_red_dark);
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
    public int getItemViewType(int position) {
        NotificationItem item = notifications.get(position);
        if (isLoseType(item.getType())) {
            return VIEW_TYPE_NOT_SELECTED;
        }
        return VIEW_TYPE_DEFAULT;
    }

    private boolean isLoseType(String type) {
        return type != null && type.equalsIgnoreCase(NotificationItem.NotificationType.LOSE.name());
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
