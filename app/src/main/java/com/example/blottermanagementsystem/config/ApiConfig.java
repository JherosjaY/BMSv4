package com.example.blottermanagementsystem.config;

/**
 * API Configuration - Centralized API endpoints and base URL
 */
public class ApiConfig {
    // Base URL for API calls
    public static final String BASE_URL = "https://bmsv3-backend.onrender.com/api/";
    
    // Alternative local development URL
    public static final String LOCAL_BASE_URL = "http://192.168.1.100:3000/api/";
    
    // API Endpoints
    public static class Endpoints {
        // Auth endpoints
        public static final String REGISTER = "auth/register";
        public static final String LOGIN = "auth/login";
        public static final String VERIFY_EMAIL = "auth/verify-email";
        public static final String REFRESH_TOKEN = "auth/refresh";
        public static final String LOGOUT = "auth/logout";
        
        // Report endpoints
        public static final String REPORTS = "reports";
        public static final String REPORT_DETAIL = "reports/{id}";
        public static final String CREATE_REPORT = "reports";
        public static final String UPDATE_REPORT = "reports/{id}";
        public static final String DELETE_REPORT = "reports/{id}";
        
        // Evidence endpoints
        public static final String EVIDENCE = "reports/{id}/evidence";
        public static final String UPLOAD_EVIDENCE = "reports/{id}/evidence";
        public static final String DELETE_EVIDENCE = "reports/{id}/evidence/{evidenceId}";
        
        // Suspects endpoints
        public static final String SUSPECTS = "reports/{id}/suspects";
        
        // Witnesses endpoints
        public static final String WITNESSES = "reports/{id}/witnesses";
        
        // User endpoints
        public static final String USER_PROFILE = "users/profile";
        public static final String UPDATE_PROFILE = "users/profile";
        
        // Dashboard endpoints
        public static final String DASHBOARD = "dashboard";
        public static final String ANALYTICS = "analytics";
    }
    
    // Timeout settings (in seconds)
    public static final int CONNECT_TIMEOUT = 30;
    public static final int READ_TIMEOUT = 30;
    public static final int WRITE_TIMEOUT = 30;
    
    // Retry settings
    public static final int MAX_RETRIES = 3;
    public static final int RETRY_DELAY_MS = 1000;
}
