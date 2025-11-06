package com.example.gainly_flow;

import java.util.List;

public class NotificationManager {
    private final NotificationLog log = new NotificationLog();

    public void notifySelected(List<String> entrantIds, String eventId) {
        //there should be a value in entrant displaying where they're joining from, but
        for(String entrandId : entrantIds){

        }
    }
    public void notifyNotSelected(List<String> entrantIds, String eventId) {}
    public void notifyCustom(List<String> userIds, String message) {}
    public NotificationLog getLog() { return log; }
}
