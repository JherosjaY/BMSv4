# Blotter Management System - Deployment Guide

## Environment Variables Required

### Database
```
DATABASE_URL=postgresql://user:password@host/dbname
```

### Email Service (Gmail SMTP)
```
GMAIL_USER=your-email@gmail.com
GMAIL_PASSWORD=your-app-password
```

### Cloudinary (Image Storage)
```
CLOUDINARY_CLOUD_NAME=doRyBtam
CLOUDINARY_API_KEY=331777292844342
CLOUDINARY_API_SECRET=WadMuNA_5INDBmB0gnQyONhUmvg
```

### Firebase Cloud Messaging (Push Notifications)
```
FIREBASE_PROJECT_ID=blotter-management-syste-5ee0f
FIREBASE_SERVICE_ACCOUNT_PATH=/path/to/firebase-service-account.json
```

### JWT Secret
```
JWT_SECRET=your-secret-key-here
```

## Deployment Steps

### 1. Push Code to GitHub
```bash
git add .
git commit -m "Add FCM, Cloudinary, and Email services"
git push origin main
```

### 2. Deploy to Render

1. Go to [Render Dashboard](https://dashboard.render.com)
2. Select your backend service
3. Go to **Settings** → **Environment**
4. Add all environment variables listed above
5. Click **Deploy** or wait for auto-deploy

### 3. Verify Deployment

Check Render logs to ensure:
- ✅ Backend starts successfully
- ✅ Database connection established
- ✅ Firebase Admin SDK initialized
- ✅ Cloudinary configured
- ✅ Email service ready

## API Endpoints

### Authentication
- `POST /api/auth/login` - Login with username/password
- `POST /api/auth/register` - Register new user
- `POST /api/auth/google-signin` - Sign in with Google
- `POST /api/auth/forgot-password` - Request password reset
- `POST /api/auth/reset-password` - Reset password with code
- `POST /api/auth/verify-email` - Verify email with code

### Users
- `GET /api/users` - Get all users
- `GET /api/users/:id` - Get user profile
- `PUT /api/users/:id` - Update user profile
- `DELETE /api/users/:id` - Delete user account
- `POST /api/users/:id/change-password` - Change password
- `POST /api/users/fcm-token` - Save FCM token
- `POST /api/users/:id/upload-photo` - Upload profile photo
- `PUT /api/users/:id/settings` - Update user settings

### Reports
- `GET /api/reports` - Get all reports
- `GET /api/reports/:id` - Get report by ID
- `POST /api/reports` - Create report
- `PUT /api/reports/:id` - Update report
- `DELETE /api/reports/:id` - Delete report
- `GET /api/reports/status/:status` - Get reports by status

### Notifications
- `GET /api/notifications/:userId` - Get user notifications
- `POST /api/notifications` - Create notification
- `POST /api/notifications/send-push` - Send push notification
- `PUT /api/notifications/:id/read` - Mark as read
- `DELETE /api/notifications/:id` - Delete notification

### Officers
- `GET /api/officers` - Get all officers
- `POST /api/officers` - Create officer
- `PUT /api/officers/:id` - Update officer
- `DELETE /api/officers/:id` - Delete officer
- `POST /api/officers/:id/send-credentials` - Send credentials via email

## Testing

### Test Email Service
```bash
curl -X POST http://localhost:3000/api/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com"}'
```

### Test Push Notifications
```bash
curl -X POST http://localhost:3000/api/notifications/send-push \
  -H "Content-Type: application/json" \
  -d '{
    "userId":"user-uuid",
    "title":"Test Notification",
    "message":"This is a test",
    "type":"test"
  }'
```

### Test Image Upload
```bash
curl -X POST http://localhost:3000/api/users/user-uuid/upload-photo \
  -H "Content-Type: application/json" \
  -d '{"imageUrl":"https://example.com/photo.jpg"}'
```

## Troubleshooting

### Firebase Admin SDK Error
- Ensure `FIREBASE_SERVICE_ACCOUNT_PATH` points to valid JSON file
- Check `FIREBASE_PROJECT_ID` matches your Firebase project

### Cloudinary Error
- Verify API credentials are correct
- Check cloud name, API key, and API secret

### Email Not Sending
- Verify Gmail App Password (not regular password)
- Check `GMAIL_USER` and `GMAIL_PASSWORD` environment variables
- Ensure "Less secure app access" is enabled (if using regular password)

### Database Connection Error
- Verify `DATABASE_URL` is correct
- Check Neon database is running
- Ensure IP is whitelisted

## Monitoring

Monitor your deployment in Render:
1. Check **Logs** for errors
2. Check **Metrics** for performance
3. Check **Events** for deployment status

## Support

For issues, check:
- Render logs
- Firebase Console
- Cloudinary Dashboard
- Gmail account settings
