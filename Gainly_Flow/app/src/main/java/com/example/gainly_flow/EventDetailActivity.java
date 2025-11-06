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

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class EventDetailActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "eventId";

    // Firestore
    private FirebaseFirestore fs;
    private ListenerRegistration reg;

    // Collection
    private static final String COL_EVENTS = "events";

    // Field keys (match your teammate’s Database.addEventDatabase)
    private static final String F_ID                 = "id";
    private static final String F_NAME               = "name";
    private static final String F_DESCRIPTION        = "description";
    private static final String F_EVENT_DATE         = "eventDate";          // UTC midnight (ms)
    private static final String F_EVENT_TIME_MS      = "eventTimeOfDayMs";   // ms from midnight
    private static final String F_REG_OPEN           = "registrationOpen";   // ms
    private static final String F_REG_CLOSE          = "registrationClose";  // ms
    private static final String F_CAPACITY           = "capacity";
    private static final String F_POSTER_URI         = "posterUri";          // optional
    // Note: no location / waiting / available in this schema

    // UI
    private TextView titleEvent, statusEvent, locationEvent, tvEntrants, tvAvailable;
    private TextView tvEventDuration, tvRegOpens, tvRegCloses, tvDescription;
    private Button btnJoin, btnLeave;
    private ImageButton btnBack;
    private ImageView qrIcon;

    private String eventId;

    // Formats
    private static final SimpleDateFormat DATE_DF = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
    private static final SimpleDateFormat TIME_DF = new SimpleDateFormat("h:mm a", Locale.getDefault());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        fs = FirebaseFirestore.getInstance();

        // Bind views (ids match your XML)
        titleEvent      = findViewById(R.id.title_event);
        statusEvent     = findViewById(R.id.event_status);
        locationEvent   = findViewById(R.id.location_event);  // we’ll leave empty (no location in schema)
        tvEntrants      = findViewById(R.id.tvEntrants);      // placeholder only
        tvAvailable     = findViewById(R.id.tvAvailable);     // will show capacity
        tvEventDuration = findViewById(R.id.tvEventDuration);
        tvRegOpens      = findViewById(R.id.tvRegOpens);
        tvRegCloses     = findViewById(R.id.tvRegCloses);
        tvDescription   = findViewById(R.id.tvDescription);
        btnJoin         = findViewById(R.id.btnJoin);
        btnLeave        = findViewById(R.id.btnLeave);
        btnBack         = findViewById(R.id.btnBack);
        qrIcon          = findViewById(R.id.qr_icon);

        // Get eventId
        Intent src = getIntent();
        eventId = src.getStringExtra(EXTRA_EVENT_ID);
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Missing eventId", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Back/QR
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        if (qrIcon != null)  qrIcon.setOnClickListener(v -> Toast.makeText(this, "QR scanner not wired yet", Toast.LENGTH_SHORT).show());

        // Safe placeholders (so nothing crashes before data arrives)
        applyPlaceholders();

        // Live doc updates
        DocumentReference ref = fs.collection(COL_EVENTS).document(eventId);
        reg = ref.addSnapshotListener((snap, err) -> {
            if (err != null || snap == null || !snap.exists()) return;
            render(snap);
        });

        // Buttons (no-ops for now, so you can test the screen safely)
        btnJoin.setOnClickListener(v -> Toast.makeText(this, "Join: not wired yet", Toast.LENGTH_SHORT).show());
        btnLeave.setOnClickListener(v -> Toast.makeText(this, "Leave: not wired yet", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (reg != null) { reg.remove(); reg = null; }
    }

    /* -------------------- Render -------------------- */

    private void render(DocumentSnapshot d) {
        String name   = getStr(d.get(F_NAME));
        String desc   = getStr(d.get(F_DESCRIPTION));
        Long eventDay = getLong(d.get(F_EVENT_DATE));      // UTC midnight millis
        Long eventTMs = getLong(d.get(F_EVENT_TIME_MS));   // ms from midnight
        Long regOpen  = getLong(d.get(F_REG_OPEN));
        Long regClose = getLong(d.get(F_REG_CLOSE));
        Integer cap   = getInt(d.get(F_CAPACITY));

        titleEvent.setText(name.isEmpty() ? "Untitled Event" : name);
        locationEvent.setText(""); // no location field in schema (leave blank or remove this view)

        // Status: Open if now within [regOpen, regClose]
        boolean openNow = isNowWithin(regOpen, regClose);
        statusEvent.setText(openNow ? "OPEN" : "CLOSED");
        applyStatusTint(openNow ? "Open" : "Closed");

        // Counts area (your XML expects these)
        tvEntrants.setText("— entrants"); // not tracked in schema
        tvAvailable.setText(cap == null ? "—" : String.valueOf(Math.max(0, cap)));

        // Event date/time line
        String eventWhen = formatEventDateTime(eventDay, eventTMs);
        tvEventDuration.setText("Event Date: " + (eventWhen.isEmpty() ? "—" : eventWhen));

        // Registration window
        tvRegOpens.setText("Registration Opens: " + (regOpen == null ? "—" : DATE_DF.format(new Date(regOpen))));
        tvRegCloses.setText("Registration Closes: " + (regClose == null ? "—" : DATE_DF.format(new Date(regClose))));

        // Description
        tvDescription.setText(desc.isEmpty() ? "—" : desc);
    }

    private void applyPlaceholders() {
        titleEvent.setText("Loading...");
        statusEvent.setText("");
        locationEvent.setText("");
        tvEntrants.setText("— entrants");
        tvAvailable.setText("—");
        tvEventDuration.setText("Event Date: —");
        tvRegOpens.setText("Registration Opens: —");
        tvRegCloses.setText("Registration Closes: —");
        tvDescription.setText("—");
    }

    private void applyStatusTint(String status) {
        @ColorInt int color;
        if ("Open".equalsIgnoreCase(status)) {
            color = ContextCompat.getColor(this, android.R.color.holo_green_dark);
        } else if ("Closed".equalsIgnoreCase(status)) {
            color = ContextCompat.getColor(this, android.R.color.darker_gray);
        } else {
            color = ContextCompat.getColor(this, android.R.color.black);
        }
        statusEvent.setTextColor(color);
    }

    /* -------------------- Helpers -------------------- */

    private static boolean isNowWithin(Long openMs, Long closeMs) {
        long now = System.currentTimeMillis();
        if (openMs == null && closeMs == null) return true;
        if (openMs != null && now < openMs) return false;
        if (closeMs != null && now > closeMs) return false;
        return true;
    }

    private String formatEventDateTime(@Nullable Long dayUtc, @Nullable Long timeMsFromMidnight) {
        if (dayUtc == null && timeMsFromMidnight == null) return "";
        Date day = (dayUtc == null) ? null : new Date(dayUtc);
        String dayPart = (day == null) ? "" : DATE_DF.format(day);

        String timePart = "";
        if (timeMsFromMidnight != null) {
            // Build a local Date for the time-of-day (on epoch day), then just format the time
            Calendar c = Calendar.getInstance();
            c.clear();
            c.set(Calendar.YEAR, 1970);
            c.set(Calendar.MONTH, Calendar.JANUARY);
            c.set(Calendar.DAY_OF_MONTH, 1);
            c.set(Calendar.MILLISECOND, 0);
            c.setTimeInMillis(c.getTimeInMillis() + timeMsFromMidnight);
            timePart = TIME_DF.format(c.getTime());
        }

        if (!dayPart.isEmpty() && !timePart.isEmpty()) return dayPart + " • " + timePart;
        if (!dayPart.isEmpty()) return dayPart;
        return timePart;
    }

    private static String getStr(Object o) { return o == null ? "" : String.valueOf(o); }
    private static Integer getInt(Object o) {
        if (o instanceof Number) return ((Number) o).intValue();
        if (o instanceof String) try { return Integer.parseInt((String) o); } catch (Exception ignored) {}
        return null;
    }
    private static Long getLong(Object o) {
        if (o instanceof Number) return ((Number) o).longValue();
        if (o instanceof String) try { return Long.parseLong((String) o); } catch (Exception ignored) {}
        return null;
    }
}
