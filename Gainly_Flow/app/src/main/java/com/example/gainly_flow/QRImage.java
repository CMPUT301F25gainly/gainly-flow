package com.example.gainly_flow;

import android.graphics.Bitmap;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import java.io.ByteArrayOutputStream;
import java.util.EnumMap;
import java.util.Map;

public final class QRImage {

    /** Render a QR Bitmap from any string (your qrUrl). */
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

    /** If you prefer PNG bytes (to store or share). */
    public static byte[] pngFromUrl(String url, int sizePx) {
        Bitmap b = bitmapFromUrl(url, sizePx);
        if (b == null) return null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.PNG, 100, out);
        return out.toByteArray();
    }

    /** Convenience: generate from eventId using your canonical URL format. */
    public static Bitmap bitmapFromEventId(String eventId, int sizePx) {
        String url = QRUrlCreator.buildDeepLink(eventId); // or buildHttpsLink(eventId)
        return bitmapFromUrl(url, sizePx);
    }
}

