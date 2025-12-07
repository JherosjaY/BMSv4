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
import com.example.blottermanagementsystem.data.database.BlotterDatabase;
import com.example.blottermanagementsystem.data.entity.Notification;
import com.example.blottermanagementsystem.ui.adapters.NotificationAdapter;
import com.example.blottermanagementsystem.utils.PreferencesManager;
import java.util.List;
import java.util.concurrent.Executors;

public class NotificationsActivity extends BaseActivity {
    private BlotterDatabase database;
    private PreferencesManager preferencesManager;
    private RecyclerView recyclerView;
    private ImageButton btnMarkAllRead, btnDelete;
    private View emptyState;
    private NotificationAdapter adapter;
    private boolean isSelectionMode = false;
    private List<Integer> selectedNotifications = new java.util.ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        android.util.Log.d("NotificationsActivity", "🚀 onCreate STARTED");
        super.onCreate(savedInstanceState);
        
        try {
            android.util.Log.d("NotificationsActivity", "Setting content view...");
            setContentView(R.layout.activity_notifications);
            android.util.Log.d("NotificationsActivity", "✅ Content view set");
            
            android.util.Log.d("NotificationsActivity", "Initializing database and preferences...");
            database = BlotterDatabase.getDatabase(this);
            preferencesManager = new PreferencesManager(this);
            android.util.Log.d("NotificationsActivity", "✅ Database and preferences initialized");
            
            android.util.Log.d("NotificationsActivity", "Setting up toolbar...");
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Notifications");
            }
            android.util.Log.d("NotificationsActivity", "✅ Toolbar set up");
            
            android.util.Log.d("NotificationsActivity", "Setting up RecyclerView...");
            recyclerView = findViewById(R.id.recyclerViewNotifications);
            if (recyclerView == null) {
                throw new NullPointerException("RecyclerView is null! Check layout XML.");
            }
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            android.util.Log.d("NotificationsActivity", "✅ RecyclerView set up");
            
            android.util.Log.d("NotificationsActivity", "Finding empty state view...");
            emptyState = findViewById(R.id.emptyState);
            if (emptyState == null) {
                throw new NullPointerException("emptyState view is null! Check layout XML.");
            }
            android.util.Log.d("NotificationsActivity", "✅ Empty state view found");
            
            android.util.Log.d("NotificationsActivity", "Setting up toolbar buttons...");
            btnMarkAllRead = findViewById(R.id.btnMarkAllRead);
            btnDelete = findViewById(R.id.btnDelete);
            if (btnMarkAllRead == null || btnDelete == null) {
                throw new NullPointerException("Toolbar buttons are null! Check layout XML.");
            }
            btnMarkAllRead.setOnClickListener(v -> markAllAsRead());
            btnDelete.setOnClickListener(v -> deleteSelectedNotifications());
            android.util.Log.d("NotificationsActivity", "✅ Toolbar buttons set up");
            
            android.util.Log.d("NotificationsActivity", "✅✅✅ onCreate completed successfully");
            
            // Add small delay to ensure activity is fully initialized
            recyclerView.postDelayed(this::loadNotifications, 100);
        } catch (Exception e) {
            android.util.Log.e("NotificationsActivity", "❌❌❌ onCreate FAILED: " + e.getMessage());
            android.util.Log.e("NotificationsActivity", "Stack trace:");
            e.printStackTrace();
            Toast.makeText(this, "ERROR: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }
    
    private void loadNotifications() {
        if (isFinishing() || isDestroyed()) {
            android.util.Log.w("NotificationsActivity", "Activity is finishing/destroyed, skipping load");
            return;
        }
        
        int userId = preferencesManager.getUserId();
        android.util.Log.d("NotificationsActivity", "Loading notifications for userId: " + userId);
        
        if (userId == -1) {
            android.util.Log.e("NotificationsActivity", "Invalid userId: -1");
            runOnUiThread(() -> {
                Toast.makeText(this, "User session invalid", Toast.LENGTH_SHORT).show();
                finish();
            });
            return;
        }
        
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<Notification> notifications = database.notificationDao()
                    .getNotificationsByUserId(userId);
            
            android.util.Log.d("NotificationsActivity", "Found " + notifications.size() + " notifications");
            
            // Check if there are unread notifications
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
                if (isFinishing() || isDestroyed()) {
                    android.util.Log.w("NotificationsActivity", "Activity finished before UI update");
                    return;
                }
                if (isEmpty) {
                    // Show empty state
                    if (recyclerView != null) {
                        recyclerView.setVisibility(View.GONE);
                    }
                    if (emptyState != null) {
                        emptyState.setVisibility(View.VISIBLE);
                    }
                    if (btnMarkAllRead != null) {
                        btnMarkAllRead.setVisibility(View.GONE);
                    }
                } else {
                    // Show notifications list
                    if (recyclerView != null) {
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                    if (emptyState != null) {
                        emptyState.setVisibility(View.GONE);
                    }
                    
                    try {
                        adapter = new NotificationAdapter(notifications, 
                            this::onNotificationClick,
                            this::onNotificationLongClick);
                        adapter.setViewDetailsListener(this::showNotificationDetails);
                        recyclerView.setAdapter(adapter);
                        android.util.Log.d("NotificationsActivity", "✅ Adapter set successfully");
                    } catch (Exception e) {
                        android.util.Log.e("NotificationsActivity", "❌ Error setting adapter: " + e.getMessage());
                        e.printStackTrace();
                    }
                    
                    // Show/hide mark all read button
                    btnMarkAllRead.setVisibility(showMarkAllRead ? View.VISIBLE : View.GONE);
                }
            });
            } catch (Exception e) {
                android.util.Log.e("NotificationsActivity", "❌ Error in background thread: " + e.getMessage());
                e.printStackTrace();
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        Toast.makeText(this, "Error loading notifications", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
    
    private void markAllAsRead() {
        int userId = preferencesManager.getUserId();
        String userRole = preferencesManager.getUserRole();
        
        android.util.Log.d("NotificationsActivity", "📌 Mark All as Read - UserId: " + userId + ", Role: " + userRole);
        
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Notification> notifications = database.notificationDao()
                .getNotificationsByUserId(userId);
            
            android.util.Log.d("NotificationsActivity", "📊 Found " + notifications.size() + " notifications for userId: " + userId);
            
            int markedCount = 0;
            for (Notification n : notifications) {
                if (!n.isRead()) {
                    n.setRead(true);
                    database.notificationDao().updateNotification(n);
                    markedCount++;
                    android.util.Log.d("NotificationsActivity", "✓ Marked notification #" + n.getId() + " as read");
                }
            }
            
            final int finalMarkedCount = markedCount;
            runOnUiThread(() -> {
                Toast.makeText(this, finalMarkedCount + " notifications marked as read", Toast.LENGTH_SHORT).show();
                loadNotifications(); // Reload to hide button
            });
        });
    }
    
    private void onNotificationLongClick(Notification notification) {
        // Enter selection mode on long press
        if (!isSelectionMode) {
            toggleSelectionMode();
            // Auto-select the long-pressed notification
            toggleNotificationSelection(notification.getId());
        }
    }
    
    private void onNotificationLongClickOld(Notification notification) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Notification")
            .setMessage("Are you sure you want to delete this notification?")
            .setPositiveButton("Delete", (dialog, which) -> {
                Executors.newSingleThreadExecutor().execute(() -> {
                    database.notificationDao().deleteNotification(notification);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Notification deleted", Toast.LENGTH_SHORT).show();
                        loadNotifications();
                    });
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void onNotificationClick(Notification notification) {
        try {
            // If in selection mode, toggle selection
            if (isSelectionMode) {
                toggleNotificationSelection(notification.getId());
                return;
            }
            
            // In normal mode, card click opens full message dialog
            showNotificationDetails(notification);
        } catch (Exception e) {
            android.util.Log.e("NotificationsActivity", "❌ Error in onNotificationClick: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void toggleSelectionMode() {
        isSelectionMode = !isSelectionMode;
        selectedNotifications.clear();
        
        if (adapter != null) {
            adapter.setSelectionMode(isSelectionMode);
            adapter.notifyDataSetChanged();
        }
        
        updateToolbarForSelectionMode();
    }
    
    private void updateToolbarForSelectionMode() {
        if (isSelectionMode) {
            // In selection mode - show trash, hide mark all read
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Select notifications");
            }
            btnMarkAllRead.setVisibility(View.GONE);
            btnDelete.setVisibility(View.VISIBLE);
        } else {
            // Normal mode - hide trash, show mark all read (if unread exist)
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Notifications");
            }
            btnDelete.setVisibility(View.GONE);
            // btnMarkAllRead visibility will be set by loadNotifications
        }
    }
    
    private void toggleNotificationSelection(int notificationId) {
        if (selectedNotifications.contains(notificationId)) {
            selectedNotifications.remove(Integer.valueOf(notificationId));
        } else {
            selectedNotifications.add(notificationId);
        }
        
        if (adapter != null) {
            adapter.setSelectedNotifications(selectedNotifications);
            adapter.notifyDataSetChanged();
        }
        
        // Update title with count
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(selectedNotifications.size() + " selected");
        }
    }
    
    private void deleteSelectedNotifications() {
        if (selectedNotifications.isEmpty()) {
            Toast.makeText(this, "No notifications selected", Toast.LENGTH_SHORT).show();
            return;
        }
        
        int userId = preferencesManager.getUserId();
        String userRole = preferencesManager.getUserRole();
        
        // ✅ Delete directly without confirmation dialog
        Executors.newSingleThreadExecutor().execute(() -> {
            int deletedCount = 0;
            
            android.util.Log.d("NotificationsActivity", "🗑️ Delete - UserId: " + userId + ", Role: " + userRole);
            
            for (int id : selectedNotifications) {
                Notification notification = database.notificationDao().getNotificationById(id);
                
                // SECURITY: Only delete if notification belongs to current user
                if (notification != null && notification.getUserId() == userId) {
                    database.notificationDao().deleteNotification(notification);
                    deletedCount++;
                    android.util.Log.d("NotificationsActivity", "✓ Deleted notification #" + id + " (belongs to userId: " + userId + ")");
                } else if (notification != null) {
                    android.util.Log.w("NotificationsActivity", "⚠️ BLOCKED: Attempted to delete notification #" + id + " belonging to userId: " + notification.getUserId() + " (current user: " + userId + ")");
                }
            }
            
            final int finalDeletedCount = deletedCount;
            runOnUiThread(() -> {
                Toast.makeText(this, finalDeletedCount + " notification(s) deleted", Toast.LENGTH_SHORT).show();
                selectedNotifications.clear();
                isSelectionMode = false;
                updateToolbarForSelectionMode();
                loadNotifications();
            });
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        android.util.Log.d("NotificationsActivity", "onResume called");
        if (!isFinishing() && !isDestroyed()) {
            loadNotifications();
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (isSelectionMode) {
                // Exit selection mode instead of closing activity
                toggleSelectionMode();
                return true;
            }
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void showNotificationDetails(Notification notification) {
        // Create a dialog to show full notification message
        android.widget.LinearLayout dialogView = new android.widget.LinearLayout(this);
        dialogView.setOrientation(android.widget.LinearLayout.VERTICAL);
        dialogView.setPadding(24, 24, 24, 24);
        
        // Title
        android.widget.TextView tvTitle = new android.widget.TextView(this);
        tvTitle.setText(notification.getTitle());
        tvTitle.setTextSize(18);
        tvTitle.setTextColor(android.graphics.Color.WHITE);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        dialogView.addView(tvTitle);
        
        // Divider
        android.view.View divider = new android.view.View(this);
        divider.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            2
        ));
        divider.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.electric_blue));
        android.widget.LinearLayout.LayoutParams dividerParams = 
            (android.widget.LinearLayout.LayoutParams) divider.getLayoutParams();
        dividerParams.setMargins(0, 16, 0, 16);
        divider.setLayoutParams(dividerParams);
        dialogView.addView(divider);
        
        // Full Message
        android.widget.TextView tvMessage = new android.widget.TextView(this);
        tvMessage.setText(notification.getMessage());
        tvMessage.setTextSize(14);
        tvMessage.setTextColor(android.graphics.Color.WHITE);
        tvMessage.setLineSpacing(1.5f, 1.5f);
        tvMessage.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        dialogView.addView(tvMessage);
        
        // Timestamp
        android.widget.TextView tvTime = new android.widget.TextView(this);
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("MMM dd, yyyy hh:mm a", java.util.Locale.getDefault());
        tvTime.setText(dateFormat.format(new java.util.Date(notification.getTimestamp())));
        tvTime.setTextSize(12);
        tvTime.setTextColor(android.graphics.Color.parseColor("#60a5fa"));
        tvTime.setTypeface(null, android.graphics.Typeface.ITALIC);
        android.widget.LinearLayout.LayoutParams timeParams = new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        timeParams.setMargins(0, 16, 0, 0);
        tvTime.setLayoutParams(timeParams);
        dialogView.addView(tvTime);
        
        // Create dialog
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .create();
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
        }
        
        dialog.show();
    }
    
    @Override
    public void onBackPressed() {
        if (isSelectionMode) {
            // Exit selection mode on back press
            toggleSelectionMode();
        } else {
            super.onBackPressed();
        }
    }
}
