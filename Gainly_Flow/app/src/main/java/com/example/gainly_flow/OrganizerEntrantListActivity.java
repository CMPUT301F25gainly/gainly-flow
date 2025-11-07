package com.example.gainly_flow;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;

import android.text.InputType;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Organizer-facing screen for browsing entrants of an event.
 * <p>
 * This activity displays two tabs:
 * <ul>
 *     <li><b>Waiting</b>: entrants currently in the waiting list (field {@code entrantIds}).</li>
 *     <li><b>Selected</b>: entrants who were selected by a lottery draw (field {@code selectedIds}).</li>
 * </ul>
 * Data is loaded from Firestore:
 * <ul>
 *     <li><code>waiting_lists/{eventId}</code> document for ID arrays.</li>
 *     <li><code>profiles</code> collection for entrant profile details (via {@link FieldPath#documentId()}).</li>
 * </ul>
 * The floating action button runs a lottery via {@link LotterySystem}, then refreshes both tabs.
 *
 * @see LotterySystem
 * @see FirebaseFirestore
 */
public class OrganizerEntrantListActivity extends AppCompatActivity {

    /** Event identifier whose entrants are displayed. */
    private String eventId;
    /** RecyclerView hosting the entrant rows. */
    private RecyclerView recycler;
    /** Indeterminate progress bar shown while loading. */
    private ProgressBar progress;
    /** Empty-state text shown when there are no rows to display. */
    private TextView empty;
    /** Tab buttons. */
    private MaterialButton btnWaiting, btnSelected, btnRefresh;
    /** Backing list for the adapter. */
    private final List<EntrantRow> data = new ArrayList<>();
    /** RecyclerView adapter for entrant rows. */
    private EntrantAdapter adapter;
    /** Firestore instance for data access. */
    private FirebaseFirestore db;
    /** Current visible mode: {@code "waiting"} or {@code "selected"}. */
    private String currentMode = "waiting";
    /** Reentrancy guards to prevent duplicate loads. */
    private boolean loadingWaiting = false;
    private boolean loadingSelected = false;

    /**
     * Lightweight row model rendered in the list.
     */
    public static class EntrantRow {
        /** Entrant profile document ID. */
        public String id;
        /** Display name of entrant (best-effort from multiple profile fields). */
        public String name;
        /** Entrant email (if available). */
        public String email;
        /** Status label, one of {@code "waiting"} or {@code "selected"}. */
        public String status;

        /**
         * Creates a new row model.
         *
         * @param id     profile document ID
         * @param name   entrant display name
         * @param email  entrant email
         * @param status status text ({@code "waiting"} or {@code "selected"})
         */
        EntrantRow(String id, String name, String email, String status) {
            this.id = id; this.name = name; this.email = email; this.status = status;
        }
    }

    /**
     * Initializes UI, reads the {@code eventId} from the intent extras, configures
     * the list and toggle buttons, and loads the initial "waiting" tab.
     *
     * @param savedInstanceState previously saved state or {@code null}
     */
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_entrantlist);

        eventId = getIntent() != null ? getIntent().getStringExtra("eventId") : null;
        if (eventId == null || eventId.isEmpty()) { finish(); return; }

        db = FirebaseFirestore.getInstance();

        TextView title = findViewById(R.id.title);
        title.setText("Entrants • " + eventId);

        recycler = findViewById(R.id.recycler);
        progress = findViewById(R.id.progress);
        empty = findViewById(R.id.empty);
        MaterialButtonToggleGroup toggle = findViewById(R.id.toggleGroup);
        btnWaiting = findViewById(R.id.btnWaiting);
        btnSelected = findViewById(R.id.btnSelected);
        btnRefresh = findViewById(R.id.btnRefresh);

        com.google.android.material.floatingactionbutton.FloatingActionButton fab =
                findViewById(R.id.fabRunLottery);
        fab.setOnClickListener(v -> promptLotterySizeAndRun());

        adapter = new EntrantAdapter(data);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);
        recycler.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        btnWaiting.setChecked(true);
        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            String mode = (checkedId == R.id.btnSelected) ? "selected" : "waiting";
            if (mode.equals(currentMode)) return;   // avoid redundant loads
            currentMode = mode;
            loadEntrants(mode);
        });

        btnRefresh.setOnClickListener(v -> {
            int checked = toggle.getCheckedButtonId();
            String mode = (checked == R.id.btnSelected) ? "selected" : "waiting";
            loadEntrants(mode);
        });

        loadEntrants("waiting");
    }

    /**
     * Loads entrant profiles for the given mode.
     * <p>
     * For <b>waiting</b>, reads {@code entrantIds}; for <b>selected</b>, reads {@code selectedIds}
     * from <code>waiting_lists/{eventId}</code>, then chunks profile lookups against
     * <code>profiles</code> using {@link #fetchProfilesByIds(List, java.util.function.Consumer)}.
     *
     * @param mode {@code "waiting"} or {@code "selected"}
     */
    private void loadEntrants(String mode) {
        boolean isSelected = "selected".equals(mode);

        if (isSelected) {
            if (loadingSelected) return;
            loadingSelected = true;
        } else {
            if (loadingWaiting) return;
            loadingWaiting = true;
        }

        progress.setVisibility(View.VISIBLE);
        showEmpty(false);

        // waiting branch
        if (!isSelected) {
            db.collection("waiting_lists").document(eventId).get()
                    .addOnSuccessListener(doc -> {
                        List<String> ids = (List<String>) doc.get("entrantIds");
                        ids = sanitizeIds(ids);
                        if (ids.isEmpty()) {
                            data.clear(); adapter.notifyDataSetChanged();
                            showEmpty(true); progress.setVisibility(View.GONE);
                            loadingWaiting = false;
                            return;
                        }
                        fetchProfilesByIds(ids, profiles -> {
                            data.clear();
                            for (DocumentSnapshot p : profiles) addRowFromProfile(p, "waiting");
                            adapter.notifyDataSetChanged();
                            showEmpty(data.isEmpty()); progress.setVisibility(View.GONE);
                            loadingWaiting = false;
                        });
                    })
                    .addOnFailureListener(e -> {
                        progress.setVisibility(View.GONE);
                        showEmpty(true);
                        loadingWaiting = false;
                        Toast.makeText(this, "Load waiting failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            return;
        }

        // selected branch
        db.collection("waiting_lists").document(eventId).get()
                .addOnSuccessListener(doc -> {
                    List<String> ids = (List<String>) doc.get("selectedIds");
                    ids = sanitizeIds(ids);
                    if (ids.isEmpty()) {
                        data.clear(); adapter.notifyDataSetChanged();
                        showEmpty(true); progress.setVisibility(View.GONE);
                        loadingSelected = false;
                        return;
                    }
                    fetchProfilesByIds(ids, profiles -> {
                        data.clear();
                        for (DocumentSnapshot p : profiles) addRowFromProfile(p, "selected");
                        adapter.notifyDataSetChanged();
                        showEmpty(data.isEmpty()); progress.setVisibility(View.GONE);
                        loadingSelected = false;
                    });
                })
                .addOnFailureListener(e -> {
                    progress.setVisibility(View.GONE);
                    showEmpty(true);
                    loadingSelected = false;
                    Toast.makeText(this, "Load selected failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Converts a profile document into an {@link EntrantRow} and appends it to {@link #data}.
     * Name and email are chosen from multiple possible fields using {@link #firstNonEmpty(String...)}.
     *
     * @param p      profile document snapshot
     * @param status {@code "waiting"} or {@code "selected"}
     */
    private void addRowFromProfile(DocumentSnapshot p, String status) {
        String id = p.getId();
        String name = firstNonEmpty(
                getStringField(p, "displayName"),
                getStringField(p, "name"),
                getStringField(p, "username"),
                id
        );
        String email = firstNonEmpty(getStringField(p, "email"), "");
        data.add(new EntrantRow(id, name, email, status));
    }

    /**
     * (Legacy helper) Loads waiting entrants specifically from the {@code entrantIds} array.
     * <p>
     * Kept for reference and parity; main flows use {@link #loadEntrants(String)}.
     *
     * @param TAG log tag to use for Android log calls
     */
    private void loadWaitingFromArray(String TAG) {
        db.collection("waiting_lists").document(eventId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        progress.setVisibility(View.GONE);
                        showEmpty(true);
                        android.util.Log.d(TAG, "waiting_lists/" + eventId + " does not exist");
                        return;
                    }

                    List<String> ids = (List<String>) doc.get("entrantIds");
                    if (ids == null || ids.isEmpty()) {
                        data.clear();
                        adapter.notifyDataSetChanged();
                        progress.setVisibility(View.GONE);
                        showEmpty(true);
                        android.util.Log.d(TAG, "waiting_lists has empty entrantIds");
                        return;
                    }

                    fetchProfilesByIds(ids, profiles -> {
                        data.clear();
                        for (DocumentSnapshot p : profiles) {
                            String id = p.getId();
                            String name = firstNonEmpty(
                                    getStringField(p, "name"),
                                    getStringField(p, "displayName"),
                                    getStringField(p, "username"),
                                    id
                            );
                            String email = firstNonEmpty(
                                    getStringField(p, "email"),
                                    getStringField(p, "mail"),
                                    ""
                            );
                            data.add(new EntrantRow(id, name, email, "waiting"));
                        }
                        adapter.notifyDataSetChanged();
                        progress.setVisibility(View.GONE);
                        showEmpty(data.isEmpty());
                        android.util.Log.d(TAG, "Loaded " + data.size() + " entrants from waiting_lists/" + eventId);
                    });
                })
                .addOnFailureListener(e -> {
                    progress.setVisibility(View.GONE);
                    showEmpty(true);
                    android.util.Log.e(TAG, "waiting_lists fetch failed: " + e.getMessage(), e);
                    Toast.makeText(this, "Load failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Fetches profile documents in chunks using {@code whereIn(documentId())} queries.
     * <p>
     * Because Firestore {@code whereIn} has a per-query limit (commonly 10 or 30),
     * this method chunks the input list to size 10 by default and aggregates results.
     *
     * @param ids    list of profile document IDs to fetch
     * @param onDone callback invoked with the aggregated {@link DocumentSnapshot} list
     */
    private void fetchProfilesByIds(List<String> ids, final java.util.function.Consumer<List<DocumentSnapshot>> onDone) {
        final List<DocumentSnapshot> out = new ArrayList<>();
        final int chunkSize = 10;
        final int[] remaining = { (int) Math.ceil(ids.size() / (double) chunkSize) };

        for (int i = 0; i < ids.size(); i += chunkSize) {
            List<String> chunk = ids.subList(i, Math.min(i + chunkSize, ids.size()));
            db.collection("profiles")
                    .whereIn(FieldPath.documentId(), chunk)
                    .get()
                    .addOnSuccessListener(snap -> {
                        out.addAll(snap.getDocuments());
                        if (--remaining[0] == 0) onDone.accept(out);
                    })
                    .addOnFailureListener(e -> {
                        // If one chunk fails, still try to return what we have
                        if (--remaining[0] == 0) onDone.accept(out);
                    });
        }
    }

    /**
     * Returns the first non-empty string from the provided arguments.
     *
     * @param vals candidate strings
     * @return the first non-empty value, or an empty string if none found
     */
    private static String firstNonEmpty(String... vals) {
        for (String v : vals) if (v != null && !v.isEmpty()) return v;
        return "";
    }

    /**
     * Reads a string-like field from a Firestore document, supporting raw strings
     * and map payloads that store the value under a {@code "value"} key.
     *
     * @param doc Firestore document
     * @param key field name
     * @return string value (never {@code null})
     */
    private static String getStringField(DocumentSnapshot doc, String key) {
        Object o = doc.get(key);
        if (o instanceof String) return (String) o;
        if (o instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) o;
            Object v = map.get("value");
            return v == null ? "" : String.valueOf(v);
        }
        return o != null ? String.valueOf(o) : "";
    }

    /**
     * Sanitizes a list of IDs by trimming, removing empties and duplicates while preserving order.
     *
     * @param in raw ID list (may be {@code null})
     * @return sanitized, de-duplicated list
     */
    private static List<String> sanitizeIds(List<String> in) {
        java.util.LinkedHashSet<String> set = new java.util.LinkedHashSet<>();
        if (in != null) {
            for (String s : in) {
                if (s != null) {
                    String t = s.trim();
                    if (!t.isEmpty()) set.add(t);
                }
            }
        }
        return new java.util.ArrayList<>(set);
    }

    /**
     * RecyclerView adapter that renders {@link EntrantRow} items.
     */
    static class EntrantAdapter extends RecyclerView.Adapter<EntrantAdapter.VH> {
        private final List<EntrantRow> items;

        /**
         * Creates an adapter with a backing list reference.
         *
         * @param items mutable list of rows to display
         */
        EntrantAdapter(List<EntrantRow> items) { this.items = items; }

        /** {@inheritDoc} */
        @NonNull
        @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_entrant, parent, false);
            return new VH(v);
        }

        /** {@inheritDoc} */
        @Override public void onBindViewHolder(@NonNull VH h, int position) {
            EntrantRow row = items.get(position);
            h.name.setText(row.name == null || row.name.isEmpty() ? "(Unnamed)" : row.name);
            h.email.setText(row.email);
            h.status.setText(cap(row.status));
        }

        /** {@inheritDoc} */
        @Override public int getItemCount() { return items.size(); }

        /**
         * View holder for entrant rows.
         */
        static class VH extends RecyclerView.ViewHolder {
            ImageView avatar;
            TextView name, email, status;

            /**
             * Binds view references for a row.
             *
             * @param itemView inflated row view
             */
            VH(@NonNull View itemView) {
                super(itemView);
                avatar = itemView.findViewById(R.id.avatar);
                name = itemView.findViewById(R.id.name);
                email = itemView.findViewById(R.id.email);
                status = itemView.findViewById(R.id.status_chip);
            }
        }
    }

    /**
     * Capitalizes the first character of a string, or returns an empty string if null/empty.
     *
     * @param s input string
     * @return capitalized string or empty
     */
    private static String cap(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    /**
     * Toggles the empty-state and list visibility.
     *
     * @param show {@code true} to show the empty view, {@code false} to show the list
     */
    private void showEmpty(boolean show) {
        empty.setVisibility(show ? View.VISIBLE : View.GONE);
        recycler.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    /**
     * Prompts the organizer for a draw size and, if valid, runs the lottery.
     * <p>
     * The value is clamped downstream by {@link LotterySystem}, and an empty input
     * defaults to {@link Integer#MAX_VALUE} to represent "as many as possible".
     */
    private void promptLotterySizeAndRun() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("How many to draw?");

        new AlertDialog.Builder(this)
                .setTitle("Run lottery & notify")
                .setMessage("Enter how many entrants to randomly select. We'll clamp to capacity and waiting size.")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Run", (d, w) -> {
                    int requested;
                    try {
                        String s = input.getText().toString().trim();
                        requested = s.isEmpty() ? Integer.MAX_VALUE : Integer.parseInt(s);
                        if (requested <= 0) throw new NumberFormatException();
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Enter a positive number", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    runLotteryNow(requested);
                })
                .show();
    }

    /**
     * Executes the lottery via {@link LotterySystem#runLottery(String, int, LotterySystem.DrawCallback)}
     * and updates UI state and data on completion.
     *
     * @param requested requested number of entrants to draw (will be clamped)
     */
    private void runLotteryNow(int requested) {
        progress.setVisibility(View.VISIBLE);
        findViewById(R.id.fabRunLottery).setEnabled(false);

        LotterySystem.runLottery(eventId, requested, new LotterySystem.DrawCallback() {
            @Override public void onSuccess(List<String> winners, int before, int after) {
                progress.setVisibility(View.GONE);
                findViewById(R.id.fabRunLottery).setEnabled(true);
                Toast.makeText(OrganizerEntrantListActivity.this,
                        "Selected " + winners.size() + " entrants. Slots left: " + after,
                        Toast.LENGTH_LONG).show();

                // Refresh both tabs
                loadEntrants("waiting");
                loadEntrants("selected");
            }

            @Override public void onFailure(Exception e) {
                progress.setVisibility(View.GONE);
                findViewById(R.id.fabRunLottery).setEnabled(true);
                Toast.makeText(OrganizerEntrantListActivity.this,
                        "Lottery failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
