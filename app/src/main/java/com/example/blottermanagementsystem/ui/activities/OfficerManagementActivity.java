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
import com.example.blottermanagementsystem.data.database.BlotterDatabase;
import com.example.blottermanagementsystem.data.entity.Officer;
import com.example.blottermanagementsystem.ui.adapters.OfficerAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.example.blottermanagementsystem.data.entity.BlotterReport;
import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class OfficerManagementActivity extends BaseActivity {
    
    private TextInputEditText etSearch;
    private RecyclerView recyclerOfficers;
    private android.widget.LinearLayout emptyState;
    private androidx.cardview.widget.CardView emptyStateCard;
    private FloatingActionButton fabAddOfficer;
    
    private BlotterDatabase database;
    private List<Officer> officersList = new ArrayList<>();
    private OfficerAdapter officerAdapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_officer_management);
        
        database = BlotterDatabase.getDatabase(this);
        
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
            
            // Update officer
            String newFullName = newFirstName + " " + newLastName;
            officer.setName(newFullName);
            officer.setContactNumber(newContact);
            officer.setEmail(newEmail);
            officer.setGender(newGender);
            officer.setRank(newRank);
            
            // Save to database
            java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
                database.officerDao().updateOfficer(officer);
                
                runOnUiThread(() -> {
                    loadOfficers();
                    dialog.dismiss();
                });
            });
        });
        
        // Apply custom background
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        dialog.show();
    }
    
    private void deleteOfficer(Officer officer) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // ✅ Get all cases assigned to this officer
                List<BlotterReport> assignedCases = database.blotterReportDao()
                    .getReportsByAssignedOfficer(officer.getId());
                
                // ✅ Remove officer from all assigned cases
                if (assignedCases != null && !assignedCases.isEmpty()) {
                    for (BlotterReport report : assignedCases) {
                        // Clear the assigned officer
                        report.setAssignedOfficer(null);
                        database.blotterReportDao().updateReport(report);
                        
                        // ✅ Delete the push notification for this case assignment
                        NotificationManager notificationManager = 
                            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        if (notificationManager != null) {
                            notificationManager.cancel(report.getId() + 2000);
                            Log.i("OfficerMgmt", "✅ Notification cancelled for case " + report.getCaseNumber());
                        }
                    }
                }
                
                // ✅ Delete the officer
                database.officerDao().deleteOfficer(officer);
                
                runOnUiThread(() -> {
                    Toast.makeText(this, "Officer deleted and cases unassigned", Toast.LENGTH_SHORT).show();
                    loadOfficers(); // Reload the list
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error deleting officer: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("OfficerMgmt", "Error: " + e.getMessage());
                });
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
        
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
            List<Officer> officers = database.officerDao().getAllOfficers();
            
            runOnUiThread(() -> {
                officersList.clear();
                officersList.addAll(officers);
                officerAdapter.notifyDataSetChanged();
                
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
            } catch (Exception e) {
                runOnUiThread(() -> {
                    com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
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
