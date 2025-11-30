package com.example.gainly_flow;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrganizerTest {

    @Test
    void defaults_areInitializedAndRoleIsOrganizer() {
        Organizer organizer = new Organizer("org-1");
        assertEquals("Organizer", organizer.getRole());
        assertNotNull(organizer.getCreatedEvents());
        assertNotNull(organizer.getActiveEvents());
    }

    @Test
    void createdEvents_addsOnce_andRemoveCleansBothLists() {
        Organizer organizer = new Organizer("org-1");
        organizer.addCreatedEvent("e1");
        organizer.addCreatedEvent("e1"); // duplicate ignored
        organizer.addActiveEvent("e1");

        assertEquals(1, organizer.getEventCount());
        assertEquals(1, organizer.getActiveEventCount());
        assertTrue(organizer.hasEvent("e1"));
        assertTrue(organizer.hasActiveEvent("e1"));

        organizer.removeCreatedEvent("e1");
        assertFalse(organizer.hasEvent("e1"));
        assertFalse(organizer.hasActiveEvent("e1")); // removed from both
        assertEquals(0, organizer.getEventCount());
        assertEquals(0, organizer.getActiveEventCount());
    }

    @Test
    void activeEvents_addsOnce_andRemoveOnlyActive() {
        Organizer organizer = new Organizer("org-1");
        organizer.addActiveEvent("e2");
        organizer.addActiveEvent("e2"); // duplicate ignored

        assertEquals(1, organizer.getActiveEventCount());
        assertTrue(organizer.hasActiveEvent("e2"));

        organizer.removeActiveEvent("e2");
        assertEquals(0, organizer.getActiveEventCount());
        assertFalse(organizer.hasActiveEvent("e2"));
    }
}
