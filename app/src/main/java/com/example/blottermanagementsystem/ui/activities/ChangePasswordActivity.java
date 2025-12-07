package com.example.blottermanagementsystem.ui.activities;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.database.BlotterDatabase;
import com.example.blottermanagementsystem.data.entity.User;
import com.example.blottermanagementsystem.utils.PreferencesManager;

public class ChangePasswordActivity extends AppCompatActivity {
    
    private TextInputEditText etCurrentPassword, etNewPassword, etConfirmPassword;
    private MaterialButton btnChangePassword, btnCancel;
    private PreferencesManager preferencesManager;
    private int userId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        
        // Initialize
        preferencesManager = new PreferencesManager(this);
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
        
        // Show loading for password change
        com.example.blottermanagementsystem.utils.GlobalLoadingManager.show(this, "Changing password...");
        
        new Thread(() -> {
            try {
            BlotterDatabase db = BlotterDatabase.getDatabase(this);
            User user = db.userDao().getUserById(userId);
            
            if (user != null) {
                // Check current password
                if (!user.getPassword().equals(currentPass)) {
                    runOnUiThread(() -> {
                        com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
                        etCurrentPassword.setError("Incorrect password");
                        etCurrentPassword.requestFocus();
                    });
                    return;
                }
                
                // Update password
                user.setPassword(newPass);
                db.userDao().updateUser(user);
                
                runOnUiThread(() -> {
                    com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
                    setResult(RESULT_OK);
                    finish();
                });
            }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
                    Toast.makeText(this, "Error changing password: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}
