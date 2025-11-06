package com.example.gainly_flow;


import android.graphics.Bitmap;
import android.net.Uri;
import androidx.annotation.Nullable;

public final class QRUrlDecoder {

    public static class Result {
        public final String eventId;
        public Result(String eventId) { this.eventId = eventId; }
    }

    /** Accepts deeplink like gainlyflow://event/{id} or https like https://gainlyflow.app/e/{id} */
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

    /** Convenience: build a QR image directly from a URL/payload. */
    @Nullable
    public static Bitmap toBitmap(String urlOrPayload, int sizePx) {
        return QRImage.bitmapFromUrl(urlOrPayload, sizePx);
    }
}