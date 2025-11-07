package com.example.gainly_flow;

import android.net.Uri;

/**
 * Utility class for generating event-related URLs in different formats.
 * <p>
 * This class provides two styles of QR-compatible URL builders:
 * <ul>
 *     <li><b>Deep link URLs</b> — App-specific URIs that trigger an intent-filter
 *     within the Gainly Flow Android app (e.g., {@code gainlyflow://event/{id}}).</li>
 *     <li><b>HTTPS URLs</b> — Web-based links suitable for Firebase Dynamic Links
 *     or sharing via a website (e.g., {@code https://gainlyflow.app/e/{id}}).</li>
 * </ul>
 * Both methods return well-formed, encoded {@link Uri} strings that can be used for
 * QR code generation or link sharing.
 */
public final class QRUrlCreator {

    /**
     * Builds an app-deep-link style URL for the specified event.
     * <p>
     * This format is designed for use with Android intent-filters so that scanning
     * a QR code or clicking the link directly opens the Gainly Flow app and navigates
     * to the corresponding event page.
     * </p>
     *
     * <pre>
     * Example output:
     *     gainlyflow://event/{eventId}
     * </pre>
     *
     * @param eventId the unique identifier of the event.
     * @return a deep link URI string in the format {@code gainlyflow://event/{id}}.
     */
    public static String buildDeepLink(String eventId) {
        return new Uri.Builder()
                .scheme("gainlyflow")
                .authority("event")
                .appendPath(eventId)
                .build()
                .toString();
    }

    /**
     * Builds an HTTPS-style link for the specified event.
     * <p>
     * This format is intended for web access or Firebase Dynamic Links integration,
     * allowing users to open event details in a browser or app depending on their context.
     * </p>
     *
     * <pre>
     * Example output:
     *     https://gainlyflow.app/e/{eventId}
     * </pre>
     *
     * @param eventId the unique identifier of the event.
     * @return an HTTPS URI string in the format {@code https://gainlyflow.app/e/{id}}.
     */
    public static String buildHttpsLink(String eventId) {
        return new Uri.Builder()
                .scheme("https")
                .authority("gainlyflow.app")
                .appendPath("e")
                .appendPath(eventId)
                .build()
                .toString();
    }
}
