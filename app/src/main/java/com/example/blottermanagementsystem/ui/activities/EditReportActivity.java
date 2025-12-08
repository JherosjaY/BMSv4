package com.example.blottermanagementsystem.ui.activities;

import android.Manifest;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.database.BlotterDatabase;
import com.example.blottermanagementsystem.data.entity.BlotterReport;
import com.example.blottermanagementsystem.ui.adapters.ImageAdapter;
import com.example.blottermanagementsystem.ui.adapters.VideoAdapter;
import com.example.blottermanagementsystem.utils.MediaManager;
import com.example.blottermanagementsystem.utils.NotificationHelper;
import com.example.blottermanagementsystem.utils.PermissionHelper;
import com.example.blottermanagementsystem.utils.PhoneNumberValidator;
import com.example.blottermanagementsystem.utils.PreferencesManager;
import com.example.blottermanagementsystem.data.api.ApiClient;
import com.example.blottermanagementsystem.utils.NetworkMonitor;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import androidx.cardview.widget.CardView;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class EditReportActivity extends BaseActivity {
    
    private BlotterDatabase database;
    private BlotterReport report;
    private int reportId;
    private PreferencesManager preferencesManager;
    private NotificationHelper notificationHelper;
    
    // UI Components
    private Toolbar toolbar;
    private TextInputEditText etIncidentDate, etIncidentTime;
    private TextInputEditText etComplainantName, etComplainantAddress;
    private TextInputEditText etComplainantContact, etNarrative, etIncidentLocation;
    private TextInputEditText etRespondentName, etRespondentAlias, etRespondentAddress, etRespondentContact;
    private AutoCompleteTextView actvIncidentType, actvRelationship, etAccusation;
    private TextView tvCaseNumber;
    private TextView tvImagesLabel, tvVideosLabel;
    private Button btnSave;
    private CardView btnTakePhoto, btnChooseImages, btnChooseVideos;
    private CardView emptyStateImages, emptyStateVideos;
    
    // RecyclerViews
    private RecyclerView recyclerImages, recyclerVideos;
    
    // Adapters
    private ImageAdapter imageAdapter;
    private VideoAdapter videoAdapter;
    
    // Data
    private List<Uri> imageList = new ArrayList<>();
    private List<Uri> videoList = new ArrayList<>();
    
    // Utilities
    private MediaManager mediaManager;
    private Uri currentPhotoUri;
    private boolean isPickingImages = true; // Track if picking images or videos
    
    // Activity Result Launchers
    private final ActivityResultLauncher<Uri> cameraLauncher = 
        registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success && currentPhotoUri != null) {
                imageList.add(0, currentPhotoUri);
                imageAdapter.notifyItemInserted(0);
                recyclerImages.scrollToPosition(0);
                updateImageView();
                Toast.makeText(this, "Photo captured!", Toast.LENGTH_SHORT).show();
            }
        });
    
    private final ActivityResultLauncher<Intent> mediaPickerLauncher = 
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Intent data = result.getData();
                
                if (data.getClipData() != null) {
                    ClipData clipData = data.getClipData();
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        Uri uri = clipData.getItemAt(i).getUri();
                        
                        // Grant persistent URI permission for gallery items
                        try {
                            getContentResolver().takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            );
                        } catch (Exception e) {
                            // Ignore - camera photos don't support this
                        }
                        
                        if (isVideoUri(uri)) {
                            videoList.add(0, uri);
                        } else {
                            imageList.add(0, uri);
                        }
                    }
                } else if (data.getData() != null) {
                    Uri uri = data.getData();
                    
                    // Grant persistent URI permission for gallery items
                    try {
                        getContentResolver().takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );
                    } catch (Exception e) {
                        // Ignore - camera photos don't support this
                    }
                    
                    if (isVideoUri(uri)) {
                        videoList.add(uri);
                    } else {
                        imageList.add(uri);
                    }
                }
                
                imageAdapter.notifyDataSetChanged();
                videoAdapter.notifyDataSetChanged();
                updateImageView();
                updateVideoView();
            }
        });
    
    private final ActivityResultLauncher<String> cameraPermissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
            }
        });
    
    private final ActivityResultLauncher<String> storagePermissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                if (isPickingImages) {
                    openImagePicker();
                } else {
                    openVideoPicker();
                }
            } else {
                Toast.makeText(this, "Storage permission required", Toast.LENGTH_SHORT).show();
            }
        });
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_report);
        
        database = BlotterDatabase.getDatabase(this);
        preferencesManager = new PreferencesManager(this);
        notificationHelper = new NotificationHelper(this);
        mediaManager = new MediaManager();
        reportId = getIntent().getIntExtra("REPORT_ID", -1);
        
        if (reportId == -1) {
            Toast.makeText(this, "Invalid report", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        initViews();
        setupToolbar();
        setupRecyclerViews();
        setupMediaListeners();
        setupEditableDropdowns();
        loadReportData();
    }
    
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvCaseNumber = findViewById(R.id.tvCaseNumber);
        etIncidentDate = findViewById(R.id.etIncidentDate);
        etIncidentTime = findViewById(R.id.etIncidentTime);
        etComplainantName = findViewById(R.id.etComplainantName);
        etComplainantContact = findViewById(R.id.etContactNumber);
        etComplainantAddress = findViewById(R.id.etAddress);
        actvIncidentType = findViewById(R.id.actvIncidentType);
        etIncidentLocation = findViewById(R.id.etIncidentLocation);
        etNarrative = findViewById(R.id.etDescription);
        etRespondentName = findViewById(R.id.etRespondentName);
        etRespondentAlias = findViewById(R.id.etRespondentAlias);
        etRespondentAddress = findViewById(R.id.etRespondentAddress);
        etRespondentContact = findViewById(R.id.etRespondentContact);
        etAccusation = findViewById(R.id.etAccusation);
        actvRelationship = findViewById(R.id.actvRelationship);
        
        // Evidence
        recyclerImages = findViewById(R.id.recyclerImages);
        recyclerVideos = findViewById(R.id.recyclerVideos);
        tvImagesLabel = findViewById(R.id.tvImagesLabel);
        tvVideosLabel = findViewById(R.id.tvVideosLabel);
        emptyStateImages = findViewById(R.id.emptyStateImages);
        emptyStateVideos = findViewById(R.id.emptyStateVideos);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnChooseImages = findViewById(R.id.btnChooseImages);
        btnChooseVideos = findViewById(R.id.btnChooseVideos);
        
        btnSave = findViewById(R.id.btnSubmitBlotterReport);
        btnSave.setText("Update Report");
    }
    
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Edit Report");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }
    
    private void setupRecyclerViews() {
        // Images
        imageAdapter = new ImageAdapter(imageList, new ImageAdapter.OnImageClickListener() {
            @Override
            public void onImageClick(Uri uri) {
                viewImage(uri);
            }
            
            @Override
            public void onImageDelete(int position) {
                // Validate position before removing
                if (position >= 0 && position < imageList.size()) {
                    imageList.remove(position);
                    imageAdapter.notifyItemRemoved(position);
                    updateImageView();
                } else {
                    Toast.makeText(EditReportActivity.this, "Error: Invalid image position", Toast.LENGTH_SHORT).show();
                }
            }
        });
        LinearLayoutManager imageLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerImages.setLayoutManager(imageLayoutManager);
        recyclerImages.setAdapter(imageAdapter);
        
        // Videos RecyclerView
        videoAdapter = new VideoAdapter(videoList, new VideoAdapter.OnVideoClickListener() {
            @Override
            public void onVideoClick(Uri videoUri) {
                playVideo(videoUri);
            }
            
            @Override
            public void onVideoDelete(int position) {
                // Check if position is valid before removing
                if (position >= 0 && position < videoList.size()) {
                    videoList.remove(position);
                    videoAdapter.notifyItemRemoved(position);
                    updateVideoView();
                } else {
                    Toast.makeText(EditReportActivity.this, "Error: Invalid video position", Toast.LENGTH_SHORT).show();
                }
            }
        });
        LinearLayoutManager videoLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerVideos.setLayoutManager(videoLayoutManager);
        recyclerVideos.setAdapter(videoAdapter);
    }
    
    private void setupMediaListeners() {
        btnTakePhoto.setOnClickListener(v -> {
            if (PermissionHelper.hasCameraPermission(this)) {
                openCamera();
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });
        
        btnChooseImages.setOnClickListener(v -> {
            isPickingImages = true;
            if (PermissionHelper.hasStoragePermission(this)) {
                openImagePicker();
            } else {
                storagePermissionLauncher.launch(PermissionHelper.getStoragePermission());
            }
        });
        
        btnChooseVideos.setOnClickListener(v -> {
            isPickingImages = false;
            if (PermissionHelper.hasStoragePermission(this)) {
                openVideoPicker();
            } else {
                storagePermissionLauncher.launch(PermissionHelper.getStoragePermission());
            }
        });
        
        btnSave.setOnClickListener(v -> saveChanges());
    }
    
    /**
     * Setup editable dropdowns for Incident Type, Accusation, and Relationship
     * Uses the same mapping system as AddReportActivity
     */
    private void setupEditableDropdowns() {
        try {
            android.util.Log.d("EditReport", "🔍 setupEditableDropdowns started");
            
            // Get all incident types
            String[] incidentTypes = getIncidentTypes();
            android.util.Log.d("EditReport", "✅ Got " + incidentTypes.length + " incident types");
            
            // Incident Type dropdown
            ArrayAdapter<String> incidentAdapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_dropdown_item_1line, incidentTypes);
            actvIncidentType.setAdapter(incidentAdapter);
            actvIncidentType.setThreshold(0);
            android.util.Log.d("EditReport", "✅ Incident Type adapter set");
            
            // Show dropdown on focus
            actvIncidentType.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    android.util.Log.d("EditReport", "🔍 Incident Type focused - showing dropdown");
                    actvIncidentType.showDropDown();
                }
            });
        
        // Accusation dropdown - populate when clicked or focused
        etAccusation.setOnClickListener(v -> {
            String selectedIncidentType = actvIncidentType.getText().toString().trim();
            if (!selectedIncidentType.isEmpty()) {
                populateAccusationDropdown(selectedIncidentType);
            } else {
                Toast.makeText(this, "Please select an incident type first", Toast.LENGTH_SHORT).show();
            }
        });
        etAccusation.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                String selectedIncidentType = actvIncidentType.getText().toString().trim();
                if (!selectedIncidentType.isEmpty()) {
                    populateAccusationDropdown(selectedIncidentType);
                } else {
                    Toast.makeText(this, "Please select an incident type first", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
            // Relationship dropdown
            String[] relationships = {
                "Co-worker", "Supervisor", "Subordinate", "Family Member", "Friend",
                "Acquaintance", "Stranger", "Business Associate", "Neighbor", "Other"
            };
            ArrayAdapter<String> relationshipAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, relationships);
            actvRelationship.setAdapter(relationshipAdapter);
            actvRelationship.setThreshold(0);
            android.util.Log.d("EditReport", "✅ Relationship adapter set");
            
            // Show dropdown on focus
            actvRelationship.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    android.util.Log.d("EditReport", "🔍 Relationship focused - showing dropdown");
                    actvRelationship.showDropDown();
                }
            });
            
            android.util.Log.d("EditReport", "✅✅✅ setupEditableDropdowns completed successfully");
        } catch (Exception e) {
            android.util.Log.e("EditReport", "❌ Error in setupEditableDropdowns: " + e.getMessage(), e);
            Toast.makeText(this, "Error setting up dropdowns: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Get all available incident types
     */
    private String[] getIncidentTypes() {
        java.util.Set<String> incidentSet = getIncidentToAccusationsMapping().keySet();
        String[] incidents = incidentSet.toArray(new String[0]);
        java.util.Arrays.sort(incidents);
        return incidents;
    }
    
    /**
     * Populate Accusation dropdown based on selected Incident Type
     * This is called when user clicks on the Accusation field
     */
    private void populateAccusationDropdown(String incidentType) {
        java.util.Map<String, String[]> mapping = getIncidentToAccusationsMapping();
        String[] accusations = mapping.getOrDefault(incidentType, new String[]{incidentType});
        
        // Create adapter for accusation dropdown
        ArrayAdapter<String> accusationAdapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            accusations
        );
        
        // Set adapter and configure dropdown
        etAccusation.setAdapter(accusationAdapter);
        etAccusation.setThreshold(0);
        etAccusation.showDropDown();
    }
    
    /**
     * Mapping table: Incident Type → List of Accusations
     * Maps incident types to multiple related legal accusations for user selection
     */
    private java.util.Map<String, String[]> getIncidentToAccusationsMapping() {
        java.util.Map<String, String[]> mapping = new java.util.HashMap<>();
        
        // Violent Crimes
        mapping.put("Assault", new String[]{"Physical Assault", "Simple Assault", "Aggravated Assault"});
        mapping.put("Domestic Violence", new String[]{"Domestic Violence", "Domestic Abuse", "Intimate Partner Violence"});
        mapping.put("Homicide", new String[]{"Homicide", "Murder", "Manslaughter"});
        mapping.put("Sexual Assault", new String[]{"Sexual Assault", "Rape", "Statutory Rape", "Sexual Battery"});
        mapping.put("Kidnapping", new String[]{"Kidnapping", "Abduction", "False Imprisonment"});
        mapping.put("Child Abuse", new String[]{"Child Abuse", "Child Neglect", "Child Endangerment"});
        mapping.put("Rape", new String[]{"Rape", "Sexual Assault", "Statutory Rape"});
        mapping.put("Aggravated Assault", new String[]{"Aggravated Assault", "Assault with Deadly Weapon", "Assault with Intent to Injure"});
        mapping.put("Murder", new String[]{"Murder", "First Degree Murder", "Second Degree Murder"});
        mapping.put("Manslaughter", new String[]{"Manslaughter", "Voluntary Manslaughter", "Involuntary Manslaughter"});
        mapping.put("Attempted Murder", new String[]{"Attempted Murder", "Assault with Intent to Kill"});
        mapping.put("Robbery with Violence", new String[]{"Armed Robbery", "Robbery with Force", "Aggravated Robbery"});
        mapping.put("Mugging", new String[]{"Mugging", "Street Robbery", "Robbery"});
        mapping.put("Carjacking", new String[]{"Carjacking", "Motor Vehicle Theft", "Armed Carjacking"});
        mapping.put("Human Trafficking", new String[]{"Human Trafficking", "Sex Trafficking", "Labor Trafficking"});
        mapping.put("Extortion", new String[]{"Extortion", "Blackmail", "Coercion"});
        mapping.put("Threatening", new String[]{"Threatening", "Criminal Threats", "Threatening with Violence", "Intimidation", "Menacing"});
        
        // Property Crimes
        mapping.put("Theft", new String[]{"Larceny", "Theft", "Grand Theft", "Petty Theft"});
        mapping.put("Burglary", new String[]{"Burglary", "Breaking and Entering", "Residential Burglary", "Commercial Burglary"});
        mapping.put("Robbery", new String[]{"Robbery", "Armed Robbery", "Strongarm Robbery"});
        mapping.put("Vandalism", new String[]{"Vandalism", "Criminal Mischief", "Property Destruction"});
        mapping.put("Property Damage", new String[]{"Property Damage", "Destruction of Property", "Malicious Mischief"});
        mapping.put("Arson", new String[]{"Arson", "Arson in First Degree", "Arson in Second Degree"});
        mapping.put("Trespassing", new String[]{"Trespassing", "Unlawful Entry", "Criminal Trespass"});
        mapping.put("Shoplifting", new String[]{"Shoplifting", "Retail Theft", "Larceny from Store"});
        mapping.put("Grand Larceny", new String[]{"Grand Larceny", "Grand Theft", "Felony Theft"});
        mapping.put("Petty Larceny", new String[]{"Petty Larceny", "Petty Theft", "Misdemeanor Theft"});
        mapping.put("Auto Theft", new String[]{"Auto Theft", "Vehicle Theft", "Motor Vehicle Theft"});
        mapping.put("Bike Theft", new String[]{"Bike Theft", "Bicycle Theft", "Larceny of Bicycle"});
        mapping.put("Breaking & Entering", new String[]{"Breaking and Entering", "Burglary", "Unlawful Entry"});
        mapping.put("Looting", new String[]{"Looting", "Theft During Emergency", "Burglary During Disaster"});
        mapping.put("Pickpocketing", new String[]{"Pickpocketing", "Theft from Person", "Larceny from Person"});
        mapping.put("Forgery", new String[]{"Forgery", "Document Falsification", "Forgery of Documents"});
        mapping.put("Counterfeiting", new String[]{"Counterfeiting", "Counterfeiting Currency", "Forgery of Currency"});
        
        // Cyber Crimes
        mapping.put("Cybercrime", new String[]{"Cybercrime", "Computer Fraud", "Unauthorized Computer Access"});
        mapping.put("Scam/Phishing", new String[]{"Phishing Scam", "Email Scam", "Online Fraud", "Phishing Attack"});
        mapping.put("Identity Theft", new String[]{"Identity Theft", "Identity Fraud", "Unauthorized Use of Identity"});
        mapping.put("Fraud", new String[]{"Fraud", "Wire Fraud", "Mail Fraud", "Internet Fraud"});
        mapping.put("Hacking", new String[]{"Unauthorized Computer Access", "Hacking", "Computer Intrusion"});
        mapping.put("Malware Distribution", new String[]{"Malware Distribution", "Computer Virus Distribution", "Malicious Software"});
        mapping.put("Data Breach", new String[]{"Data Breach", "Unauthorized Data Access", "Data Theft"});
        mapping.put("Online Harassment", new String[]{"Cyberstalking", "Online Harassment", "Cyber Harassment"});
        mapping.put("Catfishing", new String[]{"Catfishing", "Online Impersonation", "Fraud by Impersonation"});
        mapping.put("Ransomware", new String[]{"Ransomware Attack", "Extortion via Ransomware", "Computer Extortion"});
        mapping.put("Credit Card Fraud", new String[]{"Credit Card Fraud", "Unauthorized Card Use", "Card Fraud"});
        mapping.put("Money Laundering", new String[]{"Money Laundering", "Financial Crime", "Illegal Money Transfer"});
        mapping.put("Unauthorized Access", new String[]{"Unauthorized Computer Access", "Hacking", "System Intrusion"});
        
        // Public Order
        mapping.put("Noise Complaint", new String[]{"Noise Disturbance", "Excessive Noise", "Noise Violation"});
        mapping.put("Public Disturbance", new String[]{"Disorderly Conduct", "Public Disturbance", "Breach of Peace"});
        mapping.put("Harassment", new String[]{"Harassment", "Workplace Harassment", "Sexual Harassment"});
        mapping.put("Stalking", new String[]{"Stalking", "Criminal Stalking", "Harassment by Stalking"});
        mapping.put("Illegal Gambling", new String[]{"Illegal Gambling", "Unlicensed Gambling", "Gambling Violation"});
        mapping.put("Littering", new String[]{"Littering", "Illegal Dumping of Trash", "Environmental Violation"});
        mapping.put("Illegal Dumping", new String[]{"Illegal Dumping", "Improper Waste Disposal", "Environmental Crime"});
        mapping.put("Loitering", new String[]{"Loitering", "Loitering with Intent", "Suspicious Loitering"});
        
        return mapping;
    }
    
    private void loadReportData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            report = database.blotterReportDao().getReportById(reportId);
            
            runOnUiThread(() -> {
                if (report != null) {
                    populateFields();
                    loadExistingMedia();
                } else {
                    Toast.makeText(this, "Report not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        });
    }
    
    private void populateFields() {
        tvCaseNumber.setText(report.getCaseNumber());
        
        // Convert timestamp to date string
        if (report.getIncidentDate() > 0) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            String dateString = dateFormat.format(new Date(report.getIncidentDate()));
            etIncidentDate.setText(dateString);
        }
        if (report.getIncidentTime() != null) {
            etIncidentTime.setText(report.getIncidentTime());
        }
        if (report.getComplainantName() != null) {
            etComplainantName.setText(report.getComplainantName());
        }
        if (report.getComplainantContact() != null) {
            etComplainantContact.setText(report.getComplainantContact());
        }
        if (report.getComplainantAddress() != null) {
            etComplainantAddress.setText(report.getComplainantAddress());
        }
        if (report.getIncidentType() != null) {
            actvIncidentType.setText(report.getIncidentType());
        }
        if (report.getIncidentLocation() != null) {
            etIncidentLocation.setText(report.getIncidentLocation());
        }
        if (report.getNarrative() != null) {
            etNarrative.setText(report.getNarrative());
        }
        if (report.getRespondentName() != null) {
            etRespondentName.setText(report.getRespondentName());
        }
        if (report.getRespondentAlias() != null) {
            etRespondentAlias.setText(report.getRespondentAlias());
        }
        if (report.getRespondentAddress() != null) {
            etRespondentAddress.setText(report.getRespondentAddress());
        }
        if (report.getRespondentContact() != null) {
            etRespondentContact.setText(report.getRespondentContact());
        }
        if (report.getAccusation() != null) {
            etAccusation.setText(report.getAccusation());
        }
        if (report.getRelationshipToComplainant() != null) {
            actvRelationship.setText(report.getRelationshipToComplainant());
        }
    }
    
    private void loadExistingMedia() {
        // Load existing images
        if (report.getImageUris() != null && !report.getImageUris().isEmpty()) {
            String[] uris = report.getImageUris().split(",");
            for (String uriString : uris) {
                Uri uri = Uri.parse(uriString);
                
                // Try to grant persistent permission for existing URIs
                try {
                    getContentResolver().takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );
                } catch (Exception e) {
                    // Ignore - may not be supported
                }
                
                imageList.add(uri);
            }
            imageAdapter.notifyDataSetChanged();
            updateImageView();
        }
        
        // Load existing videos
        if (report.getVideoUris() != null && !report.getVideoUris().isEmpty()) {
            String[] uris = report.getVideoUris().split(",");
            for (String uriString : uris) {
                Uri uri = Uri.parse(uriString);
                
                // Try to grant persistent permission for existing URIs
                try {
                    getContentResolver().takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );
                } catch (Exception e) {
                    // Ignore - may not be supported
                }
                
                videoList.add(uri);
            }
            videoAdapter.notifyDataSetChanged();
            updateVideoView();
        }
        
    }
    
    private void openCamera() {
        try {
            File photoFile = new File(getExternalFilesDir(null), 
                "photo_" + System.currentTimeMillis() + ".jpg");
            currentPhotoUri = FileProvider.getUriForFile(this, 
                getPackageName() + ".provider", photoFile);
            cameraLauncher.launch(currentPhotoUri);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to open camera", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
    
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        mediaPickerLauncher.launch(intent);
    }
    
    private void openVideoPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        mediaPickerLauncher.launch(intent);
    }
    
    private boolean isVideoUri(Uri uri) {
        String mimeType = getContentResolver().getType(uri);
        return mimeType != null && mimeType.startsWith("video/");
    }
    
    private void viewImage(Uri uri) {
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
            android.util.Log.e("EditReportActivity", "Error showing image: " + e.getMessage());
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
    
    private void updateImageView() {
        if (imageList.isEmpty()) {
            recyclerImages.setVisibility(View.GONE);
            emptyStateImages.setVisibility(View.VISIBLE);
        } else {
            recyclerImages.setVisibility(View.VISIBLE);
            emptyStateImages.setVisibility(View.GONE);
        }
    }
    
    private void updateVideoView() {
        if (videoList.isEmpty()) {
            recyclerVideos.setVisibility(View.GONE);
            emptyStateVideos.setVisibility(View.VISIBLE);
        } else {
            recyclerVideos.setVisibility(View.VISIBLE);
            emptyStateVideos.setVisibility(View.GONE);
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
    
    private void saveChanges() {
        // ===== STRICT FIELD VALIDATION - REQUIRED FIELDS ONLY =====
        
        // 1. Complainant Name (REQUIRED)
        String complainantName = etComplainantName.getText().toString().trim();
        if (complainantName.isEmpty()) {
            etComplainantName.setError("Complainant name is required");
            etComplainantName.requestFocus();
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 2. Incident Type (REQUIRED)
        String incidentType = actvIncidentType.getText().toString().trim();
        if (incidentType.isEmpty()) {
            actvIncidentType.setError("Incident type is required");
            actvIncidentType.requestFocus();
            Toast.makeText(this, "Please select or enter an incident type", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 3. Accusation (REQUIRED)
        String accusation = etAccusation.getText().toString().trim();
        if (accusation.isEmpty()) {
            etAccusation.setError("Accusation is required");
            etAccusation.requestFocus();
            Toast.makeText(this, "Please select or enter an accusation", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 4. Relationship (REQUIRED)
        String relationship = actvRelationship.getText().toString().trim();
        if (relationship.isEmpty()) {
            actvRelationship.setError("Relationship is required");
            actvRelationship.requestFocus();
            Toast.makeText(this, "Please select or enter a relationship", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // ===== OPTIONAL FIELD VALIDATION =====
        String complainantContact = etComplainantContact.getText().toString().trim();
        
        // Validate complainant phone number (OPTIONAL but if provided, must be valid)
        if (!complainantContact.isEmpty() && !PhoneNumberValidator.isValidPhilippineNumber(complainantContact)) {
            etComplainantContact.setError(PhoneNumberValidator.getErrorMessage(complainantContact));
            etComplainantContact.requestFocus();
            Toast.makeText(this, "Invalid complainant contact number. " + 
                PhoneNumberValidator.getSupportedNetworks(), Toast.LENGTH_LONG).show();
            return;
        }
        
        // Update report fields with validated data
        // Note: Incident date/time are read-only in edit mode (set during creation)
        report.setComplainantName(complainantName);
        report.setComplainantContact(complainantContact);
        report.setComplainantAddress(etComplainantAddress.getText().toString().trim());
        report.setIncidentType(incidentType);
        report.setIncidentLocation(etIncidentLocation.getText().toString().trim());
        report.setNarrative(etNarrative.getText().toString().trim());
        
        String respondentName = etRespondentName.getText().toString().trim();
        String respondentAlias = etRespondentAlias.getText().toString().trim();
        String respondentAddress = etRespondentAddress.getText().toString().trim();
        String respondentContact = etRespondentContact.getText().toString().trim();
        
        // Validate respondent phone number if provided
        if (!respondentContact.isEmpty() && !PhoneNumberValidator.isValidPhilippineNumber(respondentContact)) {
            etRespondentContact.setError(PhoneNumberValidator.getErrorMessage(respondentContact));
            etRespondentContact.requestFocus();
            Toast.makeText(this, "Invalid respondent contact number. " + 
                PhoneNumberValidator.getSupportedNetworks(), Toast.LENGTH_LONG).show();
            return;
        }
        
        // Update ALL respondent fields (even if empty - allows clearing values)
        report.setRespondentName(respondentName);
        report.setRespondentAlias(respondentAlias);
        report.setRespondentAddress(respondentAddress);
        report.setRespondentContact(respondentContact);
        report.setAccusation(accusation);
        report.setRelationshipToComplainant(relationship);
        
        // Update media
        if (!imageList.isEmpty()) {
            StringBuilder uris = new StringBuilder();
            for (int i = 0; i < imageList.size(); i++) {
                uris.append(imageList.get(i).toString());
                if (i < imageList.size() - 1) uris.append(",");
            }
            report.setImageUris(uris.toString());
        } else {
            report.setImageUris("");
        }
        
        if (!videoList.isEmpty()) {
            StringBuilder uris = new StringBuilder();
            for (int i = 0; i < videoList.size(); i++) {
                uris.append(videoList.get(i).toString());
                if (i < videoList.size() - 1) uris.append(",");
            }
            report.setVideoUris(uris.toString());
        } else {
            report.setVideoUris("");
        }
        
        // Show loading for report update
        com.example.blottermanagementsystem.utils.GlobalLoadingManager.show(this, "Updating report...");
        
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Save to local database first
                database.blotterReportDao().updateReport(report);
                
                // Check if online and sync to API
                NetworkMonitor networkMonitor = new NetworkMonitor(EditReportActivity.this);
                if (networkMonitor.isNetworkAvailable()) {
                    // Sync to API
                    ApiClient.updateReport(reportId, report, new ApiClient.ApiCallback<BlotterReport>() {
                        @Override
                        public void onSuccess(BlotterReport result) {
                            android.util.Log.d("EditReport", "✅ Report synced to API: " + result.getId());
                            // Update local database with API response
                            database.blotterReportDao().updateReport(result);
                        }
                        
                        @Override
                        public void onError(String errorMessage) {
                            android.util.Log.w("EditReport", "⚠️ API sync failed: " + errorMessage);
                        }
                    });
                } else {
                    android.util.Log.i("EditReport", "Offline mode: Report updated locally, will sync when online");
                }
                
                runOnUiThread(() -> {
                    com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
                    
                    // Pass result back to ReportDetailActivity to trigger refresh
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("REPORT_UPDATED", true);
                    resultIntent.putExtra("REPORT_ID", reportId);
                    setResult(RESULT_OK, resultIntent);
                    
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
                    Toast.makeText(this, "Error updating report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaManager != null) {
            mediaManager.release();
        }
    }
}
