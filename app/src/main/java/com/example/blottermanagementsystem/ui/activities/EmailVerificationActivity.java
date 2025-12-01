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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

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
        
        // For testing - set verification code (remove in production)
        verificationCode = "123456";
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
        // Code 1 -> 2
        etCode1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) {
                    etCode2.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Code 2 -> 3
        etCode2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) {
                    etCode3.requestFocus();
                } else if (s.length() == 0) {
                    etCode1.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Code 3 -> 4
        etCode3.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) {
                    etCode4.requestFocus();
                } else if (s.length() == 0) {
                    etCode2.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Code 4 -> 5
        etCode4.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) {
                    etCode5.requestFocus();
                } else if (s.length() == 0) {
                    etCode3.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Code 5 -> 6
        etCode5.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) {
                    etCode6.requestFocus();
                } else if (s.length() == 0) {
                    etCode4.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Code 6 - backspace handling
        etCode6.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) {
                    etCode5.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
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

        // Simulate verification delay (in production, this would be an API call)
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (enteredCode.equals(verificationCode)) {
                // ✅ Code is correct
                showLoading(false);
                Toast.makeText(this, "Email verified successfully!", Toast.LENGTH_SHORT).show();
                
                // Navigate based on verification type
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    if ("registration".equals(verificationType)) {
                        autoLogin();
                    } else if ("reset_password".equals(verificationType)) {
                        navigateToResetPassword();
                    }
                }, 1500);
            } else {
                // ❌ Code is incorrect
                showLoading(false);
                Toast.makeText(this, "Invalid verification code. Please try again.", Toast.LENGTH_SHORT).show();
                clearAllFields();
            }
        }, 1000);
    }

    private void autoLogin() {
        // TODO: In production, this would:
        // 1. Generate JWT token from backend
        // 2. Store token in SharedPreferences
        // 3. Set user as logged in
        // 4. Navigate to profile picture selection (same as normal registration flow)

        Toast.makeText(this, "Auto-login successful!", Toast.LENGTH_SHORT).show();
        
        // Navigate to Profile Picture Selection (same as normal registration flow)
        Intent intent = new Intent(this, ProfilePictureSelectionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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
            Toast.makeText(this, "Verification code resent to " + userEmail, Toast.LENGTH_SHORT).show();
            clearAllFields();
            startTimer();
            
            // TODO: In production, call backend API to resend code
            // API: POST /api/auth/resend-verification-code
            // Body: { email: userEmail }
        } else {
            Toast.makeText(this, "Please wait before requesting a new code", Toast.LENGTH_SHORT).show();
        }
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
