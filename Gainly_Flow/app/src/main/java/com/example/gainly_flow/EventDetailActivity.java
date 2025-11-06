package com.example.gainly_flow;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
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
    private ImageView qrIcon;

    // local UI state (simple demo for join/leave)
    private boolean joined = false;
    private int waitingCount = 0;
    private int availableCount = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        // ---- Bind views from XML ----
        titleEvent    = findViewById(R.id.title_event);     // title text
        statusEvent   = findViewById(R.id.event_status);    // OPEN / FULL badge
        locationEvent = findViewById(R.id.location_event);  // small subtitle/location
        tvEntrants    = findViewById(R.id.tvEntrants);      // "45 entrants"
        tvAvailable   = findViewById(R.id.tvAvailable);     // "20"
        btnJoin       = findViewById(R.id.btnJoin);
        btnLeave      = findViewById(R.id.btnLeave);
        qrIcon        = findViewById(R.id.qr_icon);
        // (All IDs exist in your layout. :contentReference[oaicite:0]{index=0})

        // ---- Read extras from the intent (sent by EntrantViewMain) ----
        Intent src = getIntent();
        String title     = src.getStringExtra("title");
        String status    = src.getStringExtra("status");     // e.g., "Open" / "Full"
        String location  = src.getStringExtra("location");   // optional
        int capacity     = src.getIntExtra("capacity", -1);  // optional informational
        waitingCount     = src.getIntExtra("waiting", -1);
        availableCount   = src.getIntExtra("available", -1);

        // Fallbacks to whatever is in the XML if extras weren’t provided
        if (title != null)      titleEvent.setText(title);
        if (location != null)   locationEvent.setText(location);

        // Status badge text + color
        if (status != null) {
            statusEvent.setText(status.toUpperCase());
            applyStatusTint(status);
        }

        // Counts
        if (waitingCount >= 0)  tvEntrants.setText(waitingCount + " entrants");
        if (availableCount >= 0) tvAvailable.setText(String.valueOf(availableCount));

        // ---- Buttons ----
        btnJoin.setOnClickListener(v -> {
            if (joined) {
                Toast.makeText(this, "You’re already on the list.", Toast.LENGTH_SHORT).show();
                return;
            }
            joined = true;
            waitingCount = Math.max(0, waitingCount) + 1;  // ensure >= 0 then +1
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

        // ---- QR icon → open scanner ----
        qrIcon.setOnClickListener(v -> {
            Intent toQR = new Intent(this, QRCodeScanner.class);
            startActivity(toQR);
        });
    }

    private void applyStatusTint(String status) {
        @ColorInt int color;
        if ("Open".equalsIgnoreCase(status) || "OPEN".equals(status)) {
            color = ContextCompat.getColor(this, R.color.green_500);
        } else if ("Full".equalsIgnoreCase(status) || "CLOSED".equalsIgnoreCase(status)) {
            color = ContextCompat.getColor(this, R.color.gray_700);
        } else {
            color = ContextCompat.getColor(this, android.R.color.black);
        }
        statusEvent.setTextColor(color);
        // status badge is a drawable; tinting text is enough for now (badge is set in XML). :contentReference[oaicite:1]{index=1}
    }
}
