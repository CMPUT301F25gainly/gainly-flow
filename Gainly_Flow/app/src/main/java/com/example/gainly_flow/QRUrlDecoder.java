package com.example.gainly_flow;

import android.graphics.Bitmap;
import android.net.Uri;
import androidx.annotation.Nullable;

/**
 * Utility class for decoding Gainly Flow QR URLs and generating QR code bitmaps.
 * <p>
 * Supports multiple QR link formats, including:
 * <ul>
 *   <li>App deeplink format: {@code gainlyflow://event/{id}}</li>
 *   <li>HTTPS format: {@code https://gainlyflow.app/e/{id}}</li>
 *   <li>Legacy payload format: {@code event:{id}}</li>
 * </ul>
 * The decoder extracts the event ID from any of these supported URL structures.
 */
public final class QRUrlDecoder {

    /**
     * Container for a successful decode result.
     * Stores the extracted event ID.
     */
    public static class Result {
        /** The decoded event ID. */
        public final String eventId;

        /**
         * Constructs a {@code Result} instance with the provided event ID.
         *
         * @param eventId the decoded event identifier
         */
        public Result(String eventId) { this.eventId = eventId; }
    }

    /**
     * Decodes a QR URL or payload string to extract the event ID.
     * <p>
     * This method supports the following formats:
     * <ul>
     *   <li><b>App deeplink:</b> {@code gainlyflow://event/{id}}</li>
     *   <li><b>HTTPS format:</b> {@code https://gainlyflow.app/e/{id}}</li>
     *   <li><b>Legacy payload:</b> {@code event:{id}}</li>
     * </ul>
     *
     * @param urlOrPayload the raw QR URL or payload string to decode
     * @return a {@link Result} containing the decoded event ID if recognized;
     *         {@code null} if the input is invalid or does not match a supported pattern
     */
    @Nullable
    public static Result decode(String urlOrPayload) {
        if (urlOrPayload == null || urlOrPayload.trim().isEmpty()) return null;

        Uri uri = Uri.parse(urlOrPayload);

        // A) gainlyflow://event/{id}
        if ("gainlyflow".equalsIgnoreCase(uri.getScheme())
                && "event".equalsIgnoreCase(uri.getAuthority())
                && uri.getPathSegments().size() == 1) {
            String id = uri.getLastPathSegment();
            return (id == null || id.isEmpty()) ? null : new Result(id);
        }

        // B) https://gainlyflow.app/e/{id}
        if ("https".equalsIgnoreCase(uri.getScheme())
                && "gainlyflow.app".equalsIgnoreCase(uri.getAuthority())
                && uri.getPathSegments().size() == 2
                && "e".equalsIgnoreCase(uri.getPathSegments().get(0))) {
            String id = uri.getPathSegments().get(1);
            return (id == null || id.isEmpty()) ? null : new Result(id);
        }

        // C) legacy payload "event:{id}"
        if (urlOrPayload.startsWith("event:")) {
            String id = urlOrPayload.substring("event:".length());
            return id.isEmpty() ? null : new Result(id);
        }

        return null;
    }

    /**
     * Convenience method to generate a QR bitmap directly from a URL or payload string.
     * <p>
     * Internally calls {@link QRImage#bitmapFromUrl(String, int)} to create the bitmap.
     *
     * @param urlOrPayload the URL or payload to encode as a QR code
     * @param sizePx the desired width and height of the bitmap in pixels
     * @return a {@link Bitmap} representing the QR code; {@code null} if the input is invalid
     */
    @Nullable
    public static Bitmap toBitmap(String urlOrPayload, int sizePx) {
        return QRImage.bitmapFromUrl(urlOrPayload, sizePx);
    }
}
