package com.example.gainly_flow;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.not;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * UI tests for NotificationsActivity.
 * Note: The RecyclerView is hidden when there are no notifications (shows empty
 * state instead).
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class NotificationsActivityTest {

    @Rule
    public ActivityScenarioRule<NotificationsActivity> activityRule = new ActivityScenarioRule<>(
            NotificationsActivity.class);

    @Test
    public void testActivityLaunch() {
        // Verify that the title is displayed (using ID to avoid ambiguity with bottom
        // nav)
        onView(withId(R.id.titleText)).check(matches(isDisplayed()));
        onView(withId(R.id.titleText)).check(matches(withText("Notifications")));
    }

    @Test
    public void testRecyclerViewExists() {
        // Verify that the RecyclerView exists (it may be hidden if there are no
        // notifications, or visible if there are)
        // We check that it matches either isDisplayed() or not(isDisplayed()), which
        // effectively just checks existence in hierarchy
        onView(withId(R.id.recyclerViewNotifications)).check(matches(anyOf(isDisplayed(), not(isDisplayed()))));
    }

    @Test
    public void testBackButtonDisplayed() {
        // Verify that the back button is displayed
        onView(withId(R.id.backButton_notification)).check(matches(isDisplayed()));
    }

    @Test
    public void testBottomNavigationDisplayed() {
        // Verify that the bottom navigation is displayed
        onView(withId(R.id.bottomNav)).check(matches(isDisplayed()));
    }
}
