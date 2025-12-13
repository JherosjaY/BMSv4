import { Elysia, t } from "elysia";
import { v2 as cloudinary } from "cloudinary";

// Configure Cloudinary
cloudinary.config({
  cloud_name: process.env.CLOUDINARY_CLOUD_NAME,
  api_key: process.env.CLOUDINARY_API_KEY,
  api_secret: process.env.CLOUDINARY_API_SECRET,
});

export const uploadRoutes = new Elysia({ prefix: "/upload" })
  // Upload profile picture
  .post(
    "/profile-picture",
    async ({ body, set }) => {
      try {
        const { imageBase64 } = body as any;
        
        if (!imageBase64) {
          set.status = 400;
          return { success: false, message: "No image provided" };
        }

        // Upload to Cloudinary
        const result = await cloudinary.uploader.upload(
          `data:image/jpeg;base64,${imageBase64}`,
          {
            folder: "bms/profile-pictures",
            resource_type: "auto",
            overwrite: true,
          }
        );

        console.log(`✅ Profile picture uploaded: ${result.secure_url}`);
        
        return {
          success: true,
          message: "Profile picture uploaded successfully",
          data: { imageUrl: result.secure_url },
        };
      } catch (error: any) {
        console.error("Error uploading profile picture:", error);
        set.status = 500;
        return { success: false, message: "Failed to upload profile picture" };
      }
    }
  )

  // Upload report evidence/attachment
  .post(
    "/evidence",
    async ({ body, set }) => {
      try {
        // TODO: Integrate with Cloudinary
        const fileUrl = `https://cloudinary.example.com/evidence-${Date.now()}.pdf`;
        
        return {
          success: true,
          message: "Evidence uploaded successfully",
          data: { fileUrl },
        };
      } catch (error: any) {
        console.error("Error uploading evidence:", error);
        set.status = 500;
        return { success: false, message: "Failed to upload evidence" };
      }
    }
  )

  // Upload audio recording
  .post(
    "/audio",
    async ({ body, set }) => {
      try {
        // TODO: Integrate with Cloudinary
        const audioUrl = `https://cloudinary.example.com/audio-${Date.now()}.mp3`;
        
        return {
          success: true,
          message: "Audio uploaded successfully",
          data: { audioUrl },
        };
      } catch (error: any) {
        console.error("Error uploading audio:", error);
        set.status = 500;
        return { success: false, message: "Failed to upload audio" };
      }
    }
  );
