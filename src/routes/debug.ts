import { Elysia } from 'elysia';
import { db } from '../db';
import { users, blotterReports, suspects, witnesses, evidence } from '../db/schema';
import { eq } from 'drizzle-orm';

/**
 * DEBUG ENDPOINTS - For testing data flow
 * Verify data is stored correctly in Neon
 */

export const debugRoutes = new Elysia({ prefix: '/debug' })
  // Get all users
  .get('/users', async () => {
    try {
      const allUsers = await db.select().from(users);
      return {
        success: true,
        count: allUsers.length,
        data: allUsers.map(u => ({
          id: u.id,
          username: u.username,
          email: u.email,
          role: u.role,
          createdAt: u.createdAt
        }))
      };
    } catch (error) {
      console.error('❌ Error fetching users:', error);
      return { success: false, error: 'Failed to fetch users' };
    }
  })
  
  // Get all reports with counts
  .get('/reports-full', async () => {
    try {
      const allReports = await db.select().from(blotterReports);
      
      const reportsWithData = await Promise.all(
        allReports.map(async (report) => {
          const reportSuspects = await db.select().from(suspects).where(eq(suspects.blotterReportId, report.id));
          const reportWitnesses = await db.select().from(witnesses).where(eq(witnesses.blotterReportId, report.id));
          const reportEvidence = await db.select().from(evidence).where(eq(evidence.blotterReportId, report.id));
          
          return {
            report: {
              id: report.id,
              caseNumber: report.caseNumber,
              incidentType: report.incidentType,
              status: report.status,
              createdAt: report.createdAt
            },
            suspects: reportSuspects.length,
            witnesses: reportWitnesses.length,
            evidence: reportEvidence.length
          };
        })
      );
      
      return {
        success: true,
        totalReports: allReports.length,
        data: reportsWithData
      };
    } catch (error) {
      console.error('❌ Error fetching reports:', error);
      return { success: false, error: 'Failed to fetch reports' };
    }
  })
  
  // Get specific report with all details
  .get('/reports/:id/details', async ({ params }) => {
    try {
      const reportId = parseInt(params.id);
      
      const report = await db.select().from(blotterReports).where(eq(blotterReports.id, reportId));
      
      if (!report || report.length === 0) {
        return { success: false, error: 'Report not found' };
      }
      
      const reportSuspects = await db.select().from(suspects).where(eq(suspects.blotterReportId, reportId));
      const reportWitnesses = await db.select().from(witnesses).where(eq(witnesses.blotterReportId, reportId));
      const reportEvidence = await db.select().from(evidence).where(eq(evidence.blotterReportId, reportId));
      
      return {
        success: true,
        data: {
          report: report[0],
          suspects: reportSuspects,
          witnesses: reportWitnesses,
          evidence: reportEvidence
        }
      };
    } catch (error) {
      console.error('❌ Error fetching report details:', error);
      return { success: false, error: 'Failed to fetch report details' };
    }
  })
  
  // Health check
  .get('/health', () => ({
    success: true,
    message: 'Backend is running',
    timestamp: new Date().toISOString()
  }));
