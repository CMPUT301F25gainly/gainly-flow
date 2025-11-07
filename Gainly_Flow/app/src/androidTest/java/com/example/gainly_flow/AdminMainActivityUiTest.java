package com.example.gainly_flow;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Admin dashboard screen:
 *  - Renders rows to manage events and profiles
 *  - Shows counts pulled from private helpers (stubbed to 3)
 * No intent navigation assertions (per assignment).
 */
@RunWith(AndroidJUnit4.class)
public class AdminMainActivityUiTest {

    @Rule
    public ActivityScenarioRule<AdminMainActivity> rule =
            new ActivityScenarioRule<>(AdminMainActivity.class);

    @Test
    public void rendersRowsAndCounts() {
        onView(withId(R.id.rowBrowseEvents)).check(matches(isDisplayed()));
        onView(withId(R.id.rowBrowseProfiles)).check(matches(isDisplayed()));

        onView(withId(R.id.tvTotalEvents)).check(matches(isDisplayed()));
        onView(withId(R.id.tvTotalUsers)).check(matches(isDisplayed()));

        // With current stub helpers both show "3"
        onView(withId(R.id.tvTotalEvents)).check(matches(withText("3")));
        onView(withId(R.id.tvTotalUsers)).check(matches(withText("3")));
    }
}
