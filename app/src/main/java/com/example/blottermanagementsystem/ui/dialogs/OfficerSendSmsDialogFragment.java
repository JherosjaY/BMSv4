package com.example.blottermanagementsystem.ui.dialogs;

import android.os.Bundle;
import android.text.TextWatcher;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.utils.OfficerSmsNotificationManager;
import com.example.blottermanagementsystem.utils.PhilippinePhoneValidator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputLayout;

/**
 * ✅ OFFICER ROLE ONLY
 * SMS Sending Dialog Fragment for Officer Role
 * Allows officers to send SMS notifications to respondents/witnesses
 */
public class OfficerSendSmsDialogFragment extends DialogFragment {
    
    private TextInputLayout tilPhoneNumber;
    private EditText etPhoneNumber;
    private Spinner spinnerMessageType;
    private TextView tvMessagePreview;
    private TextView tvCharacterCount;
    private MaterialButton btnSend, btnCancel;
    private MaterialCardView cvMessagePreview;
    
    private String caseNumber;
    private String respondentName;
    private String hearingDate;
    private String hearingTime;
    private String hearingVenue;
    private String resolutionType;
    private OnSmsSentListener listener;
    
    public interface OnSmsSentListener {
        void onSmsSent(String phoneNumber, String messageType);
        void onSmsFailed(String errorMessage);
    }
    
    public static OfficerSendSmsDialogFragment newInstance(String caseNumber, String respondentName,
                                                     String hearingDate, String hearingTime,
                                                     String hearingVenue, String resolutionType, OnSmsSentListener listener) {
        OfficerSendSmsDialogFragment fragment = new OfficerSendSmsDialogFragment();
        Bundle args = new Bundle();
        args.putString("case_number", caseNumber);
        args.putString("respondent_name", respondentName);
        args.putString("hearing_date", hearingDate);
        args.putString("hearing_time", hearingTime);
        args.putString("hearing_venue", hearingVenue);
        args.putString("resolution_type", resolutionType);
        fragment.setArguments(args);
        fragment.listener = listener;
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Dialog_MinWidth);
        
        if (getArguments() != null) {
            caseNumber = getArguments().getString("case_number");
            respondentName = getArguments().getString("respondent_name");
            hearingDate = getArguments().getString("hearing_date");
            hearingTime = getArguments().getString("hearing_time");
            hearingVenue = getArguments().getString("hearing_venue");
            resolutionType = getArguments().getString("resolution_type");
        }
    }
    
    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            android.view.WindowManager.LayoutParams params = getDialog().getWindow().getAttributes();
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
            getDialog().getWindow().setAttributes(params);
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.officer_dialog_send_sms, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupSpinner();
        setupListeners();
    }
    
    private void initViews(View view) {
        tilPhoneNumber = view.findViewById(R.id.tilPhoneNumber);
        etPhoneNumber = view.findViewById(R.id.etPhoneNumber);
        spinnerMessageType = view.findViewById(R.id.spinnerMessageType);
        tvMessagePreview = view.findViewById(R.id.tvMessagePreview);
        tvCharacterCount = view.findViewById(R.id.tvCharacterCount);
        cvMessagePreview = view.findViewById(R.id.cvMessagePreview);
        btnSend = view.findViewById(R.id.btnSend);
        btnCancel = view.findViewById(R.id.btnCancel);
    }
    
    private void setupSpinner() {
        // ✅ Message Type Spinner with dark theme
        String[] messageTypes = {
            "Hearing Notice",
            "Initial Notice",
            "Reminder",
            "Resolution Notice"
        };
        
        ArrayAdapter<String> messageAdapter = new ArrayAdapter<String>(getContext(),
            android.R.layout.simple_spinner_item, messageTypes) {
            @Override
            public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                android.view.View view = super.getView(position, convertView, parent);
                android.widget.TextView textView = (android.widget.TextView) view;
                textView.setTextColor(getContext().getColor(android.R.color.white));
                return view;
            }
            
            @Override
            public android.view.View getDropDownView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                android.view.View view = super.getDropDownView(position, convertView, parent);
                android.widget.TextView textView = (android.widget.TextView) view;
                textView.setBackgroundColor(getContext().getColor(com.example.blottermanagementsystem.R.color.primary_dark_blue));
                textView.setTextColor(getContext().getColor(android.R.color.white));
                return view;
            }
        };
        messageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMessageType.setAdapter(messageAdapter);
    }
    
    private void setupListeners() {
        // Update preview when message type changes
        spinnerMessageType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateMessagePreview();
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        
        // Update character count as user types
        etPhoneNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateMessagePreview();
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        btnSend.setOnClickListener(v -> sendSms());
        btnCancel.setOnClickListener(v -> dismiss());
    }
    
    private void updateMessagePreview() {
        int selectedPosition = spinnerMessageType.getSelectedItemPosition();
        String message = "";
        
        switch (selectedPosition) {
            case 0: // Hearing Notice
                message = "This is to notify you that a hearing has been scheduled for Case #" + caseNumber + ".\n\n" +
                         "Date: " + (hearingDate != null && !hearingDate.isEmpty() ? hearingDate : "TBD") + "\n" +
                         "Time: " + (hearingTime != null && !hearingTime.isEmpty() ? hearingTime : "TBD") + "\n" +
                         "Venue: " + (hearingVenue != null && !hearingVenue.isEmpty() ? hearingVenue : "TBD") + "\n\n" +
                         "Your attendance is mandatory. Please bring a valid ID.";
                break;
            case 1: // Initial Notice
                message = "You are hereby notified regarding Case #" + caseNumber + ".\n\n" +
                         "You are required to appear within three (3) days from receipt of this notice for investigation and settlement proceedings.\n\n" +
                         "Please bring a valid identification document.";
                break;
            case 2: // Reminder
                message = "This is a reminder regarding Case #" + caseNumber + ".\n\n" +
                         "Please ensure your attendance as previously scheduled. Your cooperation is essential for the resolution of this matter.\n\n" +
                         "Thank you.";
                break;
            case 3: // Resolution Notice
                message = "Case #" + caseNumber + " has been officially resolved.\n\n" +
                         "Resolution: " + (resolutionType != null ? resolutionType : "Pending") + "\n\n" +
                         "For further inquiries or concerns, please contact the Barangay Hall.";
                break;
        }
        
        // ✅ NO TRUNCATION - Show full message with exact values
        android.util.Log.d("SendSMS", "📱 Message Preview (" + message.length() + " chars): " + message);
        
        tvMessagePreview.setText(message);
        tvCharacterCount.setText(message.length() + " characters");
    }
    
    private void sendSms() {
        String phoneNumber = etPhoneNumber.getText().toString().trim();
        
        if (phoneNumber.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a phone number", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // ✅ Validate Philippine phone number
        if (!PhilippinePhoneValidator.isValidPhilippineNumber(phoneNumber)) {
            String provider = PhilippinePhoneValidator.getTelecomProvider(phoneNumber);
            Toast.makeText(getContext(), 
                "Invalid phone number", 
                Toast.LENGTH_SHORT).show();
            return;
        }
        
        // ✅ Show which telecom provider
        String provider = PhilippinePhoneValidator.getTelecomProvider(phoneNumber);
        android.util.Log.d("SendSMS", "✅ Valid number from: " + provider);
        
        int messageTypePosition = spinnerMessageType.getSelectedItemPosition();
        
        OfficerSmsNotificationManager smsManager = new OfficerSmsNotificationManager(getContext());
        
        // Disable send button during sending
        btnSend.setEnabled(false);
        btnSend.setText("Sending...");
        
        switch (messageTypePosition) {
            case 0: // Hearing Notice
                smsManager.sendHearingNotice(phoneNumber, caseNumber, respondentName, hearingDate, hearingTime,
                    new OfficerSmsNotificationManager.SmsCallback() {
                        @Override
                        public void onSuccess(String message) {
                            handleSmsSuccess(message, "HEARING_NOTICE");
                        }
                        
                        @Override
                        public void onError(String errorMessage) {
                            handleSmsError(errorMessage);
                        }
                    });
                break;
            case 1: // Initial Notice
                smsManager.sendInitialNotice(phoneNumber, caseNumber, respondentName, "Investigation ongoing",
                    new OfficerSmsNotificationManager.SmsCallback() {
                        @Override
                        public void onSuccess(String message) {
                            handleSmsSuccess(message, "INITIAL_NOTICE");
                        }
                        
                        @Override
                        public void onError(String errorMessage) {
                            handleSmsError(errorMessage);
                        }
                    });
                break;
            case 2: // Reminder
                smsManager.sendReminder(phoneNumber, caseNumber, respondentName, 1,
                    new OfficerSmsNotificationManager.SmsCallback() {
                        @Override
                        public void onSuccess(String message) {
                            handleSmsSuccess(message, "REMINDER");
                        }
                        
                        @Override
                        public void onError(String errorMessage) {
                            handleSmsError(errorMessage);
                        }
                    });
                break;
            case 3: // Resolution Notice
                smsManager.sendResolutionNotification(phoneNumber, caseNumber, respondentName, resolutionType,
                    new OfficerSmsNotificationManager.SmsCallback() {
                        @Override
                        public void onSuccess(String message) {
                            handleSmsSuccess(message, "RESOLUTION_NOTICE");
                        }
                        
                        @Override
                        public void onError(String errorMessage) {
                            handleSmsError(errorMessage);
                        }
                    });
                break;
        }
    }
    
    private void handleSmsSuccess(String message, String messageType) {
        getActivity().runOnUiThread(() -> {
            Toast.makeText(getContext(), "SMS sent successfully", Toast.LENGTH_SHORT).show();
            if (listener != null) {
                listener.onSmsSent(etPhoneNumber.getText().toString(), messageType);
            }
            dismiss();
        });
    }
    
    private void handleSmsError(String errorMessage) {
        getActivity().runOnUiThread(() -> {
            btnSend.setEnabled(true);
            btnSend.setText("📱 Send SMS");
            Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
            if (listener != null) {
                listener.onSmsFailed(errorMessage);
            }
        });
    }
}
