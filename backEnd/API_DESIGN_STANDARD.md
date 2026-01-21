# Day-Pulse API Design Standard

## Authentication & Authorization

### Overview
Day-Pulse follows industry-standard OAuth 2.0-style JWT authentication:
- **Access Tokens**: Short-lived JWTs (1 hour) for API authentication
- **Refresh Tokens**: Long-lived tokens (10 hours) for renewing access tokens
- **Token Transport**: Authorization header for access tokens, body for refresh tokens

---

## Token Handling Standards

### 1. How to Send Tokens (Client → Server)

#### For Protected Endpoints (Authenticated Requests)
**ALWAYS use the `Authorization` header with Bearer scheme:**

```http
GET /api/v1/users/me
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwiaXNzIjoiZGF5cHVsc2UtYXV0aC1zZXJ2aWNlIiwiZXhwIjoxNjk4NzY0ODAwLCJpYXQiOjE2OTg3NjEyMDAsImp0aSI6IjEyMzQ1Njc4LWFiY2QtZWZnaCIsInNjb3BlIjoiUk9MRV9VU0VSIiwidXNlcklkIjoiYWJjZDEyMzQifQ.signature
```

**NEVER send access tokens in:**
- Request body
- Query parameters (vulnerable to logging and leakage)
- Custom headers (use standard `Authorization` header)

#### For Token Refresh
**Send refresh token in request body:**

```http
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "token": "refresh_token_here"
}
```

**Alternative (More Secure - Future Enhancement):**
Use `HttpOnly` cookies for refresh tokens to prevent XSS attacks.

---

### 2. How to Receive Tokens (Server → Client)

#### Login/Signup Response
Tokens are returned in the **response body**:

```json
{
  "code": 200,
  "result": {
    "user": {
      "id": "abcd-1234-efgh-5678",
      "email": "user@example.com",
      "isEmailVerified": false,
      "isSetupComplete": false
    },
    "tokens": {
      "accessToken": "eyJhbGci...",
      "refreshToken": "eyJhbGci...",
      "expiresIn": 3600,
      "tokenType": "Bearer"
    }
  }
}
```

**Client Responsibilities:**
1. Extract `accessToken` and `refreshToken` from response
2. Store securely (localStorage, sessionStorage, or memory - avoid localStorage for sensitive apps)
3. Include `accessToken` in `Authorization: Bearer <token>` header for all protected API calls
4. Use `refreshToken` to get new access token before expiration

---

## API Endpoint Classification

### Public Endpoints (No Authentication Required)

These endpoints do **NOT** require any token in the request:

| Method | Endpoint | Purpose | Request | Response |
|--------|----------|---------|---------|----------|
| POST | `/api/v1/auth/signup` | Create account | `{ email, password }` | User info, optionally tokens |
| POST | `/api/v1/auth/login` | Authenticate | `{ email, password }` | User info + tokens |
| POST | `/api/v1/auth/refresh` | Renew access token | `{ token: refreshToken }` | New tokens |
| POST | `/api/v1/auth/introspect` | Validate token | `{ token }` | `{ valid: boolean }` |

### Protected Endpoints (Authentication Required)

These endpoints **REQUIRE** `Authorization: Bearer <access_token>` header:

| Method | Endpoint | Purpose | Headers | Response |
|--------|----------|---------|---------|----------|
| POST | `/api/v1/auth/logout` | Revoke tokens | `Authorization: Bearer <token>` | Success message |
| GET | `/api/v1/users/me` | Get profile | `Authorization: Bearer <token>` | User profile |
| PATCH | `/api/v1/users/me` | Update profile | `Authorization: Bearer <token>` | Updated profile |
| POST | `/api/v1/users/me/setup` | Setup profile | `Authorization: Bearer <token>` | Setup profile |
| GET | `/api/v1/users/{id}` | Get user by ID | `Authorization: Bearer <token>` | User profile |

---

## JWT Token Structure

### Access Token Claims

```json
{
  "sub": "user@example.com",           // Subject (user email)
  "iss": "daypulse-auth-service",      // Issuer
  "exp": 1698764800,                   // Expiration time (Unix timestamp)
  "iat": 1698761200,                   // Issued at (Unix timestamp)
  "jti": "12345678-abcd-efgh",         // JWT ID (unique identifier)
  "scope": "ROLE_USER",                // User roles and permissions (space-separated)
  "userId": "abcd-1234-efgh-5678"      // User ID (UUID)
}
```

### Refresh Token Claims

```json
{
  "sub": "user@example.com",
  "iss": "daypulse-auth-service",
  "exp": 1698797200,                   // Longer expiration
  "iat": 1698761200,
  "jti": "87654321-dcba-hgfe",
  "type": "refresh",                   // Identifies as refresh token
  "userId": "abcd-1234-efgh-5678"
}
```

---

## Service Architecture Flow

### Client → API Gateway → Services

```
┌────────────────────────────────────────────────────────────────┐
│ Client (Browser/Mobile App)                                     │
│                                                                  │
│ 1. User Login                                                   │
│    POST /api/v1/auth/login                                      │
│    Body: { email, password }                                    │
│                                                                  │
│ 2. Receive Tokens                                               │
│    Response: { accessToken, refreshToken, ... }                 │
│                                                                  │
│ 3. Store Tokens (memory/storage)                                │
│                                                                  │
│ 4. Make Protected Request                                       │
│    GET /api/v1/users/me                                         │
│    Header: Authorization: Bearer <accessToken>                  │
└────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌────────────────────────────────────────────────────────────────┐
│ API Gateway (Port 8188)                                         │
│                                                                  │
│ 1. Extract token from Authorization header                      │
│ 2. Validate JWT signature and expiration                        │
│ 3. Call Auth Service to check revocation                        │
│ 4. Extract userId and roles from JWT claims                     │
│ 5. Add internal headers:                                        │
│    - X-User-Id: <userId>                                        │
│    - X-User-Roles: <scope>                                      │
│ 6. Forward request to downstream service                        │
└────────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┴────────────────┐
              ▼                                ▼
┌──────────────────────────┐    ┌──────────────────────────┐
│ Auth Service (Port 8180) │    │ User Service (Port 8181) │
│                          │    │                          │
│ - Reads X-User-Id        │    │ - Reads X-User-Id        │
│ - Validates permissions  │    │ - Fetches user profile   │
│ - Returns auth data      │    │ - Returns profile data   │
└──────────────────────────┘    └──────────────────────────┘
```

---

## Token Validation Flow

### Gateway Token Validation Process

1. **Extract Token**: Parse `Authorization: Bearer <token>` header
2. **Verify Signature**: Decode JWT and validate HMAC signature using shared secret
3. **Check Expiration**: Ensure token hasn't expired (`exp` claim)
4. **Introspect**: Call Auth Service `/auth/introspect` to check revocation
5. **Extract Claims**: Get `userId`, `scope` from JWT
6. **Forward Request**: Add `X-User-Id` and `X-User-Roles` headers for downstream services

### Auth Service Token Management

**Token Generation:**
- Access tokens: 1 hour validity (configurable via `jwt.valid-duration`)
- Refresh tokens: 10 hours validity (configurable via `jwt.refreshable-duration`)
- Both use HS512 algorithm with base64-encoded secret key

**Token Storage:**
- Refresh tokens hashed (MD5) and stored in database with expiration
- Access tokens are stateless (no server-side storage)
- Revocation via refresh token deletion and optional Redis blacklist

**Token Rotation:**
- On refresh, old refresh token is revoked
- New access + refresh token pair is issued
- Implements refresh token rotation for security

---

## Security Best Practices

### DO ✅

1. **Always use HTTPS** in production to prevent token interception
2. **Send access tokens in Authorization header** for all protected requests
3. **Validate tokens on every request** (signature, expiration, revocation)
4. **Use short-lived access tokens** (1 hour or less)
5. **Rotate refresh tokens** on each use
6. **Store refresh tokens securely** (HttpOnly cookies or secure storage)
7. **Implement CORS properly** to restrict origins
8. **Log authentication failures** for security monitoring

### DON'T ❌

1. **Never send tokens in URL query parameters** (logged and cached)
2. **Never store sensitive tokens in localStorage** without encryption
3. **Never skip HTTPS** in production
4. **Never share signing keys** across public boundaries
5. **Never expose raw tokens** in logs or error messages
6. **Never trust client claims** - always validate server-side
7. **Never use weak signing algorithms** (HS256 minimum, HS512 recommended)
8. **Never send access tokens in request bodies** (except introspect/refresh use cases)

---

## Configuration

### Auth Service (`auth-service/application.yaml`)

```yaml
jwt:
  signing-key: <base64-encoded-secret-key>  # Min 512 bits for HS512
  valid-duration: 3600           # Access token: 1 hour
  refreshable-duration: 36000    # Refresh token: 10 hours
```

### API Gateway (`api-gateway/application.yaml`)

```yaml
jwt:
  signing-key: <same-as-auth-service>  # Must match for validation

spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: http://localhost:8180
          predicates:
            - Path=/api/v1/auth/**
          filters:
            - RewritePath=/api/v1/auth/(?<segment>.*), /auth-service/auth/${segment}
        
        - id: user-service
          uri: http://localhost:8181
          predicates:
            - Path=/api/v1/users/**
          filters:
            - RewritePath=/api/v1/users/(?<segment>.*), /user-service/users/${segment}
```

---

## Error Responses

### Authentication Errors

```json
{
  "code": 1006,
  "message": "Unauthenticated"
}
```

**Common Causes:**
- Missing or invalid Authorization header
- Expired access token
- Revoked token
- Invalid JWT signature

**Client Action:**
1. Try refreshing access token using refresh token
2. If refresh fails, redirect to login
3. Clear stored tokens

---

## Future Enhancements

### Planned Security Features

1. **Redis Token Blacklist**: Fast revocation check for access tokens
2. **HttpOnly Refresh Cookies**: Move refresh tokens from body to secure cookies
3. **Rate Limiting**: Prevent brute force attacks on auth endpoints
4. **OAuth 2.0 Integration**: Google, GitHub, etc. social login
5. **Multi-Factor Authentication (MFA)**: TOTP or SMS-based 2FA
6. **Service-to-Service Auth**: mTLS for internal service communication

---

## Testing Examples

### Using curl

```bash
# 1. Sign up
curl -X POST http://localhost:8188/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com", "password": "password123"}'

# 2. Login
curl -X POST http://localhost:8188/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com", "password": "password123"}'

# Response:
# {
#   "result": {
#     "tokens": {
#       "accessToken": "eyJhbGci...",
#       "refreshToken": "eyJhbGci..."
#     }
#   }
# }

# 3. Get profile (protected)
curl -X GET http://localhost:8188/api/v1/users/me \
  -H "Authorization: Bearer eyJhbGci..."

# 4. Refresh token
curl -X POST http://localhost:8188/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"token": "eyJhbGci..."}'

# 5. Logout
curl -X POST http://localhost:8188/api/v1/auth/logout \
  -H "Authorization: Bearer eyJhbGci..."
```

### Using Postman/Thunder Client

**Collection Variables:**
- `baseUrl`: `http://localhost:8188/api/v1`
- `accessToken`: (auto-set from login response)
- `refreshToken`: (auto-set from login response)

**Pre-request Script (for protected endpoints):**
```javascript
pm.request.headers.add({
  key: 'Authorization',
  value: 'Bearer ' + pm.variables.get('accessToken')
});
```

---

## Summary

### For Frontend Developers

1. **Signup/Login**: Send credentials to `/auth/signup` or `/auth/login`, receive tokens in response body
2. **Store Tokens**: Save `accessToken` and `refreshToken` securely
3. **API Calls**: Add `Authorization: Bearer <accessToken>` header to all protected requests
4. **Token Refresh**: When access token expires (401), use refresh token at `/auth/refresh`
5. **Logout**: Call `/auth/logout` with Authorization header to revoke tokens

### For Backend Developers

1. **Public Endpoints**: No security config needed, permit all
2. **Protected Endpoints**: Require authentication, trust `X-User-Id` header from gateway
3. **Token Generation**: Use `AuthenticationService` methods, include all required claims
4. **Token Validation**: Let gateway handle validation, services read forwarded headers
5. **Error Handling**: Throw `AppException(ErrorCode.UNAUTHENTICATED)` for auth failures
