# Day-Pulse Backend - Microservices Architecture

## Overview

Day-Pulse backend follows a microservices architecture with an API Gateway pattern, implementing industry-standard OAuth 2.0-style JWT authentication.

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENT LAYER                             │
│  (Web Browser, Mobile App, Desktop App)                         │
│                                                                   │
│  Authentication: Authorization: Bearer <access_token>            │
└────────────────────────┬────────────────────────────────────────┘
                         │ HTTPS (Production)
                         │ HTTP (Development)
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                       API GATEWAY (Port 8188)                    │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Responsibilities:                                         │  │
│  │ • Route requests to appropriate microservices            │  │
│  │ • Validate JWT tokens (signature, expiration, revocation)│  │
│  │ • Extract user identity from JWT claims                  │  │
│  │ • Add internal headers (X-User-Id, X-User-Roles)         │  │
│  │ • CORS configuration                                      │  │
│  │ • Rate limiting (future)                                  │  │
│  └──────────────────────────────────────────────────────────┘  │
└──────────┬──────────────────────┬───────────────────────────────┘
           │                      │
           │                      │
    ┌──────▼──────┐        ┌─────▼─────┐
    │ Auth Service│        │User Service│
    │ Port 8180   │        │ Port 8181  │
    └─────────────┘        └────────────┘
```

---

## Services

### 1. API Gateway (Port 8188)
**Technology**: Spring Cloud Gateway (Reactive)

**Responsibilities**:
- Request routing to microservices
- JWT token validation
- User identity extraction and forwarding
- CORS handling
- Centralized logging (future)
- Rate limiting (future)

**Routes**:
- `/api/v1/auth/**` → Auth Service
- `/api/v1/users/**` → User Service

**Key Files**:
- `configuration/SecurityConfig.java` - Security rules (public vs protected)
- `security/GatewayJwtAuthenticationFilter.java` - JWT validation logic
- `client/AuthServiceClient.java` - Auth service integration

### 2. Auth Service (Port 8180)
**Technology**: Spring Boot + Spring Security + OAuth2 Resource Server

**Responsibilities**:
- User registration (signup)
- User authentication (login)
- JWT token generation (access + refresh tokens)
- Token validation and revocation
- User credential management
- Role and permission management

**Key Endpoints**:
| Endpoint | Method | Auth Required | Description |
|----------|--------|---------------|-------------|
| `/auth/signup` | POST | No | Create new account |
| `/auth/login` | POST | No | Authenticate user |
| `/auth/refresh` | POST | No | Renew access token |
| `/auth/logout` | POST | Yes | Revoke tokens |
| `/auth/introspect` | POST | No | Validate token |
| `/users/my-info` | GET | Yes | Get auth user info |

**Key Files**:
- `controller/AuthenticationController.java` - Auth endpoints
- `service/AuthenticationService.java` - Token generation & validation
- `config/SecurityConfig.java` - Security configuration
- `config/CustomJwtDecoder.java` - JWT decoding logic

### 3. User Service (Port 8181)
**Technology**: Spring Boot + JPA

**Responsibilities**:
- User profile management (CRUD)
- User stats tracking
- Follow/unfollow functionality
- User search and discovery

**Key Endpoints**:
| Endpoint | Method | Auth Required | Description |
|----------|--------|---------------|-------------|
| `/users/me` | GET | Yes | Get my profile |
| `/users/me` | PATCH | Yes | Update my profile |
| `/users/me/setup` | POST | Yes | Initial profile setup |
| `/users/{id}` | GET | Yes | Get user by ID |
| `/users/{id}/followers` | GET | Yes | Get followers |
| `/users/{id}/following` | GET | Yes | Get following |

**Internal Endpoints** (Not exposed via gateway):
| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/internal/users/{id}/summary` | GET | Get user summary for denormalization |
| `/internal/users/{id}/init` | POST | Initialize profile after registration |

**Key Files**:
- `controller/UserController.java` - User endpoints
- `controller/InternalUserController.java` - Internal service-to-service APIs
- `service/UserProfileService.java` - Profile business logic
- `service/FollowService.java` - Follow/unfollow logic

---

## Authentication & Authorization Flow

### 1. User Signup

```
Client                    API Gateway              Auth Service
  │                            │                        │
  │  POST /api/v1/auth/signup  │                        │
  ├───────────────────────────>│                        │
  │  Body: {email, password}   │   POST /auth-service/  │
  │                            │   auth/signup          │
  │                            ├───────────────────────>│
  │                            │                        │
  │                            │                        │ 1. Validate input
  │                            │                        │ 2. Hash password
  │                            │                        │ 3. Save to DB
  │                            │                        │ 4. Assign default role
  │                            │   201 Created          │
  │                            │<───────────────────────┤
  │   201 Created              │   {success, userId,    │
  │<───────────────────────────┤    email}              │
  │   {success, userId, email} │                        │
```

### 2. User Login

```
Client                    API Gateway              Auth Service
  │                            │                        │
  │  POST /api/v1/auth/login   │                        │
  ├───────────────────────────>│                        │
  │  Body: {email, password}   │   POST /auth-service/  │
  │                            │   auth/login           │
  │                            ├───────────────────────>│
  │                            │                        │
  │                            │                        │ 1. Verify credentials
  │                            │                        │ 2. Generate access token (1h)
  │                            │                        │ 3. Generate refresh token (10h)
  │                            │                        │ 4. Save refresh token hash
  │                            │   200 OK               │
  │                            │<───────────────────────┤
  │   200 OK                   │   {user, tokens}       │
  │<───────────────────────────┤                        │
  │   {user, tokens:           │                        │
  │    {accessToken,           │                        │
  │     refreshToken,          │                        │
  │     expiresIn: 3600}}      │                        │
  │                            │                        │
  │ Store tokens in memory/storage                      │
```

### 3. Accessing Protected Resources

```
Client                    API Gateway              User Service
  │                            │                        │
  │  GET /api/v1/users/me      │                        │
  ├───────────────────────────>│                        │
  │  Header:                   │                        │
  │  Authorization: Bearer <token>                      │
  │                            │                        │
  │                            │ 1. Extract JWT token   │
  │                            │ 2. Validate signature  │
  │                            │ 3. Check expiration    │
  │                            │ 4. Call auth service   │
  │                            │    for introspection   │
  │                            │ 5. Extract userId from │
  │                            │    JWT claims          │
  │                            │                        │
  │                            │   GET /user-service/   │
  │                            │   users/me             │
  │                            ├───────────────────────>│
  │                            │   Headers:             │
  │                            │   X-User-Id: abc-123   │
  │                            │   X-User-Roles: ROLE_  │
  │                            │   USER                 │
  │                            │                        │
  │                            │                        │ 1. Read X-User-Id
  │                            │                        │ 2. Fetch profile
  │                            │   200 OK               │
  │                            │<───────────────────────┤
  │   200 OK                   │   {profile data}       │
  │<───────────────────────────┤                        │
  │   {profile data}           │                        │
```

### 4. Token Refresh

```
Client                    API Gateway              Auth Service
  │                            │                        │
  │  POST /api/v1/auth/refresh │                        │
  ├───────────────────────────>│                        │
  │  Body: {token: refresh_token}                       │
  │                            │   POST /auth-service/  │
  │                            │   auth/refresh         │
  │                            ├───────────────────────>│
  │                            │                        │
  │                            │                        │ 1. Validate refresh token
  │                            │                        │ 2. Check not revoked
  │                            │                        │ 3. Revoke old refresh token
  │                            │                        │ 4. Generate new access token
  │                            │                        │ 5. Generate new refresh token
  │                            │   200 OK               │
  │                            │<───────────────────────┤
  │   200 OK                   │   {user, tokens}       │
  │<───────────────────────────┤                        │
  │   {new tokens}             │                        │
```

---

## API Design Standards

### Request Format

**Public Endpoints** (signup, login, refresh):
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "securePassword123"
}
```

**Protected Endpoints** (require authentication):
```http
GET /api/v1/users/me
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIi...
```

### Response Format

**Success Response**:
```json
{
  "code": 200,
  "message": "Success",
  "result": {
    "user": {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "email": "user@example.com"
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

**Error Response**:
```json
{
  "code": 1006,
  "message": "Unauthenticated"
}
```

### Token Standards

**Access Token**:
- Algorithm: HS512 (HMAC with SHA-512)
- Expiration: 1 hour (configurable)
- Claims: `sub` (email), `userId`, `scope` (roles), `iss`, `exp`, `iat`, `jti`

**Refresh Token**:
- Algorithm: HS512
- Expiration: 10 hours (configurable)
- Claims: `sub`, `userId`, `type: "refresh"`, `iss`, `exp`, `iat`, `jti`
- Stored: Hashed in database (MD5 for lookup)
- Rotation: New refresh token on each use

---

## Configuration

### Environment Variables (Production)

```bash
# Auth Service
JWT_SIGNING_KEY=<base64-encoded-secret-512-bits>
DB_URL=jdbc:postgresql://postgres:5432/auth-service
DB_USERNAME=auth_user
DB_PASSWORD=<secure-password>

# User Service
DB_URL=jdbc:postgresql://postgres:5432/user-service
DB_USERNAME=user_user
DB_PASSWORD=<secure-password>

# API Gateway
JWT_SIGNING_KEY=<same-as-auth-service>
AUTH_SERVICE_URL=http://auth-service:8180
USER_SERVICE_URL=http://user-service:8181
```

### application.yaml Examples

**Auth Service** (`auth-service/src/main/resources/application.yaml`):
```yaml
server:
  port: 8180
  servlet:
    context-path: /auth-service

jwt:
  signing-key: ${JWT_SIGNING_KEY}
  valid-duration: 3600      # 1 hour
  refreshable-duration: 36000  # 10 hours

spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

**API Gateway** (`api-gateway/src/main/resources/application.yaml`):
```yaml
server:
  port: 8188

jwt:
  signing-key: ${JWT_SIGNING_KEY}

spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: ${AUTH_SERVICE_URL:http://localhost:8180}
          predicates:
            - Path=/api/v1/auth/**
          filters:
            - RewritePath=/api/v1/auth/(?<segment>.*), /auth-service/auth/${segment}
```

---

## Database Schema

### Auth Service Database

**users_auth**:
- `id` (UUID, PK)
- `email` (VARCHAR, UNIQUE)
- `password_hash` (VARCHAR)
- `is_email_verified` (BOOLEAN)
- `is_setup_complete` (BOOLEAN)
- `created_at`, `updated_at`

**refresh_tokens**:
- `id` (BIGINT, PK)
- `user_id` (UUID, FK → users_auth.id)
- `token_hash` (VARCHAR, UNIQUE)
- `expires_at` (TIMESTAMP)
- `revoked_at` (TIMESTAMP, nullable)
- `created_at`

**roles**:
- `name` (VARCHAR, PK) - e.g., "USER", "ADMIN"
- `description` (VARCHAR)

**permissions**:
- `name` (VARCHAR, PK) - e.g., "READ_USER", "WRITE_POST"
- `description` (VARCHAR)

**user_roles** (many-to-many):
- `user_id` (UUID, FK)
- `role_name` (VARCHAR, FK)

**role_permissions** (many-to-many):
- `role_name` (VARCHAR, FK)
- `permission_name` (VARCHAR, FK)

### User Service Database

**user_profiles**:
- `id` (UUID, PK) - Same as auth user ID
- `username` (VARCHAR, UNIQUE)
- `display_name` (VARCHAR)
- `bio` (TEXT)
- `avatar_url` (VARCHAR)
- `cover_image_url` (VARCHAR)
- `location` (VARCHAR)
- `website` (VARCHAR)
- `birth_date` (DATE)
- `created_at`, `updated_at`

**user_stats**:
- `user_id` (UUID, PK, FK)
- `followers_count` (INT)
- `following_count` (INT)
- `pulses_count` (INT)

**follows** (many-to-many):
- `follower_id` (UUID, FK)
- `following_id` (UUID, FK)
- `created_at`

---

## Development Setup

### Prerequisites
- Java 21
- Maven 3.8+
- PostgreSQL 14+
- IDE (IntelliJ IDEA recommended)

### Database Setup

```sql
-- Create databases
CREATE DATABASE "auth-service";
CREATE DATABASE "user-service";

-- Create users (optional, for separation)
CREATE USER auth_user WITH PASSWORD 'auth_password';
CREATE USER user_user WITH PASSWORD 'user_password';

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE "auth-service" TO auth_user;
GRANT ALL PRIVILEGES ON DATABASE "user-service" TO user_user;
```

### Running Services

**Option 1: IDE**
1. Open each service as a Maven project
2. Run main application class (e.g., `AuthServiceApplication.java`)

**Option 2: Maven**
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

**Option 3: JAR**
```bash
# Build
mvn clean package -DskipTests

# Run
java -jar auth-service/target/auth-service-0.0.1-SNAPSHOT.jar
java -jar user-service/target/user-service-0.0.1-SNAPSHOT.jar
java -jar api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar
```

### Testing

```bash
# 1. Signup
curl -X POST http://localhost:8188/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com", "password": "password123"}'

# 2. Login
curl -X POST http://localhost:8188/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com", "password": "password123"}'

# Save the accessToken from response

# 3. Get Profile
curl -X GET http://localhost:8188/api/v1/users/me \
  -H "Authorization: Bearer <accessToken>"
```

---

## Production Deployment

### Docker Compose (Recommended)

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:14-alpine
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    volumes:
      - postgres_data:/var/lib/postgresql/data

  auth-service:
    build: ./auth-service
    environment:
      DB_URL: jdbc:postgresql://postgres:5432/auth-service
      JWT_SIGNING_KEY: ${JWT_SIGNING_KEY}
    depends_on:
      - postgres

  user-service:
    build: ./user-service
    environment:
      DB_URL: jdbc:postgresql://postgres:5432/user-service
    depends_on:
      - postgres

  api-gateway:
    build: ./api-gateway
    ports:
      - "8188:8188"
    environment:
      JWT_SIGNING_KEY: ${JWT_SIGNING_KEY}
      AUTH_SERVICE_URL: http://auth-service:8180
      USER_SERVICE_URL: http://user-service:8181
    depends_on:
      - auth-service
      - user-service
```

### Kubernetes

See `k8s/` directory for Kubernetes manifests (future).

---

## Security Best Practices

### In Production

✅ **DO**:
- Use HTTPS for all external communication
- Rotate JWT signing keys periodically
- Store secrets in vault (HashiCorp Vault, AWS Secrets Manager)
- Enable database encryption at rest
- Use strong password hashing (BCrypt with high cost)
- Implement rate limiting on auth endpoints
- Log authentication events for audit
- Use network policies to isolate services
- Implement database connection pooling
- Use prepared statements (prevent SQL injection)

❌ **DON'T**:
- Expose services directly to the internet (use gateway)
- Store passwords in plaintext
- Use weak JWT signing keys (<512 bits)
- Log sensitive data (passwords, tokens)
- Skip input validation
- Use default credentials
- Ignore security updates

### Network Architecture

```
Internet
    │
    ▼
[Load Balancer / CDN]
    │
    ▼
[API Gateway] ← Public Subnet
    │
    ├──[Auth Service] ← Private Subnet
    ├──[User Service] ← Private Subnet
    └──[Feed Service] ← Private Subnet (future)
         │
         ▼
    [Database] ← Private Subnet (isolated)
```

---

## Monitoring & Observability (Future)

### Planned Features

1. **Distributed Tracing** (OpenTelemetry + Jaeger)
2. **Metrics** (Prometheus + Grafana)
3. **Logging** (ELK Stack or Loki)
4. **Health Checks** (Spring Boot Actuator)
5. **Service Mesh** (Istio or Linkerd)

---

## API Documentation

### Interactive Documentation

- **Swagger/OpenAPI**: Access at `/swagger-ui.html` (future)
- **Postman Collection**: See `docs/postman/` (future)
- **API Design Standard**: See `API_DESIGN_STANDARD.md`

---

## Troubleshooting

### Common Issues

**Issue**: "Unauthenticated" error on protected endpoints
- **Cause**: Invalid/expired token or missing Authorization header
- **Solution**: Check token expiration, refresh token if needed

**Issue**: CORS errors in browser
- **Cause**: Frontend origin not whitelisted
- **Solution**: Add origin to `application.yaml` CORS config

**Issue**: "Connection refused" between services
- **Cause**: Service not running or wrong port
- **Solution**: Verify all services are running with `curl http://localhost:PORT/actuator/health`

---

## Contributing

See `CONTRIBUTING.md` for development guidelines (future).

---

## License

Copyright © 2025 Day-Pulse. All rights reserved.
