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

/**
 * Centralized data access layer for the app backed by Cloud Firestore.
 * <p>
 * This class is a lightweight repository that:
 * <ul>
 *   <li>Provides a singleton {@link #get()} instance for use across the app.</li>
 *   <li>Maintains in-memory caches of {@code events} and {@code profiles} that are
 *       kept up to date via a real-time Firestore listener.</li>
 *   <li>Exposes convenience methods to read and write Firestore documents and to
 *       transform/validate common field types.</li>
 * </ul>
 *
 * <h3>Thread-safety</h3>
 * The in-memory caches are stored in {@link LinkedHashMap}s and are mutated on the
 * Firestore listener thread. If you plan to access the caches from multiple threads,
 * coordinate access at a higher layer (e.g., post to main thread) to avoid races.
 *
 * <h3>Collections</h3>
 * By convention, events are stored in the {@code "events"} collection; profiles are
 * cached locally but their persistence strategy is app-specific.
 *
 * <h3>Errors</h3>
 * Firestore errors are logged using {@link Log} and propagated to provided callbacks
 * when applicable.
 */
public class Database {
    /** Shared Firestore instance. */
    private static final FirebaseFirestore fs = FirebaseFirestore.getInstance();
    /** Eagerly-initialized singleton instance. */
    private static final Database INSTANCE = new Database();

    /**
     * Returns the singleton repository instance.
     *
     * @return the shared {@link Database} instance
     */
    public static Database get() { return INSTANCE; }

    /**
     * Fetches a single document by collection and id and forwards the result to the provided
     * success listener.
     * <p>
     * If the read fails, the error is logged and delivered to the listener's failure path
     * via standard Tasks API behavior attached internally; this method only wires
     * {@code onSuccess} explicitly.
     *
     * @param collection the collection name (e.g., {@code "events"})
     * @param id         the document id
     * @param onSuccess  callback invoked with the fetched {@link DocumentSnapshot} on success
     */
    public static void get(String collection, String id,
                           OnSuccessListener<DocumentSnapshot> onSuccess) {
        fs.collection(collection)
                .document(id)
                .get()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error getting " + collection + "/" + id + ": " + e.getMessage(), e));
    }

    /** Log tag for this class. */
    private static final String TAG = "Database";

    /** In-memory cache of events keyed by event id; insertion-ordered. */
    private final Map<String, Event> events = new LinkedHashMap<>();
    /** In-memory cache of profiles keyed by user id; insertion-ordered. */
    private final Map<String, Profile> profiles = new LinkedHashMap<>();

    /** Firestore listener registration for the events collection. */
    private ListenerRegistration eventsListener;

    /**
     * Simple callback interface for fire-and-forget operations that either succeed
     * or fail with an exception.
     */
    public interface Callback {
        /** Invoked when the operation succeeds. */
        void onSuccess();
        /**
         * Invoked when the operation fails.
         *
         * @param e the failure reason
         */
        void onError(Exception e);
    }

    /**
     * Private constructor; sets up real-time synchronization for the events cache.
     * <p>
     * The constructor starts the events snapshot listener immediately so the cache
     * becomes warm as soon as the singleton is first accessed.
     */
    private Database() {
        // --- Seed demo data ---
        // (Disabled by default; kept for developer reference)
        // addEvent(new Event("e1", "..."));
        startEventsListener();
    }

    /**
     * Creates or updates an event document in Firestore and updates the local cache.
     * <p>
     * This method accepts textual inputs from UI, performs basic parsing/validation,
     * writes a normalized row into {@code /events/{id}}, and then updates the in-memory
     * event cache to reflect the change immediately.
     *
     * @param id                       required event id (document id)
     * @param name                     required event name
     * @param description              optional plain-text description
     * @param eventDate                optional epoch milliseconds (date portion) as string
     * @param eventTimeofDayMillis     optional epoch milliseconds (time-of-day offset) as string
     * @param registrationOpen         optional epoch milliseconds (open) as string
     * @param registrationClose        optional epoch milliseconds (close) as string
     * @param Capacity                 optional capacity as string (integer)
     * @param geolocationRequired      optional boolean-like string: "true", "1", "yes" treated as true
     * @param posterUri                optional poster image URI string
     * @param qrUrl                    optional QR code URL
     * @param cb                       optional callback invoked on success/failure
     */
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

    /**
     * Finds one document in a collection where {@code field == value} and returns it via
     * the provided success listener. If no document matches, {@code null} is passed.
     * <p>
     * Any failure to read is logged, and {@code null} is returned to the listener to keep
     * the callback contract simple.
     *
     * @param collection the collection to query
     * @param field      the field to apply an equality filter on
     * @param value      the expected value for {@code field}
     * @param onSuccess  called with the first matching {@link DocumentSnapshot} or {@code null}
     */
    public static void findOne(String collection, String field, Object value,
                               com.google.android.gms.tasks.OnSuccessListener<DocumentSnapshot> onSuccess) {
        fs.collection(collection)
                .whereEqualTo(field, value)
                .limit(1)
                .get()
                .addOnSuccessListener(qs -> {
                    DocumentSnapshot doc = qs.isEmpty() ? null : qs.getDocuments().get(0);
                    onSuccess.onSuccess(doc);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("Database", "findOne failed: " + collection + " " + field + "=" + value, e);
                    onSuccess.onSuccess(null); // keep the callback contract simple
                });
    }

    /**
     * Saves (merges) the provided POJO/map into {@code /collection/id}.
     * <p>
     * Uses {@link SetOptions#merge()} so existing fields not present in {@code data}
     * are preserved.
     *
     * @param collection the collection name
     * @param id         the document id
     * @param data       the data to upsert (POJO or map)
     */
    public static void save(String collection, String id, Object data) {
        fs.collection(collection)
                .document(id)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Document saved in " + collection + "/" + id))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error saving document: " + e.getMessage(), e));
    }

    /**
     * Starts a real-time snapshot listener on the {@code events} collection (if not already
     * started) and keeps the in-memory {@link #events} cache synchronized in reverse-chronological
     * order by {@code createdAt}.
     */
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

    /**
     * Safely reads a numeric field from a {@link DocumentSnapshot} and converts it to
     * {@link Long}, returning {@code null} on missing or incompatible types.
     *
     * @param doc the document snapshot
     * @param key the field key
     * @return the field value as {@link Long}, or {@code null} when absent/invalid
     */
    private Long getLong(DocumentSnapshot doc, String key) {
        try { Number n = (Number) doc.get(key); return n == null ? null : n.longValue(); }
        catch (Exception ignored) { return null; }
    }

    /**
     * Parses a {@link String} into a {@link Long} or returns {@code null} on empty/invalid input.
     *
     * @param s string containing a base-10 long value
     * @return parsed long or {@code null} if empty/invalid
     */
    private static Long tryParseLong(String s) {
        try { return (s == null || s.trim().isEmpty()) ? null : Long.parseLong(s.trim()); }
        catch (Exception ignore) { return null; }
    }

    /**
     * Parses a {@link String} into an {@link Integer} or returns {@code null} on empty/invalid input.
     *
     * @param s string containing a base-10 integer value
     * @return parsed integer or {@code null} if empty/invalid
     */
    private static Integer tryParseInt(String s) {
        try { return (s == null || s.trim().isEmpty()) ? null : Integer.parseInt(s.trim()); }
        catch (Exception ignore) { return null; }
    }

    /**
     * Parses a loose boolean string. Treats {@code "true"}, {@code "1"}, and {@code "yes"}
     * (case-insensitive) as {@code true}. All other inputs are {@code false}.
     *
     * @param s the input string
     * @return the parsed boolean
     */
    private static boolean parseBool(String s) {
        if (s == null) return false;
        String t = s.trim();
        return t.equalsIgnoreCase("true") || t.equals("1") || t.equalsIgnoreCase("yes");
    }

    /**
     * Returns {@code null} when the input is {@code null} or blank; otherwise returns the
     * trimmed string.
     *
     * @param s input string
     * @return trimmed string or {@code null} if empty
     */
    private static String emptyToNull(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }

    // ---------------------------
    // Events (cache) API
    // ---------------------------

    /**
     * Returns a snapshot copy of all cached events in insertion order (newest first
     * given the active listener ordering).
     *
     * @return list of events from the in-memory cache
     */
    public List<Event> getAllEvents() { return new ArrayList<>(events.values()); }

    /**
     * Inserts or replaces an event in the local cache. This does not write to Firestore.
     *
     * @param e the event to cache
     */
    public void addEvent(Event e) { events.put(e.getId(), e); }

    /**
     * Removes an event from the local cache by id. This does not delete from Firestore.
     *
     * @param id event id to remove
     */
    public void removeEvent(String id) { events.remove(id); }

    // ---------------------------
    // Profiles (cache) API
    // ---------------------------

    /**
     * Returns a snapshot copy of all cached profiles in insertion order.
     *
     * @return list of profiles from the in-memory cache
     */
    public List<Profile> getAllProfiles() { return new ArrayList<>(profiles.values()); }

    /**
     * Inserts or replaces a profile in the local cache. This does not write to Firestore.
     *
     * @param p the profile to cache
     */
    public void addProfile(Profile p) { profiles.put(p.getId(), p); }

    /**
     * Removes a profile from the local cache by id. This does not delete from Firestore.
     *
     * @param id profile/user id to remove
     */
    public void removeProfile(String id) { profiles.remove(id); }

    // ---------------------------
    // Simple stats
    // ---------------------------

    /**
     * Returns the number of events currently cached.
     *
     * @return count of cached events
     */
    public int totalEvents() { return events.size(); }

    /**
     * Returns the number of profiles currently cached.
     *
     * @return count of cached profiles
     */
    public int totalUsers() { return profiles.size(); }
}
