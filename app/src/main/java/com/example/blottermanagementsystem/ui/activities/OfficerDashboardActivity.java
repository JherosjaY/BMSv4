package com.example.blottermanagementsystem.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.database.BlotterDatabase;
import com.example.blottermanagementsystem.data.entity.BlotterReport;
import com.example.blottermanagementsystem.ui.adapters.RecentCaseAdapter;
import com.example.blottermanagementsystem.utils.PreferencesManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class OfficerDashboardActivity extends BaseActivity {

    private TextView tvWelcomeTop, tvTotalCases, tvActiveCases, tvResolvedCases, tvPendingCases, btnProfile;
    private ImageButton btnNotifications;
    private View notificationBadge;
    private CardView cardMyCases, cardHearings, cardExportExcel;
    // ❌ REMOVED: cardExportPdf - PDF export removed for Officer role
    private android.widget.LinearLayout emptyState;
    private androidx.cardview.widget.CardView emptyStateCard;
    private RecyclerView recyclerRecentCases;

    private PreferencesManager preferencesManager;
    private BlotterDatabase database;
    private List<BlotterReport> recentCases = new ArrayList<>();
    private RecentCaseAdapter recentCaseAdapter;
    private long backPressedTime = 0;
    private Toast backToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_officer_dashboard);

        preferencesManager = new PreferencesManager(this);
        database = BlotterDatabase.getDatabase(this);

        initViews();
        setupRecyclerView();
        setupListeners();
        loadData();
    }

    private void initViews() {
        tvWelcomeTop = findViewById(R.id.tvWelcomeTop);
        tvTotalCases = findViewById(R.id.tvTotalCases);
        tvActiveCases = findViewById(R.id.tvActiveCases);
        tvResolvedCases = findViewById(R.id.tvResolvedCases);
        tvPendingCases = findViewById(R.id.tvPendingCases);
        btnNotifications = findViewById(R.id.btnNotifications);
        notificationBadge = findViewById(R.id.notificationBadge);
        btnProfile = findViewById(R.id.btnProfile);
        cardMyCases = findViewById(R.id.cardMyCases);
        cardHearings = findViewById(R.id.cardHearings);
        // ❌ REMOVED: cardExportPdf = findViewById(R.id.cardExportPdf);
        cardExportExcel = findViewById(R.id.cardExportExcel);
        emptyState = findViewById(R.id.emptyState);
        emptyStateCard = findViewById(R.id.emptyStateCard);
        recyclerRecentCases = findViewById(R.id.recyclerRecentCases);

        String firstName = preferencesManager.getFirstName();
        tvWelcomeTop.setText("Welcome, Officer " + firstName + "!");
        
        checkUnreadNotifications();
    }

    private void loadData() {
        com.example.blottermanagementsystem.utils.GlobalLoadingManager.show(this, "Loading dashboard...");

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                int userId = preferencesManager.getUserId();
                
                com.example.blottermanagementsystem.data.entity.Officer officer = database.officerDao().getOfficerByUserId(userId);
                int officerId = (officer != null) ? officer.getId() : userId;
                
                List<BlotterReport> allReports = database.blotterReportDao().getAllReports();

                int total = 0;
                int active = 0;
                int resolved = 0;
                int pending = 0;
                recentCases.clear();

                for (BlotterReport report : allReports) {
                    Integer assignedId = report.getAssignedOfficerId();
                    String status = report.getStatus() != null ? report.getStatus().toLowerCase() : "";
                    
                    // Check if officer is assigned (either single or multiple officers)
                    boolean isAssignedToOfficer = false;
                    
                    // Check single officer assignment
                    if (assignedId != null && assignedId.intValue() == officerId) {
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
                        total++;
                        recentCases.add(report);
                        if ("pending".equals(status) || "assigned".equals(status)) {
                            pending++;
                        } else if ("ongoing".equals(status) || "in progress".equals(status)) {
                            active++;
                        } else if ("resolved".equals(status) || "closed".equals(status)) {
                            resolved++;
                        }
                    }
                }
                
                int finalTotal = total;
                int finalActive = active;
                int finalResolved = resolved;
                int finalPending = pending;

                runOnUiThread(() -> {
                    tvTotalCases.setText(String.valueOf(finalTotal));
                    tvActiveCases.setText(String.valueOf(finalActive));
                    tvResolvedCases.setText(String.valueOf(finalResolved));
                    tvPendingCases.setText(String.valueOf(finalPending));

                    if (recentCaseAdapter != null) {
                        recentCaseAdapter.updateCases(recentCases);
                    }

                    if (emptyStateCard != null) {
                        emptyStateCard.setVisibility(View.VISIBLE);
                    }

                    if (recentCases.isEmpty()) {
                        emptyState.setVisibility(View.VISIBLE);
                        recyclerRecentCases.setVisibility(View.GONE);
                    } else {
                        emptyState.setVisibility(View.GONE);
                        recyclerRecentCases.setVisibility(View.VISIBLE);
                    }

                    com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
                });
            }
        });
    }

    private void setupRecyclerView() {
        recyclerRecentCases.setLayoutManager(new LinearLayoutManager(this));

        recentCaseAdapter = new RecentCaseAdapter(recentCases, report -> {
            Intent intent = new Intent(this, OfficerCaseDetailActivity.class);
            intent.putExtra("REPORT_ID", report.getId());
            startActivity(intent);
        });
        recyclerRecentCases.setAdapter(recentCaseAdapter);
    }

    private void setupListeners() {
        setupStatisticsCardListeners();

        cardMyCases.setOnClickListener(v -> {
            Intent intent = new Intent(this, OfficerViewAssignedReportsActivity_New.class);
            intent.putExtra("SELECTED_CHIP", "ALL");
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        cardHearings.setOnClickListener(v -> {
            Intent intent = new Intent(this, OfficerViewAllHearingsActivity.class);
            startActivity(intent);
        });

        // ❌ REMOVED: cardExportPdf listener - PDF export removed for Officer role

        if (cardExportExcel != null) {
            cardExportExcel.setOnClickListener(v -> {
                exportToExcel();
            });
        }

        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, OfficerProfileActivity.class);
            startActivity(intent);
        });

        btnNotifications.setOnClickListener(v -> {
            Intent intent = new Intent(this, NotificationsActivity.class);
            startActivity(intent);
        });
    }

    private void setupStatisticsCardListeners() {
        try {
            View cardTotalCases = findViewById(R.id.cardTotalCases);
            if (cardTotalCases != null) {
                cardTotalCases.setOnClickListener(v -> {
                    try {
                        Intent intent = new Intent(this, OfficerViewAssignedReportsActivity_New.class);
                        intent.putExtra("SELECTED_CHIP", "ALL");
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    } catch (Exception e) {
                        android.util.Log.e("OfficerDashboard", "Error opening ViewAssignedReports", e);
                    }
                });
            }

            View cardPending = findViewById(R.id.cardPending);
            if (cardPending != null) {
                cardPending.setOnClickListener(v -> {
                    try {
                        Intent intent = new Intent(this, OfficerViewAssignedReportsActivity_New.class);
                        intent.putExtra("SELECTED_CHIP", "ASSIGNED");
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    } catch (Exception e) {
                        android.util.Log.e("OfficerDashboard", "Error opening ViewAssignedReports", e);
                    }
                });
            }

            View cardActive = findViewById(R.id.cardActive);
            if (cardActive != null) {
                cardActive.setOnClickListener(v -> {
                    try {
                        Intent intent = new Intent(this, OfficerViewOngoingReportsActivity_New.class);
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    } catch (Exception e) {
                        android.util.Log.e("OfficerDashboard", "Error opening ViewOngoingReports", e);
                    }
                });
            }

            View cardResolved = findViewById(R.id.cardResolved);
            if (cardResolved != null) {
                cardResolved.setOnClickListener(v -> {
                    try {
                        Intent intent = new Intent(this, OfficerViewResolvedReportsActivity_New.class);
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    } catch (Exception e) {
                        android.util.Log.e("OfficerDashboard", "Error opening ViewResolvedReports", e);
                    }
                });
            }
        } catch (Exception e) {
            android.util.Log.e("OfficerDashboard", "Error setting up statistics cards: " + e.getMessage());
        }
    }

    private void checkUnreadNotifications() {
        try {
            int userId = preferencesManager.getUserId();
            com.example.blottermanagementsystem.data.database.BlotterDatabase db = 
                com.example.blottermanagementsystem.data.database.BlotterDatabase.getDatabase(this);
            List<com.example.blottermanagementsystem.data.entity.Notification> unreadNotifications = 
                db.notificationDao().getUnreadNotificationsForUser(userId);
            
            boolean hasUnread = unreadNotifications != null && !unreadNotifications.isEmpty();
            
            runOnUiThread(() -> {
                if (notificationBadge != null) {
                    notificationBadge.setVisibility(hasUnread ? View.VISIBLE : View.GONE);
                }
            });
        } catch (Exception e) {
            android.util.Log.e("OfficerDashboard", "Error checking notifications: " + e.getMessage());
        }
    }

    // ❌ REMOVED: exportToPDF() - PDF export removed for Officer role

    private void exportToExcel() {
        Toast.makeText(this, "📊 Exporting to Excel...", Toast.LENGTH_SHORT).show();
    }
}
