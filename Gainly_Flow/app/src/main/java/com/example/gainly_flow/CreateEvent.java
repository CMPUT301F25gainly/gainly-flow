package com.example.gainly_flow;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

public class CreateEvent extends AppCompatActivity {
    private ImageView posterPreview;
    private ImageView qrPreview;
    private Button selectPosterBtn;
    private Button saveEventButton;
    @Nullable
    private Uri posterUri = null; // store selected image
    private EditText eventNameInput, eventDescriptionInput, eventDateInput, eventTimeInput, capacityInput;

    private CheckBox geolocationCheckbox;

    // Add these two date fields from your XML
    private EditText registrationOpenInput, registrationCloseInput;

    // Formats must match what you display in the EditTexts
    private final SimpleDateFormat dateFmt   = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat timeFmt12 = new SimpleDateFormat("h:mm a", Locale.getDefault());

    private final ActivityResultLauncher<String[]> openPosterLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    // Persist permission so we can read this later (e.g., after reboot)
                    final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                    try {
                        getContentResolver().takePersistableUriPermission(uri, takeFlags);
                    } catch (SecurityException ignored) { /* some providers don’t support persistable */ }

                    posterUri = uri;

                    // preview (use Glide/Picasso if you prefer)
                    posterPreview.setImageURI(uri);
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        androidx.activity.EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_event);

        posterPreview   = findViewById(R.id.posterPreview);
        selectPosterBtn = findViewById(R.id.selectPosterBtn);
        saveEventButton = findViewById(R.id.saveEventButton);

        View root = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        // findViewById for your existing fields (if you use them elsewhere)
        eventNameInput        = findViewById(R.id.eventNameInput);
        eventDescriptionInput = findViewById(R.id.eventDescriptionInput);
        eventDateInput        = findViewById(R.id.eventDateInput);
        eventTimeInput        = findViewById(R.id.eventTimeInput);
        capacityInput         = findViewById(R.id.eventCapacityInput);

        geolocationCheckbox = findViewById(R.id.geolocationCheckbox);
        // Hook up the registration date fields
        registrationOpenInput  = findViewById(R.id.registrationOpenInput);
        registrationCloseInput = findViewById(R.id.registrationCloseInput);

        // Ensure they are tap-only (no keyboard)
        makeTapOnly(registrationOpenInput);
        makeTapOnly(registrationCloseInput);
        makeTapOnly(eventDateInput);
        makeTapOnly(eventTimeInput);

        if (savedInstanceState != null) {
            String saved = savedInstanceState.getString("posterUri");
            if (saved != null) {
                posterUri = Uri.parse(saved);
                posterPreview.setImageURI(posterUri);
            }
        }
        eventTimeInput.setOnClickListener(v -> showEventTimePicker());
        eventDateInput.setOnClickListener(v -> showEventDatePicker());
        registrationOpenInput.setOnClickListener(v -> showOpenPicker());
        registrationCloseInput.setOnClickListener(v -> showClosePicker());


        View.OnClickListener pick = v -> openPosterLauncher.launch(new String[]{"image/*"});
        selectPosterBtn.setOnClickListener(pick);
        posterPreview.setOnClickListener(pick); // make preview tappable too
        saveEventButton.setOnClickListener(v -> saveEvent());
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (posterUri != null) outState.putString("posterUri", posterUri.toString());
    }

    // Call this when you press “Save Event”
    private void saveEvent() {
        // 1) Read text
        String name        = text(eventNameInput);
        String desc        = text(eventDescriptionInput);
        String dateStr     = text(eventDateInput);          // yyyy-MM-dd
        String timeStr     = text(eventTimeInput);          // h:mm AM/PM
        String regOpenStr  = text(registrationOpenInput);   // yyyy-MM-dd
        String regCloseStr = text(registrationCloseInput);  // yyyy-MM-dd
        String capStr      = text(capacityInput);
        String geoStr      = String.valueOf(geolocationCheckbox != null && geolocationCheckbox.isChecked());
        String poster      = (posterUri == null) ? "" : posterUri.toString();

        // 2) Light validation
        if (name.isEmpty()) { eventNameInput.setError("Required"); eventNameInput.requestFocus(); return; }
        if (dateStr.isEmpty()) { eventDateInput.setError("Required"); eventDateInput.requestFocus(); return; }
        if (timeStr.isEmpty()) { eventTimeInput.setError("Required"); eventTimeInput.requestFocus(); return; }
        if (capStr.isEmpty()) { capacityInput.setError("Required"); capacityInput.requestFocus(); return; }

        // 3) Convert to the String millis your Database.addEventDatabase expects
        Long eventDateMillis      = parseDayUtc(dateStr);                // UTC midnight of that date
        Long eventTimeOfDayMillis = parseTimeMsFromMidnight(timeStr);    // ms from midnight
        Long regOpenMillis        = regOpenStr.isEmpty()  ? null : parseDayUtc(regOpenStr);
        Long regCloseMillis       = regCloseStr.isEmpty() ? null : parseDayUtc(regCloseStr);

        if (eventDateMillis == null) { eventDateInput.setError("Invalid date"); return; }
        if (eventTimeOfDayMillis == null) { eventTimeInput.setError("Invalid time"); return; }

        //random id using uuid
        String id = UUID.randomUUID().toString();


        String qrUrl = QRUrlCreator.buildDeepLink(id); // or buildHttpsLink(id)
        Bitmap qrBmp = QRImage.bitmapFromUrl(qrUrl, 512);

        // Fallback: save without QR (or show error)
        Database.get().addEventDatabase(
                id, name, desc,
                String.valueOf(eventDateMillis),
                String.valueOf(eventTimeOfDayMillis),
                regOpenMillis == null ? "" : String.valueOf(regOpenMillis),
                regCloseMillis == null ? "" : String.valueOf(regCloseMillis),
                capStr,
                String.valueOf(geolocationCheckbox.isChecked()),
                poster,
                qrUrl,
                new Database.Callback() {
                    @Override public void onSuccess() {
                        showQrDialog(qrUrl, () -> {
                            Toast.makeText(CreateEvent.this, "Event saved", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        });
                    }
                    @Override public void onError(Exception e) {
                        Toast.makeText(CreateEvent.this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
        );
        return;

    }


    private String text(EditText et) { return et == null ? "" : et.getText().toString().trim(); }

    @Nullable
    private Integer tryParseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return null; }
    }

    /** Parse "yyyy-MM-dd" (local) as a UTC day epoch millis (00:00 UTC of that day). */
    @Nullable
    private Long parseDayUtc(String ymd) {
        try {
            dateFmt.setLenient(false);
            Date local = dateFmt.parse(ymd);
            Calendar cLocal = Calendar.getInstance();    // local tz
            cLocal.setTime(local);
            // Normalize to that calendar day (local)
            cLocal.set(Calendar.HOUR_OF_DAY, 0);
            cLocal.set(Calendar.MINUTE, 0);
            cLocal.set(Calendar.SECOND, 0);
            cLocal.set(Calendar.MILLISECOND, 0);
            // Create a UTC calendar at same Y-M-D
            Calendar cUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            cUtc.clear();
            cUtc.set(cLocal.get(Calendar.YEAR), cLocal.get(Calendar.MONTH), cLocal.get(Calendar.DAY_OF_MONTH));
            return cUtc.getTimeInMillis();
        } catch (Exception e) {
            return null;
        }
    }

    /** Parse "h:mm a" into milliseconds from midnight (0..86_399_999). */
    @Nullable
    private Long parseTimeMsFromMidnight(String time12) {
        try {
            timeFmt12.setLenient(false);
            Date d = timeFmt12.parse(time12);
            Calendar c = Calendar.getInstance();
            c.setTime(d);
            int h = c.get(Calendar.HOUR_OF_DAY);
            int m = c.get(Calendar.MINUTE);
            int s = c.get(Calendar.SECOND);
            return (long) ((h * 60 * 60 + m * 60 + s) * 1000);
        } catch (Exception e) {
            return null;
        }
    }

    /** Combine a UTC day (00:00 UTC) and a ms-from-midnight time into a single UTC timestamp. */
    private long combineDayAndTimeUtc(long dayUtcMillis, long timeOfDayMs) {
        return dayUtcMillis + timeOfDayMs;
    }


    private void makeTapOnly(EditText et) {
        et.setFocusable(false);
        et.setFocusableInTouchMode(false);
        et.setClickable(true);
        et.setLongClickable(false);
    }

    private void showEventTimePicker() {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(9)        // optional default
                .setMinute(0)
                .setTitleText("Select event time")
                .build();

        // Use View.OnClickListener form to avoid lambda signature mismatch
        picker.addOnPositiveButtonClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                int h = picker.getHour();
                int m = picker.getMinute();
                eventTimeInput.setText(formatTime(h, m));
            }
        });

        picker.show(getSupportFragmentManager(), "eventTimePicker");
    }

    private String formatTime(int hour24, int minute) {
        int hr12 = hour24 % 12 == 0 ? 12 : hour24 % 12;
        String ampm = hour24 < 12 ? "AM" : "PM";
        // If you want leading zero on hour, use "%02d" instead of "%d"
        return String.format(Locale.getDefault(), "%d:%02d %s", hr12, minute, ampm);
    }

    private void showEventDatePicker() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select event date")
                .build();

        picker.addOnPositiveButtonClickListener(selectionUtc -> {
            eventDateInput.setText(utcMillisToLocalDateString(selectionUtc)); // e.g., 2025-01-15
        });

        picker.show(getSupportFragmentManager(), "eventDatePicker");
    }
    private void showOpenPicker() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select registration open date")
                .build();

        picker.addOnPositiveButtonClickListener(selectionUtc -> {
            registrationOpenInput.setText(utcMillisToLocalDateString(selectionUtc));
            // clear/adjust close if needed...
        });

        picker.show(getSupportFragmentManager(), "openDatePicker");
    }

    private void showClosePicker() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select registration close date")
                .build();

        picker.addOnPositiveButtonClickListener(selectionUtc ->
                registrationCloseInput.setText(utcMillisToLocalDateString(selectionUtc)));

        picker.show(getSupportFragmentManager(), "closeDatePicker");
    }

    /** Convert MaterialDatePicker's UTC midnight millis to a local yyyy-MM-dd string. */
    private String utcMillisToLocalDateString(long utcMidnight) {
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.setTimeInMillis(utcMidnight);
        int y = utc.get(Calendar.YEAR), m = utc.get(Calendar.MONTH), d = utc.get(Calendar.DAY_OF_MONTH);

        Calendar local = Calendar.getInstance();
        local.clear();
        local.set(y, m, d);
        return dateFmt.format(local.getTime());  // <-- DATE format, not time
    }

    private void showQrDialog(String qrUrl, @Nullable Runnable onClose) {
        // Build QR bitmap (512px looks crisp)
        Bitmap bmp = QRImage.bitmapFromUrl(qrUrl, 512);
        if (bmp == null) {
            Toast.makeText(this, "Couldn't generate QR", Toast.LENGTH_SHORT).show();
            if (onClose != null) onClose.run();
            return;
        }

        // Inflate a small layout with an ImageView
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
                .setNeutralButton("Share", (d, w) -> {
                    // Share PNG bytes
                    byte[] png = QRImage.pngFromUrl(qrUrl, 1024);
                    if (png != null) {
                        // quick in-memory share via ACTION_SEND
                        try {
                            // write to cache file so we can share
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
                })
                .setCancelable(true)
                .show();
    }

}