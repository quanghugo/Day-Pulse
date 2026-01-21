# Day-Pulse Backend - Architecture Diagrams

## System Architecture Overview

```
┌──────────────────────────────────────────────────────────────────────┐
│                          CLIENT APPLICATIONS                          │
│                                                                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐               │
│  │ Web Browser  │  │  Mobile App  │  │ Desktop App  │               │
│  │  (React/    │  │   (iOS/      │  │  (Electron)  │               │
│  │   Vue/...)   │  │   Android)   │  │              │               │
│  └──────────────┘  └──────────────┘  └──────────────┘               │
│           │                 │                │                        │
│           └─────────────────┴────────────────┘                        │
│                             │                                         │
│              HTTP/HTTPS with Bearer Token Authentication             │
│                             │                                         │
└─────────────────────────────┼─────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────────┐
│                         PUBLIC INTERNET                               │
│                      (HTTPS in production)                            │
└─────────────────────────────┬─────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────────┐
│                      DMZ / PUBLIC SUBNET                              │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                     API GATEWAY (Port 8188)                     │ │
│  │  ┌──────────────────────────────────────────────────────────┐  │ │
│  │  │ Spring Cloud Gateway (Reactive)                          │  │ │
│  │  │                                                            │  │ │
│  │  │ ┌─────────────────────────────────────────────────────┐  │  │ │
│  │  │ │ JWT Authentication Filter                           │  │  │ │
│  │  │ │ • Extract Authorization: Bearer <token>             │  │  │ │
│  │  │ │ • Validate JWT signature (HS512)                    │  │  │ │
│  │  │ │ • Check expiration (exp claim)                      │  │  │ │
│  │  │ │ • Call Auth Service introspection                   │  │  │ │
│  │  │ │ • Extract userId, roles from JWT                    │  │  │ │
│  │  │ │ • Add internal headers: X-User-Id, X-User-Roles     │  │  │ │
│  │  │ └─────────────────────────────────────────────────────┘  │  │ │
│  │  │                                                            │  │ │
│  │  │ ┌─────────────────────────────────────────────────────┐  │  │ │
│  │  │ │ Route Configuration                                 │  │  │ │
│  │  │ │ • /api/v1/auth/** → Auth Service                    │  │  │ │
│  │  │ │ • /api/v1/users/** → User Service                   │  │  │ │
│  │  │ │ • CORS configuration                                 │  │  │ │
│  │  │ │ • Security policies (public vs protected)           │  │  │ │
│  │  │ └─────────────────────────────────────────────────────┘  │  │ │
│  │  └──────────────────────────────────────────────────────────┘  │ │
│  └────────────────────────────────────────────────────────────────┘ │
└──────────────┬────────────────────────┬──────────────────────────────┘
               │                        │
               │   Internal Network     │
               │   (Private Subnet)     │
               ▼                        ▼
┌──────────────────────────────┐ ┌──────────────────────────────┐
│   AUTH SERVICE (Port 8180)   │ │   USER SERVICE (Port 8181)   │
│  ┌────────────────────────┐  │ │  ┌────────────────────────┐  │
│  │ Spring Boot + Security │  │ │  │ Spring Boot + JPA      │  │
│  │                        │  │ │  │                        │  │
│  │ Endpoints:             │  │ │  │ Endpoints:             │  │
│  │ • POST /auth/signup    │  │ │  │ • GET /users/me        │  │
│  │ • POST /auth/login     │  │ │  │ • PATCH /users/me      │  │
│  │ • POST /auth/refresh   │  │ │  │ • GET /users/{id}      │  │
│  │ • POST /auth/logout    │  │ │  │ • Follow/Unfollow APIs │  │
│  │ • POST /auth/introspect│  │ │  │                        │  │
│  │                        │  │ │  │ Internal Endpoints:    │  │
│  │ Functions:             │  │ │  │ • POST /internal/users/│  │
│  │ • User registration    │  │ │  │   {id}/init            │  │
│  │ • Password validation  │  │ │  │ • GET /internal/users/ │  │
│  │ • JWT generation       │  │ │  │   {id}/summary         │  │
│  │ • Token revocation     │  │ │  │                        │  │
│  │ • Role management      │  │ │  └────────────────────────┘  │
│  └─────────┬──────────────┘  │ └─────────┬──────────────────┘
│            │                 │           │                    │
│            ▼                 │           ▼                    │
│  ┌─────────────────────┐    │ ┌─────────────────────┐        │
│  │ PostgreSQL Database │    │ │ PostgreSQL Database │        │
│  │ auth-service        │    │ │ user-service        │        │
│  │                     │    │ │                     │        │
│  │ Tables:             │    │ │ Tables:             │        │
│  │ • users_auth        │    │ │ • user_profiles     │        │
│  │ • refresh_tokens    │    │ │ • user_stats        │        │
│  │ • roles             │    │ │ • follows           │        │
│  │ • permissions       │    │ │                     │        │
│  └─────────────────────┘    │ └─────────────────────┘        │
└──────────────────────────────┘ └──────────────────────────────┘
```

---

## Authentication Flow - Detailed

### 1. User Registration (Signup)

```
┌────────┐                  ┌──────────┐                ┌──────────┐
│ Client │                  │ Gateway  │                │   Auth   │
│        │                  │          │                │ Service  │
└────┬───┘                  └────┬─────┘                └────┬─────┘
     │                           │                           │
     │ 1. POST /api/v1/auth/signup                          │
     ├──────────────────────────>│                           │
     │ Headers:                  │                           │
     │   Content-Type: json      │                           │
     │ Body:                     │                           │
     │   {email, password}       │                           │
     │                           │                           │
     │                           │ 2. Route to auth service  │
     │                           ├──────────────────────────>│
     │                           │ POST /auth-service/       │
     │                           │      auth/signup          │
     │                           │                           │
     │                           │                           │ 3. Process signup
     │                           │                           │    • Validate email format
     │                           │                           │    • Check if email exists
     │                           │                           │    • Hash password (BCrypt)
     │                           │                           │    • Save user to DB
     │                           │                           │    • Assign ROLE_USER
     │                           │                           │
     │                           │ 4. Response               │
     │                           │<──────────────────────────┤
     │                           │ 201 Created               │
     │                           │ {success, userId, email}  │
     │                           │                           │
     │ 5. Forward response       │                           │
     │<──────────────────────────┤                           │
     │ 201 Created               │                           │
     │ {success, userId, email}  │                           │
     │                           │                           │
     │ 6. Store userId           │                           │
     │    (optional)             │                           │
     │                           │                           │
```

### 2. User Login (Authentication)

```
┌────────┐                  ┌──────────┐                ┌──────────┐
│ Client │                  │ Gateway  │                │   Auth   │
│        │                  │          │                │ Service  │
└────┬───┘                  └────┬─────┘                └────┬─────┘
     │                           │                           │
     │ 1. POST /api/v1/auth/login                           │
     ├──────────────────────────>│                           │
     │ Body:                     │                           │
     │   {email, password}       │                           │
     │                           │                           │
     │                           │ 2. Route request          │
     │                           ├──────────────────────────>│
     │                           │                           │
     │                           │                           │ 3. Authenticate
     │                           │                           │    • Find user by email
     │                           │                           │    • Verify password hash
     │                           │                           │    • Generate access token
     │                           │                           │      - Algorithm: HS512
     │                           │                           │      - Expiry: 1 hour
     │                           │                           │      - Claims: userId, scope
     │                           │                           │    • Generate refresh token
     │                           │                           │      - Expiry: 10 hours
     │                           │                           │    • Save refresh token hash
     │                           │                           │
     │                           │ 4. Return tokens          │
     │                           │<──────────────────────────┤
     │                           │ 200 OK                    │
     │                           │ {user, tokens: {          │
     │                           │   accessToken,            │
     │                           │   refreshToken,           │
     │                           │   expiresIn: 3600,        │
     │                           │   tokenType: "Bearer"     │
     │                           │ }}                        │
     │                           │                           │
     │ 5. Forward response       │                           │
     │<──────────────────────────┤                           │
     │ 200 OK + tokens           │                           │
     │                           │                           │
     │ 6. Store tokens securely  │                           │
     │    (memory/storage)       │                           │
     │                           │                           │
```

### 3. Accessing Protected Resource

```
┌────────┐        ┌──────────┐        ┌──────────┐        ┌──────────┐
│ Client │        │ Gateway  │        │   Auth   │        │   User   │
│        │        │          │        │ Service  │        │ Service  │
└────┬───┘        └────┬─────┘        └────┬─────┘        └────┬─────┘
     │                 │                   │                   │
     │ 1. GET /api/v1/users/me              │                   │
     ├────────────────>│                   │                   │
     │ Authorization:  │                   │                   │
     │ Bearer <token>  │                   │                   │
     │                 │                   │                   │
     │                 │ 2. Extract JWT    │                   │
     │                 │    from header    │                   │
     │                 │                   │                   │
     │                 │ 3. Validate JWT   │                   │
     │                 │    • Check signature                  │
     │                 │    • Check expiration                 │
     │                 │                   │                   │
     │                 │ 4. Introspect     │                   │
     │                 ├──────────────────>│                   │
     │                 │ POST /auth/       │                   │
     │                 │ introspect        │                   │
     │                 │ {token}           │                   │
     │                 │                   │                   │
     │                 │                   │ 5. Check revocation
     │                 │                   │    • Verify not revoked
     │                 │                   │    • Return valid status
     │                 │                   │                   │
     │                 │ 6. Valid          │                   │
     │                 │<──────────────────┤                   │
     │                 │ {valid: true}     │                   │
     │                 │                   │                   │
     │                 │ 7. Extract claims │                   │
     │                 │    • userId       │                   │
     │                 │    • scope        │                   │
     │                 │                   │                   │
     │                 │ 8. Forward with   │                   │
     │                 │    internal headers                   │
     │                 ├──────────────────────────────────────>│
     │                 │ GET /user-service/users/me            │
     │                 │ X-User-Id: abc-123                    │
     │                 │ X-User-Roles: ROLE_USER               │
     │                 │                   │                   │
     │                 │                   │                   │ 9. Process request
     │                 │                   │                   │    • Read X-User-Id
     │                 │                   │                   │    • Fetch profile from DB
     │                 │                   │                   │
     │                 │ 10. Profile data  │                   │
     │                 │<──────────────────────────────────────┤
     │                 │ 200 OK {profile}  │                   │
     │                 │                   │                   │
     │ 11. Return data │                   │                   │
     │<────────────────┤                   │                   │
     │ 200 OK          │                   │                   │
     │ {profile}       │                   │                   │
     │                 │                   │                   │
```

### 4. Token Refresh Flow

```
┌────────┐                  ┌──────────┐                ┌──────────┐
│ Client │                  │ Gateway  │                │   Auth   │
│        │                  │          │                │ Service  │
└────┬───┘                  └────┬─────┘                └────┬─────┘
     │                           │                           │
     │ Access token expired!     │                           │
     │ (401 Unauthenticated)     │                           │
     │                           │                           │
     │ 1. POST /api/v1/auth/refresh                         │
     ├──────────────────────────>│                           │
     │ Body:                     │                           │
     │   {token: refreshToken}   │                           │
     │                           │                           │
     │                           │ 2. Route request          │
     │                           ├──────────────────────────>│
     │                           │                           │
     │                           │                           │ 3. Validate refresh token
     │                           │                           │    • Verify JWT signature
     │                           │                           │    • Check expiration
     │                           │                           │    • Hash token (MD5)
     │                           │                           │    • Find in DB
     │                           │                           │    • Check not revoked
     │                           │                           │
     │                           │                           │ 4. Token rotation
     │                           │                           │    • Revoke old refresh token
     │                           │                           │      (set revokedAt)
     │                           │                           │    • Generate new access token
     │                           │                           │    • Generate new refresh token
     │                           │                           │    • Save new refresh token hash
     │                           │                           │
     │                           │ 5. Return new tokens      │
     │                           │<──────────────────────────┤
     │                           │ 200 OK {user, tokens}     │
     │                           │                           │
     │ 6. Forward response       │                           │
     │<──────────────────────────┤                           │
     │ 200 OK + new tokens       │                           │
     │                           │                           │
     │ 7. Update stored tokens   │                           │
     │    • Replace old access   │                           │
     │    • Replace old refresh  │                           │
     │                           │                           │
     │ 8. Retry original request │                           │
     │    with new access token  │                           │
     │                           │                           │
```

### 5. Logout Flow

```
┌────────┐                  ┌──────────┐                ┌──────────┐
│ Client │                  │ Gateway  │                │   Auth   │
│        │                  │          │                │ Service  │
└────┬───┘                  └────┬─────┘                └────┬─────┘
     │                           │                           │
     │ 1. POST /api/v1/auth/logout                          │
     ├──────────────────────────>│                           │
     │ Authorization:            │                           │
     │ Bearer <accessToken>      │                           │
     │                           │                           │
     │                           │ 2. Validate token         │
     │                           │    (same as protected     │
     │                           │    resource flow)         │
     │                           │                           │
     │                           │ 3. Forward with token     │
     │                           ├──────────────────────────>│
     │                           │ POST /auth-service/       │
     │                           │      auth/logout          │
     │                           │ Authorization: Bearer ... │
     │                           │                           │
     │                           │                           │ 4. Process logout
     │                           │                           │    • Extract userId from JWT
     │                           │                           │    • Find all user's refresh
     │                           │                           │      tokens (not revoked)
     │                           │                           │    • Revoke all (set revokedAt)
     │                           │                           │    • [Future] Blacklist access
     │                           │                           │      token in Redis
     │                           │                           │
     │                           │ 5. Success                │
     │                           │<──────────────────────────┤
     │                           │ 200 OK                    │
     │                           │ {message: "Logout successful"}
     │                           │                           │
     │ 6. Forward response       │                           │
     │<──────────────────────────┤                           │
     │ 200 OK                    │                           │
     │                           │                           │
     │ 7. Clear stored tokens    │                           │
     │    • Delete accessToken   │                           │
     │    • Delete refreshToken  │                           │
     │    • Redirect to login    │                           │
     │                           │                           │
```

---

## Data Flow Diagrams

### JWT Token Structure

```
┌─────────────────────────────────────────────────────────────────┐
│                          JWT TOKEN                               │
│                                                                   │
│  ┌──────────────┐   ┌────────────────┐   ┌──────────────┐      │
│  │   HEADER     │ . │    PAYLOAD     │ . │  SIGNATURE   │      │
│  └──────────────┘   └────────────────┘   └──────────────┘      │
│                                                                   │
│  Header (Base64):                                                │
│  {                                                                │
│    "alg": "HS512",           ← Algorithm: HMAC-SHA512           │
│    "typ": "JWT"              ← Type: JSON Web Token             │
│  }                                                                │
│                                                                   │
│  Payload (Base64):                                               │
│  {                                                                │
│    "sub": "user@example.com",  ← Subject (user identifier)      │
│    "userId": "abc-123-...",    ← User ID (UUID)                 │
│    "scope": "ROLE_USER",       ← Roles & permissions            │
│    "iss": "daypulse-auth",     ← Issuer                         │
│    "exp": 1698764800,          ← Expiration (Unix timestamp)    │
│    "iat": 1698761200,          ← Issued at                      │
│    "jti": "unique-id"          ← JWT ID (for revocation)        │
│  }                                                                │
│                                                                   │
│  Signature (HMAC-SHA512):                                        │
│  HMACSHA512(                                                     │
│    base64UrlEncode(header) + "." +                              │
│    base64UrlEncode(payload),                                    │
│    secret_key                    ← Shared secret (base64, 512b) │
│  )                                                                │
└─────────────────────────────────────────────────────────────────┘
```

### Token Storage & Lifecycle

```
┌──────────────────────────────────────────────────────────────────┐
│                      ACCESS TOKEN LIFECYCLE                       │
│                                                                    │
│  Created ──> Used for APIs ──> Expired ──> Invalid               │
│    │           (1 hour)          │                                │
│    │                             │                                │
│    │                             ▼                                │
│    │                      Use Refresh Token                       │
│    │                             │                                │
│    │                             ▼                                │
│    └────────────> Generate New Access Token                       │
│                                                                    │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│                     REFRESH TOKEN LIFECYCLE                       │
│                                                                    │
│  Created ──> Stored in DB ──> Used Once ──> Revoked              │
│    │         (hashed)           │                                 │
│    │                            │                                 │
│    │                            ▼                                 │
│    │                     New Refresh Token                        │
│    │                            │                                 │
│    └────────────────────────────┘                                 │
│                                                                    │
│  Note: Token Rotation - Each use generates new refresh token     │
│        and revokes the old one (enhanced security)               │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│                      TOKEN STORAGE (Client)                       │
│                                                                    │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │  Option 1: Memory (Most Secure)                             │ │
│  │  • Store in JavaScript variable                             │ │
│  │  • Lost on page refresh (need re-login)                     │ │
│  │  • Immune to XSS (if no global access)                      │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                    │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │  Option 2: sessionStorage (Good for SPAs)                   │ │
│  │  • Cleared on tab close                                      │ │
│  │  • Vulnerable to XSS                                         │ │
│  │  • Requires re-login on new tab                              │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                    │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │  Option 3: localStorage (Convenient but less secure)        │ │
│  │  • Persists across sessions                                  │ │
│  │  • Vulnerable to XSS                                         │ │
│  │  • Use only with strong XSS protection                       │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                    │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │  Option 4: HttpOnly Cookies (Most Secure - Future)          │ │
│  │  • Not accessible via JavaScript                             │ │
│  │  • Immune to XSS                                             │ │
│  │  • Requires CSRF protection                                  │ │
│  │  • Best for refresh tokens                                   │ │
│  └─────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────┘
```

---

## Security Layers

```
┌──────────────────────────────────────────────────────────────────┐
│                      SECURITY IN DEPTH                            │
│                                                                    │
│  Layer 1: Network Security                                        │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ • HTTPS/TLS encryption (production)                        │  │
│  │ • Firewall rules (only gateway exposed)                    │  │
│  │ • Private VPC/subnets for services                         │  │
│  │ • DDoS protection (CDN/Load Balancer)                      │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                    │
│  Layer 2: API Gateway Security                                    │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ • JWT signature validation (HS512)                         │  │
│  │ • Token expiration enforcement                             │  │
│  │ • Token introspection (revocation check)                   │  │
│  │ • CORS policy enforcement                                   │  │
│  │ • Rate limiting (future)                                    │  │
│  │ • Request size limits                                       │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                    │
│  Layer 3: Service Security (Auth)                                 │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ • BCrypt password hashing (cost 10)                        │  │
│  │ • Strong JWT signing key (512 bits)                        │  │
│  │ • Refresh token hashing (MD5 for lookup)                   │  │
│  │ • Token rotation on refresh                                 │  │
│  │ • Role-based access control (RBAC)                         │  │
│  │ • Input validation (Bean Validation)                       │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                    │
│  Layer 4: Service Security (User)                                 │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ • Trust boundary at gateway                                 │  │
│  │ • Validate X-User-Id header presence                       │  │
│  │ • Network isolation (no direct access)                     │  │
│  │ • Input validation                                          │  │
│  │ • SQL injection prevention (JPA)                           │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                    │
│  Layer 5: Database Security                                       │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ • Separate databases per service                           │  │
│  │ • Strong database credentials                              │  │
│  │ • Connection pooling                                        │  │
│  │ • Prepared statements (prevent SQL injection)              │  │
│  │ • Encryption at rest (future)                              │  │
│  └────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
```

---

## Error Handling Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                     ERROR RESPONSE FLOW                          │
│                                                                   │
│  Request ──> Validation ──> Error? ──> Format Error Response    │
│                 │                │                               │
│                 │                ▼                               │
│                 │          Error Code                            │
│                 │          Error Message                         │
│                 │                │                               │
│                 │                ▼                               │
│                 │     {                                          │
│                 │       "code": 1006,                            │
│                 │       "message": "Unauthenticated"             │
│                 │     }                                          │
│                 │                                                 │
│                 ▼                                                 │
│           Process Request                                        │
│                 │                                                 │
│                 ▼                                                 │
│           Success Response                                       │
│           {                                                       │
│             "code": 200,                                          │
│             "message": "Success",                                 │
│             "result": {...}                                       │
│           }                                                       │
└─────────────────────────────────────────────────────────────────┘

Common Error Codes:
┌──────┬─────────────────────────────────────────────────────────┐
│ Code │ Meaning                                                  │
├──────┼─────────────────────────────────────────────────────────┤
│ 200  │ Success                                                  │
│ 201  │ Created (successful signup)                              │
│ 400  │ Bad Request (invalid input)                              │
│ 401  │ Unauthenticated (invalid/expired token)                  │
│ 403  │ Forbidden (insufficient permissions)                     │
│ 404  │ Not Found (user/resource doesn't exist)                  │
│ 409  │ Conflict (email already exists)                          │
│ 500  │ Internal Server Error                                    │
└──────┴─────────────────────────────────────────────────────────┘
```

---

## Deployment Architecture (Production)

```
                          ┌────────────────┐
                          │   CloudFlare   │
                          │   or AWS CDN   │
                          └───────┬────────┘
                                  │ HTTPS
                                  ▼
                          ┌────────────────┐
                          │ Load Balancer  │
                          │  (AWS ALB/     │
                          │   Nginx)       │
                          └───────┬────────┘
                                  │
                    ┌─────────────┴─────────────┐
                    │                           │
                    ▼                           ▼
          ┌──────────────────┐        ┌──────────────────┐
          │  API Gateway     │        │  API Gateway     │
          │  Instance 1      │        │  Instance 2      │
          └────────┬─────────┘        └────────┬─────────┘
                   │                           │
           ┌───────┴───────┬───────────────────┴────────┐
           │               │                            │
           ▼               ▼                            ▼
    ┌──────────┐    ┌──────────┐              ┌──────────┐
    │  Auth    │    │  User    │              │  Feed    │
    │ Service  │    │ Service  │              │ Service  │
    │ (Scaled) │    │ (Scaled) │              │ (Future) │
    └────┬─────┘    └────┬─────┘              └────┬─────┘
         │               │                          │
         │               │                          │
         ▼               ▼                          ▼
    ┌─────────────────────────────────────────────────────┐
    │          PostgreSQL Cluster (RDS)                   │
    │   ┌──────────────┐  ┌──────────────┐              │
    │   │ auth-service │  │ user-service │              │
    │   │   Database   │  │   Database   │              │
    │   └──────────────┘  └──────────────┘              │
    │                                                     │
    │   Read Replicas for scaling                        │
    └─────────────────────────────────────────────────────┘
         
         ┌─────────────────────┐
         │  Redis Cluster      │
         │  (Future)           │
         │  • Token blacklist  │
         │  • Session cache    │
         │  • Rate limiting    │
         └─────────────────────┘
```

---

This document provides comprehensive visual representations of the Day-Pulse backend architecture, data flows, and security models. Use it as a reference for understanding system behavior and for onboarding new team members.
