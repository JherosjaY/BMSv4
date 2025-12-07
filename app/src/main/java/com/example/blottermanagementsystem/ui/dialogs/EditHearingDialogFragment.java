package com.example.blottermanagementsystem.ui.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.database.BlotterDatabase;
import com.example.blottermanagementsystem.data.entity.Hearing;
import com.example.blottermanagementsystem.utils.NotificationHelper;
import com.example.blottermanagementsystem.utils.HearingReminderManager;
import com.google.android.material.button.MaterialButton;

import java.util.concurrent.Executors;

/**
 * ✅ EDIT/RESCHEDULE HEARING DIALOG
 * Allows officers to reschedule hearings when status is "Scheduled"
 */
public class EditHearingDialogFragment extends DialogFragment {

    private EditText etHearingDate, etHearingTime, etHearingLocation;
    private MaterialButton btnSave, btnCancel;
    private Hearing hearing;
    private OnHearingUpdatedListener listener;

    public interface OnHearingUpdatedListener {
        void onHearingUpdated(Hearing hearing);
    }

    public static EditHearingDialogFragment newInstance(Hearing hearing, OnHearingUpdatedListener listener) {
        EditHearingDialogFragment fragment = new EditHearingDialogFragment();
        fragment.hearing = hearing;
        fragment.listener = listener;
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Dialog_MinWidth);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            android.view.WindowManager.LayoutParams params = getDialog().getWindow().getAttributes();
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
            getDialog().getWindow().setAttributes(params);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_edit_hearing, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        populateFields();
        setupListeners();
    }

    private void initViews(View view) {
        etHearingDate = view.findViewById(R.id.etHearingDate);
        etHearingTime = view.findViewById(R.id.etHearingTime);
        etHearingLocation = view.findViewById(R.id.etHearingLocation);
        btnSave = view.findViewById(R.id.btnSaveHearing);
        btnCancel = view.findViewById(R.id.btnCancelHearing);
    }

    private void populateFields() {
        if (hearing != null) {
            etHearingDate.setText(hearing.getHearingDate() != null ? hearing.getHearingDate() : "");
            etHearingTime.setText(hearing.getHearingTime() != null ? hearing.getHearingTime() : "");
            etHearingLocation.setText(hearing.getLocation() != null ? hearing.getLocation() : "");
        }
    }

    private void setupListeners() {
        // ✅ Make fields non-focusable to prevent keyboard
        etHearingDate.setFocusable(false);
        etHearingTime.setFocusable(false);
        
        // ✅ Single tap on field opens picker
        etHearingDate.setOnClickListener(v -> showDatePicker());
        etHearingTime.setOnClickListener(v -> showTimePicker());
        
        btnSave.setOnClickListener(v -> saveChanges());
        btnCancel.setOnClickListener(v -> dismiss());
    }

    private void showDatePicker() {
        android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(
            getContext(),
            R.style.Theme_App_DatePickerDialog,
            (view, year, month, dayOfMonth) -> {
                java.util.Calendar calendar = java.util.Calendar.getInstance();
                calendar.set(year, month, dayOfMonth);
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
                etHearingDate.setText(sdf.format(calendar.getTime()));
            },
            java.util.Calendar.getInstance().get(java.util.Calendar.YEAR),
            java.util.Calendar.getInstance().get(java.util.Calendar.MONTH),
            java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)
        );
        
        // Set minimum date to tomorrow
        java.util.Calendar tomorrow = java.util.Calendar.getInstance();
        tomorrow.add(java.util.Calendar.DAY_OF_MONTH, 1);
        tomorrow.set(java.util.Calendar.HOUR_OF_DAY, 0);
        tomorrow.set(java.util.Calendar.MINUTE, 0);
        tomorrow.set(java.util.Calendar.SECOND, 0);
        tomorrow.set(java.util.Calendar.MILLISECOND, 0);
        datePickerDialog.getDatePicker().setMinDate(tomorrow.getTimeInMillis());
        
        datePickerDialog.show();
    }

    private void showTimePicker() {
        android.app.TimePickerDialog timePickerDialog = new android.app.TimePickerDialog(
            getContext(),
            R.style.Theme_App_TimePickerDialog,
            (view, hourOfDay, minute) -> {
                java.util.Calendar calendar = java.util.Calendar.getInstance();
                calendar.set(java.util.Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(java.util.Calendar.MINUTE, minute);
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault());
                etHearingTime.setText(sdf.format(calendar.getTime()));
            },
            java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY),
            java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE),
            false
        );
        timePickerDialog.show();
    }

    private void saveChanges() {
        String date = etHearingDate.getText().toString().trim();
        String time = etHearingTime.getText().toString().trim();
        String location = etHearingLocation.getText().toString().trim();

        if (date.isEmpty() || time.isEmpty() || location.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update hearing object
        hearing.setHearingDate(date);
        hearing.setHearingTime(time);
        hearing.setLocation(location);

        // Save to database in background
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                BlotterDatabase database = BlotterDatabase.getDatabase(getContext());
                if (database != null) {
                    database.hearingDao().updateHearing(hearing);
                    android.util.Log.d("EditHearing", "✅ Hearing rescheduled: " + hearing.getId());

                    // ✅ RESCHEDULE REMINDERS for new date/time
                    try {
                        HearingReminderManager.rescheduleHearingReminders(getContext(), hearing);
                        android.util.Log.d("EditHearing", "✅ Reminders rescheduled for hearing " + hearing.getId());
                    } catch (Exception e) {
                        android.util.Log.e("EditHearing", "Error rescheduling reminders: " + e.getMessage());
                    }

                    // ✅ NOTIFY COMPLAINANT via push notification
                    NotificationHelper notificationHelper = new NotificationHelper(getContext());
                    String rescheduleMessage = "Hearing rescheduled to " + date + " at " + time + " at " + location;
                    
                    // Get user IDs to notify (complainant who filed the case)
                    java.util.List<Integer> userIdsToNotify = new java.util.ArrayList<>();
                    String caseNumber = "Case #TBD";
                    try {
                        com.example.blottermanagementsystem.data.entity.BlotterReport report = 
                            database.blotterReportDao().getReportById(hearing.getBlotterReportId());
                        if (report != null) {
                            // ✅ Get the user who filed this case
                            if (report.getUserId() > 0) {
                                userIdsToNotify.add(report.getUserId());
                            }
                            // ✅ Get the actual case number
                            if (report.getCaseNumber() != null && !report.getCaseNumber().isEmpty()) {
                                caseNumber = "Case #" + report.getCaseNumber();
                            }
                            android.util.Log.d("EditHearing", "✅ Notifying user " + report.getUserId() + " for " + caseNumber);
                        }
                    } catch (Exception e) {
                        android.util.Log.e("EditHearing", "Error getting report: " + e.getMessage());
                    }
                    
                    // ✅ Notify the user who filed the case
                    if (!userIdsToNotify.isEmpty()) {
                        notificationHelper.notifyHearingScheduled(
                            userIdsToNotify,
                            caseNumber,
                            date + " at " + time,
                            hearing.getBlotterReportId(),
                            "Officer"
                        );
                        android.util.Log.d("EditHearing", "✅ Notification sent to " + userIdsToNotify.size() + " user(s)");
                    }

                    getActivity().runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onHearingUpdated(hearing);
                        }
                        Toast.makeText(getContext(), "Hearing rescheduled successfully", Toast.LENGTH_SHORT).show();
                        dismiss();
                    });
                }
            } catch (Exception e) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Error updating hearing: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
