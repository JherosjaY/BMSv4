import { Elysia, t } from "elysia";
import { db } from "../db";

export const resolutionsRoutes = new Elysia({ prefix: "/reports/:id/resolution" })
  // Get resolution for report
  .get("/", async ({ params, set }) => {
    try {
      const { id } = params as any;
      // TODO: Fetch resolution from database
      return {
        success: true,
        data: null,
      };
    } catch (error: any) {
      console.error("Error fetching resolution:", error);
      set.status = 500;
      return { success: false, message: "Failed to fetch resolution" };
    }
  })

  // Create resolution/close case
  .post(
    "/",
    async ({ params, body, set }) => {
      try {
        const { reportId } = params as any;
        const { resolutionType, description, closedBy, closedDate } = body as any;

        // TODO: Save resolution to database
        // Update report status to Resolved/Closed
        return {
          success: true,
          message: "Case resolved successfully",
          data: {
            id: 1,
            reportId,
            resolutionType,
            description,
            closedBy,
            closedDate,
          },
        };
      } catch (error: any) {
        console.error("Error creating resolution:", error);
        set.status = 500;
        return { success: false, message: "Failed to create resolution" };
      }
    },
    {
      body: t.Object({
        resolutionType: t.String(),
        description: t.String(),
        closedBy: t.String(),
        closedDate: t.String(),
      }),
    }
  )

  // Update resolution
  .put(
    "/",
    async ({ params, body, set }) => {
      try {
        const { reportId } = params as any;
        // TODO: Update resolution in database
        return {
          success: true,
          message: "Resolution updated successfully",
        };
      } catch (error: any) {
        console.error("Error updating resolution:", error);
        set.status = 500;
        return { success: false, message: "Failed to update resolution" };
      }
    },
    {
      body: t.Partial(
        t.Object({
          resolutionType: t.String(),
          description: t.String(),
          closedBy: t.String(),
          closedDate: t.String(),
        })
      ),
    }
  )

  // Reopen case
  .post(
    "/reopen",
    async ({ params, body, set }) => {
      try {
        const { reportId } = params as any;
        const { reason } = body as any;

        // TODO: Update report status back to Pending/In Progress
        return {
          success: true,
          message: "Case reopened successfully",
          data: { reportId, reason },
        };
      } catch (error: any) {
        console.error("Error reopening case:", error);
        set.status = 500;
        return { success: false, message: "Failed to reopen case" };
      }
    },
    {
      body: t.Object({
        reason: t.String(),
      }),
    }
  );
