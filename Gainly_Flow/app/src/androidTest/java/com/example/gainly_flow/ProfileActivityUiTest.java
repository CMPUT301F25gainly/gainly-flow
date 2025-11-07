package com.example.gainly_flow;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Profile editing flow (entrant):
 *  - Fields render
 *  - User can type new values and tap "Update"
 *  - Switches can be toggled
 * DB side effects are not asserted; we verify UI state only.
 */
@RunWith(AndroidJUnit4.class)
public class ProfileActivityUiTest {

    @Rule
    public ActivityScenarioRule<ProfileActivity> rule =
            new ActivityScenarioRule<>(ProfileActivity.class);

    @Test
    public void fieldsRender() {
        onView(withId(R.id.profile_icon)).check(matches(isDisplayed()));
        onView(withId(R.id.back_button_profile)).check(matches(isDisplayed()));
        onView(withId(R.id.edit_full_name)).check(matches(isDisplayed()));
        onView(withId(R.id.edit_email)).check(matches(isDisplayed()));
        onView(withId(R.id.edit_phone_number)).check(matches(isDisplayed()));
        onView(withId(R.id.switch_notifications)).check(matches(isDisplayed()));
        onView(withId(R.id.switch_location)).check(matches(isDisplayed()));
        onView(withId(R.id.button_update_profile)).check(matches(isDisplayed()));
        onView(withId(R.id.button_delete_account)).check(matches(isDisplayed()));
    }

    @Test
    public void updateProfile_editsValues_andClicksUpdate() {
        onView(withId(R.id.edit_full_name)).perform(clearText(), replaceText("Alice A."), closeSoftKeyboard());
        onView(withId(R.id.edit_email)).perform(clearText(), replaceText("alice@example.com"), closeSoftKeyboard());
        onView(withId(R.id.edit_phone_number)).perform(clearText(), replaceText("1234567890"), closeSoftKeyboard());

        // toggle switches (state-independent)
        onView(withId(R.id.switch_notifications)).perform(click());
        onView(withId(R.id.switch_location)).perform(click());

        onView(withId(R.id.button_update_profile)).perform(click());

        // After clicking update, the fields still show what we typed.
        onView(withId(R.id.edit_full_name)).check(matches(withText("Alice A.")));
        onView(withId(R.id.edit_email)).check(matches(withText("alice@example.com")));
        onView(withId(R.id.edit_phone_number)).check(matches(withText("1234567890")));
    }
}
