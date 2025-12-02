package com.example.gainly_flow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;

public class EntrantUnitTest {

    private Entrant entrant;

    @BeforeEach
    public void setUp() {
        entrant = new Entrant("test_entrant_id");
    }

    @Test
    public void testAddEventHistory() {
        String eventId = "event_1";

        // Test adding new event to history
        entrant.addEventHistory(eventId);
        assertTrue(entrant.hasEventInHistory(eventId));
        assertEquals(1, entrant.getEventHistory().size());

        // Test adding duplicate event
        entrant.addEventHistory(eventId);
        assertEquals(1, entrant.getEventHistory().size());
    }

    @Test
    public void testGetUnreadNotificationCount() {
        // Initially 0
        assertEquals(0, entrant.getUnreadNotificationCount());

        // Add unread notification
        NotificationItem unreadNotif = new NotificationItem("Title", "Message", "INFO", "test_recipient");
        // By default isRead is false
        entrant.addNotification(unreadNotif);
        assertEquals(1, entrant.getUnreadNotificationCount());

        // Add read notification
        NotificationItem readNotif = new NotificationItem("Title 2", "Message 2", "INFO", "test_recipient");
        readNotif.setRead(true);
        entrant.addNotification(readNotif);

        // Count should still be 1
        assertEquals(1, entrant.getUnreadNotificationCount());
    }
}
