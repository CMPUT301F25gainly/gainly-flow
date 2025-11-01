package com.example.gainly_flow;

import java.util.List;

public class WaitingList {
    private final String eventId;

    public WaitingList(String eventId) { this.eventId = eventId; }

    public void addEntrant(String entrantId) {}
    public void removeEntrant(String entrantId) {}
    public int getCount() { return 0; }
    public List<String> getEntrants() { return null; }
}
