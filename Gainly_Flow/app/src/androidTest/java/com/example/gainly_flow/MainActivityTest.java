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
public class MainActivityTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(
            MainActivity.class);

    @Test
    public void testActivityLaunch() {
        // Verify that the title "Event Lottery System" is displayed
        onView(withText("Event Lottery System")).check(matches(isDisplayed()));
    }

    @Test
    public void testEntrantButtonDisplayed() {
        // Verify that the entrant button is displayed
        onView(withId(R.id.entrantButton)).check(matches(isDisplayed()));
    }

    @Test
    public void testOrganizerButtonDisplayed() {
        // Verify that the organizer button is displayed
        onView(withId(R.id.organizerButton)).check(matches(isDisplayed()));
    }

    @Test
    public void testRoleSelectionCardsDisplayed() {
        // Verify that role selection cards with descriptions are displayed
        onView(withText("Join Events")).check(matches(isDisplayed()));
        onView(withText("Manage Events")).check(matches(isDisplayed()));
    }
}
