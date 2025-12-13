# 🚀 LOCAL BACKEND SETUP - FAST DEVELOPMENT

## **Quick Start (5 minutes)**

### **Step 1: Install Dependencies**
```bash
cd BMSv3-Backend
npm install
```

### **Step 2: Start the Backend**
```bash
bun run start
```

**Your backend is now running at:**
```
http://localhost:3000
```

**Swagger Documentation:**
```
http://localhost:3000/swagger
```

---

## **📱 Update Android App**

In your Android app, find the API base URL and change it to:

```java
// For LOCAL development
String API_BASE_URL = "http://localhost:3000";

// For PRODUCTION (Render.com)
// String API_BASE_URL = "https://bms-backend-exeb.onrender.com";
```

---

## **🧪 Test the Connection**

### **Option 1: Use Swagger UI**
1. Open: `http://localhost:3000/swagger`
2. Try endpoints directly in browser
3. Test Register, Login, Create Report, etc.

### **Option 2: Use Android App**
1. Start Android emulator
2. Run your app
3. Test login, create report, etc.
4. Check Logcat for API responses

---

## **🔧 Database Configuration**

### **Current Setup:**
- Uses **Neon PostgreSQL** (cloud)
- Connection string in `.env` file

### **To Use LOCAL PostgreSQL:**
1. Install PostgreSQL locally
2. Create a local database
3. Update `.env` with local connection string:
```
DATABASE_URL=postgresql://user:password@localhost:5432/bms_local
```

### **To Use SQLite (Easiest):**
1. Update `drizzle.config.ts` to use SQLite
2. Update `src/db/index.ts` to use SQLite driver
3. Run migrations

---

## **📊 API ENDPOINTS - TEST THESE:**

### **Authentication**
```
POST http://localhost:3000/api/auth/register
POST http://localhost:3000/api/auth/login
PUT  http://localhost:3000/api/auth/profile/:userId
```

### **Users**
```
GET    http://localhost:3000/api/users
GET    http://localhost:3000/api/users/:id
PUT    http://localhost:3000/api/users/:id
DELETE http://localhost:3000/api/users/:id
POST   http://localhost:3000/api/users/fcm-token
```

### **Reports**
```
GET    http://localhost:3000/api/reports
GET    http://localhost:3000/api/reports/:id
POST   http://localhost:3000/api/reports
PUT    http://localhost:3000/api/reports/:id
DELETE http://localhost:3000/api/reports/:id
GET    http://localhost:3000/api/reports/status/:status
```

---

## **🐛 Debugging**

### **Check Backend Logs**
```
✅ Database connection established
✅ Server running on http://localhost:3000
✅ Documentation: http://localhost:3000/swagger
```

### **Check Android Logcat**
```
Filter: "API" or "Network"
Look for: Request/Response logs
```

### **Common Issues:**

**Issue:** `Connection refused`
- **Fix:** Make sure backend is running with `bun run start`

**Issue:** `CORS error`
- **Fix:** Backend has CORS enabled, should work

**Issue:** `Database connection failed`
- **Fix:** Check `.env` DATABASE_URL is correct

---

## **✅ WORKFLOW:**

1. **Make backend changes**
2. **Backend auto-reloads** (if using watch mode)
3. **Test in Swagger or Android app**
4. **If working → Great!**
5. **If error → Check logs → Fix → Retry**
6. **Once everything works → Deploy to Render.com**

---

## **🎯 NEXT STEPS:**

1. Run `bun run start`
2. Open `http://localhost:3000/swagger`
3. Test a few endpoints
4. Update Android app to use `http://localhost:3000`
5. Test from Android app
6. Debug any issues
7. When ready → Deploy to Render.com

**Happy coding!** 🚀
