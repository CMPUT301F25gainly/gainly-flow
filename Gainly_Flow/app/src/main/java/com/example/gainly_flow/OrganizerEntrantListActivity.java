package com.example.gainly_flow;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class OrganizerEntrantListActivity extends AppCompatActivity {

    private WaitingList waitingList;
    private Event currentEvent;
    private ListView entrantListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_entrantlist);

        ListView entrantListView = findViewById(R.id.entrantList);   // <- must match XML id
        findViewById(R.id.waiting);
        findViewById(R.id.Selected);
        findViewById(R.id.sendMsg);
        // Get eventId from intent; fallback for manual testing
        String eventId = getIntent().getStringExtra("eventId");
        if (eventId == null || eventId.isEmpty()) {
            eventId = "event123";
        }

        currentEvent = new Event();                         // <- no-arg constructor
        currentEvent.load(eventId, event -> {
            // event is the loaded Event (same as currentEvent after load)
            waitingList = new WaitingList(event.getEventId());

            // Initial load of waiting list
            waitingList.load(list -> updateListView(list.getEntrants()));

            // Wire buttons AFTER we have an event id
            findViewById(R.id.waiting).setOnClickListener(v ->
                    waitingList.load(list -> updateListView(list.getEntrants()))
            );

            findViewById(R.id.Selected).setOnClickListener(v ->
                    LotterySystem.loadSelected(event.getEventId(), this::updateListView)
            );

            findViewById(R.id.sendMsg).setOnClickListener(v ->
                    LotterySystem.loadSelected(event.getEventId(), selected -> {
                        NotificationManager nm = new NotificationManager();
                        nm.notifySelected(selected, event.getEventId());
                    })
            );
        });

        // inside onCreate, after you resolved eventId from the Intent
        final String requestedEventId = eventId;

        currentEvent = new Event();
        currentEvent.load(requestedEventId, evt -> {
            // Use loaded id if present; otherwise fall back to the requested id
            String eid = (evt != null && evt.getEventId() != null && !evt.getEventId().isEmpty())
                    ? evt.getEventId()
                    : requestedEventId;

            waitingList = new WaitingList(eid);

            // Initial load
            waitingList.load(list -> updateListView(list.getEntrants()));

            // Buttons
            findViewById(R.id.waiting).setOnClickListener(v ->
                    waitingList.load(l -> updateListView(l.getEntrants()))
            );

            findViewById(R.id.Selected).setOnClickListener(v ->
                    LotterySystem.loadSelected(eid, this::updateListView)
            );

            findViewById(R.id.sendMsg).setOnClickListener(v ->
                    LotterySystem.loadSelected(eid, selected -> {
                        NotificationManager nm = new NotificationManager();
                        nm.notifySelected(selected, eid);
                    })
            );
        });
    }
    //k
    private void updateListView(List<String> entrantIds) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, entrantIds
        );
        entrantListView.setAdapter(adapter);
    }
}
