package com.example.blottermanagementsystem.ui.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class OfficerDashboardActivity extends BaseActivity {

    private TextView tvWelcomeTop, tvTotalCases, tvActiveCases, tvResolvedCases, tvPendingCases, btnProfile;
    private ImageButton btnNotifications;
    private View notificationBadge;
    private CardView cardMyCases, cardHearings, cardExportExcel;
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

                        // ✅ FIXED: Better status counting logic
                        if ("assigned".equals(status)) {
                            pending++; // Assigned cases count as Pending
                        } else if ("pending".equals(status)) {
                            pending++;
                        } else if ("ongoing".equals(status) || "in progress".equals(status) || "investigation".equals(status)) {
                            active++;
                        } else if ("resolved".equals(status) || "closed".equals(status) || "settled".equals(status)) {
                            resolved++;
                        }
                    }
                }

                // ✅ FIXED: IMPROVED SORTING - Newest Assigned ALWAYS on top
                // Priority: Assigned → Ongoing/In Progress → Pending → Resolved → Unknown
                java.util.Collections.sort(recentCases, (report1, report2) -> {
                    String status1 = report1.getStatus() != null ? report1.getStatus().toLowerCase() : "";
                    String status2 = report2.getStatus() != null ? report2.getStatus().toLowerCase() : "";

                    // Get priority for each report (lower number = higher priority)
                    int priority1 = getStatusPriority(status1);
                    int priority2 = getStatusPriority(status2);

                    android.util.Log.d("OfficerDashboard", "📊 Sorting: " +
                            report1.getCaseNumber() + " (" + status1 + ", P" + priority1 + ") vs " +
                            report2.getCaseNumber() + " (" + status2 + ", P" + priority2 + ")");

                    // 1️⃣ First sort by STATUS PRIORITY
                    if (priority1 != priority2) {
                        return Integer.compare(priority1, priority2);
                    }

                    // 2️⃣ If SAME priority, sort by DATE (newer first) for Assigned/Ongoing
                    //    For Resolved/Closed, sort by date (older first, so newest resolved still shown but at bottom)
                    long date1 = report1.getDateFiled();
                    long date2 = report2.getDateFiled();

                    if (priority1 <= 2) { // Assigned or Ongoing (P1-P2)
                        // For active cases: newest first (most recent on top)
                        return Long.compare(date2, date1);
                    } else { // Resolved/Closed (P3+)
                        // For resolved cases: newest first but they'll be at bottom due to priority
                        return Long.compare(date2, date1);
                    }
                });

                // ✅ Log final sorted order for debugging
                android.util.Log.d("OfficerDashboard", "📋 FINAL SORTED ORDER:");
                for (int i = 0; i < recentCases.size(); i++) {
                    BlotterReport r = recentCases.get(i);
                    String status = r.getStatus() != null ? r.getStatus() : "Unknown";
                    String dateStr = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date(r.getDateFiled()));
                    android.util.Log.d("OfficerDashboard",
                            (i+1) + ". " + r.getCaseNumber() + " - " + status +
                                    " (" + dateStr + ") - P" + getStatusPriority(status.toLowerCase()));
                }
                android.util.Log.d("OfficerDashboard", "✅ Cases sorted with NEWEST ASSIGNED ALWAYS ON TOP. Total: " + recentCases.size());

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
                        updateGridColumns();
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
                android.util.Log.e("OfficerDashboard", "❌ Error loading data: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
                    Toast.makeText(this, "Error loading dashboard data", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void setupRecyclerView() {
        androidx.recyclerview.widget.LinearLayoutManager linearLayoutManager =
                new androidx.recyclerview.widget.LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);

        recyclerRecentCases.setLayoutManager(linearLayoutManager);

        recentCaseAdapter = new RecentCaseAdapter(recentCases, report -> {
            Intent intent = new Intent(this, OfficerCaseDetailActivity.class);
            intent.putExtra("reportId", report.getId());
            startActivity(intent);
        });
        recyclerRecentCases.setAdapter(recentCaseAdapter);
        android.util.Log.d("OfficerDashboard", "✅ RecyclerView setup with VERTICAL LinearLayoutManager");
    }

    private void updateGridColumns() {
        android.util.Log.d("OfficerDashboard", "✅ Recent cases loaded: " + recentCases.size() + " items");
    }

    // ✅ IMPROVED: Status Priority for better sorting
    // Lower number = higher priority (appears first)
    private int getStatusPriority(String status) {
        if (status == null || status.isEmpty()) {
            return 99; // Unknown status at very bottom
        }

        String statusLower = status.toLowerCase().trim();
        
        // ✅ Use contains() for flexible matching
        if (statusLower.contains("assigned")) {
            android.util.Log.d("OfficerDashboard", "✅ Status '" + status + "' → Priority 1 (ASSIGNED - TOP)");
            return 1; // 🏆 HIGHEST PRIORITY - NEWEST ASSIGNED CASES ALWAYS ON TOP
        } else if (statusLower.contains("ongoing") || statusLower.contains("in progress") || statusLower.contains("investigation")) {
            android.util.Log.d("OfficerDashboard", "✅ Status '" + status + "' → Priority 2 (ONGOING)");
            return 2; // 🔄 Active cases being worked on
        } else if (statusLower.contains("pending")) {
            android.util.Log.d("OfficerDashboard", "✅ Status '" + status + "' → Priority 3 (PENDING)");
            return 3; // ⏳ Waiting to be started
        } else if (statusLower.contains("resolved") || statusLower.contains("closed") || statusLower.contains("settled")) {
            android.util.Log.d("OfficerDashboard", "✅ Status '" + status + "' → Priority 4 (RESOLVED - BOTTOM)");
            return 4; // ✅ Completed cases (at bottom)
        } else {
            android.util.Log.d("OfficerDashboard", "⚠️ Status '" + status + "' → Priority 5 (UNKNOWN)");
            return 5; // ❓ Unknown/other status
        }
    }

    private void setupListeners() {
        setupStatisticsCardListeners();

        cardMyCases.setOnClickListener(v -> {
            Intent intent = new Intent(this, OfficerMyCasesActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        cardHearings.setOnClickListener(v -> {
            Intent intent = new Intent(this, HearingsActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

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
                        Intent intent = new Intent(this, OfficerMyCasesActivity.class);
                        intent.putExtra("filter_type", "All");
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    } catch (Exception e) {
                        android.util.Log.e("OfficerDashboard", "Error opening OfficerMyCases", e);
                    }
                });
            }

            View cardPending = findViewById(R.id.cardPending);
            if (cardPending != null) {
                cardPending.setOnClickListener(v -> {
                    try {
                        Intent intent = new Intent(this, OfficerMyCasesActivity.class);
                        intent.putExtra("filter_type", "Assigned");
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    } catch (Exception e) {
                        android.util.Log.e("OfficerDashboard", "Error opening OfficerMyCases", e);
                    }
                });
            }

            View cardActive = findViewById(R.id.cardActive);
            if (cardActive != null) {
                cardActive.setOnClickListener(v -> {
                    try {
                        Intent intent = new Intent(this, OfficerMyCasesActivity.class);
                        intent.putExtra("filter_type", "Ongoing");
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    } catch (Exception e) {
                        android.util.Log.e("OfficerDashboard", "Error opening OfficerMyCases", e);
                    }
                });
            }

            View cardResolved = findViewById(R.id.cardResolved);
            if (cardResolved != null) {
                cardResolved.setOnClickListener(v -> {
                    try {
                        Intent intent = new Intent(this, OfficerMyCasesActivity.class);
                        intent.putExtra("filter_type", "Resolved");
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    } catch (Exception e) {
                        android.util.Log.e("OfficerDashboard", "Error opening OfficerMyCases", e);
                    }
                });
            }
        } catch (Exception e) {
            android.util.Log.e("OfficerDashboard", "Error setting up statistics cards: " + e.getMessage());
        }
    }

    private void checkUnreadNotifications() {
        Executors.newSingleThreadExecutor().execute(() -> {
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
                android.util.Log.e("OfficerDashboard", "Error checking notifications: " + e.getMessage(), e);
            }
        });
    }

    private void exportToExcel() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Workbook workbook = new XSSFWorkbook();
                Sheet sheet = workbook.createSheet("Officer Cases");

                Row headerRow = sheet.createRow(0);
                String[] headers = {"Case ID", "Title", "Description", "Status", "Date Created", "Assigned Officer"};

                for (int i = 0; i < headers.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);

                    CellStyle style = workbook.createCellStyle();
                    Font font = workbook.createFont();
                    font.setBold(true);
                    font.setColor(IndexedColors.WHITE.getIndex());
                    style.setFont(font);
                    style.setFillForegroundColor(IndexedColors.BLUE.getIndex());
                    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                    style.setAlignment(HorizontalAlignment.CENTER);
                    cell.setCellStyle(style);
                }

                int rowNum = 1;
                for (BlotterReport report : recentCases) {
                    Row row = sheet.createRow(rowNum++);

                    row.createCell(0).setCellValue(String.valueOf(report.getId()));
                    row.createCell(1).setCellValue(report.getCaseNumber() != null ? report.getCaseNumber() : "");
                    row.createCell(2).setCellValue(report.getNarrative() != null ? report.getNarrative() : "");
                    row.createCell(3).setCellValue(report.getStatus() != null ? report.getStatus() : "");
                    row.createCell(4).setCellValue(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(report.getDateFiled())));
                    row.createCell(5).setCellValue(report.getAssignedOfficerId() != null ? report.getAssignedOfficerId().toString() : "");
                }

                sheet.setColumnWidth(0, 3000);
                sheet.setColumnWidth(1, 5000);
                sheet.setColumnWidth(2, 8000);
                sheet.setColumnWidth(3, 4000);
                sheet.setColumnWidth(4, 6000);
                sheet.setColumnWidth(5, 4000);

                File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

                if (!downloadDir.exists()) {
                    downloadDir.mkdirs();
                }

                String fileName = "Officer_Cases_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".xlsx";
                File excelFile = new File(downloadDir, fileName);

                try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                    workbook.write(fos);
                    workbook.close();

                    runOnUiThread(() -> {
                        Toast.makeText(OfficerDashboardActivity.this,
                                "✅ Excel exported to Downloads: " + fileName,
                                Toast.LENGTH_LONG).show();
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(OfficerDashboardActivity.this,
                            "❌ Export failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkUnreadNotifications();
        loadData(); // ✅ RELOAD data when returning to dashboard to show latest assignments
    }

    @Override
    public void onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(android.content.Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                vibrator.vibrate(200);
            }

            if (backToast != null) {
                backToast.cancel();
            }

            finishAffinity();
        } else {
            android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(android.content.Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                vibrator.vibrate(100);
            }

            backPressedTime = System.currentTimeMillis();
            backToast = Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT);
            backToast.show();
        }
    }

    @Override
    public void onUserLeaveHint() {
        backPressedTime = 0;
        super.onUserLeaveHint();
    }
}