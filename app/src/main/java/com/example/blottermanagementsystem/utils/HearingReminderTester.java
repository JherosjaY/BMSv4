package com.example.blottermanagementsystem.utils;

import android.content.Context;
import android.util.Log;
import com.example.blottermanagementsystem.data.entity.Hearing;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * ✅ Testing utility for hearing reminders
 * Use this to test reminders without waiting for actual times
 */
public class HearingReminderTester {
    
    private static final String TAG = "HearingReminderTest";
    
    /**
     * Create a test hearing with reminders in 10 seconds (for testing)
     */
    public static void createTestHearing(Context context) {
        try {
            Hearing testHearing = new Hearing();
            testHearing.setBlotterReportId(999); // Test report ID
            
            // Set hearing for 10 seconds from now
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.SECOND, 10);
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            
            testHearing.setHearingDate(dateFormat.format(cal.getTime()));
            testHearing.setHearingTime(timeFormat.format(cal.getTime()));
            testHearing.setLocation("Test Location");
            testHearing.setPurpose("Test Hearing");
            testHearing.setStatus("Scheduled");
            testHearing.setCreatedAt(System.currentTimeMillis());
            
            Log.i(TAG, "📅 Test hearing created:");
            Log.i(TAG, "   Date: " + testHearing.getHearingDate());
            Log.i(TAG, "   Time: " + testHearing.getHearingTime());
            Log.i(TAG, "   ⏰ Reminder will trigger in ~10 seconds");
            
            // Schedule reminders
            HearingReminderManager.scheduleHearingReminders(context, testHearing);
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating test hearing: " + e.getMessage());
        }
    }
    
    /**
     * Send an immediate test notification
     */
    public static void sendTestNotification(Context context) {
        try {
            ReminderNotificationHelper.sendHearingReminder(
                context,
                "TEST-001",
                "Dec 05, 2025",
                "08:00 AM",
                "Test Location - Barangay Hall",
                9999
            );
            Log.i(TAG, "✅ Test notification sent!");
        } catch (Exception e) {
            Log.e(TAG, "Error sending test notification: " + e.getMessage());
        }
    }
    
    /**
     * Log all scheduled reminders info
     */
    public static void logReminderInfo(Hearing hearing) {
        if (hearing == null) {
            Log.w(TAG, "Hearing is null");
            return;
        }
        
        Log.i(TAG, "═══════════════════════════════════════");
        Log.i(TAG, "📋 HEARING REMINDER INFO");
        Log.i(TAG, "═══════════════════════════════════════");
        Log.i(TAG, "Hearing ID: " + hearing.getId());
        Log.i(TAG, "Case #: " + hearing.getBlotterReportId());
        Log.i(TAG, "Date: " + hearing.getHearingDate());
        Log.i(TAG, "Time: " + hearing.getHearingTime());
        Log.i(TAG, "Location: " + hearing.getLocation());
        Log.i(TAG, "Status: " + hearing.getStatus());
        Log.i(TAG, "Reminders Scheduled: " + hearing.isReminderScheduled());
        Log.i(TAG, "═══════════════════════════════════════");
        Log.i(TAG, "🔔 SCHEDULED REMINDERS:");
        Log.i(TAG, "   1️⃣  1 day before");
        Log.i(TAG, "   2️⃣  1 hour before");
        Log.i(TAG, "   3️⃣  15 minutes before");
        Log.i(TAG, "   4️⃣  At hearing time");
        Log.i(TAG, "═══════════════════════════════════════");
    }
}
