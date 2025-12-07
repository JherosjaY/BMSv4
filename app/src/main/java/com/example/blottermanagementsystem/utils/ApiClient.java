package com.example.blottermanagementsystem.utils;

import android.util.Log;

import com.example.blottermanagementsystem.data.entity.BlotterReport;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * ApiClient - Connects to Elysia backend API
 * Handles all HTTP requests to backend-elysia server
 */
public class ApiClient {
    
    private static final String TAG = "ApiClient";
    
    // Elysia Backend URL Configuration
    // For Android Emulator: http://10.0.2.2:3000/
    // For Physical Device: http://YOUR_COMPUTER_IP:3000/ (e.g., http://192.168.1.100:3000/)
    // For Production: https://your-domain.com/
    private static final String BASE_URL = "https://bms-backend-exeb.onrender.com/";
    
    private static Retrofit retrofit;
    private static ApiService apiService;
    
    /**
     * Initialize Retrofit with Elysia backend
     */
    public static void initApiClient() {
        try {
            // Create logging interceptor for debugging
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            
            // Create OkHttpClient with interceptor
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();
            
            // Create Gson instance
            Gson gson = new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                    .create();
            
            // Create Retrofit instance
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
            
            apiService = retrofit.create(ApiService.class);
            Log.d(TAG, "✅ API Client initialized with base URL: " + BASE_URL);
        } catch (Exception e) {
            Log.e(TAG, "❌ Error initializing API Client: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get API Service instance
     */
    public static ApiService getApiService() {
        if (apiService == null) {
            initApiClient();
        }
        return apiService;
    }
    
    /**
     * Verify email with 6-digit code
     */
    public static void verifyEmail(String email, String code, ApiCallback<String> callback) {
        try {
            // Create request body
            java.util.Map<String, String> body = new java.util.HashMap<>();
            body.put("email", email);
            body.put("code", code);
            
            getApiService().verifyEmail(body).enqueue(new Callback<VerifyEmailResponse>() {
                @Override
                public void onResponse(Call<VerifyEmailResponse> call, Response<VerifyEmailResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        VerifyEmailResponse verifyResponse = response.body();
                        if (verifyResponse.success) {
                            Log.d(TAG, "✅ Email verified successfully");
                            callback.onSuccess("Email verified");
                        } else {
                            Log.e(TAG, "❌ Email verification failed: " + verifyResponse.message);
                            callback.onError(verifyResponse.message);
                        }
                    } else {
                        Log.e(TAG, "❌ Error verifying email: " + response.code());
                        callback.onError("Error: " + response.code());
                    }
                }
                
                @Override
                public void onFailure(Call<VerifyEmailResponse> call, Throwable t) {
                    Log.e(TAG, "❌ Network error: " + t.getMessage(), t);
                    callback.onError("Network error: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "❌ Exception: " + e.getMessage(), e);
            callback.onError("Exception: " + e.getMessage());
        }
    }
    
    /**
     * Send verification code to email
     */
    public static void sendVerificationCode(String email, ApiCallback<String> callback) {
        try {
            java.util.Map<String, String> body = new java.util.HashMap<>();
            body.put("email", email);
            
            getApiService().sendVerificationCode(body).enqueue(new Callback<SendCodeResponse>() {
                @Override
                public void onResponse(Call<SendCodeResponse> call, Response<SendCodeResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        SendCodeResponse codeResponse = response.body();
                        if (codeResponse.success) {
                            Log.d(TAG, "✅ Verification code sent to email");
                            callback.onSuccess("Code sent");
                        } else {
                            Log.e(TAG, "❌ Failed to send code: " + codeResponse.message);
                            callback.onError(codeResponse.message);
                        }
                    } else {
                        Log.e(TAG, "❌ Error sending code: " + response.code());
                        callback.onError("Error: " + response.code());
                    }
                }
                
                @Override
                public void onFailure(Call<SendCodeResponse> call, Throwable t) {
                    Log.e(TAG, "❌ Network error: " + t.getMessage(), t);
                    callback.onError("Network error: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "❌ Exception: " + e.getMessage(), e);
            callback.onError("Exception: " + e.getMessage());
        }
    }
    
    /**
     * Login user
     */
    public static void login(String username, String password, ApiCallback<LoginResponse> callback) {
        try {
            LoginRequest loginRequest = new LoginRequest(username, password);
            getApiService().login(loginRequest).enqueue(new Callback<LoginResponse>() {
                @Override
                public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        LoginResponse loginResponse = response.body();
                        if (loginResponse.success && loginResponse.data != null) {
                            Log.d(TAG, "✅ Login successful - User ID: " + loginResponse.data.user.getId());
                            callback.onSuccess(loginResponse);
                        } else {
                            Log.e(TAG, "❌ Login failed: " + loginResponse.message);
                            callback.onError(loginResponse.message);
                        }
                    } else {
                        Log.e(TAG, "❌ Error logging in: " + response.code());
                        callback.onError("Error: " + response.code());
                    }
                }
                
                @Override
                public void onFailure(Call<LoginResponse> call, Throwable t) {
                    Log.e(TAG, "❌ Network error: " + t.getMessage(), t);
                    callback.onError("Network error: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "❌ Exception: " + e.getMessage(), e);
            callback.onError("Exception: " + e.getMessage());
        }
    }
    
    /**
     * Create a new report
     */
    public static void createReport(BlotterReport report, ApiCallback<BlotterReport> callback) {
        try {
            getApiService().createReport(report).enqueue(new Callback<BlotterReport>() {
                @Override
                public void onResponse(Call<BlotterReport> call, Response<BlotterReport> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Log.d(TAG, "✅ Report created: " + response.body().getId());
                        callback.onSuccess(response.body());
                    } else {
                        Log.e(TAG, "❌ Error creating report: " + response.code());
                        callback.onError("Error: " + response.code());
                    }
                }
                
                @Override
                public void onFailure(Call<BlotterReport> call, Throwable t) {
                    Log.e(TAG, "❌ Network error: " + t.getMessage(), t);
                    callback.onError("Network error: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "❌ Exception: " + e.getMessage(), e);
            callback.onError("Exception: " + e.getMessage());
        }
    }
    
    /**
     * Get all reports
     */
    public static void getAllReports(ApiCallback<List<BlotterReport>> callback) {
        try {
            getApiService().getAllReports().enqueue(new Callback<List<BlotterReport>>() {
                @Override
                public void onResponse(Call<List<BlotterReport>> call, Response<List<BlotterReport>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Log.d(TAG, "✅ Retrieved " + response.body().size() + " reports");
                        callback.onSuccess(response.body());
                    } else {
                        Log.e(TAG, "❌ Error fetching reports: " + response.code());
                        callback.onError("Error: " + response.code());
                    }
                }
                
                @Override
                public void onFailure(Call<List<BlotterReport>> call, Throwable t) {
                    Log.e(TAG, "❌ Network error: " + t.getMessage(), t);
                    callback.onError("Network error: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "❌ Exception: " + e.getMessage(), e);
            callback.onError("Exception: " + e.getMessage());
        }
    }
    
    /**
     * Get report by ID
     */
    public static void getReportById(int reportId, ApiCallback<BlotterReport> callback) {
        try {
            getApiService().getReportById(reportId).enqueue(new Callback<BlotterReport>() {
                @Override
                public void onResponse(Call<BlotterReport> call, Response<BlotterReport> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Log.d(TAG, "✅ Retrieved report: " + reportId);
                        callback.onSuccess(response.body());
                    } else {
                        Log.e(TAG, "❌ Error fetching report: " + response.code());
                        callback.onError("Error: " + response.code());
                    }
                }
                
                @Override
                public void onFailure(Call<BlotterReport> call, Throwable t) {
                    Log.e(TAG, "❌ Network error: " + t.getMessage(), t);
                    callback.onError("Network error: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "❌ Exception: " + e.getMessage(), e);
            callback.onError("Exception: " + e.getMessage());
        }
    }
    
    /**
     * Update report
     */
    public static void updateReport(int reportId, BlotterReport report, ApiCallback<BlotterReport> callback) {
        try {
            getApiService().updateReport(reportId, report).enqueue(new Callback<BlotterReport>() {
                @Override
                public void onResponse(Call<BlotterReport> call, Response<BlotterReport> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Log.d(TAG, "✅ Report updated: " + reportId);
                        callback.onSuccess(response.body());
                    } else {
                        Log.e(TAG, "❌ Error updating report: " + response.code());
                        callback.onError("Error: " + response.code());
                    }
                }
                
                @Override
                public void onFailure(Call<BlotterReport> call, Throwable t) {
                    Log.e(TAG, "❌ Network error: " + t.getMessage(), t);
                    callback.onError("Network error: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "❌ Exception: " + e.getMessage(), e);
            callback.onError("Exception: " + e.getMessage());
        }
    }
    
    /**
     * Delete report
     */
    public static void deleteReport(int reportId, ApiCallback<String> callback) {
        try {
            getApiService().deleteReport(reportId).enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "✅ Report deleted: " + reportId);
                        callback.onSuccess("Report deleted successfully");
                    } else {
                        Log.e(TAG, "❌ Error deleting report: " + response.code());
                        callback.onError("Error: " + response.code());
                    }
                }
                
                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    Log.e(TAG, "❌ Network error: " + t.getMessage(), t);
                    callback.onError("Network error: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "❌ Exception: " + e.getMessage(), e);
            callback.onError("Exception: " + e.getMessage());
        }
    }
    
    /**
     * Generic API callback interface
     */
    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String errorMessage);
    }
}
