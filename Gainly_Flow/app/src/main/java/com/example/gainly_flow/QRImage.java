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
 * <p>
 * This class uses the ZXing library to generate QR codes from
 * arbitrary strings, URLs, or event identifiers.
 * </p>
 *
 * <p>All methods are static, and the class cannot be instantiated.</p>
 */
public final class QRImage {

    /**
     * Generates a QR code bitmap from a given string value.
     *
     * @param url     The input string to encode into the QR code (e.g., a URL or deep link).
     * @param sizePx  The desired width and height of the output QR bitmap in pixels.
     * @return A {@link Bitmap} representing the QR code, or {@code null} if generation fails.
     *
     * <p>Example usage:</p>
     * <pre>
     * Bitmap qr = QRImage.bitmapFromUrl("https://example.com/event?id=123", 512);
     * imageView.setImageBitmap(qr);
     * </pre>
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
     *
     * @param url     The input string to encode into the QR code.
     * @param sizePx  The size of the QR code in pixels.
     * @return A byte array containing the QR code in PNG format, or {@code null} if generation fails.
     *
     * <p>This is useful when you need to store or share the QR code as an image file:</p>
     * <pre>
     * byte[] pngData = QRImage.pngFromUrl("https://example.com", 512);
     * Files.write(Paths.get("qr.png"), pngData);
     * </pre>
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
     * <p>
     * This method uses the {@link QRUrlCreator} class to convert the event ID into a canonical deep link or HTTPS URL,
     * and then encodes that URL into a QR code.
     * </p>
     *
     * @param eventId The unique identifier of the event.
     * @param sizePx  The desired width and height of the output QR bitmap in pixels.
     * @return A {@link Bitmap} containing the QR code for the event’s URL, or {@code null} if generation fails.
     *
     * <p>Example:</p>
     * <pre>
     * Bitmap eventQr = QRImage.bitmapFromEventId("abc123", 512);
     * qrImageView.setImageBitmap(eventQr);
     * </pre>
     */
    public static Bitmap bitmapFromEventId(String eventId, int sizePx) {
        String url = QRUrlCreator.buildDeepLink(eventId); // or buildHttpsLink(eventId)
        return bitmapFromUrl(url, sizePx);
    }
}
