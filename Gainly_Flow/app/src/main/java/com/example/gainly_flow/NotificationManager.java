package com.example.gainly_flow;

import java.util.List;

public class NotificationManager {
    private final NotificationLog log = new NotificationLog();

    public void notifySelected(List<String> entrantIds, String eventId) {}
    public void notifyNotSelected(List<String> entrantIds, String eventId) {}
    public void notifyCustom(List<String> userIds, String message) {}
    public NotificationLog getLog() { return log; }
}
