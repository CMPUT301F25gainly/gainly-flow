package com.example.gainly_flow;

import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class CreateEvent extends AppCompatActivity {
    private EditText eventNameInput, eventDescriptionInput, eventDateInput, eventTimeInput, capacityInput;

    // Add these two date fields from your XML
    private EditText registrationOpenInput, registrationCloseInput;

    private final SimpleDateFormat displayFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        androidx.activity.EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_event);

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

        // Hook up the registration date fields
        registrationOpenInput  = findViewById(R.id.registrationOpenInput);
        registrationCloseInput = findViewById(R.id.registrationCloseInput);

        // Ensure they are tap-only (no keyboard)
        makeTapOnly(registrationOpenInput);
        makeTapOnly(registrationCloseInput);
        makeTapOnly(eventDateInput);
        makeTapOnly(eventTimeInput);

        eventTimeInput.setOnClickListener(v -> showEventTimePicker());
        eventDateInput.setOnClickListener(v -> showEventDatePicker());
        registrationOpenInput.setOnClickListener(v -> showOpenPicker());
        registrationCloseInput.setOnClickListener(v -> showClosePicker());
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
        // Example: disallow past dates (optional — delete constraints if not desired)
        CalendarConstraints.Builder constraints = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.now());

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select event date")
                .setCalendarConstraints(constraints.build())
                .build();

        picker.addOnPositiveButtonClickListener(selectionUtc ->
                eventDateInput.setText(utcMillisToLocalDateString(selectionUtc)));

        picker.show(getSupportFragmentManager(), "eventDatePicker");
    }
    private void showOpenPicker() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Registration Open date")
                .build();

        picker.addOnPositiveButtonClickListener(selectionUtc -> {
            String openText = utcMillisToLocalDateString(selectionUtc);
            registrationOpenInput.setText(openText);

            // If close is already set but now invalid, clear it
            String closeText = registrationCloseInput.getText().toString().trim();
            if (!closeText.isEmpty()) {
                Long openUtc  = localDateStringToUtcStartOfDay(openText);
                Long closeUtc = localDateStringToUtcStartOfDay(closeText);
                if (openUtc != null && closeUtc != null && closeUtc < openUtc) {
                    registrationCloseInput.setText("");
                }
            }
        });

        picker.show(getSupportFragmentManager(), "openDatePicker");
    }

    private void showClosePicker() {
        Long minUtc = null; // enforce close >= open
        String openText = registrationOpenInput.getText().toString().trim();
        if (!openText.isEmpty()) {
            minUtc = localDateStringToUtcStartOfDay(openText);
        }

        CalendarConstraints.Builder constraints = new CalendarConstraints.Builder();
        if (minUtc != null) {
            constraints.setValidator(DateValidatorPointForward.from(minUtc));
        }

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select registration close date")
                .setCalendarConstraints(constraints.build())
                .build();

        picker.addOnPositiveButtonClickListener(selectionUtc ->
                registrationCloseInput.setText(utcMillisToLocalDateString(selectionUtc)));

        picker.show(getSupportFragmentManager(), "closeDatePicker");
    }

    /** Convert MaterialDatePicker's UTC midnight millis to a local yyyy-MM-dd string. */
    private String utcMillisToLocalDateString(long utcMidnight) {
        // Read Y-M-D in UTC
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.setTimeInMillis(utcMidnight);
        int y = utc.get(Calendar.YEAR);
        int m = utc.get(Calendar.MONTH); // 0-based
        int d = utc.get(Calendar.DAY_OF_MONTH);

        // Build a local calendar at that Y-M-D (start of day)
        Calendar local = Calendar.getInstance();
        local.clear();
        local.set(y, m, d);

        return displayFmt.format(local.getTime());
    }

    /** Parse a local yyyy-MM-dd and return the UTC millis at that day's UTC midnight (for constraints). */
    private Long localDateStringToUtcStartOfDay(String s) {
        try {
            displayFmt.setLenient(false);
            Calendar local = Calendar.getInstance();
            local.setTime(displayFmt.parse(s));
            // Normalize to local start of day
            local.set(Calendar.HOUR_OF_DAY, 0);
            local.set(Calendar.MINUTE, 0);
            local.set(Calendar.SECOND, 0);
            local.set(Calendar.MILLISECOND, 0);

            // Convert that local date to UTC midnight millis (MaterialDatePicker uses UTC days)
            Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            utc.clear();
            utc.set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH));
            return utc.getTimeInMillis();
        } catch (ParseException e) {
            return null;
        }
    }
}