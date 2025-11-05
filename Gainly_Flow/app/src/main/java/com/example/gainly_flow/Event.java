package com.example.gainly_flow;

import java.util.Date;

public class Event {
    private final String id;
    private final String name;
    private final String description; // or date range
    private final String location;

    public Event(String id, String name, String description, String location) {
        this.id = id; this.name = name; this.description = description; this.location = location;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }

    @Override public String toString() {
        String extra = (description == null ? "" : " • " + description);
        String loc = (location == null ? "" : " • " + location);
        return name + extra + loc;
    }
}

