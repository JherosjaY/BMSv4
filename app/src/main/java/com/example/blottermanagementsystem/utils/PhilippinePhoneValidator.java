package com.example.blottermanagementsystem.utils;

/**
 * ✅ Philippine Phone Number Validator
 * Validates and identifies telecom providers for Philippine phone numbers
 */
public class PhilippinePhoneValidator {
    
    /**
     * Validates if a phone number is a valid Philippine number
     * Accepts formats: 09xxxxxxxxx, +639xxxxxxxxx, 9xxxxxxxxx
     */
    public static boolean isValidPhilippineNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return false;
        }
        
        // Remove spaces and dashes
        phoneNumber = phoneNumber.replaceAll("[\\s\\-]", "");
        
        // Check if it's a valid Philippine number
        // Format: 09xxxxxxxxx (11 digits) or +639xxxxxxxxx (13 digits) or 9xxxxxxxxx (10 digits)
        if (phoneNumber.matches("^09\\d{9}$")) {
            return true; // 09xxxxxxxxx format
        } else if (phoneNumber.matches("^\\+639\\d{9}$")) {
            return true; // +639xxxxxxxxx format
        } else if (phoneNumber.matches("^9\\d{9}$")) {
            return true; // 9xxxxxxxxx format
        }
        
        return false;
    }
    
    /**
     * Gets the telecom provider for a Philippine phone number
     */
    public static String getTelecomProvider(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return "Unknown";
        }
        
        // Normalize the number
        phoneNumber = phoneNumber.replaceAll("[\\s\\-]", "");
        
        // Extract the first 4 digits (after +63 or 0)
        String prefix;
        if (phoneNumber.startsWith("+63")) {
            prefix = phoneNumber.substring(3, Math.min(7, phoneNumber.length()));
        } else if (phoneNumber.startsWith("0")) {
            prefix = phoneNumber.substring(1, Math.min(5, phoneNumber.length()));
        } else {
            prefix = phoneNumber.substring(0, Math.min(4, phoneNumber.length()));
        }
        
        // Globe/TM prefixes
        if (prefix.matches("^9(0[5-9]|1[0-9]|2[0-9]|3[0-9]|4[0-9]|5[0-9]|6[0-9]|7[0-9]|8[0-9]|9[0-9]).*")) {
            return "Globe";
        }
        
        // Smart/TNT prefixes
        if (prefix.matches("^9(0[0-4]|1[0-9]|2[0-9]|3[0-9]).*")) {
            return "Smart/TNT";
        }
        
        // SUN Cellular
        if (prefix.matches("^9(4[0-9]|5[0-9]).*")) {
            return "Sun Cellular";
        }
        
        // DITO Telecommunity
        if (prefix.matches("^9(17[0-9]|18[0-9]).*")) {
            return "DITO";
        }
        
        return "Unknown Provider";
    }
}
