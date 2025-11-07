package com.example.gainly_flow;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminBrowseProfilesActivity extends AppCompatActivity {

    private LinearLayout profileListContainer;
    private EditText searchBox;
    private ImageButton backButton;
    private List<Profile> allProfiles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_browse_profiles);

        profileListContainer = findViewById(R.id.profileListContainer);
        searchBox = findViewById(R.id.etSearchProfiles);
        backButton = findViewById(R.id.btnBack);

        backButton.setOnClickListener(v -> finish());

        loadProfilesFromFirebase();

        // Filter dynamically
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterProfiles(s.toString()); }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private void loadProfilesFromFirebase() {
        FirebaseFirestore.getInstance()
                .collection("profiles")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    allProfiles.clear();
                    profileListContainer.removeAllViews();

                    if (querySnapshot.isEmpty()) {
                        TextView emptyText = new TextView(this);
                        emptyText.setText("No profiles available");
                        emptyText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                        emptyText.setPadding(0, 50, 0, 50);
                        profileListContainer.addView(emptyText);
                        return;
                    }

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String id = doc.getId();
                        String name = doc.getString("name");
                        String email = doc.getString("email");
                        String role = doc.getString("role");

                        Profile p = new Profile(id,null,null);
                        p.setDisplayName(name);
                        p.setEmail(email);
                        p.setRole(role);

                        allProfiles.add(p);
                        addProfileCard(p);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load profiles: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void addProfileCard(Profile profile) {
        CardView card = (CardView) LayoutInflater.from(this)
                .inflate(R.layout.item_profile_admin, profileListContainer, false);

        TextView nameView = card.findViewById(R.id.profileName);
        TextView emailView = card.findViewById(R.id.profileEmail);
        TextView roleView = card.findViewById(R.id.profileRole);
        ImageButton deleteButton = card.findViewById(R.id.btnDeleteProfile);

        nameView.setText(profile.getDisplayName() != null ? profile.getDisplayName() : "(Unnamed)");
        emailView.setText(profile.getEmail() != null ? profile.getEmail() : "No email");
        roleView.setText(profile.getRole() != null ? profile.getRole() : "User");

        // Delete button
        deleteButton.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Delete Profile")
                .setMessage("Remove \"" + profile.getDisplayName() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    FirebaseFirestore.getInstance().collection("profiles")
                            .document(profile.getId())
                            .delete()
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Profile deleted", Toast.LENGTH_SHORT).show();
                                loadProfilesFromFirebase();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
                })
                .setNegativeButton("Cancel", null)
                .show());

        profileListContainer.addView(card);
    }

    private void filterProfiles(String query) {
        profileListContainer.removeAllViews();
        if (query == null) query = "";
        String lower = query.toLowerCase(Locale.getDefault());

        List<Profile> filtered = new ArrayList<>();
        for (Profile p : allProfiles) {
            if (p.getDisplayName().toLowerCase().contains(lower)
                    || (p.getEmail() != null && p.getEmail().toLowerCase().contains(lower))) {
                filtered.add(p);
            }
        }

        if (filtered.isEmpty()) {
            TextView none = new TextView(this);
            none.setText("No matching profiles");
            none.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            none.setPadding(0, 40, 0, 40);
            profileListContainer.addView(none);
        } else {
            for (Profile p : filtered) addProfileCard(p);
        }
    }
}
