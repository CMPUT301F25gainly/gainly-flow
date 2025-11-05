package com.example.gainly_flow;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gainly_flow.Database;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AdminBrowseEventsActivity extends AppCompatActivity {

    private ArrayAdapter<Event> adapter;
    private List<Event> all;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_browse_events);

        ListView lv = findViewById(R.id.lvEvents);
        EditText et = findViewById(R.id.etSearch);

        all = new ArrayList<>(Database.get().getAllEvents());
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>(all));
        lv.setAdapter(adapter);

        lv.setOnItemLongClickListener((parent, view, position, id) -> {
            Event e = adapter.getItem(position);
            if (e == null) return true;
            new AlertDialog.Builder(this)
                    .setTitle("Remove event?")
                    .setMessage(e.getName())
                    .setPositiveButton("Remove", (d, which) -> {
                        Database.get().removeEvent(e.getId());
                        refresh();
                        Toast.makeText(this, "Event removed", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });

        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private void refresh() {
        all = new ArrayList<>(Database.get().getAllEvents());
        filter(((EditText)findViewById(R.id.etSearch)).getText().toString());
    }

    private void filter(String q) {
        List<Event> filtered = all;
        if (q != null && !q.trim().isEmpty()) {
            String needle = q.toLowerCase();
            filtered = all.stream().filter(e ->
                    e.getName().toLowerCase().contains(needle) ||
                            (e.getDescription() != null && e.getDescription().toLowerCase().contains(needle))
            ).collect(Collectors.toList());
        }
        adapter.clear();
        adapter.addAll(filtered);
        adapter.notifyDataSetChanged();
    }
}
