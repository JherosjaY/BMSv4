package com.example.blottermanagementsystem.data.api;

import com.example.blottermanagementsystem.data.entity.User;

/**
 * RegisterResponse - Response from registration endpoint
 */
public class RegisterResponse {
    public boolean success;
    public String message;
    public RegisterData data;
    
    public static class RegisterData {
        public User user;
        public String token;
    }
}
