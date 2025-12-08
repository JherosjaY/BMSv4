package com.example.blottermanagementsystem.ui.activities;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.airbnb.lottie.LottieAnimationView;
import com.example.blottermanagementsystem.MainActivity;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.database.BlotterDatabase;
import com.example.blottermanagementsystem.data.entity.User;
import com.example.blottermanagementsystem.utils.PreferencesManager;
import com.example.blottermanagementsystem.data.api.ApiClient;
import com.example.blottermanagementsystem.utils.CloudinaryHelper;
import com.facebook.shimmer.ShimmerFrameLayout;
import java.util.concurrent.Executors;

public class SplashActivity extends AppCompatActivity {
    
    private static final int SPLASH_DURATION = 2500; // 2.5 seconds
    private PreferencesManager preferencesManager;
    private BlotterDatabase database;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        
        preferencesManager = new PreferencesManager(this);
        database = BlotterDatabase.getDatabase(this);
        
        // Initialize API Client (Elysia Backend)
        ApiClient.initApiClient();
        
        // Initialize Cloudinary
        CloudinaryHelper.initCloudinary(this);
        
        // Create default admin account if not exists
        createAdminAccountIfNotExists();
        
        // Initialize views
        View logoCard = findViewById(R.id.logoCard);
        TextView tvAppName = findViewById(R.id.tvAppName);
        TextView tvAppSubtitle = findViewById(R.id.tvAppSubtitle);
        
        // Animate views
        if (logoCard != null) {
            logoCard.setAlpha(0f);
            logoCard.setScaleX(0.5f);
            logoCard.setScaleY(0.5f);
            logoCard.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(800)
                .setStartDelay(200)
                .start();
        }
        
        animateText(tvAppName, 500);
        animateText(tvAppSubtitle, 700);
        
        // Navigate after delay based on onboarding status
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            navigateToNextScreen();
        }, SPLASH_DURATION);
    }
    
    private void navigateToNextScreen() {
        // SYNCED WITH KOTLIN VERSION AND MAINACTIVITY
        // All routing logic is now in MainActivity - SplashActivity just navigates there
        // This ensures consistent flow: Onboarding → Permissions → Welcome → Dashboard
        
        android.util.Log.d("SplashActivity", "🎬 Splash complete - navigating to MainActivity for routing");
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
    
    private void animateLogo(View view) {
        // Simple zoom in animation
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0f, 1f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
        
        scaleX.setDuration(800);
        scaleY.setDuration(800);
        alpha.setDuration(800);
        
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());
        alpha.setInterpolator(new AccelerateDecelerateInterpolator());
        
        scaleX.start();
        scaleY.start();
        alpha.start();
    }
    
    private void animateText(View view, int delay) {
        view.setAlpha(0f);
        view.setTranslationY(50f);
        
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(800)
            .setStartDelay(delay)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .start();
    }
    
    private void createAdminAccountIfNotExists() {
        Executors.newSingleThreadExecutor().execute(() -> {
            // Check if admin account exists
            User existingAdmin = database.userDao().getUserByUsername("admin");
            
            if (existingAdmin == null) {
                // Create built-in admin account with hashed password
                String hashedPassword = hashPassword("BMS2025");
                User admin = new User("System", "Administrator", "admin", hashedPassword, "Admin");
                admin.setActive(true);
                database.userDao().insertUser(admin);
                android.util.Log.d("SplashActivity", "✅ Default admin account created: admin/BMS2025");
            } else {
                // Update existing admin password to hashed version if it's still plain text
                if (existingAdmin.getPassword().equals("admin123")) {
                    String hashedPassword = hashPassword("BMS2025");
                    existingAdmin.setPassword(hashedPassword);
                    database.userDao().updateUser(existingAdmin);
                    android.util.Log.d("SplashActivity", "✅ Admin password updated to hashed version");
                } else {
                    android.util.Log.d("SplashActivity", "✅ Admin account already exists");
                }
            }
        });
    }
    
    /**
     * Hash password using SHA-256
     */
    private String hashPassword(String password) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            android.util.Log.e("SplashActivity", "Error hashing password", e);
            return password; // Fallback to plain text (not recommended)
        }
    }
}
