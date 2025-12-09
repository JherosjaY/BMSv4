package com.example.blottermanagementsystem.ui.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.utils.NetworkMonitor;
import com.example.blottermanagementsystem.utils.ApiClient;
import com.google.android.material.button.MaterialButton;

import java.util.HashMap;
import java.util.Map;

/**
 * ✅ PURE ONLINE ADD EVIDENCE DIALOG
 * ✅ All evidence data sent to API (Neon database)
 * ✅ No local database dependencies
 */
public class AddEvidenceDialogFragment extends DialogFragment {

    private EditText etDescription, etLocation;
    private Spinner spinnerType;
    private MaterialButton btnAdd, btnCancel;
    private NetworkMonitor networkMonitor;
    private int reportId;

    public static AddEvidenceDialogFragment newInstance(int reportId) {
        AddEvidenceDialogFragment fragment = new AddEvidenceDialogFragment();
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
        return inflater.inflate(R.layout.dialog_add_evidence, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupListeners();
    }

    private void initViews(View view) {
        etDescription = view.findViewById(R.id.etDescription);
        etLocation = view.findViewById(R.id.etLocation);
        spinnerType = view.findViewById(R.id.spinnerType);
        btnAdd = view.findViewById(R.id.btnAdd);
        btnCancel = view.findViewById(R.id.btnCancel);
    }

    private void setupListeners() {
        if (btnAdd != null) {
            btnAdd.setOnClickListener(v -> addEvidence());
        }
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dismiss());
        }
    }

    /**
     * ✅ PURE ONLINE: Add evidence via API
     */
    private void addEvidence() {
        String description = etDescription.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String type = spinnerType.getSelectedItem().toString();

        if (description.isEmpty()) {
            Toast.makeText(getContext(), "Please enter evidence description", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!networkMonitor.isOnline()) {
            Toast.makeText(getContext(), "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> evidenceData = new HashMap<>();
        evidenceData.put("description", description);
        evidenceData.put("location", location);
        evidenceData.put("type", type);

        ApiClient.addEvidence(reportId, evidenceData, new ApiClient.ApiCallback<Object>() {
            @Override
            public void onSuccess(Object response) {
                Toast.makeText(getContext(), "Evidence added successfully", Toast.LENGTH_SHORT).show();
                dismiss();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(getContext(), "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
