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
    public static void verifyEmail(String email, String code, ApiCallback<String> callback) {
        com.example.blottermanagementsystem.data.api.ApiClient.verifyEmail(email, code, 
            new com.example.blottermanagementsystem.data.api.ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String result) { callback.onSuccess(result); }
                @Override
                public void onError(String errorMessage) { callback.onError(errorMessage); }
            });
    }
    
    /**
     * Send verification code to email
     */
    public static void sendVerificationCode(String email, ApiCallback<String> callback) {
        com.example.blottermanagementsystem.data.api.ApiClient.sendVerificationCode(email, 
            new com.example.blottermanagementsystem.data.api.ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String result) { callback.onSuccess(result); }
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
    public static void deleteReport(int reportId, ApiCallback<String> callback) {
        com.example.blottermanagementsystem.data.api.ApiClient.deleteReport(reportId, 
            new com.example.blottermanagementsystem.data.api.ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String result) { callback.onSuccess(result); }
                @Override
                public void onError(String errorMessage) { callback.onError(errorMessage); }
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
