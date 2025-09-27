package com.eldercare.eldercare.repository;

import android.util.Log;
import androidx.lifecycle.MutableLiveData;
import com.eldercare.eldercare.model.FaceScanData;
import com.eldercare.eldercare.network.ApiService;
import com.eldercare.eldercare.network.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FaceScanRepository {
    private static final String TAG = "FaceScanRepository";
    private static FaceScanRepository instance;
    private ApiService apiService;

    private MutableLiveData<FaceScanData> scanResultLiveData;
    private MutableLiveData<String> uploadStatusLiveData;
    private MutableLiveData<String> errorLiveData;

    private FaceScanRepository() {
        apiService = RetrofitClient.getInstance().getApiService();
        scanResultLiveData = new MutableLiveData<>();
        uploadStatusLiveData = new MutableLiveData<>();
        errorLiveData = new MutableLiveData<>();
    }

    public static synchronized FaceScanRepository getInstance() {
        if (instance == null) {
            instance = new FaceScanRepository();
        }
        return instance;
    }

    public void uploadFaceScan(FaceScanData faceScanData) {
        uploadStatusLiveData.setValue("Uploading...");

        Call<ApiResponse> call = apiService.uploadFaceScan(faceScanData);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    uploadStatusLiveData.setValue("Upload successful");
                    Log.d(TAG, "Face scan uploaded successfully");
                } else {
                    String error = "Upload failed: " + response.message();
                    errorLiveData.setValue(error);
                    Log.e(TAG, error);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                String error = "Network error: " + t.getMessage();
                errorLiveData.setValue(error);
                Log.e(TAG, error, t);
            }
        });
    }

    public void processScanData(FaceScanData scanData) {
        // Process and validate scan data
        if (scanData != null && scanData.getPoints() != null && !scanData.getPoints().isEmpty()) {
            scanResultLiveData.setValue(scanData);
            Log.d(TAG, "Scan data processed: " + scanData.getPoints().size() + " points");
        } else {
            errorLiveData.setValue("Invalid scan data");
        }
    }

    public static class ApiResponse {
        private boolean success;
        private String message;
        private String scanId;

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getScanId() { return scanId; }
        public void setScanId(String scanId) { this.scanId = scanId; }
    }

    // LiveData getters
    public MutableLiveData<FaceScanData> getScanResultLiveData() {
        return scanResultLiveData;
    }

    public MutableLiveData<String> getUploadStatusLiveData() {
        return uploadStatusLiveData;
    }

    public MutableLiveData<String> getErrorLiveData() {
        return errorLiveData;
    }
}