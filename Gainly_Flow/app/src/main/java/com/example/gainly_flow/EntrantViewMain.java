package com.example.gainly_flow;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

public class EntrantViewMain extends AppCompatActivity {

    private MaterialButton browseEventsButton;
    private BottomNavigationView bottomNav;
    private LinearLayout eventListContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrant_home);

        browseEventsButton = findViewById(R.id.browseEventsButton);
        bottomNav = findViewById(R.id.bottomNav);
        eventListContainer = findViewById(R.id.eventListContainer);

        // Button to QR Page
        browseEventsButton.setOnClickListener(v -> {
            Intent toQR = new Intent(EntrantViewMain.this, QRCodeScanner.class);
            startActivity(toQR);
        });

        // Add Click for Each Event
        // Each <include layout="@layout/item_event"/> inside eventListContainer will become a child view.
        for (int i = 0; i < eventListContainer.getChildCount(); i++) {
            View eventView = eventListContainer.getChildAt(i);

            // Each event card can open a detailed page or show a Toast for now
            eventView.setOnClickListener(v -> {
                Toast.makeText(EntrantViewMain.this, "Event clicked!", Toast.LENGTH_SHORT).show();

                // Example: Go to Event Details activity
                Intent intent = new Intent(EntrantViewMain.this, EventDetailActivity.class);
                startActivity(intent);
            });
        }

        // Handle Bottom Navigation
        bottomNav.setOnItemSelectedListener(this::onNavItemSelected);
    }

    private boolean onNavItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_events) {
            Toast.makeText(this, "Events", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.menu_notifications) {
            Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.menu_profile) {
            Toast.makeText(this, "Profile", Toast.LENGTH_SHORT).show();
        }
        return true;
    }
}
