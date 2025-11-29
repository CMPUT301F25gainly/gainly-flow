package com.example.gainly_flow;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class EntrantViewMain extends AppCompatActivity {

    private MaterialButton browseEventsButton, lotteryGuidelinesButton, eventHistoryButton;
    private MaterialButton startDateButton, endDateButton, applyFiltersButton, clearFiltersButton;
    private BottomNavigationView bottomNav;
    private LinearLayout eventListContainer, filterOptionsLayout;
    private ImageButton backButton, filterToggleButton;
    private EditText searchInput;
    private Spinner categorySpinner;
    private RadioGroup availabilityRadioGroup;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private final SimpleDateFormat filterDateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
    private Profile currentProfile;
    private Entrant currentEntrant;

    private List<Event> allEvents = new ArrayList<>();
    private List<Event> filteredEvents = new ArrayList<>();

    // Filter state
    private String currentSearchQuery = "";
    private String currentCategoryFilter = "ALL";
    private Date startDateFilter = null;
    private Date endDateFilter = null;
    private String availabilityFilter = "all";

    // Date picker
    private Calendar calendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrant_home);

        initializeProfileData();
        initializeViews();
        setupFilters();
        loadEventsFromFirebase();
    }

    private void initializeViews() {
        browseEventsButton = findViewById(R.id.browseEventsButton);
        lotteryGuidelinesButton = findViewById(R.id.lotteryGuidelinesButton);
        bottomNav = findViewById(R.id.bottomNav);
        eventListContainer = findViewById(R.id.eventListContainer);
        eventHistoryButton = findViewById(R.id.eventHistoryButton);
        backButton = findViewById(R.id.backButton);

        // Filter views
        filterOptionsLayout = findViewById(R.id.filterOptionsLayout);
        filterToggleButton = findViewById(R.id.filterToggleButton);
        searchInput = findViewById(R.id.searchInput);
        categorySpinner = findViewById(R.id.categorySpinner);
        startDateButton = findViewById(R.id.startDateButton);
        endDateButton = findViewById(R.id.endDateButton);
        applyFiltersButton = findViewById(R.id.applyFiltersButton);
        clearFiltersButton = findViewById(R.id.clearFiltersButton);
        availabilityRadioGroup = findViewById(R.id.availabilityRadioGroup);

        setupButtonListeners();
    }

    private void setupButtonListeners() {
        browseEventsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, QRCodeScanner.class);
            if (currentEntrant != null) intent.putExtra("entrant", currentEntrant);
            else if (currentProfile != null) intent.putExtra("profile", currentProfile);
            startActivity(intent);
        });

        lotteryGuidelinesButton.setOnClickListener(v -> showLotteryGuidelines());

        eventHistoryButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, EntrantEventHistory.class);
            if (currentEntrant != null) intent.putExtra("entrant", currentEntrant);
            else if (currentProfile != null) intent.putExtra("profile", currentProfile);
            startActivity(intent);
        });

        backButton.setOnClickListener(v -> onBackPressed());
        bottomNav.setOnItemSelectedListener(this::onNavItemSelected);

        // Filter toggle
        filterToggleButton.setOnClickListener(v -> toggleFilterOptions());
    }

    private void setupFilters() {
        // Setup category spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.event_categories, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentCategoryFilter = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                currentCategoryFilter = "ALL";
            }
        });

        // Setup date pickers
        startDateButton.setOnClickListener(v -> showDatePicker(true));
        endDateButton.setOnClickListener(v -> showDatePicker(false));

        // Setup search
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().toLowerCase();
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Setup availability radio group
        availabilityRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioAll) availabilityFilter = "all";
            else if (checkedId == R.id.radioOpen) availabilityFilter = "open";
            else if (checkedId == R.id.radioUpcoming) availabilityFilter = "upcoming";
        });

        // Setup filter buttons
        applyFiltersButton.setOnClickListener(v -> {
            filterOptionsLayout.setVisibility(View.GONE);
            applyFilters();
        });

        clearFiltersButton.setOnClickListener(v -> clearFilters());
    }

    private void toggleFilterOptions() {
        boolean isVisible = filterOptionsLayout.getVisibility() == View.VISIBLE;
        filterOptionsLayout.setVisibility(isVisible ? View.GONE : View.VISIBLE);
    }

    private void showDatePicker(boolean isStartDate) {
        DatePickerDialog datePicker = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    Date selectedDate = calendar.getTime();

                    if (isStartDate) {
                        startDateFilter = selectedDate;
                        startDateButton.setText(filterDateFormat.format(selectedDate));
                    } else {
                        endDateFilter = selectedDate;
                        endDateButton.setText(filterDateFormat.format(selectedDate));
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePicker.show();
    }

    private void clearFilters() {
        // Reset all filter values
        currentSearchQuery = "";
        currentCategoryFilter = "ALL";
        startDateFilter = null;
        endDateFilter = null;
        availabilityFilter = "all";

        // Reset UI
        searchInput.setText("");
        categorySpinner.setSelection(0);
        startDateButton.setText("Start Date");
        endDateButton.setText("End Date");
        availabilityRadioGroup.check(R.id.radioAll);
        filterOptionsLayout.setVisibility(View.GONE);

        // Apply cleared filters
        applyFilters();
    }

    private void applyFilters() {
        filteredEvents.clear();

        for (Event event : allEvents) {
            if (matchesSearchQuery(event) &&
                    matchesCategory(event) &&
                    matchesDateRange(event) &&
                    matchesAvailability(event)) {
                filteredEvents.add(event);
            }
        }

        displayFilteredEvents();
    }

    private boolean matchesSearchQuery(Event event) {
        if (currentSearchQuery.isEmpty()) return true;

        String searchLower = currentSearchQuery.toLowerCase();
        return (event.getName() != null && event.getName().toLowerCase().contains(searchLower)) ||
                (event.getDescription() != null && event.getDescription().toLowerCase().contains(searchLower)) ||
                (event.getLocation() != null && event.getLocation().toLowerCase().contains(searchLower));
    }

    private boolean matchesCategory(Event event) {
        if ("ALL".equals(currentCategoryFilter)) return true;

        // Handle category matching - compare string values
        String eventCategory = event.getCategory() != null ? event.getCategory().name() : "OTHER";
        return eventCategory.equals(currentCategoryFilter);
    }

    private boolean matchesDateRange(Event event) {
        if (event.getEventDate() == null) return true;

        boolean afterStart = startDateFilter == null ||
                !event.getEventDate().before(startDateFilter);
        boolean beforeEnd = endDateFilter == null ||
                !event.getEventDate().after(endDateFilter);

        return afterStart && beforeEnd;
    }

    private boolean matchesAvailability(Event event) {
        switch (availabilityFilter) {
            case "open":
                return "OPEN".equals(event.getRegistrationStatus());
            case "upcoming":
                return event.getEventDate() != null &&
                        event.getEventDate().after(new Date());
            case "all":
            default:
                return true;
        }
    }

    private void displayFilteredEvents() {
        eventListContainer.removeAllViews();

        if (filteredEvents.isEmpty()) {
            showEmptyState("No events match your filters");
            return;
        }

        // Sort by date (soonest first)
        filteredEvents.sort((e1, e2) -> {
            if (e1.getEventDate() == null && e2.getEventDate() == null) return 0;
            if (e1.getEventDate() == null) return 1;
            if (e2.getEventDate() == null) return -1;
            return e1.getEventDate().compareTo(e2.getEventDate());
        });

        for (Event event : filteredEvents) {
            addEventToView(event);
        }
    }

    private void loadEventsFromFirebase() {
        FirebaseFirestore.getInstance()
                .collection("events")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    allEvents.clear();
                    filteredEvents.clear();

                    if (querySnapshot.isEmpty()) {
                        showEmptyState("No events available");
                        return;
                    }

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Event event = createEventFromDocument(doc);
                        allEvents.add(event);
                    }

                    // Apply current filters to newly loaded events
                    applyFilters();
                })
                .addOnFailureListener(e -> {
                    Log.e("EntrantViewMain", "Failed to load events: " + e.getMessage());
                    Toast.makeText(this, "Failed to load events", Toast.LENGTH_LONG).show();
                    showEmptyState("Failed to load events");
                });
    }

    // Keep your existing helper methods for profile data, event creation, etc.
    private void initializeProfileData() {
        currentEntrant = (Entrant) getIntent().getSerializableExtra("entrant");
        if (currentEntrant != null) {
            currentProfile = currentEntrant;
            return;
        }

        currentProfile = (Profile) getIntent().getSerializableExtra("profile");
        if (currentProfile != null && "Entrant".equals(currentProfile.getRole())) {
            currentEntrant = convertProfileToEntrant(currentProfile);
        } else if (currentProfile == null) {
            Toast.makeText(this, "Profile data not available", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private Entrant convertProfileToEntrant(Profile profile) {
        Entrant entrant = new Entrant(profile.getId());
        entrant.setDisplayName(profile.getDisplayName());
        entrant.setEmail(profile.getEmail());
        entrant.setPhone(profile.getPhone());
        entrant.setRole("Entrant");
        entrant.setDeviceId(profile.getDeviceId());
        entrant.setCreatedAt(profile.getCreatedAt());
        entrant.setLastLoginAt(profile.getLastLoginAt());
        entrant.setReceiveNotifications(profile.isReceiveNotifications());
        entrant.setEnableLocationService(profile.isEnableLocationService());
        return entrant;
    }

    private Event createEventFromDocument(DocumentSnapshot doc) {
        Event event = new Event(doc.getId());
        event.setName(doc.getString("name"));
        event.setDescription(doc.getString("description"));
        String posterId = doc.getString("posterImageId");
        if (posterId == null || posterId.isEmpty()) {
            posterId = doc.getString("posterUri");
        }
        event.setPosterImageId(posterId);
        event.setLocation(doc.getString("location"));
        event.setTimeString(doc.getString("timeString"));

        Long capLong = doc.getLong("capacity");
        event.setCapacity(capLong != null ? capLong.intValue() : 20);

        Long currentLong = doc.getLong("currentParticipants");
        event.setCurrentParticipants(currentLong != null ? currentLong.intValue() : 0);

        Boolean geo = doc.getBoolean("geolocationRequired");
        event.setGeolocationRequired(Boolean.TRUE.equals(geo));

        Object eventDateObj = doc.get("eventDate");
        if (eventDateObj instanceof Date) {
            event.setEventDate((Date) eventDateObj);
        } else if (eventDateObj instanceof Long) {
            event.setEventDate(new Date((Long) eventDateObj));
        }

        // Category handling
        String category = doc.getString("category");


        // Add other event fields as needed...
        return event;
    }

    private void showEmptyState(String message) {
        TextView emptyText = new TextView(this);
        emptyText.setText(message);
        emptyText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        emptyText.setPadding(0, 50, 0, 50);
        emptyText.setTextSize(16f);
        eventListContainer.addView(emptyText);
    }

    private void addEventToView(Event event) {
        CardView eventView = (CardView) LayoutInflater.from(this)
                .inflate(R.layout.item_event, eventListContainer, false);

        TextView eventTitle = eventView.findViewById(R.id.eventTitle);
        TextView eventDate = eventView.findViewById(R.id.eventDate);
        TextView eventSpots = eventView.findViewById(R.id.eventSpots);
        TextView eventWaiting = eventView.findViewById(R.id.eventWaiting);
        TextView eventStatus = eventView.findViewById(R.id.eventStatus);

        eventTitle.setText(event.getName() != null ? event.getName() : "Unnamed Event");

        if (event.getEventDate() != null) {
            eventDate.setText(dateFormat.format(event.getEventDate()));
        } else {
            eventDate.setText("Date TBD");
        }

        eventSpots.setText(event.getCapacity() + " spots");
        eventWaiting.setText(event.getWaitingListSize() + " waiting");

        String status = event.getRegistrationStatus();
        eventStatus.setText(status);

        // Status styling
        switch (status) {
            case "OPEN":
                eventStatus.setTextColor(getResources().getColor(R.color.green_500));
                eventStatus.setBackgroundResource(R.drawable.status_open_bg);
                break;
            case "FULL":
                eventStatus.setTextColor(getResources().getColor(R.color.yellow_500));
                eventStatus.setBackgroundResource(R.drawable.box_waiting);
                break;
            case "CLOSED":
                eventStatus.setTextColor(getResources().getColor(R.color.grey_500));
                eventStatus.setBackgroundResource(R.drawable.status_closed_bg);
                break;
            case "CANCELLED":
                eventStatus.setTextColor(getResources().getColor(R.color.grey_500));
                eventStatus.setBackgroundResource(R.drawable.box_cancelled);
                break;
            default:
                eventStatus.setTextColor(getResources().getColor(android.R.color.darker_gray));
                eventStatus.setBackgroundResource(R.drawable.status_closed_bg);
        }

        eventView.setOnClickListener(v -> {
            Intent intent = new Intent(EntrantViewMain.this, EventDetailActivity.class);
            intent.putExtra("event_id", event.getId());
            // Add other event data as needed...

            if (currentEntrant != null) {
                intent.putExtra("entrant", currentEntrant);
            } else if (currentProfile != null) {
                intent.putExtra("profile", currentProfile);
            }
            startActivity(intent);
        });

        eventListContainer.addView(eventView);
    }

    private boolean onNavItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Intent intent;

        if (id == R.id.menu_events) {
            loadEventsFromFirebase();
        } else if (id == R.id.menu_notifications) {
            intent = new Intent(this, NotificationsActivity.class);
            if (currentEntrant != null) intent.putExtra("entrant", currentEntrant);
            else if (currentProfile != null) intent.putExtra("profile", currentProfile);
            startActivity(intent);
        } else if (id == R.id.menu_profile) {
            intent = new Intent(this, ProfileActivity.class);
            if (currentEntrant != null) intent.putExtra("profile", currentEntrant);
            else if (currentProfile != null) intent.putExtra("profile", currentProfile);
            intent.putExtra("userType", "Entrant");
            startActivity(intent);
        }
        return true;
    }

    private void showLotteryGuidelines() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Lottery Guidelines & Process")
                .setMessage(
                        "1. Register for the event before the deadline.\n" +
                                "2. Join the event waiting list by scanning the QR code or using the app.\n" +
                                "3. After registration closes, the system randomly selects winners.\n" +
                                "4. Winners are notified via app notification or email.\n" +
                                "5. If a winner declines or does not confirm, a replacement is drawn from the waiting list.\n" +
                                "6. Confirm your spot to complete registration.\n" +
                                "7. If not selected, you will be notified and may have another chance if spots open.\n\n" +
                                "All selections are fair and random. For questions, contact support."
                )
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }


    @Override
    protected void onResume() {
        super.onResume();
        bottomNav.setSelectedItemId(R.id.menu_events);
        loadEventsFromFirebase();
    }
}
