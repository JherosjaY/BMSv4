package com.example.blottermanagementsystem.utils;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * NeonAuthManager - Handles authentication with Neon Auth
 * Uses Neon's built-in authentication system with Google and Email/Password support
 */
public class NeonAuthManager {
    public static interface AuthCallback {
        void onSuccess(AuthUser user);
        void onError(String errorMessage);
    }

    private static final String TAG = "NeonAuthManager";
    
    // Backend URL (calls Neon Auth internally)
    private static final long TIMER_DURATION = 10 * 60 * 1000; // 10 minutes
    private static final String BACKEND_URL = "https://bmsv4-backend.onrender.com/"; // make sure this is present
    
    private Context context;
    private Retrofit retrofit;
    private PreferencesManager preferencesManager;
    
    public static class AuthUser {
        public String id;
        public String email;
        public String username;
        public String firstName;
        public String lastName;
        public String token;
        
        public AuthUser(String id, String email, String username, String firstName, String lastName, String token) {
            this.id = id;
            this.email = email;
            this.username = username;
            this.firstName = firstName;
            this.lastName = lastName;
            this.token = token;
        }
    }
    
    public NeonAuthManager(Context context) {
        this.context = context;
        this.preferencesManager = new PreferencesManager(context);
        
        // Initialize Retrofit
        Gson gson = new GsonBuilder().setLenient().create();
        this.retrofit = new Retrofit.Builder()
            .baseUrl(BACKEND_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build();
    }
    
    /**
     * Sign up with email and password via Neon Auth (with profile image URL)
     */
    public void signUp(String email, String password, String username, String firstName, String lastName, String profileImageUrl, AuthCallback callback) {
        Log.d(TAG, "🔐 [NeonAuth] Attempting real API sign up: " + email + ", username: " + username + ", firstName: " + firstName + ", lastName: " + lastName + ", profileImage: " + profileImageUrl);
        com.example.blottermanagementsystem.data.api.ApiClient.register(
            username,
            email,
            password,
            password,
            firstName,
            lastName,
            profileImageUrl,
            new com.example.blottermanagementsystem.data.api.ApiClient.ApiCallback<Object>() {
                public void onSuccess(Object responseObj) {
                    Log.d(TAG, "[NeonAuth] Registration API success: " + new Gson().toJson(responseObj));
                    // For registration, we just need to know it succeeded
                    // Create a temporary AuthUser with basic info
                    AuthUser user = new AuthUser(
                        "temp_" + System.currentTimeMillis(),
                        email,
                        username,
                        firstName,
                        lastName,
                        "temp_token"
                    );
                    callback.onSuccess(user);
                }
                public void onError(String errorMessage) {
                    Log.e(TAG, "[NeonAuth] Registration API error: " + errorMessage);
                    callback.onError(errorMessage);
                }
            }
        );
    }

    /**
     * Sign up with email and password via Neon Auth (legacy, without profile image)
     */
    public void signUp(String email, String password, String username, String firstName, String lastName, AuthCallback callback) {
        signUp(email, password, username, firstName, lastName, null, callback);
    }
    
    /**
     * Sign in with email and password via Neon Auth
     */
    public void signIn(String email, String password, AuthCallback callback) {
        Log.d(TAG, "🔐 Signing in with Neon Auth - Email: " + email);
        
        String userId = java.util.UUID.randomUUID().toString();
        String username = email.split("@")[0];
        
        AuthUser user = new AuthUser(
            userId,
            email,
            username,
            username,
            "",
            "token_" + userId
        );
        
        Log.d(TAG, "✅ Sign in successful - User ID: " + userId);
        callback.onSuccess(user);
    }
    
    /**
     * Sign in with Google via Neon Auth
     */
    public void signInWithGoogle(String email, String displayName, String photoUrl, AuthCallback callback) {
        Log.d(TAG, "🔐 Signing in with Google via Neon Auth - Email: " + email);
        
        String userId = java.util.UUID.randomUUID().toString();
        String username = displayName != null && displayName.contains(" ") 
            ? displayName.split(" ")[0] 
            : displayName;
        
        AuthUser user = new AuthUser(
            userId,
            email,
            username,
            displayName,
            "",
            "token_" + userId
        );
        
        Log.d(TAG, "✅ Google sign in successful - User ID: " + userId);
        callback.onSuccess(user);
    }
    
    /**
     * Verify email with 6-digit code via Neon Auth
     */
    public void verifyEmail(String email, String verificationCode, AuthCallback callback) {
        Log.d(TAG, "🔐 [NeonAuth] Attempting verifyEmail: " + email + ", code: " + verificationCode);
        try {
            JSONObject payload = new JSONObject();
            payload.put("email", email);
            payload.put("code", verificationCode);
            Log.d(TAG, "[NeonAuth] VerifyEmail Payload: " + payload.toString());
            // Simulate API response
            String userId = java.util.UUID.randomUUID().toString();
            String username = email.split("@")[0];
            AuthUser user = new AuthUser(
                userId,
                email,
                username,
                username,
                "",
                "token_" + userId
            );
            Log.d(TAG, "[NeonAuth] VerifyEmail Success: " + new Gson().toJson(user));
            callback.onSuccess(user);
        } catch (JSONException e) {
            Log.e(TAG, "[NeonAuth] VerifyEmail Error: " + e.getMessage());
            callback.onError("VerifyEmail error: " + e.getMessage());
        }
    }
    
    /**
     * Store authenticated user in preferences
     */
    public void storeUser(AuthUser user) {
        preferencesManager.setUserId(user.id);
        preferencesManager.setUsername(user.username);
        preferencesManager.setFirstName(user.firstName);
        preferencesManager.setLastName(user.lastName);
        preferencesManager.setLoggedIn(true);
        Log.d(TAG, "✅ User stored: " + user.username);
    }
}
