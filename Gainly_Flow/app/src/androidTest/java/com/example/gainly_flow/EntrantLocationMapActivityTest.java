package com.example.gainly_flow;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class EntrantLocationMapActivityTest {

    @Test
    public void testActivityLaunch() {
        // Create an intent with necessary extras
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EntrantLocationMapActivity.class);
        intent.putExtra("event_id", "test_event_123");
        intent.putExtra("event_name", "Test Event");

        // Launch the activity with the intent
        try (ActivityScenario<EntrantLocationMapActivity> scenario = ActivityScenario.launch(intent)) {
            // Verify that the map title TextView is displayed (shows event name)
            onView(withId(R.id.tv_map_title)).check(matches(isDisplayed()));
            // Verify it shows the event name we passed
            onView(withId(R.id.tv_map_title)).check(matches(withText("Test Event")));
        }
    }

    @Test
    public void testEntrantCountDisplayed() {
        // Create an intent with necessary extras
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EntrantLocationMapActivity.class);
        intent.putExtra("event_id", "test_event_123");
        intent.putExtra("event_name", "Test Event");

        // Launch the activity with the intent
        try (ActivityScenario<EntrantLocationMapActivity> scenario = ActivityScenario.launch(intent)) {
            // Verify that the entrant count TextView is displayed
            onView(withId(R.id.tv_entrant_count)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testBackButtonDisplayed() {
        // Create an intent with necessary extras
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EntrantLocationMapActivity.class);
        intent.putExtra("event_id", "test_event_123");
        intent.putExtra("event_name", "Test Event");

        // Launch the activity with the intent
        try (ActivityScenario<EntrantLocationMapActivity> scenario = ActivityScenario.launch(intent)) {
            // Verify that the back button is displayed
            onView(withId(R.id.btn_back)).check(matches(isDisplayed()));
        }
    }
}
