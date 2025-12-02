package com.example.gainly_flow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;

public class EventUnitTest {

    private Event event;

    @BeforeEach
    public void setUp() {
        event = new Event("test_event_id");
        event.setCapacity(10);
    }

    @Test
    public void testAddToWaitingList() {
        String entrantId = "entrant_1";

        // Test adding new entrant
        assertTrue(event.addToWaitingList(entrantId));
        assertEquals(1, event.getWaitingListSize());
        assertTrue(event.isOnWaitingList(entrantId));

        // Test adding duplicate entrant
        assertFalse(event.addToWaitingList(entrantId));
        assertEquals(1, event.getWaitingListSize());
    }

    @Test
    public void testSelectEntrant() {
        String entrantId = "entrant_1";
        event.addToWaitingList(entrantId);

        // Test selecting entrant
        assertTrue(event.selectEntrant(entrantId));
        assertFalse(event.isOnWaitingList(entrantId));
        assertTrue(event.isSelected(entrantId));
        assertEquals(0, event.getWaitingListSize());
        assertEquals(1, event.getSelectedCount());
    }

    @Test
    public void testEnrollEntrant() {
        String entrantId = "entrant_1";
        event.addToWaitingList(entrantId);
        event.selectEntrant(entrantId);

        // Test enrolling entrant
        assertTrue(event.enrollEntrant(entrantId));
        assertFalse(event.isSelected(entrantId));
        assertTrue(event.isEnrolled(entrantId));
        assertEquals(1, event.getEnrolledCount());
        assertEquals(1, event.getCurrentParticipants());
    }

    @Test
    public void testIsFull() {
        event.setCapacity(2);

        // Initially not full
        assertFalse(event.isFull());

        // Add one participant
        event.setCurrentParticipants(1);
        assertFalse(event.isFull());

        // Add another participant (reached capacity)
        event.setCurrentParticipants(2);
        assertTrue(event.isFull());
    }
}
