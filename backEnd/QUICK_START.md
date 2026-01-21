# Day-Pulse Backend - Quick Start Guide

## 🚀 5-Minute Setup

### 1. Prerequisites Check
```bash
# Verify Java
java -version  # Should be 21+

# Verify Maven
mvn -version   # Should be 3.8+

# Verify PostgreSQL
psql --version # Should be 14+
```

### 2. Database Setup
```bash
# Start PostgreSQL (if not running)
# Windows: Start from Services
# Mac: brew services start postgresql
# Linux: sudo systemctl start postgresql

# Create databases
psql -U postgres -c "CREATE DATABASE \"auth-service\";"
psql -U postgres -c "CREATE DATABASE \"user-service\";"
```

### 3. Start Services
```bash
# Terminal 1 - Auth Service
cd backEnd/auth-service
mvn spring-boot:run

# Terminal 2 - User Service  
cd backEnd/user-service
mvn spring-boot:run

# Terminal 3 - API Gateway
cd backEnd/api-gateway
mvn spring-boot:run
```

### 4. Test APIs

**Signup**:
```bash
curl -X POST http://localhost:8188/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email": "demo@example.com", "password": "demo123"}'
```

**Login**:
```bash
curl -X POST http://localhost:8188/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "demo@example.com", "password": "demo123"}'
```

Copy the `accessToken` from the response.

**Get Profile** (replace `<TOKEN>` with your access token):
```bash
curl -X GET http://localhost:8188/api/v1/users/me \
  -H "Authorization: Bearer <TOKEN>"
```

---

## 📋 API Cheat Sheet

### Authentication APIs

| Endpoint | Method | Auth | Body | Response |
|----------|--------|------|------|----------|
| `/api/v1/auth/signup` | POST | ❌ | `{email, password}` | `{success, userId, email}` |
| `/api/v1/auth/login` | POST | ❌ | `{email, password}` | `{user, tokens}` |
| `/api/v1/auth/refresh` | POST | ❌ | `{token: refreshToken}` | `{user, tokens}` |
| `/api/v1/auth/logout` | POST | ✅ | Header only | `{message}` |

### User APIs

| Endpoint | Method | Auth | Body | Response |
|----------|--------|------|------|----------|
| `/api/v1/users/me` | GET | ✅ | - | `{profile}` |
| `/api/v1/users/me` | PATCH | ✅ | `{updates}` | `{profile}` |
| `/api/v1/users/{id}` | GET | ✅ | - | `{profile}` |

**Auth Required (✅)**: Include `Authorization: Bearer <access_token>` header

---

## 🔑 Token Management

### How Tokens Work

1. **Login** → Receive `accessToken` + `refreshToken`
2. **Store** tokens securely (memory/localStorage)
3. **Use** `accessToken` in `Authorization: Bearer <token>` header for ALL protected APIs
4. **Refresh** when access token expires (1 hour)
5. **Logout** to revoke tokens

### Token Lifecycle

```
Login
  │
  ├─→ accessToken (1 hour) ─────┐
  │                              │
  └─→ refreshToken (10 hours) ──┤
                                 │
                                 ▼
                    Use accessToken for API calls
                                 │
                                 ▼
                    Access token expired? (401 error)
                                 │
                                 ▼
                    Call /auth/refresh with refreshToken
                                 │
                                 ├─→ New accessToken
                                 └─→ New refreshToken
                                 │
                                 ▼
                    Continue using new accessToken
```

---

## 🧪 Testing with cURL

### Complete User Journey

```bash
# 1. Signup
curl -X POST http://localhost:8188/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com", "password": "test123"}' \
  | jq

# 2. Login and save tokens
RESPONSE=$(curl -s -X POST http://localhost:8188/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com", "password": "test123"}')

ACCESS_TOKEN=$(echo $RESPONSE | jq -r '.result.tokens.accessToken')
REFRESH_TOKEN=$(echo $RESPONSE | jq -r '.result.tokens.refreshToken')

echo "Access Token: $ACCESS_TOKEN"

# 3. Get my profile
curl -X GET http://localhost:8188/api/v1/users/me \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  | jq

# 4. Setup profile
curl -X POST http://localhost:8188/api/v1/users/me/setup \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "displayName": "Test User",
    "bio": "Testing Day-Pulse APIs"
  }' \
  | jq

# 5. Update profile
curl -X PATCH http://localhost:8188/api/v1/users/me \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "bio": "Updated bio",
    "location": "San Francisco"
  }' \
  | jq

# 6. Refresh token (when access token expires)
curl -X POST http://localhost:8188/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"token\": \"$REFRESH_TOKEN\"}" \
  | jq

# 7. Logout
curl -X POST http://localhost:8188/api/v1/auth/logout \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  | jq
```

---

## 🧪 Testing with Postman

### Setup

1. **Create Environment**:
   - Variable: `baseUrl` = `http://localhost:8188/api/v1`
   - Variable: `accessToken` = (leave empty, will be auto-filled)
   - Variable: `refreshToken` = (leave empty, will be auto-filled)

2. **Login Request**:
   ```
   POST {{baseUrl}}/auth/login
   Body (JSON):
   {
     "email": "test@example.com",
     "password": "test123"
   }
   
   Tests (Script):
   const response = pm.response.json();
   pm.environment.set("accessToken", response.result.tokens.accessToken);
   pm.environment.set("refreshToken", response.result.tokens.refreshToken);
   ```

3. **Protected Request**:
   ```
   GET {{baseUrl}}/users/me
   Headers:
   Authorization: Bearer {{accessToken}}
   ```

---

## 🛠️ Configuration

### Port Mapping

| Service | Port | Context Path | Access URL |
|---------|------|--------------|------------|
| API Gateway | 8188 | `/` | `http://localhost:8188/api/v1/*` |
| Auth Service | 8180 | `/auth-service` | `http://localhost:8180/auth-service/*` |
| User Service | 8181 | `/user-service` | `http://localhost:8181/user-service/*` |

**Note**: Clients should ONLY call API Gateway, not services directly.

### Database Configuration

**Auth Service** (`auth-service/src/main/resources/application.yaml`):
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/auth-service
    username: postgres
    password: 123456  # Change in production!
```

**User Service** (`user-service/src/main/resources/application.yaml`):
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/user-service
    username: postgres
    password: 123456  # Change in production!
```

### JWT Configuration

**Signing Key** (both auth-service and api-gateway must use the same key):
```yaml
jwt:
  signing-key: fbX2a4nQ4tdMnfExFUl+uA9aD9IFS+csS8GP96pR75RxrCiUcEYvpn+b4wWsgJshvXMUQiDUxhEBxA9RdPj+OQ==
  valid-duration: 3600      # Access token: 1 hour
  refreshable-duration: 36000  # Refresh token: 10 hours
```

**⚠️ IMPORTANT**: Generate a new signing key for production using:
```bash
openssl rand -base64 64
```

---

## 🐛 Common Issues & Solutions

### Issue: "Connection refused" on service startup
**Solution**:
- Check if PostgreSQL is running
- Verify database exists: `psql -U postgres -l`
- Check port is not in use: `lsof -i :8180` (Mac/Linux) or `netstat -ano | findstr :8180` (Windows)

### Issue: "Unauthenticated" error (401)
**Solution**:
- Verify token is included in Authorization header
- Check token hasn't expired (1 hour validity)
- Use refresh token to get new access token
- Ensure token format is `Bearer <token>` (with space)

### Issue: CORS error in browser
**Solution**:
- Add your frontend origin to gateway CORS config
- Check browser console for actual error
- Verify preflight OPTIONS request succeeds

### Issue: "User already exists" on signup
**Solution**:
- Email is already registered
- Use different email or login with existing account

### Issue: Token validation fails at gateway
**Solution**:
- Verify JWT signing key matches in both auth-service and api-gateway
- Check token isn't malformed
- Ensure clock sync across services

---

## 📚 Documentation

- **Full Documentation**: `README.md`
- **API Design Standard**: `API_DESIGN_STANDARD.md`
- **User Service Security**: `user-service/SECURITY_MODEL.md`

---

## 🎯 Next Steps

1. ✅ **Setup Complete** - All services running
2. 📱 **Build Frontend** - Connect web/mobile app
3. 🔒 **Enhance Security** - Add rate limiting, Redis caching
4. 📊 **Add Monitoring** - Prometheus + Grafana
5. 🚀 **Deploy** - Docker Compose or Kubernetes

---

## 💡 Pro Tips

### Development Workflow

```bash
# Watch logs in real-time
tail -f auth-service/logs/application.log

# Quick restart service
# Ctrl+C to stop, then:
mvn spring-boot:run

# Skip tests for faster build
mvn clean package -DskipTests

# Run specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Database Management

```bash
# Connect to database
psql -U postgres -d auth-service

# View tables
\dt

# View table schema
\d users_auth

# View data
SELECT * FROM users_auth;

# Clear all data (reset)
TRUNCATE TABLE refresh_tokens CASCADE;
TRUNCATE TABLE user_roles CASCADE;
TRUNCATE TABLE users_auth CASCADE;
```

### JWT Debugging

Paste your JWT token at [jwt.io](https://jwt.io) to inspect claims.

Example decoded token:
```json
{
  "sub": "test@example.com",
  "iss": "daypulse-auth-service",
  "exp": 1698764800,
  "iat": 1698761200,
  "jti": "123e4567-e89b-12d3-a456-426614174000",
  "scope": "ROLE_USER",
  "userId": "abcd-1234-efgh-5678"
}
```

---

## 🤝 Getting Help

- Read error messages carefully (Spring provides detailed errors)
- Check service logs for stack traces
- Verify all prerequisites are installed
- Ensure all services are running
- Test services individually before testing through gateway

---

**Happy Coding! 🎉**
