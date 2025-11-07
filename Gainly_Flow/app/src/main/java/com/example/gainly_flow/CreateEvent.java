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

/**
 * Activity for creating a new event.
 * <p>
 * This screen collects event metadata (name, description, date, time, capacity, registration window,
 * geolocation flag and an optional poster image URL), validates the inputs, writes a normalized
 * record to Firestore, and displays a QR code for the generated deep link.
 * </p>
 *
 * <h3>Key features</h3>
 * <ul>
 *   <li>Tap-only date/time inputs using Material pickers</li>
 *   <li>Live poster preview with URL normalization (Google Images / Google Drive)</li>
 *   <li>Edge-to-edge layout insets handling</li>
 *   <li>QR code generation and sharing stub</li>
 * </ul>
 *
 * <p><b>Firestore schema (fields written)</b>:
 * <code>id, name, description, eventDateUtc, eventTimeOfDayMs, registrationOpenUtc,
 * registrationCloseUtc, capacity, geolocationEnabled, qrUrl, createdAt, posterUrl</code></p>
 *
 * <p><b>Threading</b>: Firestore calls are asynchronous and display user feedback via Toasts.</p>
 */
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

    /**
     * Lifecycle entry point. Wires up views, edge-to-edge behavior, pickers, and listeners.
     *
     * @param savedInstanceState previously saved instance state, or {@code null}.
     */
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
        wireUrlPreview();
    }

    /**
     * Binds all view components from the layout to corresponding fields.
     * <p>Assumes IDs are present in {@code activity_create_event.xml}.</p>
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
     * Applies edge-to-edge window insets so the layout avoids status/navigation bars.
     * <p>Pads the root view with system bar insets.</p>
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
     * Makes date/time and registration fields "tap-only" so soft keyboard does not appear.
     * <p>Clicking the fields opens the appropriate picker.</p>
     */
    private void configureTapOnlyInputs() {
        makeTapOnly(eventDateInput);
        makeTapOnly(eventTimeInput);
        makeTapOnly(registrationOpenInput);
        makeTapOnly(registrationCloseInput);
    }

    /**
     * Attaches Material pickers to the date/time inputs.
     * <ul>
     *   <li>Event date, registration open/close use {@link MaterialDatePicker}</li>
     *   <li>Event time uses {@link MaterialTimePicker}</li>
     * </ul>
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
     * Wires primary button listeners (e.g., Save).
     * <p>Invokes {@link #saveEvent()} on click.</p>
     */
    private void wireButtons() {
        findViewById(R.id.saveEventButton).setOnClickListener(v -> saveEvent());
    }

    /**
     * Sets up a live preview for the poster URL.
     * <p>
     * Normalizes Google Images/Drive links and loads with Glide when the text changes.
     * If the field is empty, clears the preview.
     * </p>
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
                String normalized = normalizePosterUrl(pasted);
                if (!isLikelyHttp(normalized)) return;

                Glide.with(CreateEvent.this)
                        .load(normalized)
                        .into(posterPreview);
            }
        });
    }

    // --------------------- Save Event ---------------------

    /**
     * Validates inputs, parses/normalizes values, creates an event payload, and writes to Firestore.
     * <p>
     * On success, shows the QR dialog and finishes the activity; on failure, shows a Toast.
     * Field-level errors are set for missing/invalid values.
     * </p>
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
     * Persists the event document in Firestore and, upon success, shows the QR dialog.
     *
     * @param id    unique event UUID.
     * @param data  event payload to store under {@code events/{id}}.
     * @param qrUrl deep link used for the QR code.
     * @see FirebaseFirestore
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

    /** Callback for a picked date represented as UTC midnight epoch millis. */
    private interface DatePicked { void onPicked(long utcMidnightMillis); }

    /** Callback for a picked time represented in 24h format and minutes. */
    private interface TimePicked { void onPicked(int hour24, int minute); }

    /**
     * Shows a {@link MaterialDatePicker} and returns the selected day in UTC midnight millis.
     *
     * @param title    dialog title.
     * @param callback invoked with {@code utcMidnightMillis} on positive selection.
     */
    private void showDatePicker(String title, DatePicked callback) {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(title)
                .build();
        picker.addOnPositiveButtonClickListener(callback::onPicked);
        picker.show(getSupportFragmentManager(), "mdp:" + title);
    }

    /**
     * Shows a {@link MaterialTimePicker} and returns the selected time as 24h hour/minute.
     *
     * @param title    dialog title.
     * @param callback invoked with selected {@code hour24} and {@code minute}.
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
     * Builds a QR bitmap for {@code qrUrl} and then displays it in a dialog.
     *
     * @param qrUrl   deep link to encode.
     * @param onClose optional callback after dialog dismissal.
     * @see #showQrDialog(String, Bitmap, Runnable)
     */
    private void showQrDialog(String qrUrl, @Nullable Runnable onClose) {
        Bitmap bmp = QRImage.bitmapFromUrl(qrUrl, 512);
        showQrDialog(qrUrl, bmp, onClose);
    }

    /**
     * Displays a dialog with a QR image representing {@code qrUrl}.
     *
     * @param qrUrl   the deep link embedded in the QR code.
     * @param bmp     pre-rendered QR bitmap; if {@code null}, shows a failure toast and returns.
     * @param onClose optional callback invoked after "Done".
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
     * Shares the QR code as a PNG image via Android Sharesheet (stub not yet enabled).
     * <p>
     * The implementation is intentionally commented out for a later project stage.
     * To enable, generate PNG bytes, write to a cache file, wrap with a FileProvider URI,
     * and send an {@link Intent#ACTION_SEND} with {@code image/png} MIME type.
     * </p>
     *
     * @param qrUrl the deep link to encode in the shared QR.
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
     * Parses a local date string {@code yyyy-MM-dd} and returns the corresponding UTC midnight epoch millis.
     *
     * @param ymd local date string (e.g., {@code 2025-03-14}).
     * @return UTC midnight timestamp for that day, or {@code null} if parsing fails.
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
     * Parses a 12-hour time string {@code h:mm a} into milliseconds since midnight.
     *
     * @param time12 time string like {@code 9:05 AM} or {@code 12:30 PM}.
     * @return milliseconds from midnight in the range {@code [0, 86_399_999]}, or {@code null} on failure.
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
     * Converts a UTC midnight epoch millis value into a local {@code yyyy-MM-dd} string.
     *
     * @param utcMidnight UTC midnight in milliseconds.
     * @return formatted local date string (e.g., {@code 2025-03-14}).
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
     * Formats a 24-hour time into a {@code h:mm a} 12-hour string.
     *
     * @param hour24 hour in 24-hour format ({@code 0..23}).
     * @param minute minute ({@code 0..59}).
     * @return formatted string such as {@code 9:05 AM} or {@code 12:30 PM}.
     */
    private String formatTime(int hour24, int minute) {
        int hr12 = (hour24 % 12 == 0) ? 12 : (hour24 % 12);
        String ampm = hour24 < 12 ? "AM" : "PM";
        return String.format(Locale.getDefault(), "%d:%02d %s", hr12, minute, ampm);
    }

    // --------------------- Small utils ---------------------

    /**
     * Returns the trimmed text content of an {@link EditText}, or an empty string if {@code null}.
     *
     * @param et input to read.
     * @return trimmed value; never {@code null}.
     */
    private static String text(@Nullable EditText et) {
        return (et == null) ? "" : et.getText().toString().trim();
    }

    /**
     * Configures an {@link EditText} to be "tap-only" (non-focusable) so it opens a picker instead of a keyboard.
     *
     * @param et input field to adjust (must be non-null).
     */
    private static void makeTapOnly(EditText et) {
        et.setFocusable(false);
        et.setFocusableInTouchMode(false);
        et.setClickable(true);
        et.setLongClickable(false);
    }

    /**
     * Ensures a required field is non-empty; otherwise sets an error and focuses the field.
     *
     * @param value current string value.
     * @param field input field to mark with an error if invalid.
     * @return {@code true} if {@code value} is non-empty; {@code false} otherwise.
     */
    private static boolean require(String value, EditText field) {
        if (!value.isEmpty()) return true;
        field.setError("Required");
        field.requestFocus();
        return false;
    }

    // --------------------- URL normalization ---------------------

    /**
     * Normalizes a user-supplied poster URL by:
     * <ol>
     *     <li>Extracting a direct image link from Google Images wrappers</li>
     *     <li>Converting Google Drive share links to a direct {@code uc?export=download} URL</li>
     * </ol>
     *
     * @param raw raw URL pasted by the user.
     * @return normalized URL suitable for image loading; original {@code raw} if no transformation applies.
     * @see #normalizeImageUrlFromGoogleSearch(String)
     * @see #normalizeGoogleDriveUrl(String)
     */
    private String normalizePosterUrl(String raw) {
        String s = raw.trim();
        s = normalizeImageUrlFromGoogleSearch(s);
        s = normalizeGoogleDriveUrl(s);
        return s;
    }

    /**
     * Extracts the direct image URL from a Google Images result wrapper if applicable.
     * <p>Handles URLs like {@code https://www.google.com/imgres?imgurl=...}.</p>
     *
     * @param raw potentially wrapped Google Images URL.
     * @return direct image URL if found; otherwise original {@code raw}.
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
     * Converts supported Google Drive sharing URLs to a direct content URL.
     * <ul>
     *   <li>{@code https://drive.google.com/file/d/<FILE_ID>/view?...}</li>
     *   <li>{@code https://drive.google.com/open?id=<FILE_ID>} (or any {@code ?id=<FILE_ID>})</li>
     * </ul>
     *
     * @param url original Google Drive URL.
     * @return direct content URL if recognized; otherwise original input.
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
     * Checks whether a string looks like an HTTP(S) URL.
     *
     * @param url candidate URL.
     * @return {@code true} if it starts with {@code http://} or {@code https://}; otherwise {@code false}.
     */
    private boolean isLikelyHttp(String url) {
        String u = url.toLowerCase(Locale.US);
        return u.startsWith("http://") || u.startsWith("https://");
    }
}
