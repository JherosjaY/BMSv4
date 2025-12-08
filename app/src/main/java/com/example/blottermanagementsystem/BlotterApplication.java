package com.example.blottermanagementsystem;

import android.app.Application;
import android.util.Log;

import com.example.blottermanagementsystem.config.AppConfig;
import com.example.blottermanagementsystem.data.api.ApiClient;

/**
 * BlotterApplication - Main application class
 * Initializes app-wide components and services
 */
public class BlotterApplication extends Application {
    
    private static final String TAG = AppConfig.LOG_TAG;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        Log.d(TAG, "🚀 Blotter Management System v" + AppConfig.APP_VERSION + " starting...");
        
        // Initialize API Client with context
        ApiClient.initApiClient(this);
        Log.d(TAG, "✅ API Client initialized");
        
        // Initialize other app components here
        // Example: Cloudinary, Firebase, Biometric, etc.
        
        Log.d(TAG, "✅ Application initialized successfully");
    }
}
