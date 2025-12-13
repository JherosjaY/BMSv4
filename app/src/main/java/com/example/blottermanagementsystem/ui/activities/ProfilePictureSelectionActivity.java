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
        String userId = preferencesManager.getUserId();
        int userIdFromIntent = getIntent().getIntExtra("USER_ID", -1);
        
        android.util.Log.d("ProfilePictureSelection", "=== ONCREATE ===");
        android.util.Log.d("ProfilePictureSelection", "UserId from PreferencesManager: " + userId);
        android.util.Log.d("ProfilePictureSelection", "UserId from Intent: " + userIdFromIntent);
        android.util.Log.d("ProfilePictureSelection", "Is logged in: " + preferencesManager.isLoggedIn());
        android.util.Log.d("ProfilePictureSelection", "User role: " + preferencesManager.getUserRole());
        
        // If PreferencesManager has empty userId but Intent has valid userId, save it
        if ((userId == null || userId.isEmpty()) && userIdFromIntent != -1) {
            android.util.Log.d("ProfilePictureSelection", "⚠️ PreferencesManager lost userId! Restoring from Intent: " + userIdFromIntent);
            preferencesManager.setUserId(String.valueOf(userIdFromIntent));
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
            if (cameraLauncher == null) {
                android.util.Log.e("ProfilePictureSelection", "❌ cameraLauncher is NULL!");
                Toast.makeText(this, "Camera launcher not initialized", Toast.LENGTH_SHORT).show();
                return;
            }
            
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
                android.util.Log.d("ProfilePictureSelection", "📸 Launching camera...");
                cameraLauncher.launch(intent);
            }
        } catch (IOException e) {
            Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
    
    private void openGallery() {
        if (galleryLauncher == null) {
            android.util.Log.e("ProfilePictureSelection", "❌ galleryLauncher is NULL!");
            Toast.makeText(this, "Gallery launcher not initialized", Toast.LENGTH_SHORT).show();
            return;
        }
        // Use native photo picker (same as ProfileActivity)
        android.util.Log.d("ProfilePictureSelection", "🖼️ Launching gallery...");
        galleryLauncher.launch("image/*");
    }
    
    private void setupListeners() {
        if (btnTakePhoto == null) {
            android.util.Log.e("ProfilePictureSelection", "❌ btnTakePhoto is NULL!");
        } else {
            btnTakePhoto.setOnClickListener(v -> {
                android.util.Log.d("ProfilePictureSelection", "📸 Camera button clicked");
                if (checkCameraPermission()) {
                    // Permission already granted - open camera directly
                    openCamera();
                } else {
                    // Request camera permission
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                }
            });
        }
        
        if (btnChooseGallery == null) {
            android.util.Log.e("ProfilePictureSelection", "❌ btnChooseGallery is NULL!");
        } else {
            btnChooseGallery.setOnClickListener(v -> {
                android.util.Log.d("ProfilePictureSelection", "🖼️ Gallery button clicked");
                // Native photo picker doesn't need storage permission!
                openGallery();
            });
        }
        
        btnSkip.setOnClickListener(v -> {
            // ✅ FIXED: Validate firstname and lastname BEFORE allowing skip
            final String firstName = etFirstName.getText().toString().trim();
            final String lastName = etLastName.getText().toString().trim();
            
            android.util.Log.d("ProfilePictureSelection", "=== SKIP BUTTON CLICKED ===");
            android.util.Log.d("ProfilePictureSelection", "FirstName: '" + firstName + "'");
            android.util.Log.d("ProfilePictureSelection", "LastName: '" + lastName + "'");
            android.util.Log.d("ProfilePictureSelection", "Image selected: " + (selectedImageUri != null));
            
            // ✅ STRICT: FirstName and LastName are MANDATORY (even for skip)
            if (firstName.isEmpty() || lastName.isEmpty()) {
                Toast.makeText(this, "First name and last name are required", Toast.LENGTH_SHORT).show();
                android.util.Log.w("ProfilePictureSelection", "⚠️ User tried to skip without entering names!");
                return;
            }
            
            // ✅ Picture is OPTIONAL when skipping - user can skip picture selection
            // But firstName and lastName are REQUIRED
            
            // ✅ All validations passed - proceed with skip
            android.util.Log.d("ProfilePictureSelection", "✅ Skip validation passed - proceeding");
            preferencesManager.setHasSelectedPfp(false);
            
            // Update user in database with names and picture
            String userIdStr = preferencesManager.getUserId();
            String userIdTemp = userIdStr;
            if (userIdTemp == null || userIdTemp.isEmpty()) {
                int userIdFromIntent = getIntent().getIntExtra("USER_ID", -1);
                if (userIdFromIntent != -1) {
                    userIdTemp = String.valueOf(userIdFromIntent);
                    preferencesManager.setUserId(userIdTemp);
                }
            }
            
            final String userId = userIdTemp;
            
            if (userId != null && !userId.isEmpty()) {
                Executors.newSingleThreadExecutor().execute(() -> {
                    com.example.blottermanagementsystem.data.database.BlotterDatabase database = 
                        com.example.blottermanagementsystem.data.database.BlotterDatabase.getDatabase(this);
                    int userIdInt = Integer.parseInt(userId);
                    com.example.blottermanagementsystem.data.entity.User user = database.userDao().getUserById(userIdInt);
                    
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
            
            android.util.Log.d("ProfilePictureSelection", "=== PURE ONLINE PROFILE UPDATE ===");
            android.util.Log.d("ProfilePictureSelection", "FirstName: '" + firstName + "'");
            android.util.Log.d("ProfilePictureSelection", "LastName: '" + lastName + "'");
            
            if (firstName.isEmpty() || lastName.isEmpty()) {
                Toast.makeText(this, "Please enter your first and last name", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Check internet connection
            com.example.blottermanagementsystem.utils.NetworkMonitor networkMonitor = 
                new com.example.blottermanagementsystem.utils.NetworkMonitor(this);
            
            if (!networkMonitor.isNetworkAvailable()) {
                android.util.Log.e("ProfilePictureSelection", "❌ No internet connection");
                Toast.makeText(this, "No internet connection. Please check your connection.", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Retrieve registration data from preferences
            String regUsername = preferencesManager.getTempUsername();
            String regEmail = preferencesManager.getTempEmail();
            String regPassword = preferencesManager.getTempPassword();

            if (regUsername == null || regEmail == null || regPassword == null) {
                Toast.makeText(this, "Registration data missing. Please re-register.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show global loading animation
            com.example.blottermanagementsystem.utils.GlobalLoadingManager.show(this, "Creating your account...");

            // If image is selected, upload to Cloudinary first
            if (selectedImageUri != null) {
                android.util.Log.d("ProfilePictureSelection", "📤 Uploading profile picture to Cloudinary...");
                com.example.blottermanagementsystem.utils.CloudinaryUploadManager uploadManager = 
                    new com.example.blottermanagementsystem.utils.CloudinaryUploadManager(this);
                
                uploadManager.uploadProfilePicture(selectedImageUri, new com.example.blottermanagementsystem.utils.CloudinaryUploadManager.UploadCallback() {
                    @Override
                    public void onSuccess(String cloudinaryUrl) {
                        android.util.Log.d("ProfilePictureSelection", "✅ Profile picture uploaded to Cloudinary: " + cloudinaryUrl);
                        // Save Cloudinary URL to preferences
                        preferencesManager.setProfileImageUri(cloudinaryUrl);
                        // Proceed with registration, passing the Cloudinary URL
                        proceedWithRegistration(firstName, lastName, regUsername, regEmail, regPassword, cloudinaryUrl);
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        android.util.Log.e("ProfilePictureSelection", "❌ Cloudinary upload failed: " + errorMessage);
                        com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
                        Toast.makeText(ProfilePictureSelectionActivity.this, "Image upload failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                // No image selected, proceed with registration with null image URL
                android.util.Log.d("ProfilePictureSelection", "No profile picture selected, proceeding with registration");
                proceedWithRegistration(firstName, lastName, regUsername, regEmail, regPassword, null);
            }
        });
    }
    
    /**
     * Proceed with user registration after image upload (if any)
     */
    private void proceedWithRegistration(String firstName, String lastName, String regUsername, String regEmail, String regPassword, String profileImageUrl) {
        // Call backend registration API with firstName, lastName, and profile image URL
        com.example.blottermanagementsystem.utils.NeonAuthManager neonAuthManager = new com.example.blottermanagementsystem.utils.NeonAuthManager(this);
        neonAuthManager.signUp(regEmail, regPassword, regUsername, firstName, lastName, profileImageUrl, new com.example.blottermanagementsystem.utils.NeonAuthManager.AuthCallback() {
            @Override
            public void onSuccess(com.example.blottermanagementsystem.utils.NeonAuthManager.AuthUser user) {
                android.util.Log.d("ProfilePictureSelection", "✅ Registration API success in PFP");
                // Hide loading animation
                com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
                
                // Save user info to preferences
                if (user != null && user.id != null) {
                    preferencesManager.setUserId(user.id);
                    preferencesManager.setUsername(user.username);
                    preferencesManager.setLoggedIn(true);
                    preferencesManager.setUserRole("user");
                    
                    // Save email from registration
                    preferencesManager.saveString("user_email", regEmail);
                    
                    android.util.Log.d("ProfilePictureSelection", "✅ Saved userId: " + user.id);
                    android.util.Log.d("ProfilePictureSelection", "✅ Saved username: " + user.username);
                    android.util.Log.d("ProfilePictureSelection", "✅ Saved email: " + regEmail);
                }
                preferencesManager.setHasSelectedProfilePicture(true);
                preferencesManager.setFirstName(firstName);
                preferencesManager.setLastName(lastName);
                
                // ✅ CRITICAL: Save profile image URL to PreferencesManager
                if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                    preferencesManager.setProfileImageUri(profileImageUrl);
                    android.util.Log.d("ProfilePictureSelection", "✅ Saved profileImageUrl: " + profileImageUrl);
                } else {
                    android.util.Log.d("ProfilePictureSelection", "⚠️ No profile image URL to save");
                }
                
                android.util.Log.d("ProfilePictureSelection", "✅ Saved firstName: " + firstName);
                android.util.Log.d("ProfilePictureSelection", "✅ Saved lastName: " + lastName);
                
                // Optionally clear temp registration data
                preferencesManager.clearTempUserData();
                // Navigate to dashboard
                navigateToDashboard();
            }
            @Override
            public void onError(String errorMessage) {
                android.util.Log.e("ProfilePictureSelection", "❌ Registration API error in PFP: " + errorMessage);
                // Hide loading animation on error
                com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
                Toast.makeText(ProfilePictureSelectionActivity.this, "Account creation failed: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
    
    /**
     * Pure Online: Update user profile via API (Neon database only)
     */
    private void updateProfileViaApi(String userId, String firstName, String lastName) {
        showLoading(true);
        
        android.util.Log.d("ProfilePictureSelection", "🌐 Calling API to update profile...");
        
        // Call API to update profile
        com.example.blottermanagementsystem.utils.ApiClient.updateProfile(userId, firstName, lastName, 
            new com.example.blottermanagementsystem.utils.ApiClient.ApiCallback<Object>() {
                @Override
                public void onSuccess(Object response) {
                    android.util.Log.d("ProfilePictureSelection", "✅ Profile updated successfully via API");
                    showLoading(false);
                    Toast.makeText(ProfilePictureSelectionActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    navigateToDashboard();
                }
                
                @Override
                public void onError(String errorMessage) {
                    android.util.Log.e("ProfilePictureSelection", "❌ Profile update failed: " + errorMessage);
                    showLoading(false);
                    Toast.makeText(ProfilePictureSelectionActivity.this, "Profile update failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                    // Still navigate to dashboard (profile was saved locally)
                    navigateToDashboard();
                }
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
    
    private void showLoading(boolean show) {
        if (show) {
            com.example.blottermanagementsystem.utils.GlobalLoadingManager.show(this, "Updating profile...");
        } else {
            com.example.blottermanagementsystem.utils.GlobalLoadingManager.hide();
        }
    }
}
