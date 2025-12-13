# Security Guide - Blotter Management System

## 🔒 CRITICAL SECURITY PRACTICES

### 1. NEVER Commit Sensitive Files

**NEVER commit these to GitHub:**
- ❌ Firebase service account JSON files
- ❌ `.env` files with credentials
- ❌ API keys or secrets
- ❌ Private keys (`.key`, `.pem`)
- ❌ Database credentials

**These are protected in `.gitignore`:**
```
firebase-service-account.json
*firebase*adminsdk*.json
.env
.env.production
credentials.json
secrets.json
```

---

## 🔐 Firebase Service Account - SECURE SETUP

### The Problem
Firebase service account files contain **private keys** that can compromise your entire Firebase project if exposed.

### The Solution: Use Environment Variables

**Instead of storing the file, store the JSON as an environment variable:**

#### Step 1: Get Your Firebase Service Account JSON
1. Go to [Firebase Console](https://console.firebase.google.com)
2. Select: **blotter-management-syste-5ee0f**
3. Go to **Settings** → **Service Accounts**
4. Click **Generate New Private Key**
5. Save the JSON file locally (NOT in repo)

#### Step 2: Convert JSON to Environment Variable
The JSON file looks like:
```json
{
  "type": "service_account",
  "project_id": "blotter-management-syste-5ee0f",
  "private_key_id": "...",
  "private_key": "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n",
  "client_email": "...",
  ...
}
```

**Copy the ENTIRE JSON as a single line** (no newlines):
```
{"type":"service_account","project_id":"blotter-management-syste-5ee0f",...}
```

#### Step 3: Add to Render Environment Variables
1. Go to Render Dashboard
2. Select backend service
3. Go to **Settings** → **Environment**
4. Add new variable:
   - **Name:** `FIREBASE_SERVICE_ACCOUNT_JSON`
   - **Value:** (paste the entire JSON as one line)
5. Click **Save**

#### Step 4: Code Handles It Securely
The `fcmService.ts` now:
- ✅ Reads from `FIREBASE_SERVICE_ACCOUNT_JSON` env var
- ✅ Parses the JSON string
- ✅ Validates all required fields
- ✅ Initializes Firebase Admin SDK
- ✅ Never writes to disk

---

## 📋 Environment Variables Checklist

### Required Variables
```
# Database
DATABASE_URL=postgresql://user:password@host/dbname

# Email Service
GMAIL_USER=official.bms.2025@gmail.com
GMAIL_PASSWORD=lksouhuhlacowobv

# Cloudinary
CLOUDINARY_CLOUD_NAME=doRyBtam
CLOUDINARY_API_KEY=331777292844342
CLOUDINARY_API_SECRET=WadMuNA_5INDBmB0gnQyONhUmvg

# Firebase (SECURE - as JSON string)
FIREBASE_PROJECT_ID=blotter-management-syste-5ee0f
FIREBASE_SERVICE_ACCOUNT_JSON={"type":"service_account",...}

# JWT
JWT_SECRET=your-secret-key-here

# Environment
NODE_ENV=production
```

### ✅ DO
- ✅ Store all secrets in environment variables
- ✅ Use `.gitignore` to prevent commits
- ✅ Rotate credentials periodically
- ✅ Use strong, unique secrets
- ✅ Limit access to credentials
- ✅ Monitor for exposed credentials

### ❌ DON'T
- ❌ Commit `.env` files
- ❌ Commit JSON credential files
- ❌ Share credentials in chat/email
- ❌ Use weak or default secrets
- ❌ Hardcode secrets in code
- ❌ Store credentials in version control

---

## 🚨 If Credentials Are Exposed

**Immediately:**
1. Revoke the exposed credentials
2. Generate new ones
3. Update environment variables
4. Redeploy the application
5. Monitor for unauthorized access

### Revoke Firebase Credentials
1. Go to Firebase Console
2. Settings → Service Accounts
3. Delete the compromised key
4. Generate a new one

### Revoke Gmail App Password
1. Go to [Google Account Security](https://myaccount.google.com/security)
2. App passwords
3. Delete the compromised password
4. Generate a new one

### Revoke Cloudinary Credentials
1. Go to Cloudinary Dashboard
2. Settings → Security
3. Regenerate API Key/Secret

---

## 🔍 Verify Security

### Check .gitignore is Working
```bash
# List files that would be committed
git status

# Should NOT show:
# - firebase-service-account.json
# - .env files
# - credentials.json
```

### Check Environment Variables
```bash
# On Render, verify all variables are set
# Go to Settings → Environment
# Verify all required variables are present
```

### Check fcmService Initialization
```bash
# Check Render logs for:
# ✅ Firebase Admin SDK initialized for FCM
# ✅ Using Firebase project: blotter-management-syste-5ee0f
```

---

## 📚 Security Best Practices

### 1. Principle of Least Privilege
- Only grant necessary permissions
- Use service accounts with limited scope
- Rotate credentials regularly

### 2. Secure Communication
- Always use HTTPS (Render provides this)
- Validate all inputs
- Sanitize all outputs

### 3. Monitoring & Logging
- Monitor for failed authentication attempts
- Log all API calls
- Alert on suspicious activity

### 4. Backup & Recovery
- Keep backups of important data
- Test recovery procedures
- Document disaster recovery plan

---

## 🛡️ Security Checklist

- [ ] All credentials in environment variables
- [ ] `.gitignore` prevents credential commits
- [ ] Firebase service account NOT in repo
- [ ] `.env` files NOT in repo
- [ ] All API keys rotated recently
- [ ] Strong JWT secret configured
- [ ] HTTPS enabled on Render
- [ ] Database password is strong
- [ ] Gmail App Password is secure
- [ ] Cloudinary credentials are safe
- [ ] Firebase project access is restricted
- [ ] Regular security audits planned

---

## 📞 Support

If you suspect a security breach:
1. Immediately revoke all credentials
2. Check Render logs for unauthorized access
3. Review Firebase Console activity
4. Regenerate all secrets
5. Redeploy application

**Never ignore security warnings!** 🔒
