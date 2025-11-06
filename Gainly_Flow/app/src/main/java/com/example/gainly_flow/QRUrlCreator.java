package com.example.gainly_flow;


import android.net.Uri;

public final class QRUrlCreator {
    // Choose ONE style and keep it consistent across the app:

    // A) App-deeplink style (works great with an intent-filter)
    // Example: gainlyflow://event/{id}
    public static String buildDeepLink(String eventId) {
        return new Uri.Builder()
                .scheme("gainlyflow")
                .authority("event")
                .appendPath(eventId)
                .build()
                .toString();
    }

    // B) HTTPS style (good for Firebase Dynamic Links / website)
    // Example: https://gainlyflow.app/e/{id}
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
