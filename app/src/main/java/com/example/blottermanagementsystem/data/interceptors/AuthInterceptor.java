package com.example.blottermanagementsystem.data.interceptors;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.blottermanagementsystem.config.AppConfig;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * HTTP Interceptor for adding authentication token to requests
 */
public class AuthInterceptor implements Interceptor {
    private final Context context;
    
    public AuthInterceptor(Context context) {
        this.context = context;
    }
    
    @Override
    public Response intercept(Chain chain) throws java.io.IOException {
        Request originalRequest = chain.request();
        
        // Get token from SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences(
            AppConfig.PREFS_NAME, 
            Context.MODE_PRIVATE
        );
        String token = prefs.getString(AppConfig.PREFS_USER_TOKEN, null);
        
        // Add token to request if available
        Request.Builder requestBuilder = originalRequest.newBuilder();
        if (token != null && !token.isEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer " + token);
        }
        
        Request newRequest = requestBuilder.build();
        return chain.proceed(newRequest);
    }
}
