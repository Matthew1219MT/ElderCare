package com.eldercare.eldercare.repository;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class FaceScanRepository {

    public static class ApiResponse {
        @SerializedName("success")
        private boolean success;

        @SerializedName("message")
        private String message;

        @SerializedName("healthy")
        private boolean healthy;

        @SerializedName("conditions")
        private List<String> conditions;

        @SerializedName("confidence")
        private float confidence;

        @SerializedName("recommendations")
        private List<String> recommendations;

        public ApiResponse() {}

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public boolean isHealthy() {
            return healthy;
        }

        public void setHealthy(boolean healthy) {
            this.healthy = healthy;
        }

        public List<String> getConditions() {
            return conditions;
        }

        public void setConditions(List<String> conditions) {
            this.conditions = conditions;
        }

        public float getConfidence() {
            return confidence;
        }

        public void setConfidence(float confidence) {
            this.confidence = confidence;
        }

        public List<String> getRecommendations() {
            return recommendations;
        }

        public void setRecommendations(List<String> recommendations) {
            this.recommendations = recommendations;
        }
    }
}