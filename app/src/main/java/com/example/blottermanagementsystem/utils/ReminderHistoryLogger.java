package com.example.blottermanagementsystem.utils;

import android.content.Context;
import android.util.Log;
import com.example.blottermanagementsystem.data.database.BlotterDatabase;
import com.example.blottermanagementsystem.data.entity.Hearing;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

/**
 * ✅ Logger for tracking reminder history
 * Records when reminders are sent, cancelled, or rescheduled
 */
public class ReminderHistoryLogger {
    
    private static final String TAG = "ReminderHistory";
    
    /**
     * Log reminder sent event
     */
    public static void logReminderSent(Context context, int hearingId, String reminderType, String caseNumber) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                String message = String.format(
                    "📤 REMINDER SENT | Hearing: %d | Type: %s | Case: %s | Time: %s",
                    hearingId, reminderType, caseNumber, timestamp
                );
                Log.i(TAG, message);
                
                // Could save to database for admin dashboard
                // saveToDatabase(context, "SENT", hearingId, reminderType, caseNumber);
                
            } catch (Exception e) {
                Log.e(TAG, "Error logging reminder sent: " + e.getMessage());
            }
        });
    }
    
    /**
     * Log reminder cancelled event
     */
    public static void logReminderCancelled(int hearingId, String reason) {
        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            String message = String.format(
                "❌ REMINDER CANCELLED | Hearing: %d | Reason: %s | Time: %s",
                hearingId, reason, timestamp
            );
            Log.i(TAG, message);
        } catch (Exception e) {
            Log.e(TAG, "Error logging reminder cancelled: " + e.getMessage());
        }
    }
    
    /**
     * Log reminder rescheduled event
     */
    public static void logReminderRescheduled(int hearingId, String oldDateTime, String newDateTime) {
        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            String message = String.format(
                "🔄 REMINDER RESCHEDULED | Hearing: %d | Old: %s | New: %s | Time: %s",
                hearingId, oldDateTime, newDateTime, timestamp
            );
            Log.i(TAG, message);
        } catch (Exception e) {
            Log.e(TAG, "Error logging reminder rescheduled: " + e.getMessage());
        }
    }
    
    /**
     * Log reminder failed event
     */
    public static void logReminderFailed(int hearingId, String reminderType, String errorMessage) {
        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            String message = String.format(
                "⚠️ REMINDER FAILED | Hearing: %d | Type: %s | Error: %s | Time: %s",
                hearingId, reminderType, errorMessage, timestamp
            );
            Log.e(TAG, message);
        } catch (Exception e) {
            Log.e(TAG, "Error logging reminder failed: " + e.getMessage());
        }
    }
    
    /**
     * Log reminder skipped event (e.g., hearing cancelled)
     */
    public static void logReminderSkipped(int hearingId, String reminderType, String reason) {
        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            String message = String.format(
                "⏭️ REMINDER SKIPPED | Hearing: %d | Type: %s | Reason: %s | Time: %s",
                hearingId, reminderType, reason, timestamp
            );
            Log.w(TAG, message);
        } catch (Exception e) {
            Log.e(TAG, "Error logging reminder skipped: " + e.getMessage());
        }
    }
    
    /**
     * Log reminder statistics
     */
    public static void logReminderStatistics(Context context) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                BlotterDatabase database = BlotterDatabase.getDatabase(context);
                int totalHearings = database.hearingDao().getAllHearings().size();
                
                Log.i(TAG, "═══════════════════════════════════════");
                Log.i(TAG, "📊 REMINDER STATISTICS");
                Log.i(TAG, "═══════════════════════════════════════");
                Log.i(TAG, "Total hearings in system: " + totalHearings);
                Log.i(TAG, "Timestamp: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
                Log.i(TAG, "═══════════════════════════════════════");
                
            } catch (Exception e) {
                Log.e(TAG, "Error logging statistics: " + e.getMessage());
            }
        });
    }
}
