package com.example.gainly_flow;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.sql.Time;
import java.util.Date;

public class QRCodeScanner extends AppCompatActivity {

    private DecoratedBarcodeView barcodeView;
    private TextView tvInstruction, tvSubtitle;
    private MaterialButton btnUploadQR;
    private Chip chipCameraStatus;
    private boolean isScanProcessed = false;

    private FirebaseFirestore db;

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                        String decoded = decodeFromBitmap(bitmap);
                        if (decoded != null) {
                            processScannedResult(decoded);
                        } else {
                            tvSubtitle.setText("No QR found in image.");
                        }
                    } catch (Exception e) {
                        tvSubtitle.setText("Error decoding image.");
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Toolbar back button setup
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        barcodeView = findViewById(R.id.cameraPreview);
        tvInstruction = findViewById(R.id.tvInstruction);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        btnUploadQR = findViewById(R.id.btnUploadQR);
        chipCameraStatus = findViewById(R.id.chipCameraStatus);

        checkCameraPermission();

        barcodeView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (result != null && !isScanProcessed) {
                    isScanProcessed = true;
                    processScannedResult(result.getText());
                }
            }
        });

        btnUploadQR.setOnClickListener(v -> openImagePicker());
    }

    private void processScannedResult(String scannedText) {
        tvSubtitle.setText("✅ QR Detected: " + scannedText);
        chipCameraStatus.setText("Processing...");

        // Decode the QR content to get event ID using your QRUrlDecoder
        String eventId = decodeEventIdFromQR(scannedText);

        if (eventId != null && !eventId.isEmpty()) {
            // Fetch event details from Firebase
            fetchEventFromFirebase(eventId);
        } else {
            // Handle invalid QR code
            tvSubtitle.setText("❌ Invalid event QR code");
            chipCameraStatus.setText("Scan failed");
            resetScannerAfterDelay();
        }
    }

    private String decodeEventIdFromQR(String scannedText) {
        try {
            // Use your existing QRUrlDecoder class
            Object decodedResult = QRUrlDecoder.decode(scannedText);

            // Since we can't access the Result class directly, let's use reflection or alternative approach
            // Alternative approach: Check if the scanned text contains event patterns
            if (scannedText != null) {
                // Check for gainlyflow://event/{id} pattern
                if (scannedText.startsWith("gainlyflow://event/")) {
                    return scannedText.substring("gainlyflow://event/".length());
                }
                // Check for https://gainlyflow.app/e/{id} pattern
                else if (scannedText.startsWith("https://gainlyflow.app/e/")) {
                    return scannedText.substring("https://gainlyflow.app/e/".length());
                }
                // Check for legacy event:{id} pattern
                else if (scannedText.startsWith("event:")) {
                    return scannedText.substring("event:".length());
                }
                // If it's just a plain event ID, use it directly
                else if (scannedText.matches("[a-zA-Z0-9]{20,}")) { // Simple pattern for Firestore IDs
                    return scannedText;
                }
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void fetchEventFromFirebase(String eventId) {
        db.collection("events")
                .document(eventId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        // Convert Firestore document to your Event object
                        DocumentSnapshot document = task.getResult();
                        Event event = documentToEvent(document);
                        if (event != null) {
                            navigateToEventDetail(event);
                        } else {
                            showError("Failed to parse event data");
                        }
                    } else {
                        // Event not found or error
                        showError("Event not found or has been removed");
                    }
                })
                .addOnFailureListener(e -> {
                    showError("Error fetching event: " + e.getMessage());
                });
    }

    private Event documentToEvent(DocumentSnapshot document) {
        try {
            Event event = new Event(document.getId());

            // Set basic event info
            if (document.contains("name")) {
                event.setName(document.getString("name"));
            }
            if (document.contains("description")) {
                event.setDescription(document.getString("description"));
            }
            if (document.contains("eventDate")) {
                Date eventDate = document.getDate("eventDate");
                event.setEventDate(eventDate);
            }
            if (document.contains("eventTime")) {
                // Handle time - you might need to adjust based on how you store time in Firestore
                String timeString = document.getString("eventTime");
                if (timeString != null) {
                    // Try to parse as SQL Time or store as string
                    try {
                        Time eventTime = Time.valueOf(timeString);
                        event.setEventTime(eventTime);
                    } catch (Exception e) {
                        // If it's not in SQL Time format, store as string
                        event.setTimeString(timeString);
                    }
                }
            }
            if (document.contains("registrationOpen") && document.contains("registrationClose")) {
                Date regOpen = document.getDate("registrationOpen");
                Date regClose = document.getDate("registrationClose");
                event.setRegistrationPeriod(regOpen, regClose);
            }
            if (document.contains("capacity")) {
                Long capacity = document.getLong("capacity");
                if (capacity != null) {
                    event.setCapacity(capacity.intValue());
                }
            }
            if (document.contains("geolocationRequired")) {
                Boolean geoRequired = document.getBoolean("geolocationRequired");
                if (geoRequired != null) {
                    event.setGeolocationRequired(geoRequired);
                }
            }
            if (document.contains("posterImageId")) {
                event.setPosterImage(document.getString("posterImageId"));
            }
            if (document.contains("organizerId")) {
                event.setOrganizerId(document.getString("organizerId"));
            }
            if (document.contains("qrUrl")) {
                event.setQrUrl(document.getString("qrUrl"));
            }
            if (document.contains("location")) {
                event.setLocation(document.getString("location"));
            }

            return event;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void navigateToEventDetail(Event event) {
        Intent intent = new Intent(this, EventDetailActivity.class);

        // Pass event data as individual extras (matching your current EventDetailActivity)
        intent.putExtra("event_id", event.getId());
        intent.putExtra("event_name", event.getName());
        intent.putExtra("event_description", event.getDescription());
        intent.putExtra("event_capacity", event.getCapacity());
        intent.putExtra("geo_required", event.isGeolocationRequired());

        // Pass date information
        if (event.getEventDate() != null) {
            intent.putExtra("event_date", event.getEventDate().getTime());
        }
        if (event.getRegistrationOpen() != null) {
            intent.putExtra("registration_open", event.getRegistrationOpen().getTime());
        }
        if (event.getRegistrationClose() != null) {
            intent.putExtra("registration_close", event.getRegistrationClose().getTime());
        }

        // Pass location if available
        if (event.getLocation() != null) {
            intent.putExtra("event_location", event.getLocation());
        }

        // Pass time information
        intent.putExtra("event_time_string", event.getEventTimeString());

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        Toast.makeText(this, "Event found! Redirecting...", Toast.LENGTH_SHORT).show();
        startActivity(intent);
    }

    private void showError(String message) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Scan Error")
                .setMessage(message)
                .setPositiveButton("Try Again", (dialog, which) -> resetScanner())
                .setCancelable(false)
                .show();
    }

    private void resetScanner() {
        isScanProcessed = false;
        chipCameraStatus.setText("Ready to scan");
        tvSubtitle.setText("Scan a QR code to view event details");
        barcodeView.resume();
    }

    private void resetScannerAfterDelay() {
        barcodeView.postDelayed(this::resetScanner, 2000);
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 100);
        } else {
            barcodeView.resume();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            barcodeView.resume();
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
        }
    }

    private String decodeFromBitmap(Bitmap bitmap) {
        int[] intArray = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(intArray, 0, bitmap.getWidth(), 0, 0,
                bitmap.getWidth(), bitmap.getHeight());
        LuminanceSource source = new RGBLuminanceSource(bitmap.getWidth(),
                bitmap.getHeight(), intArray);
        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
        try {
            Result result = new MultiFormatReader().decode(binaryBitmap);
            return result.getText();
        } catch (NotFoundException e) {
            return null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();
        isScanProcessed = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        barcodeView.resume();
        resetScanner();
    }
}