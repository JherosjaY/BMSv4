import { Elysia, t } from "elysia";

export const witnessesRoutes = new Elysia({ prefix: "/reports/:id/witnesses" })
  // Get witnesses for report
  .get("/", async ({ params, set }) => {
    try {
      const { id } = params as any;
      // TODO: Fetch witnesses from database
      return {
        success: true,
        data: [],
      };
    } catch (error: any) {
      console.error("Error fetching witnesses:", error);
      set.status = 500;
      return { success: false, message: "Failed to fetch witnesses" };
    }
  })

  // Add witness to report
  .post(
    "/",
    async ({ params, body, set }) => {
      try {
        const { reportId } = params as any;
        const { name, contactNumber, address, statement } = body as any;
        // TODO: Save witness to database
        return {
          success: true,
          message: "Witness added successfully",
          data: { id: 1, reportId, name, contactNumber, address, statement },
        };
      } catch (error: any) {
        console.error("Error adding witness:", error);
        set.status = 500;
        return { success: false, message: "Failed to add witness" };
      }
    },
    {
      body: t.Object({
        name: t.String(),
        contactNumber: t.Optional(t.String()),
        address: t.Optional(t.String()),
        statement: t.Optional(t.String()),
      }),
    }
  )

  // Update witness
  .put(
    "/:witnessId",
    async ({ params, body, set }) => {
      try {
        const { reportId, witnessId } = params as any;
        // TODO: Update witness in database
        return {
          success: true,
          message: "Witness updated successfully",
        };
      } catch (error: any) {
        console.error("Error updating witness:", error);
        set.status = 500;
        return { success: false, message: "Failed to update witness" };
      }
    },
    {
      body: t.Partial(
        t.Object({
          name: t.String(),
          contactNumber: t.String(),
          address: t.String(),
          statement: t.String(),
        })
      ),
    }
  )

  // Delete witness
  .delete("/:witnessId", async ({ params, set }) => {
    try {
      const { reportId, witnessId } = params as any;
      // TODO: Delete witness from database
      return {
        success: true,
        message: "Witness deleted successfully",
      };
    } catch (error: any) {
      console.error("Error deleting witness:", error);
      set.status = 500;
      return { success: false, message: "Failed to delete witness" };
    }
  });
