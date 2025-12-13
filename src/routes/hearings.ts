import { Elysia, t } from "elysia";
import { db } from "../db";
import { hearings } from "../db/schema";
import { eq, desc } from "drizzle-orm";

export const hearingsRoutes = new Elysia({ prefix: "/hearings" })
  // Get all hearings
  .get("/", async ({ set }) => {
    try {
      const allHearings = await db.query.hearings.findMany({
        orderBy: desc(hearings.createdAt),
      });

      return {
        success: true,
        data: allHearings,
      };
    } catch (error: any) {
      console.error("❌ Error fetching hearings:", error);
      set.status = 500;
      return { success: false, message: "Failed to fetch hearings" };
    }
  })

  // Get hearings by report ID
  .get("/report/:reportId", async ({ params, set }) => {
    try {
      const { reportId } = params as any;
      const reportHearings = await db.query.hearings.findMany({
        where: eq(hearings.blotterReportId, parseInt(reportId)),
        orderBy: desc(hearings.createdAt),
      });

      return {
        success: true,
        data: reportHearings,
      };
    } catch (error: any) {
      console.error("❌ Error fetching report hearings:", error);
      set.status = 500;
      return { success: false, message: "Failed to fetch hearings" };
    }
  })

  // Get hearings for specific month
  .get("/calendar", async ({ query, set }) => {
    try {
      const { month, year } = query as any;
      const allHearings = await db.query.hearings.findMany({
        orderBy: desc(hearings.createdAt),
      });

      // Filter by month/year in application
      const filtered = allHearings.filter((h) => {
        const date = new Date(h.hearingDate);
        return date.getMonth() + 1 === parseInt(month) && date.getFullYear() === parseInt(year);
      });

      return {
        success: true,
        data: filtered,
      };
    } catch (error: any) {
      console.error("❌ Error fetching calendar hearings:", error);
      set.status = 500;
      return { success: false, message: "Failed to fetch hearings" };
    }
  })

  // Create hearing
  .post(
    "/",
    async ({ body, set }) => {
      try {
        const { blotterReportId, hearingDate, hearingTime, location, purpose, presider, attendees, notes, status } = body as any;
        
        const [newHearing] = await db
          .insert(hearings)
          .values({
            blotterReportId,
            hearingDate,
            hearingTime,
            location,
            purpose,
            presider,
            attendees,
            notes,
            status: status || "Scheduled",
          })
          .returning();

        console.log(`✅ Hearing created for report ${blotterReportId}`);

        return {
          success: true,
          message: "Hearing created successfully",
          data: newHearing,
        };
      } catch (error: any) {
        console.error("❌ Error creating hearing:", error);
        set.status = 500;
        return { success: false, message: "Failed to create hearing" };
      }
    },
    {
      body: t.Object({
        blotterReportId: t.Number(),
        hearingDate: t.String(),
        hearingTime: t.String(),
        location: t.String(),
        purpose: t.Optional(t.String()),
        presider: t.Optional(t.String()),
        attendees: t.Optional(t.String()),
        notes: t.Optional(t.String()),
        status: t.Optional(t.String()),
      }),
    }
  )

  // Update hearing
  .put(
    "/:id",
    async ({ params, body, set }) => {
      try {
        const hearingId = parseInt(params.id);
        const updateData: any = { };

        if (body.hearingDate) updateData.hearingDate = body.hearingDate;
        if (body.hearingTime) updateData.hearingTime = body.hearingTime;
        if (body.location) updateData.location = body.location;
        if (body.purpose) updateData.purpose = body.purpose;
        if (body.presider) updateData.presider = body.presider;
        if (body.attendees) updateData.attendees = body.attendees;
        if (body.notes) updateData.notes = body.notes;
        if (body.status) updateData.status = body.status;

        const [updatedHearing] = await db
          .update(hearings)
          .set(updateData)
          .where(eq(hearings.id, hearingId))
          .returning();

        if (!updatedHearing) {
          set.status = 404;
          return { success: false, message: "Hearing not found" };
        }

        console.log(`✅ Hearing ${hearingId} updated`);

        return {
          success: true,
          message: "Hearing updated successfully",
          data: updatedHearing,
        };
      } catch (error: any) {
        console.error("❌ Error updating hearing:", error);
        set.status = 500;
        return { success: false, message: "Failed to update hearing" };
      }
    },
    {
      body: t.Partial(
        t.Object({
          hearingDate: t.String(),
          hearingTime: t.String(),
          location: t.String(),
          purpose: t.String(),
          presider: t.String(),
          attendees: t.String(),
          notes: t.String(),
          status: t.String(),
        })
      ),
    }
  )

  // Delete hearing
  .delete("/:id", async ({ params, set }) => {
    try {
      const hearingId = parseInt(params.id);
      const [deletedHearing] = await db
        .delete(hearings)
        .where(eq(hearings.id, hearingId))
        .returning();

      if (!deletedHearing) {
        set.status = 404;
        return { success: false, message: "Hearing not found" };
      }

      console.log(`✅ Hearing ${hearingId} deleted`);

      return {
        success: true,
        message: "Hearing deleted successfully",
      };
    } catch (error: any) {
      console.error("❌ Error deleting hearing:", error);
      set.status = 500;
      return { success: false, message: "Failed to delete hearing" };
    }
  });
