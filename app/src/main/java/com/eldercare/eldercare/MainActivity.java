package com.eldercare.eldercare;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button testFallDetectButton = findViewById(R.id.test_fall_detect_btn);
        testFallDetectButton.setOnClickListener(v -> {showFallDetectDialog();});

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    100
            );
        } else {
            Intent serviceIntent = new Intent(this, FallDetectionService.class);
            ContextCompat.startForegroundService(this, serviceIntent);
        }

        if (getIntent().getBooleanExtra("SHOW_DIALOG", false)) {
            // cancel the pending auto-launch if any
            FallDetectionService.cancelAutoLaunch();
            android.util.Log.d("ShowDialogAfterTap", "Auto-launch cancelled");
            showFallDetectDialog();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // update intent
        if (intent.getBooleanExtra("SHOW_DIALOG", false)) {
            showFallDetectDialog();
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

        Button cancelBtn = dialogView.findViewById(R.id.fall_detect_cancel_btn);
        Button confirmBtn = dialogView.findViewById(R.id.fall_detect_confirm_btn);

        cancelBtn.setOnClickListener(v -> {
            Toast.makeText(dialog.getContext(), "Closing Fall Detection Warning", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        confirmBtn.setOnClickListener(v -> {
            Toast.makeText(dialog.getContext(), "Confirming Emergency Responses", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
    }

}

          