package com.example.blottermanagementsystem.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.database.BlotterDatabase;
import com.example.blottermanagementsystem.data.entity.Evidence;
import com.example.blottermanagementsystem.ui.adapters.EvidenceAdapter;
import com.example.blottermanagementsystem.utils.PreferencesManager;
import com.example.blottermanagementsystem.utils.RoleAccessControl;
import java.util.List;

public class EvidenceListActivity extends BaseActivity {
    
    private RecyclerView recyclerView;
    private EvidenceAdapter adapter;
    private TextView tvEmpty;
    private FloatingActionButton fabAdd;
    private BlotterDatabase database;
    private int reportId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        PreferencesManager preferencesManager = new PreferencesManager(this);
        if (!RoleAccessControl.checkAnyRole(this, new String[]{"Admin", "Officer"}, preferencesManager)) {
            return;
        }
        
        setContentView(R.layout.activity_evidence_list);
        
        database = BlotterDatabase.getDatabase(this);
        reportId = getIntent().getIntExtra("REPORT_ID", -1);
        
        setupToolbar();
        initViews();
        setupListeners();
        loadEvidence();
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Evidence");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }
    
    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        tvEmpty = findViewById(R.id.tvEmpty);
        fabAdd = findViewById(R.id.fabAdd);
        
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new EvidenceAdapter(evidence -> {
            // View evidence details
            Toast.makeText(this, "Evidence: " + evidence.getId(), Toast.LENGTH_SHORT).show();
        });
        recyclerView.setAdapter(adapter);
    }
    
    private void setupListeners() {
        fabAdd.setOnClickListener(v -> {
            // AddEvidenceActivity removed - now using floating dialog
            Toast.makeText(this, "Evidence feature moved to Case Investigation screen", Toast.LENGTH_SHORT).show();
        });
    }
    
    private void loadEvidence() {
        new Thread(() -> {
            List<Evidence> evidenceList;
            
            if (reportId != -1) {
                evidenceList = database.evidenceDao().getEvidenceByReportId(reportId);
            } else {
                evidenceList = database.evidenceDao().getAllEvidence();
            }
            
            runOnUiThread(() -> {
                if (evidenceList.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    tvEmpty.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    tvEmpty.setVisibility(View.GONE);
                    adapter.setEvidenceList(evidenceList);
                }
            });
        }).start();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadEvidence();
    }
}
