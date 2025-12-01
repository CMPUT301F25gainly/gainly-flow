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

import static org.hamcrest.Matchers.not;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AdminDashboardTest {

    @Rule
    public ActivityScenarioRule<AdminMainActivity> activityRule = new ActivityScenarioRule<>(AdminMainActivity.class);

    @Test
    public void testTotalEventsCountDisplay() {
        // Verify that the total events TextView is displayed
        onView(withId(R.id.tvTotalEvents)).check(matches(isDisplayed()));

        // Optionally, check that it's not empty (it might be "-" initially or a number)
        onView(withId(R.id.tvTotalEvents)).check(matches(not(withText(""))));
    }

    @Test
    public void testTotalUsersCountDisplay() {
        // Verify that the total users TextView is displayed
        onView(withId(R.id.tvTotalUsers)).check(matches(isDisplayed()));

        // Optionally, check that it's not empty
        onView(withId(R.id.tvTotalUsers)).check(matches(not(withText(""))));
    }
}
