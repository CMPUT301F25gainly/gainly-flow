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
public class AdminNotificationLogsActivityTest {

    @Rule
    public ActivityScenarioRule<AdminNotificationLogsActivity> activityRule = new ActivityScenarioRule<>(
            AdminNotificationLogsActivity.class);

    @Test
    public void testActivityLaunch() {
        // Verify that the title "Notification Logs" is displayed
        onView(withText("Notification Logs")).check(matches(isDisplayed()));
    }

    @Test
    public void testLogCountDisplayed() {
        // Verify that the log count TextView is displayed
        onView(withId(R.id.tv_log_count)).check(matches(isDisplayed()));
    }

    @Test
    public void testRecyclerViewDisplayed() {
        // Verify that the RecyclerView for logs is displayed
        onView(withId(R.id.recycler_logs)).check(matches(isDisplayed()));
    }

    @Test
    public void testBackButtonDisplayed() {
        // Verify that the back button is displayed
        onView(withId(R.id.btn_back)).check(matches(isDisplayed()));
    }
}
