package com.example.blottermanagementsystem.ui.activities;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
import androidx.cardview.widget.CardView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;

public class AddReportActivity extends BaseActivity {
    
    // UI Components
    private TextInputEditText etIncidentDate, etIncidentTime, etComplainantName, etComplainantAddress, 
                              etComplainantContact, etNarrative, etIncidentLocation;
    private TextInputEditText etRespondentName, etRespondentAlias, etRespondentAddress, etRespondentContact;
    private AutoCompleteTextView actvIncidentType, actvRelationship, etAccusation;
    private TextView tvCaseNumber;
    private TextView tvImagesLabel, tvVideosLabel;
    private Button btnSubmit;
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
    private BlotterDatabase database;
    private PreferencesManager preferencesManager;
    private MediaManager mediaManager;
    private Calendar selectedDate = Calendar.getInstance();
    private Calendar selectedTime = Calendar.getInstance();
    private Uri currentPhotoUri;
    private boolean isPickingImages = true; // Track if picking images or videos
    
    // Activity Result Launchers
    private final ActivityResultLauncher<Uri> cameraLauncher = 
        registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            android.util.Log.d("AddReportActivity", "Camera result: success=" + success);
            if (success && currentPhotoUri != null) {
                android.util.Log.d("AddReportActivity", "✅ Photo captured: " + currentPhotoUri);
                
                // Check image limit (max 5 images)
                if (imageList.size() < 5) {
                    imageList.add(0, currentPhotoUri);
                    imageAdapter.notifyItemInserted(0);
                    recyclerImages.scrollToPosition(0);
                    updateImageView();
                } else {
                    Toast.makeText(this, "Maximum 5 images reached. Please delete some images first.", Toast.LENGTH_LONG).show();
                }
            } else {
                android.util.Log.e("AddReportActivity", "❌ Photo capture failed or cancelled");
                if (!success) {
                    Toast.makeText(this, "Photo capture cancelled", Toast.LENGTH_SHORT).show();
                }
            }
        });
    
    private final ActivityResultLauncher<Intent> mediaPickerLauncher = 
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Intent data = result.getData();
                int imagesAdded = 0;
                int videosAdded = 0;
                int imagesSkipped = 0;
                int videosSkipped = 0;
                
                if (data.getClipData() != null) {
                    // Multiple selection
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
                            // Check video limit (max 5 videos) and duration (max 2 minutes)
                            if (videoList.size() < 5) {
                                long duration = mediaManager.getVideoDuration(this, uri);
                                if (duration <= 120000) { // 2 minutes = 120,000 ms
                                    videoList.add(0, uri);
                                    videosAdded++;
                                } else {
                                    videosSkipped++;
                                    Toast.makeText(this, "Video too long (max 2 minutes)", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                videosSkipped++;
                            }
                        } else {
                            // Check image limit (max 5 images)
                            if (imageList.size() < 5) {
                                imageList.add(0, uri);
                                imagesAdded++;
                            } else {
                                imagesSkipped++;
                            }
                        }
                    }
                } else if (data.getData() != null) {
                    // Single selection
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
                        // Check video limit (max 5 videos) and duration (max 2 minutes)
                        if (videoList.size() < 5) {
                            long duration = mediaManager.getVideoDuration(this, uri);
                            if (duration <= 120000) { // 2 minutes = 120,000 ms
                                videoList.add(0, uri);
                                videosAdded++;
                            } else {
                                videosSkipped++;
                                Toast.makeText(this, "Video too long (max 2 minutes)", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            videosSkipped++;
                        }
                    } else {
                        // Check image limit (max 5 images)
                        if (imageList.size() < 5) {
                            imageList.add(0, uri);
                            imagesAdded++;
                        } else {
                            imagesSkipped++;
                        }
                    }
                }
                
                // Show feedback
                String message = "";
                if (imagesAdded > 0) message += imagesAdded + " image(s) added";
                if (videosAdded > 0) {
                    if (!message.isEmpty()) message += ", ";
                    message += videosAdded + " video(s) added";
                }
                if (imagesSkipped > 0) {
                    if (!message.isEmpty()) message += ". ";
                    message += imagesSkipped + " image(s) skipped (max 5)";
                }
                if (videosSkipped > 0) {
                    if (!message.isEmpty()) message += ". ";
                    message += videosSkipped + " video(s) skipped (max 5, 2min each)";
                }
                
                if (!message.isEmpty()) {
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
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
        android.util.Log.d("AddReportActivity", "🚀 onCreate started");
        
        try {
            setContentView(R.layout.activity_add_report);
            android.util.Log.d("AddReportActivity", "✅ Layout set");
            
            database = BlotterDatabase.getDatabase(this);
            preferencesManager = new PreferencesManager(this);
            mediaManager = new MediaManager();
            android.util.Log.d("AddReportActivity", "✅ Database and managers initialized");
            
            setupToolbar();
            android.util.Log.d("AddReportActivity", "✅ Toolbar setup complete");
            
            initViews();
            android.util.Log.d("AddReportActivity", "✅ Views initialized");
            
            setupRecyclerViews();
            android.util.Log.d("AddReportActivity", "✅ RecyclerViews setup complete");
            
            setupListeners();
            android.util.Log.d("AddReportActivity", "✅ Listeners setup complete");
            
            // Initialize views visibility
            updateImageView();
            updateVideoView();
            android.util.Log.d("AddReportActivity", "✅ Initial view states set");
            
            android.util.Log.d("AddReportActivity", "✅✅✅ onCreate completed successfully");
        } catch (Exception e) {
            android.util.Log.e("AddReportActivity", "❌ ERROR in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading Add Report screen: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }
    
    private void initViews() {
        try {
            android.util.Log.d("AddReportActivity", "🔍 initViews started");
            
            // Case info
            android.util.Log.d("AddReportActivity", "Finding tvCaseNumber...");
            tvCaseNumber = findViewById(R.id.tvCaseNumber);
            android.util.Log.d("AddReportActivity", "✅ tvCaseNumber found: " + (tvCaseNumber != null));
            
            // Complainant info
            android.util.Log.d("AddReportActivity", "Finding complainant fields...");
            etComplainantName = findViewById(R.id.etComplainantName);
            etComplainantContact = findViewById(R.id.etContactNumber);
            etComplainantAddress = findViewById(R.id.etAddress);
            android.util.Log.d("AddReportActivity", "✅ Complainant fields found");
            
            // Incident info
            android.util.Log.d("AddReportActivity", "Finding incident fields...");
            actvIncidentType = findViewById(R.id.actvIncidentType);
            etIncidentDate = findViewById(R.id.etIncidentDate);
            etIncidentTime = findViewById(R.id.etIncidentTime);
            etIncidentLocation = findViewById(R.id.etIncidentLocation);
            etNarrative = findViewById(R.id.etDescription);
            android.util.Log.d("AddReportActivity", "✅ Incident fields found");
            
            // Respondent info
            android.util.Log.d("AddReportActivity", "Finding respondent fields...");
            etRespondentName = findViewById(R.id.etRespondentName);
            etRespondentAlias = findViewById(R.id.etRespondentAlias);
            etRespondentAddress = findViewById(R.id.etRespondentAddress);
            etRespondentContact = findViewById(R.id.etRespondentContact);
            etAccusation = findViewById(R.id.etAccusation);
            actvRelationship = findViewById(R.id.actvRelationship);
            android.util.Log.d("AddReportActivity", "✅ Respondent fields found");
            
            // Evidence
            android.util.Log.d("AddReportActivity", "Finding evidence views...");
            recyclerImages = findViewById(R.id.recyclerImages);
            recyclerVideos = findViewById(R.id.recyclerVideos);
            tvImagesLabel = findViewById(R.id.tvImagesLabel);
            tvVideosLabel = findViewById(R.id.tvVideosLabel);
            emptyStateImages = findViewById(R.id.emptyStateImages);
            emptyStateVideos = findViewById(R.id.emptyStateVideos);
            btnTakePhoto = findViewById(R.id.btnTakePhoto);
            btnChooseImages = findViewById(R.id.btnChooseImages);
            btnChooseVideos = findViewById(R.id.btnChooseVideos);
            android.util.Log.d("AddReportActivity", "✅ Evidence views found");
            
            // Submit
            android.util.Log.d("AddReportActivity", "Finding submit button...");
            btnSubmit = findViewById(R.id.btnSubmitBlotterReport);
            android.util.Log.d("AddReportActivity", "✅ Submit button found: " + (btnSubmit != null));
            
            // Generate case number
            android.util.Log.d("AddReportActivity", "Generating case number...");
            generateCaseNumber();
            android.util.Log.d("AddReportActivity", "✅ Case number generated");
            
            // Setup incident types
            android.util.Log.d("AddReportActivity", "Setting up incident types...");
            setupIncidentTypes();
            android.util.Log.d("AddReportActivity", "✅ Incident types setup");
            
            // Setup relationship dropdown
            android.util.Log.d("AddReportActivity", "Setting up relationship dropdown...");
            setupRelationshipDropdown();
            android.util.Log.d("AddReportActivity", "✅ Relationship dropdown setup");
            
            android.util.Log.d("AddReportActivity", "✅✅✅ initViews completed successfully");
        } catch (Exception e) {
            android.util.Log.e("AddReportActivity", "❌ ERROR in initViews: " + e.getMessage(), e);
            throw e;
        }
    }
    
    private void setupRecyclerViews() {
        // Images RecyclerView
        imageAdapter = new ImageAdapter(imageList, new ImageAdapter.OnImageClickListener() {
            @Override
            public void onImageClick(Uri uri) {
                viewImage(uri);
            }
            
            @Override
            public void onImageDelete(int position) {
                imageList.remove(position);
                imageAdapter.notifyItemRemoved(position);
                updateImageView();
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
                videoList.remove(position);
                videoAdapter.notifyItemRemoved(position);
                updateVideoView();
            }
        });
        LinearLayoutManager videoLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerVideos.setLayoutManager(videoLayoutManager);
        recyclerVideos.setAdapter(videoAdapter);
    }
    
    private void setupListeners() {
        // Accusation field - populate dropdown based on selected incident type
        etAccusation.setOnClickListener(v -> {
            String selectedIncidentType = actvIncidentType.getText().toString().trim();
            if (!selectedIncidentType.isEmpty()) {
                populateAccusationDropdown(selectedIncidentType);
            }
        });
        
        // Date picker
        etIncidentDate.setOnClickListener(v -> showDatePicker());
        
        // Time picker
        etIncidentTime.setOnClickListener(v -> showTimePicker());
        
        // Camera
        btnTakePhoto.setOnClickListener(v -> {
            if (PermissionHelper.hasCameraPermission(this)) {
                openCamera();
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });
        
        // Image picker
        btnChooseImages.setOnClickListener(v -> {
            isPickingImages = true;
            if (PermissionHelper.hasStoragePermission(this)) {
                openImagePicker();
            } else {
                storagePermissionLauncher.launch(PermissionHelper.getStoragePermission());
            }
        });
        
        // Video picker
        btnChooseVideos.setOnClickListener(v -> {
            isPickingImages = false;
            if (PermissionHelper.hasStoragePermission(this)) {
                openVideoPicker();
            } else {
                storagePermissionLauncher.launch(PermissionHelper.getStoragePermission());
            }
        });
        
        // Submit
        btnSubmit.setOnClickListener(v -> submitReport());
    }
    
    private void generateCaseNumber() {
        Random random = new Random();
        long number = 1000000000L + (long)(random.nextDouble() * 9000000000L);
        String caseNumber = "BLT-" + number;
        tvCaseNumber.setText(caseNumber);
    }
    
    /**
     * Populate Accusation dropdown based on selected Incident Type
     * This is called when user clicks on the Accusation field
     */
    private void populateAccusationDropdown(String incidentType) {
        Map<String, String[]> mapping = getIncidentToAccusationsMapping();
        String[] accusations = mapping.getOrDefault(incidentType, new String[]{incidentType});
        
        // Create adapter for accusation dropdown
        ArrayAdapter<String> accusationAdapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            accusations
        );
        
        // Set adapter and configure dropdown
        etAccusation.setAdapter(accusationAdapter);
        etAccusation.setThreshold(0); // Show dropdown immediately
        
        // DON'T auto-fill text - let user click to see dropdown
        // Just show the dropdown with all options
        etAccusation.showDropDown();
        
        android.util.Log.d("AddReportActivity", "✅ Populated Accusation dropdown for: " + incidentType + " with " + accusations.length + " option(s)");
    }
    
    /**
     * Mapping table: Incident Type → List of Accusations
     * Maps incident types to multiple related legal accusations for user selection
     */
    private Map<String, String[]> getIncidentToAccusationsMapping() {
        Map<String, String[]> mapping = new HashMap<>();
        
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
        mapping.put("Disorderly Conduct", new String[]{"Disorderly Conduct", "Disruptive Behavior", "Breach of Peace"});
        mapping.put("Indecent Exposure", new String[]{"Indecent Exposure", "Indecent Act", "Lewdness"});
        mapping.put("Vagrancy", new String[]{"Vagrancy", "Vagrancy Violation", "Loitering"});
        mapping.put("Public Intoxication", new String[]{"Public Intoxication", "Drunk and Disorderly", "Intoxication in Public"});
        mapping.put("Prostitution", new String[]{"Prostitution", "Solicitation", "Engaging in Prostitution"});
        mapping.put("Unlicensed Vending", new String[]{"Unlicensed Vending", "Illegal Street Vending", "Vending Without Permit"});
        
        // Traffic & Vehicle
        mapping.put("Traffic Accident", new String[]{"Traffic Accident", "Motor Vehicle Accident", "Traffic Collision"});
        mapping.put("Illegal Parking", new String[]{"Illegal Parking", "Parking Violation", "Improper Parking"});
        mapping.put("Speeding", new String[]{"Speeding", "Exceeding Speed Limit", "Speed Violation"});
        mapping.put("Reckless Driving", new String[]{"Reckless Driving", "Dangerous Driving", "Careless Driving"});
        mapping.put("DUI/DWI", new String[]{"Driving Under Influence", "DUI", "DWI", "Drunk Driving"});
        mapping.put("Hit and Run", new String[]{"Hit and Run", "Leaving Scene of Accident", "Failure to Report Accident"});
        mapping.put("Expired Registration", new String[]{"Expired Registration", "Registration Violation", "Unregistered Vehicle"});
        mapping.put("Broken Headlight", new String[]{"Equipment Violation", "Defective Equipment", "Safety Equipment Violation"});
        mapping.put("Expired License", new String[]{"Expired License", "Driving with Expired License", "License Violation"});
        mapping.put("Improper Lane Change", new String[]{"Improper Lane Change", "Unsafe Lane Change", "Traffic Violation"});
        mapping.put("Running Red Light", new String[]{"Traffic Violation", "Running Red Light", "Signal Violation"});
        mapping.put("Unregistered Vehicle", new String[]{"Unregistered Vehicle", "Unregistered Motor Vehicle", "Registration Violation"});
        mapping.put("No Insurance", new String[]{"No Insurance", "Driving Without Insurance", "Insurance Violation"});
        mapping.put("Unsafe Lane Change", new String[]{"Unsafe Lane Change", "Improper Lane Change", "Traffic Violation"});
        
        // Other Incidents
        mapping.put("Missing Person", new String[]{"Missing Person Report", "Missing Adult", "Missing Child"});
        mapping.put("Drug-related", new String[]{"Drug Possession", "Drug Distribution", "Drug Manufacturing"});
        mapping.put("Animal Cruelty", new String[]{"Animal Cruelty", "Animal Abuse", "Animal Neglect"});
        mapping.put("Lost Property", new String[]{"Lost Property", "Property Report", "Lost Item Report"});
        mapping.put("Found Property", new String[]{"Found Property", "Property Report", "Found Item Report"});
        mapping.put("Suspicious Activity", new String[]{"Suspicious Activity", "Suspicious Behavior", "Suspicious Incident"});
        mapping.put("Welfare Check", new String[]{"Welfare Check", "Welfare Concern", "Safety Check"});
        mapping.put("Environmental Violation", new String[]{"Environmental Violation", "Environmental Crime", "Pollution Violation"});
        mapping.put("Permit Violation", new String[]{"Permit Violation", "License Violation", "Regulatory Violation"});
        mapping.put("Unauthorized Entry", new String[]{"Unauthorized Entry", "Unlawful Entry", "Trespass"});
        
        return mapping;
    }
    
    private void setupIncidentTypes() {
        String[] incidentTypes = {
            "Theft", 
            "Assault", 
            "Vandalism", 
            "Domestic Violence",
            "Noise Complaint", 
            "Trespassing", 
            "Fraud", 
            "Harassment",
            "Property Damage", 
            "Missing Person", 
            "Traffic Accident",
            "Drug-related", 
            "Burglary", 
            "Robbery",
            "Cybercrime",
            "Scam/Phishing",
            "Child Abuse",
            "Animal Cruelty",
            "Public Disturbance",
            "Illegal Gambling",
            "Illegal Parking",
            "Littering",
            "Arson",
            "Kidnapping",
            "Homicide",
            "Sexual Assault",
            "Stalking",
            "Identity Theft",
            "Extortion",
            "Illegal Dumping",
            "Other"
        };
        
        // Use custom dialog with modern theme and icons
        actvIncidentType.setOnClickListener(v -> showIncidentTypeDialog(incidentTypes));
        actvIncidentType.setKeyListener(null); // Disable keyboard
    }
    
    private void showIncidentTypeDialog(String[] incidentTypes) {
        // Show category selection first
        String[] categories = {
            "🔴 Violent Crimes",
            "🟠 Property Crimes", 
            "🟡 Cyber Crimes",
            "🟢 Public Order",
            "🔵 Traffic & Vehicle",
            "🟣 Other Incidents"
        };
        
        // Create custom centered title for category selection
        android.widget.TextView categoryTitle = new android.widget.TextView(this);
        categoryTitle.setText("Select Category");
        categoryTitle.setTextSize(20);
        categoryTitle.setTextColor(android.graphics.Color.WHITE);
        categoryTitle.setGravity(android.view.Gravity.CENTER);
        categoryTitle.setPadding(16, 16, 16, 16);
        categoryTitle.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        
        // Modern dark dialog with border
        androidx.appcompat.app.AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setCustomTitle(categoryTitle)
            .setItems(categories, (dialogInterface, which) -> {
                showIncidentTypesForCategory(which);
            })
            .setNegativeButton("Cancel", null)
            .create();
        
        // Apply custom background with border
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
        }
        
        dialog.show();
    }
    
    private void showIncidentTypesForCategory(int categoryIndex) {
        String categoryTitle;
        String[] types;
        
        switch (categoryIndex) {
            case 0: // Violent Crimes
                categoryTitle = "Violent Crimes";
                types = new String[]{
                    "Assault", "Domestic Violence", "Homicide", "Sexual Assault",
                    "Kidnapping", "Child Abuse", "Rape", "Aggravated Assault",
                    "Murder", "Manslaughter", "Attempted Murder", "Robbery with Violence",
                    "Mugging", "Carjacking", "Human Trafficking", "Extortion", "Threatening"
                };
                break;
            case 1: // Property Crimes
                categoryTitle = "Property Crimes";
                types = new String[]{
                    "Theft", "Burglary", "Robbery", "Vandalism", "Property Damage",
                    "Arson", "Shoplifting", "Grand Larceny",
                    "Petty Larceny", "Auto Theft", "Bike Theft", "Breaking & Entering",
                    "Looting", "Pickpocketing", "Forgery", "Counterfeiting"
                };
                break;
            case 2: // Cyber Crimes
                categoryTitle = "Cyber Crimes";
                types = new String[]{
                    "Cybercrime", "Scam/Phishing", "Identity Theft", "Fraud",
                    "Hacking", "Malware Distribution", "Data Breach",
                    "Online Harassment", "Catfishing", "Ransomware", "Credit Card Fraud",
                    "Money Laundering", "Unauthorized Access"
                };
                break;
            case 3: // Public Order
                categoryTitle = "Public Order";
                types = new String[]{
                    "Noise Complaint", "Public Disturbance", "Harassment", "Stalking",
                    "Illegal Gambling", "Littering", "Illegal Dumping",
                    "Loitering", "Disorderly Conduct", "Indecent Exposure", "Vagrancy",
                    "Public Intoxication", "Prostitution", "Unlicensed Vending"
                };
                break;
            case 4: // Traffic & Vehicle
                categoryTitle = "Traffic & Vehicle";
                types = new String[]{
                    "Traffic Accident", "Illegal Parking", "Speeding", "Reckless Driving",
                    "DUI/DWI", "Hit and Run", "Expired Registration", "Broken Headlight",
                    "Expired License", "Improper Lane Change", "Running Red Light",
                    "Unregistered Vehicle", "No Insurance", "Unsafe Lane Change"
                };
                break;
            case 5: // Other
                categoryTitle = "Other Incidents";
                types = new String[]{
                    "Missing Person", "Drug-related", "Animal Cruelty",
                    "Lost Property", "Found Property", "Suspicious Activity", "Welfare Check",
                    "Environmental Violation", "Permit Violation",
                    "Unauthorized Entry"
                };
                break;
            default:
                return;
        }
        
        // Parse current selections (comma-separated)
        String currentTypes = actvIncidentType.getText().toString();
        List<String> selectedTypes = new ArrayList<>();
        if (!currentTypes.isEmpty()) {
            for (String type : currentTypes.split(",")) {
                selectedTypes.add(type.trim());
            }
        }
        
        // Create custom layout with ScrollView
        android.view.View customView = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_incident_types, null);
        android.widget.LinearLayout container = customView.findViewById(R.id.incidentTypesContainer);
        
        // Remove container from its parent (the ScrollView in the layout)
        android.view.ViewGroup parentOfContainer = (android.view.ViewGroup) container.getParent();
        if (parentOfContainer != null) {
            parentOfContainer.removeView(container);
        }
        
        // Add info text to parent (outside scroll) - will be sticky at top
        android.widget.LinearLayout parentLayout = new android.widget.LinearLayout(this);
        parentLayout.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        parentLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        
        android.widget.TextView tvInfo = new android.widget.TextView(this);
        tvInfo.setText("Select up to 2 incident types");
        tvInfo.setTextSize(12);
        tvInfo.setTextColor(android.graphics.Color.GRAY);
        tvInfo.setGravity(android.view.Gravity.CENTER);
        tvInfo.setPadding(16, 16, 16, 8);
        tvInfo.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        parentLayout.addView(tvInfo);
        
        // Add the scrollable container
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        scrollView.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        ));
        scrollView.addView(container);
        parentLayout.addView(scrollView);
        
        // Add checkboxes to container
        android.widget.LinearLayout checkboxGroup = new android.widget.LinearLayout(this);
        checkboxGroup.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        checkboxGroup.setOrientation(android.widget.LinearLayout.VERTICAL);
        
        int blueColor = android.graphics.Color.BLUE;
        android.content.res.ColorStateList colorStateList = android.content.res.ColorStateList.valueOf(blueColor);
        
        for (int i = 0; i < types.length; i++) {
            android.widget.CheckBox checkBox = new android.widget.CheckBox(this);
            checkBox.setText(types[i]);
            checkBox.setTextSize(16);
            checkBox.setTextColor(android.graphics.Color.WHITE);
            checkBox.setPadding(32, 20, 16, 20);
            
            // Apply electric blue color to CheckBox
            androidx.core.widget.CompoundButtonCompat.setButtonTintList(checkBox, colorStateList);
            
            android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 8, 0, 8);
            checkBox.setLayoutParams(params);
            
            // Check if already selected
            if (selectedTypes.contains(types[i])) {
                checkBox.setChecked(true);
            }
            
            // Listener to enforce max 2 selections
            final int index = i;
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (selectedTypes.size() >= 2) {
                        checkBox.setChecked(false);
                        Toast.makeText(this, "Maximum 2 incident types allowed", Toast.LENGTH_SHORT).show();
                    } else {
                        selectedTypes.add(types[index]);
                    }
                } else {
                    selectedTypes.remove(types[index]);
                }
            });
            
            checkboxGroup.addView(checkBox);
        }
        
        container.addView(checkboxGroup);
        
        // Create a custom title view that's center-aligned
        android.widget.TextView customTitle = new android.widget.TextView(this);
        customTitle.setText(categoryTitle);
        customTitle.setTextSize(20);
        customTitle.setTextColor(android.graphics.Color.WHITE);
        customTitle.setGravity(android.view.Gravity.CENTER);
        customTitle.setPadding(16, 16, 16, 16);
        customTitle.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        
        // Create a container for title + content
        android.widget.LinearLayout dialogContent = new android.widget.LinearLayout(this);
        dialogContent.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        dialogContent.setOrientation(android.widget.LinearLayout.VERTICAL);
        dialogContent.addView(customTitle);
        dialogContent.addView(parentLayout);
        
        // Modern dark dialog with border
        androidx.appcompat.app.AlertDialog dialog2 = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setView(dialogContent)
            .setPositiveButton("Select", (dialogInterface, which) -> {
                if (!selectedTypes.isEmpty()) {
                    // Join selected types with comma
                    String result = String.join(", ", selectedTypes);
                    actvIncidentType.setText(result);
                    
                    // Clear accusation field - user will click to see dropdown
                    etAccusation.setText("");
                    
                    android.util.Log.d("AddReportActivity", "✅ Selected incident types: " + result);
                    dialogInterface.dismiss();
                } else {
                    Toast.makeText(this, "Please select at least one incident type", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Back", (dialogInterface, which) -> {
                dialogInterface.dismiss();
                showIncidentTypeDialog(null); // Show categories again
            })
            .create();
        
        // Apply custom background with border
        if (dialog2.getWindow() != null) {
            dialog2.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
        }
        
        dialog2.show();
    }
    
    private void setupRelationshipDropdown() {
        String[] relationships = {
            "Stranger", "Neighbor", "Friend", "Acquaintance",
            "Family Member", "Relative", "Spouse", "Ex-Spouse",
            "Partner", "Ex-Partner", "Co-worker", "Employer",
            "Employee", "Landlord", "Tenant", "Other", "Unknown"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_dropdown_item_1line, relationships);
        actvRelationship.setAdapter(adapter);
    }
    
    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            R.style.Theme_App_DatePickerDialog,
            (view, year, month, dayOfMonth) -> {
                selectedDate.set(year, month, dayOfMonth);
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                etIncidentDate.setText(dateFormat.format(selectedDate.getTime()));
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        
        // Set max date to TODAY - cannot select future dates
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        
        datePickerDialog.show();
    }
    
    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
            this,
            R.style.Theme_App_TimePickerDialog,
            (view, hourOfDay, minute) -> {
                selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selectedTime.set(Calendar.MINUTE, minute);
                SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                etIncidentTime.setText(timeFormat.format(selectedTime.getTime()));
            },
            selectedTime.get(Calendar.HOUR_OF_DAY),
            selectedTime.get(Calendar.MINUTE),
            false
        );
        timePickerDialog.show();
    }
    
    private void openCamera() {
        try {
            android.util.Log.d("AddReportActivity", "📷 Opening camera...");
            
            // Create photo file
            File photoFile = new File(getExternalFilesDir(null), 
                "photo_" + System.currentTimeMillis() + ".jpg");
            android.util.Log.d("AddReportActivity", "Photo file path: " + photoFile.getAbsolutePath());
            
            // Get URI from FileProvider
            currentPhotoUri = FileProvider.getUriForFile(this, 
                getPackageName() + ".provider", photoFile);
            android.util.Log.d("AddReportActivity", "Photo URI: " + currentPhotoUri);
            
            // Launch camera
            cameraLauncher.launch(currentPhotoUri);
            android.util.Log.d("AddReportActivity", "✅ Camera launched successfully");
        } catch (Exception e) {
            android.util.Log.e("AddReportActivity", "❌ Failed to open camera: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Failed to open camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
            android.util.Log.e("AddReportActivity", "Error showing image: " + e.getMessage());
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
            emptyStateImages.setVisibility(View.VISIBLE);
            recyclerImages.setVisibility(View.GONE);
        } else {
            imageAdapter.notifyDataSetChanged();
            emptyStateImages.setVisibility(View.GONE);
            recyclerImages.setVisibility(View.VISIBLE);
        }
    }
    
    private void updateVideoView() {
        if (videoList.isEmpty()) {
            emptyStateVideos.setVisibility(View.VISIBLE);
            recyclerVideos.setVisibility(View.GONE);
        } else {
            videoAdapter.notifyDataSetChanged();
            emptyStateVideos.setVisibility(View.GONE);
            recyclerVideos.setVisibility(View.VISIBLE);
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
    
    private void submitReport() {
        String complainantName = etComplainantName.getText().toString().trim();
        String complainantContact = etComplainantContact.getText().toString().trim();
        String complainantAddress = etComplainantAddress.getText().toString().trim();
        String incidentType = actvIncidentType.getText().toString().trim();
        String incidentDate = etIncidentDate.getText().toString().trim();
        String incidentTime = etIncidentTime.getText().toString().trim();
        String incidentLocation = etIncidentLocation.getText().toString().trim();
        String narrative = etNarrative.getText().toString().trim();
        
        if (complainantName.isEmpty() || complainantContact.isEmpty() || 
            incidentType.isEmpty() || incidentDate.isEmpty() || 
            incidentTime.isEmpty() || incidentLocation.isEmpty() || narrative.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Validate complainant contact
        if (!PhoneNumberValidator.isValidPhilippineNumber(complainantContact)) {
            etComplainantContact.setError("Invalid contact number");
            etComplainantContact.requestFocus();
            Toast.makeText(this, "Invalid complainant contact number. " + 
                         "Please use format: 09XXXXXXXXX or +639XXXXXXXXX", Toast.LENGTH_LONG).show();
            return;
        }
        
        BlotterReport report = new BlotterReport();
        report.setCaseNumber(tvCaseNumber.getText().toString());
        report.setComplainantName(complainantName);
        report.setComplainantContact(complainantContact);
        report.setComplainantAddress(complainantAddress);
        report.setIncidentType(incidentType);
        report.setIncidentTime(incidentTime);
        report.setIncidentLocation(incidentLocation);
        report.setNarrative(narrative);
        report.setStatus("PENDING");
        
        // Set respondent info if provided
        String respondentName = etRespondentName.getText().toString().trim();
        String respondentAlias = etRespondentAlias.getText().toString().trim();
        String respondentAddress = etRespondentAddress.getText().toString().trim();
        String respondentContact = etRespondentContact.getText().toString().trim();
        String accusation = etAccusation.getText().toString().trim();
        String relationship = actvRelationship.getText().toString().trim();
        
        if (!respondentName.isEmpty()) {
            report.setRespondentName(respondentName);
        }
        if (!respondentAlias.isEmpty()) {
            report.setRespondentAlias(respondentAlias);
        }
        if (!respondentAddress.isEmpty()) {
            report.setRespondentAddress(respondentAddress);
        }
        if (!respondentContact.isEmpty()) {
            // Validate respondent contact if provided
            if (!PhoneNumberValidator.isValidPhilippineNumber(respondentContact)) {
                etRespondentContact.setError("Invalid contact number");
                etRespondentContact.requestFocus();
                Toast.makeText(this, "Invalid respondent contact number. " + 
                             "Please use format: 09XXXXXXXXX or +639XXXXXXXXX", Toast.LENGTH_LONG).show();
                return;
            }
            report.setRespondentContact(respondentContact);
        }
        if (!accusation.isEmpty()) {
            report.setAccusation(accusation);
        }
        if (!relationship.isEmpty()) {
            report.setRelationshipToComplainant(relationship);
        }
        
        // Set incident date and time
        selectedDate.set(Calendar.HOUR_OF_DAY, selectedTime.get(Calendar.HOUR_OF_DAY));
        selectedDate.set(Calendar.MINUTE, selectedTime.get(Calendar.MINUTE));
        report.setIncidentDate(selectedDate.getTimeInMillis());
        
        // Set user ID
        int userId = preferencesManager.getUserId();
        report.setReportedById(userId);
        
        // Save images
        if (!imageList.isEmpty()) {
            StringBuilder uris = new StringBuilder();
            for (int i = 0; i < imageList.size(); i++) {
                uris.append(imageList.get(i).toString());
                if (i < imageList.size() - 1) uris.append(",");
            }
            report.setImageUris(uris.toString());
        }
        
        // Save videos
        if (!videoList.isEmpty()) {
            StringBuilder uris = new StringBuilder();
            for (int i = 0; i < videoList.size(); i++) {
                uris.append(videoList.get(i).toString());
                if (i < videoList.size() - 1) uris.append(",");
            }
            report.setVideoUris(uris.toString());
        }
        
        // Show loading for report submission
        com.example.blottermanagementsystem.utils.GlobalLoadingManager.show(this, "Submitting report...");
        
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Save to local database first
                long reportId = database.blotterReportDao().insertReport(report);
                
                if (reportId > 0) {
                    report.setId((int) reportId);
                    
                    // Check if online and sync to API
                    NetworkMonitor networkMonitor = new NetworkMonitor(AddReportActivity.this);
                    if (networkMonitor.isNetworkAvailable()) {
                        // Sync to API
                        ApiClient.createReport(report, new ApiClient.ApiCallback<BlotterReport>() {
                            @Override
                            public void onSuccess(BlotterReport result) {
                                android.util.Log.d("AddReport", "✅ Report synced to API - API ID: " + result.getId());
                                // Store the API ID in the local report for future delete operations
                                report.setApiId(result.getId());
                                // Update local database with API response on background thread
                                new Thread(() -> {
                                    database.blotterReportDao().updateReport(report);
                                    android.util.Log.d("AddReport", "✅ Local database updated with API ID: " + report.getApiId());
                                }).start();
                            }
                            
                            @Override
                            public void onError(String errorMessage) {
                                android.util.Log.w("AddReport", "⚠️ API sync failed: " + errorMessage);
                                // Report saved locally, will sync when online
                            }
                        });
                    } else {
                        android.util.Log.i("AddReport", "Offline mode: Report saved locally, will sync when online");
                    }
                    
                    // Get user name from database
                    com.example.blottermanagementsystem.data.entity.User user = 
                        database.userDao().getUserById(userId);
                    
                    String userName = "Unknown User";
                    if (user != null) {
                        userName = user.getFirstName() + " " + user.getLastName();
                    }
                
                // Send notification to admin about new report
                NotificationHelper notificationHelper = new NotificationHelper(this);
                notificationHelper.notifyNewReport(
                    userId, // Current user ID (the one who filed the report)
                    report.getCaseNumber(),
                    userName,
                    (int) reportId,
                    userName
                );
                
                runOnUiThread(() -> {
                    com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
                    // Redirect to Report Details instead of going back to dashboard
                    Intent detailsIntent = new Intent(AddReportActivity.this, ReportDetailActivity.class);
                    detailsIntent.putExtra("REPORT_ID", (int) reportId);
                    startActivity(detailsIntent);
                    finish();
                });
            } else {
                runOnUiThread(() -> {
                    com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
                    Toast.makeText(this, "Failed to submit report", Toast.LENGTH_SHORT).show();
                });
            }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
                    Toast.makeText(this, "Error submitting report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}

