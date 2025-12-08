package com.example.blottermanagementsystem.ui.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.database.BlotterDatabase;
import com.example.blottermanagementsystem.data.entity.Resolution;
import com.example.blottermanagementsystem.data.entity.Suspect;
import com.example.blottermanagementsystem.data.entity.PersonHistory;
import com.example.blottermanagementsystem.data.api.ApiClient;
import com.example.blottermanagementsystem.utils.NetworkMonitor;
import java.util.List;
import com.google.android.material.button.MaterialButton;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.concurrent.Executors;

public class DocumentResolutionDialogFragment extends DialogFragment {

    private Spinner spinnerResolutionType;
    private EditText etResolutionDetails;
    private MaterialButton btnSave;
    private int reportId;
    private OnResolutionSavedListener listener;

    public interface OnResolutionSavedListener {
        void onResolutionSaved(Resolution resolution);
    }

    public static DocumentResolutionDialogFragment newInstance(int reportId, OnResolutionSavedListener listener) {
        DocumentResolutionDialogFragment fragment = new DocumentResolutionDialogFragment();
        Bundle args = new Bundle();
        args.putInt("report_id", reportId);
        fragment.setArguments(args);
        fragment.listener = listener;
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Use transparent background to show the MaterialCardView properly
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Dialog_MinWidth);
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
            // Set dialog to match parent width with padding
            android.view.WindowManager.LayoutParams params = getDialog().getWindow().getAttributes();
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
            getDialog().getWindow().setAttributes(params);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_document_resolution, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupSpinner();
        setupListeners();
    }

    private void initViews(View view) {
        spinnerResolutionType = view.findViewById(R.id.spinnerResolutionType);
        etResolutionDetails = view.findViewById(R.id.etResolutionDetails);
        btnSave = view.findViewById(R.id.btnSaveResolution);
    }

    private void setupSpinner() {
        // ✅ SIMPLIFIED: Only show Settled and Withdrawn options
        String[] resolutionTypes = {"Settled", "Withdrawn"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), 
            android.R.layout.simple_spinner_item, resolutionTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerResolutionType.setAdapter(adapter);
    }

    private void setupListeners() {
        btnSave.setOnClickListener(v -> saveResolution());
    }
    
    private void saveResolution() {
        String type = spinnerResolutionType.getSelectedItem().toString();
        String details = etResolutionDetails.getText().toString().trim();

        if (details.isEmpty()) {
            Toast.makeText(getContext(), "Resolution details are required", Toast.LENGTH_SHORT).show();
            return;
        }

        Resolution resolution = new Resolution(reportId, type, details, 0);
        resolution.setResolvedDate(System.currentTimeMillis());
        resolution.setCreatedAt(System.currentTimeMillis());

        // Save to database in background thread
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                BlotterDatabase database = BlotterDatabase.getDatabase(getContext());
                if (database != null) {
                    long id = database.resolutionDao().insertResolution(resolution);
                    resolution.setId((int) id);
                    
                    // ✅ UPDATE CASE STATUS BASED ON RESOLUTION TYPE
                    try {
                        com.example.blottermanagementsystem.data.entity.BlotterReport report = database.blotterReportDao().getReportById(reportId);
                        if (report != null) {
                            // Map resolution type to case status
                            if ("Settled".equals(type)) {
                                report.setStatus("Resolved");
                                android.util.Log.d("DocumentResolution", "✅ Case status updated to 'Resolved'");
                            } else if ("Withdrawn".equals(type)) {
                                report.setStatus("Cancelled");
                                android.util.Log.d("DocumentResolution", "✅ Case status updated to 'Cancelled'");
                            }
                            database.blotterReportDao().updateReport(report);
                        }
                    } catch (Exception e) {
                        android.util.Log.e("DocumentResolution", "❌ Error updating case status: " + e.getMessage());
                    }
                    
                    // ✅ FIXED: AUTO-SAVE PERSON TO HISTORY - DETERMINE ROLE BASED ON INVESTIGATION
                    // Logic: If witnesses OR suspects added → SUSPECT, else → RESPONDENT
                    try {
                        List<Suspect> suspects = database.suspectDao().getSuspectsByReport(reportId);
                        List<com.example.blottermanagementsystem.data.entity.Witness> witnesses = database.witnessDao().getWitnessesByReport(reportId);
                        
                        // Check if case has witnesses or suspects
                        boolean hasWitnesses = witnesses != null && !witnesses.isEmpty();
                        boolean hasSuspects = suspects != null && !suspects.isEmpty();
                        
                        android.util.Log.d("DocumentResolution", "📊 Investigation Summary - Witnesses: " + (hasWitnesses ? witnesses.size() : 0) + ", Suspects: " + (hasSuspects ? suspects.size() : 0));
                        
                        // ✅ LOGIC: If witnesses OR suspects exist → Person is SUSPECT
                        if (hasWitnesses || hasSuspects) {
                            // Save suspects as "Suspect"
                            if (hasSuspects) {
                                for (Suspect suspect : suspects) {
                                    PersonHistory history = new PersonHistory(
                                        suspect.getId(),
                                        "Suspect",
                                        "Suspect in case: " + type + " - " + details
                                    );
                                    history.setBlotterReportId(reportId);
                                    history.setTimestamp(System.currentTimeMillis());
                                    history.setPerformedByPersonId(0); // System auto-save
                                    history.setMetadata("resolution_id:" + resolution.getId() + ",resolution_type:" + type);
                                    
                                    database.personHistoryDao().insertPersonHistory(history);
                                    android.util.Log.d("DocumentResolution", "✅ Person " + suspect.getId() + " saved as SUSPECT to PersonHistory");
                                }
                            }
                        } else {
                            // ✅ LOGIC: If NO witnesses AND NO suspects → Person is RESPONDENT
                            // Get respondent name from report (the accused person)
                            com.example.blottermanagementsystem.data.entity.BlotterReport report = database.blotterReportDao().getReportById(reportId);
                            if (report != null && report.getRespondentName() != null && !report.getRespondentName().isEmpty()) {
                                // Try to find or create person record for respondent
                                String respondentName = report.getRespondentName();
                                
                                // Search for existing person with similar name
                                List<com.example.blottermanagementsystem.data.entity.Person> existingPersons = 
                                    database.personDao().searchPersonByNameSimilar(respondentName);
                                
                                int respondentPersonId;
                                if (existingPersons != null && !existingPersons.isEmpty()) {
                                    // Use existing person (first match)
                                    respondentPersonId = existingPersons.get(0).getId();
                                    android.util.Log.d("DocumentResolution", "📌 Found existing person: " + respondentName);
                                } else {
                                    // Create new person record for respondent
                                    com.example.blottermanagementsystem.data.entity.Person newPerson = 
                                        new com.example.blottermanagementsystem.data.entity.Person();
                                    newPerson.setFirstName(respondentName.split(" ")[0]); // First name
                                    if (respondentName.split(" ").length > 1) {
                                        newPerson.setLastName(respondentName.substring(respondentName.indexOf(" ") + 1)); // Last name
                                    }
                                    respondentPersonId = (int) database.personDao().insertPerson(newPerson);
                                    android.util.Log.d("DocumentResolution", "✨ Created new person record for respondent: " + respondentName);
                                }
                                
                                // Save respondent to PersonHistory
                                PersonHistory history = new PersonHistory(
                                    respondentPersonId,
                                    "Respondent",
                                    "Respondent in case: " + type + " - " + details
                                );
                                history.setBlotterReportId(reportId);
                                history.setTimestamp(System.currentTimeMillis());
                                history.setPerformedByPersonId(0); // System auto-save
                                history.setMetadata("resolution_id:" + resolution.getId() + ",resolution_type:" + type);
                                
                                database.personHistoryDao().insertPersonHistory(history);
                                android.util.Log.d("DocumentResolution", "✅ Respondent '" + respondentName + "' saved as RESPONDENT to PersonHistory (no witnesses/suspects added)");
                            }
                        }
                    } catch (Exception e) {
                        android.util.Log.e("DocumentResolution", "❌ Error saving person to PersonHistory: " + e.getMessage());
                    }
                    
                    // Sync to API if network available
                    NetworkMonitor networkMonitor = new NetworkMonitor(getContext());
                    if (networkMonitor.isNetworkAvailable()) {
                        ApiClient.getApiService().createResolution(resolution).enqueue(new Callback<Object>() {
                            @Override
                            public void onResponse(Call<Object> call, Response<Object> response) {
                                if (response.isSuccessful()) {
                                    android.util.Log.d("DocumentResolution", "Synced to API");
                                }
                            }
                            
                            @Override
                            public void onFailure(Call<Object> call, Throwable t) {
                                android.util.Log.w("DocumentResolution", "API sync failed: " + t.getMessage());
                            }
                        });
                    }
                    
                    getActivity().runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onResolutionSaved(resolution);
                        }
                        // ✅ Toast removed - timeline refresh provides visual feedback
                        dismiss();
                    });
                }
            } catch (Exception e) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Error saving resolution: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
