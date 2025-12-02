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
public class CreateEventTest {

    @Rule
    public ActivityScenarioRule<CreateEvent> activityRule = new ActivityScenarioRule<>(
            CreateEvent.class);

    @Test
    public void testActivityLaunch() {
        // Verify that the toolbar title "Create New Event" is displayed
        onView(withText("Create New Event")).check(matches(isDisplayed()));
    }

    @Test
    public void testEventNameInputDisplayed() {
        // Verify that the event name input field is displayed
        onView(withId(R.id.eventNameInput)).check(matches(isDisplayed()));
    }

    @Test
    public void testEventDescriptionInputDisplayed() {
        // Verify that the event description input field is displayed
        onView(withId(R.id.eventDescriptionInput)).check(matches(isDisplayed()));
    }

    @Test
    public void testCapacityInputDisplayed() {
        // Scroll to the capacity input field since it's lower in the form
        onView(withId(R.id.eventCapacityInput))
                .perform(androidx.test.espresso.action.ViewActions.scrollTo())
                .check(matches(isDisplayed()));
    }

    @Test
    public void testSaveButtonDisplayed() {
        // Scroll to the save button since it's at the bottom of the form
        onView(withId(R.id.saveEventButton))
                .perform(androidx.test.espresso.action.ViewActions.scrollTo())
                .check(matches(isDisplayed()));
    }

    @Test
    public void testBasicInformationSectionDisplayed() {
        // Verify that the basic information section header is displayed
        onView(withText("Basic Information")).check(matches(isDisplayed()));
    }
}
