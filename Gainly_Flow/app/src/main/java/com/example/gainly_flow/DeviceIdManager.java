package com.example.gainly_flow;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

public class DeviceIdManager {
    private static final String TAG = "DeviceIdManager";

    public static String getDeviceId(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    /**
     * Check if profile exists and create appropriate class (Entrant/Organizer) if needed
     */
    public static void checkAndCreateProfile(String deviceId, String role, ProfileCreationCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Use deviceId as the document ID for easy lookup
        db.collection("profiles").document(deviceId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Profile exists, return it as appropriate class
                        Profile profile = convertToRoleClass(documentSnapshot, role);
                        if (profile != null) {
                            callback.onProfileChecked(profile);
                        } else {
                            callback.onError(new Exception("Failed to parse profile"));
                        }
                    } else {
                        // Create new profile with appropriate class
                        createNewProfileWithRole(deviceId, role, callback);
                    }
                })
                .addOnFailureListener(e -> {
                    callback.onError(e);
                });
    }

    /**
     * Convert Firestore document to appropriate role class
     */
    public static Profile convertToRoleClass(DocumentSnapshot documentSnapshot, String targetRole) {
        try {
            String role = documentSnapshot.getString("role");
            if (role == null) {
                role = targetRole; // Use target role if not specified
            }

            Profile profile;
            switch (role.toLowerCase()) {
                case "entrant":
                    profile = documentSnapshot.toObject(Entrant.class);
                    break;
                case "organizer":
                    profile = documentSnapshot.toObject(Organizer.class);
                    break;
                default:
                    profile = documentSnapshot.toObject(Profile.class);
            }

            if (profile != null) {
                // Ensure the profile has the device ID set
                profile.setDeviceId(documentSnapshot.getId());
            }
            return profile;

        } catch (Exception e) {
            Log.e(TAG, "Error converting to role class: " + e.getMessage());
            return null;
        }
    }

    /**
     * Create new profile with specific role class (Entrant or Organizer)
     */
    private static void createNewProfileWithRole(String deviceId, String role, ProfileCreationCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Profile newProfile;

        switch (role.toLowerCase()) {
            case "entrant":
                newProfile = new Entrant(deviceId);
                break;
            case "organizer":
                newProfile = new Organizer(deviceId);
                break;
            default:
                newProfile = new Profile(deviceId, "", "");
                newProfile.setRole(role);
        }

        // Set device ID and default values (leave name empty so user must set it)
        newProfile.setDeviceId(deviceId);
        newProfile.setDisplayName("");
        newProfile.setEmail("");
        newProfile.setPhone("");
        newProfile.setRole(role);

        // Save to Firestore
        db.collection("profiles").document(deviceId)
                .set(newProfile)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "New " + role + " profile created for device: " + deviceId);
                    callback.onProfileChecked(newProfile);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating profile: ", e);
                    callback.onError(e);
                });
    }

    /**
     * Update an existing profile to a specific role class
     */
    public static void updateProfileRole(String deviceId, String newRole, ProfileUpdateCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Create final variables for use in lambda
        final String finalDeviceId = deviceId;
        final String finalNewRole = newRole;

        db.collection("profiles").document(deviceId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Create new profile with updated role class
                        Profile updatedProfile;

                        switch (finalNewRole.toLowerCase()) {
                            case "entrant":
                                updatedProfile = documentSnapshot.toObject(Entrant.class);
                                if (updatedProfile == null) {
                                    updatedProfile = new Entrant(finalDeviceId);
                                }
                                break;
                            case "organizer":
                                updatedProfile = documentSnapshot.toObject(Organizer.class);
                                if (updatedProfile == null) {
                                    updatedProfile = new Organizer(finalDeviceId);
                                }
                                break;
                            default:
                                updatedProfile = documentSnapshot.toObject(Profile.class);
                                if (updatedProfile == null) {
                                    updatedProfile = new Profile(finalDeviceId, "", "");
                                }
                        }

                        // Update role and save
                        updatedProfile.setRole(finalNewRole);
                        updatedProfile.setDeviceId(finalDeviceId);

                        // Create final variable for lambda
                        final Profile finalUpdatedProfile = updatedProfile;

                        db.collection("profiles").document(finalDeviceId)
                                .set(finalUpdatedProfile)
                                .addOnSuccessListener(aVoid -> {
                                    callback.onProfileUpdated(finalUpdatedProfile);
                                })
                                .addOnFailureListener(e -> {
                                    callback.onError(e);
                                });
                    } else {
                        callback.onError(new Exception("Profile not found"));
                    }
                })
                .addOnFailureListener(e -> {
                    callback.onError(e);
                });
    }

    public interface ProfileCreationCallback {
        void onProfileChecked(Profile profile);
        void onError(Exception e);
    }

    public interface ProfileUpdateCallback {
        void onProfileUpdated(Profile profile);
        void onError(Exception e);
    }
}
