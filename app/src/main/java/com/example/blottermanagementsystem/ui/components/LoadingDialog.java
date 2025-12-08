package com.example.blottermanagementsystem.ui.components;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

/**
 * Reusable Loading Dialog Component
 * Programmatically creates a loading dialog without layout file dependency
 */
public class LoadingDialog extends Dialog {
    private TextView messageText;
    private ProgressBar progressBar;
    private String message = "Loading...";
    
    public LoadingDialog(@NonNull Context context) {
        super(context);
    }
    
    public LoadingDialog(@NonNull Context context, String message) {
        super(context);
        this.message = message;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Create dialog layout programmatically
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);
        
        // Add progress bar
        progressBar = new ProgressBar(getContext());
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        progressParams.setMargins(0, 0, 0, 20);
        layout.addView(progressBar, progressParams);
        
        // Add message text
        messageText = new TextView(getContext());
        messageText.setText(message);
        messageText.setTextSize(16);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layout.addView(messageText, textParams);
        
        setContentView(layout);
        
        // Make dialog non-cancelable
        setCancelable(false);
        setCanceledOnTouchOutside(false);
    }
    
    public void setMessage(String newMessage) {
        this.message = newMessage;
        if (messageText != null) {
            messageText.setText(newMessage);
        }
    }
}
