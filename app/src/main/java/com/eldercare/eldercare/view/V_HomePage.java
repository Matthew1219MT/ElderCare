package com.eldercare.eldercare.view;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.eldercare.eldercare.R;
import com.eldercare.eldercare.activity.EmergencyActivity;
import com.eldercare.eldercare.activity.FaceScanActivity;
import com.eldercare.eldercare.service.FallDetectionService;
import com.eldercare.eldercare.viewmodel.VM_HomePage;

public class V_HomePage extends AppCompatActivity {

    private CardView facialAnalysis, emergency, aiDoctor;
    private VM_HomePage viewModel;

    private TextView emergencyCardText;
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
                    emergency.setCardBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
                    Toast.makeText(V_HomePage.this, "Emergency Calling Activating!", Toast.LENGTH_SHORT).show();
                    activateEmergencyService();
                    emergencyIsPressed = false;
                    emergency.setCardBackgroundColor(Color.parseColor("#BF5F56"));
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
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        facialAnalysis = findViewById(R.id.facial_analysis_btn);
        emergency = findViewById(R.id.emergency_btn);
        emergencyCardText = findViewById(R.id.emergencyCardText);
        aiDoctor = findViewById(R.id.ai_doctor_btn);
        facialAnalysis.setOnClickListener(v->{
            Intent intent = new Intent(this, FaceScanActivity.class);
            startActivity(intent);
        });
        GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                startActivity(new Intent(V_HomePage.this, EmergencyActivity.class));
                return true;
            }
        });
        emergency.setOnTouchListener((v, event) -> {
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
        aiDoctor.setOnClickListener(v->{
            Intent intent = new Intent(this, V_AIDoctor.class);
            startActivity(intent);
        });

        viewModel = new ViewModelProvider(this).get(VM_HomePage.class);
        observeViewModel();
        checkNotificationPermissions();
        viewModel.handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        viewModel.handleIntent(intent);
    }

    private void observeViewModel() {
        viewModel.showFallDialog.observe(this, show -> {
            if (show) showFallDetectDialog();
        });

        viewModel.startFallService.observe(this, start -> {
            if (start) {
                Intent serviceIntent = new Intent(this, FallDetectionService.class);
                ContextCompat.startForegroundService(this, serviceIntent);
            }
        });
    }

    private void checkNotificationPermissions(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    100
            );
        } else {
            viewModel.onPermissionGranted();
        }
    }

    private void showFallDetectDialog() {

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_fall_detect, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_fall_detect_bg);
        dialog.show();

        android.os.Handler handler = new android.os.Handler();
        Runnable autoLaunch = () -> {
            if (dialog.isShowing()) {
                dialog.dismiss();
                Intent intent = new Intent(this, SecondActivity.class);
                startActivity(intent);
                finish();
            }
        };

        handler.postDelayed(autoLaunch, 10_000);

        Button cancelBtn = dialogView.findViewById(R.id.fall_detect_cancel_btn);
        Button confirmBtn = dialogView.findViewById(R.id.fall_detect_confirm_btn);

        cancelBtn.setOnClickListener(v -> {
            dialog.dismiss();
        });

        //TO BE REPLACED WITH EMERGENCY ACTIVITY
        confirmBtn.setOnClickListener(v -> {
            dialog.dismiss();
        });
    }

    private void activateEmergencyService() {
        Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
        smsIntent.setData(Uri.parse("smsto:000  "));
        smsIntent.putExtra("sms_body", "This is an emergency! Please help!");
        startActivity(smsIntent);
    }
}