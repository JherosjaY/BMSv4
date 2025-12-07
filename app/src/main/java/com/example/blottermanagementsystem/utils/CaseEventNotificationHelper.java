package com.example.blottermanagementsystem.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.ui.activities.OfficerCaseDetailActivity;
import com.example.blottermanagementsystem.ui.activities.AdminCaseDetailActivity;
import com.example.blottermanagementsystem.ui.activities.ReportDetailActivity;
import com.example.blottermanagementsystem.utils.PreferencesManager;

/**
 * ✅ Helper class for sending case event notifications
 * Sends notifications when cases are filed, assigned, or updated
 */
public class CaseEventNotificationHelper {
    
    private static final String TAG = "CaseEventNotif";
    private static final String CHANNEL_ID = "case_events";
    private static final String CHANNEL_NAME = "Case Events";
    private static final int NOTIFICATION_ID_BASE = 2000;
    
    /**
     * Create notification channel for case events
     */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for case events");
            channel.enableVibration(true);
            channel.setSound(
                android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                new android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                    .build()
            );
            
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
    
    /**
     * Send notification when user files a case
     */
    public static void notifyCaseFiledByUser(Context context, int caseId, String caseNumber, String incidentType) {
        createNotificationChannel(context);
        
        // Create intent to open case details (User role uses ReportDetailActivity)
        Intent intent = new Intent(context, ReportDetailActivity.class);
        intent.putExtra("REPORT_ID", caseId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            caseId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("✅ Case Filed Successfully")
            .setContentText("Case #" + caseNumber + " - " + incidentType)
            .setStyle(new NotificationCompat.BigTextStyle()
                .bigText("Your case has been filed successfully.\n\nCase #" + caseNumber + "\nType: " + incidentType))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(new long[]{0, 500, 250, 500})
            .setCategory(NotificationCompat.CATEGORY_STATUS);
        
        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID_BASE + caseId, builder.build());
            Log.i(TAG, "✅ Case filed notification sent for case " + caseNumber);
        }
    }
    
    /**
     * Send notification when officer is assigned to case
     */
    public static void notifyOfficerAssignedToCase(Context context, int caseId, String caseNumber, String incidentType) {
        createNotificationChannel(context);
        
        // Create intent to open case details
        Intent intent = new Intent(context, OfficerCaseDetailActivity.class);
        intent.putExtra("REPORT_ID", caseId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            caseId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("👮 Case Assigned to You")
            .setContentText("Case #" + caseNumber + " - " + incidentType)
            .setStyle(new NotificationCompat.BigTextStyle()
                .bigText("You have been assigned to a new case.\n\nCase #" + caseNumber + "\nType: " + incidentType))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(new long[]{0, 500, 250, 500})
            .setCategory(NotificationCompat.CATEGORY_STATUS);
        
        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID_BASE + caseId, builder.build());
            Log.i(TAG, "👮 Officer assigned notification sent for case " + caseNumber);
        }
    }
    
    /**
     * Send notification when case status is updated
     */
    public static void notifyCaseStatusUpdated(Context context, int caseId, String caseNumber, String newStatus, String userRole) {
        createNotificationChannel(context);
        
        // Determine which activity to open based on role
        Class<?> targetActivity;
        if (userRole.equals("OFFICER")) {
            targetActivity = OfficerCaseDetailActivity.class;
        } else if (userRole.equals("ADMIN")) {
            targetActivity = AdminCaseDetailActivity.class;
        } else {
            // USER role
            targetActivity = ReportDetailActivity.class;
        }
        
        Intent intent = new Intent(context, targetActivity);
        intent.putExtra("REPORT_ID", caseId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            caseId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("📋 Case Status Updated")
            .setContentText("Case #" + caseNumber + " is now " + newStatus)
            .setStyle(new NotificationCompat.BigTextStyle()
                .bigText("Case status has been updated.\n\nCase #" + caseNumber + "\nStatus: " + newStatus))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(new long[]{0, 500, 250, 500})
            .setCategory(NotificationCompat.CATEGORY_STATUS);
        
        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID_BASE + caseId, builder.build());
            Log.i(TAG, "📋 Case status updated notification sent for case " + caseNumber);
        }
    }
    
    /**
     * Send notification for case comment/update
     */
    public static void notifyCaseUpdate(Context context, int caseId, String caseNumber, String message) {
        createNotificationChannel(context);
        
        // Use ReportDetailActivity for user role (default)
        Intent intent = new Intent(context, ReportDetailActivity.class);
        intent.putExtra("REPORT_ID", caseId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            caseId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("💬 New Update on Case #" + caseNumber)
            .setContentText(message)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVibrate(new long[]{0, 250})
            .setCategory(NotificationCompat.CATEGORY_MESSAGE);
        
        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID_BASE + caseId, builder.build());
            Log.i(TAG, "💬 Case update notification sent for case " + caseNumber);
        }
    }
}
