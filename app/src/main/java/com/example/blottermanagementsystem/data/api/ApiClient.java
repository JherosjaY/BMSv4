package com.example.blottermanagementsystem.data.api;

import android.content.Context;
import android.util.Log;

import com.example.blottermanagementsystem.config.ApiConfig;
import com.example.blottermanagementsystem.config.AppConfig;
import com.example.blottermanagementsystem.data.entity.BlotterReport;
import com.example.blottermanagementsystem.data.interceptors.AuthInterceptor;
import com.example.blottermanagementsystem.data.interceptors.LoggingInterceptor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * ApiClient - Connects to Elysia backend API
 * Handles all HTTP requests to backend-elysia server
 * Uses centralized configuration from ApiConfig
 */
public class ApiClient {
    
    private static final String TAG = AppConfig.LOG_TAG;
    
    private static Retrofit retrofit;
    private static ApiService apiService;
    private static Context appContext;
    
    /**
     * Initialize Retrofit with Elysia backend
     * @param context Application context for interceptors
     */
    public static void initApiClient(Context context) {
        appContext = context.getApplicationContext();
        try {
            // Create custom interceptors
            AuthInterceptor authInterceptor = new AuthInterceptor(appContext);
            LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
            
            // Create OkHttpClient with interceptors and timeout settings
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(authInterceptor)
                    .addInterceptor(loggingInterceptor)
                    .connectTimeout(ApiConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
                    .readTimeout(ApiConfig.READ_TIMEOUT, TimeUnit.SECONDS)
                    .writeTimeout(ApiConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .build();
            
            // Create Gson instance with date format
            Gson gson = new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                    .create();
            
            // Create Retrofit instance using config
            retrofit = new Retrofit.Builder()
                    .baseUrl(ApiConfig.BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
            
            apiService = retrofit.create(ApiService.class);
            Log.d(TAG, "✅ API Client initialized with base URL: " + ApiConfig.BASE_URL);
        } catch (Exception e) {
            Log.e(TAG, "❌ Error initializing API Client: " + e.getMessage(), e);
        }
    }
    
    /**
     * Initialize Retrofit (legacy method for backward compatibility)
     */
    public static void initApiClient() {
        if (appContext != null) {
            initApiClient(appContext);
        } else {
            Log.w(TAG, "⚠️ Context not set. Use initApiClient(Context) instead.");
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
