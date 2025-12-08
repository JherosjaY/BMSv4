package com.example.blottermanagementsystem.ui.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.database.BlotterDatabase;
import com.example.blottermanagementsystem.data.entity.Suspect;
import com.example.blottermanagementsystem.data.entity.PersonHistory;
import com.example.blottermanagementsystem.ui.adapters.SuspectListAdapter;
import androidx.appcompat.app.AlertDialog;
import com.example.blottermanagementsystem.data.api.ApiClient;
import com.example.blottermanagementsystem.utils.NetworkMonitor;
import com.google.android.material.button.MaterialButton;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class AddSuspectDialogFragment extends DialogFragment {

    private EditText etFullName, etAlias, etAddress, etDescription;
    private MaterialButton btnSave, btnAddAnother, btnSkip;
    private RecyclerView recyclerSuspects;
    private LinearLayout suspectListSection;
    private SuspectListAdapter suspectAdapter;
    private List<Suspect> suspectsList;
    private int reportId;
    private String respondentName;
    private String respondentAddress;
    private String respondentAlias;
    private android.widget.TextView tvRespondentName, tvRespondentAlias, tvRespondentAddress;
    private OnSuspectSavedListener listener;

    public interface OnSuspectSavedListener {
        void onSuspectSaved(Suspect suspect);
    }

    public static AddSuspectDialogFragment newInstance(int reportId, OnSuspectSavedListener listener) {
        AddSuspectDialogFragment fragment = new AddSuspectDialogFragment();
        Bundle args = new Bundle();
        args.putInt("report_id", reportId);
        fragment.setArguments(args);
        fragment.listener = listener;
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Use transparent theme to show only the MaterialCardView without dark background
        setStyle(DialogFragment.STYLE_NO_FRAME, android.R.style.Theme_Translucent_NoTitleBar);
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
            // Set dialog to wrap content (width and height)
            android.view.WindowManager.LayoutParams params = getDialog().getWindow().getAttributes();
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
            params.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
            getDialog().getWindow().setAttributes(params);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_add_suspect, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupListeners();
        loadSuspects();
        // ✅ Auto-populate respondent info from the report
        loadAndPopulateRespondent();
    }

    private void initViews(View view) {
        etFullName = view.findViewById(R.id.etSuspectFullName);
        etAlias = view.findViewById(R.id.etSuspectAlias);
        etAddress = view.findViewById(R.id.etSuspectAddress);
        etDescription = view.findViewById(R.id.etSuspectDescription);
        btnSave = view.findViewById(R.id.btnSaveSuspect);
        btnAddAnother = view.findViewById(R.id.btnAddAnother);
        btnSkip = view.findViewById(R.id.btnSkipSuspect);
        recyclerSuspects = view.findViewById(R.id.recyclerSuspects);
        suspectListSection = view.findViewById(R.id.suspectListSection);
        
        // ✅ Initialize respondent info TextViews (read-only display)
        tvRespondentName = view.findViewById(R.id.tvRespondentName);
        tvRespondentAlias = view.findViewById(R.id.tvRespondentAlias);
        tvRespondentAddress = view.findViewById(R.id.tvRespondentAddress);
        
        // Setup RecyclerView
        suspectsList = new ArrayList<>();
        suspectAdapter = new SuspectListAdapter(suspectsList, true); // true = read-only mode
        recyclerSuspects.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerSuspects.setAdapter(suspectAdapter);
    }

    private void setupListeners() {
        btnSave.setOnClickListener(v -> saveSuspect());
        btnAddAnother.setOnClickListener(v -> addAnotherSuspect());
        btnSkip.setOnClickListener(v -> skipSuspect());
        
        // ✅ Add TextWatcher to check suspect history when name is entered
        etFullName.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 2) {
                    checkSuspectHistory(s.toString().trim());
                }
            }
            
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }
    
    // ✅ CHECK SUSPECT HISTORY
    private void checkSuspectHistory(String suspectName) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                BlotterDatabase database = BlotterDatabase.getDatabase(getContext());
                if (database != null) {
                    // Get all suspects from all reports (not just this one)
                    List<Suspect> allSuspects = database.suspectDao().getSuspectsByReport(0);
                    if (allSuspects == null || allSuspects.isEmpty()) {
                        // If no suspects found, try alternative approach
                        return;
                    }
                    
                    for (Suspect suspect : allSuspects) {
                        if (suspect.getName() != null && 
                            suspect.getName().toLowerCase().contains(suspectName.toLowerCase())) {
                            
                            // Found a suspect with similar name, get their history
                            List<PersonHistory> history = database.personHistoryDao()
                                .getHistoryByPersonId(suspect.getId());
                            
                            if (history != null && !history.isEmpty()) {
                                getActivity().runOnUiThread(() -> {
                                    showSuspectHistoryDialog(suspect, history);
                                });
                                return;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("AddSuspect", "Error checking history: " + e.getMessage());
            }
        });
    }
    
    // ✅ SHOW SUSPECT HISTORY DIALOG
    private void showSuspectHistoryDialog(Suspect suspect, List<PersonHistory> history) {
        StringBuilder historyDetails = new StringBuilder();
        historyDetails.append("SUSPECT HAS CRIMINAL HISTORY\n\n");
        historyDetails.append("Name: ").append(suspect.getName()).append("\n");
        historyDetails.append("Previous Cases: ").append(history.size()).append("\n\n");
        
        historyDetails.append("Case History:\n");
        for (PersonHistory h : history) {
            historyDetails.append("• Case #").append(h.getBlotterReportId())
                .append(" - ").append(h.getActivityType())
                .append("\n");
        }
        
        historyDetails.append("\nProceed with caution!");
        
        AlertDialog dialog = new AlertDialog.Builder(getContext())
            .setTitle("Suspect History Alert")
            .setMessage(historyDetails.toString())
            .setIcon(R.drawable.ic_warning)
            .setPositiveButton("Add Anyway", (dialogInterface, which) -> {
                Toast.makeText(getContext(), "Suspect with history added", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", (dialogInterface, which) -> {
                etFullName.setText("");
                etFullName.requestFocus();
            })
            .show();
    }
    
    private void addAnotherSuspect() {
        String fullName = etFullName.getText().toString().trim();
        String alias = etAlias.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (fullName.isEmpty()) {
            Toast.makeText(getContext(), "Full Name is required", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // ✅ Log when adding suspect with history
        android.util.Log.d("AddSuspect", "Adding suspect: " + fullName);

        Suspect suspect = new Suspect();
        suspect.setBlotterReportId(reportId);
        suspect.setName(fullName);
        suspect.setAlias(alias);
        suspect.setAddress(address);
        suspect.setDescription(description);
        suspect.setDateAdded(System.currentTimeMillis());

        // Save to database in background thread
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                BlotterDatabase database = BlotterDatabase.getDatabase(getContext());
                if (database != null) {
                    long id = database.suspectDao().insertSuspect(suspect);
                    suspect.setId((int) id);
                    
                    // Sync to API if network available
                    NetworkMonitor networkMonitor = new NetworkMonitor(getContext());
                    if (networkMonitor.isNetworkAvailable()) {
                        ApiClient.getApiService().createSuspect(suspect).enqueue(new Callback<Object>() {
                            @Override
                            public void onResponse(Call<Object> call, Response<Object> response) {
                                if (response.isSuccessful()) {
                                    android.util.Log.d("AddSuspect", "Synced to API");
                                }
                            }
                            
                            @Override
                            public void onFailure(Call<Object> call, Throwable t) {
                                android.util.Log.w("AddSuspect", "API sync failed: " + t.getMessage());
                            }
                        });
                    }
                    
                    getActivity().runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onSuspectSaved(suspect);
                        }
                        Toast.makeText(getContext(), "Suspect added. Add another or save.", Toast.LENGTH_SHORT).show();
                        loadSuspects(); // Refresh the list
                        
                        // Clear fields for next suspect
                        etFullName.setText("");
                        etAlias.setText("");
                        etAddress.setText("");
                        etDescription.setText("");
                        etFullName.requestFocus();
                    });
                }
            } catch (Exception e) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Error saving suspect: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void saveSuspect() {
        String fullName = etFullName.getText().toString().trim();
        String alias = etAlias.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (fullName.isEmpty()) {
            Toast.makeText(getContext(), "Full Name is required", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // ✅ Log when saving suspect with history
        android.util.Log.d("AddSuspect", "Saving suspect: " + fullName);

        Suspect suspect = new Suspect();
        suspect.setBlotterReportId(reportId);
        suspect.setName(fullName);
        suspect.setAlias(alias);
        suspect.setAddress(address);
        suspect.setDescription(description);
        suspect.setDateAdded(System.currentTimeMillis());

        // Save to database in background thread
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                BlotterDatabase database = BlotterDatabase.getDatabase(getContext());
                if (database != null) {
                    long id = database.suspectDao().insertSuspect(suspect);
                    suspect.setId((int) id);
                    
                    // Sync to API if network available
                    NetworkMonitor networkMonitor = new NetworkMonitor(getContext());
                    if (networkMonitor.isNetworkAvailable()) {
                        ApiClient.getApiService().createSuspect(suspect).enqueue(new Callback<Object>() {
                            @Override
                            public void onResponse(Call<Object> call, Response<Object> response) {
                                if (response.isSuccessful()) {
                                    android.util.Log.d("AddSuspect", "Synced to API");
                                }
                            }
                            
                            @Override
                            public void onFailure(Call<Object> call, Throwable t) {
                                android.util.Log.w("AddSuspect", "API sync failed: " + t.getMessage());
                            }
                        });
                    }
                    
                    getActivity().runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onSuspectSaved(suspect);
                        }
                        Toast.makeText(getContext(), "Suspect saved", Toast.LENGTH_SHORT).show();
                        loadSuspects(); // Refresh the list
                        dismiss();
                    });
                }
            } catch (Exception e) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Error saving suspect: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void loadSuspects() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                BlotterDatabase database = BlotterDatabase.getDatabase(getContext());
                if (database != null) {
                    List<Suspect> suspects = database.suspectDao().getSuspectsByReportId(reportId);
                    
                    getActivity().runOnUiThread(() -> {
                        suspectsList.clear();
                        suspectsList.addAll(suspects);
                        suspectAdapter.notifyDataSetChanged();
                        
                        // Show/hide the suspects list section
                        if (suspectsList.isEmpty()) {
                            suspectListSection.setVisibility(View.GONE);
                        } else {
                            suspectListSection.setVisibility(View.VISIBLE);
                        }
                    });
                }
            } catch (Exception e) {
                android.util.Log.e("AddSuspect", "Error loading suspects: " + e.getMessage());
            }
        });
    }
    
    // ✅ AUTO-POPULATE RESPONDENT INFO (READ-ONLY DISPLAY)
    // Load respondent name, alias, and address from the report and display in read-only section
    private void loadAndPopulateRespondent() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                BlotterDatabase database = BlotterDatabase.getDatabase(getContext());
                if (database != null) {
                    // Get the report to retrieve respondent info
                    com.example.blottermanagementsystem.data.entity.BlotterReport report = 
                        database.blotterReportDao().getReportById(reportId);
                    
                    if (report != null) {
                        respondentName = report.getRespondentName();
                        respondentAlias = report.getRespondentAlias();
                        respondentAddress = report.getRespondentAddress();
                        
                        // Update UI on main thread
                        getActivity().runOnUiThread(() -> {
                            // Display respondent name in read-only section
                            if (respondentName != null && !respondentName.isEmpty() && !respondentName.equals("N/A")) {
                                tvRespondentName.setText(respondentName);
                                android.util.Log.d("AddSuspect", "✅ Displayed respondent name: " + respondentName);
                            } else {
                                tvRespondentName.setText("N/A");
                            }
                            
                            // Display respondent alias in read-only section
                            if (respondentAlias != null && !respondentAlias.isEmpty() && !respondentAlias.equals("N/A")) {
                                tvRespondentAlias.setText(respondentAlias);
                                android.util.Log.d("AddSuspect", "✅ Displayed respondent alias: " + respondentAlias);
                            } else {
                                tvRespondentAlias.setText("N/A");
                            }
                            
                            // Display respondent address in read-only section
                            if (respondentAddress != null && !respondentAddress.isEmpty() && !respondentAddress.equals("N/A")) {
                                tvRespondentAddress.setText(respondentAddress);
                                android.util.Log.d("AddSuspect", "✅ Displayed respondent address: " + respondentAddress);
                            } else {
                                tvRespondentAddress.setText("N/A");
                            }
                            
                            // Keep input fields EMPTY for officer to add new suspects
                            etFullName.setText("");
                            etAlias.setText("");
                            etAddress.setText("");
                            etDescription.setText("");
                        });
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("AddSuspect", "Error loading respondent: " + e.getMessage());
            }
        });
    }
    
    // ✅ DONE - Save unsaved suspect data first, then show confirmation
    private void skipSuspect() {
        android.util.Log.d("AddSuspect", "🔵 skipSuspect() called");
        
        String fullName = etFullName.getText().toString().trim();
        android.util.Log.d("AddSuspect", "🔵 Checking for unsaved data - Full Name: '" + fullName + "'");
        
        // If there's unsaved suspect data, save it first
        if (!fullName.isEmpty()) {
            android.util.Log.d("AddSuspect", "✅ Found unsaved suspect - saving before closing");
            // Save the suspect, which will dismiss the dialog
            saveSuspect();
            return;
        }
        
        // No unsaved data, show confirmation
        android.util.Log.d("AddSuspect", "🔵 No unsaved data - showing confirmation dialog");
        
        // Create callback INLINE to ensure it's captured properly
        ConfirmationDialogFragment.OnConfirmListener callback = new ConfirmationDialogFragment.OnConfirmListener() {
            @Override
            public void onConfirm() {
                android.util.Log.d("AddSuspect", "✅✅✅ CALLBACK EXECUTED! Saving 'None' suspect");
                saveNoneSuspect();
            }
        };
        
        android.util.Log.d("AddSuspect", "🔵 Creating confirmation dialog with callback");
        ConfirmationDialogFragment confirmDialog = ConfirmationDialogFragment.newInstance(
            "Confirm",
            "No other suspects to add?",
            "Yes, Done",
            callback
        );
        
        android.util.Log.d("AddSuspect", "🔵 Showing confirmation dialog");
        confirmDialog.show(getParentFragmentManager(), "confirmation_dialog");
    }
    
    // ✅ CONFIRM SKIP - Creates marker and closes dialog
    private void confirmSkipSuspect() {
        // Run on background thread to avoid blocking UI
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                BlotterDatabase database = BlotterDatabase.getDatabase(getContext());
                if (database != null) {
                    // Check if marker already exists for this report
                    List<Suspect> existingSuspects = database.suspectDao().getSuspectsByReportId(reportId);
                    boolean markerExists = false;
                    
                    for (Suspect suspect : existingSuspects) {
                        if (suspect.getAlias() != null && suspect.getAlias().equals("SKIP_MARKER")) {
                            markerExists = true;
                            break;
                        }
                    }
                    
                    // Only create marker if it doesn't exist
                    if (!markerExists) {
                        Suspect markerSuspect = new Suspect();
                        markerSuspect.setBlotterReportId(reportId);
                        markerSuspect.setName("[No Other Suspects]");
                        markerSuspect.setAlias("SKIP_MARKER");
                        markerSuspect.setAddress("N/A");
                        markerSuspect.setDescription("Officer confirmed no other suspects");
                        markerSuspect.setDateAdded(System.currentTimeMillis());
                        
                        // Insert marker suspect
                        long id = database.suspectDao().insertSuspect(markerSuspect);
                        markerSuspect.setId((int) id);
                        
                        android.util.Log.d("AddSuspect", "✅ Skip marker created - case can proceed to next step");
                    } else {
                        android.util.Log.d("AddSuspect", "✅ Skip marker already exists - skipping duplicate");
                    }
                }
                
                // Notify on main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Investigation complete", Toast.LENGTH_SHORT).show();
                        dismiss();
                    });
                }
                
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
                android.util.Log.e("AddSuspect", "Error in confirmSkipSuspect: " + e.getMessage());
            }
        });
    }
    
    // ✅ Save "None" when officer confirms no more suspects
    private void saveNoneSuspect() {
        Suspect suspect = new Suspect();
        suspect.setBlotterReportId(reportId);
        suspect.setName("None");
        suspect.setAlias("None");
        suspect.setAddress("None");
        suspect.setDescription("None");
        suspect.setDateAdded(System.currentTimeMillis());

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                BlotterDatabase database = BlotterDatabase.getDatabase(getContext());
                if (database != null) {
                    long id = database.suspectDao().insertSuspect(suspect);
                    suspect.setId((int) id);
                    android.util.Log.d("AddSuspect", "✅ 'None' suspect saved with ID: " + id);
                    
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Suspect collection complete", Toast.LENGTH_SHORT).show();
                        
                        // Trigger timeline refresh
                        if (getActivity() instanceof com.example.blottermanagementsystem.ui.activities.OfficerCaseDetailActivity) {
                            ((com.example.blottermanagementsystem.ui.activities.OfficerCaseDetailActivity) getActivity()).refreshTimelineDirectly();
                        }
                        
                        dismiss();
                    });
                }
            } catch (Exception e) {
                android.util.Log.e("AddSuspect", "Error saving 'None' suspect: " + e.getMessage());
            }
        });
    }
}
