package com.example.gainly_flow;

import static org.junit.Assert.*;

import android.net.Uri;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class QRUrlCreatorTest {

    @Test
    public void buildDeepLink_basic() {
        String id = "abc123";
        String url = QRUrlCreator.buildDeepLink(id);

        assertEquals("gainlyflow://event/abc123", url);

        Uri u = Uri.parse(url);
        assertEquals("gainlyflow", u.getScheme());
        assertEquals("event", u.getAuthority());
        assertEquals(1, u.getPathSegments().size());
        assertEquals("abc123", u.getLastPathSegment());
        assertEquals("/abc123", u.getPath());
    }

    @Test
    public void buildHttpsLink_basic() {
        String id = "xyz789";
        String url = QRUrlCreator.buildHttpsLink(id);

        assertEquals("https://gainlyflow.app/e/xyz789", url);

        Uri u = Uri.parse(url);
        assertEquals("https", u.getScheme());
        assertEquals("gainlyflow.app", u.getAuthority());
        assertEquals(2, u.getPathSegments().size());
        assertEquals("e", u.getPathSegments().get(0));
        assertEquals("xyz789", u.getPathSegments().get(1));
        assertEquals("/e/xyz789", u.getPath());
    }

    @Test
    public void buildDeepLink_encodesSpecialCharacters() {
        String id = "ID with spaces & symbols/#?";
        String url = QRUrlCreator.buildDeepLink(id);

        // Ensure it round-trips via Uri parsing
        Uri u = Uri.parse(url);
        assertEquals("gainlyflow", u.getScheme());
        assertEquals("event", u.getAuthority());
        assertEquals(1, u.getPathSegments().size());
        // The segment should decode back to the original ID
        assertEquals(id, u.getLastPathSegment());

        // And the raw string should be percent-encoded where needed
        assertTrue(url.startsWith("gainlyflow://event/"));
        assertTrue("Should contain %20 for spaces", url.contains("%20"));
        assertFalse("Should not contain raw space", url.contains(" "));
        // '#' and '?' in path must be encoded
        assertFalse("Raw # should not appear", url.contains("#"));
        assertFalse("Raw ? should not appear", url.contains("?"));
    }

    @Test
    public void buildHttpsLink_encodesSpecialCharacters() {
        String id = "Üñîçødë/part?x=1#y";
        String url = QRUrlCreator.buildHttpsLink(id);

        Uri u = Uri.parse(url);
        assertEquals("https", u.getScheme());
        assertEquals("gainlyflow.app", u.getAuthority());
        assertEquals(2, u.getPathSegments().size());
        // path segment 1 should be the encoded original id; parsing decodes it
        assertEquals("e", u.getPathSegments().get(0));
        assertEquals(id, u.getPathSegments().get(1));

        assertTrue(url.startsWith("https://gainlyflow.app/e/"));
        // Ensure reserved characters were encoded in the string form
        assertFalse(url.contains("#"));
        assertFalse(url.contains("?")); // any '?' from id should be encoded, not start a query
    }

    @Test
    public void buildDeepLink_emptyId_resultsInTrailingSlash() {
        String url = QRUrlCreator.buildDeepLink("");
        assertEquals("gainlyflow://event/", url);

        Uri u = Uri.parse(url);
        assertEquals("gainlyflow", u.getScheme());
        assertEquals("event", u.getAuthority());
        // appendPath("") creates an empty segment -> path "/"
        assertEquals("/", u.getPath());
        assertEquals(0, u.getPathSegments().size()); // no non-empty segments
    }

    @Test
    public void buildHttpsLink_emptyId_resultsInTrailingSlash() {
        String url = QRUrlCreator.buildHttpsLink("");
        assertEquals("https://gainlyflow.app/e/", url);

        Uri u = Uri.parse(url);
        assertEquals("https", u.getScheme());
        assertEquals("gainlyflow.app", u.getAuthority());
        assertEquals("/e/", u.getPath());
        assertEquals(1, u.getPathSegments().size());
        assertEquals("e", u.getPathSegments().get(0));
    }
}
