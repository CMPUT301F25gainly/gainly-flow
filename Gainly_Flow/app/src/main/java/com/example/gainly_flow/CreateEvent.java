package com.example.gainly_flow;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.net.Uri;

public class CreateEvent extends AppCompatActivity {

    // UI Components
    private ImageView posterPreview, uploadIcon;
    private EditText eventNameInput, eventDescriptionInput, eventStartDateInput,
            scheduleDetailsInput, capacityInput, locationInput, priceInput, waitingListInput;
    private EditText registrationOpenInput, registrationCloseInput;
    private EditText tagsInput;
    private CheckBox geolocationCheckbox;
    private AutoCompleteTextView categoryDropdown;
    private Button saveEventButton, cancelButton;
    private MaterialCardView uploadCard;
    private TextView uploadText, uploadSubtext;

    // Firebase
    private FirebaseFirestore db;

    // Current user/organizer
    private String currentOrganizerId;

    // Image Upload
    private Uri selectedImageUri;
    private ImageManager imageManager;
    private ActivityResultLauncher<String> imagePickerLauncher;

    // Formats
    private final SimpleDateFormat dateFmt;
    private final SimpleDateFormat firestoreDateFormat;

    private boolean isUpdatePosterOnly = false;
    private String existingEventId = null;

    {
        dateFmt = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
        dateFmt.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));

        firestoreDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        firestoreDateFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        androidx.activity.EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_event);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Get current organizer ID (you'll need to set this from your login system)
        currentOrganizerId = getCurrentOrganizerId();

        imageManager = new ImageManager();
        setupImagePicker();
        wireToolbar();

        bindViews();

        // NEW: check if we are in update-poster-only mode
        Intent intent = getIntent();
        if (intent != null) {
            String mode = intent.getStringExtra("mode");
            existingEventId = intent.getStringExtra("event_id");
            isUpdatePosterOnly = "update_poster".equals(mode) && existingEventId != null;
        }

        if (isUpdatePosterOnly) {
            setupUpdatePosterOnlyMode();
        }

        applyEdgeToEdgeInsets();
        setupCategoryDropdown();
        configureTapOnlyInputs();
        wirePickers();
        wireButtons();
        wireUploadArea();
        setupFormValidation();

        applyEdgeToEdgeInsets();
        setupCategoryDropdown();
        configureTapOnlyInputs();
        wirePickers();
        wireButtons();
        wireUploadArea();
        setupFormValidation();
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        posterPreview.setImageURI(uri);
                        posterPreview.setVisibility(View.VISIBLE);
                        uploadIcon.setVisibility(View.GONE);
                        uploadText.setVisibility(View.GONE);
                        uploadSubtext.setVisibility(View.GONE);
                    }
                });
    }

    private void wireToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }
    }

    private String getCurrentOrganizerId() {
        return android.provider.Settings.Secure.getString(getContentResolver(),
                android.provider.Settings.Secure.ANDROID_ID);
    }

    // ... (rest of the file)

    private void updateOrganizerEvents(String eventId) {
        if (currentOrganizerId == null)
            return;

        // Use set with merge to create the document if it doesn't exist
        HashMap<String, Object> data = new HashMap<>();
        data.put("createdEvents", com.google.firebase.firestore.FieldValue.arrayUnion(eventId));

        db.collection("profiles")
                .document(currentOrganizerId)
                .set(data, SetOptions.merge())
                .addOnFailureListener(e -> {
                    Log.e("CreateEvent", "Failed to update organizer events: " + e.getMessage());
                });
    }

    private void bindViews() {
        // Basic Information
        eventNameInput = findViewById(R.id.eventNameInput);
        eventDescriptionInput = findViewById(R.id.eventDescriptionInput);
        locationInput = findViewById(R.id.eventLocationInput);
        priceInput = findViewById(R.id.eventPriceInput);
        tagsInput = findViewById(R.id.eventTagsInput);

        // Event Schedule
        eventStartDateInput = findViewById(R.id.eventStartDateInput);
        scheduleDetailsInput = findViewById(R.id.scheduleDetailsInput);

        // Registration Period
        registrationOpenInput = findViewById(R.id.registrationOpenInput);
        registrationCloseInput = findViewById(R.id.registrationCloseInput);

        // Capacity & Lottery Settings
        capacityInput = findViewById(R.id.eventCapacityInput);
        waitingListInput = findViewById(R.id.waitingListInput);
        geolocationCheckbox = findViewById(R.id.geolocationCheckbox);

        // Category
        categoryDropdown = findViewById(R.id.categoryDropdown);

        // Event Poster
        uploadCard = findViewById(R.id.uploadCard);
        uploadIcon = findViewById(R.id.uploadIcon);
        uploadText = findViewById(R.id.uploadText);
        uploadSubtext = findViewById(R.id.uploadSubtext);
        posterPreview = findViewById(R.id.posterPreview);

        // Buttons
        saveEventButton = findViewById(R.id.saveEventButton);
        cancelButton = findViewById(R.id.cancelButton);
    }

    private void setupUpdatePosterOnlyMode() {
        // Change button text so it’s clear
        saveEventButton.setText("Update Poster");

        // Disable all non-poster inputs
        eventNameInput.setEnabled(false);
        eventDescriptionInput.setEnabled(false);
        eventStartDateInput.setEnabled(false);
        scheduleDetailsInput.setEnabled(false);
        capacityInput.setEnabled(false);
        locationInput.setEnabled(false);
        priceInput.setEnabled(false);
        waitingListInput.setEnabled(false);
        registrationOpenInput.setEnabled(false);
        registrationCloseInput.setEnabled(false);
        geolocationCheckbox.setEnabled(false);
        categoryDropdown.setEnabled(false);
        tagsInput.setEnabled(false);

        // The upload card & posterPreview stay enabled so user can pick a new image
    }

    private void setupCategoryDropdown() {
        String[] categories = new String[] {
                "All", "Sport", "Music", "Art", "Education"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                categories);
        categoryDropdown.setAdapter(adapter);
        categoryDropdown.setText("All", false); // Default value
    }

    private void applyEdgeToEdgeInsets() {
        View root = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    private void configureTapOnlyInputs() {
        makeTapOnly(eventStartDateInput);
        makeTapOnly(registrationOpenInput);
        makeTapOnly(registrationCloseInput);
        makeTapOnly(categoryDropdown);
    }

    private void setupFormValidation() {
        // Add text watchers for real-time validation
        TextWatcher validationWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                validateForm();
            }
        };

        eventNameInput.addTextChangedListener(validationWatcher);
        eventStartDateInput.addTextChangedListener(validationWatcher);
        capacityInput.addTextChangedListener(validationWatcher);
    }

    private void validateForm() {
        boolean isValid = !text(eventNameInput).isEmpty() &&
                !text(eventStartDateInput).isEmpty() &&
                !text(capacityInput).isEmpty();

        // Validate capacity
        if (!text(capacityInput).isEmpty()) {
            try {
                int capacity = Integer.parseInt(text(capacityInput));
                if (capacity <= 0) {
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                isValid = false;
            }
        }

        saveEventButton.setEnabled(isValid);
        saveEventButton.setAlpha(isValid ? 1.0f : 0.5f);
    }

    private void wirePickers() {
        // Event dates
        eventStartDateInput.setOnClickListener(v -> showDatePicker("Select event start date",
                selectedUtc -> eventStartDateInput.setText(utcMillisToLocalDateString(selectedUtc))));

        // Registration dates
        registrationOpenInput.setOnClickListener(v -> showDatePicker("Registration opens",
                selectedUtc -> registrationOpenInput.setText(utcMillisToLocalDateString(selectedUtc))));

        registrationCloseInput.setOnClickListener(v -> showDatePicker("Registration closes",
                selectedUtc -> registrationCloseInput.setText(utcMillisToLocalDateString(selectedUtc))));
    }

    private void wireButtons() {
        saveEventButton.setOnClickListener(v -> saveEvent());
        cancelButton.setOnClickListener(v -> finish());
    }

    private void wireUploadArea() {
        uploadCard.setOnClickListener(v -> {
            imagePickerLauncher.launch("image/*");
        });
    }

    private void saveEvent() {
        // If we are only updating the poster for an existing event, follow a simpler
        // path
        if (isUpdatePosterOnly) {
            updatePosterForExistingEvent();
            return;
        }
        // Collect and validate input data
        final String name = text(eventNameInput);
        final String desc = text(eventDescriptionInput);
        final String startDateStr = text(eventStartDateInput);
        final String scheduleDetails = text(scheduleDetailsInput);
        final String location = text(locationInput);
        final String priceStr = text(priceInput);
        final String capStr = text(capacityInput);
        final String waitingListStr = text(waitingListInput);
        final String regOpenStr = text(registrationOpenInput);
        final String regCloseStr = text(registrationCloseInput);
        final String category = text(categoryDropdown);
        final String tagsStr = text(tagsInput);
        final boolean geoEnabled = geolocationCheckbox.isChecked();

        // Validation
        if (!require(name, eventNameInput))
            return;
        if (!require(startDateStr, eventStartDateInput))
            return;
        if (!require(capStr, capacityInput))
            return;

        // Parse dates
        final Date eventStartDate = parseDate(startDateStr);
        final Date regOpen = regOpenStr.isEmpty() ? null : parseDate(regOpenStr);
        final Date regClose = regCloseStr.isEmpty() ? null : parseDate(regCloseStr);

        if (eventStartDate == null) {
            eventStartDateInput.setError("Invalid date format");
            return;
        }

        // Validate registration dates if provided
        if (regOpen != null && regClose != null && regClose.before(regOpen)) {
            registrationCloseInput.setError("Registration close must be after open date");
            return;
        }

        if (eventStartDate.before(new Date())) {
            eventStartDateInput.setError("Event date cannot be in the past");
            return;
        }

        // Parse numeric values
        int capacity = 20; // Default from XML
        try {
            capacity = Integer.parseInt(capStr);
            if (capacity <= 0) {
                capacityInput.setError("Capacity must be positive");
                return;
            }
        } catch (NumberFormatException e) {
            capacityInput.setError("Invalid capacity");
            return;
        }

        // Parse waiting list limit
        Integer waitingListLimit = null;
        try {
            if (!waitingListStr.isEmpty() && !waitingListStr.equalsIgnoreCase("Unlimited")) {
                waitingListLimit = Integer.parseInt(waitingListStr);
                if (waitingListLimit < 0) {
                    waitingListInput.setError("Waiting list limit cannot be negative");
                    return;
                }
            }
        } catch (NumberFormatException e) {
            waitingListInput.setError("Invalid waiting list limit");
            return;
        }

        double price = 0.0;
        try {
            if (!priceStr.isEmpty()) {
                price = Double.parseDouble(priceStr);
                if (price < 0) {
                    priceInput.setError("Price cannot be negative");
                    return;
                }
            }
        } catch (NumberFormatException e) {
            priceInput.setError("Invalid price");
            return;
        }

        // Parse category
        Event.Category eventCategory;
        try {
            eventCategory = Event.Category.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            eventCategory = Event.Category.ALL;
        }

        // Create event object
        Event event = new Event();
        event.setId(UUID.randomUUID().toString());
        event.setName(name);
        event.setDescription(desc);
        event.setEventDate(eventStartDate);
        event.setTimeString(scheduleDetails.isEmpty() ? "Time not specified" : scheduleDetails);
        event.setLocation(location);
        event.setPrice(price);
        event.setCapacity(capacity);
        event.setCurrentParticipants(0);
        event.setGeolocationRequired(geoEnabled);
        event.setCategory(eventCategory);
        event.setActive(true);
        event.setOrganizerId(currentOrganizerId); // Set the organizer ID
        event.setTags(parseTags(tagsStr));
        event.setWaitingListLimit(waitingListLimit);

        if (regOpen != null && regClose != null) {
            event.setRegistrationOpen(regOpen);
            event.setRegistrationClose(regClose);
        }

        // Save to Firestore
        if (selectedImageUri != null) {
            // Upload image first
            Toast.makeText(this, "Uploading poster...", Toast.LENGTH_SHORT).show();
            saveEventButton.setEnabled(false); // Prevent double submission

            imageManager.uploadPoster(selectedImageUri,
                    imageId -> {
                        event.setPosterImageId(imageId);
                        saveEventToFirestore(event);
                    },
                    e -> {
                        saveEventButton.setEnabled(true);
                        Toast.makeText(this, "Failed to upload poster: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            saveEventToFirestore(event);
        }
    }

    private void updatePosterForExistingEvent() {
        if (existingEventId == null) {
            Toast.makeText(this, "Error: missing event ID", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedImageUri == null) {
            Toast.makeText(this, "Please choose a new poster image", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Uploading new poster...", Toast.LENGTH_SHORT).show();
        saveEventButton.setEnabled(false);

        imageManager.uploadPoster(selectedImageUri,
                imageId -> {
                    // Only update the poster field on the existing event
                    HashMap<String, Object> updates = new HashMap<>();
                    updates.put("posterImageId", imageId);
                    updates.put("updatedAt", new Date());
                    updates.put("updatedAtTimestamp", System.currentTimeMillis());

                    db.collection("events")
                            .document(existingEventId)
                            .update(updates)
                            .addOnSuccessListener(unused -> {
                                saveEventButton.setEnabled(true);
                                Toast.makeText(CreateEvent.this,
                                        "Poster updated successfully",
                                        Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);
                                finish(); // go back to OrganizerEventActivity
                            })
                            .addOnFailureListener(e -> {
                                saveEventButton.setEnabled(true);
                                Toast.makeText(CreateEvent.this,
                                        "Failed to update poster: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                },
                e -> {
                    saveEventButton.setEnabled(true);
                    Toast.makeText(this,
                            "Failed to upload poster: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void saveEventToFirestore(Event event) {
        // Convert event to map for Firestore
        HashMap<String, Object> data = new HashMap<>();

        // Basic fields
        data.put("id", event.getId());
        data.put("name", event.getName());
        data.put("description", event.getDescription());
        data.put("eventDate", event.getEventDate());
        data.put("timeString", event.getTimeString());
        data.put("location", event.getLocation());
        data.put("price", event.getPrice());
        data.put("capacity", event.getCapacity());
        data.put("currentParticipants", event.getCurrentParticipants());
        data.put("geolocationRequired", event.isGeolocationRequired());
        data.put("category", event.getCategory().name());
        data.put("isActive", event.isActive());
        data.put("organizerId", event.getOrganizerId());
        data.put("tags", event.getTags());
        if (event.getWaitingListLimit() != null) {
            data.put("waitingListLimit", event.getWaitingListLimit());
        }

        // Save poster image ID if present
        if (event.getPosterImageId() != null && !event.getPosterImageId().isEmpty()) {
            data.put("posterImageId", event.getPosterImageId());
        }

        // Store dates as both Date objects and timestamps for flexibility
        if (event.getEventDate() != null) {
            data.put("eventDate", event.getEventDate());
            data.put("eventDateTimestamp", event.getEventDate().getTime());
        }

        if (event.getRegistrationOpen() != null) {
            data.put("registrationOpen", event.getRegistrationOpen());
            data.put("registrationOpenTimestamp", event.getRegistrationOpen().getTime());
        }

        if (event.getRegistrationClose() != null) {
            data.put("registrationClose", event.getRegistrationClose());
            data.put("registrationCloseTimestamp", event.getRegistrationClose().getTime());
        }

        // Initialize empty lists for entrant management
        data.put("waitingList", new ArrayList<String>());
        data.put("selected", new ArrayList<String>());
        data.put("cancelled", new ArrayList<String>());
        data.put("enrolled", new ArrayList<String>());

        // Add created timestamp
        data.put("createdAt", new Date());
        data.put("createdAtTimestamp", System.currentTimeMillis());

        db.collection("events")
                .document(event.getId())
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Log.d("CreateEvent", "Event saved successfully: " + event.getId());

                    // Generate and store QR URL
                    String qrUrl = QRUrlCreator.buildDeepLink(event.getId());
                    updateQrUrlInFirestore(event.getId(), qrUrl);

                    // Update organizer's created events list
                    updateOrganizerEvents(event.getId());

                    showQrDialog(qrUrl, () -> {
                        Toast.makeText(CreateEvent.this, "Event created successfully!", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("CreateEvent", "Failed to create event: " + e.getMessage());
                    Toast.makeText(CreateEvent.this, "Failed to create event: " + e.getMessage(), Toast.LENGTH_LONG)
                            .show();
                });
    }

    private void updateQrUrlInFirestore(String eventId, String qrUrl) {
        HashMap<String, Object> updateData = new HashMap<>();
        updateData.put("qrUrl", qrUrl);

        db.collection("events")
                .document(eventId)
                .update(updateData)
                .addOnFailureListener(e -> {
                    Log.e("CreateEvent", "Failed to update QR URL: " + e.getMessage());
                });
    }

    // Date parsing methods
    @Nullable
    private Date parseDate(String dateStr) {
        try {
            return dateFmt.parse(dateStr);
        } catch (Exception e) {
            Log.e("CreateEvent", "Error parsing date: " + dateStr, e);
            return null;
        }
    }

    // Picker interfaces and methods
    private interface DatePicked {
        void onPicked(long utcMidnightMillis);
    }

    private void showDatePicker(String title, DatePicked callback) {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(title)
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();
        picker.addOnPositiveButtonClickListener(callback::onPicked);
        picker.show(getSupportFragmentManager(), "mdp:" + title);
    }

    // Utility methods
    private static String text(EditText et) {
        return (et == null) ? "" : et.getText().toString().trim();
    }

    private static String text(AutoCompleteTextView actv) {
        return (actv == null) ? "" : actv.getText().toString().trim();
    }

    private static void makeTapOnly(EditText et) {
        et.setFocusable(false);
        et.setFocusableInTouchMode(false);
        et.setClickable(true);
        et.setLongClickable(false);
    }

    private static void makeTapOnly(AutoCompleteTextView actv) {
        actv.setFocusable(false);
        actv.setFocusableInTouchMode(false);
        actv.setClickable(true);
        actv.setLongClickable(false);
    }

    private static ArrayList<String> parseTags(String tagsStr) {
        ArrayList<String> tags = new ArrayList<>();
        if (tagsStr == null || tagsStr.trim().isEmpty())
            return tags;

        String[] parts = tagsStr.split(",");
        for (String p : parts) {
            String tag = p.trim();
            if (!tag.isEmpty()) {
                tags.add(tag);
            }
        }
        return tags;
    }

    private static boolean require(String value, EditText field) {
        if (!value.isEmpty())
            return true;
        field.setError("Required");
        field.requestFocus();
        return false;
    }

    private String utcMillisToLocalDateString(long utcMidnight) {
        Calendar local = Calendar.getInstance();
        local.setTimeInMillis(utcMidnight);
        return dateFmt.format(local.getTime());
    }

    // QR Dialog methods
    private void showQrDialog(String qrUrl, @Nullable Runnable onClose) {
        // Generate QR code bitmap (you'll need to implement QRImage.bitmapFromUrl)
        Bitmap bmp = generateQRCode(qrUrl, 512);
        showQrDialog(qrUrl, bmp, onClose);
    }

    private Bitmap generateQRCode(String qrUrl, int size) {
        // TODO: Implement QR code generation
        // This is a placeholder - you'll need to use a QR code library like ZXing
        // Toast.makeText(this, "QR generation to be implemented",
        // Toast.LENGTH_SHORT).show();
        return null;
    }

    private void showQrDialog(String qrUrl, @Nullable Bitmap bmp, @Nullable Runnable onClose) {
        if (bmp == null) {
            // Toast.makeText(this, "Couldn't generate QR", Toast.LENGTH_SHORT).show();
            if (onClose != null)
                onClose.run();
            return;
        }

        View view = getLayoutInflater().inflate(R.layout.dialog_qr, null);
        ImageView qr = view.findViewById(R.id.qrImage);
        qr.setImageBitmap(bmp);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Event QR Code")
                .setView(view)
                .setPositiveButton("Done", (d, w) -> {
                    d.dismiss();
                    if (onClose != null)
                        onClose.run();
                })
                .setNeutralButton("Share", (d, w) -> shareQrPng(qrUrl))
                .show();
    }

    private void shareQrPng(String qrUrl) {
        // TODO: Implement QR code sharing
        Toast.makeText(this, "Share QR functionality to be implemented", Toast.LENGTH_SHORT).show();
    }
}
