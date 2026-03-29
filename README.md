# OrthoPro Connect — Backend

Spring Boot REST API for the OrthoPro school management system.

## What changed from the original

| Before | After |
|--------|-------|
| 6 config files (CorsConfig, WebConfig, WebConfig2, SecurityConfig, StaticResourceConfig, WebSecurityConfig) | **1 file**: `AppConfig.java` |
| Hardcoded `C:/Users/LENOVO/Desktop/...` paths | All paths via **environment variables** |
| DB credentials in `application.properties` | Env vars: `DATABASE_URL`, `DB_USERNAME`, `DB_PASSWORD` |
| CORS origins hardcoded | Env var: `CORS_ORIGINS` |
| Port hardcoded to 8081 | Env var: `PORT` (auto-injected by cloud platforms) |
| Duplicate DataInitializer classes | Single clean initializer |

---

## Local development

### Prerequisites
- Java 17+
- PostgreSQL running locally
- Gradle 8+

### Steps

```bash
# 1. Clone / extract the project
cd orthoproconnect

# 2. Create your local env file
cp .env.example .env
# Edit .env with your local DB credentials

# 3. Set env vars and run (Linux/Mac)
export $(cat .env | xargs)
./gradlew bootRun

# On Windows PowerShell:
# Get-Content .env | ForEach-Object { $v = $_ -split '=',2; [System.Environment]::SetEnvironmentVariable($v[0], $v[1]) }
# .\gradlew.bat bootRun
```

The API will start on `http://localhost:8081`.  
Default admin account: `admin@orthoproconnect.com` / `admin123` (change it!)

---

## Free cloud deployment (Railway — recommended)

Railway gives you **free PostgreSQL + free app hosting** with zero credit card for small projects.

### Step 1 — Push to GitHub
```bash
git init
git add .
git commit -m "initial commit"
gh repo create orthoproconnect --public --push
# or push to your existing GitHub repo
```

### Step 2 — Create Railway project
1. Go to [railway.app](https://railway.app) → **New Project**
2. Click **Deploy from GitHub repo** → select your repo
3. Railway auto-detects Gradle and builds with `./gradlew bootJar`

### Step 3 — Add PostgreSQL
1. In your Railway project → **+ New** → **Database** → **PostgreSQL**
2. Railway automatically injects `DATABASE_URL` into your app ✅

### Step 4 — Set environment variables
In Railway → your service → **Variables**, add:

| Variable | Value |
|----------|-------|
| `DB_USERNAME` | (from Railway PostgreSQL → Variables → `PGUSER`) |
| `DB_PASSWORD` | (from Railway PostgreSQL → Variables → `PGPASSWORD`) |
| `CORS_ORIGINS` | your frontend URLs, comma-separated |
| `QRCODE_SALT` | any random secret string |

> `DATABASE_URL` and `PORT` are **auto-injected by Railway** — don't add them.

### Step 5 — Deploy
Railway builds and deploys automatically on every `git push`. 🎉

Your API URL will look like: `https://orthoproconnect-production.up.railway.app`

---

## Alternative: Render.com (also free)

1. Go to [render.com](https://render.com) → **New Web Service** → connect GitHub
2. Build command: `./gradlew bootJar -x test`
3. Start command: `java -jar build/libs/orthoproconnect.jar`
4. Add a **PostgreSQL** database (free tier)
5. Copy the **Internal Database URL** → set as `DATABASE_URL` env var
6. Set `DB_USERNAME`, `DB_PASSWORD`, `CORS_ORIGINS`, `QRCODE_SALT`

---

## Updating frontend projects

After deployment, update the API base URL in your frontend files:

### WebApp — file: `api-service.js`
```js
// Change this line:
const API_BASE_URL = 'http://localhost:8081';
// To:
const API_BASE_URL = 'https://your-app.up.railway.app';
```

### orthoStock — file: `config/api-config.js`
```js
const API_BASE_URL = 'https://your-app.up.railway.app';
```

### MobApp — file: `app/src/main/java/com/orthopro/mobapp/database/RetrofitClient.kt`
```kotlin
private const val BASE_URL = "https://your-app.up.railway.app/"
```

Don't forget to add your frontend URL to `CORS_ORIGINS` on Railway.

---

## API Endpoints

| Resource | Base URL |
|----------|----------|
| Health | `GET /api/health` |
| Admins | `/api/admins` |
| Teachers | `/api/teachers` |
| Students | `/api/students` |
| Appointments | `/api/appointments` |
| Pieces (stock) | `/api/pieces` |
| Transactions | `/api/transactions` |
| Documents | `/api/documents` |
| 3D Models | `/api/models` |

