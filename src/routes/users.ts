import { Elysia, t } from "elysia";
import { db } from "../db";
import { users } from "../db/schema";
import { eq } from "drizzle-orm";
import { UpdateUserRequest, SaveFCMTokenRequest } from "../models";
import { cloudinaryService } from "../services/cloudinaryService";

export const usersRoutes = new Elysia({ prefix: "/users" })
  .get("/", async () => {
    const allUsers = await db.query.users.findMany({
      columns: {
        password: false,
      },
    });

    return {
      success: true,
      data: allUsers,
    };
  })

  .get("/:id", async ({ params, set }) => {
    const user = await db.query.users.findFirst({
      where: eq(users.id, params.id),
      columns: {
        password: false,
      },
    });

    if (!user) {
      set.status = 404;
      return { success: false, message: "User not found" };
    }

    return {
      success: true,
      data: user,
    };
  })

  .put(
    "/:id",
    async ({ params, body, set }) => {
      const [updatedUser] = await db
        .update(users)
        .set({ ...body, updatedAt: new Date() })
        .where(eq(users.id, params.id))
        .returning({
          id: users.id,
          username: users.username,
          firstName: users.firstName,
          lastName: users.lastName,
          role: users.role,
          isActive: users.isActive,
        });

      if (!updatedUser) {
        set.status = 404;
        return { success: false, message: "User not found" };
      }

      return {
        success: true,
        data: updatedUser,
      };
    },
    {
      body: t.Partial(
        t.Object({
          firstName: t.String(),
          lastName: t.String(),
          isActive: t.Boolean(),
          profilePhotoUri: t.String(),
        })
      ),
    }
  )

  .delete("/:id", async ({ params, set }) => {
    const [deletedUser] = await db
      .delete(users)
      .where(eq(users.id, params.id))
      .returning();

    if (!deletedUser) {
      set.status = 404;
      return { success: false, message: "User not found" };
    }

    return {
      success: true,
      message: "User deleted successfully",
    };
  })

  .post(
    "/fcm-token",
    async ({ body, set }) => {
      try {
        const { userId, fcmToken, deviceId } = body as SaveFCMTokenRequest;

        const [updatedUser] = await db
          .update(users)
          .set({
            fcmToken,
            deviceId,
            updatedAt: new Date(),
          })
          .where(eq(users.id, String(userId)))
          .returning();

        if (!updatedUser) {
          set.status = 404;
          return { success: false, message: "User not found" };
        }

        console.log(`✅ FCM token saved for user ${userId}`);

        return {
          success: true,
          message: "FCM token saved successfully",
        };
      } catch (error) {
        set.status = 500;
        return {
          success: false,
          message: "Failed to save FCM token",
        };
      }
    },
    {
      body: t.Object({
        userId: t.Number(),
        fcmToken: t.String(),
        deviceId: t.Optional(t.String()),
      }),
    }
  )

  // Change password
  .post(
    "/:id/change-password",
    async ({ params, body, set }) => {
      try {
        const { oldPassword, newPassword } = body as any;
        const user = await db.query.users.findFirst({
          where: eq(users.id, params.id),
        });

        if (!user) {
          set.status = 404;
          return { success: false, message: "User not found" };
        }

        // Verify old password with bcrypt
        const bcrypt = require("bcryptjs");
        const isPasswordValid = await bcrypt.compare(oldPassword, user.password);

        if (!isPasswordValid) {
          set.status = 401;
          return { success: false, message: "Current password is incorrect" };
        }

        // Hash and update new password
        const hashedPassword = await bcrypt.hash(newPassword, 10);

        await db
          .update(users)
          .set({ password: hashedPassword, updatedAt: new Date() })
          .where(eq(users.id, params.id));

        console.log(`✅ Password changed for user ${params.id}`);

        return {
          success: true,
          message: "Password changed successfully",
        };
      } catch (error: any) {
        console.error("Error changing password:", error);
        set.status = 500;
        return { success: false, message: "Failed to change password" };
      }
    },
    {
      body: t.Object({
        oldPassword: t.String(),
        newPassword: t.String(),
      }),
    }
  )

  // Update user settings
  .put(
    "/:id/settings",
    async ({ params, body, set }) => {
      try {
        const [updated] = await db
          .update(users)
          .set({ ...body, updatedAt: new Date() })
          .where(eq(users.id, params.id))
          .returning();

        if (!updated) {
          set.status = 404;
          return { success: false, message: "User not found" };
        }

        return {
          success: true,
          message: "Settings updated successfully",
          data: updated,
        };
      } catch (error: any) {
        console.error("Error updating settings:", error);
        set.status = 500;
        return { success: false, message: "Failed to update settings" };
      }
    },
    {
      body: t.Partial(
        t.Object({
          notificationsEnabled: t.Boolean(),
          theme: t.String(),
          language: t.String(),
        })
      ),
    }
  )

  // Upload profile photo
  .post(
    "/:id/upload-photo",
    async ({ params, body, set }) => {
      try {
        const userId = params.id;
        const { imageUrl } = body as any;

        // Verify user exists
        const user = await db.query.users.findFirst({
          where: eq(users.id, userId),
        });

        if (!user) {
          set.status = 404;
          return { success: false, message: "User not found" };
        }

        // Upload image to Cloudinary
        const cloudinaryUrl = await cloudinaryService.uploadImageFromUrl(
          imageUrl,
          `bms/profiles/${userId}`
        );

        // Update user profile photo in database
        const [updatedUser] = await db
          .update(users)
          .set({
            profilePhoto: cloudinaryUrl,
            updatedAt: new Date(),
          })
          .where(eq(users.id, userId))
          .returning({
            id: users.id,
            firstName: users.firstName,
            lastName: users.lastName,
            profilePhoto: users.profilePhoto,
          });

        console.log(`✅ Profile photo uploaded for user ${userId}`);

        return {
          success: true,
          message: "Profile photo uploaded successfully",
          data: {
            photoUrl: updatedUser?.profilePhoto,
          },
        };
      } catch (error: any) {
        console.error("❌ Error uploading profile photo:", error);
        set.status = 500;
        return {
          success: false,
          message: "Failed to upload profile photo",
          error: error.message,
        };
      }
    },
    {
      body: t.Object({
        imageUrl: t.String(),
      }),
    }
  );
