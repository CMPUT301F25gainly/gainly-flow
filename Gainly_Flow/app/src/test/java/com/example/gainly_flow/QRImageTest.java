package com.example.gainly_flow;

import static org.junit.Assert.*;

import android.graphics.Bitmap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class QRImageTest {

    private static final int BLACK = 0xFF000000;
    private static final int WHITE = 0xFFFFFFFF;

    @Test
    public void bitmapFromUrl_validInput_returnsBitmapWithRequestedSize() {
        String url = "https://example.com/abc?id=123";
        int size = 128;

        Bitmap bmp = QRImage.bitmapFromUrl(url, size);

        assertNotNull("Bitmap should not be null for a valid URL", bmp);
        assertEquals(size, bmp.getWidth());
        assertEquals(size, bmp.getHeight());
    }

    @Test
    public void bitmapFromUrl_invalidInput_returnsNull() {
        // ZXing throws on empty contents; QRImage catches and returns null
        Bitmap bmpEmpty = QRImage.bitmapFromUrl("", 128);
        assertNull("Empty string should yield null", bmpEmpty);

        // Also guard very small sizes
        Bitmap bmpTiny = QRImage.bitmapFromUrl("https://example.com", 0);
        assertNull("Zero size should yield null", bmpTiny);
    }

    @Test
    public void bitmapFromUrl_pixelsAreOnlyBlackOrWhite() {
        String url = "https://example.com/qr";
        int size = 96;

        Bitmap bmp = QRImage.bitmapFromUrl(url, size);
        assertNotNull(bmp);

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int px = bmp.getPixel(x, y);
                assertTrue(
                        "Pixel must be strictly black or white at (" + x + "," + y + ")",
                        px == BLACK || px == WHITE
                );
            }
        }
    }

    @Test
    public void bitmapFromUrl_hasWhiteQuietZoneBorder() {
        // Margin=1 in QRImage; the very corner should be white
        String url = "https://example.com/quiet-zone";
        Bitmap bmp = QRImage.bitmapFromUrl(url, 128);
        assertNotNull(bmp);

        // Check a few corners for white (quiet zone). Not a strict spec test,
        // but good sanity for our MARGIN(1) hint.
        assertEquals("Top-left should be white", WHITE, bmp.getPixel(0, 0));
        assertEquals("Top-right should be white", WHITE, bmp.getPixel(bmp.getWidth()-1, 0));
        assertEquals("Bottom-left should be white", WHITE, bmp.getPixel(0, bmp.getHeight()-1));
        assertEquals("Bottom-right should be white", WHITE, bmp.getPixel(bmp.getWidth()-1, bmp.getHeight()-1));
    }

    @Test
    public void pngFromUrl_validInput_returnsPngWithSignature() {
        String url = "https://example.com/share?id=42";
        byte[] png = QRImage.pngFromUrl(url, 128);

        assertNotNull("PNG bytes should not be null", png);
        assertTrue("PNG should be non-empty", png.length > 8);

        // Check PNG magic number: 89 50 4E 47 0D 0A 1A 0A
        byte[] sig = new byte[] {(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        assertTrue("PNG signature mismatch", Arrays.equals(sig, Arrays.copyOfRange(png, 0, 8)));
    }

    @Test
    public void pngFromUrl_invalidInput_returnsNull() {
        assertNull(QRImage.pngFromUrl("", 128));
        assertNull(QRImage.pngFromUrl("https://example.com", 0));
    }

    @Test
    public void bitmapFromEventId_matchesBitmapFromUrlOfDeepLink() {
        // This test assumes your QRUrlCreator.buildDeepLink(eventId) is available
        // and deterministic. We compare the two generated bitmaps pixel-by-pixel.
        String eventId = "event-12345";
        int size = 128;

        String deepLink = QRUrlCreator.buildDeepLink(eventId);
        Bitmap fromEvent = QRImage.bitmapFromEventId(eventId, size);
        Bitmap fromUrl   = QRImage.bitmapFromUrl(deepLink, size);

        assertNotNull(fromEvent);
        assertNotNull(fromUrl);

        assertTrue("Bitmaps must be identical for same payload",
                bitmapsEqual(fromEvent, fromUrl));
    }

    // ---------- helpers ----------

    private static boolean bitmapsEqual(Bitmap a, Bitmap b) {
        if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) return false;

        int w = a.getWidth(), h = a.getHeight();
        int[] rowA = new int[w];
        int[] rowB = new int[w];

        for (int y = 0; y < h; y++) {
            a.getPixels(rowA, 0, w, 0, y, w, 1);
            b.getPixels(rowB, 0, w, 0, y, w, 1);
            if (!Arrays.equals(rowA, rowB)) return false;
        }
        return true;
    }
}
