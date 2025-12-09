package com.example.blottermanagementsystem.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.entity.Officer;
import com.example.blottermanagementsystem.ui.adapters.OfficerAdapter;
import com.example.blottermanagementsystem.utils.NetworkMonitor;
import com.example.blottermanagementsystem.utils.ApiClient;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class OfficerManagementActivity extends BaseActivity {
    
    private TextInputEditText etSearch;
    private RecyclerView recyclerOfficers;
    private android.widget.LinearLayout emptyState;
    private androidx.cardview.widget.CardView emptyStateCard;
    private FloatingActionButton fabAddOfficer;
    
    private NetworkMonitor networkMonitor;
    private List<Officer> officersList = new ArrayList<>();
    private OfficerAdapter officerAdapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_officer_management);
        
        networkMonitor = new NetworkMonitor(this);
        
        setupToolbar();
        initViews();
        setupRecyclerView();
        setupListeners();
        loadOfficers();
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }
    
    private void initViews() {
        etSearch = findViewById(R.id.etSearch);
        recyclerOfficers = findViewById(R.id.recyclerView);
        emptyState = findViewById(R.id.emptyState);
        emptyStateCard = findViewById(R.id.emptyStateCard);
        fabAddOfficer = findViewById(R.id.fabAddOfficer);
    }
    
    private void setupRecyclerView() {
        officerAdapter = new OfficerAdapter(officersList, officer -> {
            // Show officer details dialog
            showOfficerDetailsDialog(officer);
        });
        recyclerOfficers.setLayoutManager(new LinearLayoutManager(this));
        recyclerOfficers.setAdapter(officerAdapter);
    }
    
    private void showOfficerDetailsDialog(Officer officer) {
        // Inflate dialog layout
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_officer_details, null);
        
        // Find views
        android.widget.TextView tvAvatar = dialogView.findViewById(R.id.tvAvatar);
        android.widget.TextView tvOfficerName = dialogView.findViewById(R.id.tvOfficerName);
        com.google.android.material.chip.Chip chipStatus = dialogView.findViewById(R.id.chipStatus);
        android.widget.TextView tvContactNumber = dialogView.findViewById(R.id.tvContactNumber);
        android.widget.TextView tvEmail = dialogView.findViewById(R.id.tvEmail);
        android.widget.TextView tvGender = dialogView.findViewById(R.id.tvGender);
        android.widget.TextView tvRank = dialogView.findViewById(R.id.tvRank);
        android.widget.TextView tvBadgeNumber = dialogView.findViewById(R.id.tvBadgeNumber);
        android.widget.TextView tvAssignedCases = dialogView.findViewById(R.id.tvAssignedCases);
        android.widget.TextView tvDateAdded = dialogView.findViewById(R.id.tvDateAdded);
        com.google.android.material.button.MaterialButton btnClose = dialogView.findViewById(R.id.btnClose);
        com.google.android.material.button.MaterialButton btnEdit = dialogView.findViewById(R.id.btnEdit);
        com.google.android.material.button.MaterialButton btnDelete = dialogView.findViewById(R.id.btnDelete);
        
        // Set data
        String initials = getInitials(officer.getName());
        tvAvatar.setText(initials);
        tvOfficerName.setText(officer.getName());
        
        // Status chip
        if (officer.isActive()) {
            chipStatus.setText("Active");
            chipStatus.setChipBackgroundColorResource(R.color.success_green);
        } else {
            chipStatus.setText("Inactive");
            chipStatus.setChipBackgroundColorResource(R.color.text_secondary);
        }
        
        // Personal details
        tvContactNumber.setText(officer.getContactNumber() != null ? officer.getContactNumber() : "N/A");
        tvEmail.setText(officer.getEmail() != null ? officer.getEmail() : "N/A");
        
        // Gender with dynamic icon
        String gender = officer.getGender();
        if (gender != null && !gender.isEmpty()) {
            String genderIcon = "";
            if (gender.equalsIgnoreCase("Male")) {
                genderIcon = "👨 ";
            } else if (gender.equalsIgnoreCase("Female")) {
                genderIcon = "👩 ";
            } else {
                genderIcon = "⚧ ";
            }
            tvGender.setText(genderIcon + gender);
        } else {
            tvGender.setText("N/A");
        }
        
        // Official details
        tvRank.setText(officer.getRank() != null ? officer.getRank() : "N/A");
        tvBadgeNumber.setText(officer.getBadgeNumber() != null ? officer.getBadgeNumber() : "N/A");
        tvAssignedCases.setText(String.valueOf(officer.getAssignedCases()));
        
        // Format date
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
        tvDateAdded.setText(sdf.format(new java.util.Date(officer.getDateAdded())));
        
        // Create dialog
        androidx.appcompat.app.AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setView(dialogView)
            .create();
        
        // Close button
        btnClose.setOnClickListener(v -> dialog.dismiss());
        
        // Edit button
        btnEdit.setOnClickListener(v -> {
            dialog.dismiss();
            showEditOfficerDialog(officer);
        });
        
        // Delete button
        btnDelete.setOnClickListener(v -> {
            showDeleteConfirmation(officer, dialog);
        });
        
        // Apply custom background
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        dialog.show();
    }
    
    private String getInitials(String name) {
        if (name == null || name.isEmpty()) return "??";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase();
        } else {
            return name.substring(0, Math.min(2, name.length())).toUpperCase();
        }
    }
    
    private void showEditOfficerDialog(Officer officer) {
        // Inflate edit dialog layout
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_officer, null);
        
        // Find views
        com.google.android.material.textfield.TextInputEditText etFirstName = dialogView.findViewById(R.id.etFirstName);
        com.google.android.material.textfield.TextInputEditText etLastName = dialogView.findViewById(R.id.etLastName);
        com.google.android.material.textfield.TextInputEditText etContactNumber = dialogView.findViewById(R.id.etContactNumber);
        com.google.android.material.textfield.TextInputEditText etEmail = dialogView.findViewById(R.id.etEmail);
        android.widget.Spinner spinnerGender = dialogView.findViewById(R.id.spinnerGender);
        android.widget.Spinner spinnerRank = dialogView.findViewById(R.id.spinnerRank);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        com.google.android.material.button.MaterialButton btnSave = dialogView.findViewById(R.id.btnSave);
        
        // Split name into first and last
        String[] nameParts = officer.getName().split(" ", 2);
        String firstName = nameParts.length > 0 ? nameParts[0] : "";
        String lastName = nameParts.length > 1 ? nameParts[1] : "";
        
        // Populate current values
        etFirstName.setText(firstName);
        etLastName.setText(lastName);
        etContactNumber.setText(officer.getContactNumber());
        etEmail.setText(officer.getEmail());
        
        // Setup gender spinner
        String[] genders = {"Male", "Female", "Other"};
        android.widget.ArrayAdapter<String> genderAdapter = new android.widget.ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, genders);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(genderAdapter);
        if (officer.getGender() != null) {
            int genderPos = java.util.Arrays.asList(genders).indexOf(officer.getGender());
            if (genderPos >= 0) spinnerGender.setSelection(genderPos);
        }
        
        // Setup rank spinner
        String[] ranks = {
            "Police Executive Master Sergeant (PEMS)",
            "Police Chief Master Sergeant (PCMS)",
            "Police Senior Master Sergeant (PSMS)",
            "Police Master Sergeant (PMSg)",
            "Police Staff Sergeant (PSSg)",
            "Police Corporal (PCpl)",
            "Patrolman (PTLM)",
            "Patrolwoman (PTLW)"
        };
        android.widget.ArrayAdapter<String> rankAdapter = new android.widget.ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, ranks);
        rankAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRank.setAdapter(rankAdapter);
        if (officer.getRank() != null) {
            int rankPos = java.util.Arrays.asList(ranks).indexOf(officer.getRank());
            if (rankPos >= 0) spinnerRank.setSelection(rankPos);
        }
        
        // Create dialog
        androidx.appcompat.app.AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create();
        
        // Cancel button
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        // Save button
        btnSave.setOnClickListener(v -> {
            String newFirstName = etFirstName.getText().toString().trim();
            String newLastName = etLastName.getText().toString().trim();
            String newContact = etContactNumber.getText().toString().trim();
            String newEmail = etEmail.getText().toString().trim();
            String newGender = spinnerGender.getSelectedItem().toString();
            String newRank = spinnerRank.getSelectedItem().toString();
            
            // Validate
            if (newFirstName.isEmpty() || newLastName.isEmpty()) {
                android.widget.Toast.makeText(this, "Name is required", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (!networkMonitor.isOnline()) {
                android.widget.Toast.makeText(this, "No internet connection", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Update officer via API
            String newFullName = newFirstName + " " + newLastName;
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("name", newFullName);
            updateData.put("contactNumber", newContact);
            updateData.put("email", newEmail);
            updateData.put("gender", newGender);
            updateData.put("rank", newRank);
            
            ApiClient.updateOfficer(officer.getId(), updateData, new ApiClient.ApiCallback<Object>() {
                @Override
                public void onSuccess(Object response) {
                    runOnUiThread(() -> {
                        android.widget.Toast.makeText(OfficerManagementActivity.this, "Officer updated successfully", android.widget.Toast.LENGTH_SHORT).show();
                        loadOfficers();
                        dialog.dismiss();
                    });
                }
                
                @Override
                public void onError(String errorMessage) {
                    runOnUiThread(() -> {
                        android.widget.Toast.makeText(OfficerManagementActivity.this, "Error: " + errorMessage, android.widget.Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });
        
        // Apply custom background
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        dialog.show();
    }
    
    private void deleteOfficer(Officer officer) {
        if (!networkMonitor.isOnline()) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Delete officer via API (pure online)
        ApiClient.deleteOfficer(officer.getId(), new ApiClient.ApiCallback<Object>() {
            @Override
            public void onSuccess(Object response) {
                Toast.makeText(OfficerManagementActivity.this, "Officer deleted successfully", Toast.LENGTH_SHORT).show();
                loadOfficers(); // Reload the list
            }
            
            @Override
            public void onError(String errorMessage) {
                Toast.makeText(OfficerManagementActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                Log.e("OfficerMgmt", "Error deleting officer: " + errorMessage);
            }
        });
    }
    
    private void setupListeners() {
        fabAddOfficer.setOnClickListener(v -> {
            startActivity(new Intent(this, AddOfficerActivity.class));
        });
    }
    
    private void loadOfficers() {
        // Show loading for officers list
        com.example.blottermanagementsystem.utils.GlobalLoadingManager.show(this, "Loading officers...");
        
        // ✅ PURE ONLINE: Check internet first
        com.example.blottermanagementsystem.utils.NetworkMonitor networkMonitor = 
            new com.example.blottermanagementsystem.utils.NetworkMonitor(this);
        
        if (!networkMonitor.isNetworkAvailable()) {
            android.util.Log.e("OfficerManagement", "❌ No internet connection");
            com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Online - load from API
        android.util.Log.d("OfficerManagement", "🌐 Loading officers from API");
        loadOfficersViaApi();
    }
    
    /**
     * Pure Online: Load officers via API (Neon database only)
     */
    private void loadOfficersViaApi() {
        com.example.blottermanagementsystem.utils.ApiClient.getAdminOfficers(
            new com.example.blottermanagementsystem.utils.ApiClient.ApiCallback<java.util.List<Officer>>() {
                @Override
                public void onSuccess(java.util.List<Officer> officers) {
                    android.util.Log.d("OfficerManagement", "✅ Loaded " + officers.size() + " officers from API");
                    
                    runOnUiThread(() -> {
                        officersList.clear();
                        officersList.addAll(officers);
                        if (officerAdapter != null) {
                            officerAdapter.notifyDataSetChanged();
                        }
                        
                        // CardView always visible as background
                        emptyStateCard.setVisibility(android.view.View.VISIBLE);
                        
                        if (officers.isEmpty()) {
                            emptyState.setVisibility(android.view.View.VISIBLE);
                            recyclerOfficers.setVisibility(android.view.View.GONE);
                        } else {
                            emptyState.setVisibility(android.view.View.GONE);
                            recyclerOfficers.setVisibility(android.view.View.VISIBLE);
                        }
                        
                        // Hide loading
                        com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
                    });
                }
                
                @Override
                public void onError(String errorMessage) {
                    android.util.Log.e("OfficerManagement", "❌ Failed to load officers: " + errorMessage);
                    runOnUiThread(() -> {
                        com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
                        Toast.makeText(OfficerManagementActivity.this, "Failed to load officers", Toast.LENGTH_SHORT).show();
                    });
                }
            });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadOfficers();
    }
    
    private void showDeleteConfirmation(Officer officer, androidx.appcompat.app.AlertDialog parentDialog) {
        // Inflate custom confirmation dialog
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_confirmation, null);
        
        // Get views
        android.widget.TextView tvIcon = dialogView.findViewById(R.id.tvIcon);
        android.widget.TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
        android.widget.TextView tvMessage = dialogView.findViewById(R.id.tvMessage);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        com.google.android.material.button.MaterialButton btnConfirm = dialogView.findViewById(R.id.btnConfirm);
        
        // Set content
        tvIcon.setText("🗑️");
        tvTitle.setText("Delete Officer");
        tvMessage.setText("Are you sure you want to delete " + officer.getName() + "? This action cannot be undone.");
        btnConfirm.setText("Delete");
        
        // Create dialog
        androidx.appcompat.app.AlertDialog confirmDialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create();
        
        // Set button listeners
        btnCancel.setOnClickListener(v -> confirmDialog.dismiss());
        
        btnConfirm.setOnClickListener(v -> {
            deleteOfficer(officer);
            confirmDialog.dismiss();
            parentDialog.dismiss();
        });
        
        // Apply custom background
        if (confirmDialog.getWindow() != null) {
            confirmDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        confirmDialog.show();
    }
}
