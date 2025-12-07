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
import com.example.blottermanagementsystem.data.entity.User;
import com.example.blottermanagementsystem.utils.PreferencesManager;
import com.example.blottermanagementsystem.viewmodel.AuthViewModel;
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
    private AuthViewModel authViewModel;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private PreferencesManager preferencesManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        
        preferencesManager = new PreferencesManager(this);
        setupGoogleSignIn();
        initViews();
        setupViewModel();
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
                handleGoogleSignInResult(task);
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
    
    private void setupViewModel() {
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        
        // Observe registerState instead of authState for registration
        authViewModel.getRegisterState().observe(this, authState -> {
            if (authState == AuthViewModel.AuthState.LOADING) {
                showLoading(true);
            } else if (authState == AuthViewModel.AuthState.SUCCESS) {
                showLoading(false);
                handleRegisterSuccess();
            } else if (authState == AuthViewModel.AuthState.EMAIL_EXISTS) {
                showLoading(false);
                showError("This email is already registered. Please use a different email or sign in.");
            } else if (authState == AuthViewModel.AuthState.ERROR) {
                showLoading(false);
                showError("Registration failed. Username may already exist.");
            } else {
                showLoading(false);
            }
        });
    }
    
    private void setupListeners() {
        btnRegister.setOnClickListener(v -> attemptRegister());
        
        tvLogin.setOnClickListener(v -> {
            // Go back to Login screen
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }
    
    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            
            // Get user info
            String email = account.getEmail();
            String displayName = account.getDisplayName();
            String photoUrl = account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : null;
            
            // Parse display name into first and last name
            final String firstName;
            final String lastName;
            if (displayName != null && !displayName.isEmpty()) {
                String[] nameParts = displayName.split(" ", 2);
                firstName = nameParts[0];
                if (nameParts.length > 1) {
                    lastName = nameParts[1];
                } else {
                    lastName = "Account";
                }
            } else {
                firstName = "User";
                lastName = "Account";
            }
            
            // Save Google account info
            preferencesManager.saveGoogleAccountInfo(email, displayName, photoUrl);
            
            // Create User in database
            User newUser = new User(firstName, lastName, email, "", "User");
            newUser.setProfilePhotoUri(photoUrl); // Save Google photo
            
            // Register user in database using ViewModel
            authViewModel.register(newUser);
            
            // Wait for registration to complete, then navigate
            authViewModel.getRegisterState().observe(this, state -> {
                if (state == AuthViewModel.AuthState.SUCCESS) {
                    // Get the created user ID
                    java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
                        com.example.blottermanagementsystem.data.database.BlotterDatabase database = 
                            com.example.blottermanagementsystem.data.database.BlotterDatabase.getDatabase(this);
                        User createdUser = database.userDao().getUserByUsername(email);
                        
                        android.util.Log.d("RegisterActivity", "=== GOOGLE SIGN-UP ===");
                        android.util.Log.d("RegisterActivity", "Email: " + email);
                        android.util.Log.d("RegisterActivity", "User found: " + (createdUser != null));
                        
                        if (createdUser != null) {
                            int userId = createdUser.getId();
                            android.util.Log.d("RegisterActivity", "✅ User ID: " + userId);
                            
                            runOnUiThread(() -> {
                                // Save user ID and data to preferences
                                preferencesManager.setUserId(userId);
                                preferencesManager.setLoggedIn(true);
                                preferencesManager.setUserRole("User");
                                preferencesManager.setFirstName(firstName);
                                preferencesManager.setLastName(lastName);
                                
                                android.util.Log.d("RegisterActivity", "✅ Saved userId to preferences: " + userId);
                                
                                Toast.makeText(this, "Signed up with Google: " + displayName, Toast.LENGTH_SHORT).show();
                                
                                // Navigate to Profile Picture Selection
                                Intent intent = new Intent(RegisterActivity.this, ProfilePictureSelectionActivity.class);
                                startActivity(intent);
                                finish();
                            });
                        } else {
                            android.util.Log.e("RegisterActivity", "❌ User NOT found in database after registration!");
                        }
                    });
                } else if (state == AuthViewModel.AuthState.EMAIL_EXISTS) {
                    Toast.makeText(this, "This Google account is already registered. Please sign in instead.", Toast.LENGTH_LONG).show();
                    // Navigate back to login
                    Intent intent = new Intent(this, com.example.blottermanagementsystem.ui.activities.LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else if (state == AuthViewModel.AuthState.ERROR) {
                    Toast.makeText(this, "Registration failed. Account may already exist.", Toast.LENGTH_SHORT).show();
                }
            });
            
        } catch (ApiException e) {
            Toast.makeText(this, "Google Sign-Up failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void attemptRegister() {
        String username = etUsernameField.getText().toString().trim();
        String email = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        
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
        
        // ✅ FIXED: DO NOT save to database yet!
        // Store user data temporarily in SharedPreferences
        // User will ONLY be saved AFTER successful email verification
        
        preferencesManager.setTempUsername(username);
        preferencesManager.setTempEmail(email);
        preferencesManager.setTempPassword(hashPassword(password));
        
        android.util.Log.d("RegisterActivity", "✅ Registration successful!");
        android.util.Log.d("RegisterActivity", "  Username: " + username);
        android.util.Log.d("RegisterActivity", "  Email: " + email);
        
        // ✅ Skip email verification for now - go straight to login
        android.widget.Toast.makeText(RegisterActivity.this, 
            "Registration successful! Please log in.", 
            android.widget.Toast.LENGTH_SHORT).show();
        
        // Navigate to login screen
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    
    }
    
    private void handleRegisterSuccess() {
        // This method is no longer used for email/password registration
        // Email verification now handles the flow
    }
    
    private void showCredentialsDialog(String userName, String username, String password) {
        try {
            // Inflate custom dialog layout
            android.view.LayoutInflater inflater = getLayoutInflater();
            android.view.View dialogView = inflater.inflate(R.layout.dialog_user_credentials, null);
            
            // Get views from dialog
            android.widget.TextView tvUserName = dialogView.findViewById(R.id.tvUserName);
            android.widget.TextView tvUsername = dialogView.findViewById(R.id.tvUsername);
            android.widget.TextView tvPassword = dialogView.findViewById(R.id.tvPassword);
            MaterialButton btnCopyCredentials = dialogView.findViewById(R.id.btnCopyCredentials);
            MaterialButton btnDone = dialogView.findViewById(R.id.btnDone);
            
            // Set data
            tvUserName.setText(userName);
            tvUsername.setText(username);
            tvPassword.setText(password);
            
            // Create dialog
            AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();
            
            // Set button listeners
            btnCopyCredentials.setOnClickListener(v -> {
                // Copy both username and password
                String credentials = "Username: " + username + "\nPassword: " + password;
                copyToClipboard("Credentials", credentials);
                Toast.makeText(this, "Credentials copied to clipboard", Toast.LENGTH_SHORT).show();
            });
            
            btnDone.setOnClickListener(v -> {
                dialog.dismiss();
                // Navigate to Login screen
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
            
            // Show dialog
            dialog.show();
            
            android.util.Log.d("Register", "Dialog shown successfully");
        } catch (Exception e) {
            android.util.Log.e("Register", "Error showing dialog", e);
            // Navigate to Login screen without showing toast
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
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
