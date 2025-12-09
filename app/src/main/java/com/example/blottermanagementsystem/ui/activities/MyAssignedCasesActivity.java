package com.example.blottermanagementsystem.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.entity.BlotterReport;
import com.example.blottermanagementsystem.ui.adapters.ReportAdapter;
import com.example.blottermanagementsystem.utils.PreferencesManager;
import com.example.blottermanagementsystem.utils.NetworkMonitor;
import com.example.blottermanagementsystem.utils.ApiClient;
import java.util.ArrayList;
import java.util.List;

public class MyAssignedCasesActivity extends BaseActivity {
    
    private RecyclerView recyclerCases;
    private LinearLayout emptyState;
    private PreferencesManager preferencesManager;
    private NetworkMonitor networkMonitor;
    private ReportAdapter adapter;
    private List<BlotterReport> casesList = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assigned_cases);
        
        preferencesManager = new PreferencesManager(this);
        networkMonitor = new NetworkMonitor(this);
        
        setupToolbar();
        initViews();
        setupRecyclerView();
        loadAssignedCases();
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Assigned Cases");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }
    
    private void initViews() {
        recyclerCases = findViewById(R.id.recyclerReports);
        emptyState = findViewById(R.id.emptyState);
    }
    
    private void setupRecyclerView() {
        adapter = new ReportAdapter(casesList, report -> {
            Intent intent = new Intent(this, OfficerCaseDetailActivity.class);
            intent.putExtra("REPORT_ID", report.getId());
            startActivity(intent);
        });
        recyclerCases.setLayoutManager(new LinearLayoutManager(this));
        recyclerCases.setAdapter(adapter);
    }
    
    private void loadAssignedCases() {
        if (!networkMonitor.isOnline()) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            emptyState.setVisibility(android.view.View.VISIBLE);
            recyclerCases.setVisibility(android.view.View.GONE);
            return;
        }
        
        // Load from API (pure online)
        ApiClient.getAllReports(new ApiClient.ApiCallback<List<BlotterReport>>() {
            @Override
            public void onSuccess(List<BlotterReport> allReports) {
                if (isFinishing() || isDestroyed()) return;
                
                // Filter reports assigned to this officer
                String officerIdStr = preferencesManager.getUserId();
                int officerId = 0;
                try {
                    officerId = Integer.parseInt(officerIdStr);
                } catch (NumberFormatException e) {
                    android.util.Log.e("MyAssignedCasesActivity", "Invalid officerId: " + officerIdStr);
                }
                List<BlotterReport> assignedReports = new ArrayList<>();
                
                for (BlotterReport report : allReports) {
                    if (report.getAssignedOfficerId() != null && report.getAssignedOfficerId() == officerId) {
                        assignedReports.add(report);
                    }
                }
                
                runOnUiThread(() -> {
                    casesList.clear();
                    casesList.addAll(assignedReports);
                    adapter.updateReports(casesList);
                    
                    if (assignedReports.isEmpty()) {
                        emptyState.setVisibility(android.view.View.VISIBLE);
                        recyclerCases.setVisibility(android.view.View.GONE);
                    } else {
                        emptyState.setVisibility(android.view.View.GONE);
                        recyclerCases.setVisibility(android.view.View.VISIBLE);
                    }
                });
            }
            
            @Override
            public void onError(String errorMessage) {
                if (isFinishing() || isDestroyed()) return;
                
                runOnUiThread(() -> {
                    Toast.makeText(MyAssignedCasesActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                    emptyState.setVisibility(android.view.View.VISIBLE);
                    recyclerCases.setVisibility(android.view.View.GONE);
                });
            }
        });
    }
    
    // Quiet loading method to prevent black screen flicker
    private void loadAssignedCasesQuietly() {
        // Load from API quietly (no loading dialog)
        ApiClient.getAllReports(new ApiClient.ApiCallback<List<BlotterReport>>() {
            @Override
            public void onSuccess(List<BlotterReport> allReports) {
                if (isFinishing() || isDestroyed()) return;
                
                // Filter reports assigned to this officer
                String officerIdStr = preferencesManager.getUserId();
                int officerId = 0;
                try {
                    officerId = Integer.parseInt(officerIdStr);
                } catch (NumberFormatException e) {
                    android.util.Log.e("MyAssignedCasesActivity", "Invalid officerId: " + officerIdStr);
                }
                List<BlotterReport> assignedReports = new ArrayList<>();
                
                for (BlotterReport report : allReports) {
                    if (report.getAssignedOfficerId() != null && report.getAssignedOfficerId() == officerId) {
                        assignedReports.add(report);
                    }
                }
                
                // Only update if data actually changed
                if (assignedReports.size() != casesList.size()) {
                    runOnUiThread(() -> {
                        casesList.clear();
                        casesList.addAll(assignedReports);
                        if (adapter != null) {
                            adapter.notifyDataSetChanged();
                        }
                        
                        if (assignedReports.isEmpty()) {
                            if (emptyState != null) emptyState.setVisibility(android.view.View.VISIBLE);
                            if (recyclerCases != null) recyclerCases.setVisibility(android.view.View.GONE);
                        } else {
                            if (emptyState != null) emptyState.setVisibility(android.view.View.GONE);
                            if (recyclerCases != null) recyclerCases.setVisibility(android.view.View.VISIBLE);
                        }
                    });
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                android.util.Log.w("MyAssignedCases", "Error in quiet loading: " + errorMessage);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAssignedCasesQuietly(); // Use quiet refresh to prevent black screen flicker
    }
}
