package com.example.gainly_flow;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Database helper combining:
 * 1) Firestore static CRUD utilities (save/get/update/delete/createData)
 * 2) A lightweight in-memory singleton cache with seed data (events/profiles)
 *
 * NOTE: The in-memory maps are just a demo/cache layer and are NOT auto-synced
 * with Firestore. Use Firestore methods for real persistence.
 */
public class Database {

    // ----------------------------
    // Firestore static utilities
    // ----------------------------
    private static final String TAG = "Database";
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Save data to Firestore under a specific collection and document ID.
     *
     * @param collection Firestore collection name (e.g., "events")
     * @param id         Document ID (e.g., eventId)
     * @param data       Data map or POJO to store
     */
    public static void save(String collection, String id, Object data) {
        db.collection(collection)
                .document(id)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Document saved in " + collection + "/" + id))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error saving document: " + e.getMessage(), e));
    }

    /**
     * Fetch a single document from Firestore asynchronously.
     *
     * @param collection Firestore collection name
     * @param id         Document ID
     * @param listener   Callback that receives the DocumentSnapshot
     */
    public static void get(String collection, String id, OnSuccessListener<DocumentSnapshot> listener) {
        db.collection(collection)
                .document(id)
                .get()
                .addOnSuccessListener(listener)
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error fetching " + collection + "/" + id + ": " + e.getMessage(), e));
    }

    /**
     * Update specific fields in a document.
     */
    public static void update(String collection, String id, Map<String, Object> updates) {
        db.collection(collection)
                .document(id)
                .update(updates)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Updated fields in " + collection + "/" + id))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error updating " + collection + "/" + id + ": " + e.getMessage(), e));
    }

    /**
     * Delete a document from Firestore.
     */
    public static void delete(String collection, String id) {
        db.collection(collection)
                .document(id)
                .delete()
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Deleted document " + collection + "/" + id))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error deleting document: " + e.getMessage(), e));
    }

    /**
     * Helper to create simple map data if not using a model POJO.
     */
    public static Map<String, Object> createData(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    // ---------------------------------
    // In-memory singleton (demo/cache)
    // ---------------------------------
    private static final Database INSTANCE = new Database();
    public static Database get() { return INSTANCE; }

    private final Map<String, Event> events = new LinkedHashMap<>();
    private final Map<String, Profile> profiles = new LinkedHashMap<>();

    private Database() {
        // ---- Seed demo data (local, non-persistent) ----
        // Safe to keep for offline/demo/admin-browse screens.
        try {
            addEvent(new Event("e1", "Swimming Lessons for Beginners", "Jan 15–Mar 15, 2025", "Pool A"));
            addEvent(new Event("e2", "Interpretive Dance Class", "Jan 1–Mar 1, 2025", "Studio B"));
            addEvent(new Event("e3", "Piano for Beginners", "Feb 1–Apr 1, 2025", "Room 203"));
        } catch (Throwable t) {
            Log.w(TAG, "Event seeding skipped: " + t.getMessage());
        }

        try {
            addProfile(new Profile("u1", "Alex Johnson", "alex@demo.com"));
            addProfile(new Profile("u2", "Sam Rivera", "sam@demo.com"));
            addProfile(new Profile("u3", "Taylor Kim", "taylor@demo.com"));
        } catch (Throwable t) {
            Log.w(TAG, "Profile seeding skipped: " + t.getMessage());
        }
    }

    // ---- Events (in-memory) ----
    public List<Event> getAllEvents() { return new ArrayList<>(events.values()); }
    public void addEvent(Event e) { if (e != null && e.getId() != null) events.put(e.getId(), e); }
    public void removeEvent(String id) { if (id != null) events.remove(id); }

    // ---- Profiles (in-memory) ----
    public List<Profile> getAllProfiles() { return new ArrayList<>(profiles.values()); }
    public void addProfile(Profile p) { if (p != null && p.getId() != null) profiles.put(p.getId(), p); }
    public void removeProfile(String id) { if (id != null) profiles.remove(id); }

    // ---- Simple stats (in-memory) ----
    public int totalEvents() { return events.size(); }
    public int totalUsers() { return profiles.size(); }
}
