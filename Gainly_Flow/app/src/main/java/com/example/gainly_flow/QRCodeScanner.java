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
 * Activity that scans QR codes, validates event links, and opens the corresponding
 * event details screen after fetching the event from Firestore.
 * Expects codes in the form {@code gainlyflow://event/{eventId}} and requests camera
 * permission at runtime if it has not yet been granted.
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
     * Launches {@link EventDetailActivity} with the event ID from the Firestore document.
     * EventDetailActivity will load the full event details using Event.fromDocument().
     *
     * @param doc The Firestore document containing event information.
     */
    private void launchEventDetail(DocumentSnapshot doc) {
        try {
            String eventId = doc.getId();
            if (eventId == null || eventId.isEmpty()) {
                Toast.makeText(this, "Invalid event ID", Toast.LENGTH_SHORT).show();
                isScanning = false;
                return;
            }

            Intent intent = new Intent(this, EventDetailActivity.class);
            intent.putExtra("event_id", eventId);

            startActivity(intent);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Error launching event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            isScanning = false;
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
