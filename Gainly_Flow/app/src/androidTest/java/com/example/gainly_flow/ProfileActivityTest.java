package com.example.gainly_flow;

import static org.junit.Assert.*;

import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Instrumented test for ProfileActivity
 * Tests US 01.02.01 (Create Profile) and US 01.02.02 (Update Profile)
 */
@RunWith(AndroidJUnit4.class)
public class ProfileActivityTest {

    private FirebaseFirestore db;
    private String testDeviceId;

    @Before
    public void setUp() {
        db = FirebaseFirestore.getInstance();
        testDeviceId = Settings.Secure.getString(
                InstrumentationRegistry.getInstrumentation().getTargetContext().getContentResolver(),
                Settings.Secure.ANDROID_ID
        );
    }

    @After
    public void tearDown() {
        // Clean up test data
        if (testDeviceId != null) {
            db.collection("profiles").document(testDeviceId).delete();
        }
    }

    /**
     * Test US 01.02.01 - Create Profile
     * Verify that user can create a new profile with name, email, and optional phone
     */
    @Test
    public void testCreateProfile() throws InterruptedException {
        ActivityScenario<ProfileActivity> scenario = ActivityScenario.launch(ProfileActivity.class);

        scenario.onActivity(activity -> {
            EditText editTextName = activity.findViewById(R.id.editTextName);
            EditText editTextEmail = activity.findViewById(R.id.editTextEmail);
            EditText editTextPhone = activity.findViewById(R.id.editTextPhone);
            Button btnSave = activity.findViewById(R.id.btnSaveProfile);

            // Enter test data
            editTextName.setText("Test User");
            editTextEmail.setText("test@example.com");
            editTextPhone.setText("1234567890");

            // Click save
            btnSave.performClick();
        });

        // Wait for Firebase operation
        Thread.sleep(3000);

        // Verify profile was saved to Firestore
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] testPassed = {false};

        db.collection("profiles").document(testDeviceId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    testPassed[0] = documentSnapshot.exists() &&
                            "Test User".equals(documentSnapshot.getString("displayName")) &&
                            "test@example.com".equals(documentSnapshot.getString("email")) &&
                            "1234567890".equals(documentSnapshot.getString("phoneNumber"));
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    testPassed[0] = false;
                    latch.countDown();
                });

        latch.await(5, TimeUnit.SECONDS);
        assertTrue("Profile should be saved in Firestore", testPassed[0]);

        scenario.close();
    }

    /**
     * Test US 01.02.02 - Update Profile
     * Verify that user can update existing profile information
     */
    @Test
    public void testUpdateProfile() throws InterruptedException {
        // First create a profile
        ProfileEntrant profile = new ProfileEntrant(testDeviceId, "Initial Name", "initial@example.com", "1111111111");
        db.collection("profiles").document(testDeviceId).set(profile);

        // Wait for save
        Thread.sleep(2000);

        ActivityScenario<ProfileActivity> scenario = ActivityScenario.launch(ProfileActivity.class);

        scenario.onActivity(activity -> {
            // Wait for profile to load
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                fail("Thread interrupted: " + e.getMessage());
            }

            EditText editTextName = activity.findViewById(R.id.editTextName);
            EditText editTextEmail = activity.findViewById(R.id.editTextEmail);
            EditText editTextPhone = activity.findViewById(R.id.editTextPhone);
            Button btnSave = activity.findViewById(R.id.btnSaveProfile);

            // Verify profile loaded
            assertEquals("Initial Name", editTextName.getText().toString());
            assertEquals("initial@example.com", editTextEmail.getText().toString());

            // Update data
            editTextName.setText("Updated Name");
            editTextEmail.setText("updated@example.com");
            editTextPhone.setText("9999999999");

            // Click update
            btnSave.performClick();
        });

        // Wait for Firebase operation
        Thread.sleep(3000);

        // Verify profile was updated in Firestore
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] testPassed = {false};

        db.collection("profiles").document(testDeviceId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    testPassed[0] = documentSnapshot.exists() &&
                            "Updated Name".equals(documentSnapshot.getString("displayName")) &&
                            "updated@example.com".equals(documentSnapshot.getString("email"));
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    testPassed[0] = false;
                    latch.countDown();
                });

        latch.await(5, TimeUnit.SECONDS);
        assertTrue("Profile should be updated in Firestore", testPassed[0]);

        scenario.close();
    }

    /**
     * Test that profile requires name and email
     */
    @Test
    public void testProfileValidation() {
        ActivityScenario<ProfileActivity> scenario = ActivityScenario.launch(ProfileActivity.class);

        scenario.onActivity(activity -> {
            EditText editTextName = activity.findViewById(R.id.editTextName);
            EditText editTextEmail = activity.findViewById(R.id.editTextEmail);
            Button btnSave = activity.findViewById(R.id.btnSaveProfile);

            // Try to save without name
            editTextName.setText("");
            editTextEmail.setText("test@example.com");
            btnSave.performClick();

            // Verify error is shown
            assertNotNull(editTextName.getError());

            // Try to save without email
            editTextName.setText("Test User");
            editTextEmail.setText("");
            btnSave.performClick();

            // Verify error is shown
            assertNotNull(editTextEmail.getError());

            // Try with invalid email
            editTextEmail.setText("invalid-email");
            btnSave.performClick();

            // Verify error is shown
            assertNotNull(editTextEmail.getError());
        });

        scenario.close();
    }

    /**
     * Test that phone number is optional
     */
    @Test
    public void testPhoneNumberOptional() {
        ActivityScenario<ProfileActivity> scenario = ActivityScenario.launch(ProfileActivity.class);

        scenario.onActivity(activity -> {
            EditText editTextName = activity.findViewById(R.id.editTextName);
            EditText editTextEmail = activity.findViewById(R.id.editTextEmail);
            EditText editTextPhone = activity.findViewById(R.id.editTextPhone);
            Button btnSave = activity.findViewById(R.id.btnSaveProfile);

            // Enter data without phone
            editTextName.setText("Test User");
            editTextEmail.setText("test@example.com");
            editTextPhone.setText(""); // Empty phone

            // Should save successfully
            btnSave.performClick();

            // Wait for Firebase operation
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                fail("Thread interrupted: " + e.getMessage());
            }

            // Verify no error on phone field
            assertNull(editTextPhone.getError());
        });

        scenario.close();
    }
}