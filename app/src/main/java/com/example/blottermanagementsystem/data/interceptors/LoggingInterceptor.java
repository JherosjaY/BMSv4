package com.example.blottermanagementsystem.data.interceptors;

import android.util.Log;

import com.example.blottermanagementsystem.config.AppConfig;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * HTTP Interceptor for logging API requests and responses
 */
public class LoggingInterceptor implements Interceptor {
    private static final String TAG = AppConfig.LOG_TAG;
    
    @Override
    public Response intercept(Chain chain) throws java.io.IOException {
        Request request = chain.request();
        
        // Log request
        long startTime = System.currentTimeMillis();
        Log.d(TAG, "→ Sending request: " + request.url());
        Log.d(TAG, "  Method: " + request.method());
        
        // Proceed with request
        Response response = chain.proceed(request);
        
        // Log response
        long duration = System.currentTimeMillis() - startTime;
        Log.d(TAG, "← Received response: " + response.code() + " (" + duration + "ms)");
        Log.d(TAG, "  Message: " + response.message());
        
        return response;
    }
}
