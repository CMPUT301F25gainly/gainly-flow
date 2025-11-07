package com.example.gainly_flow;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * UI tests for EntrantViewMain.
 * Note: We do NOT assert activity switches (Browse → QRCodeScanner, bottom nav),
 * per the assignment guidance. We only verify on-screen behavior.
 */
@RunWith(AndroidJUnit4.class)
public class EntrantViewMainUiTest {

    @Rule
    public ActivityScenarioRule<EntrantViewMain> rule =
            new ActivityScenarioRule<>(EntrantViewMain.class);


    @Test
    public void lotteryGuidelines_showsDialog_andDismiss() {
        onView(withId(R.id.lotteryGuidelinesButton)).perform(click());
        // Your AlertDialog uses title "Lottery Guidelines" and positive "OK"
        onView(withText("Lottery Guidelines")).check(matches(isDisplayed()));
        onView(withText("OK")).perform(click());
        // Still on the same screen
        onView(withId(R.id.eventListContainer)).check(matches(isDisplayed()));
    }
}
