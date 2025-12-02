package com.example.gainly_flow;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.example.gainly_flow.Profile; // Assuming Profile class is in the same package

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ProfileActivityTest {

    private Intent intent;

    @Before
    public void setUp() {
        // Create a test profile to pass to the activity
        // Use constructor: Profile(String id, String displayName, String email)
        Profile testProfile = new Profile("test_device_123", "Test User", "test@example.com");
        testProfile.setPhone("1234567890");
        testProfile.setRole("Entrant");

        // Create intent with profile data
        intent = new Intent(ApplicationProvider.getApplicationContext(), ProfileActivity.class);
        intent.putExtra("profile", testProfile);
        intent.putExtra("userType", "Entrant");
    }

    @Test
    public void testNameFieldDisplayed() {
        try (ActivityScenario<ProfileActivity> scenario = ActivityScenario.launch(intent)) {
            // Verify that the full name field is displayed
            onView(withId(R.id.edit_full_name)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testEmailFieldDisplayed() {
        try (ActivityScenario<ProfileActivity> scenario = ActivityScenario.launch(intent)) {
            // Verify that the email field is displayed
            onView(withId(R.id.edit_email)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testPhoneFieldDisplayed() {
        try (ActivityScenario<ProfileActivity> scenario = ActivityScenario.launch(intent)) {
            // Verify that the phone number field is displayed
            onView(withId(R.id.edit_phone_number)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testUpdateProfileButtonDisplayed() {
        try (ActivityScenario<ProfileActivity> scenario = ActivityScenario.launch(intent)) {
            // Verify that the update profile button is displayed
            onView(withId(R.id.button_update_profile)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testProfileIconDisplayed() {
        try (ActivityScenario<ProfileActivity> scenario = ActivityScenario.launch(intent)) {
            // Verify that the profile icon is displayed
            onView(withId(R.id.profile_icon)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testBottomNavigationDisplayed() {
        try (ActivityScenario<ProfileActivity> scenario = ActivityScenario.launch(intent)) {
            // Verify that the bottom navigation is displayed
            onView(withId(R.id.bottomNav)).check(matches(isDisplayed()));
        }
    }
}
