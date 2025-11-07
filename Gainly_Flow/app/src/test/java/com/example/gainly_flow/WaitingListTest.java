package com.example.gainly_flow;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class WaitingListTest {
    @BeforeAll
    static void disableDb() {
        WaitingList.disablePersistenceForUnitTests();
    }

    private WaitingList newList() {
        return new WaitingList("event-123");
    }

    @Test
    void addEntrant_addsUnique_andRespectsCapacity_andIgnoresDuplicate() {
        WaitingList wl = newList();
        int cap = 2;

        wl.addEntrant("u1", cap);
        assertEquals(1, wl.getCount());
        assertTrue(wl.getEntrants().contains("u1"));

        wl.addEntrant("u1", cap); // duplicate — should NO-OP (per file)
        assertEquals(1, wl.getCount());

        wl.addEntrant("u2", cap);
        assertEquals(2, wl.getCount());

        wl.addEntrant("u3", cap); // over capacity — should NO-OP
        assertEquals(2, wl.getCount());
        assertFalse(wl.getEntrants().contains("u3"));
    }

    @Test
    void removeEntrant_removesIfPresent_andNoopsIfMissing() {
        WaitingList wl = newList();
        wl.addEntrant("u1", 5);
        wl.addEntrant("u2", 5);

        wl.removeEntrant("u1");
        assertEquals(1, wl.getCount());
        assertFalse(wl.getEntrants().contains("u1"));

        wl.removeEntrant("nope"); // NO-OP
        assertEquals(1, wl.getCount());
    }

    @Test
    void getEntrants_returnsDefensiveCopy() {
        WaitingList wl = newList();
        wl.addEntrant("a", 3);
        List<String> copy = wl.getEntrants();
        copy.clear();
        assertEquals(1, wl.getCount());
        assertTrue(wl.getEntrants().contains("a"));
    }
}
