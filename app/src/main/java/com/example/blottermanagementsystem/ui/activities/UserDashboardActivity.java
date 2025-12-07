package com.example.blottermanagementsystem.ui.activities;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.database.BlotterDatabase;
import com.example.blottermanagementsystem.data.entity.BlotterReport;
import com.example.blottermanagementsystem.data.entity.User;
import com.example.blottermanagementsystem.ui.adapters.ReportAdapter;
import com.example.blottermanagementsystem.utils.PreferencesManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.takusemba.spotlight.Spotlight;
import com.takusemba.spotlight.Target;
import com.takusemba.spotlight.shape.Circle;
import com.takusemba.spotlight.shape.RoundedRectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class UserDashboardActivity extends BaseActivity {
    
    private TextView tvWelcome, tvTotalReports, tvPendingReports, tvOngoingReports, tvResolvedReports, tvUserAvatar;
    private FloatingActionButton fabMenu, fabAddReport, fabViewReports, fabViewHearings;
    private TextView tvAddReport, tvViewReports, tvViewHearings;
    private View fabOverlay;
    private ImageButton btnNotifications, btnSettings;
    private android.widget.LinearLayout emptyState;
    private androidx.cardview.widget.CardView emptyStateCard;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;
    private boolean isFabMenuOpen = false;
    
    private PreferencesManager preferencesManager;
    private RecyclerView recyclerReports;
    private ReportAdapter adapter;
    private List<BlotterReport> reportsList = new ArrayList<>();
    private BlotterDatabase database;
    private ImageView ivUserProfile;
    private CardView ivProfilePic;
    private TextView tvNotificationBadge;
    private long backPressedTime = 0;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_dashboard);
        
        preferencesManager = new PreferencesManager(this);
        database = BlotterDatabase.getDatabase(this);
        initViews();
        setupRecyclerView();
        setupListeners();
        loadData();
        
        // Start periodic refresh for real-time dashboard updates
        startPeriodicRefresh();
        
        // Show tutorial for first-time users (per-user, not per-device)
        checkAndShowTutorial();
    }
    
    private void initViews() {
        tvWelcome = findViewById(R.id.tvWelcome);
        tvTotalReports = findViewById(R.id.tvTotalReports);
        tvPendingReports = findViewById(R.id.tvPendingReports);
        tvOngoingReports = findViewById(R.id.tvOngoingReports);
        tvResolvedReports = findViewById(R.id.tvResolvedReports);
        recyclerReports = findViewById(R.id.recyclerReports);
        
        // FAB Menu
        fabMenu = findViewById(R.id.fabMenu);
        fabAddReport = findViewById(R.id.fabAddReport);
        fabViewReports = findViewById(R.id.fabViewReports);
        fabViewHearings = findViewById(R.id.fabViewHearings);
        tvAddReport = findViewById(R.id.tvAddReport);
        tvViewReports = findViewById(R.id.tvViewReports);
        tvViewHearings = findViewById(R.id.tvViewHearings);
        fabOverlay = findViewById(R.id.fabOverlay);
        
        btnNotifications = findViewById(R.id.btnNotifications);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
        ivProfilePic = findViewById(R.id.ivProfilePic);
        ivUserProfile = findViewById(R.id.ivUserProfile);
        emptyState = findViewById(R.id.emptyState);
        emptyStateCard = findViewById(R.id.emptyStateCard);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        
        // Null checks
        if (emptyState == null || emptyStateCard == null || recyclerReports == null) {
            android.util.Log.e("UserDashboard", "Critical views are null!");
            if (emptyStateCard == null) android.util.Log.e("UserDashboard", "emptyStateCard is NULL!");
            if (emptyState == null) android.util.Log.e("UserDashboard", "emptyState is NULL!");
            if (recyclerReports == null) android.util.Log.e("UserDashboard", "recyclerReports is NULL!");
        }
        
        // Setup swipe refresh
        swipeRefresh.setColorSchemeResources(
            R.color.electric_blue,
            R.color.success_green,
            R.color.warning_yellow
        );
        swipeRefresh.setOnRefreshListener(() -> {
            loadData();
        });
        
        // Load user data from database
        loadUserFromDatabase();
    }
    
    private void loadUserFromDatabase() {
        // CRITICAL FIX: Try to get user ID from PreferencesManager first
        int userId = preferencesManager.getUserId();
        android.util.Log.d("UserDashboard", "=== LOADING USER FROM DATABASE ===");
        android.util.Log.d("UserDashboard", "UserID from PreferencesManager: " + userId);
        
        // If PreferencesManager fails, try to get the LAST logged in user from database
        if (userId == -1) {
            android.util.Log.e("UserDashboard", "⚠️ PreferencesManager returned -1, checking database for last user");
            Executors.newSingleThreadExecutor().execute(() -> {
                java.util.List<User> allUsers = database.userDao().getAllUsers();
                android.util.Log.d("UserDashboard", "Total users in database: " + allUsers.size());
                
                // Get the last non-admin user (assuming it's the one who just logged in)
                User lastUser = null;
                for (User u : allUsers) {
                    if (!"admin".equals(u.getRole())) {
                        lastUser = u;
                        android.util.Log.d("UserDashboard", "Found user: ID=" + u.getId() + ", Username=" + u.getUsername());
                    }
                }
                
                if (lastUser != null) {
                    final User currentUser = lastUser;
                    android.util.Log.d("UserDashboard", "✅ Using user from database: " + currentUser.getUsername());
                    
                    // Save to PreferencesManager for future use
                    preferencesManager.setUserId(currentUser.getId());
                    preferencesManager.setLoggedIn(true);
                    preferencesManager.setUserRole(currentUser.getRole());
                    preferencesManager.setFirstName(currentUser.getFirstName());
                    preferencesManager.setLastName(currentUser.getLastName());
                    
                    runOnUiThread(() -> {
                        String firstName = currentUser.getFirstName();
                        if (firstName != null && !firstName.isEmpty()) {
                            tvWelcome.setText("Welcome, " + firstName + "!");
                        } else {
                            tvWelcome.setText("Welcome!");
                        }
                        loadProfilePicture();
                    });
                } else {
                    runOnUiThread(() -> {
                        tvWelcome.setText("Welcome!");
                        Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
                    });
                }
            });
            return;
        }
        
        android.util.Log.d("UserDashboard", "Is logged in? " + preferencesManager.isLoggedIn());
        android.util.Log.d("UserDashboard", "User role: " + preferencesManager.getUserRole());
        android.util.Log.d("UserDashboard", "First name from prefs: " + preferencesManager.getFirstName());
        android.util.Log.d("UserDashboard", "Last name from prefs: " + preferencesManager.getLastName());
        
        Executors.newSingleThreadExecutor().execute(() -> {
            User currentUser = database.userDao().getUserById(userId);
            android.util.Log.d("UserDashboard", "User loaded: " + (currentUser != null ? currentUser.getUsername() : "NULL"));
            
            runOnUiThread(() -> {
                if (currentUser != null) {
                    // Set welcome message with REAL data from database
                    String firstName = currentUser.getFirstName();
                    android.util.Log.d("UserDashboard", "FirstName: " + firstName);
                    if (firstName != null && !firstName.isEmpty()) {
                        tvWelcome.setText("Welcome, " + firstName + "!");
                    } else {
                        tvWelcome.setText("Welcome!");
                    }
                    
                    // Load profile picture
                    loadProfilePicture();
                } else {
                    android.util.Log.e("UserDashboard", "ERROR: User with ID " + userId + " not found in database!");
                    tvWelcome.setText("Welcome!");
                    Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
    
    private void loadProfilePicture() {
        if (ivUserProfile == null) return;
        
        int userId = preferencesManager.getUserId();
        
        // Load from database for real-time sync
        Executors.newSingleThreadExecutor().execute(() -> {
            User user = database.userDao().getUserById(userId);
            String profileImageUri = null;
            
            if (user != null && user.getProfilePhotoUri() != null && !user.getProfilePhotoUri().isEmpty()) {
                profileImageUri = user.getProfilePhotoUri();
            }
            
            final String finalProfileImageUri = profileImageUri;
            
            runOnUiThread(() -> {
                if (finalProfileImageUri != null && !finalProfileImageUri.isEmpty()) {
                    android.util.Log.d("UserDashboard", "Loading profile image: " + finalProfileImageUri);
                    try {
                        // Clear tint before loading image
                        ivUserProfile.setImageTintList(null);
                        
                        Uri imageUri = Uri.parse(finalProfileImageUri);
                        Glide.with(this)
                            .load(imageUri)
                            .apply(RequestOptions.circleCropTransform())
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .into(ivUserProfile);
                        android.util.Log.d("UserDashboard", "✅ Profile image loaded successfully");
                    } catch (Exception e) {
                        android.util.Log.e("UserDashboard", "❌ Error loading profile image: " + e.getMessage());
                        ivUserProfile.setImageResource(R.drawable.ic_person);
                    }
                } else {
                    android.util.Log.d("UserDashboard", "No profile image URI found");
                    ivUserProfile.setImageResource(R.drawable.ic_person);
                }
            });
        });
    }
    
    private void setupRecyclerView() {
        adapter = new ReportAdapter(reportsList, report -> {
            Intent intent = new Intent(this, ReportDetailActivity.class);
            intent.putExtra("REPORT_ID", report.getId());
            startActivity(intent);
        });
        recyclerReports.setLayoutManager(new LinearLayoutManager(this));
        recyclerReports.setAdapter(adapter);
    }
    
    private void setupListeners() {
        // Main FAB menu toggle
        fabMenu.setOnClickListener(v -> toggleFabMenu());
        
        // Overlay click to close menu
        fabOverlay.setOnClickListener(v -> closeFabMenu());
        
        // FAB menu items
        fabAddReport.setOnClickListener(v -> {
            closeFabMenu();
            startActivity(new Intent(this, AddReportActivity.class));
        });
        
        fabViewReports.setOnClickListener(v -> {
            closeFabMenu();
            // Add small delay to prevent black flicker
            v.postDelayed(() -> {
                Intent intent = new Intent(this, UserViewReportsActivity.class);
                intent.putExtra("filter_type", "ALL");
                startActivity(intent);
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            }, 150);
        });
        
        fabViewHearings.setOnClickListener(v -> {
            closeFabMenu();
            // Add small delay to prevent black flicker
            v.postDelayed(() -> {
                Intent intent = new Intent(this, HearingsActivity.class);
                intent.putExtra("filter_type", "ALL");
                startActivity(intent);
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            }, 150);
        });
        
        btnNotifications.setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationsActivity.class));
        });
        
        // Profile picture click -> Go to Profile Screen
        if (ivProfilePic != null) {
            ivProfilePic.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(this, ProfileActivity.class));
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Error opening profile", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        // Also add click listener to the image itself
        if (ivUserProfile != null) {
            ivUserProfile.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(this, ProfileActivity.class));
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Error opening profile", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        // Statistics Cards Click Listeners
        setupStatisticsCardListeners();
        
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                startActivity(new Intent(this, SettingsActivity.class));
            });
        }
    }
    
    private void setupStatisticsCardListeners() {
        try {
            // Total Reports Card -> View Reports (All filter)
            View cardTotalReports = findViewById(R.id.cardTotalReports);
            if (cardTotalReports != null) {
                addCardTouchAnimation(cardTotalReports);
                cardTotalReports.setOnClickListener(v -> {
                    try {
                        // Add small delay to prevent black flicker
                        v.postDelayed(() -> {
                            Intent intent = new Intent(this, UserViewReportsActivity.class);
                            intent.putExtra("filter_type", "ALL");
                            startActivity(intent);
                            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                        }, 100);
                    } catch (Exception e) {
                        android.util.Log.e("UserDashboard", "Error opening UserViewReports: " + e.getMessage());
                        android.widget.Toast.makeText(this, "Error opening reports", android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            // Pending Card -> View Reports (Pending filter)
            View cardPending = findViewById(R.id.cardPending);
            if (cardPending != null) {
                addCardTouchAnimation(cardPending);
                cardPending.setOnClickListener(v -> {
                    try {
                        v.postDelayed(() -> {
                            Intent intent = new Intent(this, UserViewReportsActivity.class);
                            intent.putExtra("filter_type", "PENDING");
                            startActivity(intent);
                            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                        }, 100);
                    } catch (Exception e) {
                        android.util.Log.e("UserDashboard", "Error opening UserViewReports: " + e.getMessage());
                        android.widget.Toast.makeText(this, "Error opening pending reports", android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            // Ongoing Card -> View Reports (Ongoing filter)
            View cardOngoing = findViewById(R.id.cardOngoing);
            if (cardOngoing != null) {
                addCardTouchAnimation(cardOngoing);
                cardOngoing.setOnClickListener(v -> {
                    try {
                        v.postDelayed(() -> {
                            Intent intent = new Intent(this, UserViewReportsActivity.class);
                            intent.putExtra("filter_type", "ONGOING");
                            startActivity(intent);
                            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                        }, 100);
                    } catch (Exception e) {
                        android.util.Log.e("UserDashboard", "Error opening UserViewReports: " + e.getMessage());
                        android.widget.Toast.makeText(this, "Error opening ongoing reports", android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            // Resolved Card -> View Reports (Resolved filter)
            View cardResolved = findViewById(R.id.cardResolved);
            if (cardResolved != null) {
                addCardTouchAnimation(cardResolved);
                cardResolved.setOnClickListener(v -> {
                    try {
                        v.postDelayed(() -> {
                            Intent intent = new Intent(this, UserViewReportsActivity.class);
                            intent.putExtra("filter_type", "RESOLVED");
                            startActivity(intent);
                            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                        }, 100);
                    } catch (Exception e) {
                        android.util.Log.e("UserDashboard", "Error opening UserViewReports: " + e.getMessage());
                        android.widget.Toast.makeText(this, "Error opening resolved reports", android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } catch (Exception e) {
            android.util.Log.e("UserDashboard", "Error setting up statistics card listeners: " + e.getMessage());
        }
    }
    
    private void loadData() {
        int userId = preferencesManager.getUserId();
        
        // Show loading for user dashboard
        com.example.blottermanagementsystem.utils.GlobalLoadingManager.show(this, "Loading reports...");
        
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
            List<BlotterReport> allReports = database.blotterReportDao().getAllReports();
            
            // Filter reports by current user
            List<BlotterReport> userReports = new ArrayList<>();
            for (BlotterReport report : allReports) {
                if (report.getReportedById() == userId) {
                    userReports.add(report);
                }
            }
            
            // Sort by date descending (newest first)
            userReports.sort((r1, r2) -> Long.compare(r2.getIncidentDate(), r1.getIncidentDate()));
            
            // Show all user reports (no limit)
            List<BlotterReport> recentReports = userReports;
            
            // Count reports by status
            int pendingCount = 0;
            int ongoingCount = 0;
            int resolvedCount = 0;
            
            for (BlotterReport report : userReports) {
                String status = report.getStatus();
                if (status != null && ("pending".equalsIgnoreCase(status))) {
                    pendingCount++;
                } else if (status != null && ("ongoing".equalsIgnoreCase(status) || "in-progress".equalsIgnoreCase(status))) {
                    ongoingCount++;
                } else if (status != null && ("resolved".equalsIgnoreCase(status))) {
                    resolvedCount++;
                }
            }
            
            // Get unread notifications count
            int unreadNotifications = database.notificationDao()
                .getUnreadNotificationsForUser(userId).size();
            
            final int totalCount = userReports.size();
            final int finalPendingCount = pendingCount;
            final int finalOngoingCount = ongoingCount;
            final int finalResolvedCount = resolvedCount;
            final int finalUnreadCount = unreadNotifications;
            
            runOnUiThread(() -> {
                reportsList.clear();
                reportsList.addAll(recentReports); // Show all reports
                adapter.notifyDataSetChanged();
                
                // Update counts
                tvTotalReports.setText(String.valueOf(totalCount));
                tvPendingReports.setText(String.valueOf(finalPendingCount));
                tvOngoingReports.setText(String.valueOf(finalOngoingCount));
                tvResolvedReports.setText(String.valueOf(finalResolvedCount));
                
                // Load profile picture
                loadProfilePicture();
                
                // CardView always visible as background
                if (emptyStateCard != null) {
                    emptyStateCard.setVisibility(View.VISIBLE);
                }
                
                if (recentReports.isEmpty()) {
                    // Empty state - show empty message, hide RecyclerView
                    if (emptyState != null) {
                        emptyState.setVisibility(View.VISIBLE);
                    }
                    if (recyclerReports != null) {
                        recyclerReports.setVisibility(View.GONE);
                    }
                } else {
                    // Has data - hide empty message, show RecyclerView on top of CardView
                    if (emptyState != null) {
                        emptyState.setVisibility(View.GONE);
                    }
                    if (recyclerReports != null) {
                        recyclerReports.setVisibility(View.VISIBLE);
                    }
                }
                
                // Stop refresh animation
                swipeRefresh.setRefreshing(false);
                
                // Hide loading
                com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
            });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
                    swipeRefresh.setRefreshing(false);
                });
            }
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Reload user data from database
        loadUserFromDatabase();
        
        // Always refresh data when returning to dashboard to show latest updates
        loadData();
        
        // Update notification badge
        updateNotificationBadge();
    }
    
    // Quiet data loading without GlobalLoadingManager to prevent black screen flicker
    private void loadDataQuietly() {
        int userId = preferencesManager.getUserId();
        
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<BlotterReport> allReports = database.blotterReportDao().getAllReports();
                
                // Filter reports by current user
                List<BlotterReport> userReports = new ArrayList<>();
                for (BlotterReport report : allReports) {
                    if (report.getReportedById() == userId) {
                        userReports.add(report);
                    }
                }
                
                // Count by status
                int totalReports = userReports.size();
                int pendingReports = 0;
                int ongoingReports = 0;
                int resolvedReports = 0;
                
                for (BlotterReport report : userReports) {
                    String status = report.getStatus();
                    if (status != null && "pending".equalsIgnoreCase(status)) {
                        pendingReports++;
                    } else if (status != null && ("ongoing".equalsIgnoreCase(status) || "in-progress".equalsIgnoreCase(status))) {
                        ongoingReports++;
                    } else if (status != null && "resolved".equalsIgnoreCase(status)) {
                        resolvedReports++;
                    }
                }
                
                final int finalTotalReports = totalReports;
                final int finalPendingReports = pendingReports;
                final int finalOngoingReports = ongoingReports;
                final int finalResolvedReports = resolvedReports;
                
                runOnUiThread(() -> {
                    // Update statistics quietly
                    if (tvTotalReports != null) tvTotalReports.setText(String.valueOf(finalTotalReports));
                    if (tvPendingReports != null) tvPendingReports.setText(String.valueOf(finalPendingReports));
                    if (tvOngoingReports != null) tvOngoingReports.setText(String.valueOf(finalOngoingReports));
                    if (tvResolvedReports != null) tvResolvedReports.setText(String.valueOf(finalResolvedReports));
                    
                    // Update recent reports list quietly
                    reportsList.clear();
                    if (userReports.size() > 5) {
                        reportsList.addAll(userReports.subList(0, 5));
                    } else {
                        reportsList.addAll(userReports);
                    }
                    
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                    
                    // Update empty state
                    if (userReports.isEmpty()) {
                        if (emptyStateCard != null) emptyStateCard.setVisibility(View.VISIBLE);
                        if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
                        if (recyclerReports != null) recyclerReports.setVisibility(View.GONE);
                    } else {
                        if (emptyStateCard != null) emptyStateCard.setVisibility(View.VISIBLE);
                        if (emptyState != null) emptyState.setVisibility(View.GONE);
                        if (recyclerReports != null) recyclerReports.setVisibility(View.VISIBLE);
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("UserDashboard", "Error in quiet data loading: " + e.getMessage());
            }
        });
    }

    private void updateNotificationBadge() {
        int userId = preferencesManager.getUserId();
        
        Executors.newSingleThreadExecutor().execute(() -> {
            // Get unread notification count
            int unreadCount = database.notificationDao().getUnreadCount(userId);
            
            runOnUiThread(() -> {
                android.util.Log.d("UserDashboard", "Unread notifications: " + unreadCount);
                
                if (unreadCount > 0) {
                    // Show badge with count
                    tvNotificationBadge.setVisibility(View.VISIBLE);
                    tvNotificationBadge.setText(unreadCount > 9 ? "9+" : String.valueOf(unreadCount));
                } else {
                    // Hide badge
                    tvNotificationBadge.setVisibility(View.GONE);
                }
            });
        });
    }
    
    private void toggleFabMenu() {
        if (isFabMenuOpen) {
            closeFabMenu();
        } else {
            openFabMenu();
        }
    }
    
    private void openFabMenu() {
        isFabMenuOpen = true;
        
        // Show overlay
        fabOverlay.setVisibility(View.VISIBLE);
        fabOverlay.setAlpha(0f);
        fabOverlay.animate().alpha(1f).setDuration(200).start();
        
        // Show menu items with animation
        fabAddReport.setVisibility(View.VISIBLE);
        fabViewReports.setVisibility(View.VISIBLE);
        fabViewHearings.setVisibility(View.VISIBLE);
        tvAddReport.setVisibility(View.VISIBLE);
        tvViewReports.setVisibility(View.VISIBLE);
        tvViewHearings.setVisibility(View.VISIBLE);
        
        // Animate FABs
        fabAddReport.setAlpha(0f);
        fabAddReport.setTranslationY(100f);
        fabAddReport.animate().alpha(1f).translationY(0f).setDuration(200).start();
        
        fabViewReports.setAlpha(0f);
        fabViewReports.setTranslationY(100f);
        fabViewReports.animate().alpha(1f).translationY(0f).setDuration(250).start();
        
        fabViewHearings.setAlpha(0f);
        fabViewHearings.setTranslationY(100f);
        fabViewHearings.animate().alpha(1f).translationY(0f).setDuration(300).start();
        
        // Animate labels
        tvAddReport.setAlpha(0f);
        tvAddReport.animate().alpha(1f).setDuration(200).start();
        
        tvViewReports.setAlpha(0f);
        tvViewReports.animate().alpha(1f).setDuration(250).start();
        
        tvViewHearings.setAlpha(0f);
        tvViewHearings.animate().alpha(1f).setDuration(300).start();
        
        // Rotate main FAB
        fabMenu.animate().rotation(45f).setDuration(200).start();
    }
    
    private void closeFabMenu() {
        isFabMenuOpen = false;
        
        // Hide overlay
        fabOverlay.animate().alpha(0f).setDuration(200).withEndAction(() -> 
            fabOverlay.setVisibility(View.GONE)
        ).start();
        
        // Hide menu items
        fabAddReport.animate().alpha(0f).translationY(100f).setDuration(200).withEndAction(() ->
            fabAddReport.setVisibility(View.GONE)
        ).start();
        
        fabViewReports.animate().alpha(0f).translationY(100f).setDuration(200).withEndAction(() ->
            fabViewReports.setVisibility(View.GONE)
        ).start();
        
        fabViewHearings.animate().alpha(0f).translationY(100f).setDuration(200).withEndAction(() ->
            fabViewHearings.setVisibility(View.GONE)
        ).start();
        
        tvAddReport.animate().alpha(0f).setDuration(200).withEndAction(() ->
            tvAddReport.setVisibility(View.GONE)
        ).start();
        
        tvViewReports.animate().alpha(0f).setDuration(200).withEndAction(() ->
            tvViewReports.setVisibility(View.GONE)
        ).start();
        
        tvViewHearings.animate().alpha(0f).setDuration(200).withEndAction(() ->
            tvViewHearings.setVisibility(View.GONE)
        ).start();
        
        // Rotate main FAB back
        fabMenu.animate().rotation(0f).setDuration(200).start();
    }
    
    @Override
    public void onBackPressed() {
        if (isFabMenuOpen) {
            closeFabMenu();
        } else {
            // Double press to exit with vibration
            if (backPressedTime + 2000 > System.currentTimeMillis()) {
                super.onBackPressed();
                finishAffinity(); // Close all activities
                return;
            } else {
                // Vibrate
                Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null && vibrator.hasVibrator()) {
                    vibrator.vibrate(100); // Vibrate for 100ms
                }
                
                Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();
            }
            backPressedTime = System.currentTimeMillis();
        }
    }
    
    private void animateProgressBar(ProgressBar progressBar, int progress) {
        ObjectAnimator animation = ObjectAnimator.ofInt(progressBar, "progress", 0, progress);
        animation.setDuration(1000);
        animation.setInterpolator(new DecelerateInterpolator());
        animation.start();
    }
    
    private boolean isTutorialInProgress = false;
    private boolean isStepTransitioning = false;
    private Spotlight currentSpotlight = null;
    private int currentStep = 1;
    
    private void showTutorial() {
        if (!isTutorialInProgress) {
            isTutorialInProgress = true;
            currentStep = 1;
            
            // DISABLE all UI elements first
            fabMenu.setEnabled(false);
            fabMenu.setClickable(false);
            fabAddReport.setEnabled(false);
            fabAddReport.setClickable(false);
            fabViewReports.setEnabled(false);
            fabViewReports.setClickable(false);
            fabViewHearings.setEnabled(false);
            fabViewHearings.setClickable(false);
            
            if (btnNotifications != null) {
                btnNotifications.setEnabled(false);
                btnNotifications.setClickable(false);
            }
            if (btnSettings != null) {
                btnSettings.setEnabled(false);
                btnSettings.setClickable(false);
            }
            if (tvUserAvatar != null) {
                tvUserAvatar.setEnabled(false);
                tvUserAvatar.setClickable(false);
            }
            if (recyclerReports != null) {
                recyclerReports.setEnabled(false);
                recyclerReports.setClickable(false);
            }
            
            // Start with Step 1 - FAB button (closed)
            showTutorialStep1();
        }
    }
    
    private void showTutorialStep1() {
        // Step 1: Highlight FAB Menu Button (closed state)
        View fabOverlay = getLayoutInflater().inflate(R.layout.layout_tutorial_overlay, null);
        TextView tvIcon1 = fabOverlay.findViewById(R.id.tvTutorialIcon);
        TextView tvTitle1 = fabOverlay.findViewById(R.id.tvTutorialTitle);
        TextView tvDesc1 = fabOverlay.findViewById(R.id.tvTutorialDescription);
        com.google.android.material.button.MaterialButton btnNext = fabOverlay.findViewById(R.id.btnNext);
        com.google.android.material.button.MaterialButton btnPrevious = fabOverlay.findViewById(R.id.btnPrevious);
        com.google.android.material.button.MaterialButton btnFinish = fabOverlay.findViewById(R.id.btnFinish);
        
        tvIcon1.setText("➕");
        tvTitle1.setText("Quick Actions Menu");
        tvDesc1.setText("Click here to access Add Report, View Reports, and View Hearings options");
        
        // Step 1: No previous button, show Next only
        btnPrevious.setVisibility(View.GONE);
        btnNext.setVisibility(View.VISIBLE);
        btnFinish.setVisibility(View.GONE);
        
        // Add glow effect to FAB menu
        fabMenu.setElevation(24f);
        fabMenu.setTranslationZ(8f);
    
        Target fabTarget = new Target.Builder()
                .setAnchor(fabMenu)
                .setShape(new RoundedRectangle(fabMenu.getHeight(), fabMenu.getWidth(), 50f))
                .setOverlay(fabOverlay)
                .build();
        
        Spotlight spotlight1 = new Spotlight.Builder(this)
                .setTargets(fabTarget)
                .setBackgroundColor(0xCC000000) // 80% black opacity
                .setDuration(1L) // Instant - no animation
                .build();
        
        currentSpotlight = spotlight1;
        
        btnNext.setOnClickListener(v -> {
            if (!isStepTransitioning) {
                isStepTransitioning = true;
                btnNext.setEnabled(false);
                
                fabMenu.setElevation(6f);
                fabMenu.setTranslationZ(0f);
                
                // Open FAB menu
                openFabMenu();
                
                // Hide text labels
                tvAddReport.setVisibility(View.GONE);
                tvViewReports.setVisibility(View.GONE);
                tvViewHearings.setVisibility(View.GONE);
                
                // Wait for FAB animation, then transition to Step 2
                fabMenu.postDelayed(() -> {
                    // Finish and start new spotlight immediately to minimize dim flicker
                    if (currentSpotlight != null) {
                        currentSpotlight.finish();
                        currentSpotlight = null;
                    }
                    // Start Step 2 immediately
                    fabMenu.post(() -> {
                        showTutorialStep2();
                        isStepTransitioning = false;
                    });
                }, 300);
            }
        });
        
        spotlight1.start();
    }
    
    private void showTutorialStep2() {
        // Step 2: Highlight "Add New Report" button
        View addReportOverlay = getLayoutInflater().inflate(R.layout.layout_tutorial_overlay, null);
        TextView tvIcon2 = addReportOverlay.findViewById(R.id.tvTutorialIcon);
        TextView tvTitle2 = addReportOverlay.findViewById(R.id.tvTutorialTitle);
        TextView tvDesc2 = addReportOverlay.findViewById(R.id.tvTutorialDescription);
        com.google.android.material.button.MaterialButton btnNext = addReportOverlay.findViewById(R.id.btnNext);
        com.google.android.material.button.MaterialButton btnPrevious = addReportOverlay.findViewById(R.id.btnPrevious);
        com.google.android.material.button.MaterialButton btnFinish = addReportOverlay.findViewById(R.id.btnFinish);
        
        tvIcon2.setText("📋");
        tvTitle2.setText("Add New Report");
        tvDesc2.setText("Tap here to file a new incident report with photos and videos");
        
        // Step 2: Show both buttons
        btnPrevious.setVisibility(View.VISIBLE);
        btnNext.setVisibility(View.VISIBLE);
        btnFinish.setVisibility(View.GONE);
        
        // Add glow effect
        fabAddReport.setElevation(24f);
        fabAddReport.setTranslationZ(8f);
        
        Target addReportTarget = new Target.Builder()
                .setAnchor(fabAddReport)
                .setShape(new RoundedRectangle(fabAddReport.getHeight(), fabAddReport.getWidth(), 50f))
                .setOverlay(addReportOverlay)
                .build();
        
        Spotlight spotlight2 = new Spotlight.Builder(this)
                .setTargets(addReportTarget)
                .setBackgroundColor(0xCC000000) // 80% black opacity - same as step 1
                .setDuration(1L) // Instant - no animation
                .build();
        
        currentSpotlight = spotlight2;
        
        btnNext.setOnClickListener(v -> {
            if (!isStepTransitioning) {
                isStepTransitioning = true;
                btnNext.setEnabled(false);
                fabAddReport.setElevation(6f);
                fabAddReport.setTranslationZ(0f);
                fabMenu.postDelayed(() -> {
                    if (currentSpotlight != null) {
                        currentSpotlight.finish();
                    }
                    showTutorialStep3();
                    isStepTransitioning = false;
                }, 50);
            }
        });
        
        btnPrevious.setOnClickListener(v -> {
            if (!isStepTransitioning) {
                isStepTransitioning = true;
                btnPrevious.setEnabled(false);
                
                fabAddReport.setElevation(6f);
                fabAddReport.setTranslationZ(0f);
                
                // Close FAB menu
                closeFabMenu();
                
                // Wait for FAB close animation, then transition to Step 1
                fabMenu.postDelayed(() -> {
                    // Finish and start new spotlight immediately to minimize dim flicker
                    if (currentSpotlight != null) {
                        currentSpotlight.finish();
                        currentSpotlight = null;
                    }
                    // Start Step 1 immediately
                    fabMenu.post(() -> {
                        showTutorialStep1();
                        isStepTransitioning = false;
                    });
                }, 300);
            }
        });
        
        spotlight2.start();
    }
    
    private void showTutorialStep3() {
        // Step 3: Highlight "View Reports" button
        View viewReportsOverlay = getLayoutInflater().inflate(R.layout.layout_tutorial_overlay, null);
        TextView tvIcon3 = viewReportsOverlay.findViewById(R.id.tvTutorialIcon);
        TextView tvTitle3 = viewReportsOverlay.findViewById(R.id.tvTutorialTitle);
        TextView tvDesc3 = viewReportsOverlay.findViewById(R.id.tvTutorialDescription);
        com.google.android.material.button.MaterialButton btnNext = viewReportsOverlay.findViewById(R.id.btnNext);
        com.google.android.material.button.MaterialButton btnPrevious = viewReportsOverlay.findViewById(R.id.btnPrevious);
        com.google.android.material.button.MaterialButton btnFinish = viewReportsOverlay.findViewById(R.id.btnFinish);
        
        tvIcon3.setText("📊");
        tvTitle3.setText("View Reports");
        tvDesc3.setText("View all your submitted reports and track their status");
        
        // Step 3: Show both buttons
        btnPrevious.setVisibility(View.VISIBLE);
        btnNext.setVisibility(View.VISIBLE);
        btnFinish.setVisibility(View.GONE);
        
        // Add glow effect
        fabViewReports.setElevation(24f);
        fabViewReports.setTranslationZ(8f);
        
        Target viewReportsTarget = new Target.Builder()
                .setAnchor(fabViewReports)
                .setShape(new RoundedRectangle(fabViewReports.getHeight(), fabViewReports.getWidth(), 50f))
                .setOverlay(viewReportsOverlay)
                .build();
        
        Spotlight spotlight3 = new Spotlight.Builder(this)
                .setTargets(viewReportsTarget)
                .setBackgroundColor(0xCC000000) // 80% black opacity - same as step 1
                .setDuration(1L) // Instant - no animation
                .build();
        
        currentSpotlight = spotlight3;
        
        btnNext.setOnClickListener(v -> {
            if (!isStepTransitioning) {
                isStepTransitioning = true;
                btnNext.setEnabled(false);
                fabViewReports.setElevation(6f);
                fabViewReports.setTranslationZ(0f);
                fabMenu.postDelayed(() -> {
                    if (currentSpotlight != null) {
                        currentSpotlight.finish();
                    }
                    showTutorialStep4();
                    isStepTransitioning = false;
                }, 50);
            }
        });
        
        btnPrevious.setOnClickListener(v -> {
            if (!isStepTransitioning) {
                isStepTransitioning = true;
                btnPrevious.setEnabled(false);
                fabViewReports.setElevation(6f);
                fabViewReports.setTranslationZ(0f);
                fabMenu.postDelayed(() -> {
                    if (currentSpotlight != null) {
                        currentSpotlight.finish();
                    }
                    showTutorialStep2();
                    isStepTransitioning = false;
                }, 50);
            }
        });
        
        spotlight3.start();
    }
    
    private void showTutorialStep4() {
        // Step 4: Highlight "View Hearings" button
        View viewHearingsOverlay = getLayoutInflater().inflate(R.layout.layout_tutorial_overlay, null);
        TextView tvIcon4 = viewHearingsOverlay.findViewById(R.id.tvTutorialIcon);
        TextView tvTitle4 = viewHearingsOverlay.findViewById(R.id.tvTutorialTitle);
        TextView tvDesc4 = viewHearingsOverlay.findViewById(R.id.tvTutorialDescription);
        com.google.android.material.button.MaterialButton btnNext = viewHearingsOverlay.findViewById(R.id.btnNext);
        com.google.android.material.button.MaterialButton btnPrevious = viewHearingsOverlay.findViewById(R.id.btnPrevious);
        com.google.android.material.button.MaterialButton btnFinish = viewHearingsOverlay.findViewById(R.id.btnFinish);
        
        tvIcon4.setText("⚖️");
        tvTitle4.setText("View Hearings");
        tvDesc4.setText("Check your scheduled hearings and mediation sessions");
        
        // Step 4: Last step - show Previous and Finish
        btnPrevious.setVisibility(View.VISIBLE);
        btnNext.setVisibility(View.GONE);
        btnFinish.setVisibility(View.VISIBLE);
        
        // Add glow effect
        fabViewHearings.setElevation(24f);
        fabViewHearings.setTranslationZ(8f);
        
        Target viewHearingsTarget = new Target.Builder()
                .setAnchor(fabViewHearings)
                .setShape(new RoundedRectangle(fabViewHearings.getHeight(), fabViewHearings.getWidth(), 50f))
                .setOverlay(viewHearingsOverlay)
                .build();
        
        Spotlight spotlight4 = new Spotlight.Builder(this)
                .setTargets(viewHearingsTarget)
                .setBackgroundColor(0xCC000000) // 80% black opacity
                .setDuration(1L) // Instant - no animation
                .build();
        
        currentSpotlight = spotlight4;
        
        btnFinish.setOnClickListener(v -> {
            if (!isStepTransitioning) {
                isStepTransitioning = true;
                btnFinish.setEnabled(false);
                
                spotlight4.finish();
                fabViewHearings.setElevation(6f);
                fabViewHearings.setTranslationZ(0f);
                
                // Show text labels back - restore normal state
                tvAddReport.setVisibility(View.VISIBLE);
                tvViewReports.setVisibility(View.VISIBLE);
                tvViewHearings.setVisibility(View.VISIBLE);
                
                // Tutorial completed - close FAB menu and mark as done
                closeFabMenu();
                
                fabMenu.postDelayed(() -> {
                    // Re-enable all UI elements
                    fabMenu.setEnabled(true);
                    fabMenu.setClickable(true);
                    fabAddReport.setEnabled(true);
                    fabAddReport.setClickable(true);
                    fabViewReports.setEnabled(true);
                    fabViewReports.setClickable(true);
                    fabViewHearings.setEnabled(true);
                    fabViewHearings.setClickable(true);
                    
                    if (btnNotifications != null) {
                        btnNotifications.setEnabled(true);
                        btnNotifications.setClickable(true);
                    }
                    if (btnSettings != null) {
                        btnSettings.setEnabled(true);
                        btnSettings.setClickable(true);
                    }
                    if (tvUserAvatar != null) {
                        tvUserAvatar.setEnabled(true);
                        tvUserAvatar.setClickable(true);
                    }
                    if (recyclerReports != null) {
                        recyclerReports.setEnabled(true);
                        recyclerReports.setClickable(true);
                    }
                    
                    isTutorialInProgress = false;
                    currentSpotlight = null;
                    
                    // Mark tutorial as completed (per-user in database)
                    markTutorialCompleted();
                    
                    Toast.makeText(UserDashboardActivity.this, "Tutorial completed! You're all set! 🎉", Toast.LENGTH_SHORT).show();
                }, 300);
            }
        });
        
        btnPrevious.setOnClickListener(v -> {
            if (!isStepTransitioning) {
                isStepTransitioning = true;
                btnPrevious.setEnabled(false);
                fabViewHearings.setElevation(6f);
                fabViewHearings.setTranslationZ(0f);
                fabMenu.postDelayed(() -> {
                    if (currentSpotlight != null) {
                        currentSpotlight.finish();
                    }
                    showTutorialStep3();
                    isStepTransitioning = false;
                }, 50);
            }
        });
        
        spotlight4.start();
    }
    
    /**
     * Check if user has seen tooltips and show if not (per-user, not per-device)
     */
    private void checkAndShowTutorial() {
        int userId = preferencesManager.getUserId();
        
        Executors.newSingleThreadExecutor().execute(() -> {
            User currentUser = database.userDao().getUserById(userId);
            
            runOnUiThread(() -> {
                if (currentUser != null && !currentUser.hasSeenTooltips()) {
                    // User hasn't seen tooltips yet - show them with smooth transition
                    fabMenu.postDelayed(this::showTutorial, 300);
                }
            });
        });
    }
    
    /**
     * Mark tutorial as completed in database (per-user)
     */
    private void markTutorialCompleted() {
        int userId = preferencesManager.getUserId();
        
        Executors.newSingleThreadExecutor().execute(() -> {
            User currentUser = database.userDao().getUserById(userId);
            
            if (currentUser != null) {
                currentUser.setHasSeenTooltips(true);
                database.userDao().updateUser(currentUser);
                
                android.util.Log.d("UserDashboard", "✅ Tutorial marked as completed for user: " + currentUser.getUsername());
            }
        });
    }
    
    // Add periodic refresh for real-time dashboard updates (silent background refresh)
    private void startPeriodicRefresh() {
        android.os.Handler handler = new android.os.Handler();
        Runnable refreshRunnable = new Runnable() {
            @Override
            public void run() {
                // Only refresh if activity is visible and not finishing
                if (!isFinishing() && !isDestroyed()) {
                    loadDataQuietly(); // Use quiet refresh to prevent black screen flicker
                }
                handler.postDelayed(this, 20000); // Refresh every 20 seconds
            }
        };
        handler.postDelayed(refreshRunnable, 20000);
    }
    
    // Add smooth touch animation to cards (same as Officer Dashboard)
    private void addCardTouchAnimation(View card) {
        card.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    // Scale down with smooth animation
                    v.animate()
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .setDuration(100)
                        .setInterpolator(new android.view.animation.DecelerateInterpolator())
                        .start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    // Scale back to normal with bounce effect
                    v.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(150)
                        .setInterpolator(new android.view.animation.OvershootInterpolator(1.2f))
                        .start();
                    break;
            }
            return false; // Allow click events to continue
        });
    }
}
