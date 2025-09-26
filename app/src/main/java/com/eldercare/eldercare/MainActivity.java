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
    private TextView emergencyCardText;
    private CardView facialAnalysisCard, emergencyCard, aiDoctorCard;
    // Emergency button variables
    private boolean emergencyIsPressed = false;
    private long emergencyPressStartTime = 0L;
    private final long triggerRequiredHoldingTime = 4000L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable updateCountdown = new Runnable() {
        @Override
        public void run() {
            if (emergencyIsPressed) {
                long elapsed = System.currentTimeMillis() - emergencyPressStartTime;
                int remaining = (int) Math.max((triggerRequiredHoldingTime - elapsed) / 1000, 0);

                if (elapsed >= triggerRequiredHoldingTime) {
                    emergencyCardText.setText("Emergency");
                    emergencyCard.setCardBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
                    Toast.makeText(MainActivity.this, "Emergency Calling Activating!", Toast.LENGTH_SHORT).show();
                    activateEmergencyService();
                    emergencyIsPressed = false;
                    emergencyCard.setCardBackgroundColor(Color.parseColor("#BF5F56"));
                } else {
                    emergencyCardText.setText("Keep pressing " + remaining + "s more to activate the emergency calling");
                    handler.postDelayed(this, 1000);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        welcomeText = findViewById(R.id.welcomeText);
        emergencyCardText = findViewById(R.id.emergencyCardText);
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

        GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                startActivity(new Intent(MainActivity.this, EmergencyActivity.class));
                return true;
            }
        });

        emergencyCard.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    emergencyIsPressed = true;
                    emergencyPressStartTime = System.currentTimeMillis();
                    handler.post(updateCountdown);
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    emergencyIsPressed = false;
                    handler.removeCallbacks(updateCountdown);
                    emergencyCardText.setText("Emergency");
                    return true;
            }

            return false;
        });
    }

    private void activateEmergencyService() {
        Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
        smsIntent.setData(Uri.parse("smsto:000  "));
        smsIntent.putExtra("sms_body", "This is an emergency! Please help!");
        startActivity(smsIntent);
    }
}
