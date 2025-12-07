package com.example.blottermanagementsystem.ui.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.database.BlotterDatabase;
import com.example.blottermanagementsystem.data.entity.BlotterReport;
import com.example.blottermanagementsystem.data.entity.Witness;
import com.example.blottermanagementsystem.data.entity.Suspect;
import com.example.blottermanagementsystem.ui.adapters.ImageAdapter;
import com.example.blottermanagementsystem.ui.adapters.VideoAdapter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class ViewPersonHistoryDetailActivity extends AppCompatActivity {

    private int reportId;
    private BlotterDatabase database;
    private BlotterReport currentReport;
    
    // UI Components
    private ImageButton btnBack;
    private TextView tvCaseNumber, tvIncidentType, tvIncidentDate, tvIncidentTime, tvIncidentLocation;
    private TextView tvComplainantName, tvComplainantContact, tvComplainantAddress;
    private TextView tvRespondentName, tvRespondentAlias, tvRespondentAddress, tvRespondentContact;
    private TextView tvAccusation, tvRelationship, tvNarrative;
    private RecyclerView recyclerImages, recyclerVideos;
    private RecyclerView recyclerWitnesses, recyclerSuspects;
    
    private ImageAdapter imageAdapter;
    private VideoAdapter videoAdapter;
    private List<Uri> imageList = new ArrayList<>();
    private List<Uri> videoList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_person_history_detail);

        // ✅ Set status bar color to primary dark
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.background_primary));
        }
        
        // ✅ Set navigation bar color to primary dark
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(getResources().getColor(R.color.background_primary));
        }

        database = BlotterDatabase.getDatabase(this);
        
        // Get report ID from intent
        Intent intent = getIntent();
        reportId = intent.getIntExtra("reportId", -1);
        
        if (reportId == -1) {
            Toast.makeText(this, "Invalid case ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        initViews();
        setupListeners();
        loadCaseDetails();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        
        // Incident Details
        tvCaseNumber = findViewById(R.id.tvCaseNumber);
        tvIncidentType = findViewById(R.id.tvIncidentType);
        tvIncidentDate = findViewById(R.id.tvIncidentDate);
        tvIncidentTime = findViewById(R.id.tvIncidentTime);
        tvIncidentLocation = findViewById(R.id.tvIncidentLocation);
        tvNarrative = findViewById(R.id.tvNarrative);
        
        // Complainant Details
        tvComplainantName = findViewById(R.id.tvComplainantName);
        tvComplainantContact = findViewById(R.id.tvComplainantContact);
        tvComplainantAddress = findViewById(R.id.tvComplainantAddress);
        
        // Respondent Details
        tvRespondentName = findViewById(R.id.tvRespondentName);
        tvRespondentAlias = findViewById(R.id.tvRespondentAlias);
        tvRespondentAddress = findViewById(R.id.tvRespondentAddress);
        tvRespondentContact = findViewById(R.id.tvRespondentContact);
        tvAccusation = findViewById(R.id.tvAccusation);
        tvRelationship = findViewById(R.id.tvRelationship);
        
        // Evidence RecyclerViews
        recyclerImages = findViewById(R.id.recyclerImages);
        recyclerVideos = findViewById(R.id.recyclerVideos);
        recyclerWitnesses = findViewById(R.id.recyclerWitnesses);
        recyclerSuspects = findViewById(R.id.recyclerSuspects);
        
        // Setup image adapter
        imageAdapter = new ImageAdapter(imageList, new ImageAdapter.OnImageClickListener() {
            @Override
            public void onImageClick(Uri uri) {
                viewImage(uri);
            }
            
            @Override
            public void onImageDelete(int position) {
                // No delete for view-only
            }
        });
        recyclerImages.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerImages.setAdapter(imageAdapter);
        
        // Setup video adapter
        videoAdapter = new VideoAdapter(videoList, new VideoAdapter.OnVideoClickListener() {
            @Override
            public void onVideoClick(Uri videoUri) {
                playVideo(videoUri);
            }
            
            @Override
            public void onVideoDelete(int position) {
                // No delete for view-only
            }
        });
        recyclerVideos.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerVideos.setAdapter(videoAdapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadCaseDetails() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                currentReport = database.blotterReportDao().getReportById(reportId);
                
                if (currentReport != null) {
                    runOnUiThread(() -> {
                        // ✅ CASE NUMBER & INCIDENT DETAILS
                        tvCaseNumber.setText(currentReport.getCaseNumber());
                        tvIncidentType.setText(currentReport.getIncidentType());
                        
                        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                        
                        tvIncidentDate.setText(dateFormat.format(new Date(currentReport.getIncidentDate())));
                        tvIncidentTime.setText(currentReport.getIncidentTime() != null ? 
                            currentReport.getIncidentTime() : "Not specified");
                        tvIncidentLocation.setText(currentReport.getLocation() != null ? 
                            currentReport.getLocation() : "Not specified");
                        tvNarrative.setText(currentReport.getNarrative() != null ? 
                            currentReport.getNarrative() : "No description");
                        
                        // ✅ COMPLAINANT DETAILS
                        tvComplainantName.setText(currentReport.getComplainantName() != null ? 
                            currentReport.getComplainantName() : "Unknown");
                        tvComplainantContact.setText(currentReport.getComplainantContact() != null ? 
                            currentReport.getComplainantContact() : "N/A");
                        tvComplainantAddress.setText(currentReport.getComplainantAddress() != null ? 
                            currentReport.getComplainantAddress() : "Not specified");
                        
                        // ✅ RESPONDENT DETAILS
                        tvRespondentName.setText(currentReport.getRespondentName() != null ? 
                            currentReport.getRespondentName() : "Unknown");
                        tvRespondentAlias.setText(currentReport.getRespondentAlias() != null ? 
                            currentReport.getRespondentAlias() : "N/A");
                        tvRespondentAddress.setText(currentReport.getRespondentAddress() != null ? 
                            currentReport.getRespondentAddress() : "Not specified");
                        tvRespondentContact.setText(currentReport.getRespondentContact() != null ? 
                            currentReport.getRespondentContact() : "N/A");
                        tvAccusation.setText(currentReport.getAccusation() != null ? 
                            currentReport.getAccusation() : "Not specified");
                        tvRelationship.setText(currentReport.getRelationshipToComplainant() != null ? 
                            currentReport.getRelationshipToComplainant() : "Not specified");
                    });
                    
                    // ✅ LOAD EVIDENCE (Images & Videos)
                    loadEvidence();
                    
                    // ✅ LOAD WITNESSES & SUSPECTS
                    loadWitnessesAndSuspects();
                }
            } catch (Exception e) {
                android.util.Log.e("ViewPersonHistoryDetail", "Error loading case: " + e.getMessage());
            }
        });
    }

    private void loadEvidence() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<com.example.blottermanagementsystem.data.entity.Evidence> evidenceList = 
                    database.evidenceDao().getEvidenceByReport(reportId);
                
                imageList.clear();
                videoList.clear();
                
                if (evidenceList != null && !evidenceList.isEmpty()) {
                    for (com.example.blottermanagementsystem.data.entity.Evidence evidence : evidenceList) {
                        // Use getPhotoUris() and getVideoUris() to get evidence URIs
                        String photoUris = evidence.getPhotoUris();
                        String videoUris = evidence.getVideoUris();
                        
                        if (photoUris != null && !photoUris.isEmpty()) {
                            // Photo URIs can be comma-separated, split and add each
                            String[] photos = photoUris.split(",");
                            for (String photoUri : photos) {
                                if (!photoUri.trim().isEmpty()) {
                                    imageList.add(Uri.parse(photoUri.trim()));
                                }
                            }
                        }
                        if (videoUris != null && !videoUris.isEmpty()) {
                            // Video URIs can be comma-separated, split and add each
                            String[] videos = videoUris.split(",");
                            for (String videoUri : videos) {
                                if (!videoUri.trim().isEmpty()) {
                                    videoList.add(Uri.parse(videoUri.trim()));
                                }
                            }
                        }
                    }
                }
                
                runOnUiThread(() -> {
                    imageAdapter.notifyDataSetChanged();
                    videoAdapter.notifyDataSetChanged();
                    
                    // Hide/show sections based on content
                    findViewById(R.id.imagesSection).setVisibility(imageList.isEmpty() ? View.GONE : View.VISIBLE);
                    findViewById(R.id.videosSection).setVisibility(videoList.isEmpty() ? View.GONE : View.VISIBLE);
                });
            } catch (Exception e) {
                android.util.Log.e("ViewPersonHistoryDetail", "Error loading evidence: " + e.getMessage());
            }
        });
    }

    private void loadWitnessesAndSuspects() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<Witness> witnesses = database.witnessDao().getWitnessesByReport(reportId);
                List<Suspect> suspects = database.suspectDao().getSuspectsByReport(reportId);
                
                runOnUiThread(() -> {
                    // Hide witness/suspect sections for now (no layout files)
                    findViewById(R.id.witnessesSection).setVisibility(View.GONE);
                    findViewById(R.id.suspectsSection).setVisibility(View.GONE);
                });
            } catch (Exception e) {
                android.util.Log.e("ViewPersonHistoryDetail", "Error loading witnesses/suspects: " + e.getMessage());
            }
        });
    }

    private void viewImage(Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "image/*");
        startActivity(intent);
    }

    private void playVideo(Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "video/*");
        startActivity(intent);
    }
}
