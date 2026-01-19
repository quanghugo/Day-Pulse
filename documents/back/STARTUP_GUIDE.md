# 🚀 DayPulse Backend - First Time Startup Guide

Complete guide to get the DayPulse backend services running from scratch.

---

## 📋 Prerequisites

### Required Software

1. **Java Development Kit 21**
   - Download: https://adoptium.net/
   - Verify: `java -version`
   - Should show: Java 21.x.x

2. **Maven 3.8+**
   - Verify: `mvn -version`
   - Should show: Apache Maven 3.8.x or higher

3. **PostgreSQL 15+**
   - Download: https://www.postgresql.org/download/
   - Verify: `psql --version`

4. **Git**
   - Verify: `git --version`

### Optional (Recommended)

- **pgAdmin** - PostgreSQL GUI tool
- **Postman** - API testing (alternative to curl)
- **IntelliJ IDEA** - IDE for development

---

## 🗄️ Database Setup

### Step 1: Start PostgreSQL Service

**Windows:**
```powershell
# Check if PostgreSQL is running
Get-Service -Name postgresql*

# Start if not running
Start-Service -Name postgresql-x64-15
```

**macOS:**
```bash
brew services start postgresql@15
```

**Linux:**
```bash
sudo systemctl start postgresql
sudo systemctl enable postgresql  # Start on boot
```

### Step 2: Create Databases

Open PostgreSQL command line:

```bash
# Connect to PostgreSQL
psql -U postgres

# Or on Windows:
psql -U postgres -W
```

Create the databases:

```sql
-- Create databases
CREATE DATABASE "auth-service";
CREATE DATABASE "user-service";

-- Verify databases were created
\l

-- Exit
\q
```

### Step 3: Verify Database Connection

Test connection to each database:

```bash
# Test auth-service database
psql -U postgres -d auth-service -c "SELECT version();"

# Test user-service database
psql -U postgres -d user-service -c "SELECT version();"
```

**Expected Output:** Should show PostgreSQL version information.

### Step 4: Update Database Credentials (If Needed)

If your PostgreSQL username/password is different from `postgres/postgres`:

**Auth Service:** Edit `backEnd/auth-service/src/main/resources/application.yaml`
```yaml
spring:
  datasource:
    username: YOUR_USERNAME
    password: YOUR_PASSWORD
```

**User Service:** Edit `backEnd/user-service/src/main/resources/application.yaml`
```yaml
spring:
  datasource:
    username: YOUR_USERNAME
    password: YOUR_PASSWORD
```

---

## 🏗️ Build Services

### Step 1: Navigate to Backend Directory

```bash
cd D:\Day-Pulse\backEnd
```

### Step 2: Build Auth Service

```bash
cd auth-service

# Clean and build
mvn clean install -DskipTests

# Verify build success
# Should see: BUILD SUCCESS
```

**Common Issues:**
- **Maven not found:** Add Maven to PATH
- **Java version mismatch:** Ensure Java 21 is active
- **Dependencies fail:** Check internet connection

### Step 3: Build API Gateway

```bash
cd ../api-gateway

# Clean and build
mvn clean install -DskipTests
```

### Step 4: Build User Service

```bash
cd ../user-service

# Clean and build
mvn clean install -DskipTests
```

**Expected Result:** All three services should show `BUILD SUCCESS`

---

## 🚀 Start Services

You need **three separate terminals** to run the services.

### Terminal 1: Auth Service

```bash
cd D:\Day-Pulse\backEnd\auth-service
mvn spring-boot:run
```

**Wait for:**
```
Started AuthServiceApplication in X.XXX seconds
```

**Verify:**
```bash
curl http://localhost:8080/auth-service/actuator/health
# Should return: {"status":"UP"}
```

### Terminal 2: User Service

```bash
cd D:\Day-Pulse\backEnd\user-service
mvn spring-boot:run
```

**Wait for:**
```
Started UserServiceApplication in X.XXX seconds
```

**Verify:**
```bash
curl http://localhost:8081/user-service/actuator/health
# Should return: {"status":"UP"}
```

### Terminal 3: API Gateway

```bash
cd D:\Day-Pulse\backEnd\api-gateway
mvn spring-boot:run
```

**Wait for:**
```
Started ApiGatewayApplication in X.XXX seconds
Netty started on port 8888
```

**Verify:**
```bash
curl http://localhost:8888/actuator/health
# Should return: {"status":"UP"}
```

---

## ✅ Verify Setup

### Check All Services are Running

**PowerShell:**
```powershell
# Check running Java processes
Get-Process java | Select-Object Id, ProcessName, MainWindowTitle

# Should see 3 Java processes
```

**Bash:**
```bash
# Check listening ports
netstat -an | grep "LISTEN" | grep "8080\|8081\|8888"

# Should see:
# TCP    0.0.0.0:8080    LISTENING  (Auth Service)
# TCP    0.0.0.0:8081    LISTENING  (User Service)
# TCP    0.0.0.0:8888    LISTENING  (API Gateway)
```

### Quick API Test

```bash
# Test registration endpoint
curl -X POST http://localhost:8888/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test123"}'

# Expected response:
# {"code":1000,"result":{"success":true,"email":"test@example.com"}}
```

If you see this response, **congratulations! Your backend is running! 🎉**

---

## 🧪 Run Complete Test Suite

### Option 1: Using the Bash Script

```bash
cd D:\Day-Pulse
chmod +x API_TEST.sh
./API_TEST.sh
```

**Expected:** Script will run all 19 test cases and show results.

### Option 2: Using the Manual Curl Commands

Follow the commands in `API_TEST_CURL.md` file.

### Option 3: Using Postman

1. Import the curl commands from `API_TEST_CURL.md`
2. Create environment variables:
   - `BASE_URL`: http://localhost:8888/api/v1
   - `USER1_ACCESS_TOKEN`: (set after login)
   - `USER2_ACCESS_TOKEN`: (set after login)
3. Run tests sequentially

---

## 📊 Database Verification

After running tests, verify data was created:

```sql
-- Connect to auth-service database
psql -U postgres -d auth-service

-- Check users
SELECT id, email, is_email_verified, is_setup_complete FROM users_auth;

-- Check refresh tokens
SELECT id, user_id, expires_at, revoked_at FROM refresh_tokens;

-- Exit
\q

-- Connect to user-service database
psql -U postgres -d user-service

-- Check profiles
SELECT id, username, name, bio FROM user_profiles;

-- Check follows
SELECT * FROM follows;

-- Check stats
SELECT * FROM user_stats;

-- Exit
\q
```

**Expected:** You should see the test users and their relationships.

---

## 🛑 Stopping Services

### Graceful Shutdown

In each terminal running a service:
1. Press `Ctrl + C`
2. Wait for "Stopped [ServiceName]Application"

### Force Stop (If Needed)

**PowerShell:**
```powershell
# Find Java processes
Get-Process java

# Kill specific process
Stop-Process -Id <PROCESS_ID>

# Kill all Java processes (careful!)
Get-Process java | Stop-Process
```

**Bash:**
```bash
# Find processes on ports
lsof -ti:8080,8081,8888

# Kill processes
kill $(lsof -ti:8080,8081,8888)
```

---

## 🐛 Troubleshooting

### Problem: Port Already in Use

**Error:** `Port 8080 is already in use`

**Solution:**
```powershell
# Windows: Find and kill process using port
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Linux/Mac:
lsof -ti:8080 | xargs kill -9
```

### Problem: Database Connection Failed

**Error:** `Could not connect to PostgreSQL`

**Check:**
1. PostgreSQL is running: `psql -U postgres -l`
2. Database exists: `\l` in psql
3. Credentials are correct in application.yaml
4. Firewall allows port 5432

### Problem: Maven Build Fails

**Error:** `Failed to execute goal`

**Solutions:**
```bash
# Clear Maven cache
rm -rf ~/.m2/repository

# Update Maven
mvn -v

# Check Java version
java -version  # Must be 21

# Try with verbose output
mvn clean install -X
```

### Problem: Service Starts But Endpoints Don't Work

**Check:**
1. Service logs for errors
2. Database schema created correctly:
   ```sql
   -- In psql
   \dt  # List tables
   ```
3. Port is accessible:
   ```bash
   curl http://localhost:8080/auth-service/actuator/health
   ```

### Problem: JWT Token Invalid

**Possible Causes:**
1. Token expired (default: 1 hour)
2. Logout was called (token revoked)
3. Service restarted (tokens invalidated)

**Solution:** Login again to get new token.

---

## 📝 Configuration Files Reference

### Auth Service Configuration

**File:** `backEnd/auth-service/src/main/resources/application.yaml`

```yaml
server:
  port: 8080                    # Auth service port
  servlet:
    context-path: /auth-service # Base path

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/auth-service
    username: postgres          # Change if needed
    password: postgres          # Change if needed

jwt:
  signing-key: <base64-key>     # Keep this secret!
  valid-duration: 3600          # 1 hour
  refreshable-duration: 36000   # 10 hours
```

### User Service Configuration

**File:** `backEnd/user-service/src/main/resources/application.yaml`

```yaml
server:
  port: 8081                    # User service port
  servlet:
    context-path: /user-service

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/user-service
    username: postgres
    password: postgres
```

### API Gateway Configuration

**File:** `backEnd/api-gateway/src/main/resources/application.yaml`

```yaml
server:
  port: 8888                    # Gateway port

spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: http://localhost:8080
          predicates:
            - Path=/api/v1/auth/**
        
        - id: user-service
          uri: http://localhost:8081
          predicates:
            - Path=/api/v1/users/**

jwt:
  signing-key: <base64-key>     # Must match auth-service!
```

---

## 🔒 Security Notes

### Development Environment

Current configuration is for **DEVELOPMENT ONLY**:
- Default PostgreSQL credentials (postgres/postgres)
- CORS allows localhost origins
- JWT signing key is in source code
- No rate limiting
- No HTTPS

### Production Checklist

Before deploying to production:

- [ ] Change database credentials
- [ ] Store JWT key in environment variables
- [ ] Enable HTTPS/TLS
- [ ] Configure proper CORS origins
- [ ] Enable rate limiting
- [ ] Set up monitoring
- [ ] Use production database (not localhost)
- [ ] Enable database connection pooling
- [ ] Set up backup strategy
- [ ] Configure log aggregation

---

## 📚 Next Steps

After successful startup:

1. **Read API Documentation:** `API_TEST_CURL.md`
2. **Read Code Review:** `CODE_REVIEW_REPORT.md`
3. **Read Refactoring Summary:** `REFACTORING_SUMMARY.md`
4. **Read Backend Design:** `documents/back/BACKEND_DESIGN.md`

---

## 🤝 Getting Help

If you encounter issues:

1. **Check Service Logs**
   - Look for error messages in terminal
   - Check for port conflicts
   - Verify database connections

2. **Verify Prerequisites**
   - Java 21 installed
   - PostgreSQL running
   - Databases created
   - Maven working

3. **Common Solutions**
   - Restart services
   - Clear Maven cache
   - Drop and recreate databases
   - Check firewall settings

---

## 🎉 Success Indicators

You know everything is working when:

✅ All 3 services start without errors  
✅ Health check endpoints return `{"status":"UP"}`  
✅ Database tables are created automatically  
✅ Registration endpoint creates users  
✅ Login endpoint returns JWT tokens  
✅ Profile setup works with authentication  
✅ Follow/unfollow operations work  

**If all these work, you're ready to develop! 🚀**

---

**Last Updated:** 2026-01-19  
**Services Version:** v0.0.1  
**Java Version:** 21  
**Spring Boot Version:** 3.5.10-SNAPSHOT
