package com.example.gainly_flow;

import android.os.Bundle;
import android.provider.Settings;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import android.view.View;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * {@code ProfileActivity} manages the display and modification of user profile data within the app.
 * <p>
 * This activity interacts with Firebase Firestore to:
 * <ul>
 *     <li>Retrieve and display an existing profile.</li>
 *     <li>Create a new profile if none exists.</li>
 *     <li>Allow users to update profile information such as name, email, and phone number.</li>
 *     <li>Toggle notification preferences.</li>
 *     <li>Delete the profile document if desired.</li>
 * </ul>
 *
 * <p>The class also generates a unique device ID to associate with each profile and ensures
 * synchronization between UI inputs and Firestore records.</p>
 *
 * <p><b>Firestore Collection:</b> {@code profiles}</p>
 */
public class ProfileActivity extends AppCompatActivity {

    // --- UI Elements ---
    /** Displays the user's full name. */
    private TextView textUserName;

    /** Displays the user's unique device ID. */
    private TextView textDeviceId;

    /** Editable field for user's full name. */
    private TextInputEditText editFullName;

    /** Editable field for user's email address. */
    private TextInputEditText editEmail;

    /** Editable field for user's phone number. */
    private TextInputEditText editPhoneNumber;

    /** Toggle switch for enabling or disabling app notifications. */
    private SwitchMaterial switchNotifications;

    /** Toggle switch for enabling or disabling location services (currently unused). */
    private SwitchMaterial switchLocation;

    /** Button to confirm and upload profile updates to Firestore. */
    private MaterialButton buttonUpdateProfile;

    /** Button to delete the user's profile from Firestore. */
    private MaterialButton buttonDeleteAccount;

    /** Navigates back to the previous screen. */
    private ImageButton backButton;

    /** Displays the user's profile icon. */
    private ImageView profileIcon;

    // --- Firebase ---
    /** Instance of Firestore database. */
    private FirebaseFirestore db;

    /** Hardcoded profile ID (intended to be dynamic in future implementations). */
    private String profileId = "114514";

    /** Reference to the user's document in the "profiles" collection. */
    private DocumentReference profileRef;

    // --- Device ID ---
    /** Unique device identifier associated with the profile. */
    private String deviceId;

    /**
     * Called when the activity is first created.
     * Initializes Firestore, binds UI components, and loads or creates the user's profile.
     *
     * @param savedInstanceState previously saved state of the activity, if any.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        profileRef = db.collection("profiles").document(profileId);

        // Generate unique device ID
        deviceId = generateDeviceId();

        // Bind UI components
        textUserName = findViewById(R.id.text_user_name);
        textDeviceId = findViewById(R.id.text_device_id);
        editFullName = findViewById(R.id.edit_full_name);
        editEmail = findViewById(R.id.edit_email);
        editPhoneNumber = findViewById(R.id.edit_phone_number);
        switchNotifications = findViewById(R.id.switch_notifications);
        switchLocation = findViewById(R.id.switch_location);
        buttonUpdateProfile = findViewById(R.id.button_update_profile);
        buttonDeleteAccount = findViewById(R.id.button_delete_account);
        backButton = findViewById(R.id.back_button_profile);
        profileIcon = findViewById(R.id.profile_icon);

        // Load or create user profile
        loadOrCreateProfile();

        // Set button listeners
        buttonUpdateProfile.setOnClickListener(v -> updateProfile());
        buttonDeleteAccount.setOnClickListener(v -> deleteAccount());
        backButton.setOnClickListener(v -> onBackPressed());
    }

    /**
     * Generates a unique device identifier for the current device.
     * <p>
     * Attempts to use {@link Settings.Secure#ANDROID_ID}, and falls back to a randomly generated
     * UUID if unavailable.
     *
     * @return a unique device ID string.
     */
    private String generateDeviceId() {
        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        if (androidId == null || androidId.isEmpty()) {
            return UUID.randomUUID().toString();
        }
        return androidId;
    }

    /**
     * Loads an existing user profile from Firestore, or creates a new one if it does not exist.
     * <p>
     * Displays a toast message upon failure.
     */
    private void loadOrCreateProfile() {
        profileRef.get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        loadProfile(snapshot);
                    } else {
                        createNewProfile();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    /**
     * Populates the UI fields with profile information retrieved from Firestore.
     *
     * @param snapshot Firestore {@link DocumentSnapshot} containing user profile data.
     */
    private void loadProfile(DocumentSnapshot snapshot) {
        String name = snapshot.getString("name");
        String email = snapshot.getString("email");
        String phone = snapshot.getString("phoneNumber");
        String device = snapshot.getString("deviceId");
        Boolean notifications = snapshot.getBoolean("notificationsEnabled");

        textUserName.setText(name != null ? name : "Unknown User");
        textDeviceId.setText("Device ID: " + (device != null ? device : deviceId));
        editFullName.setText(name);
        editEmail.setText(email);
        editPhoneNumber.setText(phone);
        switchNotifications.setChecked(notifications != null && notifications);
    }

    /**
     * Creates a new Firestore document with default user profile values if none exists.
     * <p>
     * Called when no document is found during {@link #loadOrCreateProfile()}.
     */
    private void createNewProfile() {
        Map<String, Object> data = new HashMap<>();
        data.put("profileId", profileId);
        data.put("name", "New User");
        data.put("email", "");
        data.put("phoneNumber", "");
        data.put("notificationsEnabled", true);
        data.put("valid", true);
        data.put("deviceId", deviceId);

        profileRef.set(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "New profile created!", Toast.LENGTH_SHORT).show();
                    textUserName.setText("New User");
                    textDeviceId.setText("Device ID: " + deviceId);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to create profile: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    /**
     * Updates user profile fields in Firestore.
     * <p>
     * Uses {@link DocumentReference#set(Object)} with {@code merge=true} to overwrite or create
     * the document as needed.
     */
    private void updateProfile() {
        String name = editFullName.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String phone = editPhoneNumber.getText().toString().trim();
        boolean notificationsEnabled = switchNotifications.isChecked();

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("email", email);
        updates.put("phoneNumber", phone);
        updates.put("notificationsEnabled", notificationsEnabled);
        updates.put("deviceId", deviceId);
        updates.put("valid", true);

        profileRef.set(updates)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to update: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    /**
     * Deletes the user's profile document from Firestore.
     * <p>
     * Displays a success or failure message depending on the result.
     */
    private void deleteAccount() {
        profileRef.delete()
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Account deleted.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }


}
