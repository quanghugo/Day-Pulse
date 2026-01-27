# Quick Start Guide

Get DayPulse running on your local machine in **under 10 minutes**.

---

## ✅ Prerequisites Checklist

Before starting, ensure you have:

- [ ] **Java 21** installed ([Download](https://www.oracle.com/java/technologies/downloads/#java21))
- [ ] **Maven 3.8+** installed ([Download](https://maven.apache.org/download.cgi))
- [ ] **PostgreSQL 15+** running ([Download](https://www.postgresql.org/download/))
- [ ] **Node.js 18+** and **npm** (for frontend) ([Download](https://nodejs.org/))
- [ ] **Git** installed ([Download](https://git-scm.com/downloads))

**Verify installations:**

```bash
java -version    # Should show Java 21
mvn -version     # Should show Maven 3.8+
psql --version   # Should show PostgreSQL 15+
node -version    # Should show v18+
npm -version     # Should show 9+
```

---

## 🚀 Quick Setup (Backend Only)

### Step 1: Clone Repository

```bash
git clone <repository-url>
cd DayPulse
```

### Step 2: Create Databases

```bash
# Connect to PostgreSQL
psql -U postgres

# Create databases
CREATE DATABASE "auth-service";
CREATE DATABASE "user-service";
\q
```

> 💡 **Tip:** Default PostgreSQL credentials are `postgres/postgres`. Update `application.yaml` if yours differ.

### Step 3: Build Services

```bash
# Build Auth Service
cd backEnd/auth-service
mvn clean install -DskipTests

# Build User Service
cd ../user-service
mvn clean install -DskipTests

# Build API Gateway
cd ../api-gateway
mvn clean install -DskipTests
```

> ⏱️ **Time:** ~2-3 minutes depending on internet speed (downloading dependencies)

### Step 4: Start Services

**Open 3 separate terminal windows:**

**Terminal 1 - Auth Service:**

```bash
cd backEnd/auth-service
mvn spring-boot:run
```

Wait for: `Started AuthServiceApplication`

**Terminal 2 - User Service:**

```bash
cd backEnd/user-service
mvn spring-boot:run
```

Wait for: `Started UserServiceApplication`

**Terminal 3 - API Gateway:**

```bash
cd backEnd/api-gateway
mvn spring-boot:run
```

Wait for: `Started ApiGatewayApplication`

### Step 5: Verify Backend

```bash
# Check API Gateway health
curl http://localhost:8188/actuator/health

# Expected response: {"status":"UP"}
```

**All services running? ✅ You're ready to go!**

---

## 🖥️ Full Setup (Backend + Frontend)

### Additional Prerequisites

- [ ] **Node.js 18+** and **npm**

### Step 6: Setup Frontend

```bash
# Navigate to frontend directory
cd frontEnd

# Install dependencies
npm install

# Create environment file
cp .env.local .env

# Start development server
npm run dev
```

### Step 7: Open Application

Open your browser and navigate to:

- **Frontend:** http://localhost:5173
- **API Gateway:** http://localhost:8188

---

## 🔐 Optional: Keycloak Setup

For OAuth/OIDC authentication (optional for development):

### Prerequisites

- [ ] **Docker** and **Docker Compose** installed

### Start Keycloak

```bash
# From project root
docker-compose up -d

# Wait for Keycloak to start (~60 seconds)
docker-compose logs -f keycloak
```

**Access Keycloak Admin Console:**

- **URL:** http://localhost:8888
- **Username:** `admin`
- **Password:** `admin`

> 📖 **Full setup guide:** See [keycloak-setup.md](keycloak-setup.md)

---

## ✅ Verification Checklist

After setup, verify everything works:

### Backend Verification

```bash
# 1. Check all services are healthy
curl http://localhost:8188/actuator/health  # API Gateway
curl http://localhost:8180/auth-service/actuator/health  # Auth Service
curl http://localhost:8181/user-service/actuator/health  # User Service

# 2. Test signup
curl -X POST http://localhost:8188/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'

# Expected: {"code":201,"message":"Created",...}

# 3. Test login
curl -X POST http://localhost:8188/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'

# Expected: {"code":200,"result":{"user":{...},"tokens":{...}}}
```

### Frontend Verification

1. Open http://localhost:5173
2. You should see the DayPulse login page
3. Click "Sign Up" - form should appear
4. Click "Login" - form should appear

---

## 🎯 What's Next?

Now that everything is running, explore these areas:

### For Backend Developers

1. **Understand the architecture:** [System Architecture](SYSTEM_ARCHITECTURE.md)
2. **Learn the APIs:** [API Reference](back/API_REFERENCE.md)
3. **Read coding standards:** [Development Guide](back/DEVELOPMENT_GUIDE.md)
4. **Explore services:** [Services Documentation](back/services/README.md)

### For Frontend Developers

1. **Understand the app structure:** [Frontend Architecture](front/ARCHITECTURE.md)
2. **Learn feature flows:** [Feature Flows](front/FEATURE_FLOWS.md)
3. **Setup your IDE:** [Getting Started](front/GETTING_STARTED.md)

### For Everyone

1. **Learn terminology:** [Glossary](GLOSSARY.md)
2. **Test APIs:** [API Testing Guide](API_TESTING.md)
3. **Contribute:** [Contributing Guide](CONTRIBUTING.md)

---

## 🐛 Troubleshooting

### Services won't start

**Problem:** Port already in use

```
Port 8180 was already in use
```

**Solution:** Kill process using the port

```bash
# Windows
netstat -ano | findstr :8180
taskkill /PID <PID> /F

# macOS/Linux
lsof -i :8180
kill -9 <PID>
```

---

### Database connection errors

**Problem:** Cannot connect to PostgreSQL

```
Connection refused: postgres
```

**Solutions:**

1. ✅ Verify PostgreSQL is running:

   ```bash
   # Windows
   sc query postgresql-x64-15

   # macOS
   brew services list | grep postgresql

   # Linux
   systemctl status postgresql
   ```

2. ✅ Check database exists:

   ```bash
   psql -U postgres -l | grep "auth-service"
   psql -U postgres -l | grep "user-service"
   ```

3. ✅ Verify credentials in `application.yaml`:
   ```yaml
   spring:
     datasource:
       username: postgres # Update if different
       password: postgres # Update if different
   ```

---

### Build failures

**Problem:** Maven build fails

```
Failed to execute goal ... compilation failure
```

**Solutions:**

1. ✅ Clean Maven cache:

   ```bash
   mvn clean
   rm -rf ~/.m2/repository
   ```

2. ✅ Verify Java version:

   ```bash
   java -version  # Must be Java 21
   ```

3. ✅ Update dependencies:
   ```bash
   mvn clean install -U
   ```

---

### JWT token errors

**Problem:** "Unauthenticated" on API calls

```
{"code":1006,"message":"Unauthenticated"}
```

**Solutions:**

1. ✅ Check token hasn't expired (default: 1 hour)
2. ✅ Verify `Authorization: Bearer <token>` header format
3. ✅ Ensure JWT signing key matches across services:

   ```yaml
   # auth-service/application.yaml
   jwt:
     signing-key: "very-secure-secret-key-at-least-512-bits-long-for-HS512-algorithm"

   # api-gateway/application.yaml
   jwt:
     signing-key: "very-secure-secret-key-at-least-512-bits-long-for-HS512-algorithm"
   ```

---

### Frontend won't start

**Problem:** `npm run dev` fails

```
Error: Cannot find module...
```

**Solutions:**

1. ✅ Delete node_modules and reinstall:

   ```bash
   cd frontEnd
   rm -rf node_modules package-lock.json
   npm install
   ```

2. ✅ Clear npm cache:

   ```bash
   npm cache clean --force
   ```

3. ✅ Verify Node.js version:
   ```bash
   node -version  # Should be v18+
   ```

---

### Still having issues?

1. **Check detailed guides:**
   - [Backend Development Guide](back/DEVELOPMENT_GUIDE.md#troubleshooting)
   - [Keycloak Troubleshooting](back/TROUBLESHOOTING_KEYCLOAK.md)

2. **Verify prerequisites:**
   - All required software installed and correct versions
   - All services started in correct order
   - Database created and accessible

3. **Check logs:**
   ```bash
   # Backend service logs appear in terminal where you ran mvn spring-boot:run
   # Look for ERROR or WARN messages
   ```

---

## 📚 Additional Resources

- **[Complete Documentation](README.md)** - Full documentation index
- **[System Architecture](SYSTEM_ARCHITECTURE.md)** - Understand the big picture
- **[API Reference](back/API_REFERENCE.md)** - All available endpoints
- **[Glossary](GLOSSARY.md)** - DayPulse terminology

---

**Setup Time:** ~10 minutes  
**Difficulty:** Beginner-friendly  
**Last Updated:** 2026-01-27
