package com.example.blottermanagementsystem.utils;

import com.example.blottermanagementsystem.R;

/**
 * ✅ Global utility class for status color and formatting
 * Used across all 3 roles: User, Officer, Admin
 * Ensures consistent status display throughout the app
 */
public class StatusColorUtil {
    
    /**
     * Format status as Title Case (first letter uppercase, rest lowercase)
     * Example: "ASSIGNED" → "Assigned", "resolved" → "Resolved"
     */
    public static String formatStatusToTitleCase(String status) {
        if (status == null || status.isEmpty()) {
            return "Pending";
        }
        return status.substring(0, 1).toUpperCase() + status.substring(1).toLowerCase();
    }
    
    /**
     * Get status color resource ID based on case status
     * Returns appropriate color for:
     * - Resolved/Closed → GREEN
     * - Assigned → ELECTRIC BLUE
     * - Ongoing/In Progress → INFO BLUE
     * - Pending → WARNING YELLOW
     * - Default → ELECTRIC BLUE
     */
    public static int getStatusColor(String status) {
        if (status == null || status.isEmpty()) {
            return R.color.electric_blue;
        }
        
        // Normalize status to lowercase and trim whitespace
        String normalizedStatus = status.trim().toLowerCase();
        
        // Status-based colors
        if (normalizedStatus.contains("resolved") || normalizedStatus.contains("closed")) {
            return R.color.success_green; // Green for resolved
        } else if (normalizedStatus.contains("assigned")) {
            return R.color.electric_blue; // Electric blue for assigned
        } else if (normalizedStatus.contains("ongoing") || normalizedStatus.contains("in progress")) {
            return R.color.info_blue; // Info blue for ongoing
        } else if (normalizedStatus.contains("pending")) {
            return R.color.warning_yellow; // Yellow for pending
        } else {
            return R.color.electric_blue; // Default to blue
        }
    }
    
    /**
     * Check if status is resolved/closed
     */
    public static boolean isResolved(String status) {
        if (status == null || status.isEmpty()) {
            return false;
        }
        String normalized = status.trim().toLowerCase();
        return normalized.contains("resolved") || normalized.contains("closed");
    }
    
    /**
     * Check if status is assigned
     */
    public static boolean isAssigned(String status) {
        if (status == null || status.isEmpty()) {
            return false;
        }
        return status.trim().toLowerCase().contains("assigned");
    }
    
    /**
     * Check if status is ongoing
     */
    public static boolean isOngoing(String status) {
        if (status == null || status.isEmpty()) {
            return false;
        }
        String normalized = status.trim().toLowerCase();
        return normalized.contains("ongoing") || normalized.contains("in progress");
    }
    
    /**
     * Check if status is pending
     */
    public static boolean isPending(String status) {
        if (status == null || status.isEmpty()) {
            return false;
        }
        return status.trim().toLowerCase().contains("pending");
    }
}
