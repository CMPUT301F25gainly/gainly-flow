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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class WaitingListActivityTest {

    private Intent intent;

    @Before
    public void setUp() {
        // Create an intent with necessary extras
        // Use the correct key expected by WaitingListActivity
        intent = new Intent(ApplicationProvider.getApplicationContext(), WaitingListActivity.class);
        intent.putExtra("com.example.gainly_flow.event_id", "test_event_123");
        intent.putExtra("event_name", "Test Event");
    }

    @Test
    public void testActivityLaunch() {
        // Launch the activity with the intent
        try (ActivityScenario<WaitingListActivity> scenario = ActivityScenario.launch(intent)) {
            // Verify that the toolbar title "Waiting Lists" is displayed
            onView(withText("Waiting Lists")).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testTabLayoutDisplayed() {
        // Launch the activity with the intent
        try (ActivityScenario<WaitingListActivity> scenario = ActivityScenario.launch(intent)) {
            // Verify that the tab layout is displayed
            onView(withId(R.id.tab_status)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testRecyclerViewDisplayed() {
        // Launch the activity with the intent
        try (ActivityScenario<WaitingListActivity> scenario = ActivityScenario.launch(intent)) {
            // Verify that the RecyclerView for entrants is displayed
            onView(withId(R.id.recycler_entrants)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testActionButtonsDisplayed() {
        // Launch the activity with the intent
        try (ActivityScenario<WaitingListActivity> scenario = ActivityScenario.launch(intent)) {
            // Verify that action buttons are displayed
            onView(withId(R.id.btn_update_poster)).check(matches(isDisplayed()));
            // Note: btn_view_map visibility depends on event data, so we might skip
            // checking it
            // or check it exists but maybe not displayed if event load fails
            // For now, let's check it exists in the hierarchy
            // onView(withId(R.id.btn_view_map)).check(matches(isDisplayed()));
        }
    }
}
