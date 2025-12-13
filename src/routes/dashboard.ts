import { Elysia, t } from "elysia";
import { db } from "../db";
import { blotterReports, users } from "../db/schema";

export const dashboardRoutes = new Elysia({ prefix: "/dashboard" })
  // Get admin dashboard summary
  .get("/admin", async ({ set }) => {
    try {
      const allReports = await db.query.blotterReports.findMany();
      const allUsers = await db.query.users.findMany();

      const summary = {
        totalReports: allReports.length,
        pendingReports: allReports.filter(r => r.status === "Pending").length,
        inProgressReports: allReports.filter(r => r.status === "In Progress")
          .length,
        resolvedReports: allReports.filter(r => r.status === "Resolved").length,
        totalUsers: allUsers.length,
        totalOfficers: allUsers.filter(u => u.role === "officer").length,
        activeUsers: allUsers.filter(u => u.isActive).length,
        recentReports: allReports.slice(0, 5),
      };

      return {
        success: true,
        data: summary,
      };
    } catch (error: any) {
      console.error("Error fetching admin dashboard:", error);
      set.status = 500;
      return { success: false, message: "Failed to fetch dashboard" };
    }
  })

  // Get officer dashboard summary
  .get("/officer/:userId", async ({ params, set }) => {
    try {
      const { userId } = params as any;

      const allReports = await db.query.blotterReports.findMany();
      // TODO: Filter by assigned officer
      const assignedReports = allReports;

      const summary = {
        assignedCases: assignedReports.length,
        pendingCases: assignedReports.filter(r => r.status === "Pending")
          .length,
        inProgressCases: assignedReports.filter(r => r.status === "In Progress")
          .length,
        resolvedCases: assignedReports.filter(r => r.status === "Resolved")
          .length,
        recentCases: assignedReports.slice(0, 5),
        resolutionRate:
          assignedReports.length > 0
            ? (
                (assignedReports.filter(r => r.status === "Resolved").length /
                  assignedReports.length) *
                100
              ).toFixed(2)
            : 0,
      };

      return {
        success: true,
        data: summary,
      };
    } catch (error: any) {
      console.error("Error fetching officer dashboard:", error);
      set.status = 500;
      return { success: false, message: "Failed to fetch dashboard" };
    }
  })

  // Get quick stats
  .get("/stats", async ({ set }) => {
    try {
      const allReports = await db.query.blotterReports.findMany();

      const stats = {
        today: {
          newReports: allReports.filter(r => {
            // TODO: Filter by today's date
            return true;
          }).length,
        },
        thisWeek: {
          newReports: allReports.length,
        },
        thisMonth: {
          newReports: allReports.length,
          resolvedReports: allReports.filter(r => r.status === "Resolved")
            .length,
        },
      };

      return {
        success: true,
        data: stats,
      };
    } catch (error: any) {
      console.error("Error fetching stats:", error);
      set.status = 500;
      return { success: false, message: "Failed to fetch statistics" };
    }
  })

  // Get pending actions (for quick view)
  .get("/pending-actions", async ({ set }) => {
    try {
      const allReports = await db.query.blotterReports.findMany();

      const actions = {
        pendingReports: allReports.filter(r => r.status === "Pending").length,
        overdueReports: 0, // TODO: Calculate overdue
        unassignedReports: allReports.filter(r => !r.assignedOfficer).length,
        hearingsToday: 0, // TODO: Get hearings for today
      };

      return {
        success: true,
        data: actions,
      };
    } catch (error: any) {
      console.error("Error fetching pending actions:", error);
      set.status = 500;
      return { success: false, message: "Failed to fetch pending actions" };
    }
  });
