package com.example.gainly_flow;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button organizerViewButton = findViewById(R.id.organizerButton);
        Button adminViewButton = findViewById(R.id.adminButton);
        Button entrantViewButton = findViewById(R.id.entrantButton);

        organizerViewButton.setOnClickListener(v -> {
            Intent toOrganizer = new Intent(MainActivity.this, OrganizerViewMain.class);
            startActivity(toOrganizer);
        });
        adminViewButton.setOnClickListener(v -> {
            Intent toAdmin = new Intent(MainActivity.this, AdminViewMain.class);
            startActivity(toAdmin);
        });
        entrantViewButton.setOnClickListener(v -> {
            Intent toEntrant = new Intent(MainActivity.this, EntrantViewMain.class);
            startActivity(toEntrant);
        });

    }
}