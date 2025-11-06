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

import java.util.HashMap;
import java.util.Map;

public class EventDetailActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "eventId";

    // DB + schema
    private final Database db = new Database();
    private static final String COL_EVENTS     = "events";
    private static final String COL_WAITLISTS  = "waitlists"; // optional per-user doc

    private static final String F_NAME      = "name";
    private static final String F_LOCATION  = "location";
    private static final String F_STATUS    = "status";          // "Open"/"Full"/"Closed"
    private static final String F_WAITING   = "waitingCount";    // int
    private static final String F_AVAILABLE = "availableCount";  // int

    private String eventId;

    // UI
    private TextView titleEvent, statusEvent, locationEvent, tvEntrants, tvAvailable;
    private Button btnJoin, btnLeave;
    private ImageButton btnBack;
    private ImageView qrIcon;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        titleEvent    = findViewById(R.id.title_event);
        statusEvent   = findViewById(R.id.event_status);
        locationEvent = findViewById(R.id.location_event);
        tvEntrants    = findViewById(R.id.tvEntrants);
        tvAvailable   = findViewById(R.id.tvAvailable);
        btnJoin       = findViewById(R.id.btnJoin);
        btnLeave      = findViewById(R.id.btnLeave);
        btnBack       = findViewById(R.id.btnBack);   // make sure this exists in XML
        qrIcon        = findViewById(R.id.qr_icon);

        // 1) Grab eventId
        Intent src = getIntent();
        eventId = src.getStringExtra(EXTRA_EVENT_ID);
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Missing eventId", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 2) Initial render (may be null until first snapshot)
        renderFrom(db.get(COL_EVENTS, eventId));

        // 3) Live updates
        db.subscribe(COL_EVENTS, eventId, () -> renderFrom(db.get(COL_EVENTS, eventId)));

        // 4) Back / QR
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        qrIcon.setOnClickListener(v -> startActivity(new Intent(this, QRCodeScanner.class)));

        // 5) Join/Leave
        btnJoin.setOnClickListener(v -> {
            Map<String, Object> ev = ensureDefaults(db.get(COL_EVENTS, eventId));
            int waiting   = asInt(ev.get(F_WAITING));
            int available = asInt(ev.get(F_AVAILABLE));
            ev.put(F_WAITING, waiting + 1);
            ev.put(F_AVAILABLE, Math.max(0, available - 1));
            db.save(COL_EVENTS, eventId, ev);

            String userId = "demo-user"; // TODO: real auth uid
            Map<String, Object> wl = new HashMap<>();
            wl.put("eventId", eventId);
            wl.put("userId", userId);
            db.save(COL_WAITLISTS, eventId + ":" + userId, wl);

            Toast.makeText(this, "Joined waiting list!", Toast.LENGTH_SHORT).show();
        });

        btnLeave.setOnClickListener(v -> {
            Map<String, Object> ev = ensureDefaults(db.get(COL_EVENTS, eventId));
            int waiting   = asInt(ev.get(F_WAITING));
            int available = asInt(ev.get(F_AVAILABLE));
            ev.put(F_WAITING, Math.max(0, waiting - 1));
            ev.put(F_AVAILABLE, available + 1);
            db.save(COL_EVENTS, eventId, ev);

            String userId = "demo-user";
            db.delete(COL_WAITLISTS, eventId + ":" + userId);
            Toast.makeText(this, "Left waiting list!", Toast.LENGTH_SHORT).show();
        });
    }

    private Map<String, Object> ensureDefaults(Map<String, Object> ev) {
        if (ev == null) ev = new HashMap<>();
        if (!ev.containsKey(F_NAME))      ev.put(F_NAME, "Untitled Event");
        if (!ev.containsKey(F_LOCATION))  ev.put(F_LOCATION, "");
        if (!ev.containsKey(F_STATUS))    ev.put(F_STATUS, "Open");
        if (!ev.containsKey(F_WAITING))   ev.put(F_WAITING, 0);
        if (!ev.containsKey(F_AVAILABLE)) ev.put(F_AVAILABLE, 0);
        return ev;
    }

    private void renderFrom(@Nullable Map<String, Object> ev) {
        if (ev == null) {
            // Optional: show placeholders while waiting for snapshot
            titleEvent.setText("Loading...");
            locationEvent.setText("");
            statusEvent.setText("");
            tvEntrants.setText("0 entrants");
            tvAvailable.setText("0");
            return;
        }
        String name     = str(ev.get(F_NAME));
        String location = str(ev.get(F_LOCATION));
        String status   = str(ev.get(F_STATUS));
        int waiting     = asInt(ev.get(F_WAITING));
        int available   = asInt(ev.get(F_AVAILABLE));

        titleEvent.setText(name.isEmpty() ? "Untitled Event" : name);
        locationEvent.setText(location);
        statusEvent.setText(status.isEmpty() ? "" : status.toUpperCase());
        applyStatusTint(status);
        tvEntrants.setText(waiting + " entrants");
        tvAvailable.setText(String.valueOf(available));
    }

    private void applyStatusTint(String status) {
        @ColorInt int color;
        if ("Open".equalsIgnoreCase(status)) {
            color = ContextCompat.getColor(this, android.R.color.holo_green_dark);
        } else if ("Full".equalsIgnoreCase(status) || "Closed".equalsIgnoreCase(status)) {
            color = ContextCompat.getColor(this, android.R.color.darker_gray);
        } else {
            color = ContextCompat.getColor(this, android.R.color.black);
        }
        statusEvent.setTextColor(color);
    }

    private static int asInt(Object o) {
        if (o instanceof Number) return ((Number) o).intValue();
        if (o instanceof String) try { return Integer.parseInt((String) o); } catch (Exception ignored) {}
        return 0;
    }
    private static String str(Object o) { return o == null ? "" : String.valueOf(o); }
}
