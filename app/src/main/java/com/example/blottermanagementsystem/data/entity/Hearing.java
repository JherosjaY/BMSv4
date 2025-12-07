package com.example.blottermanagementsystem.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "hearings",
    foreignKeys = @ForeignKey(
        entity = BlotterReport.class,
        parentColumns = "id",
        childColumns = "blotterReportId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("blotterReportId")}
)
public class Hearing {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int blotterReportId;
    private String hearingDate;
    private String hearingTime;
    private String location;
    private String purpose;
    private String status;
    private long createdAt;
    
    // ✅ Approval fields for Admin workflow
    private String approvalStatus; // "PENDING", "APPROVED", "DECLINED"
    private int approvedBy; // Admin user ID who approved/declined
    private long approvalDate; // Timestamp of approval/decline
    private String declineReason; // Optional reason if declined
    
    // ✅ Reminder notification fields
    private boolean reminderScheduled; // Whether reminders have been scheduled
    private String remindersSent; // JSON array of reminder timestamps sent
    
    // ✅ Hearing completion tracking fields
    private String attendanceStatus; // "ATTENDED", "NOT_ATTENDED", "PENDING"
    private long completedAt; // Timestamp when hearing was completed/cancelled
    
    // ✅ Presiding officer field
    private String presidingOfficer; // Name of the officer presiding over the hearing
    
    public Hearing() {
        this.status = "Scheduled";
        this.approvalStatus = "PENDING"; // Default to pending approval
        this.attendanceStatus = "PENDING"; // Default to pending attendance
        this.createdAt = System.currentTimeMillis();
    }

    @Ignore
    public Hearing(int blotterReportId, String hearingDate, String hearingTime, String location, String purpose) {
        this.blotterReportId = blotterReportId;
        this.hearingDate = hearingDate;
        this.hearingTime = hearingTime;
        this.location = location;
        this.purpose = purpose;
        this.createdAt = System.currentTimeMillis();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getBlotterReportId() { return blotterReportId; }
    public void setBlotterReportId(int blotterReportId) { this.blotterReportId = blotterReportId; }
    public String getHearingDate() { return hearingDate; }
    public void setHearingDate(String hearingDate) { this.hearingDate = hearingDate; }
    public String getHearingTime() { return hearingTime; }
    public void setHearingTime(String hearingTime) { this.hearingTime = hearingTime; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    
    // ✅ Approval field getters and setters
    public String getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }
    public int getApprovedBy() { return approvedBy; }
    public void setApprovedBy(int approvedBy) { this.approvedBy = approvedBy; }
    public long getApprovalDate() { return approvalDate; }
    public void setApprovalDate(long approvalDate) { this.approvalDate = approvalDate; }
    public String getDeclineReason() { return declineReason; }
    public void setDeclineReason(String declineReason) { this.declineReason = declineReason; }
    
    // ✅ Reminder notification getters and setters
    public boolean isReminderScheduled() { return reminderScheduled; }
    public void setReminderScheduled(boolean reminderScheduled) { this.reminderScheduled = reminderScheduled; }
    public String getRemindersSent() { return remindersSent; }
    public void setRemindersSent(String remindersSent) { this.remindersSent = remindersSent; }
    
    // ✅ Hearing completion tracking getters and setters
    public String getAttendanceStatus() { return attendanceStatus; }
    public void setAttendanceStatus(String attendanceStatus) { this.attendanceStatus = attendanceStatus; }
    public long getCompletedAt() { return completedAt; }
    public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }
    
    // ✅ Presiding officer getters and setters
    public String getPresidingOfficer() { return presidingOfficer; }
    public void setPresidingOfficer(String presidingOfficer) { this.presidingOfficer = presidingOfficer; }
    
    // ✅ Helper method to check if hearing is completed or cancelled
    public boolean isHearingCompleted() {
        return "Completed".equals(status) || "Cancelled".equals(status);
    }
    
    // ✅ Helper method to check if resolution can be documented
    // ✅ TESTING MODE: Enable immediately after hearing is scheduled
    public boolean canEnableResolution() {
        // ✅ TESTING: Enable button immediately if hearing has been scheduled
        // In production, this would check: currentTime >= hearingTime
        return hearingDate != null && hearingTime != null;
    }
    
    // Alias for compatibility
    public String getTitle() { return purpose != null ? purpose : "Hearing"; }
}
