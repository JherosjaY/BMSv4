package com.example.blottermanagementsystem.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import com.example.blottermanagementsystem.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class ForgotPasswordActivity extends BaseActivity {
    
    private ImageView btnBack;
    private TextView tvSubtitle, tvResetCode, tvCodeExpiry, tvError;
    private TextInputEditText etEmail, etResetCode, etNewPassword, etConfirmPassword;
    private MaterialButton btnSendCode, btnResetPassword;
    private LinearLayout layoutEmailStep, layoutCodeStep;
    private CardView cardResetCode;
    private ProgressBar progressBar;
    
    private String userEmail;
    private android.os.Handler countdownHandler;
    private Runnable countdownRunnable;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        
        initViews();
        setupListeners();
        animateViews();
    }
    
    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvResetCode = findViewById(R.id.tvResetCode);
        tvCodeExpiry = findViewById(R.id.tvCodeExpiry);
        tvError = findViewById(R.id.tvError);
        
        etEmail = findViewById(R.id.etEmail);
        etResetCode = findViewById(R.id.etResetCode);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        
        btnSendCode = findViewById(R.id.btnSendCode);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        
        layoutEmailStep = findViewById(R.id.layoutEmailStep);
        layoutCodeStep = findViewById(R.id.layoutCodeStep);
        cardResetCode = findViewById(R.id.cardResetCode);
        progressBar = findViewById(R.id.progressBar);
    }
    
    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        btnSendCode.setOnClickListener(v -> sendResetCode());
        
        btnResetPassword.setOnClickListener(v -> resetPassword());
    }
    
    private void sendResetCode() {
        String email = etEmail.getText().toString().trim();
        
        if (email.isEmpty()) {
            showError("Please enter your email address");
            return;
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Please enter a valid email address");
            return;
        }
        
        hideError();
        
        // ✅ PURE ONLINE: Check internet first
        com.example.blottermanagementsystem.utils.NetworkMonitor networkMonitor = 
            new com.example.blottermanagementsystem.utils.NetworkMonitor(this);
        
        if (!networkMonitor.isNetworkAvailable()) {
            android.util.Log.e("ForgotPassword", "❌ No internet connection");
            showError("No internet connection. Please check your connection and try again.");
            return;
        }
        
        // Online - send reset code via API
        android.util.Log.d("ForgotPassword", "🌐 Sending reset code via API for email: " + email);
        com.example.blottermanagementsystem.utils.GlobalLoadingManager.show(this, "📧 Sending reset code...");
        
        com.example.blottermanagementsystem.utils.ApiClient.forgotPassword(email,
            new com.example.blottermanagementsystem.utils.ApiClient.ApiCallback<Object>() {
                @Override
                public void onSuccess(Object response) {
                    android.util.Log.d("ForgotPassword", "✅ Reset code sent via API");
                    
                    runOnUiThread(() -> {
                        com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
                        
                        // Store email for password reset step
                        userEmail = email;
                        
                        // Show success message
                        Toast.makeText(ForgotPasswordActivity.this, "✅ Reset code sent to your email", Toast.LENGTH_SHORT).show();
                        
                        // Navigate to email verification screen for password reset
                        Intent intent = new Intent(ForgotPasswordActivity.this, EmailVerificationActivity.class);
                        intent.putExtra("email", email);
                        intent.putExtra("type", "reset_password");
                        startActivity(intent);
                        finish();
                    });
                }
                
                @Override
                public void onError(String errorMessage) {
                    android.util.Log.e("ForgotPassword", "❌ Failed to send reset code: " + errorMessage);
                    
                    runOnUiThread(() -> {
                        com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
                        
                        if (errorMessage.contains("not found")) {
                            showError("No account found with this email address");
                        } else {
                            showError("Failed to send reset code: " + errorMessage);
                        }
                    });
                }
            });
    }
    
    private void resetPassword() {
        String code = etResetCode.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        
        if (code.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }
        
        if (code.length() != 6) {
            showError("Reset code must be 6 digits");
            return;
        }
        
        // Strict password validation
        String passwordError = validateStrongPassword(newPassword);
        if (passwordError != null) {
            showError(passwordError);
            return;
        }
        
        if (!newPassword.equals(confirmPassword)) {
            showError("Passwords do not match");
            return;
        }
        
        hideError();
        
        // ✅ PURE ONLINE: Check internet first
        com.example.blottermanagementsystem.utils.NetworkMonitor networkMonitor = 
            new com.example.blottermanagementsystem.utils.NetworkMonitor(this);
        
        if (!networkMonitor.isNetworkAvailable()) {
            android.util.Log.e("ForgotPassword", "❌ No internet connection");
            showError("No internet connection. Please check your connection and try again.");
            return;
        }
        
        showLoading(true);
        
        // Online - reset password via API
        android.util.Log.d("ForgotPassword", "🌐 Resetting password via API for email: " + userEmail);
        
        com.example.blottermanagementsystem.utils.ApiClient.resetPassword(userEmail, code, newPassword,
            new com.example.blottermanagementsystem.utils.ApiClient.ApiCallback<Object>() {
                @Override
                public void onSuccess(Object response) {
                    android.util.Log.d("ForgotPassword", "✅ Password reset successfully via API");
                    
                    runOnUiThread(() -> {
                        showLoading(false);
                        
                        Toast.makeText(ForgotPasswordActivity.this, "✅ Password reset successfully!", Toast.LENGTH_SHORT).show();
                        
                        // Navigate back to login
                        Intent intent = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    });
                }
                
                @Override
                public void onError(String errorMessage) {
                    android.util.Log.e("ForgotPassword", "❌ Failed to reset password: " + errorMessage);
                    
                    runOnUiThread(() -> {
                        showLoading(false);
                        
                        if (errorMessage.contains("expired")) {
                            showError("Reset code has expired. Please request a new one.");
                        } else if (errorMessage.contains("Invalid")) {
                            showError("Invalid reset code. Please check and try again.");
                        } else {
                            showError("Failed to reset password: " + errorMessage);
                        }
                    });
                }
            });
    }
    
    
    private void showResetCodeStep() {
        // Hide email step
        layoutEmailStep.setVisibility(View.GONE);
        
        // Show code step
        layoutCodeStep.setVisibility(View.VISIBLE);
        
        // HIDE the reset code display card (code sent via email)
        cardResetCode.setVisibility(View.GONE);
        
        // Update subtitle
        tvSubtitle.setText("Check your email for the reset code");
    }
    
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSendCode.setEnabled(!show);
        btnResetPassword.setEnabled(!show);
        etEmail.setEnabled(!show);
        etResetCode.setEnabled(!show);
        etNewPassword.setEnabled(!show);
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
        View resetCard = findViewById(R.id.resetCard);
        
        if (resetCard != null) {
            resetCard.setAlpha(0f);
            resetCard.setTranslationY(50f);
            resetCard.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .start();
        }
    }
    
    /**
     * Format code with spaces (e.g., "123456" -> "1 2 3 4 5 6")
     */
    private String formatCodeWithSpaces(String code) {
        if (code == null || code.isEmpty()) {
            return "";
        }
        
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < code.length(); i++) {
            formatted.append(code.charAt(i));
            if (i < code.length() - 1) {
                formatted.append(" ");
            }
        }
        return formatted.toString();
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
     * Start countdown timer for code expiry
     * Note: This method is kept for potential future use with countdown display
     * Currently, the backend handles code expiry validation
     */
    private void startCountdownTimer() {
        // Placeholder for future countdown implementation
        // Backend now handles reset code expiry validation
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop countdown timer
        if (countdownHandler != null && countdownRunnable != null) {
            countdownHandler.removeCallbacks(countdownRunnable);
        }
    }
}
