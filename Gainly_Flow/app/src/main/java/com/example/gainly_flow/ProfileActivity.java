package com.example.gainly_flow;

import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Activity for creating and updating entrant profile
 * Implements US 01.02.01 and US 01.02.02
 */
public class ProfileActivity extends AppCompatActivity {

    private EditText editTextName;
    private EditText editTextEmail;
    private EditText editTextPhone;
    private Button btnSaveProfile;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private String deviceId;
    private ProfileEntrant currentProfile;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Get device ID
        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        // Initialize views
        initializeViews();

        // Load existing profile if available
        loadProfile();

        // Set up save button
        btnSaveProfile.setOnClickListener(v -> saveProfile());
    }

    /**
     * Initialize all UI components
     */
    private void initializeViews() {
        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPhone = findViewById(R.id.editTextPhone);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        progressBar = findViewById(R.id.progressBar);

        // Back button
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Cancel button
        Button btnCancel = findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(v -> finish());

        progressBar.setVisibility(View.GONE);
    }

    /**
     * Load existing profile from Firestore
     * Part of US 01.02.02 - Update profile
     */
    private void loadProfile() {
        progressBar.setVisibility(View.VISIBLE);

        DocumentReference docRef = db.collection("profiles").document(deviceId);
        docRef.get().addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);

            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    // Profile exists - load data for editing
                    currentProfile = document.toObject(ProfileEntrant.class);
                    if (currentProfile != null) {
                        populateFields(currentProfile);
                        isEditMode = true;
                        btnSaveProfile.setText(R.string.update_profile);
                        setTitle(R.string.edit_profile_title);
                    }
                } else {
                    // New profile
                    isEditMode = false;
                    btnSaveProfile.setText(R.string.create_profile);
                    setTitle(R.string.create_profile_title);
                }
            } else {
                String errorMessage = task.getException() != null ?
                        task.getException().getMessage() : "Unknown error";
                Toast.makeText(ProfileActivity.this,
                        getString(R.string.error_loading_profile) + errorMessage,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Populate form fields with existing profile data
     * @param profile ProfileEntrant object with profile data
     */
    private void populateFields(ProfileEntrant profile) {
        editTextName.setText(profile.getDisplayName());
        editTextEmail.setText(profile.getEmail());

        // Phone number is optional
        if (profile.getPhoneNumber() != null && !profile.getPhoneNumber().isEmpty()) {
            editTextPhone.setText(profile.getPhoneNumber());
        }
    }

    /**
     * Save or update profile to Firestore
     * Implements US 01.02.01 (Create) and US 01.02.02 (Update)
     */
    private void saveProfile() {
        // Get input values
        String name = editTextName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String phone = editTextPhone.getText().toString().trim();

        // Validate required fields
        if (name.isEmpty()) {
            editTextName.setError(getString(R.string.error_name_required));
            editTextName.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            editTextEmail.setError(getString(R.string.error_email_required));
            editTextEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError(getString(R.string.error_email_invalid));
            editTextEmail.requestFocus();
            return;
        }

        // Phone is optional, but validate format if provided
        if (!phone.isEmpty() && !isValidPhoneNumber(phone)) {
            editTextPhone.setError(getString(R.string.error_phone_invalid));
            editTextPhone.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSaveProfile.setEnabled(false);

        // Create or update ProfileEntrant object
        ProfileEntrant profile;
        if (isEditMode && currentProfile != null) {
            // Update existing profile
            profile = currentProfile;
            profile.setDisplayName(name);
            profile.setEmail(email);
            profile.setPhoneNumber(phone.isEmpty() ? null : phone);
        } else {
            // Create new profile
            profile = new ProfileEntrant(deviceId, name, email, phone.isEmpty() ? null : phone);
        }

        // Save to Firestore
        db.collection("profiles")
                .document(deviceId)
                .set(profile)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    btnSaveProfile.setEnabled(true);

                    if (task.isSuccessful()) {
                        String message = isEditMode ?
                                getString(R.string.profile_updated_success) :
                                getString(R.string.profile_created_success);
                        Toast.makeText(ProfileActivity.this, message, Toast.LENGTH_SHORT).show();

                        // Update mode for subsequent saves
                        if (!isEditMode) {
                            isEditMode = true;
                            currentProfile = profile;
                            btnSaveProfile.setText(R.string.update_profile);
                            setTitle(R.string.edit_profile_title);
                        }
                    } else {
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() : "Unknown error";
                        Toast.makeText(ProfileActivity.this,
                                getString(R.string.error_saving_profile) + errorMessage,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Validate phone number format
     * @param phone Phone number to validate
     * @return true if valid
     */
    private boolean isValidPhoneNumber(String phone) {
        // Basic validation - at least 10 digits
        String cleanPhone = phone.replaceAll("[^0-9]", "");
        return cleanPhone.length() >= 10;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}