package com.example.gainly_flow;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gainly_flow.AdminMainActivity;

public class MainActivity extends AppCompatActivity {

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button entrant = findViewById(R.id.entrantButton);
        Button organizer = findViewById(R.id.organizerButton);
        Button adminLogin = findViewById(R.id.btnAdminLogin);

//        entrant.setOnClickListener(v ->
//                startActivity(new Intent(this, EntrantViewMain.class)));
//
        organizer.setOnClickListener(v ->
                startActivity(new Intent(this, CreateEvent.class)));

        adminLogin.setOnClickListener(v ->
                startActivity(new Intent(this, AdminMainActivity.class)));

    }
}