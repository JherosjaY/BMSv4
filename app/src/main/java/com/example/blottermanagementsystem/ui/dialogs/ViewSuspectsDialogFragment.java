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
import com.example.blottermanagementsystem.data.entity.Suspect;
import com.example.blottermanagementsystem.ui.adapters.SuspectListAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class ViewSuspectsDialogFragment extends DialogFragment {
    
    private int reportId;
    private RecyclerView rvSuspectList;
    private TextView tvEmptyState;
    private SuspectListAdapter adapter;
    private List<Suspect> suspectList = new ArrayList<>();
    
    public static ViewSuspectsDialogFragment newInstance(int reportId) {
        ViewSuspectsDialogFragment fragment = new ViewSuspectsDialogFragment();
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
        return inflater.inflate(R.layout.dialog_view_suspects, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        loadSuspects();
    }
    
    private void initViews(View view) {
        rvSuspectList = view.findViewById(R.id.rvSuspectList);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        
        rvSuspectList.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SuspectListAdapter(suspectList, true); // true = read-only
        rvSuspectList.setAdapter(adapter);
    }
    
    private void loadSuspects() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                BlotterDatabase database = BlotterDatabase.getDatabase(getContext());
                if (database != null) {
                    suspectList.clear();
                    suspectList.addAll(database.suspectDao().getSuspectsByReport(reportId));
                    
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (suspectList.isEmpty()) {
                                tvEmptyState.setVisibility(View.VISIBLE);
                                rvSuspectList.setVisibility(View.GONE);
                            } else {
                                tvEmptyState.setVisibility(View.GONE);
                                rvSuspectList.setVisibility(View.VISIBLE);
                                adapter.notifyDataSetChanged();
                            }
                        });
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("ViewSuspects", "Error loading suspects: " + e.getMessage());
            }
        });
    }
}
