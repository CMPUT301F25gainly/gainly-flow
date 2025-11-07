package com.example.gainly_flow;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE;

import android.content.Intent;
import android.widget.EditText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Combined UI tests for:
 *  - AdminBrowseEventsActivity
 *  - AdminBrowseProfilesActivity
 *
 * We assert stable, on-screen behavior only (no DB effects, no simple activity switches).
 */
@RunWith(AndroidJUnit4.class)
public class AdminBrowseUiTest {

    /** AdminBrowseEventsActivity: list container is present. */
    @Test
    public void adminBrowseEvents_rendersEventListContainer() {
        Intent events = new Intent(
                ApplicationProvider.getApplicationContext(),
                AdminBrowseEventsActivity.class
        );
        try (ActivityScenario<AdminBrowseEventsActivity> ignored =
                     ActivityScenario.launch(events)) {
            onView(withId(R.id.eventListContainer))
                    .check(matches(withEffectiveVisibility(VISIBLE)));
        }
    }


}
