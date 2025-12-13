import { pgTable, uuid, varchar, text, timestamp, boolean, serial, integer, bigint } from 'drizzle-orm/pg-core';

// Users Table - Matches actual Neon database schema (pure users, no officer fields)
export const users = pgTable('users', {
  id: serial('id').primaryKey(),
  username: varchar('username', { length: 100 }).unique(),
  email: varchar('email', { length: 255 }).unique().notNull(),
  password: varchar('password', { length: 255 }).notNull(),
  firstName: varchar('firstName', { length: 100 }).default(''),
  lastName: varchar('lastName', { length: 100 }).default(''),
  role: varchar('role', { length: 50 }).notNull().default('user'),
  status: varchar('status', { length: 20 }).notNull().default('active'),
  emailVerified: boolean('emailVerified').default(false),
  isActive: boolean('isActive').default(true),
  profileCompleted: boolean('profileCompleted').default(false),
  resetCode: varchar('resetCode', { length: 10 }),
  resetCodeExpiry: bigint('resetCodeExpiry', { mode: 'number' }),
  profilePhoto: varchar('profilePhoto', { length: 255 }),
  authMethod: varchar('authMethod', { length: 50 }).default('PASSWORD'),
  deviceId: varchar('deviceId', { length: 255 }),
  fcmToken: varchar('fcmToken', { length: 255 }),
  lastLogin: timestamp('lastLogin'),
  createdAt: timestamp('createdAt').defaultNow(),
});

// Officers Table (extends users with officer-specific data)
export const officers = pgTable('officers', {
  id: serial('id').primaryKey(),
  userId: uuid('user_id').notNull().unique(),
  badgeNumber: varchar('badge_number', { length: 50 }).unique(),
  rank: varchar('rank', { length: 100 }),
  department: varchar('department', { length: 100 }).notNull(),
  yearsOfService: integer('years_of_service'),
  assignedCases: integer('assigned_cases').default(0),
  resolvedCases: integer('resolved_cases').default(0),
  performanceRating: varchar('performance_rating', { length: 20 }),
  certifications: text('certifications'),
  licenseExpiry: varchar('license_expiry', { length: 50 }),
  isAvailable: boolean('is_available').default(true),
  createdAt: timestamp('created_at').notNull().defaultNow(),
  updatedAt: timestamp('updated_at').notNull().defaultNow(),
});

// Blotter Reports Table
export const blotterReports = pgTable('blotter_reports', {
  id: serial('id').primaryKey(),
  caseNumber: varchar('case_number', { length: 50 }).notNull().unique(),
  incidentType: varchar('incident_type', { length: 100 }).notNull(),
  incidentDate: varchar('incident_date', { length: 50 }).notNull(),
  incidentTime: varchar('incident_time', { length: 50 }).notNull(),
  incidentLocation: varchar('incident_location', { length: 255 }).notNull(),
  narrative: text('narrative').notNull(),
  complainantName: varchar('complainant_name', { length: 100 }),
  complainantContact: varchar('complainant_contact', { length: 20 }),
  complainantAddress: varchar('complainant_address', { length: 255 }),
  complainantEmail: varchar('complainant_email', { length: 100 }),
  status: varchar('status', { length: 20 }).notNull().default('Pending'),
  priority: varchar('priority', { length: 20 }).notNull().default('Normal'),
  assignedOfficer: varchar('assigned_officer', { length: 100 }),
  assignedOfficerIds: varchar('assigned_officer_ids', { length: 255 }),
  filedBy: varchar('filed_by', { length: 100 }),
  filedById: integer('filed_by_id'),
  audioRecordingUri: varchar('audio_recording_uri', { length: 255 }),
  isArchived: boolean('is_archived').notNull().default(false),
  createdAt: timestamp('created_at').notNull().defaultNow(),
  updatedAt: timestamp('updated_at').notNull().defaultNow(),
});

// Suspects Table
export const suspects = pgTable('suspects', {
  id: serial('id').primaryKey(),
  blotterReportId: integer('blotter_report_id').notNull(),
  name: varchar('name', { length: 100 }).notNull(),
  age: integer('age'),
  address: varchar('address', { length: 255 }),
  description: text('description'),
  createdAt: timestamp('created_at').notNull().defaultNow(),
});

// Witnesses Table
export const witnesses = pgTable('witnesses', {
  id: serial('id').primaryKey(),
  blotterReportId: integer('blotter_report_id').notNull(),
  name: varchar('name', { length: 100 }).notNull(),
  contactNumber: varchar('contact_number', { length: 20 }),
  address: varchar('address', { length: 255 }),
  statement: text('statement'),
  createdAt: timestamp('created_at').notNull().defaultNow(),
});

// Evidence Table
export const evidence = pgTable('evidence', {
  id: serial('id').primaryKey(),
  blotterReportId: integer('blotter_report_id').notNull(),
  evidenceType: varchar('evidence_type', { length: 100 }).notNull(),
  description: text('description').notNull(),
  locationFound: varchar('location_found', { length: 255 }),
  photoUri: varchar('photo_uri', { length: 255 }),
  collectedBy: varchar('collected_by', { length: 100 }),
  createdAt: timestamp('created_at').notNull().defaultNow(),
});

// Hearings Table
export const hearings = pgTable('hearings', {
  id: serial('id').primaryKey(),
  blotterReportId: integer('blotter_report_id').notNull(),
  hearingDate: varchar('hearing_date', { length: 50 }).notNull(),
  hearingTime: varchar('hearing_time', { length: 50 }).notNull(),
  location: varchar('location', { length: 255 }).notNull(),
  purpose: text('purpose'),
  presider: varchar('presider', { length: 100 }),
  attendees: text('attendees'),
  notes: text('notes'),
  status: varchar('status', { length: 20 }).notNull().default('Scheduled'),
  createdAt: timestamp('created_at').notNull().defaultNow(),
});

// Resolutions Table
export const resolutions = pgTable('resolutions', {
  id: serial('id').primaryKey(),
  blotterReportId: integer('blotter_report_id').notNull(),
  resolutionDate: varchar('resolution_date', { length: 50 }).notNull(),
  resolutionType: varchar('resolution_type', { length: 100 }).notNull(),
  description: text('description'),
  outcome: text('outcome'),
  approvedBy: varchar('approved_by', { length: 100 }),
  documentUri: varchar('document_uri', { length: 255 }),
  status: varchar('status', { length: 20 }).notNull().default('Pending'),
  createdAt: timestamp('created_at').notNull().defaultNow(),
});

// Notifications Table
export const notifications = pgTable('notifications', {
  id: serial('id').primaryKey(),
  userId: uuid('user_id').notNull(),
  title: varchar('title', { length: 255 }).notNull(),
  message: text('message').notNull(),
  type: varchar('type', { length: 50 }).notNull(),
  relatedReportId: integer('related_report_id'),
  isRead: boolean('is_read').notNull().default(false),
  readAt: timestamp('read_at'),
  createdAt: timestamp('created_at').notNull().defaultNow(),
});

// Activity Logs Table
export const activityLogs = pgTable('activity_logs', {
  id: serial('id').primaryKey(),
  userId: uuid('user_id').notNull(),
  action: varchar('action', { length: 255 }).notNull(),
  entityType: varchar('entity_type', { length: 100 }),
  entityId: integer('entity_id'),
  details: text('details'),
  ipAddress: varchar('ip_address', { length: 45 }),
  userAgent: varchar('user_agent', { length: 255 }),
  createdAt: timestamp('created_at').notNull().defaultNow(),
});

// Audit Logs Table
export const auditLogs = pgTable('audit_logs', {
  id: serial('id').primaryKey(),
  blotterReportId: integer('blotter_report_id').notNull(),
  changedBy: uuid('changed_by').notNull(),
  action: varchar('action', { length: 255 }).notNull(),
  fieldName: varchar('field_name', { length: 100 }),
  oldValue: text('old_value'),
  newValue: text('new_value'),
  timestamp: timestamp('timestamp').notNull().defaultNow(),
});

// Login Logs Table
export const loginLogs = pgTable('login_logs', {
  id: serial('id').primaryKey(),
  userId: uuid('user_id').notNull(),
  ipAddress: varchar('ip_address', { length: 45 }).notNull(),
  device: varchar('device', { length: 100 }),
  userAgent: varchar('user_agent', { length: 255 }),
  status: varchar('status', { length: 20 }).notNull().default('success'),
  failureReason: varchar('failure_reason', { length: 255 }),
  loginAt: timestamp('login_at').notNull().defaultNow(),
});

// Error Logs Table
export const errorLogs = pgTable('error_logs', {
  id: serial('id').primaryKey(),
  severity: varchar('severity', { length: 20 }).notNull(),
  message: text('message').notNull(),
  stack: text('stack'),
  endpoint: varchar('endpoint', { length: 255 }),
  userId: uuid('user_id'),
  timestamp: timestamp('timestamp').notNull().defaultNow(),
});
