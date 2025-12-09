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
import com.example.blottermanagementsystem.data.entity.Evidence;
import com.example.blottermanagementsystem.ui.adapters.EvidenceListAdapter;
import com.example.blottermanagementsystem.utils.NetworkMonitor;
import com.example.blottermanagementsystem.utils.ApiClient;
import com.google.android.material.card.MaterialCardView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class ViewEvidenceDialogFragment extends DialogFragment {
    
    private int reportId;
    private RecyclerView rvEvidenceList;
    private TextView tvEmptyState;
    private MaterialCardView cardContainer;
    private EvidenceListAdapter adapter;
    private List<Evidence> evidenceList = new ArrayList<>();
    private NetworkMonitor networkMonitor;
    
    public static ViewEvidenceDialogFragment newInstance(int reportId) {
        ViewEvidenceDialogFragment fragment = new ViewEvidenceDialogFragment();
        Bundle args = new Bundle();
        args.putInt("report_id", reportId);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Dialog_MinWidth);
        networkMonitor = new NetworkMonitor(getContext());
        if (getArguments() != null) {
            reportId = getArguments().getInt("report_id");
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_view_evidence, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        loadEvidence();
    }
    
    private void initViews(View view) {
        cardContainer = view.findViewById(R.id.cardEvidenceContainer);
        rvEvidenceList = view.findViewById(R.id.rvEvidenceList);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        
        rvEvidenceList.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new EvidenceListAdapter(evidenceList, true); // true = read-only mode
        rvEvidenceList.setAdapter(adapter);
    }
    
    private void loadEvidence() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                BlotterDatabase database = BlotterDatabase.getDatabase(getContext());
                if (database != null) {
                    evidenceList.clear();
                    evidenceList.addAll(database.evidenceDao().getEvidenceByReport(reportId));
                    
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (evidenceList.isEmpty()) {
                                tvEmptyState.setVisibility(View.VISIBLE);
                                rvEvidenceList.setVisibility(View.GONE);
                            } else {
                                tvEmptyState.setVisibility(View.GONE);
                                rvEvidenceList.setVisibility(View.VISIBLE);
                                adapter.notifyDataSetChanged();
                            }
                        });
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("ViewEvidence", "Error loading evidence: " + e.getMessage());
            }
        });
    }
}
