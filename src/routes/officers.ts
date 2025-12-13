import { Elysia, t } from "elysia";
import { db } from "../db";
import { users, officers } from "../db/schema";
import { eq, desc } from "drizzle-orm";
import bcrypt from "bcryptjs";
import { emailService } from "../services/emailService";

/**
 * Utility: Generate random username
 */
function generateUsername(firstName: string, lastName: string): string {
  const randomNum = Math.floor(Math.random() * 10000);
  return `${firstName.toLowerCase()}.${lastName.toLowerCase()}.${randomNum}`;
}

/**
 * Utility: Generate random password
 */
function generatePassword(): string {
  const chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
  let password = "";
  for (let i = 0; i < 12; i++) {
    password += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return password;
}

export const officersRoutes = new Elysia({ prefix: "/officers" })
  // Get all officers
  .get("/", async ({ set }) => {
    try {
      const allOfficers = await db.query.users.findMany({
        where: eq(users.role, "officer"),
        columns: {
          password: false,
        },
      });

      return {
        success: true,
        data: allOfficers,
      };
    } catch (error: any) {
      console.error("❌ Error fetching officers:", error);
      set.status = 500;
      return { success: false, message: "Failed to fetch officers" };
    }
  })

  // Get officer by ID
  .get("/:id", async ({ params, set }) => {
    try {
      const officer = await db.query.users.findFirst({
        where: eq(users.id, params.id),
        columns: {
          password: false,
        },
      });

      if (!officer) {
        set.status = 404;
        return { success: false, message: "Officer not found" };
      }

      return {
        success: true,
        data: officer,
      };
    } catch (error: any) {
      console.error("❌ Error fetching officer:", error);
      set.status = 500;
      return { success: false, message: "Failed to fetch officer" };
    }
  })

  // Create officer account with generated credentials
  .post(
    "/",
    async ({ body, set }) => {
      try {
        const { firstName, lastName, email, department, badgeNumber, rank } = body as any;

        // Check if email already exists
        const existingUser = await db.query.users.findFirst({
          where: eq(users.email, email),
        });

        if (existingUser) {
          set.status = 400;
          return { success: false, message: "Email already exists" };
        }

        // Generate credentials
        const username = generateUsername(firstName, lastName);
        const password = generatePassword();
        const hashedPassword = await bcrypt.hash(password, 10);

        // Create user account
        const [newUser] = await db
          .insert(users)
          .values({
            username,
            email,
            password: hashedPassword,
            firstName,
            lastName,
            role: "officer",
            status: "active",
            emailVerified: true,
            isActive: true,
            profileCompleted: true,
            mustChangePassword: false,
            department,
            badgeNumber,
          })
          .returning();

        console.log(`✅ Officer account created: ${username}`);
        console.log(`   Email: ${email}`);
        console.log(`   Generated Password: ${password}`);

        return {
          success: true,
          message: "Officer account created successfully",
          data: {
            user: {
              id: newUser.id,
              username: newUser.username,
              firstName: newUser.firstName,
              lastName: newUser.lastName,
              email: newUser.email,
              role: newUser.role,
              department: newUser.department,
              badgeNumber: newUser.badgeNumber,
            },
            credentials: {
              username: username,
              password: password,
              note: "⚠️ Share these credentials with the officer. They can change password after first login.",
            },
          },
        };
      } catch (error: any) {
        console.error("❌ Error creating officer:", error);
        set.status = 500;
        return {
          success: false,
          message: "Failed to create officer account",
          error: error.message,
        };
      }
    },
    {
      body: t.Object({
        firstName: t.String(),
        lastName: t.String(),
        email: t.String(),
        department: t.String(),
        badgeNumber: t.Optional(t.String()),
        rank: t.Optional(t.String()),
      }),
    }
  )

  // Update officer details
  .put(
    "/:id",
    async ({ params, body, set }) => {
      try {
        const updateData: any = { updatedAt: new Date() };

        if (body.firstName) updateData.firstName = body.firstName;
        if (body.lastName) updateData.lastName = body.lastName;
        if (body.department) updateData.department = body.department;
        if (body.badgeNumber) updateData.badgeNumber = body.badgeNumber;
        if (body.gender) updateData.gender = body.gender;

        const [updatedOfficer] = await db
          .update(users)
          .set(updateData)
          .where(eq(users.id, params.id))
          .returning({
            id: users.id,
            username: users.username,
            firstName: users.firstName,
            lastName: users.lastName,
            email: users.email,
            role: users.role,
            department: users.department,
            badgeNumber: users.badgeNumber,
          });

        if (!updatedOfficer) {
          set.status = 404;
          return { success: false, message: "Officer not found" };
        }

        console.log(`✅ Officer updated: ${updatedOfficer.id}`);

        return {
          success: true,
          message: "Officer updated successfully",
          data: updatedOfficer,
        };
      } catch (error: any) {
        console.error("❌ Error updating officer:", error);
        set.status = 500;
        return { success: false, message: "Failed to update officer" };
      }
    },
    {
      body: t.Partial(
        t.Object({
          firstName: t.String(),
          lastName: t.String(),
          department: t.String(),
          badgeNumber: t.String(),
          gender: t.String(),
        })
      ),
    }
  )

  // Delete officer
  .delete("/:id", async ({ params, set }) => {
    try {
      const [deletedOfficer] = await db
        .delete(users)
        .where(eq(users.id, params.id))
        .returning();

      if (!deletedOfficer) {
        set.status = 404;
        return { success: false, message: "Officer not found" };
      }

      console.log(`✅ Officer deleted: ${deletedOfficer.id}`);

      return {
        success: true,
        message: "Officer deleted successfully",
      };
    } catch (error: any) {
      console.error("❌ Error deleting officer:", error);
      set.status = 500;
      return { success: false, message: "Failed to delete officer" };
    }
  })

  // Deactivate officer
  .put(
    "/:id/deactivate",
    async ({ params, set }) => {
      try {
        const [updated] = await db
          .update(users)
          .set({ isActive: false })
          .where(eq(users.id, params.id))
          .returning();

        if (!updated) {
          set.status = 404;
          return { success: false, message: "Officer not found" };
        }

        console.log(`✅ Officer deactivated: ${params.id}`);

        return {
          success: true,
          message: "Officer deactivated successfully",
        };
      } catch (error: any) {
        console.error("❌ Error deactivating officer:", error);
        set.status = 500;
        return { success: false, message: "Failed to deactivate officer" };
      }
    }
  )

  // Activate officer
  .put(
    "/:id/activate",
    async ({ params, set }) => {
      try {
        const [updated] = await db
          .update(users)
          .set({ isActive: true })
          .where(eq(users.id, params.id))
          .returning();

        if (!updated) {
          set.status = 404;
          return { success: false, message: "Officer not found" };
        }

        console.log(`✅ Officer activated: ${params.id}`);

        return {
          success: true,
          message: "Officer activated successfully",
        };
      } catch (error: any) {
        console.error("❌ Error activating officer:", error);
        set.status = 500;
        return { success: false, message: "Failed to activate officer" };
      }
    }
  )

  // Send officer credentials via email
  .post(
    "/:id/send-credentials",
    async ({ params, body, set }) => {
      try {
        const officerId = params.id;
        const { username, password, email, firstName, lastName } = body as any;

        // Verify officer exists in database
        const officer = await db.query.users.findFirst({
          where: eq(users.id, officerId),
        });

        if (!officer) {
          set.status = 404;
          return { success: false, message: "Officer not found" };
        }

        // Send credentials email
        const emailSent = await emailService.sendOfficerCredentialsEmail(
          email,
          firstName + " " + lastName,
          username,
          password
        );

        if (!emailSent) {
          set.status = 500;
          return { success: false, message: "Failed to send email" };
        }

        console.log(`✅ Officer credentials sent to ${email}`);
        console.log(`   Officer: ${firstName} ${lastName}`);
        console.log(`   Username: ${username}`);

        return {
          success: true,
          message: "Credentials sent successfully to " + email,
        };
      } catch (error: any) {
        console.error("❌ Error sending credentials:", error);
        set.status = 500;
        return {
          success: false,
          message: "Failed to send credentials",
          error: error.message,
        };
      }
    },
    {
      body: t.Object({
        username: t.String(),
        password: t.String(),
        email: t.String(),
        firstName: t.String(),
        lastName: t.String(),
      }),
    }
  );
