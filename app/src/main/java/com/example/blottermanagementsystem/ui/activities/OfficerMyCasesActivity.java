package com.example.blottermanagementsystem.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.PopupMenu;
import androidx.appcompat.widget.SearchView;
import android.widget.LinearLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.database.BlotterDatabase;
import com.example.blottermanagementsystem.data.entity.BlotterReport;
import com.example.blottermanagementsystem.ui.adapters.BlotterReportAdapter;
import com.example.blottermanagementsystem.utils.PreferencesManager;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
public class OfficerMyCasesActivity extends BaseActivity {
    
    private RecyclerView recyclerView;
    private BlotterReportAdapter adapter;
    private ProgressBar progressBar;
    private androidx.cardview.widget.CardView emptyStateCard;
    private android.widget.LinearLayout emptyState;
    private android.widget.EditText searchView;
    private ChipGroup chipGroupFilter;
    private ImageButton btnBack;
    private ImageView emptyStateIcon;
    private TextView emptyStateTitle, emptyStateMessage;
    private ImageButton btnSort;
    private Chip chipAll, chipPending, chipOngoing, chipResolved;
    private TextView tvTotalCount, tvPendingCount, tvOngoingCount, tvResolvedCount;
    private TextView tvTotalCases;
    
    private BlotterDatabase database;
    private PreferencesManager preferencesManager;
    private List<BlotterReport> allCases = new ArrayList<>();
    private String currentSort = "Newest First";
    private String currentFilter = "All";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_officer_cases);
        
        database = BlotterDatabase.getDatabase(this);
        preferencesManager = new PreferencesManager(this);
        
        // Get filter type from intent (same as User role)
        String filterType = getIntent().getStringExtra("filter_type");
        if (filterType == null) {
            filterType = "All";
        }
        currentFilter = filterType;
        
        initViews();
        setupRecyclerView();
        setupListeners();
        
        // Select chip based on filter type (same as User role)
        selectChipByType();
        
        loadMyCases();
        startPeriodicRefresh();
    }
    
    private void selectChipByType() {
        // Uncheck all first
        if (chipAll != null) chipAll.setChecked(false);
        if (chipPending != null) chipPending.setChecked(false);
        if (chipOngoing != null) chipOngoing.setChecked(false);
        if (chipResolved != null) chipResolved.setChecked(false);
        
        // Check the appropriate chip based on currentFilter
        switch (currentFilter) {
            case "Assigned":
                if (chipPending != null) chipPending.setChecked(true);
                break;
            case "Ongoing":
                if (chipOngoing != null) chipOngoing.setChecked(true);
                break;
            case "Resolved":
                if (chipResolved != null) chipResolved.setChecked(true);
                break;
            default:
                if (chipAll != null) chipAll.setChecked(true);
                break;
        }
    }
    
    private void initViews() {
        recyclerView = findViewById(R.id.recyclerReports);
        progressBar = findViewById(R.id.progressBar);
        emptyStateCard = findViewById(R.id.emptyStateCard);
        emptyState = findViewById(R.id.emptyState);
        searchView = findViewById(R.id.etSearch);
        chipGroupFilter = findViewById(R.id.chipGroupFilter);
        emptyStateIcon = findViewById(R.id.emptyStateIcon);
        emptyStateTitle = findViewById(R.id.emptyStateTitle);
        emptyStateMessage = findViewById(R.id.emptyStateMessage);
        btnSort = findViewById(R.id.btnSort);
        
        // Initialize chips
        chipAll = findViewById(R.id.chipAll);
        chipPending = findViewById(R.id.chipPending);
        chipOngoing = findViewById(R.id.chipOngoing);
        chipResolved = findViewById(R.id.chipResolved);
        
        // Initialize statistics TextViews
        tvTotalCount = findViewById(R.id.tvTotalCount);
        tvPendingCount = findViewById(R.id.tvPendingCount);
        tvOngoingCount = findViewById(R.id.tvOngoingCount);
        tvResolvedCount = findViewById(R.id.tvResolvedCount);
        tvTotalCases = findViewById(R.id.tvTotalCases);
    }
    
    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BlotterReportAdapter(new ArrayList<>(), report -> {
            // Open officer case detail activity (VIEW-ONLY with officer functions)
            Intent intent = new Intent(this, OfficerCaseDetailActivity.class);
            intent.putExtra("reportId", report.getId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
    }
    
    private void setupListeners() {
        // Toolbar back button (navigation icon)
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> {
                Intent intent = new Intent(this, OfficerDashboardActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            });
        }
        
        // ImageButton back (if exists)
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                Intent intent = new Intent(this, OfficerDashboardActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            });
        }
        
        // Sort button functionality
        if (btnSort != null) {
            btnSort.setOnClickListener(v -> showSortMenu());
        }
        
        // Search functionality with suggestions
        if (searchView != null) {
            searchView.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterCases(s.toString());
                    updateSearchSuggestions(s.toString());
                }
                
                @Override
                public void afterTextChanged(android.text.Editable s) {}
            });
        }
        
        // Filter chips - Use individual OnCheckedChangeListener for each chip (WORKING PATTERN)
        if (chipAll != null) {
            chipAll.setChecked(true); // Set initial selection
            chipAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    currentFilter = "All";
                    android.util.Log.d("OfficerMyCases", "All chip selected");
                    // Uncheck other chips
                    if (chipPending != null) chipPending.setChecked(false);
                    if (chipOngoing != null) chipOngoing.setChecked(false);
                    if (chipResolved != null) chipResolved.setChecked(false);
                    applyFilter();
                    // Statistics stay the same (showing totals)
                }
            });
        }
        
        if (chipPending != null) {
            chipPending.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    currentFilter = "Assigned";
                    android.util.Log.d("OfficerMyCases", "Assigned chip selected");
                    // Uncheck other chips
                    if (chipAll != null) chipAll.setChecked(false);
                    if (chipOngoing != null) chipOngoing.setChecked(false);
                    if (chipResolved != null) chipResolved.setChecked(false);
                    applyFilter();
                    // Statistics stay the same (showing totals)
                }
            });
        }
        
        if (chipOngoing != null) {
            chipOngoing.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    currentFilter = "Ongoing";
                    android.util.Log.d("OfficerMyCases", "Ongoing chip selected");
                    // Uncheck other chips
                    if (chipAll != null) chipAll.setChecked(false);
                    if (chipPending != null) chipPending.setChecked(false);
                    if (chipResolved != null) chipResolved.setChecked(false);
                    applyFilter();
                    // Statistics stay the same (showing totals)
                }
            });
        }
        
        if (chipResolved != null) {
            chipResolved.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    currentFilter = "Resolved";
                    android.util.Log.d("OfficerMyCases", "Resolved chip selected");
                    // Uncheck other chips
                    if (chipAll != null) chipAll.setChecked(false);
                    if (chipPending != null) chipPending.setChecked(false);
                    if (chipOngoing != null) chipOngoing.setChecked(false);
                    applyFilter();
                    // Statistics stay the same (showing totals)
                }
            });
        }
    }
    
    private void loadMyCases() {
        // Only show progress bar on FIRST load, not on refresh
        boolean isFirstLoad = allCases.isEmpty();
        if (isFirstLoad && progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (isFirstLoad && recyclerView != null) recyclerView.setVisibility(View.GONE);
        // Container card ALWAYS visible - never hide it
        if (isFirstLoad && emptyStateCard != null) emptyStateCard.setVisibility(View.VISIBLE);
        
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                int userId = preferencesManager.getUserId();
                // Get the officer record for this user
                com.example.blottermanagementsystem.data.entity.Officer officer = database.officerDao().getOfficerByUserId(userId);
                int officerId = (officer != null) ? officer.getId() : -1;
                
                List<BlotterReport> reports = database.blotterReportDao().getAllReports();
                
                android.util.Log.d("OfficerMyCases", "Loading cases for user ID: " + userId + ", Officer ID: " + officerId);
                android.util.Log.d("OfficerMyCases", "Total reports in database: " + reports.size());
                
                // Filter only officer's assigned cases
                allCases.clear();
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
                        allCases.add(report);
                        android.util.Log.d("OfficerMyCases", "Found assigned case: " + report.getCaseNumber());
                    }
                }
                
                android.util.Log.d("OfficerMyCases", "Total assigned cases: " + allCases.size());
                
                runOnUiThread(() -> {
                    // Hide progress bar only on first load
                    if (progressBar != null && progressBar.getVisibility() == View.VISIBLE) {
                        progressBar.setVisibility(View.GONE);
                    }
                    
                    // Background CardView ALWAYS stays visible - never hide it
                    // Only toggle between empty state content and recycler view
                    if (emptyStateCard != null) emptyStateCard.setVisibility(View.VISIBLE);
                    
                    if (allCases.isEmpty()) {
                        if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
                        if (recyclerView != null) recyclerView.setVisibility(View.GONE);
                        if (tvTotalCases != null) tvTotalCases.setText("0");
                    } else {
                        if (emptyState != null) emptyState.setVisibility(View.GONE);
                        if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
                        if (tvTotalCases != null) tvTotalCases.setText(String.valueOf(allCases.size()));
                        updateStatistics();
                        updateFilterCounts();
                        applyFilter();
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("OfficerMyCases", "Error loading cases: " + e.getMessage(), e);
            }
        });
    }
    
    private void updateStatistics() {
        // Count cases by status
        int totalCount = allCases.size();
        int assignedCount = 0;
        int ongoingCount = 0;
        int resolvedCount = 0;
        
        for (BlotterReport report : allCases) {
            String status = report.getStatus();
            if (status != null) {
                if ("Assigned".equals(status)) {
                    assignedCount++;
                } else if ("Ongoing".equals(status) || "In Progress".equals(status)) {
                    ongoingCount++;
                } else if ("Resolved".equals(status) || "Closed".equals(status)) {
                    resolvedCount++;
                }
            }
        }
        
        // Update statistics display (if you have TextViews for these)
        android.util.Log.d("OfficerMyCases", "Statistics - Total: " + totalCount + 
            ", Assigned: " + assignedCount + ", Ongoing: " + ongoingCount + ", Resolved: " + resolvedCount);
    }
    
    
    private void updateFilterCounts() {
        // Count cases by REPORT STATUS (Assigned/Ongoing/Resolved) - NOT incident type
        // This shows the TOTAL count for each status
        int totalCount = allCases.size();
        int assignedCount = 0;
        int ongoingCount = 0;
        int resolvedCount = 0;
        
        // Count ALL cases by their REPORT STATUS
        for (BlotterReport report : allCases) {
            String status = report.getStatus();
            android.util.Log.d("OfficerMyCases", "Case #" + report.getId() + " - Report Status: '" + status + "'");
            
            if (status != null) {
                status = status.toLowerCase().trim();
                
                // Count by REPORT STATUS ONLY (not incident type)
                if ("assigned".equals(status) || "pending".equals(status)) {
                    assignedCount++;
                    android.util.Log.d("OfficerMyCases", "  → Counted as ASSIGNED");
                } 
                else if ("ongoing".equals(status) || "in progress".equals(status) || "in_progress".equals(status)) {
                    ongoingCount++;
                    android.util.Log.d("OfficerMyCases", "  → Counted as ONGOING");
                } 
                else if ("resolved".equals(status) || "closed".equals(status) || "completed".equals(status)) {
                    resolvedCount++;
                    android.util.Log.d("OfficerMyCases", "  → Counted as RESOLVED");
                } 
                else {
                    // Unknown status - don't count it
                    android.util.Log.d("OfficerMyCases", "  → Unknown status '" + status + "', not counted");
                }
            } else {
                // If status is null, count as ASSIGNED (newly assigned)
                assignedCount++;
                android.util.Log.d("OfficerMyCases", "  → Null status, counted as ASSIGNED");
            }
        }
        
        android.util.Log.d("OfficerMyCases", "Filter counts - Total: " + totalCount + 
            ", Assigned: " + assignedCount + ", Ongoing: " + ongoingCount + ", Resolved: " + resolvedCount);
        
        // Update statistics display with TOTAL counts from database
        if (tvTotalCount != null) tvTotalCount.setText(String.valueOf(totalCount));
        if (tvPendingCount != null) tvPendingCount.setText(String.valueOf(assignedCount));
        if (tvOngoingCount != null) tvOngoingCount.setText(String.valueOf(ongoingCount));
        if (tvResolvedCount != null) tvResolvedCount.setText(String.valueOf(resolvedCount));
        
        // ALL CHIPS ALWAYS VISIBLE - No hiding logic
        // Users should be able to see all filter options even if count is 0
        
        android.util.Log.d("OfficerMyCases", "Filter counts updated - Total: " + totalCount + 
            ", Assigned: " + assignedCount + ", Ongoing: " + ongoingCount + ", Resolved: " + resolvedCount);
    }
    
    private void filterCases(String query) {
        if (query.isEmpty()) {
            applyFilter();
            return;
        }
        
        // Dynamically determine selected filter by checking which chip is checked (WORKING PATTERN)
        String selectedFilter = "All";
        if (chipPending != null && chipPending.isChecked()) selectedFilter = "Assigned";
        else if (chipOngoing != null && chipOngoing.isChecked()) selectedFilter = "Ongoing";
        else if (chipResolved != null && chipResolved.isChecked()) selectedFilter = "Resolved";
        
        List<BlotterReport> filtered = new ArrayList<>();
        
        for (BlotterReport report : allCases) {
            String status = report.getStatus() != null ? report.getStatus().toUpperCase() : "";
            boolean matchesFilter = false;
            
            // Apply status filter first
            if ("All".equals(selectedFilter)) {
                matchesFilter = true;
            } else if ("Assigned".equals(selectedFilter) && "ASSIGNED".equals(status)) {
                matchesFilter = true;
            } else if ("Ongoing".equals(selectedFilter) && 
                       ("ONGOING".equals(status) || "IN PROGRESS".equals(status))) {
                matchesFilter = true;
            } else if ("Resolved".equals(selectedFilter) && 
                       ("RESOLVED".equals(status) || "CLOSED".equals(status))) {
                matchesFilter = true;
            }
            
            // Then apply search query
            boolean matchesSearch = (report.getCaseNumber() != null && report.getCaseNumber().toLowerCase().contains(query.toLowerCase())) ||
                                   (report.getIncidentType() != null && report.getIncidentType().toLowerCase().contains(query.toLowerCase())) ||
                                   (report.getNarrative() != null && report.getNarrative().toLowerCase().contains(query.toLowerCase()));
            
            if (matchesFilter && matchesSearch) {
                filtered.add(report);
            }
        }
        
        // Update UI - Container card ALWAYS visible
        if (emptyStateCard != null) emptyStateCard.setVisibility(View.VISIBLE);
        
        if (filtered.isEmpty()) {
            if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
            if (recyclerView != null) recyclerView.setVisibility(View.GONE);
            if (tvTotalCases != null) tvTotalCases.setText("0");
        } else {
            if (emptyState != null) emptyState.setVisibility(View.GONE);
            if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
            if (adapter != null) adapter.updateReports(filtered);
            if (tvTotalCases != null) tvTotalCases.setText(String.valueOf(filtered.size()));
        }
    }
    
    private void applyFilter() {
        List<BlotterReport> filtered = new ArrayList<>();
        
        // Dynamically determine selected filter by checking which chip is checked (WORKING PATTERN)
        String selectedFilter = "All";
        if (chipPending != null && chipPending.isChecked()) selectedFilter = "Assigned";
        else if (chipOngoing != null && chipOngoing.isChecked()) selectedFilter = "Ongoing";
        else if (chipResolved != null && chipResolved.isChecked()) selectedFilter = "Resolved";
        
        android.util.Log.d("OfficerMyCases", "Applying filter: " + selectedFilter);
        android.util.Log.d("OfficerMyCases", "Total cases to filter: " + allCases.size());
        
        for (BlotterReport report : allCases) {
            String status = report.getStatus() != null ? report.getStatus().toLowerCase() : "";
            boolean matchesFilter = false;
            
            android.util.Log.d("OfficerMyCases", "Case " + report.getCaseNumber() + " has status: '" + status + "'");
            
            if ("All".equals(selectedFilter)) {
                matchesFilter = true;
            } else if ("Assigned".equals(selectedFilter)) {
                // Show "pending", "assigned", or empty status cases
                if (status.isEmpty() || status.contains("pending") || status.contains("assigned")) {
                    matchesFilter = true;
                    android.util.Log.d("OfficerMyCases", "Added assigned case: " + report.getCaseNumber());
                }
            } else if ("Ongoing".equals(selectedFilter)) {
                // Show "ongoing" or "in progress" status cases
                if (status.contains("ongoing") || status.contains("in progress")) {
                    matchesFilter = true;
                    android.util.Log.d("OfficerMyCases", "Added ongoing case: " + report.getCaseNumber());
                }
            } else if ("Resolved".equals(selectedFilter)) {
                // Show "resolved" or "closed" status cases
                if (status.contains("resolved") || status.contains("closed")) {
                    matchesFilter = true;
                    android.util.Log.d("OfficerMyCases", "Added resolved case: " + report.getCaseNumber());
                }
            }
            
            if (matchesFilter) {
                filtered.add(report);
            }
        }
        
        android.util.Log.d("OfficerMyCases", "Filtered cases count: " + filtered.size());
        
        // Apply sorting
        applySorting(filtered);
        
        // Update UI - Container card ALWAYS visible, only toggle empty state vs recycler
        if (filtered.isEmpty()) {
            updateEmptyState(filtered);
            if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
            if (recyclerView != null) recyclerView.setVisibility(View.GONE);
        } else {
            if (emptyState != null) emptyState.setVisibility(View.GONE);
            if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
            if (adapter != null) adapter.updateReports(filtered);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to this screen
        loadMyCases();
        android.util.Log.d("OfficerMyCases", "onResume: Refreshing data");
    }
    
    // Add method to refresh data when status changes
    public void refreshData() {
        loadMyCases();
    }
    
    // Add periodic refresh for real-time updates
    private void startPeriodicRefresh() {
        android.os.Handler handler = new android.os.Handler();
        Runnable refreshRunnable = new Runnable() {
            @Override
            public void run() {
                // Silent background refresh - NO progress bar shown
                // Just quietly update data in background
                Executors.newSingleThreadExecutor().execute(() -> {
                    try {
                        int userId = preferencesManager.getUserId();
                        com.example.blottermanagementsystem.data.entity.Officer officer = database.officerDao().getOfficerByUserId(userId);
                        int officerId = (officer != null) ? officer.getId() : -1;
                        
                        List<BlotterReport> reports = database.blotterReportDao().getAllReports();
                        
                        // Filter only officer's assigned cases
                        List<BlotterReport> updatedCases = new ArrayList<>();
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
                                updatedCases.add(report);
                            }
                        }
                        
                        // Update data silently
                        runOnUiThread(() -> {
                            allCases.clear();
                            allCases.addAll(updatedCases);
                            updateFilterCounts();
                            applyFilter();
                        });
                    } catch (Exception e) {
                        android.util.Log.e("OfficerMyCases", "Error in background refresh: " + e.getMessage());
                    }
                });
                
                handler.postDelayed(this, 15000); // Refresh every 15 seconds
            }
        };
        handler.postDelayed(refreshRunnable, 15000);
    }
    
    private void navigateToChipActivity(String chipText) {
        // Stay on current activity - just apply filter
        // No navigation needed, filtering is handled by chip listeners
    }
    
    private void showSortMenu() {
        PopupMenu popupMenu = new PopupMenu(this, btnSort);
        popupMenu.getMenuInflater().inflate(R.menu.menu_sort, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.sort_newest) {
                currentSort = "Newest First";
                applyFilter();
                return true;
            } else if (itemId == R.id.sort_oldest) {
                currentSort = "Oldest First";
                applyFilter();
                return true;
            } else if (itemId == R.id.sort_case_number) {
                currentSort = "Case # A-Z";
                applyFilter();
                return true;
            }
            return false;
        });
        popupMenu.show();
    }
    
    private void updateSearchSuggestions(String query) {
        // Build suggestions from all cases
        Set<String> suggestions = new HashSet<>();
        
        if (!query.isEmpty()) {
            for (BlotterReport report : allCases) {
                // Add case numbers that match
                if (report.getCaseNumber() != null && 
                    report.getCaseNumber().toLowerCase().contains(query.toLowerCase())) {
                    suggestions.add(report.getCaseNumber());
                }
                // Add incident types that match
                if (report.getIncidentType() != null && 
                    report.getIncidentType().toLowerCase().contains(query.toLowerCase())) {
                    suggestions.add(report.getIncidentType());
                }
            }
            
            android.util.Log.d("OfficerMyCases", "Search suggestions: " + suggestions.size() + " found for query: " + query);
        }
    }
    
    private void updateEmptyState(List<BlotterReport> displayedCases) {
        if (displayedCases.isEmpty() && emptyStateTitle != null && emptyStateMessage != null && emptyStateIcon != null) {
            switch (currentFilter) {
                case "Assigned":
                    emptyStateTitle.setText("No Assigned Cases");
                    emptyStateMessage.setText("You don't have any newly assigned cases.\nCheck back soon for new assignments.");
                    emptyStateIcon.setImageResource(R.drawable.ic_folder);
                    break;
                case "Ongoing":
                    emptyStateTitle.setText("No Ongoing Cases");
                    emptyStateMessage.setText("No cases are currently in progress.\nStart investigating an assigned case to see it here.");
                    emptyStateIcon.setImageResource(R.drawable.ic_cases);
                    break;
                case "Resolved":
                    emptyStateTitle.setText("No Resolved Cases");
                    emptyStateMessage.setText("You haven't completed any cases yet.\nComplete an investigation to see it here.");
                    emptyStateIcon.setImageResource(R.drawable.ic_check_circle);
                    break;
                default:
                    emptyStateTitle.setText("No Cases Found");
                    emptyStateMessage.setText("No cases are currently assigned to you.\nWait for new case assignments.");
                    emptyStateIcon.setImageResource(R.drawable.ic_clipboard);
                    break;
            }
        }
    }
    
    private void applySorting(List<BlotterReport> cases) {
        if ("Newest First".equals(currentSort)) {
            cases.sort((a, b) -> Long.compare(b.getDateFiled(), a.getDateFiled()));
        } else if ("Oldest First".equals(currentSort)) {
            cases.sort((a, b) -> Long.compare(a.getDateFiled(), b.getDateFiled()));
        } else if ("Case # A-Z".equals(currentSort)) {
            cases.sort((a, b) -> {
                String caseA = a.getCaseNumber() != null ? a.getCaseNumber() : "";
                String caseB = b.getCaseNumber() != null ? b.getCaseNumber() : "";
                return caseA.compareTo(caseB);
            });
        }
    }
}
