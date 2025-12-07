package com.example.blottermanagementsystem.ui.fragments;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.database.BlotterDatabase;
import com.example.blottermanagementsystem.data.entity.Hearing;
import com.example.blottermanagementsystem.data.entity.BlotterReport;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import java.util.concurrent.Executors;

/**
 * ✅ PROFESSIONAL HEARING DETAILS DIALOG
 * Shows full hearing information in a beautiful bottom sheet
 * Includes Edit button for rescheduling (officers only, Scheduled status)
 */
public class ViewHearingDetailsDialogFragment extends BottomSheetDialogFragment {

    private Hearing hearing;
    private TextView tvCaseNumber, tvPurpose, tvDate, tvTime, tvLocation, tvOfficer, tvClose;
    private Chip chipStatus;

    public static ViewHearingDetailsDialogFragment newInstance(Hearing hearing) {
        ViewHearingDetailsDialogFragment fragment = new ViewHearingDetailsDialogFragment();
        fragment.hearing = hearing;
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_view_hearing_details, container, false);
        // ✅ Add slide-up animation
        view.startAnimation(android.view.animation.AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up));
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        try {
            initializeViews(view);
            populateData();
            setupListeners();
        } catch (Exception e) {
            android.util.Log.e("ViewHearingDetails", "Error: " + e.getMessage(), e);
        }
    }

    private void initializeViews(View view) {
        tvCaseNumber = view.findViewById(R.id.tvCaseNumber);
        tvPurpose = view.findViewById(R.id.tvPurpose);
        tvDate = view.findViewById(R.id.tvDate);
        tvTime = view.findViewById(R.id.tvTime);
        tvLocation = view.findViewById(R.id.tvLocation);
        tvOfficer = view.findViewById(R.id.tvOfficer);
        chipStatus = view.findViewById(R.id.chipStatus);
        tvClose = view.findViewById(R.id.ivClose);
    }

    private void populateData() {
        if (hearing == null) return;

        // ✅ Case Number / Title - Fetch actual case number from database
        if (tvCaseNumber != null) {
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    BlotterDatabase database = BlotterDatabase.getDatabase(requireContext());
                    BlotterReport report = database.blotterReportDao().getReportById(hearing.getBlotterReportId());
                    
                    String caseNumber = "Case #" + (report != null && report.getCaseNumber() != null ? report.getCaseNumber() : hearing.getBlotterReportId());
                    
                    requireActivity().runOnUiThread(() -> {
                        if (tvCaseNumber != null) {
                            tvCaseNumber.setText(caseNumber);
                        }
                    });
                } catch (Exception e) {
                    android.util.Log.e("ViewHearingDetails", "Error fetching case number: " + e.getMessage());
                }
            });
        }

        // ✅ Purpose
        if (tvPurpose != null) {
            String purpose = hearing.getPurpose() != null ? hearing.getPurpose() : "No purpose specified";
            tvPurpose.setText(purpose);
        }

        // ✅ Date
        if (tvDate != null) {
            String date = hearing.getHearingDate() != null ? hearing.getHearingDate() : "TBD";
            tvDate.setText(date);
        }

        // ✅ Time
        if (tvTime != null) {
            String time = hearing.getHearingTime() != null ? hearing.getHearingTime() : "TBD";
            tvTime.setText(time);
        }

        // ✅ Location
        if (tvLocation != null) {
            String location = hearing.getLocation() != null ? hearing.getLocation() : "Location TBD";
            tvLocation.setText(location);
        }

        // ✅ Officer - Display actual presiding officer name from hearing
        if (tvOfficer != null) {
            String officerName = hearing.getPresidingOfficer() != null && !hearing.getPresidingOfficer().isEmpty() 
                ? hearing.getPresidingOfficer() 
                : "Officer TBD";
            tvOfficer.setText(officerName);
            android.util.Log.d("ViewHearingDetails", "✅ Presiding Officer: " + officerName);
        }

        // ✅ Status Chip - Get from case status, not hearing status
        if (chipStatus != null) {
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    BlotterDatabase database = BlotterDatabase.getDatabase(requireContext());
                    BlotterReport report = database.blotterReportDao().getReportById(hearing.getBlotterReportId());
                    
                    String displayStatus = "Scheduled";  // Default
                    if (report != null && report.getStatus() != null) {
                        String caseStatus = report.getStatus().toUpperCase();
                        
                        // Map case status to hearing status
                        if (caseStatus.contains("RESOLVED") || caseStatus.contains("SETTLED")) {
                            displayStatus = "Completed";  // Case resolved = Hearing completed
                        } else if (caseStatus.contains("CANCELLED") || caseStatus.contains("WITHDRAWN")) {
                            displayStatus = "Cancelled";  // Case cancelled = Hearing cancelled
                        } else {
                            displayStatus = "Scheduled";  // Default
                        }
                    }
                    
                    String finalStatus = displayStatus;
                    requireActivity().runOnUiThread(() -> {
                        if (chipStatus != null) {
                            chipStatus.setText(finalStatus);
                            
                            // Color code the status
                            switch (finalStatus.toUpperCase()) {
                                case "SCHEDULED":
                                    chipStatus.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.electric_blue)));
                                    break;
                                case "COMPLETED":
                                    chipStatus.setChipBackgroundColor(ColorStateList.valueOf(0xFF22c55e));  // Green
                                    break;
                                case "CANCELLED":
                                    chipStatus.setChipBackgroundColor(ColorStateList.valueOf(0xFFef4444));  // Red
                                    break;
                                default:
                                    chipStatus.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.electric_blue)));
                                    break;
                            }
                        }
                    });
                } catch (Exception e) {
                    android.util.Log.e("ViewHearingDetails", "Error getting case status: " + e.getMessage());
                    requireActivity().runOnUiThread(() -> {
                        if (chipStatus != null) {
                            chipStatus.setText("Scheduled");
                            chipStatus.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.electric_blue)));
                        }
                    });
                }
            });
        }
    }

    private void setupListeners() {
        if (tvClose != null) {
            tvClose.setOnClickListener(v -> dismiss());
        }
    }
}
