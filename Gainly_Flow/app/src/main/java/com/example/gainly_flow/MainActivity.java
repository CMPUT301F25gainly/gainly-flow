package com.example.gainly_flow;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gainly_flow.AdminMainActivity;

public class MainActivity extends AppCompatActivity {

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button entrant = findViewById(R.id.entrantButton);
        Button adminLogin = findViewById(R.id.btnAdminLogin);
        // MainActivity.java
        Button organizerBtn = findViewById(R.id.organizerButton);        // <- use your real ID
        organizerBtn.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, OrganizerEntrantListActivity.class);
            // pass a test eventId so the screen can load data
            i.putExtra("eventId", "event123");
            startActivity(i);
        });


//        entrant.setOnClickListener(v ->
//                startActivity(new Intent(this, EntrantViewMain.class)));
//
//        organizer.setOnClickListener(v ->
//                startActivity(new Intent(this, OrganizerEntrantListActivity.class)));

        adminLogin.setOnClickListener(v ->
                startActivity(new Intent(this, AdminMainActivity.class)));

    }
}
