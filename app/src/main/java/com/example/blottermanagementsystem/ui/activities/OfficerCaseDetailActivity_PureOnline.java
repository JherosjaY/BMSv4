package com.example.blottermanagementsystem.ui.activities;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.entity.BlotterReport;
import com.example.blottermanagementsystem.utils.PreferencesManager;
import com.example.blottermanagementsystem.utils.NetworkMonitor;
import com.example.blottermanagementsystem.utils.ApiClient;
import com.example.blottermanagementsystem.utils.GlobalLoadingManager;
import java.util.ArrayList;
import java.util.List;

/**
 * ✅ PURE ONLINE OFFICER CASE DETAIL ACTIVITY
 * ✅ All case data loaded from API (Neon database)
 * ✅ No local database dependencies
 */
public class OfficerCaseDetailActivity_PureOnline extends BaseActivity {

    private PreferencesManager preferencesManager;
    private NetworkMonitor networkMonitor;
    private int reportId;
    private BlotterReport currentReport;
    
    private TextView tvCaseNumber;
    private TextView tvIncidentType;
    private TextView tvStatus;
    private TextView tvLocation;
    private TextView tvNarrative;
    private TextView tvComplainant;
    private TextView tvAssignedOfficer;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_case_detail);

        preferencesManager = new PreferencesManager(this);
        networkMonitor = new NetworkMonitor(this);

        reportId = getIntent().getIntExtra("reportId", -1);
        if (reportId == -1) {
            Toast.makeText(this, "Invalid report ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupToolbar();
        initViews();
        loadCaseDetails();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Case Details");
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void initViews() {
        tvCaseNumber = findViewById(R.id.tvCaseNumber);
        tvIncidentType = findViewById(R.id.tvIncidentType);
        tvStatus = findViewById(R.id.tvStatus);
        tvLocation = findViewById(R.id.tvLocation);
        tvNarrative = findViewById(R.id.tvNarrative);
        tvComplainant = findViewById(R.id.tvComplainant);
        tvAssignedOfficer = findViewById(R.id.tvAssignedOfficer);
        recyclerView = findViewById(R.id.recyclerView);

        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
        }
    }

    /**
     * ✅ PURE ONLINE: Load case details from API
     */
    private void loadCaseDetails() {
        if (!networkMonitor.isOnline()) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }

        GlobalLoadingManager.show(this, "Loading case details...");

        ApiClient.getReportById(reportId, new ApiClient.ApiCallback<BlotterReport>() {
            @Override
            public void onSuccess(BlotterReport report) {
                if (isFinishing() || isDestroyed()) return;

                currentReport = report;
                runOnUiThread(() -> {
                    GlobalLoadingManager.hide();
                    displayCaseDetails(report);
                });
            }

            @Override
            public void onError(String errorMessage) {
                if (isFinishing() || isDestroyed()) return;

                runOnUiThread(() -> {
                    GlobalLoadingManager.hide();
                    Toast.makeText(OfficerCaseDetailActivity_PureOnline.this, 
                        "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void displayCaseDetails(BlotterReport report) {
        if (tvCaseNumber != null) {
            tvCaseNumber.setText("Case: " + (report.getCaseNumber() != null ? report.getCaseNumber() : "N/A"));
        }
        if (tvIncidentType != null) {
            tvIncidentType.setText("Type: " + (report.getIncidentType() != null ? report.getIncidentType() : "Unknown"));
        }
        if (tvStatus != null) {
            tvStatus.setText("Status: " + (report.getStatus() != null ? report.getStatus() : "Pending"));
        }
        if (tvLocation != null) {
            tvLocation.setText("Location: " + (report.getIncidentLocation() != null ? report.getIncidentLocation() : "N/A"));
        }
        if (tvNarrative != null) {
            tvNarrative.setText(report.getNarrative() != null ? report.getNarrative() : "No narrative");
        }
        if (tvComplainant != null) {
            tvComplainant.setText("Complainant: " + (report.getComplainantName() != null ? report.getComplainantName() : "N/A"));
        }
        if (tvAssignedOfficer != null) {
            tvAssignedOfficer.setText("Assigned: " + (report.getAssignedOfficer() != null ? report.getAssignedOfficer() : "Unassigned"));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentReport == null) {
            loadCaseDetails();
        }
    }
}
