package com.example.gainly_flow;

import org.junit.jupiter.api.Test;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;

class EventTest {

    private Event makeBaseEvent() {
        Event e = new Event();
        // capacity/currentParticipants
        // price/category/location/flags
        // Only touching pure logic fields here:
        // (Assumes standard setters exist as in your file.)
        e.setCapacity(10);
        e.setCurrentParticipants(3);
        e.setActive(true);
        // registration window: open yesterday, closes tomorrow
        Date now = new Date();
        e.setRegistrationPeriod(new Date(now.getTime() - 24L*3600*1000), new Date(now.getTime() + 24L*3600*1000));
        e.setPrice(12.5);
        return e;
    }

    @Test
    void isFull_false_whenBelowCapacity() {
        Event e = makeBaseEvent();
        assertFalse(e.isFull());
    }

    @Test
    void isFull_true_whenAtOrAboveCapacity() {
        Event e = makeBaseEvent();
        e.setCurrentParticipants(10);
        assertTrue(e.isFull());
        e.setCurrentParticipants(11);
        assertTrue(e.isFull());
    }

    @Test
    void getAvailableSpots_correct() {
        Event e = makeBaseEvent();
        assertEquals(7, e.getAvailableSpots());
        e.setCurrentParticipants(10);
        assertEquals(0, e.getAvailableSpots());
    }

    @Test
    void isRegistrationOpen_true_whenWithinWindowActiveAndNotFull() {
        Event e = makeBaseEvent();
        assertTrue(e.isRegistrationOpen());
    }

    @Test
    void isRegistrationOpen_false_whenInactive_orOutsideWindow_orFull() {
        Event e = makeBaseEvent();
        // inactive
        e.setActive(false);
        assertFalse(e.isRegistrationOpen());

        // outside window
        e.setActive(true);
        Date now = new Date();
        e.setRegistrationPeriod(new Date(now.getTime() + 60_000), new Date(now.getTime() + 120_000)); // opens in 1 min
        assertFalse(e.isRegistrationOpen());

        // full
        e.setRegistrationPeriod(new Date(now.getTime() - 60_000), new Date(now.getTime() + 60_000)); // opened 1 min ago
        e.setCurrentParticipants(e.getCapacity());
        assertFalse(e.isRegistrationOpen());
    }

    @Test
    void formatPrice_formatsToCurrencyOrFree() {
        Event e = makeBaseEvent();
        e.setPrice(0.0);
        assertEquals("Free", e.getFormattedPrice());
        e.setPrice(12.5);
        assertEquals("$12.50", e.getFormattedPrice());
    }
}
