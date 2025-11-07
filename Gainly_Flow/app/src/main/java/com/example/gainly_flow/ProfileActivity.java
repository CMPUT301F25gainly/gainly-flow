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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ProfileActivity extends AppCompatActivity {

    // --- UI Elements ---
    private TextView textUserName, textDeviceId;
    private TextInputEditText editFullName, editEmail, editPhoneNumber;
    private SwitchMaterial switchNotifications, switchLocation;
    private MaterialButton buttonUpdateProfile, buttonDeleteAccount;
    private ImageButton backButton;
    private ImageView profileIcon;

    // --- Firebase ---
    private FirebaseFirestore db;
    private String profileId = "114514"; // Replace this dynamically later
    private DocumentReference profileRef;

    // --- Device ID ---
    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        profileRef = db.collection("profiles").document(profileId);

        // --- Generate unique device ID ---
        deviceId = generateDeviceId();

        // --- Bind UI ---
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

        // --- Load or create profile ---
        loadOrCreateProfile();

        // --- Button Listeners ---
        buttonUpdateProfile.setOnClickListener(v -> updateProfile());
        buttonDeleteAccount.setOnClickListener(v -> deleteAccount());
        backButton.setOnClickListener(v -> onBackPressed());
    }

    /**
     * Generates a unique device ID.
     */
    private String generateDeviceId() {
        String androidId = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID
        );
        if (androidId == null || androidId.isEmpty()) {
            return UUID.randomUUID().toString();
        }
        return androidId;
    }


    /**
     * Loads existing profile or creates a new one if missing.
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
     * Populates UI with Firestore data.
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
     * Creates a new profile if none exists.
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
     * Updates profile data in Firestore (creates if missing).
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

        // Use set() with merge = true to update or create if missing
        profileRef.set(updates)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to update: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    /**
     * Deletes the profile document from Firestore.
     */
    private void deleteAccount() {
        profileRef.delete()
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Account deleted.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
