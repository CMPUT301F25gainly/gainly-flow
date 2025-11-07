package com.example.gainly_flow;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
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

        // Extract event ID from QR code content using direct parsing
        String eventId = extractEventIdFromQR(scannedText);

        if (eventId != null && !eventId.isEmpty()) {
            Log.d("QRCodeScanner", "Extracted event ID: " + eventId);
            // Fetch event details from Firebase
            fetchEventFromFirebase(eventId);
        } else {
            // Handle invalid QR code
            tvSubtitle.setText("❌ Invalid event QR code");
            chipCameraStatus.setText("Scan failed");
            resetScannerAfterDelay();
        }
    }

    private String extractEventIdFromQR(String scannedText) {
        if (scannedText == null || scannedText.trim().isEmpty()) {
            return null;
        }

        Log.d("QRCodeScanner", "Parsing QR content: " + scannedText);

        // Direct parsing - no dependency on QRUrlDecoder
        // Check for gainlyflow://event/{id} pattern
        if (scannedText.startsWith("gainlyflow://event/")) {
            String eventId = scannedText.substring("gainlyflow://event/".length());
            Log.d("QRCodeScanner", "Found gainlyflow pattern, eventId: " + eventId);
            return eventId;
        }
        // Check for https://gainlyflow.app/e/{id} pattern
        else if (scannedText.startsWith("https://gainlyflow.app/e/")) {
            String eventId = scannedText.substring("https://gainlyflow.app/e/".length());
            Log.d("QRCodeScanner", "Found https pattern, eventId: " + eventId);
            return eventId;
        }
        // Check for legacy event:{id} pattern
        else if (scannedText.startsWith("event:")) {
            String eventId = scannedText.substring("event:".length());
            Log.d("QRCodeScanner", "Found event: pattern, eventId: " + eventId);
            return eventId;
        }
        // If it's just a plain event ID (Firestore document ID pattern)
        else if (scannedText.matches("[a-zA-Z0-9]{1,}")) {
            Log.d("QRCodeScanner", "Found plain event ID: " + scannedText);
            return scannedText;
        }

        Log.d("QRCodeScanner", "No valid event ID pattern found");
        return null;
    }

    private void fetchEventFromFirebase(String eventId) {
        Log.d("QRCodeScanner", "Fetching event with ID: " + eventId);

        db.collection("events")
                .document(eventId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            Log.d("QRCodeScanner", "Document exists, converting to event...");
                            // Convert Firestore document to your Event object
                            Event event = documentToEvent(document);
                            if (event != null) {
                                Log.d("QRCodeScanner", "Event created successfully: " + event.getName());
                                navigateToEventDetail(event);
                            } else {
                                Log.e("QRCodeScanner", "Failed to convert document to event");
                                showError("Failed to parse event data");
                            }
                        } else {
                            Log.e("QRCodeScanner", "Document does not exist for ID: " + eventId);
                            showError("Event not found or has been removed");
                        }
                    } else {
                        Log.e("QRCodeScanner", "Firestore error: " + task.getException());
                        showError("Error fetching event: " + task.getException().getMessage());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("QRCodeScanner", "Firestore failure: " + e.getMessage());
                    showError("Error fetching event: " + e.getMessage());
                });
    }

    private Event documentToEvent(DocumentSnapshot document) {
        try {
            Event event = new Event(document.getId());

            Log.d("QRCodeScanner", "Document fields: " + document.getData());

            // Set basic event info - using your actual Firebase field names
            if (document.contains("name")) {
                event.setName(document.getString("name"));
                Log.d("QRCodeScanner", "Set name: " + document.getString("name"));
            }
            if (document.contains("description")) {
                event.setDescription(document.getString("description"));
                Log.d("QRCodeScanner", "Set description");
            }
            if (document.contains("eventDate")) {
                Date eventDate = document.getDate("eventDate");
                event.setEventDate(eventDate);
                Log.d("QRCodeScanner", "Set event date: " + eventDate);
            }
            if (document.contains("eventTimeUtc")) {
                // Handle UTC time
                String timeUtc = document.getString("eventTimeUtc");
                if (timeUtc != null) {
                    try {
                        Time eventTime = Time.valueOf(timeUtc);
                        event.setEventTime(eventTime);
                        Log.d("QRCodeScanner", "Set event time from UTC: " + timeUtc);
                    } catch (Exception e) {
                        Log.d("QRCodeScanner", "Could not parse UTC time: " + timeUtc);
                    }
                }
            }
            if (document.contains("eventTimeofDayMs")) {
                // Handle milliseconds time of day
                Long timeMs = document.getLong("eventTimeofDayMs");
                if (timeMs != null) {
                    Time eventTime = new Time(timeMs);
                    event.setEventTime(eventTime);
                    Log.d("QRCodeScanner", "Set event time from ms: " + timeMs);
                }
            }
            if (document.contains("registrationOpen")) {
                Date regOpen = document.getDate("registrationOpen");
                Date regClose = document.getDate("registrationClose");
                event.setRegistrationPeriod(regOpen, regClose);
                Log.d("QRCodeScanner", "Set registration period");
            }
            if (document.contains("capacity")) {
                Long capacity = document.getLong("capacity");
                if (capacity != null) {
                    event.setCapacity(capacity.intValue());
                    Log.d("QRCodeScanner", "Set capacity: " + capacity);
                }
            }
            if (document.contains("geolactionRequired")) { // Note: your field name has typo "geolaction"
                Boolean geoRequired = document.getBoolean("geolactionRequired");
                if (geoRequired != null) {
                    event.setGeolocationRequired(geoRequired);
                    Log.d("QRCodeScanner", "Set geolocation required: " + geoRequired);
                }
            }
            if (document.contains("posterUrl")) {
                event.setPosterImage(document.getString("posterUrl"));
                Log.d("QRCodeScanner", "Set poster URL");
            }
            if (document.contains("organizerId")) {
                event.setOrganizerId(document.getString("organizerId"));
                Log.d("QRCodeScanner", "Set organizer ID");
            }
            if (document.contains("qrUrl")) {
                event.setQrUrl(document.getString("qrUrl"));
                Log.d("QRCodeScanner", "Set QR URL");
            }

            Log.d("QRCodeScanner", "Event created: " + event.getName());
            return event;

        } catch (Exception e) {
            Log.e("QRCodeScanner", "Error converting document to event: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private void navigateToEventDetail(Event event) {
        if (event == null) {
            showError("Event data is null");
            return;
        }

        Log.d("QRCodeScanner", "Navigating to event detail: " + event.getName());

        Intent intent = new Intent(this, EventDetailActivity.class);

        // Pass event data as individual extras
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

        // Pass poster URL if available
        if (event.getPosterImageId() != null) {
            intent.putExtra("poster_url", event.getPosterImageId());
        }

        // Pass time information - handle without getEventTimeString method
        String eventTimeString = "Time not specified";
        if (event.getEventTime() != null) {
            eventTimeString = event.getEventTime().toString();
        }
        intent.putExtra("event_time_string", eventTimeString);

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        Toast.makeText(this, "Event found! Redirecting...", Toast.LENGTH_SHORT).show();
        startActivity(intent);
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Scan Error")
                    .setMessage(message)
                    .setPositiveButton("Try Again", (dialog, which) -> resetScanner())
                    .setCancelable(false)
                    .show();
        });
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