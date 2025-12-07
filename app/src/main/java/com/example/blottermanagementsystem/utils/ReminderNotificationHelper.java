package com.example.blottermanagementsystem.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.ui.activities.HearingsActivity;

/**
 * ✅ Helper class for creating and sending hearing reminder notifications
 */
public class ReminderNotificationHelper {
    
    private static final String CHANNEL_ID = "hearing_reminders";
    private static final String CHANNEL_NAME = "Hearing Reminders";
    private static final int NOTIFICATION_ID = 1001;
    
    /**
     * Create notification channel for hearing reminders
     */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for scheduled hearings");
            channel.enableVibration(true);
            
            // ✅ Use default notification sound
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
     * Send hearing reminder notification
     */
    public static void sendHearingReminder(
        Context context,
        String caseNumber,
        String hearingDate,
        String hearingTime,
        String location,
        int hearingId
    ) {
        createNotificationChannel(context);
        
        // ✅ HearingsActivity is role-aware, works for both User and Officer
        Intent intent = new Intent(context, HearingsActivity.class);
        intent.putExtra("hearing_id", hearingId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            hearingId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🔔 Hearing Reminder")
            .setContentText("Case #" + caseNumber + " - " + hearingDate + " at " + hearingTime)
            .setStyle(new NotificationCompat.BigTextStyle()
                .bigText("📍 Location: " + location + "\n" +
                        "📅 Date: " + hearingDate + "\n" +
                        "⏰ Time: " + hearingTime))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(new long[]{0, 500, 250, 500})
            .setCategory(NotificationCompat.CATEGORY_REMINDER);
        
        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID + hearingId, builder.build());
        }
    }
    
    /**
     * Cancel hearing reminder notification
     */
    public static void cancelHearingReminder(Context context, int hearingId) {
        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (notificationManager != null) {
            notificationManager.cancel(NOTIFICATION_ID + hearingId);
        }
    }
}
