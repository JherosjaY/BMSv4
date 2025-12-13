package com.example.blottermanagementsystem.ui.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import androidx.appcompat.app.AlertDialog;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.utils.PreferencesManager;
import com.example.blottermanagementsystem.utils.NeonAuthManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class RegisterActivity extends BaseActivity {
    
    private TextInputEditText etUsernameField, etUsername, etPassword, etConfirmPassword;
    private MaterialButton btnRegister;
    private TextView tvError, tvLogin;
    private ProgressBar progressBar;
    private NeonAuthManager neonAuthManager;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private PreferencesManager preferencesManager;
    private boolean isRegistering = false; // Prevent duplicate submissions
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        
        // Initialize ApiClient for all API calls
        com.example.blottermanagementsystem.data.api.ApiClient.initApiClient(getApplicationContext());
        preferencesManager = new PreferencesManager(this);
        neonAuthManager = new NeonAuthManager(this);
        setupGoogleSignIn();
        initViews();
        setupListeners();
        animateViews();
    }
    
    private void setupGoogleSignIn() {
        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build();
        
        googleSignInClient = GoogleSignIn.getClient(this, gso);
        
        // Register for activity result
        googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                // NeonAuth handles Google Sign-In now; no custom handler needed.
            }
        );
    }
    
    private void initViews() {
        etUsernameField = findViewById(R.id.etUsernameField);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvError = findViewById(R.id.tvError);
        tvLogin = findViewById(R.id.tvLogin);
        progressBar = findViewById(R.id.progressBar);
    }
    
    private void setupListeners() {
        btnRegister.setOnClickListener(v -> attemptRegister());
        
        tvLogin.setOnClickListener(v -> {
            // Go back to Login screen
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }
    
    private void attemptRegister() {
        // Prevent duplicate submissions
        if (isRegistering) {
            Toast.makeText(this, "Registration in progress. Please wait...", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String username = etUsernameField.getText().toString().trim();
        String email = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        
        // Validation
        if (username.isEmpty() || email.isEmpty() || 
            password.isEmpty() || confirmPassword.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            return;
        }
        
        // Strict password validation
        String passwordError = validateStrongPassword(password);
        if (passwordError != null) {
            showError(passwordError);
            return;
        }
        
        hideError();
        isRegistering = true;
        showLoading(true);
        
        android.util.Log.d("RegisterActivity", "=== PURE ONLINE REGISTRATION ===");
        android.util.Log.d("RegisterActivity", "Username: " + username);
        android.util.Log.d("RegisterActivity", "Email: " + email);
        
        // ✅ PURE ONLINE: Check internet first
        com.example.blottermanagementsystem.utils.NetworkMonitor networkMonitor = 
            new com.example.blottermanagementsystem.utils.NetworkMonitor(this);
        
        if (!networkMonitor.isNetworkAvailable()) {
            android.util.Log.e("RegisterActivity", "❌ No internet connection");
            showError("No internet connection. Please check your connection and try again.");
            showLoading(false);
            return;
        }
        
        // Online - proceed with API registration
        android.util.Log.d("RegisterActivity", "🌐 Internet available - Attempting API registration");
        attemptApiRegister(username, email, password);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        isRegistering = false; // Reset flag when activity is destroyed
    }
    
    /**
     * Pure Online Registration via Neon Auth
     */
    private void attemptApiRegister(String username, String email, String password) {
        android.util.Log.d("RegisterActivity", "🔐 Attempting Neon Auth registration");
        
        // Store registration data locally for later use
        preferencesManager.setTempUsername(username);
        preferencesManager.setTempEmail(email);
        preferencesManager.setTempPassword(password);

        // Send verification code to email (do NOT register user yet)
        com.example.blottermanagementsystem.data.api.ApiClient.sendVerificationCode(email, new com.example.blottermanagementsystem.data.api.ApiClient.ApiCallback<Object>() {
            public void onSuccess(Object result) {
                android.util.Log.d("RegisterActivity", "✅ Verification code sent to email");
                // Navigate to Email Verification screen
                Intent intent = new Intent(RegisterActivity.this, EmailVerificationActivity.class);
                intent.putExtra("email", email);
                intent.putExtra("type", "registration");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
            public void onError(String errorMessage) {
                showLoading(false);
                showError("Failed to send verification code: " + errorMessage);
            }
        });
    }
    
    private void copyToClipboard(String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);
    }
    private void showLoading(boolean show) {
        if (show) {
            com.example.blottermanagementsystem.utils.GlobalLoadingManager.show(this, "Creating account...");
        } else {
            com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
        }
        btnRegister.setEnabled(!show);
        etUsernameField.setEnabled(!show);
        etUsername.setEnabled(!show);
        etPassword.setEnabled(!show);
        etConfirmPassword.setEnabled(!show);
    }
    
    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }
    
    private void hideError() {
        tvError.setVisibility(View.GONE);
    }
    
    private void animateViews() {
        View registerCard = findViewById(R.id.registerCard);
        
        // Register card animation - fade in and slide up
        if (registerCard != null) {
            registerCard.setAlpha(0f);
            registerCard.setTranslationY(50f);
            registerCard.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(700)
                .setStartDelay(200)
                .start();
        }
    }
    
    /**
     * Validate strong password requirements
     * Returns null if valid, error message if invalid
     */
    private String validateStrongPassword(String password) {
        if (password.length() < 8) {
            return "Password must be at least 8 characters";
        }
        
        boolean hasUpperCase = false;
        boolean hasLowerCase = false;
        boolean hasDigit = false;
        boolean hasSpecialChar = false;
        
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasUpperCase = true;
            } else if (Character.isLowerCase(c)) {
                hasLowerCase = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            } else if (!Character.isLetterOrDigit(c)) {
                hasSpecialChar = true;
            }
        }
        
        if (!hasUpperCase) {
            return "Password must contain at least 1 uppercase letter";
        }
        if (!hasLowerCase) {
            return "Password must contain at least 1 lowercase letter";
        }
        if (!hasDigit) {
            return "Password must contain at least 1 number";
        }
        if (!hasSpecialChar) {
            return "Password must contain at least 1 special character (@, #, $, etc.)";
        }
        
        return null; // Password is valid
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
            android.util.Log.e("RegisterActivity", "Error hashing password", e);
            return password; // Fallback to plain text (not recommended)
        }
    }
}
