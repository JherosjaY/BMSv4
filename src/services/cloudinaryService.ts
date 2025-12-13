import { v2 as cloudinary } from "cloudinary";

/**
 * Cloudinary Service - Handles image uploads and management
 * Uses Cloudinary API for cloud storage of images
 */
export class CloudinaryService {
  constructor() {
    // Configure Cloudinary with environment variables
    cloudinary.config({
      cloud_name: process.env.CLOUDINARY_CLOUD_NAME || "",
      api_key: process.env.CLOUDINARY_API_KEY || "",
      api_secret: process.env.CLOUDINARY_API_SECRET || "",
    });
  }

  /**
   * Upload image to Cloudinary
   * @param fileBuffer - Image file buffer
   * @param fileName - Name of the file
   * @param folder - Cloudinary folder path
   * @returns URL of uploaded image
   */
  async uploadImage(
    fileBuffer: Buffer,
    fileName: string,
    folder: string = "bms"
  ): Promise<string> {
    try {
      return new Promise((resolve, reject) => {
        const uploadStream = cloudinary.uploader.upload_stream(
          {
            folder: folder,
            resource_type: "auto",
            public_id: fileName.replace(/\.[^/.]+$/, ""), // Remove extension
          },
          (error, result) => {
            if (error) {
              console.error("❌ Cloudinary upload error:", error);
              reject(error);
            } else {
              console.log(`✅ Image uploaded to Cloudinary: ${result?.secure_url}`);
              resolve(result?.secure_url || "");
            }
          }
        );

        uploadStream.end(fileBuffer);
      });
    } catch (error: any) {
      console.error("❌ Error uploading image:", error);
      throw error;
    }
  }

  /**
   * Upload image from URL
   * @param imageUrl - URL of the image
   * @param folder - Cloudinary folder path
   * @returns URL of uploaded image
   */
  async uploadImageFromUrl(
    imageUrl: string,
    folder: string = "bms"
  ): Promise<string> {
    try {
      const result = await cloudinary.uploader.upload(imageUrl, {
        folder: folder,
        resource_type: "auto",
      });

      console.log(`✅ Image uploaded from URL: ${result.secure_url}`);
      return result.secure_url;
    } catch (error: any) {
      console.error("❌ Error uploading image from URL:", error);
      throw error;
    }
  }

  /**
   * Delete image from Cloudinary
   * @param publicId - Public ID of the image in Cloudinary
   */
  async deleteImage(publicId: string): Promise<boolean> {
    try {
      const result = await cloudinary.uploader.destroy(publicId);

      if (result.result === "ok") {
        console.log(`✅ Image deleted from Cloudinary: ${publicId}`);
        return true;
      } else {
        console.warn(`⚠️ Failed to delete image: ${publicId}`);
        return false;
      }
    } catch (error: any) {
      console.error("❌ Error deleting image:", error);
      throw error;
    }
  }

  /**
   * Get image URL with transformations
   * @param publicId - Public ID of the image
   * @param width - Image width
   * @param height - Image height
   * @param quality - Image quality (1-100)
   */
  getImageUrl(
    publicId: string,
    width?: number,
    height?: number,
    quality: number = 80
  ): string {
    try {
      let url = cloudinary.url(publicId, {
        secure: true,
        quality: quality,
      });

      if (width && height) {
        url = cloudinary.url(publicId, {
          secure: true,
          width: width,
          height: height,
          crop: "fill",
          quality: quality,
        });
      }

      return url;
    } catch (error: any) {
      console.error("❌ Error generating image URL:", error);
      return "";
    }
  }
}

// Export singleton instance
export const cloudinaryService = new CloudinaryService();
