package com.example.gainly_flow;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class LotterySystemPrivateTest {

    @SuppressWarnings("unchecked")
    private static List<String> sanitize(List<String> in) throws Exception {
        Method m = LotterySystem.class.getDeclaredMethod("sanitizeIds", List.class);
        m.setAccessible(true);
        return (List<String>) m.invoke(null, in);
    }

    @Test
    void sanitizeIds_trimsDedupesAndDropsNulls() throws Exception {
        List<String> out = sanitize(Arrays.asList("  a", "b", "a", null, "  ", "b  ", " c "));
        assertEquals(Arrays.asList("a", "b", "c"), out);
    }

    @Test
    void sanitizeIds_handlesNull() throws Exception {
        assertTrue(sanitize(null).isEmpty());
    }
}
