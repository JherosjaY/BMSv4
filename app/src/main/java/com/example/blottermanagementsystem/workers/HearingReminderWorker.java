package com.example.blottermanagementsystem.workers;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.example.blottermanagementsystem.data.database.BlotterDatabase;
import com.example.blottermanagementsystem.data.entity.Hearing;
import com.example.blottermanagementsystem.utils.ReminderNotificationHelper;

/**
 * ✅ Background worker for sending hearing reminder notifications
 */
public class HearingReminderWorker extends Worker {
    
    private static final String TAG = "HearingReminder";
    
    public HearingReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }
    
    @NonNull
    @Override
    public Result doWork() {
        try {
            int hearingId = getInputData().getInt("hearing_id", -1);
            String reminderType = getInputData().getString("reminder_type"); // "1_day", "1_hour", "15_min", "at_time"
            
            if (hearingId == -1 || reminderType == null) {
                Log.e(TAG, "Invalid hearing ID or reminder type");
                return Result.failure();
            }
            
            // Get hearing from database
            BlotterDatabase database = BlotterDatabase.getDatabase(getApplicationContext());
            Hearing hearing = database.hearingDao().getHearingById(hearingId);
            
            if (hearing == null) {
                Log.e(TAG, "Hearing not found: " + hearingId);
                return Result.failure();
            }
            
            // Check if hearing is still scheduled
            if (!hearing.getStatus().equalsIgnoreCase("Scheduled")) {
                Log.w(TAG, "Hearing is not scheduled, skipping reminder");
                return Result.success();
            }
            
            // Send notification
            ReminderNotificationHelper.sendHearingReminder(
                getApplicationContext(),
                String.valueOf(hearing.getBlotterReportId()),
                hearing.getHearingDate(),
                hearing.getHearingTime(),
                hearing.getLocation(),
                hearingId
            );
            
            Log.i(TAG, "✅ Reminder sent for hearing " + hearingId + " (" + reminderType + ")");
            return Result.success();
            
        } catch (Exception e) {
            Log.e(TAG, "Error sending reminder: " + e.getMessage());
            return Result.retry();
        }
    }
}
