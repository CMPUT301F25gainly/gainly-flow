package com.example.gainly_flow;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class DeviceIdManager {
    private static final String TAG = "DeviceIdManager";

    public static String getDeviceId(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public static void checkAndCreateProfile(String deviceId, String role, ProfileCreationCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Check if profile exists for this device ID
        db.collection("profiles")
                .whereEqualTo("id", deviceId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            // No profile found, create new one
                            createNewProfile(deviceId, role, callback);
                        } else {
                            // Profile exists, return the existing profile
                            Profile existingProfile = task.getResult().getDocuments().get(0).toObject(Profile.class);
                            callback.onProfileChecked(existingProfile);
                        }
                    } else {
                        Log.e(TAG, "Error checking profile: ", task.getException());
                        callback.onError(task.getException());
                    }
                });
    }

    private static void createNewProfile(String deviceId, String role, ProfileCreationCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Profile newProfile;
        switch (role) {
            case "Entrant":
                newProfile = new Entrant(deviceId, "User_" + deviceId.substring(0, 8), "", "");
                break;
            case "Organizer":
                newProfile = new Organizer(deviceId, "Organizer_" + deviceId.substring(0, 8), "", "");
                break;
            default:
                newProfile = new Profile(deviceId, "User_" + deviceId.substring(0, 8), "");
                newProfile.setRole(role);
        }

        // Add profile to Firestore
        db.collection("profiles")
                .document(deviceId)
                .set(newProfile)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "New profile created for device: " + deviceId);
                    callback.onProfileChecked(newProfile);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating profile: ", e);
                    callback.onError(e);
                });
    }

    public interface ProfileCreationCallback {
        void onProfileChecked(Profile profile);
        void onError(Exception e);
    }
}