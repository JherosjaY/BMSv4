package com.example.blottermanagementsystem.utils;

import android.content.Context;
import android.util.Log;
import com.example.blottermanagementsystem.data.repository.ApiRepository;
import com.example.blottermanagementsystem.data.api.BlotterApiService;
import com.google.firebase.messaging.FirebaseMessaging;

public class FCMHelper {
    private static final String TAG = "FCMHelper";
    private final Context context;
    private final PreferencesManager preferencesManager;
    private final ApiRepository apiRepository;
    
    public FCMHelper(Context context) {
        this.context = context;
        this.preferencesManager = new PreferencesManager(context);
        this.apiRepository = new ApiRepository();
    }
    
    public void initializeFCM() {
        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    String token = task.getResult();
                    Log.d(TAG, "FCM Token: " + token);
                    
                    preferencesManager.saveString("fcm_token", token);
                    
                    String userId = preferencesManager.getUserId();
                    if (userId != null && !userId.isEmpty()) {
                        try {
                            int userIdInt = Integer.parseInt(userId);
                            if (userIdInt > 0) {
                                sendTokenToServer(userIdInt, token);
                            }
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "Invalid userId format: " + userId);
                        }
                    }
                } else {
                    Log.e(TAG, "Failed to get FCM token", task.getException());
                }
            });
    }
    
    private void sendTokenToServer(int userId, String token) {
        BlotterApiService.FcmTokenRequest request = 
            new BlotterApiService.FcmTokenRequest(userId, token);
        
        apiRepository.updateFcmToken(request, new ApiRepository.ApiCallback<Object>() {
            @Override
            public void onSuccess(Object data) {
                Log.d(TAG, "FCM token sent to server");
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to send FCM token: " + error);
            }
        });
    }
    
    public void subscribeToTopic(String topic) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Subscribed to topic: " + topic);
                } else {
                    Log.e(TAG, "Failed to subscribe to topic", task.getException());
                }
            });
    }
    
    public void unsubscribeFromTopic(String topic) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Unsubscribed from topic: " + topic);
                } else {
                    Log.e(TAG, "Failed to unsubscribe from topic", task.getException());
                }
            });
    }
}
