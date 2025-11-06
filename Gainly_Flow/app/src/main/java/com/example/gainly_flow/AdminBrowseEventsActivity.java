package com.example.gainly_flow;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AdminBrowseEventsActivity
 * Allows administrators to view and remove events.
 */
public class AdminBrowseEventsActivity extends AppCompatActivity {

    private Administrator admin;
    private ArrayAdapter<Event> adapter;
    private List<Event> allEvents;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_browse_events);
        ImageButton backButton = findViewById(R.id.btnBack);
        backButton.setOnClickListener(v -> finish());
        // Attempt to obtain Administrator instance
        try {
            // If Administrator has a singleton method
            admin = Administrator.get();
        } catch (Throwable t) {
            // Fallback if no singleton method exists
            try {
                admin = Administrator.class.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Administrator instance not available", e);
            }
        }

        ListView lv = findViewById(R.id.lvEvents);
        EditText et = findViewById(R.id.etSearch);

        // Retrieve events from the Database
        allEvents = new ArrayList<>(Database.get().getAllEvents());

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>(allEvents));
        lv.setAdapter(adapter);

        // Text filter for searching events
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) {
                filter(s == null ? "" : s.toString());
            }
        });

        // Long-press an event to remove it (admin action)
        lv.setOnItemLongClickListener((parent, view, position, id) -> {
            Event e = adapter.getItem(position);
            if (e == null) return true;

            new AlertDialog.Builder(this)
                    .setTitle("Remove event?")
                    .setMessage("Are you sure you want to remove \"" + e.getName() + "\"?")
                    .setPositiveButton("Remove", (d, w) -> {
                        // Admin removes the event via Database
                        Database.get().removeEvent(e.getId());
                        allEvents = new ArrayList<>(Database.get().getAllEvents());
                        filter(et.getText() == null ? "" : et.getText().toString());
                        Toast.makeText(this, "Event removed", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });
    }

    private void filter(String query) {
        List<Event> filtered = allEvents;
        if (query != null && !query.trim().isEmpty()) {
            String lower = query.toLowerCase();
            filtered = allEvents.stream()
                    .filter(e -> e.getName().toLowerCase().contains(lower) ||
                            (e.getDescription() != null && e.getDescription().toLowerCase().contains(lower)))
                    .collect(Collectors.toList());
        }
        adapter.clear();
        adapter.addAll(filtered);
        adapter.notifyDataSetChanged();
    }


}
