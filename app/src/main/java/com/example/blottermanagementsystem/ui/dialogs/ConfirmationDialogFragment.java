package com.example.blottermanagementsystem.ui.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.blottermanagementsystem.R;
import com.google.android.material.button.MaterialButton;

public class ConfirmationDialogFragment extends DialogFragment {

    private String title;
    private String message;
    private String confirmText;
    private OnConfirmListener confirmListener;
    private static OnConfirmListener staticListener; // ✅ Static reference to preserve listener

    public interface OnConfirmListener {
        void onConfirm();
    }

    public static ConfirmationDialogFragment newInstance(String title, String message, String confirmText, OnConfirmListener listener) {
        ConfirmationDialogFragment fragment = new ConfirmationDialogFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("message", message);
        args.putString("confirmText", confirmText);
        fragment.setArguments(args);
        fragment.confirmListener = listener;
        staticListener = listener; // ✅ Store in static reference
        android.util.Log.d("ConfirmationDialog", "✅ newInstance: listener set (static: " + (staticListener != null) + ")");
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_FRAME, android.R.style.Theme_Translucent_NoTitleBar);
        if (getArguments() != null) {
            title = getArguments().getString("title", "Confirm");
            message = getArguments().getString("message", "Are you sure?");
            confirmText = getArguments().getString("confirmText", "Yes, Done");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            android.view.WindowManager.LayoutParams params = getDialog().getWindow().getAttributes();
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.85);
            params.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
            getDialog().getWindow().setAttributes(params);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_confirmation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        android.util.Log.d("ConfirmationDialog", "🔵 onViewCreated called");
        
        TextView tvTitle = view.findViewById(R.id.tvTitle);
        TextView tvMessage = view.findViewById(R.id.tvMessage);
        MaterialButton btnCancel = view.findViewById(R.id.btnCancel);
        MaterialButton btnConfirm = view.findViewById(R.id.btnConfirm);

        android.util.Log.d("ConfirmationDialog", "🔵 Views found - tvTitle: " + (tvTitle != null) + ", tvMessage: " + (tvMessage != null) + ", btnCancel: " + (btnCancel != null) + ", btnConfirm: " + (btnConfirm != null));

        // Set texts
        if (tvTitle != null) tvTitle.setText(title);
        if (tvMessage != null) tvMessage.setText(message);
        if (btnConfirm != null) btnConfirm.setText(confirmText);

        // ✅ INCREASE BUTTON SIZES PROGRAMMATICALLY
        if (btnCancel != null) {
            // Set minimum height to 56dp
            btnCancel.setMinHeight((int) (56 * getResources().getDisplayMetrics().density));
            // Set padding for better touch target
            btnCancel.setPadding(16, 16, 16, 16);
        }
        
        if (btnConfirm != null) {
            // Set minimum height to 56dp
            btnConfirm.setMinHeight((int) (56 * getResources().getDisplayMetrics().density));
            // Set padding for better touch target
            btnConfirm.setPadding(16, 16, 16, 16);
        }
        
        // Set listeners
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> {
                android.util.Log.d("ConfirmationDialog", "Cancel clicked");
                dismiss();
            });
        } else {
            android.util.Log.e("ConfirmationDialog", "❌ btnCancel is NULL!");
        }
        
        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                android.util.Log.d("ConfirmationDialog", "✅ Confirm button clicked - confirmListener: " + (confirmListener != null) + ", staticListener: " + (staticListener != null));
                
                // ✅ Use static listener if instance listener is null
                OnConfirmListener listener = confirmListener != null ? confirmListener : staticListener;
                
                if (listener != null) {
                    android.util.Log.d("ConfirmationDialog", "✅ Executing callback...");
                    listener.onConfirm();
                } else {
                    android.util.Log.e("ConfirmationDialog", "❌ Both confirmListener and staticListener are NULL!");
                }
                dismiss();
            });
        } else {
            android.util.Log.e("ConfirmationDialog", "❌ btnConfirm is NULL!");
        }
    }
}
