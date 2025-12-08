package com.example.blottermanagementsystem.ui.activities;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.entity.User;
import com.example.blottermanagementsystem.utils.PreferencesManager;
import com.example.blottermanagementsystem.data.api.ApiService;
import com.example.blottermanagementsystem.viewmodel.AuthViewModel;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends BaseActivity {
    
    private TextInputEditText etUsername, etPassword;
    private MaterialButton btnLogin, btnGoogleSignIn;
    private TextView tvError, tvRegister, tvForgotPassword;
    private ProgressBar progressBar;
    private AuthViewModel authViewModel;
    private PreferencesManager preferencesManager;
    
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        initViews();
        setupGoogleSignIn();
        setupViewModel();
        setupListeners();
        animateViews();
    }
    
    private void initViews() {
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        tvError = findViewById(R.id.tvError);
        tvRegister = findViewById(R.id.tvRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        progressBar = findViewById(R.id.progressBar);
        preferencesManager = new PreferencesManager(this);
    }
    
    private void setupGoogleSignIn() {
        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .build();
        
        googleSignInClient = GoogleSignIn.getClient(this, gso);
        
        // Setup Activity Result Launcher
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        handleGoogleSignInResult(task);
                    }
                });
    }
    
    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            
            // Get user info
            String email = account.getEmail();
            String displayName = account.getDisplayName();
            String photoUrl = account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : null;
            
            // Extract clean username from email (before @)
            String username = email.split("@")[0];
            android.util.Log.d("LoginActivity", "Google Sign-In - Email: " + email + ", Username: " + username);
            
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
            
            // ✅ SEPARATE WORKFLOW: Check if email already registered
            java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
                com.example.blottermanagementsystem.data.database.BlotterDatabase database = 
                    com.example.blottermanagementsystem.data.database.BlotterDatabase.getDatabase(this);
                
                // ✅ Check if email exists in database
                com.example.blottermanagementsystem.data.entity.User existingUserByEmail = 
                    database.userDao().getUserByEmail(email);
                
                // ✅ Use final variable for lambda
                final com.example.blottermanagementsystem.data.entity.User finalUserByEmail = existingUserByEmail;
                
                runOnUiThread(() -> {
                    if (finalUserByEmail != null) {
                        // ✅ Email exists - Check how it was registered
                        String authMethod = finalUserByEmail.getAuthMethod();
                        
                        if ("GOOGLE".equals(authMethod)) {
                            // ✅ Google account exists - LOGIN
                            android.util.Log.d("LoginActivity", "Google user exists, logging in: " + email);
                            
                            // SECURITY: Google Sign-In users are ALWAYS "User" role
                            preferencesManager.setUserId(finalUserByEmail.getId());
                            preferencesManager.setLoggedIn(true);
                            preferencesManager.setUserRole("User");
                            preferencesManager.setFirstName(finalUserByEmail.getFirstName());
                            preferencesManager.setLastName(finalUserByEmail.getLastName());
                            
                            // Navigate to Dashboard
                            Intent intent = new Intent(LoginActivity.this, com.example.blottermanagementsystem.ui.activities.UserDashboardActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                            
                        } else {
                            // ✅ Email registered via Sign Up - REJECT Google Sign-In
                            android.util.Log.w("LoginActivity", "Email already registered via Sign Up: " + email);
                            Toast.makeText(LoginActivity.this, "This email is already registered. Please use Sign in with username and password.", Toast.LENGTH_LONG).show();
                        }
                        return;
                    }
                    
                    // ✅ Email not registered - Create new Google account
                    android.util.Log.d("LoginActivity", "Email not registered, creating new Google account: " + email);
                    
                    // Create new user in database with clean username
                    com.example.blottermanagementsystem.data.entity.User newUser = 
                        new com.example.blottermanagementsystem.data.entity.User(firstName, lastName, username, "", "User");
                    newUser.setEmail(email);
                    newUser.setProfilePhotoUri(photoUrl);
                    newUser.setAuthMethod("GOOGLE"); // ✅ Mark as Google account
                    
                    java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
                        long userId = database.userDao().insertUser(newUser);
                        
                        runOnUiThread(() -> {
                            // Save user data to preferences
                            preferencesManager.setUserId((int) userId);
                            preferencesManager.setLoggedIn(true);
                            preferencesManager.setUserRole("User");
                            preferencesManager.setFirstName(firstName);
                            preferencesManager.setLastName(lastName);
                            
                            // Navigate to Profile Picture Selection
                            Intent intent = new Intent(LoginActivity.this, com.example.blottermanagementsystem.ui.activities.ProfilePictureSelectionActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        });
                    });
                });
            });
            
        } catch (ApiException e) {
            Toast.makeText(this, "Google Sign-In failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void setupViewModel() {
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        
        // Set callback for direct communication (more reliable than LiveData observer)
        authViewModel.setLoginCallback(new AuthViewModel.LoginCallback() {
            @Override
            public void onLoginSuccess(String role) {
                android.util.Log.d("LoginActivity", "📞 CALLBACK RECEIVED! Role: " + role);
                showLoading(false);
                handleLoginSuccess(role);
            }
            
            @Override
            public void onLoginError(String message) {
                android.util.Log.d("LoginActivity", "📞 CALLBACK ERROR: " + message);
                showLoading(false);
                showError(message);
            }
        });
        
        // Keep LiveData observer for other states
        authViewModel.getAuthState().observe(this, authState -> {
            android.util.Log.d("LoginActivity", "=== AUTH STATE CHANGED ===");
            android.util.Log.d("LoginActivity", "State: " + authState);
            
            if (authState == AuthViewModel.AuthState.LOADING) {
                android.util.Log.d("LoginActivity", "State: LOADING");
                showLoading(true);
            } else if (authState == AuthViewModel.AuthState.USER_NOT_FOUND) {
                showLoading(false);
                showError("User not found, register one.");
            } else if (authState == AuthViewModel.AuthState.WRONG_PASSWORD) {
                showLoading(false);
                showError("Invalid username or password");
            } else if (authState == AuthViewModel.AuthState.ERROR) {
                showLoading(false);
                showError("Login failed. Please try again.");
            }
        });
    }
    
    private void setupListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());
        
        btnGoogleSignIn.setOnClickListener(v -> {
            // Sign out from Google first to allow account selection
            googleSignInClient.signOut().addOnCompleteListener(this, task -> {
                // Launch Google Sign-In with account picker
                Intent signInIntent = googleSignInClient.getSignInIntent();
                googleSignInLauncher.launch(signInIntent);
            });
        });
        
        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
        
        tvForgotPassword.setOnClickListener(v -> {
            // Show forgot password dialog
            showForgotPasswordDialog();
        });
        
        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            attemptLogin();
            return true;
        });
    }
    
    private void showForgotPasswordDialog() {
        // Navigate to Forgot Password Activity
        Intent intent = new Intent(this, ForgotPasswordActivity.class);
        startActivity(intent);
    }
    
    private void attemptLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        
        if (username.isEmpty() || password.isEmpty()) {
            showError(getString(R.string.fill_all_fields));
            return;
        }
        
        // STRICT PASSWORD VALIDATION - Only for Officer and User roles
        // Admin role is built-in and exempt from strict validation
        if (!username.equalsIgnoreCase("admin") && !username.equalsIgnoreCase("official.bms.admin") && !isValidPassword(password)) {
            showError("Password must be at least 8 characters with uppercase, lowercase, number, and special character");
            return;
        }
        
        hideError();
        showLoading(true);
        
        android.util.Log.d("LoginActivity", "=== LOGIN DEBUG ===");
        android.util.Log.d("LoginActivity", "Attempting login with:");
        android.util.Log.d("LoginActivity", "Username: " + username);
        
        // ✅ Try API login first if online
        com.example.blottermanagementsystem.utils.NetworkMonitor networkMonitor = 
            new com.example.blottermanagementsystem.utils.NetworkMonitor(this);
        
        if (networkMonitor.isNetworkAvailable()) {
            android.util.Log.d("LoginActivity", "🌐 Online - Attempting API login");
            attemptApiLogin(username, password);
        } else {
            android.util.Log.d("LoginActivity", "📴 Offline - Using local database login");
            attemptLocalLogin(username, password);
        }
    }
    
    /**
     * Attempt login via API (Neon database)
     */
    private void attemptApiLogin(String username, String password) {
        com.example.blottermanagementsystem.utils.ApiClient.login(username, password, 
            new com.example.blottermanagementsystem.utils.ApiClient.ApiCallback<Object>() {
                @Override
                public void onSuccess(Object loginResponseObj) {
                    android.util.Log.d("LoginActivity", "✅ API login successful");
                    
                    // Get user from response using reflection to avoid inner class reference
                    try {
                        Object dataObj = loginResponseObj.getClass().getField("data").get(loginResponseObj);
                        com.example.blottermanagementsystem.data.entity.User apiUser = 
                            (com.example.blottermanagementsystem.data.entity.User) dataObj.getClass().getField("user").get(dataObj);
                    
                    // Store API ID in user
                    apiUser.setApiId(apiUser.getId());
                    
                    // Save to local database on background thread
                    java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
                        com.example.blottermanagementsystem.data.database.BlotterDatabase database = 
                            com.example.blottermanagementsystem.data.database.BlotterDatabase.getDatabase(LoginActivity.this);
                        
                        // Check if user exists locally
                        com.example.blottermanagementsystem.data.entity.User existingUser = 
                            database.userDao().getUserByEmail(apiUser.getEmail());
                        
                        if (existingUser != null) {
                            // Update existing user with API ID
                            existingUser.setApiId(apiUser.getId());
                            database.userDao().updateUser(existingUser);
                            android.util.Log.d("LoginActivity", "✅ Updated local user with API ID: " + apiUser.getId());
                        } else {
                            // Insert new user
                            database.userDao().insertUser(apiUser);
                            android.util.Log.d("LoginActivity", "✅ Inserted new user with API ID: " + apiUser.getId());
                        }
                        
                        // Now proceed with local login
                        runOnUiThread(() -> attemptLocalLogin(apiUser.getUsername(), password));
                    });
                    } catch (Exception e) {
                        android.util.Log.e("LoginActivity", "❌ Error processing login response: " + e.getMessage(), e);
                        onError("Error processing login response");
                    }
                }
                
                @Override
                public void onError(String errorMessage) {
                    android.util.Log.w("LoginActivity", "⚠️ API login failed: " + errorMessage);
                    // Fall back to local login
                    attemptLocalLogin(username, password);
                }
            });
    }
    
    /**
     * Attempt login using local database
     */
    private void attemptLocalLogin(String username, String password) {
        android.util.Log.d("LoginActivity", "🔐 Attempting local database login");
        authViewModel.login(username, password);
    }
    
    /**
     * Validate password strength
     * Requirements:
     * - At least 8 characters
     * - At least one uppercase letter
     * - At least one lowercase letter
     * - At least one digit
     * - At least one special character
     */
    private boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        
        boolean hasUppercase = password.matches(".*[A-Z].*");
        boolean hasLowercase = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecialChar = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");
        
        return hasUppercase && hasLowercase && hasDigit && hasSpecialChar;
    }
    
    private void handleLoginSuccess(String role) {
        android.util.Log.d("LoginActivity", "Navigating to dashboard, role: " + role);
        
        Intent intent;
        switch (role.toLowerCase()) {
            case "admin":
                intent = new Intent(this, AdminDashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                break;
            case "officer":
                // Check if officer has changed password
                if (!preferencesManager.hasPasswordChanged()) {
                    // First time login - force password change
                    intent = new Intent(this, OfficerWelcomeActivity.class);
                } else {
                    intent = new Intent(this, OfficerDashboardActivity.class);
                }
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                break;
            default:
                // For regular users, check profile completion from database
                checkUserProfileCompletion();
                break;
        }
    }
    
    private void checkUserProfileCompletion() {
        int userId = preferencesManager.getUserId();
        
        android.util.Log.d("LoginActivity", "=== CHECK PROFILE COMPLETION ===");
        android.util.Log.d("LoginActivity", "UserId from PreferencesManager: " + userId);
        
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            com.example.blottermanagementsystem.data.database.BlotterDatabase database = 
                com.example.blottermanagementsystem.data.database.BlotterDatabase.getDatabase(this);
            com.example.blottermanagementsystem.data.entity.User user = database.userDao().getUserById(userId);
            
            runOnUiThread(() -> {
                Intent intent;
                
                if (user != null && user.isProfileCompleted()) {
                    // Profile completed - go to dashboard
                    android.util.Log.d("LoginActivity", "✅ Profile completed - navigating to dashboard");
                    android.util.Log.d("LoginActivity", "User: " + user.getFirstName() + " " + user.getLastName());
                    intent = new Intent(this, UserDashboardActivity.class);
                } else {
                    // First time or profile not complete - go to profile setup
                    android.util.Log.d("LoginActivity", "⚠️ Profile not completed - navigating to profile setup");
                    android.util.Log.d("LoginActivity", "Passing userId: " + userId + " to ProfilePictureSelectionActivity");
                    intent = new Intent(this, ProfilePictureSelectionActivity.class);
                    // Pass userId explicitly via Intent
                    intent.putExtra("USER_ID", userId);
                }
                
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        });
    }
    
    private void showLoading(boolean show) {
        if (show) {
            com.example.blottermanagementsystem.utils.GlobalLoadingManager.show(this, "Signing in...");
            btnLogin.setEnabled(false);
            etUsername.setEnabled(false);
            etPassword.setEnabled(false);
        } else {
            com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
            btnLogin.setEnabled(true);
            etUsername.setEnabled(true);
            etPassword.setEnabled(true);
        }
    }
    
    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }
    
    private void hideError() {
        tvError.setVisibility(View.GONE);
    }
    
    
    private void animateViews() {
        View loginCard = findViewById(R.id.loginCard);
        
        // Login card animation - fade in and slide up
        if (loginCard != null) {
            loginCard.setAlpha(0f);
            loginCard.setTranslationY(50f);
            loginCard.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(700)
                .setStartDelay(200)
                .start();
        }
    }
    
}
