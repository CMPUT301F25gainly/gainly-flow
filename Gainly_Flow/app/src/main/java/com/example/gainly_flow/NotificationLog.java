package com.example.gainly_flow;

import java.util.Date;

public class NotificationLog {
    public static class Entry {
        public String id;
        public String fromUserId;
        public String toUserId;
        public String channel; // e.g., push
        public String payload;
        public Date timestamp;
        public String status;  // SENT/FAILED/DELIVERED
    }

    public void record(Entry entry) {}
    public void listAll() {}
}
