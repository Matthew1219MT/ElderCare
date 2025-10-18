package com.eldercare.eldercare.network;

import com.eldercare.eldercare.model.FaceScanData;
import com.eldercare.eldercare.repository.FaceScanRepository.ApiResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {
    @POST("api/face-scan")
    Call<ApiResponse> uploadFaceScan(@Body FaceScanData faceScanData);
}
