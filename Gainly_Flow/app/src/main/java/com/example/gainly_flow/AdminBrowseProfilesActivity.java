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

/**
 * {@code AdminBrowseProfilesActivity} is an activity that allows administrators to
 * view, search, and delete user profiles from the Firebase Firestore database.
 * <p>
 * The activity displays all profiles as cards in a scrollable list and includes
 * a search box for dynamic filtering. Administrators can delete a profile by
 * clicking the delete button on each profile card.
 * </p>
 *
 * <p><b>Features:</b></p>
 * <ul>
 *     <li>Retrieves all profiles from the Firestore "profiles" collection.</li>
 *     <li>Dynamically filters profiles by name or email.</li>
 *     <li>Displays profile information in card views.</li>
 *     <li>Allows administrators to delete profiles.</li>
 *     <li>Includes a back button to return to the previous screen.</li>
 * </ul>
 *
 * <p><b>Associated Layouts:</b></p>
 * <ul>
 *     <li>{@code activity_admin_browse_profiles.xml} — Defines the layout for this activity.</li>
 *     <li>{@code item_profile_admin.xml} — Defines the layout for each profile card.</li>
 * </ul>
 *
 * @author
 * @version 1.0
 */
public class AdminBrowseProfilesActivity extends AppCompatActivity {

    /** Container that holds all dynamically generated profile cards. */
    private LinearLayout profileListContainer;

    /** Search box for filtering profiles by name or email. */
    private EditText searchBox;

    /** Back button to return to the previous screen. */
    private ImageButton backButton;

    /** List of all profiles loaded from Firebase. */
    private List<Profile> allProfiles = new ArrayList<>();

    /**
     * Initializes the activity, sets up event listeners, and loads profiles from Firestore.
     *
     * @param savedInstanceState the previously saved instance state, if available
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_browse_profiles);

        profileListContainer = findViewById(R.id.profileListContainer);
        searchBox = findViewById(R.id.etSearchProfiles);
        backButton = findViewById(R.id.btnBack);

        // Return to the previous screen when the back button is clicked
        backButton.setOnClickListener(v -> finish());

        // Load profiles from Firestore
        loadProfilesFromFirebase();

        // Set up dynamic filtering based on text input
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterProfiles(s.toString());
            }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    /**
     * Loads all user profiles from the Firestore "profiles" collection and populates the UI.
     * If no profiles are found, displays a message to the administrator.
     */
    private void loadProfilesFromFirebase() {
        FirebaseFirestore.getInstance()
                .collection("profiles")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    allProfiles.clear();
                    profileListContainer.removeAllViews();

                    // Display message if no profiles exist
                    if (querySnapshot.isEmpty()) {
                        TextView emptyText = new TextView(this);
                        emptyText.setText("No profiles available");
                        emptyText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                        emptyText.setPadding(0, 50, 0, 50);
                        profileListContainer.addView(emptyText);
                        return;
                    }

                    // Create Profile objects and add cards for each
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String id = doc.getId();
                        String displayName = doc.getString("displayName");
                        String email = doc.getString("email");
                        String role = doc.getString("role");

                        Profile p = new Profile(id, null, null);
                        p.setDisplayName(displayName);
                        p.setEmail(email);
                        p.setRole(role);

                        allProfiles.add(p);
                        addProfileCard(p);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load profiles: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    /**
     * Creates and adds a profile card to the list container.
     * Each card displays a user's name, email, and role, and includes a delete button.
     *
     * @param profile the {@link Profile} object containing user data
     */
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

        // Set up deletion confirmation dialog
        deleteButton.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Delete Profile")
                .setMessage("Remove \"" + profile.getDisplayName() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteProfileAndAssociatedEvents(profile);
                })
                .setNegativeButton("Cancel", null)
                .show());

        profileListContainer.addView(card);
    }

    /**
     * Filters the list of profiles based on the provided query string.
     * Matches are case-insensitive and checked against both name and email fields.
     *
     * @param query the search string entered by the administrator
     */
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

        // Display message if no results found
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

    /**
     * Deletes a profile and all events created by that profile if the profile is an organizer.
     * This method first queries for all events created by the organizer (using organizerId),
     * deletes each event, and then deletes the profile itself.
     *
     * @param profile the {@link Profile} object to delete
     */
    private void deleteProfileAndAssociatedEvents(Profile profile) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String profileId = profile.getId();

        // Query for all events created by this organizer
        db.collection("events")
                .whereEqualTo("organizerId", profileId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // If there are events created by this organizer, delete them first
                    if (!querySnapshot.isEmpty()) {
                        int totalEvents = querySnapshot.size();
                        int[] deletedCount = {0};

                        for (DocumentSnapshot eventDoc : querySnapshot.getDocuments()) {
                            db.collection("events")
                                    .document(eventDoc.getId())
                                    .delete()
                                    .addOnSuccessListener(unused -> {
                                        deletedCount[0]++;
                                        // Once all events are deleted, delete the profile
                                        if (deletedCount[0] == totalEvents) {
                                            deleteProfile(profile, totalEvents);
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Failed to delete event: " + e.getMessage(),
                                                Toast.LENGTH_LONG).show();
                                    });
                        }
                    } else {
                        // No events to delete, just delete the profile
                        deleteProfile(profile, 0);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to query events: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Deletes the profile from Firestore and displays a success message.
     *
     * @param profile the {@link Profile} object to delete
     * @param deletedEventsCount the number of events that were deleted
     */
    private void deleteProfile(Profile profile, int deletedEventsCount) {
        FirebaseFirestore.getInstance()
                .collection("profiles")
                .document(profile.getId())
                .delete()
                .addOnSuccessListener(unused -> {
                    String message = "Profile deleted";
                    if (deletedEventsCount > 0) {
                        message += " along with " + deletedEventsCount + " event(s)";
                    }
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    loadProfilesFromFirebase();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to delete profile: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }
}
