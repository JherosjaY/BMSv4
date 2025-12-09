package com.example.blottermanagementsystem.data.repository;

import com.example.blottermanagementsystem.data.api.ApiConfig;
import com.example.blottermanagementsystem.data.api.ApiResponse;
import com.example.blottermanagementsystem.data.api.BlotterApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ApiRepositoryExtensions {
    private final BlotterApiService apiService;
    
    public ApiRepositoryExtensions() {
        this.apiService = ApiConfig.getApiService();
    }
    
    public void updateFcmToken(BlotterApiService.FcmTokenRequest request, 
                              ApiRepository.ApiCallback<Object> callback) {
        apiService.updateFcmToken(request).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, 
                                 Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError("Failed to update FCM token");
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
}
