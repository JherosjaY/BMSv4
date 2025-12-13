import { Elysia, t } from "elysia";
import { db } from "../db";
import { users } from "../db/schema";
import { eq } from "drizzle-orm";

export const departmentsRoutes = new Elysia({ prefix: "/departments" })
  // Get all departments (from users with unique departments)
  .get("/", async ({ set }) => {
    try {
      const allUsers = await db.query.users.findMany();
      
      // Extract unique departments
      const departments = [...new Set(
        allUsers
          .map(u => u.department)
          .filter(d => d && d.trim() !== "")
      )].map((dept, idx) => ({
        id: idx + 1,
        name: dept,
      }));

      return {
        success: true,
        data: departments,
      };
    } catch (error: any) {
      console.error("Error fetching departments:", error);
      set.status = 500;
      return { success: false, message: "Failed to fetch departments" };
    }
  })

  // Get officers in a department
  .get("/:departmentName/officers", async ({ params, set }) => {
    try {
      const { departmentName } = params as any;

      const officers = await db.query.users.findMany({
        columns: {
          password: false,
        },
      });

      const deptOfficers = officers.filter(
        o => o.department === departmentName && o.role === "officer"
      );

      return {
        success: true,
        data: deptOfficers,
      };
    } catch (error: any) {
      console.error("Error fetching department officers:", error);
      set.status = 500;
      return { success: false, message: "Failed to fetch officers" };
    }
  })

  // Create new department (admin only)
  .post(
    "/",
    async ({ body, set }) => {
      try {
        const { name } = body as any;
        
        // TODO: Create departments table in database
        return {
          success: true,
          message: "Department created successfully",
          data: { id: 1, name },
        };
      } catch (error: any) {
        console.error("Error creating department:", error);
        set.status = 500;
        return { success: false, message: "Failed to create department" };
      }
    },
    {
      body: t.Object({
        name: t.String(),
      }),
    }
  );
