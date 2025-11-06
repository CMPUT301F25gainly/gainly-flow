package com.example.gainly_flow;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class EventDetailActivity extends AppCompatActivity {

    private TextView titleEvent, statusEvent, locationEvent, tvEntrants, tvAvailable;
    private Button btnJoin, btnLeave;
    private ImageButton btnBack;           // <-- declare only
    private ImageView qrIcon;

    // local UI state (simple demo for join/leave)
    private boolean joined = false;
    private int waitingCount = 0;
    private int availableCount = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        // ---- Bind views from XML (AFTER setContentView) ----
        titleEvent    = findViewById(R.id.title_event);
        statusEvent   = findViewById(R.id.event_status);
        locationEvent = findViewById(R.id.location_event);
        tvEntrants    = findViewById(R.id.tvEntrants);
        tvAvailable   = findViewById(R.id.tvAvailable);
        btnJoin       = findViewById(R.id.btnJoin);
        btnLeave      = findViewById(R.id.btnLeave);
        qrIcon        = findViewById(R.id.qr_icon);
        btnBack       = findViewById(R.id.btnBack);       // <-- now valid

        // ---- Read extras from the intent ----
        Intent src = getIntent();
        String title     = src.getStringExtra("title");
        String status    = src.getStringExtra("status");
        String location  = src.getStringExtra("location");
        int capacity     = src.getIntExtra("capacity", -1); // (optional, not used here)
        waitingCount     = src.getIntExtra("waiting", -1);
        availableCount   = src.getIntExtra("available", -1);

        if (title != null)      titleEvent.setText(title);
        if (location != null)   locationEvent.setText(location);

        if (status != null) {
            statusEvent.setText(status.toUpperCase());
            applyStatusTint(status);
        }

        if (waitingCount >= 0)   tvEntrants.setText(waitingCount + " entrants");
        if (availableCount >= 0) tvAvailable.setText(String.valueOf(availableCount));

        // ---- Buttons ----
        btnBack.setOnClickListener(v -> finish());

        btnJoin.setOnClickListener(v -> {
            if (joined) {
                Toast.makeText(this, "You’re already on the list.", Toast.LENGTH_SHORT).show();
                return;
            }
            joined = true;
            waitingCount = Math.max(0, waitingCount) + 1;
            tvEntrants.setText(waitingCount + " entrants");
            Toast.makeText(this, "Joined waiting list!", Toast.LENGTH_SHORT).show();
        });

        btnLeave.setOnClickListener(v -> {
            if (!joined) {
                Toast.makeText(this, "You’re not on the list.", Toast.LENGTH_SHORT).show();
                return;
            }
            joined = false;
            waitingCount = Math.max(0, waitingCount - 1);
            tvEntrants.setText(waitingCount + " entrants");
            Toast.makeText(this, "Left waiting list!", Toast.LENGTH_SHORT).show();
        });

        qrIcon.setOnClickListener(v -> {
            Intent toQR = new Intent(this, QRCodeScanner.class);
            startActivity(toQR);
        });
    }

    private void applyStatusTint(String status) {
        @ColorInt int color;
        if ("Open".equalsIgnoreCase(status) || "OPEN".equals(status)) {
            // Use built-in colors to avoid missing resource errors
            color = ContextCompat.getColor(this, android.R.color.holo_green_dark);
        } else if ("Full".equalsIgnoreCase(status) || "CLOSED".equalsIgnoreCase(status)) {
            color = ContextCompat.getColor(this, android.R.color.darker_gray);
        } else {
            color = ContextCompat.getColor(this, android.R.color.black);
        }
        statusEvent.setTextColor(color);
    }
}
