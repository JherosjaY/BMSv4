package com.example.blottermanagementsystem.ui.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.database.BlotterDatabase;
import com.example.blottermanagementsystem.data.entity.BlotterReport;
import com.example.blottermanagementsystem.data.model.InvestigationStep;
import com.example.blottermanagementsystem.ui.adapters.ImageAdapter;
import com.example.blottermanagementsystem.ui.adapters.InvestigationStepAdapter;
import com.example.blottermanagementsystem.ui.adapters.InvestigationActionAdapter;
import com.example.blottermanagementsystem.ui.adapters.VideoAdapter;
import com.example.blottermanagementsystem.ui.dialogs.AddSuspectDialogFragment;
import com.example.blottermanagementsystem.ui.dialogs.AddWitnessDialogFragment;
import com.example.blottermanagementsystem.ui.dialogs.DocumentResolutionDialogFragment;
import com.example.blottermanagementsystem.ui.dialogs.OfficerSendSmsDialogFragment;
import com.example.blottermanagementsystem.ui.dialogs.ScheduleHearingDialogFragment;
import com.example.blottermanagementsystem.ui.dialogs.ViewWitnessesDialogFragment;
import com.example.blottermanagementsystem.ui.dialogs.ViewSuspectsDialogFragment;
import com.example.blottermanagementsystem.ui.dialogs.ViewHearingsDialogFragment;
import com.example.blottermanagementsystem.ui.dialogs.ViewResolutionDialogFragment;
import com.example.blottermanagementsystem.utils.MediaManager;
import com.example.blottermanagementsystem.utils.NotificationHelper;
import com.example.blottermanagementsystem.utils.PreferencesManager;
import com.example.blottermanagementsystem.data.api.ApiClient;
import com.example.blottermanagementsystem.utils.NetworkMonitor;
import com.example.blottermanagementsystem.utils.TimelineUpdateManager;
import com.example.blottermanagementsystem.utils.GlobalLoadingManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.imageview.ShapeableImageView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

public class OfficerCaseDetailActivity extends AppCompatActivity {
    
    // UI Components (using existing layout)
    private TextView tvCaseNumber, tvIncidentType, tvIncidentDate, tvIncidentLocation;
    private TextView tvComplainantName, tvComplainantContact, tvComplainantAddress;
    private TextView tvRespondentName, tvRespondentAlias, tvRespondentAddress, tvRespondentContact;
    private TextView tvAccusation, tvRelationship;
    private TextView tvNarrative;
    private androidx.appcompat.widget.Toolbar toolbar;
    private com.google.android.material.chip.Chip chipStatus;
    
    // Officer Action Buttons (using existing layout)
    private MaterialButton btnUpdateStatus, btnEdit, btnDelete, btnResolveCase;
    private MaterialButton btnStartInvestigation;  // ← Dedicated Start Investigation button
    private MaterialButton btnViewPersonHistory;  // ← View person history button
    private MaterialButton btnAddWitness, btnAddSuspect, btnCreateHearing, btnDocumentResolution, btnKPForms;
    // ❌ REMOVED: btnAddEvidence - Officer focuses on user-provided evidence only
    
    // ScrollView for content
    private androidx.core.widget.NestedScrollView nestedScrollView;
    
    // Media Components
    private RecyclerView recyclerImages, recyclerVideos;
    private ImageAdapter imageAdapter;
    private VideoAdapter videoAdapter;
    private TextView tvImagesLabel, tvVideosLabel;
    
    // Data
    private BlotterDatabase database;
    private PreferencesManager preferencesManager;
    private NotificationHelper notificationHelper;
    private BlotterReport currentReport;
    private List<Uri> imageList = new ArrayList<>();
    private List<Uri> videoList = new ArrayList<>();
    private int reportId;
    
    // Investigation Timeline - Two Containers
    private RecyclerView rvCaseProgress;  // Container 1: View-only progress
    private RecyclerView rvInvestigationActions;  // Container 2: Interactive actions
    private InvestigationStepAdapter caseProgressAdapter;
    private InvestigationActionAdapter investigationActionsAdapter;
    private List<InvestigationStep> caseProgressSteps = new ArrayList<>();
    private List<InvestigationStep> investigationActionSteps = new ArrayList<>();
    private boolean isTimelineInitializing = false;  // ✅ Prevent concurrent initialization
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_officer_case_investigation);
        preferencesManager = new PreferencesManager(this);
        notificationHelper = new NotificationHelper(this);
        
        // Set status bar color to match dark theme
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.primary_dark_blue));
        }
        
        // Get report ID from intent
        reportId = getIntent().getIntExtra("reportId", -1);
        if (reportId == -1) {
            Toast.makeText(this, "Invalid case ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        initViews();
        setupRecyclerViews();
        setupListeners();
        loadCaseDetails();
    }
    
    private void initViews() {
        // Toolbar setup
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Case Investigation");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        
        // Case Information (using existing layout)
        tvCaseNumber = findViewById(R.id.tvCaseNumber);
        chipStatus = findViewById(R.id.chipStatus);
        tvIncidentType = findViewById(R.id.tvIncidentType);
        tvIncidentDate = findViewById(R.id.tvIncidentDate);
        tvIncidentLocation = findViewById(R.id.tvIncidentLocation);
        
        // Complainant Information
        tvComplainantName = findViewById(R.id.tvComplainantName);
        tvComplainantContact = findViewById(R.id.tvContactNumber);
        tvComplainantAddress = findViewById(R.id.tvAddress);
        
        // Respondent Information
        tvRespondentName = findViewById(R.id.tvRespondentName);
        tvRespondentAlias = findViewById(R.id.tvRespondentAlias);
        tvRespondentAddress = findViewById(R.id.tvRespondentAddress);
        tvRespondentContact = findViewById(R.id.tvRespondentContact);
        tvAccusation = findViewById(R.id.tvAccusation);
        tvRelationship = findViewById(R.id.tvRelationship);
        
        // Narrative
        tvNarrative = findViewById(R.id.tvDescription);
        
        // Officer Action Buttons (using existing layout)
        btnUpdateStatus = findViewById(R.id.btnUpdateStatus);
        btnStartInvestigation = findViewById(R.id.btnStartInvestigation);  // ← Initialize dedicated button
        btnViewPersonHistory = findViewById(R.id.btnViewPersonHistory);  // ← Initialize person history button
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
        // Resolve button may not exist in layout - will be created dynamically if needed
        btnResolveCase = null;
        
        // Investigation Feature Buttons
        btnAddWitness = findViewById(R.id.btnAddWitness);
        btnAddSuspect = findViewById(R.id.btnAddSuspect);
        // ❌ REMOVED: btnAddEvidence - Officer focuses on user-provided evidence only
        btnCreateHearing = findViewById(R.id.btnCreateHearing);
        btnDocumentResolution = findViewById(R.id.btnDocumentResolution);
        btnKPForms = findViewById(R.id.btnKPForms);
        
        // ScrollView
        nestedScrollView = findViewById(R.id.nestedScrollView);
        
        // Media Components
        recyclerImages = findViewById(R.id.recyclerImages);
        recyclerVideos = findViewById(R.id.recyclerVideos);
        tvImagesLabel = findViewById(R.id.tvImagesLabel);
        tvVideosLabel = findViewById(R.id.tvVideosLabel);
        
        // Set dynamic bottom padding
        setDynamicBottomPadding();
    }
    
    private void setDynamicBottomPadding() {
        nestedScrollView.post(() -> {
            // Get screen height and density
            int screenHeight = getResources().getDisplayMetrics().heightPixels;
            float density = getResources().getDisplayMetrics().density;
            
            // Toolbar height is approximately 56dp
            int toolbarHeightPx = (int) (56 * density);
            
            // Available height for content
            int availableHeight = screenHeight - toolbarHeightPx;
            
            // Get the content LinearLayout (first child of NestedScrollView)
            View child = nestedScrollView.getChildAt(0);
            if (child instanceof LinearLayout) {
                LinearLayout contentLayout = (LinearLayout) child;
                
                // Measure content height
                contentLayout.measure(
                    android.view.View.MeasureSpec.makeMeasureSpec(nestedScrollView.getWidth(), android.view.View.MeasureSpec.AT_MOST),
                    android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
                );
                int contentHeight = contentLayout.getMeasuredHeight();
                
                // Calculate bottom padding
                int bottomPadding = 8; // Minimum 8dp
                
                // If content fits on screen, add extra padding for balance
                if (contentHeight < availableHeight) {
                    int extraSpace = availableHeight - contentHeight;
                    bottomPadding = extraSpace / 4; // Use 1/4 of extra space
                }
                
                // Apply padding
                int currentStart = contentLayout.getPaddingStart();
                int currentEnd = contentLayout.getPaddingEnd();
                int currentTop = contentLayout.getPaddingTop();
                contentLayout.setPadding(currentStart, currentTop, currentEnd, bottomPadding);
                
                Log.d("OfficerCaseDetail", "✅ Dynamic bottom padding: " + bottomPadding + "px (~" + (int)(bottomPadding/density) + "dp)");
            }
        });
    }
    
    private void setupRecyclerViews() {
        // Container 1: Case Progress (VIEW-ONLY)
        rvCaseProgress = findViewById(R.id.rvCaseProgress);
        caseProgressAdapter = new InvestigationStepAdapter(caseProgressSteps, new InvestigationStepAdapter.OnStepActionListener() {
            @Override
            public void onStepAction(InvestigationStep step) {
                android.util.Log.d("OfficerCaseDetail", "Case progress step clicked: " + step.getTitle());
            }
            @Override
            public void onViewWitnesses(int reportId) {}
            @Override
            public void onViewSuspects(int reportId) {}
            @Override
            public void onViewEvidence(int reportId) {}
            @Override
            public void onViewHearings(int reportId) {}
            @Override
            public void onViewResolution(int reportId) {}
        });
        rvCaseProgress.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        rvCaseProgress.setAdapter(caseProgressAdapter);
        
        // Container 2: Investigation Actions (INTERACTIVE)
        // Check if investigation has started based on case status
        boolean isInvestigationStarted = currentReport != null && 
            (currentReport.getStatus() != null && 
             (currentReport.getStatus().equalsIgnoreCase("ONGOING") || 
              currentReport.getStatus().equalsIgnoreCase("IN PROGRESS") ||
              currentReport.getStatus().equalsIgnoreCase("RESOLVED")));
        
        rvInvestigationActions = findViewById(R.id.rvInvestigationActions);
        investigationActionsAdapter = new InvestigationActionAdapter(investigationActionSteps, new InvestigationActionAdapter.OnActionListener() {
            @Override
            public void onStepAction(InvestigationStep step) {
                android.util.Log.d("OfficerCaseDetail", "Investigation action clicked: " + step.getTitle());
                String stepId = step.getId();
                if ("A1".equals(stepId)) {
                    openAddWitness();
                } else if ("A2".equals(stepId)) {
                    openAddSuspect();
                } else if ("A3".equals(stepId)) {
                    openCreateHearing(); // ✅ Schedule Hearing button
                } else if ("A4".equals(stepId)) {
                    openSendSms(); // ✅ Send SMS button
                } else if ("A5".equals(stepId)) {
                    openDocumentResolution(); // ✅ Document Resolution button
                }
            }
            @Override
            public void onViewWitnesses(int reportId) {}
            @Override
            public void onViewSuspects(int reportId) {}
            @Override
            public void onViewEvidence(int reportId) {}
            @Override
            public void onViewHearings(int reportId) {}
            @Override
            public void onViewResolution(int reportId) {}
            @Override
            public void onViewStepDetails(String stepTag, int reportId) {
                // ✅ Handle "View Details" click for completed steps
                if ("record_witness".equals(stepTag)) {
                    openViewWitnesses();
                } else if ("identify_suspect".equals(stepTag)) {
                    openViewSuspects();
                } else if ("schedule_hearing".equals(stepTag)) {
                    openViewHearings();
                } else if ("document_resolution".equals(stepTag)) {
                    openViewResolution();
                }
            }
        }, isInvestigationStarted);
        rvInvestigationActions.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        rvInvestigationActions.setAdapter(investigationActionsAdapter);
        
        // ✅ Set reportId and user role for adapter
        investigationActionsAdapter.setReportId(reportId);
        investigationActionsAdapter.setUserRole("OFFICER");
        
        // ⚠️ DO NOT initialize timeline here - database is null!
        // Timeline will be initialized in populateViews() after report is loaded
        
        // Images RecyclerView (VIEW-ONLY)
        imageAdapter = new ImageAdapter(imageList, new ImageAdapter.OnImageClickListener() {
            @Override
            public void onImageClick(Uri uri) {
                viewImage(uri);
            }
            
            @Override
            public void onImageDelete(int position) {
                // NO DELETE for officers - view only
                Toast.makeText(OfficerCaseDetailActivity.this, "View only - cannot delete evidence", Toast.LENGTH_SHORT).show();
            }
        });
        recyclerImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerImages.setAdapter(imageAdapter);
        
        // Videos RecyclerView (VIEW-ONLY)
        videoAdapter = new VideoAdapter(videoList, new VideoAdapter.OnVideoClickListener() {
            @Override
            public void onVideoClick(Uri videoUri) {
                playVideo(videoUri);
            }
            
            @Override
            public void onVideoDelete(int position) {
                // NO DELETE for officers - view only
                Toast.makeText(OfficerCaseDetailActivity.this, "View only - cannot delete evidence", Toast.LENGTH_SHORT).show();
            }
        });
        recyclerVideos.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerVideos.setAdapter(videoAdapter);
    }
    
    /**
     * Initialize the investigation timeline into two containers:
     * Container 1: Case Progress (View-Only) - Steps 1-5
     * Container 2: Investigation Actions (Interactive) - Steps 6-8
     * ⚠️ MUST run on background thread to avoid blocking UI
     */
    private void initializeInvestigationTimeline() {
        // ✅ Prevent concurrent initialization (avoid duplicates)
        if (isTimelineInitializing) {
            android.util.Log.d("OfficerCaseDetail", "⚠️ Timeline initialization already in progress, skipping...");
            return;
        }
        
        isTimelineInitializing = true;
        
        // ✅ Clear lists BEFORE background thread to prevent duplicates
        caseProgressSteps.clear();
        investigationActionSteps.clear();
        
        // Run on background thread to avoid "Cannot access database on the main thread" error
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            
            // ===== CONTAINER 1: CASE PROGRESS (VIEW-ONLY) =====
            
            // Step 1: Case Created (Always completed)
            InvestigationStep step1 = new InvestigationStep("1", "Case Created", "Initial report submitted", "case_created");
            step1.setCompleted(true);
            caseProgressSteps.add(step1);
            
            // Step 2: Case Assigned
            // ✅ Check if case is assigned to this officer
            InvestigationStep step2 = new InvestigationStep("2", "Case Assigned", "Waiting for officer assignment", "case_assigned");
            boolean isCaseAssigned = currentReport != null && 
                (currentReport.getAssignedOfficerId() != null || 
                 (currentReport.getAssignedOfficerIds() != null && !currentReport.getAssignedOfficerIds().isEmpty()));
            
            if (isCaseAssigned) {
                step2.setCompleted(true);
                step2.setInProgress(false);
                android.util.Log.d("OfficerCaseDetail", "✅ Case Assigned: COMPLETED");
            } else {
                step2.setCompleted(false);
                step2.setInProgress(true);
                android.util.Log.d("OfficerCaseDetail", "⏳ Case Assigned: IN PROGRESS (waiting for assignment)");
            }
            caseProgressSteps.add(step2);
            
            // Step 3: Investigation Started
            // ✅ Check if investigation has started (case status is ONGOING, IN PROGRESS, or RESOLVED)
            InvestigationStep step3 = new InvestigationStep("3", "Investigation Started", "Officer begins investigation", "investigation_started");
            boolean isInvestigationStarted = currentReport != null && 
                currentReport.getStatus() != null && 
                (currentReport.getStatus().equalsIgnoreCase("ONGOING") || 
                 currentReport.getStatus().equalsIgnoreCase("IN PROGRESS") ||
                 currentReport.getStatus().equalsIgnoreCase("RESOLVED"));
            
            if (isInvestigationStarted) {
                // Investigation has started - show as COMPLETED (checkmark)
                step3.setCompleted(true);
                step3.setInProgress(false);
                android.util.Log.d("OfficerCaseDetail", "✅ Investigation Started: COMPLETED");
            } else {
                // Investigation not started yet - show as IN PROGRESS (hourglass - current active)
                step3.setCompleted(false);
                step3.setInProgress(true);
                android.util.Log.d("OfficerCaseDetail", "⏳ Investigation Started: IN PROGRESS (waiting to start)");
            }
            caseProgressSteps.add(step3);
            
            // Step 4: Witnesses & Suspects
            // ✅ Check if witness AND suspect both exist
            InvestigationStep step4 = new InvestigationStep("4", "Witnesses & Suspects", "Gathering case information", "evidence_collected");
            int witnessCount = database.witnessDao().getWitnessCountByReport(reportId);
            int suspectCount = database.suspectDao().getSuspectCountByReport(reportId);
            int evidenceCount = database.evidenceDao().getEvidenceCountByReport(reportId);
        
        if (witnessCount > 0 && suspectCount > 0) {
            // Both witness and suspect collected - COMPLETED
            step4.setCompleted(true);
            step4.setInProgress(false);
            android.util.Log.d("OfficerCaseDetail", "✅ Step 4: COMPLETED (witness and suspect present)");
        } else if (isInvestigationStarted) {
            // ✅ Investigation started - show as IN PROGRESS (hourglass) even if no data yet
            step4.setCompleted(false);
            step4.setInProgress(true);
            android.util.Log.d("OfficerCaseDetail", "⏳ Step 4: IN PROGRESS (investigation started, collecting W:" + witnessCount + " S:" + suspectCount + ")");
        } else {
            // Investigation not started - PENDING (empty circle)
            step4.setCompleted(false);
            step4.setInProgress(false);
            android.util.Log.d("OfficerCaseDetail", "⭕ Step 4: PENDING (waiting for investigation to start)");
        }
        caseProgressSteps.add(step4);
        
        // Step 5: Hearing Scheduled
        // ✅ Check if hearing exists
        InvestigationStep step5 = new InvestigationStep("5", "Hearing Scheduled", "Court hearing date set", "hearing_scheduled");
        int hearingCount = database.hearingDao().getHearingCountByReport(reportId);
        
        if (hearingCount > 0) {
            // Hearing scheduled - COMPLETED (checkmark)
            step5.setCompleted(true);
            step5.setInProgress(false);
            android.util.Log.d("OfficerCaseDetail", "✅ Step 5: COMPLETED (hearing exists)");
        } else if (witnessCount > 0 && suspectCount > 0 && evidenceCount > 0) {
            // All evidence collected - show as IN PROGRESS (hourglass - current active)
            step5.setCompleted(false);
            step5.setInProgress(true);
            android.util.Log.d("OfficerCaseDetail", "⏳ Step 5: IN PROGRESS (waiting to schedule hearing)");
        } else {
            // Evidence not all collected - PENDING
            step5.setCompleted(false);
            step5.setInProgress(false);
            android.util.Log.d("OfficerCaseDetail", "⭕ Step 5: PENDING");
        }
        caseProgressSteps.add(step5);
        
        // Step 6: Resolution Documented
        // ✅ Check if resolution exists
        InvestigationStep step6 = new InvestigationStep("6", "Resolution Documented", "Case outcome documented", "resolution_documented");
        int resolutionCount = database.resolutionDao().getResolutionCountByReport(reportId);
        
        if (resolutionCount > 0) {
            // Resolution documented - COMPLETED (checkmark)
            step6.setCompleted(true);
            step6.setInProgress(false);
            android.util.Log.d("OfficerCaseDetail", "✅ Step 6: COMPLETED (resolution exists)");
        } else if (hearingCount > 0) {
            // Hearing scheduled - show as IN PROGRESS (hourglass - current active)
            step6.setCompleted(false);
            step6.setInProgress(true);
            android.util.Log.d("OfficerCaseDetail", "⏳ Step 6: IN PROGRESS (waiting to document resolution)");
        } else {
            // Hearing not scheduled - PENDING
            step6.setCompleted(false);
            step6.setInProgress(false);
            android.util.Log.d("OfficerCaseDetail", "⭕ Step 6: PENDING");
        }
        caseProgressSteps.add(step6);
        
        // Step 7: Case Closed
        // ✅ Check resolution type: Settled = Completed ✅, Withdrawn = Hourglass ⏳
        InvestigationStep step7 = new InvestigationStep("7", "Case Closed", "Case finalized", "case_closed");
        if (resolutionCount > 0) {
            // Check resolution type
            try {
                java.util.List<com.example.blottermanagementsystem.data.entity.Resolution> resolutions = 
                    database.resolutionDao().getResolutionsByReport(reportId);
                
                if (resolutions != null && !resolutions.isEmpty()) {
                    com.example.blottermanagementsystem.data.entity.Resolution resolution = resolutions.get(0);
                    
                    if ("Settled".equals(resolution.getResolutionType())) {
                        // ✅ Settled = Case is COMPLETED (checkmark)
                        step7.setCompleted(true);
                        step7.setInProgress(false);
                        android.util.Log.d("OfficerCaseDetail", "✅ Step 7: COMPLETED (Settled)");
                    } else if ("Withdrawn".equals(resolution.getResolutionType())) {
                        // ⏳ Withdrawn = Show hourglass (respondent didn't appear)
                        step7.setCompleted(false);
                        step7.setInProgress(true);
                        android.util.Log.d("OfficerCaseDetail", "⏳ Step 7: IN PROGRESS (Withdrawn - respondent didn't appear)");
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("OfficerCaseDetail", "Error checking resolution type: " + e.getMessage());
                step7.setCompleted(true);
                step7.setInProgress(false);
            }
        } else if (hearingCount > 0) {
            // Hearing scheduled - show as IN PROGRESS (hourglass - current active)
            step7.setCompleted(false);
            step7.setInProgress(true);
            android.util.Log.d("OfficerCaseDetail", "⏳ Step 7: IN PROGRESS (waiting for resolution)");
        } else {
            // Hearing not scheduled - PENDING
            step7.setCompleted(false);
            step7.setInProgress(false);
            android.util.Log.d("OfficerCaseDetail", "⭕ Step 7: PENDING");
        }
        caseProgressSteps.add(step7);
        
        // ===== CONTAINER 2: INVESTIGATION ACTIONS (INTERACTIVE) =====
        
        // Action 1: Record Witness (ALWAYS ENABLED)
        InvestigationStep actionWitness = new InvestigationStep("A1", "Record Witness", "Document statements from witnesses", "record_witness");
        actionWitness.setCompleted(false);
        actionWitness.setInProgress(false);
        actionWitness.setActionText("Add Witness");
        actionWitness.setActionIcon(R.drawable.ic_witness);
        actionWitness.setEnabled(true); // ✅ Always enabled
        investigationActionSteps.add(actionWitness);
        
        // Action 2: Identify Suspect (DISABLED - unlock after witness added)
        InvestigationStep actionSuspect = new InvestigationStep("A2", "Identify", "Document information about suspects", "identify_suspect");
        actionSuspect.setCompleted(false);
        actionSuspect.setInProgress(false);
        actionSuspect.setActionText("Add Suspect");
        actionSuspect.setActionIcon(R.drawable.ic_suspect);
        actionSuspect.setEnabled(witnessCount > 0); // ✅ Enabled if witness exists
        investigationActionSteps.add(actionSuspect);
        
        // ❌ REMOVED: Action 3: Gather Evidence - Officer focuses on user-provided evidence only
        // Evidence is now view-only for officers (uploaded by users)
        
        // Action 3: Schedule Hearing (WITH ACTION BUTTON)
        InvestigationStep actionHearing = new InvestigationStep("A3", "Schedule Hearings", "Conduct hearings with involved parties", "schedule_hearing");
        actionHearing.setCompleted(false);
        actionHearing.setInProgress(false);
        actionHearing.setActionText("Schedule Hearing"); // ✅ ACTION BUTTON - allow scheduling
        actionHearing.setActionIcon(R.drawable.ic_hearing);
        actionHearing.setEnabled(suspectCount > 0);
        investigationActionSteps.add(actionHearing);
        
        // Action 4: Send SMS Notification (MOVED HERE - after Schedule Hearings)
        InvestigationStep actionSendSms = new InvestigationStep("A4", "Send SMS Notification", "Notify respondent/witness via SMS", "send_sms");
        actionSendSms.setCompleted(false);
        actionSendSms.setInProgress(false);
        actionSendSms.setActionText("Send SMS");
        actionSendSms.setActionIcon(R.drawable.ic_sms);
        actionSendSms.setEnabled(hearingCount > 0); // ✅ Enabled only if hearing is scheduled
        investigationActionSteps.add(actionSendSms);
        
        // Action 5: Document Resolution (WITH ACTION BUTTON)
        InvestigationStep actionResolution = new InvestigationStep("A5", "Document Resolution", "Record the case outcome", "document_resolution");
        actionResolution.setCompleted(false);
        actionResolution.setInProgress(false);
        actionResolution.setActionText("Document Resolution"); // ✅ ACTION BUTTON - allow documenting resolution
        actionResolution.setActionIcon(R.drawable.ic_resolution);
        
        // ✅ ENABLE ONLY IF: Hearing is completed/cancelled OR 30 mins have passed since scheduled time
        boolean canEnableResolution = false;
        if (hearingCount > 0) {
            try {
                List<com.example.blottermanagementsystem.data.entity.Hearing> hearings = database.hearingDao().getHearingsByReportId(reportId);
                if (hearings != null && !hearings.isEmpty()) {
                    com.example.blottermanagementsystem.data.entity.Hearing hearing = hearings.get(0);
                    // Check if hearing is completed/cancelled OR 30 mins have passed
                    canEnableResolution = hearing.isHearingCompleted() || hearing.canEnableResolution();
                    android.util.Log.d("OfficerCaseDetail", "Hearing status: " + hearing.getStatus() + 
                        ", Attendance: " + hearing.getAttendanceStatus() + 
                        ", Can enable: " + canEnableResolution);
                }
            } catch (Exception e) {
                android.util.Log.e("OfficerCaseDetail", "Error checking hearing status: " + e.getMessage());
            }
        }
        actionResolution.setEnabled(canEnableResolution);
        investigationActionSteps.add(actionResolution);
        
        // ✅ MARK STEPS AS COMPLETED IF DATA EXISTS
        if (witnessCount > 0) {
            investigationActionSteps.get(0).setCompleted(true);  // Record Witness
            android.util.Log.d("OfficerCaseDetail", "✅ Marked 'Record Witness' as completed");
        }
        if (suspectCount > 0) {
            investigationActionSteps.get(1).setCompleted(true);  // Identify Suspect
            android.util.Log.d("OfficerCaseDetail", "✅ Marked 'Identify Suspect' as completed");
        }
        if (hearingCount > 0) {
            investigationActionSteps.get(2).setCompleted(true);  // Schedule Hearings
            android.util.Log.d("OfficerCaseDetail", "✅ Marked 'Schedule Hearings' as completed");
        }
        if (resolutionCount > 0) {
            investigationActionSteps.get(4).setCompleted(true);  // Document Resolution (now at index 4)
            android.util.Log.d("OfficerCaseDetail", "✅ Marked 'Document Resolution' as completed");
        }
            
            // ✅ Update UI on main thread
            runOnUiThread(() -> {
                // Notify adapters of changes
                if (caseProgressAdapter != null) {
                    caseProgressAdapter.updateSteps(caseProgressSteps);
                    caseProgressAdapter.notifyDataSetChanged();
                }
                if (investigationActionsAdapter != null) {
                    investigationActionsAdapter.updateSteps(investigationActionSteps);
                    investigationActionsAdapter.notifyDataSetChanged();
                }
                
                android.util.Log.d("OfficerCaseDetail", "✅ Investigation timeline initialized: " + caseProgressSteps.size() + " progress steps + " + investigationActionSteps.size() + " action steps");
                android.util.Log.d("OfficerCaseDetail", "✅ Adapters notified - UI should update now");
                
                // ✅ Reset flag to allow next initialization
                isTimelineInitializing = false;
            });
        });
    }
    
    private void setupListeners() {
        // Officer Action Buttons
        if (btnUpdateStatus != null) {
            btnUpdateStatus.setOnClickListener(v -> showUpdateStatusDialog());
        }
        if (btnViewPersonHistory != null) {
            btnViewPersonHistory.setOnClickListener(v -> openViewPersonHistory());
        }
        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> showEditRestriction());
        }
        if (btnResolveCase != null) {
            btnResolveCase.setOnClickListener(v -> showResolveCaseDialog());
        }
        
        // Investigation Feature Buttons
        if (btnAddWitness != null) {
            btnAddWitness.setOnClickListener(v -> openAddWitness());
        }
        if (btnAddSuspect != null) {
            btnAddSuspect.setOnClickListener(v -> openAddSuspect());
        }
        // ❌ REMOVED: btnAddEvidence listener - Officer focuses on user-provided evidence only
        if (btnCreateHearing != null) {
            btnCreateHearing.setOnClickListener(v -> openCreateHearing());
        }
        if (btnDocumentResolution != null) {
            btnDocumentResolution.setOnClickListener(v -> openDocumentResolution());
        }
    }
    
    private void loadCaseDetails() {
        GlobalLoadingManager.show(this, "Loading case details...");
        
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                database = BlotterDatabase.getDatabase(this);
                if (database == null) {
                    runOnUiThread(() -> {
                        GlobalLoadingManager.hide();
                        Toast.makeText(this, "Database error", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return;
                }
                
                currentReport = database.blotterReportDao().getReportById(reportId);
                android.util.Log.d("OfficerCaseDetail", "Loaded report ID: " + reportId + ", Report: " + (currentReport != null ? currentReport.getCaseNumber() : "NULL"));
                
                runOnUiThread(() -> {
                    GlobalLoadingManager.hide();
                    if (currentReport != null) {
                        populateViews();
                        loadMediaFiles();
                    } else {
                        Toast.makeText(this, "Case not found (ID: " + reportId + ")", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("OfficerCaseDetail", "Error loading case", e);
                runOnUiThread(() -> {
                    GlobalLoadingManager.hide();
                    Toast.makeText(this, "Error loading case: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }
    
    private void populateViews() {
        // ✅ UPDATE Investigation Actions buttons based on actual case status
        if (investigationActionsAdapter != null) {
            boolean isInvestigationStarted = currentReport != null && 
                currentReport.getStatus() != null && 
                (currentReport.getStatus().equalsIgnoreCase("ONGOING") || 
                 currentReport.getStatus().equalsIgnoreCase("IN PROGRESS") ||
                 currentReport.getStatus().equalsIgnoreCase("RESOLVED"));
            investigationActionsAdapter.setInvestigationStarted(isInvestigationStarted);
            android.util.Log.d("OfficerCaseDetail", "✅ Updated adapter - Investigation started: " + isInvestigationStarted);
        }
        
        // ✅ NOW initialize timeline - database is available and report is loaded
        initializeInvestigationTimeline();
        
        // Case Information
        tvCaseNumber.setText(currentReport.getCaseNumber());
        chipStatus.setText(currentReport.getStatus());
        setStatusChipColor(currentReport.getStatus());
        tvIncidentType.setText(currentReport.getIncidentType());
        tvIncidentDate.setText(new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
            .format(new java.util.Date(currentReport.getIncidentDate())));
        tvIncidentLocation.setText(currentReport.getIncidentLocation());
        
        // Complainant Information
        tvComplainantName.setText(currentReport.getComplainantName());
        tvComplainantContact.setText(currentReport.getComplainantContact());
        tvComplainantAddress.setText(currentReport.getComplainantAddress());
        
        // Respondent Information - ALWAYS SHOW with N/A for empty fields
        String respondentName = currentReport.getRespondentName();
        String respondentAlias = currentReport.getRespondentAlias();
        String respondentAddress = currentReport.getRespondentAddress();
        String respondentContact = currentReport.getRespondentContact();
        String accusation = currentReport.getAccusation();
        String relationship = currentReport.getRelationshipToComplainant();
        
        // Set text with N/A for empty values
        tvRespondentName.setText(respondentName != null && !respondentName.isEmpty() ? respondentName : "N/A");
        tvRespondentAlias.setText(respondentAlias != null && !respondentAlias.isEmpty() ? respondentAlias : "N/A");
        tvRespondentAddress.setText(respondentAddress != null && !respondentAddress.isEmpty() ? respondentAddress : "N/A");
        tvRespondentContact.setText(respondentContact != null && !respondentContact.isEmpty() ? respondentContact : "N/A");
        if (tvAccusation != null) tvAccusation.setText(accusation != null && !accusation.isEmpty() ? accusation : "N/A");
        if (tvRelationship != null) tvRelationship.setText(relationship != null && !relationship.isEmpty() ? relationship : "N/A");
        
        // ALWAYS show Respondent section (even if empty)
        TextView tvRespondentTitle = findViewById(R.id.tvRespondentTitle);
        androidx.cardview.widget.CardView cardRespondent = findViewById(R.id.cardRespondent);
        if (tvRespondentTitle != null) tvRespondentTitle.setVisibility(View.VISIBLE);
        if (cardRespondent != null) cardRespondent.setVisibility(View.VISIBLE);
        
        // ALWAYS show respondent details
        LinearLayout layoutRespondentName = findViewById(R.id.layoutRespondentName);
        LinearLayout layoutRespondentAlias = findViewById(R.id.layoutRespondentAlias);
        LinearLayout layoutRespondentAddress = findViewById(R.id.layoutRespondentAddress);
        LinearLayout layoutRespondentContact = findViewById(R.id.layoutRespondentContact);
        LinearLayout layoutAccusation = findViewById(R.id.layoutAccusation);
        LinearLayout layoutRelationship = findViewById(R.id.layoutRelationship);
        
        if (layoutRespondentName != null) layoutRespondentName.setVisibility(View.VISIBLE);
        if (layoutRespondentAlias != null) layoutRespondentAlias.setVisibility(View.VISIBLE);
        if (layoutRespondentAddress != null) layoutRespondentAddress.setVisibility(View.VISIBLE);
        if (layoutRespondentContact != null) layoutRespondentContact.setVisibility(View.VISIBLE);
        if (layoutAccusation != null) layoutAccusation.setVisibility(View.VISIBLE);
        if (layoutRelationship != null) layoutRelationship.setVisibility(View.VISIBLE);
        
        // View Person History button - ALWAYS VISIBLE and CLICKABLE
        // When clicked with N/A, it will show toast message
        if (btnViewPersonHistory != null) {
            btnViewPersonHistory.setEnabled(true);
            btnViewPersonHistory.setAlpha(1.0f);
        }
        
        Log.d("OfficerCaseDetail", "✅ Respondent Information ALWAYS SHOWN: " + tvRespondentName.getText());
        
        // Narrative
        tvNarrative.setText(currentReport.getNarrative());
        
        // Update button visibility based on status
        updateButtonVisibility();
    }
    
    private void updateButtonVisibility() {
        if (currentReport == null) return;
        
        String status = currentReport.getStatus() != null ? currentReport.getStatus().toUpperCase() : "PENDING";
        Log.d("OfficerCaseDetail", "updateButtonVisibility - Status: " + status + ", btnUpdateStatus: " + (btnUpdateStatus != null ? "NOT NULL" : "NULL"));
        
        // Hide EDIT and DELETE buttons (user role buttons)
        if (btnEdit != null) btnEdit.setVisibility(View.GONE);
        if (btnDelete != null) btnDelete.setVisibility(View.GONE);
        
        // ALWAYS show View Person History button (informational, not an action)
        if (btnViewPersonHistory != null) {
            btnViewPersonHistory.setVisibility(View.VISIBLE);
            Log.d("OfficerCaseDetail", "✅ View Person History button ALWAYS SHOWN");
        }
        
        // Show "Start Investigation" button when status is "ASSIGNED"
        if ("ASSIGNED".equals(status)) {
            if (btnStartInvestigation != null) {
                btnStartInvestigation.setVisibility(View.VISIBLE);
                btnStartInvestigation.setOnClickListener(v -> showUpdateStatusDialog());
                Log.d("OfficerCaseDetail", "✅ Start Investigation button SHOWN");
            } else {
                Log.e("OfficerCaseDetail", "❌ btnStartInvestigation is NULL!");
            }
            
            // Hide Update Status button
            if (btnUpdateStatus != null) btnUpdateStatus.setVisibility(View.GONE);
            
            // Hide all investigation feature buttons
            if (btnAddWitness != null) btnAddWitness.setVisibility(View.GONE);
            if (btnAddSuspect != null) btnAddSuspect.setVisibility(View.GONE);
            // ❌ REMOVED: btnAddEvidence - Officer focuses on user-provided evidence only
            if (btnCreateHearing != null) btnCreateHearing.setVisibility(View.GONE);
            if (btnDocumentResolution != null) btnDocumentResolution.setVisibility(View.GONE);
            if (btnKPForms != null) btnKPForms.setVisibility(View.GONE);
            // btnSummons removed - Summons feature deleted from system
            if (btnResolveCase != null) {
                btnResolveCase.setVisibility(View.GONE);
            }
        }
        // Show investigation feature buttons and "Resolve Case" when status is "ONGOING" or "IN PROGRESS"
        else if ("ONGOING".equals(status) || "IN PROGRESS".equals(status)) {
            // Hide Start Investigation button (but keep View Person History visible)
            if (btnStartInvestigation != null) btnStartInvestigation.setVisibility(View.GONE);
            
            // Show Update Status button
            if (btnUpdateStatus != null) {
                btnUpdateStatus.setText("Update Status");
                btnUpdateStatus.setVisibility(View.VISIBLE);
                btnUpdateStatus.setOnClickListener(v -> showUpdateStatusDialog());
                Log.d("OfficerCaseDetail", "✅ Update Status button SHOWN");
            }
            
            // Show all investigation feature buttons
            if (btnAddWitness != null) btnAddWitness.setVisibility(View.VISIBLE);
            if (btnAddSuspect != null) btnAddSuspect.setVisibility(View.VISIBLE);
            // ❌ REMOVED: btnAddEvidence - Officer focuses on user-provided evidence only
            if (btnCreateHearing != null) btnCreateHearing.setVisibility(View.VISIBLE);
            if (btnDocumentResolution != null) btnDocumentResolution.setVisibility(View.VISIBLE);
            if (btnKPForms != null) btnKPForms.setVisibility(View.VISIBLE);
            // btnSummons removed - Summons feature deleted from system
            
            // Make sure resolve case button is created and visible
            if (btnResolveCase == null) {
                createResolveButton();
            } else {
                btnResolveCase.setVisibility(View.VISIBLE);
            }
        }
        // Hide all action buttons when resolved
        else if ("RESOLVED".equals(status)) {
            // Hide all buttons
            if (btnUpdateStatus != null) btnUpdateStatus.setVisibility(View.GONE);
            if (btnStartInvestigation != null) btnStartInvestigation.setVisibility(View.GONE);
            if (btnViewPersonHistory != null) btnViewPersonHistory.setVisibility(View.GONE);
            if (btnAddWitness != null) btnAddWitness.setVisibility(View.GONE);
            if (btnAddSuspect != null) btnAddSuspect.setVisibility(View.GONE);
            // ❌ REMOVED: btnAddEvidence - Officer focuses on user-provided evidence only
            if (btnCreateHearing != null) btnCreateHearing.setVisibility(View.GONE);
            if (btnDocumentResolution != null) btnDocumentResolution.setVisibility(View.GONE);
            if (btnKPForms != null) btnKPForms.setVisibility(View.GONE);
            // btnSummons removed - Summons feature deleted from system
            if (btnResolveCase != null) btnResolveCase.setVisibility(View.GONE);
            
            // ✅ REMOVED: Toast - Don't show on every load, only when first resolved
        }
        // Handle any other status
        else {
            // Hide all buttons by default for unknown statuses
            if (btnUpdateStatus != null) btnUpdateStatus.setVisibility(View.GONE);
            if (btnAddWitness != null) btnAddWitness.setVisibility(View.GONE);
            if (btnAddSuspect != null) btnAddSuspect.setVisibility(View.GONE);
            // ❌ REMOVED: btnAddEvidence - Officer focuses on user-provided evidence only
            if (btnCreateHearing != null) btnCreateHearing.setVisibility(View.GONE);
            if (btnDocumentResolution != null) btnDocumentResolution.setVisibility(View.GONE);
            if (btnKPForms != null) btnKPForms.setVisibility(View.GONE);
            // btnSummons removed - Summons feature deleted from system
            if (btnResolveCase != null) btnResolveCase.setVisibility(View.GONE);
            
            Log.w("OfficerCaseDetail", "Unknown status: " + status);
        }
    }
    
    private void createResolveButton() {
        // Create resolve button dynamically if not in layout
        if (btnResolveCase == null && btnUpdateStatus != null) {
            btnResolveCase = new MaterialButton(this);
            btnResolveCase.setText("Resolve Case");
            btnResolveCase.setLayoutParams(btnUpdateStatus.getLayoutParams());
            // Add to parent view if possible
            View parent = (View) btnUpdateStatus.getParent();
            if (parent != null && parent instanceof android.view.ViewGroup) {
                ((android.view.ViewGroup) parent).addView(btnResolveCase);
                btnResolveCase.setOnClickListener(v -> showResolveCaseDialog());
            }
        }
    }
    
    private void loadMediaFiles() {
        // Load images and videos from BlotterReport (same source as case list badge)
        try {
            if (currentReport == null) return;
            
            imageList.clear();
            videoList.clear();
            
            Log.d("OfficerCaseDetail", "Loading media for reportId: " + reportId);
            Log.d("OfficerCaseDetail", "ImageUris: " + currentReport.getImageUris());
            Log.d("OfficerCaseDetail", "VideoUris: " + currentReport.getVideoUris());
            
            // Load image URIs from report
            if (currentReport.getImageUris() != null && !currentReport.getImageUris().isEmpty()) {
                String[] imageArray = currentReport.getImageUris().split(",");
                for (String imageUri : imageArray) {
                    if (!imageUri.trim().isEmpty()) {
                        imageList.add(Uri.parse(imageUri.trim()));
                        Log.d("OfficerCaseDetail", "Added image: " + imageUri.trim());
                    }
                }
            }
            
            // Load video URIs from report
            if (currentReport.getVideoUris() != null && !currentReport.getVideoUris().isEmpty()) {
                String[] videoArray = currentReport.getVideoUris().split(",");
                for (String videoUri : videoArray) {
                    if (!videoUri.trim().isEmpty()) {
                        videoList.add(Uri.parse(videoUri.trim()));
                        Log.d("OfficerCaseDetail", "Added video: " + videoUri.trim());
                    }
                }
            }
            
            Log.d("OfficerCaseDetail", "Final counts - Images: " + imageList.size() + ", Videos: " + videoList.size());
            updateMediaViews();
        } catch (Exception e) {
            Log.e("OfficerCaseDetail", "Error loading media files: " + e.getMessage(), e);
            e.printStackTrace();
        }
    }
    
    private void updateMediaViews() {
        // Show/hide media sections based on content
        if (imageList.isEmpty()) {
            tvImagesLabel.setVisibility(View.GONE);
            recyclerImages.setVisibility(View.GONE);
            android.view.View cardImages = findViewById(R.id.cardImages);
            if (cardImages != null) cardImages.setVisibility(View.GONE);
        } else {
            tvImagesLabel.setVisibility(View.VISIBLE);
            recyclerImages.setVisibility(View.VISIBLE);
            android.view.View cardImages = findViewById(R.id.cardImages);
            if (cardImages != null) cardImages.setVisibility(View.VISIBLE);
            imageAdapter.notifyDataSetChanged();
        }
        
        if (videoList.isEmpty()) {
            tvVideosLabel.setVisibility(View.GONE);
            recyclerVideos.setVisibility(View.GONE);
            android.view.View cardVideos = findViewById(R.id.cardVideos);
            if (cardVideos != null) cardVideos.setVisibility(View.GONE);
        } else {
            tvVideosLabel.setVisibility(View.VISIBLE);
            recyclerVideos.setVisibility(View.VISIBLE);
            android.view.View cardVideos = findViewById(R.id.cardVideos);
            if (cardVideos != null) cardVideos.setVisibility(View.VISIBLE);
            videoAdapter.notifyDataSetChanged();
        }
    }
    
    // Officer-specific functions
    private void showUpdateStatusDialog() {
        if (currentReport == null) {
            Toast.makeText(this, "Case not loaded", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String status = currentReport.getStatus() != null ? currentReport.getStatus().toLowerCase() : "pending";
        
        // Check if case is in "assigned" status
        if ("assigned".equals(status)) {
            try {
                // Show "Start Investigation" dialog with modern design
                LayoutInflater inflater = LayoutInflater.from(this);
                android.view.View dialogView = inflater.inflate(R.layout.dialog_start_investigation, null);
                
                com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
                com.google.android.material.button.MaterialButton btnStart = dialogView.findViewById(R.id.btnStartInvestigation);
                
                if (btnCancel == null || btnStart == null) {
                    Log.e("OfficerCaseDetail", "Dialog buttons not found!");
                    return;
                }
                
                com.google.android.material.dialog.MaterialAlertDialogBuilder dialogBuilder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setView(dialogView)
                    .setCancelable(false);
                
                androidx.appcompat.app.AlertDialog alertDialog = dialogBuilder.create();
                
                btnCancel.setOnClickListener(v -> alertDialog.dismiss());
                btnStart.setOnClickListener(v -> {
                    alertDialog.dismiss();
                    startInvestigation();
                });
                
                alertDialog.show();
            } catch (Exception e) {
                Log.e("OfficerCaseDetail", "Error showing dialog: " + e.getMessage(), e);
                Toast.makeText(this, "Error showing dialog", Toast.LENGTH_SHORT).show();
            }
        } else if ("ongoing".equals(status) || "in-progress".equals(status)) {
            // Show dialog to resolve case
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Case Status: Ongoing")
                .setMessage("Would you like to resolve this case?")
                .setPositiveButton("Resolve Case", (dialog, which) -> {
                    showResolveCaseDialog();
                })
                .setNegativeButton("Cancel", null)
                .show();
        } else {
            Toast.makeText(this, "Cannot start investigation for this case status: " + currentReport.getStatus(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void startInvestigation() {
        GlobalLoadingManager.show(this, "Starting investigation...");
        
        // Update status from "assigned" to "ongoing"
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String oldStatus = currentReport.getStatus();
                currentReport.setStatus("ONGOING");
                currentReport.setUpdatedAt(System.currentTimeMillis());
                database.blotterReportDao().updateReport(currentReport);
                
                // Get officer user info for notifications
                int officerUserId = preferencesManager.getUserId();
                com.example.blottermanagementsystem.data.entity.User officerUser = database.userDao().getUserById(officerUserId);
                String officerName = officerUser != null ? officerUser.getFirstName() + " " + officerUser.getLastName() : "Officer";
                
                // Notify user and admin about status change
                if (currentReport.getUserId() > 0) {
                    notificationHelper.notifyStatusChange(
                        currentReport.getUserId(),
                        currentReport.getCaseNumber(),
                        oldStatus,
                        "ongoing",
                        currentReport.getId(),
                        officerName
                    );
                }
                
                // Notify all admins
                List<com.example.blottermanagementsystem.data.entity.User> admins = database.userDao().getUsersByRole("Admin");
                for (com.example.blottermanagementsystem.data.entity.User admin : admins) {
                    notificationHelper.notifyStatusChange(
                        admin.getId(),
                        currentReport.getCaseNumber(),
                        oldStatus,
                        "ongoing",
                        currentReport.getId(),
                        officerName
                    );
                }
                
                runOnUiThread(() -> {
                    GlobalLoadingManager.hide();
                    Toast.makeText(this, "Investigation started! Status changed to: Ongoing", Toast.LENGTH_LONG).show();
                    chipStatus.setText("ONGOING");
                    updateButtonVisibility();
                    
                    // ✅ ENABLE Investigation Actions buttons now that investigation has started
                    if (investigationActionsAdapter != null) {
                        investigationActionsAdapter.setInvestigationStarted(true);
                        android.util.Log.d("OfficerCaseDetail", "✅ Investigation Actions buttons ENABLED");
                    }
                    
                    initializeInvestigationTimeline();  // ← Refresh timeline to show investigation features
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    GlobalLoadingManager.hide();
                    Toast.makeText(this, "Error starting investigation: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    /**
     * Checks if all required investigation steps are completed
     * @return Pair<Boolean, String> where first is true if all requirements are met,
     *         and second contains error message if not met
     */
    private Pair<Boolean, String> checkInvestigationRequirements() {
        if (currentReport == null) {
            return new Pair<>(false, "Case not loaded");
        }
        
        // Get counts of investigation items
        int witnessCount = database.witnessDao().getWitnessesByReportId(currentReport.getId()).size();
        int suspectCount = database.suspectDao().getSuspectsByReportId(currentReport.getId()).size();
        int evidenceCount = database.evidenceDao().getEvidenceByReportId(currentReport.getId()).size();
        int hearingCount = database.hearingDao().getHearingsByReportId(currentReport.getId()).size();
        
        // Check for documents
        boolean hasResolution = !database.resolutionDao().getResolutionsByReportId(currentReport.getId()).isEmpty();
        boolean hasSummons = database.summonsDao().getSummonsByReportId(currentReport.getId()) != null;
        
        // Build list of missing requirements
        List<String> missingRequirements = new ArrayList<>();
        
        if (witnessCount == 0) missingRequirements.add("At least one witness must be added");
        if (suspectCount == 0) missingRequirements.add("At least one suspect must be added");
        if (evidenceCount == 0) missingRequirements.add("At least one piece of evidence must be added");
        if (hearingCount == 0) {
            missingRequirements.add("At least one hearing must be conducted");
        }
        if (!hasResolution) missingRequirements.add("A resolution document is required");
        if (!hasSummons) missingRequirements.add("A Summons document is required");
        
        if (missingRequirements.isEmpty()) {
            return new Pair<>(true, "");
        } else {
            StringBuilder errorMessage = new StringBuilder("Please complete the following before resolving the case:\n\n");
            for (String req : missingRequirements) {
                errorMessage.append("• ").append(req).append("\n");
            }
            errorMessage.append("\nUse the investigation buttons below to complete these actions.");
            return new Pair<>(false, errorMessage.toString());
        }
    }
    
    private void showResolveCaseDialog() {
        if (currentReport == null) {
            Toast.makeText(this, "Case not loaded", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String status = currentReport.getStatus() != null ? currentReport.getStatus().toLowerCase() : "pending";
        
        // Only allow resolving if status is "ongoing" or "in-progress"
        if (!"ongoing".equals(status) && !"in-progress".equals(status)) {
            Toast.makeText(this, "Case must be in 'Ongoing' status to be resolved", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check investigation requirements
        Pair<Boolean, String> requirementsCheck = checkInvestigationRequirements();
        if (!requirementsCheck.first) {
            // Show what needs to be done before resolving
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Action Required")
                .setMessage(requirementsCheck.second)
                .setPositiveButton("OK", null)
                .show();
            return;
        }
        
        // If all requirements are met, show confirmation dialog
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Resolve Case")
            .setMessage("Are you sure you want to mark this case as resolved? This will change the status from 'Ongoing' to 'Resolved' and close the case.")
            .setPositiveButton("Resolve Case", (dialog, which) -> {
                resolveCase();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void resolveCase() {
        GlobalLoadingManager.show(this, "Resolving case...");
        
        // Update status from "ongoing" to "resolved"
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String oldStatus = currentReport.getStatus();
                currentReport.setStatus("RESOLVED");
                currentReport.setUpdatedAt(System.currentTimeMillis());
                database.blotterReportDao().updateReport(currentReport);
                
                // Get officer user info for notifications
                int officerUserId = preferencesManager.getUserId();
                com.example.blottermanagementsystem.data.entity.User officerUser = database.userDao().getUserById(officerUserId);
                String officerName = officerUser != null ? officerUser.getFirstName() + " " + officerUser.getLastName() : "Officer";
                
                // Notify user who filed the case
                List<Integer> userIds = new ArrayList<>();
                if (currentReport.getUserId() > 0) {
                    userIds.add(currentReport.getUserId());
                }
                
                // Notify all admins
                List<com.example.blottermanagementsystem.data.entity.User> admins = database.userDao().getUsersByRole("Admin");
                for (com.example.blottermanagementsystem.data.entity.User admin : admins) {
                    userIds.add(admin.getId());
                }
                
                // Send case resolved notifications
                if (!userIds.isEmpty()) {
                    notificationHelper.notifyCaseResolved(
                        userIds,
                        currentReport.getCaseNumber(),
                        "Case has been resolved by " + officerName,
                        currentReport.getId(),
                        officerName
                    );
                }
                
                // Also send status change notification
                if (currentReport.getUserId() > 0) {
                    notificationHelper.notifyStatusChange(
                        currentReport.getUserId(),
                        currentReport.getCaseNumber(),
                        oldStatus,
                        "resolved",
                        currentReport.getId(),
                        officerName
                    );
                }
                
                runOnUiThread(() -> {
                    GlobalLoadingManager.hide();
                    Toast.makeText(this, "Case resolved! Status changed to: Resolved", Toast.LENGTH_LONG).show();
                    chipStatus.setText("RESOLVED");
                    updateButtonVisibility();
                    initializeInvestigationTimeline();  // ← Refresh timeline to hide all investigation features
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    GlobalLoadingManager.hide();
                    Toast.makeText(this, "Error resolving case: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            if (data != null && data.getBooleanExtra("INVESTIGATION_COMPLETE", false)) {
                // Investigation completed - reload case details
                loadCaseDetails();
            }
        }
    }
    
    
    private void showEditRestriction() {
        // Officers cannot edit original case details - investigation only
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Edit Restriction")
            .setMessage("Officers cannot edit original case details to maintain evidence integrity. Use investigation functions to update case status and add notes.")
            .setPositiveButton("OK", null)
            .show();
    }
    
    
    // Media viewing functions (same as User and Admin roles)
    private void viewImage(Uri uri) {
        try {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_image_viewer, null);
            android.widget.ImageView imageView = dialogView.findViewById(R.id.imageView);
            com.google.android.material.button.MaterialButton btnClose = dialogView.findViewById(R.id.btnClose);
            
            // Use Glide for better image loading
            com.bumptech.glide.Glide.with(this)
                .load(uri)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .into(imageView);
            
            androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();
            
            btnClose.setOnClickListener(v -> dialog.dismiss());
            
            // Make dialog full screen
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.show();
            
            // Set dialog to full screen after showing
            android.view.Window window = dialog.getWindow();
            if (window != null) {
                window.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, 
                               android.view.ViewGroup.LayoutParams.MATCH_PARENT);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
            Log.e("OfficerCaseDetail", "Error showing image: " + e.getMessage());
        }
    }
    
    private void playVideo(Uri uri) {
        // Use the same video player implementation as ReportDetailActivity
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_video_player, null);
        android.widget.VideoView videoView = dialogView.findViewById(R.id.videoView);
        android.widget.ImageButton btnClose = dialogView.findViewById(R.id.btnClose);
        android.widget.ImageButton btnPlayPause = dialogView.findViewById(R.id.btnPlayPause);
        android.widget.ImageButton btnRewind = dialogView.findViewById(R.id.btnRewind);
        android.widget.ImageButton btnForward = dialogView.findViewById(R.id.btnForward);
        android.view.View videoControlsOverlay = dialogView.findViewById(R.id.videoControlsOverlay);
        android.view.View centerControls = dialogView.findViewById(R.id.centerControls);
        android.view.View bottomControls = dialogView.findViewById(R.id.bottomControls);
        android.widget.SeekBar seekBar = dialogView.findViewById(R.id.seekBar);
        android.widget.TextView tvCurrentTime = dialogView.findViewById(R.id.tvCurrentTime);
        android.widget.TextView tvDuration = dialogView.findViewById(R.id.tvDuration);
        android.widget.ProgressBar progressBar = dialogView.findViewById(R.id.progressBar);
        
        // Fade Animation Handler - declare early
        android.os.Handler controlsHandler = new android.os.Handler();
        
        // Set video URI and start
        videoView.setVideoURI(uri);
        
        // Show progress while loading
        progressBar.setVisibility(View.VISIBLE);
        
        // Hide progress when ready
        videoView.setOnPreparedListener(mp -> {
            progressBar.setVisibility(View.GONE);
            
            // Set up SeekBar
            int duration = videoView.getDuration();
            seekBar.setMax(duration);
            tvDuration.setText("-" + formatTime(duration));
            
            videoView.start();
            
            // Start updating progress
            updateVideoProgress(videoView, seekBar, tvCurrentTime, tvDuration, controlsHandler);
        });
        
        // Handle errors
        videoView.setOnErrorListener((mp, what, extra) -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Error playing video", Toast.LENGTH_SHORT).show();
            return true;
        });
        
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create();
        
        // Fade Out Animation
        Runnable fadeOutControls = () -> {
            centerControls.animate()
                .alpha(0.0f)
                .setDuration(300)
                .start();
            bottomControls.animate()
                .alpha(0.0f)
                .setDuration(300)
                .start();
        };
        
        // Fade In Animation
        Runnable fadeInControls = () -> {
            centerControls.animate()
                .alpha(1.0f)
                .setDuration(300)
                .start();
            bottomControls.animate()
                .alpha(1.0f)
                .setDuration(300)
                .start();
            
            // Auto-hide after 3 seconds
            controlsHandler.removeCallbacks(fadeOutControls);
            controlsHandler.postDelayed(fadeOutControls, 3000);
        };
        
        // Show/hide controls on video tap with fade animation
        videoControlsOverlay.setOnClickListener(v -> {
            if (centerControls.getAlpha() > 0.5f) {
                // Currently visible, fade out
                controlsHandler.removeCallbacks(fadeOutControls);
                fadeOutControls.run();
            } else {
                // Currently hidden, fade in
                fadeInControls.run();
            }
        });
        
        // Play/Pause button with new icons
        btnPlayPause.setOnClickListener(v -> {
            if (videoView.isPlaying()) {
                videoView.pause();
                btnPlayPause.setImageResource(R.drawable.ic_play);
            } else {
                videoView.start();
                btnPlayPause.setImageResource(R.drawable.ic_pause);
            }
            fadeInControls.run(); // Reset fade timer
        });
        
        // Rewind 5 seconds
        btnRewind.setOnClickListener(v -> {
            int currentPosition = videoView.getCurrentPosition();
            int newPosition = Math.max(0, currentPosition - 5000);
            videoView.seekTo(newPosition);
            
            // UPDATE SEEKBAR AND TIMESTAMPS IMMEDIATELY
            int duration = videoView.getDuration();
            int remaining = duration - newPosition;
            seekBar.setProgress(newPosition);
            tvCurrentTime.setText(formatTime(newPosition));
            tvDuration.setText("-" + formatTime(remaining));
            
            fadeInControls.run(); // Reset fade timer
        });
        
        // Forward 5 seconds
        btnForward.setOnClickListener(v -> {
            int currentPosition = videoView.getCurrentPosition();
            int duration = videoView.getDuration();
            int newPosition = Math.min(duration, currentPosition + 5000);
            videoView.seekTo(newPosition);
            
            // UPDATE SEEKBAR AND TIMESTAMPS IMMEDIATELY
            int remaining = duration - newPosition;
            seekBar.setProgress(newPosition);
            tvCurrentTime.setText(formatTime(newPosition));
            tvDuration.setText("-" + formatTime(remaining));
            
            fadeInControls.run(); // Reset fade timer
        });
        
        // SeekBar interaction
        seekBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    videoView.seekTo(progress);
                    int duration = videoView.getDuration();
                    int remaining = duration - progress;
                    tvCurrentTime.setText(formatTime(progress));
                    tvDuration.setText("-" + formatTime(remaining));
                }
            }
            
            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {
                // PAUSE auto-hide while dragging
                controlsHandler.removeCallbacks(fadeOutControls);
                // Show controls and keep them visible
                centerControls.animate().alpha(1.0f).setDuration(300).start();
                bottomControls.animate().alpha(1.0f).setDuration(300).start();
            }
            
            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {
                // RESUME auto-hide after dragging stops
                fadeInControls.run();
            }
        });
        
        // Close button
        btnClose.setOnClickListener(v -> dialog.dismiss());
        
        // Start with controls visible, then auto-hide
        fadeInControls.run();
        
        // Show dialog
        dialog.show();
    }
    
    /**
     * Helper method to update video progress in the player
     */
    private void updateVideoProgress(
        android.widget.VideoView videoView,
        android.widget.SeekBar seekBar,
        TextView tvCurrentTime,
        TextView tvDuration,
        Handler handler
    ) {
        if (videoView.isPlaying()) {
            int currentPosition = videoView.getCurrentPosition();
            int duration = videoView.getDuration();
            int remaining = duration - currentPosition;
            
            // Update SeekBar progress
            seekBar.setProgress(currentPosition);
            
            // Update timestamps
            tvCurrentTime.setText(formatTime(currentPosition));
            tvDuration.setText("-" + formatTime(remaining));
            
            // Schedule next update
            handler.postDelayed(() -> updateVideoProgress(videoView, seekBar, tvCurrentTime, tvDuration, handler), 1000);
        }
    }
    
    private String formatTime(int milliseconds) {
        int seconds = (milliseconds / 1000) % 60;
        int minutes = (milliseconds / (1000 * 60)) % 60;
        int hours = (milliseconds / (1000 * 60 * 60));
        
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%d:%02d", minutes, seconds);
        }
    }
    
    // Investigation Feature Methods - Floating Dialogs
    private void openAddWitness() {
        AddWitnessDialogFragment dialog = AddWitnessDialogFragment.newInstance(reportId, witness -> {
            // ✅ Witness added successfully - refresh timeline to unlock next button
            Toast.makeText(this, "✅ Witness added! Next step unlocked.", Toast.LENGTH_SHORT).show();
            refreshInvestigationTimeline();
        });
        dialog.show(getSupportFragmentManager(), "AddWitness");
    }
    
    private void openAddSuspect() {
        AddSuspectDialogFragment dialog = AddSuspectDialogFragment.newInstance(reportId, suspect -> {
            // ✅ Suspect added successfully - refresh timeline to unlock next button
            Toast.makeText(this, "✅ Suspect added! Next step unlocked.", Toast.LENGTH_SHORT).show();
            refreshInvestigationTimeline();
        });
        dialog.show(getSupportFragmentManager(), "AddSuspect");
    }
    
    // ❌ REMOVED: openAddEvidence() - Officer focuses on user-provided evidence only
    
    private void openCreateHearing() {
        ScheduleHearingDialogFragment dialog = ScheduleHearingDialogFragment.newInstance(reportId, hearing -> {
            // ✅ Hearing scheduled successfully - refresh timeline to unlock next button
            refreshInvestigationTimeline();
        });
        dialog.show(getSupportFragmentManager(), "ScheduleHearing");
    }
    
    // ✅ EDIT/RESCHEDULE HEARING
    private void openEditHearing(com.example.blottermanagementsystem.data.entity.Hearing hearing) {
        com.example.blottermanagementsystem.ui.dialogs.EditHearingDialogFragment dialog = 
            com.example.blottermanagementsystem.ui.dialogs.EditHearingDialogFragment.newInstance(hearing, updatedHearing -> {
                // ✅ Hearing rescheduled successfully - refresh timeline
                Toast.makeText(this, "Hearing rescheduled successfully", Toast.LENGTH_SHORT).show();
                refreshInvestigationTimeline();
            });
        dialog.show(getSupportFragmentManager(), "EditHearing");
    }
    
    private void openDocumentResolution() {
        DocumentResolutionDialogFragment dialog = DocumentResolutionDialogFragment.newInstance(reportId, resolution -> {
            // ✅ Resolution documented successfully - refresh timeline
            refreshInvestigationTimeline();
        });
        dialog.show(getSupportFragmentManager(), "DocumentResolution");
    }
    
    private void openSendSms() {
        if (currentReport == null) {
            Toast.makeText(this, "Case not loaded", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // ✅ Fetch hearing details from database if available
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<com.example.blottermanagementsystem.data.entity.Hearing> hearings = 
                    database.hearingDao().getHearingsByReportId(reportId);
                
                String hearingDate = "TBD";
                String hearingTime = "TBD";
                
                if (hearings != null && !hearings.isEmpty()) {
                    // Get the first (most recent) hearing
                    com.example.blottermanagementsystem.data.entity.Hearing hearing = hearings.get(0);
                    hearingDate = hearing.getHearingDate() != null ? hearing.getHearingDate() : "TBD";
                    hearingTime = hearing.getHearingTime() != null ? hearing.getHearingTime() : "TBD";
                    Log.d("OfficerCaseDetail", "✅ Hearing found - Date: " + hearingDate + ", Time: " + hearingTime);
                } else {
                    Log.d("OfficerCaseDetail", "⚠️ No hearing scheduled yet");
                }
                
                final String finalHearingDate = hearingDate;
                final String finalHearingTime = hearingTime;
                
                runOnUiThread(() -> {
                    OfficerSendSmsDialogFragment dialog = OfficerSendSmsDialogFragment.newInstance(
                        currentReport.getCaseNumber(),
                        currentReport.getRespondentName(),
                        finalHearingDate,
                        finalHearingTime,
                        "Barangay Hall",
                        currentReport.getStatus(),
                        new OfficerSendSmsDialogFragment.OnSmsSentListener() {
                            @Override
                            public void onSmsSent(String phoneNumber, String messageType) {
                                // Toast removed - SMS sent silently
                            }
                            
                            @Override
                            public void onSmsFailed(String errorMessage) {
                                Toast.makeText(OfficerCaseDetailActivity.this, "❌ SMS failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        }
                    );
                    dialog.show(getSupportFragmentManager(), "SendSms");
                });
            } catch (Exception e) {
                Log.e("OfficerCaseDetail", "Error fetching hearing details: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    OfficerSendSmsDialogFragment dialog = OfficerSendSmsDialogFragment.newInstance(
                        currentReport.getCaseNumber(),
                        currentReport.getRespondentName(),
                        "TBD",
                        "TBD",
                        "Barangay Hall",
                        currentReport.getStatus(),
                        new OfficerSendSmsDialogFragment.OnSmsSentListener() {
                            @Override
                            public void onSmsSent(String phoneNumber, String messageType) {
                                // Toast removed - SMS sent silently
                            }
                            
                            @Override
                            public void onSmsFailed(String errorMessage) {
                                Toast.makeText(OfficerCaseDetailActivity.this, "❌ SMS failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        }
                    );
                    dialog.show(getSupportFragmentManager(), "SendSms");
                });
            }
        });
    }
    
    // ✅ View Details Methods for Completed Steps
    private void openViewWitnesses() {
        ViewWitnessesDialogFragment dialog = ViewWitnessesDialogFragment.newInstance(reportId);
        dialog.show(getSupportFragmentManager(), "ViewWitnesses");
    }
    
    private void openViewSuspects() {
        ViewSuspectsDialogFragment dialog = ViewSuspectsDialogFragment.newInstance(reportId);
        dialog.show(getSupportFragmentManager(), "ViewSuspects");
    }
    
    private void openViewHearings() {
        ViewHearingsDialogFragment dialog = ViewHearingsDialogFragment.newInstance(reportId);
        dialog.show(getSupportFragmentManager(), "ViewHearings");
    }
    
    private void openViewResolution() {
        ViewResolutionDialogFragment dialog = ViewResolutionDialogFragment.newInstance(reportId);
        dialog.show(getSupportFragmentManager(), "ViewResolution");
    }
    
    private void openViewPersonHistory() {
        if (currentReport == null) {
            Toast.makeText(this, "No case information available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get respondent name from database
        String respondentName = currentReport.getRespondentName();
        
        // If no respondent name, use "Unknown" as placeholder
        final String finalRespondentName;
        if (respondentName == null || respondentName.isEmpty() || respondentName.equals("N/A")) {
            finalRespondentName = "Unknown Respondent";
            Log.d("OfficerCaseDetail", "⚠️ View Person History - No respondent name, using placeholder");
        } else {
            finalRespondentName = respondentName;
        }
        
        // Create or get person ID from database
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Get or create Person record
                com.example.blottermanagementsystem.data.entity.Person person = 
                    database.personDao().getPersonByName(finalRespondentName);
                
                int personId;
                if (person == null) {
                    // Create new person record
                    person = new com.example.blottermanagementsystem.data.entity.Person();
                    person.setName(finalRespondentName);
                    person.setCreatedDate(System.currentTimeMillis());
                    personId = (int) database.personDao().insertPerson(person);
                    Log.d("OfficerCaseDetail", "Created new person record: " + finalRespondentName + " (ID: " + personId + ")");
                } else {
                    personId = person.getId();
                    Log.d("OfficerCaseDetail", "Found existing person: " + finalRespondentName + " (ID: " + personId + ")");
                }
                
                // Open View Person History Activity
                runOnUiThread(() -> {
                    Intent intent = new Intent(OfficerCaseDetailActivity.this, OfficerViewPersonHistoryActivity.class);
                    intent.putExtra("person_id", personId);
                    intent.putExtra("person_name", finalRespondentName);
                    startActivity(intent);
                });
            } catch (Exception e) {
                Log.e("OfficerCaseDetail", "Error opening person history: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(OfficerCaseDetailActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        android.util.Log.d("OfficerCaseDetail", "🔄 onResume called");
        // ✅ Refresh case details AND timeline when returning to this screen
        if (reportId != -1) {
            android.util.Log.d("OfficerCaseDetail", "🔄 Loading case details...");
            loadCaseDetails();
            // ✅ Delay timeline refresh to ensure data is loaded
            new Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                android.util.Log.d("OfficerCaseDetail", "🔄 Refreshing timeline after delay...");
                refreshInvestigationTimeline();
            }, 1000); // 1 second delay to ensure loadCaseDetails completes
        }
    }
    
    /**
     * Refresh the entire investigation timeline
     * Called when returning to this activity OR when hearing time is set
     * ✅ PUBLIC - Can be called from dialog fragments
     */
    public void refreshInvestigationTimeline() {
        // ✅ Add delay to ensure data is saved to database before refreshing
        new Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            android.util.Log.d("OfficerCaseDetail", "Starting timeline refresh...");
            initializeInvestigationTimeline();
            android.util.Log.d("OfficerCaseDetail", "Timeline refresh complete - Document Resolution button should now be enabled");
        }, 500); // 500ms delay to ensure database write
    }
    
    // ✅ PUBLIC METHOD - Called directly from dialog fragments
    public void refreshTimelineDirectly() {
        android.util.Log.d("OfficerCaseDetail", "refreshTimelineDirectly() called from dialog");
        // Add small delay to ensure data is saved
        new Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            android.util.Log.d("OfficerCaseDetail", "🔄 Direct timeline refresh starting...");
            initializeInvestigationTimeline();
            android.util.Log.d("OfficerCaseDetail", "✅ Direct timeline refresh complete");
        }, 300); // 300ms delay
    }
    
    /**
     * Set the status chip color based on the status value
     * ✅ FIXED: Resolved cases use GREEN, others use BLUE
     * Color coding:
     * - Pending/Assigned/Ongoing → Electric Blue
     * - Resolved/Closed/Settled → Green
     */
    private void setStatusChipColor(String status) {
        if (chipStatus == null) return;
        
        int backgroundColor;
        String statusLower = status != null ? status.toLowerCase() : "";
        
        // ✅ GREEN for resolved/completed cases
        if (statusLower.contains("resolved") || statusLower.contains("closed") || statusLower.contains("settled")) {
            backgroundColor = getColor(R.color.status_resolved); // GREEN
            android.util.Log.d("OfficerCaseDetail", "✅ Status '" + status + "' set to GREEN (Resolved)");
        }
        // ✅ BLUE for active/pending cases
        else if (statusLower.contains("pending") || statusLower.contains("assigned") || 
                 statusLower.contains("ongoing") || statusLower.contains("in progress")) {
            backgroundColor = getColor(R.color.electric_blue); // BLUE
            android.util.Log.d("OfficerCaseDetail", "✅ Status '" + status + "' set to BLUE (Active)");
        }
        // Default to BLUE for unknown statuses
        else {
            backgroundColor = getColor(R.color.electric_blue);
            android.util.Log.d("OfficerCaseDetail", "ℹ️ Status '" + status + "' set to default BLUE");
        }
        
        chipStatus.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(backgroundColor));
    }
}
