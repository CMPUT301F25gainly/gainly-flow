package com.example.gainly_flow;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get device ID
        deviceId = DeviceIdManager.getDeviceId(this);

        setupEntrantButton();
        setupOrganizerButton();
        setupAdminButton();
    }

    private void setupEntrantButton() {
        Button entrantViewButton = findViewById(R.id.entrantButton);
        entrantViewButton.setOnClickListener(v -> {
            // Check device ID and create profile if needed
            DeviceIdManager.checkAndCreateProfile(deviceId, "Entrant", new DeviceIdManager.ProfileCreationCallback() {
                @Override
                public void onProfileChecked(Profile profile) {
                    // Check if this is a new profile (incomplete profile)
                    if (!profile.isProfileComplete() || isNewDeviceProfile(profile)) {
                        // New device or incomplete profile, go to Profile activity
                        Intent toProfile = new Intent(MainActivity.this, ProfileActivity.class);
                        toProfile.putExtra("profile", profile);
                        toProfile.putExtra("userType", "Entrant");
                        startActivity(toProfile);
                    } else {
                        // Existing complete profile, proceed to Entrant view
                        Intent toEntrant = new Intent(MainActivity.this, EntrantViewMain.class);
                        toEntrant.putExtra("profile", profile);
                        startActivity(toEntrant);
                    }
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(MainActivity.this, "Error accessing profile", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void setupOrganizerButton() {
        Button organizer = findViewById(R.id.organizerButton);
        organizer.setOnClickListener(v -> {
            // Check device ID and create profile if needed
            DeviceIdManager.checkAndCreateProfile(deviceId, "Organizer", new DeviceIdManager.ProfileCreationCallback() {
                @Override
                public void onProfileChecked(Profile profile) {
                    // Check if this is a new profile (incomplete profile)
                    if (!profile.isProfileComplete() || isNewDeviceProfile(profile)) {
                        // New device or incomplete profile, go to Profile activity
                        Intent toProfile = new Intent(MainActivity.this, ProfileActivity.class);
                        toProfile.putExtra("profile", profile);
                        toProfile.putExtra("userType", "Organizer");
                        startActivity(toProfile);
                    } else {
                        // Existing complete profile, proceed to Organizer view
                        Intent toOrganizer = new Intent(MainActivity.this, OrganizerViewMain.class);
                        toOrganizer.putExtra("profile", profile);
                        startActivity(toOrganizer);
                    }
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(MainActivity.this, "Error accessing profile", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void setupAdminButton() {
        Button adminLogin = findViewById(R.id.btnAdminLogin);
        adminLogin.setOnClickListener(v -> {
            showAdminPasswordDialog();
        });
    }

    private void showAdminPasswordDialog() {
        // Create a dialog with password input
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Admin Login");
        builder.setMessage("Enter admin password:");

        // Set up the input
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setTransformationMethod(PasswordTransformationMethod.getInstance());
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Login", (dialog, which) -> {
            String password = input.getText().toString();
            if ("1234".equals(password)) {
                // Password correct, proceed to admin activity
                startActivity(new Intent(MainActivity.this, AdminMainActivity.class));
            } else {
                Toast.makeText(MainActivity.this, "Incorrect password", Toast.LENGTH_SHORT).show();
                // Optionally show the dialog again
                showAdminPasswordDialog();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.cancel();
        });

        builder.show();
    }

    // Helper method to check if this is a new device profile
    private boolean isNewDeviceProfile(Profile profile) {
        // A profile is considered "new" if it has default/empty values
        return profile.getDisplayName() == null ||
                profile.getDisplayName().isEmpty() ||
                profile.getDisplayName().equals("New User") ||
                profile.getEmail() == null ||
                profile.getEmail().isEmpty();
    }
}