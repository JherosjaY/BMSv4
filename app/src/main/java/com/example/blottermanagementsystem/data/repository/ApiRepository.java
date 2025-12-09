package com.example.blottermanagementsystem.data.repository;

import android.util.Log;
import com.example.blottermanagementsystem.data.api.ApiConfig;
import com.example.blottermanagementsystem.data.api.ApiResponse;
import com.example.blottermanagementsystem.data.api.BlotterApiService;
import com.example.blottermanagementsystem.data.entity.BlotterReport;
import com.example.blottermanagementsystem.data.entity.User;
import com.example.blottermanagementsystem.data.entity.Officer;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ApiRepository {
    private static final String TAG = "ApiRepository";
    private final BlotterApiService apiService;
    
    public ApiRepository() {
        this.apiService = ApiConfig.getApiService();
    }
    
    // ==================== Authentication ====================
    
    public void login(String username, String password, ApiCallback<BlotterApiService.LoginData> callback) {
        BlotterApiService.LoginRequest request = new BlotterApiService.LoginRequest(username, password);
        
        apiService.login(request).enqueue(new Callback<ApiResponse<BlotterApiService.LoginData>>() {
            @Override
            public void onResponse(Call<ApiResponse<BlotterApiService.LoginData>> call, 
                                 Response<ApiResponse<BlotterApiService.LoginData>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().isSuccess()) {
                        callback.onSuccess(response.body().getData());
                    } else {
                        callback.onError(response.body().getMessage());
                    }
                } else {
                    callback.onError("Login failed: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<BlotterApiService.LoginData>> call, Throwable t) {
                Log.e(TAG, "Login error", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
    
    public void register(User user, String password, ApiCallback<BlotterApiService.LoginData> callback) {
        BlotterApiService.RegisterRequest request = new BlotterApiService.RegisterRequest(
            user.getUsername(), password, user.getFirstName(), user.getLastName(),
            user.getEmail(), user.getPhoneNumber(), user.getRole()
        );
        
        apiService.register(request).enqueue(new Callback<ApiResponse<BlotterApiService.LoginData>>() {
            @Override
            public void onResponse(Call<ApiResponse<BlotterApiService.LoginData>> call,
                                 Response<ApiResponse<BlotterApiService.LoginData>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().isSuccess()) {
                        callback.onSuccess(response.body().getData());
                    } else {
                        callback.onError(response.body().getMessage());
                    }
                } else {
                    callback.onError("Registration failed");
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<BlotterApiService.LoginData>> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
    
    // ==================== Users ====================
    
    public void getAllUsers(ApiCallback<List<User>> callback) {
        apiService.getAllUsers().enqueue(new Callback<ApiResponse<List<User>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<User>>> call,
                                 Response<ApiResponse<List<User>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError("Failed to fetch users");
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<List<User>>> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
    
    // ==================== Reports ====================
    
    public void getAllReports(ApiCallback<List<BlotterReport>> callback) {
        apiService.getAllReports().enqueue(new Callback<ApiResponse<List<BlotterReport>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<BlotterReport>>> call,
                                 Response<ApiResponse<List<BlotterReport>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError("Failed to fetch reports");
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<List<BlotterReport>>> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
    
    public void createReport(BlotterReport report, ApiCallback<BlotterReport> callback) {
        apiService.createReport(report).enqueue(new Callback<ApiResponse<BlotterReport>>() {
            @Override
            public void onResponse(Call<ApiResponse<BlotterReport>> call,
                                 Response<ApiResponse<BlotterReport>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError("Failed to create report");
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<BlotterReport>> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
    
    public void updateReport(int id, BlotterReport report, ApiCallback<BlotterReport> callback) {
        apiService.updateReport(id, report).enqueue(new Callback<ApiResponse<BlotterReport>>() {
            @Override
            public void onResponse(Call<ApiResponse<BlotterReport>> call,
                                 Response<ApiResponse<BlotterReport>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError("Failed to update report");
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<BlotterReport>> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
    
    // ==================== Sync ====================
    
    public void syncData(BlotterApiService.SyncRequest request, ApiCallback<BlotterApiService.SyncResponse> callback) {
        apiService.syncUpload(request).enqueue(new Callback<ApiResponse<BlotterApiService.SyncResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<BlotterApiService.SyncResponse>> call,
                                 Response<ApiResponse<BlotterApiService.SyncResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError("Sync failed");
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<BlotterApiService.SyncResponse>> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
    
    // ==================== FCM Token ====================
    
    public void updateFcmToken(BlotterApiService.FcmTokenRequest request, ApiCallback<Object> callback) {
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
    
    // ==================== Callback Interface ====================
    
    public interface ApiCallback<T> {
        void onSuccess(T data);
        void onError(String error);
    }
}
