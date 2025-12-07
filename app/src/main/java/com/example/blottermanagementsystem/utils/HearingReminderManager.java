package com.example.blottermanagementsystem.utils;

import android.content.Context;
import android.util.Log;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.example.blottermanagementsystem.data.entity.Hearing;
import com.example.blottermanagementsystem.workers.HearingReminderWorker;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * ✅ Manager for scheduling and cancelling hearing reminder notifications
 */
public class HearingReminderManager {
    
    private static final String TAG = "HearingReminderMgr";
    
    /**
     * Schedule all reminders for a hearing (1 day, 1 hour, 15 min, at time)
     */
    public static void scheduleHearingReminders(Context context, Hearing hearing) {
        if (hearing == null || hearing.getHearingDate() == null || hearing.getHearingTime() == null) {
            Log.e(TAG, "Invalid hearing data");
            return;
        }
        
        // ✅ Check if reminders are globally enabled
        ReminderPreferencesManager prefsManager = new ReminderPreferencesManager(context);
        if (!prefsManager.areRemindersEnabled()) {
            Log.w(TAG, "⏸️ Reminders are globally disabled");
            return;
        }
        
        try {
            // Parse hearing date and time
            String dateTimeStr = hearing.getHearingDate() + " " + hearing.getHearingTime();
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
            Date hearingDateTime = sdf.parse(dateTimeStr);
            
            if (hearingDateTime == null) {
                Log.e(TAG, "Could not parse hearing date/time");
                return;
            }
            
            long hearingTimeMs = hearingDateTime.getTime();
            long currentTimeMs = System.currentTimeMillis();
            
            // ✅ Schedule reminders based on user preferences
            if (prefsManager.isOneDayBeforeEnabled()) {
                scheduleReminder(context, hearing, "1_day", hearingTimeMs - TimeUnit.DAYS.toMillis(1), currentTimeMs);
            }
            
            if (prefsManager.isOneHourBeforeEnabled()) {
                scheduleReminder(context, hearing, "1_hour", hearingTimeMs - TimeUnit.HOURS.toMillis(1), currentTimeMs);
            }
            
            if (prefsManager.isFifteenMinBeforeEnabled()) {
                scheduleReminder(context, hearing, "15_min", hearingTimeMs - TimeUnit.MINUTES.toMillis(15), currentTimeMs);
            }
            
            if (prefsManager.isAtTimeEnabled()) {
                scheduleReminder(context, hearing, "at_time", hearingTimeMs, currentTimeMs);
            }
            
            Log.i(TAG, "✅ All reminders scheduled for hearing " + hearing.getId());
            ReminderHistoryLogger.logReminderSent(context, hearing.getId(), "SCHEDULED", String.valueOf(hearing.getBlotterReportId()));
            
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling reminders: " + e.getMessage());
        }
    }
    
    /**
     * Schedule a single reminder
     */
    private static void scheduleReminder(
        Context context,
        Hearing hearing,
        String reminderType,
        long reminderTimeMs,
        long currentTimeMs
    ) {
        if (reminderTimeMs <= currentTimeMs) {
            Log.w(TAG, "Reminder time is in the past, skipping: " + reminderType);
            return;
        }
        
        long delayMs = reminderTimeMs - currentTimeMs;
        
        // Create work request
        Data inputData = new Data.Builder()
            .putInt("hearing_id", hearing.getId())
            .putString("reminder_type", reminderType)
            .build();
        
        OneTimeWorkRequest reminderRequest = new OneTimeWorkRequest.Builder(HearingReminderWorker.class)
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag("hearing_reminder_" + hearing.getId())
            .build();
        
        WorkManager.getInstance(context).enqueueUniqueWork(
            "hearing_reminder_" + hearing.getId() + "_" + reminderType,
            androidx.work.ExistingWorkPolicy.KEEP,
            reminderRequest
        );
        
        Log.i(TAG, "📅 Scheduled " + reminderType + " reminder for hearing " + hearing.getId() + 
              " (in " + (delayMs / 1000 / 60) + " minutes)");
    }
    
    /**
     * Cancel all reminders for a hearing
     */
    public static void cancelHearingReminders(Context context, int hearingId) {
        try {
            WorkManager.getInstance(context).cancelAllWorkByTag("hearing_reminder_" + hearingId);
            ReminderNotificationHelper.cancelHearingReminder(context, hearingId);
            Log.i(TAG, "✅ All reminders cancelled for hearing " + hearingId);
            ReminderHistoryLogger.logReminderCancelled(hearingId, "Hearing cancelled/deleted");
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling reminders: " + e.getMessage());
            ReminderHistoryLogger.logReminderFailed(hearingId, "CANCEL", e.getMessage());
        }
    }
    
    /**
     * Reschedule reminders for a hearing (when hearing is updated)
     */
    public static void rescheduleHearingReminders(Context context, Hearing hearing) {
        try {
            // Cancel old reminders
            cancelHearingReminders(context, hearing.getId());
            
            // Schedule new reminders
            scheduleHearingReminders(context, hearing);
            
            Log.i(TAG, "🔄 Reminders rescheduled for hearing " + hearing.getId());
            ReminderHistoryLogger.logReminderRescheduled(hearing.getId(), "Old time", hearing.getHearingDate() + " " + hearing.getHearingTime());
        } catch (Exception e) {
            Log.e(TAG, "Error rescheduling reminders: " + e.getMessage());
            ReminderHistoryLogger.logReminderFailed(hearing.getId(), "RESCHEDULE", e.getMessage());
        }
    }
}
