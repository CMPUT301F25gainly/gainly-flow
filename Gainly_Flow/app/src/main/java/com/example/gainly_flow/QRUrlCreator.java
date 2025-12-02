package com.example.gainly_flow;

import android.net.Uri;

/**
 * Utility for building QR-compatible deep links and HTTPS links for events.
 * Provides helpers for {@code gainlyflow://event/{id}} URIs used by the app
 * and {@code https://gainlyflow.app/e/{id}} links suitable for sharing or
 * integration with Firebase Dynamic Links.
 */
public final class QRUrlCreator {

    /**
     * Builds an app-deep-link style URL for the specified event so scanned QR codes
     * or clicked links can open the Gainly Flow app at the event page.
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
     * Builds an HTTPS-style link for the specified event for web access or use
     * with Firebase Dynamic Links so browsers or apps can open the event.
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
