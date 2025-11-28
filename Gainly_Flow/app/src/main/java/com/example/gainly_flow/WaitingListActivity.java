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
import android.app.AlertDialog;
import android.widget.ImageButton;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.DocumentSnapshot;


/**
 * Activity for organizers to view and manage entrant lists (Selected, Enrolled, Cancelled, Waiting)
 * For a specific event. This activity implements the new vertically stacked button UI.
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
        btnExportCsv = findViewById(R.id.btn_update_poster);
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
    /**
     * Fetch entrant details for the given IDs from Firestore and update the RecyclerView.
     */
    private void updateEntrantList(List<String> entrantIds, String status) {
        if (entrantIds == null || entrantIds.isEmpty()) {
            entrantsAdapter.setEntrants(new ArrayList<>());
            Toast.makeText(this,
                    "No entrants found for status: " + status,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Firestore: load the matching profile docs by their document IDs
        FirebaseFirestore.getInstance()
                .collection("profiles")
                .whereIn(FieldPath.documentId(), entrantIds)  // IDs in your event lists
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Entrant> realEntrants = new ArrayList<>();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String id          = doc.getId();
                        String displayName = doc.getString("displayName");
                        String email       = doc.getString("email");
                        String phone       = doc.getString("phone");  // or null if you don’t store it

                        Entrant e = new Entrant(id, displayName, email, phone);
                        realEntrants.add(e);
                    }

                    entrantsAdapter.setEntrants(realEntrants);

                    Toast.makeText(this,
                            "Displaying " + realEntrants.size() + " " + status + " entrants.",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Failed to load entrants: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
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
            Toast.makeText(
                    this,
                    "No " + statusLabel.toLowerCase() + " entrants to export.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        CSVExporter exporter = new CSVExporter();

        // ✅ get the actual Entrant objects currently shown in the list
        List<Entrant> entrantsToExport = entrantsAdapter.getEntrants();

        if (entrantsToExport == null || entrantsToExport.isEmpty()) {
            Toast.makeText(
                    this,
                    "No " + statusLabel.toLowerCase() + " entrants to export.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        File csvFile = exporter.exportEntrants(
                this,
                currentEvent.getName() + "_" + statusLabel,
                entrantsToExport
        );

        if (csvFile != null) {
            Toast.makeText(
                    this,
                    "CSV exported to:\n" + csvFile.getAbsolutePath(),
                    Toast.LENGTH_LONG
            ).show();
        } else {
            Toast.makeText(
                    this,
                    "Failed to export CSV.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }


    /**
     * Set up the click listeners for the three main action buttons.
     */
    private void setupActionListeners() {
        // Export CSV Button Action
        btnExportCsv.setOnClickListener(v -> exportCurrentTabToCsv());

        // View Map Button Action - Shows where entrants joined the waiting list from
        btnViewMap.setOnClickListener(v -> {
            if (currentEvent != null) {
                // Launch the EntrantLocationMapActivity to show entrant locations
                Intent mapIntent = new Intent(WaitingListActivity.this, EntrantLocationMapActivity.class);
                mapIntent.putExtra("event_id", eventId);
                mapIntent.putExtra("event_name", currentEvent.getName());
                startActivity(mapIntent);
            } else {
                Toast.makeText(WaitingListActivity.this, "Event not loaded yet.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * RecyclerView Adapter to display Entrant items.
     * This uses a placeholder Entrant view for demonstration.
     */
    private class EntrantsAdapter extends RecyclerView.Adapter<EntrantsAdapter.EntrantHolder> {
        private List<Entrant> entrants;

        public EntrantsAdapter(List<Entrant> entrants) {
            this.entrants = entrants;
        }

        public void setEntrants(List<Entrant> newEntrants) {
            this.entrants = newEntrants;
            notifyDataSetChanged();
        }
        public List<Entrant> getEntrants() {
            return new ArrayList<>(entrants); // copy so callers can’t mess with internal list
        }


        @NonNull
        @Override
        public EntrantHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_profile_admin, parent, false);
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
// ViewHolder class for the Entrant item (admin-style card)
        class EntrantHolder extends RecyclerView.ViewHolder {

            private final TextView nameTextView;
            private final TextView emailTextView;
            private final TextView roleTextView;
            private final ImageButton deleteButton;

            private Entrant boundEntrant;

            public EntrantHolder(@NonNull View itemView) {
                super(itemView);
                // IDs from item_profile_admin.xml
                nameTextView  = itemView.findViewById(R.id.profileName);
                emailTextView = itemView.findViewById(R.id.profileEmail);
                roleTextView  = itemView.findViewById(R.id.profileRole);
                deleteButton  = itemView.findViewById(R.id.btnDeleteProfile);
            }

            public void bind(Entrant entrant) {
                boundEntrant = entrant;

                nameTextView.setText(entrant.getDisplayName());
                emailTextView.setText(entrant.getEmail());
                roleTextView.setText("Waiting list entrant"); // or based on tab if you want

                deleteButton.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position == RecyclerView.NO_POSITION) return;

                    Entrant toDelete = entrants.get(position);
                    String entrantId = toDelete.getId();

                    // 0 = Selected, 1 = Enrolled, 2 = Cancelled, 3 = Waiting
                    int tabPos = tabStatus.getSelectedTabPosition();

                    String fromField;
                    boolean moveToCancelled;
                    String dialogTitle;
                    String dialogMessage;

                    switch (tabPos) {
                        case 0: // Selected
                            fromField = "selected";
                            moveToCancelled = true;
                            dialogTitle = "Cancel selected entrant";
                            dialogMessage = "Move \"" + toDelete.getDisplayName()
                                    + "\" from Selected to Cancelled?";
                            break;
                        case 1: // Enrolled
                            fromField = "enrolled";
                            moveToCancelled = true;
                            dialogTitle = "Cancel enrolled entrant";
                            dialogMessage = "Move \"" + toDelete.getDisplayName()
                                    + "\" from Enrolled to Cancelled?";
                            break;
                        case 3: // Waiting
                            fromField = "waitingList";
                            moveToCancelled = true;
                            dialogTitle = "Remove from waiting list";
                            dialogMessage = "Move \"" + toDelete.getDisplayName()
                                    + "\" from Waiting to Cancelled?";
                            break;
                        case 2: // Already in Cancelled
                        default:
                            fromField = "cancelled";
                            moveToCancelled = false; // just remove from cancelled
                            dialogTitle = "Remove from cancelled";
                            dialogMessage = "Remove \"" + toDelete.getDisplayName()
                                    + "\" from Cancelled?";
                            break;
                    }

                    // 🔒 Make final copies for use inside lambdas/callbacks
                    final int fTabPos = tabPos;
                    final String fFromField = fromField;
                    final boolean fMoveToCancelled = moveToCancelled;
                    final String fDialogTitle = dialogTitle;
                    final String fDialogMessage = dialogMessage;
                    final String fEntrantId = entrantId;

                    new AlertDialog.Builder(itemView.getContext())
                            .setTitle(fDialogTitle)
                            .setMessage(fDialogMessage)
                            .setPositiveButton(
                                    fMoveToCancelled ? "Cancel entrant" : "Remove",
                                    (dialog, which) -> {

                                        FirebaseFirestore db = FirebaseFirestore.getInstance();

                                        if (fMoveToCancelled) {
                                            // Remove from current list AND add to cancelled
                                            db.collection("events")
                                                    .document(eventId)
                                                    .update(
                                                            fFromField, FieldValue.arrayRemove(fEntrantId),
                                                            "cancelled", FieldValue.arrayUnion(fEntrantId)
                                                    )
                                                    .addOnSuccessListener(unused -> {
                                                        // Update local Event model
                                                        if (currentEvent != null) {
                                                            List<String> fromList = null;
                                                            switch (fTabPos) {
                                                                case 0: fromList = currentEvent.getSelected(); break;
                                                                case 1: fromList = currentEvent.getEnrolled(); break;
                                                                case 3: fromList = currentEvent.getWaitingList(); break;
                                                            }
                                                            if (fromList != null) {
                                                                fromList.remove(fEntrantId);
                                                            }
                                                            if (currentEvent.getCancelled() != null &&
                                                                    !currentEvent.getCancelled().contains(fEntrantId)) {
                                                                currentEvent.getCancelled().add(fEntrantId);
                                                            }

                                                            updateEventDetails(currentEvent);
                                                        }

                                                        // Remove from current RecyclerView list
                                                        entrants.remove(position);
                                                        notifyItemRemoved(position);

                                                        Toast.makeText(
                                                                itemView.getContext(),
                                                                "Moved to Cancelled.",
                                                                Toast.LENGTH_SHORT
                                                        ).show();
                                                    })
                                                    .addOnFailureListener(e -> Toast.makeText(
                                                            itemView.getContext(),
                                                            "Failed: " + e.getMessage(),
                                                            Toast.LENGTH_LONG
                                                    ).show());
                                        } else {
                                            // We’re already in Cancelled: just remove
                                            db.collection("events")
                                                    .document(eventId)
                                                    .update(
                                                            fFromField, FieldValue.arrayRemove(fEntrantId)
                                                    )
                                                    .addOnSuccessListener(unused -> {
                                                        if (currentEvent != null &&
                                                                currentEvent.getCancelled() != null) {
                                                            currentEvent.getCancelled().remove(fEntrantId);
                                                            updateEventDetails(currentEvent);
                                                        }

                                                        entrants.remove(position);
                                                        notifyItemRemoved(position);

                                                        Toast.makeText(
                                                                itemView.getContext(),
                                                                "Removed from Cancelled.",
                                                                Toast.LENGTH_SHORT
                                                        ).show();
                                                    })
                                                    .addOnFailureListener(e -> Toast.makeText(
                                                            itemView.getContext(),
                                                            "Failed: " + e.getMessage(),
                                                            Toast.LENGTH_LONG
                                                    ).show());
                                        }
                                    }
                            )
                            .setNegativeButton("Keep", null)
                            .show();
                });
            }
        }

    }
}