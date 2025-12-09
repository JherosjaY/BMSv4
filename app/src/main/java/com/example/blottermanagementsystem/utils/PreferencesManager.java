package com.example.blottermanagementsystem.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * PreferencesManager - Synced with Kotlin version
 * Manages all app preferences including user sessions, settings, and per-user data
 */
public class PreferencesManager {
    private static final String PREFS_NAME = "blotter_prefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_FIRST_NAME = "first_name";
    private static final String KEY_LAST_NAME = "last_name";
    private static final String KEY_PROFILE_PHOTO = "profile_photo";
    private static final String KEY_ONBOARDING_COMPLETED = "onboarding_completed";
    private static final String KEY_GUIDE_COMPLETED = "guide_completed";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_PROFILE_EMOJI = "profile_emoji";
    private static final String KEY_HAS_SELECTED_PFP = "has_selected_pfp";
    private static final String KEY_LANGUAGE = "user_language";
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    private static final String KEY_NOTIFICATION_SOUND = "notification_sound";
    private static final String KEY_BIOMETRIC_ENABLED = "biometric_enabled";
    private static final String KEY_LAST_USER_ID = "last_user_id";
    private static final String KEY_FCM_TOKEN = "fcm_token";
    private static final String KEY_PERMISSIONS_GRANTED = "permissions_granted";
    private static final String KEY_GOOGLE_EMAIL = "google_email";
    private static final String KEY_GOOGLE_DISPLAY_NAME = "google_display_name";
    private static final String KEY_GOOGLE_PHOTO_URL = "google_photo_url";
    private static final String KEY_IS_GOOGLE_ACCOUNT = "is_google_account";
    
    private final SharedPreferences prefs;
    
    public PreferencesManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    // ==================== Login State ====================
    
    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }
    
    public void setLoggedIn(boolean value) {
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).commit();
        android.util.Log.d("PreferencesManager", "✅ setLoggedIn(" + value + ") - Value saved to preferences");
        android.util.Log.d("PreferencesManager", "✅ Verification: isLoggedIn() = " + isLoggedIn());
    }
    
    // ==================== User ID (UUID String) ====================
    
    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }
    
    public void setUserId(String value) {
        prefs.edit().putString(KEY_USER_ID, value).commit();
    }
    
    // Last logged-in user ID (persists after logout)
    public String getLastUserId() {
        return prefs.getString(KEY_LAST_USER_ID, null);
    }
    
    private void setLastUserId(String value) {
        prefs.edit().putString(KEY_LAST_USER_ID, value).apply();
    }
    
    // ==================== User Info ====================
    
    public String getUsername() {
        return prefs.getString(KEY_USERNAME, null);
    }
    
    public void setUsername(String value) {
        prefs.edit().putString(KEY_USERNAME, value).apply();
    }
    
    public String getUserRole() {
        return prefs.getString(KEY_USER_ROLE, null);
    }
    
    public void setUserRole(String value) {
        prefs.edit().putString(KEY_USER_ROLE, value).commit();
    }
    
    public String getFirstName() {
        return prefs.getString(KEY_FIRST_NAME, null);
    }
    
    public void setFirstName(String value) {
        prefs.edit().putString(KEY_FIRST_NAME, value).commit();
    }
    
    public String getLastName() {
        return prefs.getString(KEY_LAST_NAME, null);
    }
    
    public void setLastName(String value) {
        prefs.edit().putString(KEY_LAST_NAME, value).commit();
    }
    
    public String getProfilePhoto() {
        return prefs.getString(KEY_PROFILE_PHOTO, null);
    }
    
    public void setProfilePhoto(String value) {
        prefs.edit().putString(KEY_PROFILE_PHOTO, value).apply();
    }
    
    public String getGender() {
        return prefs.getString("user_gender", "Male");
    }
    
    public void setGender(String value) {
        prefs.edit().putString("user_gender", value).commit();
    }
    
    // ==================== Onboarding ====================
    
    public boolean isOnboardingCompleted() {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false);
    }
    
    public void setOnboardingCompleted(boolean value) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).commit();
        android.util.Log.d("PreferencesManager", "✅ setOnboardingCompleted(" + value + ") - Saved synchronously");
    }
    
    // ==================== Guide ====================
    
    public boolean isGuideCompleted() {
        return prefs.getBoolean(KEY_GUIDE_COMPLETED, false);
    }
    
    public void setGuideCompleted(boolean value) {
        prefs.edit().putBoolean(KEY_GUIDE_COMPLETED, value).apply();
    }
    
    // ==================== Profile Picture ====================
    
    public boolean hasSelectedPfp() {
        return prefs.getBoolean(KEY_HAS_SELECTED_PFP, false);
    }
    
    public void setHasSelectedPfp(boolean value) {
        prefs.edit().putBoolean(KEY_HAS_SELECTED_PFP, value).apply();
    }
    
    // ==================== Dark Mode (Per-User) ====================
    
    public boolean isDarkMode() {
        String currentUserId = getUserId();
        if (currentUserId != null && !currentUserId.isEmpty()) {
            return prefs.getBoolean("dark_mode_user_" + currentUserId, true);
        } else {
            return prefs.getBoolean("dark_mode_default", true);
        }
    }
    
    public void setDarkMode(boolean value) {
        String currentUserId = getUserId();
        if (currentUserId != null && !currentUserId.isEmpty()) {
            prefs.edit().putBoolean("dark_mode_user_" + currentUserId, value).apply();
        } else {
            prefs.edit().putBoolean(KEY_DARK_MODE, value).apply();
        }
    }

    
    // ==================== Profile Emoji (Per-User) ====================
    
    public String getProfileEmoji() {
        String currentUserId = getUserId();
        if (currentUserId != null && !currentUserId.isEmpty()) {
            return prefs.getString("profile_emoji_" + currentUserId, "👤");
        } else {
            return "👤";
        }
    }
    
    public void setProfileEmoji(String value) {
        String currentUserId = getUserId();
        if (currentUserId != null && !currentUserId.isEmpty()) {
            prefs.edit().putString("profile_emoji_" + currentUserId, value).apply();
        }
    }
    
    // ==================== Profile Image URI (Per-User) ====================
    
    public String getProfileImageUri() {
        String currentUserId = getUserId();
        if (currentUserId != null && !currentUserId.isEmpty()) {
            return prefs.getString("profile_image_uri_" + currentUserId, null);
        } else {
            return null;
        }
    }
    
    public void setProfileImageUri(String value) {
        String currentUserId = getUserId();
        if (currentUserId != null && !currentUserId.isEmpty()) {
            prefs.edit().putString("profile_image_uri_" + currentUserId, value).apply();
        }
    }
    
    // ==================== Has Selected Profile Picture (Per-User) ====================
    
    public boolean hasSelectedProfilePicture() {
        String currentUserId = getUserId();
        if (currentUserId != null && !currentUserId.isEmpty()) {
            return prefs.getBoolean("has_selected_pfp_user_" + currentUserId, false);
        } else {
            return prefs.getBoolean(KEY_HAS_SELECTED_PFP, false);
        }
    }
    
    public void setHasSelectedProfilePicture(boolean value) {
        String currentUserId = getUserId();
        if (currentUserId != null && !currentUserId.isEmpty()) {
            prefs.edit().putBoolean("has_selected_pfp_user_" + currentUserId, value).commit();
        } else {
            prefs.edit().putBoolean(KEY_HAS_SELECTED_PFP, value).apply();
        }
    }
    
    // ==================== Must Change Password (Per-User) ====================
    
    public boolean mustChangePassword() {
        String currentUserId = getUserId();
        if (currentUserId != null && !currentUserId.isEmpty()) {
            return prefs.getBoolean("must_change_password_user_" + currentUserId, false);
        } else {
            return prefs.getBoolean("must_change_password", false);
        }
    }
    
    public void setMustChangePassword(boolean value) {
        String currentUserId = getUserId();
        if (currentUserId != null && !currentUserId.isEmpty()) {
            prefs.edit().putBoolean("must_change_password_user_" + currentUserId, value).apply();
        } else {
            prefs.edit().putBoolean("must_change_password", value).apply();
        }
    }
    
    // ==================== Has Changed Password (Per-User) ====================
    
    public boolean hasPasswordChanged() {
        String currentUserId = getUserId();
        if (currentUserId != null && !currentUserId.isEmpty()) {
            return prefs.getBoolean("password_changed_user_" + currentUserId, false);
        } else {
            return prefs.getBoolean("password_changed", false);
        }
    }
    
    public void setPasswordChanged(boolean value) {
        String currentUserId = getUserId();
        if (currentUserId != null && !currentUserId.isEmpty()) {
            prefs.edit().putBoolean("password_changed_user_" + currentUserId, value).commit();
        } else {
            prefs.edit().putBoolean("password_changed", value).commit();
        }
    }
    
    // ==================== Language (Per-User) ====================
    
    public String getUserLanguage() {
        String currentUserId = getUserId();
        if (currentUserId != null && !currentUserId.isEmpty()) {
            return prefs.getString("language_user_" + currentUserId, "en");
        } else {
            return prefs.getString(KEY_LANGUAGE, "en");
        }
    }
    
    public void setUserLanguage(String value) {
        String currentUserId = getUserId();
        if (currentUserId != null && !currentUserId.isEmpty()) {
            prefs.edit().putString("language_user_" + currentUserId, value).apply();
        } else {
            prefs.edit().putString(KEY_LANGUAGE, value).apply();
        }
    }
    
    // ==================== Role Helper ====================
    
    public String getRole() {
        String role = getUserRole();
        return role != null ? role : "User";
    }
    
    // ==================== Notification Settings ====================
    
    public boolean isNotificationsEnabled() {
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
    }
    
    public void setNotificationsEnabled(boolean value) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, value).apply();
    }
    
    public boolean isNotificationSoundEnabled() {
        return prefs.getBoolean(KEY_NOTIFICATION_SOUND, true);
    }
    
    public void setNotificationSoundEnabled(boolean value) {
        prefs.edit().putBoolean(KEY_NOTIFICATION_SOUND, value).apply();
    }
    
    // ==================== Biometric (Per-User) ====================
    
    public boolean isBiometricEnabled() {
        String currentUserId = getUserId();
        if (currentUserId != null && !currentUserId.isEmpty()) {
            return prefs.getBoolean("biometric_enabled_user_" + currentUserId, false);
        } else {
            return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false);
        }
    }
    
    public void setBiometricEnabled(boolean value) {
        String currentUserId = getUserId();
        if (currentUserId != null && !currentUserId.isEmpty()) {
            prefs.edit().putBoolean("biometric_enabled_user_" + currentUserId, value).apply();
        } else {
            prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply();
        }
    }
    
    public String getBiometricPin() {
        String currentUserId = getUserId();
        if (currentUserId != null && !currentUserId.isEmpty()) {
            return prefs.getString("biometric_pin_user_" + currentUserId, "");
        } else {
            return "";
        }
    }
    
    public void setBiometricPin(String value) {
        String currentUserId = getUserId();
        if (currentUserId != null && !currentUserId.isEmpty()) {
            prefs.edit().putString("biometric_pin_user_" + currentUserId, value).apply();
        }
    }
    
    // Helper to check if biometric is enabled for a specific user
    public boolean isBiometricEnabledForUser(String userId) {
        return prefs.getBoolean("biometric_enabled_user_" + userId, false);
    }
    
    // Helper to get PIN for a specific user
    public String getBiometricPinForUser(String userId) {
        String pin = prefs.getString("biometric_pin_user_" + userId, "");
        return pin != null ? pin : "";
    }
    
    // ==================== FCM Token ====================
    
    public String getFcmToken() {
        return prefs.getString(KEY_FCM_TOKEN, null);
    }
    
    public void setFcmToken(String value) {
        prefs.edit().putString(KEY_FCM_TOKEN, value).apply();
    }
    
    // ==================== Permissions ====================
    
    public boolean isPermissionsGranted() {
        return prefs.getBoolean(KEY_PERMISSIONS_GRANTED, false);
    }
    
    public void setPermissionsGranted(boolean value) {
        prefs.edit().putBoolean(KEY_PERMISSIONS_GRANTED, value).commit();
        android.util.Log.d("PreferencesManager", "✅ setPermissionsGranted(" + value + ") - Saved synchronously");
    }
    
    // ==================== Save User Session ====================
    
    public void saveUserSession(int userId, String username, String role, 
                                String firstName, String lastName, String profilePhoto) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putInt(KEY_USER_ID, userId);
        editor.putInt(KEY_LAST_USER_ID, userId);  // Save last user ID
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_USER_ROLE, role);
        editor.putString("user_role_" + userId, role);  // Save role per user
        editor.putString(KEY_FIRST_NAME, firstName);
        editor.putString(KEY_LAST_NAME, lastName);
        editor.putString(KEY_PROFILE_PHOTO, profilePhoto);
        editor.commit();  // Use commit() for immediate save
    }
    
    // ==================== Clear Session ====================
    
    public void clearSession() {
        String currentUserId = getUserId();
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_IS_LOGGED_IN);
        editor.remove(KEY_USER_ID);
        editor.remove(KEY_USERNAME);
        editor.remove(KEY_USER_ROLE);
        editor.remove(KEY_FIRST_NAME);
        editor.remove(KEY_LAST_NAME);
        editor.remove(KEY_PROFILE_PHOTO);
        
        // Clear Google account info to prevent cross-user contamination
        editor.remove(KEY_GOOGLE_EMAIL);
        editor.remove(KEY_GOOGLE_DISPLAY_NAME);
        editor.remove(KEY_GOOGLE_PHOTO_URL);
        editor.remove(KEY_IS_GOOGLE_ACCOUNT);
        
        // ✅ IMPORTANT: DO NOT remove password_changed flag on logout!
        // Each officer's password_changed flag is stored per-user ID (password_changed_user_X)
        // Removing it would force password change on next login even if already changed
        // Password should only be reset if admin explicitly resets it
        
        // DON'T remove per-user profile data (profile_image_uri_*, profile_emoji_*, etc.)
        // DON'T remove FCM token - it's device-specific, not user-specific
        // This allows users to keep their profile pictures when they log back in
        editor.apply();
        
        android.util.Log.d("PreferencesManager", "✅ Session cleared (password_changed flag preserved for each officer)");
    }
    
    // ==================== Generic Helpers ====================
    
    public void saveBoolean(String key, boolean value) {
        prefs.edit().putBoolean(key, value).commit();
        android.util.Log.d("PreferencesManager", "✅ saveBoolean(" + key + ", " + value + ") - Saved synchronously");
    }
    
    public boolean getBoolean(String key, boolean defaultValue) {
        return prefs.getBoolean(key, defaultValue);
    }
    
    public void saveString(String key, String value) {
        prefs.edit().putString(key, value).apply();
    }
    
    public String getString(String key, String defaultValue) {
        return prefs.getString(key, defaultValue);
    }
    
    public void saveInt(String key, int value) {
        prefs.edit().putInt(key, value).apply();
    }
    
    public int getInt(String key, int defaultValue) {
        return prefs.getInt(key, defaultValue);
    }
    
    // Legacy methods for compatibility
    public void logout() {
        clearSession();
    }
    
    public boolean isPushNotificationsEnabled() {
        return isNotificationsEnabled();
    }
    
    public void setPushNotificationsEnabled(boolean value) {
        setNotificationsEnabled(value);
    }
    
    public boolean isEmailNotificationsEnabled() {
        return getBoolean("email_notifications", true);
    }
    
    public void setEmailNotificationsEnabled(boolean value) {
        saveBoolean("email_notifications", value);
    }
    
    public boolean isSmsNotificationsEnabled() {
        return getBoolean("sms_notifications", true);
    }
    
    public void setSmsNotificationsEnabled(boolean value) {
        saveBoolean("sms_notifications", value);
    }
    
    // ==================== Google Account Info ====================
    
    public void saveGoogleAccountInfo(String email, String displayName, String photoUrl) {
        prefs.edit()
            .putString(KEY_GOOGLE_EMAIL, email)
            .putString(KEY_GOOGLE_DISPLAY_NAME, displayName)
            .putString(KEY_GOOGLE_PHOTO_URL, photoUrl)
            .putBoolean(KEY_IS_GOOGLE_ACCOUNT, true)
            .apply();
    }
    
    public boolean isGoogleAccount() {
        return prefs.getBoolean(KEY_IS_GOOGLE_ACCOUNT, false);
    }
    
    public String getGoogleEmail() {
        return prefs.getString(KEY_GOOGLE_EMAIL, null);
    }
    
    public String getGoogleDisplayName() {
        return prefs.getString(KEY_GOOGLE_DISPLAY_NAME, null);
    }
    
    public String getGooglePhotoUrl() {
        return prefs.getString(KEY_GOOGLE_PHOTO_URL, null);
    }
    
    public void clearGoogleAccountInfo() {
        prefs.edit()
            .remove(KEY_GOOGLE_EMAIL)
            .remove(KEY_GOOGLE_DISPLAY_NAME)
            .remove(KEY_GOOGLE_PHOTO_URL)
            .remove(KEY_IS_GOOGLE_ACCOUNT)
            .apply();
    }
    
    // ==================== Temporary User Data (For Email Verification) ====================
    // ✅ FIXED: Store user data temporarily during registration
    // User is ONLY saved to database AFTER successful email verification
    
    private static final String KEY_TEMP_USERNAME = "temp_username";
    private static final String KEY_TEMP_EMAIL = "temp_email";
    private static final String KEY_TEMP_PASSWORD = "temp_password";
    
    public void setTempUsername(String username) {
        prefs.edit().putString(KEY_TEMP_USERNAME, username).apply();
    }
    
    public String getTempUsername() {
        return prefs.getString(KEY_TEMP_USERNAME, null);
    }
    
    public void setTempEmail(String email) {
        prefs.edit().putString(KEY_TEMP_EMAIL, email).apply();
    }
    
    public String getTempEmail() {
        return prefs.getString(KEY_TEMP_EMAIL, null);
    }
    
    public void setTempPassword(String password) {
        prefs.edit().putString(KEY_TEMP_PASSWORD, password).apply();
    }
    
    public String getTempPassword() {
        return prefs.getString(KEY_TEMP_PASSWORD, null);
    }
    
    public void clearTempUserData() {
        prefs.edit()
            .remove(KEY_TEMP_USERNAME)
            .remove(KEY_TEMP_EMAIL)
            .remove(KEY_TEMP_PASSWORD)
            .apply();
        android.util.Log.d("PreferencesManager", "✅ Cleared temp user data");
    }
    
    public void clearUserData() {
        prefs.edit()
            .remove(KEY_IS_LOGGED_IN)
            .remove(KEY_USER_ID)
            .remove(KEY_USERNAME)
            .remove(KEY_USER_ROLE)
            .remove(KEY_FIRST_NAME)
            .remove(KEY_LAST_NAME)
            .remove(KEY_PROFILE_PHOTO)
            .remove(KEY_FCM_TOKEN)
            .apply();
        android.util.Log.d("PreferencesManager", "✅ Cleared all user data");
    }
}
