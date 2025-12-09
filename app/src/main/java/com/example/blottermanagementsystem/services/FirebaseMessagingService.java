package com.example.blottermanagementsystem.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.ui.activities.UserDashboardActivity;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Firebase Cloud Messaging Service
 * Handles incoming push notifications from backend
 */
public class FirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService {

    private static final String TAG = "FCM_Service";
    private static final String CHANNEL_ID = "bms_notifications";
    private static final String CHANNEL_NAME = "BMS Notifications";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "🔔 Message received from: " + remoteMessage.getFrom());

        // Check if message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "✅ Notification Title: " + remoteMessage.getNotification().getTitle());
            Log.d(TAG, "✅ Notification Body: " + remoteMessage.getNotification().getBody());

            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();

            // Get data payload
            String notificationId = remoteMessage.getData().get("notificationId");
            String type = remoteMessage.getData().get("type");
            String relatedReportId = remoteMessage.getData().get("relatedReportId");

            Log.d(TAG, "📦 Data - ID: " + notificationId + ", Type: " + type + ", ReportID: " + relatedReportId);

            // Show notification
            showNotification(title, body, notificationId, type, relatedReportId);
        }

        // Check if message contains a data payload
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "📊 Message data: " + remoteMessage.getData());
        }
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);

        Log.d(TAG, "🔑 New FCM Token: " + token);

        // Save FCM token to backend
        saveFCMTokenToBackend(token);
    }

    /**
     * Display notification in notification tray
     */
    private void showNotification(
            String title,
            String body,
            String notificationId,
            String type,
            String relatedReportId
    ) {
        try {
            // Create notification channel (required for Android 8+)
            createNotificationChannel();

            // Create intent to open app when notification is tapped
            Intent intent = new Intent(this, UserDashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            // Add data to intent
            if (notificationId != null) {
                intent.putExtra("notificationId", notificationId);
            }
            if (type != null) {
                intent.putExtra("notificationType", type);
            }
            if (relatedReportId != null) {
                intent.putExtra("relatedReportId", relatedReportId);
            }

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this,
                    (int) System.currentTimeMillis(), // Unique ID
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Build notification
            NotificationCompat.Builder notificationBuilder =
                    new NotificationCompat.Builder(this, CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_launcher_foreground)
                            .setContentTitle(title)
                            .setContentText(body)
                            .setAutoCancel(true)
                            .setContentIntent(pendingIntent)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(body));

            // Set notification color
            notificationBuilder.setColor(getResources().getColor(R.color.purple_500));

            // Show notification
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager != null) {
                int notificationId_int = (int) System.currentTimeMillis();
                notificationManager.notify(notificationId_int, notificationBuilder.build());
                Log.d(TAG, "✅ Notification displayed: " + title);
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Error showing notification: " + e.getMessage(), e);
        }
    }

    /**
     * Create notification channel (required for Android 8+)
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );

            channel.setDescription("Blotter Management System Notifications");
            channel.enableVibration(true);
            channel.setShowBadge(true);

            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "✅ Notification channel created");
            }
        }
    }

    /**
     * Save FCM token to backend
     */
    private void saveFCMTokenToBackend(String token) {
        try {
            // Get user ID from preferences
            com.example.blottermanagementsystem.utils.PreferencesManager preferencesManager =
                    new com.example.blottermanagementsystem.utils.PreferencesManager(this);

            String userId = preferencesManager.getUserId();

            if (userId == null || userId.isEmpty()) {
                Log.w(TAG, "⚠️ User not logged in, cannot save FCM token");
                return;
            }

            // Get device ID
            String deviceId = android.provider.Settings.Secure.getString(
                    getContentResolver(),
                    android.provider.Settings.Secure.ANDROID_ID
            );

            Log.d(TAG, "💾 Saving FCM token for user: " + userId);

            // Call API to save FCM token
            com.example.blottermanagementsystem.utils.ApiClient.saveFCMToken(
                    userId,
                    token,
                    deviceId,
                    new com.example.blottermanagementsystem.utils.ApiClient.ApiCallback<Object>() {
                        @Override
                        public void onSuccess(Object response) {
                            Log.d(TAG, "✅ FCM token saved to backend");
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Log.e(TAG, "❌ Failed to save FCM token: " + errorMessage);
                        }
                    }
            );

        } catch (Exception e) {
            Log.e(TAG, "❌ Error saving FCM token: " + e.getMessage(), e);
        }
    }
}
