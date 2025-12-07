package com.example.blottermanagementsystem.data.dao;

import androidx.room.*;
import com.example.blottermanagementsystem.data.entity.Hearing;
import java.util.List;

@Dao
public interface HearingDao {
    @Query("SELECT * FROM hearings WHERE blotterReportId = :reportId ORDER BY hearingDate DESC")
    List<Hearing> getHearingsByReportId(int reportId);

    @Query("SELECT * FROM hearings WHERE id = :hearingId")
    Hearing getHearingById(int hearingId);

    @Query("SELECT * FROM hearings ORDER BY hearingDate DESC")
    List<Hearing> getAllHearings();

    @Query("SELECT * FROM hearings WHERE status = 'Scheduled' OR status = 'Upcoming' ORDER BY hearingDate ASC")
    List<Hearing> getUpcomingHearings();

    @Query("SELECT * FROM hearings WHERE status = 'Completed' OR status = 'Concluded' ORDER BY hearingDate DESC")
    List<Hearing> getCompletedHearings();

    @Query("SELECT * FROM hearings WHERE status IN ('Scheduled', 'Upcoming') AND hearingDate >= :currentDate ORDER BY hearingDate ASC")
    List<Hearing> getUpcomingHearingsFromDate(String currentDate);

    @Query("SELECT * FROM hearings WHERE status = 'Canceled' OR status = 'Cancelled' ORDER BY hearingDate DESC")
    List<Hearing> getCanceledHearings();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertHearing(Hearing hearing);

    @Update
    void updateHearing(Hearing hearing);

    @Delete
    void deleteHearing(Hearing hearing);
    
    @Query("SELECT COUNT(*) FROM hearings WHERE blotterReportId = :reportId")
    int getHearingCountByReport(int reportId);
    
    @Query("SELECT * FROM hearings WHERE blotterReportId = :reportId ORDER BY hearingDate DESC")
    List<Hearing> getHearingsByReport(int reportId);
    
    @Query("SELECT * FROM hearings WHERE blotterReportId IN (:reportIds) ORDER BY hearingDate DESC")
    List<Hearing> getHearingsByReportIds(List<Integer> reportIds);
    
    @Query("SELECT * FROM hearings WHERE approvalStatus = 'PENDING' ORDER BY createdAt DESC")
    List<Hearing> getPendingHearingRequests();
    
    @Query("SELECT * FROM hearings WHERE approvalStatus IN ('APPROVED', 'DECLINED') ORDER BY approvalDate DESC")
    List<Hearing> getApprovedOrDeclinedHearings();
}
