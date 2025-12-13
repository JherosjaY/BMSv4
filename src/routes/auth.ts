import { Elysia, t } from "elysia";
import { db } from "../db";
import { users } from "../db/schema";
import { eq } from "drizzle-orm";
import bcrypt from "bcryptjs";
import { CreateUserRequest, LoginRequest, UpdateProfileRequest, UserResponse } from "../models";
import { emailService } from "../services/emailService";

export const authRoutes = new Elysia({ prefix: "/auth" })
  .post(
    "/register",
    async ({ body, set }) => {
      try {
        console.log("📝 Register endpoint called");
        const { username, email, password, confirmPassword, firstName, lastName, profilePhoto } = body as any;
        console.log(`📝 Received: username=${username}, email=${email}, firstName=${firstName}, lastName=${lastName}, profilePhoto=${profilePhoto}`);

        // Validate passwords match
        if (password !== confirmPassword) {
          console.log("❌ Passwords do not match");
          set.status = 400;
          return { success: false, message: "Passwords do not match" };
        }

      const existingUser = await db.query.users.findFirst({
        where: eq(users.email, email),
      });

      if (existingUser) {
        set.status = 400;
        return { success: false, message: "Email already exists" };
      }

      const hashedPassword = await bcrypt.hash(password, 10);

      const [newUser] = await db
        .insert(users)
        .values({
          username,
          email,
          password: hashedPassword,
          firstName,
          lastName,
          profilePhoto: profilePhoto || null, // Save Cloudinary URL if provided
          role: "user",
          status: "active",
          emailVerified: false,
          isActive: true,
          profileCompleted: false,
        })
        .returning();

      // ✅ Skip email verification for now - mark as verified immediately
      console.log(`✅ User created: ${email}`);
      await db
        .update(users)
        .set({
          emailVerified: true, // Mark as verified immediately
        })
        .where(eq(users.id, newUser.id));
      console.log(`✅ Email marked as verified (skipped email sending)`);

      // Generate and send verification code
      const verificationCode = emailService.generateVerificationCode();
      await emailService.sendVerificationEmail(email, verificationCode, username);

      const token = Buffer.from(`${newUser.id}:${newUser.email}:${Date.now()}`).toString("base64");

      return {
        success: true,
        message: "Registration successful. Verification code sent to your email.",
        data: {
          user: {
            id: newUser.id,
            username: newUser.firstName,
            firstName: newUser.firstName,
            lastName: newUser.lastName,
            role: newUser.role,
            profileCompleted: newUser.emailVerified,
          },
          token: token,
        },
      };
      } catch (error: any) {
        console.error("❌ Register error:", error);
        set.status = 500;
        return { success: false, message: `Registration failed: ${error.message}` };
      }
    },
    {
      body: t.Object({
        username: t.String(),
        email: t.String(),
        password: t.String(),
        confirmPassword: t.String(),
        firstName: t.String(),
        lastName: t.String(),
        profilePhoto: t.Optional(t.String()), // Cloudinary URL
      }),
    }
  )

  .post(
    "/login",
    async ({ body, set }) => {
      const { username, password } = body as LoginRequest;

      const user = await db.query.users.findFirst({
        where: eq(users.email, username),
      });

      if (!user) {
        set.status = 401;
        return { success: false, message: "Invalid credentials" };
      }

      let passwordMatch = false;

      if (user.password.startsWith("$2b$") || user.password.startsWith("$2a$")) {
        passwordMatch = await bcrypt.compare(password, user.password);
      } else {
        passwordMatch = user.password === password;

        if (passwordMatch) {
          const hashedPassword = await bcrypt.hash(password, 10);
          await db.update(users).set({ password: hashedPassword }).where(eq(users.id, user.id));
        }
      }
      if (!passwordMatch) {
        set.status = 401;
        return { success: false, message: "Invalid credentials" };
      }

      if (user.status !== "active" || !user.is_active) {
        set.status = 403;
        return { success: false, message: "Account is inactive" };
      }

      const token = Buffer.from(`${user.id}:${user.email}:${Date.now()}`).toString("base64");

      return {
        success: true,
        message: "Login successful",
        data: {
          user: {
            id: user.id,
            username: user.username || user.firstName,
            firstName: user.firstName,
            lastName: user.lastName,
            role: user.role,
            profilePhotoUri: user.profilePhoto,
            profileCompleted: user.profileCompleted,
          },
          token: token,
        },
      };
    },
    {
      body: t.Object({
        username: t.String(),
        password: t.String(),
      }),
    }
  )

  .post(
    "/send-verification-code",
    async ({ body, set }) => {
      try {
        const { email } = body as any;

        // Generate 6-digit code
        const verificationCode = emailService.generateVerificationCode();

        // Send email (works for both new and existing users)
        const emailSent = await emailService.sendVerificationEmail(
          email,
          verificationCode,
          email.split("@")[0] // Use email prefix as username for new users
        );

        if (!emailSent) {
          set.status = 500;
          return { success: false, message: "Failed to send verification email" };
        }

        console.log(`✅ Verification code sent to ${email}: ${verificationCode}`);

        return {
          success: true,
          message: "Verification code sent to your email",
        };
      } catch (error: any) {
        console.error("❌ Error sending verification code:", error);
        set.status = 500;
        return {
          success: false,
          message: "Failed to send verification code",
        };
      }
    },
    {
      body: t.Object({
        email: t.String(),
      }),
    }
  )

  .post(
    "/verify-email",
    async ({ body, set }) => {
      try {
        const { email, code } = body as any;

        // Find user by email
        const user = await db.query.users.findFirst({
          where: eq(users.email, email),
        });

        if (!user) {
          set.status = 404;
          return { success: false, message: "User not found" };
        }

        // Check if code matches and hasn't expired
        if (user.resetCode !== code) {
          set.status = 400;
          return { success: false, message: "Invalid verification code" };
        }

        if (!user.resetCodeExpiry || user.resetCodeExpiry < Date.now()) {
          set.status = 400;
          return { success: false, message: "Verification code has expired" };
        }

        // Mark email as verified
        await db
          .update(users)
          .set({
            emailVerified: true,
            resetCode: null,
            resetCodeExpiry: null,
          })
          .where(eq(users.id, user.id));

        console.log(`✅ Email verified for user: ${email}`);

        return {
          success: true,
          message: "Email verified successfully",
        };
      } catch (error: any) {
        console.error("❌ Error verifying email:", error);
        set.status = 500;
        return {
          success: false,
          message: "Failed to verify email",
        };
      }
    },
    {
      body: t.Object({
        email: t.String(),
        code: t.String(),
      }),
    }
  )

  .put(
    "/profile/:userId",
    async ({ params, body, set }) => {
      try {
        const userId = params.userId as string;
        const { profilePhotoUri, profileCompleted } = body as UpdateProfileRequest;

        const [updatedUser] = await db
          .update(users)
          .set({
            profilePhoto: profilePhotoUri,
            profileCompleted: profileCompleted !== undefined ? profileCompleted : true,
            updatedAt: new Date(),
          })
          .where(eq(users.id, userId))
          .returning();

        if (!updatedUser) {
          set.status = 404;
          return {
            success: false,
            message: "User not found",
          };
        }

        return {
          success: true,
          message: "Profile updated successfully",
          user: updatedUser,
        };
      } catch (error: any) {
        set.status = 500;
        return {
          success: false,
          message: "Failed to update profile",
        };
      }
    },
    {
      body: t.Object({
        profilePhotoUri: t.String(),
        profileCompleted: t.Optional(t.Boolean()),
      }),
    }
  )

  // Forgot password - send reset code
  .post(
    "/forgot-password",
    async ({ body, set }) => {
      try {
        const { email } = body as any;
        
        const user = await db.query.users.findFirst({
          where: eq(users.email, email),
        });

        if (!user) {
          set.status = 404;
          return { success: false, message: "Email not found" };
        }

        // Generate reset code
        const resetCode = Math.floor(100000 + Math.random() * 900000).toString();
        const expiresAt = new Date(Date.now() + 1 * 60 * 60 * 1000); // 1 hour

        await db
          .update(users)
          .set({
            resetCode,
            resetCodeExpiry: expiresAt.getTime(),
          })
          .where(eq(users.id, user.id));

        // ✅ Send password reset email
        const emailSent = await emailService.sendPasswordResetEmail(
          email,
          resetCode,
          user.username || user.firstName
        );

        if (!emailSent) {
          console.warn(`⚠️ Failed to send password reset email to ${email}, but code was generated`);
          // Don't fail the request - code is still valid in database
        }

        console.log(`✅ Password reset code sent to ${email}: ${resetCode}`);

        return {
          success: true,
          message: "Password reset code sent to your email",
        };
      } catch (error: any) {
        console.error("❌ Error sending reset code:", error);
        set.status = 500;
        return { success: false, message: "Failed to send reset code" };
      }
    },
    {
      body: t.Object({
        email: t.String(),
      }),
    }
  )

  // Reset password with code
  .post(
    "/reset-password",
    async ({ body, set }) => {
      try {
        const { email, code, newPassword } = body as any;

        const user = await db.query.users.findFirst({
          where: eq(users.email, email),
        });

        if (!user) {
          set.status = 404;
          return { success: false, message: "User not found" };
        }

        // Verify code
        if (user.resetCode !== code) {
          set.status = 400;
          return { success: false, message: "Invalid reset code" };
        }

        if (!user.resetCodeExpiry || user.resetCodeExpiry < Date.now()) {
          set.status = 400;
          return { success: false, message: "Reset code has expired" };
        }

        // Hash new password
        const hashedPassword = await bcrypt.hash(newPassword, 10);

        await db
          .update(users)
          .set({
            password: hashedPassword,
            resetCode: null,
            resetCodeExpiry: null,
          })
          .where(eq(users.id, user.id));

        console.log(`✅ Password reset successfully for ${email}`);

        return {
          success: true,
          message: "Password reset successfully",
        };
      } catch (error: any) {
        console.error("Error resetting password:", error);
        set.status = 500;
        return { success: false, message: "Failed to reset password" };
      }
    },
    {
      body: t.Object({
        email: t.String(),
        code: t.String(),
        newPassword: t.String(),
      }),
    }
  )

  // Google Sign-In
  .post(
    "/google-signin",
    async ({ body, set }) => {
      try {
        const { email, displayName, photoUrl } = body as any;

        console.log(`🔐 Google Sign-In request: ${email}`);

        // Check if user already exists
        let user = await db.query.users.findFirst({
          where: eq(users.email, email),
        });

        if (user) {
          // User exists - check if it's a Google account
          if (user.authMethod && user.authMethod !== "GOOGLE") {
            console.warn(`⚠️ Email ${email} registered via different method`);
            set.status = 400;
            return {
              success: false,
              message: "This email is already registered. Please use Sign in with username and password.",
            };
          }

          console.log(`✅ Google user exists, logging in: ${email}`);
        } else {
          // Create new Google user
          console.log(`✅ Creating new Google user: ${email}`);

          // Parse display name
          let firstName = "User";
          let lastName = "Account";

          if (displayName && displayName.trim()) {
            const nameParts = displayName.split(" ", 2);
            firstName = nameParts[0];
            if (nameParts.length > 1) {
              lastName = nameParts[1];
            }
          }

          // Extract username from email (before @)
          const username = email.split("@")[0];

          // Create user with Google auth method
          const [newUser] = await db
            .insert(users)
            .values({
              email,
              password: "", // Google users don't have passwords
              firstName,
              lastName,
              role: "user", // Google Sign-In users are always "user" role
              status: "active",
              emailVerified: true, // Google emails are pre-verified
              isActive: true,
              profileCompleted: false,
              mustChangePassword: false,
              profilePhoto: photoUrl || null,
              authMethod: "GOOGLE", // Mark as Google account
            })
            .returning();

          user = newUser;
          console.log(`✅ New Google user created: ${user.id}`);
        }

        // Generate token
        const token = Buffer.from(`${user.id}:${user.email}:${Date.now()}`).toString("base64");

        return {
          success: true,
          message: "Google Sign-In successful",
          data: {
            user: {
              id: user.id,
              username: user.username || user.firstName,
              firstName: user.firstName,
              lastName: user.lastName,
              email: user.email,
              role: user.role,
              profilePhotoUri: user.profilePhoto,
              profileCompleted: user.profileCompleted,
            },
            token: token,
          },
        };
      } catch (error: any) {
        console.error("❌ Google Sign-In error:", error);
        set.status = 500;
        return {
          success: false,
          message: "Google Sign-In failed",
          error: error.message,
        };
      }
    },
    {
      body: t.Object({
        email: t.String(),
        displayName: t.Optional(t.String()),
        photoUrl: t.Optional(t.String()),
      }),
    }
  );
