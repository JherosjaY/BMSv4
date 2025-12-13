# Firebase Setup - Secure Environment Variable Method

## 🔐 SECURE SETUP (No Files in Repo)

### Step 1: Download Firebase Service Account

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Select project: **blotter-management-syste-5ee0f**
3. Click **Settings** (gear icon) → **Project Settings**
4. Go to **Service Accounts** tab
5. Click **Generate New Private Key**
6. Save the JSON file locally (e.g., `firebase-key.json`)
7. **DO NOT commit this file to GitHub**

### Step 2: Convert JSON to Environment Variable

The downloaded JSON looks like:
```json
{
  "type": "service_account",
  "project_id": "blotter-management-syste-5ee0f",
  "private_key_id": "0eaf6a9dcdb5f51d0285ab5d3435e5bb029e52c4",
  "private_key": "-----BEGIN PRIVATE KEY-----\nMIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQC23y6veMaKTguN\n...\n-----END PRIVATE KEY-----\n",
  "client_email": "firebase-adminsdk-fbsvc@blotter-management-syste-5ee0f.iam.gserviceaccount.com",
  "client_id": "103341866456585774454",
  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
  "token_uri": "https://oauth2.googleapis.com/token",
  "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
  "client_x509_cert_url": "https://www.googleapis.com/robot/v1/metadata/x509/firebase-adminsdk-fbsvc%40blotter-management-syste-5ee0f.iam.gserviceaccount.com",
  "universe_domain": "googleapis.com"
}
```

**Convert to single-line JSON (remove all newlines):**

Option A: Use Python
```bash
python3 -c "import json; print(json.dumps(json.load(open('firebase-key.json'))))"
```

Option B: Use Node.js
```bash
node -e "console.log(JSON.stringify(require('./firebase-key.json')))"
```

Option C: Manual (copy entire JSON, remove newlines)
```
{"type":"service_account","project_id":"blotter-management-syste-5ee0f","private_key_id":"0eaf6a9dcdb5f51d0285ab5d3435e5bb029e52c4","private_key":"-----BEGIN PRIVATE KEY-----\nMIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQC23y6veMaKTguN\n...\n-----END PRIVATE KEY-----\n",...}
```

### Step 3: Add to Render Environment Variables

1. Go to [Render Dashboard](https://dashboard.render.com)
2. Select your **BMS-backend** service
3. Click **Settings**
4. Go to **Environment** section
5. Click **Add Environment Variable**
6. **Name:** `FIREBASE_SERVICE_ACCOUNT_JSON`
7. **Value:** (paste the single-line JSON from Step 2)
8. Click **Save**

### Step 4: Verify Setup

Check Render logs:
```
✅ Firebase Admin SDK initialized for FCM
✅ Using Firebase project: blotter-management-syste-5ee0f
```

If you see errors:
```
❌ Invalid FIREBASE_SERVICE_ACCOUNT_JSON format
❌ Ensure the JSON is properly formatted and escaped
```

**Solution:** Make sure the JSON is on a single line with no extra newlines.

---

## 📋 All Required Environment Variables

```
# Database
DATABASE_URL=postgresql://user:password@host/dbname

# Email Service
GMAIL_USER=your-email@gmail.com
GMAIL_PASSWORD=your-app-password

# Cloudinary
CLOUDINARY_CLOUD_NAME=your-cloud-name
CLOUDINARY_API_KEY=your-api-key
CLOUDINARY_API_SECRET=your-api-secret

# Firebase (SECURE - as JSON string, no file)
FIREBASE_PROJECT_ID=blotter-management-syste-5ee0f
FIREBASE_SERVICE_ACCOUNT_JSON={"type":"service_account",...entire json...}

# JWT
JWT_SECRET=your-jwt-secret-key

# Environment
NODE_ENV=production
```

---

## ✅ Security Checklist

- [ ] Firebase service account JSON downloaded locally
- [ ] JSON converted to single-line format
- [ ] `FIREBASE_SERVICE_ACCOUNT_JSON` added to Render
- [ ] `firebase-service-account.json` NOT in Git repo
- [ ] `.gitignore` includes Firebase files
- [ ] Render logs show successful initialization
- [ ] No sensitive files in GitHub
- [ ] All environment variables set in Render

---

## 🚀 Deploy

Once environment variables are set:

1. Push code to GitHub
2. Render auto-deploys
3. Check logs for success
4. Test FCM notifications

---

## 🔒 Security Notes

- ✅ Firebase credentials stored as environment variable
- ✅ No credential files in repository
- ✅ Credentials encrypted in Render
- ✅ Private keys never exposed
- ✅ Can rotate credentials anytime
- ✅ Follows industry best practices

---

## 🆘 Troubleshooting

### Error: "FIREBASE_SERVICE_ACCOUNT_JSON environment variable not set"
- **Solution:** Add the environment variable to Render
- Check spelling: `FIREBASE_SERVICE_ACCOUNT_JSON` (exact case)

### Error: "Invalid FIREBASE_SERVICE_ACCOUNT_JSON format"
- **Solution:** Ensure JSON is on a single line
- Remove all newlines from the JSON
- Test JSON validity: `echo '{"type":"service_account",...}' | jq .`

### Error: "Firebase service account JSON is missing required fields"
- **Solution:** Verify the JSON has these fields:
  - `type`
  - `project_id`
  - `private_key`
  - `client_email`

### FCM Not Sending
- Check Render logs for Firebase initialization
- Verify `FIREBASE_SERVICE_ACCOUNT_JSON` is set
- Verify user has FCM token saved
- Check Firebase Console for quota limits

---

## 📞 Support

For issues:
1. Check Render logs
2. Verify environment variables
3. Check Firebase Console
4. Review SECURITY.md for best practices
