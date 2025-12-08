package com.example.blottermanagementsystem.config;

/**
 * Application Constants - Centralized constant values
 */
public class Constants {
    // Request codes
    public static final int REQUEST_CODE_PICK_IMAGE = 1001;
    public static final int REQUEST_CODE_CAMERA = 1002;
    public static final int REQUEST_CODE_AUDIO = 1003;
    public static final int REQUEST_CODE_LOCATION = 1004;
    public static final int REQUEST_CODE_BIOMETRIC = 1005;
    
    // Permission codes
    public static final int PERMISSION_CODE_CAMERA = 2001;
    public static final int PERMISSION_CODE_STORAGE = 2002;
    public static final int PERMISSION_CODE_LOCATION = 2003;
    public static final int PERMISSION_CODE_AUDIO = 2004;
    
    // Intent extras
    public static final String EXTRA_REPORT_ID = "report_id";
    public static final String EXTRA_REPORT_DATA = "report_data";
    public static final String EXTRA_USER_ID = "user_id";
    public static final String EXTRA_ACTION = "action";
    public static final String EXTRA_MESSAGE = "message";
    
    // Incident types
    public static final String[] INCIDENT_TYPES = {
        "Theft", "Robbery", "Assault", "Burglary", "Vandalism",
        "Traffic Violation", "Lost Property", "Found Property",
        "Disturbance", "Other"
    };
    
    // Report status
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_INVESTIGATING = "investigating";
    public static final String STATUS_RESOLVED = "resolved";
    public static final String STATUS_CLOSED = "closed";
    public static final String STATUS_ARCHIVED = "archived";
    
    // Priority levels
    public static final String PRIORITY_LOW = "low";
    public static final String PRIORITY_MEDIUM = "medium";
    public static final String PRIORITY_HIGH = "high";
    public static final String PRIORITY_CRITICAL = "critical";
    
    // Date formats
    public static final String DATE_FORMAT_FULL = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_FORMAT_DATE_ONLY = "yyyy-MM-dd";
    public static final String DATE_FORMAT_TIME_ONLY = "HH:mm:ss";
    public static final String DATE_FORMAT_DISPLAY = "MMM dd, yyyy";
    
    // File types
    public static final String FILE_TYPE_IMAGE = "image/*";
    public static final String FILE_TYPE_AUDIO = "audio/*";
    public static final String FILE_TYPE_PDF = "application/pdf";
    
    // Max file sizes (in bytes)
    public static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5 MB
    public static final long MAX_AUDIO_SIZE = 10 * 1024 * 1024; // 10 MB
    public static final long MAX_FILE_SIZE = 20 * 1024 * 1024; // 20 MB
    
    // Error messages
    public static final String ERROR_NETWORK = "Network error. Please check your connection.";
    public static final String ERROR_INVALID_INPUT = "Invalid input. Please check your data.";
    public static final String ERROR_UNAUTHORIZED = "Unauthorized. Please log in again.";
    public static final String ERROR_SERVER = "Server error. Please try again later.";
    public static final String ERROR_UNKNOWN = "An unknown error occurred.";
    
    // Success messages
    public static final String SUCCESS_REPORT_CREATED = "Report created successfully";
    public static final String SUCCESS_REPORT_UPDATED = "Report updated successfully";
    public static final String SUCCESS_REPORT_DELETED = "Report deleted successfully";
    public static final String SUCCESS_LOGIN = "Login successful";
    public static final String SUCCESS_LOGOUT = "Logout successful";
}
