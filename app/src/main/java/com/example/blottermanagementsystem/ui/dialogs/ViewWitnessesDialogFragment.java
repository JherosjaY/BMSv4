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
import com.example.blottermanagementsystem.data.entity.Witness;
import com.example.blottermanagementsystem.ui.adapters.WitnessListAdapter;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class ViewWitnessesDialogFragment extends DialogFragment {
    
    private int reportId;
    private RecyclerView rvWitnessList;
    private TextView tvEmptyState;
    private WitnessListAdapter adapter;
    private List<Witness> witnessList = new ArrayList<>();
    
    public static ViewWitnessesDialogFragment newInstance(int reportId) {
        ViewWitnessesDialogFragment fragment = new ViewWitnessesDialogFragment();
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
        return inflater.inflate(R.layout.dialog_view_witnesses, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        loadWitnesses();
    }
    
    private void initViews(View view) {
        rvWitnessList = view.findViewById(R.id.rvWitnessList);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        
        rvWitnessList.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new WitnessListAdapter(witnessList, true); // true = read-only
        rvWitnessList.setAdapter(adapter);
    }
    
    private void loadWitnesses() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                BlotterDatabase database = BlotterDatabase.getDatabase(getContext());
                if (database != null) {
                    witnessList.clear();
                    witnessList.addAll(database.witnessDao().getWitnessesByReport(reportId));
                    
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (witnessList.isEmpty()) {
                                tvEmptyState.setVisibility(View.VISIBLE);
                                rvWitnessList.setVisibility(View.GONE);
                            } else {
                                tvEmptyState.setVisibility(View.GONE);
                                rvWitnessList.setVisibility(View.VISIBLE);
                                adapter.notifyDataSetChanged();
                            }
                        });
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("ViewWitnesses", "Error loading witnesses: " + e.getMessage());
            }
        });
    }
}
