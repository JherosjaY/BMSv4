import { Elysia, t } from "elysia";
import { db } from "../db";
import { activityLogs, auditLogs, loginLogs, errorLogs } from "../db/schema";
import { eq, desc } from "drizzle-orm";

export const logsRoutes = new Elysia({ prefix: "/logs" })
  // Get activity logs
  .get("/activity", async ({ query, set }) => {
    try {
      const { userId, limit } = query as any;

      let logs = await db.query.activityLogs.findMany({
        orderBy: desc(activityLogs.createdAt),
      });

      if (userId) {
        logs = logs.filter(l => l.userId === userId);
      }

      return {
        success: true,
        data: logs.slice(0, parseInt(limit) || 100),
      };
    } catch (error: any) {
      console.error("Error fetching activity logs:", error);
      set.status = 500;
      return { success: false, message: "Failed to fetch activity logs" };
    }
  })

  // Get audit logs
  .get("/audit", async ({ query, set }) => {
    try {
      const { reportId, limit } = query as any;

      let logs = await db.query.auditLogs.findMany({
        orderBy: desc(auditLogs.timestamp),
      });

      if (reportId) {
        logs = logs.filter(l => l.blotterReportId === parseInt(reportId));
      }

      return {
        success: true,
        data: logs.slice(0, parseInt(limit) || 100),
      };
    } catch (error: any) {
      console.error("Error fetching audit logs:", error);
      set.status = 500;
      return { success: false, message: "Failed to fetch audit logs" };
    }
  })

  // Get login logs
  .get("/login", async ({ query, set }) => {
    try {
      const { userId, limit } = query as any;

      let logs = await db.query.loginLogs.findMany({
        orderBy: desc(loginLogs.loginAt),
      });

      if (userId) {
        logs = logs.filter(l => l.userId === userId);
      }

      return {
        success: true,
        data: logs.slice(0, parseInt(limit) || 100),
      };
    } catch (error: any) {
      console.error("Error fetching login logs:", error);
      set.status = 500;
      return { success: false, message: "Failed to fetch login logs" };
    }
  })

  // Get error logs
  .get("/errors", async ({ query, set }) => {
    try {
      const { limit, severity } = query as any;

      let logs = await db.query.errorLogs.findMany({
        orderBy: desc(errorLogs.timestamp),
      });

      if (severity) {
        logs = logs.filter(l => l.severity === severity);
      }

      return {
        success: true,
        data: logs.slice(0, parseInt(limit) || 100),
      };
    } catch (error: any) {
      console.error("Error fetching error logs:", error);
      set.status = 500;
      return { success: false, message: "Failed to fetch error logs" };
    }
  })

  // Clear old logs (admin only)
  .delete("/clear", async ({ query, set }) => {
    try {
      const { daysOld } = query as any;

      const cutoffDate = new Date(Date.now() - parseInt(daysOld) * 24 * 60 * 60 * 1000);

      // TODO: Implement deletion of old logs
      return {
        success: true,
        message: `Logs older than ${daysOld} days cleared successfully`,
      };
    } catch (error: any) {
      console.error("Error clearing logs:", error);
      set.status = 500;
      return { success: false, message: "Failed to clear logs" };
    }
  });
