package com.example.blottermanagementsystem.ui.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.entity.BlotterReport;
import com.example.blottermanagementsystem.ui.adapters.RecentCasesAdapter;
import com.example.blottermanagementsystem.utils.PreferencesManager;
import com.example.blottermanagementsystem.utils.NetworkMonitor;
import com.example.blottermanagementsystem.utils.ApiClient;
import com.example.blottermanagementsystem.utils.GlobalLoadingManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ✅ PURE ONLINE OFFICER DASHBOARD ACTIVITY
 * ✅ All cases loaded from API (Neon database)
 * ✅ No local database dependencies
 */
public class OfficerDashboardActivity extends BaseActivity {

    // UI Components
    private RecyclerView recyclerRecentCases;
    private TextView tvTotalCases, tvActiveCases, tvResolvedCases, tvPendingCases;
    private CardView emptyStateCard;
    private View emptyState;

    // Data
    private PreferencesManager preferencesManager;
    private NetworkMonitor networkMonitor;
    private List<BlotterReport> recentCases = new ArrayList<>();
    private RecentCasesAdapter recentCaseAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_officer_dashboard);

        preferencesManager = new PreferencesManager(this);
        networkMonitor = new NetworkMonitor(this);

        setupToolbar();
        initViews();
        loadData();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Officer Dashboard");
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void initViews() {
        recyclerRecentCases = findViewById(R.id.recyclerRecentCases);
        tvTotalCases = findViewById(R.id.tvTotalCases);
        tvActiveCases = findViewById(R.id.tvActiveCases);
        tvResolvedCases = findViewById(R.id.tvResolvedCases);
        tvPendingCases = findViewById(R.id.tvPendingCases);
        emptyStateCard = findViewById(R.id.emptyStateCard);
        emptyState = findViewById(R.id.emptyState);

        if (recyclerRecentCases != null) {
            recyclerRecentCases.setLayoutManager(new LinearLayoutManager(this));
            recentCaseAdapter = new RecentCasesAdapter(recentCases);
            recyclerRecentCases.setAdapter(recentCaseAdapter);
        }
    }

    /**
     * ✅ PURE ONLINE: Load dashboard data from API
     */
    private void loadData() {
        GlobalLoadingManager.show(this, "Loading dashboard...");

        if (!networkMonitor.isOnline()) {
            GlobalLoadingManager.hide();
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }

        loadCasesViaApi();
    }

    /**
     * ✅ PURE ONLINE: Load assigned cases via API
     */
    private void loadCasesViaApi() {
        ApiClient.getAllReports(new ApiClient.ApiCallback<List<BlotterReport>>() {
            @Override
            public void onSuccess(List<BlotterReport> allReports) {
                if (isFinishing() || isDestroyed()) return;

                String userId = preferencesManager.getUserId();

                List<BlotterReport> assignedCases = new ArrayList<>();
                int total = 0, active = 0, resolved = 0, pending = 0;

                for (BlotterReport report : allReports) {
                    Integer assignedId = report.getAssignedOfficerId();
                    String status = report.getStatus() != null ? report.getStatus().toLowerCase() : "";

                    boolean isAssigned = false;
                    if (assignedId != null && assignedId.toString().equals(userId)) {
                        isAssigned = true;
                    }

                    if (!isAssigned && report.getAssignedOfficerIds() != null && !report.getAssignedOfficerIds().isEmpty()) {
                        String[] officerIds = report.getAssignedOfficerIds().split(",");
                        for (String id : officerIds) {
                            if (id.trim().equals(userId)) {
                                isAssigned = true;
                                break;
                            }
                        }
                    }

                    if (isAssigned) {
                        total++;
                        assignedCases.add(report);

                        if ("assigned".equalsIgnoreCase(status) || "pending".equalsIgnoreCase(status)) {
                            pending++;
                        } else if ("ongoing".equalsIgnoreCase(status) || "in progress".equalsIgnoreCase(status) || "investigation".equalsIgnoreCase(status)) {
                            active++;
                        } else if ("resolved".equalsIgnoreCase(status) || "closed".equalsIgnoreCase(status) || "settled".equalsIgnoreCase(status)) {
                            resolved++;
                        }
                    }
                }

                Collections.sort(assignedCases, (r1, r2) -> {
                    int p1 = getStatusPriority(r1.getStatus() != null ? r1.getStatus().toLowerCase() : "");
                    int p2 = getStatusPriority(r2.getStatus() != null ? r2.getStatus().toLowerCase() : "");
                    if (p1 != p2) return Integer.compare(p1, p2);
                    return Long.compare(r2.getDateFiled(), r1.getDateFiled());
                });

                final int finalTotal = total;
                final int finalActive = active;
                final int finalResolved = resolved;
                final int finalPending = pending;

                runOnUiThread(() -> {
                    recentCases.clear();
                    recentCases.addAll(assignedCases);
                    if (recentCaseAdapter != null) {
                        recentCaseAdapter.notifyDataSetChanged();
                    }

                    if (tvTotalCases != null) tvTotalCases.setText(String.valueOf(finalTotal));
                    if (tvActiveCases != null) tvActiveCases.setText(String.valueOf(finalActive));
                    if (tvResolvedCases != null) tvResolvedCases.setText(String.valueOf(finalResolved));
                    if (tvPendingCases != null) tvPendingCases.setText(String.valueOf(finalPending));

                    if (assignedCases.isEmpty()) {
                        if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
                        if (recyclerRecentCases != null) recyclerRecentCases.setVisibility(View.GONE);
                    } else {
                        if (emptyState != null) emptyState.setVisibility(View.GONE);
                        if (recyclerRecentCases != null) recyclerRecentCases.setVisibility(View.VISIBLE);
                    }

                    GlobalLoadingManager.hide();
                });
            }

            @Override
            public void onError(String errorMessage) {
                if (isFinishing() || isDestroyed()) return;

                runOnUiThread(() -> {
                    GlobalLoadingManager.hide();
                    Toast.makeText(OfficerDashboardActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private int getStatusPriority(String status) {
        if ("pending".equalsIgnoreCase(status) || "assigned".equalsIgnoreCase(status)) return 1;
        if ("ongoing".equalsIgnoreCase(status) || "in progress".equalsIgnoreCase(status)) return 2;
        if ("resolved".equalsIgnoreCase(status) || "closed".equalsIgnoreCase(status)) return 3;
        return 4;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }
}
