package com.example.blottermanagementsystem.ui.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.database.BlotterDatabase;
import com.example.blottermanagementsystem.data.entity.BlotterReport;
import com.example.blottermanagementsystem.data.entity.User;
import com.example.blottermanagementsystem.ui.adapters.ImageAdapter;
import com.example.blottermanagementsystem.ui.adapters.VideoAdapter;
import com.example.blottermanagementsystem.ui.adapters.InvestigationStepAdapter;
import com.example.blottermanagementsystem.data.model.InvestigationStep;
import com.example.blottermanagementsystem.utils.MediaManager;
import com.example.blottermanagementsystem.utils.NotificationHelper;
import com.example.blottermanagementsystem.utils.PreferencesManager;
import com.example.blottermanagementsystem.data.api.ApiClient;
import com.example.blottermanagementsystem.utils.NetworkMonitor;
import com.example.blottermanagementsystem.utils.TimelineUpdateManager;
import com.example.blottermanagementsystem.utils.GlobalLoadingManager;
import com.example.blottermanagementsystem.utils.CaseEventNotificationHelper;
import com.example.blottermanagementsystem.data.entity.Evidence;
import android.util.Log;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import java.util.ArrayList;
import java.util.List;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.Executors;
import android.content.Intent;
import androidx.core.content.FileProvider;
import java.io.File;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

public class ReportDetailActivity extends BaseActivity {
    
    private BlotterDatabase database;
    private BlotterReport report;
    private int reportId;
    private PreferencesManager preferencesManager;
    private NotificationHelper notificationHelper;
    
    private Toolbar toolbar;
    private TextView tvReportNumber, tvStatus, tvIncidentType, tvIncidentDate;
    private TextView tvComplainantName, tvComplainantContact, tvComplainantAddress;
    private TextView tvNarrative, tvIncidentLocation;
    private Chip chipStatus;
    private MaterialButton btnEdit, btnDelete, btnExportPdf;
    private LinearLayout layoutAdminActions, layoutKPForms;
    private MaterialButton btnAssignOfficer, btnUpdateStatus, btnStartInvestigation;
    private MaterialButton btnKPForm1, btnKPForm7, btnKPForm16, btnCertification;
    
    private TextView tvRespondentTitle;
    private androidx.cardview.widget.CardView cardRespondent;
    private LinearLayout layoutRespondentName, layoutRespondentAlias, layoutRespondentAddress;
    private LinearLayout layoutRespondentContact, layoutAccusation, layoutRelationship;
    private TextView tvRespondentName, tvRespondentAlias, tvRespondentAddress;
    private TextView tvRespondentContact, tvAccusation, tvRelationship;
    
    private TextView tvEvidenceTitle, tvImagesLabel, tvVideosLabel;
    private RecyclerView recyclerImages, recyclerVideos;
    private CardView cardImages, cardVideos;
    private ImageAdapter imageAdapter;
    private VideoAdapter videoAdapter;
    private MediaManager mediaManager;
    
    // Investigation Timeline
    private RecyclerView rvInvestigationSteps;
    private InvestigationStepAdapter stepAdapter;
    private List<InvestigationStep> investigationSteps = new ArrayList<>();
    private boolean isTimelineInitializing = false;  // Prevent concurrent initialization
    
    private List<Uri> imageList = new ArrayList<>();
    private List<Uri> videoList = new ArrayList<>();
    
    // Activity Result Launcher for Edit Report
    private final ActivityResultLauncher<Intent> editReportLauncher = 
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                boolean reportUpdated = result.getData().getBooleanExtra("REPORT_UPDATED", false);
                if (reportUpdated) {
                    // Reload report data to show updated UI immediately (without loading dialog)
                    loadReportDetailsQuietly();
                }
            }
        });
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_report_detail);
        
        try {
            database = BlotterDatabase.getDatabase(this);
            preferencesManager = new PreferencesManager(this);
            notificationHelper = new NotificationHelper(this);
            reportId = getIntent().getIntExtra("REPORT_ID", -1);
            
            if (reportId == -1) {
                Toast.makeText(this, "Invalid report", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            
            initViews();
            setupToolbar();
            setupListeners();
            loadReportDetails();
        } catch (Exception e) {
            Toast.makeText(this, "Error loading report: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }
    
    private TextView tvAssignedOfficers;
    private androidx.cardview.widget.CardView cardAssignedOfficers;
    
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvReportNumber = findViewById(R.id.tvCaseNumber);
        chipStatus = findViewById(R.id.chipStatus);
        tvAssignedOfficers = findViewById(R.id.tvAssignedOfficers);
        cardAssignedOfficers = findViewById(R.id.cardAssignedOfficers);
        tvIncidentType = findViewById(R.id.tvIncidentType);
        tvIncidentDate = findViewById(R.id.tvIncidentDate);
        tvComplainantName = findViewById(R.id.tvComplainantName);
        tvComplainantContact = findViewById(R.id.tvContactNumber);
        tvComplainantAddress = findViewById(R.id.tvAddress);
        tvNarrative = findViewById(R.id.tvDescription);
        tvIncidentLocation = findViewById(R.id.tvIncidentLocation);
        
        layoutAdminActions = findViewById(R.id.layoutAdminActions);
        btnAssignOfficer = findViewById(R.id.btnAssignOfficer);
        btnUpdateStatus = findViewById(R.id.btnUpdateStatus);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
        btnExportPdf = findViewById(R.id.btnExportPdf);
        
        // Hide admin/edit buttons for user role - this is read-only view for users
        if (btnEdit != null) {
            btnEdit.setVisibility(View.GONE);
        }
        if (btnDelete != null) {
            btnDelete.setVisibility(View.GONE);
        }
        if (layoutAdminActions != null) {
            layoutAdminActions.setVisibility(View.GONE);
        }
        
        // PDF Export button handler
        if (btnExportPdf != null) {
            btnExportPdf.setOnClickListener(v -> exportReportToPdf());
        }
        
        layoutKPForms = findViewById(R.id.layoutKPForms);
        btnKPForm1 = findViewById(R.id.btnKPForm1);
        btnKPForm7 = findViewById(R.id.btnKPForm7);
        btnKPForm16 = findViewById(R.id.btnKPForm16);
        btnCertification = findViewById(R.id.btnCertification);
        
        btnStartInvestigation = findViewById(R.id.btnStartInvestigation);
        
        tvRespondentTitle = findViewById(R.id.tvRespondentTitle);
        cardRespondent = findViewById(R.id.cardRespondent);
        layoutRespondentName = findViewById(R.id.layoutRespondentName);
        layoutRespondentAlias = findViewById(R.id.layoutRespondentAlias);
        layoutRespondentAddress = findViewById(R.id.layoutRespondentAddress);
        layoutRespondentContact = findViewById(R.id.layoutRespondentContact);
        layoutAccusation = findViewById(R.id.layoutAccusation);
        layoutRelationship = findViewById(R.id.layoutRelationship);
        tvRespondentName = findViewById(R.id.tvRespondentName);
        tvRespondentAlias = findViewById(R.id.tvRespondentAlias);
        tvRespondentAddress = findViewById(R.id.tvRespondentAddress);
        tvRespondentContact = findViewById(R.id.tvRespondentContact);
        tvAccusation = findViewById(R.id.tvAccusation);
        tvRelationship = findViewById(R.id.tvRelationship);
        
        tvEvidenceTitle = findViewById(R.id.tvEvidenceTitle);
        tvImagesLabel = findViewById(R.id.tvImagesLabel);
        tvVideosLabel = findViewById(R.id.tvVideosLabel);
        recyclerImages = findViewById(R.id.recyclerImages);
        recyclerVideos = findViewById(R.id.recyclerVideos);
        cardImages = findViewById(R.id.cardImages);
        cardVideos = findViewById(R.id.cardVideos);
        
        // Investigation Timeline (READ-ONLY for users)
        rvInvestigationSteps = findViewById(R.id.rvInvestigationSteps);
        if (rvInvestigationSteps != null) {
            stepAdapter = new InvestigationStepAdapter(investigationSteps, new InvestigationStepAdapter.OnStepActionListener() {
                @Override
                public void onStepAction(InvestigationStep step) {
                    android.util.Log.d("ReportDetail", "Timeline step clicked: " + step.getTitle());
                }
                @Override
                public void onViewWitnesses(int reportId) {
                    showWitnessesDialog(reportId);
                }
                @Override
                public void onViewSuspects(int reportId) {
                    showSuspectsDialog(reportId);
                }
                @Override
                public void onViewEvidence(int reportId) {
                    showEvidenceDialog(reportId);
                }
                @Override
                public void onViewHearings(int reportId) {
                    showHearingsDialog(reportId);
                }
                @Override
                public void onViewResolution(int reportId) {
                    showResolutionDialog(reportId);
                }
            });
            rvInvestigationSteps.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
            rvInvestigationSteps.setAdapter(stepAdapter);
        }
        
        mediaManager = new MediaManager();
        
        if (recyclerImages != null && recyclerVideos != null) {
            setupEvidenceRecyclerViews();
        }
    }
    
    private void setupEvidenceRecyclerViews() {
        imageAdapter = new ImageAdapter(imageList, new ImageAdapter.OnImageClickListener() {
            @Override
            public void onImageClick(Uri uri) {
                // Preview image using built-in viewer
                previewImage(uri);
            }
            
            @Override
            public void onImageDelete(int position) {
                // Not allowed in view mode
            }
        }, false);
        LinearLayoutManager imageLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerImages.setLayoutManager(imageLayoutManager);
        recyclerImages.setAdapter(imageAdapter);
        
        videoAdapter = new VideoAdapter(videoList, new VideoAdapter.OnVideoClickListener() {
            @Override
            public void onVideoClick(Uri uri) {
                // Play video using built-in player
                playVideo(uri);
            }
            
            @Override
            public void onVideoDelete(int position) {
                // Not allowed in view mode
            }
        }, false);
        LinearLayoutManager videoLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerVideos.setLayoutManager(videoLayoutManager);
        recyclerVideos.setAdapter(videoAdapter);
    }
    
    private void previewImage(Uri uri) {
        try {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_image_viewer, null);
            android.widget.ImageView imageView = dialogView.findViewById(R.id.imageView);
            com.google.android.material.button.MaterialButton btnClose = dialogView.findViewById(R.id.btnClose);
            
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
            android.util.Log.e("ReportDetailActivity", "Error showing image: " + e.getMessage());
        }
    }
    
    private void playVideo(Uri uri) {
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
    
    private void updateVideoProgress(android.widget.VideoView videoView, android.widget.SeekBar seekBar, 
                                   android.widget.TextView tvCurrentTime, android.widget.TextView tvDuration, 
                                   android.os.Handler handler) {
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
    
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Report Details");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }
    
    private void setupListeners() {
        // Edit button listener
        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> editReport());
        }
        
        // Delete button listener
        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> showDeleteConfirmation());
        }
    }
    
    private void editReport() {
        if (report == null) {
            Toast.makeText(this, "Report data not loaded", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent intent = new Intent(this, EditReportActivity.class);
        intent.putExtra("REPORT_ID", reportId);
        editReportLauncher.launch(intent);
    }
    
    /**
     * Show modern dark theme delete confirmation dialog
     * Header: Dark primary color background
     * Body: Light dark secondary color
     * Buttons: Red (Cancel) and Electric Blue (Ok)
     */
    private void showDeleteConfirmation() {
        // Create custom dialog view
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_delete_report, null);
        
        // Get dialog components
        android.widget.TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
        android.widget.TextView tvMessage = dialogView.findViewById(R.id.tvMessage);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        com.google.android.material.button.MaterialButton btnOk = dialogView.findViewById(R.id.btnOk);
        
        // Create dialog
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setCancelable(false);
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        
        // Set dialog background to transparent (we'll use the custom layout background)
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        // Set button listeners
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnOk.setOnClickListener(v -> {
            dialog.dismiss();
            deleteReport();
        });
        
        dialog.show();
    }
    
    private void deleteReport() {
        if (report == null) return;
        
        com.example.blottermanagementsystem.utils.GlobalLoadingManager.show(this, "Deleting report...");
        
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // ✅ Delete from local database first
                database.blotterReportDao().deleteReport(report);
                Log.d("ReportDetail", "✅ Report deleted from local database: " + report.getCaseNumber());
                
                // ✅ Sync delete to API if online (wait for response)
                NetworkMonitor networkMonitor = new NetworkMonitor(ReportDetailActivity.this);
                Integer apiId = report.getApiId();  // Use API ID if available
                int deleteId = (apiId != null && apiId > 0) ? apiId : report.getId();
                
                if (networkMonitor.isNetworkAvailable() && deleteId > 0) {
                    Log.d("ReportDetail", "🗑️ Attempting to delete from API - API ID: " + deleteId + ", Local ID: " + report.getId() + ", Case: " + report.getCaseNumber());
                    
                    // Use a CountDownLatch to wait for the API response
                    java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
                    final int finalDeleteId = deleteId;
                    
                    ApiClient.deleteReport(finalDeleteId, new ApiClient.ApiCallback<String>() {
                        @Override
                        public void onSuccess(String result) {
                            Log.d("ReportDetail", "✅ Report deleted from API: " + report.getCaseNumber() + " (API ID: " + finalDeleteId + ")");
                            latch.countDown();
                        }
                        
                        @Override
                        public void onError(String errorMessage) {
                            Log.w("ReportDetail", "⚠️ Failed to delete from API (API ID: " + finalDeleteId + "): " + errorMessage);
                            // Don't fail - report is already deleted locally
                            latch.countDown();
                        }
                    });
                    
                    // Wait for API response (max 10 seconds)
                    try {
                        latch.await(10, java.util.concurrent.TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Log.e("ReportDetail", "Timeout waiting for API delete response");
                    }
                } else {
                    Log.i("ReportDetail", "Offline mode or invalid ID - skipping API delete (API ID: " + apiId + ", Local ID: " + report.getId() + ")");
                }
                
                // ✅ Cancel the push notification for this case
                android.app.NotificationManager notificationManager = 
                    (android.app.NotificationManager) getSystemService(android.content.Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    notificationManager.cancel(report.getId() + 2000);  // Same ID used in CaseEventNotificationHelper
                    Log.i("ReportDetail", "✅ Notification cancelled for case " + report.getCaseNumber());
                }
                
                runOnUiThread(() -> {
                    com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
                    Toast.makeText(ReportDetailActivity.this, "Report deleted", Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
                    Toast.makeText(ReportDetailActivity.this, "Error deleting report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void loadReportDetails() {
        com.example.blottermanagementsystem.utils.GlobalLoadingManager.show(this, "Loading report...");

        // Load from local database FIRST (fast) - don't wait for API
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                report = database.blotterReportDao().getReportById(reportId);
                
                runOnUiThread(() -> {
                    com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
                    if (report != null) {
                        displayReportDetails();
                    } else {
                        Toast.makeText(this, "Report not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
                    Toast.makeText(this, "Error loading report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
        
        // Sync with API in background (don't block UI)
        NetworkMonitor networkMonitor = new NetworkMonitor(this);
        if (networkMonitor.isNetworkAvailable()) {
            Executors.newSingleThreadExecutor().execute(() -> {
                ApiClient.getReportById(reportId, new ApiClient.ApiCallback<BlotterReport>() {
                    @Override
                    public void onSuccess(BlotterReport apiReport) {
                        // Update local database silently
                        Executors.newSingleThreadExecutor().execute(() -> {
                            try {
                                database.blotterReportDao().updateReport(apiReport);
                                // Refresh UI with updated data (WITHOUT reinitializing timeline to avoid duplication)
                                runOnUiThread(() -> {
                                    report = apiReport;
                                    updateReportDetailsOnly();
                                });
                            } catch (Exception e) {
                                android.util.Log.e("ReportDetail", "Error syncing report: " + e.getMessage());
                            }
                        });
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        android.util.Log.w("ReportDetail", "API sync error: " + errorMessage);
                        // Continue with local data - no need to show error
                    }
                });
            });
        }
    }
    
    private void loadFromDatabase() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                report = database.blotterReportDao().getReportById(reportId);
                
                runOnUiThread(() -> {
                    com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
                    if (report != null) {
                        displayReportDetails();
                    } else {
                        Toast.makeText(this, "Report not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
                    Toast.makeText(this, "Error loading report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    /**
     * Load report details quietly without showing loading dialog
     * Used when refreshing after edit to avoid blocking UI
     */
    private void loadReportDetailsQuietly() {
        // Load from local database FIRST (fast) - don't wait for API
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                report = database.blotterReportDao().getReportById(reportId);
                
                runOnUiThread(() -> {
                    if (report != null) {
                        displayReportDetails();
                    } else {
                        Toast.makeText(this, "Report not found", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error loading report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
        
        // Sync with API in background (don't block UI)
        NetworkMonitor networkMonitor = new NetworkMonitor(this);
        if (networkMonitor.isNetworkAvailable()) {
            Executors.newSingleThreadExecutor().execute(() -> {
                ApiClient.getReportById(reportId, new ApiClient.ApiCallback<BlotterReport>() {
                    @Override
                    public void onSuccess(BlotterReport apiReport) {
                        // Update local database silently
                        Executors.newSingleThreadExecutor().execute(() -> {
                            try {
                                database.blotterReportDao().updateReport(apiReport);
                                // Refresh UI with updated data
                                runOnUiThread(() -> {
                                    report = apiReport;
                                    updateReportDetailsOnly();
                                });
                            } catch (Exception e) {
                                android.util.Log.e("ReportDetail", "Error syncing report: " + e.getMessage());
                            }
                        });
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        android.util.Log.w("ReportDetail", "API sync error: " + errorMessage);
                        // Continue with local data - no need to show error
                    }
                });
            });
        }
    }
    
    private void displayReportDetails() {
        if (report == null) return;
        
        tvReportNumber.setText(report.getCaseNumber());
        chipStatus.setText(report.getStatus());
        setStatusChipColor(report.getStatus());
        
        // Display Assigned Officers
        displayAssignedOfficers();
        
        tvIncidentType.setText(report.getIncidentType() != null ? report.getIncidentType() : "N/A");
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
        tvIncidentDate.setText(report.getIncidentDate() > 0 ? 
            dateFormat.format(new java.util.Date(report.getIncidentDate())) : "N/A");
        
        tvIncidentLocation.setText(report.getIncidentLocation() != null ? 
            report.getIncidentLocation() : "N/A");
        
        tvComplainantName.setText(report.getComplainantName() != null ? 
            report.getComplainantName() : "N/A");
        tvComplainantContact.setText(report.getComplainantContact() != null ? 
            report.getComplainantContact() : "N/A");
        tvComplainantAddress.setText(report.getComplainantAddress() != null ? 
            report.getComplainantAddress() : "N/A");
        
        tvNarrative.setText(report.getNarrative() != null ? 
            report.getNarrative() : "No narrative provided");
        
        // Display Respondent Information if available
        displayRespondentInformation();
        
        // Display Evidence (Images and Videos)
        displayEvidence();
        
        updateButtonVisibility();
        
        // Configure timeline adapter for USER role (read-only with view buttons)
        if (stepAdapter != null) {
            stepAdapter.setUserRole("USER");
            stepAdapter.setReportId(reportId);
        }
        
        // Initialize/refresh timeline on background thread (requires database access)
        // Timeline will refresh every time report is loaded (via onResume)
        // This ensures User and Admin see real-time updates from Officer actions
        Executors.newSingleThreadExecutor().execute(this::initializeInvestigationTimeline);
    }
    
    /**
     * Update report details WITHOUT reinitializing the timeline
     * Used by API sync to avoid duplicate timeline steps
     */
    private void updateReportDetailsOnly() {
        if (report == null) return;
        
        tvReportNumber.setText(report.getCaseNumber());
        chipStatus.setText(report.getStatus());
        setStatusChipColor(report.getStatus());
        
        // Display Assigned Officers
        displayAssignedOfficers();
        
        tvIncidentType.setText(report.getIncidentType() != null ? report.getIncidentType() : "N/A");
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
        tvIncidentDate.setText(report.getIncidentDate() > 0 ? 
            dateFormat.format(new java.util.Date(report.getIncidentDate())) : "N/A");
        
        tvIncidentLocation.setText(report.getIncidentLocation() != null ? 
            report.getIncidentLocation() : "N/A");
        
        tvComplainantName.setText(report.getComplainantName() != null ? 
            report.getComplainantName() : "N/A");
        tvComplainantContact.setText(report.getComplainantContact() != null ? 
            report.getComplainantContact() : "N/A");
        tvComplainantAddress.setText(report.getComplainantAddress() != null ? 
            report.getComplainantAddress() : "N/A");
        
        tvNarrative.setText(report.getNarrative() != null ? 
            report.getNarrative() : "No narrative provided");
        
        // Display Respondent Information if available
        displayRespondentInformation();
        
        // Display Evidence (Images and Videos)
        displayEvidence();
        
        updateButtonVisibility();
        
        // NOTE: Timeline is NOT reinitialized here to avoid duplication
        // Timeline was already initialized in displayReportDetails()
    }
    
    private void displayRespondentInformation() {
        if (report == null) return;
        
        // Always show respondent section
        if (tvRespondentTitle != null) tvRespondentTitle.setVisibility(View.VISIBLE);
        if (cardRespondent != null) cardRespondent.setVisibility(View.VISIBLE);
        
        // Always show all respondent fields with N/A if empty
        if (layoutRespondentName != null) {
            layoutRespondentName.setVisibility(View.VISIBLE);
            if (tvRespondentName != null) {
                String name = report.getRespondentName();
                tvRespondentName.setText((name != null && !name.isEmpty()) ? name : "N/A");
            }
        }
        
        if (layoutRespondentAlias != null) {
            layoutRespondentAlias.setVisibility(View.VISIBLE);
            if (tvRespondentAlias != null) {
                String alias = report.getRespondentAlias();
                tvRespondentAlias.setText((alias != null && !alias.isEmpty()) ? alias : "N/A");
            }
        }
        
        if (layoutRespondentAddress != null) {
            layoutRespondentAddress.setVisibility(View.VISIBLE);
            if (tvRespondentAddress != null) {
                String address = report.getRespondentAddress();
                tvRespondentAddress.setText((address != null && !address.isEmpty()) ? address : "N/A");
            }
        }
        
        if (layoutRespondentContact != null) {
            layoutRespondentContact.setVisibility(View.VISIBLE);
            if (tvRespondentContact != null) {
                String contact = report.getRespondentContact();
                tvRespondentContact.setText((contact != null && !contact.isEmpty()) ? contact : "N/A");
            }
        }
        
        if (layoutAccusation != null) {
            layoutAccusation.setVisibility(View.VISIBLE);
            if (tvAccusation != null) {
                String accusation = report.getAccusation();
                tvAccusation.setText((accusation != null && !accusation.isEmpty()) ? accusation : "N/A");
            }
        }
        
        if (layoutRelationship != null) {
            layoutRelationship.setVisibility(View.VISIBLE);
            if (tvRelationship != null) {
                String relationship = report.getRelationshipToComplainant();
                tvRelationship.setText((relationship != null && !relationship.isEmpty()) ? relationship : "N/A");
            }
        }
    }
    
    private void displayEvidence() {
        if (report == null) return;
        
        // Clear lists to prevent duplicates
        imageList.clear();
        videoList.clear();
        
        // Display Images
        if (report.getImageUris() != null && !report.getImageUris().isEmpty()) {
            String[] imageUris = report.getImageUris().split(",");
            for (String uriString : imageUris) {
                if (!uriString.trim().isEmpty()) {
                    imageList.add(android.net.Uri.parse(uriString.trim()));
                }
            }
        }
        
        // Update image visibility and adapter
        if (!imageList.isEmpty()) {
            if (tvEvidenceTitle != null) tvEvidenceTitle.setVisibility(View.VISIBLE);
            if (tvImagesLabel != null) tvImagesLabel.setVisibility(View.VISIBLE);
            if (cardImages != null) cardImages.setVisibility(View.VISIBLE);
        } else {
            if (tvImagesLabel != null) tvImagesLabel.setVisibility(View.GONE);
            if (cardImages != null) cardImages.setVisibility(View.GONE);
        }
        // Always notify adapter of changes
        if (imageAdapter != null) {
            imageAdapter.notifyDataSetChanged();
        }
        
        // Display Videos
        if (report.getVideoUris() != null && !report.getVideoUris().isEmpty()) {
            String[] videoUris = report.getVideoUris().split(",");
            for (String uriString : videoUris) {
                if (!uriString.trim().isEmpty()) {
                    videoList.add(android.net.Uri.parse(uriString.trim()));
                }
            }
        }
        
        // Update video visibility and adapter
        if (!videoList.isEmpty()) {
            if (tvEvidenceTitle != null) tvEvidenceTitle.setVisibility(View.VISIBLE);
            if (tvVideosLabel != null) tvVideosLabel.setVisibility(View.VISIBLE);
            if (cardVideos != null) cardVideos.setVisibility(View.VISIBLE);
        } else {
            if (tvVideosLabel != null) tvVideosLabel.setVisibility(View.GONE);
            if (cardVideos != null) cardVideos.setVisibility(View.GONE);
        }
        // Always notify adapter of changes
        if (videoAdapter != null) {
            videoAdapter.notifyDataSetChanged();
        }
        
        // Hide evidence title if no images and no videos
        if (imageList.isEmpty() && videoList.isEmpty()) {
            if (tvEvidenceTitle != null) tvEvidenceTitle.setVisibility(View.GONE);
        }
    }
    
    private void updateButtonVisibility() {
        String userRole = preferencesManager.getUserRole();
        boolean isOfficer = "Officer".equalsIgnoreCase(userRole);
        boolean isAdmin = "Admin".equalsIgnoreCase(userRole);
        
        if (isOfficer) {
            if (btnEdit != null) btnEdit.setVisibility(View.GONE);
            if (btnDelete != null) btnDelete.setVisibility(View.GONE);
        } else if (isAdmin) {
            if (btnEdit != null) btnEdit.setVisibility(View.GONE);
            if (btnDelete != null) btnDelete.setVisibility(View.GONE);
        } else {
            String status = report != null && report.getStatus() != null 
                ? report.getStatus().toLowerCase() 
                : "";
            boolean isPending = "pending".equals(status);
            
            if (btnEdit != null) {
                btnEdit.setVisibility(isPending ? View.VISIBLE : View.GONE);
            }
            
            if (btnDelete != null) {
                btnDelete.setVisibility(isPending ? View.VISIBLE : View.GONE);
            }
        }
    }
    
    private void displayAssignedOfficers() {
        if (report == null) return;
        
        String assignedOfficer = report.getAssignedOfficer();
        
        // Show card only if officers are assigned
        if (assignedOfficer != null && !assignedOfficer.isEmpty()) {
            cardAssignedOfficers.setVisibility(View.VISIBLE);
            tvAssignedOfficers.setText(assignedOfficer);
        } else {
            cardAssignedOfficers.setVisibility(View.GONE);
        }
    }
    
    /**
     * Initialize the investigation timeline with 7 steps (READ-ONLY for Users)
     * MUST be called on background thread due to database access
     */
    private void initializeInvestigationTimeline() {
        // ✅ Prevent concurrent initialization (avoid duplicates)
        if (isTimelineInitializing) {
            android.util.Log.d("ReportDetail", "⚠️ Timeline initialization already in progress, skipping...");
            return;
        }
        
        isTimelineInitializing = true;
        
        try {
            investigationSteps.clear();
            
            // Step 1: Case Created (Always completed)
            InvestigationStep step1 = new InvestigationStep("1", "Case Created", "Initial report submitted", "case_created");
            step1.setCompleted(true);
            investigationSteps.add(step1);
            
            // Step 2: Case Assigned
            // ✅ Check if case has been assigned to an officer
            InvestigationStep step2 = new InvestigationStep("2", "Case Assigned", "Waiting for officer assignment", "case_assigned");
            boolean isCaseAssigned = report != null && 
                report.getAssignedOfficer() != null && 
                !report.getAssignedOfficer().trim().isEmpty();
            
            step2.setCompleted(false);
            if (isCaseAssigned) {
                // Case has been assigned - show as COMPLETED (checkmark)
                step2.setCompleted(true);
                step2.setInProgress(false);
                android.util.Log.d("ReportDetail", "✅ Case Assigned: COMPLETED (assigned to officer)");
            } else {
                // Case not assigned yet - show as IN PROGRESS (hourglass - waiting for assignment)
                step2.setInProgress(true);
                android.util.Log.d("ReportDetail", "⏳ Case Assigned: IN PROGRESS (waiting for assignment)");
            }
            investigationSteps.add(step2);
            
            // Step 3: Investigation Started
            // ✅ Check if investigation has started (case status is ONGOING, IN PROGRESS, or RESOLVED)
            InvestigationStep step3 = new InvestigationStep("3", "Investigation Started", "Officer begins investigation", "investigation_started");
            boolean isInvestigationStarted = report != null && 
                report.getStatus() != null && 
                (report.getStatus().equalsIgnoreCase("ONGOING") || 
                 report.getStatus().equalsIgnoreCase("IN PROGRESS") ||
                 report.getStatus().equalsIgnoreCase("RESOLVED"));
            
            step3.setCompleted(false);
            if (isInvestigationStarted) {
                // Investigation has started - show as COMPLETED (checkmark)
                step3.setCompleted(true);
                step3.setInProgress(false);
                android.util.Log.d("ReportDetail", "✅ Investigation Started: COMPLETED");
            } else if (isCaseAssigned) {
                // Case assigned but investigation not started - show as IN PROGRESS (hourglass - current active step)
                step3.setInProgress(true);
                android.util.Log.d("ReportDetail", "⏳ Investigation Started: IN PROGRESS (waiting to start)");
            } else {
                // Case not assigned yet - show as PENDING (red circle)
                step3.setInProgress(false);
                android.util.Log.d("ReportDetail", "⭕ Investigation Started: PENDING");
            }
            investigationSteps.add(step3);
            
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
                android.util.Log.d("ReportDetail", "✅ Step 4: COMPLETED (witness and suspect present)");
            } else if (witnessCount > 0 || suspectCount > 0) {
                // At least one collected - show as IN PROGRESS (hourglass - current active step)
                step4.setCompleted(false);
                step4.setInProgress(true);
                android.util.Log.d("ReportDetail", "⏳ Step 4: IN PROGRESS (collecting W:" + witnessCount + " S:" + suspectCount + ")");
            } else {
                // Neither collected - PENDING
                step4.setCompleted(false);
                step4.setInProgress(false);
                android.util.Log.d("ReportDetail", "⭕ Step 4: PENDING");
            }
            investigationSteps.add(step4);
            
            // Step 5: Hearing Scheduled
            // ✅ Show hourglass if hearing exists OR if all evidence collected (current active step)
            InvestigationStep step5 = new InvestigationStep("5", "Hearing Scheduled", "Court hearing date set", "hearing_scheduled");
            int hearingCount = database.hearingDao().getHearingCountByReport(reportId);
            
            if (hearingCount > 0) {
                // Hearing scheduled - COMPLETED (checkmark)
                step5.setCompleted(true);
                step5.setInProgress(false);
                android.util.Log.d("ReportDetail", "✅ Step 5: COMPLETED (hearing exists)");
            } else if (witnessCount > 0 && suspectCount > 0 && evidenceCount > 0) {
                // All evidence collected - show as IN PROGRESS (hourglass - current active step)
                step5.setCompleted(false);
                step5.setInProgress(true);
                android.util.Log.d("ReportDetail", "⏳ Step 5: IN PROGRESS (waiting to schedule hearing)");
            } else {
                // Evidence not all collected - PENDING
                step5.setCompleted(false);
                step5.setInProgress(false);
                android.util.Log.d("ReportDetail", "⭕ Step 5: PENDING");
            }
            investigationSteps.add(step5);
            
            // Step 6: Resolution Documented
            // ✅ Show hourglass if resolution exists OR if hearing scheduled (current active step)
            InvestigationStep step6 = new InvestigationStep("6", "Resolution Documented", "Case outcome documented", "resolution_documented");
            int resolutionCount = database.resolutionDao().getResolutionCountByReport(reportId);
            
            if (resolutionCount > 0) {
                // Resolution documented - COMPLETED (checkmark)
                step6.setCompleted(true);
                step6.setInProgress(false);
                android.util.Log.d("ReportDetail", "✅ Step 6: COMPLETED (resolution exists)");
            } else if (hearingCount > 0) {
                // Hearing scheduled - show as IN PROGRESS (hourglass - current active step)
                step6.setCompleted(false);
                step6.setInProgress(true);
                android.util.Log.d("ReportDetail", "⏳ Step 6: IN PROGRESS (waiting to document resolution)");
            } else {
                // Hearing not scheduled - PENDING
                step6.setCompleted(false);
                step6.setInProgress(false);
                android.util.Log.d("ReportDetail", "⭕ Step 6: PENDING");
            }
            investigationSteps.add(step6);
            
            // Step 7: Case Closed
            // ✅ Auto-complete if resolution exists, otherwise show hourglass if resolution being documented
            InvestigationStep step7 = new InvestigationStep("7", "Case Closed", "Case finalized", "case_closed");
            if (resolutionCount > 0) {
                // Resolution exists - case is closed - COMPLETED (checkmark)
                step7.setCompleted(true);
                step7.setInProgress(false);
                android.util.Log.d("ReportDetail", "✅ Step 7: COMPLETED (auto-complete due to resolution)");
            } else if (hearingCount > 0) {
                // Hearing scheduled - show as IN PROGRESS (hourglass - current active step)
                step7.setCompleted(false);
                step7.setInProgress(true);
                android.util.Log.d("ReportDetail", "⏳ Step 7: IN PROGRESS (waiting for resolution)");
            } else {
                // Hearing not scheduled - PENDING
                step7.setCompleted(false);
                step7.setInProgress(false);
                android.util.Log.d("ReportDetail", "⭕ Step 7: PENDING");
            }
            investigationSteps.add(step7);
            
            // Update UI on main thread
            runOnUiThread(() -> {
                if (stepAdapter != null) {
                    stepAdapter.updateSteps(investigationSteps);
                }
                android.util.Log.d("ReportDetail", "✅ Investigation timeline initialized with 7 steps");
                isTimelineInitializing = false;  // ✅ Reset flag after UI update
            });
        } catch (Exception e) {
            android.util.Log.e("ReportDetail", "❌ Error initializing timeline: " + e.getMessage());
            isTimelineInitializing = false;  // ✅ Reset flag on error
        }
    }
    
    // Dialog methods for viewing investigation results
    private void showWitnessesDialog(int reportId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                int witnessCount = database.witnessDao().getWitnessCountByReport(reportId);
                runOnUiThread(() -> {
                    androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
                    builder.setTitle("👥 Witnesses (" + witnessCount + ")");
                    builder.setMessage(witnessCount > 0 ? 
                        "Witnesses have been recorded for this case." : 
                        "No witnesses recorded yet.");
                    builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
                    builder.show();
                });
            } catch (Exception e) {
                android.util.Log.e("ReportDetail", "Error loading witnesses: " + e.getMessage());
            }
        });
    }
    
    private void showSuspectsDialog(int reportId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                int suspectCount = database.suspectDao().getSuspectCountByReport(reportId);
                runOnUiThread(() -> {
                    androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
                    builder.setTitle("🚨 Suspects (" + suspectCount + ")");
                    builder.setMessage(suspectCount > 0 ? 
                        "Suspects have been identified for this case." : 
                        "No suspects identified yet.");
                    builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
                    builder.show();
                });
            } catch (Exception e) {
                android.util.Log.e("ReportDetail", "Error loading suspects: " + e.getMessage());
            }
        });
    }
    
    private void showEvidenceDialog(int reportId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                int evidenceCount = database.evidenceDao().getEvidenceCountByReport(reportId);
                runOnUiThread(() -> {
                    androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
                    builder.setTitle("📸 Evidence (" + evidenceCount + ")");
                    builder.setMessage(evidenceCount > 0 ? 
                        "Evidence has been collected for this case." : 
                        "No evidence collected yet.");
                    builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
                    builder.show();
                });
            } catch (Exception e) {
                android.util.Log.e("ReportDetail", "Error loading evidence: " + e.getMessage());
            }
        });
    }
    
    private void showHearingsDialog(int reportId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                int hearingCount = database.hearingDao().getHearingCountByReport(reportId);
                runOnUiThread(() -> {
                    androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
                    builder.setTitle("📅 Hearings (" + hearingCount + ")");
                    builder.setMessage(hearingCount > 0 ? 
                        "Hearing(s) have been scheduled for this case." : 
                        "No hearings scheduled yet.");
                    builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
                    builder.show();
                });
            } catch (Exception e) {
                android.util.Log.e("ReportDetail", "Error loading hearings: " + e.getMessage());
            }
        });
    }
    
    private void showResolutionDialog(int reportId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                int resolutionCount = database.resolutionDao().getResolutionCountByReport(reportId);
                runOnUiThread(() -> {
                    androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
                    builder.setTitle("✅ Resolution (" + resolutionCount + ")");
                    builder.setMessage(resolutionCount > 0 ? 
                        "Case resolution has been documented." : 
                        "Case resolution not yet documented.");
                    builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
                    builder.show();
                });
            } catch (Exception e) {
                android.util.Log.e("ReportDetail", "Error loading resolution: " + e.getMessage());
            }
        });
    }
    
    /**
     * Set the status chip color based on the status value
     * Color coding: All statuses = Blue, except Resolved/Closed = Green
     */
    private void setStatusChipColor(String status) {
        if (chipStatus == null || status == null) return;
        
        int backgroundColor;
        // All statuses are blue except Resolved/Closed which are green
        if ("resolved".equalsIgnoreCase(status) || "closed".equalsIgnoreCase(status)) {
            // 🟢 Resolved - Green
            backgroundColor = getColor(R.color.success_green);
        } else {
            // 🔵 All other statuses - Electric Blue
            backgroundColor = getColor(R.color.electric_blue);
        }
        
        chipStatus.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(backgroundColor));
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (reportId != -1) {
            loadReportDetails();
            // Timeline will be initialized in displayReportDetails() via loadReportDetails()
            // No need to call refreshInvestigationTimeline() here to avoid duplication
        }
    }
    
    /**
     * Export comprehensive PDF with data from all roles
     * - User's initial input (complainant, respondent, case details)
     * - Officer's investigation data (witnesses, suspects, evidence)
     * - Admin's resolution data (hearing, resolution)
     */
    private void exportReportToPdf() {
        if (report == null) {
            Toast.makeText(this, "Report data not loaded", Toast.LENGTH_SHORT).show();
            return;
        }
        
        GlobalLoadingManager.show(this, "Generating PDF...");
        
        // Get user role for PDF generation
        String userRole = preferencesManager.getUserRole();
        
        com.example.blottermanagementsystem.utils.ComprehensivePdfGenerator.generateComprehensivePdf(
            this, reportId, userRole,
            new com.example.blottermanagementsystem.utils.ComprehensivePdfGenerator.PdfGenerationCallback() {
                @Override
                public void onSuccess(String filePath) {
                    runOnUiThread(() -> {
                        GlobalLoadingManager.hide();
                        // Show preview dialog
                        showPdfPreviewDialog(filePath, userRole);
                    });
                }
                
                @Override
                public void onError(String errorMessage) {
                    runOnUiThread(() -> {
                        GlobalLoadingManager.hide();
                        Toast.makeText(ReportDetailActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        );
    }
    
    /**
     * Show PDF preview dialog with Save and Share buttons
     */
    private void showPdfPreviewDialog(String filePath, String userRole) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        
        // Inflate custom layout
        android.view.LayoutInflater inflater = getLayoutInflater();
        android.view.View dialogView = inflater.inflate(R.layout.dialog_pdf_preview, null);
        builder.setView(dialogView);
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        
        // Reload report data from database for dynamic updates
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Fetch fresh data from database
                BlotterReport freshReport = database.blotterReportDao().getReportById(reportId);
                
                runOnUiThread(() -> {
                    // Update preview text with actual data
                    android.widget.TextView tvCaseNumber = dialogView.findViewById(R.id.tvPreviewCaseNumber);
                    android.widget.TextView tvIncidentType = dialogView.findViewById(R.id.tvPreviewIncidentType);
                    android.widget.TextView tvIncidentDate = dialogView.findViewById(R.id.tvPreviewIncidentDate);
                    android.widget.TextView tvIncidentTime = dialogView.findViewById(R.id.tvPreviewIncidentTime);
                    android.widget.TextView tvIncidentLocation = dialogView.findViewById(R.id.tvPreviewIncidentLocation);
                    android.widget.TextView tvNarrative = dialogView.findViewById(R.id.tvPreviewNarrative);
                    android.widget.TextView tvComplainantName = dialogView.findViewById(R.id.tvPreviewComplainantName);
                    android.widget.TextView tvComplainantContact = dialogView.findViewById(R.id.tvPreviewComplainantContact);
                    android.widget.TextView tvComplainantAddress = dialogView.findViewById(R.id.tvPreviewComplainantAddress);
                    android.widget.TextView tvRespondentName = dialogView.findViewById(R.id.tvPreviewRespondentName);
                    android.widget.TextView tvRespondentAlias = dialogView.findViewById(R.id.tvPreviewRespondentAlias);
                    android.widget.TextView tvRespondentAddress = dialogView.findViewById(R.id.tvPreviewRespondentAddress);
                    android.widget.TextView tvRespondentContact = dialogView.findViewById(R.id.tvPreviewRespondentContact);
                    android.widget.TextView tvAccusation = dialogView.findViewById(R.id.tvPreviewAccusation);
                    android.widget.TextView tvRelationship = dialogView.findViewById(R.id.tvPreviewRelationship);
                    
                    // Set preview data with fresh case information
                    if (freshReport != null) {
                        tvCaseNumber.setText("Case Number: " + (freshReport.getCaseNumber() != null ? freshReport.getCaseNumber() : "N/A"));
                        
                        // Status
                        android.widget.TextView tvStatus = dialogView.findViewById(R.id.tvPreviewStatus);
                        if (tvStatus != null) {
                            tvStatus.setText(freshReport.getStatus() != null ? freshReport.getStatus() : "Pending");
                        }
                        
                        // Date Filed
                        android.widget.TextView tvDateFiled = dialogView.findViewById(R.id.tvPreviewDateFiled);
                        if (tvDateFiled != null) {
                            String dateFiledStr = "N/A";
                            if (freshReport.getIncidentDate() > 0) {
                                dateFiledStr = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(new java.util.Date(freshReport.getIncidentDate()));
                            }
                            tvDateFiled.setText(dateFiledStr);
                        }
                        
                        // Incident Details
                        tvIncidentType.setText("Type: " + (freshReport.getIncidentType() != null ? freshReport.getIncidentType() : "N/A"));
                        
                        String dateTimeStr = "N/A";
                        String timeStr = "N/A";
                        if (freshReport.getIncidentDate() > 0) {
                            dateTimeStr = new java.text.SimpleDateFormat("MMM dd, yyyy hh:mm a", java.util.Locale.getDefault()).format(new java.util.Date(freshReport.getIncidentDate()));
                            timeStr = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(new java.util.Date(freshReport.getIncidentDate()));
                        }
                        tvIncidentDate.setText("Date: " + dateTimeStr);
                        tvIncidentTime.setText("Time: " + timeStr);
                        tvIncidentLocation.setText("Location: " + (freshReport.getIncidentLocation() != null ? freshReport.getIncidentLocation() : "N/A"));
                        
                        // Narrative
                        tvNarrative.setText(freshReport.getNarrative() != null ? freshReport.getNarrative() : "N/A");
                        
                        // Show complainant info
                        tvComplainantName.setText("Name: " + (freshReport.getComplainantName() != null ? freshReport.getComplainantName() : "N/A"));
                        tvComplainantContact.setText("Contact: " + (freshReport.getComplainantContact() != null ? freshReport.getComplainantContact() : "N/A"));
                        tvComplainantAddress.setText("Address: " + (freshReport.getComplainantAddress() != null ? freshReport.getComplainantAddress() : "N/A"));
                        
                        // Show respondent info (the person being reported against)
                        tvRespondentName.setText("Name: " + (freshReport.getRespondentName() != null ? freshReport.getRespondentName() : "N/A"));
                        tvRespondentAlias.setText("Alias: " + (freshReport.getRespondentAlias() != null ? freshReport.getRespondentAlias() : "N/A"));
                        tvRespondentAddress.setText("Address: " + (freshReport.getRespondentAddress() != null ? freshReport.getRespondentAddress() : "N/A"));
                        tvRespondentContact.setText("Contact: " + (freshReport.getRespondentContact() != null ? freshReport.getRespondentContact() : "N/A"));
                        tvAccusation.setText("Accusation: " + (freshReport.getAccusation() != null ? freshReport.getAccusation() : "N/A"));
                        tvRelationship.setText("Relationship: " + (freshReport.getRelationshipToComplainant() != null ? freshReport.getRelationshipToComplainant() : "N/A"));
                    }
                    
                    // Setup evidence thumbnails - separate images and videos
                    // Show evidence for all roles in preview
                    androidx.recyclerview.widget.RecyclerView rvImagesPreview = dialogView.findViewById(R.id.rvImagesPreview);
                    androidx.recyclerview.widget.RecyclerView rvVideosPreview = dialogView.findViewById(R.id.rvVideosPreview);
                    android.widget.LinearLayout layoutImagesSection = dialogView.findViewById(R.id.layoutImagesSection);
                    android.widget.LinearLayout layoutVideosSection = dialogView.findViewById(R.id.layoutVideosSection);
                    android.widget.TextView tvNoEvidence = dialogView.findViewById(R.id.tvNoEvidence);
                    
                    if (freshReport != null) {
                        List<Evidence> imageList = new java.util.ArrayList<>();
                        List<Evidence> videoList = new java.util.ArrayList<>();
                        
                        // Get images from report's image URIs
                        if (freshReport.getImageUris() != null && !freshReport.getImageUris().isEmpty()) {
                            android.util.Log.d("PdfPreview", "Raw ImageUris: " + freshReport.getImageUris());
                            String[] imageUris = freshReport.getImageUris().split(",");
                            for (String uriString : imageUris) {
                                if (!uriString.trim().isEmpty()) {
                                    Evidence evidence = new Evidence();
                                    evidence.setEvidenceType("Image");
                                    evidence.setPhotoUris(uriString.trim());
                                    imageList.add(evidence);
                                    android.util.Log.d("PdfPreview", "Added image URI: " + uriString.trim());
                                }
                            }
                        }
                        
                        // Get videos from report's video URIs
                        if (freshReport.getVideoUris() != null && !freshReport.getVideoUris().isEmpty()) {
                            android.util.Log.d("PdfPreview", "Raw VideoUris: " + freshReport.getVideoUris());
                            String[] videoUris = freshReport.getVideoUris().split(",");
                            for (String uriString : videoUris) {
                                if (!uriString.trim().isEmpty()) {
                                    Evidence evidence = new Evidence();
                                    evidence.setEvidenceType("Video");
                                    evidence.setVideoUris(uriString.trim());
                                    videoList.add(evidence);
                                    android.util.Log.d("PdfPreview", "Added video URI: " + uriString.trim());
                                }
                            }
                        }
                        
                        // Setup images section
                        if (!imageList.isEmpty() && rvImagesPreview != null) {
                            androidx.recyclerview.widget.LinearLayoutManager layoutManager = 
                                new androidx.recyclerview.widget.LinearLayoutManager(ReportDetailActivity.this, 
                                    androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false);
                            rvImagesPreview.setLayoutManager(layoutManager);
                            
                            com.example.blottermanagementsystem.ui.adapters.EvidenceThumbnailAdapter adapter = 
                                new com.example.blottermanagementsystem.ui.adapters.EvidenceThumbnailAdapter(
                                    ReportDetailActivity.this, imageList);
                            rvImagesPreview.setAdapter(adapter);
                            if (layoutImagesSection != null) {
                                layoutImagesSection.setVisibility(android.view.View.VISIBLE);
                            }
                            android.util.Log.d("PdfPreview", "Images loaded: " + imageList.size());
                        }
                        
                        // Setup videos section
                        if (!videoList.isEmpty() && rvVideosPreview != null) {
                            androidx.recyclerview.widget.LinearLayoutManager layoutManager = 
                                new androidx.recyclerview.widget.LinearLayoutManager(ReportDetailActivity.this, 
                                    androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false);
                            rvVideosPreview.setLayoutManager(layoutManager);
                            
                            com.example.blottermanagementsystem.ui.adapters.EvidenceThumbnailAdapter adapter = 
                                new com.example.blottermanagementsystem.ui.adapters.EvidenceThumbnailAdapter(
                                    ReportDetailActivity.this, videoList);
                            rvVideosPreview.setAdapter(adapter);
                            if (layoutVideosSection != null) {
                                layoutVideosSection.setVisibility(android.view.View.VISIBLE);
                            }
                            android.util.Log.d("PdfPreview", "Videos loaded: " + videoList.size());
                        }
                        
                        // Show/hide "No evidence" message
                        if (imageList.isEmpty() && videoList.isEmpty()) {
                            if (tvNoEvidence != null) {
                                tvNoEvidence.setVisibility(android.view.View.VISIBLE);
                            }
                            android.util.Log.d("PdfPreview", "No evidence found");
                        } else {
                            if (tvNoEvidence != null) {
                                tvNoEvidence.setVisibility(android.view.View.GONE);
                            }
                        }
                        
                        // Show dialog AFTER all data is loaded
                        dialog.show();
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("PdfPreview", "Error loading report: " + e.getMessage());
                // Show dialog even if there's an error
                runOnUiThread(() -> dialog.show());
            }
        });
        
        // Save button
        com.google.android.material.button.MaterialButton btnSave = dialogView.findViewById(R.id.btnSavePdf);
        btnSave.setOnClickListener(v -> {
            dialog.dismiss();
            Toast.makeText(this, "PDF saved to Downloads/BlotterReports/", Toast.LENGTH_LONG).show();
        });
        
        // Share button - show share options as overlay without closing preview
        com.google.android.material.button.MaterialButton btnShare = dialogView.findViewById(R.id.btnSharePdf);
        btnShare.setOnClickListener(v -> {
            showPdfShareDialog(filePath);
        });
        
        // Close button
        android.widget.ImageButton btnClose = dialogView.findViewById(R.id.btnClosePreview);
        btnClose.setOnClickListener(v -> dialog.dismiss());
        
        // NOTE: dialog.show() is called inside the data loading runOnUiThread block
        // to ensure data is loaded before showing the dialog
    }
    
    /**
     * Show share dialog to share PDF via Messenger, Bluetooth, Email, Print, etc.
     */
    private void showPdfShareDialog(String filePath) {
        File pdfFile = new File(filePath);
        if (!pdfFile.exists()) {
            Toast.makeText(this, "PDF file not found", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get URI using FileProvider
        Uri pdfUri = FileProvider.getUriForFile(
            this,
            getPackageName() + ".provider",
            pdfFile
        );
        
        // Create share intent
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Blotter Report: " + report.getCaseNumber());
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Please find attached the comprehensive blotter report.");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        
        // Show chooser dialog with all available share options
        Intent chooser = Intent.createChooser(shareIntent, "Share PDF Report via:");
        startActivity(chooser);
    }
    
}
