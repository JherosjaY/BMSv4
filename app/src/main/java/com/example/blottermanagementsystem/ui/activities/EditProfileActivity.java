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

public class EditProfileActivity extends AppCompatActivity {
    
    private TextInputEditText etFirstName, etLastName;
    private MaterialButton btnSave, btnCancel;
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
            getSupportActionBar().setTitle("Edit Profile");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        
        // Find views
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        
        // Load current data
        loadCurrentData();
        
        // Setup buttons
        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveProfile());
    }
    
    private void loadCurrentData() {
        if (!networkMonitor.isOnline()) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        ApiClient.getProfile(userId, new ApiClient.ApiCallback<Object>() {
            @Override
            public void onSuccess(Object result) {
                try {
                    com.google.gson.JsonObject userObj = (com.google.gson.JsonObject) result;
                    String firstName = userObj.get("firstName").getAsString();
                    String lastName = userObj.get("lastName").getAsString();
                    
                    etFirstName.setText(firstName);
                    etLastName.setText(lastName);
                } catch (Exception e) {
                    Toast.makeText(EditProfileActivity.this, "Error loading profile", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                Toast.makeText(EditProfileActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
    
    private void saveProfile() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        
        // Validate
        if (firstName.isEmpty()) {
            etFirstName.setError("Required");
            etFirstName.requestFocus();
            return;
        }
        
        if (lastName.isEmpty()) {
            etLastName.setError("Required");
            etLastName.requestFocus();
            return;
        }
        
        // Check internet
        if (!networkMonitor.isOnline()) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show loading for profile update
        com.example.blottermanagementsystem.utils.GlobalLoadingManager.show(this, "Updating profile...");
        
        // Call API to update profile
        ApiClient.updateProfile(userId, firstName, lastName, new ApiClient.ApiCallback<Object>() {
            @Override
            public void onSuccess(Object result) {
                com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
                Toast.makeText(EditProfileActivity.this, "Profile updated!", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
            
            @Override
            public void onError(String errorMessage) {
                com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
                Toast.makeText(EditProfileActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
