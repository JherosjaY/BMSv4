package com.example.blottermanagementsystem.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.database.BlotterDatabase;
import com.example.blottermanagementsystem.utils.PreferencesManager;
import java.util.concurrent.Executors;

public class AdminDashboardActivity extends BaseActivity {
    
    private PreferencesManager preferencesManager;
    private BlotterDatabase database;
    private TextView tvTotalUsers, tvTotalOfficers, tvTotalReports, tvPendingReports;
    private TextView tvNotificationBadge;
    private ImageButton btnNotifications, btnProfile;
    private CardView cardManageUsers, cardManageOfficers, cardViewReports, cardSendNotification;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;
    private long backPressedTime = 0;
    private Toast backToast;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);
        
        preferencesManager = new PreferencesManager(this);
        database = BlotterDatabase.getDatabase(this);
        
        initViews();
        setupListeners();
        loadDashboard();
        
        // Start periodic refresh for real-time dashboard updates
        startPeriodicRefresh();
    }
    
    private void initViews() {
        tvTotalUsers = findViewById(R.id.tvTotalUsers);
        tvTotalOfficers = findViewById(R.id.tvTotalOfficers);
        tvTotalReports = findViewById(R.id.tvTotalReports);
        tvPendingReports = findViewById(R.id.tvPendingReports);
        btnNotifications = findViewById(R.id.btnNotifications);
        btnProfile = findViewById(R.id.btnProfile);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
        cardManageUsers = findViewById(R.id.cardManageUsers);
        cardManageOfficers = findViewById(R.id.cardManageOfficers);
        cardViewReports = findViewById(R.id.cardViewReports);
        cardSendNotification = findViewById(R.id.cardSendNotification);
        
        // Swipe refresh
        swipeRefresh = findViewById(R.id.swipeRefresh);
        swipeRefresh.setColorSchemeResources(
            R.color.electric_blue,
            R.color.success_green,
            R.color.warning_yellow
        );
        swipeRefresh.setOnRefreshListener(() -> {
            loadDashboard();
        });
    }
    
    private void setupListeners() {
        btnNotifications.setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationsActivity.class));
        });
        
        btnProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminProfileActivity.class));
        });
        
        cardManageUsers.setOnClickListener(v -> {
            startActivity(new Intent(this, UserManagementActivity.class));
        });
        
        cardManageOfficers.setOnClickListener(v -> {
            startActivity(new Intent(this, OfficerManagementActivity.class));
        });
        
        cardViewReports.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminReportOversightActivity.class));
        });
        
        cardSendNotification.setOnClickListener(v -> {
            startActivity(new Intent(this, SendNotificationActivity.class));
        });
        
    }
    
    private void loadDashboard() {
        // Show loading for admin dashboard
        com.example.blottermanagementsystem.utils.GlobalLoadingManager.show(this, "Loading dashboard...");
        
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
            int totalUsers = database.userDao().getTotalUserCount(); // Exclude Admin and Officer
            int totalOfficers = database.officerDao().getAllOfficers().size();
            int totalReports = database.blotterReportDao().getAllReports().size();
            int pendingReports = database.blotterReportDao().getReportsByStatus("Pending").size();
            
            // Get unread notifications count
            int unreadNotifications = database.notificationDao()
                .getUnreadNotificationsForUser(preferencesManager.getUserId()).size();
            
            runOnUiThread(() -> {
                tvTotalUsers.setText(String.valueOf(totalUsers));
                tvTotalOfficers.setText(String.valueOf(totalOfficers));
                tvTotalReports.setText(String.valueOf(totalReports));
                tvPendingReports.setText(String.valueOf(pendingReports));
                
                // Update notification badge
                updateNotificationBadge(unreadNotifications);
                
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
    
    private void updateNotificationBadge(int count) {
        if (count > 0) {
            tvNotificationBadge.setVisibility(android.view.View.VISIBLE);
            // Show max 99+
            if (count > 99) {
                tvNotificationBadge.setText("99+");
            } else {
                tvNotificationBadge.setText(String.valueOf(count));
            }
        } else {
            tvNotificationBadge.setVisibility(android.view.View.GONE);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadDashboard(); // Refresh when returning to dashboard
    }
    
    @Override
    public void onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            if (backToast != null) backToast.cancel();
            super.onBackPressed();
            finishAffinity(); // Close all activities
            return;
        } else {
            // Vibrate
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(100); // Vibrate for 100ms
            }
            
            backToast = Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT);
            backToast.show();
        }
        backPressedTime = System.currentTimeMillis();
    }
    
    // Add periodic refresh for real-time dashboard updates (silent background refresh)
    private void startPeriodicRefresh() {
        android.os.Handler handler = new android.os.Handler();
        Runnable refreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isFinishing() && !isDestroyed()) {
                    loadDashboardQuietly(); // Silent background refresh
                }
                handler.postDelayed(this, 15000); // Refresh every 15 seconds
            }
        };
        handler.postDelayed(refreshRunnable, 15000);
    }
    
    private void loadDashboardQuietly() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                int totalUsers = database.userDao().getTotalUserCount();
                int totalOfficers = database.officerDao().getAllOfficers().size();
                int totalReports = database.blotterReportDao().getAllReports().size();
                int pendingReports = database.blotterReportDao().getReportsByStatus("Pending").size();
                int unreadNotifications = database.notificationDao()
                    .getUnreadNotificationsForUser(preferencesManager.getUserId()).size();
                
                // Only update UI if values changed
                if (hasStatisticsChanged(totalUsers, totalOfficers, totalReports, pendingReports, unreadNotifications)) {
                    runOnUiThread(() -> {
                        tvTotalUsers.setText(String.valueOf(totalUsers));
                        tvTotalOfficers.setText(String.valueOf(totalOfficers));
                        tvTotalReports.setText(String.valueOf(totalReports));
                        tvPendingReports.setText(String.valueOf(pendingReports));
                        updateNotificationBadge(unreadNotifications);
                    });
                }
            } catch (Exception e) {
                android.util.Log.e("AdminDashboard", "Error in quiet refresh: " + e.getMessage());
            }
        });
    }
    
    private int lastTotalUsers = -1, lastTotalOfficers = -1, lastTotalReports = -1, lastPendingReports = -1, lastUnreadNotifications = -1;
    
    private boolean hasStatisticsChanged(int totalUsers, int totalOfficers, int totalReports, int pendingReports, int unreadNotifications) {
        if (lastTotalUsers != totalUsers || lastTotalOfficers != totalOfficers || 
            lastTotalReports != totalReports || lastPendingReports != pendingReports || 
            lastUnreadNotifications != unreadNotifications) {
            lastTotalUsers = totalUsers;
            lastTotalOfficers = totalOfficers;
            lastTotalReports = totalReports;
            lastPendingReports = pendingReports;
            lastUnreadNotifications = unreadNotifications;
            return true;
        }
        return false;
    }
}
