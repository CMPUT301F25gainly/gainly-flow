package com.example.gainly_flow;

import android.graphics.Bitmap;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import java.io.ByteArrayOutputStream;
import java.util.EnumMap;
import java.util.Map;

/**
 * Utility class for generating QR code images and PNG data.
 * This class uses the ZXing library to generate QR codes from
 * arbitrary strings, URLs, or event identifiers.
 * All methods are static, and the class cannot be instantiated.
 *
 * @author Gainly Flow Team
 * @version 1.0
 */
public final class QRImage {

    /**
     * Generates a QR code bitmap from a given string value.
     *
     * @param url     The input string to encode into the QR code (e.g., a URL or deep link).
     * @param sizePx  The desired width and height of the output QR bitmap in pixels.
     * @return A Bitmap representing the QR code, or null if generation fails.
     */
    public static Bitmap bitmapFromUrl(String url, int sizePx) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix m = new MultiFormatWriter()
                    .encode(url, BarcodeFormat.QR_CODE, sizePx, sizePx, hints);

            Bitmap bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
            for (int y = 0; y < sizePx; y++) {
                for (int x = 0; x < sizePx; x++) {
                    bmp.setPixel(x, y, m.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            return bmp;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Generates a PNG byte array representation of a QR code for the given string.
     * This is useful when you need to store or share the QR code as an image file.
     *
     * @param url     The input string to encode into the QR code.
     * @param sizePx  The size of the QR code in pixels.
     * @return A byte array containing the QR code in PNG format, or null if generation fails.
     */
    public static byte[] pngFromUrl(String url, int sizePx) {
        Bitmap b = bitmapFromUrl(url, sizePx);
        if (b == null) return null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.PNG, 100, out);
        return out.toByteArray();
    }

    /**
     * Generates a QR code bitmap from an event identifier.
     * This method uses the QRUrlCreator class to convert the event ID into a canonical
     * deep link or HTTPS URL, and then encodes that URL into a QR code.
     *
     * @param eventId The unique identifier of the event.
     * @param sizePx  The desired width and height of the output QR bitmap in pixels.
     * @return A Bitmap containing the QR code for the event's URL, or null if generation fails.
     * @see QRUrlCreator
     */
    public static Bitmap bitmapFromEventId(String eventId, int sizePx) {
        String url = QRUrlCreator.buildDeepLink(eventId); // or buildHttpsLink(eventId)
        return bitmapFromUrl(url, sizePx);
    }
}
