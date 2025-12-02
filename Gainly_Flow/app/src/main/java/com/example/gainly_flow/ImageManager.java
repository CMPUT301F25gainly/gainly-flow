package com.example.gainly_flow;

import android.net.Uri;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.UUID;

/**
 * Helper around Firebase Storage for uploading, deleting, and resolving poster images
 * used by events so callers do not deal with storage paths directly.
 */
public class ImageManager {
    private static final String TAG = "ImageManager";
    private final FirebaseStorage storage;
    private final StorageReference storageRef;

    public ImageManager() {
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
    }

    /**
     * Uploads a poster image to Firebase Storage.
     * @param imageUri The URI of the image to upload.
     * @param onSuccess Listener for successful upload, returns the image ID (filename).
     * @param onFailure Listener for failed upload.
     */
    public void uploadPoster(Uri imageUri, OnSuccessListener<String> onSuccess, OnFailureListener onFailure) {
        if (imageUri == null) {
            onFailure.onFailure(new Exception("Image URI is null"));
            return;
        }

        String imageId = UUID.randomUUID().toString();
        StorageReference posterRef = storageRef.child("posters/" + imageId);

        posterRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d(TAG, "Image uploaded successfully: " + imageId);
                    onSuccess.onSuccess(imageId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to upload image", e);
                    onFailure.onFailure(e);
                });
    }

    /**
     * Deletes an image from Firebase Storage.
     * @param imageId The ID of the image to delete.
     */
    public void deleteImage(String imageId) {
        if (imageId == null || imageId.isEmpty()) return;

        StorageReference imageRef = storageRef.child("posters/" + imageId);
        imageRef.delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Image deleted: " + imageId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to delete image: " + imageId, e));
    }

    /**
     * Gets the full StorageReference for an image ID.
     * Useful for Glide.
     */
    public StorageReference getPosterReference(String imageId) {
        if (imageId == null || imageId.isEmpty()) return null;

        // If caller already passed a path (e.g., "posters/abc" or "some/folder/abc"), respect it.
        if (imageId.contains("/")) {
            return storageRef.child(imageId);
        }

        // Default to posters folder
        return storageRef.child("posters/" + imageId);
    }
}
