package com.example.gainly_flow;

 // ← replace with your package

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AdminBrowseProfilesActivity extends AppCompatActivity {

    private ArrayAdapter<Profile> adapter;
    private List<Profile> all;
    private Administrator admin; // ← use Administrator hook

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_browse_profiles);

        // Obtain Administrator instance (singleton preferred; fallback shown)
        try {
            // If you have Administrator.get()
            admin = Administrator.get();
        } catch (Throwable t) {
            // Fallback if your class doesn't use a singleton pattern
            try {
                admin = Administrator.class.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Administrator instance not available", e);
            }
        }

        ListView lv = findViewById(R.id.lvProfiles);
        EditText et = findViewById(R.id.etSearchProfiles);

        all = new ArrayList<>(Database.get().getAllProfiles());
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>(all));
        lv.setAdapter(adapter);

        // Long-press to REMOVE profile via Administrator hook
        lv.setOnItemLongClickListener((parent, view, position, id) -> {
            Profile p = adapter.getItem(position);
            if (p == null) return true;
            new AlertDialog.Builder(this)
                    .setTitle("Remove profile?")
                    .setMessage(p.getDisplayName())
                    .setPositiveButton("Remove", (d, which) -> {
                        // Route through Administrator (US 03.02.01)
                        admin.removeProfile(p.getId());
                        refresh();
                        Toast.makeText(this, "Profile removed", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });

        // Simple filter for browsing (US 03.05.01)
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filter(s.toString()); }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private void refresh() {
        all = new ArrayList<>(Database.get().getAllProfiles());
        filter(((EditText)findViewById(R.id.etSearchProfiles)).getText().toString());
    }

    private void filter(String q) {
        List<Profile> filtered = all;
        if (q != null && !q.trim().isEmpty()) {
            String needle = q.toLowerCase();
            filtered = all.stream().filter(p ->
                    p.getDisplayName().toLowerCase().contains(needle) ||
                            (p.getEmail() != null && p.getEmail().toLowerCase().contains(needle))
            ).collect(Collectors.toList());
        }
        adapter.clear();
        adapter.addAll(filtered);
        adapter.notifyDataSetChanged();
    }
}

