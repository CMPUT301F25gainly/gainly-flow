package com.example.gainly_flow;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

public class QRCodeScanner extends AppCompatActivity {

    private DecoratedBarcodeView cameraPreview;
    private FirebaseFirestore db;
    private boolean isScanning = false;

    // Handle camera permission request
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    cameraPreview.resume();
                } else {
                    Toast.makeText(this, "Camera permission denied", Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

        // 🔹 Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // 🔹 Setup Toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // 🔹 Setup Barcode Scanner
        cameraPreview = findViewById(R.id.cameraPreview);
        cameraPreview.decodeContinuous(callback);

        checkCameraPermission();
    }

    private void checkCameraPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            cameraPreview.resume();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    // 🔹 Handle scanned QR result
    private final BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            if (result.getText() == null || isScanning) return;

            isScanning = true; // Prevent double scans
            String qrContent = result.getText().trim();

            Toast.makeText(QRCodeScanner.this, "QR Scanned!", Toast.LENGTH_SHORT).show();

            // Example QR: gainlyflow://event/b276fd55-41c4-4373-aefd-b017aeebcf5d
            if (qrContent.startsWith("gainlyflow://event/")) {
                String eventId = qrContent.substring("gainlyflow://event/".length());
                fetchEventFromFirebase(eventId);
            } else {
                Toast.makeText(QRCodeScanner.this, "Invalid QR Code format", Toast.LENGTH_SHORT).show();
                isScanning = false;
            }
        }
    };

    private void fetchEventFromFirebase(String eventId) {
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        launchEventDetail(documentSnapshot);
                    } else {
                        Toast.makeText(this, "Event not found in Firebase", Toast.LENGTH_SHORT).show();
                        isScanning = false;
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    isScanning = false;
                });
    }

    private void launchEventDetail(DocumentSnapshot doc) {
        try {
            Intent intent = new Intent(this, EventDetailActivity.class);

            // Extract fields safely
            intent.putExtra("event_id", doc.getString("id"));
            intent.putExtra("event_name", doc.getString("name"));
            intent.putExtra("event_description", doc.getString("description"));
            intent.putExtra("event_capacity", parseIntSafely(doc.getString("capacity")));
            intent.putExtra("event_date", parseLongSafely(doc.getString("eventDateUtc")));
            intent.putExtra("registration_open", parseLongSafely(doc.getString("registrationOpenUtc")));
            intent.putExtra("registration_close", parseLongSafely(doc.getString("registrationCloseUtc")));
            intent.putExtra("geo_required", Boolean.parseBoolean(doc.getString("geolocationEnabled")));
            intent.putExtra("event_location", "Unknown");
            intent.putExtra("event_time_string", convertMsToTime(doc.getString("eventTimeOfDayMs")));

            startActivity(intent);
            finish();

        } catch (Exception e) {
            Toast.makeText(this, "Error launching event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            isScanning = false;
        }
    }

    private int parseIntSafely(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }

    private long parseLongSafely(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return 0L;
        }
    }

    private String convertMsToTime(String msString) {
        try {
            long ms = Long.parseLong(msString);
            long hours = (ms / (1000 * 60 * 60)) % 24;
            long minutes = (ms / (1000 * 60)) % 60;
            return String.format("%02d:%02d", hours, minutes);
        } catch (Exception e) {
            return "Not specified";
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraPreview.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraPreview.pause();
    }

    @Override
    protected void onDestroy() {
        cameraPreview.pause();
        super.onDestroy();
    }
}
