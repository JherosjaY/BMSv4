package com.example.blottermanagementsystem.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.database.BlotterDatabase;
import com.example.blottermanagementsystem.data.entity.BlotterReport;
import com.example.blottermanagementsystem.ui.adapters.ReportAdapter;
import com.example.blottermanagementsystem.data.api.ApiClient;
import com.example.blottermanagementsystem.utils.EmptyStateBuilder;
import com.example.blottermanagementsystem.utils.NetworkMonitor;
import com.example.blottermanagementsystem.utils.PreferencesManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

public class AdminReportOversightActivity extends BaseActivity {
    
    private RecyclerView recyclerReports;
    private ReportAdapter adapter;
    private CardView emptyStateCard;
    private EmptyStateBuilder emptyStateBuilder;
    private EditText etSearch;
    private ImageButton btnFilter;
    private List<BlotterReport> allReports = new ArrayList<>();
    private List<BlotterReport> filteredReports = new ArrayList<>();
    private PreferencesManager preferencesManager;
    private String searchQuery = "";
    private String currentStatusFilter = "ALL";
    private String currentSort = "Newest First";
    private BlotterDatabase database;
    private CardView emptyStateCardOverlay;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("AdminOverview", "onCreate() called");
        setContentView(R.layout.activity_admin_report_oversight);
        
        try {
            android.util.Log.d("AdminOverview", "Initializing database and preferences...");
            database = BlotterDatabase.getDatabase(this);
            preferencesManager = new PreferencesManager(this);
            
            android.util.Log.d("AdminOverview", "Initializing views...");
            initializeViews();
            android.util.Log.d("AdminOverview", "Setting up toolbar...");
            setupToolbar();
            android.util.Log.d("AdminOverview", "Setting up listeners...");
            setupListeners();
            android.util.Log.d("AdminOverview", "Loading reports...");
            loadReports();
            android.util.Log.d("AdminOverview", "Starting periodic refresh...");
            startPeriodicRefresh();
            android.util.Log.d("AdminOverview", "onCreate() completed successfully");
        } catch (Exception e) {
            android.util.Log.e("AdminOverview", "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading reports", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void initializeViews() {
        recyclerReports = findViewById(R.id.recyclerReports);
        emptyStateCardOverlay = findViewById(R.id.emptyStateCardOverlay);
        etSearch = findViewById(R.id.etSearch);
        btnFilter = findViewById(R.id.btnFilter);
        
        // Setup RecyclerView
        if (recyclerReports != null) {
            android.util.Log.d("AdminOverview", "RecyclerView found, creating adapter...");
            adapter = new ReportAdapter(filteredReports, report -> {
                Intent intent = new Intent(this, AdminCaseDetailActivity.class);
                intent.putExtra("REPORT_ID", report.getId());
                startActivity(intent);
            });
            android.util.Log.d("AdminOverview", "Adapter created: " + (adapter != null ? "NOT NULL" : "NULL"));
            recyclerReports.setLayoutManager(new LinearLayoutManager(this));
            android.util.Log.d("AdminOverview", "LayoutManager set");
            recyclerReports.setAdapter(adapter);
            android.util.Log.d("AdminOverview", "Adapter set to RecyclerView");
        } else {
            android.util.Log.e("AdminOverview", "ERROR: recyclerReports is NULL!");
        }
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Report Oversight");
            }
            toolbar.setNavigationOnClickListener(v -> goToAdminDashboard());
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        goToAdminDashboard();
        return true;
    }
    
    private void goToAdminDashboard() {
        Intent intent = new Intent(this, AdminDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
    
    private void setupListeners() {
        // Search functionality with suggestions
        if (etSearch != null) {
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
        
        // Filter dropdown menu
        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> showFilterMenu());
        }
    }
    
    private void showFilterMenu() {
        // Create custom dialog with XML layout
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_filter_status, null);
        builder.setView(dialogView);
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        
        // Set dialog background to transparent for rounded corners
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        // Setup click listeners for each card
        dialogView.findViewById(R.id.cardAll).setOnClickListener(v -> {
            currentStatusFilter = "ALL";
            android.util.Log.d("AdminOverview", "Selected filter: ALL");
            filterReports();
            dialog.dismiss();
        });
        
        dialogView.findViewById(R.id.cardPending).setOnClickListener(v -> {
            currentStatusFilter = "PENDING";
            android.util.Log.d("AdminOverview", "Selected filter: PENDING");
            filterReports();
            dialog.dismiss();
        });
        
        dialogView.findViewById(R.id.cardAssigned).setOnClickListener(v -> {
            currentStatusFilter = "ASSIGNED";
            android.util.Log.d("AdminOverview", "Selected filter: ASSIGNED");
            filterReports();
            dialog.dismiss();
        });
        
        dialogView.findViewById(R.id.cardOngoing).setOnClickListener(v -> {
            currentStatusFilter = "ONGOING";
            android.util.Log.d("AdminOverview", "Selected filter: ONGOING");
            filterReports();
            dialog.dismiss();
        });
        
        dialogView.findViewById(R.id.cardResolved).setOnClickListener(v -> {
            currentStatusFilter = "RESOLVED";
            android.util.Log.d("AdminOverview", "Selected filter: RESOLVED");
            filterReports();
            dialog.dismiss();
        });
        
        dialogView.findViewById(R.id.cardClosed).setOnClickListener(v -> {
            currentStatusFilter = "CLOSED";
            android.util.Log.d("AdminOverview", "Selected filter: CLOSED");
            filterReports();
            dialog.dismiss();
        });
        
        dialog.show();
    }
    
    private void filterReports() {
        filteredReports.clear();
        
        android.util.Log.d("AdminOverview", "Total reports: " + allReports.size() + ", Filter: " + currentStatusFilter);
        
        for (BlotterReport report : allReports) {
            // Filter by status
            String status = report.getStatus() != null ? report.getStatus().toUpperCase().trim() : "";
            android.util.Log.d("AdminOverview", "Report status: " + status + " vs filter: " + currentStatusFilter);
            
            if (!currentStatusFilter.equals("ALL") && !status.equals(currentStatusFilter)) {
                continue;
            }
            
            // Filter by search query
            if (!searchQuery.isEmpty()) {
                String caseNumber = report.getCaseNumber() != null ? report.getCaseNumber().toLowerCase() : "";
                String incidentType = report.getIncidentType() != null ? report.getIncidentType().toLowerCase() : "";
                String complainant = report.getComplainantName() != null ? report.getComplainantName().toLowerCase() : "";
                
                if (!caseNumber.contains(searchQuery) && !incidentType.contains(searchQuery) && !complainant.contains(searchQuery)) {
                    continue;
                }
            }
            
            filteredReports.add(report);
        }
        
        android.util.Log.d("AdminOverview", "Filtered reports: " + filteredReports.size());
        
        sortReports();
        
        if (adapter != null) {
            android.util.Log.d("AdminOverview", "Notifying adapter of " + filteredReports.size() + " items");
            adapter.notifyDataSetChanged();
        } else {
            android.util.Log.e("AdminOverview", "ERROR: adapter is NULL!");
        }
        
        updateEmptyState();
    }
    
    private void sortReports() {
        switch (currentSort) {
            case "Newest First":
                Collections.sort(filteredReports, (r1, r2) -> Long.compare(r2.getDateFiled(), r1.getDateFiled()));
                break;
            case "Oldest First":
                Collections.sort(filteredReports, (r1, r2) -> Long.compare(r1.getDateFiled(), r2.getDateFiled()));
                break;
        }
    }
    
    private void updateEmptyState() {
        android.util.Log.d("AdminOverview", "updateEmptyState() - isEmpty: " + filteredReports.isEmpty() + ", size: " + filteredReports.size());
        
        if (filteredReports.isEmpty()) {
            android.util.Log.d("AdminOverview", "Showing empty state overlay for filter: " + currentStatusFilter);
            if (emptyStateCardOverlay != null) {
                emptyStateCardOverlay.setVisibility(View.VISIBLE);
                updateEmptyStateContent(currentStatusFilter);
                android.util.Log.d("AdminOverview", "Empty state overlay visible");
            }
        } else {
            android.util.Log.d("AdminOverview", "Showing data - " + filteredReports.size() + " items");
            if (emptyStateCardOverlay != null) {
                emptyStateCardOverlay.setVisibility(View.GONE);
                android.util.Log.d("AdminOverview", "Empty state overlay hidden");
            }
        }
    }
    
    private void updateEmptyStateContent(String filterType) {
        android.util.Log.d("AdminOverview", "Updating empty state content for: " + filterType);
        
        TextView titleView = emptyStateCardOverlay.findViewById(R.id.emptyStateTitle);
        TextView messageView = emptyStateCardOverlay.findViewById(R.id.emptyStateMessage);
        ImageView iconView = emptyStateCardOverlay.findViewById(R.id.emptyStateIcon);
        
        String title = "No Reports Found";
        String message = "Try adjusting your filters\nor search criteria.";
        int icon = R.drawable.ic_all_reports_modern;
        
        switch (filterType.toUpperCase()) {
            case "ALL":
                title = "No Reports Found";
                message = "No reports available at this time.";
                icon = R.drawable.ic_all_reports_modern;
                break;
            case "PENDING":
                title = "No Pending Reports";
                message = "All reports have been assigned or resolved.";
                icon = R.drawable.ic_all_reports_modern;
                break;
            case "ASSIGNED":
                title = "No Assigned Reports";
                message = "No reports are currently assigned to officers.";
                icon = R.drawable.ic_all_reports_modern;
                break;
            case "ONGOING":
                title = "No Ongoing Reports";
                message = "No investigations are currently in progress.";
                icon = R.drawable.ic_all_reports_modern;
                break;
            case "RESOLVED":
                title = "No Resolved Reports";
                message = "No cases have been resolved yet.";
                icon = R.drawable.ic_all_reports_modern;
                break;
            case "CLOSED":
                title = "No Closed Reports";
                message = "No cases have been closed yet.";
                icon = R.drawable.ic_all_reports_modern;
                break;
        }
        
        if (titleView != null) {
            titleView.setText(title);
        }
        if (messageView != null) {
            messageView.setText(message);
        }
        if (iconView != null) {
            iconView.setImageResource(icon);
        }
    }
    
    private void loadReports() {
        android.util.Log.d("AdminOverview", "loadReports() called");
        
        // Load from LOCAL DATABASE FIRST (fast)
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                android.util.Log.d("AdminOverview", "Querying database for all reports...");
                List<BlotterReport> reports = database.blotterReportDao().getAllReports();
                android.util.Log.d("AdminOverview", "Database returned " + (reports != null ? reports.size() : "null") + " reports");
                
                runOnUiThread(() -> {
                    android.util.Log.d("AdminOverview", "Updating UI with " + (reports != null ? reports.size() : 0) + " reports");
                    allReports.clear();
                    if (reports != null) {
                        allReports.addAll(reports);
                    }
                    android.util.Log.d("AdminOverview", "allReports now has " + allReports.size() + " items");
                    filterReports();
                });
            } catch (Exception e) {
                android.util.Log.e("AdminOverview", "Error loading from database: " + e.getMessage(), e);
            }
        });
        
        // Sync with API in background (don't block UI)
        NetworkMonitor networkMonitor = new NetworkMonitor(this);
        if (networkMonitor.isNetworkAvailable()) {
            Executors.newSingleThreadExecutor().execute(() -> {
                ApiClient.getAllReports(new ApiClient.ApiCallback<List<BlotterReport>>() {
                    @Override
                    public void onSuccess(List<BlotterReport> apiReports) {
                        Executors.newSingleThreadExecutor().execute(() -> {
                            try {
                                for (BlotterReport report : apiReports) {
                                    BlotterReport existing = database.blotterReportDao().getReportById(report.getId());
                                    if (existing == null) {
                                        database.blotterReportDao().insertReport(report);
                                    } else {
                                        database.blotterReportDao().updateReport(report);
                                    }
                                }
                                
                                runOnUiThread(() -> {
                                    allReports.clear();
                                    allReports.addAll(apiReports);
                                    filterReports();
                                });
                            } catch (Exception e) {
                                android.util.Log.e("AdminOverview", "Error syncing API data: " + e.getMessage());
                            }
                        });
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        android.util.Log.w("AdminOverview", "API sync error: " + errorMessage);
                    }
                });
            });
        }
    }
    
    private void startPeriodicRefresh() {
        android.os.Handler handler = new android.os.Handler();
        Runnable refreshRunnable = new Runnable() {
            public void run() {
                if (!isFinishing() && !isDestroyed()) {
                    loadReports();
                }
                handler.postDelayed(this, 15000);
            }
        };
        handler.postDelayed(refreshRunnable, 15000);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadReports();
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
