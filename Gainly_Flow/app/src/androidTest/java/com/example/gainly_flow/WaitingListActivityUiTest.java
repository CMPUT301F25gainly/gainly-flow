package com.example.gainly_flow;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Basic UI sanity for WaitingListActivity (organizer entrant list).
 * This avoids backend dependencies by only asserting that the key controls render
 * when launched with a dummy event ID.
 */
@RunWith(AndroidJUnit4.class)
public class WaitingListActivityUiTest {

    private static Intent intentWithFakeEvent() {
        Context ctx = ApplicationProvider.getApplicationContext();
        return WaitingListActivity.newIntent(ctx, "test-event-id");
    }

    @Rule
    public ActivityScenarioRule<WaitingListActivity> rule =
            new ActivityScenarioRule<>(intentWithFakeEvent());

    @Test
    public void rendersToolbarTabsAndActions() {
        onView(withId(R.id.toolbar_entrant_lists)).check(matches(isDisplayed()));
        onView(withId(R.id.text_event_name)).check(matches(isDisplayed()));
        onView(withId(R.id.tab_status)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_update_poster)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_view_map)).check(matches(isDisplayed()));
        onView(withId(R.id.recycler_entrants)).check(matches(isDisplayed()));
    }
}
