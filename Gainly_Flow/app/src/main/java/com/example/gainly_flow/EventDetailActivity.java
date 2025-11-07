package com.example.gainly_flow;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

public class EventDetailActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "eventId";

    // Firestore
    private FirebaseFirestore fs;
    private ListenerRegistration eventReg;
    private ListenerRegistration waitReg;

    // Collections / fields
    private static final String COL_EVENTS      = "events";
    private static final String COL_WAITLISTS   = "waiting_lists";
    private static final String F_NAME          = "name";
    private static final String F_DESCRIPTION   = "description";
    private static final String F_EVENT_DATE    = "eventDate";          // UTC midnight (ms)
    private static final String F_EVENT_TIME_MS = "eventTimeOfDayMs";   // ms from midnight
    private static final String F_REG_OPEN      = "registrationOpen";   // ms
    private static final String F_REG_CLOSE     = "registrationClose";  // ms
    private static final String F_CAPACITY      = "capacity";
    private static final String F_POSTER_URL    = "posterUrl";          // HTTPS image url

    // waitlist fields
    private static final String WL_ENTRANT_IDS  = "entrantIds";
    private static final String WL_SIZE         = "size";
    private static final String WL_EVENT_ID     = "eventId";
    private static final String WL_CREATED_AT   = "createdAt";
    private static final String WL_MAX_CAP      = "maxCapacity";

    // UI
    private TextView titleEvent, statusEvent, tvEntrants, tvAvailable;
    private TextView tvEventDuration, tvRegOpens, tvRegCloses, tvDescription;
    private Button btnJoin, btnLeave;
    private ImageButton btnBack;
    private ImageView qrIcon;
    private ImageView posterImage;

    private String eventId;
    private boolean joined = false; // current user in waitlist?

    // Formats
    private static final SimpleDateFormat DATE_DF = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
    private static final SimpleDateFormat TIME_DF = new SimpleDateFormat("h:mm a", Locale.getDefault());

    // TODO: replace with FirebaseAuth uid when you wire auth
    private String getCurrentUserId() { return "22ae419f5bed11cd"; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        fs = FirebaseFirestore.getInstance();

        // Bind views (only what's actually used)
        posterImage     = findViewById(R.id.posterImage);
        titleEvent      = findViewById(R.id.title_event);
        statusEvent     = findViewById(R.id.event_status);
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

        // Get eventId
        Intent src = getIntent();
        eventId = src.getStringExtra(EXTRA_EVENT_ID);
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Missing eventId", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Basic UI hooks
        btnBack.setOnClickListener(v -> finish());
        qrIcon.setOnClickListener(v ->
                startActivity(new Intent(this, QRCodeScanner.class))
        );

        applyPlaceholders();

        // Live event doc
        DocumentReference eventRef = fs.collection(COL_EVENTS).document(eventId);
        eventReg = eventRef.addSnapshotListener((snap, err) -> {
            if (err != null || snap == null || !snap.exists()) return;
            renderEvent(snap);
        });

        // Live waitlist doc (one doc per event)
        DocumentReference wlRef = fs.collection(COL_WAITLISTS).document(eventId);
        waitReg = wlRef.addSnapshotListener((snap, err) -> {
            if (err != null) return;
            int size = 0;
            List<String> ids = null;
            if (snap != null && snap.exists()) {
                ids = (List<String>) snap.get(WL_ENTRANT_IDS);
                Long s = snap.getLong(WL_SIZE);
                size = (s != null) ? s.intValue() : (ids == null ? 0 : ids.size());
            }
            tvEntrants.setText(size + " entrants");

            String uid = getCurrentUserId();
            joined = (ids != null && uid != null) && ids.contains(uid);
            updateButtons();
        });

        // Join/Leave actions
        btnJoin.setOnClickListener(v -> joinWaitlist());
        btnLeave.setOnClickListener(v -> leaveWaitlist());
        updateButtons();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (eventReg != null) { eventReg.remove(); eventReg = null; }
        if (waitReg  != null) { waitReg.remove();  waitReg  = null; }
    }

    /* -------------------- Render -------------------- */

    private void renderEvent(DocumentSnapshot d) {
        String name      = getStr(d.get(F_NAME));
        String desc      = getStr(d.get(F_DESCRIPTION));
        Long eventDay    = getLong(d.get(F_EVENT_DATE));
        Long eventTMs    = getLong(d.get(F_EVENT_TIME_MS));
        Long regOpen     = getLong(d.get(F_REG_OPEN));
        Long regClose    = getLong(d.get(F_REG_CLOSE));
        Integer cap      = getInt(d.get(F_CAPACITY));
        String posterUrl = getStr(d.get(F_POSTER_URL));

        // Poster
        loadPoster(posterUrl);

        // Text bits
        titleEvent.setText(name.isEmpty() ? "Untitled Event" : name);

        boolean openNow = isNowWithin(regOpen, regClose);
        statusEvent.setText(openNow ? "OPEN" : "CLOSED");
        applyStatusTint(openNow ? "Open" : "Closed");

        tvAvailable.setText(cap == null ? "—" : String.valueOf(Math.max(0, cap)));

        String eventWhen = formatEventDateTime(eventDay, eventTMs);
        tvEventDuration.setText("Event Date: " + (eventWhen.isEmpty() ? "—" : eventWhen));

        tvRegOpens.setText("Registration Opens: " + (regOpen == null ? "—" : DATE_DF.format(new Date(regOpen))));
        tvRegCloses.setText("Registration Closes: " + (regClose == null ? "—" : DATE_DF.format(new Date(regClose))));

        tvDescription.setText(desc.isEmpty() ? "—" : desc);
    }

    private void applyPlaceholders() {
        // A clean “loading” state that won’t flicker
        posterImage.setImageResource(R.drawable.blue_gradient_bg);
        titleEvent.setText("Loading...");
        statusEvent.setText("");
        tvEntrants.setText("— entrants");
        tvAvailable.setText("—");
        tvEventDuration.setText("Event Date: —");
        tvRegOpens.setText("Registration Opens: —");
        tvRegCloses.setText("Registration Closes: —");
        tvDescription.setText("—");
    }

    private void updateButtons() {
        btnJoin.setEnabled(!joined);
        btnLeave.setEnabled(joined);
    }

    /* -------------------- Join / Leave logic -------------------- */

    private void joinWaitlist() {
        final String uid = getCurrentUserId();
        if (uid == null || uid.isEmpty()) {
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show();
            return;
        }

        final DocumentReference wlRef = fs.collection(COL_WAITLISTS).document(eventId);

        fs.runTransaction(tx -> {
            DocumentSnapshot snap = tx.get(wlRef);

            // Ensure doc exists and has basic fields
            Map<String, Object> base = new HashMap<>();
            base.put(WL_EVENT_ID, eventId);
            base.put(WL_CREATED_AT, FieldValue.serverTimestamp());
            base.put(WL_MAX_CAP, -1);
            tx.set(wlRef, base, SetOptions.merge());

            List<String> ids = (List<String>) (snap.exists() ? snap.get(WL_ENTRANT_IDS) : null);
            boolean alreadyIn = ids != null && ids.contains(uid);
            if (alreadyIn) return null; // idempotent

            // arrayUnion + increment
            Map<String, Object> updates = new HashMap<>();
            updates.put(WL_ENTRANT_IDS, FieldValue.arrayUnion(uid));
            updates.put(WL_SIZE, FieldValue.increment(1));
            tx.set(wlRef, updates, SetOptions.merge());
            return null;
        }).addOnSuccessListener(v ->
                Toast.makeText(this, "Joined waiting list", Toast.LENGTH_SHORT).show()
        ).addOnFailureListener(e ->
                Toast.makeText(this, "Join failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
        );
    }

    private void leaveWaitlist() {
        final String uid = getCurrentUserId();
        if (uid == null || uid.isEmpty()) {
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show();
            return;
        }

        final DocumentReference wlRef = fs.collection(COL_WAITLISTS).document(eventId);

        fs.runTransaction(tx -> {
            DocumentSnapshot snap = tx.get(wlRef);
            if (!snap.exists()) return null;

            List<String> ids = (List<String>) snap.get(WL_ENTRANT_IDS);
            if (ids == null || !ids.contains(uid)) return null; // idempotent

            int current = ids.size();
            Map<String, Object> updates = new HashMap<>();
            updates.put(WL_ENTRANT_IDS, FieldValue.arrayRemove(uid));
            updates.put(WL_SIZE, Math.max(0, current - 1)); // keep non-negative
            tx.set(wlRef, updates, SetOptions.merge());
            return null;
        }).addOnSuccessListener(v ->
                Toast.makeText(this, "Left waiting list", Toast.LENGTH_SHORT).show()
        ).addOnFailureListener(e ->
                Toast.makeText(this, "Leave failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
        );
    }

    /* -------------------- Helpers -------------------- */

    private void loadPoster(String url) {
        int fallback = R.drawable.blue_gradient_bg;

        if (url == null || url.trim().isEmpty()) {
            posterImage.setImageResource(fallback);
            return;
        }

        Glide.with(this)
                .load(url)
                .placeholder(fallback)
                .error(fallback)
                .centerCrop()
                .into(posterImage);
    }

    private void applyStatusTint(String status) {
        int color;
        if ("Open".equalsIgnoreCase(status)) {
            color = ContextCompat.getColor(this, android.R.color.holo_green_dark);
        } else if ("Closed".equalsIgnoreCase(status)) {
            color = ContextCompat.getColor(this, android.R.color.darker_gray);
        } else {
            color = ContextCompat.getColor(this, android.R.color.black);
        }
        statusEvent.setTextColor(color);
    }

    private static boolean isNowWithin(Long openMs, Long closeMs) {
        long now = System.currentTimeMillis();
        if (openMs == null && closeMs == null) return true;
        if (openMs != null && now < openMs) return false;
        if (closeMs != null && now > closeMs) return false;
        return true;
    }

    private String formatEventDateTime(Long dayUtc, Long timeMsFromMidnight) {
        if (dayUtc == null && timeMsFromMidnight == null) return "";
        Date day = (dayUtc == null) ? null : new Date(dayUtc);
        String dayPart = (day == null) ? "" : DATE_DF.format(day);

        String timePart = "";
        if (timeMsFromMidnight != null) {
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
