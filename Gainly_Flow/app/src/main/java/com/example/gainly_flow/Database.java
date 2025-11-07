package com.example.gainly_flow;

import static java.lang.Long.getLong;

import android.util.Log;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.*;

import java.util.Map;

import java.util.*;

import javax.security.auth.callback.Callback;

public class Database {
    private static final Database INSTANCE = new Database();
    public static Database get() { return INSTANCE; }
    public static void get(String collection, String id,
                           OnSuccessListener<DocumentSnapshot> onSuccess) {
        fs.collection(collection)
                .document(id)
                .get()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error getting " + collection + "/" + id + ": " + e.getMessage(), e));
    }

    private static final String TAG = "Database";
    private static final FirebaseFirestore fs = FirebaseFirestore.getInstance();
    private final Map<String, Event> events = new LinkedHashMap<>();
    private final Map<String, Profile> profiles = new LinkedHashMap<>();

    private ListenerRegistration eventsListener;
    public interface Callback {
        void onSuccess();
        void onError(Exception e);
    }

    private Database() {
        // --- Seed demo data ---
//        addEvent(new Event("e1", "Swimming Lessons for Beginners", "Jan 15–Mar 15, 2025", "Pool A"));
//        addEvent(new Event("e2", "Interpretive Dance Class", "Jan 1–Mar 1, 2025", "Studio B"));
//        addEvent(new Event("e3", "Piano for Beginners", "Feb 1–Apr 1, 2025", "Room 203"));

       // addProfile(new Profile("u1", "Alex Johnson", "alex@demo.com"));
        //addProfile(new Profile("u2", "Sam Rivera", "sam@demo.com"));
        //addProfile(new Profile("u3", "Taylor Kim", "taylor@demo.com"));
        startEventsListener();
    }
    public void addEventDatabase(
            String id, String name, String description, String eventDate,
            String eventTimeofDayMillis, String registrationOpen,
            String registrationClose, String Capacity,
            String geolocationRequired, String posterUri, String qrUrl,
            @Nullable Callback cb) {

        if (id == null || id.trim().isEmpty()) { if (cb != null) cb.onError(new IllegalArgumentException("id required")); return; }
        if (name == null || name.trim().isEmpty()) { if (cb != null) cb.onError(new IllegalArgumentException("name required")); return; }

        Long eventDateMs      = tryParseLong(eventDate);
        Long eventTimeMs      = tryParseLong(eventTimeofDayMillis);
        Long regOpenMs        = tryParseLong(registrationOpen);
        Long regCloseMs       = tryParseLong(registrationClose);
        Integer capacity      = tryParseInt(Capacity);
        boolean geoReq        = parseBool(geolocationRequired);

        long eventDateTimeUtc = (eventDateMs == null ? 0L : eventDateMs) + (eventTimeMs == null ? 0L : eventTimeMs);

        // Build Firestore row
        Map<String, Object> row = new HashMap<>();
        row.put("id", id);
        row.put("name", name);
        row.put("description", emptyToNull(description));
        row.put("eventDate", eventDateMs);
        row.put("eventTimeOfDayMs", eventTimeMs);
        row.put("eventDateTimeUtc", eventDateTimeUtc);
        row.put("registrationOpen", regOpenMs);
        row.put("registrationClose", regCloseMs);
        row.put("capacity", capacity);
        row.put("geolocationRequired", geoReq);
        row.put("posterUri", emptyToNull(posterUri));
        if (qrUrl != null && !qrUrl.isEmpty()) row.put("qrUrl", qrUrl);
        row.put("createdAt", FieldValue.serverTimestamp());

        fs.collection("events")
                .document(id)
                .set(row)
                .addOnSuccessListener(aVoid -> {
                    // also update local cache immediately
                    Event e = new Event(id);
                    e.setName(name);
                    e.setDescription(emptyToNull(description));
                    if (capacity != null) e.setCapacity(capacity);
                    e.setGeolocationRequired(geoReq);
                    if (posterUri != null && !posterUri.trim().isEmpty()) e.setPosterImage(posterUri.trim());
                    if (regOpenMs != null || regCloseMs != null) {
                        e.setRegistrationPeriod(regOpenMs == null ? null : new Date(regOpenMs),
                                regCloseMs == null ? null : new Date(regCloseMs));
                    }
                    addEvent(e);
                    if (cb != null) cb.onSuccess();
                })
                .addOnFailureListener(err -> { if (cb != null) cb.onError(err); });
    }
    public static void save(String collection, String id, Object data) {
        fs.collection(collection)
                .document(id)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Document saved in " + collection + "/" + id))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error saving document: " + e.getMessage(), e));
    }
    private void startEventsListener() {
        if (eventsListener != null) return;
        eventsListener = fs.collection("events")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, err) -> {
                    if (err != null || snap == null) return;
                    events.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String id = doc.getString("id");
                        if (id == null) continue;

                        Event e = new Event(id);
                        e.setName(doc.getString("name"));
                        e.setDescription(doc.getString("description"));

                        Long cap = getLong(doc, "capacity");
                        if (cap != null) e.setCapacity(cap.intValue());

                        e.setGeolocationRequired(Boolean.TRUE.equals(doc.getBoolean("geolocationRequired")));

                        String poster = doc.getString("posterUri");
                        if (poster != null) e.setPosterImage(poster);

                        Long ro = getLong(doc, "registrationOpen");
                        Long rc = getLong(doc, "registrationClose");
                        e.setRegistrationPeriod(ro == null ? null : new Date(ro),
                                rc == null ? null : new Date(rc));



                        String qrUrl = doc.getString("qrUrl");
                        if (qrUrl != null && !qrUrl.isEmpty()) {
                            e.setQrUrl(qrUrl);
                        }
                        // ------------------------

                        events.put(id, e);
                    }
                });
    }
    private Long getLong(DocumentSnapshot doc, String key) {
        try { Number n = (Number) doc.get(key); return n == null ? null : n.longValue(); }
        catch (Exception ignored) { return null; }
    }

    private static Long tryParseLong(String s) {
        try { return (s == null || s.trim().isEmpty()) ? null : Long.parseLong(s.trim()); }
        catch (Exception ignore) { return null; }
    }
    private static Integer tryParseInt(String s) {
        try { return (s == null || s.trim().isEmpty()) ? null : Integer.parseInt(s.trim()); }
        catch (Exception ignore) { return null; }
    }

    private static boolean parseBool(String s) {
        if (s == null) return false;
        String t = s.trim();
        return t.equalsIgnoreCase("true") || t.equals("1") || t.equalsIgnoreCase("yes");
    }

    private static String emptyToNull(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }
    // Events
    public List<Event> getAllEvents() { return new ArrayList<>(events.values()); }
    public void addEvent(Event e) { events.put(e.getId(), e); }
    public void removeEvent(String id) { events.remove(id); }

    // Profiles
    public List<Profile> getAllProfiles() { return new ArrayList<>(profiles.values()); }
    public void addProfile(Profile p) { profiles.put(p.getId(), p); }
    public void removeProfile(String id) { profiles.remove(id); }

    // Simple stats
    public int totalEvents() { return events.size(); }
    public int totalUsers() { return profiles.size(); }
}