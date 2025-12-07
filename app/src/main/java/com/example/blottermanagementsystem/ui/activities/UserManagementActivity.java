package com.example.blottermanagementsystem.ui.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.AutoCompleteTextView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.database.BlotterDatabase;
import com.example.blottermanagementsystem.data.entity.User;
import com.example.blottermanagementsystem.ui.adapters.UserAdapter;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class UserManagementActivity extends BaseActivity {
    
    private AutoCompleteTextView etSearch;
    private RecyclerView recyclerUsers;
    private android.widget.LinearLayout emptyState;
    private androidx.cardview.widget.CardView emptyStateCard;
    
    private BlotterDatabase database;
    private List<User> usersList = new ArrayList<>();
    private UserAdapter userAdapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);
        
        database = BlotterDatabase.getDatabase(this);
        setupToolbar();
        initViews();
        setupRecyclerView();
        setupListeners();
        loadUsers();
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("User Management");
        }
        
        // Handle back button click
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }
    }
    
    private void initViews() {
        etSearch = findViewById(R.id.etSearch);
        recyclerUsers = findViewById(R.id.recyclerViewUsers);
        emptyState = findViewById(R.id.emptyState);
        emptyStateCard = findViewById(R.id.emptyStateCard);
        
        // Setup filter button click listener
        if (etSearch != null) {
            etSearch.setOnClickListener(v -> showFilterMenu());
        }
    }
    
    private void setupRecyclerView() {
        if (recyclerUsers != null) {
            recyclerUsers.setLayoutManager(new LinearLayoutManager(this));
            userAdapter = new UserAdapter(usersList, user -> showUserOptionsDialog(user));
            recyclerUsers.setAdapter(userAdapter);
        }
    }
    
    private void setupListeners() {
        if (etSearch != null) {
            // Setup search suggestions
            setupSearchSuggestions();
            
            etSearch.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterUsers(s.toString());
                    updateSearchSuggestions(s.toString());
                }
                
                @Override
                public void afterTextChanged(android.text.Editable s) {}
            });
        }
    }
    
    private void setupSearchSuggestions() {
        List<String> suggestions = new ArrayList<>();
        for (User user : usersList) {
            suggestions.add(user.getFirstName() + " " + user.getLastName());
            suggestions.add(user.getUsername());
            if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                suggestions.add(user.getEmail());
            }
        }
        
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            suggestions
        );
        etSearch.setAdapter(adapter);
    }
    
    private void updateSearchSuggestions(String query) {
        List<String> suggestions = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        
        for (User user : usersList) {
            String fullName = user.getFirstName() + " " + user.getLastName();
            if (fullName.toLowerCase().contains(lowerQuery)) {
                suggestions.add(fullName);
            }
            if (user.getUsername().toLowerCase().contains(lowerQuery)) {
                suggestions.add(user.getUsername());
            }
            if (user.getEmail() != null && user.getEmail().toLowerCase().contains(lowerQuery)) {
                suggestions.add(user.getEmail());
            }
        }
        
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            suggestions
        );
        etSearch.setAdapter(adapter);
    }
    
    private void loadUsers() {
        com.example.blottermanagementsystem.utils.GlobalLoadingManager.show(this, "Loading users...");
        
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                usersList.clear();
                List<User> allUsers = database.userDao().getAllUsers();
                
                // Filter to show ONLY User role accounts (exclude Admin and Officer)
                for (User user : allUsers) {
                    if (user.getRole() != null && user.getRole().equalsIgnoreCase("USER")) {
                        usersList.add(user);
                    }
                }
                
                runOnUiThread(() -> {
                    com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
                    if (userAdapter != null) {
                        userAdapter.notifyDataSetChanged();
                    }
                    updateEmptyState();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
                    Toast.makeText(this, "Error loading users: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void filterUsers(String query) {
        List<User> filteredList = new ArrayList<>();
        for (User user : usersList) {
            if (user.getFirstName().toLowerCase().contains(query.toLowerCase()) ||
                user.getLastName().toLowerCase().contains(query.toLowerCase()) ||
                user.getUsername().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(user);
            }
        }
        if (userAdapter != null) {
            userAdapter.notifyDataSetChanged();
        }
        updateEmptyState();
    }
    
    private void updateEmptyState() {
        if (userAdapter != null && userAdapter.getItemCount() == 0) {
            if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
            if (recyclerUsers != null) recyclerUsers.setVisibility(View.GONE);
        } else {
            if (emptyState != null) emptyState.setVisibility(View.GONE);
            if (recyclerUsers != null) recyclerUsers.setVisibility(View.VISIBLE);
        }
    }
    
    private void showUserOptionsDialog(User user) {
        // Create dialog with beautiful XML layout
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        
        // Inflate XML layout directly
        android.view.View dialogView = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_user_details, null);
        builder.setView(dialogView);
        
        // Get views from layout
        TextView tvInitials = dialogView.findViewById(R.id.tvInitials);
        TextView tvUserName = dialogView.findViewById(R.id.tvUserName);
        Chip chipStatus = dialogView.findViewById(R.id.chipStatus);
        TextView tvUsername = dialogView.findViewById(R.id.tvUsername);
        TextView tvEmail = dialogView.findViewById(R.id.tvEmail);
        TextView tvRole = dialogView.findViewById(R.id.tvRole);
        TextView tvDateJoined = dialogView.findViewById(R.id.tvDateJoined);
        com.google.android.material.button.MaterialButton btnTerminate = dialogView.findViewById(R.id.btnTerminate);
        com.google.android.material.button.MaterialButton btnDelete = dialogView.findViewById(R.id.btnDelete);
        com.google.android.material.button.MaterialButton btnClose = dialogView.findViewById(R.id.btnClose);
        
        // Populate data
        String fullName = user.getFirstName() + " " + user.getLastName();
        String initials = getInitials(user.getFirstName(), user.getLastName());
        
        tvInitials.setText(initials);
        tvUserName.setText(fullName);
        tvUsername.setText(user.getUsername());
        tvEmail.setText(user.getEmail() != null && !user.getEmail().isEmpty() ? user.getEmail() : "No email");
        tvRole.setText(user.getRole());
        
        // Format date joined
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
        tvDateJoined.setText(sdf.format(new java.util.Date(user.getAccountCreated())));
        
        // Set status chip
        if (user.isActive()) {
            chipStatus.setText("Active");
            chipStatus.setChipBackgroundColorResource(R.color.success_green);
        } else {
            chipStatus.setText("Inactive");
            chipStatus.setChipBackgroundColorResource(R.color.error_red);
        }
        
        // Create dialog
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        
        // Make dialog background transparent to show custom XML background
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        // Set button listeners
        btnTerminate.setOnClickListener(v -> {
            terminateUser(user);
            dialog.dismiss();
        });
        
        btnDelete.setOnClickListener(v -> {
            confirmDeleteUser(user);
            dialog.dismiss();
        });
        
        btnClose.setOnClickListener(v -> dialog.dismiss());
        
        // Show dialog
        dialog.show();
    }
    
    private String getInitials(String firstName, String lastName) {
        String initials = "";
        if (firstName != null && !firstName.isEmpty()) {
            initials += firstName.charAt(0);
        }
        if (lastName != null && !lastName.isEmpty()) {
            initials += lastName.charAt(0);
        }
        return initials.toUpperCase();
    }
    
    private void terminateUser(User user) {
        // Inflate custom dialog layout
        android.view.View dialogView = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_confirm_terminate_user, null);
        
        // Get views
        android.widget.TextView tvUserName = dialogView.findViewById(R.id.tvUserName);
        com.google.android.material.button.MaterialButton btnConfirmTerminate = dialogView.findViewById(R.id.btnConfirmTerminate);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        
        // Set user name
        tvUserName.setText(user.getFirstName() + " " + user.getLastName());
        
        // Create dialog
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setView(dialogView);
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        
        // Make background transparent
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        // Cancel button
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        // Terminate button
        btnConfirmTerminate.setOnClickListener(v -> {
            // Deactivate user in database
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    user.setActive(false);
                    database.userDao().updateUser(user);
                    
                    runOnUiThread(() -> {
                        Toast.makeText(UserManagementActivity.this, "User terminated successfully", Toast.LENGTH_SHORT).show();
                        loadUsers(); // Refresh list
                        dialog.dismiss();
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(UserManagementActivity.this, "Error terminating user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });
        
        dialog.show();
    }
    
    private void confirmDeleteUser(User user) {
        // Inflate custom dialog layout
        android.view.View dialogView = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_confirm_delete_user, null);
        
        // Get views
        android.widget.TextView tvUserName = dialogView.findViewById(R.id.tvUserName);
        com.google.android.material.button.MaterialButton btnConfirmDelete = dialogView.findViewById(R.id.btnConfirmDelete);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        
        // Set user name
        tvUserName.setText(user.getFirstName() + " " + user.getLastName());
        
        // Create dialog
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setView(dialogView);
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        
        // Make background transparent
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        // Cancel button
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        // Delete button
        btnConfirmDelete.setOnClickListener(v -> {
            // Delete user from database
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    database.userDao().deleteUser(user);
                    
                    runOnUiThread(() -> {
                        Toast.makeText(UserManagementActivity.this, "User deleted successfully", Toast.LENGTH_SHORT).show();
                        loadUsers(); // Refresh list - user will disappear from UI
                        dialog.dismiss();
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(UserManagementActivity.this, "Error deleting user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });
        
        dialog.show();
    }
    
    private void showFilterMenu() {
        android.widget.PopupMenu popupMenu = new android.widget.PopupMenu(this, etSearch);
        popupMenu.getMenuInflater().inflate(R.menu.menu_user_filter, popupMenu.getMenu());
        
        // Align popup to the right
        popupMenu.setGravity(android.view.Gravity.END);
        
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_sort_name) {
                sortUsersByName();
                return true;
            } else if (itemId == R.id.action_sort_date) {
                sortUsersByDate();
                return true;
            } else if (itemId == R.id.action_filter_active) {
                filterByActive();
                return true;
            } else if (itemId == R.id.action_filter_inactive) {
                filterByInactive();
                return true;
            }
            return false;
        });
        
        popupMenu.show();
    }
    
    private void sortUsersByName() {
        java.util.Collections.sort(usersList, (u1, u2) -> 
            (u1.getFirstName() + u1.getLastName()).compareTo(u2.getFirstName() + u2.getLastName())
        );
        if (userAdapter != null) {
            userAdapter.notifyDataSetChanged();
        }
        Toast.makeText(this, "Sorted by name", Toast.LENGTH_SHORT).show();
    }
    
    private void sortUsersByDate() {
        java.util.Collections.sort(usersList, (u1, u2) -> 
            Long.compare(u2.getAccountCreated(), u1.getAccountCreated())
        );
        if (userAdapter != null) {
            userAdapter.notifyDataSetChanged();
        }
        Toast.makeText(this, "Sorted by date (newest first)", Toast.LENGTH_SHORT).show();
    }
    
    private void filterByActive() {
        List<User> filteredList = new ArrayList<>();
        for (User user : usersList) {
            if (user.isActive()) {
                filteredList.add(user);
            }
        }
        if (userAdapter != null) {
            userAdapter.updateUsers(filteredList);
        }
        Toast.makeText(this, "Showing active users only", Toast.LENGTH_SHORT).show();
    }
    
    private void filterByInactive() {
        List<User> filteredList = new ArrayList<>();
        for (User user : usersList) {
            if (!user.isActive()) {
                filteredList.add(user);
            }
        }
        if (userAdapter != null) {
            userAdapter.updateUsers(filteredList);
        }
        Toast.makeText(this, "Showing inactive users only", Toast.LENGTH_SHORT).show();
    }
}
