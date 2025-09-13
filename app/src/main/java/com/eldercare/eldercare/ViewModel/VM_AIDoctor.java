package com.eldercare.eldercare.ViewModel;

import android.app.Application;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.AndroidViewModel;

import com.eldercare.eldercare.Model.M_AIDoctor;
import com.eldercare.eldercare.R;

public class VM_AIDoctor extends AppCompatActivity {
    private TextView data;
    private Button btn;
    private M_AIDoctor model;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ai_doctor);
        //Initialize views
        data = findViewById(R.id.data);
        btn = findViewById(R.id.ai_doctor_btn);
        //Initialize Model
        model = new M_AIDoctor(this);
        btn.setOnClickListener(v -> requestQuery());
    }

    private void requestQuery() {
        data.setText("Loading...");
        btn.setEnabled(false);
        model.sendQuery(
                "Tell me a joke",
                response -> {
                    //Success callback
                    data.setText(response);
                    btn.setEnabled(true);
                },
                errorMessage -> {
                    //Error callback
                    data.setText(errorMessage);
                    btn.setEnabled(true);
                }
        );
    }
}
