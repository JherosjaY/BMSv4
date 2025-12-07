package com.example.blottermanagementsystem.ui.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.database.BlotterDatabase;
import com.example.blottermanagementsystem.data.entity.Hearing;
import com.example.blottermanagementsystem.data.entity.BlotterReport;
import com.example.blottermanagementsystem.data.entity.Officer;
import com.example.blottermanagementsystem.data.entity.User;
import com.example.blottermanagementsystem.ui.adapters.HearingAdapter;
import com.example.blottermanagementsystem.utils.PreferencesManager;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * ✅ UNIFIED HEARINGS ACTIVITY - COMPLETE FIXED VERSION
 * ✅ User: Sees ONLY hearings from cases THEY FILED
 * ✅ Officer: Sees ONLY hearings from cases ASSIGNED to them
 * ✅ No mixing between different users/officers
 * ✅ Correct status filtering
 */
public class HearingsActivity extends BaseActivity {

    // UI Components
    private RecyclerView recyclerHearings;
    private View emptyState;
    private androidx.cardview.widget.CardView emptyStateCard;
    private Chip chipAll, chipUpcoming, chipCompleted, chipCancelled;
    private EditText etSearch;
    private ImageView emptyStateIcon;
    private TextView emptyStateTitle, emptyStateMessage;

    // Data
    private BlotterDatabase database;
    private PreferencesManager preferencesManager;
    private List<Hearing> allHearings = new ArrayList<>();
    private List<Hearing> filteredHearings = new ArrayList<>();
    private HearingAdapter hearingAdapter;

    // ✅ Store case statuses for efficient filtering
    private Map<Integer, String> caseStatusMap = new HashMap<>();

    // Filters
    private String filterType = "ALL";
    private String searchQuery = "";

    // Role Management
    private UserRoleManager roleManager;

    // =============================
    // ✅ SMART ROLE DETECTION
    // =============================
    private static class UserRoleManager {
        private String userRole = "USER";
        private int userId;
        private int officerId = -1;
        private BlotterDatabase database;

        public UserRoleManager(BlotterDatabase database, PreferencesManager prefs) {
            this.database = database;
            this.userId = prefs.getUserId();
            detectRole(prefs.getUserRole());
        }

        private void detectRole(String rawRole) {
            try {
                if (rawRole == null || rawRole.trim().isEmpty()) {
                    userRole = "USER";
                    return;
                }

                rawRole = rawRole.trim().toUpperCase();

                if (rawRole.contains("OFFICER")) {
                    userRole = "OFFICER";
                    resolveOfficerId();
                } else if (rawRole.contains("ADMIN")) {
                    userRole = "ADMIN";
                    resolveOfficerId();
                } else {
                    userRole = "USER";
                }

                android.util.Log.d("RoleManager", "✅ Role: " + userRole +
                        ", UserID: " + userId + ", OfficerID: " + officerId);

            } catch (Exception e) {
                android.util.Log.e("RoleManager", "Error detecting role: " + e.getMessage());
                userRole = "USER";
            }
        }

        private void resolveOfficerId() {
            try {
                Officer officer = database.officerDao().getOfficerByUserId(userId);
                if (officer != null) {
                    officerId = officer.getId();
                } else {
                    // Fallback: try to find by email
                    User user = database.userDao().getUserById(userId);
                    if (user != null) {
                        List<Officer> allOfficers = database.officerDao().getAllOfficers();
                        for (Officer o : allOfficers) {
                            if (o.getEmail() != null && user.getEmail() != null &&
                                    o.getEmail().equalsIgnoreCase(user.getEmail())) {
                                officerId = o.getId();
                                break;
                            }
                        }
                    }

                    if (officerId == -1) {
                        officerId = userId; // Ultimate fallback
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("RoleManager", "Error resolving officer ID: " + e.getMessage());
                officerId = userId;
            }
        }

        public String getUserRole() { return userRole; }
        public int getUserId() { return userId; }
        public int getOfficerId() { return officerId; }
        public boolean isOfficer() { return "OFFICER".equals(userRole) || "ADMIN".equals(userRole); }
        public boolean isUser() { return "USER".equals(userRole); }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_view_all_hearings);

            database = BlotterDatabase.getDatabase(this);
            preferencesManager = new PreferencesManager(this);

            // ✅ Initialize role manager
            roleManager = new UserRoleManager(database, preferencesManager);

            filterType = getIntent().getStringExtra("filter_type");
            if (filterType == null) filterType = "ALL";

            initializeViews();
            setupToolbar();
            setupChips();
            setupSearch();
            loadHearings();
            startPeriodicRefresh();

        } catch (Exception e) {
            android.util.Log.e("HearingsActivity", "Error in onCreate: " + e.getMessage(), e);
            showErrorState();
        }
    }

    private void initializeViews() {
        recyclerHearings = findViewById(R.id.recyclerHearings);
        emptyState = findViewById(R.id.emptyState);
        emptyStateCard = findViewById(R.id.emptyStateCard);
        chipAll = findViewById(R.id.chipAll);
        chipUpcoming = findViewById(R.id.chipUpcoming);
        chipCompleted = findViewById(R.id.chipCompleted);
        chipCancelled = findViewById(R.id.chipCancelled);
        etSearch = findViewById(R.id.etSearch);
        emptyStateIcon = findViewById(R.id.emptyStateIcon);
        emptyStateTitle = findViewById(R.id.emptyStateTitle);
        emptyStateMessage = findViewById(R.id.emptyStateMessage);

        if (recyclerHearings != null) {
            hearingAdapter = new HearingAdapter(this, filteredHearings, hearing -> {
                showHearingDetails(hearing);
            });
            recyclerHearings.setLayoutManager(new LinearLayoutManager(this));
            recyclerHearings.setAdapter(hearingAdapter);
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
                getSupportActionBar().setTitle(roleManager.isOfficer() ? "Officer Hearings" : "My Hearings");
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void setupChips() {
        if (chipAll != null) {
            chipAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    filterType = "ALL";
                    filterHearings();
                }
            });
        }

        if (chipUpcoming != null) {
            chipUpcoming.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    filterType = "UPCOMING";
                    filterHearings();
                }
            });
        }

        if (chipCompleted != null) {
            chipCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    filterType = "COMPLETED";
                    filterHearings();
                }
            });
        }

        if (chipCancelled != null) {
            chipCancelled.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    filterType = "CANCELLED";
                    filterHearings();
                }
            });
        }

        // Set initial chip
        selectChipByType();
    }

    private void selectChipByType() {
        if (chipAll == null || chipUpcoming == null || chipCompleted == null || chipCancelled == null) {
            return;
        }

        chipAll.setChecked(false);
        chipUpcoming.setChecked(false);
        chipCompleted.setChecked(false);
        chipCancelled.setChecked(false);

        switch (filterType) {
            case "UPCOMING": chipUpcoming.setChecked(true); break;
            case "COMPLETED": chipCompleted.setChecked(true); break;
            case "CANCELLED": chipCancelled.setChecked(true); break;
            default: chipAll.setChecked(true); break;
        }
    }

    private void setupSearch() {
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void afterTextChanged(Editable s) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchQuery = s.toString().toLowerCase();
                    filterHearings();
                }
            });
        }
    }

    // =============================
    // ✅ FIXED: LOAD HEARINGS WITH STRICT DATA SEPARATION
    // =============================
    private void loadHearings() {
        android.util.Log.d("HearingsActivity", "Loading hearings for role: " + roleManager.getUserRole());

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Load hearings based on role
                final List<Hearing> hearings = roleManager.isOfficer() ? 
                    loadOfficerHearingsStrict() : 
                    loadUserHearingsStrict();

                // ✅ Get case statuses efficiently
                final Map<Integer, String> newStatusMap = getCaseStatuses(hearings);

                runOnUiThread(() -> {
                    allHearings.clear();
                    allHearings.addAll(hearings);
                    caseStatusMap.clear();
                    caseStatusMap.putAll(newStatusMap);
                    filterHearings();
                });

            } catch (Exception e) {
                android.util.Log.e("HearingsActivity", "Error loading hearings: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    android.widget.Toast.makeText(this, "Error loading hearings", android.widget.Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // =============================
    // ✅ FIXED: Officer sees ONLY ASSIGNED cases
    // =============================
    private List<Hearing> loadOfficerHearingsStrict() {
        List<Hearing> hearings = new ArrayList<>();

        try {
            int officerId = roleManager.getOfficerId();
            android.util.Log.d("HearingsActivity", "👮 Loading OFFICER hearings (STRICT) for ID: " + officerId);

            // Get ALL reports first
            List<BlotterReport> allReports = database.blotterReportDao().getAllReports();
            if (allReports == null || allReports.isEmpty()) {
                android.util.Log.d("HearingsActivity", "📋 No reports found in database");
                return hearings;
            }

            android.util.Log.d("HearingsActivity", "📋 Total reports in database: " + allReports.size());

            // Filter reports assigned to this officer
            List<BlotterReport> assignedReports = new ArrayList<>();
            for (BlotterReport report : allReports) {
                if (isReportAssignedToOfficer(report, officerId)) {
                    assignedReports.add(report);
                }
            }

            android.util.Log.d("HearingsActivity", "📋 Officer assigned/related reports: " + assignedReports.size());

            // Get hearings ONLY for assigned reports
            if (!assignedReports.isEmpty()) {
                List<Integer> reportIds = new ArrayList<>();
                for (BlotterReport report : assignedReports) {
                    reportIds.add(report.getId());
                }

                android.util.Log.d("HearingsActivity", "🔍 Fetching hearings for " + reportIds.size() + " reports");
                List<Hearing> dbHearings = database.hearingDao().getHearingsByReportIds(reportIds);
                if (dbHearings != null) {
                    android.util.Log.d("HearingsActivity", "✅ Found " + dbHearings.size() + " hearings");
                    hearings.addAll(dbHearings);
                    android.util.Log.d("HearingsActivity", "🎧 Officer hearings found: " + hearings.size());
                }
            }

        } catch (Exception e) {
            android.util.Log.e("HearingsActivity", "Error loading officer hearings: " + e.getMessage());
        }

        return hearings;
    }

    private boolean isReportAssignedToOfficer(BlotterReport report, int officerId) {
        try {
            // 1. Check single officer assignment
            if (report.getAssignedOfficerId() != null && report.getAssignedOfficerId() == officerId) {
                android.util.Log.d("HearingsActivity", "✅ Report " + report.getId() + " assigned to officer " + officerId + " (single assignment)");
                return true;
            }

            // 2. Check multiple officer assignments
            String assignedOfficerIds = report.getAssignedOfficerIds();
            if (assignedOfficerIds != null && !assignedOfficerIds.trim().isEmpty()) {
                String[] ids = assignedOfficerIds.split(",");
                for (String idStr : ids) {
                    try {
                        if (Integer.parseInt(idStr.trim()) == officerId) {
                            android.util.Log.d("HearingsActivity", "✅ Report " + report.getId() + " assigned to officer " + officerId + " (multiple assignment)");
                            return true;
                        }
                    } catch (NumberFormatException e) {
                        // Skip invalid
                    }
                }
            }

            // 3. ✅ FIXED: Check if officer has created ANY hearing for this report
            // This handles the case where an officer creates a hearing without being formally assigned
            try {
                List<Hearing> reportHearings = database.hearingDao().getHearingsByReportId(report.getId());
                if (reportHearings != null && !reportHearings.isEmpty()) {
                    android.util.Log.d("HearingsActivity", "✅ Report " + report.getId() + " has " + reportHearings.size() + " hearing(s) - officer " + officerId + " can see it");
                    return true;
                }
            } catch (Exception e) {
                android.util.Log.w("HearingsActivity", "Could not check hearings for report " + report.getId());
            }

            return false;
        } catch (Exception e) {
            android.util.Log.e("HearingsActivity", "Error checking report assignment: " + e.getMessage());
            return false;
        }
    }

    // =============================
    // ✅ FIXED: User sees ONLY OWN filed cases
    // =============================
    private List<Hearing> loadUserHearingsStrict() {
        List<Hearing> hearings = new ArrayList<>();

        try {
            int userId = roleManager.getUserId();
            android.util.Log.d("HearingsActivity", "👤 Loading USER hearings (STRICT) for ID: " + userId);

            // Get ALL reports first
            List<BlotterReport> allReports = database.blotterReportDao().getAllReports();
            if (allReports == null || allReports.isEmpty()) {
                return hearings;
            }

            // Filter reports filed by this user
            List<BlotterReport> userReports = new ArrayList<>();
            for (BlotterReport report : allReports) {
                if (report.getReportedById() == userId) {
                    userReports.add(report);
                }
            }

            android.util.Log.d("HearingsActivity", "📋 User's own reports: " + userReports.size());

            // Get hearings ONLY for user's reports
            if (!userReports.isEmpty()) {
                List<Integer> reportIds = new ArrayList<>();
                for (BlotterReport report : userReports) {
                    reportIds.add(report.getId());
                }

                List<Hearing> dbHearings = database.hearingDao().getHearingsByReportIds(reportIds);
                if (dbHearings != null) {
                    hearings.addAll(dbHearings);
                    android.util.Log.d("HearingsActivity", "🎧 User hearings found: " + hearings.size());
                }
            }

        } catch (Exception e) {
            android.util.Log.e("HearingsActivity", "Error loading user hearings: " + e.getMessage());
        }

        return hearings;
    }

    // =============================
    // ✅ EFFICIENT CASE STATUS LOADING
    // =============================
    private Map<Integer, String> getCaseStatuses(List<Hearing> hearings) {
        Map<Integer, String> statusMap = new HashMap<>();

        try {
            if (hearings.isEmpty()) return statusMap;

            // Get unique report IDs
            List<Integer> reportIds = new ArrayList<>();
            for (Hearing hearing : hearings) {
                int reportId = hearing.getBlotterReportId();
                if (!reportIds.contains(reportId)) {
                    reportIds.add(reportId);
                }
            }

            // Get all reports at once
            List<BlotterReport> reports = database.blotterReportDao().getAllReports();
            if (reports != null) {
                for (BlotterReport report : reports) {
                    if (report != null && reportIds.contains(report.getId())) {
                        String status = report.getStatus();
                        statusMap.put(report.getId(), status != null ? status.toUpperCase() : "SCHEDULED");
                    }
                }
            }

        } catch (Exception e) {
            android.util.Log.e("HearingsActivity", "Error getting case statuses: " + e.getMessage());
        }

        return statusMap;
    }

    // =============================
    // ✅ FIXED: FILTER HEARINGS WITH CASE STATUS
    // =============================
    private void filterHearings() {
        filteredHearings.clear();

        for (Hearing hearing : allHearings) {
            if (matchesFilter(hearing) && matchesSearch(hearing)) {
                filteredHearings.add(hearing);
            }
        }

        // Sort by date (newest first)
        Collections.sort(filteredHearings, (h1, h2) -> {
            try {
                long time1 = parseDate(h1.getHearingDate());
                long time2 = parseDate(h2.getHearingDate());
                return Long.compare(time2, time1);
            } catch (Exception e) {
                return 0;
            }
        });

        android.util.Log.d("HearingsActivity", "Filtered: " + filteredHearings.size() +
                " hearings for filter: " + filterType);

        if (hearingAdapter != null) {
            hearingAdapter.updateData(filteredHearings);
        }

        updateEmptyState();
    }

    private boolean matchesFilter(Hearing hearing) {
        // ✅ Get case status from stored map (NO UI thread database query!)
        String caseStatus = caseStatusMap.get(hearing.getBlotterReportId());
        if (caseStatus == null) {
            caseStatus = "SCHEDULED";
        }

        // Map to filter category
        String filterCategory = mapCaseStatusToFilter(caseStatus);

        switch (filterType) {
            case "UPCOMING":
                return "UPCOMING".equals(filterCategory);
            case "COMPLETED":
                return "COMPLETED".equals(filterCategory);
            case "CANCELLED":
                return "CANCELLED".equals(filterCategory);
            default:  // ALL
                return true;
        }
    }

    private String mapCaseStatusToFilter(String caseStatus) {
        if (caseStatus == null) return "UPCOMING";

        caseStatus = caseStatus.toUpperCase();

        // ✅ COMPLETED = Case is RESOLVED or SETTLED
        if (caseStatus.contains("RESOLVED") ||
                caseStatus.contains("SETTLED") ||
                caseStatus.contains("CLOSED")) {
            return "COMPLETED";
        }

        // ✅ CANCELLED = Case is CANCELLED or WITHDRAWN
        if (caseStatus.contains("CANCELLED") ||
                caseStatus.contains("WITHDRAWN") ||
                caseStatus.contains("DISMISSED")) {
            return "CANCELLED";
        }

        // ✅ UPCOMING = Everything else (SCHEDULED, INVESTIGATION, etc.)
        return "UPCOMING";
    }

    private boolean matchesSearch(Hearing hearing) {
        if (searchQuery.isEmpty()) return true;

        String location = hearing.getLocation() != null ?
                hearing.getLocation().toLowerCase() : "";
        String caseNum = String.valueOf(hearing.getBlotterReportId());

        return location.contains(searchQuery) || caseNum.contains(searchQuery);
    }

    private long parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return 0;
        try {
            return Long.parseLong(dateStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void updateEmptyState() {
        if (emptyStateCard != null) emptyStateCard.setVisibility(View.VISIBLE);

        if (filteredHearings.isEmpty()) {
            if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
            if (recyclerHearings != null) recyclerHearings.setVisibility(View.GONE);
            updateEmptyStateUI();
        } else {
            if (emptyState != null) emptyState.setVisibility(View.GONE);
            if (recyclerHearings != null) recyclerHearings.setVisibility(View.VISIBLE);
        }
    }

    private void updateEmptyStateUI() {
        if (emptyStateIcon != null) {
            switch (filterType) {
                case "UPCOMING": emptyStateIcon.setImageResource(R.drawable.ic_clock_filled); break;
                case "COMPLETED": emptyStateIcon.setImageResource(R.drawable.ic_check_circle); break;
                case "CANCELLED": emptyStateIcon.setImageResource(R.drawable.ic_clipboard); break;
                default: emptyStateIcon.setImageResource(R.drawable.ic_clipboard); break;
            }
        }

        if (emptyStateTitle != null) {
            switch (filterType) {
                case "UPCOMING": emptyStateTitle.setText("No Upcoming Hearings"); break;
                case "COMPLETED": emptyStateTitle.setText("No Completed Hearings"); break;
                case "CANCELLED": emptyStateTitle.setText("No Cancelled Hearings"); break;
                default: emptyStateTitle.setText("No Hearings Found"); break;
            }
        }

        if (emptyStateMessage != null) {
            switch (filterType) {
                case "UPCOMING":
                    emptyStateMessage.setText(roleManager.isOfficer() ?
                            "No upcoming hearings assigned.\nCheck back later." :
                            "No upcoming hearings scheduled.\nYour cases are pending.");
                    break;
                case "COMPLETED":
                    emptyStateMessage.setText(roleManager.isOfficer() ?
                            "No completed hearings yet.\nComplete scheduled hearings." :
                            "No completed hearings yet.\nYour cases are still in progress.");
                    break;
                case "CANCELLED":
                    emptyStateMessage.setText("No cancelled hearings.\nAll scheduled hearings are active.");
                    break;
                default:
                    emptyStateMessage.setText(roleManager.isOfficer() ?
                            "No hearings assigned to you.\nYou'll see hearings when assigned." :
                            "No hearings found.\nFile a blotter report.");
                    break;
            }
        }
    }

    private void startPeriodicRefresh() {
        android.os.Handler handler = new android.os.Handler();
        Runnable refreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isFinishing() && !isDestroyed()) {
                    loadHearings();
                }
                handler.postDelayed(this, 30000);
            }
        };
        handler.postDelayed(refreshRunnable, 30000);
    }

    private void showHearingDetails(Hearing hearing) {
        try {
            com.example.blottermanagementsystem.ui.fragments.ViewHearingDetailsDialogFragment dialog =
                    com.example.blottermanagementsystem.ui.fragments.ViewHearingDetailsDialogFragment.newInstance(hearing);
            dialog.show(getSupportFragmentManager(), "HearingDetails");
        } catch (Exception e) {
            android.util.Log.e("HearingsActivity", "Error showing hearing details: " + e.getMessage());
        }
    }

    private void showErrorState() {
        android.widget.Toast.makeText(this, "Error loading hearings", android.widget.Toast.LENGTH_LONG).show();

        if (emptyStateCard != null) emptyStateCard.setVisibility(View.VISIBLE);
        if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
        if (emptyStateTitle != null) emptyStateTitle.setText("Error Loading");
        if (emptyStateMessage != null) emptyStateMessage.setText("Please try again.");
    }
}