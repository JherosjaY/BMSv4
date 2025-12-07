package com.example.blottermanagementsystem.ui.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.database.BlotterDatabase;
import com.example.blottermanagementsystem.data.entity.Hearing;
import com.example.blottermanagementsystem.ui.adapters.HearingListAdapter;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class ViewHearingsDialogFragment extends DialogFragment {
    
    private int reportId;
    private RecyclerView rvHearingList;
    private TextView tvEmptyState, btnEditAllHearings;
    private MaterialCardView cardContainer;
    private HearingListAdapter adapter;
    private List<Hearing> hearingList = new ArrayList<>();
    
    public static ViewHearingsDialogFragment newInstance(int reportId) {
        ViewHearingsDialogFragment fragment = new ViewHearingsDialogFragment();
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
        return inflater.inflate(R.layout.dialog_view_hearings, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        loadHearings();
    }
    
    private void initViews(View view) {
        cardContainer = view.findViewById(R.id.cardHearingContainer);
        rvHearingList = view.findViewById(R.id.rvHearingList);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        btnEditAllHearings = view.findViewById(R.id.btnEditAllHearings);
        
        // ✅ Show edit button ONLY if case status is "Scheduled"
        if (btnEditAllHearings != null) {
            // Check case status - only show edit if "Scheduled"
            BlotterDatabase database = BlotterDatabase.getDatabase(getContext());
            if (database != null) {
                Executors.newSingleThreadExecutor().execute(() -> {
                    try {
                        com.example.blottermanagementsystem.data.entity.BlotterReport report = 
                            database.blotterReportDao().getReportById(reportId);
                        
                        boolean canEdit = report != null && 
                            report.getStatus() != null && 
                            report.getStatus().equalsIgnoreCase("SCHEDULED");
                        
                        getActivity().runOnUiThread(() -> {
                            if (canEdit) {
                                btnEditAllHearings.setVisibility(View.VISIBLE);
                                btnEditAllHearings.setOnClickListener(v -> {
                                    if (!hearingList.isEmpty()) {
                                        Hearing hearingToEdit = hearingList.get(0);
                                        EditHearingDialogFragment editDialog = EditHearingDialogFragment.newInstance(hearingToEdit, updatedHearing -> {
                                            loadHearings();
                                        });
                                        editDialog.show(getChildFragmentManager(), "EditHearing");
                                    } else {
                                        android.widget.Toast.makeText(getContext(), "No hearings to edit", android.widget.Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } else {
                                // ✅ Hide edit button if case is Resolved, Settled, or other status
                                btnEditAllHearings.setVisibility(View.GONE);
                            }
                        });
                    } catch (Exception e) {
                        android.util.Log.e("ViewHearings", "Error checking case status: " + e.getMessage());
                    }
                });
            }
        }
        
        rvHearingList.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new HearingListAdapter(hearingList, false); // false = editable
        
        // ✅ Set edit listener to open edit dialog (for individual hearing cards)
        adapter.setEditListener(hearing -> {
            // Open edit hearing dialog with callback
            EditHearingDialogFragment editDialog = EditHearingDialogFragment.newInstance(hearing, updatedHearing -> {
                // Refresh hearings after edit
                loadHearings();
            });
            editDialog.show(getChildFragmentManager(), "EditHearing");
        });
        
        rvHearingList.setAdapter(adapter);
    }
    
    private void loadHearings() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                BlotterDatabase database = BlotterDatabase.getDatabase(getContext());
                if (database != null) {
                    hearingList.clear();
                    hearingList.addAll(database.hearingDao().getHearingsByReport(reportId));
                    
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (hearingList.isEmpty()) {
                                tvEmptyState.setVisibility(View.VISIBLE);
                                rvHearingList.setVisibility(View.GONE);
                            } else {
                                tvEmptyState.setVisibility(View.GONE);
                                rvHearingList.setVisibility(View.VISIBLE);
                                adapter.notifyDataSetChanged();
                            }
                        });
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("ViewHearings", "Error loading hearings: " + e.getMessage());
            }
        });
    }
}
