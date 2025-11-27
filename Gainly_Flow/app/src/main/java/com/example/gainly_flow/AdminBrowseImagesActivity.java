package com.example.gainly_flow;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Activity for administrators to browse and manage all uploaded images.
 * Implements US 03.06.01: "As an administrator, I want to be able to browse images
 * that are uploaded so I can remove them if necessary."
 */
public class AdminBrowseImagesActivity extends AppCompatActivity {

    private static final String TAG = "AdminBrowseImages";

    private RecyclerView recyclerView;
    private ImageAdapter imageAdapter;
    private List<ImageItem> imageList;
    private TextView tvImageCount;
    private ImageButton btnBack;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_browse_images);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        imageList = new ArrayList<>();

        initializeViews();
        setupRecyclerView();
        loadImages();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recycler_images);
        tvImageCount = findViewById(R.id.tv_image_count);
        btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        imageAdapter = new ImageAdapter(imageList);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(imageAdapter);
    }

    /**
     * Loads all images from Firebase Storage and matches them with events.
     * Optimized to use bulk queries instead of N+1 pattern.
     * Handles both "posterImageId" and "posterUri" field names for backward compatibility.
     */
    private void loadImages() {
        imageList.clear();

        // Load all events first (single query)
        db.collection("events").get()
                .addOnSuccessListener(eventsSnapshot -> {
                    // Create lookup maps for both field names
                    Map<String, DocumentSnapshot> posterImageIdMap = new HashMap<>();
                    Map<String, DocumentSnapshot> posterUriMap = new HashMap<>();

                    for (DocumentSnapshot doc : eventsSnapshot.getDocuments()) {
                        String posterImageId = doc.getString("posterImageId");
                        String posterUri = doc.getString("posterUri");

                        if (posterImageId != null && !posterImageId.isEmpty()) {
                            posterImageIdMap.put(posterImageId, doc);
                        }
                        if (posterUri != null && !posterUri.isEmpty()) {
                            posterUriMap.put(posterUri, doc);
                        }
                    }

                    // Now load images from Storage
                    StorageReference postersRef = storage.getReference().child("posters");
                    postersRef.listAll()
                            .addOnSuccessListener(listResult -> {
                                for (StorageReference item : listResult.getItems()) {
                                    String imageId = item.getName();

                                    // Check both maps for the image
                                    DocumentSnapshot doc = posterImageIdMap.get(imageId);
                                    if (doc == null) {
                                        doc = posterUriMap.get(imageId);
                                    }

                                    if (doc != null) {
                                        // Found matching event
                                        String eventName = doc.getString("name");
                                        String eventId = doc.getId();

                                        ImageItem imageItem = new ImageItem(
                                                imageId,
                                                "Event: " + (eventName != null ? eventName : "Unknown"),
                                                "poster",
                                                eventId
                                        );
                                        imageList.add(imageItem);
                                    } else {
                                        // Orphaned image
                                        ImageItem imageItem = new ImageItem(
                                                imageId,
                                                "Orphaned poster (no event found)",
                                                "poster",
                                                ""
                                        );
                                        imageList.add(imageItem);
                                    }
                                }

                                // Load profile images
                                loadProfileImages();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to load images from storage: " + e.getMessage());
                                Toast.makeText(this, "Failed to load images", Toast.LENGTH_SHORT).show();
                                updateUI();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load events: " + e.getMessage());
                    Toast.makeText(this, "Failed to load event data", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadProfileImages() {
        db.collection("profiles").get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String profileImageId = doc.getString("profileImageId");
                        String displayName = doc.getString("displayName");
                        String profileId = doc.getId();

                        if (profileImageId != null && !profileImageId.isEmpty()) {
                            ImageItem item = new ImageItem(
                                    profileImageId,
                                    "Profile: " + (displayName != null ? displayName : "Unknown"),
                                    "profile",
                                    profileId
                            );
                            imageList.add(item);
                        }
                    }

                    updateUI();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load profile images: " + e.getMessage());
                    updateUI();
                });
    }

    private void updateUI() {
        tvImageCount.setText("Total Images: " + imageList.size());
        imageAdapter.notifyDataSetChanged();

        if (imageList.isEmpty()) {
            Toast.makeText(this, "No images found", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Shows a fullscreen preview of the selected image with zoom support.
     */
    private void showImagePreview(ImageItem item) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_image_preview, null);

        PhotoView previewImage = dialogView.findViewById(R.id.preview_image);
        TextView tvDescription = dialogView.findViewById(R.id.tv_preview_description);
        TextView tvImageId = dialogView.findViewById(R.id.tv_preview_image_id);
        ImageButton btnClose = dialogView.findViewById(R.id.btn_close_preview);

        // Load full-size image with download URL
        StorageReference imageRef = storage.getReference()
                .child((item.getType().equals("poster") ? "posters/" : "profiles/") + item.getImageId());

        imageRef.getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    Glide.with(this)
                            .load(uri)
                            .fitCenter()
                            .into(previewImage);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load preview image: " + e.getMessage());
                    Toast.makeText(this, "Failed to load image preview", Toast.LENGTH_SHORT).show();
                });

        // Set metadata
        tvDescription.setText(item.getDescription());
        tvImageId.setText("Image ID: " + item.getImageId());

        // Create fullscreen dialog
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        // Wire up close button
        btnClose.setOnClickListener(v -> dialog.dismiss());

        // Disable tap-to-dismiss on the PhotoView itself to allow zooming/panning
        // Only the close button will dismiss the dialog

        dialog.show();
    }

    /**
     * Deletes an image from Firebase Storage and updates Firestore.
     */
    private void deleteImage(ImageItem item, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Image")
                .setMessage("Are you sure you want to delete this image?\n\n" + item.getDescription())
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Delete from Firebase Storage
                    StorageReference imageRef = storage.getReference()
                            .child((item.getType().equals("poster") ? "posters/" : "profiles/") + item.getImageId());

                    imageRef.delete()
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Image deleted from storage: " + item.getImageId());

                                // Update Firestore to remove the image reference
                                updateFirestoreAfterDeletion(item);

                                // Remove from list and update UI
                                imageList.remove(position);
                                imageAdapter.notifyItemRemoved(position);
                                updateUI();

                                Toast.makeText(this, "Image deleted successfully", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to delete image: " + e.getMessage());
                                Toast.makeText(this, "Failed to delete image", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Updates Firestore to remove the image reference after deletion.
     */
    private void updateFirestoreAfterDeletion(ImageItem item) {
        String collection = item.getType().equals("poster") ? "events" : "profiles";
        String field = item.getType().equals("poster") ? "posterImageId" : "profileImageId";

        db.collection(collection).document(item.getOwnerId())
                .update(field, null)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Firestore updated after image deletion");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update Firestore: " + e.getMessage());
                });
    }

    /**
     * Represents an image item with metadata.
     */
    private static class ImageItem {
        private final String imageId;
        private final String description;
        private final String type; // "poster" or "profile"
        private final String ownerId; // event ID or profile ID

        public ImageItem(String imageId, String description, String type, String ownerId) {
            this.imageId = imageId;
            this.description = description;
            this.type = type;
            this.ownerId = ownerId;
        }

        public String getImageId() { return imageId; }
        public String getDescription() { return description; }
        public String getType() { return type; }
        public String getOwnerId() { return ownerId; }
    }

    /**
     * RecyclerView adapter for displaying images in a grid.
     */
    private class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {
        private final List<ImageItem> items;

        public ImageAdapter(List<ImageItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_admin_image, parent, false);
            return new ImageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
            ImageItem item = items.get(position);
            holder.bind(item, position);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ImageViewHolder extends RecyclerView.ViewHolder {
            private final ImageView imageView;
            private final TextView tvDescription;
            private final Button btnDelete;

            public ImageViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.image_view);
                tvDescription = itemView.findViewById(R.id.tv_description);
                btnDelete = itemView.findViewById(R.id.btn_delete);
            }

            public void bind(ImageItem item, int position) {
                // Load image using Glide
                StorageReference imageRef = storage.getReference()
                        .child((item.getType().equals("poster") ? "posters/" : "profiles/") + item.getImageId());

                // Get download URL and load with Glide
                imageRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            Glide.with(itemView.getContext())
                                    .load(uri)
                                    .placeholder(R.drawable.blue_gradient_bg)
                                    .error(R.drawable.blue_gradient_bg)
                                    .centerCrop()
                                    .into(imageView);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to get download URL for " + item.getImageId() + ": " + e.getMessage());
                            imageView.setImageResource(R.drawable.blue_gradient_bg);
                        });

                tvDescription.setText(item.getDescription());

                // Click on image to preview
                imageView.setOnClickListener(v -> showImagePreview(item));

                btnDelete.setOnClickListener(v -> deleteImage(item, position));
            }
        }
    }
}