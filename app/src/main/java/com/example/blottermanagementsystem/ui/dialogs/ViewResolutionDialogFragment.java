package com.example.blottermanagementsystem.ui.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.database.BlotterDatabase;
import com.example.blottermanagementsystem.data.entity.Resolution;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class ViewResolutionDialogFragment extends DialogFragment {
    
    private int reportId;
    private MaterialCardView cardContainer;
    private TextView tvEmptyState;
    private TextView tvResolutionType;
    private TextView tvDescription;
    private TextView tvDocumentedBy;
    private TextView tvDocumentedDate;
    private TextView tvNotes;
    
    public static ViewResolutionDialogFragment newInstance(int reportId) {
        ViewResolutionDialogFragment fragment = new ViewResolutionDialogFragment();
        Bundle args = new Bundle();
        args.putInt("report_id", reportId);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_FRAME, android.R.style.Theme_Material_Dialog_MinWidth);
        if (getArguments() != null) {
            reportId = getArguments().getInt("report_id");
        }
    }
    
    @Override
    public void onStart() {
        super.onStart();
        // ✅ Set dim effect (0.5f = 50% dim, looks nice)
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setDimAmount(0.5f);
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_view_resolution, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        loadResolution();
    }
    
    private void initViews(View view) {
        cardContainer = view.findViewById(R.id.cardResolutionContainer);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        tvResolutionType = view.findViewById(R.id.tvResolutionType);
        tvDescription = view.findViewById(R.id.tvResolutionDescription);
        tvDocumentedBy = view.findViewById(R.id.tvDocumentedBy);
        tvDocumentedDate = view.findViewById(R.id.tvDocumentedDate);
        tvNotes = view.findViewById(R.id.tvResolutionNotes);
    }
    
    private void loadResolution() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                BlotterDatabase database = BlotterDatabase.getDatabase(getContext());
                if (database != null) {
                    List<Resolution> resolutions = database.resolutionDao().getResolutionsByReport(reportId);
                    
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (resolutions.isEmpty()) {
                                tvEmptyState.setVisibility(View.VISIBLE);
                                cardContainer.setVisibility(View.GONE);
                            } else {
                                tvEmptyState.setVisibility(View.GONE);
                                cardContainer.setVisibility(View.VISIBLE);
                                
                                Resolution resolution = resolutions.get(0); // Get latest
                                tvResolutionType.setText(resolution.getResolutionType() != null ? resolution.getResolutionType() : "");
                                tvDescription.setText(resolution.getResolutionDetails() != null ? resolution.getResolutionDetails() : "");
                                tvDocumentedBy.setText("Documented by: Officer");
                                // ✅ Hide notes field - description is sufficient
                                tvNotes.setVisibility(View.GONE);
                                
                                // Format date
                                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
                                String dateStr = sdf.format(new Date(resolution.getCreatedAt()));
                                tvDocumentedDate.setText(dateStr);
                            }
                        });
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("ViewResolution", "Error loading resolution: " + e.getMessage());
            }
        });
    }
}
