package com.example.blottermanagementsystem.data.api;

import com.example.blottermanagementsystem.data.entity.BlotterReport;
import com.example.blottermanagementsystem.data.entity.User;
import com.example.blottermanagementsystem.data.entity.Officer;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.*;

public interface BlotterApiService {
    
    // ==================== Authentication ====================
    
    @POST("api/auth/login")
    Call<ApiResponse<LoginData>> login(@Body LoginRequest request);
    
    @POST("api/auth/register")
    Call<ApiResponse<LoginData>> register(@Body RegisterRequest request);
    
    // ==================== Users ====================
    
    @GET("api/users")
    Call<ApiResponse<List<User>>> getAllUsers();
    
    @GET("api/users/{id}")
    Call<ApiResponse<User>> getUserById(@Path("id") int id);
    
    @PUT("api/users/{id}")
    Call<ApiResponse<User>> updateUser(@Path("id") int id, @Body User user);
    
    @DELETE("api/users/{id}")
    Call<ApiResponse<String>> deleteUser(@Path("id") int id);
    
    @POST("api/users/fcm-token")
    Call<ApiResponse<String>> updateFcmToken(@Body FcmTokenRequest request);
    
    // ==================== Officers ====================
    
    @GET("api/officers")
    Call<ApiResponse<List<Officer>>> getAllOfficers();
    
    @POST("api/officers")
    Call<ApiResponse<Officer>> createOfficer(@Body Officer officer);
    
    @PUT("api/officers/{id}")
    Call<ApiResponse<Officer>> updateOfficer(@Path("id") int id, @Body Officer officer);
    
    @DELETE("api/officers/{id}")
    Call<ApiResponse<String>> deleteOfficer(@Path("id") int id);
    
    // ==================== Blotter Reports ====================
    
    @GET("api/reports")
    Call<ApiResponse<List<BlotterReport>>> getAllReports();
    
    @GET("api/reports/{id}")
    Call<ApiResponse<BlotterReport>> getReportById(@Path("id") int id);
    
    @POST("api/reports")
    Call<ApiResponse<BlotterReport>> createReport(@Body BlotterReport report);
    
    @PUT("api/reports/{id}")
    Call<ApiResponse<BlotterReport>> updateReport(@Path("id") int id, @Body BlotterReport report);
    
    @DELETE("api/reports/{id}")
    Call<ApiResponse<String>> deleteReport(@Path("id") int id);
    
    @GET("api/reports/user/{userId}")
    Call<ApiResponse<List<BlotterReport>>> getReportsByUserId(@Path("userId") int userId);
    
    @GET("api/reports/officer/{officerId}")
    Call<ApiResponse<List<BlotterReport>>> getReportsByOfficerId(@Path("officerId") int officerId);
    
    // ==================== Sync ====================
    
    @POST("api/sync/upload")
    Call<ApiResponse<SyncResponse>> syncUpload(@Body SyncRequest request);
    
    @GET("api/sync/download")
    Call<ApiResponse<SyncData>> syncDownload(@Query("lastSync") long lastSync);
    
    // ==================== Request/Response Models ====================
    
    class LoginRequest {
        public String username;
        public String password;
        
        public LoginRequest(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }
    
    class LoginData {
        public User user;
        public String token;
    }
    
    class FcmTokenRequest {
        public int userId;
        public String fcmToken;
        
        public FcmTokenRequest(int userId, String fcmToken) {
            this.userId = userId;
            this.fcmToken = fcmToken;
        }
    }
    
    class SyncRequest {
        public List<BlotterReport> reports;
        public List<User> users;
        public long timestamp;
    }
    
    class SyncResponse {
        public int uploaded;
        public String message;
    }
    
    class SyncData {
        public List<BlotterReport> reports;
        public List<User> users;
        public List<Officer> officers;
        public long timestamp;
    }
}
