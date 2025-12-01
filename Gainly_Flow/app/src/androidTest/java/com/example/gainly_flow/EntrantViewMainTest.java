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

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class EntrantViewMainTest {

    @Test
    public void testNavigationButtonsDisplayed() {
        // Launch activity with "fromRoleSwitch" to bypass profile checks
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EntrantViewMain.class);
        intent.putExtra("fromRoleSwitch", true);

        try (ActivityScenario<EntrantViewMain> scenario = ActivityScenario.launch(intent)) {
            // Verify "Browse Events" button
            onView(withId(R.id.browseEventsButton)).check(matches(isDisplayed()));

            // Verify "Lottery Guidelines" button
            onView(withId(R.id.lotteryGuidelinesButton)).check(matches(isDisplayed()));

            // Verify "Event History" button
            onView(withId(R.id.eventHistoryButton)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testEventListContainerDisplayed() {
        // Launch activity with "fromRoleSwitch" to bypass profile checks
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EntrantViewMain.class);
        intent.putExtra("fromRoleSwitch", true);

        try (ActivityScenario<EntrantViewMain> scenario = ActivityScenario.launch(intent)) {
            // Verify event list container
            onView(withId(R.id.eventListContainer)).check(matches(isDisplayed()));
        }
    }
}
