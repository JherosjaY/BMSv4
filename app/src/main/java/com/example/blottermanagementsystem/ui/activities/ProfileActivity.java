package com.example.blottermanagementsystem.ui.activities;

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
    private ImageView ivProfilePhoto;
    private Button btnEditProfile, btnChangePassword, btnLogout;
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
    }

    /**
     * ✅ PURE ONLINE: Load user profile from API
     */
    private void loadUserProfile() {
        if (!networkMonitor.isOnline()) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = preferencesManager.getUserId();

        ApiClient.getUserProfile(userId, new ApiClient.ApiCallback<Object>() {
            @Override
            public void onSuccess(Object user) {
                if (isFinishing() || isDestroyed()) return;

                if (user instanceof User) {
                    currentUser = (User) user;
                    runOnUiThread(() -> displayUserProfile((User) user));
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (isFinishing() || isDestroyed()) return;

                runOnUiThread(() -> {
                    Toast.makeText(ProfileActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void displayUserProfile(User user) {
        if (tvUserName != null) {
            tvUserName.setText(user.getFirstName() + " " + user.getLastName());
        }
        if (tvUserEmail != null) {
            tvUserEmail.setText(user.getEmail());
        }
        if (tvUserRole != null) {
            tvUserRole.setText(user.getRole());
        }
    }

    private void showEditProfileDialog() {
        // Show edit profile dialog
    }

    private void showChangePasswordDialog() {
        // Show change password dialog
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
                            displayUserProfile((User) updatedUser);
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
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserProfile();
    }
}
