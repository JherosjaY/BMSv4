package com.example.blottermanagementsystem.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class User {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String firstName;
    private String lastName;
    private String username;
    private String password;
    private String role;
    private String email;
    private String phoneNumber;
    private String profilePhotoUri;
    private String gender;
    private long accountCreated;
    private boolean profileCompleted;
    private String badgeNumber;
    private String rank;
    private String dutyStatus;
    private boolean mustChangePassword;
    private boolean isActive;
    private String fcmToken;
    private String deviceId;
    private String resetCode;
    private long resetCodeExpiry;
    private boolean hasSeenTooltips;
    
    // ✅ Track authentication method: "EMAIL_PASSWORD" or "GOOGLE"
    private String authMethod;

    // No-arg constructor required by Room
    public User() {
        this.hasSeenTooltips = false;
        this.authMethod = "EMAIL_PASSWORD"; // Default
    }

    @Ignore
    public User(String firstName, String lastName, String username, String password, String role) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.password = password;
        this.role = role;
        this.accountCreated = System.currentTimeMillis();
        this.profileCompleted = false;
        this.mustChangePassword = false;
        this.isActive = true;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getProfilePhotoUri() {
        return profilePhotoUri;
    }

    public void setProfilePhotoUri(String profilePhotoUri) {
        this.profilePhotoUri = profilePhotoUri;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public long getAccountCreated() {
        return accountCreated;
    }

    public void setAccountCreated(long accountCreated) {
        this.accountCreated = accountCreated;
    }

    public boolean isProfileCompleted() {
        return profileCompleted;
    }

    public void setProfileCompleted(boolean profileCompleted) {
        this.profileCompleted = profileCompleted;
    }

    public String getBadgeNumber() {
        return badgeNumber;
    }

    public void setBadgeNumber(String badgeNumber) {
        this.badgeNumber = badgeNumber;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public String getDutyStatus() {
        return dutyStatus;
    }

    public void setDutyStatus(String dutyStatus) {
        this.dutyStatus = dutyStatus;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public String getResetCode() {
        return resetCode;
    }
    
    public void setResetCode(String resetCode) {
        this.resetCode = resetCode;
    }
    
    public long getResetCodeExpiry() {
        return resetCodeExpiry;
    }
    
    public void setResetCodeExpiry(long resetCodeExpiry) {
        this.resetCodeExpiry = resetCodeExpiry;
    }
    
    // Alias for compatibility
    public void setCreatedAt(long createdAt) {
        this.accountCreated = createdAt;
    }
    
    public boolean hasSeenTooltips() {
        return hasSeenTooltips;
    }
    
    // ✅ Authentication method getters and setters
    public String getAuthMethod() {
        return authMethod;
    }
    
    public void setAuthMethod(String authMethod) {
        this.authMethod = authMethod;
    }
    
    public void setHasSeenTooltips(boolean hasSeenTooltips) {
        this.hasSeenTooltips = hasSeenTooltips;
    }
}
