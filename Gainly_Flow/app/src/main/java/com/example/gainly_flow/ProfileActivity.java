package com.example.gainly_flow;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Screen for creating, viewing, and updating user profile details and preferences
 * for both entrant and organizer roles, backed by Firestore.
 */
public class ProfileActivity extends AppCompatActivity {

    // UI Elements
    private TextView textUserName;
    private TextView textDeviceId;
    private TextInputEditText editFullName;
    private TextInputEditText editEmail;
    private TextInputEditText editPhoneNumber;
    private SwitchMaterial switchNotifications;
    private SwitchMaterial switchLocation;
    private MaterialButton buttonUpdateProfile;
    private MaterialButton buttonDeleteAccount;
    private ImageButton backButton;
    private ImageView profileIcon;
    private BottomNavigationView bottomNav;

    // Firebase
    private FirebaseFirestore db;
    private DocumentReference profileRef;

    // Profile data
    private Profile profile;
    private String userType;
    private String deviceId;
    private boolean isExistingProfile = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Get profile and user type from intent
        profile = (Profile) getIntent().getSerializableExtra("profile");
        userType = getIntent().getStringExtra("userType");

        if (profile == null) {
            Toast.makeText(this, "Error: No profile data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Use profile's device ID or generate new one
        deviceId = profile.getDeviceId() != null ? profile.getDeviceId() : generateDeviceId();

        // Set up Firestore reference using device ID as document ID
        profileRef = db.collection("profiles").document(deviceId);

        // Bind UI components
        initializeViews();

        // Load or create user profile
        loadOrCreateProfile();

        // Set button listeners
        setupButtonListeners();

        // Setup modern back press handling
        setupBackPressHandler();

        setupBottomNavigation();
    }

    private void initializeViews() {
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
        bottomNav = findViewById(R.id.bottomNav);
    }

    private void setupBottomNavigation() {
        if ("Entrant".equals(userType) || profile instanceof Entrant) {
            bottomNav.setVisibility(View.VISIBLE);
            Entrant entrant = (profile instanceof Entrant) ? (Entrant) profile : null;
            BottomNavHelper.setupBottomNav(this, bottomNav, entrant, profile);
        } else {
            bottomNav.setVisibility(View.GONE);
        }
    }

    private void setupButtonListeners() {
        buttonUpdateProfile.setOnClickListener(v -> updateProfile());
        buttonDeleteAccount.setOnClickListener(v -> deleteAccount());
        backButton.setOnClickListener(v -> handleBackPress());
    }

    private void setupBackPressHandler() {
        // Modern back press handling for Android 13+
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackPress();
            }
        });
    }

    private void handleBackPress() {
        // If profile is complete, navigate to user activity, otherwise go to main
        if (profile.isProfileComplete()) {
            navigateToUserActivity();
        } else {
            finish();
        }
    }

    private String generateDeviceId() {
        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        if (androidId == null || androidId.isEmpty()) {
            return UUID.randomUUID().toString();
        }
        return androidId;
    }

    private void loadOrCreateProfile() {
        profileRef.get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        isExistingProfile = true;
                        loadProfileFromFirestore(snapshot);
                    } else {
                        isExistingProfile = false;
                        // Create new profile in Firestore with appropriate class
                        createProfileInFirestore();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    // Load from local profile as fallback
                    loadFromLocalProfile();
                });
    }

    private void loadProfileFromFirestore(DocumentSnapshot snapshot) {
        try {
            // Convert to appropriate role class
            Profile firestoreProfile = DeviceIdManager.convertToRoleClass(snapshot, userType);
            if (firestoreProfile != null) {
                this.profile = firestoreProfile;
                updateUIWithProfile();
            } else {
                throw new Exception("Failed to convert profile");
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error loading profile data", Toast.LENGTH_SHORT).show();
            loadFromLocalProfile();
        }
    }

    private void loadFromLocalProfile() {
        updateUIWithProfile();
    }

    private void createProfileInFirestore() {
        // Create appropriate profile class based on user type
        Profile profileToSave = createRoleSpecificProfile();

        // Create final variable for lambda
        final Profile finalProfileToSave = profileToSave;

        // Save to Firestore
        profileRef.set(profileToSave)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile created successfully!", Toast.LENGTH_SHORT).show();
                    // Store compatibility flag for notification preference
                    profileRef.update("notificationsEnabled", profileToSave.isReceiveNotifications());
                    this.profile = finalProfileToSave;
                    isExistingProfile = true;
                    updateUIWithProfile();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to create profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    updateUIWithProfile(); // Still update UI with local data
                });
    }

    private Profile createRoleSpecificProfile() {
        Profile profileToSave;

        if (userType != null) {
            switch (userType.toLowerCase()) {
                case "entrant":
                    if (profile instanceof Entrant) {
                        profileToSave = profile;
                    } else {
                        profileToSave = convertToEntrant(profile);
                    }
                    break;
                case "organizer":
                    if (profile instanceof Organizer) {
                        profileToSave = profile;
                    } else {
                        profileToSave = convertToOrganizer(profile);
                    }
                    break;
                default:
                    profileToSave = profile;
            }
        } else {
            profileToSave = profile;
        }

        // Ensure device ID is set
        profileToSave.setDeviceId(deviceId);
        profileToSave.setRole(userType != null ? userType : "User");
        profileToSave.setLastLoginAt(new Date());

        return profileToSave;
    }

    private Entrant convertToEntrant(Profile profile) {
        Entrant entrant = new Entrant(profile.getId());
        copyProfileProperties(profile, entrant);
        return entrant;
    }

    private Organizer convertToOrganizer(Profile profile) {
        Organizer organizer = new Organizer(profile.getId());
        copyProfileProperties(profile, organizer);
        return organizer;
    }

    private void copyProfileProperties(Profile source, Profile target) {
        target.setDisplayName(source.getDisplayName());
        target.setEmail(source.getEmail());
        target.setPhone(source.getPhone());
        target.setDeviceId(source.getDeviceId());
        target.setCreatedAt(source.getCreatedAt());
        target.setLastLoginAt(source.getLastLoginAt());
        target.setReceiveNotifications(source.isReceiveNotifications());
        target.setEnableLocationService(source.isEnableLocationService());
    }

    private void updateUIWithProfile() {
        String displayName = profile.getDisplayName();
        if (displayName == null || displayName.equals("New User")) {
            displayName = "";
        }

        String headerName = displayName.isEmpty() ? "Create Profile" : displayName;
        textUserName.setText(headerName);
        textDeviceId.setText("Device ID: " + deviceId);
        editFullName.setText(displayName);
        editEmail.setText(profile.getEmail());
        editPhoneNumber.setText(profile.getPhone());
        switchNotifications.setChecked(profile.isReceiveNotifications());
        switchLocation.setChecked(profile.isEnableLocationService());
        updatePrimaryButtonLabel();

        // Show role in the title or user name if needed
        // You can append role to the user name or show it in a toast
        if (profile.getRole() != null && !profile.getRole().isEmpty() && !headerName.equals("Create Profile")) {
            // Optionally show role in user name text
            String roleText = " (" + profile.getDisplayRole() + ")";
            if (!textUserName.getText().toString().contains(roleText)) {
                textUserName.setText(headerName + roleText);
            }
        }
    }

    private void updatePrimaryButtonLabel() {
        boolean complete = profile != null && profile.isProfileComplete();
        buttonUpdateProfile.setText(complete ? "Update Profile" : "Create Profile");
    }

    private void updateProfile() {
        String name = editFullName.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String phone = editPhoneNumber.getText().toString().trim();
        boolean notificationsEnabled = switchNotifications.isChecked();
        boolean locationEnabled = switchLocation.isChecked();

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Please fill in name and email", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update local profile
        profile.setDisplayName(name);
        profile.setEmail(email);
        profile.setPhone(phone);
        profile.setReceiveNotifications(notificationsEnabled);
        profile.setEnableLocationService(locationEnabled);
        profile.setLastLoginAt(new Date());

        // Ensure profile has correct role class before saving
        Profile profileToSave = ensureCorrectRoleClass(profile);

        // Create final variable for lambda
        final Profile finalProfileToSave = profileToSave;

        // Save to Firestore
        profileRef.set(profileToSave)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    // Keep backward-compatible flag in Firestore for notification preference
                    profileRef.update("notificationsEnabled", notificationsEnabled);

                    // Update UI with new name and role
                    String displayText = name;
                    if (profile.getRole() != null && !profile.getRole().isEmpty()) {
                        displayText += " (" + profile.getDisplayRole() + ")";
                    }
                    textUserName.setText(displayText);

                    this.profile = finalProfileToSave;

                    // Navigate to appropriate activity after profile completion
                    if (profile.isProfileComplete()) {
                        navigateToUserActivity();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private Profile ensureCorrectRoleClass(Profile profile) {
        if (userType == null) {
            return profile;
        }

        // Check if profile is already the correct class
        switch (userType.toLowerCase()) {
            case "entrant":
                if (profile instanceof Entrant) {
                    return profile;
                } else {
                    return convertToEntrant(profile);
                }
            case "organizer":
                if (profile instanceof Organizer) {
                    return profile;
                } else {
                    return convertToOrganizer(profile);
                }
            default:
                return profile;
        }
    }

    private void deleteAccount() {
        profileRef.delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Account deleted.", Toast.LENGTH_SHORT).show();
                    // Go back to main activity
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void navigateToUserActivity() {
        Intent intent;

        if (profile instanceof Entrant || "Entrant".equals(userType)) {
            intent = new Intent(this, EntrantViewMain.class);
            if (profile instanceof Entrant) {
                intent.putExtra("entrant", (Entrant) profile);
            } else {
                intent.putExtra("entrant", convertToEntrant(profile));
            }
        } else if (profile instanceof Organizer || "Organizer".equals(userType)) {
            intent = new Intent(this, OrganizerViewMain.class);
            if (profile instanceof Organizer) {
                intent.putExtra("organizer", (Organizer) profile);
            } else {
                intent.putExtra("organizer", convertToOrganizer(profile));
            }
        } else {
            // Default fallback
            intent = new Intent(this, MainActivity.class);
            intent.putExtra("profile", profile);
        }

        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavHelper.setSelectedItem(this, bottomNav);
    }
}
