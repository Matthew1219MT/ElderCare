package com.eldercare.eldercare;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

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
        testFallDetectButton.setOnClickListener(v -> showFallDetectDialog());
    }

    private void showFallDetectDialog(){

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_fall_detect, null); // replace with your CardView XML name

        // Create AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_fall_detect_bg);
        dialog.show();

        Button cancelBtn = dialogView.findViewById(R.id.fall_detect_cancel_btn);
        Button confirmBtn = dialogView.findViewById(R.id.fall_detect_confirm_btn);

        cancelBtn.setOnClickListener(v -> {
            Toast.makeText(dialog.getContext(), "Closing Fall Detection Warning",1).show();
            dialog.dismiss();
        });

        confirmBtn.setOnClickListener(v -> {
            Toast.makeText(dialog.getContext(), "Confirming Emergency Responses",1).show();
            dialog.dismiss();
        });

    }


}