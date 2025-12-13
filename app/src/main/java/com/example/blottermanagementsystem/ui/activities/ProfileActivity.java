package com.example.blottermanagementsystem.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.entity.User;
import com.example.blottermanagementsystem.utils.PreferencesManager;
import com.example.blottermanagementsystem.utils.NetworkMonitor;
import com.example.blottermanagementsystem.utils.ApiClient;
import com.example.blottermanagementsystem.utils.GlobalLoadingManager;

/**
 * ✅ PURE ONLINE PROFILE ACTIVITY
 * ✅ All profile operations via API (Neon database)
 * ✅ No local database dependencies
 */
public class ProfileActivity extends BaseActivity {

    private PreferencesManager preferencesManager;
    private NetworkMonitor networkMonitor;
    private TextView tvUserName, tvUserEmail, tvUserRole;
    private TextView tvFirstName, tvLastName, tvUsernameInfo, tvEmailInfo;
    private ImageView ivProfilePhoto;
    private android.view.View btnEditProfile, btnChangePassword, btnLogout;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        preferencesManager = new PreferencesManager(this);
        networkMonitor = new NetworkMonitor(this);

        setupToolbar();
        initViews();
        setupListeners();
        loadUserProfile();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Profile");
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void initViews() {
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvUserRole = findViewById(R.id.tvUserRole);
        tvFirstName = findViewById(R.id.tvFirstName);
        tvLastName = findViewById(R.id.tvLastName);
        tvUsernameInfo = findViewById(R.id.tvUsernameInfo);
        tvEmailInfo = findViewById(R.id.tvEmailInfo);
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void setupListeners() {
        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
        }
        if (btnChangePassword != null) {
            btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        }
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> logout());
        }
        
        // Delete Account button
        com.google.android.material.button.MaterialButton btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
        if (btnDeleteAccount != null) {
            btnDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());
        }
    }

    /**
     * ✅ PURE ONLINE: Load user profile from PreferencesManager
     */
    private void loadUserProfile() {
        // Get user data from PreferencesManager (no API call needed)
        String firstName = preferencesManager.getFirstName();
        String lastName = preferencesManager.getLastName();
        String username = preferencesManager.getUsername();
        String email = preferencesManager.getString("user_email", null); // Get email from preferences
        String profileImageUri = preferencesManager.getProfileImageUri();

        android.util.Log.d("Profile", "Loading user profile from PreferencesManager:");
        android.util.Log.d("Profile", "firstName: " + firstName);
        android.util.Log.d("Profile", "lastName: " + lastName);
        android.util.Log.d("Profile", "username: " + username);
        android.util.Log.d("Profile", "email: " + email);
        android.util.Log.d("Profile", "profileImageUri: " + profileImageUri);

        // Display user info in header
        if (tvUserName != null) {
            String fullName = (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
            tvUserName.setText(fullName.trim().isEmpty() ? username : fullName);
        }
        if (tvUserEmail != null) {
            tvUserEmail.setText(email != null ? email : "No email");
        }
        if (tvUserRole != null) {
            tvUserRole.setText("User");
        }

        // Display user info in Account Information section
        if (tvFirstName != null) {
            tvFirstName.setText(firstName != null ? firstName : "Not set");
        }
        if (tvLastName != null) {
            tvLastName.setText(lastName != null ? lastName : "Not set");
        }
        if (tvUsernameInfo != null) {
            tvUsernameInfo.setText(username != null ? username : "Not set");
        }
        if (tvEmailInfo != null) {
            tvEmailInfo.setText(email != null ? email : "Not set");
        }

        // Load profile picture from Cloudinary
        loadProfilePicture(profileImageUri);
    }

    private void loadProfilePicture(String profileImageUri) {
        if (ivProfilePhoto == null) return;

        if (profileImageUri != null && !profileImageUri.isEmpty()) {
            android.util.Log.d("Profile", "Loading profile image: " + profileImageUri);
            try {
                ivProfilePhoto.setImageTintList(null); // Clear tint
                android.net.Uri imageUri = android.net.Uri.parse(profileImageUri);
                com.bumptech.glide.Glide.with(this)
                    .load(imageUri)
                    .apply(com.bumptech.glide.request.RequestOptions.circleCropTransform())
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(ivProfilePhoto);
                android.util.Log.d("Profile", "✅ Profile image loaded successfully");
            } catch (Exception e) {
                android.util.Log.e("Profile", "❌ Error loading profile image: " + e.getMessage());
                ivProfilePhoto.setImageResource(R.drawable.ic_person);
            }
        } else {
            android.util.Log.d("Profile", "No profile image URI found");
            ivProfilePhoto.setImageResource(R.drawable.ic_person);
        }
    }

    private void showEditProfileDialog() {
        // Use XML layout for Edit Profile dialog
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        
        android.view.View dialogView = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null);
        builder.setView(dialogView);
        
        com.google.android.material.textfield.TextInputEditText etFirstName = dialogView.findViewById(R.id.etFirstName);
        com.google.android.material.textfield.TextInputEditText etLastName = dialogView.findViewById(R.id.etLastName);
        com.google.android.material.button.MaterialButton btnCancelEdit = dialogView.findViewById(R.id.btnCancelEdit);
        com.google.android.material.button.MaterialButton btnSaveEdit = dialogView.findViewById(R.id.btnSaveEdit);
        
        // Pre-fill with current values
        etFirstName.setText(preferencesManager.getFirstName());
        etLastName.setText(preferencesManager.getLastName());
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        btnSaveEdit.setOnClickListener(v -> {
            String firstName = etFirstName.getText().toString().trim();
            String lastName = etLastName.getText().toString().trim();

            if (firstName.isEmpty() || lastName.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            updateProfileViaApi(firstName, lastName);
            dialog.dismiss();
        });
        
        btnCancelEdit.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }

    private void showChangePasswordDialog() {
        // Use XML layout for Change Password dialog
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        
        android.view.View dialogView = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null);
        builder.setView(dialogView);
        
        com.google.android.material.textfield.TextInputEditText etCurrentPassword = dialogView.findViewById(R.id.etCurrentPassword);
        com.google.android.material.textfield.TextInputEditText etNewPassword = dialogView.findViewById(R.id.etNewPassword);
        com.google.android.material.textfield.TextInputEditText etConfirmPassword = dialogView.findViewById(R.id.etConfirmPassword);
        com.google.android.material.button.MaterialButton btnCancelPassword = dialogView.findViewById(R.id.btnCancelPassword);
        com.google.android.material.button.MaterialButton btnChangePassword = dialogView.findViewById(R.id.btnChangePassword);
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        btnChangePassword.setOnClickListener(v -> {
            String currentPassword = etCurrentPassword.getText().toString().trim();
            String newPassword = etNewPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPassword.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            changePasswordViaApi(currentPassword, newPassword);
            dialog.dismiss();
        });
        
        btnCancelPassword.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }

    /**
     * ✅ PURE ONLINE: Update profile via API
     */
    private void updateProfileViaApi(String firstName, String lastName) {
        GlobalLoadingManager.show(this, "Updating profile...");

        if (!networkMonitor.isOnline()) {
            GlobalLoadingManager.hide();
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = preferencesManager.getUserId();

        ApiClient.updateProfile(userId, firstName, lastName,
            new ApiClient.ApiCallback<Object>() {
                @Override
                public void onSuccess(Object updatedUser) {
                    if (isFinishing() || isDestroyed()) return;

                    if (updatedUser instanceof User) {
                        currentUser = (User) updatedUser;
                        preferencesManager.setFirstName(firstName);
                        preferencesManager.setLastName(lastName);

                        runOnUiThread(() -> {
                            GlobalLoadingManager.hide();
                            Toast.makeText(ProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                            loadUserProfile(); // Reload profile from PreferencesManager
                        });
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    if (isFinishing() || isDestroyed()) return;

                    runOnUiThread(() -> {
                        GlobalLoadingManager.hide();
                        Toast.makeText(ProfileActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                    });
                }
            });
    }

    /**
     * ✅ PURE ONLINE: Change password via API
     */
    private void changePasswordViaApi(String currentPassword, String newPassword) {
        GlobalLoadingManager.show(this, "Changing password...");

        if (!networkMonitor.isOnline()) {
            GlobalLoadingManager.hide();
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = preferencesManager.getUserId();

        ApiClient.changePassword(userId, currentPassword, newPassword,
            new ApiClient.ApiCallback<Object>() {
                @Override
                public void onSuccess(Object response) {
                    if (isFinishing() || isDestroyed()) return;

                    runOnUiThread(() -> {
                        GlobalLoadingManager.hide();
                        Toast.makeText(ProfileActivity.this, "Password changed successfully", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    if (isFinishing() || isDestroyed()) return;

                    runOnUiThread(() -> {
                        GlobalLoadingManager.hide();
                        Toast.makeText(ProfileActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                    });
                }
            });
    }

    private void logout() {
        preferencesManager.clearUserData();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        // Navigate to login screen
        Intent intent = new Intent(this, com.example.blottermanagementsystem.ui.activities.LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showDeleteAccountDialog() {
        // Use XML layout for Delete Account dialog
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        
        android.view.View dialogView = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_delete_account, null);
        builder.setView(dialogView);
        
        com.google.android.material.textfield.TextInputEditText etDeleteConfirmation = dialogView.findViewById(R.id.etDeleteConfirmation);
        com.google.android.material.button.MaterialButton btnCancelDelete = dialogView.findViewById(R.id.btnCancelDelete);
        com.google.android.material.button.MaterialButton btnConfirmDelete = dialogView.findViewById(R.id.btnConfirmDelete);
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        btnConfirmDelete.setOnClickListener(v -> {
            String confirmText = etDeleteConfirmation.getText().toString().trim();
            
            if (!confirmText.equalsIgnoreCase("DELETE MY ACCOUNT")) {
                Toast.makeText(this, "Please type 'DELETE MY ACCOUNT' to confirm", Toast.LENGTH_SHORT).show();
                return;
            }
            
            deleteAccountViaApi();
            dialog.dismiss();
        });
        
        btnCancelDelete.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }

    private void deleteAccountViaApi() {
        GlobalLoadingManager.show(this, "Deleting account...");

        if (!networkMonitor.isOnline()) {
            GlobalLoadingManager.hide();
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = preferencesManager.getUserId();

        ApiClient.deleteUser(userId, new ApiClient.ApiCallback<Object>() {
            @Override
            public void onSuccess(Object response) {
                if (isFinishing() || isDestroyed()) return;

                runOnUiThread(() -> {
                    GlobalLoadingManager.hide();
                    Toast.makeText(ProfileActivity.this, "Account deleted successfully", Toast.LENGTH_SHORT).show();
                    preferencesManager.clearUserData();
                    Intent intent = new Intent(ProfileActivity.this, com.example.blottermanagementsystem.ui.activities.LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
            }

            @Override
            public void onError(String errorMessage) {
                if (isFinishing() || isDestroyed()) return;

                runOnUiThread(() -> {
                    GlobalLoadingManager.hide();
                    Toast.makeText(ProfileActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserProfile();
    }
}
