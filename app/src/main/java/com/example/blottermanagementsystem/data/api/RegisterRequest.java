package com.example.blottermanagementsystem.data.api;

/**
 * RegisterRequest - Request body for user registration
 */
public class RegisterRequest {
    public String username;
    public String email;
    public String password;
    public String confirmPassword;
    public String firstName;
    public String lastName;
    public String profilePhoto;
    
    public RegisterRequest(String username, String email, String password, String confirmPassword) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.confirmPassword = confirmPassword;
        this.firstName = "";
        this.lastName = "";
        this.profilePhoto = null;
    }
    
    public RegisterRequest(String username, String email, String password, String confirmPassword, String firstName, String lastName) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.confirmPassword = confirmPassword;
        this.firstName = firstName;
        this.lastName = lastName;
        this.profilePhoto = null;
    }
}
