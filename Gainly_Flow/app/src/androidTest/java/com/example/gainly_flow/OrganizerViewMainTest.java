package com.example.gainly_flow;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class OrganizerViewMainTest {

    @Rule
    public ActivityScenarioRule<OrganizerViewMain> activityRule = new ActivityScenarioRule<>(OrganizerViewMain.class);

    @Test
    public void testCreateEventButtonDisplayed() {
        // Verify that the "Create Event" button is displayed
        onView(withId(R.id.createEventButton)).check(matches(isDisplayed()));
    }

    @Test
    public void testEventListContainerDisplayed() {
        // Verify that the event list container is displayed
        onView(withId(R.id.eventListContainer)).check(matches(isDisplayed()));
    }
}
