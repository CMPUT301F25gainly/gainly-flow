package com.example.gainly_flow;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class EventDetailActivity extends AppCompatActivity {

    private TextView tvEntrants, tvAvailable;
    private Button btnJoin, btnLeave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        tvEntrants = findViewById(R.id.tvEntrants);
        tvAvailable = findViewById(R.id.tvAvailable);
        btnJoin = findViewById(R.id.btnJoin);
        btnLeave = findViewById(R.id.btnLeave);

        btnJoin.setOnClickListener(v -> {
            Toast.makeText(this, "Joined waiting list!", Toast.LENGTH_SHORT).show();
        });

        btnLeave.setOnClickListener(v -> {
            Toast.makeText(this, "Left waiting list!", Toast.LENGTH_SHORT).show();
        });
    }
}
