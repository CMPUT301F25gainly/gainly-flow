package com.example.gainly_flow;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class CreateEvent extends AppCompatActivity {

    // UI
    private ImageView posterPreview;
    private EditText posterUrlInput;
    private EditText eventNameInput, eventDescriptionInput, eventDateInput, eventTimeInput, capacityInput;
    private EditText registrationOpenInput, registrationCloseInput;
    private CheckBox geolocationCheckbox;

    // Formats for date and time
    private final SimpleDateFormat dateFmt   = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat timeFmt12 = new SimpleDateFormat("h:mm a",     Locale.getDefault());

    /** Constructor for the view */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        androidx.activity.EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_event);

        bindViews(); // bind views to the xml components
        applyEdgeToEdgeInsets();
        configureTapOnlyInputs();
        wirePickers();
        wireButtons();
        wireUrlPreview();
    }

    /**
     * Initializes and binds all view components from layout to  corresponding variables.
     */
    private void bindViews() {
        posterPreview           = findViewById(R.id.posterPreview);
        posterUrlInput          = findViewById(R.id.posterUrlInput);

        eventNameInput          = findViewById(R.id.eventNameInput);
        eventDescriptionInput   = findViewById(R.id.eventDescriptionInput);
        eventDateInput          = findViewById(R.id.eventDateInput);
        eventTimeInput          = findViewById(R.id.eventTimeInput);
        capacityInput           = findViewById(R.id.eventCapacityInput);
        geolocationCheckbox     = findViewById(R.id.geolocationCheckbox);
        registrationOpenInput   = findViewById(R.id.registrationOpenInput);
        registrationCloseInput  = findViewById(R.id.registrationCloseInput);
    }

    /**
     * Applies edge-to-edge window insets to root view so layout adjusts for status bar and nav bar areas.
     */
    private void applyEdgeToEdgeInsets() {
        View root = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    /**
     * make certain edit texts tap only - for date and time inputs to create a fragment.
     */
    private void configureTapOnlyInputs() {
        makeTapOnly(eventDateInput);
        makeTapOnly(eventTimeInput);
        makeTapOnly(registrationOpenInput);
        makeTapOnly(registrationCloseInput);
    }

    /**
     * set up listeners for date and time fields.
     */
    private void wirePickers() {
        eventDateInput.setOnClickListener(v -> showDatePicker("Select event date",
                selectedUtc -> eventDateInput.setText(utcMillisToLocalDateString(selectedUtc))));

        registrationOpenInput.setOnClickListener(v -> showDatePicker("Select registration open date",
                selectedUtc -> registrationOpenInput.setText(utcMillisToLocalDateString(selectedUtc))));

        registrationCloseInput.setOnClickListener(v -> showDatePicker("Select registration close date",
                selectedUtc -> registrationCloseInput.setText(utcMillisToLocalDateString(selectedUtc))));

        eventTimeInput.setOnClickListener(v -> showTimePicker("Select event time",
                (hour24, minute) -> eventTimeInput.setText(formatTime(hour24, minute))));
    }

    /**
     * on click listener for buttons - currently only for save event button.
     */
    private void wireButtons() {
        findViewById(R.id.saveEventButton).setOnClickListener(v -> saveEvent());
    }

    /**
     * Live preview of inserted image url.
     */
    private void wireUrlPreview() {
        posterUrlInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                String pasted = s.toString().trim();
                if (pasted.isEmpty()) {
                    posterPreview.setImageDrawable(null);
                    return;
                }
                String normalized = normalizePosterUrl(pasted); // <-- uses both methods below
                if (!isLikelyHttp(normalized)) return;

                Glide.with(CreateEvent.this)
                        .load(normalized)
                        .into(posterPreview);
            }
        });
    }

    // --------------------- Save Event ---------------------

    /**
     * Validates form inputs, normalizes img url, assembles what to send, and initiates saving to firestore.
     *
     * inline  field errors or toast confirmation for valid and invalid inputs.
     */
    private void saveEvent() {
        final String name        = text(eventNameInput);
        final String desc        = text(eventDescriptionInput);
        final String dateStr     = text(eventDateInput);
        final String timeStr     = text(eventTimeInput);
        final String regOpenStr  = text(registrationOpenInput);
        final String regCloseStr = text(registrationCloseInput);
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

        if (eventDateMillis == null)      { eventDateInput.setError("Invalid date"); return; }
        if (eventTimeOfDayMillis == null) { eventTimeInput.setError("Invalid time"); return; }

        final String id    = java.util.UUID.randomUUID().toString();
        final String qrUrl = QRUrlCreator.buildDeepLink(id);

        // URL-only: read from field, normalize (Google Images + Google Drive)
        String pasted = text(posterUrlInput).trim();
        String posterUrl = pasted.isEmpty() ? "" : normalizePosterUrl(pasted);

        if (!posterUrl.isEmpty() && !isLikelyHttp(posterUrl)) {
            Toast.makeText(this, "Poster URL must start with http or https.", Toast.LENGTH_LONG).show();
            return;
        }

        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("id", id);
        data.put("name", name);
        data.put("description", desc);
        data.put("eventDateUtc", String.valueOf(eventDateMillis));
        data.put("eventTimeOfDayMs", String.valueOf(eventTimeOfDayMillis));
        data.put("registrationOpenUtc", regOpenMillis == null ? "" : String.valueOf(regOpenMillis));
        data.put("registrationCloseUtc", regCloseMillis == null ? "" : String.valueOf(regCloseMillis));
        data.put("capacity", capStr);
        data.put("geolocationEnabled", String.valueOf(geoEnabled));
        data.put("qrUrl", qrUrl);
        data.put("createdAt", System.currentTimeMillis());
        data.put("posterUrl", posterUrl);

        saveEventToFirestore(id, data, qrUrl);
    }

    /**
     * Writes event details to the events document in Firestore.
     * and, on success, shows QR dialog before finishing the activity.
     *
     * @param id    Unique uuid generated for event.
     * @param data  Event details like date, time, name, etc.
     * @param qrUrl from the qrimage decoded to a qrurl to store in database for retrieval when scanning similar qr code.
     */
    private void saveEventToFirestore(String id, java.util.Map<String, Object> data, String qrUrl) {
        FirebaseFirestore.getInstance()
                .collection("events")
                .document(id)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
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

    // --------------------- Pickers ---------------------
    private interface DatePicked { void onPicked(long utcMidnightMillis); }
    private interface TimePicked { void onPicked(int hour24, int minute); }

    /**
     * Date picker for Date Fields.
     *
     * @param title    Title  of pop up window when picking date.
     * @param callback  Event fields to persist; merged into the existing document if present.
     */
    private void showDatePicker(String title, DatePicked callback) {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(title)
                .build();
        picker.addOnPositiveButtonClickListener(callback::onPicked);
        picker.show(getSupportFragmentManager(), "mdp:" + title);
    }

    /**
     * Time picker for Event Time.
     *
     * @param title    Title  of pop up window when picking Time.
     * @param callback  Event fields to persist; merged into the existing document if present.
     */
    private void showTimePicker(String title, TimePicked callback) {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(9).setMinute(0)
                .setTitleText(title)
                .build();
        picker.addOnPositiveButtonClickListener(v -> callback.onPicked(picker.getHour(), picker.getMinute()));
        picker.show(getSupportFragmentManager(), "mtp:" + title);
    }

    // --------------------- QR dialog (unchanged) ---------------------


    /**
     * Builds QR bitmap from provided deep link and calls another function to get the QR code.
     *
     * @param qrUrl   The deep link URL to encode in the QR image.
     * @param onClose Optional callback invoked after the dialog is dismissed via the positive button.
     */
    private void showQrDialog(String qrUrl, @Nullable Runnable onClose) {
        Bitmap bmp = QRImage.bitmapFromUrl(qrUrl, 512);
        showQrDialog(qrUrl, bmp, onClose);
    }

    /**
     * Displays a dialog containing a QR Code for given deep link URL and bitmap.
     *
     * @param qrUrl   The deep link URL represented by the QR code.
     * @param bmp     The pre-rendered QR bitmap to display.
     * @param onClose Optional callback invoked after the user taps "Done".
     */
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

    /**
     * Functioanlity not yet implemented, will do in project part 4.
     */
    private void shareQrPng(String qrUrl) {
//        byte[] png = QRImage.pngFromUrl(qrUrl, 1024);
//        if (png == null) {
//            Toast.makeText(this, "Share failed", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        try {
//            File out = new File(getCacheDir(), "event_qr.png");
//            try (FileOutputStream fos = new FileOutputStream(out)) { fos.write(png); }
//            Uri uri = androidx.core.content.FileProvider.getUriForFile(
//                    this, getPackageName() + ".fileprovider", out);
//
//            Intent share = new Intent(Intent.ACTION_SEND);
//            share.setType("image/png");
//            share.putExtra(Intent.EXTRA_STREAM, uri);
//            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//            startActivity(Intent.createChooser(share, "Share QR"));
//        } catch (Exception e) {
//            Toast.makeText(this, "Share failed", Toast.LENGTH_SHORT).show();
//        }
    }

    // --------------------- Date/time parsing ---------------------

    /**
     * Parses a yyyy-MM-dd local-date string and returns the corresponding UTC midnight epoch millis.
     *
     * @param ymd Local date string in the format yyyy-MM-dd.
     * @return UTC midnight of that calendar day in milliseconds or null on failure.
     */
    @Nullable
    private Long parseDayUtc(String ymd) {
        try {
            dateFmt.setLenient(false);
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

    /**
     * Parses a 12-hour time string of format 'h:mm a' into a milliseconds from midnight.
     *
     * @param time12 Time string in the format 'h:mm a'.
     * @return Milliseconds since midnight (0..86,399,999), or null on failure.
     */
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

    /**
     * Converts a UTC midnight epoch millis value into a local date string of format yyyy-MM-dd.
     *
     * @param utcMidnight UTC midnight timestamp in milliseconds.
     * @return Local date string formatted as yyyy-MM-dd.
     */
    private String utcMillisToLocalDateString(long utcMidnight) {
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.setTimeInMillis(utcMidnight);

        Calendar local = Calendar.getInstance();
        local.clear();
        local.set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH));
        return dateFmt.format(local.getTime());
    }

    /**
     * Formats a 24-hour time into a 12-hour time string.
     *
     * @param hour24 Hour of day in 24-hour format.
     * @param minute Minute of hour.
     * @return A formatted time string such as 9:05 AM or 12:30 PM.
     */
    private String formatTime(int hour24, int minute) {
        int hr12 = (hour24 % 12 == 0) ? 12 : (hour24 % 12);
        String ampm = hour24 < 12 ? "AM" : "PM";
        return String.format(Locale.getDefault(), "%d:%02d %s", hr12, minute, ampm);
    }

    // --------------------- Small utils ---------------------
    /**
     * Returns the trimmed text content of an EditText or an empty string if it is null.
     *
     * @param et The EditText to read.
     * @return Trimmed text value, never null.
     */
    private static String text(@Nullable EditText et) {
        return (et == null) ? "" : et.getText().toString().trim();
    }

    /**
     * Configures an EditText to be tap-only
     *
     * @param et The input field to adjust.
     */
    private static void makeTapOnly(EditText et) {
        et.setFocusable(false);
        et.setFocusableInTouchMode(false);
        et.setClickable(true);
        et.setLongClickable(false);
    }

    /**
     * Ensures a required field is non-empty; if empty, sets an error and focuses the field.
     *
     * @param value The current string value to validate.
     * @param field The corresponding EditText to mark with an error if invalid.
     * @return true if value is non-empty; otherwise false.
     */
    private static boolean require(String value, EditText field) {
        if (!value.isEmpty()) return true;
        field.setError("Required");
        field.requestFocus();
        return false;
    }

    // --------------------- BOTH NORMALIZER METHODS + wrapper ---------------------

    /**
     * Normalizes a user-supplied URL in the format of an imgurl or google drive url link
     *
     * @param raw The raw URL pasted by the user.
     * @return A normalized, more-direct URL suitable for image loading.
     */
    private String normalizePosterUrl(String raw) {
        String s = raw.trim();
        s = normalizeImageUrlFromGoogleSearch(s); // extract ?imgurl=... from Google Images
        s = normalizeGoogleDriveUrl(s);           // turn Drive share into direct uc?export=download
        return s;
    }

    /**
     * Extracts the direct image URL from a Google Images wrapper link, if applicable.
     *
     * <p>Handles URLs like https://www.google.com/imgres?imgurl=...
     * If the input is not a Google Images wrapper, the original string is returned unchanged.
     *
     * @param raw The potentially wrapped Google Images URL.
     * @return The direct image URL if found; otherwise the original input.
     */
    private String normalizeImageUrlFromGoogleSearch(String raw) {
        try {
            Uri uri = Uri.parse(raw);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.US);

            // e.g., https://www.google.com/imgres?imgurl=<direct>&imgrefurl=...
            if (host.contains("google.") && uri.getPath() != null && uri.getPath().contains("/imgres")) {
                String direct = uri.getQueryParameter("imgurl");
                if (direct != null && !direct.isEmpty()) return direct;
            }
            return raw;
        } catch (Exception e) {
            return raw;
        }
    }

    /**
     * Converts common Google Drive share links into a direct file URL usable by image loaders.
     *
     * Supported patterns include:
     *  https://drive.google.com/file/d/<FILE_ID>/view?...
     *  https://drive.google.com/open?id=<FILE_ID>.
     * These are transformed into https://drive.google.com/uc?export=download&id=<FILE_ID>.
     *
     * @param url The original Google Drive URL.
     * @return A direct content URL if recognized; otherwise the original input.
     */
    private String normalizeGoogleDriveUrl(String url) {
        // Case 1: https://drive.google.com/file/d/FILE_ID/view?usp=sharing
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("https?://drive\\.google\\.com/file/d/([a-zA-Z0-9_-]+)")
                .matcher(url);
        if (m.find()) {
            String id = m.group(1);
            return "https://drive.google.com/uc?export=download&id=" + id;
        }

        // Case 2: https://drive.google.com/open?id=FILE_ID  or any ?id=FILE_ID
        m = java.util.regex.Pattern
                .compile("[?&]id=([a-zA-Z0-9_-]+)")
                .matcher(url);
        if (m.find()) {
            String id = m.group(1);
            return "https://drive.google.com/uc?export=download&id=" + id;
        }

        return url; // unchanged
    }

    /**
     * Checks whether a URL appears to be an HTTP(S) link.
     *
     * @param url The URL string to test.
     * @return true if the URL starts with http:// or https:// otherwise false.
     */
    private boolean isLikelyHttp(String url) {
        String u = url.toLowerCase(Locale.US);
        return u.startsWith("http://") || u.startsWith("https://");
    }
}
