package com.example.blottermanagementsystem.ui.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.utils.PreferencesManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.concurrent.Executors;

public class EmailVerificationActivity extends AppCompatActivity {

    private TextInputEditText etCode1, etCode2, etCode3, etCode4, etCode5, etCode6;
    private MaterialButton btnVerifyCode;
    private TextView tvResendCode, tvTimer, tvEmailDisplay;
    private ProgressBar progressBar;
    private android.widget.ImageView btnBack;
    
    private String userEmail = "";
    private String verificationCode = ""; // For testing: set to actual code
    private String verificationType = ""; // "registration" or "reset_password"
    private CountDownTimer countDownTimer;
    private static final long TIMER_DURATION = 5 * 60 * 1000; // 5 minutes
    private boolean timerRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_verification);
        
        // Set status bar and navigation bar colors to match background
        setSystemBarColors();
        
        // Get email and type from intent
        userEmail = getIntent().getStringExtra("email");
        verificationType = getIntent().getStringExtra("type"); // "registration" or "reset_password"
        
        if (userEmail == null) {
            userEmail = "user@example.com";
        }
        if (verificationType == null) {
            verificationType = "registration";
        }
        
        initViews();
        setupListeners();
        startTimer();
        
        // Disable verify button by default
        btnVerifyCode.setEnabled(false);
        btnVerifyCode.setAlpha(0.5f);
    }

    private void setSystemBarColors() {
        // Set status bar color to primary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.background_primary));
        }
        
        // Set navigation bar color to primary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.background_primary));
        }
        
        // Set light status bar icons for better visibility
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        etCode1 = findViewById(R.id.etCode1);
        etCode2 = findViewById(R.id.etCode2);
        etCode3 = findViewById(R.id.etCode3);
        etCode4 = findViewById(R.id.etCode4);
        etCode5 = findViewById(R.id.etCode5);
        etCode6 = findViewById(R.id.etCode6);
        
        btnVerifyCode = findViewById(R.id.btnVerifyCode);
        tvResendCode = findViewById(R.id.tvResendCode);
        tvTimer = findViewById(R.id.tvTimer);
        tvEmailDisplay = findViewById(R.id.tvEmailDisplay);
        progressBar = findViewById(R.id.progressBar);
        
        // Set masked email display
        tvEmailDisplay.setText(maskEmail(userEmail));
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        
        String[] parts = email.split("@");
        String localPart = parts[0]; // part before @
        String domain = parts[1];    // part after @
        
        // Mask local part: show first 2 chars and last 1 char, rest asterisks
        String maskedLocal;
        if (localPart.length() <= 3) {
            maskedLocal = localPart.charAt(0) + "**";
        } else {
            int asteriskCount = localPart.length() - 3;
            maskedLocal = localPart.substring(0, 2) + 
                         "*".repeat(asteriskCount) + 
                         localPart.charAt(localPart.length() - 1);
        }
        
        return maskedLocal + "@" + domain;
    }

    private void setupListeners() {
        // Back button
        btnBack.setOnClickListener(v -> goBack());
        
        // Code input auto-focus logic
        setupCodeInputs();
        
        // Verify button
        btnVerifyCode.setOnClickListener(v -> verifyCode());
        
        // Resend code
        tvResendCode.setOnClickListener(v -> resendCode());
    }

    private void goBack() {
        // Navigate back to appropriate screen based on verification type
        if ("registration".equals(verificationType)) {
            // Go back to Register screen
            Intent intent = new Intent(this, RegisterActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else if ("reset_password".equals(verificationType)) {
            // Go back to Forgot Password screen
            Intent intent = new Intent(this, ForgotPasswordActivity.class);
            startActivity(intent);
        } else {
            // Default: go back to Login
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }
        finish();
    }

    private void setupCodeInputs() {
        // ✅ FIXED: Proper OTP input handling with backspace support
        
        // Code 1 -> 2
        setupOTPField(etCode1, null, etCode2);

        // Code 2 -> 3
        setupOTPField(etCode2, etCode1, etCode3);

        // Code 3 -> 4
        setupOTPField(etCode3, etCode2, etCode4);

        // Code 4 -> 5
        setupOTPField(etCode4, etCode3, etCode5);

        // Code 5 -> 6
        setupOTPField(etCode5, etCode4, etCode6);

        // Code 6 - last field
        setupOTPField(etCode6, etCode5, null);
    }

    private void setupOTPField(TextInputEditText currentField, TextInputEditText previousField, TextInputEditText nextField) {
        // ✅ FIXED: Proper backspace handling for ALL cases
        
        currentField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // ✅ FIX: Handle both adding and deleting digits
                if (s.length() == 1) {
                    // Digit added - move to next field
                    if (nextField != null) {
                        nextField.requestFocus();
                    }
                } else if (s.length() == 0 && before == 1) {
                    // Digit deleted - move to previous field
                    if (previousField != null) {
                        previousField.requestFocus();
                    }
                }
                updateVerifyButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {
                // ✅ FIX: Limit to 1 digit only
                if (s.length() > 1) {
                    s.delete(1, s.length());
                }
            }
        });
        
        // ✅ FIXED: Add key listener for backspace handling
        currentField.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == android.view.KeyEvent.KEYCODE_DEL && 
                event.getAction() == android.view.KeyEvent.ACTION_DOWN) {
                
                String currentText = currentField.getText().toString();
                
                // If field is empty, move to previous field and delete its content
                if (currentText.isEmpty() && previousField != null) {
                    previousField.setText("");
                    previousField.requestFocus();
                    return true; // Consume the event
                }
            }
            return false;
        });
    }

    private void updateVerifyButtonState() {
        // Get all code values
        String code1 = etCode1.getText().toString().trim();
        String code2 = etCode2.getText().toString().trim();
        String code3 = etCode3.getText().toString().trim();
        String code4 = etCode4.getText().toString().trim();
        String code5 = etCode5.getText().toString().trim();
        String code6 = etCode6.getText().toString().trim();
        
        // Check if at least first digit is entered
        boolean hasFirstDigit = !code1.isEmpty();
        
        // Enable button only if first digit is entered
        if (hasFirstDigit) {
            btnVerifyCode.setEnabled(true);
            btnVerifyCode.setAlpha(1.0f);
        } else {
            btnVerifyCode.setEnabled(false);
            btnVerifyCode.setAlpha(0.5f);
        }
    }

    private void verifyCode() {
        // Get all code digits
        String code1 = etCode1.getText().toString().trim();
        String code2 = etCode2.getText().toString().trim();
        String code3 = etCode3.getText().toString().trim();
        String code4 = etCode4.getText().toString().trim();
        String code5 = etCode5.getText().toString().trim();
        String code6 = etCode6.getText().toString().trim();

        // Check if all fields are filled
        if (code1.isEmpty() || code2.isEmpty() || code3.isEmpty() || 
            code4.isEmpty() || code5.isEmpty() || code6.isEmpty()) {
            Toast.makeText(this, "Please enter all 6 digits", Toast.LENGTH_SHORT).show();
            return;
        }

        // Combine code
        String enteredCode = code1 + code2 + code3 + code4 + code5 + code6;

        // Show loading
        showLoading(true);

        // Call backend API to verify code
        callVerifyEmailAPI(enteredCode);
    }

    private void callVerifyEmailAPI(String code) {
        // ✅ Call backend API to verify email with 6-digit code
        android.util.Log.d("EmailVerification", "🔐 Verifying email with code: " + code);
        
        // Check network availability
        com.example.blottermanagementsystem.utils.NetworkMonitor networkMonitor = 
            new com.example.blottermanagementsystem.utils.NetworkMonitor(this);
        
        if (!networkMonitor.isNetworkAvailable()) {
            android.util.Log.w("EmailVerification", "⚠️ No network available");
            showLoading(false);
            Toast.makeText(this, "No internet connection. Please check your network.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Call backend API
        com.example.blottermanagementsystem.utils.ApiClient.verifyEmail(userEmail, code,
            new com.example.blottermanagementsystem.utils.ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    android.util.Log.d("EmailVerification", "✅ Email verified successfully!");
                    showLoading(false);
                    Toast.makeText(EmailVerificationActivity.this, "Email verified successfully!", Toast.LENGTH_SHORT).show();
                    
                    // Navigate based on verification type
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        if ("registration".equals(verificationType)) {
                            autoLogin();
                        } else if ("reset_password".equals(verificationType)) {
                            navigateToResetPassword();
                        }
                    }, 1500);
                }
                
                @Override
                public void onError(String errorMessage) {
                    android.util.Log.e("EmailVerification", "❌ Verification failed: " + errorMessage);
                    showLoading(false);
                    Toast.makeText(EmailVerificationActivity.this, "Invalid verification code. Please try again.", Toast.LENGTH_SHORT).show();
                    clearAllFields();
                }
            });
    }

    private void autoLogin() {
        // ✅ FIXED: Save user to database ONLY after successful email verification
        
        String userEmail = getIntent().getStringExtra("email");
        String verificationType = getIntent().getStringExtra("type");
        
        if ("registration".equals(verificationType)) {
            // ✅ Registration flow: Save user to database NOW (after email verified)
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    PreferencesManager preferencesManager = new PreferencesManager(this);
                    
                    // Get temp user data from SharedPreferences
                    String tempUsername = preferencesManager.getTempUsername();
                    String tempEmail = preferencesManager.getTempEmail();
                    String tempPassword = preferencesManager.getTempPassword();
                    
                    android.util.Log.d("EmailVerification", "✅ Email verified! Now saving user to database...");
                    android.util.Log.d("EmailVerification", "  Username: " + tempUsername);
                    android.util.Log.d("EmailVerification", "  Email: " + tempEmail);
                    
                    if (tempUsername != null && tempEmail != null && tempPassword != null) {
                        // Create user object
                        com.example.blottermanagementsystem.data.entity.User newUser = 
                            new com.example.blottermanagementsystem.data.entity.User("User", "Account", tempUsername, tempPassword, "User");
                        newUser.setEmail(tempEmail);
                        newUser.setAuthMethod("EMAIL_PASSWORD");
                        
                        // Save to database
                        com.example.blottermanagementsystem.data.database.BlotterDatabase database = 
                            com.example.blottermanagementsystem.data.database.BlotterDatabase.getDatabase(this);
                        
                        long userId = database.userDao().insertUser(newUser);
                        android.util.Log.d("EmailVerification", "✅ User saved to database with ID: " + userId);
                        
                        // Clear temp data
                        preferencesManager.clearTempUserData();
                        
                        // Save to PreferencesManager
                        preferencesManager.setUserId((int) userId);
                        preferencesManager.setLoggedIn(true);
                        preferencesManager.setUserRole("User");
                        
                        runOnUiThread(() -> {
                            // Navigate to Profile Picture Selection
                            Intent intent = new Intent(this, ProfilePictureSelectionActivity.class);
                            intent.putExtra("USER_ID", (int) userId);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        });
                    } else {
                        android.util.Log.e("EmailVerification", "❌ Temp user data not found!");
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Error: Registration data lost", Toast.LENGTH_SHORT).show();
                        });
                    }
                } catch (Exception e) {
                    android.util.Log.e("EmailVerification", "❌ Error saving user: " + e.getMessage(), e);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Error saving account: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } else {
            // Password reset flow - just navigate back
            navigateToResetPassword();
        }
    }

    private void navigateToResetPassword() {
        // Navigate back to forgot password screen to complete reset
        Intent intent = new Intent(this, ForgotPasswordActivity.class);
        intent.putExtra("email", userEmail);
        intent.putExtra("verified", true);
        startActivity(intent);
        finish();
    }

    private void resendCode() {
        if (!timerRunning) {
            // Call backend API to resend code
            callResendCodeAPI();
        } else {
            Toast.makeText(this, "Please wait before requesting a new code", Toast.LENGTH_SHORT).show();
        }
    }

    private void callResendCodeAPI() {
        // LOCAL OFFLINE RESEND (No backend needed)
        // Code remains "123456" for testing
        
        Toast.makeText(this, "Verification code resent to " + userEmail, Toast.LENGTH_SHORT).show();
        clearAllFields();
        startTimer();
    }

    private void startTimer() {
        timerRunning = true;
        tvResendCode.setEnabled(false);
        tvResendCode.setAlpha(0.5f);

        countDownTimer = new CountDownTimer(TIMER_DURATION, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = millisUntilFinished / 60000;
                long seconds = (millisUntilFinished % 60000) / 1000;
                tvTimer.setText(String.format("Time remaining: %d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                timerRunning = false;
                tvTimer.setText("Code expired. Please request a new one.");
                tvResendCode.setEnabled(true);
                tvResendCode.setAlpha(1.0f);
                btnVerifyCode.setEnabled(false);
                btnVerifyCode.setAlpha(0.5f);
            }
        }.start();
    }

    private void clearAllFields() {
        etCode1.setText("");
        etCode2.setText("");
        etCode3.setText("");
        etCode4.setText("");
        etCode5.setText("");
        etCode6.setText("");
        etCode1.requestFocus();
    }

    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            btnVerifyCode.setEnabled(false);
            btnVerifyCode.setAlpha(0.5f);
        } else {
            progressBar.setVisibility(View.GONE);
            btnVerifyCode.setEnabled(true);
            btnVerifyCode.setAlpha(1.0f);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
