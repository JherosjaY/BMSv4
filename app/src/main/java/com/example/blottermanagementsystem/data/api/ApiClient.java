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
     * Register user
     */
    public static void register(String username, String email, String password, String confirmPassword, ApiCallback<Object> callback) {
        try {
            RegisterRequest registerRequest = new RegisterRequest(username, email, password, confirmPassword);
            
            getApiService().register(registerRequest).enqueue(new Callback<RegisterResponse>() {
                @Override
                public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        RegisterResponse registerResponse = response.body();
                        if (registerResponse.success) {
                            Log.d(TAG, "✅ Registration successful - User: " + email);
                            callback.onSuccess(registerResponse);
                        } else {
                            Log.e(TAG, "❌ Registration failed: " + registerResponse.message);
                            callback.onError(registerResponse.message);
                        }
                    } else {
                        Log.e(TAG, "❌ Error registering: " + response.code());
                        callback.onError("Error: " + response.code());
                    }
                }
                
                @Override
                public void onFailure(Call<RegisterResponse> call, Throwable t) {
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
     * Verify email with 6-digit code
     */
    public static void verifyEmail(String email, String code, ApiCallback<Object> callback) {
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
    public static void sendVerificationCode(String email, ApiCallback<Object> callback) {
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
     * Google Sign-In
     */
    public static void googleSignIn(String email, String displayName, String photoUrl, ApiCallback<LoginResponse> callback) {
        try {
            java.util.Map<String, String> body = new java.util.HashMap<>();
            body.put("email", email);
            if (displayName != null && !displayName.isEmpty()) {
                body.put("displayName", displayName);
            }
            if (photoUrl != null && !photoUrl.isEmpty()) {
                body.put("photoUrl", photoUrl);
            }
            
            getApiService().googleSignIn(body).enqueue(new Callback<LoginResponse>() {
                @Override
                public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        LoginResponse loginResponse = response.body();
                        if (loginResponse.success && loginResponse.data != null) {
                            Log.d(TAG, "✅ Google Sign-In successful - User ID: " + loginResponse.data.user.getId());
                            callback.onSuccess(loginResponse);
                        } else {
                            Log.e(TAG, "❌ Google Sign-In failed: " + loginResponse.message);
                            callback.onError(loginResponse.message);
                        }
                    } else {
                        Log.e(TAG, "❌ Error with Google Sign-In: " + response.code());
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
     * Delete report
     */
    public static void deleteReport(int reportId, ApiCallback<Object> callback) {
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
     * Save FCM token to backend
     */
    public static void saveFCMToken(String userId, String fcmToken, String deviceId, ApiCallback<Object> callback) {
        try {
            java.util.Map<String, String> body = new java.util.HashMap<>();
            body.put("userId", userId);
            body.put("fcmToken", fcmToken);
            if (deviceId != null && !deviceId.isEmpty()) {
                body.put("deviceId", deviceId);
            }

            getApiService().saveFCMToken(body).enqueue(new Callback<Object>() {
                @Override
                public void onResponse(Call<Object> call, Response<Object> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Log.d(TAG, "✅ FCM token saved successfully");
                        callback.onSuccess(response.body());
                    } else {
                        Log.e(TAG, "❌ Error saving FCM token: " + response.code());
                        callback.onError("Error: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<Object> call, Throwable t) {
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
     * Get all hearings
     */
    public static void getHearings(ApiCallback<List<com.example.blottermanagementsystem.data.entity.Hearing>> callback) {
        try {
            getApiService().getHearings().enqueue(new Callback<List<com.example.blottermanagementsystem.data.entity.Hearing>>() {
                @Override
                public void onResponse(Call<List<com.example.blottermanagementsystem.data.entity.Hearing>> call, Response<List<com.example.blottermanagementsystem.data.entity.Hearing>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        callback.onSuccess(response.body());
                    } else {
                        callback.onError("Error: " + response.code());
                    }
                }
                @Override
                public void onFailure(Call<List<com.example.blottermanagementsystem.data.entity.Hearing>> call, Throwable t) {
                    callback.onError("Network error: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            callback.onError("Exception: " + e.getMessage());
        }
    }

    /**
     * Get notifications for user
     */
    public static void getNotifications(String userId, ApiCallback<List<com.example.blottermanagementsystem.data.entity.Notification>> callback) {
        try {
            getApiService().getNotifications(userId).enqueue(new Callback<List<com.example.blottermanagementsystem.data.entity.Notification>>() {
                @Override
                public void onResponse(Call<List<com.example.blottermanagementsystem.data.entity.Notification>> call, Response<List<com.example.blottermanagementsystem.data.entity.Notification>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        callback.onSuccess(response.body());
                    } else {
                        callback.onError("Error: " + response.code());
                    }
                }
                @Override
                public void onFailure(Call<List<com.example.blottermanagementsystem.data.entity.Notification>> call, Throwable t) {
                    callback.onError("Network error: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            callback.onError("Exception: " + e.getMessage());
        }
    }

    /**
     * Get hearings calendar
     */
    public static void getHearingsCalendar(ApiCallback<List<com.example.blottermanagementsystem.data.entity.Hearing>> callback) {
        getHearings(callback);
    }

    /**
     * Mark all notifications as read
     */
    public static void markAllNotificationsAsRead(String userId, ApiCallback<Object> callback) {
        try {
            getApiService().markAllNotificationsAsRead(userId).enqueue(new Callback<Object>() {
                @Override
                public void onResponse(Call<Object> call, Response<Object> response) {
                    if (response.isSuccessful()) {
                        callback.onSuccess(response.body());
                    } else {
                        callback.onError("Error: " + response.code());
                    }
                }
                @Override
                public void onFailure(Call<Object> call, Throwable t) {
                    callback.onError("Network error: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            callback.onError("Exception: " + e.getMessage());
        }
    }

    /**
     * Delete notification
     */
    public static void deleteNotification(Integer notificationId, ApiCallback<Object> callback) {
        try {
            getApiService().deleteNotification(notificationId).enqueue(new Callback<Object>() {
                @Override
                public void onResponse(Call<Object> call, Response<Object> response) {
                    if (response.isSuccessful()) {
                        callback.onSuccess(response.body());
                    } else {
                        callback.onError("Error: " + response.code());
                    }
                }
                @Override
                public void onFailure(Call<Object> call, Throwable t) {
                    callback.onError("Network error: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            callback.onError("Exception: " + e.getMessage());
        }
    }

    /**
     * Forgot password
     */
    public static void forgotPassword(String email, ApiCallback<Object> callback) {
        try {
            java.util.Map<String, String> body = new java.util.HashMap<>();
            body.put("email", email);
            getApiService().forgotPassword(body).enqueue(new Callback<Object>() {
                @Override
                public void onResponse(Call<Object> call, Response<Object> response) {
                    if (response.isSuccessful()) {
                        callback.onSuccess(response.body());
                    } else {
                        callback.onError("Error: " + response.code());
                    }
                }
                @Override
                public void onFailure(Call<Object> call, Throwable t) {
                    callback.onError("Network error: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            callback.onError("Exception: " + e.getMessage());
        }
    }

    /**
     * Reset password
     */
    public static void resetPassword(String email, String code, String newPassword, ApiCallback<Object> callback) {
        try {
            java.util.Map<String, String> body = new java.util.HashMap<>();
            body.put("email", email);
            body.put("code", code);
            body.put("newPassword", newPassword);
            getApiService().resetPassword(body).enqueue(new Callback<Object>() {
                @Override
                public void onResponse(Call<Object> call, Response<Object> response) {
                    if (response.isSuccessful()) {
                        callback.onSuccess(response.body());
                    } else {
                        callback.onError("Error: " + response.code());
                    }
                }
                @Override
                public void onFailure(Call<Object> call, Throwable t) {
                    callback.onError("Network error: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            callback.onError("Exception: " + e.getMessage());
        }
    }


    /**
     * Get admin statistics
     */
    public static void getAdminStatistics(ApiCallback<Object> callback) {
        try {
            getApiService().getAdminStatistics().enqueue(new Callback<Object>() {
                @Override
                public void onResponse(Call<Object> call, Response<Object> response) {
                    if (response.isSuccessful()) {
                        callback.onSuccess(response.body());
                    } else {
                        callback.onError("Error: " + response.code());
                    }
                }
                @Override
                public void onFailure(Call<Object> call, Throwable t) {
                    callback.onError("Network error: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            callback.onError("Exception: " + e.getMessage());
        }
    }

    /**
     * Get profile
     */
    public static void getProfile(String userId, ApiCallback<Object> callback) {
        try {
            getApiService().getProfile(userId).enqueue(new Callback<Object>() {
                @Override
                public void onResponse(Call<Object> call, Response<Object> response) {
                    if (response.isSuccessful()) {
                        callback.onSuccess(response.body());
                    } else {
                        callback.onError("Error: " + response.code());
                    }
                }
                @Override
                public void onFailure(Call<Object> call, Throwable t) {
                    callback.onError("Network error: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            callback.onError("Exception: " + e.getMessage());
        }
    }

    /**
     * Update profile
     */
    public static void updateProfile(String userId, String firstName, String lastName, ApiCallback<Object> callback) {
        try {
            java.util.Map<String, String> body = new java.util.HashMap<>();
            body.put("firstName", firstName);
            body.put("lastName", lastName);
            getApiService().updateProfile(userId, body).enqueue(new Callback<Object>() {
                @Override
                public void onResponse(Call<Object> call, Response<Object> response) {
                    if (response.isSuccessful()) {
                        callback.onSuccess(response.body());
                    } else {
                        callback.onError("Error: " + response.code());
                    }
                }
                @Override
                public void onFailure(Call<Object> call, Throwable t) {
                    callback.onError("Network error: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            callback.onError("Exception: " + e.getMessage());
        }
    }

    /**
     * Delete user
     */
    public static void deleteUser(String userId, ApiCallback<Object> callback) {
        try {
            getApiService().deleteUser(userId).enqueue(new Callback<Object>() {
                @Override
                public void onResponse(Call<Object> call, Response<Object> response) {
                    if (response.isSuccessful()) {
                        callback.onSuccess(response.body());
                    } else {
                        callback.onError("Error: " + response.code());
                    }
                }
                @Override
                public void onFailure(Call<Object> call, Throwable t) {
                    callback.onError("Network error: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            callback.onError("Exception: " + e.getMessage());
        }
    }

    /**
     * Add witness
     */
    public static void addWitness(Integer reportId, java.util.Map<String, Object> witnessData, ApiCallback<Object> callback) {
        try {
            getApiService().addWitness(reportId, witnessData).enqueue(new Callback<Object>() {
                @Override
                public void onResponse(Call<Object> call, Response<Object> response) {
                    if (response.isSuccessful()) {
                        callback.onSuccess(response.body());
                    } else {
                        callback.onError("Error: " + response.code());
                    }
                }
                @Override
                public void onFailure(Call<Object> call, Throwable t) {
                    callback.onError("Network error: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            callback.onError("Exception: " + e.getMessage());
        }
    }

    /**
     * Create officer
     */
    public static void createOfficer(String firstName, String lastName, String email, String rank, String badgeNumber, ApiCallback<Object> callback) {
        try {
            java.util.Map<String, String> body = new java.util.HashMap<>();
            body.put("firstName", firstName);
            body.put("lastName", lastName);
            body.put("email", email);
            body.put("rank", rank);
            body.put("badgeNumber", badgeNumber);
            getApiService().createOfficer(body).enqueue(new Callback<Object>() {
                @Override
                public void onResponse(Call<Object> call, Response<Object> response) {
                    if (response.isSuccessful()) {
                        callback.onSuccess(response.body());
                    } else {
                        callback.onError("Error: " + response.code());
                    }
                }
                @Override
                public void onFailure(Call<Object> call, Throwable t) {
                    callback.onError("Network error: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            callback.onError("Exception: " + e.getMessage());
        }
    }

    /**
     * Register admin
     */
    public static void registerAdmin(String email, String password, String firstName, String lastName, ApiCallback<Object> callback) {
        try {
            java.util.Map<String, String> body = new java.util.HashMap<>();
            body.put("email", email);
            body.put("password", password);
            body.put("firstName", firstName);
            body.put("lastName", lastName);
            getApiService().registerAdmin(body).enqueue(new Callback<Object>() {
                @Override
                public void onResponse(Call<Object> call, Response<Object> response) {
                    if (response.isSuccessful()) {
                        callback.onSuccess(response.body());
                    } else {
                        callback.onError("Error: " + response.code());
                    }
                }
                @Override
                public void onFailure(Call<Object> call, Throwable t) {
                    callback.onError("Network error: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            callback.onError("Exception: " + e.getMessage());
        }
    }

    /**
     * Change password
     */
    public static void changePassword(String userId, java.util.Map<String, String> body, ApiCallback<Object> callback) {
        try {
            getApiService().changePassword(userId, body).enqueue(new Callback<Object>() {
                @Override
                public void onResponse(Call<Object> call, Response<Object> response) {
                    if (response.isSuccessful()) {
                        callback.onSuccess(response.body());
                    } else {
                        callback.onError("Error: " + response.code());
                    }
                }
                @Override
                public void onFailure(Call<Object> call, Throwable t) {
                    callback.onError("Network error: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            callback.onError("Exception: " + e.getMessage());
        }
    }

    /**
     * Update officer
     */
    public static void updateOfficer(int officerId, java.util.Map<String, Object> body, ApiCallback<Object> callback) {
        try {
            getApiService().updateOfficer(officerId, body).enqueue(new Callback<Object>() {
                @Override
                public void onResponse(Call<Object> call, Response<Object> response) {
                    if (response.isSuccessful()) {
                        callback.onSuccess(response.body());
                    } else {
                        callback.onError("Error: " + response.code());
                    }
                }
                @Override
                public void onFailure(Call<Object> call, Throwable t) {
                    callback.onError("Network error: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            callback.onError("Exception: " + e.getMessage());
        }
    }

    /**
     * Delete officer
     */
    public static void deleteOfficer(int officerId, ApiCallback<Object> callback) {
        try {
            getApiService().deleteOfficer(officerId).enqueue(new Callback<Object>() {
                @Override
                public void onResponse(Call<Object> call, Response<Object> response) {
                    if (response.isSuccessful()) {
                        callback.onSuccess(response.body());
                    } else {
                        callback.onError("Error: " + response.code());
                    }
                }
                @Override
                public void onFailure(Call<Object> call, Throwable t) {
                    callback.onError("Network error: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            callback.onError("Exception: " + e.getMessage());
        }
    }

    /**
     * Get admin officers
     */
    public static void getAdminOfficers(ApiCallback<java.util.List<com.example.blottermanagementsystem.data.entity.Officer>> callback) {
        try {
            getApiService().getAllOfficers().enqueue(new Callback<java.util.List<com.example.blottermanagementsystem.data.entity.Officer>>() {
                @Override
                public void onResponse(Call<java.util.List<com.example.blottermanagementsystem.data.entity.Officer>> call, Response<java.util.List<com.example.blottermanagementsystem.data.entity.Officer>> response) {
                    if (response.isSuccessful()) {
                        callback.onSuccess(response.body());
                    } else {
                        callback.onError("Error: " + response.code());
                    }
                }
                @Override
                public void onFailure(Call<java.util.List<com.example.blottermanagementsystem.data.entity.Officer>> call, Throwable t) {
                    callback.onError("Network error: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            callback.onError("Exception: " + e.getMessage());
        }
    }

    /**
     * Get assigned reports
     */
    public static void getAssignedReports(ApiCallback<java.util.List<com.example.blottermanagementsystem.data.entity.BlotterReport>> callback) {
        try {
            getApiService().getAllReports().enqueue(new Callback<java.util.List<com.example.blottermanagementsystem.data.entity.BlotterReport>>() {
                @Override
                public void onResponse(Call<java.util.List<com.example.blottermanagementsystem.data.entity.BlotterReport>> call, Response<java.util.List<com.example.blottermanagementsystem.data.entity.BlotterReport>> response) {
                    if (response.isSuccessful()) {
                        callback.onSuccess(response.body());
                    } else {
                        callback.onError("Error: " + response.code());
                    }
                }
                @Override
                public void onFailure(Call<java.util.List<com.example.blottermanagementsystem.data.entity.BlotterReport>> call, Throwable t) {
                    callback.onError("Network error: " + t.getMessage());
                }
            });
        } catch (Exception e) {
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

// FCM Token Response
class SaveFCMTokenResponse {
    public boolean success;
    public String message;
}
