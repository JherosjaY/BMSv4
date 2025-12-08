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
import com.example.blottermanagementsystem.data.entity.Witness;
import com.example.blottermanagementsystem.ui.adapters.WitnessListAdapter;
import com.example.blottermanagementsystem.data.api.ApiClient;
import com.example.blottermanagementsystem.utils.NetworkMonitor;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class AddWitnessDialogFragment extends DialogFragment {

    private EditText etFullName, etAddress, etContactNumber, etStatement;
    private MaterialButton btnSave, btnAddAnother, btnDone;
    private RecyclerView recyclerWitnesses;
    private LinearLayout witnessListSection;
    private WitnessListAdapter witnessAdapter;
    private List<Witness> witnessesList;
    private int reportId;
    private OnWitnessSavedListener listener;

    public interface OnWitnessSavedListener {
        void onWitnessSaved(Witness witness);
    }

    public static AddWitnessDialogFragment newInstance(int reportId, OnWitnessSavedListener listener) {
        AddWitnessDialogFragment fragment = new AddWitnessDialogFragment();
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
        return inflater.inflate(R.layout.dialog_add_witness, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupListeners();
        loadWitnesses();
    }

    private void initViews(View view) {
        etFullName = view.findViewById(R.id.etWitnessFullName);
        etAddress = view.findViewById(R.id.etWitnessAddress);
        etContactNumber = view.findViewById(R.id.etWitnessContactNumber);
        etStatement = view.findViewById(R.id.etWitnessStatement);
        btnSave = view.findViewById(R.id.btnSaveWitness);
        btnAddAnother = view.findViewById(R.id.btnAddAnother);
        btnDone = view.findViewById(R.id.btnDoneWitness);
        recyclerWitnesses = view.findViewById(R.id.recyclerWitnesses);
        witnessListSection = view.findViewById(R.id.witnessListSection);
        
        // Setup RecyclerView
        witnessesList = new ArrayList<>();
        witnessAdapter = new WitnessListAdapter(witnessesList, true); // true = read-only mode
        recyclerWitnesses.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerWitnesses.setAdapter(witnessAdapter);
    }

    private void setupListeners() {
        btnSave.setOnClickListener(v -> saveWitness());
        btnAddAnother.setOnClickListener(v -> addAnotherWitness());
        btnDone.setOnClickListener(v -> doneWithWitnesses());
    }

    private void addAnotherWitness() {
        String fullName = etFullName.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String contactNumber = etContactNumber.getText().toString().trim();
        String statement = etStatement.getText().toString().trim();

        if (fullName.isEmpty()) {
            Toast.makeText(getContext(), "Full Name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        Witness witness = new Witness();
        witness.setBlotterReportId(reportId);
        witness.setName(fullName);
        witness.setAddress(address);
        witness.setContactNumber(contactNumber);
        witness.setStatement(statement);
        witness.setCreatedAt(System.currentTimeMillis());

        // Save to database in background thread
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                BlotterDatabase database = BlotterDatabase.getDatabase(getContext());
                if (database != null) {
                    long id = database.witnessDao().insertWitness(witness);
                    witness.setId((int) id);
                    
                    // Sync to API if network available
                    NetworkMonitor networkMonitor = new NetworkMonitor(getContext());
                    if (networkMonitor.isNetworkAvailable()) {
                        ApiClient.getApiService().createWitness(witness).enqueue(new Callback<Object>() {
                            @Override
                            public void onResponse(Call<Object> call, Response<Object> response) {
                                if (response.isSuccessful()) {
                                    android.util.Log.d("AddWitness", "Synced to API");
                                }
                            }
                            
                            @Override
                            public void onFailure(Call<Object> call, Throwable t) {
                                android.util.Log.w("AddWitness", "API sync failed: " + t.getMessage());
                            }
                        });
                    }
                    
                    // Notify on main thread
                    getActivity().runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onWitnessSaved(witness);
                        }
                        Toast.makeText(getContext(), "Witness added. Add another or save.", Toast.LENGTH_SHORT).show();
                        loadWitnesses(); // Refresh the list
                        
                        // Clear fields for next witness
                        etFullName.setText("");
                        etAddress.setText("");
                        etContactNumber.setText("");
                        etStatement.setText("");
                        etFullName.requestFocus();
                    });
                }
            } catch (Exception e) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Error saving witness: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void saveWitness() {
        String fullName = etFullName.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String contactNumber = etContactNumber.getText().toString().trim();
        String statement = etStatement.getText().toString().trim();

        if (fullName.isEmpty()) {
            Toast.makeText(getContext(), "Full Name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        Witness witness = new Witness();
        witness.setBlotterReportId(reportId);
        witness.setName(fullName);
        witness.setAddress(address);
        witness.setContactNumber(contactNumber);
        witness.setStatement(statement);
        witness.setCreatedAt(System.currentTimeMillis());

        // Save to database in background thread
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                BlotterDatabase database = BlotterDatabase.getDatabase(getContext());
                if (database != null) {
                    long id = database.witnessDao().insertWitness(witness);
                    witness.setId((int) id);
                    
                    // Sync to API if network available
                    NetworkMonitor networkMonitor = new NetworkMonitor(getContext());
                    if (networkMonitor.isNetworkAvailable()) {
                        ApiClient.getApiService().createWitness(witness).enqueue(new Callback<Object>() {
                            @Override
                            public void onResponse(Call<Object> call, Response<Object> response) {
                                if (response.isSuccessful()) {
                                    android.util.Log.d("AddWitness", "Synced to API");
                                }
                            }
                            
                            @Override
                            public void onFailure(Call<Object> call, Throwable t) {
                                android.util.Log.w("AddWitness", "API sync failed: " + t.getMessage());
                            }
                        });
                    }
                    
                    // Notify on main thread
                    getActivity().runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onWitnessSaved(witness);
                        }
                        Toast.makeText(getContext(), "Witness saved", Toast.LENGTH_SHORT).show();
                        loadWitnesses(); // Refresh the list
                        dismiss();
                    });
                }
            } catch (Exception e) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Error saving witness: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void loadWitnesses() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                BlotterDatabase database = BlotterDatabase.getDatabase(getContext());
                if (database != null) {
                    List<Witness> witnesses = database.witnessDao().getWitnessesByReportId(reportId);
                    
                    getActivity().runOnUiThread(() -> {
                        witnessesList.clear();
                        witnessesList.addAll(witnesses);
                        witnessAdapter.notifyDataSetChanged();
                        
                        // Show/hide the witnesses list section
                        if (witnessesList.isEmpty()) {
                            witnessListSection.setVisibility(View.GONE);
                        } else {
                            witnessListSection.setVisibility(View.VISIBLE);
                        }
                    });
                }
            } catch (Exception e) {
                android.util.Log.e("AddWitness", "Error loading witnesses: " + e.getMessage());
            }
        });
    }
    
    // ✅ DONE - Save unsaved witness data first, then show confirmation
    private void doneWithWitnesses() {
        android.util.Log.d("AddWitness", "🔵 doneWithWitnesses() called");
        
        String fullName = etFullName.getText().toString().trim();
        android.util.Log.d("AddWitness", "🔵 Checking for unsaved data - Full Name: '" + fullName + "'");
        
        // If there's unsaved witness data, save it first
        if (!fullName.isEmpty()) {
            android.util.Log.d("AddWitness", "✅ Found unsaved witness - saving before closing");
            // Save the witness, which will dismiss the dialog
            saveWitness();
            return;
        }
        
        // No unsaved data, show confirmation
        android.util.Log.d("AddWitness", "🔵 No unsaved data - showing confirmation dialog");
        
        // Create callback INLINE to ensure it's captured properly
        ConfirmationDialogFragment.OnConfirmListener callback = new ConfirmationDialogFragment.OnConfirmListener() {
            @Override
            public void onConfirm() {
                android.util.Log.d("AddWitness", "✅✅✅ CALLBACK EXECUTED! Saving 'None' witness");
                saveNoneWitness();
            }
        };
        
        android.util.Log.d("AddWitness", "🔵 Creating confirmation dialog with callback");
        ConfirmationDialogFragment confirmDialog = ConfirmationDialogFragment.newInstance(
            "Confirm",
            "No more witnesses to add?",
            "Yes, Done",
            callback
        );
        
        android.util.Log.d("AddWitness", "🔵 Showing confirmation dialog");
        confirmDialog.show(getParentFragmentManager(), "confirmation_dialog");
    }
    
    // ✅ Save "None" when officer confirms no more witnesses
    private void saveNoneWitness() {
        Witness witness = new Witness();
        witness.setBlotterReportId(reportId);
        witness.setName("None");
        witness.setAddress("None");
        witness.setContactNumber("None");
        witness.setStatement("None");
        witness.setCreatedAt(System.currentTimeMillis());

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                BlotterDatabase database = BlotterDatabase.getDatabase(getContext());
                if (database != null) {
                    long id = database.witnessDao().insertWitness(witness);
                    witness.setId((int) id);
                    android.util.Log.d("AddWitness", "✅ 'None' witness saved with ID: " + id);
                    
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Witness collection complete", Toast.LENGTH_SHORT).show();
                        
                        // Trigger timeline refresh
                        if (getActivity() instanceof com.example.blottermanagementsystem.ui.activities.OfficerCaseDetailActivity) {
                            ((com.example.blottermanagementsystem.ui.activities.OfficerCaseDetailActivity) getActivity()).refreshTimelineDirectly();
                        }
                        
                        dismiss();
                    });
                }
            } catch (Exception e) {
                android.util.Log.e("AddWitness", "Error saving 'None' witness: " + e.getMessage());
            }
        });
    }
}
