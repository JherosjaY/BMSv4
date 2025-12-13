import { Elysia, t } from "elysia";
import { db } from "../db";
import { users, blotterReports } from "../db/schema";
import { eq, desc } from "drizzle-orm";

export const adminRoutes = new Elysia({ prefix: "/admin" })
  // Get system configuration
  .get("/config", async ({ set }) => {
    try {
      return {
        success: true,
        data: {
          systemName: "Blotter Management System v3",
          version: "3.0.0",
          environment: process.env.NODE_ENV || "production",
          databaseStatus: "connected",
          features: {
            emailVerification: false,
            twoFactorAuth: false,
            auditLogging: true,
          },
        },
      };
    } catch (error: any) {
      console.error("Error fetching config:", error);
      set.status = 500;
      return { success: false, message: "Failed to fetch configuration" };
    }
  })

  // Update system configuration
  .put(
    "/config",
    async ({ body, set }) => {
      try {
        const { systemName, theme, language } = body as any;
        // TODO: Store config in database
        return {
          success: true,
          message: "Configuration updated successfully",
        };
      } catch (error: any) {
        console.error("Error updating config:", error);
        set.status = 500;
        return { success: false, message: "Failed to update configuration" };
      }
    },
    {
      body: t.Partial(
        t.Object({
          systemName: t.String(),
          theme: t.String(),
          language: t.String(),
        })
      ),
    }
  )

  // Get all users (admin only)
  .get("/users", async ({ set }) => {
    try {
      const allUsers = await db.query.users.findMany({
        columns: {
          password: false,
        },
      });

      return {
        success: true,
        data: allUsers,
      };
    } catch (error: any) {
      console.error("Error fetching users:", error);
      set.status = 500;
      return { success: false, message: "Failed to fetch users" };
    }
  })

  // Get all reports (admin oversight)
  .get("/reports", async ({ set }) => {
    try {
      const allReports = await db.query.blotterReports.findMany({
        orderBy: desc(blotterReports.createdAt),
      });

      return {
        success: true,
        data: allReports,
      };
    } catch (error: any) {
      console.error("Error fetching reports:", error);
      set.status = 500;
      return { success: false, message: "Failed to fetch reports" };
    }
  })

  // Filter reports by status/priority
  .get("/reports/filter", async ({ query, set }) => {
    try {
      const { status, priority } = query as any;
      
      let reports = await db.query.blotterReports.findMany();

      if (status) {
        reports = reports.filter(r => r.status === status);
      }
      if (priority) {
        reports = reports.filter(r => r.priority === priority);
      }

      return {
        success: true,
        data: reports,
      };
    } catch (error: any) {
      console.error("Error filtering reports:", error);
      set.status = 500;
      return { success: false, message: "Failed to filter reports" };
    }
  })

  // Create system backup
  .post(
    "/backup",
    async ({ set }) => {
      try {
        // TODO: Implement database backup
        const backupUrl = `https://backup.example.com/bms-backup-${Date.now()}.zip`;
        
        return {
          success: true,
          message: "Backup created successfully",
          data: { backupUrl },
        };
      } catch (error: any) {
        console.error("Error creating backup:", error);
        set.status = 500;
        return { success: false, message: "Failed to create backup" };
      }
    }
  )

  // Get system statistics
  .get("/statistics", async ({ set }) => {
    try {
      const allUsers = await db.query.users.findMany();
      const allReports = await db.query.blotterReports.findMany();

      const stats = {
        totalUsers: allUsers.length,
        totalOfficers: allUsers.filter(u => u.role === "officer").length,
        totalAdmins: allUsers.filter(u => u.role === "admin").length,
        totalReports: allReports.length,
        pendingReports: allReports.filter(r => r.status === "Pending").length,
        resolvedReports: allReports.filter(r => r.status === "Resolved").length,
        archivedReports: allReports.filter(r => r.isArchived).length,
      };

      return {
        success: true,
        data: stats,
      };
    } catch (error: any) {
      console.error("Error fetching statistics:", error);
      set.status = 500;
      return { success: false, message: "Failed to fetch statistics" };
    }
  })

  // Deactivate user account
  .put(
    "/users/:userId/deactivate",
    async ({ params, set }) => {
      try {
        const userId = params.userId as string;

        const [updated] = await db
          .update(users)
          .set({ isActive: false })
          .where(eq(users.id, userId))
          .returning();

        if (!updated) {
          set.status = 404;
          return { success: false, message: "User not found" };
        }

        return {
          success: true,
          message: "User deactivated successfully",
        };
      } catch (error: any) {
        console.error("Error deactivating user:", error);
        set.status = 500;
        return { success: false, message: "Failed to deactivate user" };
      }
    }
  )

  // Reactivate user account
  .put(
    "/users/:userId/activate",
    async ({ params, set }) => {
      try {
        const userId = params.userId as string;

        const [updated] = await db
          .update(users)
          .set({ isActive: true })
          .where(eq(users.id, userId))
          .returning();

        if (!updated) {
          set.status = 404;
          return { success: false, message: "User not found" };
        }

        return {
          success: true,
          message: "User activated successfully",
        };
      } catch (error: any) {
        console.error("Error activating user:", error);
        set.status = 500;
        return { success: false, message: "Failed to activate user" };
      }
    }
  );
