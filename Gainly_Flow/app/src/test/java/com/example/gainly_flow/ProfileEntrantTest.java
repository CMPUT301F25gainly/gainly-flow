package com.example.gainly_flow;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProfileEntrantTest {

    @Test
    void toString_formats() {
        ProfileEntrant p = new ProfileEntrant("id1", "Alice", "a@x.com");
        assertEquals("Alice • a@x.com", p.toString());

        ProfileEntrant q = new ProfileEntrant("id2", "Bob", null);
        assertEquals("Bob", q.toString());
    }
}
