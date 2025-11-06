package com.example.gainly_flow;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;
import java.util.EnumMap;
import java.util.Map;

public class QRCodeGenerator {

    public byte[] generateForEvent(String eventId) {
        if (eventId == null || eventId.trim().isEmpty()) return null;

        // Encode whatever you want scanned. Here: a compact payload with the id.
        String payload = "event:" + eventId;   // you can switch to JSON if you like

        int size = 512; // px
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix m = new MultiFormatWriter()
                    .encode(payload, BarcodeFormat.QR_CODE, size, size, hints);

            Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    bmp.setPixel(x, y, m.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
            return out.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }
}
