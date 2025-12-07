package com.example.blottermanagementsystem.ui.activities;

import android.os.Bundle;
import android.widget.CompoundButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.utils.ReminderPreferencesManager;
import com.google.android.material.appbar.MaterialToolbar;

/**
 * ✅ Settings Activity for customizing hearing reminder preferences
 */
public class ReminderSettingsActivity extends AppCompatActivity {
    
    private ReminderPreferencesManager prefsManager;
    private SwitchCompat switchRemindersEnabled;
    private SwitchCompat switchOneDayBefore;
    private SwitchCompat switchOneHourBefore;
    private SwitchCompat switchFifteenMinBefore;
    private SwitchCompat switchAtTime;
    private SwitchCompat switchSound;
    private SwitchCompat switchVibration;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder_settings);
        
        prefsManager = new ReminderPreferencesManager(this);
        
        setupToolbar();
        initializeViews();
        loadPreferences();
        setupListeners();
    }
    
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Reminder Settings");
        }
    }
    
    private void initializeViews() {
        switchRemindersEnabled = findViewById(R.id.switchRemindersEnabled);
        switchOneDayBefore = findViewById(R.id.switchOneDayBefore);
        switchOneHourBefore = findViewById(R.id.switchOneHourBefore);
        switchFifteenMinBefore = findViewById(R.id.switchFifteenMinBefore);
        switchAtTime = findViewById(R.id.switchAtTime);
        switchSound = findViewById(R.id.switchSound);
        switchVibration = findViewById(R.id.switchVibration);
    }
    
    private void loadPreferences() {
        switchRemindersEnabled.setChecked(prefsManager.areRemindersEnabled());
        switchOneDayBefore.setChecked(prefsManager.isOneDayBeforeEnabled());
        switchOneHourBefore.setChecked(prefsManager.isOneHourBeforeEnabled());
        switchFifteenMinBefore.setChecked(prefsManager.isFifteenMinBeforeEnabled());
        switchAtTime.setChecked(prefsManager.isAtTimeEnabled());
        switchSound.setChecked(prefsManager.isSoundEnabled());
        switchVibration.setChecked(prefsManager.isVibrationEnabled());
        
        // Disable individual reminders if global is disabled
        updateReminderStates();
    }
    
    private void setupListeners() {
        switchRemindersEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefsManager.setRemindersEnabled(isChecked);
            updateReminderStates();
        });
        
        switchOneDayBefore.setOnCheckedChangeListener((buttonView, isChecked) -> 
            prefsManager.setOneDayBefore(isChecked));
        
        switchOneHourBefore.setOnCheckedChangeListener((buttonView, isChecked) -> 
            prefsManager.setOneHourBefore(isChecked));
        
        switchFifteenMinBefore.setOnCheckedChangeListener((buttonView, isChecked) -> 
            prefsManager.setFifteenMinBefore(isChecked));
        
        switchAtTime.setOnCheckedChangeListener((buttonView, isChecked) -> 
            prefsManager.setAtTime(isChecked));
        
        switchSound.setOnCheckedChangeListener((buttonView, isChecked) -> 
            prefsManager.setSoundEnabled(isChecked));
        
        switchVibration.setOnCheckedChangeListener((buttonView, isChecked) -> 
            prefsManager.setVibrationEnabled(isChecked));
    }
    
    private void updateReminderStates() {
        boolean remindersEnabled = prefsManager.areRemindersEnabled();
        switchOneDayBefore.setEnabled(remindersEnabled);
        switchOneHourBefore.setEnabled(remindersEnabled);
        switchFifteenMinBefore.setEnabled(remindersEnabled);
        switchAtTime.setEnabled(remindersEnabled);
        switchSound.setEnabled(remindersEnabled);
        switchVibration.setEnabled(remindersEnabled);
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
