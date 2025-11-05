package com.example.gainly_flow;

import java.util.ArrayList;
import java.util.List;

public class WaitingList {
    private final String eventId;
    public List<String> entrantList = new ArrayList<>();

    public WaitingList(String eventId) { this.eventId = eventId; }

    public void addEntrant(String entrantId) {
        if (entrantList.contains(entrantId)) {
            throw new IllegalArgumentException();
        }
        entrantList.add(entrantId);
    }

    public void removeEntrant(String entrantId) {
        if(entrantList.contains(eventId)){
            entrantList.remove(entrantId);
        } else{
            throw new IllegalArgumentException();
        }
    }
    public int getCount() { return entrantList.size(); }
    public List<String> getEntrants() { return entrantList; }
}
