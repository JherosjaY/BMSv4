package com.example.blottermanagementsystem.config;

/**
 * Application Configuration - App-wide settings
 */
public class AppConfig {
    // App version
    public static final String APP_VERSION = "3.0.0";
    
    // Feature flags
    public static final boolean ENABLE_BIOMETRIC = true;
    public static final boolean ENABLE_OFFLINE_MODE = true;
    public static final boolean ENABLE_PUSH_NOTIFICATIONS = true;
    public static final boolean ENABLE_AUDIO_RECORDING = true;
    public static final boolean ENABLE_IMAGE_UPLOAD = true;
    
    // Database settings
    public static final String DATABASE_NAME = "blotter_management.db";
    public static final int DATABASE_VERSION = 1;
    
    // SharedPreferences settings
    public static final String PREFS_NAME = "blotter_prefs";
    public static final String PREFS_USER_TOKEN = "user_token";
    public static final String PREFS_USER_ID = "user_id";
    public static final String PREFS_USER_EMAIL = "user_email";
    public static final String PREFS_IS_LOGGED_IN = "is_logged_in";
    public static final String PREFS_LAST_SYNC = "last_sync";
    
    // Firebase settings
    public static final String FIREBASE_PROJECT_ID = "blotter-management-system";
    
    // Cloudinary settings
    public static final String CLOUDINARY_CLOUD_NAME = "your_cloud_name";
    public static final String CLOUDINARY_UPLOAD_PRESET = "your_upload_preset";
    
    // Pagination
    public static final int PAGE_SIZE = 20;
    public static final int INITIAL_LOAD_SIZE = 20;
    
    // Cache settings
    public static final long CACHE_DURATION_MS = 5 * 60 * 1000; // 5 minutes
    public static final long SYNC_INTERVAL_MS = 15 * 60 * 1000; // 15 minutes
    
    // UI settings
    public static final int ANIMATION_DURATION_MS = 300;
    public static final int TOAST_DURATION_MS = 2000;
    
    // Logging
    public static final boolean ENABLE_LOGGING = true;
    public static final String LOG_TAG = "BMS";
}
