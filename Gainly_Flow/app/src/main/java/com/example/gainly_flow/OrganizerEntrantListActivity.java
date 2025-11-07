package com.example.gainly_flow;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class OrganizerEntrantListActivity extends AppCompatActivity {

    private WaitingList waitingList;
    private Event currentEvent;
    private ListView entrantListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_entrantlist);

        String eventId = getIntent().getStringExtra("eventId");
        String eventName = getIntent().getStringExtra("eventName");

        TextView header = findViewById(R.id.txtEventName);
        if (eventName != null) header.setText(eventName);

        if (eventId != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("waiting_lists").document(eventId).get()
                    .addOnSuccessListener(doc -> {
                        @SuppressWarnings("unchecked")
                        List<String> entrants = (List<String>) doc.get("entrants");
                        if (entrants != null) {
                            LinearLayout container = findViewById(R.id.entrantListContainer); // (line ~40)
                            container.removeAllViews();
                            for (String entrant : entrants) {
                                TextView tv = new TextView(this);
                                tv.setText(entrant);
                                tv.setTextSize(16);
                                tv.setPadding(16, 8, 16, 8);
                                container.addView(tv);
                            }
                        }
                    })
                    .addOnFailureListener(e -> Log.e("EntrantList", "Failed to load waiting list", e));
        }
    }

    //kk
    private void updateListView(List<String> entrantIds) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, entrantIds
        );
        entrantListView.setAdapter(adapter);
    }
}