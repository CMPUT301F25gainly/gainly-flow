package com.example.gainly_flow;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class OrganizerEventActivityTest {

    private String testEventId;
    private FirebaseFirestore db;

    @Before
    public void setUp() throws ExecutionException, InterruptedException {
        db = FirebaseFirestore.getInstance();
        Context context = ApplicationProvider.getApplicationContext();
        String deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

        testEventId = UUID.randomUUID().toString();

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("id", testEventId);
        eventData.put("name", "Test Event");
        eventData.put("organizerId", deviceId);
        eventData.put("waitingList", java.util.Collections.emptyList());
        eventData.put("capacity", 10);
        eventData.put("availableSpots", 10);

        // Create the event in Firestore synchronously
        Tasks.await(db.collection("events").document(testEventId).set(eventData));
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException {
        if (testEventId != null) {
            Tasks.await(db.collection("events").document(testEventId).delete());
        }
    }

    @Test
    public void testEventDetailsDisplayed() {
        // Create an intent with necessary extras
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), OrganizerEventActivity.class);
        intent.putExtra("event_id", testEventId);
        intent.putExtra("event_name", "Test Event");

        // Launch the activity with the intent
        try (ActivityScenario<OrganizerEventActivity> scenario = ActivityScenario.launch(intent)) {
            // Verify that the event name TextView is displayed
            onView(withId(R.id.text_event_name)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testQuickActionsDisplayed() {
        // Create an intent with necessary extras
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), OrganizerEventActivity.class);
        intent.putExtra("event_id", testEventId);
        intent.putExtra("event_name", "Test Event");

        // Launch the activity with the intent
        try (ActivityScenario<OrganizerEventActivity> scenario = ActivityScenario.launch(intent)) {
            // Verify that the "View Waiting List" button is displayed
            onView(withId(R.id.btn_view_waiting_list)).check(matches(isDisplayed()));

            // Verify that the "Update Poster" button is displayed
            onView(withId(R.id.btn_update_poster)).check(matches(isDisplayed()));
        }
    }
}
