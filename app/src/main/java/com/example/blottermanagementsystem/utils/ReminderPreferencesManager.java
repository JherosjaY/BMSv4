package com.example.blottermanagementsystem.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * ✅ Manager for user reminder preferences
 * Allows users to customize reminder times and enable/disable reminders
 */
public class ReminderPreferencesManager {
    
    private static final String TAG = "ReminderPreferences";
    private static final String PREFS_NAME = "hearing_reminders_prefs";
    
    // Preference keys
    private static final String KEY_REMINDERS_ENABLED = "reminders_enabled";
    private static final String KEY_ONE_DAY_BEFORE = "one_day_before";
    private static final String KEY_ONE_HOUR_BEFORE = "one_hour_before";
    private static final String KEY_FIFTEEN_MIN_BEFORE = "fifteen_min_before";
    private static final String KEY_AT_TIME = "at_time";
    private static final String KEY_SOUND_ENABLED = "sound_enabled";
    private static final String KEY_VIBRATION_ENABLED = "vibration_enabled";
    
    private final SharedPreferences prefs;
    
    public ReminderPreferencesManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Check if reminders are globally enabled
     */
    public boolean areRemindersEnabled() {
        return prefs.getBoolean(KEY_REMINDERS_ENABLED, true);
    }
    
    /**
     * Enable/disable all reminders
     */
    public void setRemindersEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_REMINDERS_ENABLED, enabled).apply();
        Log.i(TAG, "🔔 Reminders " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Check if 1-day-before reminder is enabled
     */
    public boolean isOneDayBeforeEnabled() {
        return prefs.getBoolean(KEY_ONE_DAY_BEFORE, true);
    }
    
    /**
     * Set 1-day-before reminder
     */
    public void setOneDayBefore(boolean enabled) {
        prefs.edit().putBoolean(KEY_ONE_DAY_BEFORE, enabled).apply();
        Log.i(TAG, "📅 1-day reminder " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Check if 1-hour-before reminder is enabled
     */
    public boolean isOneHourBeforeEnabled() {
        return prefs.getBoolean(KEY_ONE_HOUR_BEFORE, true);
    }
    
    /**
     * Set 1-hour-before reminder
     */
    public void setOneHourBefore(boolean enabled) {
        prefs.edit().putBoolean(KEY_ONE_HOUR_BEFORE, enabled).apply();
        Log.i(TAG, "⏰ 1-hour reminder " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Check if 15-minute-before reminder is enabled
     */
    public boolean isFifteenMinBeforeEnabled() {
        return prefs.getBoolean(KEY_FIFTEEN_MIN_BEFORE, true);
    }
    
    /**
     * Set 15-minute-before reminder
     */
    public void setFifteenMinBefore(boolean enabled) {
        prefs.edit().putBoolean(KEY_FIFTEEN_MIN_BEFORE, enabled).apply();
        Log.i(TAG, "⏱️ 15-min reminder " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Check if at-time reminder is enabled
     */
    public boolean isAtTimeEnabled() {
        return prefs.getBoolean(KEY_AT_TIME, true);
    }
    
    /**
     * Set at-time reminder
     */
    public void setAtTime(boolean enabled) {
        prefs.edit().putBoolean(KEY_AT_TIME, enabled).apply();
        Log.i(TAG, "🔔 At-time reminder " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Check if notification sound is enabled
     */
    public boolean isSoundEnabled() {
        return prefs.getBoolean(KEY_SOUND_ENABLED, true);
    }
    
    /**
     * Set notification sound
     */
    public void setSoundEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply();
        Log.i(TAG, "🔊 Sound " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Check if vibration is enabled
     */
    public boolean isVibrationEnabled() {
        return prefs.getBoolean(KEY_VIBRATION_ENABLED, true);
    }
    
    /**
     * Set vibration
     */
    public void setVibrationEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply();
        Log.i(TAG, "📳 Vibration " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Reset all preferences to defaults
     */
    public void resetToDefaults() {
        prefs.edit().clear().apply();
        Log.i(TAG, "🔄 Preferences reset to defaults");
    }
    
    /**
     * Log all current preferences
     */
    public void logPreferences() {
        Log.i(TAG, "═══════════════════════════════════════");
        Log.i(TAG, "📋 REMINDER PREFERENCES");
        Log.i(TAG, "═══════════════════════════════════════");
        Log.i(TAG, "Reminders enabled: " + areRemindersEnabled());
        Log.i(TAG, "1-day reminder: " + isOneDayBeforeEnabled());
        Log.i(TAG, "1-hour reminder: " + isOneHourBeforeEnabled());
        Log.i(TAG, "15-min reminder: " + isFifteenMinBeforeEnabled());
        Log.i(TAG, "At-time reminder: " + isAtTimeEnabled());
        Log.i(TAG, "Sound enabled: " + isSoundEnabled());
        Log.i(TAG, "Vibration enabled: " + isVibrationEnabled());
        Log.i(TAG, "═══════════════════════════════════════");
    }
}
