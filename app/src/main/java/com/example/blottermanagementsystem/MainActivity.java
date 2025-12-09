package com.example.blottermanagementsystem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import com.example.blottermanagementsystem.utils.NetworkMonitor;
import com.example.blottermanagementsystem.utils.ApiClient;
import com.example.blottermanagementsystem.ui.activities.BaseActivity;
import com.example.blottermanagementsystem.ui.activities.OnboardingActivity;
import com.example.blottermanagementsystem.ui.activities.AdminDashboardActivity;
import com.example.blottermanagementsystem.ui.activities.OfficerDashboardActivity;
import com.example.blottermanagementsystem.ui.activities.UserDashboardActivity;
import com.example.blottermanagementsystem.ui.activities.ProfilePictureSelectionActivity;
import com.example.blottermanagementsystem.utils.PreferencesManager;

public class MainActivity extends BaseActivity {
    
    private PreferencesManager preferencesManager;
    private NetworkMonitor networkMonitor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // MainActivity is just a router - no layout needed
        android.util.Log.d("MainActivity", "🚀 MainActivity started - routing to appropriate screen");
        
        preferencesManager = new PreferencesManager(this);
        networkMonitor = new NetworkMonitor(this);
        
        // CRITICAL FIX: On first launch after clear data, ensure flags are properly initialized
        boolean isOnboardingCompleted = preferencesManager.isOnboardingCompleted();
        boolean isPermissionsGranted = preferencesManager.isPermissionsGranted();
        boolean isLoggedIn = preferencesManager.isLoggedIn();
        
        android.util.Log.d("MainActivity", "Initial state - Onboarding: " + isOnboardingCompleted + ", Permissions: " + isPermissionsGranted + ", LoggedIn: " + isLoggedIn);
        
        // CRITICAL: If onboarding is NOT completed, we're in a fresh/reset state
        // Reset ALL flags to false to ensure clean onboarding flow
        if (!isOnboardingCompleted) {
            android.util.Log.d("MainActivity", "⚠️ FRESH STATE DETECTED - Onboarding not completed");
            android.util.Log.d("MainActivity", "🔄 Resetting ALL flags to ensure clean onboarding flow");
            preferencesManager.setOnboardingCompleted(false);
            preferencesManager.setPermissionsGranted(false);
            preferencesManager.setLoggedIn(false);
            preferencesManager.setHasSelectedProfilePicture(false);
            isOnboardingCompleted = false;
            isPermissionsGranted = false;
            isLoggedIn = false;
            android.util.Log.d("MainActivity", "✅ All flags reset to false - Onboarding will show");
        }
        
        // Determine start destination - SYNCED WITH KOTLIN VERSION (MainActivity.kt lines 231-243)
        // Flags naturally default to false and persist across app launches
        // They only reset when user clears app data (which is correct behavior)
        android.util.Log.d("MainActivity", "🔍 === APP START ROUTING ===");
        android.util.Log.d("MainActivity", "Onboarding completed: " + isOnboardingCompleted);
        android.util.Log.d("MainActivity", "Permissions granted: " + isPermissionsGranted);
        android.util.Log.d("MainActivity", "Logged in: " + isLoggedIn);
        android.util.Log.d("MainActivity", "User Role: " + preferencesManager.getUserRole());
        // KOTLIN LOGIC: Check flags in order (using local variables to ensure consistency)
        if (!isOnboardingCompleted) {
            // 1. Show onboarding first
            android.util.Log.d("MainActivity", "🎬 ONBOARDING NOT COMPLETED - Launching OnboardingActivity");
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
        } else if (!isPermissionsGranted) {
            // 2. Then show permissions
            android.util.Log.d("MainActivity", "🔐 PERMISSIONS NOT GRANTED - Launching PermissionsSetupActivity");
            startActivity(new Intent(this, com.example.blottermanagementsystem.ui.activities.PermissionsSetupActivity.class));
            finish();
        } else if (!isLoggedIn) {
            // 3. Then show login/welcome
            android.util.Log.d("MainActivity", "🔓 NOT LOGGED IN - Going to WelcomeActivity (Login/Register)");
            startActivity(new Intent(this, com.example.blottermanagementsystem.ui.activities.WelcomeActivity.class));
            finish();
        } else {
            // 4. User is logged in - check role and profile picture requirement
            String role = preferencesManager.getUserRole();
            android.util.Log.d("MainActivity", "✅ USER LOGGED IN - User role: " + role);
            
            // Admin and Officer roles require re-login when app is reopened
            // They must go to WelcomeActivity (Login page) for security
            if ("Admin".equals(role)) {
                android.util.Log.d("MainActivity", "👨‍💼 ADMIN ROLE - Requiring re-login, going to WelcomeActivity");
                // Clear login flag to force re-login
                preferencesManager.setLoggedIn(false);
                startActivity(new Intent(this, com.example.blottermanagementsystem.ui.activities.WelcomeActivity.class));
                finish();
            } else if ("Officer".equals(role)) {
                android.util.Log.d("MainActivity", "👮 OFFICER ROLE - Requiring re-login, going to WelcomeActivity");
                // Clear login flag to force re-login
                preferencesManager.setLoggedIn(false);
                startActivity(new Intent(this, com.example.blottermanagementsystem.ui.activities.WelcomeActivity.class));
                finish();
            } else {
                // User role - check if profile is completed in database
                android.util.Log.d("MainActivity", "👤 USER ROLE - Checking profile completion status...");
                checkProfileCompletionAndNavigate();
            }
        }
    }
    
    // ✅ PURE ONLINE: Check profile completion status from preferences
    private void checkProfileCompletionAndNavigate() {
        String userId = preferencesManager.getUserId();
        boolean isProfileCompleted = preferencesManager.hasSelectedProfilePicture();
        
        android.util.Log.d("MainActivity", "🔍 Checking profile for userId: " + userId);
        android.util.Log.d("MainActivity", "📋 Profile Status: isProfileCompleted=" + isProfileCompleted);
        
        Intent intent;
        if (!isProfileCompleted) {
            android.util.Log.d("MainActivity", "🖼️ PROFILE NOT COMPLETED - Going to ProfilePictureSelectionActivity");
            intent = new Intent(this, ProfilePictureSelectionActivity.class);
            intent.putExtra("USER_ID", userId);
        } else {
            android.util.Log.d("MainActivity", "✅ PROFILE COMPLETED - Going to UserDashboardActivity");
            intent = new Intent(this, UserDashboardActivity.class);
        }
        startActivity(intent);
        finish();
    }
    
    private void checkProfilePictureAndNavigate() {
        boolean hasSelectedPfp = preferencesManager.hasSelectedProfilePicture();
        android.util.Log.d("MainActivity", "User hasSelectedProfilePicture: " + hasSelectedPfp);
        
        Intent intent;
        if (!hasSelectedPfp) {
            android.util.Log.d("MainActivity", "→ Going to ProfilePictureSelectionActivity");
            intent = new Intent(this, ProfilePictureSelectionActivity.class);
        } else {
            android.util.Log.d("MainActivity", "→ Going to UserDashboardActivity");
            intent = new Intent(this, UserDashboardActivity.class);
        }
        startActivity(intent);
        finish();
    }
    
    // ✅ PURE ONLINE: Admin account creation is handled by backend
    // No need to create admin locally - backend handles this
    private void createAdminAccountIfNotExists() {
        // Admin account creation is managed by backend API
        // This method is kept for compatibility but does nothing
        android.util.Log.d("MainActivity", "✅ Admin account management delegated to backend");
    }
    
    // ✅ Using SecurityUtils.hashPassword() instead of local implementation for consistency
    
    /**
     * DEBUG METHOD: Reset all flags to see the full onboarding flow
     * Call this to reset: onboarding, permissions, and login flags
     */
    private void resetAllFlags() {
        android.util.Log.d("MainActivity", "🔄 RESETTING ALL FLAGS FOR TESTING");
        preferencesManager.setOnboardingCompleted(false);
        preferencesManager.setPermissionsGranted(false);
        preferencesManager.setLoggedIn(false);
        android.util.Log.d("MainActivity", "✅ All flags reset!");
        android.util.Log.d("MainActivity", "   - onboarding_completed: false");
        android.util.Log.d("MainActivity", "   - permissions_granted: false");
        android.util.Log.d("MainActivity", "   - is_logged_in: false");
    }
}
