package com.eldercare.eldercare.ViewModel;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.eldercare.eldercare.Model.M_AIDoctor;
import com.eldercare.eldercare.R;

public class Decrypted_VM_AIDoctor extends AppCompatActivity {
    private TextView data;
    private Button btn;
    private M_AIDoctor model;

    private android.os.Handler typingHandler = new android.os.Handler();
    private Runnable typingRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.fragment_ai_doctor_query);
        //Initialize views
        data = findViewById(R.id.title);
        btn = findViewById(R.id.send_message_btn);
        //Initialize Model
        model = new M_AIDoctor(this);
        btn.setOnClickListener(v -> requestQuery());
    }

    private void requestQuery() {
        data.setText("Loading...");
        btn.setEnabled(false);
        model.askAI(
                "Tell me a joke",
                response -> {
                    //Success callback
                    typeText(response);
                    btn.setEnabled(true);
                },
                errorMessage -> {
                    //Error callback
                    typeText(errorMessage);
                    btn.setEnabled(true);
                }
        );
    }


    private void typeText(String content) {
        final int[] wordIndex = {0};

        typingRunnable = new Runnable() {
            @Override
            public void run() {
                if (wordIndex[0] < content.length()) {
                    String current_text = content.substring(0, wordIndex[0] + 1);
                    data.setText(current_text);
                    wordIndex[0]++;
                    typingHandler.postDelayed(this, 50); // Adjust the delay as needed
                }
            }
        };
        typingHandler.post(typingRunnable);
    }
}
