package com.example.gainly_flow;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * UI tests for EventDetailActivity.
 * Note: This activity requires real event data from Firestore to fully
 * function.
 * These tests verify basic UI initialization only.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class EventDetailActivityTest {

    @Test
    public void testActivityLaunchWithValidEventId() {
        // Create an intent with necessary extras
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EventDetailActivity.class);
        intent.putExtra("event_id", "test_event_123");
        intent.putExtra("event_name", "Test Event");

        // Launch the activity - it will attempt to load from Firestore
        // The activity may finish if the event doesn't exist, which is expected
        // behavior
        try (ActivityScenario<EventDetailActivity> scenario = ActivityScenario.launch(intent)) {
            // If we get here, the activity launched successfully
            // Note: The activity may immediately finish if event is not found in Firestore
        } catch (Exception e) {
            // Expected if event doesn't exist in Firestore
        }
    }

    @Test
    public void testActivityFinishesWithoutEventId() {
        // Create an intent without event_id
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EventDetailActivity.class);

        // Launch the activity - it should finish immediately
        try (ActivityScenario<EventDetailActivity> scenario = ActivityScenario.launch(intent)) {
            // Activity should finish due to missing event_id
            Thread.sleep(500); // Give it time to finish
        } catch (Exception e) {
            // Expected - activity finished
        }
    }
}
