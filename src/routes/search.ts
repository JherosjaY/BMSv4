import { Elysia, t } from "elysia";
import { db } from "../db";
import { blotterReports } from "../db/schema";
import { like, or } from "drizzle-orm";

export const searchRoutes = new Elysia({ prefix: "/search" })
  // Search reports by case number, incident type, or location
  .get("/reports", async ({ query, set }) => {
    try {
      const { q, type, status, priority } = query as any;
      
      let reports = await db.query.blotterReports.findMany();

      // Filter by search query
      if (q) {
        const searchTerm = q.toLowerCase();
        reports = reports.filter(r =>
          r.caseNumber?.toLowerCase().includes(searchTerm) ||
          r.incidentType?.toLowerCase().includes(searchTerm) ||
          r.incidentLocation?.toLowerCase().includes(searchTerm) ||
          r.narrative?.toLowerCase().includes(searchTerm)
        );
      }

      // Filter by type
      if (type) {
        reports = reports.filter(r => r.incidentType === type);
      }

      // Filter by status
      if (status) {
        reports = reports.filter(r => r.status === status);
      }

      // Filter by priority
      if (priority) {
        reports = reports.filter(r => r.priority === priority);
      }

      return {
        success: true,
        data: reports,
      };
    } catch (error: any) {
      console.error("Error searching reports:", error);
      set.status = 500;
      return { success: false, message: "Failed to search reports" };
    }
  })

  // Advanced search with multiple filters
  .post(
    "/advanced",
    async ({ body, set }) => {
      try {
        const {
          caseNumber,
          incidentType,
          dateFrom,
          dateTo,
          status,
          priority,
          location,
        } = body as any;

        let reports = await db.query.blotterReports.findMany();

        // Apply filters
        if (caseNumber) {
          reports = reports.filter(r =>
            r.caseNumber?.includes(caseNumber)
          );
        }
        if (incidentType) {
          reports = reports.filter(r => r.incidentType === incidentType);
        }
        if (status) {
          reports = reports.filter(r => r.status === status);
        }
        if (priority) {
          reports = reports.filter(r => r.priority === priority);
        }
        if (location) {
          reports = reports.filter(r =>
            r.incidentLocation?.toLowerCase().includes(location.toLowerCase())
          );
        }

        return {
          success: true,
          data: reports,
          count: reports.length,
        };
      } catch (error: any) {
        console.error("Error in advanced search:", error);
        set.status = 500;
        return { success: false, message: "Failed to perform advanced search" };
      }
    },
    {
      body: t.Partial(
        t.Object({
          caseNumber: t.String(),
          incidentType: t.String(),
          dateFrom: t.String(),
          dateTo: t.String(),
          status: t.String(),
          priority: t.String(),
          location: t.String(),
        })
      ),
    }
  )

  // Get incident types (for filtering)
  .get("/incident-types", async ({ set }) => {
    try {
      const reports = await db.query.blotterReports.findMany();
      const types = [...new Set(reports.map(r => r.incidentType))].filter(
        t => t && t.trim() !== ""
      );

      return {
        success: true,
        data: types,
      };
    } catch (error: any) {
      console.error("Error fetching incident types:", error);
      set.status = 500;
      return { success: false, message: "Failed to fetch incident types" };
    }
  })

  // Get report statuses
  .get("/statuses", async ({ set }) => {
    try {
      const statuses = ["Pending", "In Progress", "Resolved", "Closed"];
      return {
        success: true,
        data: statuses,
      };
    } catch (error: any) {
      console.error("Error fetching statuses:", error);
      set.status = 500;
      return { success: false, message: "Failed to fetch statuses" };
    }
  })

  // Get report priorities
  .get("/priorities", async ({ set }) => {
    try {
      const priorities = ["Low", "Normal", "High", "Critical"];
      return {
        success: true,
        data: priorities,
      };
    } catch (error: any) {
      console.error("Error fetching priorities:", error);
      set.status = 500;
      return { success: false, message: "Failed to fetch priorities" };
    }
  });
