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
public class AdminBrowseImagesActivityTest {

    @Rule
    public ActivityScenarioRule<AdminBrowseImagesActivity> activityRule = new ActivityScenarioRule<>(
            AdminBrowseImagesActivity.class);

    @Test
    public void testActivityLaunch() {
        // Verify that the title "Browse Images" is displayed
        onView(withText("Browse Images")).check(matches(isDisplayed()));
    }

    @Test
    public void testImageCountDisplayed() {
        // Verify that the image count TextView is displayed
        onView(withId(R.id.tv_image_count)).check(matches(isDisplayed()));
    }

    @Test
    public void testRecyclerViewDisplayed() {
        // Verify that the RecyclerView for images is displayed
        onView(withId(R.id.recycler_images)).check(matches(isDisplayed()));
    }

    @Test
    public void testBackButtonDisplayed() {
        // Verify that the back button is displayed
        onView(withId(R.id.btn_back)).check(matches(isDisplayed()));
    }
}
