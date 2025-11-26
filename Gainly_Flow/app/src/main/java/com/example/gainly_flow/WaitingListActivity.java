package com.example.gainly_flow;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Activity for organizers to view and manage entrant lists (Selected, Enrolled, Cancelled, Waiting)
 * for a specific event. This activity implements the new vertically stacked button UI.
 */
public class WaitingListActivity extends AppCompatActivity {

    private static final String TAG = "WaitingListActivity";
    private static final String EXTRA_EVENT_ID = "com.example.gainly_flow.event_id";

    private String eventId;
    private Event currentEvent;
    private Database database;

    // UI Components
    private TextView textEventName;
    private TabLayout tabStatus;
    private RecyclerView recyclerEntrants;
    private EntrantsAdapter entrantsAdapter;

    // Action Buttons (now solid and stacked)
    private MaterialButton btnExportCsv;
    private MaterialButton btnViewMap;

    /**
     * Factory method to create an Intent to start this activity.
     * @param context The application context.
     * @param eventId The ID of the event to view.
     * @return An Intent configured to start WaitingListActivity.
     */
    public static Intent newIntent(Context context, String eventId) {
        Intent intent = new Intent(context, WaitingListActivity.class);
        intent.putExtra(EXTRA_EVENT_ID, eventId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Uses the modified layout: activity_view_waiting_lists.xml
        setContentView(R.layout.activity_view_waiting_lists);

        // Get Event ID from Intent
        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        database = Database.get();

        if (eventId == null) {
            Log.e(TAG, "Event ID not provided. Finishing activity.");
            Toast.makeText(this, "Error: No event specified.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 1. Initialize UI components
        initializeToolbar();
        textEventName = findViewById(R.id.text_event_name);
        tabStatus = findViewById(R.id.tab_status);
        recyclerEntrants = findViewById(R.id.recycler_entrants);

        // 2. Initialize buttons
        btnExportCsv = findViewById(R.id.btn_export_csv);
        btnViewMap = findViewById(R.id.btn_view_map);

        // 3. Setup RecyclerView
        entrantsAdapter = new EntrantsAdapter(new ArrayList<>());
        recyclerEntrants.setAdapter(entrantsAdapter);

        // 4. Load event data and set up listeners
        loadEventData();
        setupActionListeners();
        setupTabListener();
    }

    /**
     * Set up the MaterialToolbar (Purple top bar).
     */
    private void initializeToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_entrant_lists);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Waiting Lists");
        }
    }

    /**
     * Handle the back button press in the toolbar.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Load the Event object from the database and update the UI.
     */
    private void loadEventData() {
        currentEvent = new Event();
        currentEvent.load(eventId, event -> {
            this.currentEvent = event;
            updateEventDetails(event);
            // Default to showing the Waiting list
            updateEntrantList(event.getWaitingList(), "Waiting");
        });
    }

    /**
     * Update non-list UI elements with event data.
     */
    private void updateEventDetails(Event event) {
        // Example: Swimming Lessons • Entrants
        String subtitle = event.getName() + " • Entrants";
        textEventName.setText(subtitle);

        // Update tab counts (assuming tab indexes are 0=Selected, 1=Enrolled, 2=Cancelled, 3=Waiting)
        tabStatus.getTabAt(0).setText("Selected (" + event.getSelectedCount() + ")");
        tabStatus.getTabAt(1).setText("Enrolled (" + event.getEnrolledCount() + ")");
        tabStatus.getTabAt(2).setText("Cancelled (" + (event.getCancelled() != null ? event.getCancelled().size() : 0) + ")");
        tabStatus.getTabAt(3).setText("Waiting (" + event.getWaitingListSize() + ")");
    }

    /**
     * Set up the listener for the tab changes.
     */
    private void setupTabListener() {
        tabStatus.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (currentEvent == null) return;
                List<String> entrantIds = new ArrayList<>();
                String status = "";

                // Determine which list to display based on the selected tab index
                switch (tab.getPosition()) {
                    case 0: // Selected
                        entrantIds = currentEvent.getSelected();
                        status = "Selected";
                        break;
                    case 1: // Enrolled
                        entrantIds = currentEvent.getEnrolled();
                        status = "Enrolled";
                        break;
                    case 2: // Cancelled
                        entrantIds = currentEvent.getCancelled();
                        status = "Cancelled";
                        break;
                    case 3: // Waiting (Default)
                    default:
                        entrantIds = currentEvent.getWaitingList();
                        status = "Waiting";
                        break;
                }
                updateEntrantList(entrantIds, status);
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    /**
     * Fetch entrant details for the given IDs and update the RecyclerView.
     */
    private void updateEntrantList(List<String> entrantIds, String status) {
        if (entrantIds == null || entrantIds.isEmpty()) {
            entrantsAdapter.setEntrants(new ArrayList<>());
            Toast.makeText(this,
                    "No entrants found for status: " + status,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        List<Entrant> mockEntrants = buildMockEntrants(entrantIds);
        entrantsAdapter.setEntrants(mockEntrants);

        Toast.makeText(this,
                "Displaying " + mockEntrants.size() + " " + status + " entrants.",
                Toast.LENGTH_SHORT).show();
    }


    /**
     * Build mock Entrant objects from their IDs.
     * In a real app you would load entrants from Firestore instead.
     */
    private List<Entrant> buildMockEntrants(List<String> entrantIds) {
        List<Entrant> mockEntrants = new ArrayList<>();
        if (entrantIds == null) return mockEntrants;

        for (int i = 0; i < entrantIds.size(); i++) {
            String id = entrantIds.get(i);
            String mockName = "Entrant User " + id.substring(0, 4);
            String mockEmail = id.substring(0, 4) + "@example.com";
            String mockPhone = "555-010" + (i + 1);

            Entrant mockEntrant = new Entrant(id, mockName, mockEmail, mockPhone);
            mockEntrants.add(mockEntrant);
        }
        return mockEntrants;
    }

    /**
     * Export the entrants for the currently selected tab (Selected / Enrolled / Cancelled / Waiting)
     * to a CSV file using CSVExporter.
     */
    private void exportCurrentTabToCsv() {
        if (currentEvent == null) {
            Toast.makeText(this,
                    "Event not loaded yet – please try again.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        int position = tabStatus.getSelectedTabPosition();
        List<String> entrantIds;
        String statusLabel;

        switch (position) {
            case 0: // Selected
                entrantIds = currentEvent.getSelected();
                statusLabel = "Selected";
                break;
            case 1: // Enrolled
                entrantIds = currentEvent.getEnrolled();
                statusLabel = "Enrolled";
                break;
            case 2: // Cancelled
                entrantIds = currentEvent.getCancelled();
                statusLabel = "Cancelled";
                break;
            case 3: // Waiting
            default:
                entrantIds = currentEvent.getWaitingList();
                statusLabel = "Waiting";
                break;
        }

        if (entrantIds == null || entrantIds.isEmpty()) {
            Toast.makeText(this,
                    "No " + statusLabel.toLowerCase() + " entrants to export.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Build the same mock entrants used for display
        List<Entrant> entrants = buildMockEntrants(entrantIds);

        CSVExporter exporter = new CSVExporter();
        File csvFile = exporter.exportEntrants(
                this,
                currentEvent.getName() + "_" + statusLabel,
                entrants
        );

        if (csvFile != null) {
            Toast.makeText(this,
                    "CSV exported to:\n" + csvFile.getAbsolutePath(),
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this,
                    "Failed to export CSV.",
                    Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * Set up the click listeners for the three main action buttons.
     */
    private void setupActionListeners() {
        // Export CSV Button Action
        btnExportCsv.setOnClickListener(v -> exportCurrentTabToCsv());

        // View Map Button Action
        btnViewMap.setOnClickListener(v -> {
            // This button now features a map icon
            if (currentEvent != null && currentEvent.getLocation() != null) {
                Toast.makeText(WaitingListActivity.this, "Showing Event Location: " + currentEvent.getLocation(), Toast.LENGTH_LONG).show();
                // Implement logic to start a Map Activity (e.g., Google Maps Intent)
            } else {
                Toast.makeText(WaitingListActivity.this, "Map functionality disabled or location not set.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * RecyclerView Adapter to display Entrant items.
     * This uses a placeholder Entrant view for demonstration.
     */
    private static class EntrantsAdapter extends RecyclerView.Adapter<EntrantsAdapter.EntrantHolder> {
        private List<Entrant> entrants;

        public EntrantsAdapter(List<Entrant> entrants) {
            this.entrants = entrants;
        }

        public void setEntrants(List<Entrant> newEntrants) {
            this.entrants = newEntrants;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public EntrantHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Assumes R.layout.item_entrant exists based on the XML snippet provided
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_entrant, parent, false);
            return new EntrantHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull EntrantHolder holder, int position) {
            Entrant entrant = entrants.get(position);
            holder.bind(entrant);
        }

        @Override
        public int getItemCount() {
            return entrants.size();
        }

        // ViewHolder class for the Entrant item
        class EntrantHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            private final TextView nameTextView;
            private final TextView emailTextView;
            private final TextView statusChipTextView;
            private Entrant boundEntrant;

            public EntrantHolder(@NonNull View itemView) {
                super(itemView);
                // Assume IDs from item_entrant.xml
                nameTextView = itemView.findViewById(R.id.name);
                emailTextView = itemView.findViewById(R.id.email);
                statusChipTextView = itemView.findViewById(R.id.status_chip); // Assuming this is used for the status chip
                itemView.setOnClickListener(this);
            }

            public void bind(Entrant entrant) {
                boundEntrant = entrant;
                nameTextView.setText(entrant.getDisplayName());
                emailTextView.setText(entrant.getEmail());
                // The status will be set externally based on the current tab, but we use the adapter to display the name/email
                statusChipTextView.setText("Status: " + entrant.getId().substring(0, 4)); // Placeholder status
            }

            @Override
            public void onClick(View v) {
                // Handle item click, e.g., open entrant's detailed profile
                Toast.makeText(v.getContext(), "Clicked Entrant: " + boundEntrant.getDisplayName(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}