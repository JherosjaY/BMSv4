package com.example.blottermanagementsystem.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
// import com.example.blottermanagementsystem.data.database.BlotterDatabase; // Pure online
import com.example.blottermanagementsystem.data.entity.BlotterReport;
import com.example.blottermanagementsystem.data.entity.Officer;
import com.example.blottermanagementsystem.data.entity.User;
import java.util.List;
import java.util.concurrent.Executors;

public class DashboardViewModel extends AndroidViewModel {
    // private final BlotterDatabase database; // Pure online
    private final MutableLiveData<List<BlotterReport>> allReports = new MutableLiveData<>();
    private final MutableLiveData<List<User>> allUsers = new MutableLiveData<>();
    private final MutableLiveData<List<Officer>> allOfficers = new MutableLiveData<>();
    private final MutableLiveData<DashboardStats> dashboardStats = new MutableLiveData<>();
    
    public DashboardViewModel(@NonNull Application application) {
        super(application);
        // database = BlotterDatabase.getDatabase(application); // Pure online
        loadData();
    }
    
    private void loadData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<BlotterReport> reports = new java.util.ArrayList<>(); // Pure online
            List<User> users = new java.util.ArrayList<>(); // Pure online
            List<Officer> officers = new java.util.ArrayList<>(); // Pure online
            
            allReports.postValue(reports);
            allUsers.postValue(users);
            allOfficers.postValue(officers);
            
            DashboardStats stats = new DashboardStats(
                reports.size(),
                (int) reports.stream().filter(r -> "Pending".equals(r.getStatus())).count(),
                (int) reports.stream().filter(r -> "Under Investigation".equals(r.getStatus())).count(),
                (int) reports.stream().filter(r -> "Resolved".equals(r.getStatus())).count(),
                officers.size(),
                users.size()
            );
            dashboardStats.postValue(stats);
        });
    }
    
    public LiveData<List<BlotterReport>> getAllReports() {
        return allReports;
    }
    
    public LiveData<List<User>> getAllUsers() {
        return allUsers;
    }
    
    public LiveData<List<Officer>> getAllOfficers() {
        return allOfficers;
    }
    
    public LiveData<DashboardStats> getDashboardStats() {
        return dashboardStats;
    }
    
    public BlotterReport getReportByIdDirect(int reportId) {
        return null; // Pure online
    }
    
    public void refreshData() {
        loadData();
    }
    
    public static class DashboardStats {
        public final int totalReports;
        public final int pendingReports;
        public final int ongoingReports;
        public final int resolvedReports;
        public final int totalOfficers;
        public final int totalUsers;
        
        public DashboardStats(int totalReports, int pendingReports, int ongoingReports, 
                            int resolvedReports, int totalOfficers, int totalUsers) {
            this.totalReports = totalReports;
            this.pendingReports = pendingReports;
            this.ongoingReports = ongoingReports;
            this.resolvedReports = resolvedReports;
            this.totalOfficers = totalOfficers;
            this.totalUsers = totalUsers;
        }
    }
}
