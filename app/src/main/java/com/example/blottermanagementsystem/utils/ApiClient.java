package com.example.blottermanagementsystem.utils;

import android.content.Context;

import com.example.blottermanagementsystem.data.api.ApiService;
import com.example.blottermanagementsystem.data.entity.BlotterReport;

import java.util.List;

/**
 * ApiClient Wrapper - For backward compatibility
 * Delegates to com.example.blottermanagementsystem.data.api.ApiClient
 */
public class ApiClient {
    
    /**
     * Initialize API Client - delegates to data.api.ApiClient
     */
    public static void initApiClient(Context context) {
        com.example.blottermanagementsystem.data.api.ApiClient.initApiClient(context);
    }
    
    /**
     * Initialize API Client (legacy)
     */
    public static void initApiClient() {
        com.example.blottermanagementsystem.data.api.ApiClient.initApiClient();
    }
    
    /**
     * Get API Service instance
     */
    public static ApiService getApiService() {
        return com.example.blottermanagementsystem.data.api.ApiClient.getApiService();
    }
    
    /**
     * Verify email with 6-digit code
     */
    public static void verifyEmail(String email, String code, ApiCallback<Object> callback) {
        com.example.blottermanagementsystem.data.api.ApiClient.verifyEmail(email, code, 
            new com.example.blottermanagementsystem.data.api.ApiClient.ApiCallback<Object>() {
                @Override
                public void onSuccess(Object result) { callback.onSuccess(result); }
                @Override
                public void onError(String errorMessage) { callback.onError(errorMessage); }
            });
    }
    
    /**
     * Send verification code to email
     */
    public static void sendVerificationCode(String email, ApiCallback<Object> callback) {
        com.example.blottermanagementsystem.data.api.ApiClient.sendVerificationCode(email, 
            new com.example.blottermanagementsystem.data.api.ApiClient.ApiCallback<Object>() {
                @Override
                public void onSuccess(Object result) { callback.onSuccess(result); }
                @Override
                public void onError(String errorMessage) { callback.onError(errorMessage); }
            });
    }
    
    /**
     * Login user
     */
    @SuppressWarnings("unchecked")
    public static void login(String username, String password, ApiCallback<Object> callback) {
        com.example.blottermanagementsystem.data.api.ApiClient.login(username, password, 
            (com.example.blottermanagementsystem.data.api.ApiClient.ApiCallback) 
            new com.example.blottermanagementsystem.data.api.ApiClient.ApiCallback() {
                @Override
                public void onSuccess(Object result) { 
                    callback.onSuccess(result); 
                }
                @Override
                public void onError(String errorMessage) { callback.onError(errorMessage); }
            });
    }
    
    /**
     * Register user
     */
    public static void register(String username, String email, String password, String confirmPassword, ApiCallback<Object> callback) {
        com.example.blottermanagementsystem.data.api.ApiClient.register(username, email, password, confirmPassword,
            new com.example.blottermanagementsystem.data.api.ApiClient.ApiCallback<Object>() {
                @Override
                public void onSuccess(Object result) { callback.onSuccess(result); }
                @Override
                public void onError(String errorMessage) { callback.onError(errorMessage); }
            });
    }
    
    /**
     * Create a new report
     */
    public static void createReport(BlotterReport report, ApiCallback<BlotterReport> callback) {
        com.example.blottermanagementsystem.data.api.ApiClient.createReport(report, 
            new com.example.blottermanagementsystem.data.api.ApiClient.ApiCallback<BlotterReport>() {
                @Override
                public void onSuccess(BlotterReport result) { callback.onSuccess(result); }
                @Override
                public void onError(String errorMessage) { callback.onError(errorMessage); }
            });
    }
    
    /**
     * Get all reports
     */
    public static void getAllReports(ApiCallback<List<BlotterReport>> callback) {
        com.example.blottermanagementsystem.data.api.ApiClient.getAllReports(
            new com.example.blottermanagementsystem.data.api.ApiClient.ApiCallback<List<BlotterReport>>() {
                @Override
                public void onSuccess(List<BlotterReport> result) { callback.onSuccess(result); }
                @Override
                public void onError(String errorMessage) { callback.onError(errorMessage); }
            });
    }
    
    /**
     * Get report by ID
     */
    public static void getReportById(int reportId, ApiCallback<BlotterReport> callback) {
        com.example.blottermanagementsystem.data.api.ApiClient.getReportById(reportId, 
            new com.example.blottermanagementsystem.data.api.ApiClient.ApiCallback<BlotterReport>() {
                @Override
                public void onSuccess(BlotterReport result) { callback.onSuccess(result); }
                @Override
                public void onError(String errorMessage) { callback.onError(errorMessage); }
            });
    }
    
    /**
     * Update report
     */
    public static void updateReport(int reportId, BlotterReport report, ApiCallback<BlotterReport> callback) {
        com.example.blottermanagementsystem.data.api.ApiClient.updateReport(reportId, report, 
            new com.example.blottermanagementsystem.data.api.ApiClient.ApiCallback<BlotterReport>() {
                @Override
                public void onSuccess(BlotterReport result) { callback.onSuccess(result); }
                @Override
                public void onError(String errorMessage) { callback.onError(errorMessage); }
            });
    }
    
    /**
     * Delete report
     */
    public static void deleteReport(int reportId, ApiCallback<Object> callback) {
        com.example.blottermanagementsystem.data.api.ApiClient.deleteReport(reportId, 
            new com.example.blottermanagementsystem.data.api.ApiClient.ApiCallback<Object>() {
                @Override
                public void onSuccess(Object result) { callback.onSuccess(result); }
                @Override
                public void onError(String errorMessage) { callback.onError(errorMessage); }
            });
    }
    
    /**
     * Get all hearings
     */
    public static void getHearings(ApiCallback<java.util.List<com.example.blottermanagementsystem.data.entity.Hearing>> callback) {
        com.example.blottermanagementsystem.data.api.ApiClient.getHearings(
            new com.example.blottermanagementsystem.data.api.ApiClient.ApiCallback<java.util.List<com.example.blottermanagementsystem.data.entity.Hearing>>() {
                @Override
                public void onSuccess(java.util.List<com.example.blottermanagementsystem.data.entity.Hearing> result) { callback.onSuccess(result); }
                @Override
                public void onError(String errorMessage) { callback.onError(errorMessage); }
            });
    }
    
    /**
     * Get notifications
     */
    public static void getNotifications(String userId, ApiCallback<java.util.List<com.example.blottermanagementsystem.data.entity.Notification>> callback) {
        com.example.blottermanagementsystem.data.api.ApiClient.getNotifications(userId,
            new com.example.blottermanagementsystem.data.api.ApiClient.ApiCallback<java.util.List<com.example.blottermanagementsystem.data.entity.Notification>>() {
                @Override
                public void onSuccess(java.util.List<com.example.blottermanagementsystem.data.entity.Notification> result) { callback.onSuccess(result); }
                @Override
                public void onError(String errorMessage) { callback.onError(errorMessage); }
            });
    }
    
    /**
     * Get hearings calendar
     */
    public static void getHearingsCalendar(ApiCallback<java.util.List<com.example.blottermanagementsystem.data.entity.Hearing>> callback) {
        com.example.blottermanagementsystem.data.api.ApiClient.getHearingsCalendar(
            new com.example.blottermanagementsystem.data.api.ApiClient.ApiCallback<java.util.List<com.example.blottermanagementsystem.data.entity.Hearing>>() {
                @Override
                public void onSuccess(java.util.List<com.example.blottermanagementsystem.data.entity.Hearing> result) { callback.onSuccess(result); }
                @Override
                public void onError(String errorMessage) { callback.onError(errorMessage); }
            });
    }
    
    /**
     * Mark all notifications as read
     */
    public static void markAllNotificationsAsRead(String userId, ApiCallback<Object> callback) {
        com.example.blottermanagementsystem.data.api.ApiClient.markAllNotificationsAsRead(userId,
            new com.example.blottermanagementsystem.data.api.ApiClient.ApiCallback<Object>() {
                @Override
                public void onSuccess(Object result) { callback.onSuccess(result); }
                @Override
                public void onError(String errorMessage) { callback.onError(errorMessage); }
            });
    }
    
    /**
     * Delete notification
     */
    public static void deleteNotification(Integer notificationId, ApiCallback<Object> callback) {
        com.example.blottermanagementsystem.data.api.ApiClient.deleteNotification(notificationId,
            new com.example.blottermanagementsystem.data.api.ApiClient.ApiCallback<Object>() {
                @Override
                public void onSuccess(Object result) { callback.onSuccess(result); }
                @Override
                public void onError(String errorMessage) { callback.onError(errorMessage); }
            });
    }
    
    /**
     * Forgot password
     */
    public static void forgotPassword(String email, ApiCallback<Object> callback) {
        com.example.blottermanagementsystem.data.api.ApiClient.forgotPassword(email,
            new com.example.blottermanagementsystem.data.api.ApiClient.ApiCallback<Object>() {
                @Override
                public void onSuccess(Object result) { callback.onSuccess(result); }
                @Override
                public void onError(String errorMessage) { callback.onError(errorMessage); }
            });
    }
    
    /**
     * Reset password
     */
    public static void resetPassword(String email, String code, String newPassword, ApiCallback<Object> callback) {
        com.example.blottermanagementsystem.data.api.ApiClient.resetPassword(email, code, newPassword,
            new com.example.blottermanagementsystem.data.api.ApiClient.ApiCallback<Object>() {
                @Override
                public void onSuccess(Object result) { callback.onSuccess(result); }
                @Override
                public void onError(String errorMessage) { callback.onError(errorMessage); }
            });
    }
    
    /**
     * Google sign in
     */
    public static void googleSignIn(String email, String displayName, String photoUrl, ApiCallback<Object> callback) {
        try {
            java.util.Map<String, String> body = new java.util.HashMap<>();
            body.put("email", email);
            if (displayName != null && !displayName.isEmpty()) {
                body.put("displayName", displayName);
            }
            if (photoUrl != null && !photoUrl.isEmpty()) {
                body.put("photoUrl", photoUrl);
            }
            
            // Call the API directly
            @SuppressWarnings("unchecked")
            retrofit2.Call<Object> call = (retrofit2.Call<Object>) (Object) getApiService().googleSignIn(body);
            call.enqueue(new retrofit2.Callback<Object>() {
                @Override
                public void onResponse(retrofit2.Call<Object> call, retrofit2.Response<Object> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        callback.onSuccess(response.body());
                    } else {
                        callback.onError("Error: " + response.code());
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<Object> call, Throwable t) {
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
        com.example.blottermanagementsystem.data.api.ApiClient.getAdminStatistics(
            new com.example.blottermanagementsystem.data.api.ApiClient.ApiCallback<Object>() {
                @Override
                public void onSuccess(Object result) { callback.onSuccess(result); }
                @Override
                public void onError(String errorMessage) { callback.onError(errorMessage); }
            });
    }
    
    /**
     * Get profile
     */
    public static void getProfile(String userId, ApiCallback<Object> callback) {
        com.example.blottermanagementsystem.data.api.ApiClient.getProfile(userId,
            new com.example.blottermanagementsystem.data.api.ApiClient.ApiCallback<Object>() {
                @Override
                public void onSuccess(Object result) { callback.onSuccess(result); }
                @Override
                public void onError(String error) { callback.onError(error); }
            });
    }
    
    /**
     * Delete user
     */
    public static void deleteUser(String userId, ApiCallback<Object> callback) {
        com.example.blottermanagementsystem.data.api.ApiClient.deleteUser(userId,
            new com.example.blottermanagementsystem.data.api.ApiClient.ApiCallback<Object>() {
                @Override
                public void onSuccess(Object result) { callback.onSuccess(result); }
                @Override
                public void onError(String errorMessage) { callback.onError(errorMessage); }
            });
    }
    
    /**
     * Add witness
     */
    public static void addWitness(Integer reportId, java.util.Map<String, Object> witnessData, ApiCallback<Object> callback) {
        com.example.blottermanagementsystem.data.api.ApiClient.addWitness(reportId, witnessData,
            new com.example.blottermanagementsystem.data.api.ApiClient.ApiCallback<Object>() {
                @Override
                public void onSuccess(Object result) { callback.onSuccess(result); }
                @Override
                public void onError(String errorMessage) { callback.onError(errorMessage); }
            });
    }
    
    /**
     * Create officer
     */
    public static void createOfficer(String firstName, String lastName, String email, String rank, String badgeNumber, ApiCallback<Object> callback) {
        com.example.blottermanagementsystem.data.api.ApiClient.createOfficer(firstName, lastName, email, rank, badgeNumber,
            new com.example.blottermanagementsystem.data.api.ApiClient.ApiCallback<Object>() {
                @Override
                public void onSuccess(Object result) { callback.onSuccess(result); }
                @Override
                public void onError(String errorMessage) { callback.onError(errorMessage); }
            });
    }
    
    /**
     * Register admin
     */
    public static void registerAdmin(String email, String password, String firstName, String lastName, ApiCallback<Object> callback) {
        com.example.blottermanagementsystem.data.api.ApiClient.registerAdmin(email, password, firstName, lastName,
            new com.example.blottermanagementsystem.data.api.ApiClient.ApiCallback<Object>() {
                @Override
                public void onSuccess(Object result) { callback.onSuccess(result); }
                @Override
                public void onError(String errorMessage) { callback.onError(errorMessage); }
            });
    }
    
    /**
     * Change password
     */
    public static void changePassword(String userId, String currentPassword, String newPassword, ApiCallback<Object> callback) {
        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("currentPassword", currentPassword);
        body.put("newPassword", newPassword);
        com.example.blottermanagementsystem.data.api.ApiClient.changePassword(userId, body,
            new com.example.blottermanagementsystem.data.api.ApiClient.ApiCallback<Object>() {
                @Override
                public void onSuccess(Object result) { callback.onSuccess(result); }
                @Override
                public void onError(String errorMessage) { callback.onError(errorMessage); }
            });
    }
    
    /**
     * Update officer
     */
    public static void updateOfficer(int officerId, java.util.Map<String, Object> updateData, ApiCallback<Object> callback) {
        com.example.blottermanagementsystem.data.api.ApiClient.updateOfficer(officerId, updateData,
            new com.example.blottermanagementsystem.data.api.ApiClient.ApiCallback<Object>() {
                @Override
                public void onSuccess(Object result) { callback.onSuccess(result); }
                @Override
                public void onError(String errorMessage) { callback.onError(errorMessage); }
            });
    }
    
    /**
     * Delete officer
     */
    public static void deleteOfficer(int officerId, ApiCallback<Object> callback) {
        com.example.blottermanagementsystem.data.api.ApiClient.deleteOfficer(officerId,
            new com.example.blottermanagementsystem.data.api.ApiClient.ApiCallback<Object>() {
                @Override
                public void onSuccess(Object result) { callback.onSuccess(result); }
                @Override
                public void onError(String errorMessage) { callback.onError(errorMessage); }
            });
    }
    
    /**
     * Get admin officers
     */
    public static void getAdminOfficers(ApiCallback<java.util.List<com.example.blottermanagementsystem.data.entity.Officer>> callback) {
        com.example.blottermanagementsystem.data.api.ApiClient.getAdminOfficers(
            new com.example.blottermanagementsystem.data.api.ApiClient.ApiCallback<java.util.List<com.example.blottermanagementsystem.data.entity.Officer>>() {
                @Override
                public void onSuccess(java.util.List<com.example.blottermanagementsystem.data.entity.Officer> result) { callback.onSuccess(result); }
                @Override
                public void onError(String errorMessage) { callback.onError(errorMessage); }
            });
    }
    
    /**
     * Get assigned reports
     */
    public static void getAssignedReports(ApiCallback<java.util.List<com.example.blottermanagementsystem.data.entity.BlotterReport>> callback) {
        com.example.blottermanagementsystem.data.api.ApiClient.getAssignedReports(
            new com.example.blottermanagementsystem.data.api.ApiClient.ApiCallback<java.util.List<com.example.blottermanagementsystem.data.entity.BlotterReport>>() {
                @Override
                public void onSuccess(java.util.List<com.example.blottermanagementsystem.data.entity.BlotterReport> result) { callback.onSuccess(result); }
                @Override
                public void onError(String errorMessage) { callback.onError(errorMessage); }
            });
    }
    
    /**
     * Update user profile
     */
    public static void updateProfile(String userId, String firstName, String lastName, ApiCallback<Object> callback) {
        com.example.blottermanagementsystem.data.api.ApiClient.updateProfile(userId, firstName, lastName,
            new com.example.blottermanagementsystem.data.api.ApiClient.ApiCallback<Object>() {
                @Override
                public void onSuccess(Object result) { callback.onSuccess(result); }
                @Override
                public void onError(String error) { callback.onError(error); }
            });
    }
    
    /**
     * Get user profile
     */
    public static void getUserProfile(String userId, ApiCallback<Object> callback) {
        com.example.blottermanagementsystem.data.api.ApiClient.getProfile(userId,
            new com.example.blottermanagementsystem.data.api.ApiClient.ApiCallback<Object>() {
                @Override
                public void onSuccess(Object result) { callback.onSuccess(result); }
                @Override
                public void onError(String error) { callback.onError(error); }
            });
    }
    
    /**
     * Get admin users
     */
    public static void getAdminUsers(ApiCallback<java.util.List<com.example.blottermanagementsystem.data.entity.User>> callback) {
        // Placeholder - would call backend endpoint
        callback.onSuccess(new java.util.ArrayList<>());
    }
    
    /**
     * Send notification
     */
    public static void sendNotification(String title, String message, String recipientType, 
            java.util.List<com.example.blottermanagementsystem.data.entity.User> selectedUsers,
            java.util.List<com.example.blottermanagementsystem.data.entity.Officer> selectedOfficers,
            ApiCallback<Object> callback) {
        // Placeholder - would call backend endpoint
        callback.onSuccess(new Object());
    }
    
    /**
     * Add evidence
     */
    public static void addEvidence(int reportId, java.util.Map<String, Object> evidenceData, ApiCallback<Object> callback) {
        // Placeholder - would call backend endpoint
        callback.onSuccess(new Object());
    }
    
    /**
     * Deactivate user
     */
    public static void deactivateUser(int userId, ApiCallback<Object> callback) {
        // Placeholder - would call backend endpoint
        callback.onSuccess(new Object());
    }
    
    /**
     * Save FCM token
     */
    public static void saveFCMToken(String userId, String token, String deviceId, ApiCallback<Object> callback) {
        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("userId", userId);
        body.put("token", token);
        body.put("deviceId", deviceId);
        
        getApiService().saveFCMToken(body).enqueue(new retrofit2.Callback<Object>() {
            @Override
            public void onResponse(retrofit2.Call<Object> call, retrofit2.Response<Object> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Error: " + response.code());
                }
            }

            @Override
            public void onFailure(retrofit2.Call<Object> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    /**
     * Generic API callback interface
     */
    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String errorMessage);
    }
}
