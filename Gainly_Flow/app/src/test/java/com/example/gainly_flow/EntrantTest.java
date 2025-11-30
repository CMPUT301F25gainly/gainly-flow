package com.example.gainly_flow;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class EntrantTest {

    @Test
    void defaults_areInitializedAndRoleIsEntrant() {
        Entrant entrant = new Entrant("user-1");

        assertEquals("Entrant", entrant.getRole());
        assertNotNull(entrant.getEventHistory());
        assertNotNull(entrant.getNotifications());
        assertNotNull(entrant.getCurrentWaitingLists());
        assertNotNull(entrant.getPendingInvitations());
    }

    @Test
    void eventHistory_addsOnceAndRejectsNull() {
        Entrant entrant = new Entrant("user-1");
        entrant.addEventHistory("event-1");
        entrant.addEventHistory("event-1"); // duplicate should be ignored
        entrant.addEventHistory(null);      // ignore nulls

        assertEquals(1, entrant.getEventHistory().size());
        assertTrue(entrant.hasEventInHistory("event-1"));
    }

    @Test
    void waitingList_joinLeaveAndDedup() {
        Entrant entrant = new Entrant("user-1");

        entrant.joinWaitingList("ev1");
        entrant.joinWaitingList("ev1"); // duplicate ignored
        entrant.joinWaitingList("ev2");

        assertTrue(entrant.isOnWaitingList("ev1"));
        assertEquals(2, entrant.getCurrentWaitingLists().size());

        entrant.leaveWaitingList("ev1");
        assertFalse(entrant.isOnWaitingList("ev1"));
        assertEquals(1, entrant.getCurrentWaitingLists().size());
    }

    @Test
    void pendingInvitations_addRemoveAndDedup() {
        Entrant entrant = new Entrant("user-1");
        entrant.addPendingInvitation("inv1");
        entrant.addPendingInvitation("inv1"); // duplicate ignored
        entrant.addPendingInvitation("inv2");

        assertTrue(entrant.hasPendingInvitation("inv1"));
        assertEquals(2, entrant.getPendingInvitations().size());

        entrant.removePendingInvitation("inv1");
        assertFalse(entrant.hasPendingInvitation("inv1"));
        assertEquals(1, entrant.getPendingInvitations().size());
    }

    @Test
    void notifications_unreadCountRespectsReadFlag() {
        Entrant entrant = new Entrant("user-1");

        NotificationItem n1 = new NotificationItem("Win", "msg", "WIN", "user-1");
        NotificationItem n2 = new NotificationItem("Info", "msg2", "INFO", "user-1");
        n2.setRead(true);

        entrant.setNotifications(Arrays.asList(n1, n2));
        assertEquals(1, entrant.getUnreadNotificationCount());

        n1.markAsRead();
        assertEquals(0, entrant.getUnreadNotificationCount());
    }
}
