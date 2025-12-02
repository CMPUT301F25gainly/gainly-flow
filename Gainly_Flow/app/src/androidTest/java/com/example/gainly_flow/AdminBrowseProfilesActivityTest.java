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
public class AdminBrowseProfilesActivityTest {

    @Rule
    public ActivityScenarioRule<AdminBrowseProfilesActivity> activityRule = new ActivityScenarioRule<>(
            AdminBrowseProfilesActivity.class);

    @Test
    public void testActivityLaunch() {
        // Verify that the title "Browse Profiles" is displayed
        onView(withText("Browse Profiles")).check(matches(isDisplayed()));
    }

    @Test
    public void testSearchBoxDisplayed() {
        // Verify that the search box is displayed
        onView(withId(R.id.etSearchProfiles)).check(matches(isDisplayed()));
    }

    @Test
    public void testProfileListContainerDisplayed() {
        // Verify that the profile list container is displayed
        onView(withId(R.id.profileListContainer)).check(matches(isDisplayed()));
    }

    @Test
    public void testBackButtonDisplayed() {
        // Verify that the back button is displayed
        onView(withId(R.id.btnBack)).check(matches(isDisplayed()));
    }
}
