package com.eldercare.eldercare;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class MainActivity extends AppCompatActivity {

    private TextView welcomeText;
    private CardView facialAnalysisCard, emergencyCard, aiDoctorCard;

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
                        Toast.makeText(this, "Facial Analysis selected", Toast.LENGTH_SHORT).show()
                // startActivity(new Intent(this, FacialAnalysisActivity.class));
        );

        emergencyCard.setOnClickListener(v ->
                        Toast.makeText(this, "Emergency selected", Toast.LENGTH_SHORT).show()
                // startActivity(new Intent(this, EmergencyActivity.class));
        );

        aiDoctorCard.setOnClickListener(v ->
                        Toast.makeText(this, "AI Doctor selected", Toast.LENGTH_SHORT).show()
                // startActivity(new Intent(this, AiDoctorActivity.class));
        );
    }
}
