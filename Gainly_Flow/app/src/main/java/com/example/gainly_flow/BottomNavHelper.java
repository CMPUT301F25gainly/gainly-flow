package com.example.gainly_flow;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Centralizes bottom navigation wiring for entrant-facing screens so each activity
 * can share consistent tab behavior and extras when switching destinations.
 */
public class BottomNavHelper {

    public static void setupBottomNav(Activity activity, BottomNavigationView bottomNav, Entrant entrant,
            Profile profile) {
        if (bottomNav == null)
            return;

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Intent intent = null;

            if (id == R.id.menu_events) {
                if (!(activity instanceof EntrantViewMain)) {
                    intent = new Intent(activity, EntrantViewMain.class);
                }
            } else if (id == R.id.menu_notifications) {
                if (!(activity instanceof NotificationsActivity)) {
                    intent = new Intent(activity, NotificationsActivity.class);
                }
            } else if (id == R.id.menu_profile) {
                if (!(activity instanceof ProfileActivity)) {
                    intent = new Intent(activity, ProfileActivity.class);
                    intent.putExtra("userType", "Entrant");
                }
            }

            if (intent != null) {
                if (entrant != null) {
                    intent.putExtra("entrant", entrant);
                    // For ProfileActivity, it expects "profile" extra for the data
                    if (id == R.id.menu_profile) {
                        intent.putExtra("profile", entrant);
                    }
                } else if (profile != null) {
                    intent.putExtra("profile", profile);
                }

                // If we are going to ProfileActivity, we need to ensure userType is set
                if (id == R.id.menu_profile) {
                    intent.putExtra("userType", "Entrant");
                }

                activity.startActivity(intent);
                // Optional: activity.overridePendingTransition(0, 0); to remove animation
                return true;
            }

            return true;
        });
    }

    public static void setSelectedItem(Activity activity, BottomNavigationView bottomNav) {
        if (bottomNav == null)
            return;

        int id = -1;
        if (activity instanceof EntrantViewMain) {
            id = R.id.menu_events;
        } else if (activity instanceof NotificationsActivity) {
            id = R.id.menu_notifications;
        } else if (activity instanceof ProfileActivity) {
            id = R.id.menu_profile;
        } else if (activity instanceof EventDetailActivity) {
            id = R.id.menu_events;
        }
        // Add other activities if they map to a specific tab
        // e.g. add more screens to preserve selection state

        if (id != -1) {
            bottomNav.getMenu().findItem(id).setChecked(true);
        }
    }
}
