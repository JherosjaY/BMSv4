package com.example.blottermanagementsystem.ui.dialogs;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import android.widget.CalendarView;

import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.database.BlotterDatabase;
import com.example.blottermanagementsystem.data.entity.Hearing;
import com.example.blottermanagementsystem.data.api.ApiClient;
import com.example.blottermanagementsystem.utils.NetworkMonitor;
import com.example.blottermanagementsystem.utils.PreferencesManager;
import com.example.blottermanagementsystem.utils.HearingReminderManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class ScheduleHearingDialogFragment extends DialogFragment {

    private EditText etHearingDate, etHearingTime, etHearingLocation, etHearingPurpose, etPresidingOfficer, etHearingNotes;
    private MaterialButton btnSchedule;
    private int reportId;
    private OnHearingSavedListener listener;
    private Calendar selectedDate;

    public interface OnHearingSavedListener {
        void onHearingSaved(Hearing hearing);
    }

    public static ScheduleHearingDialogFragment newInstance(int reportId, OnHearingSavedListener listener) {
        ScheduleHearingDialogFragment fragment = new ScheduleHearingDialogFragment();
        Bundle args = new Bundle();
        args.putInt("report_id", reportId);
        fragment.setArguments(args);
        fragment.listener = listener;
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Use transparent background to show the MaterialCardView properly
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Dialog_MinWidth);
        if (getArguments() != null) {
            reportId = getArguments().getInt("report_id");
        }
    }
    
    @Override
    public void onStart() {
        super.onStart();
        // Make dialog background transparent to show MaterialCardView
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            // Set dialog to match parent width with padding
            android.view.WindowManager.LayoutParams params = getDialog().getWindow().getAttributes();
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
            getDialog().getWindow().setAttributes(params);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_schedule_hearing, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupListeners();
    }

    private void initViews(View view) {
        etHearingDate = view.findViewById(R.id.etHearingDate);
        etHearingTime = view.findViewById(R.id.etHearingTime);
        etHearingLocation = view.findViewById(R.id.etHearingLocation);
        etHearingPurpose = view.findViewById(R.id.etHearingPurpose);
        etPresidingOfficer = view.findViewById(R.id.etPresidingOfficer);
        etHearingNotes = view.findViewById(R.id.etHearingNotes);
        btnSchedule = view.findViewById(R.id.btnScheduleHearing);
        
        // ✅ Initialize calendar with current date/time
        selectedDate = Calendar.getInstance();
        
        // ✅ Setup Purpose dropdown with predefined options
        setupPurposeDropdown();
        
        // ✅ Auto-fill Presiding Officer with current officer name
        autoFillPresidingOfficer();
    }
    
    private void setupPurposeDropdown() {
        // ✅ Predefined hearing purposes
        List<String> purposes = new ArrayList<>();
        purposes.add("Settlement");
        purposes.add("Investigation");
        purposes.add("Mediation");
        purposes.add("Hearing");
        purposes.add("Preliminary Inquiry");
        purposes.add("Conciliation");
        purposes.add("Other");
        
        // ✅ Create adapter for dropdown
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            getContext(),
            android.R.layout.simple_dropdown_item_1line,
            purposes
        );
        
        // ✅ Set adapter to MaterialAutoCompleteTextView
        if (etHearingPurpose instanceof MaterialAutoCompleteTextView) {
            MaterialAutoCompleteTextView autoCompleteTextView = (MaterialAutoCompleteTextView) etHearingPurpose;
            autoCompleteTextView.setAdapter(adapter);
        }
    }
    
    private void autoFillPresidingOfficer() {
        // ✅ FIXED: Get CURRENT officer's name (the one creating the hearing)
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                PreferencesManager prefs = new PreferencesManager(getContext());
                int currentOfficerId = prefs.getUserId();
                
                // Get current officer's name from preferences
                String firstName = prefs.getFirstName();
                String lastName = prefs.getLastName();
                
                String officerName = null;
                
                // If name is in preferences, use it
                if (firstName != null && !firstName.isEmpty() && lastName != null && !lastName.isEmpty()) {
                    officerName = firstName + " " + lastName;
                    android.util.Log.d("ScheduleHearing", "✅ Using officer name from preferences: " + officerName);
                } else {
                    // Otherwise, fetch from database
                    BlotterDatabase database = BlotterDatabase.getDatabase(getContext());
                    com.example.blottermanagementsystem.data.entity.User officer = 
                        database.userDao().getUserById(currentOfficerId);
                    
                    if (officer != null) {
                        String dbFirstName = officer.getFirstName();
                        String dbLastName = officer.getLastName();
                        if (dbFirstName != null && !dbFirstName.isEmpty() && dbLastName != null && !dbLastName.isEmpty()) {
                            officerName = dbFirstName + " " + dbLastName;
                            android.util.Log.d("ScheduleHearing", "✅ Using officer name from database: " + officerName);
                        }
                    }
                }
                
                final String finalOfficerName = officerName;
                getActivity().runOnUiThread(() -> {
                    if (finalOfficerName != null && !finalOfficerName.isEmpty()) {
                        etPresidingOfficer.setText(finalOfficerName);
                        android.util.Log.d("ScheduleHearing", "✅ Presiding Officer set to: " + finalOfficerName);
                    } else {
                        etPresidingOfficer.setText("Officer");
                        android.util.Log.w("ScheduleHearing", "⚠️ Could not determine officer name, using default");
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("ScheduleHearing", "Error fetching officer name: " + e.getMessage());
                getActivity().runOnUiThread(() -> {
                    etPresidingOfficer.setText("Officer");
                });
            }
        });
    }

    private void setupListeners() {
        btnSchedule.setOnClickListener(v -> saveHearing());
        etHearingDate.setOnClickListener(v -> showDatePicker());
        etHearingTime.setOnClickListener(v -> showTimePicker());
    }
    
    private void showDatePicker() {
        // ✅ Use standard DatePickerDialog with professional theme (like Add Report)
        android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(
            getContext(),
            R.style.Theme_App_DatePickerDialog,
            (view, year, month, dayOfMonth) -> {
                selectedDate.set(year, month, dayOfMonth);
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                etHearingDate.setText(dateFormat.format(selectedDate.getTime()));
                android.util.Log.d("DatePicker", "✅ Hearing date set to: " + dateFormat.format(selectedDate.getTime()));
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        
        // ✅ Set minimum date to TOMORROW - prevent selecting today and past dates
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_MONTH, 1); // Add 1 day to get tomorrow
        tomorrow.set(Calendar.HOUR_OF_DAY, 0);
        tomorrow.set(Calendar.MINUTE, 0);
        tomorrow.set(Calendar.SECOND, 0);
        tomorrow.set(Calendar.MILLISECOND, 0);
        datePickerDialog.getDatePicker().setMinDate(tomorrow.getTimeInMillis());
        android.util.Log.d("DatePicker", "✅ Today and past dates disabled - minimum date set to TOMORROW");
        
        
        datePickerDialog.show();
    }

    private void showTimePicker() {
        // ✅ Use standard TimePickerDialog with professional theme (like Add Report)
        android.app.TimePickerDialog timePickerDialog = new android.app.TimePickerDialog(
            getContext(),
            R.style.Theme_App_TimePickerDialog,
            (view, hourOfDay, minute) -> {
                selectedDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selectedDate.set(Calendar.MINUTE, minute);
                SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                etHearingTime.setText(timeFormat.format(selectedDate.getTime()));
                android.util.Log.d("TimePicker", "✅ Hearing time set to: " + timeFormat.format(selectedDate.getTime()));
            },
            selectedDate.get(Calendar.HOUR_OF_DAY),
            selectedDate.get(Calendar.MINUTE),
            false // 12-hour format
        );
        
        
        timePickerDialog.show();
    }

    private void saveHearing() {
        String date = etHearingDate.getText().toString().trim();
        String time = etHearingTime.getText().toString().trim();
        String location = etHearingLocation.getText().toString().trim();
        String purpose = etHearingPurpose.getText().toString().trim();

        if (date.isEmpty() || time.isEmpty() || location.isEmpty() || purpose.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Hearing hearing = new Hearing();
        hearing.setBlotterReportId(reportId);
        hearing.setHearingDate(date);
        hearing.setHearingTime(time);
        hearing.setLocation(location);
        hearing.setPurpose(purpose);
        hearing.setCreatedAt(System.currentTimeMillis());
        
        // ✅ Save presiding officer name
        String presidingOfficer = etPresidingOfficer.getText().toString().trim();
        if (!presidingOfficer.isEmpty()) {
            hearing.setPresidingOfficer(presidingOfficer);
        }

        // Save to database in background thread
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                BlotterDatabase database = BlotterDatabase.getDatabase(getContext());
                if (database != null) {
                    long id = database.hearingDao().insertHearing(hearing);
                    hearing.setId((int) id);
                    
                    // ✅ FIXED: Add officer ID to assigned list when creating hearing
                    PreferencesManager prefs = new PreferencesManager(getContext());
                    int currentOfficerId = prefs.getUserId();
                    
                    // Get the report and update assigned officers
                    com.example.blottermanagementsystem.data.entity.BlotterReport report = 
                        database.blotterReportDao().getReportById(reportId);
                    
                    if (report != null && currentOfficerId != -1) {
                        String currentAssignedIds = report.getAssignedOfficerIds();
                        
                        // Check if officer ID is already in the list
                        boolean alreadyAssigned = false;
                        if (currentAssignedIds != null && !currentAssignedIds.trim().isEmpty()) {
                            String[] ids = currentAssignedIds.split(",");
                            for (String idStr : ids) {
                                if (Integer.parseInt(idStr.trim()) == currentOfficerId) {
                                    alreadyAssigned = true;
                                    break;
                                }
                            }
                        }
                        
                        // Add officer ID if not already assigned
                        if (!alreadyAssigned) {
                            if (currentAssignedIds == null || currentAssignedIds.trim().isEmpty()) {
                                report.setAssignedOfficerIds(String.valueOf(currentOfficerId));
                            } else {
                                report.setAssignedOfficerIds(currentAssignedIds + "," + currentOfficerId);
                            }
                            
                            // Update report in database
                            database.blotterReportDao().updateReport(report);
                            android.util.Log.d("ScheduleHearing", "✅ Added officer " + currentOfficerId + " to assigned list for report " + reportId);
                        } else {
                            android.util.Log.d("ScheduleHearing", "ℹ️ Officer " + currentOfficerId + " already in assigned list");
                        }
                    }
                    
                    // ✅ Schedule reminder notifications (1 day, 1 hour, 15 min, at time)
                    HearingReminderManager.scheduleHearingReminders(getContext(), hearing);
                    android.util.Log.i("ScheduleHearing", "✅ Reminders scheduled for hearing " + hearing.getId());
                    
                    // ✅ FIXED: Refresh investigation timeline to enable "Document Resolution" button
                    // When hearing time is set, the next step (Document Resolution) should auto-enable
                    if (getActivity() instanceof com.example.blottermanagementsystem.ui.activities.OfficerCaseDetailActivity) {
                        com.example.blottermanagementsystem.ui.activities.OfficerCaseDetailActivity activity = 
                            (com.example.blottermanagementsystem.ui.activities.OfficerCaseDetailActivity) getActivity();
                        activity.refreshInvestigationTimeline();
                        android.util.Log.d("ScheduleHearing", "✅ Investigation timeline refreshed - Document Resolution button should now be enabled");
                    }
                    
                    // Sync to API if network available
                    NetworkMonitor networkMonitor = new NetworkMonitor(getContext());
                    if (networkMonitor.isNetworkAvailable()) {
                        ApiClient.getApiService().createHearing(hearing).enqueue(new Callback<Object>() {
                            @Override
                            public void onResponse(Call<Object> call, Response<Object> response) {
                                if (response.isSuccessful()) {
                                    android.util.Log.d("ScheduleHearing", "Synced to API");
                                }
                            }
                            
                            @Override
                            public void onFailure(Call<Object> call, Throwable t) {
                                android.util.Log.w("ScheduleHearing", "API sync failed: " + t.getMessage());
                            }
                        });
                    }
                    
                    getActivity().runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onHearingSaved(hearing);
                        }
                        Toast.makeText(getContext(), "Hearing scheduled successfully", Toast.LENGTH_SHORT).show();
                        dismiss();
                    });
                }
            } catch (Exception e) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Error saving hearing: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
