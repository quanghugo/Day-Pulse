# Auth Service

Authentication and authorization service for the DayPulse platform.

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Configuration](#configuration)
- [Database Schema](#database-schema)
- [API Endpoints](#api-endpoints)
- [Security](#security)
- [RBAC System](#rbac-system)
- [Authentication Flows](#authentication-flows)
- [Integration](#integration)
- [Development](#development)
- [Troubleshooting](#troubleshooting)

---

## Overview

### Purpose

The Auth Service is the **central authentication and authorization hub** for DayPulse, providing:
- User registration and account management
- JWT-based authentication (OAuth 2.0 style)
- Token generation and validation
- Refresh token management with rotation
- Role-Based Access Control (RBAC)
- Admin user management

### Basic Information

| Property | Value |
|----------|-------|
| **Service Name** | auth-service |
| **Port** | 8180 |
| **Context Path** | `/auth-service` |
| **Technology** | Spring Boot + Spring Security |
| **Framework** | Spring Boot 3.5.10 |
| **Database** | PostgreSQL (`auth-service`) |
| **Base URL** | `http://localhost:8180/auth-service` |

### Technology Stack

```yaml
Core:
  - Spring Boot 3.5.10
  - Spring Security
  - OAuth2 Resource Server
  - Spring Data JPA

Authentication:
  - JWT (HS512)
  - BCrypt password hashing
  - Token rotation

Database:
  - PostgreSQL 15+
  - Flyway migrations

Utilities:
  - MapStruct 1.5.5 (DTO mapping)
  - Lombok (boilerplate reduction)
  - Jakarta Validation

Build:
  - Maven
  - Java 21
```

### Responsibilities

1. **User Management**
   - Registration (signup)
   - Email/password authentication
   - Account creation and verification (future)

2. **Token Management**
   - Access token generation (JWT, 1 hour)
   - Refresh token generation (10 hours)
   - Token validation (introspection)
   - Token revocation (logout)
   - Automatic token rotation

3. **Authorization**
   - Role-Based Access Control (RBAC)
   - Three roles: USER, MODERATOR, ADMIN
   - 12 granular permissions
   - Hierarchical permission inheritance

4. **Admin Operations**
   - Update user roles
   - View all roles and permissions
   - User management (future)

---

## Architecture

### Component Structure

```
auth-service/
├── src/main/java/com/daypulse/auth_serivce/
│   ├── AuthSerivceApplication.java              # Main entry point
│   ├── config/
│   │   ├── CustomJwtDecoder.java                # JWT decoder configuration
│   │   ├── DataInitializer.java                 # Create default admin
│   │   ├── JwtAuthenticationEntryPoint.java     # Auth error handler
│   │   └── SecurityConfig.java                  # Security rules
│   ├── controller/
│   │   ├── AuthenticationController.java        # Auth endpoints
│   │   ├── AdminController.java                 # Admin endpoints
│   │   └── UserController.java                  # User info endpoints
│   ├── service/
│   │   ├── AuthenticationService.java           # Auth business logic
│   │   ├── UserRoleService.java                 # Role management
│   │   └── UserService.java                     # User operations
│   ├── entity/
│   │   ├── UserAuth.java                        # User account entity
│   │   ├── RefreshToken.java                    # Refresh token entity
│   │   └── OtpCode.java                         # OTP code entity (future)
│   ├── enums/
│   │   ├── RoleEnum.java                        # Role definitions
│   │   └── PermissionEnum.java                  # Permission definitions
│   ├── repository/
│   │   ├── UserRepository.java                  # User data access
│   │   ├── RefreshTokenRepository.java          # Token data access
│   │   └── OtpCodeRepository.java               # OTP data access
│   ├── dto/
│   │   ├── request/                             # Request DTOs
│   │   └── response/                            # Response DTOs
│   ├── exception/
│   │   ├── AppException.java                    # Custom exception
│   │   ├── ErrorCode.java                       # Error code enum
│   │   └── GlobalExceptionHandler.java          # Global error handler
│   └── mapper/
│       └── UserMapper.java                      # MapStruct DTO mapper
└── src/main/resources/
    ├── application.yaml                          # Configuration
    └── db/migration/
        ├── V1__initial_schema.sql               # Initial schema
        └── V2__add_role_enum_column.sql         # RBAC migration
```

### Key Components

#### 1. AuthenticationController

Public and protected authentication endpoints:
- `POST /auth/signup` - Create account
- `POST /auth/login` - Authenticate user
- `POST /auth/refresh` - Renew access token
- `POST /auth/logout` - Revoke tokens
- `POST /auth/introspect` - Validate token

#### 2. AdminController

Admin-only endpoints (requires ADMIN role):
- `PATCH /admin/users/{id}/role` - Update user role
- `GET /admin/roles` - List all roles with permissions

#### 3. AuthenticationService

Core authentication logic:
- User registration with BCrypt password hashing
- Login authentication
- JWT generation (access + refresh tokens)
- Token validation and introspection
- Logout with token revocation
- Token refresh with automatic rotation

#### 4. RoleEnum & PermissionEnum

Enum-based RBAC system:
- **RoleEnum**: USER, MODERATOR, ADMIN
- **PermissionEnum**: 12 granular permissions
- Hierarchical: ADMIN includes MODERATOR + USER permissions

#### 5. UserAuth Entity

User account information:
- UUID primary key
- Email (unique)
- Password hash (BCrypt)
- Role (RoleEnum)
- Email verification status
- Profile setup status
- OAuth integration (future)

#### 6. RefreshToken Entity

Refresh token storage:
- Token hash (MD5 for lookup)
- User reference
- Expiration timestamp
- Revocation timestamp
- Device info (future)

---

## Configuration

### application.yaml

```yaml
server:
  port: 8180
  servlet:
    context-path: /auth-service

spring:
  datasource:
    url: "jdbc:postgresql://localhost:5432/auth-service"
    username: postgres
    password: 123456
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
    show-sql: true

# JWT Configuration
jwt:
  signing-key: fbX2a4nQ4tdMnfExFUl+uA9aD9IFS+csS8GP96pR75RxrCiUcEYvpn+b4wWsgJshvXMUQiDUxhEBxA9RdPj+OQ==
  valid-duration: 3600        # Access token: 1 hour
  refreshable-duration: 36000 # Refresh token: 10 hours
```

### JWT Configuration Details

**Signing Key**:
- Algorithm: HS512 (HMAC with SHA-512)
- Base64-encoded secret (512 bits minimum)
- **CRITICAL**: Must match API Gateway's signing key
- Change in production!

**Token Durations**:
- **Access Token**: 3600 seconds (1 hour)
  - Short-lived for security
  - Used for API authentication
  - Stored in memory/storage (not cookies)

- **Refresh Token**: 36000 seconds (10 hours)
  - Long-lived but revocable
  - Used to get new access tokens
  - Automatically rotated on use
  - Hashed (MD5) in database

### Environment Variables (Production)

```bash
# Server
SERVER_PORT=8180

# Database
DB_URL=jdbc:postgresql://postgres:5432/auth-service
DB_USERNAME=auth_service_user
DB_PASSWORD=<strong-password>

# JWT
JWT_SIGNING_KEY=<base64-512bit-secret>
JWT_VALID_DURATION=3600
JWT_REFRESHABLE_DURATION=36000

# Admin
DEFAULT_ADMIN_EMAIL=admin@daypulse.com
DEFAULT_ADMIN_PASSWORD=<strong-password>
```

### Dependencies (pom.xml)

```xml
<dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Database -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Utilities -->
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
        <version>1.5.5.Final</version>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
    </dependency>
</dependencies>
```

---

## Database Schema

### Entity-Relationship Diagram

```
┌─────────────────────────┐
│      users_auth         │
├─────────────────────────┤
│ id (UUID, PK)           │
│ email (VARCHAR, UNIQUE) │
│ password_hash (VARCHAR) │
│ role (VARCHAR)          │  ← RoleEnum (USER, MODERATOR, ADMIN)
│ oauth_provider          │
│ oauth_id                │
│ is_email_verified       │
│ is_setup_complete       │
│ created_at              │
│ updated_at              │
└──────────┬──────────────┘
           │ 1:N
           │
           ▼
┌─────────────────────────┐
│   refresh_tokens        │
├─────────────────────────┤
│ id (UUID, PK)           │
│ user_id (UUID, FK)      │──┐
│ token_hash (VARCHAR)    │  │ FK → users_auth.id
│ device_info (TEXT)      │  │
│ expires_at (TIMESTAMP)  │  │
│ revoked_at (TIMESTAMP)  │  │
│ created_at (TIMESTAMP)  │  │
└─────────────────────────┘  │
                             │
┌─────────────────────────┐  │
│      otp_codes          │  │
├─────────────────────────┤  │
│ id (UUID, PK)           │  │
│ user_id (UUID, FK)      │──┘
│ code (VARCHAR(6))       │
│ type (VARCHAR(20))      │
│ expires_at (TIMESTAMP)  │
│ used_at (TIMESTAMP)     │
│ created_at (TIMESTAMP)  │
└─────────────────────────┘
```

### Table: users_auth

User account and authentication information.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK | User identifier |
| `email` | VARCHAR | UNIQUE, NOT NULL | User's email address |
| `password_hash` | VARCHAR | NOT NULL | BCrypt hashed password |
| `role` | VARCHAR | NOT NULL | RoleEnum (USER, MODERATOR, ADMIN) |
| `oauth_provider` | VARCHAR | NULL | OAuth provider (google, facebook, etc.) |
| `oauth_id` | VARCHAR | NULL | Provider-specific user ID |
| `is_email_verified` | BOOLEAN | DEFAULT false | Email verification status |
| `is_setup_complete` | BOOLEAN | DEFAULT false | Profile setup status |
| `created_at` | TIMESTAMP | NOT NULL | Account creation time |
| `updated_at` | TIMESTAMP | NOT NULL | Last update time |

**Indexes**:
```sql
CREATE INDEX idx_users_auth_email ON users_auth(email);
CREATE INDEX idx_users_auth_role ON users_auth(role);
```

### Table: refresh_tokens

Refresh token storage for token rotation.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK | Token identifier |
| `user_id` | UUID | FK, NOT NULL | Reference to users_auth |
| `token_hash` | VARCHAR | NOT NULL | MD5 hash of refresh token |
| `device_info` | TEXT | NULL | Device information (future) |
| `expires_at` | TIMESTAMP | NOT NULL | Token expiration time |
| `revoked_at` | TIMESTAMP | NULL | Revocation time (if revoked) |
| `created_at` | TIMESTAMP | NOT NULL | Token creation time |

**Indexes**:
```sql
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
```

**Token Cleanup**: Expired tokens should be periodically deleted:
```sql
DELETE FROM refresh_tokens WHERE expires_at < NOW();
```

### Table: otp_codes (Future)

One-time passwords for email verification and password reset.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK | OTP identifier |
| `user_id` | UUID | FK, NOT NULL | Reference to users_auth |
| `code` | VARCHAR(6) | NOT NULL | 6-digit OTP code |
| `type` | VARCHAR(20) | NOT NULL | 'email_verify', 'password_reset' |
| `expires_at` | TIMESTAMP | NOT NULL | OTP expiration (5 minutes) |
| `used_at` | TIMESTAMP | NULL | When OTP was used |
| `created_at` | TIMESTAMP | NOT NULL | OTP generation time |

---

## API Endpoints

### Public Endpoints (No Authentication)

#### 1. POST /auth/signup

Create a new user account.

**Request**:
```bash
curl -X POST http://localhost:8188/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

**Response** (201 Created):
```json
{
  "code": 200,
  "result": {
    "success": true,
    "userId": "abcd-1234-efgh-5678",
    "email": "user@example.com"
  }
}
```

**Business Logic**:
1. Validate email format and uniqueness
2. Hash password with BCrypt (cost 10)
3. Create user with ROLE_USER
4. Set `isEmailVerified = false`, `isSetupComplete = false`
5. Return user ID and email

**Error Codes**:
- `1002`: User with email already exists
- `400`: Invalid email or password format

---

#### 2. POST /auth/login

Authenticate user and receive tokens.

**Request**:
```bash
curl -X POST http://localhost:8188/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

**Response** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "user": {
      "id": "abcd-1234-efgh-5678",
      "email": "user@example.com",
      "isEmailVerified": false,
      "isSetupComplete": false
    },
    "tokens": {
      "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
      "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
      "expiresIn": 3600,
      "tokenType": "Bearer"
    }
  }
}
```

**Business Logic**:
1. Find user by email
2. Verify password with BCrypt.matches()
3. Generate access token (JWT, 1 hour)
4. Generate refresh token (JWT, 10 hours)
5. Hash and store refresh token in database
6. Return user info and tokens

**Access Token Claims**:
```json
{
  "sub": "user@example.com",
  "userId": "abcd-1234-efgh-5678",
  "scope": "ROLE_USER SEND_MESSAGE JOIN_ROOM VIEW_PROFILE EDIT_OWN_PROFILE",
  "iss": "daypulse-auth",
  "exp": 1706123456,
  "iat": 1706119856,
  "jti": "unique-token-id"
}
```

**Error Codes**:
- `1004`: Invalid credentials
- `1003`: User not found

---

#### 3. POST /auth/refresh

Renew access token using refresh token.

**Request**:
```bash
curl -X POST http://localhost:8188/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "token": "eyJhbGciOiJIUzUxMiJ9..."
  }'
```

**Response** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "user": {
      "id": "abcd-1234-efgh-5678",
      "email": "user@example.com"
    },
    "tokens": {
      "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
      "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
      "expiresIn": 3600,
      "tokenType": "Bearer"
    }
  }
}
```

**Business Logic** (Token Rotation):
1. Decode refresh token
2. Verify signature and expiration
3. Find token hash in database
4. Check if token is revoked
5. **Revoke old refresh token**
6. Generate new access token
7. **Generate new refresh token**
8. Store new refresh token hash
9. Return both new tokens

**Security Notes**:
- Old refresh token is immediately revoked
- Prevents token reuse attacks
- Forces clients to update stored refresh token

---

#### 4. POST /auth/introspect

Validate token (used by API Gateway).

**Request**:
```bash
curl -X POST http://localhost:8188/api/v1/auth/introspect \
  -H "Content-Type: application/json" \
  -d '{
    "token": "eyJhbGciOiJIUzUxMiJ9..."
  }'
```

**Response** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "valid": true
  }
}
```

**Business Logic**:
1. Decode JWT
2. Verify signature
3. Check expiration
4. Check if token is in revoked list (future: Redis blacklist)
5. Return validity status

**Used By**: API Gateway for token validation.

---

### Protected Endpoints (Require JWT)

#### 5. POST /auth/logout

Revoke user's refresh tokens.

**Request**:
```bash
curl -X POST http://localhost:8188/api/v1/auth/logout \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
```

**Response** (200 OK):
```json
{
  "code": 1000,
  "message": "Logout successful"
}
```

**Business Logic**:
1. Extract JWT from Authorization header
2. Decode JWT to get userId
3. Revoke all user's refresh tokens (set `revoked_at = NOW()`)
4. Future: Add access token to Redis blacklist

**Note**: Access token remains valid until expiration (1 hour). In production, implement Redis blacklist for immediate revocation.

---

### Admin Endpoints (Require ADMIN Role)

#### 6. PATCH /admin/users/{id}/role

Update a user's role.

**Request**:
```bash
curl -X PATCH http://localhost:8188/api/v1/admin/users/{userId}/role \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "role": "MODERATOR"
  }'
```

**Response** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "id": "user-uuid",
    "email": "user@example.com",
    "role": "MODERATOR"
  }
}
```

**Authorization**:
- Requires `ROLE_ADMIN` authority
- Checked via `@PreAuthorize("hasRole('ADMIN')")`

---

#### 7. GET /admin/roles

List all roles with permissions.

**Request**:
```bash
curl -X GET http://localhost:8188/api/v1/admin/roles \
  -H "Authorization: Bearer <admin-token>"
```

**Response** (200 OK):
```json
{
  "code": 1000,
  "result": [
    {
      "name": "USER",
      "displayName": "Regular User",
      "permissions": [
        "SEND_MESSAGE",
        "JOIN_ROOM",
        "VIEW_PROFILE",
        "EDIT_OWN_PROFILE"
      ]
    },
    {
      "name": "MODERATOR",
      "displayName": "Moderator",
      "permissions": [
        "SEND_MESSAGE", "JOIN_ROOM", "VIEW_PROFILE", "EDIT_OWN_PROFILE",
        "DELETE_MESSAGE", "MUTE_USER", "BAN_USER", "PIN_MESSAGE"
      ]
    },
    {
      "name": "ADMIN",
      "displayName": "Administrator",
      "permissions": [
        "SEND_MESSAGE", "JOIN_ROOM", "VIEW_PROFILE", "EDIT_OWN_PROFILE",
        "DELETE_MESSAGE", "MUTE_USER", "BAN_USER", "PIN_MESSAGE",
        "MANAGE_ROOMS", "MANAGE_USERS", "MANAGE_ROLES", "VIEW_ANALYTICS"
      ]
    }
  ]
}
```

---

## Security

### Password Security

**BCrypt Hashing**:
```java
// Registration
String passwordHash = BCryptPasswordEncoder().encode(plainPassword);
// Cost factor: 10 (default)
// Salt: Automatically generated per password
// Output: 60-character hash
```

**Password Requirements** (Validation):
- Minimum 8 characters
- No maximum (unlimited)
- No complexity requirements (consider adding in production)

### JWT Security

**Token Structure**:
```
Header.Payload.Signature

Header:
{
  "alg": "HS512",
  "typ": "JWT"
}

Payload:
{
  "sub": "user@example.com",
  "userId": "abcd-1234-efgh-5678",
  "scope": "ROLE_USER ...",
  "iss": "daypulse-auth",
  "exp": 1706123456,
  "iat": 1706119856,
  "jti": "unique-token-id"
}

Signature:
HMACSHA512(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  secret_key
)
```

**Signing Key Requirements**:
- Minimum 512 bits (64 bytes)
- Base64-encoded
- **MUST** match API Gateway's key
- **MUST** be changed in production
- Store in environment variables, not config files

### Refresh Token Security

**Storage**:
- Tokens hashed with MD5 before database storage
- Only hash stored, not actual token
- Lookup by hash for validation

**Rotation**:
- New refresh token generated on each use
- Old token immediately revoked
- Prevents token reuse attacks

**Expiration**:
- 10 hours validity
- Automatic cleanup of expired tokens (future)

### Default Admin Account

**Auto-Created on Startup**:
```
Email: admin@daypulse.com
Password: Admin@123
Role: ADMIN
```

**Security Warning**:
- ⚠️ **CHANGE PASSWORD IMMEDIATELY** in production
- ⚠️ Disable auto-creation in production
- ⚠️ Use strong, unique password

---

## RBAC System

### Role Hierarchy

```
┌──────────────────────────────────────────────────┐
│                    ADMIN                         │
│  All MODERATOR permissions +                     │
│  • MANAGE_ROOMS                                  │
│  • MANAGE_USERS                                  │
│  • MANAGE_ROLES                                  │
│  • VIEW_ANALYTICS                                │
└──────────────────────┬───────────────────────────┘
                       │ includes
                       ▼
┌──────────────────────────────────────────────────┐
│                  MODERATOR                       │
│  All USER permissions +                          │
│  • DELETE_MESSAGE                                │
│  • MUTE_USER                                     │
│  • BAN_USER                                      │
│  • PIN_MESSAGE                                   │
└──────────────────────┬───────────────────────────┘
                       │ includes
                       ▼
┌──────────────────────────────────────────────────┐
│                    USER                          │
│  • SEND_MESSAGE                                  │
│  • JOIN_ROOM                                     │
│  • VIEW_PROFILE                                  │
│  • EDIT_OWN_PROFILE                              │
└──────────────────────────────────────────────────┘
```

### Roles

**USER** (Default):
- Basic user functionality
- Can send messages and join rooms
- Can view and edit own profile

**MODERATOR**:
- All USER permissions
- Content moderation (delete messages, pin)
- User moderation (mute, ban)

**ADMIN**:
- All MODERATOR permissions
- System administration
- User role management
- Analytics access

### Permissions (12 Total)

| Permission | Description | Roles |
|------------|-------------|-------|
| `SEND_MESSAGE` | Send chat messages | USER, MODERATOR, ADMIN |
| `JOIN_ROOM` | Join chat rooms | USER, MODERATOR, ADMIN |
| `VIEW_PROFILE` | View user profiles | USER, MODERATOR, ADMIN |
| `EDIT_OWN_PROFILE` | Edit own profile | USER, MODERATOR, ADMIN |
| `DELETE_MESSAGE` | Delete any message | MODERATOR, ADMIN |
| `MUTE_USER` | Mute users in chat | MODERATOR, ADMIN |
| `BAN_USER` | Ban users from rooms | MODERATOR, ADMIN |
| `PIN_MESSAGE` | Pin important messages | MODERATOR, ADMIN |
| `MANAGE_ROOMS` | Create/edit/delete rooms | ADMIN |
| `MANAGE_USERS` | User administration | ADMIN |
| `MANAGE_ROLES` | Role management | ADMIN |
| `VIEW_ANALYTICS` | View system analytics | ADMIN |

### JWT Scope Claim

**Scope Format**: Space-separated permissions

```
USER:
"ROLE_USER SEND_MESSAGE JOIN_ROOM VIEW_PROFILE EDIT_OWN_PROFILE"

MODERATOR:
"ROLE_MODERATOR SEND_MESSAGE JOIN_ROOM VIEW_PROFILE EDIT_OWN_PROFILE DELETE_MESSAGE MUTE_USER BAN_USER PIN_MESSAGE"

ADMIN:
"ROLE_ADMIN SEND_MESSAGE JOIN_ROOM VIEW_PROFILE EDIT_OWN_PROFILE DELETE_MESSAGE MUTE_USER BAN_USER PIN_MESSAGE MANAGE_ROOMS MANAGE_USERS MANAGE_ROLES VIEW_ANALYTICS"
```

**Usage in Services**:
```java
// Spring Security checks
@PreAuthorize("hasRole('ADMIN')")
@PreAuthorize("hasAuthority('DELETE_MESSAGE')")
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
```

---

## Authentication Flows

### 1. User Registration Flow

```
┌────────┐           ┌──────────┐           ┌──────────┐
│ Client │           │ Gateway  │           │   Auth   │
│        │           │          │           │ Service  │
└───┬────┘           └────┬─────┘           └────┬─────┘
    │                     │                      │
    │ 1. POST /auth/signup                      │
    ├─────────────────────>│                      │
    │ {email, password}   │                      │
    │                     │                      │
    │                     │ 2. Forward request   │
    │                     ├─────────────────────>│
    │                     │                      │
    │                     │                      │ 3. Validate email
    │                     │                      │    • Check unique
    │                     │                      │    • Check format
    │                     │                      │
    │                     │                      │ 4. Hash password
    │                     │                      │    BCrypt (cost 10)
    │                     │                      │
    │                     │                      │ 5. Create user
    │                     │                      │    • role = USER
    │                     │                      │    • verified = false
    │                     │                      │
    │                     │ 6. Return user info  │
    │                     │<─────────────────────┤
    │                     │ {userId, email}      │
    │                     │                      │
    │ 7. Success          │                      │
    │<─────────────────────┤                      │
    │ 201 Created         │                      │
```

### 2. Login Flow

```
┌────────┐           ┌──────────┐           ┌──────────┐
│ Client │           │ Gateway  │           │   Auth   │
│        │           │          │           │ Service  │
└───┬────┘           └────┬─────┘           └────┬─────┘
    │                     │                      │
    │ 1. POST /auth/login                       │
    ├─────────────────────>│                      │
    │ {email, password}   │                      │
    │                     │                      │
    │                     │ 2. Forward           │
    │                     ├─────────────────────>│
    │                     │                      │
    │                     │                      │ 3. Find user
    │                     │                      │    by email
    │                     │                      │
    │                     │                      │ 4. Verify password
    │                     │                      │    BCrypt.matches()
    │                     │                      │
    │                     │                      │ 5. Generate tokens
    │                     │                      │    • Access JWT (1h)
    │                     │                      │    • Refresh JWT (10h)
    │                     │                      │
    │                     │                      │ 6. Store refresh
    │                     │                      │    token hash in DB
    │                     │                      │
    │                     │ 7. Return tokens     │
    │                     │<─────────────────────┤
    │                     │ {access, refresh}    │
    │                     │                      │
    │ 8. Store tokens     │                      │
    │<─────────────────────┤                      │
    │ 200 OK              │                      │
    │                     │                      │
    │ 9. Save tokens      │                      │
    │    in storage       │                      │
```

### 3. Token Refresh Flow

```
┌────────┐           ┌──────────┐           ┌──────────┐
│ Client │           │ Gateway  │           │   Auth   │
│        │           │          │           │ Service  │
└───┬────┘           └────┬─────┘           └────┬─────┘
    │                     │                      │
    │ 1. POST /auth/refresh                     │
    ├─────────────────────>│                      │
    │ {refreshToken}      │                      │
    │                     │                      │
    │                     │ 2. Forward           │
    │                     ├─────────────────────>│
    │                     │                      │
    │                     │                      │ 3. Decode refresh
    │                     │                      │    token (JWT)
    │                     │                      │
    │                     │                      │ 4. Verify signature
    │                     │                      │    & expiration
    │                     │                      │
    │                     │                      │ 5. Hash token
    │                     │                      │    MD5(token)
    │                     │                      │
    │                     │                      │ 6. Find in DB
    │                     │                      │    by token_hash
    │                     │                      │
    │                     │                      │ 7. Check revoked_at
    │                     │                      │    (must be NULL)
    │                     │                      │
    │                     │                      │ 8. REVOKE old token
    │                     │                      │    SET revoked_at=NOW()
    │                     │                      │
    │                     │                      │ 9. Generate new tokens
    │                     │                      │    • New access JWT
    │                     │                      │    • New refresh JWT
    │                     │                      │
    │                     │                      │ 10. Store new refresh
    │                     │                      │     token hash
    │                     │                      │
    │                     │ 11. Return NEW tokens│
    │                     │<─────────────────────┤
    │                     │ {access, refresh}    │
    │                     │                      │
    │ 12. Update tokens   │                      │
    │<─────────────────────┤                      │
    │ 200 OK              │                      │
    │                     │                      │
    │ 13. Replace old     │                      │
    │     tokens in       │                      │
    │     storage         │                      │
```

**Token Rotation Benefits**:
- Prevents token reuse attacks
- Limits impact of token theft
- Automatic invalidation of old tokens

### 4. Logout Flow

```
┌────────┐           ┌──────────┐           ┌──────────┐
│ Client │           │ Gateway  │           │   Auth   │
│        │           │          │           │ Service  │
└───┬────┘           └────┬─────┘           └────┬─────┘
    │                     │                      │
    │ 1. POST /auth/logout                      │
    ├─────────────────────>│                      │
    │ Authorization: Bearer│                      │
    │ <accessToken>       │                      │
    │                     │                      │
    │                     │ 2. Validate JWT      │
    │                     │    (signature, exp)  │
    │                     │                      │
    │                     │ 3. Add headers       │
    │                     │    X-User-Id: uuid   │
    │                     │                      │
    │                     │ 4. Forward           │
    │                     ├─────────────────────>│
    │                     │ X-User-Id: uuid      │
    │                     │                      │
    │                     │                      │ 5. Extract userId
    │                     │                      │    from JWT/header
    │                     │                      │
    │                     │                      │ 6. Revoke ALL user's
    │                     │                      │    refresh tokens
    │                     │                      │    UPDATE refresh_tokens
    │                     │                      │    SET revoked_at = NOW()
    │                     │                      │    WHERE user_id = ?
    │                     │                      │
    │                     │                      │ 7. Future: Blacklist
    │                     │                      │    access token in Redis
    │                     │                      │
    │                     │ 8. Success           │
    │                     │<─────────────────────┤
    │                     │ "Logout successful"  │
    │                     │                      │
    │ 9. Clear tokens     │                      │
    │<─────────────────────┤                      │
    │ 200 OK              │                      │
    │                     │                      │
    │ 10. Delete tokens   │                      │
    │     from storage    │                      │
```

**Note**: Access token remains valid until expiration. Implement Redis blacklist for immediate revocation.

---

## Integration

### Service-to-Service

#### Gateway → Auth Service

**Token Introspection**:
```http
POST /auth-service/auth/introspect
Content-Type: application/json

{
  "token": "eyJhbGciOiJIUzUxMiJ9..."
}

Response:
{
  "code": 1000,
  "result": {
    "valid": true
  }
}
```

**Frequency**: Every authenticated request through gateway

**Performance**: ~50-200ms per request

**Future Optimization**: Redis token blacklist for faster validation

#### Auth Service → User Service

**Initialize User Profile** (After Registration):
```http
POST /user-service/internal/users/{userId}/init
X-User-Id: {userId}

{
  "email": "user@example.com"
}
```

**Called**: After successful signup to create user profile

---

## Development

### Running Locally

```bash
# Navigate to auth service directory
cd backEnd/auth-service

# Run with Maven
mvn spring-boot:run

# Or build and run JAR
mvn clean package -DskipTests
java -jar target/auth-service-0.0.1-SNAPSHOT.jar
```

### Database Setup

```bash
# Create database
psql -U postgres -c "CREATE DATABASE \"auth-service\";"

# Flyway migrations run automatically on startup
# Check migration history
psql -U postgres -d auth-service -c "SELECT * FROM flyway_schema_history;"
```

### Health Check

```bash
curl http://localhost:8180/auth-service/actuator/health
```

Expected response:
```json
{
  "status": "UP"
}
```

### Testing Endpoints

**Register User**:
```bash
curl -X POST http://localhost:8180/auth-service/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com", "password": "password123"}'
```

**Login**:
```bash
curl -X POST http://localhost:8180/auth-service/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com", "password": "password123"}'
```

**Login as Admin**:
```bash
curl -X POST http://localhost:8180/auth-service/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@daypulse.com", "password": "Admin@123"}'
```

### Common Development Tasks

**Generate New JWT Signing Key**:
```bash
# Generate 512-bit random key
openssl rand -base64 64

# Use in application.yaml
jwt:
  signing-key: <generated-key>
```

**Change Admin Password**:
```java
// In DataInitializer.java
String adminPassword = "NewSecurePassword123!";
```

**Add New Permission**:
```java
// In PermissionEnum.java
public enum PermissionEnum {
    // ... existing permissions
    NEW_PERMISSION("Description")
}

// In RoleEnum.java
ADMIN(
    "Administrator",
    Set.of(
        // ... existing permissions
        PermissionEnum.NEW_PERMISSION
    )
)
```

---

## Troubleshooting

### Common Issues

#### 1. 401 Unauthenticated on Login

**Symptom**: Login returns 401

**Possible Causes**:
- Wrong email/password
- User doesn't exist
- Database connection issues

**Debug**:
```bash
# Check if user exists
psql -U postgres -d auth-service -c \
  "SELECT * FROM users_auth WHERE email = 'user@example.com';"

# Check password hash
psql -U postgres -d auth-service -c \
  "SELECT email, password_hash FROM users_auth WHERE email = 'user@example.com';"
```

#### 2. JWT Signature Mismatch

**Symptom**: Token validation fails at gateway

**Cause**: Signing key mismatch between Auth Service and Gateway

**Fix**:
```yaml
# Verify keys match exactly
# auth-service/application.yaml
jwt:
  signing-key: ABC123...

# api-gateway/application.yaml
jwt:
  signing-key: ABC123...  # Must match!
```

#### 3. Refresh Token Revoked

**Symptom**: Token refresh returns error

**Possible Causes**:
- Token already used (rotation)
- Token manually revoked (logout)
- Token expired

**Debug**:
```bash
# Check refresh token status
psql -U postgres -d auth-service -c \
  "SELECT * FROM refresh_tokens WHERE user_id = 'user-uuid';"

# Check for revoked tokens
psql -U postgres -d auth-service -c \
  "SELECT * FROM refresh_tokens WHERE revoked_at IS NOT NULL;"
```

#### 4. Default Admin Not Created

**Symptom**: Can't login as admin

**Fix**:
```bash
# Check if admin exists
psql -U postgres -d auth-service -c \
  "SELECT * FROM users_auth WHERE role = 'ADMIN';"

# If not, restart service (DataInitializer runs on startup)
mvn spring-boot:run

# Or manually create admin
psql -U postgres -d auth-service -c \
  "INSERT INTO users_auth (id, email, password_hash, role, is_email_verified, is_setup_complete) 
   VALUES (gen_random_uuid(), 'admin@daypulse.com', '<bcrypt-hash>', 'ADMIN', true, true);"
```

#### 5. Database Connection Failed

**Symptom**: Service won't start, connection errors

**Debug**:
```bash
# Check PostgreSQL running
psql -U postgres -l

# Check database exists
psql -U postgres -c "SELECT datname FROM pg_database WHERE datname = 'auth-service';"

# Test connection
psql -U postgres -d auth-service -c "SELECT 1;"

# Verify credentials in application.yaml
spring:
  datasource:
    url: "jdbc:postgresql://localhost:5432/auth-service"
    username: postgres
    password: 123456  # Check this matches your PostgreSQL password
```

### Performance Issues

**Slow Login** (>1 second):
- BCrypt cost too high (default 10 is fine)
- Database query optimization needed
- Add index on email column

**Slow Token Introspection**:
- Add Redis blacklist for faster checks
- Implement caching for user lookups
- Optimize database queries

### Debugging Tips

**Enable SQL Logging**:
```yaml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true

logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

**Check JWT Contents**:
- Decode at https://jwt.io
- Verify claims (sub, userId, scope, exp)
- Check signature with signing key

---

## Future Enhancements

### Planned Features

**1. Email Verification**:
- Send OTP codes via email
- Verify email before full account access
- OTP expiration and resend logic

**2. Password Reset**:
- Forgot password flow
- Email-based password reset
- Secure OTP generation

**3. OAuth2 Integration**:
- Google Sign-In
- Facebook Login
- Apple Sign-In
- GitHub OAuth

**4. Multi-Factor Authentication (MFA)**:
- TOTP (Time-based One-Time Password)
- SMS verification
- Backup codes

**5. Redis Token Blacklist**:
- Immediate access token revocation
- Faster introspection (sub-millisecond)
- Reduced database load

**6. Session Management**:
- Multi-device tracking
- Device fingerprinting
- Force logout all devices
- View active sessions

**7. Rate Limiting**:
- Login attempt limiting
- Brute force protection
- Account lockout after failed attempts

**8. Audit Logging**:
- Track authentication events
- Failed login attempts
- Role changes
- Admin actions

---

**Last Updated**: 2026-01-22  
**Version**: 0.1.0  
**Status**: Production Ready
