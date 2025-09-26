package com.eldercare.eldercare.View;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.eldercare.eldercare.R;
import com.eldercare.eldercare.viewmodel.MainViewModel;

public class V_HomePage extends AppCompatActivity {

    private MainViewModel viewModel;
    private CardView facialAnalysis;
    private CardView emergency;
    private CardView aiDoctor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        facialAnalysis = findViewById(R.id.facial_analysis_btn);
        emergency = findViewById(R.id.emergency_btn);
        aiDoctor = findViewById(R.id.ai_doctor_btn);
        facialAnalysis.setOnClickListener(v->{
            //Logic for opening facial analysis activity
        });
        emergency.setOnClickListener(v->{
            //Logic for opening emergency activity
        });
        aiDoctor.setOnClickListener(v->{
            Intent intent = new Intent(V_HomePage.this, V_AIDoctor.class);
            startActivity(intent);
        });
    }
}