package com.example.gainly_flow;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;
import java.util.EnumMap;
import java.util.Map;

/**
 * Utility class for generating QR codes as byte arrays.
 * <p>
 * This class uses the ZXing library to encode text payloads (typically event IDs)
 * into QR codes. The resulting QR image is rendered as a {@link Bitmap} and then
 * converted into a PNG byte array for easy storage or transmission (e.g., for
 * displaying or uploading to Firebase Storage).
 * </p>
 */
public class QRCodeGenerator {

    /**
     * Generates a QR code image representing a specific event ID.
     * <p>
     * The encoded payload follows the format {@code "event:<eventId>"} to identify
     * the associated event when scanned. The generated QR code is square (512×512 pixels)
     * and has a minimal white margin.
     * </p>
     *
     * @param eventId the unique identifier of the event to encode into the QR code.
     *                Must be non-null and non-empty.
     * @return a byte array containing the PNG-encoded QR code image,
     *         or {@code null} if the {@code eventId} is invalid or encoding fails.
     */
    public byte[] generateForEvent(String eventId) {
        if (eventId == null || eventId.trim().isEmpty()) return null;

        // Encode the payload: a simple event reference.
        String payload = "event:" + eventId;

        int size = 512; // Output image dimensions in pixels.
        try {
            // Configure QR generation options (e.g., minimal border).
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.MARGIN, 1);

            // Generate the QR code matrix using ZXing.
            BitMatrix m = new MultiFormatWriter()
                    .encode(payload, BarcodeFormat.QR_CODE, size, size, hints);

            // Convert the matrix to a bitmap image.
            Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    bmp.setPixel(x, y, m.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }

            // Compress the bitmap into a PNG byte array.
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
            return out.toByteArray();
        } catch (Exception e) {
            // Return null if any step fails (e.g., encoding or I/O).
            return null;
        }
    }
}
