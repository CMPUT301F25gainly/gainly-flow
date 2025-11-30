package com.example.gainly_flow;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * UI smoke tests for entrant browsing/filtering and waiting-list join/leave
 * visibility.
 * Uses injected in-memory events to avoid network/Firestore dependency.
 */
@RunWith(AndroidJUnit4.class)
public class EntrantFlowTest {

    private ActivityScenario<EntrantViewMain> scenario;

    @Before
    public void setUp() {
        Intent intent = new Intent(
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getTargetContext(),
                EntrantViewMain.class);
        intent.putExtra("fromRoleSwitch", true); // skip profile redirect
        scenario = ActivityScenario.launch(intent);
    }

    @After
    public void tearDown() {
        if (scenario != null) {
            scenario.close();
        }
    }

    @Test
    public void filterOpenAndSearch_reactsToInjectedEvents() {
        scenario.onActivity(activity -> {
            List<Event> events = new ArrayList<>();
            events.add(makeEvent("1", "Music Fest", Event.Category.MUSIC, daysFromNow(1), true));
            events.add(makeEvent("2", "Closed Sport", Event.Category.SPORT, daysFromNow(-1), false));

            setField(activity, "allEvents", events);
            callApplyFilters(activity);
        });

        // Both events visible initially
        onView(withText("Music Fest")).perform(scrollTo()).check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
        onView(withText("Closed Sport")).perform(scrollTo())
                .check(matches(withEffectiveVisibility(Visibility.VISIBLE)));

        // Search narrows to music
        onView(withId(R.id.searchInput)).perform(replaceText("music"), closeSoftKeyboard());
        onView(withText("Music Fest")).perform(scrollTo()).check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
        onView(withText("Closed Sport")).check(doesNotExist());

        // Clear search and filter to OPEN only
        onView(withId(R.id.searchInput)).perform(replaceText(""), closeSoftKeyboard());
        // Show filter options before tapping radio buttons
        onView(withId(R.id.filterToggleButton)).perform(click());
        onView(withId(R.id.radioOpen)).perform(click());
        onView(withId(R.id.applyFiltersButton)).perform(click());

        onView(withText("Music Fest")).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withText("Closed Sport")).check(doesNotExist());
    }

    // Helpers
    private static Event makeEvent(String id, String name, Event.Category cat, Date date, boolean open) {
        Event e = new Event(id);
        e.setName(name);
        e.setCategory(cat);
        e.setCapacity(10);
        e.setCurrentParticipants(open ? 0 : 10); // closed if full
        e.setWaitingList(new ArrayList<>());
        e.setActive(true);
        e.setEventDate(date);

        // Registration window
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        e.setRegistrationOpen(cal.getTime());
        cal.add(Calendar.DAY_OF_YEAR, open ? 2 : -2);
        e.setRegistrationClose(cal.getTime());

        return e;
    }

    private static Date daysFromNow(int days) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, days);
        return c.getTime();
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void callApplyFilters(EntrantViewMain activity) {
        try {
            Method m = EntrantViewMain.class.getDeclaredMethod("applyFilters");
            m.setAccessible(true);
            m.invoke(activity);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
