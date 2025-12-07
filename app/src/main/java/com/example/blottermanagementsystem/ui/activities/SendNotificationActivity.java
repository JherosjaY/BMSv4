package com.example.blottermanagementsystem.ui.activities;

import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.database.BlotterDatabase;
import com.example.blottermanagementsystem.data.entity.Notification;
import com.example.blottermanagementsystem.data.entity.User;
import com.example.blottermanagementsystem.data.entity.Officer;
import com.example.blottermanagementsystem.utils.PreferencesManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Executors;

public class SendNotificationActivity extends BaseActivity {
    
    private RadioGroup radioGroupRecipients;
    private RadioButton radioAllUsers, radioSpecificUsers, radioSpecificOfficers, radioAllOfficers;
    private TextInputEditText etNotificationTitle, etNotificationMessage;
    private MaterialButton btnSendNotification;
    private BlotterDatabase database;
    private PreferencesManager preferencesManager;
    private List<User> selectedUsers = new ArrayList<>();
    private List<Officer> selectedOfficers = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_notification);
        
        database = BlotterDatabase.getDatabase(this);
        preferencesManager = new PreferencesManager(this);
        
        setupToolbar();
        initViews();
        setupListeners();
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Send Notification");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }
    
    private void initViews() {
        radioGroupRecipients = findViewById(R.id.radioGroupRecipients);
        radioAllUsers = findViewById(R.id.radioAllUsers);
        radioSpecificUsers = findViewById(R.id.radioSpecificUsers);
        radioSpecificOfficers = findViewById(R.id.radioSpecificOfficers);
        radioAllOfficers = findViewById(R.id.radioAllOfficers);
        etNotificationTitle = findViewById(R.id.etNotificationTitle);
        etNotificationMessage = findViewById(R.id.etNotificationMessage);
        btnSendNotification = findViewById(R.id.btnSendNotification);
    }
    
    private void setupListeners() {
        btnSendNotification.setOnClickListener(v -> sendNotification());
        
        // Specific Users - Show dialog to select users
        radioSpecificUsers.setOnClickListener(v -> showSelectUsersDialog());
        
        // Specific Officers - Show dialog to select officers
        radioSpecificOfficers.setOnClickListener(v -> showSelectOfficersDialog());
    }
    
    private void sendNotification() {
        String title = etNotificationTitle.getText().toString().trim();
        String message = etNotificationMessage.getText().toString().trim();
        
        if (title.isEmpty() || message.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show loading
        btnSendNotification.setEnabled(false);
        btnSendNotification.setText("Sending...");
        
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                int selectedId = radioGroupRecipients.getCheckedRadioButtonId();
                List<User> recipients = new ArrayList<>();
                
                if (selectedId == R.id.radioAllUsers) {
                    // Send to all users (exclude Admins and Officers)
                    recipients = database.userDao().getUsersByRole("User");
                } else if (selectedId == R.id.radioSpecificUsers) {
                    // Send to selected users
                    recipients = selectedUsers;
                } else if (selectedId == R.id.radioSpecificOfficers) {
                    // Send to selected officers (convert to users)
                    for (Officer officer : selectedOfficers) {
                        if (officer.getUserId() != null) {
                            User user = database.userDao().getUserById(officer.getUserId());
                            if (user != null) {
                                recipients.add(user);
                            }
                        }
                    }
                } else if (selectedId == R.id.radioAllOfficers) {
                    // Send to all officers (get from Officer table)
                    List<Officer> officers = database.officerDao().getAllOfficers();
                    for (Officer officer : officers) {
                        if (officer.getUserId() != null) {
                            User user = database.userDao().getUserById(officer.getUserId());
                            if (user != null) {
                                recipients.add(user);
                            }
                        }
                    }
                }
                
                // Send notifications
                if (!recipients.isEmpty()) {
                    for (User user : recipients) {
                        Notification notification = new Notification(
                            user.getId(),
                            title,
                            message,
                            "ANNOUNCEMENT"
                        );
                        database.notificationDao().insertNotification(notification);
                    }
                    
                    int finalCount = recipients.size();
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Notification sent to " + finalCount + " recipient(s)", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "No recipients selected", Toast.LENGTH_SHORT).show();
                        btnSendNotification.setEnabled(true);
                        btnSendNotification.setText("Send Notification");
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSendNotification.setEnabled(true);
                    btnSendNotification.setText("Send Notification");
                });
            }
        });
    }
    
    private void showSelectUsersDialog() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Get all users (both Google & manual sign-up)
                List<User> allUsers = database.userDao().getUsersByRole("User");
                
                runOnUiThread(() -> {
                    // Create custom dialog view
                    LinearLayout dialogView = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_select_users, null);
                    LinearLayout listContainer = dialogView.findViewById(R.id.usersRecyclerView);
                    TextView emptyStateText = dialogView.findViewById(R.id.emptyStateText);
                    MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
                    MaterialButton btnSelect = dialogView.findViewById(R.id.btnSelect);
                    
                    // Create dialog
                    AlertDialog.Builder builder = new AlertDialog.Builder(SendNotificationActivity.this);
                    builder.setView(dialogView);
                    AlertDialog dialog = builder.create();
                    
                    // Set dialog background
                    if (dialog.getWindow() != null) {
                        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                    }
                    
                    if (allUsers.isEmpty()) {
                        // Show empty state
                        emptyStateText.setVisibility(android.view.View.VISIBLE);
                        listContainer.setVisibility(android.view.View.GONE);
                    } else {
                        // Setup list with items
                        listContainer.setVisibility(android.view.View.VISIBLE);
                        emptyStateText.setVisibility(android.view.View.GONE);
                        
                        // Create list items dynamically
                        for (User user : allUsers) {
                            android.view.View itemView = getLayoutInflater().inflate(R.layout.item_selectable_user, null, false);
                            android.widget.CheckBox checkbox = itemView.findViewById(R.id.checkbox);
                            android.widget.TextView userName = itemView.findViewById(R.id.userName);
                            android.widget.TextView userEmail = itemView.findViewById(R.id.userEmail);
                            android.widget.TextView checkmarkIcon = itemView.findViewById(R.id.checkmarkIcon);
                            
                            // Set user info
                            String fullName = (user.getFirstName() != null ? user.getFirstName() : "") + " " + 
                                            (user.getLastName() != null ? user.getLastName() : "");
                            userName.setText(!fullName.trim().isEmpty() ? fullName.trim() : user.getUsername());
                            userEmail.setText(user.getEmail() != null ? user.getEmail() : "No email");
                            
                            // Check if already selected
                            boolean isSelected = false;
                            for (User selected : selectedUsers) {
                                if (selected.getId() == user.getId()) {
                                    isSelected = true;
                                    break;
                                }
                            }
                            checkbox.setChecked(isSelected);
                            checkmarkIcon.setVisibility(isSelected ? android.view.View.VISIBLE : android.view.View.GONE);
                            
                            // Handle checkbox change
                            checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                                if (isChecked) {
                                    if (!selectedUsers.contains(user)) {
                                        selectedUsers.add(user);
                                    }
                                    checkmarkIcon.setVisibility(android.view.View.VISIBLE);
                                } else {
                                    selectedUsers.remove(user);
                                    checkmarkIcon.setVisibility(android.view.View.GONE);
                                }
                                btnSelect.setText("Select (" + selectedUsers.size() + ")");
                            });
                            
                            // Handle row click to toggle checkbox - works on name/email too
                            itemView.setOnClickListener(v -> {
                                checkbox.setChecked(!checkbox.isChecked());
                            });
                            
                            // Also make the text areas clickable
                            userName.setOnClickListener(v -> {
                                checkbox.setChecked(!checkbox.isChecked());
                            });
                            userEmail.setOnClickListener(v -> {
                                checkbox.setChecked(!checkbox.isChecked());
                            });
                            
                            listContainer.addView(itemView);
                        }
                    }
                    
                    // Cancel button
                    btnCancel.setOnClickListener(v -> dialog.dismiss());
                    
                    // Select button
                    btnSelect.setOnClickListener(v -> {
                        if (selectedUsers.isEmpty()) {
                            Toast.makeText(SendNotificationActivity.this, 
                                "No users selected", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(SendNotificationActivity.this, 
                                "Selected " + selectedUsers.size() + " user(s)", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        }
                    });
                    
                    // Update button text with count
                    btnSelect.setText("Select (" + selectedUsers.size() + ")");
                    
                    dialog.show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error loading users: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }
    
    private void showSelectOfficersDialog() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Get all officers
                List<Officer> allOfficers = database.officerDao().getAllOfficers();
                
                runOnUiThread(() -> {
                    // Create custom dialog view
                    LinearLayout dialogView = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_select_officers, null);
                    LinearLayout listContainer = dialogView.findViewById(R.id.officersRecyclerView);
                    TextView emptyStateText = dialogView.findViewById(R.id.emptyStateText);
                    MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
                    MaterialButton btnSelect = dialogView.findViewById(R.id.btnSelect);
                    
                    // Create dialog
                    AlertDialog.Builder builder = new AlertDialog.Builder(SendNotificationActivity.this);
                    builder.setView(dialogView);
                    AlertDialog dialog = builder.create();
                    
                    // Set dialog background
                    if (dialog.getWindow() != null) {
                        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                    }
                    
                    if (allOfficers.isEmpty()) {
                        // Show empty state
                        emptyStateText.setVisibility(android.view.View.VISIBLE);
                        listContainer.setVisibility(android.view.View.GONE);
                    } else {
                        // Setup list with items
                        listContainer.setVisibility(android.view.View.VISIBLE);
                        emptyStateText.setVisibility(android.view.View.GONE);
                        
                        // Create list items dynamically
                        for (Officer officer : allOfficers) {
                            android.view.View itemView = getLayoutInflater().inflate(R.layout.item_selectable_user, null, false);
                            android.widget.CheckBox checkbox = itemView.findViewById(R.id.checkbox);
                            android.widget.TextView userName = itemView.findViewById(R.id.userName);
                            android.widget.TextView userEmail = itemView.findViewById(R.id.userEmail);
                            android.widget.TextView checkmarkIcon = itemView.findViewById(R.id.checkmarkIcon);
                            
                            // Set officer info
                            userName.setText(officer.getName() != null ? officer.getName() : "Officer " + officer.getId());
                            
                            // Get user email
                            String email = "No email";
                            if (officer.getUserId() != null) {
                                User user = database.userDao().getUserById(officer.getUserId());
                                if (user != null && user.getEmail() != null) {
                                    email = user.getEmail();
                                }
                            }
                            userEmail.setText(email);
                            
                            // Check if already selected
                            boolean isSelected = false;
                            for (Officer selected : selectedOfficers) {
                                if (selected.getId() == officer.getId()) {
                                    isSelected = true;
                                    break;
                                }
                            }
                            checkbox.setChecked(isSelected);
                            checkmarkIcon.setVisibility(isSelected ? android.view.View.VISIBLE : android.view.View.GONE);
                            
                            // Handle checkbox change
                            checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                                if (isChecked) {
                                    if (!selectedOfficers.contains(officer)) {
                                        selectedOfficers.add(officer);
                                    }
                                    checkmarkIcon.setVisibility(android.view.View.VISIBLE);
                                } else {
                                    selectedOfficers.remove(officer);
                                    checkmarkIcon.setVisibility(android.view.View.GONE);
                                }
                                btnSelect.setText("Select (" + selectedOfficers.size() + ")");
                            });
                            
                            // Handle row click to toggle checkbox - works on name/email too
                            itemView.setOnClickListener(v -> {
                                checkbox.setChecked(!checkbox.isChecked());
                            });
                            
                            // Also make the text areas clickable
                            userName.setOnClickListener(v -> {
                                checkbox.setChecked(!checkbox.isChecked());
                            });
                            userEmail.setOnClickListener(v -> {
                                checkbox.setChecked(!checkbox.isChecked());
                            });
                            
                            listContainer.addView(itemView);
                        }
                    }
                    
                    // Cancel button
                    btnCancel.setOnClickListener(v -> dialog.dismiss());
                    
                    // Select button
                    btnSelect.setOnClickListener(v -> {
                        if (selectedOfficers.isEmpty()) {
                            Toast.makeText(SendNotificationActivity.this, 
                                "No officers selected", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(SendNotificationActivity.this, 
                                "Selected " + selectedOfficers.size() + " officer(s)", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        }
                    });
                    
                    // Update button text with count
                    btnSelect.setText("Select (" + selectedOfficers.size() + ")");
                    
                    dialog.show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error loading officers: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }
}
