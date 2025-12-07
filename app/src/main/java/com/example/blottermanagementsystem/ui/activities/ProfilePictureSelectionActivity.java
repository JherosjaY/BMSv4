package com.example.blottermanagementsystem.ui.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStream;
import java.util.concurrent.Executors;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import com.bumptech.glide.Glide;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.entity.User;
import com.example.blottermanagementsystem.utils.PreferencesManager;
import com.example.blottermanagementsystem.utils.PermissionHelper;
import com.google.android.material.button.MaterialButton;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfilePictureSelectionActivity extends BaseActivity {
    
    private TextView tvSelectedAvatar;
    private ImageView ivSelectedImage, ivDefaultAvatar;
    private MaterialButton btnTakePhoto, btnChooseGallery, btnContinue, btnSkip;
    private com.google.android.material.textfield.TextInputEditText etFirstName, etLastName;
    private PreferencesManager preferencesManager;
    
    // Stepper UI elements
    private androidx.cardview.widget.CardView step1Circle, step2Circle;
    private TextView step1Text, step2Text, step1Label, step2Label;
    private android.view.View timelineLine;
    private ActivityResultLauncher<String> galleryLauncher; // Changed to String for GetContent
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<String> storagePermissionLauncher;
    
    private Uri selectedImageUri = null;
    private Uri photoUri = null;
    private boolean waitingForCameraPermission = false;
    private boolean waitingForStoragePermission = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_picture_selection);
        
        preferencesManager = new PreferencesManager(this);
        
        // Debug: Check userId immediately
        int userId = preferencesManager.getUserId();
        int userIdFromIntent = getIntent().getIntExtra("USER_ID", -1);
        
        android.util.Log.d("ProfilePictureSelection", "=== ONCREATE ===");
        android.util.Log.d("ProfilePictureSelection", "UserId from PreferencesManager: " + userId);
        android.util.Log.d("ProfilePictureSelection", "UserId from Intent: " + userIdFromIntent);
        android.util.Log.d("ProfilePictureSelection", "Is logged in: " + preferencesManager.isLoggedIn());
        android.util.Log.d("ProfilePictureSelection", "User role: " + preferencesManager.getUserRole());
        
        // If PreferencesManager has -1 but Intent has valid userId, save it
        if (userId == -1 && userIdFromIntent != -1) {
            android.util.Log.d("ProfilePictureSelection", "⚠️ PreferencesManager lost userId! Restoring from Intent: " + userIdFromIntent);
            preferencesManager.setUserId(userIdFromIntent);
            preferencesManager.setLoggedIn(true);
        }
        
        setupLaunchers();
        initViews();
        loadGoogleProfilePicture();
        setupListeners();
    }
    
    private void loadGoogleProfilePicture() {
        // Check if user signed in with Google
        if (preferencesManager.isGoogleAccount()) {
            String photoUrl = preferencesManager.getGooglePhotoUrl();
            String displayName = preferencesManager.getGoogleDisplayName();
            
            if (photoUrl != null && !photoUrl.isEmpty()) {
                // Load Google profile picture using Glide
                Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .circleCrop()
                    .into(ivSelectedImage);
                
                ivSelectedImage.setVisibility(android.view.View.VISIBLE);
                tvSelectedAvatar.setVisibility(android.view.View.GONE);
                
                // Mark step 1 as completed since Google photo is loaded
                markStepCompleted(1);
                
                // Mark as having profile picture
                preferencesManager.setHasSelectedPfp(true);
                
                Toast.makeText(this, "Loaded Google profile picture for " + displayName, Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    public void onBackPressed() {
        // Prevent going back to registration screen
        // User must complete profile picture selection
        Toast.makeText(this, "Please select a profile picture to continue", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Recheck permissions when returning from settings
        // This ensures buttons work after user grants permission
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        // If user hasn't selected a profile picture and leaves the app,
        // clear the login session so they have to log in again
        if (!preferencesManager.hasSelectedProfilePicture() && !isFinishing()) {
            // User pressed HOME or switched apps without completing registration
            // Clear the session to force re-login
            preferencesManager.clearSession();
        }
    }
    
    private void setupLaunchers() {
        // Camera permission launcher
        cameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    // Permission granted - open camera
                    openCamera();
                } else {
                    // Permission denied - show explanation
                    if (PermissionHelper.shouldShowRationale(ProfilePictureSelectionActivity.this, Manifest.permission.CAMERA)) {
                        // User denied but didn't check "Don't ask again"
                        showPermissionRationaleDialog(
                            PermissionHelper.getPermissionName(Manifest.permission.CAMERA), 
                            PermissionHelper.getPermissionDescription(Manifest.permission.CAMERA),
                            () -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA));
                    } else {
                        // User denied with "Don't ask again" or first time
                        Toast.makeText(ProfilePictureSelectionActivity.this, 
                            "Camera permission is required to take photos", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        );
        
        // Storage permission launcher
        storagePermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    // Permission granted - open gallery
                    openGallery();
                } else {
                    // Permission denied - show explanation
                    String permission = PermissionHelper.getStoragePermission();
                    
                    if (PermissionHelper.shouldShowRationale(ProfilePictureSelectionActivity.this, permission)) {
                        // User denied but didn't check "Don't ask again"
                        showPermissionRationaleDialog(
                            PermissionHelper.getPermissionName(permission), 
                            PermissionHelper.getPermissionDescription(permission),
                            () -> storagePermissionLauncher.launch(permission));
                    } else {
                        // User denied with "Don't ask again" or first time
                        Toast.makeText(ProfilePictureSelectionActivity.this, 
                            "Storage permission is required to select photos", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        );
        
        // Gallery launcher - Use native photo picker (same as ProfileActivity)
        galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    loadAndDisplayImage(selectedImageUri); // Use correct method name
                    
                    // Try to grant persistent permission
                    try {
                        getContentResolver().takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );
                    } catch (Exception e) {
                        // Ignore if not supported
                    }
                }
            }
        );
        
        // Camera launcher
        cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (photoUri != null) {
                        // Copy camera image to permanent location
                        Uri permanentUri = copyImageToPermanentStorage(photoUri);
                        if (permanentUri != null) {
                            selectedImageUri = permanentUri;
                            loadAndDisplayImage(permanentUri);
                        } else {
                            // Fallback: use temporary URI
                            selectedImageUri = photoUri;
                            loadAndDisplayImage(photoUri);
                        }
                    }
                }
            }
        );
    }
    
    private void initViews() {
        tvSelectedAvatar = findViewById(R.id.tvSelectedAvatar);
        ivSelectedImage = findViewById(R.id.ivSelectedImage);
        ivDefaultAvatar = findViewById(R.id.ivDefaultAvatar);
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnChooseGallery = findViewById(R.id.btnChooseGallery);
        btnContinue = findViewById(R.id.btnContinue);
        btnSkip = findViewById(R.id.btnSkip);
        
        // Initialize stepper UI elements
        step1Circle = findViewById(R.id.step1Circle);
        step2Circle = findViewById(R.id.step2Circle);
        step1Text = findViewById(R.id.step1Text);
        step2Text = findViewById(R.id.step2Text);
        step1Label = findViewById(R.id.step1Label);
        step2Label = findViewById(R.id.step2Label);
        timelineLine = findViewById(R.id.timelineLine);
        
        // Pre-fill from Google if available
        if (preferencesManager.isGoogleAccount()) {
            String displayName = preferencesManager.getGoogleDisplayName();
            if (displayName != null && displayName.contains(" ")) {
                String[] parts = displayName.split(" ", 2);
                etFirstName.setText(parts[0]);
                etLastName.setText(parts[1]);
            }
        }
    }
    
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }
    
    private boolean checkCameraPermission() {
        return PermissionHelper.hasCameraPermission(this);
    }
    
    private boolean checkStoragePermission() {
        return PermissionHelper.hasStoragePermission(this);
    }
    
    private void showPermissionRationaleDialog(String permissionType, String message, Runnable onRetry) {
        new AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage(message)
            .setPositiveButton("Try Again", (dialog, which) -> {
                onRetry.run();
            })
            .setNegativeButton("Cancel", (dialog, which) -> {
                dialog.dismiss();
            })
            .setCancelable(true)
            .show();
    }
    
    private void showSettingsDialog(String permissionType) {
        new AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage(permissionType + " permission is required. Please enable it in Settings.")
            .setPositiveButton("Open Settings", (dialog, which) -> {
                // Open app settings
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            })
            .setNegativeButton("Cancel", (dialog, which) -> {
                dialog.dismiss();
            })
            .setCancelable(true)
            .show();
    }
    
    private void openCamera() {
        try {
            // Create image file
            File photoFile = createImageFile();
            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(this,
                    "com.example.blottermanagementsystem.provider",
                    photoFile);
                
                // Launch camera with URI permissions and front camera
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                intent.putExtra("android.intent.extras.CAMERA_FACING", android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT);
                intent.putExtra("android.intent.extras.LENS_FACING_FRONT", 1);
                intent.putExtra("android.intent.extra.USE_FRONT_CAMERA", true);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                cameraLauncher.launch(intent);
            }
        } catch (IOException e) {
            Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
    
    private void openGallery() {
        // Use native photo picker (same as ProfileActivity)
        galleryLauncher.launch("image/*");
    }
    
    private void setupListeners() {
        btnTakePhoto.setOnClickListener(v -> {
            if (checkCameraPermission()) {
                // Permission already granted - open camera directly
                openCamera();
            } else {
                // Request camera permission
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });
        
        btnChooseGallery.setOnClickListener(v -> {
            // Native photo picker doesn't need storage permission!
            openGallery();
        });
        
        btnSkip.setOnClickListener(v -> {
            // ✅ FIXED: Validate firstname and lastname BEFORE allowing skip
            final String firstName = etFirstName.getText().toString().trim();
            final String lastName = etLastName.getText().toString().trim();
            
            android.util.Log.d("ProfilePictureSelection", "=== SKIP BUTTON CLICKED ===");
            android.util.Log.d("ProfilePictureSelection", "FirstName: '" + firstName + "'");
            android.util.Log.d("ProfilePictureSelection", "LastName: '" + lastName + "'");
            android.util.Log.d("ProfilePictureSelection", "Image selected: " + (selectedImageUri != null));
            
            // ✅ STRICT: FirstName and LastName are MANDATORY
            if (firstName.isEmpty() || lastName.isEmpty()) {
                Toast.makeText(this, "First name and last name are required", Toast.LENGTH_SHORT).show();
                android.util.Log.w("ProfilePictureSelection", "⚠️ User tried to skip without entering names!");
                return;
            }
            
            // ✅ Can only skip if picture is taken/imported (selectedImageUri is not null)
            if (selectedImageUri == null) {
                Toast.makeText(this, "Please take or import a photo before skipping", Toast.LENGTH_SHORT).show();
                android.util.Log.w("ProfilePictureSelection", "⚠️ User tried to skip without selecting a picture!");
                return;
            }
            
            // ✅ All validations passed - proceed with skip
            android.util.Log.d("ProfilePictureSelection", "✅ Skip validation passed - proceeding");
            preferencesManager.setHasSelectedPfp(false);
            
            // Update user in database with names and picture
            int userIdTemp = preferencesManager.getUserId();
            if (userIdTemp == -1) {
                int userIdFromIntent = getIntent().getIntExtra("USER_ID", -1);
                if (userIdFromIntent != -1) {
                    userIdTemp = userIdFromIntent;
                    preferencesManager.setUserId(userIdTemp);
                }
            }
            
            final int userId = userIdTemp;
            
            if (userId != -1) {
                Executors.newSingleThreadExecutor().execute(() -> {
                    com.example.blottermanagementsystem.data.database.BlotterDatabase database = 
                        com.example.blottermanagementsystem.data.database.BlotterDatabase.getDatabase(this);
                    com.example.blottermanagementsystem.data.entity.User user = database.userDao().getUserById(userId);
                    
                    if (user != null) {
                        user.setFirstName(firstName);
                        user.setLastName(lastName);
                        
                        if (selectedImageUri != null) {
                            String uriString = selectedImageUri.toString();
                            user.setProfilePhotoUri(uriString);
                            android.util.Log.d("ProfilePictureSelection", "✅ Setting profile URI: " + uriString);
                        }
                        
                        user.setProfileCompleted(true);
                        database.userDao().updateUser(user);
                        android.util.Log.d("ProfilePictureSelection", "✅ Updated user in database (skip flow)");
                        
                        preferencesManager.setFirstName(firstName);
                        preferencesManager.setLastName(lastName);
                        
                        runOnUiThread(() -> navigateToDashboard());
                    } else {
                        android.util.Log.e("ProfilePictureSelection", "❌ User not found in database!");
                        runOnUiThread(() -> navigateToDashboard());
                    }
                });
            } else {
                navigateToDashboard();
            }
        });
        
        btnContinue.setOnClickListener(v -> {
            // Validate name fields
            final String firstName = etFirstName.getText().toString().trim();
            final String lastName = etLastName.getText().toString().trim();
            
            android.util.Log.d("ProfilePictureSelection", "=== CONTINUE BUTTON CLICKED ===");
            android.util.Log.d("ProfilePictureSelection", "FirstName input: '" + firstName + "'");
            android.util.Log.d("ProfilePictureSelection", "LastName input: '" + lastName + "'");
            android.util.Log.d("ProfilePictureSelection", "FirstName empty? " + firstName.isEmpty());
            android.util.Log.d("ProfilePictureSelection", "LastName empty? " + lastName.isEmpty());
            
            if (firstName.isEmpty() || lastName.isEmpty()) {
                Toast.makeText(this, "Please enter your first and last name", Toast.LENGTH_SHORT).show();
                android.util.Log.w("ProfilePictureSelection", "⚠️ User tried to continue without entering names!");
                return;
            }
            
            // Save profile picture URI if selected
            if (selectedImageUri != null) {
                // Only try to grant persistent permission for non-FileProvider URIs
                // FileProvider URIs are already in our app's storage and don't need persistent permissions
                String uriString = selectedImageUri.toString();
                if (!uriString.contains("com.example.blottermanagementsystem.provider")) {
                    try {
                        getContentResolver().takePersistableUriPermission(
                            selectedImageUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );
                        android.util.Log.d("ProfilePictureSelection", "✅ Granted persistent permission for gallery URI");
                    } catch (Exception e) {
                        android.util.Log.w("ProfilePictureSelection", "Could not grant persistent permission: " + e.getMessage());
                    }
                } else {
                    android.util.Log.d("ProfilePictureSelection", "ℹ️ FileProvider URI - no persistent permission needed");
                }
                
                preferencesManager.setProfileImageUri(uriString);
                android.util.Log.d("ProfilePictureSelection", "✅ Saved URI to preferences: " + uriString);
            } else {
                android.util.Log.w("ProfilePictureSelection", "⚠️ No image selected - selectedImageUri is NULL");
            }
            
            preferencesManager.setHasSelectedProfilePicture(true);
            
            // Update user in database (MUST complete before navigation)
            int userIdTemp = preferencesManager.getUserId();
            
            // If userId is -1, try to get it from Intent
            if (userIdTemp == -1) {
                int userIdFromIntent = getIntent().getIntExtra("USER_ID", -1);
                if (userIdFromIntent != -1) {
                    android.util.Log.d("ProfilePictureSelection", "⚠️ userId was -1, restoring from Intent: " + userIdFromIntent);
                    userIdTemp = userIdFromIntent;
                    preferencesManager.setUserId(userIdTemp);
                }
            }
            
            final int userId = userIdTemp; // Make it final for lambda
            
            android.util.Log.d("ProfilePictureSelection", "🔍 Checking userId: " + userId);
            android.util.Log.d("ProfilePictureSelection", "🔍 selectedImageUri: " + (selectedImageUri != null ? selectedImageUri.toString() : "NULL"));
            
            if (userId != -1) {
                android.util.Log.d("ProfilePictureSelection", "✅ Starting database update thread...");
                Executors.newSingleThreadExecutor().execute(() -> {
                    com.example.blottermanagementsystem.data.database.BlotterDatabase database = 
                        com.example.blottermanagementsystem.data.database.BlotterDatabase.getDatabase(this);
                    com.example.blottermanagementsystem.data.entity.User user = database.userDao().getUserById(userId);
                    
                    android.util.Log.d("ProfilePictureSelection", "=== DATABASE UPDATE ===");
                    android.util.Log.d("ProfilePictureSelection", "UserID: " + userId);
                    android.util.Log.d("ProfilePictureSelection", "User found: " + (user != null));
                    
                    if (user != null) {
                        android.util.Log.d("ProfilePictureSelection", "🔧 SETTING USER DATA:");
                        android.util.Log.d("ProfilePictureSelection", "  FirstName: '" + firstName + "'");
                        android.util.Log.d("ProfilePictureSelection", "  LastName: '" + lastName + "'");
                        
                        user.setFirstName(firstName);
                        user.setLastName(lastName);
                        
                        // Save profile picture URI to database
                        if (selectedImageUri != null) {
                            String uriString = selectedImageUri.toString();
                            user.setProfilePhotoUri(uriString);
                            android.util.Log.d("ProfilePictureSelection", "✅ Setting profile URI in database: " + uriString);
                        } else {
                            android.util.Log.w("ProfilePictureSelection", "⚠️ selectedImageUri is NULL - not saving to database");
                        }
                        
                        // Mark profile as completed
                        user.setProfileCompleted(true);
                        android.util.Log.d("ProfilePictureSelection", "✅ Marking profile as completed");
                        
                        database.userDao().updateUser(user);
                        android.util.Log.d("ProfilePictureSelection", "✅ Updated user in database: " + firstName + " " + lastName);
                        
                        // VERIFY the save
                        User verifyAfterSave = database.userDao().getUserById(userId);
                        if (verifyAfterSave != null) {
                            android.util.Log.d("ProfilePictureSelection", "✅ VERIFY AFTER SAVE:");
                            android.util.Log.d("ProfilePictureSelection", "  FirstName in DB: '" + verifyAfterSave.getFirstName() + "'");
                            android.util.Log.d("ProfilePictureSelection", "  LastName in DB: '" + verifyAfterSave.getLastName() + "'");
                            android.util.Log.d("ProfilePictureSelection", "  ProfilePhotoUri in DB: '" + verifyAfterSave.getProfilePhotoUri() + "'");
                        }
                        
                        // Update PreferencesManager AFTER database update
                        preferencesManager.setFirstName(firstName);
                        preferencesManager.setLastName(lastName);
                        android.util.Log.d("ProfilePictureSelection", "✅ Updated preferences with name");
                        
                        
                        // Verify the update
                        com.example.blottermanagementsystem.data.entity.User verifyUser = database.userDao().getUserById(userId);
                        if (verifyUser != null) {
                            android.util.Log.d("ProfilePictureSelection", "✅ VERIFY: FirstName = " + verifyUser.getFirstName());
                            android.util.Log.d("ProfilePictureSelection", "✅ VERIFY: LastName = " + verifyUser.getLastName());
                            android.util.Log.d("ProfilePictureSelection", "✅ VERIFY: ProfilePhotoUri = " + verifyUser.getProfilePhotoUri());
                            android.util.Log.d("ProfilePictureSelection", "✅ VERIFY: ProfileCompleted = " + verifyUser.isProfileCompleted());
                        } else {
                            android.util.Log.e("ProfilePictureSelection", "❌ VERIFY FAILED: Could not reload user from database!");
                        }
                        
                        // Navigate AFTER database update
                        runOnUiThread(() -> navigateToDashboard());
                    } else {
                        android.util.Log.e("ProfilePictureSelection", "❌ User not found in database!");
                        runOnUiThread(() -> navigateToDashboard());
                    }
                });
                return; // Don't navigate yet - wait for database update
            }
            
            // If userId is -1, navigate immediately (shouldn't happen but fallback)
            navigateToDashboard();
        });
    }
    
    private void navigateToDashboard() {
        // Navigate to appropriate dashboard based on user role
        String role = preferencesManager.getUserRole();
        Intent intent;
        
        switch (role != null ? role.toLowerCase() : "user") {
            case "admin":
                intent = new Intent(this, AdminDashboardActivity.class);
                break;
            case "officer":
                intent = new Intent(this, OfficerDashboardActivity.class);
                break;
            default:
                intent = new Intent(this, UserDashboardActivity.class);
                break;
        }
        
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    private Uri copyImageToPermanentStorage(Uri tempUri) {
        try {
            // Create permanent file in app's private storage
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "profile_" + timeStamp + ".jpg";
            File permanentFile = new File(getFilesDir(), fileName);
            
            // Copy from temp URI to permanent file
            InputStream inputStream = getContentResolver().openInputStream(tempUri);
            java.io.FileOutputStream outputStream = new java.io.FileOutputStream(permanentFile);
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            inputStream.close();
            outputStream.close();
            
            // Return permanent URI
            return FileProvider.getUriForFile(this,
                "com.example.blottermanagementsystem.provider",
                permanentFile);
                
        } catch (Exception e) {
            android.util.Log.e("ProfilePictureSelection", "Error copying image to permanent storage", e);
            return null;
        }
    }
    
    private void loadAndDisplayImage(Uri imageUri) {
        try {
            // Open input stream
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            
            // First decode with inJustDecodeBounds=true to check dimensions
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();
            
            // Calculate inSampleSize to reduce memory usage
            int imageHeight = options.outHeight;
            int imageWidth = options.outWidth;
            int inSampleSize = 1;
            
            // Target size: 1024x1024 max
            int maxSize = 1024;
            
            if (imageHeight > maxSize || imageWidth > maxSize) {
                final int halfHeight = imageHeight / 2;
                final int halfWidth = imageWidth / 2;
                
                while ((halfHeight / inSampleSize) >= maxSize
                        && (halfWidth / inSampleSize) >= maxSize) {
                    inSampleSize *= 2;
                }
            }
            
            // Decode with inSampleSize
            inputStream = getContentResolver().openInputStream(imageUri);
            options.inJustDecodeBounds = false;
            options.inSampleSize = inSampleSize;
            options.inPreferredConfig = Bitmap.Config.RGB_565; // Use less memory
            
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();
            
            if (bitmap != null) {
                // Mark step 1 as completed
                markStepCompleted(1);
                
                // Display the compressed bitmap
                ivSelectedImage.setImageBitmap(bitmap);
                ivSelectedImage.setVisibility(android.view.View.VISIBLE);
                tvSelectedAvatar.setVisibility(android.view.View.GONE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void markStepCompleted(int step) {
        try {
            if (step == 1) {
                // Check if views are initialized
                if (step1Circle == null || step1Text == null || step1Label == null || timelineLine == null) {
                    android.util.Log.w("ProfilePictureSelection", "Stepper views not initialized yet");
                    return;
                }
                
                // Step 1 completed - green with checkmark
                step1Circle.setCardBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
                step1Text.setText("✓");
                step1Text.setTextSize(20);
                step1Label.setTextColor(getResources().getColor(android.R.color.white));
                
                // Animate the circle
                step1Circle.animate()
                    .scaleX(1.1f)
                    .scaleY(1.1f)
                    .setDuration(200)
                    .withEndAction(() -> {
                        step1Circle.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(200)
                            .start();
                    })
                    .start();
                
                // Update timeline line to green
                timelineLine.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
                
                // Activate step 2
                activateStep(2);
            }
        } catch (Exception e) {
            android.util.Log.e("ProfilePictureSelection", "Error marking step completed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void activateStep(int step) {
        try {
            if (step == 2) {
                // Check if views are initialized
                if (step2Circle == null || step2Text == null || step2Label == null) {
                    android.util.Log.w("ProfilePictureSelection", "Step 2 views not initialized yet");
                    return;
                }
                
                // Step 2 active - blue
                step2Circle.setCardBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark));
                step2Text.setTextColor(getResources().getColor(android.R.color.white));
                step2Label.setTextColor(getResources().getColor(android.R.color.white));
                
                // Animate the circle
                step2Circle.animate()
                    .scaleX(1.1f)
                    .scaleY(1.1f)
                    .setDuration(200)
                    .withEndAction(() -> {
                        step2Circle.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(200)
                            .start();
                    })
                    .start();
                
                // Scroll to step 2
                android.view.View step2View = findViewById(R.id.stepIndicator2);
                if (step2View != null) {
                    step2View.post(() -> {
                        try {
                            // Find the NestedScrollView in the layout
                            android.view.View rootView = findViewById(android.R.id.content);
                            if (rootView != null && rootView.getParent() instanceof androidx.core.widget.NestedScrollView) {
                                androidx.core.widget.NestedScrollView scrollView = 
                                    (androidx.core.widget.NestedScrollView) rootView.getParent();
                                scrollView.smoothScrollTo(0, step2View.getTop() - 100);
                            }
                        } catch (Exception e) {
                            android.util.Log.w("ProfilePictureSelection", "Could not scroll to step 2: " + e.getMessage());
                        }
                    });
                }
            }
        } catch (Exception e) {
            android.util.Log.e("ProfilePictureSelection", "Error activating step: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
