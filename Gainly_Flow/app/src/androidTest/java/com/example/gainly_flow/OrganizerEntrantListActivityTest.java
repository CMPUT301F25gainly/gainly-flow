package com.example.gainly_flow;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * OrganizerEntrantListActivity:
 * - Verifies header + primary controls render
 * - Toggles between Waiting/Selected
 * - Tapping "Run Lottery" keeps the screen responsive (no crash)
 *
 * Notes:
 *  * We avoid asserting a RecyclerView because:
 *    - its ID can differ across layouts
 *    - it may be replaced/hidden while loading or empty
 *  * We assert only stable controls that remain in the hierarchy.
 */
@RunWith(AndroidJUnit4.class)
public class OrganizerEntrantListActivityTest {

    private static Intent intentWithEvent() {
        Intent i = new Intent(
                ApplicationProvider.getApplicationContext(),
                OrganizerEntrantListActivity.class
        );
        i.putExtra("eventId", "event-espresso");
        return i;
    }

    @Rule
    public ActivityScenarioRule<OrganizerEntrantListActivity> rule =
            new ActivityScenarioRule<>(intentWithEvent());

    @Test
    public void rendersHeaderAndPrimaryControls() {
        // Always-present views (match your activity code)
        onView(withId(R.id.title)).check(matches(isDisplayed()));
        onView(withId(R.id.toggleGroup)).check(matches(isDisplayed()));
        onView(withId(R.id.btnWaiting)).check(matches(isDisplayed()));
        onView(withId(R.id.btnSelected)).check(matches(isDisplayed()));
        onView(withId(R.id.btnRefresh)).check(matches(isDisplayed()));
        onView(withId(R.id.fabRunLottery)).check(matches(isDisplayed()));

        // Optional: progress/empty (only check if present in layout)
        // onView(withId(R.id.progress)).check(matches(isDisplayed())). // uncomment if needed
        // onView(withId(R.id.empty)).check(matches(isDisplayed()));     // uncomment if needed
    }

    @Test
    public void toggleWaitingAndSelected_keepsControlsVisible() {
        onView(withId(R.id.btnWaiting)).perform(click());
        onView(withId(R.id.toggleGroup)).check(matches(isDisplayed()));

        onView(withId(R.id.btnSelected)).perform(click());
        onView(withId(R.id.toggleGroup)).check(matches(isDisplayed()));
    }

    @Test
    public void runLottery_clickKeepsScreenResponsive_noCrash() {
        onView(withId(R.id.fabRunLottery)).perform(click());

        // Assert the Activity is still alive (not finishing/destroyed).
        rule.getScenario().onActivity(activity -> {
            // activity still running
            org.junit.Assert.assertFalse(activity.isFinishing());
            org.junit.Assert.assertFalse(activity.isDestroyed());
        });

        // Optionally, also assert the scenario is at least STARTED/RESUMED
        // (avoids relying on any particular view being visible).
        androidx.lifecycle.Lifecycle.State state = rule.getScenario().getState();
        org.junit.Assert.assertTrue(
                "Activity should be STARTED or RESUMED after clicking FAB",
                state.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)
        );
    }


}
