import { Elysia, t } from "elysia";
import { db } from "../db";
import { blotterReports } from "../db/schema";
import { eq, desc } from "drizzle-orm";
import { CreateBlotterReportRequest, UpdateBlotterReportRequest } from "../models";

export const reportsRoutes = new Elysia({ prefix: "/reports" })
  .get("/", async () => {
    const reports = await db.query.blotterReports.findMany({
      orderBy: desc(blotterReports.createdAt),
    });

    return {
      success: true,
      data: reports,
    };
  })

  .get("/:id", async ({ params, set }) => {
    const report = await db.query.blotterReports.findFirst({
      where: eq(blotterReports.id, parseInt(params.id)),
    });

    if (!report) {
      set.status = 404;
      return { success: false, message: "Report not found" };
    }

    return {
      success: true,
      data: report,
    };
  })

  .post(
    "/",
    async ({ body, set }) => {
      try {
        // Only extract fields that exist in the database schema
        const reportData: any = {
          caseNumber: body.caseNumber,
          incidentType: body.incidentType,
          incidentDate: String(body.incidentDate), // Convert number to string if needed
          incidentTime: body.incidentTime,
          incidentLocation: body.incidentLocation,
          narrative: body.narrative,
          status: body.status || "Pending",
          priority: body.priority || "Normal",
          isArchived: false,
        };

        // Add optional fields only if they're provided
        if (body.complainantName) reportData.complainantName = body.complainantName;
        if (body.complainantContact) reportData.complainantContact = body.complainantContact;
        if (body.complainantAddress) reportData.complainantAddress = body.complainantAddress;
        if (body.complainantEmail) reportData.complainantEmail = body.complainantEmail;
        if (body.assignedOfficer) reportData.assignedOfficer = body.assignedOfficer;
        if (body.assignedOfficerIds) reportData.assignedOfficerIds = body.assignedOfficerIds;
        if (body.filedBy) reportData.filedBy = body.filedBy;
        if (body.filedById) reportData.filedById = body.filedById;

        console.log("📝 Inserting report with data:", JSON.stringify(reportData, null, 2));

        const [newReport] = await db
          .insert(blotterReports)
          .values(reportData)
          .returning();

        console.log(`✅ New case filed: ${newReport.caseNumber}`);

        return {
          success: true,
          data: newReport,
        };
      } catch (error) {
        console.error("❌ Error creating report:", error);
        set.status = 500;
        return {
          success: false,
          error: error instanceof Error ? error.message : "Unknown error",
          details: error instanceof Error ? error.stack : null,
        };
      }
    },
    {
      body: t.Object({
        caseNumber: t.String(),
        incidentType: t.String(),
        incidentDate: t.Union([t.String(), t.Number()]),
        incidentTime: t.String(),
        incidentLocation: t.String(),
        narrative: t.String(),
        complainantName: t.Optional(t.String()),
        complainantContact: t.Optional(t.String()),
        complainantAddress: t.Optional(t.String()),
        complainantEmail: t.Optional(t.String()),
        respondentName: t.Optional(t.String()),
        respondentAlias: t.Optional(t.String()),
        respondentAddress: t.Optional(t.String()),
        respondentContact: t.Optional(t.String()),
        accusation: t.Optional(t.String()),
        relationshipToComplainant: t.Optional(t.String()),
        status: t.Optional(t.String()),
        priority: t.Optional(t.String()),
        assignedOfficer: t.Optional(t.String()),
        assignedOfficerIds: t.Optional(t.String()),
        filedBy: t.Optional(t.String()),
        filedById: t.Optional(t.Number()),
        userId: t.Optional(t.Number()),
        dateFiled: t.Optional(t.Number()),
        imageUris: t.Optional(t.String()),
        videoUris: t.Optional(t.String()),
        videoDurations: t.Optional(t.String()),
        audioUri: t.Optional(t.String()),
        audioUris: t.Optional(t.String()),
        audioDurations: t.Optional(t.String()),
        latitude: t.Optional(t.Number()),
        longitude: t.Optional(t.Number()),
      }),
    }
  )

  .put(
    "/:id",
    async ({ params, body, set }) => {
      try {
        const reportId = parseInt(params.id);
        
        const oldReport = await db.query.blotterReports.findFirst({
          where: eq(blotterReports.id, reportId),
        });

        if (!oldReport) {
          set.status = 404;
          return { success: false, message: "Report not found" };
        }

        // Filter body to only include valid fields
        const updateData: any = { updatedAt: new Date() };
        
        if (body.incidentType) updateData.incidentType = body.incidentType;
        if (body.incidentDate) updateData.incidentDate = String(body.incidentDate);
        if (body.incidentTime) updateData.incidentTime = body.incidentTime;
        if (body.incidentLocation) updateData.incidentLocation = body.incidentLocation;
        if (body.narrative) updateData.narrative = body.narrative;
        if (body.complainantName) updateData.complainantName = body.complainantName;
        if (body.complainantContact) updateData.complainantContact = body.complainantContact;
        if (body.complainantAddress) updateData.complainantAddress = body.complainantAddress;
        if (body.complainantEmail) updateData.complainantEmail = body.complainantEmail;
        if (body.status) updateData.status = body.status;
        if (body.priority) updateData.priority = body.priority;
        if (body.assignedOfficer) updateData.assignedOfficer = body.assignedOfficer;
        if (body.assignedOfficerIds) updateData.assignedOfficerIds = body.assignedOfficerIds;
        if (body.isArchived !== undefined) updateData.isArchived = body.isArchived;

        const [updatedReport] = await db
          .update(blotterReports)
          .set(updateData)
          .where(eq(blotterReports.id, reportId))
          .returning();

        console.log(`✅ Report updated: ${updatedReport.caseNumber}`);

        return {
          success: true,
          data: updatedReport,
        };
      } catch (error: any) {
        console.error("❌ Error updating report:", error);
        set.status = 500;
        return {
          success: false,
          message: "Failed to update report",
          error: error instanceof Error ? error.message : "Unknown error",
        };
      }
    },
    {
      body: t.Partial(
        t.Object({
          incidentType: t.Optional(t.String()),
          incidentDate: t.Optional(t.Union([t.String(), t.Number()])),
          incidentTime: t.Optional(t.String()),
          incidentLocation: t.Optional(t.String()),
          narrative: t.Optional(t.String()),
          complainantName: t.Optional(t.String()),
          complainantContact: t.Optional(t.String()),
          complainantAddress: t.Optional(t.String()),
          complainantEmail: t.Optional(t.String()),
          status: t.Optional(t.String()),
          priority: t.Optional(t.String()),
          assignedOfficer: t.Optional(t.String()),
          assignedOfficerIds: t.Optional(t.String()),
          isArchived: t.Optional(t.Boolean()),
        })
      ),
    }
  )

  .delete("/:id", async ({ params, set }) => {
    const [deletedReport] = await db
      .delete(blotterReports)
      .where(eq(blotterReports.id, parseInt(params.id)))
      .returning();

    if (!deletedReport) {
      set.status = 404;
      return { success: false, message: "Report not found" };
    }

    return {
      success: true,
      message: "Report deleted successfully",
    };
  })

  .get("/status/:status", async ({ params }) => {
    const reports = await db.query.blotterReports.findMany({
      where: eq(blotterReports.status, params.status),
      orderBy: desc(blotterReports.createdAt),
    });

    return {
      success: true,
      data: reports,
    };
  })

  // Assign report to officer
  .put(
    "/:id/assign",
    async ({ params, body, set }) => {
      try {
        const [updated] = await db
          .update(blotterReports)
          .set({
            assignedOfficer: (body as any).assignedOfficerName,
            assignedOfficerIds: (body as any).assignedOfficerId,
          })
          .where(eq(blotterReports.id, parseInt(params.id)))
          .returning();

        if (!updated) {
          set.status = 404;
          return { success: false, message: "Report not found" };
        }

        return {
          success: true,
          message: "Report assigned successfully",
          data: updated,
        };
      } catch (error: any) {
        console.error("Error assigning report:", error);
        set.status = 500;
        return { success: false, message: "Failed to assign report" };
      }
    },
    {
      body: t.Object({
        assignedOfficerId: t.String(),
        assignedOfficerName: t.String(),
      }),
    }
  )

  // Archive report
  .put(
    "/:id/archive",
    async ({ params, set }) => {
      try {
        const [updated] = await db
          .update(blotterReports)
          .set({ isArchived: true })
          .where(eq(blotterReports.id, parseInt(params.id)))
          .returning();

        if (!updated) {
          set.status = 404;
          return { success: false, message: "Report not found" };
        }

        return {
          success: true,
          message: "Report archived successfully",
          data: updated,
        };
      } catch (error: any) {
        console.error("Error archiving report:", error);
        set.status = 500;
        return { success: false, message: "Failed to archive report" };
      }
    }
  );
