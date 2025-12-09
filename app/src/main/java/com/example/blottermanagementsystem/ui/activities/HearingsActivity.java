package com.example.blottermanagementsystem.ui.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.entity.Hearing;
import com.example.blottermanagementsystem.ui.adapters.HearingAdapter;
import com.example.blottermanagementsystem.utils.PreferencesManager;
import com.example.blottermanagementsystem.utils.NetworkMonitor;
import com.example.blottermanagementsystem.utils.ApiClient;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ✅ PURE ONLINE HEARINGS ACTIVITY
 * ✅ All hearings loaded from API (Neon database)
 * ✅ No local database dependencies
 */
public class HearingsActivity extends BaseActivity {

    // UI Components
    private RecyclerView recyclerView;
    private View emptyState;
    private androidx.cardview.widget.CardView emptyStateCard;
    private Chip chipAll, chipUpcoming, chipCompleted, chipCancelled;
    private EditText etSearch;
    private ImageView emptyStateIcon;
    private TextView emptyStateTitle, emptyStateMessage;

    // Data
    private PreferencesManager preferencesManager;
    private NetworkMonitor networkMonitor;
    private List<Hearing> allHearings = new ArrayList<>();
    private List<Hearing> filteredHearings = new ArrayList<>();
    private HearingAdapter hearingAdapter;
    private String currentFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hearings);
        
        preferencesManager = new PreferencesManager(this);
        networkMonitor = new NetworkMonitor(this);
        
        setupToolbar();
        initViews();
        setupListeners();
        loadHearings();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Hearings");
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        emptyState = findViewById(R.id.emptyState);
        emptyStateCard = findViewById(R.id.emptyStateCard);
        chipAll = findViewById(R.id.chipAll);
        chipUpcoming = findViewById(R.id.chipUpcoming);
        chipCompleted = findViewById(R.id.chipCompleted);
        chipCancelled = findViewById(R.id.chipCancelled);
        etSearch = findViewById(R.id.etSearch);
        emptyStateIcon = findViewById(R.id.emptyStateIcon);
        emptyStateTitle = findViewById(R.id.emptyStateTitle);
        emptyStateMessage = findViewById(R.id.emptyStateMessage);

        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            hearingAdapter = new HearingAdapter(this, filteredHearings, this::showHearingDetails);
            recyclerView.setAdapter(hearingAdapter);
        }
    }

    private void showHearingDetails(com.example.blottermanagementsystem.data.entity.Hearing hearing) {
        // Handle hearing details display
        Toast.makeText(this, "Hearing: " + hearing.getId(), Toast.LENGTH_SHORT).show();
    }
    
    private void setupListeners() {
        if (chipAll != null) chipAll.setOnClickListener(v -> filterByStatus("ALL"));
        if (chipUpcoming != null) chipUpcoming.setOnClickListener(v -> filterByStatus("Scheduled"));
        if (chipCompleted != null) chipCompleted.setOnClickListener(v -> filterByStatus("Completed"));
        if (chipCancelled != null) chipCancelled.setOnClickListener(v -> filterByStatus("Cancelled"));

        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterHearings();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    /**
     * ✅ PURE ONLINE: Load hearings from API
     */
    private void loadHearings() {
        if (!networkMonitor.isOnline()) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            showEmptyState();
            return;
        }

        String userId = preferencesManager.getUserId();
        String userRole = preferencesManager.getUserRole();

        ApiClient.getHearings(new ApiClient.ApiCallback<List<Hearing>>() {
            @Override
            public void onSuccess(List<Hearing> apiHearings) {
                if (isFinishing() || isDestroyed()) return;

                List<Hearing> userHearings = new ArrayList<>();

                if ("OFFICER".equalsIgnoreCase(userRole) || "ADMIN".equalsIgnoreCase(userRole)) {
                    userHearings.addAll(apiHearings);
                } else {
                    for (Hearing hearing : apiHearings) {
                        if (hearing.getUserId() != null && hearing.getUserId().equals(userId)) {
                            userHearings.add(hearing);
                        }
                    }
                }

                runOnUiThread(() -> {
                    allHearings.clear();
                    allHearings.addAll(userHearings);
                    filterHearings();
                    updateEmptyState();
                });
            }

            @Override
            public void onError(String errorMessage) {
                if (isFinishing() || isDestroyed()) return;

                runOnUiThread(() -> {
                    Toast.makeText(HearingsActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                    showEmptyState();
                });
            }
        });
    }

    private void filterByStatus(String status) {
        currentFilter = status;
        filterHearings();
    }

    private void filterHearings() {
        filteredHearings.clear();
        String searchQuery = etSearch != null ? etSearch.getText().toString().toLowerCase() : "";

        for (Hearing hearing : allHearings) {
            boolean matchesStatus = currentFilter.equals("ALL") || 
                (hearing.getStatus() != null && hearing.getStatus().equalsIgnoreCase(currentFilter));
            
            boolean matchesSearch = searchQuery.isEmpty() || 
                (hearing.getLocation() != null && hearing.getLocation().toLowerCase().contains(searchQuery));

            if (matchesStatus && matchesSearch) {
                filteredHearings.add(hearing);
            }
        }

        if (hearingAdapter != null) {
            hearingAdapter.notifyDataSetChanged();
        }
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (filteredHearings.isEmpty()) {
            showEmptyState();
        } else {
            hideEmptyState();
        }
    }

    private void showEmptyState() {
        if (recyclerView != null) recyclerView.setVisibility(View.GONE);
        if (emptyStateCard != null) emptyStateCard.setVisibility(View.VISIBLE);
    }

    private void hideEmptyState() {
        if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
        if (emptyStateCard != null) emptyStateCard.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHearings();
    }
}
