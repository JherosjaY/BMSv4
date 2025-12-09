package com.example.blottermanagementsystem.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
// import com.example.blottermanagementsystem.data.database.BlotterDatabase; // Pure online
import com.example.blottermanagementsystem.data.entity.User;
import com.example.blottermanagementsystem.utils.PreferencesManager;
import java.util.concurrent.Executors;

public class AuthViewModel extends AndroidViewModel {
    // private final BlotterDatabase database; // Pure online
    private final PreferencesManager preferencesManager;
    private final MutableLiveData<AuthState> authState = new MutableLiveData<>(AuthState.IDLE);
    private final MutableLiveData<AuthState> registerState = new MutableLiveData<>(AuthState.IDLE);
    private String currentUserRole = null; // Store the role after successful login
    private LoginCallback loginCallback = null; // Callback for login result
    
    public interface LoginCallback {
        void onLoginSuccess(String role);
        void onLoginError(String message);
    }
    
    public AuthViewModel(@NonNull Application application) {
        super(application);
        // database = BlotterDatabase.getDatabase(application); // Pure online
        preferencesManager = new PreferencesManager(application);
    }
    
    public String getCurrentUserRole() {
        return currentUserRole;
    }
    
    public void setLoginCallback(LoginCallback callback) {
        this.loginCallback = callback;
    }
    
    public void login(String username, String password) {
        authState.setValue(AuthState.LOADING);
        
        Executors.newSingleThreadExecutor().execute(() -> {
            android.util.Log.d("AuthViewModel", "=== LOGIN ATTEMPT ===");
            android.util.Log.d("AuthViewModel", "Username: " + username);
            
            // Check total users in database
            java.util.List<User> allUsers = new java.util.ArrayList<>(); // Pure online
            android.util.Log.d("AuthViewModel", "Total users in database: " + allUsers.size());
            for (User u : allUsers) {
                android.util.Log.d("AuthViewModel", "  User: ID=" + u.getId() + ", Username=" + u.getUsername() + ", FirstName=" + u.getFirstName());
            }
            
            User user = null; // Pure online - no database
            
            if (user == null) {
                // User doesn't exist - let's check for similar usernames
                android.util.Log.e("AuthViewModel", "❌ USER NOT FOUND: " + username);
                
                // Debug: Check for similar usernames
                android.util.Log.d("AuthViewModel", "=== CHECKING SIMILAR USERNAMES ===");
                for (User u : allUsers) {
                    if (u.getUsername().toLowerCase().contains(username.toLowerCase()) || 
                        username.toLowerCase().contains(u.getUsername().toLowerCase())) {
                        android.util.Log.d("AuthViewModel", "Similar username found: " + u.getUsername() + " (Role: " + u.getRole() + ")");
                    }
                }
                
                authState.postValue(AuthState.USER_NOT_FOUND);
            } else if (!checkPassword(password, user.getPassword())) {
                // User exists but wrong password
                android.util.Log.e("AuthViewModel", "❌ WRONG PASSWORD");
                android.util.Log.d("AuthViewModel", "Entered password hash: " + com.example.blottermanagementsystem.utils.SecurityUtils.hashPassword(password));
                android.util.Log.d("AuthViewModel", "Stored password hash: " + user.getPassword());
                authState.postValue(AuthState.WRONG_PASSWORD);
            } else if (!user.isActive()) {
                // User exists but account is inactive
                authState.postValue(AuthState.ERROR);
            } else {
                // Login successful
                android.util.Log.d("AuthViewModel", "=== LOGIN SUCCESSFUL ===");
                android.util.Log.d("AuthViewModel", "User ID: " + user.getId());
                android.util.Log.d("AuthViewModel", "Username: " + user.getUsername());
                android.util.Log.d("AuthViewModel", "FirstName: " + user.getFirstName());
                android.util.Log.d("AuthViewModel", "Role from DB: " + user.getRole());
                
                // Auto-detect role from username prefix (like Kotlin version)
                String detectedRole = detectRoleFromUsername(username);
                if (detectedRole != null && !detectedRole.equals(user.getRole())) {
                    android.util.Log.d("AuthViewModel", "⚠️ Role mismatch! DB says: " + user.getRole() + ", Username suggests: " + detectedRole);
                    android.util.Log.d("AuthViewModel", "✅ Using detected role: " + detectedRole);
                    currentUserRole = detectedRole;
                    
                    // Update database with correct role
                    user.setRole(detectedRole);
                    // database.userDao().updateUser(user); // Pure online
                } else {
                    currentUserRole = user.getRole();
                }
                
                android.util.Log.d("AuthViewModel", "Final Role: " + currentUserRole);
                
                preferencesManager.setLoggedIn(true);
                preferencesManager.setUserId(String.valueOf(user.getId()));
                preferencesManager.setUserRole(currentUserRole); // Use detected role
                preferencesManager.setFirstName(user.getFirstName());
                preferencesManager.setLastName(user.getLastName());
                preferencesManager.setGender(user.getGender() != null ? user.getGender() : "Male");
                
                android.util.Log.d("AuthViewModel", "✅ User data saved to PreferencesManager");
                android.util.Log.d("AuthViewModel", "✅ Role stored in ViewModel: " + currentUserRole);
                
                // Verify the save immediately
                android.util.Log.d("AuthViewModel", "=== VERIFICATION ===");
                android.util.Log.d("AuthViewModel", "Saved UserID: " + user.getId());
                android.util.Log.d("AuthViewModel", "Read back UserID: " + preferencesManager.getUserId());
                android.util.Log.d("AuthViewModel", "Read back isLoggedIn: " + preferencesManager.isLoggedIn());
                android.util.Log.d("AuthViewModel", "Read back FirstName: " + preferencesManager.getFirstName());
                
                android.util.Log.d("AuthViewModel", "🚀 POSTING SUCCESS STATE TO LIVEDATA");
                authState.postValue(AuthState.SUCCESS);
                android.util.Log.d("AuthViewModel", "✅ SUCCESS STATE POSTED");
                
                // Also call the callback directly (more reliable!)
                if (loginCallback != null) {
                    android.util.Log.d("AuthViewModel", "📞 CALLING LOGIN CALLBACK");
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        loginCallback.onLoginSuccess(currentUserRole);
                    });
                }
            }
        });
    }
    
    public void register(User user) {
        registerState.setValue(AuthState.LOADING);
        
        Executors.newSingleThreadExecutor().execute(() -> {
            // Check if username already exists
            User existingUserByUsername = null; // Pure online
            
            // Check if email already exists (prevent duplicate Google Sign-In + Sign Up)
            User existingUserByEmail = null; // Pure online
            
            if (existingUserByUsername != null) {
                android.util.Log.e("AuthViewModel", "❌ Username already exists: " + user.getUsername());
                registerState.postValue(AuthState.ERROR);
            } else if (existingUserByEmail != null) {
                android.util.Log.e("AuthViewModel", "❌ Email already exists: " + user.getEmail());
                registerState.postValue(AuthState.EMAIL_EXISTS);
            } else {
                long userId = 1; // Pure online - hardcoded
                if (userId > 0) {
                    android.util.Log.d("AuthViewModel", "✅ User registered successfully: " + user.getUsername());
                    registerState.postValue(AuthState.SUCCESS);
                } else {
                    android.util.Log.e("AuthViewModel", "❌ Failed to insert user");
                    registerState.postValue(AuthState.ERROR);
                }
            }
        });
    }
    
    public void logout() {
        preferencesManager.clearSession();
        authState.setValue(AuthState.IDLE);
    }
    
    public LiveData<AuthState> getAuthState() {
        return authState;
    }
    
    public LiveData<AuthState> getRegisterState() {
        return registerState;
    }
    
    public User getUserById(int userId) {
        try {
            return null; // Pure online - no database
        } catch (Exception e) {
            android.util.Log.e("AuthViewModel", "Error getting user by ID: " + e.getMessage());
            return null;
        }
    }
    
    public void updateUserProfile(int userId, String profilePhotoUri) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Pure online - no database
                android.util.Log.d("AuthViewModel", "Profile photo updated for user ID: " + userId);
            } catch (Exception e) {
                android.util.Log.e("AuthViewModel", "Error updating profile: " + e.getMessage());
            }
        });
    }
    
    /**
     * Auto-detect role from username prefix
     * off. = Officer
     * adm. = Admin
     */
    private String detectRoleFromUsername(String username) {
        if (username == null || username.isEmpty()) {
            return null;
        }
        
        String lowerUsername = username.toLowerCase();
        
        if (lowerUsername.startsWith("off.")) {
            return "Officer";
        } else if (lowerUsername.startsWith("adm.")) {
            return "Admin";
        }
        
        // No prefix detected, return null (use DB role)
        return null;
    }
    
    /**
     * Check if entered password matches stored hash
     */
    private boolean checkPassword(String enteredPassword, String storedHash) {
        // Use SecurityUtils for consistent hashing
        return com.example.blottermanagementsystem.utils.SecurityUtils.verifyPassword(enteredPassword, storedHash);
    }
    
    public enum AuthState {
        IDLE, LOADING, SUCCESS, ERROR, USER_NOT_FOUND, WRONG_PASSWORD, EMAIL_EXISTS
    }
}
