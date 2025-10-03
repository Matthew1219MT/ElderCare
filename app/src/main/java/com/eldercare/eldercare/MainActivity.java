package com.eldercare.eldercare;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.eldercare.eldercare.activity.EmergencyActivity;
import com.eldercare.eldercare.activity.FaceScanActivity;

public class MainActivity extends AppCompatActivity {

    private TextView welcomeText;

    private CardView facialAnalysisCard, emergencyCard, aiDoctorCard;
    // Emergency button variables


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        welcomeText = findViewById(R.id.welcomeText);

        facialAnalysisCard = findViewById(R.id.facialAnalysisCard);
        emergencyCard = findViewById(R.id.emergencyCard);
        aiDoctorCard = findViewById(R.id.aiDoctorCard);

        // Get dynamic username if passed, else fallback
        String username = getIntent().getStringExtra("username");
        if (username == null) {
            username = "John Smith";
        }
        welcomeText.setText("Welcome back, " + username + ".");

        // Click listeners
        facialAnalysisCard.setOnClickListener(v ->
                startActivity(new Intent(this, FaceScanActivity.class))
        );

        aiDoctorCard.setOnClickListener(v ->
                        Toast.makeText(this, "AI Doctor selected", Toast.LENGTH_SHORT).show()
                // startActivity(new Intent(this, AiDoctorActivity.class));
        );


    }


}
