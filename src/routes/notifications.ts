import { Elysia, t } from "elysia";
import { db } from "../db";
import { notifications, users } from "../db/schema";
import { eq, desc } from "drizzle-orm";
import { fcmService } from "../services/fcmService";

export const notificationsRoutes = new Elysia({ prefix: "/notifications" })
  // Get notifications for user
  .get("/:userId", async ({ params, set }) => {
    try {
      const { userId } = params as any;
      const userNotifications = await db.query.notifications.findMany({
        where: eq(notifications.userId, userId),
        orderBy: desc(notifications.createdAt),
      });

      return {
        success: true,
        data: userNotifications,
      };
    } catch (error: any) {
      console.error("Error fetching notifications:", error);
      set.status = 500;
      return { success: false, message: "Failed to fetch notifications" };
    }
  })

  // Mark notification as read
  .put(
    "/:id/read",
    async ({ params, set }) => {
      try {
        const { id } = params as any;
        const [updated] = await db
          .update(notifications)
          .set({ isRead: true, readAt: new Date() })
          .where(eq(notifications.id, parseInt(id)))
          .returning();

        if (!updated) {
          set.status = 404;
          return { success: false, message: "Notification not found" };
        }

        return {
          success: true,
          message: "Notification marked as read",
        };
      } catch (error: any) {
        console.error("Error marking notification as read:", error);
        set.status = 500;
        return { success: false, message: "Failed to mark notification as read" };
      }
    }
  )

  // Delete notification
  .delete("/:id", async ({ params, set }) => {
    try {
      const { id } = params as any;
      const [deleted] = await db
        .delete(notifications)
        .where(eq(notifications.id, parseInt(id)))
        .returning();

      if (!deleted) {
        set.status = 404;
        return { success: false, message: "Notification not found" };
      }

      return {
        success: true,
        message: "Notification deleted successfully",
      };
    } catch (error: any) {
      console.error("Error deleting notification:", error);
      set.status = 500;
      return { success: false, message: "Failed to delete notification" };
    }
  })

  // Send notification (admin/system)
  .post(
    "/",
    async ({ body, set }) => {
      try {
        const { userId, title, message, type, relatedReportId } = body as any;

        const [created] = await db
          .insert(notifications)
          .values({
            userId,
            title,
            message,
            type,
            relatedReportId,
          })
          .returning();

        return {
          success: true,
          message: "Notification sent successfully",
          data: created,
        };
      } catch (error: any) {
        console.error("Error sending notification:", error);
        set.status = 500;
        return { success: false, message: "Failed to send notification" };
      }
    },
    {
      body: t.Object({
        userId: t.String(),
        title: t.String(),
        message: t.String(),
        type: t.String(),
        relatedReportId: t.Optional(t.Number()),
      }),
    }
  )

  // Send push notification via FCM
  .post(
    "/send-push",
    async ({ body, set }) => {
      try {
        const { userId, title, message, type, relatedReportId } = body as any;

        // Get user's FCM token
        const user = await db.query.users.findFirst({
          where: eq(users.id, userId),
        });

        if (!user) {
          set.status = 404;
          return { success: false, message: "User not found" };
        }

        if (!user.fcmToken) {
          console.warn(`⚠️ User ${userId} has no FCM token`);
          set.status = 400;
          return { success: false, message: "User has no FCM token registered" };
        }

        // Store notification in database
        const [created] = await db
          .insert(notifications)
          .values({
            userId,
            title,
            message,
            type,
            relatedReportId,
          })
          .returning();

        // Send push notification via FCM
        try {
          const customData: Record<string, string> = {
            notificationId: String(created.id),
            type: type,
          };

          if (relatedReportId) {
            customData.relatedReportId = String(relatedReportId);
          }

          await fcmService.sendDataNotification(
            user.fcmToken,
            title,
            message,
            customData
          );

          console.log(`✅ Push notification sent to user ${userId}`);
        } catch (fcmError: any) {
          console.warn(`⚠️ FCM error (notification still stored): ${fcmError.message}`);
          // Don't fail the request - notification is stored in database
        }

        return {
          success: true,
          message: "Notification sent successfully",
          data: created,
        };
      } catch (error: any) {
        console.error("❌ Error sending push notification:", error);
        set.status = 500;
        return { success: false, message: "Failed to send push notification" };
      }
    },
    {
      body: t.Object({
        userId: t.String(),
        title: t.String(),
        message: t.String(),
        type: t.String(),
        relatedReportId: t.Optional(t.Number()),
      }),
    }
  );
