package com.example.blottermanagementsystem.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.entity.Notification;
import com.example.blottermanagementsystem.ui.adapters.NotificationAdapter;
import com.example.blottermanagementsystem.utils.PreferencesManager;
import com.example.blottermanagementsystem.utils.NetworkMonitor;
import com.example.blottermanagementsystem.utils.ApiClient;
import java.util.ArrayList;
import java.util.List;

/**
 * PURE ONLINE NOTIFICATIONS ACTIVITY
 * All notifications loaded from API (Neon database)
 * No local database dependencies
 */
public class NotificationsActivity extends AppCompatActivity {
    private PreferencesManager preferencesManager;
    private NetworkMonitor networkMonitor;
    private RecyclerView recyclerView;
    private ImageButton btnMarkAllRead, btnDelete;
    private View emptyState;
    private NotificationAdapter adapter;
    private boolean isSelectionMode = false;
    private List<Integer> selectedNotifications = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        preferencesManager = new PreferencesManager(this);
        networkMonitor = new NetworkMonitor(this);

        setupToolbar();
        initViews();
        setupListeners();
        loadNotifications();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Notifications");
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        btnMarkAllRead = findViewById(R.id.btnMarkAllRead);
        btnDelete = findViewById(R.id.btnDelete);
        emptyState = findViewById(R.id.emptyState);

        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
        }
    }

    private void setupListeners() {
        if (btnMarkAllRead != null) {
            btnMarkAllRead.setOnClickListener(v -> markAllAsRead());
        }
        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> deleteSelected());
        }
    }

    /**
     * PURE ONLINE: Load notifications from API
     */
    private void loadNotifications() {
        if (!networkMonitor.isOnline()) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            showEmptyState();
            return;
        }

        String userId = preferencesManager.getUserId();

        ApiClient.getNotifications(userId, new ApiClient.ApiCallback<List<Notification>>() {
            @Override
            public void onSuccess(List<Notification> notifications) {
                if (isFinishing() || isDestroyed()) return;

                boolean hasUnread = false;
                for (Notification n : notifications) {
                    if (!n.isRead()) {
                        hasUnread = true;
                        break;
                    }
                }

                final boolean showMarkAllRead = hasUnread;
                final boolean isEmpty = notifications.isEmpty();

                runOnUiThread(() -> {
                    adapter = new NotificationAdapter(notifications, n -> {}, n -> {});

                    if (isEmpty) {
                        showEmptyState();
                        if (btnMarkAllRead != null) btnMarkAllRead.setVisibility(View.GONE);
                    } else {
                        hideEmptyState();

                        try {
                            adapter.setViewDetailsListener(NotificationsActivity.this::showNotificationDetails);
                            recyclerView.setAdapter(adapter);
                        } catch (Exception e) {
                            Toast.makeText(NotificationsActivity.this, "Error loading notifications", Toast.LENGTH_SHORT).show();
                        }

                        if (btnMarkAllRead != null) {
                            btnMarkAllRead.setVisibility(showMarkAllRead ? View.VISIBLE : View.GONE);
                        }
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                if (isFinishing() || isDestroyed()) return;

                runOnUiThread(() -> {
                    Toast.makeText(NotificationsActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                    showEmptyState();
                });
            }
        });
    }

    private void markAllAsRead() {
        String userId = preferencesManager.getUserId();
        ApiClient.markAllNotificationsAsRead(userId, new ApiClient.ApiCallback<Object>() {
            @Override
            public void onSuccess(Object response) {
                Toast.makeText(NotificationsActivity.this, "All marked as read", Toast.LENGTH_SHORT).show();
                loadNotifications();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(NotificationsActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteSelected() {
        if (selectedNotifications.isEmpty()) {
            Toast.makeText(this, "No notifications selected", Toast.LENGTH_SHORT).show();
            return;
        }

        for (Integer notificationId : selectedNotifications) {
            ApiClient.deleteNotification(notificationId, new ApiClient.ApiCallback<Object>() {
                @Override
                public void onSuccess(Object response) {
                    loadNotifications();
                }

                @Override
                public void onError(String errorMessage) {
                    Toast.makeText(NotificationsActivity.this, "Error deleting notification", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showNotificationDetails(Notification notification) {
        // Handle notification tap
        if (notification.getRelatedReportId() > 0) {
            Intent intent = new Intent(this, ReportDetailActivity.class);
            intent.putExtra("reportId", notification.getRelatedReportId());
            startActivity(intent);
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
        loadNotifications();
    }
}
