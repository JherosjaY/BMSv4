package com.example.blottermanagementsystem.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
// import com.example.blottermanagementsystem.data.database.BlotterDatabase; // Pure online
import com.example.blottermanagementsystem.data.entity.Notification;
import com.example.blottermanagementsystem.utils.PreferencesManager;
import java.util.List;
import java.util.concurrent.Executors;

public class NotificationViewModel extends AndroidViewModel {
    // private final BlotterDatabase database; // Pure online
    private final PreferencesManager preferencesManager;
    private final MutableLiveData<List<Notification>> notifications = new MutableLiveData<>();
    private final MutableLiveData<Integer> unreadCount = new MutableLiveData<>(0);
    
    public NotificationViewModel(@NonNull Application application) {
        super(application);
        // database = BlotterDatabase.getDatabase(application); // Pure online
        preferencesManager = new PreferencesManager(application);
        loadNotifications();
    }
    
    private void loadNotifications() {
        String userId = preferencesManager.getUserId();
        
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Notification> notifList = new java.util.ArrayList<>(); // Pure online
            notifications.postValue(notifList);
            
            int unread = (int) notifList.stream().filter(n -> !n.isRead()).count();
            unreadCount.postValue(unread);
        });
    }
    
    public void markAsRead(Notification notification) {
        Executors.newSingleThreadExecutor().execute(() -> {
            notification.setRead(true);
            // database.notificationDao().updateNotification(notification); // Pure online
            loadNotifications();
        });
    }
    
    public void markAllAsRead() {
        String userId = preferencesManager.getUserId();
        
        Executors.newSingleThreadExecutor().execute(() -> {
            // database.notificationDao().markAllAsRead(userId); // Pure online
            loadNotifications();
        });
    }
    
    public LiveData<List<Notification>> getNotifications() {
        return notifications;
    }
    
    public LiveData<Integer> getUnreadCount() {
        return unreadCount;
    }
}
