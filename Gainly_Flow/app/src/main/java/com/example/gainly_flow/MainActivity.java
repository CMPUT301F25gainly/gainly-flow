package com.example.gainly_flow;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.gainly_flow.AdminMainActivity;

public class MainActivity extends AppCompatActivity {

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });

//         Button organizerViewButton = findViewById(R.id.organizerButton);
//         Button adminViewButton = findViewById(R.id.adminButton);
        Button entrantViewButton = findViewById(R.id.entrantButton);

        /*organizerViewButton.setOnClickListener(v -> {
            Intent toOrganizer = new Intent(MainActivity.this, OrganizerViewMain.class);
            startActivity(toOrganizer);
        });
        adminViewButton.setOnClickListener(v -> {
            Intent toAdmin = new Intent(MainActivity.this, AdminViewMain.class);
            startActivity(toAdmin);
        });*/
        entrantViewButton.setOnClickListener(v -> {
            Intent toEntrant = new Intent(MainActivity.this, EntrantViewMain.class);
            startActivity(toEntrant);
        });

        Button entrant = findViewById(R.id.entrantButton);
        Button organizer = findViewById(R.id.organizerButton);
        Button adminLogin = findViewById(R.id.btnAdminLogin);

        entrant.setOnClickListener(v ->
                startActivity(new Intent(this, EntrantViewMain.class)));

        organizer.setOnClickListener(v ->
                startActivity(new Intent(this, OrganizerLanding.class)));

        adminLogin.setOnClickListener(v ->
                startActivity(new Intent(this, AdminMainActivity.class)));


    }
}