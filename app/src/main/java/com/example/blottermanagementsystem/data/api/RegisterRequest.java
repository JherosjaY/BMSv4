package com.example.blottermanagementsystem.data.api;

/**
 * RegisterRequest - Request body for user registration
 */
public class RegisterRequest {
    public String username;
    public String email;
    public String password;
    public String confirmPassword;
    
    public RegisterRequest(String username, String email, String password, String confirmPassword) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.confirmPassword = confirmPassword;
    }
}
