package com.example.blottermanagementsystem.ui.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.utils.PreferencesManager;
import com.example.blottermanagementsystem.utils.NetworkMonitor;
import com.example.blottermanagementsystem.utils.ApiClient;
import com.example.blottermanagementsystem.utils.GlobalLoadingManager;

/**
 * ✅ PURE ONLINE OFFICER WELCOME ACTIVITY
 * ✅ All operations via API (Neon database)
 * ✅ No local database dependencies
 */
public class OfficerWelcomeActivity extends BaseActivity {

    private PreferencesManager preferencesManager;
    private NetworkMonitor networkMonitor;
    private TextView tvWelcomeMessage;
    private Button btnChangePassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_officer_welcome);

        preferencesManager = new PreferencesManager(this);
        networkMonitor = new NetworkMonitor(this);

        setupToolbar();
        initViews();
        loadWelcomeData();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Welcome");
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void initViews() {
        tvWelcomeMessage = findViewById(R.id.tvWelcomeMessage);
        btnChangePassword = findViewById(R.id.btnChangePassword);

        if (btnChangePassword != null) {
            btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        }
    }

    private void loadWelcomeData() {
        String firstName = preferencesManager.getFirstName();
        String lastName = preferencesManager.getLastName();

        if (tvWelcomeMessage != null) {
            tvWelcomeMessage.setText("Welcome, " + firstName + " " + lastName);
        }
    }

    private void showChangePasswordDialog() {
        // Show change password dialog
        // This would typically show a dialog with current password, new password, confirm password fields
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
                        Toast.makeText(OfficerWelcomeActivity.this, "Password changed successfully", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    if (isFinishing() || isDestroyed()) return;

                    runOnUiThread(() -> {
                        GlobalLoadingManager.hide();
                        Toast.makeText(OfficerWelcomeActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                    });
                }
            });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadWelcomeData();
    }
}
