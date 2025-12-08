package com.example.blottermanagementsystem.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.database.BlotterDatabase;
import com.example.blottermanagementsystem.data.entity.BlotterReport;
import com.example.blottermanagementsystem.ui.adapters.ReportAdapter;
import com.example.blottermanagementsystem.utils.PreferencesManager;
import com.example.blottermanagementsystem.data.api.ApiClient;
import com.example.blottermanagementsystem.utils.NetworkMonitor;
import android.widget.Toast;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

public class UserViewReportsActivity extends BaseActivity {
    
    private RecyclerView recyclerReports;
    private ReportAdapter adapter;
    private View emptyState;
    private androidx.cardview.widget.CardView emptyStateCard;
    private Chip chipAll, chipPending, chipAssigned, chipOngoing, chipResolved;
    private EditText etSearch;
    private TextView tvTotalCount, tvAssignedCount, tvOngoingCount, tvResolvedCount;
    private ImageButton btnSort;
    private ImageView emptyStateIcon;
    private TextView emptyStateTitle, emptyStateMessage;
    private List<BlotterReport> allReports = new ArrayList<>();
    private List<BlotterReport> filteredReports = new ArrayList<>();
    private PreferencesManager preferencesManager;
    private int userId;
    private String searchQuery = "";
    private String currentSort = "Newest First";
    private String filterType = "ALL";  // ALL, PENDING, ASSIGNED, ONGOING, RESOLVED
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            android.util.Log.d("UserViewReports", "Starting onCreate...");
            setContentView(R.layout.activity_user_view_reports);
            
            preferencesManager = new PreferencesManager(this);
            userId = preferencesManager.getUserId();
            filterType = getIntent().getStringExtra("filter_type");
            if (filterType == null) {
                filterType = "ALL";
            }
            
            initializeViews();
            setupToolbar();
            setupListeners();
            
            // ✅ Set the appropriate chip as checked based on filter type
            selectChipByType();
            
            loadReports();
            startPeriodicRefresh();
            
            android.util.Log.d("UserViewReports", "onCreate completed successfully");
        } catch (Exception e) {
            android.util.Log.e("UserViewReports", "Error in onCreate: " + e.getMessage(), e);
            showErrorState();
        }
    }
    
    private void selectChipByType() {
        if (chipAll == null || chipPending == null || chipAssigned == null || 
            chipOngoing == null || chipResolved == null) {
            return;
        }
        
        // Uncheck all first
        chipAll.setChecked(false);
        chipPending.setChecked(false);
        chipAssigned.setChecked(false);
        chipOngoing.setChecked(false);
        chipResolved.setChecked(false);
        
        // Check the appropriate chip
        switch (filterType) {
            case "PENDING":
                chipPending.setChecked(true);
                break;
            case "ASSIGNED":
                chipAssigned.setChecked(true);
                break;
            case "ONGOING":
                chipOngoing.setChecked(true);
                break;
            case "RESOLVED":
                chipResolved.setChecked(true);
                break;
            default:
                chipAll.setChecked(true);
                break;
        }
    }
    
    private void initializeViews() {
        try {
            recyclerReports = findViewById(R.id.recyclerReports);
            emptyState = findViewById(R.id.emptyState);
            emptyStateCard = findViewById(R.id.emptyStateCard);
            etSearch = findViewById(R.id.etSearch);
            tvTotalCount = findViewById(R.id.tvTotalCount);
            tvOngoingCount = findViewById(R.id.tvOngoingCount);
            tvResolvedCount = findViewById(R.id.tvResolvedCount);
            btnSort = findViewById(R.id.btnSort);
            chipAll = findViewById(R.id.chipAll);
            chipPending = findViewById(R.id.chipPending);
            chipAssigned = findViewById(R.id.chipAssigned);
            chipOngoing = findViewById(R.id.chipOngoing);
            chipResolved = findViewById(R.id.chipResolved);
            emptyStateIcon = findViewById(R.id.emptyStateIcon);
            emptyStateMessage = findViewById(R.id.emptyStateMessage);
            
            // Scale empty state icon to be larger
            if (emptyStateIcon != null) {
                emptyStateIcon.setScaleX(1.5f);
                emptyStateIcon.setScaleY(1.5f);
            }
            
            // Setup RecyclerView
            if (recyclerReports != null) {
                adapter = new ReportAdapter(filteredReports, report -> {
                    try {
                        String userRole = preferencesManager.getUserRole();
                        Class<?> targetActivity;
                        
                        if ("Admin".equalsIgnoreCase(userRole)) {
                            targetActivity = AdminCaseDetailActivity.class;
                        } else if ("Officer".equalsIgnoreCase(userRole)) {
                            targetActivity = OfficerCaseDetailActivity.class;
                        } else {
                            targetActivity = ReportDetailActivity.class;
                        }
                        
                        Intent intent = new Intent(this, targetActivity);
                        intent.putExtra("REPORT_ID", report.getId());
                        startActivity(intent);
                    } catch (Exception e) {
                        android.util.Log.e("UserViewReports", "Error opening report detail: " + e.getMessage());
                    }
                });
                recyclerReports.setLayoutManager(new LinearLayoutManager(this));
                recyclerReports.setAdapter(adapter);
            }
        } catch (Exception e) {
            android.util.Log.e("UserViewReports", "Error initializing views: " + e.getMessage());
            throw e;
        }
    }
    
    private void setupToolbar() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
    
    private void setupListeners() {
        try {
            // Setup search with suggestions
            if (etSearch != null) {
                // Setup AutoCompleteTextView adapter
                setupSearchSuggestions();
                
                etSearch.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        searchQuery = s.toString().toLowerCase();
                        filterReports();
                        updateSearchSuggestions(s.toString());
                    }
                    
                    @Override
                    public void afterTextChanged(Editable s) {}
                });
            }
            
            // Setup chip listeners
            if (chipAll != null) {
                chipAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        filterType = "ALL";
                        allReports.clear();
                        filteredReports.clear();
                        loadReports();
                        updateStatistics();
                    }
                });
            }
            
            if (chipPending != null) {
                chipPending.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        filterType = "PENDING";
                        filterReports();
                    }
                });
            }
            
            if (chipAssigned != null) {
                chipAssigned.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        filterType = "ASSIGNED";
                        filterReports();
                    }
                });
            }
            
            if (chipOngoing != null) {
                chipOngoing.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        filterType = "ONGOING";
                        filterReports();
                    }
                });
            }
            
            if (chipResolved != null) {
                chipResolved.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        filterType = "RESOLVED";
                        filterReports();
                    }
                });
            }
            
            // Setup sort button
            if (btnSort != null) {
                btnSort.setOnClickListener(v -> showSortDialog());
            }
        } catch (Exception e) {
            android.util.Log.e("UserViewReports", "Error setting up listeners: " + e.getMessage());
        }
    }
    
    private void loadReports() {
        // Load from LOCAL DATABASE FIRST (fast)
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            try {
                BlotterDatabase db = BlotterDatabase.getDatabase(this);
                List<BlotterReport> reports = db.blotterReportDao().getAllReports();
                
                runOnUiThread(() -> {
                    filterReportsByUser(reports);
                    updateStatistics();
                    filterReports();
                });
            } catch (Exception e) {
                android.util.Log.e("UserViewReports", "Error loading from database: " + e.getMessage());
            }
        });
        
        // Sync with API in background
        NetworkMonitor networkMonitor = new NetworkMonitor(this);
        if (networkMonitor.isNetworkAvailable()) {
            java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
                ApiClient.getAllReports(new ApiClient.ApiCallback<List<BlotterReport>>() {
                    @Override
                    public void onSuccess(List<BlotterReport> apiReports) {
                        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
                            try {
                                BlotterDatabase db = BlotterDatabase.getDatabase(UserViewReportsActivity.this);
                                for (BlotterReport report : apiReports) {
                                    BlotterReport existing = db.blotterReportDao().getReportById(report.getId());
                                    if (existing == null) {
                                        db.blotterReportDao().insertReport(report);
                                    } else {
                                        db.blotterReportDao().updateReport(report);
                                    }
                                }
                                
                                runOnUiThread(() -> {
                                    filterReportsByUser(apiReports);
                                    updateStatistics();
                                    filterReports();
                                });
                            } catch (Exception e) {
                                android.util.Log.e("UserViewReports", "Error syncing API data: " + e.getMessage());
                            }
                        });
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        android.util.Log.w("UserViewReports", "API sync error: " + errorMessage);
                    }
                });
            });
        }
    }
    
    private void filterReportsByUser(List<BlotterReport> reports) {
        allReports.clear();
        for (BlotterReport report : reports) {
            if (report.getReportedById() == userId) {
                allReports.add(report);
            }
        }
    }
    
    private void updateStatistics() {
        int total = allReports.size();
        int pending = 0;
        int ongoing = 0;
        int resolved = 0;
        
        for (BlotterReport report : allReports) {
            String status = report.getStatus();
            if (status != null) {
                status = status.toUpperCase();
                if ("PENDING".equals(status) || "ASSIGNED".equals(status)) {
                    pending++;
                } else if ("ONGOING".equals(status) || "IN PROGRESS".equals(status)) {
                    ongoing++;
                } else if ("RESOLVED".equals(status)) {
                    resolved++;
                }
            }
        }
        
        if (tvTotalCount != null) tvTotalCount.setText(String.valueOf(total));
        if (tvAssignedCount != null) tvAssignedCount.setText(String.valueOf(pending));
        if (tvOngoingCount != null) tvOngoingCount.setText(String.valueOf(ongoing));
        if (tvResolvedCount != null) tvResolvedCount.setText(String.valueOf(resolved));
    }
    
    private void filterReports() {
        filteredReports.clear();
        
        // First filter by status
        List<BlotterReport> statusFiltered = new ArrayList<>();
        for (BlotterReport report : allReports) {
            String status = report.getStatus() != null ? report.getStatus().toUpperCase() : "";
            
            switch (filterType) {
                case "PENDING":
                    if ("PENDING".equals(status)) {
                        statusFiltered.add(report);
                    }
                    break;
                case "ASSIGNED":
                    if ("ASSIGNED".equals(status)) {
                        statusFiltered.add(report);
                    }
                    break;
                case "ONGOING":
                    if ("ONGOING".equals(status) || "IN PROGRESS".equals(status)) {
                        statusFiltered.add(report);
                    }
                    break;
                case "RESOLVED":
                    if ("RESOLVED".equals(status)) {
                        statusFiltered.add(report);
                    }
                    break;
                default:  // ALL
                    statusFiltered.add(report);
                    break;
            }
        }
        
        // Then apply search filter
        if (searchQuery.isEmpty()) {
            filteredReports.addAll(statusFiltered);
        } else {
            for (BlotterReport report : statusFiltered) {
                String caseNumber = report.getCaseNumber() != null ? report.getCaseNumber().toLowerCase() : "";
                String incidentType = report.getIncidentType() != null ? report.getIncidentType().toLowerCase() : "";
                String complainant = report.getComplainantName() != null ? report.getComplainantName().toLowerCase() : "";
                
                if (caseNumber.contains(searchQuery) || 
                    incidentType.contains(searchQuery) || 
                    complainant.contains(searchQuery)) {
                    filteredReports.add(report);
                }
            }
        }
        
        sortReports();
        
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        
        updateEmptyState();
    }
    
    private void sortReports() {
        switch (currentSort) {
            case "Newest First":
                Collections.sort(filteredReports, (r1, r2) -> 
                    Long.compare(r2.getDateFiled(), r1.getDateFiled()));
                break;
            case "Oldest First":
                Collections.sort(filteredReports, (r1, r2) -> 
                    Long.compare(r1.getDateFiled(), r2.getDateFiled()));
                break;
        }
    }
    
    private void updateEmptyState() {
        if (filteredReports.isEmpty()) {
            if (emptyStateCard != null) emptyStateCard.setVisibility(View.VISIBLE);
            if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
            if (recyclerReports != null) recyclerReports.setVisibility(View.GONE);
            
            // ✅ Set empty state based on filter type with different icons
            if (emptyStateIcon != null) {
                switch (filterType) {
                    case "PENDING":
                        emptyStateIcon.setImageResource(R.drawable.ic_clock_filled);
                        break;
                    case "ASSIGNED":
                        emptyStateIcon.setImageResource(R.drawable.ic_person);
                        break;
                    case "ONGOING":
                        emptyStateIcon.setImageResource(R.drawable.ic_clipboard);
                        break;
                    case "RESOLVED":
                        emptyStateIcon.setImageResource(R.drawable.ic_check_circle);
                        break;
                    default:
                        emptyStateIcon.setImageResource(R.drawable.ic_clipboard);
                        break;
                }
            }
            
            if (emptyStateTitle != null) {
                switch (filterType) {
                    case "PENDING":
                        emptyStateTitle.setText("No Pending Reports");
                        break;
                    case "ONGOING":
                        emptyStateTitle.setText("No Ongoing Reports");
                        break;
                    case "RESOLVED":
                        emptyStateTitle.setText("No Resolved Reports");
                        break;
                    default:
                        emptyStateTitle.setText("No Reports Found");
                        break;
                }
            }
            
            if (emptyStateMessage != null) {
                switch (filterType) {
                    case "PENDING":
                        emptyStateMessage.setText("No pending cases at the moment.");
                        break;
                    case "ONGOING":
                        emptyStateMessage.setText("No ongoing cases at the moment.");
                        break;
                    case "RESOLVED":
                        emptyStateMessage.setText("No resolved cases at the moment.");
                        break;
                    default:
                        emptyStateMessage.setText("Try adjusting your filters\nor search criteria.");
                        break;
                }
            }
        } else {
            if (emptyStateCard != null) emptyStateCard.setVisibility(View.VISIBLE);
            if (emptyState != null) emptyState.setVisibility(View.GONE);
            if (recyclerReports != null) recyclerReports.setVisibility(View.VISIBLE);
        }
    }
    
    private void showSortDialog() {
        String[] sortOptions = {"Newest First", "Oldest First"};
        
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this, R.style.CustomAlertDialog);
        builder.setTitle("Sort Reports")
            .setSingleChoiceItems(sortOptions, currentSort.equals("Newest First") ? 0 : 1, 
                (dialog, which) -> {
                    currentSort = sortOptions[which];
                    filterReports();
                    dialog.dismiss();
                })
            .setNegativeButton("Cancel", null);
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
        }
        dialog.show();
    }
    
    private void showErrorState() {
        if (emptyStateCard != null) emptyStateCard.setVisibility(View.VISIBLE);
        if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
        if (emptyStateTitle != null) emptyStateTitle.setText("Error Loading");
        if (emptyStateMessage != null) emptyStateMessage.setText("Please try again or\ncontact support if issue persists.");
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadReports();
    }
    
    private void startPeriodicRefresh() {
        android.os.Handler handler = new android.os.Handler();
        Runnable refreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isFinishing() && !isDestroyed()) {
                    loadReportsQuietly();
                }
                handler.postDelayed(this, 15000);
            }
        };
        handler.postDelayed(refreshRunnable, 15000);
    }
    
    private void loadReportsQuietly() {
        new Thread(() -> {
            try {
                BlotterDatabase db = BlotterDatabase.getDatabase(this);
                List<BlotterReport> reports = db.blotterReportDao().getAllReports();
                
                List<BlotterReport> userReports = new ArrayList<>();
                for (BlotterReport report : reports) {
                    if (report.getReportedById() == userId) {
                        userReports.add(report);
                    }
                }
                
                if (userReports.size() != allReports.size()) {
                    runOnUiThread(() -> {
                        allReports.clear();
                        allReports.addAll(userReports);
                        filterReports();
                        updateStatistics();
                    });
                }
            } catch (Exception e) {
                android.util.Log.e("UserViewReports", "Error in quiet refresh: " + e.getMessage());
            }
        }).start();
    }
    
    // ✅ Setup search suggestions
    private void setupSearchSuggestions() {
        if (!(etSearch instanceof AutoCompleteTextView)) {
            return;
        }
        
        AutoCompleteTextView autoCompleteTextView = (AutoCompleteTextView) etSearch;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            new ArrayList<>()
        );
        autoCompleteTextView.setAdapter(adapter);
        autoCompleteTextView.setThreshold(1);  // Show suggestions after 1 character
    }
    
    // ✅ Update search suggestions based on user input
    private void updateSearchSuggestions(String query) {
        if (!(etSearch instanceof AutoCompleteTextView)) {
            return;
        }
        
        AutoCompleteTextView autoCompleteTextView = (AutoCompleteTextView) etSearch;
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) autoCompleteTextView.getAdapter();
        
        if (adapter == null) {
            return;
        }
        
        adapter.clear();
        
        if (query.isEmpty()) {
            return;
        }
        
        String queryLower = query.toLowerCase();
        java.util.Set<String> suggestions = new java.util.HashSet<>();
        
        // Generate suggestions from current reports
        for (BlotterReport report : allReports) {
            // Add case number suggestions
            if (report.getCaseNumber() != null && report.getCaseNumber().toLowerCase().contains(queryLower)) {
                suggestions.add(report.getCaseNumber());
            }
            
            // Add incident type suggestions
            if (report.getIncidentType() != null && report.getIncidentType().toLowerCase().contains(queryLower)) {
                suggestions.add(report.getIncidentType());
            }
            
            // Add complainant name suggestions
            if (report.getComplainantName() != null && report.getComplainantName().toLowerCase().contains(queryLower)) {
                suggestions.add(report.getComplainantName());
            }
        }
        
        // Add suggestions to adapter
        adapter.addAll(new java.util.ArrayList<>(suggestions));
        adapter.notifyDataSetChanged();
    }
}
