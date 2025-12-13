package com.example.blottermanagementsystem.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.Executors;

/**
 * CloudinaryUploadManager - Handle profile picture uploads to Cloudinary
 */
public class CloudinaryUploadManager {
    private static final String TAG = "CloudinaryUploadManager";
    private final Context context;

    public interface UploadCallback {
        void onSuccess(String cloudinaryUrl);
        void onError(String errorMessage);
    }

    public CloudinaryUploadManager(Context context) {
        this.context = context;
    }

    /**
     * Upload image from URI to Cloudinary
     */
    public void uploadProfilePicture(Uri imageUri, UploadCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Convert image URI to base64
                String base64Image = convertImageToBase64(imageUri);
                
                if (base64Image == null || base64Image.isEmpty()) {
                    callback.onError("Failed to convert image to base64");
                    return;
                }

                Log.d(TAG, "📤 Uploading profile picture to Cloudinary...");

                // Call backend upload endpoint
                ApiClient.uploadProfilePicture(base64Image, new ApiClient.ApiCallback<Object>() {
                    @Override
                    public void onSuccess(Object response) {
                        try {
                            // Parse response to get Cloudinary URL
                            if (response instanceof java.util.Map) {
                                java.util.Map<String, Object> responseMap = (java.util.Map<String, Object>) response;
                                java.util.Map<String, Object> dataMap = (java.util.Map<String, Object>) responseMap.get("data");
                                String cloudinaryUrl = (String) dataMap.get("imageUrl");
                                
                                if (cloudinaryUrl != null && !cloudinaryUrl.isEmpty()) {
                                    Log.d(TAG, "✅ Profile picture uploaded successfully: " + cloudinaryUrl);
                                    callback.onSuccess(cloudinaryUrl);
                                } else {
                                    callback.onError("No image URL in response");
                                }
                            } else {
                                callback.onError("Invalid response format");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing upload response: " + e.getMessage());
                            callback.onError("Error parsing response: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "❌ Upload failed: " + errorMessage);
                        callback.onError(errorMessage);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error uploading profile picture: " + e.getMessage());
                callback.onError(e.getMessage());
            }
        });
    }

    /**
     * Convert image URI to base64 string
     */
    private String convertImageToBase64(Uri imageUri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            
            // Decode with inJustDecodeBounds to check dimensions
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            // Calculate inSampleSize to reduce memory usage
            int imageHeight = options.outHeight;
            int imageWidth = options.outWidth;
            int inSampleSize = 1;
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
            inputStream = context.getContentResolver().openInputStream(imageUri);
            options.inJustDecodeBounds = false;
            options.inSampleSize = inSampleSize;
            options.inPreferredConfig = Bitmap.Config.RGB_565;

            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            if (bitmap != null) {
                // Convert bitmap to base64
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream);
                byte[] byteArray = byteArrayOutputStream.toByteArray();
                
                Log.d(TAG, "Image size: " + byteArray.length + " bytes");
                
                return Base64.encodeToString(byteArray, Base64.DEFAULT);
            } else {
                Log.e(TAG, "Failed to decode bitmap");
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error converting image to base64: " + e.getMessage());
            return null;
        }
    }
}
