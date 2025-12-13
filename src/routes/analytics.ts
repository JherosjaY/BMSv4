import { Elysia, t } from "elysia";
import { db } from "../db";
import { blotterReports } from "../db/schema";
import { eq, and } from "drizzle-orm";

export const analyticsRoutes = new Elysia({ prefix: "/analytics" })
  // Get dashboard analytics
  .get("/dashboard", async ({ set }) => {
    try {
      const allReports = await db.query.blotterReports.findMany();
      
      const totalReports = allReports.length;
      const pendingReports = allReports.filter(r => r.status === "Pending").length;
      const resolvedReports = allReports.filter(r => r.status === "Resolved").length;
      const archivedReports = allReports.filter(r => r.isArchived).length;

      return {
        success: true,
        data: {
          totalReports,
          pendingReports,
          resolvedReports,
          archivedReports,
          activeReports: totalReports - archivedReports,
        },
      };
    } catch (error: any) {
      console.error("Error fetching dashboard analytics:", error);
      set.status = 500;
      return { success: false, message: "Failed to fetch analytics" };
    }
  })

  // Get officer analytics
  .get("/officer/:userId", async ({ params, set }) => {
    try {
      // TODO: Filter reports assigned to this officer
      const officerReports = await db.query.blotterReports.findMany();
      
      const assignedCases = officerReports.length;
      const resolvedCases = officerReports.filter(r => r.status === "Resolved").length;
      const pendingCases = officerReports.filter(r => r.status === "Pending").length;

      return {
        success: true,
        data: {
          assignedCases,
          resolvedCases,
          pendingCases,
          resolutionRate: assignedCases > 0 ? (resolvedCases / assignedCases * 100).toFixed(2) : 0,
        },
      };
    } catch (error: any) {
      console.error("Error fetching officer analytics:", error);
      set.status = 500;
      return { success: false, message: "Failed to fetch officer analytics" };
    }
  })

  // Get reports by date range
  .get("/officer/:userId/reports", async ({ params, query, set }) => {
    try {
      const { startDate, endDate } = query as any;
      
      // TODO: Filter by date range and officer
      const reports = await db.query.blotterReports.findMany();

      return {
        success: true,
        data: reports,
      };
    } catch (error: any) {
      console.error("Error fetching reports by date:", error);
      set.status = 500;
      return { success: false, message: "Failed to fetch reports" };
    }
  });
