package com.example.gainly_flow;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AdminBrowseEventsTest {

    @Rule
    public ActivityScenarioRule<AdminBrowseEventsActivity> activityRule = new ActivityScenarioRule<>(
            AdminBrowseEventsActivity.class);

    @Test
    public void testActivityLaunch() {
        // Verify that the title "Browse Events" is displayed
        onView(withText("Browse Events")).check(matches(isDisplayed()));

        // Verify that the search box is displayed
        onView(withId(R.id.etSearch)).check(matches(isDisplayed()));
    }
}
