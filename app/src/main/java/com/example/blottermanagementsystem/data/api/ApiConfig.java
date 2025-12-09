package com.example.blottermanagementsystem.data.api;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

public class ApiConfig {
    
    // Cloud API Base URL (for production)
    private static final String BASE_URL = "https://bms-backend-exeb.onrender.com/";
    
    // Localhost API Base URL (for development)
    // Use 10.0.2.2 for Android Emulator (maps to host machine's localhost)
    // Use your computer's IP (e.g., 192.168.1.XXX) for physical device
    // private static final String BASE_URL = "http://10.0.2.2:3000/";
    
    private static Retrofit retrofit;
    private static BlotterApiService apiService;
    
    public static Retrofit getRetrofit() {
        if (retrofit == null) {
            // Logging Interceptor for debugging
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            
            // OkHttp Client with timeout and logging
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
            
            // Retrofit Instance
            retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        }
        return retrofit;
    }
    
    public static BlotterApiService getApiService() {
        if (apiService == null) {
            apiService = getRetrofit().create(BlotterApiService.class);
        }
        return apiService;
    }
}
