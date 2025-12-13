import { db } from "./db";
import { users } from "./db/schema";
import bcrypt from "bcryptjs";

/**
 * Seed script to create built-in admin account
 * Run with: npx tsx src/seed.ts
 */
async function seed() {
  try {
    console.log("🌱 Starting database seed...");
    console.log("📝 Creating admin account...");

    // Hash the password
    const hashedPassword = await bcrypt.hash("@BMSOFFICIAL2025", 10);

    // Create admin account using Drizzle
    const [adminUser] = await db
      .insert(users)
      .values({
        username: "official.bms.admin",
        email: "official.bms.2025@gmail.com",
        password: hashedPassword,
        firstName: "System",
        lastName: "Administrator",
        role: "admin",
        status: "active",
        emailVerified: true,
        isActive: true,
        profileCompleted: true,
        mustChangePassword: false,
      })
      .returning();

    console.log(`✅ Admin account created successfully`);
    console.log(`   Username: official.bms.admin`);
    console.log(`   Email: official.bms.2025@gmail.com`);
    console.log(`   Password: @BMSOFFICIAL2025`);
    console.log(`   Role: admin`);
    console.log(`   ID: ${adminUser.id}`);

    console.log("🌱 Seed completed successfully!");
  } catch (error: any) {
    if (error.message && error.message.includes("duplicate key")) {
      console.log("✅ Admin account already exists");
    } else {
      console.error("❌ Seed failed:", error.message);
      process.exit(1);
    }
  }
}

seed();
