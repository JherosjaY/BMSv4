package com.example.blottermanagementsystem.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.database.BlotterDatabase;
import com.example.blottermanagementsystem.data.entity.BlotterReport;
import com.example.blottermanagementsystem.data.entity.Officer;
import com.example.blottermanagementsystem.ui.adapters.ReportAdapter;
import com.example.blottermanagementsystem.utils.PreferencesManager;
import com.example.blottermanagementsystem.utils.ApiClient;
import com.example.blottermanagementsystem.utils.NetworkMonitor;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

public class OfficerViewAssignedReportsActivity_New extends BaseActivity {
    
    private RecyclerView recyclerReports;
    private ReportAdapter adapter;
    private View emptyState;
    private androidx.cardview.widget.CardView emptyStateCard;
    private Chip chipAll, chipPending, chipOngoing, chipResolved;
    private EditText etSearch;
    private TextView tvTotalCount, tvPendingCount, tvOngoingCount, tvResolvedCount;
    private ImageButton btnSort;
    private ImageView emptyStateIcon;
    private TextView emptyStateTitle, emptyStateMessage;
    private List<BlotterReport> allReports = new ArrayList<>();
    private List<BlotterReport> filteredReports = new ArrayList<>();
    private PreferencesManager preferencesManager;
    private int officerId = -1;
    private String searchQuery = "";
    private String currentSort = "Newest First";
    private java.util.Timer refreshTimer;
    private String selectedChip = "ASSIGNED"; // Default chip to select
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            android.util.Log.d("OfficerAssigned", "✅ onCreate() started");
            setContentView(R.layout.activity_officer_view_assigned_reports_new);
            android.util.Log.d("OfficerAssigned", "✅ Layout inflated");
            
            preferencesManager = new PreferencesManager(this);
            BlotterDatabase database = BlotterDatabase.getDatabase(this);
            android.util.Log.d("OfficerAssigned", "✅ Database initialized");
            
            // Get chip parameter from intent
            Intent intent = getIntent();
            if (intent != null && intent.hasExtra("SELECTED_CHIP")) {
                selectedChip = intent.getStringExtra("SELECTED_CHIP");
                android.util.Log.d("OfficerAssigned", "✅ Selected chip from intent: " + selectedChip);
            }
            
            initViews();
            android.util.Log.d("OfficerAssigned", "✅ Views initialized");
            setupToolbar();
            android.util.Log.d("OfficerAssigned", "✅ Toolbar setup");
            setEmptyStateIcon();
            android.util.Log.d("OfficerAssigned", "✅ Empty state icon set");
            setupListeners();
            android.util.Log.d("OfficerAssigned", "✅ Listeners setup");
            setupRecyclerView();
            android.util.Log.d("OfficerAssigned", "✅ RecyclerView setup");
            
            // Get officer ID on background thread
            int userId = preferencesManager.getUserId();
            android.util.Log.d("OfficerAssigned", "✅ User ID: " + userId);
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    Officer officer = database.officerDao().getOfficerByUserId(userId);
                    if (officer != null) {
                        officerId = officer.getId();
                        android.util.Log.d("OfficerAssigned", "✅ Officer ID: " + officerId);
                        
                        // IMMEDIATELY load statistics from database (before UI loads)
                        loadStatisticsFromDatabase();
                    } else {
                        android.util.Log.e("OfficerAssigned", "❌ Officer is null!");
                    }
                    runOnUiThread(() -> {
                        // Set chip checked AFTER officer ID is loaded
                        selectChipByType(selectedChip);
                        loadReports();
                        startPeriodicRefresh(); // Start real-time sync
                    });
                } catch (Exception e) {
                    android.util.Log.e("OfficerAssigned", "❌ Background thread error: " + e.getMessage(), e);
                }
            });
        } catch (Exception e) {
            android.util.Log.e("OfficerAssigned", "❌ onCreate() CRASH: " + e.getClass().getSimpleName() + " - " + e.getMessage(), e);
            android.widget.Toast.makeText(this, "❌ CRASH: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
        }
    }
    
    private void selectChipByType(String chipType) {
        // Uncheck all chips first
        if (chipAll != null) chipAll.setChecked(false);
        if (chipPending != null) chipPending.setChecked(false);
        if (chipOngoing != null) chipOngoing.setChecked(false);
        if (chipResolved != null) chipResolved.setChecked(false);
        
        // Check the selected chip
        switch (chipType) {
            case "ALL":
                if (chipAll != null) chipAll.setChecked(true);
                break;
            case "ASSIGNED":
                if (chipPending != null) chipPending.setChecked(true);
                break;
            case "ONGOING":
                if (chipOngoing != null) chipOngoing.setChecked(true);
                break;
            case "RESOLVED":
                if (chipResolved != null) chipResolved.setChecked(true);
                break;
            default:
                if (chipPending != null) chipPending.setChecked(true);
        }
    }
    
    private void setEmptyStateIcon() {
        // Set icon for "Assigned" screen (default)
        updateEmptyStateIcon("ASSIGNED");
    }
    
    private void updateEmptyStateIcon(String chipType) {
        if (emptyStateIcon == null) return;
        
        switch (chipType) {
            case "ALL":
                emptyStateIcon.setImageResource(R.drawable.ic_clipboard);
                break;
            case "ASSIGNED":
                emptyStateIcon.setImageResource(R.drawable.ic_folder);
                break;
            case "ONGOING":
                emptyStateIcon.setImageResource(R.drawable.ic_cases);
                break;
            case "RESOLVED":
                emptyStateIcon.setImageResource(R.drawable.ic_check_circle);
                break;
            default:
                emptyStateIcon.setImageResource(R.drawable.ic_folder);
        }
    }
    
    private void initViews() {
        recyclerReports = findViewById(R.id.recyclerReports);
        emptyState = findViewById(R.id.emptyState);
        emptyStateCard = findViewById(R.id.emptyStateCard);
        etSearch = findViewById(R.id.etSearch);
        tvTotalCount = findViewById(R.id.tvTotalCount);
        tvPendingCount = findViewById(R.id.tvPendingCount);
        tvOngoingCount = findViewById(R.id.tvOngoingCount);
        tvResolvedCount = findViewById(R.id.tvResolvedCount);
        btnSort = findViewById(R.id.btnSort);
        emptyStateIcon = findViewById(R.id.emptyStateIcon);
        emptyStateTitle = findViewById(R.id.emptyStateTitle);
        emptyStateMessage = findViewById(R.id.emptyStateMessage);
        
        chipAll = findViewById(R.id.chipAll);
        chipPending = findViewById(R.id.chipPending);
        chipOngoing = findViewById(R.id.chipOngoing);
        chipResolved = findViewById(R.id.chipResolved);
    }
    
    private void setupToolbar() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle("Assigned Cases");
            toolbar.setNavigationOnClickListener(v -> goBackToOfficerDashboard());
        }
    }
    
    private void goBackToOfficerDashboard() {
        Intent intent = new Intent(this, OfficerDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
    
    @Override
    public void onBackPressed() {
        goBackToOfficerDashboard();
    }
    
    private void setupListeners() {
        // Search
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchQuery = s.toString().toLowerCase();
                    filterReports();
                }
                
                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
        
        // Sort button
        if (btnSort != null) {
            btnSort.setOnClickListener(v -> showSortDialog());
        }
        
        // Chip listeners with animations
        if (chipAll != null) {
            chipAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    animateChip(chipAll);
                    updateEmptyStateIcon("ALL");
                    navigateToScreen(OfficerViewAllReportsActivity_New.class);
                }
            });
        }
        
        if (chipPending != null) {
            chipPending.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    animateChip(chipPending);
                    updateEmptyStateIcon("ASSIGNED");
                    // Already on assigned screen, just refresh
                    loadReports();
                }
            });
        }
        
        if (chipOngoing != null) {
            chipOngoing.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    animateChip(chipOngoing);
                    updateEmptyStateIcon("ONGOING");
                    navigateToScreen(OfficerViewOngoingReportsActivity_New.class);
                }
            });
        }
        
        if (chipResolved != null) {
            chipResolved.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    animateChip(chipResolved);
                    updateEmptyStateIcon("RESOLVED");
                    navigateToScreen(OfficerViewResolvedReportsActivity_New.class);
                }
            });
        }
    }
    
    private void navigateToScreen(Class<?> activityClass) {
        Intent intent = new Intent(this, activityClass);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        // Pass the selected chip type to the new activity
        if (activityClass == OfficerViewAllReportsActivity_New.class) {
            intent.putExtra("SELECTED_CHIP", "ALL");
        } else if (activityClass == OfficerViewOngoingReportsActivity_New.class) {
            intent.putExtra("SELECTED_CHIP", "ONGOING");
        } else if (activityClass == OfficerViewResolvedReportsActivity_New.class) {
            intent.putExtra("SELECTED_CHIP", "RESOLVED");
        }
        
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
    
    private void setupRecyclerView() {
        recyclerReports.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReportAdapter(filteredReports, report -> {
            Intent intent = new Intent(this, OfficerCaseDetailActivity.class);
            intent.putExtra("reportId", report.getId());
            startActivity(intent);
        });
        recyclerReports.setAdapter(adapter);
    }
    
    private void loadReports() {
        NetworkMonitor networkMonitor = new NetworkMonitor(this);
        if (networkMonitor.isNetworkAvailable()) {
            loadReportsFromApi();
        } else {
            loadReportsFromDatabase();
        }
    }
    
    private void loadReportsFromApi() {
        ApiClient.getAllReports(new ApiClient.ApiCallback<List<BlotterReport>>() {
            @Override
            public void onSuccess(List<BlotterReport> apiReports) {
                BlotterDatabase db = BlotterDatabase.getDatabase(OfficerViewAssignedReportsActivity_New.this);
                new Thread(() -> {
                    try {
                        for (BlotterReport report : apiReports) {
                            BlotterReport existing = db.blotterReportDao().getReportById(report.getId());
                            if (existing == null) {
                                db.blotterReportDao().insertReport(report);
                            } else {
                                db.blotterReportDao().updateReport(report);
                            }
                        }
                        runOnUiThread(() -> {
                            allReports.clear();
                            allReports.addAll(apiReports);
                            updateStatistics();
                            filterReports();
                        });
                    } catch (Exception e) {
                        android.util.Log.e("OfficerAssigned", "Error saving API data: " + e.getMessage());
                        loadReportsFromDatabase();
                    }
                }).start();
            }
            
            @Override
            public void onError(String errorMessage) {
                android.util.Log.w("OfficerAssigned", "API error: " + errorMessage);
                loadReportsFromDatabase();
            }
        });
    }
    
    private void loadReportsFromDatabase() {
        new Thread(() -> {
            try {
                BlotterDatabase database = BlotterDatabase.getDatabase(this);
                List<BlotterReport> reports = database.blotterReportDao().getAllReports();
                
                // Filter reports assigned to this officer (ALL statuses)
                List<BlotterReport> officerReports = new ArrayList<>();
                for (BlotterReport report : reports) {
                    boolean isAssignedToOfficer = false;
                    if (report.getAssignedOfficerId() != null && report.getAssignedOfficerId().intValue() == officerId) {
                        isAssignedToOfficer = true;
                    }
                    if (!isAssignedToOfficer && report.getAssignedOfficerIds() != null && !report.getAssignedOfficerIds().isEmpty()) {
                        String[] officerIds = report.getAssignedOfficerIds().split(",");
                        for (String id : officerIds) {
                            try {
                                if (Integer.parseInt(id.trim()) == officerId) {
                                    isAssignedToOfficer = true;
                                    break;
                                }
                            } catch (NumberFormatException e) {
                                // Ignore
                            }
                        }
                    }
                    
                    if (isAssignedToOfficer) {
                        officerReports.add(report);
                    }
                }
                
                runOnUiThread(() -> {
                    allReports.clear();
                    allReports.addAll(officerReports);
                    updateStatistics();
                    filterReports();
                });
            } catch (Exception e) {
                android.util.Log.e("OfficerAssigned", "Error loading from database: " + e.getMessage());
            }
        }).start();
    }
    
    private void loadAllReports() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                BlotterDatabase database = BlotterDatabase.getDatabase(this);
                List<BlotterReport> reports = database.blotterReportDao().getAllReports();
                
                allReports.clear();
                // Load ALL cases (all statuses) assigned to this officer
                for (BlotterReport report : reports) {
                    // Check if officer is assigned (either single or multiple officers)
                    boolean isAssignedToOfficer = false;
                    
                    // Check single officer assignment
                    if (report.getAssignedOfficerId() != null && report.getAssignedOfficerId().intValue() == officerId) {
                        isAssignedToOfficer = true;
                    }
                    
                    // Check multiple officers assignment
                    if (!isAssignedToOfficer && report.getAssignedOfficerIds() != null && !report.getAssignedOfficerIds().isEmpty()) {
                        String[] officerIds = report.getAssignedOfficerIds().split(",");
                        for (String id : officerIds) {
                            try {
                                if (Integer.parseInt(id.trim()) == officerId) {
                                    isAssignedToOfficer = true;
                                    break;
                                }
                            } catch (NumberFormatException e) {
                                // Ignore invalid IDs
                            }
                        }
                    }
                    
                    if (isAssignedToOfficer) {
                        allReports.add(report);
                    }
                }
                
                runOnUiThread(this::filterReports);
            } catch (Exception e) {
                android.util.Log.e("OfficerAssigned", "Error loading all reports: " + e.getMessage(), e);
            }
        });
    }
    
    private void updateStatistics() {
        // FAST: Calculate statistics from already-loaded allReports (no database query needed!)
        int total = allReports.size();
        int assigned = 0, ongoing = 0, resolved = 0;
        
        android.util.Log.d("OfficerAssigned", "⚡ FAST: Calculating statistics from allReports (" + total + " reports)");
        
        for (BlotterReport report : allReports) {
            String status = report.getStatus() != null ? report.getStatus().toUpperCase().trim() : "";
            android.util.Log.d("OfficerAssigned", "  ✓ Case: " + report.getCaseNumber() + " | Status: '" + status + "'");
            
            if ("ASSIGNED".equals(status)) {
                assigned++;
            } else if ("ONGOING".equals(status) || "IN PROGRESS".equals(status)) {
                ongoing++;
            } else if ("RESOLVED".equals(status) || "CLOSED".equals(status)) {
                resolved++;
            }
        }
        
        android.util.Log.d("OfficerAssigned", "✅ Statistics: Total=" + total + ", Assigned=" + assigned + ", Ongoing=" + ongoing + ", Resolved=" + resolved);
        
        // Update UI immediately on main thread (no background thread needed!)
        if (tvTotalCount != null) {
            tvTotalCount.setText(String.valueOf(total));
            android.util.Log.d("OfficerAssigned", "✅ tvTotalCount updated: " + total);
        }
        if (tvPendingCount != null) {
            tvPendingCount.setText(String.valueOf(assigned));
            android.util.Log.d("OfficerAssigned", "✅ tvPendingCount updated: " + assigned);
        }
        if (tvOngoingCount != null) {
            tvOngoingCount.setText(String.valueOf(ongoing));
            android.util.Log.d("OfficerAssigned", "✅ tvOngoingCount updated: " + ongoing);
        }
        if (tvResolvedCount != null) {
            tvResolvedCount.setText(String.valueOf(resolved));
            android.util.Log.d("OfficerAssigned", "✅ tvResolvedCount updated: " + resolved);
        }
    }
    
    private void filterReports() {
        filteredReports.clear();
        
        // Determine which chip is currently checked to apply correct filter
        String currentFilter = "ASSIGNED"; // Default to ASSIGNED for this activity
        
        if (chipAll != null && chipAll.isChecked()) {
            currentFilter = "ALL";
        } else if (chipPending != null && chipPending.isChecked()) {
            currentFilter = "ASSIGNED";
        } else if (chipOngoing != null && chipOngoing.isChecked()) {
            currentFilter = "ONGOING";
        } else if (chipResolved != null && chipResolved.isChecked()) {
            currentFilter = "RESOLVED";
        }
        
        android.util.Log.d("OfficerAssigned", "🔍 FILTER: Current chip filter: " + currentFilter + " | Total reports: " + allReports.size());
        
        for (BlotterReport report : allReports) {
            String status = report.getStatus() != null ? report.getStatus().toUpperCase().trim() : "";
            
            // Apply status filter based on which chip is checked
            boolean matchesStatusFilter = false;
            
            if ("ALL".equals(currentFilter)) {
                matchesStatusFilter = true; // Show all statuses
            } else if ("ASSIGNED".equals(currentFilter)) {
                matchesStatusFilter = "ASSIGNED".equals(status);
            } else if ("ONGOING".equals(currentFilter)) {
                matchesStatusFilter = "ONGOING".equals(status) || "IN PROGRESS".equals(status);
            } else if ("RESOLVED".equals(currentFilter)) {
                matchesStatusFilter = "RESOLVED".equals(status) || "CLOSED".equals(status);
            }
            
            // Apply search filter
            boolean matchesSearch = searchQuery.isEmpty() || 
                (report.getCaseNumber() != null && report.getCaseNumber().toLowerCase().contains(searchQuery)) ||
                (report.getIncidentType() != null && report.getIncidentType().toLowerCase().contains(searchQuery));
            
            // Add if both filters match
            if (matchesStatusFilter && matchesSearch) {
                filteredReports.add(report);
                android.util.Log.d("OfficerAssigned", "  ✅ ADDED: " + report.getCaseNumber() + " | Status: " + status);
            }
        }
        
        android.util.Log.d("OfficerAssigned", "✅ FILTER COMPLETE: " + filteredReports.size() + " cases");
        
        sortReports();
        
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        
        updateEmptyState();
    }
    
    private void sortReports() {
        if ("Newest First".equals(currentSort)) {
            Collections.sort(filteredReports, (a, b) -> Long.compare(b.getDateFiled(), a.getDateFiled()));
        } else {
            Collections.sort(filteredReports, (a, b) -> Long.compare(a.getDateFiled(), b.getDateFiled()));
        }
    }
    
    private void updateEmptyState() {
        if (filteredReports.isEmpty()) {
            if (recyclerReports != null) recyclerReports.setVisibility(View.GONE);
            if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
            
            // Show appropriate empty state message based on current chip
            if (emptyStateTitle != null) emptyStateTitle.setText("No Assigned Cases");
            if (emptyStateMessage != null) emptyStateMessage.setText("All your assigned cases\nhave been processed.");
        } else {
            if (recyclerReports != null) recyclerReports.setVisibility(View.VISIBLE);
            if (emptyState != null) emptyState.setVisibility(View.GONE);
        }
    }
    
    private void showSortDialog() {
        String[] options = {"Newest First", "Oldest First"};
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Sort By")
            .setSingleChoiceItems(options, "Newest First".equals(currentSort) ? 0 : 1, (dialog, which) -> {
                currentSort = options[which];
                sortReports();
                if (adapter != null) adapter.notifyDataSetChanged();
                dialog.dismiss();
            })
            .setNegativeButton("Cancel", null);
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
        }
        dialog.show();
    }
    
    private void startPeriodicRefresh() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
        }
        
        refreshTimer = new java.util.Timer();
        refreshTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                if (!isFinishing() && !isDestroyed()) {
                    loadReports(); // Refresh data every 5 seconds for real-time sync
                }
            }
        }, 5000, 5000); // Start after 5 seconds, repeat every 5 seconds
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer = null;
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadReports();
        if (refreshTimer == null) {
            startPeriodicRefresh(); // Restart if stopped
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer = null;
        }
    }
    
    private void animateChip(com.google.android.material.chip.Chip chip) {
        // Scale up animation when chip is selected
        android.view.animation.Animation scaleUp = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.chip_scale_up);
        chip.startAnimation(scaleUp);
    }
    
    private void loadStatisticsFromDatabase() {
        // Load statistics IMMEDIATELY from local database (no API calls)
        // This runs on background thread, so it won't block UI
        try {
            BlotterDatabase database = BlotterDatabase.getDatabase(this);
            List<BlotterReport> allSystemReports = database.blotterReportDao().getAllReports();
            
            android.util.Log.d("OfficerAssigned", "📊 IMMEDIATE: Loading statistics from database (" + allSystemReports.size() + " total reports)");
            
            int total = 0, assigned = 0, ongoing = 0, resolved = 0;
            
            for (BlotterReport report : allSystemReports) {
                // Check if officer is assigned
                boolean isAssignedToOfficer = false;
                
                if (report.getAssignedOfficerId() != null && report.getAssignedOfficerId().intValue() == officerId) {
                    isAssignedToOfficer = true;
                }
                
                if (!isAssignedToOfficer && report.getAssignedOfficerIds() != null && !report.getAssignedOfficerIds().isEmpty()) {
                    String[] officerIds = report.getAssignedOfficerIds().split(",");
                    for (String id : officerIds) {
                        try {
                            if (Integer.parseInt(id.trim()) == officerId) {
                                isAssignedToOfficer = true;
                                break;
                            }
                        } catch (NumberFormatException e) {
                            // Ignore
                        }
                    }
                }
                
                if (isAssignedToOfficer) {
                    total++;
                    String status = report.getStatus() != null ? report.getStatus().toUpperCase().trim() : "";
                    
                    if ("ASSIGNED".equals(status)) {
                        assigned++;
                    } else if ("ONGOING".equals(status) || "IN PROGRESS".equals(status)) {
                        ongoing++;
                    } else if ("RESOLVED".equals(status) || "CLOSED".equals(status)) {
                        resolved++;
                    }
                }
            }
            
            final int finalTotal = total;
            final int finalAssigned = assigned;
            final int finalOngoing = ongoing;
            final int finalResolved = resolved;
            
            // Update UI immediately on main thread
            runOnUiThread(() -> {
                android.util.Log.d("OfficerAssigned", "✅ IMMEDIATE UPDATE: Total=" + finalTotal + ", Assigned=" + finalAssigned + ", Ongoing=" + finalOngoing + ", Resolved=" + finalResolved);
                
                if (tvTotalCount != null) tvTotalCount.setText(String.valueOf(finalTotal));
                if (tvPendingCount != null) tvPendingCount.setText(String.valueOf(finalAssigned));
                if (tvOngoingCount != null) tvOngoingCount.setText(String.valueOf(finalOngoing));
                if (tvResolvedCount != null) tvResolvedCount.setText(String.valueOf(finalResolved));
            });
        } catch (Exception e) {
            android.util.Log.e("OfficerAssigned", "❌ Error loading statistics: " + e.getMessage(), e);
        }
    }
}
