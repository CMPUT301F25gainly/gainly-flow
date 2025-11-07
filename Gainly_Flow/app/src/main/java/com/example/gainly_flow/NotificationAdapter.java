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
 * {@code NotificationAdapter} is a RecyclerView adapter responsible for
 * displaying a list of notifications within the app. Each notification may
 * optionally provide "Accept" and "Decline" actions, which are handled via a
 * callback listener interface.
 *
 * <p>This adapter binds data from {@link NotificationItem} objects to a
 * custom layout defined in {@code item_notification.xml}. It supports dynamic
 * visibility of action buttons based on whether the notification requires
 * user interaction.</p>
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    /** List of notification items to be displayed. */
    private final List<NotificationItem> notifications;

    /** Listener for handling user actions such as Accept or Decline. */
    private final OnActionClickListener actionClickListener;

    /**
     * Interface definition for handling user interaction on notification items.
     */
    public interface OnActionClickListener {
        /**
         * Called when the user clicks the "Accept" button on a notification item.
         *
         * @param item the {@link NotificationItem} that was accepted
         */
        void onAcceptClicked(NotificationItem item);

        /**
         * Called when the user clicks the "Decline" button on a notification item.
         *
         * @param item the {@link NotificationItem} that was declined
         */
        void onDeclineClicked(NotificationItem item);
    }

    /**
     * Constructs a new {@code NotificationAdapter}.
     *
     * @param notifications the list of notifications to display
     * @param listener the callback listener for user actions
     */
    public NotificationAdapter(List<NotificationItem> notifications, OnActionClickListener listener) {
        this.notifications = notifications;
        this.actionClickListener = listener;
    }

    /**
     * Inflates the layout for each notification item and creates a {@link ViewHolder}.
     *
     * @param parent the parent view group
     * @param viewType the view type of the new view
     * @return a new {@link ViewHolder} instance containing the inflated view
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Binds the data from a {@link NotificationItem} to the views in the {@link ViewHolder}.
     * Displays the notification's title, description, timestamp, and action buttons if applicable.
     *
     * @param holder the {@link ViewHolder} to bind data to
     * @param position the position of the item within the adapter
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationItem item = notifications.get(position);
        holder.tvTitle.setText(item.getTitle());
        holder.tvDescription.setText(item.getDescription());

        // Format timestamp to human-readable form
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        holder.tvTime.setText(sdf.format(item.getTimestamp()));

        // Toggle visibility of action buttons depending on item state
        if (item.isActionRequired()) {
            holder.layoutActions.setVisibility(View.VISIBLE);
            holder.btnAccept.setOnClickListener(v -> actionClickListener.onAcceptClicked(item));
            holder.btnDecline.setOnClickListener(v -> actionClickListener.onDeclineClicked(item));
        } else {
            holder.layoutActions.setVisibility(View.GONE);
        }
    }

    /**
     * Returns the total number of notifications managed by this adapter.
     *
     * @return the size of the {@code notifications} list
     */
    @Override
    public int getItemCount() {
        return notifications.size();
    }

    /**
     * {@code ViewHolder} class that holds and manages references to the views used
     * in each notification item layout. It includes UI components for displaying
     * the notification’s title, description, timestamp, and action buttons.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        /** Text view displaying the notification title. */
        TextView tvTitle;

        /** Text view displaying the notification description. */
        TextView tvDescription;

        /** Text view displaying the formatted timestamp. */
        TextView tvTime;

        /** Layout container holding the action buttons. */
        LinearLayout layoutActions;

        /** Button allowing the user to accept the notification action. */
        Button btnAccept;

        /** Button allowing the user to decline the notification action. */
        Button btnDecline;

        /**
         * Constructs a new {@code ViewHolder} and initializes its view references.
         *
         * @param itemView the root view of the notification item layout
         */
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvDescription = itemView.findViewById(R.id.tv_description);
            tvTime = itemView.findViewById(R.id.tv_time);
            layoutActions = itemView.findViewById(R.id.layout_actions);
            btnAccept = itemView.findViewById(R.id.btn_accept);
            btnDecline = itemView.findViewById(R.id.btn_decline);
        }
    }
}
