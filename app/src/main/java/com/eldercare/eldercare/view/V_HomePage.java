package com.eldercare.eldercare.view;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.eldercare.eldercare.R;
import com.eldercare.eldercare.activity.FaceScanActivity;

public class V_HomePage extends AppCompatActivity {

    private CardView facialAnalysis, emergency, aiDoctor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        facialAnalysis = findViewById(R.id.facial_analysis_btn);
        emergency = findViewById(R.id.emergency_btn);
        aiDoctor = findViewById(R.id.ai_doctor_btn);
        facialAnalysis.setOnClickListener(v->{
            Intent intent = new Intent(this, FaceScanActivity.class);
            startActivity(intent);
        });
        emergency.setOnClickListener(v->{
            //Logic for opening emergency activity
        });
        aiDoctor.setOnClickListener(v->{
            Intent intent = new Intent(this, V_AIDoctor.class);
            startActivity(intent);
        });
    }
}