package com.example.gainly_flow;

import java.util.Map;

import java.util.*;

public class Database {
    private static final Database INSTANCE = new Database();
    public static Database get() { return INSTANCE; }

    private final Map<String, Event> events = new LinkedHashMap<>();
    private final Map<String, Profile> profiles = new LinkedHashMap<>();

    private Database() {
        // --- Seed demo data ---
        addEvent(new Event("e1", "Swimming Lessons for Beginners", "Jan 15–Mar 15, 2025", "Pool A"));
        addEvent(new Event("e2", "Interpretive Dance Class", "Jan 1–Mar 1, 2025", "Studio B"));
        addEvent(new Event("e3", "Piano for Beginners", "Feb 1–Apr 1, 2025", "Room 203"));

        addProfile(new Profile("u1", "Alex Johnson", "alex@demo.com"));
        addProfile(new Profile("u2", "Sam Rivera", "sam@demo.com"));
        addProfile(new Profile("u3", "Taylor Kim", "taylor@demo.com"));
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

