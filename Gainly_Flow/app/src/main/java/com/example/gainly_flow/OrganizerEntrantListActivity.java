package com.example.gainly_flow;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.firestore.FieldPath;


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
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OrganizerEntrantListActivity extends AppCompatActivity {

    private String eventId;
    private RecyclerView recycler;
    private ProgressBar progress;
    private TextView empty;
    private MaterialButton btnWaiting, btnSelected, btnRefresh;
    private final List<EntrantRow> data = new ArrayList<>();
    private EntrantAdapter adapter;
    private FirebaseFirestore db;

    public static class EntrantRow {
        public String id, name, email, status;
        EntrantRow(String id, String name, String email, String status) {
            this.id = id; this.name = name; this.email = email; this.status = status;
        }
    }

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

        adapter = new EntrantAdapter(data);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);
        recycler.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        btnWaiting.setChecked(true);
        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            loadEntrants(checkedId == R.id.btnSelected ? "selected" : "waiting");
        });

        btnRefresh.setOnClickListener(v -> {
            int checked = toggle.getCheckedButtonId();
            String mode = (checked == R.id.btnSelected) ? "selected" : "waiting";
            loadEntrants(mode);
        });

        loadEntrants("waiting");
    }

    private void loadEntrants(String mode) {
        final String TAG = "OrganizerEntrantList";
        progress.setVisibility(View.VISIBLE);
        showEmpty(false);

        // 1) Try original subcollection path (kept for compatibility)
        db.collection("events").document(eventId).collection(mode)
                .orderBy("joinedAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        data.clear();
                        for (DocumentSnapshot doc : snap) {
                            String id = doc.getId();
                            String name = getStringField(doc, "name");
                            String email = getStringField(doc, "email");
                            data.add(new EntrantRow(id, name, email, mode));
                        }
                        data.clear();
                        adapter.notifyDataSetChanged();
                        showEmpty(true);
                        progress.setVisibility(View.GONE);
                        showEmpty(data.isEmpty());
                        android.util.Log.d(TAG, "Loaded from events/" + eventId + "/" + mode + ": " + data.size());
                        return;
                    }

                    // 2) Fallback to your actual schema
                    if ("waiting".equals(mode)) {
                        loadWaitingFromArray(TAG);
                    } else {
                        // If you later store selectedIds, mirror the waiting loader with that field.
                        progress.setVisibility(View.GONE);
                        showEmpty(true);
                        android.util.Log.d(TAG, "No 'selected' data source found for event " + eventId);
                    }
                })
                .addOnFailureListener(e -> {
                    progress.setVisibility(View.GONE);
                    showEmpty(true);
                    android.util.Log.e(TAG, "Primary load failed: " + e.getMessage(), e);
                    Toast.makeText(this, "Load failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadWaitingFromArray(String TAG) {
        db.collection("waiting_lists").document(eventId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        progress.setVisibility(View.GONE);
                        showEmpty(true);
                        android.util.Log.d(TAG, "waiting_lists/" + eventId + " does not exist");
                        return;
                    }

                    // entrantIds is an array of strings
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

    // Firestore whereIn has a per-query limit (commonly 10 or 30).
// We'll chunk to 10 to be safe across devices/SDKs.
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

    private static String firstNonEmpty(String... vals) {
        for (String v : vals) if (v != null && !v.isEmpty()) return v;
        return "";
    }


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

    static class EntrantAdapter extends RecyclerView.Adapter<EntrantAdapter.VH> {
        private final List<EntrantRow> items;
        EntrantAdapter(List<EntrantRow> items) { this.items = items; }

        @NonNull
        @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_entrant, parent, false);
            return new VH(v);
        }

        @Override public void onBindViewHolder(@NonNull VH h, int position) {
            EntrantRow row = items.get(position);
            h.name.setText(row.name == null || row.name.isEmpty() ? "(Unnamed)" : row.name);
            h.email.setText(row.email);
            h.status.setText(cap(row.status));
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            ImageView avatar;
            TextView name, email, status;
            VH(@NonNull View itemView) {
                super(itemView);
                avatar = itemView.findViewById(R.id.avatar);
                name = itemView.findViewById(R.id.name);
                email = itemView.findViewById(R.id.email);
                status = itemView.findViewById(R.id.status_chip);
            }
        }
    }

    private static String cap(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private void showEmpty(boolean show) {
        empty.setVisibility(show ? View.VISIBLE : View.GONE);
        recycler.setVisibility(show ? View.GONE : View.VISIBLE);
    }

}
