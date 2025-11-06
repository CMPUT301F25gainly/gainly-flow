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

import com.google.firebase.Firebase;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

public class EventDetailActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "eventId";

    // Firestore-backed wrapper you already have
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private static final String COL_EVENTS     = "events";
    private static final String COL_WAITLISTS  = "waitlists";

    // Fields expected in events/{eventId}
    private static final String F_NAME        = "name";
    private static final String F_LOCATION    = "location";
    private static final String F_STATUS      = "status";
    private static final String F_WAITING     = "waitingCount";
    private static final String F_AVAILABLE   = "availableCount";
    private static final String F_DESCRIPTION = "description";
    private static final String F_REG_OPEN    = "registrationOpen";
    private static final String F_REG_CLOSE   = "registrationClose";

    private String eventId;

    // UI
    private TextView titleEvent, statusEvent, locationEvent, tvEntrants, tvAvailable;
    private TextView tvEventDuration, tvRegOpens, tvRegCloses, tvDescription;
    private Button btnJoin, btnLeave;
    private ImageButton btnBack;
    private ImageView qrIcon;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        // Find views already in your XML
        titleEvent      = findViewById(R.id.title_event);
        statusEvent     = findViewById(R.id.event_status);
        locationEvent   = findViewById(R.id.location_event);
        tvEntrants      = findViewById(R.id.tvEntrants);
        tvAvailable     = findViewById(R.id.tvAvailable);
        tvEventDuration = findViewById(R.id.tvEventDuration);
        tvRegOpens      = findViewById(R.id.tvRegOpens);
        tvRegCloses     = findViewById(R.id.tvRegCloses);
        tvDescription   = findViewById(R.id.tvDescription);
        btnJoin         = findViewById(R.id.btnJoin);
        btnLeave        = findViewById(R.id.btnLeave);
        btnBack         = findViewById(R.id.btnBack);
        qrIcon          = findViewById(R.id.qr_icon);

        Intent src = getIntent();
        eventId = src.getStringExtra(EXTRA_EVENT_ID);
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Missing eventId", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initial render (may be null until first snapshot arrives)
        renderFrom(db.get(COL_EVENTS, eventId));

        // Live updates
        db.subscribe(COL_EVENTS, eventId, () -> renderFrom(db.get(COL_EVENTS, eventId)));

        // Back + QR
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        qrIcon.setOnClickListener(v -> startActivity(new Intent(this, QRCodeScanner.class)));

        // Join / Leave (simple counter updates for now)
        btnJoin.setOnClickListener(v -> {
            Map<String, Object> ev = ensureDefaults(db.get(COL_EVENTS, eventId));
            int waiting   = i(ev.get(F_WAITING));
            int available = i(ev.get(F_AVAILABLE));
            ev.put(F_WAITING, waiting + 1);
            ev.put(F_AVAILABLE, Math.max(0, available - 1));
            db.save(COL_EVENTS, eventId, ev);

            String userId = "demo-user"; // TODO: use your real auth uid
            Map<String, Object> wl = new HashMap<>();
            wl.put("eventId", eventId);
            wl.put("userId", userId);
            db.save(COL_WAITLISTS, eventId + ":" + userId, wl);

            Toast.makeText(this, "Joined waiting list!", Toast.LENGTH_SHORT).show();
        });

        btnLeave.setOnClickListener(v -> {
            Map<String, Object> ev = ensureDefaults(db.get(COL_EVENTS, eventId));
            int waiting   = i(ev.get(F_WAITING));
            int available = i(ev.get(F_AVAILABLE));
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
        if (!ev.containsKey(F_NAME))        ev.put(F_NAME, "Untitled Event");
        if (!ev.containsKey(F_LOCATION))    ev.put(F_LOCATION, "");
        if (!ev.containsKey(F_STATUS))      ev.put(F_STATUS, "Open");
        if (!ev.containsKey(F_WAITING))     ev.put(F_WAITING, 0);
        if (!ev.containsKey(F_AVAILABLE))   ev.put(F_AVAILABLE, 0);
        if (!ev.containsKey(F_DESCRIPTION)) ev.put(F_DESCRIPTION, "");
        return ev;
    }

    private void renderFrom(@Nullable Map<String, Object> ev) {
        if (ev == null) {
            titleEvent.setText("Loading...");
            locationEvent.setText("");
            statusEvent.setText("");
            tvEntrants.setText("0 entrants");
            tvAvailable.setText("0");
            if (tvDescription != null) tvDescription.setText("—");
            if (tvRegOpens != null) tvRegOpens.setText("Registration Opens: —");
            if (tvRegCloses != null) tvRegCloses.setText("Registration Closes: —");
            if (tvEventDuration != null) tvEventDuration.setText("Event Duration: —");
            return;
        }

        String name     = s(ev.get(F_NAME));
        String location = s(ev.get(F_LOCATION));
        String status   = s(ev.get(F_STATUS));
        int waiting     = i(ev.get(F_WAITING));
        int available   = i(ev.get(F_AVAILABLE));
        String desc     = s(ev.get(F_DESCRIPTION));
        Date open       = toDate(ev.get(F_REG_OPEN));
        Date close      = toDate(ev.get(F_REG_CLOSE));

        titleEvent.setText(name.isEmpty() ? "Untitled Event" : name);
        locationEvent.setText(location);
        statusEvent.setText(status.isEmpty() ? "" : status.toUpperCase());
        applyStatusTint(status);
        tvEntrants.setText(waiting + " entrants");
        tvAvailable.setText(String.valueOf(available));

        if (tvDescription != null) tvDescription.setText(desc.isEmpty() ? "—" : desc);
        if (tvRegOpens != null) tvRegOpens.setText("Registration Opens: " + (open == null ? "—" : formatDate(open)));
        if (tvRegCloses != null) tvRegCloses.setText("Registration Closes: " + (close == null ? "—" : formatDate(close)));
        if (tvEventDuration != null) tvEventDuration.setText("Event Duration: " + formatRange(open, close));
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

    // ---- date helpers ----
    private static final SimpleDateFormat DF = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
    private static String formatDate(Date d) { return d == null ? "—" : DF.format(d); }

    private static String formatRange(Date start, Date end) {
        if (start == null && end == null) return "—";
        if (start != null && end == null) return DF.format(start) + " – ?";
        if (start == null) return "? – " + DF.format(end);
        Calendar cs = Calendar.getInstance(); cs.setTime(start);
        Calendar ce = Calendar.getInstance(); ce.setTime(end);
        if (cs.get(Calendar.YEAR) == ce.get(Calendar.YEAR)) {
            SimpleDateFormat dfStart = new SimpleDateFormat("MMM d", Locale.getDefault());
            return dfStart.format(start) + " – " + DF.format(end);
        }
        return DF.format(start) + " – " + DF.format(end);
    }

    private static Date toDate(Object o) {
        if (o == null) return null;
        if (o instanceof Timestamp) return ((Timestamp) o).toDate();
        if (o instanceof Date) return (Date) o;
        if (o instanceof String) {
            try {
                long ms = java.time.Instant.parse((String) o).toEpochMilli();
                return new Date(ms);
            } catch (Exception ignored) {}
        }
        return null;
    }

    // ---- tiny utils ----
    private static String s(Object o) { return o == null ? "" : String.valueOf(o); }
    private static int i(Object o) {
        if (o instanceof Number) return ((Number) o).intValue();
        if (o instanceof String) try { return Integer.parseInt((String) o); } catch (Exception ignored) {}
        return 0;
    }
}
