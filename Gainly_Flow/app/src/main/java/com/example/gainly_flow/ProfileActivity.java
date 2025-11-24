package com.example.gainly_flow;

import android.content.Intent;
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

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

    // Firebase
    private FirebaseFirestore db;
    private DocumentReference profileRef;

    // Profile data
    private Profile profile;
    private String userType;
    private String deviceId;

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
    }

    private void setupButtonListeners() {
        buttonUpdateProfile.setOnClickListener(v -> updateProfile());
        buttonDeleteAccount.setOnClickListener(v -> deleteAccount());
        backButton.setOnClickListener(v -> onBackPressed());
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
                        loadProfileFromFirestore(snapshot);
                    } else {
                        // Create new profile in Firestore
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
        // Update local profile with Firestore data
        profile.setDisplayName(snapshot.getString("displayName"));
        profile.setEmail(snapshot.getString("email"));
        profile.setPhone(snapshot.getString("phone"));
        profile.setRole(snapshot.getString("role"));
        profile.setReceiveNotifications(Boolean.TRUE.equals(snapshot.getBoolean("receiveNotifications")));
        profile.setEnableLocationService(Boolean.TRUE.equals(snapshot.getBoolean("enableLocationService")));

        updateUIWithProfile();
    }

    private void loadFromLocalProfile() {
        updateUIWithProfile();
    }

    private void createProfileInFirestore() {
        // Set role if not already set
        if (profile.getRole() == null && userType != null) {
            profile.setRole(userType);
        }

        // Update profile with device ID
        profile.setDeviceId(deviceId);

        // Convert profile to Map for Firestore
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("id", profile.getId());
        profileData.put("displayName", profile.getDisplayName());
        profileData.put("email", profile.getEmail());
        profileData.put("phone", profile.getPhone());
        profileData.put("role", profile.getRole());
        profileData.put("deviceId", profile.getDeviceId());
        profileData.put("receiveNotifications", profile.isReceiveNotifications());
        profileData.put("enableLocationService", profile.isEnableLocationService());
        profileData.put("createdAt", profile.getCreatedAt());
        profileData.put("lastLoginAt", new Date());

        profileRef.set(profileData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile created successfully!", Toast.LENGTH_SHORT).show();
                    updateUIWithProfile();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to create profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    updateUIWithProfile(); // Still update UI with local data
                });
    }

    private void updateUIWithProfile() {
        String displayName = profile.getDisplayName();
        if (displayName == null || displayName.isEmpty()) {
            displayName = "New User";
        }

        textUserName.setText(displayName);
        textDeviceId.setText("Device ID: " + deviceId);
        editFullName.setText(displayName);
        editEmail.setText(profile.getEmail());
        editPhoneNumber.setText(profile.getPhone());
        switchNotifications.setChecked(profile.isReceiveNotifications());
        switchLocation.setChecked(profile.isEnableLocationService());
    }

    private void updateProfile() {
        String name = editFullName.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String phone = editPhoneNumber.getText().toString().trim();
        boolean notificationsEnabled = switchNotifications.isChecked();
        boolean locationEnabled = switchLocation.isChecked();

        // Update local profile
        profile.setDisplayName(name);
        profile.setEmail(email);
        profile.setPhone(phone);
        profile.setReceiveNotifications(notificationsEnabled);
        profile.setEnableLocationService(locationEnabled);
        profile.setLastLoginAt(new Date());

        // Update Firestore
        Map<String, Object> updates = new HashMap<>();
        updates.put("displayName", name);
        updates.put("email", email);
        updates.put("phone", phone);
        updates.put("receiveNotifications", notificationsEnabled);
        updates.put("enableLocationService", locationEnabled);
        updates.put("lastLoginAt", new Date());
        updates.put("deviceId", deviceId);

        profileRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    textUserName.setText(name);

                    // Navigate to appropriate activity after profile completion
                    if (profile.isProfileComplete()) {
                        navigateToUserActivity();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
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
        if ("Entrant".equals(userType)) {
            intent = new Intent(this, EntrantViewMain.class);
        } else if ("Organizer".equals(userType)) {
            intent = new Intent(this, OrganizerViewMain.class);
        } else {
            // Default fallback
            intent = new Intent(this, MainActivity.class);
        }
        intent.putExtra("profile", profile);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // If profile is complete, navigate to user activity, otherwise go to main
        if (profile.isProfileComplete()) {
            navigateToUserActivity();
        } else {
            super.onBackPressed();
        }
    }

}