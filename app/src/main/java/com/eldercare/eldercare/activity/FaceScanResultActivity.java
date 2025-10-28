package com.eldercare.eldercare.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import com.eldercare.eldercare.R;
import com.eldercare.eldercare.databinding.ActivityFaceScanResultBinding;
import java.util.ArrayList;

public class FaceScanResultActivity extends AppCompatActivity {
    private ActivityFaceScanResultBinding binding;
    private boolean isSuccess;
    private boolean isHealthy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_face_scan_result);

        setupViews();
        displayResult();
    }

    private void setupViews() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnScanAgain.setOnClickListener(v -> {
            finish();
        });

        binding.btnGoHome.setOnClickListener(v -> {
            finish();
        });
    }

    private void displayResult() {
        Intent intent = getIntent();
        isSuccess = intent.getBooleanExtra("success", false);

        if (isSuccess) {
            displaySuccessResult(intent);
        } else {
            displayErrorResult(intent);
        }
    }

    private void displaySuccessResult(Intent intent) {
        isHealthy = intent.getBooleanExtra("healthy", true);
        String message = intent.getStringExtra("message");
        float confidence = intent.getFloatExtra("confidence", 0f);
        ArrayList<String> conditions = intent.getStringArrayListExtra("conditions");
        ArrayList<String> recommendations = intent.getStringArrayListExtra("recommendations");

        binding.errorLayout.setVisibility(View.GONE);
        binding.successLayout.setVisibility(View.VISIBLE);

        if (isHealthy) {
            binding.resultIcon.setText("✓");
            binding.resultIcon.setTextColor(getColor(android.R.color.holo_green_dark));
            binding.tvResultTitle.setText("Healthy Face Scan");
            binding.tvResultTitle.setTextColor(getColor(android.R.color.holo_green_dark));
            binding.resultCard.setCardBackgroundColor(getColor(R.color.success_light));
        } else {
            binding.resultIcon.setText("⚠");
            binding.resultIcon.setTextColor(getColor(android.R.color.holo_orange_dark));
            binding.tvResultTitle.setText("Health Alert");
            binding.tvResultTitle.setTextColor(getColor(android.R.color.holo_orange_dark));
            binding.resultCard.setCardBackgroundColor(getColor(R.color.warning_light));
        }

        binding.tvResultMessage.setText(message != null ? message : "Scan completed successfully");

        if (confidence > 0) {
            binding.tvConfidence.setVisibility(View.VISIBLE);
            binding.tvConfidence.setText(String.format("Confidence: %.1f%%", confidence * 100));
        } else {
            binding.tvConfidence.setVisibility(View.GONE);
        }

        if (conditions != null && !conditions.isEmpty()) {
            binding.conditionsLayout.setVisibility(View.VISIBLE);
            StringBuilder conditionsText = new StringBuilder();
            for (String condition : conditions) {
                conditionsText.append("• ").append(condition).append("\n");
            }
            binding.tvConditions.setText(conditionsText.toString().trim());
        } else {
            binding.conditionsLayout.setVisibility(View.GONE);
        }

        if (recommendations != null && !recommendations.isEmpty()) {
            binding.recommendationsLayout.setVisibility(View.VISIBLE);
            StringBuilder recommendationsText = new StringBuilder();
            for (String recommendation : recommendations) {
                recommendationsText.append("• ").append(recommendation).append("\n");
            }
            binding.tvRecommendations.setText(recommendationsText.toString().trim());
        } else {
            binding.recommendationsLayout.setVisibility(View.GONE);
        }
    }

    private void displayErrorResult(Intent intent) {
        String error = intent.getStringExtra("error");

        binding.successLayout.setVisibility(View.GONE);
        binding.errorLayout.setVisibility(View.VISIBLE);

        binding.tvErrorMessage.setText(error != null ? error : "An unknown error occurred");
    }
}