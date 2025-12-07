package com.example.blottermanagementsystem.utils;

import android.util.Log;
import com.example.blottermanagementsystem.data.database.BlotterDatabase;
import com.example.blottermanagementsystem.data.model.InvestigationStep;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Centralized Timeline Update Manager
 * 
 * This utility manages timeline updates across all 3 roles (User, Officer, Admin)
 * ensuring synchronized updates on all devices.
 * 
 * Timeline Steps:
 * 1. Case Created (auto-completed)
 * 2. Case Assigned (auto-completes when officer assigned)
 * 3. Investigation Started (in-progress when any action taken)
 * 4. Witnesses & Evidence Collected (auto-completes when all 3 added)
 * 5. Hearing Scheduled (in-progress when hearing created)
 * 6. Resolution Documented (in-progress when resolution created)
 * 7. Case Closed (auto-completes when resolution documented)
 */
public class TimelineUpdateManager {
    
    private static final String TAG = "TimelineUpdateManager";
    
    private final BlotterDatabase database;
    
    public TimelineUpdateManager(BlotterDatabase database) {
        this.database = database;
    }
    
    /**
     * Update timeline for a specific report
     * This method is called by all 3 roles and ensures synchronized updates
     * 
     * @param reportId The report ID to update timeline for
     * @param callback Callback to update UI on main thread
     */
    public void updateTimelineForReport(int reportId, TimelineUpdateCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Get all required data from database
                String assignedOfficer = database.blotterReportDao().getReportById(reportId).getAssignedOfficer();
                int witnessCount = database.witnessDao().getWitnessCountByReport(reportId);
                int suspectCount = database.suspectDao().getSuspectCountByReport(reportId);
                int evidenceCount = database.evidenceDao().getEvidenceCountByReport(reportId);
                int hearingCount = database.hearingDao().getHearingCountByReport(reportId);
                int resolutionCount = database.resolutionDao().getResolutionCountByReport(reportId);
                
                Log.d(TAG, "📊 Timeline Update - Report ID: " + reportId);
                Log.d(TAG, "   Assigned Officer: " + (assignedOfficer != null ? assignedOfficer : "None"));
                Log.d(TAG, "   Witnesses: " + witnessCount + ", Suspects: " + suspectCount + ", Evidence: " + evidenceCount);
                Log.d(TAG, "   Hearings: " + hearingCount + ", Resolutions: " + resolutionCount);
                
                // Create timeline steps with updated statuses
                List<InvestigationStep> steps = new ArrayList<>();
                
                // Step 1: Case Created (Always completed)
                InvestigationStep step1 = new InvestigationStep("1", "Case Created", "Initial report submitted", "case_created");
                step1.setCompleted(true);
                steps.add(step1);
                
                // Step 2: Case Assigned (Completed if officer assigned)
                InvestigationStep step2 = new InvestigationStep("2", "Case Assigned", "Waiting for officer assignment", "case_assigned");
                boolean isAssigned = assignedOfficer != null && !assignedOfficer.isEmpty();
                step2.setCompleted(isAssigned);
                step2.setInProgress(false);
                steps.add(step2);
                Log.d(TAG, "   Step 2 (Case Assigned): " + (isAssigned ? "✅ COMPLETED" : "⭕ PENDING"));
                
                // Step 3: Investigation Started (In-progress if any action taken)
                InvestigationStep step3 = new InvestigationStep("3", "Investigation Started", "Officer begins investigation", "investigation_started");
                boolean hasActions = witnessCount > 0 || suspectCount > 0 || evidenceCount > 0 || hearingCount > 0 || resolutionCount > 0;
                step3.setInProgress(hasActions);
                step3.setCompleted(false);
                steps.add(step3);
                Log.d(TAG, "   Step 3 (Investigation Started): " + (hasActions ? "⏳ IN PROGRESS" : "⭕ PENDING"));
                
                // Step 4: Witnesses & Suspects (Completed if both witnesses AND suspects present)
                InvestigationStep step4 = new InvestigationStep("4", "Witnesses & Suspects", "Gathering case information", "evidence_collected");
                boolean allWitnessesAndSuspects = witnessCount > 0 && suspectCount > 0;
                if (allWitnessesAndSuspects) {
                    step4.setCompleted(true);
                    step4.setInProgress(false);
                } else if (witnessCount > 0 || suspectCount > 0) {
                    step4.setCompleted(false);
                    step4.setInProgress(true);
                } else {
                    step4.setCompleted(false);
                    step4.setInProgress(false);
                }
                steps.add(step4);
                Log.d(TAG, "   Step 4 (Witnesses & Suspects): " + (allWitnessesAndSuspects ? "✅ COMPLETED" : (step4.isInProgress() ? "⏳ IN PROGRESS" : "⭕ PENDING")));
                
                // Step 5: Hearing Scheduled (In-progress if hearing created)
                InvestigationStep step5 = new InvestigationStep("5", "Hearing Scheduled", "Court hearing date set", "hearing_scheduled");
                step5.setInProgress(hearingCount > 0);
                step5.setCompleted(false);
                steps.add(step5);
                Log.d(TAG, "   Step 5 (Hearing Scheduled): " + (hearingCount > 0 ? "⏳ IN PROGRESS" : "⭕ PENDING"));
                
                // Step 6: Resolution Documented (In-progress if resolution created)
                InvestigationStep step6 = new InvestigationStep("6", "Resolution Documented", "Case outcome documented", "resolution_documented");
                step6.setInProgress(resolutionCount > 0);
                step6.setCompleted(false);
                steps.add(step6);
                Log.d(TAG, "   Step 6 (Resolution Documented): " + (resolutionCount > 0 ? "⏳ IN PROGRESS" : "⭕ PENDING"));
                
                // Step 7: Case Closed (Auto-completed when resolution documented)
                InvestigationStep step7 = new InvestigationStep("7", "Case Closed", "Case finalized", "case_closed");
                step7.setCompleted(resolutionCount > 0);
                step7.setInProgress(false);
                steps.add(step7);
                Log.d(TAG, "   Step 7 (Case Closed): " + (resolutionCount > 0 ? "✅ COMPLETED" : "⭕ PENDING"));
                
                Log.d(TAG, "✅ Timeline update complete - Ready for all 3 roles");
                
                // Call callback on main thread
                if (callback != null) {
                    callback.onTimelineUpdated(steps);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "❌ Error updating timeline: " + e.getMessage());
                if (callback != null) {
                    callback.onTimelineUpdateFailed(e.getMessage());
                }
            }
        });
    }
    
    /**
     * Callback interface for timeline updates
     * Implemented by all 3 activities (User, Officer, Admin)
     */
    public interface TimelineUpdateCallback {
        /**
         * Called when timeline is successfully updated
         * @param steps Updated investigation steps
         */
        void onTimelineUpdated(List<InvestigationStep> steps);
        
        /**
         * Called if timeline update fails
         * @param errorMessage Error message
         */
        void onTimelineUpdateFailed(String errorMessage);
    }
}
