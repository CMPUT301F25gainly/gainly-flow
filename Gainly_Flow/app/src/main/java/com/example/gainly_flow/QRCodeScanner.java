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

/**
 * {@code QRCodeScanner} is an {@link AppCompatActivity} responsible for scanning QR codes
 * and retrieving corresponding event data from Firebase Firestore.
 * <p>
 * The activity uses {@link com.journeyapps.barcodescanner.DecoratedBarcodeView} to continuously
 * decode QR codes through the camera feed. When a QR code matching the expected
 * "gainlyflow://event/{eventId}" format is detected, the associated event is fetched
 * from the "events" collection in Firestore, and the user is redirected to
 * {@link EventDetailActivity} to view event details.
 * </p>
 *
 * <p>
 * If the camera permission is not granted, the activity requests it at runtime.
 * </p>
 *
 * <h3>Expected QR Format:</h3>
 * <pre>
 *     gainlyflow://event/{eventId}
 * </pre>
 *
 * <h3>Responsibilities:</h3>
 * <ul>
 *     <li>Request and verify camera permission.</li>
 *     <li>Continuously scan QR codes using the device camera.</li>
 *     <li>Validate QR content and extract event IDs.</li>
 *     <li>Fetch event data from Firebase Firestore.</li>
 *     <li>Launch the {@link EventDetailActivity} with event data.</li>
 * </ul>
 */
public class QRCodeScanner extends AppCompatActivity {

    /** The camera preview used to scan QR codes. */
    private DecoratedBarcodeView cameraPreview;

    /** Firestore instance for database operations. */
    private FirebaseFirestore db;

    /** Flag to prevent duplicate scanning of the same QR code. */
    private boolean isScanning = false;

    /**
     * Activity result launcher for requesting camera permission at runtime.
     * If granted, resumes the camera preview; otherwise, terminates the activity.
     */
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    cameraPreview.resume();
                } else {
                    Toast.makeText(this, "Camera permission denied", Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    /**
     * Initializes the activity, sets up the toolbar and barcode scanner, and checks camera permission.
     *
     * @param savedInstanceState Saved instance state for restoring the previous UI state, if available.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

        db = FirebaseFirestore.getInstance();

        // Setup toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Setup barcode scanner
        cameraPreview = findViewById(R.id.cameraPreview);
        cameraPreview.decodeContinuous(callback);

        checkCameraPermission();
    }

    /**
     * Checks whether the camera permission is granted.
     * If not, it requests the permission from the user.
     */
    private void checkCameraPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            cameraPreview.resume();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    /**
     * Callback handler invoked when a barcode is successfully scanned.
     * Validates the scanned content and fetches the event from Firebase if the format is valid.
     */
    private final BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            if (result.getText() == null || isScanning) return;

            isScanning = true;
            String qrContent = result.getText().trim();
            Toast.makeText(QRCodeScanner.this, "QR Scanned!", Toast.LENGTH_SHORT).show();

            if (qrContent.startsWith("gainlyflow://event/")) {
                String eventId = qrContent.substring("gainlyflow://event/".length());
                fetchEventFromFirebase(eventId);
            } else {
                Toast.makeText(QRCodeScanner.this, "Invalid QR Code format", Toast.LENGTH_SHORT).show();
                isScanning = false;
            }
        }
    };

    /**
     * Fetches event data from Firebase Firestore using the provided event ID.
     *
     * @param eventId The unique identifier of the event to be retrieved.
     */
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

    /**
     * Launches {@link EventDetailActivity} with the details extracted from the given Firestore document.
     *
     * @param doc The Firestore document containing event information.
     */
    private void launchEventDetail(DocumentSnapshot doc) {
        try {
            Intent intent = new Intent(this, EventDetailActivity.class);

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

    /**
     * Safely parses an integer value from a string.
     *
     * @param value The string to parse.
     * @return The integer value, or {@code 0} if parsing fails.
     */
    private int parseIntSafely(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Safely parses a long value from a string.
     *
     * @param value The string to parse.
     * @return The long value, or {@code 0L} if parsing fails.
     */
    private long parseLongSafely(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * Converts milliseconds since midnight to a human-readable time string (HH:mm).
     *
     * @param msString Milliseconds as a string.
     * @return The formatted time string, or "Not specified" if parsing fails.
     */
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

    /** Resumes the camera preview when the activity is resumed. */
    @Override
    protected void onResume() {
        super.onResume();
        cameraPreview.resume();
    }

    /** Pauses the camera preview when the activity is paused. */
    @Override
    protected void onPause() {
        super.onPause();
        cameraPreview.pause();
    }

    /** Releases camera resources when the activity is destroyed. */
    @Override
    protected void onDestroy() {
        cameraPreview.pause();
        super.onDestroy();
    }
}
