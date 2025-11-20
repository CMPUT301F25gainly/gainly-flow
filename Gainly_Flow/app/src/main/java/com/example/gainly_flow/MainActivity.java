package com.example.gainly_flow;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

/**
 * {@code MainActivity} serves as the entry point of the Gainly Flow application.
 * <p>
 * It provides navigation options for different types of users, including
 * entrants, organizers, and administrators. Each button in this activity
 * launches the corresponding main view for that user role.
 * </p>
 *
 * <p>Currently implemented roles:</p>
 * <ul>
 *     <li><b>Entrant:</b> Navigates to {@link EntrantViewMain}</li>
 *     <li><b>Organizer:</b> Navigates to {@link OrganizerLanding}</li>
 *     <li><b>Administrator:</b> Navigates to {@link AdminMainActivity}</li>
 * </ul>
 *
 * <p>
 * The commented sections in the code indicate placeholders for potential future
 * functionality or layout adjustments (e.g., system bar insets).
 * </p>
 */
public class MainActivity extends AppCompatActivity {

    /**
     * Initializes the main activity and sets up navigation buttons for each user role.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down, this bundle contains
     *                           the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the Entrant View button and set its navigation behavior.
        Button entrantViewButton = findViewById(R.id.entrantButton);
        entrantViewButton.setOnClickListener(v -> {
            Intent toEntrant = new Intent(MainActivity.this, EntrantViewMain.class);
            startActivity(toEntrant);
        });

        // Initialize the Organizer button and set its navigation behavior.
        Button organizer = findViewById(R.id.organizerButton);
        organizer.setOnClickListener(v ->
                //startActivity(new Intent(this, OrganizerLanding.class)));
                startActivity(new Intent(this, OrganizerViewMain.class)));

        // Initialize the Administrator button and set its navigation behavior.
        Button adminLogin = findViewById(R.id.btnAdminLogin);
        adminLogin.setOnClickListener(v ->
                startActivity(new Intent(this, AdminMainActivity.class)));
    }

}
