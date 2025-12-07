package com.example.blottermanagementsystem.data.dao;

import androidx.room.*;
import com.example.blottermanagementsystem.data.entity.BlotterReport;
import java.util.List;

@Dao
public interface BlotterReportDao {
    @Query("SELECT * FROM blotter_reports WHERE isArchived = 0 ORDER BY dateFiled DESC")
    List<BlotterReport> getAllActiveReports();
    
    @Query("SELECT * FROM blotter_reports WHERE isArchived = 1 ORDER BY archivedDate DESC")
    List<BlotterReport> getAllArchivedReports();
    
    @Query("SELECT * FROM blotter_reports ORDER BY dateFiled DESC")
    List<BlotterReport> getAllReports();
    
    @Query("SELECT * FROM blotter_reports WHERE id = :reportId")
    BlotterReport getReportById(int reportId);
    
    @Query("SELECT * FROM blotter_reports WHERE caseNumber = :caseNumber")
    BlotterReport getReportByCaseNumber(String caseNumber);
    
    @Query("SELECT * FROM blotter_reports WHERE assignedOfficer = :officerName AND isArchived = 0")
    List<BlotterReport> getReportsByOfficer(String officerName);
    
    @Query("SELECT * FROM blotter_reports WHERE (assignedOfficerId = :officerId OR assignedOfficerIds LIKE '%' || :officerId || '%') AND isArchived = 0")
    List<BlotterReport> getReportsByOfficerId(int officerId);
    
    @Query("SELECT * FROM blotter_reports WHERE status = :status AND isArchived = 0")
    List<BlotterReport> getReportsByStatus(String status);
    
    @Query("SELECT * FROM blotter_reports WHERE userId = :userId AND isArchived = 0")
    List<BlotterReport> getReportsByUser(int userId);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertReport(BlotterReport report);
    
    @Update
    void updateReport(BlotterReport report);
    
    @Delete
    void deleteReport(BlotterReport report);
    
    @Query("DELETE FROM blotter_reports WHERE userId = :userId")
    void deleteReportsByUserId(int userId);
    
    @Query("SELECT COUNT(*) FROM blotter_reports WHERE isArchived = 0")
    int getActiveReportCount();
    
    @Query("SELECT COUNT(*) FROM blotter_reports WHERE status = :status AND isArchived = 0")
    int getReportCountByStatus(String status);
    
    @Query("SELECT COUNT(*) FROM blotter_reports WHERE isArchived = 1")
    int getArchivedReportCount();
    
    // ✅ New methods for hearing visibility filtering
    @Query("SELECT * FROM blotter_reports WHERE userId = :userId AND isArchived = 0")
    List<BlotterReport> getReportsByUserId(int userId);
    
    @Query("SELECT * FROM blotter_reports WHERE (assignedOfficerId = :officerId OR assignedOfficerIds LIKE '%' || :officerId || '%') AND isArchived = 0")
    List<BlotterReport> getReportsByAssignedOfficer(int officerId);
}
