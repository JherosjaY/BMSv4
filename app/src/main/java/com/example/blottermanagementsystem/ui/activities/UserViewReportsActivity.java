package com.example.blottermanagementsystem.ui.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
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

/**
 * ✅ PURE ONLINE USER VIEW REPORTS ACTIVITY
 * ✅ All reports loaded from API (Neon database)
 * ✅ No local database dependencies
 */
public class UserViewReportsActivity extends BaseActivity {

    private PreferencesManager preferencesManager;
    private NetworkMonitor networkMonitor;
    private RecyclerView recyclerView;
    private View emptyState;
    private EditText etSearch;
    private ReportAdapter adapter;
    private List<BlotterReport> allReports = new ArrayList<>();
    private List<BlotterReport> filteredReports = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_view_reports);

        preferencesManager = new PreferencesManager(this);
        networkMonitor = new NetworkMonitor(this);

        setupToolbar();
        initViews();
        setupListeners();
        loadReports();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("My Reports");
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        emptyState = findViewById(R.id.emptyState);
        etSearch = findViewById(R.id.etSearch);

        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new ReportAdapter(filteredReports, report -> {
                // Handle report click
                android.content.Intent intent = new android.content.Intent(this, com.example.blottermanagementsystem.ui.activities.ReportDetailActivity.class);
                intent.putExtra("reportId", report.getId());
                startActivity(intent);
            });
            recyclerView.setAdapter(adapter);
        }
    }

    private void setupListeners() {
        if (etSearch != null) {
            etSearch.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterReports();
                }

                @Override
                public void afterTextChanged(android.text.Editable s) {}
            });
        }
    }

    /**
     * ✅ PURE ONLINE: Load reports from API
     */
    private void loadReports() {
        if (!networkMonitor.isOnline()) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            showEmptyState();
            return;
        }

        String userId = preferencesManager.getUserId();

        ApiClient.getAllReports(new ApiClient.ApiCallback<List<BlotterReport>>() {
            @Override
            public void onSuccess(List<BlotterReport> apiReports) {
                if (isFinishing() || isDestroyed()) return;

                List<BlotterReport> userReports = new ArrayList<>();
                for (BlotterReport report : apiReports) {
                    if (report.getFiledById() != null && report.getFiledById().equals(userId)) {
                        userReports.add(report);
                    }
                }

                runOnUiThread(() -> {
                    allReports.clear();
                    allReports.addAll(userReports);
                    filterReports();

                    if (userReports.isEmpty()) {
                        showEmptyState();
                    } else {
                        hideEmptyState();
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                if (isFinishing() || isDestroyed()) return;

                runOnUiThread(() -> {
                    Toast.makeText(UserViewReportsActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                    showEmptyState();
                });
            }
        });
    }

    private void filterReports() {
        filteredReports.clear();
        String searchQuery = etSearch != null ? etSearch.getText().toString().toLowerCase() : "";

        for (BlotterReport report : allReports) {
            boolean matches = searchQuery.isEmpty() ||
                (report.getCaseNumber() != null && report.getCaseNumber().toLowerCase().contains(searchQuery)) ||
                (report.getIncidentType() != null && report.getIncidentType().toLowerCase().contains(searchQuery));

            if (matches) {
                filteredReports.add(report);
            }
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void showEmptyState() {
        if (recyclerView != null) recyclerView.setVisibility(View.GONE);
        if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
    }

    private void hideEmptyState() {
        if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
        if (emptyState != null) emptyState.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadReports();
    }
}
