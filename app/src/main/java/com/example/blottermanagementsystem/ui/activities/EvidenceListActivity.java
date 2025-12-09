package com.example.blottermanagementsystem.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.entity.BlotterReport;
import com.example.blottermanagementsystem.data.entity.Evidence;
import com.example.blottermanagementsystem.ui.adapters.EvidenceAdapter;
import com.example.blottermanagementsystem.utils.PreferencesManager;
import com.example.blottermanagementsystem.utils.RoleAccessControl;
import com.example.blottermanagementsystem.utils.NetworkMonitor;
import com.example.blottermanagementsystem.utils.ApiClient;
import java.util.List;

public class EvidenceListActivity extends BaseActivity {
    
    private RecyclerView recyclerView;
    private EvidenceAdapter adapter;
    private TextView tvEmpty;
    private FloatingActionButton fabAdd;
    private NetworkMonitor networkMonitor;
    private int reportId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        PreferencesManager preferencesManager = new PreferencesManager(this);
        if (!RoleAccessControl.checkAnyRole(this, new String[]{"Admin", "Officer"}, preferencesManager)) {
            return;
        }
        
        setContentView(R.layout.activity_evidence_list);
        
        networkMonitor = new NetworkMonitor(this);
        reportId = getIntent().getIntExtra("REPORT_ID", -1);
        
        setupToolbar();
        initViews();
        setupListeners();
        loadEvidence();
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Evidence");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }
    
    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        tvEmpty = findViewById(R.id.tvEmpty);
        fabAdd = findViewById(R.id.fabAdd);
        
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new EvidenceAdapter(evidence -> {
            // View evidence details
            Toast.makeText(this, "Evidence: " + evidence.getId(), Toast.LENGTH_SHORT).show();
        });
        recyclerView.setAdapter(adapter);
    }
    
    private void setupListeners() {
        fabAdd.setOnClickListener(v -> {
            // AddEvidenceActivity removed - now using floating dialog
            Toast.makeText(this, "Evidence feature moved to Case Investigation screen", Toast.LENGTH_SHORT).show();
        });
    }
    
    private void loadEvidence() {
        if (!networkMonitor.isOnline()) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            recyclerView.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }
        
        // Load from API (pure online)
        if (reportId != -1) {
            // Get evidence for specific report
            ApiClient.getReportById(reportId, new ApiClient.ApiCallback<BlotterReport>() {
                @Override
                public void onSuccess(BlotterReport result) {
                    if (isFinishing() || isDestroyed()) return;
                    
                    // Extract evidence list from report (assuming it's included in response)
                    // For now, we'll show empty state as evidence is typically managed in ReportDetailActivity
                    runOnUiThread(() -> {
                        recyclerView.setVisibility(View.GONE);
                        tvEmpty.setVisibility(View.VISIBLE);
                        tvEmpty.setText("Evidence management moved to Case Investigation screen");
                    });
                }
                
                @Override
                public void onError(String errorMessage) {
                    if (isFinishing() || isDestroyed()) return;
                    
                    runOnUiThread(() -> {
                        Toast.makeText(EvidenceListActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                        recyclerView.setVisibility(View.GONE);
                        tvEmpty.setVisibility(View.VISIBLE);
                    });
                }
            });
        } else {
            // Show empty state if no report ID
            recyclerView.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText("No report selected");
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadEvidence();
    }
}
