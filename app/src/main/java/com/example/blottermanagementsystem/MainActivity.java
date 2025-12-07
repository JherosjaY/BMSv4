package com.example.blottermanagementsystem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import com.example.blottermanagementsystem.data.database.BlotterDatabase;
import com.example.blottermanagementsystem.data.entity.User;
import com.example.blottermanagementsystem.ui.activities.BaseActivity;
import com.example.blottermanagementsystem.ui.activities.OnboardingActivity;
import com.example.blottermanagementsystem.ui.activities.AdminDashboardActivity;
import com.example.blottermanagementsystem.ui.activities.OfficerDashboardActivity;
import com.example.blottermanagementsystem.ui.activities.UserDashboardActivity;
import com.example.blottermanagementsystem.ui.activities.ProfilePictureSelectionActivity;
import com.example.blottermanagementsystem.utils.PreferencesManager;
import java.util.concurrent.Executors;

public class MainActivity extends BaseActivity {
    
    private PreferencesManager preferencesManager;
    private BlotterDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // MainActivity is just a router - no layout needed
        android.util.Log.d("MainActivity", "🚀 MainActivity started - routing to appropriate screen");
        
        preferencesManager = new PreferencesManager(this);
        database = BlotterDatabase.getDatabase(this);
        createAdminAccountIfNotExists();
        
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
    
    // ✅ NEW METHOD: Check profile completion status from database
    private void checkProfileCompletionAndNavigate() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                int userId = preferencesManager.getUserId();
                android.util.Log.d("MainActivity", "🔍 Checking profile for userId: " + userId);
                
                User user = database.userDao().getUserById(userId);
                
                if (user == null) {
                    android.util.Log.e("MainActivity", "❌ User not found in database!");
                    runOnUiThread(() -> {
                        startActivity(new Intent(this, com.example.blottermanagementsystem.ui.activities.WelcomeActivity.class));
                        finish();
                    });
                    return;
                }
                
                // ✅ Check if profile is completed in database
                boolean isProfileCompleted = user.isProfileCompleted();
                String firstName = user.getFirstName();
                String lastName = user.getLastName();
                String profilePhotoUri = user.getProfilePhotoUri();
                
                android.util.Log.d("MainActivity", "📋 Profile Status:");
                android.util.Log.d("MainActivity", "   - isProfileCompleted: " + isProfileCompleted);
                android.util.Log.d("MainActivity", "   - FirstName: " + firstName);
                android.util.Log.d("MainActivity", "   - LastName: " + lastName);
                android.util.Log.d("MainActivity", "   - ProfilePhotoUri: " + (profilePhotoUri != null ? "✅ Set" : "❌ Not set"));
                
                // Update preferences to match database state
                if (isProfileCompleted) {
                    preferencesManager.setHasSelectedProfilePicture(true);
                    preferencesManager.setFirstName(firstName);
                    preferencesManager.setLastName(lastName);
                    android.util.Log.d("MainActivity", "✅ Profile is completed - syncing to preferences");
                }
                
                // Navigate on UI thread
                runOnUiThread(() -> {
                    Intent intent;
                    if (!isProfileCompleted) {
                        android.util.Log.d("MainActivity", "🖼️ PROFILE NOT COMPLETED - Going to ProfilePictureSelectionActivity");
                        intent = new Intent(this, ProfilePictureSelectionActivity.class);
                        intent.putExtra("USER_ID", userId);  // Pass userId to ensure it's available
                    } else {
                        android.util.Log.d("MainActivity", "✅ PROFILE COMPLETED - Going to UserDashboardActivity");
                        intent = new Intent(this, UserDashboardActivity.class);
                    }
                    startActivity(intent);
                    finish();
                });
            } catch (Exception e) {
                android.util.Log.e("MainActivity", "Error checking profile completion: " + e.getMessage());
                e.printStackTrace();
                // Fallback: go to dashboard
                runOnUiThread(() -> {
                    startActivity(new Intent(this, UserDashboardActivity.class));
                    finish();
                });
            }
        });
    }
    
    private void checkProfilePictureAndNavigate() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                int userId = preferencesManager.getUserId();
                User user = database.userDao().getUserById(userId);
                boolean hasProfilePhotoInDb = user != null && user.getProfilePhotoUri() != null && !user.getProfilePhotoUri().isEmpty();
                
                // If user has profile photo in DB, set the flag to true
                if (hasProfilePhotoInDb) {
                    preferencesManager.setHasSelectedProfilePicture(true);
                    android.util.Log.d("MainActivity", "✅ User has profile photo in DB, setting flag to TRUE");
                }
                
                boolean hasSelectedPfp = preferencesManager.hasSelectedProfilePicture();
                android.util.Log.d("MainActivity", "User hasSelectedProfilePicture: " + hasSelectedPfp);
                
                // Navigate on UI thread
                runOnUiThread(() -> {
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
                });
            } catch (Exception e) {
                android.util.Log.e("MainActivity", "Error checking profile picture: " + e.getMessage());
                e.printStackTrace();
                // Fallback: go to dashboard
                runOnUiThread(() -> {
                    startActivity(new Intent(this, UserDashboardActivity.class));
                    finish();
                });
            }
        });
    }
    
    private void createAdminAccountIfNotExists() {
        Executors.newSingleThreadExecutor().execute(() -> {
            // Check if admin account exists (new username)
            User existingAdmin = database.userDao().getUserByUsername("official.bms.admin");
            
            // Also check for old username for migration
            User oldAdmin = database.userDao().getUserByUsername("admin");
            
            if (existingAdmin == null && oldAdmin == null) {
                // Create built-in admin account with hashed password using SecurityUtils
                String hashedPassword = com.example.blottermanagementsystem.utils.SecurityUtils.hashPassword("@BMSOFFICIAL2025");
                User admin = new User("System", "Administrator", "official.bms.admin", hashedPassword, "Admin");
                admin.setActive(true);
                database.userDao().insertUser(admin);
                android.util.Log.d("MainActivity", "✅ Default admin account created: official.bms.admin/@BMSOFFICIAL2025");
            } else if (existingAdmin == null && oldAdmin != null) {
                // Migrate old admin account to new username
                String hashedPassword = com.example.blottermanagementsystem.utils.SecurityUtils.hashPassword("@BMSOFFICIAL2025");
                oldAdmin.setUsername("official.bms.admin");
                oldAdmin.setPassword(hashedPassword);
                database.userDao().updateUser(oldAdmin);
                android.util.Log.d("MainActivity", "✅ Admin account migrated to new username: official.bms.admin/@BMSOFFICIAL2025");
            } else {
                // Admin account exists with new username - update password if needed
                String hashedPassword = com.example.blottermanagementsystem.utils.SecurityUtils.hashPassword("@BMSOFFICIAL2025");
                if (!existingAdmin.getPassword().equals(hashedPassword)) {
                    existingAdmin.setPassword(hashedPassword);
                    database.userDao().updateUser(existingAdmin);
                    android.util.Log.d("MainActivity", "✅ Admin password updated to: @BMSOFFICIAL2025");
                }
            }
        });
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
