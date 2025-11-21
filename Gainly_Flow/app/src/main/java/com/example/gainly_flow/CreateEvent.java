package com.example.gainly_flow;

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
import java.util.ArrayList;
import java.util.List;


import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

public class CreateEvent extends AppCompatActivity {

    // UI Components
    private ImageView posterPreview, uploadIcon;
    private EditText eventNameInput, eventDescriptionInput, eventStartDateInput,
            scheduleDetailsInput, capacityInput, locationInput, priceInput, waitingListInput;
    private EditText registrationOpenInput, registrationCloseInput;
    private CheckBox geolocationCheckbox;
    private AutoCompleteTextView categoryDropdown;
    private Button saveEventButton, cancelButton;
    private MaterialCardView uploadCard;
    private TextView uploadText, uploadSubtext;

    // Formats
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
    private final SimpleDateFormat firestoreDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        androidx.activity.EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_event);

        bindViews();
        applyEdgeToEdgeInsets();
        setupCategoryDropdown();
        configureTapOnlyInputs();
        wirePickers();
        wireButtons();
        wireUploadArea();
        setupFormValidation();
    }

    private void bindViews() {
        // Basic Information
        eventNameInput = findViewById(R.id.eventNameInput);
        eventDescriptionInput = findViewById(R.id.eventDescriptionInput);
        locationInput = findViewById(R.id.eventLocationInput);
        priceInput = findViewById(R.id.eventPriceInput);

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

    private void setupCategoryDropdown() {
        String[] categories = new String[]{
                "All", "Sport", "Music", "Art", "Education"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                categories
        );
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
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                validateForm();
            }
        };

        eventNameInput.addTextChangedListener(validationWatcher);
        eventStartDateInput.addTextChangedListener(validationWatcher);
    }

    private void validateForm() {
        boolean isValid = !text(eventNameInput).isEmpty() &&
                !text(eventStartDateInput).isEmpty();

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
            // TODO: Implement image upload from device
            // For now, show a toast message
            Toast.makeText(this, "Image upload functionality to be implemented", Toast.LENGTH_SHORT).show();
        });
    }

    private void saveEvent() {
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
        final boolean geoEnabled = geolocationCheckbox.isChecked();

        // Validation
        if (!require(name, eventNameInput)) return;
        if (!require(startDateStr, eventStartDateInput)) return;

        // Parse dates
        final Date eventStartDate = parseDate(startDateStr);
        final Date regOpen = regOpenStr.isEmpty() ? null : parseDate(regOpenStr);
        final Date regClose = regCloseStr.isEmpty() ? null : parseDate(regCloseStr);

        if (eventStartDate == null) {
            eventStartDateInput.setError("Invalid date format");
            return;
        }


        // Parse numeric values
        int capacity = 20; // Default from XML
        try {
            if (!capStr.isEmpty()) {
                capacity = Integer.parseInt(capStr);
                if (capacity <= 0) {
                    capacityInput.setError("Capacity must be positive");
                    return;
                }
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
        event.setId(java.util.UUID.randomUUID().toString());
        event.setName(name);
        event.setDescription(desc);
        event.setEventDate(eventStartDate); // Using start date as main event date
        event.setTimeString(scheduleDetails.isEmpty() ? "Time not specified" : scheduleDetails);
        event.setLocation(location);
        event.setPrice(price);
        event.setCapacity(capacity);
        event.setCurrentParticipants(0);
        event.setGeolocationRequired(geoEnabled);
        event.setCategory(eventCategory);
        event.setActive(true);

        // Note: Poster image handling would be implemented separately
        // event.setPosterImageId(posterImageId);

        if (regOpen != null && regClose != null) {
            event.setRegistrationPeriod(regOpen, regClose);
        }

        // TODO: Set organizer from current user
        // event.setOrganizer(currentUser);

        // Initialize waiting list with limit if specified
        if (waitingListLimit != null) {
            // Note: You might need to add setWaitingListLimit method to WaitingList class
            // event.getWaitingList().setLimit(waitingListLimit);
        }

        // Save to Firestore
        saveEventToFirestore(event);
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
        data.put("posterImageId", event.getPosterImageId());

        // FIX: Save registration dates as UTC epoch milliseconds
        if (event.getRegistrationOpen() != null) {
            data.put("registrationOpenUtc", event.getRegistrationOpen().getTime());
        }
        if (event.getRegistrationClose() != null) {
            data.put("registrationCloseUtc", event.getRegistrationClose().getTime());
        }

        // Also save the event date as UTC milliseconds for consistency
        if (event.getEventDate() != null) {
            data.put("eventDateUtc", event.getEventDate().getTime());
        }

        // NEW: Store the missing fields
        if (event.getOrganizer() != null) {
            // Create organizer map for Firestore
            HashMap<String, Object> organizerMap = new HashMap<>();
            organizerMap.put("id", event.getOrganizer().getId());
            organizerMap.put("name", event.getOrganizer().getDisplayName());
            data.put("organizer", organizerMap);
        }

        if (event.getQrUrl() != null) {
            data.put("qrUrl", event.getQrUrl());
        }

        // Collections/lists - initialize empty if null
        data.put(
                "waitingList",
                event.getWaitingList() != null ? event.getWaitingList() : new ArrayList<Entrant>()
        );
        data.put("selected", event.getSelected() != null ? event.getSelected() : new ArrayList<Entrant>());
        data.put("cancelled", event.getCancelled() != null ? event.getCancelled() : new ArrayList<Entrant>());
        data.put("enrolled", event.getEnrolled() != null ? event.getEnrolled() : new ArrayList<Entrant>());

        FirebaseFirestore.getInstance()
                .collection("events")
                .document(event.getId())
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    // Generate and store QR URL
                    String qrUrl = QRUrlCreator.buildDeepLink(event.getId());
                    event.setQrUrl(qrUrl);

                    // Update Firestore with QR URL
                    updateQrUrlInFirestore(event.getId(), qrUrl);

                    showQrDialog(qrUrl, () -> {
                        Toast.makeText(CreateEvent.this, "Event created successfully!", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CreateEvent.this, "Failed to create event: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // Helper method to update QR URL separately
    private void updateQrUrlInFirestore(String eventId, String qrUrl) {
        HashMap<String, Object> updateData = new HashMap<>();
        updateData.put("qrUrl", qrUrl);

        FirebaseFirestore.getInstance()
                .collection("events")
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
            return null;
        }
    }

    // Picker interfaces and methods
    private interface DatePicked { void onPicked(long utcMidnightMillis); }

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

    private static boolean require(String value, EditText field) {
        if (!value.isEmpty()) return true;
        field.setError("Required");
        field.requestFocus();
        return false;
    }

    private String utcMillisToLocalDateString(long utcMidnight) {
        Calendar local = Calendar.getInstance();
        local.setTimeInMillis(utcMidnight);
        return dateFmt.format(local.getTime());
    }

    // QR Dialog methods (keep from your original code)
    private void showQrDialog(String qrUrl, @Nullable Runnable onClose) {
        Bitmap bmp = QRImage.bitmapFromUrl(qrUrl, 512);
        showQrDialog(qrUrl, bmp, onClose);
    }

    private void showQrDialog(String qrUrl, @Nullable Bitmap bmp, @Nullable Runnable onClose) {
        if (bmp == null) {
            Toast.makeText(this, "Couldn't generate QR", Toast.LENGTH_SHORT).show();
            if (onClose != null) onClose.run();
            return;
        }
        View view = getLayoutInflater().inflate(R.layout.dialog_qr, null);
        ImageView qr = view.findViewById(R.id.qrImage);
        qr.setImageBitmap(bmp);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Event QR Code")
                .setView(view)
                .setPositiveButton("Done", (d, w) -> {
                    d.dismiss();
                    if (onClose != null) onClose.run();
                })
                .setNeutralButton("Share", (d, w) -> shareQrPng(qrUrl))
                .show();
    }

    private void shareQrPng(String qrUrl) {
        // Implementation from your original code
        // This would share the QR code image
        Toast.makeText(this, "Share QR functionality to be implemented", Toast.LENGTH_SHORT).show();
    }
}