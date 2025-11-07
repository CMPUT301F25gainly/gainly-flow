package com.example.gainly_flow;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.android.gms.tasks.Task;


import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

public class CreateEvent extends AppCompatActivity {

    // ---------------------------------------
    // UI
    // ---------------------------------------
    private ImageView posterPreview;
    private ImageView qrPreview; // reserved if you want inline QR preview in the future
    private EditText eventNameInput, eventDescriptionInput, eventDateInput, eventTimeInput, capacityInput;
    private EditText registrationOpenInput, registrationCloseInput;
    private CheckBox geolocationCheckbox;

    // Image selection
    @Nullable private Uri posterUri = null;

    // ---------------------------------------
    // Formats (Activity scope – SimpleDateFormat not thread-safe)
    // ---------------------------------------
    private final SimpleDateFormat dateFmt   = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat timeFmt12 = new SimpleDateFormat("h:mm a",     Locale.getDefault());

    // ---------------------------------------
    // Document picker
    // ---------------------------------------
    private final ActivityResultLauncher<String[]> openPosterLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;
                try {
                    // persist read permission (some providers don't support it—ignore on failure)
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException ignored) {}
                posterUri = uri;
                posterPreview.setImageURI(uri);
            });

    // ---------------------------------------
    // Lifecycle
    // ---------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        androidx.activity.EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_event);

        bindViews();
        applyEdgeToEdgeInsets();
        configureTapOnlyInputs();
        wirePickers();
        wireButtons();

        restorePoster(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (posterUri != null) outState.putString("posterUri", posterUri.toString());
    }

    // ---------------------------------------
    // Wiring
    // ---------------------------------------
    private void bindViews() {
        posterPreview           = findViewById(R.id.posterPreview);
        eventNameInput          = findViewById(R.id.eventNameInput);
        eventDescriptionInput   = findViewById(R.id.eventDescriptionInput);
        eventDateInput          = findViewById(R.id.eventDateInput);
        eventTimeInput          = findViewById(R.id.eventTimeInput);
        capacityInput           = findViewById(R.id.eventCapacityInput);
        geolocationCheckbox     = findViewById(R.id.geolocationCheckbox);
        registrationOpenInput   = findViewById(R.id.registrationOpenInput);
        registrationCloseInput  = findViewById(R.id.registrationCloseInput);
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
        makeTapOnly(eventDateInput);
        makeTapOnly(eventTimeInput);
        makeTapOnly(registrationOpenInput);
        makeTapOnly(registrationCloseInput);
    }

    private void wirePickers() {
        eventDateInput.setOnClickListener(v -> showDatePicker("Select event date", selectedUtc ->
                eventDateInput.setText(utcMillisToLocalDateString(selectedUtc))));

        registrationOpenInput.setOnClickListener(v -> showDatePicker("Select registration open date", selectedUtc ->
                registrationOpenInput.setText(utcMillisToLocalDateString(selectedUtc))));

        registrationCloseInput.setOnClickListener(v -> showDatePicker("Select registration close date", selectedUtc ->
                registrationCloseInput.setText(utcMillisToLocalDateString(selectedUtc))));

        eventTimeInput.setOnClickListener(v -> showTimePicker("Select event time", (hour24, minute) ->
                eventTimeInput.setText(formatTime(hour24, minute))));
    }

    private void wireButtons() {
        View.OnClickListener pickPoster = v -> openPosterLauncher.launch(new String[]{"image/*"});
        findViewById(R.id.selectPosterBtn).setOnClickListener(pickPoster);
        posterPreview.setOnClickListener(pickPoster);

        findViewById(R.id.saveEventButton).setOnClickListener(v -> saveEvent());
    }

    private void restorePoster(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) return;
        String saved = savedInstanceState.getString("posterUri");
        if (saved != null) {
            posterUri = Uri.parse(saved);
            posterPreview.setImageURI(posterUri);
        }
    }

    // ---------------------------------------
    // Save flow
    // ---------------------------------------
    private void saveEvent() {
        // 1) Read inputs (same style you had)
        final String name        = text(eventNameInput);
        final String desc        = text(eventDescriptionInput);
        final String dateStr     = text(eventDateInput);          // yyyy-MM-dd
        final String timeStr     = text(eventTimeInput);          // h:mm AM/PM
        final String regOpenStr  = text(registrationOpenInput);   // yyyy-MM-dd
        final String regCloseStr = text(registrationCloseInput);  // yyyy-MM-dd
        final String capStr      = text(capacityInput);
        final boolean geoEnabled = geolocationCheckbox != null && geolocationCheckbox.isChecked();

        if (!require(name, eventNameInput)) return;
        if (!require(dateStr, eventDateInput)) return;
        if (!require(timeStr, eventTimeInput)) return;
        if (!require(capStr, capacityInput)) return;

        final Long eventDateMillis      = parseDayUtc(dateStr);
        final Long eventTimeOfDayMillis = parseTimeMsFromMidnight(timeStr);
        final Long regOpenMillis        = regOpenStr.isEmpty()  ? null : parseDayUtc(regOpenStr);
        final Long regCloseMillis       = regCloseStr.isEmpty() ? null : parseDayUtc(regCloseStr);

        if (eventDateMillis == null)  { eventDateInput.setError("Invalid date"); return; }
        if (eventTimeOfDayMillis == null) { eventTimeInput.setError("Invalid time"); return; }

        // 2) Build ID, QR URL
        final String id    = java.util.UUID.randomUUID().toString();
        final String qrUrl = QRUrlCreator.buildDeepLink(id);

        // 3) Build event map ready for Firestore
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("id", id);
        data.put("name", name);
        data.put("description", desc);
        data.put("eventDateUtc", String.valueOf(eventDateMillis));       // keep your storage style
        data.put("eventTimeOfDayMs", String.valueOf(eventTimeOfDayMillis));
        data.put("registrationOpenUtc", regOpenMillis == null ? "" : String.valueOf(regOpenMillis));
        data.put("registrationCloseUtc", regCloseMillis == null ? "" : String.valueOf(regCloseMillis));
        data.put("capacity", capStr);
        data.put("geolocationEnabled", String.valueOf(geoEnabled));
        data.put("qrUrl", qrUrl);
        data.put("createdAt", System.currentTimeMillis());

        // 4) If there’s a poster, upload to Storage first, then save Firestore with posterUrl
        if (posterUri != null) {
            Toast.makeText(this, "Uploading poster…", Toast.LENGTH_SHORT).show();
            uploadPosterAndGetUrl(id, posterUri)
                    .addOnSuccessListener(posterUrl -> {
                        data.put("posterUrl", posterUrl);
                        saveEventToFirestore(id, data, qrUrl);
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(CreateEvent.this, "Poster upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
        } else {
            data.put("posterUrl", "");
            saveEventToFirestore(id, data, qrUrl);
        }
    }

    private Task<String> uploadPosterAndGetUrl(String id, Uri uri) {
        // (A) Make sure the bucket is correct. If in doubt, hardcode your bucket:
        // StorageReference root = FirebaseStorage.getInstance("gs://<your-bucket>.appspot.com").getReference();
        StorageReference root = FirebaseStorage.getInstance().getReference();

        // (B) Make sure the path has NO leading slash
        StorageReference ref = root.child("events").child(id).child("poster.jpg");

        Log.d("CreateEvent", "Uploading to: " + ref.getPath() + "  bucket=" + ref.getBucket() + "  uri=" + uri);

        return ref.putFile(uri)
                .addOnSuccessListener(taskSnapshot -> Log.d("CreateEvent", "putFile SUCCESS bytes=" + taskSnapshot.getTotalByteCount()))
                .addOnFailureListener(e -> Log.e("CreateEvent", "putFile FAILED: " + e.getMessage(), e))
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(uri1 -> Log.d("CreateEvent", "getDownloadUrl SUCCESS: " + uri1))
                .addOnFailureListener(e -> Log.e("CreateEvent", "getDownloadUrl FAILED: " + e.getMessage(), e))
                .continueWith(t -> t.getResult().toString());
    }
    private void saveEventToFirestore(String id, java.util.Map<String, Object> data, String qrUrl) {
        FirebaseFirestore.getInstance()
                .collection("events")
                .document(id)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    // Show QR dialog, then finish
                    showQrDialog(qrUrl, () -> {
                        Toast.makeText(CreateEvent.this, "Event saved", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(CreateEvent.this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    // ---------------------------------------
    // Pickers
    // ---------------------------------------
    private interface DatePicked { void onPicked(long utcMidnightMillis); }
    private interface TimePicked { void onPicked(int hour24, int minute); }

    private void showDatePicker(String title, DatePicked callback) {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(title)
                .build();
        picker.addOnPositiveButtonClickListener(callback::onPicked);
        picker.show(getSupportFragmentManager(), "mdp:" + title);
    }

    private void showTimePicker(String title, TimePicked callback) {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(9)
                .setMinute(0)
                .setTitleText(title)
                .build();
        picker.addOnPositiveButtonClickListener(v -> callback.onPicked(picker.getHour(), picker.getMinute()));
        picker.show(getSupportFragmentManager(), "mtp:" + title);
    }

    // ---------------------------------------
    // QR dialog + share
    // ---------------------------------------

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

    // has not been implemented fully yet, will be ready for project part 4
    private void shareQrPng(String qrUrl) {
        byte[] png = QRImage.pngFromUrl(qrUrl, 1024);
        if (png == null) {
            Toast.makeText(this, "Share failed", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            File out = new File(getCacheDir(), "event_qr.png");
            try (FileOutputStream fos = new FileOutputStream(out)) { fos.write(png); }
            Uri uri = androidx.core.content.FileProvider.getUriForFile(
                    this, getPackageName() + ".fileprovider", out);

            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("image/png");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, "Share QR"));
        } catch (Exception e) {
            Toast.makeText(this, "Share failed", Toast.LENGTH_SHORT).show();
        }
    }

    // ---------------------------------------
    // Parsing / formatting
    // ---------------------------------------
    @Nullable
    private Long parseDayUtc(String ymd) {
        try {
            dateFmt.setLenient(false);
            // Interpret as local calendar day, then convert that Y-M-D to UTC midnight
            Calendar local = Calendar.getInstance();
            local.setTime(dateFmt.parse(ymd));
            local.set(Calendar.HOUR_OF_DAY, 0);
            local.set(Calendar.MINUTE, 0);
            local.set(Calendar.SECOND, 0);
            local.set(Calendar.MILLISECOND, 0);

            Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            utc.clear();
            utc.set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH));
            return utc.getTimeInMillis();
        } catch (Exception e) {
            return null;
        }
    }

    /** Parse "h:mm a" into milliseconds from midnight (0..86,399,999). */
    @Nullable
    private Long parseTimeMsFromMidnight(String time12) {
        try {
            timeFmt12.setLenient(false);
            Calendar c = Calendar.getInstance();
            c.setTime(timeFmt12.parse(time12));
            int h = c.get(Calendar.HOUR_OF_DAY);
            int m = c.get(Calendar.MINUTE);
            int s = c.get(Calendar.SECOND);
            return (long) ((h * 3600 + m * 60 + s) * 1000);
        } catch (Exception e) {
            return null;
        }
    }

    /** Convert MaterialDatePicker UTC midnight millis → local yyyy-MM-dd. */
    private String utcMillisToLocalDateString(long utcMidnight) {
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.setTimeInMillis(utcMidnight);

        Calendar local = Calendar.getInstance();
        local.clear();
        local.set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH));
        return dateFmt.format(local.getTime());
    }

    private String formatTime(int hour24, int minute) {
        int hr12 = (hour24 % 12 == 0) ? 12 : (hour24 % 12);
        String ampm = hour24 < 12 ? "AM" : "PM";
        return String.format(Locale.getDefault(), "%d:%02d %s", hr12, minute, ampm);
    }

    // ---------------------------------------
    // Small utilities
    // ---------------------------------------
    private static String text(@Nullable EditText et) {
        return (et == null) ? "" : et.getText().toString().trim();
    }

    private static void makeTapOnly(EditText et) {
        et.setFocusable(false);
        et.setFocusableInTouchMode(false);
        et.setClickable(true);
        et.setLongClickable(false);
        // If you ever see keyboards pop up via accessibility, also consider: et.setKeyListener(null);
    }

    private static boolean require(String value, EditText field) {
        if (!value.isEmpty()) return true;
        field.setError("Required");
        field.requestFocus();
        return false;
    }

    @SuppressWarnings("unused")
    private static long combineDayAndTimeUtc(long dayUtcMillis, long timeOfDayMs) {
        return dayUtcMillis + timeOfDayMs;
    }
}