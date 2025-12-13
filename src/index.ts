import 'dotenv/config';
import { Elysia } from 'elysia';
import { cors } from '@elysiajs/cors';
import { swagger } from '@elysiajs/swagger';
import { db } from './db';
import { authRoutes, usersRoutes, reportsRoutes, notificationsRoutes, analyticsRoutes, hearingsRoutes, evidenceRoutes, adminRoutes, officersRoutes, departmentsRoutes, uploadRoutes, searchRoutes, exportRoutes, logsRoutes, suspectsRoutes, witnessesRoutes, resolutionsRoutes, dashboardRoutes } from './routes';

const app = new Elysia()
  .use(cors())
  .use(swagger({
    documentation: {
      info: {
        title: 'Blotter Management System API',
        version: '1.0.0',
        description: 'Complete RESTful API for Blotter Management System with User, Officer, and Admin roles',
      },
      tags: [
        { name: 'Auth', description: 'Authentication endpoints' },
        { name: 'Users', description: 'User management endpoints' },
        { name: 'Reports', description: 'Blotter report endpoints' },
        { name: 'Suspects', description: 'Suspect management' },
        { name: 'Respondents', description: 'Respondent management' },
        { name: 'Witnesses', description: 'Witness management' },
        { name: 'Evidence', description: 'Evidence management' },
        { name: 'Hearings', description: 'Hearing management' },
        { name: 'Resolutions', description: 'Resolution management' },
        { name: 'Admin', description: 'Admin operations' },
      ],
    },
  }))
  .get('/', () => ({
    message: 'Blotter Management System API',
    version: '1.0.0',
    status: 'running',
    documentation: '/swagger',
  }))
  .get('/health', () => ({
    status: 'healthy',
    timestamp: new Date().toISOString(),
  }))
  .group('/api', (app) =>
    app
      .use(authRoutes)
      .use(usersRoutes)
      .use(reportsRoutes)
      .use(notificationsRoutes)
      .use(analyticsRoutes)
      .use(hearingsRoutes)
      .use(evidenceRoutes)
      .use(adminRoutes)
      .use(officersRoutes)
      .use(departmentsRoutes)
      .use(uploadRoutes)
      .use(searchRoutes)
      .use(exportRoutes)
      .use(logsRoutes)
      .use(suspectsRoutes)
      .use(witnessesRoutes)
      .use(resolutionsRoutes)
      .use(dashboardRoutes)
  )
  .listen({
    port: process.env.PORT || 3000,
    hostname: '0.0.0.0',
  });

console.log(`
╔════════════════════════════════════════════════════════════╗
║   Blotter Management System - Backend API                  ║
║   ✅ Server running on http://localhost:${process.env.PORT || 3000}               ║
║   📚 Documentation: http://localhost:${process.env.PORT || 3000}/swagger         ║
║   🗄️  Database: Connected to Neon PostgreSQL               ║
╚════════════════════════════════════════════════════════════╝
`);
