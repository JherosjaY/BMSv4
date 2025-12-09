package com.example.blottermanagementsystem.ui.activities;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.utils.PreferencesManager;
import com.example.blottermanagementsystem.utils.NetworkMonitor;
import com.example.blottermanagementsystem.utils.ApiClient;

public class ChangePasswordActivity extends AppCompatActivity {
    
    private TextInputEditText etCurrentPassword, etNewPassword, etConfirmPassword;
    private MaterialButton btnChangePassword, btnCancel;
    private PreferencesManager preferencesManager;
    private NetworkMonitor networkMonitor;
    private String userId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        
        // Initialize
        preferencesManager = new PreferencesManager(this);
        networkMonitor = new NetworkMonitor(this);
        userId = preferencesManager.getUserId();
        
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Change Password");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        
        // Find views
        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnCancel = findViewById(R.id.btnCancel);
        
        // Setup buttons
        btnCancel.setOnClickListener(v -> finish());
        btnChangePassword.setOnClickListener(v -> changePassword());
    }
    
    private void changePassword() {
        String currentPass = etCurrentPassword.getText().toString().trim();
        String newPass = etNewPassword.getText().toString().trim();
        String confirmPass = etConfirmPassword.getText().toString().trim();
        
        // Validate
        if (currentPass.isEmpty()) {
            etCurrentPassword.setError("Required");
            etCurrentPassword.requestFocus();
            return;
        }
        
        if (newPass.isEmpty()) {
            etNewPassword.setError("Required");
            etNewPassword.requestFocus();
            return;
        }
        
        if (newPass.length() < 6) {
            etNewPassword.setError("Min 6 characters");
            etNewPassword.requestFocus();
            return;
        }
        
        if (confirmPass.isEmpty()) {
            etConfirmPassword.setError("Required");
            etConfirmPassword.requestFocus();
            return;
        }
        
        if (!newPass.equals(confirmPass)) {
            etConfirmPassword.setError("Passwords don't match");
            etConfirmPassword.requestFocus();
            return;
        }
        
        // Check internet
        if (!networkMonitor.isOnline()) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show loading for password change
        com.example.blottermanagementsystem.utils.GlobalLoadingManager.show(this, "Changing password...");
        
        // Call API to change password
        ApiClient.changePassword(userId, currentPass, newPass, new ApiClient.ApiCallback<Object>() {
            @Override
            public void onSuccess(Object result) {
                com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
                Toast.makeText(ChangePasswordActivity.this, "Password changed successfully!", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
            
            @Override
            public void onError(String errorMessage) {
                com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
                if (errorMessage.contains("Incorrect")) {
                    etCurrentPassword.setError("Incorrect password");
                    etCurrentPassword.requestFocus();
                } else {
                    Toast.makeText(ChangePasswordActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
