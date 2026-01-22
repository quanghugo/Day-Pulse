# API Gateway Service

Single entry point for all client requests in the DayPulse microservices architecture.

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Configuration](#configuration)
- [Security](#security)
- [Routes](#routes)
- [JWT Validation Flow](#jwt-validation-flow)
- [Integration](#integration)
- [Development](#development)
- [Troubleshooting](#troubleshooting)

---

## Overview

### Purpose

The API Gateway serves as the **single entry point** for all client requests, providing:
- Centralized authentication and authorization
- Request routing to downstream microservices
- User identity extraction and forwarding
- CORS handling
- Cross-cutting concerns (rate limiting, circuit breakers - future)

### Basic Information

| Property | Value |
|----------|-------|
| **Service Name** | api-gateway |
| **Port** | 8188 |
| **Technology** | Spring Cloud Gateway (Reactive) |
| **Framework** | Spring Boot 3.5.10 |
| **Base URL** | `http://localhost:8188` |
| **API Version** | `/api/v1` |

### Technology Stack

```yaml
Core:
  - Spring Cloud Gateway 2025.0.1 (Reactive)
  - Spring WebFlux (Non-blocking I/O)
  - Spring Security
  - OAuth2 Resource Server

Authentication:
  - JWT (HS512 signature validation)
  - Token introspection via Auth Service

Build:
  - Maven
  - Java 21
  - Lombok
```

### Responsibilities

1. **Request Routing**
   - Route `/api/v1/auth/**` → Auth Service (port 8180)
   - Route `/api/v1/users/**` → User Service (port 8181)
   - Path rewriting for service context paths

2. **Authentication & Authorization**
   - Validate JWT signature and expiration
   - Check token revocation via Auth Service introspection
   - Extract user identity (userId, roles) from JWT claims
   - Differentiate public vs protected endpoints

3. **User Context Propagation**
   - Add `X-User-Id` header with user's UUID
   - Add `X-User-Roles` header with user's roles
   - Downstream services trust these headers

4. **Cross-Cutting Concerns**
   - CORS configuration for frontend origins
   - Future: Rate limiting, circuit breakers, retry logic

---

## Architecture

### Component Structure

```
api-gateway/
├── src/main/java/com/daypulse/api_gateway/
│   ├── ApiGatewayApplication.java          # Main entry point
│   ├── configuration/
│   │   ├── SecurityConfig.java             # Security rules
│   │   ├── JwtDecoderConfig.java           # JWT decoder setup
│   │   └── WebClientConfig.java            # WebClient for service calls
│   ├── security/
│   │   └── GatewayJwtAuthenticationFilter.java  # JWT validation filter
│   ├── client/
│   │   └── AuthServiceClient.java          # Auth service integration
│   └── dto/
│       ├── ApiBaseResponse.java            # Standard response wrapper
│       ├── IntrospectRequest.java          # Token introspection request
│       └── IntrospectResponse.java         # Token introspection response
└── src/main/resources/
    └── application.yaml                     # Configuration
```

### Key Components

#### 1. SecurityConfig.java

Defines public and protected endpoints:

```java
PUBLIC_ENDPOINTS (No authentication):
  - /api/v1/auth/signup
  - /api/v1/auth/register
  - /api/v1/auth/login
  - /api/v1/auth/refresh
  - /api/v1/auth/introspect
  - /api/v1/auth/verify-otp
  - /api/v1/auth/forgot-password

PROTECTED_ENDPOINTS (Require JWT):
  - /api/v1/auth/logout
  - /api/v1/users/**
```

#### 2. GatewayJwtAuthenticationFilter.java

Custom filter for JWT validation:
1. Extracts JWT from `Authorization: Bearer <token>` header
2. Validates JWT signature and expiration locally
3. Calls Auth Service introspection to check revocation
4. Extracts userId and roles from JWT claims
5. Adds `X-User-Id` and `X-User-Roles` headers
6. Sets authentication in security context

#### 3. AuthServiceClient.java

Reactive WebClient for Auth Service communication:
- Calls `/auth-service/auth/introspect` endpoint
- 15-second timeout
- Returns token validity status
- Handles errors gracefully

---

## Configuration

### application.yaml

```yaml
server:
  port: 8188

spring:
  application:
    name: api-gateway
  
  cloud:
    gateway:
      routes:
        # Auth Service Routes
        - id: auth-service
          uri: http://localhost:8180
          predicates:
            - Path=/api/v1/auth/**
          filters:
            - RewritePath=/api/v1/auth/(?<segment>.*), /auth-service/auth/$\{segment}

        # User Service Routes
        - id: user-service
          uri: http://localhost:8181
          predicates:
            - Path=/api/v1/users/**
          filters:
            - RewritePath=/api/v1/users/(?<segment>.*), /user-service/users/$\{segment}

      # CORS Configuration
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOrigins:
              - "http://localhost:3000"    # React dev server
              - "http://localhost:4200"    # Angular dev server
            allowedMethods:
              - GET
              - POST
              - PUT
              - DELETE
              - PATCH
              - OPTIONS
            allowedHeaders: "*"
            allowCredentials: true
            maxAge: 3600

# JWT Configuration
jwt:
  signing-key: fbX2a4nQ4tdMnfExFUl+uA9aD9IFS+csS8GP96pR75RxrCiUcEYvpn+b4wWsgJshvXMUQiDUxhEBxA9RdPj+OQ==

# Service URLs
auth-service:
  url: http://localhost:8180

# Logging
logging:
  level:
    org.springframework.cloud.gateway: DEBUG
    org.springframework.security: DEBUG
    com.daypulse.api_gateway: DEBUG
```

### Environment Variables (Production)

```bash
# Server
SERVER_PORT=8188

# JWT
JWT_SIGNING_KEY=<base64-encoded-secret>

# Service URLs
AUTH_SERVICE_URL=http://auth-service:8180
USER_SERVICE_URL=http://user-service:8181

# CORS
ALLOWED_ORIGINS=https://app.daypulse.com,https://www.daypulse.com
```

### Dependencies (pom.xml)

```xml
<dependencies>
    <!-- Spring Cloud Gateway -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-gateway</artifactId>
    </dependency>

    <!-- Reactive WebFlux -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>

    <!-- Security + OAuth2 Resource Server -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
    </dependency>

    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

---

## Security

### Security Model

```
┌────────────────────────────────────────────────────────────┐
│                  SECURITY BOUNDARY                         │
│                   (API Gateway)                            │
│                                                            │
│  1. Validate JWT signature                                │
│  2. Check token expiration                                │
│  3. Verify token not revoked (introspection)              │
│  4. Extract user identity                                 │
│  5. Add internal headers                                  │
└─────────────────────┬──────────────────────────────────────┘
                      │
              Trusted Headers
              (X-User-Id, X-User-Roles)
                      │
                      ▼
        ┌─────────────────────────────┐
        │   Downstream Services       │
        │   (Trust gateway headers)   │
        └─────────────────────────────┘
```

### Authentication Levels

**Level 1: Public Endpoints**
- No authentication required
- Examples: signup, login, refresh token
- Anyone can access

**Level 2: Protected Endpoints**
- Valid JWT required in Authorization header
- Examples: logout, user profile operations
- Gateway validates token before forwarding

**Level 3: Admin Endpoints** (Future)
- Valid JWT + ADMIN role required
- Gateway checks `X-User-Roles` header
- Examples: admin operations, role management

### JWT Validation Process

The gateway performs multi-stage validation:

```java
1. Local Validation (Fast):
   - JWT signature verification (HS512)
   - Expiration check (exp claim)
   - Malformed token detection

2. Remote Validation (Slower):
   - Token introspection via Auth Service
   - Revocation check (logout, compromised tokens)
   - 15-second timeout with fallback

3. User Context Extraction:
   - userId from JWT claims
   - scope (roles) from JWT claims
   - Store in security context

4. Header Propagation:
   - Add X-User-Id: <uuid>
   - Add X-User-Roles: <space-separated-roles>
   - Forward to downstream service
```

### Internal Headers

Headers added by gateway for downstream services:

| Header | Description | Example |
|--------|-------------|---------|
| `X-User-Id` | User's UUID | `abcd-1234-efgh-5678` |
| `X-User-Roles` | Space-separated roles | `ROLE_USER ROLE_MODERATOR` |

**Security Note**: Downstream services should ONLY accept requests from the gateway. In production:
- Use network isolation (private VPC/subnet)
- Implement gateway signature verification
- Block direct external access to services

---

## Routes

### Route Configuration

#### 1. Auth Service Route

```yaml
Route ID: auth-service
From: /api/v1/auth/**
To: http://localhost:8180/auth-service/auth/**

Examples:
  /api/v1/auth/signup    → http://localhost:8180/auth-service/auth/signup
  /api/v1/auth/login     → http://localhost:8180/auth-service/auth/login
  /api/v1/auth/logout    → http://localhost:8180/auth-service/auth/logout
```

#### 2. User Service Route

```yaml
Route ID: user-service
From: /api/v1/users/**
To: http://localhost:8181/user-service/users/**

Examples:
  /api/v1/users/me              → http://localhost:8181/user-service/users/me
  /api/v1/users/123/follow      → http://localhost:8181/user-service/users/123/follow
  /api/v1/users/123/followers   → http://localhost:8181/user-service/users/123/followers
```

### Path Rewriting

The gateway uses regex-based path rewriting:

```yaml
RewritePath=/api/v1/auth/(?<segment>.*), /auth-service/auth/$\{segment}
```

**How it works**:
1. Capture everything after `/api/v1/auth/` as `segment`
2. Rewrite to `/auth-service/auth/{segment}`
3. Forward to service at configured URI

### Future Routes (Planned)

```yaml
# Feed Service
- id: feed-service
  uri: http://localhost:8182
  predicates:
    - Path=/api/v1/feed/**
  filters:
    - RewritePath=/api/v1/feed/(?<segment>.*), /feed-service/feed/$\{segment}
    - name: CircuitBreaker
      args:
        name: feedServiceCB

# Chat Service
- id: chat-service
  uri: http://localhost:8183
  predicates:
    - Path=/api/v1/chat/**
  filters:
    - RewritePath=/api/v1/chat/(?<segment>.*), /chat-service/chat/$\{segment}
```

---

## JWT Validation Flow

### Complete Flow Diagram

```
┌────────┐                 ┌──────────┐              ┌──────────┐              ┌──────────┐
│ Client │                 │ Gateway  │              │   Auth   │              │   User   │
│        │                 │          │              │ Service  │              │ Service  │
└───┬────┘                 └────┬─────┘              └────┬─────┘              └────┬─────┘
    │                           │                         │                         │
    │ 1. GET /api/v1/users/me  │                         │                         │
    ├──────────────────────────>│                         │                         │
    │ Authorization: Bearer JWT │                         │                         │
    │                           │                         │                         │
    │                           │ 2. Extract JWT          │                         │
    │                           │    from header          │                         │
    │                           │                         │                         │
    │                           │ 3. Decode & validate    │                         │
    │                           │    JWT signature        │                         │
    │                           │    (HS512)              │                         │
    │                           │                         │                         │
    │                           │ 4. POST /auth/introspect│                         │
    │                           ├────────────────────────>│                         │
    │                           │ {token: "..."}          │                         │
    │                           │                         │                         │
    │                           │                         │ 5. Check revocation     │
    │                           │                         │    in database          │
    │                           │                         │                         │
    │                           │ 6. {valid: true}        │                         │
    │                           │<────────────────────────┤                         │
    │                           │                         │                         │
    │                           │ 7. Extract claims:      │                         │
    │                           │    - userId             │                         │
    │                           │    - scope (roles)      │                         │
    │                           │                         │                         │
    │                           │ 8. GET /user-service/users/me                     │
    │                           ├──────────────────────────────────────────────────>│
    │                           │ X-User-Id: abc-123                                │
    │                           │ X-User-Roles: ROLE_USER                           │
    │                           │                         │                         │
    │                           │                         │                         │ 9. Process request
    │                           │                         │                         │    using X-User-Id
    │                           │                         │                         │
    │                           │ 10. {user profile data}                           │
    │                           │<──────────────────────────────────────────────────┤
    │                           │                         │                         │
    │ 11. {user profile}        │                         │                         │
    │<──────────────────────────┤                         │                         │
    │                           │                         │                         │
```

### Step-by-Step Explanation

**Step 1: Client Request**
```http
GET /api/v1/users/me HTTP/1.1
Host: localhost:8188
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

**Step 2: Gateway Extracts Token**
```java
String token = extractToken(request);
// Expects "Bearer <token>" format
```

**Step 3: Local JWT Validation**
```java
jwtDecoder.decode(token)
  - Validates signature using shared secret key
  - Checks expiration (exp claim)
  - Parses JWT claims
```

**Step 4: Token Introspection**
```http
POST /auth-service/auth/introspect HTTP/1.1
Content-Type: application/json

{
  "token": "eyJhbGciOiJIUzUxMiJ9..."
}
```

**Step 5: Auth Service Validates**
```java
- Checks if token exists in revoked_tokens table
- Returns {valid: true/false}
- Timeout: 15 seconds
```

**Step 6: Extract User Identity**
```java
String userId = jwt.getClaimAsString("userId");
String scope = jwt.getClaimAsString("scope");
// scope = "ROLE_USER ROLE_MODERATOR"
```

**Step 7: Add Internal Headers**
```http
X-User-Id: abcd-1234-efgh-5678
X-User-Roles: ROLE_USER ROLE_MODERATOR
```

**Step 8: Forward to Service**
```http
GET /user-service/users/me HTTP/1.1
Host: localhost:8181
X-User-Id: abcd-1234-efgh-5678
X-User-Roles: ROLE_USER ROLE_MODERATOR
```

**Step 9-11: Service Processes & Returns**

---

## Integration

### Service-to-Service Communication

#### Gateway → Auth Service

**Purpose**: Token introspection

```java
POST http://localhost:8180/auth-service/auth/introspect
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

**Configuration**:
- Timeout: 15 seconds
- Fallback: Assume invalid on error
- Retry: None (future enhancement)

#### Gateway → User Service

**Purpose**: Forward authenticated requests

```http
GET /user-service/users/me
X-User-Id: abcd-1234-efgh-5678
X-User-Roles: ROLE_USER
```

**No authentication** - User Service trusts gateway headers.

### CORS Configuration

Allows frontend applications to make cross-origin requests:

```yaml
Allowed Origins:
  - http://localhost:3000 (React dev)
  - http://localhost:4200 (Angular dev)

Allowed Methods:
  - GET, POST, PUT, DELETE, PATCH, OPTIONS

Allowed Headers:
  - * (all headers)

Credentials:
  - true (cookies, auth headers)

Max Age:
  - 3600 seconds (1 hour cache)
```

**Production**: Update allowed origins to production domains:
```yaml
allowedOrigins:
  - https://app.daypulse.com
  - https://www.daypulse.com
```

---

## Development

### Running Locally

```bash
# Navigate to gateway directory
cd backEnd/api-gateway

# Run with Maven
mvn spring-boot:run

# Or build and run JAR
mvn clean package -DskipTests
java -jar target/api-gateway-0.0.1-SNAPSHOT.jar
```

### Health Check

```bash
curl http://localhost:8188/actuator/health
```

Expected response:
```json
{
  "status": "UP"
}
```

### Testing Routes

**Test Public Endpoint** (no auth):
```bash
curl -X POST http://localhost:8188/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "password": "password123"}'
```

**Test Protected Endpoint** (with JWT):
```bash
curl -X GET http://localhost:8188/api/v1/users/me \
  -H "Authorization: Bearer <your-jwt-token>"
```

### Debug Logging

Enable debug logging in `application.yaml`:

```yaml
logging:
  level:
    org.springframework.cloud.gateway: DEBUG
    org.springframework.security: DEBUG
    com.daypulse.api_gateway: DEBUG
```

View logs:
```bash
# Real-time logs
mvn spring-boot:run

# Check JWT validation
[DEBUG] GatewayJwtAuthenticationFilter - Authenticated user: user@example.com (userId: abc-123, roles: ROLE_USER)

# Check route matching
[DEBUG] RoutePredicateHandlerMapping - Route matched: auth-service
```

### Common Development Tasks

**Add New Route**:
```yaml
# In application.yaml
spring:
  cloud:
    gateway:
      routes:
        - id: new-service
          uri: http://localhost:8185
          predicates:
            - Path=/api/v1/new/**
          filters:
            - RewritePath=/api/v1/new/(?<segment>.*), /new-service/new/$\{segment}
```

**Update JWT Secret**:
```yaml
jwt:
  signing-key: <new-base64-encoded-secret>
```

**Add CORS Origin**:
```yaml
globalcors:
  cors-configurations:
    '[/**]':
      allowedOrigins:
        - "http://localhost:3000"
        - "http://localhost:5173"  # Add Vite dev server
```

---

## Troubleshooting

### Common Issues

#### 1. 401 Unauthorized on Protected Endpoints

**Symptom**: All protected endpoints return 401

**Possible Causes**:
- JWT token expired
- Invalid JWT signature
- Token revoked
- Wrong signing key

**Solutions**:
```bash
# Check token expiration
# Decode JWT at https://jwt.io
# Verify "exp" claim

# Verify signing key matches auth-service
# gateway/application.yaml jwt.signing-key
# auth-service/application.yaml jwt.signing-key

# Get new token
curl -X POST http://localhost:8188/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "password": "password123"}'
```

#### 2. 503 Service Unavailable

**Symptom**: Gateway returns 503 for all routes

**Possible Causes**:
- Downstream service not running
- Wrong service URL in configuration

**Solutions**:
```bash
# Check if services are running
curl http://localhost:8180/auth-service/actuator/health
curl http://localhost:8181/user-service/actuator/health

# Verify service URLs in application.yaml
auth-service:
  url: http://localhost:8180
```

#### 3. CORS Errors

**Symptom**: Browser console shows CORS errors

**Possible Causes**:
- Frontend origin not in allowedOrigins
- Missing credentials: true

**Solutions**:
```yaml
# Add frontend origin
globalcors:
  cors-configurations:
    '[/**]':
      allowedOrigins:
        - "http://localhost:3000"  # Add your frontend URL
```

#### 4. Token Introspection Timeout

**Symptom**: Slow authentication, 15-second delays

**Possible Causes**:
- Auth Service slow/overloaded
- Network issues

**Solutions**:
```bash
# Check Auth Service response time
time curl -X POST http://localhost:8180/auth-service/auth/introspect \
  -H "Content-Type: application/json" \
  -d '{"token": "your-token"}'

# Reduce timeout in AuthServiceClient.java
.timeout(Duration.ofSeconds(5))

# Future: Implement Redis token blacklist for faster checks
```

#### 5. Routes Not Matching

**Symptom**: 404 Not Found for valid endpoints

**Possible Causes**:
- Path predicate mismatch
- Incorrect path rewriting

**Debug**:
```bash
# Enable gateway debug logging
logging:
  level:
    org.springframework.cloud.gateway: TRACE

# Check logs for route matching
[TRACE] RoutePredicateHandlerMapping - Route matched: user-service
[TRACE] RoutePredicateHandlerMapping - Mapping to /user-service/users/me
```

### Performance Monitoring

**Check Gateway Performance**:
```bash
# Request latency
curl -w "@curl-format.txt" -o /dev/null -s http://localhost:8188/api/v1/users/me

# curl-format.txt:
time_namelookup:  %{time_namelookup}
time_connect:  %{time_connect}
time_starttransfer:  %{time_starttransfer}
time_total:  %{time_total}
```

**Bottlenecks to Monitor**:
1. JWT decoding: ~10ms
2. Token introspection: ~50-200ms (depends on Auth Service)
3. Route matching: ~1ms
4. Downstream service call: varies by service

---

## Future Enhancements

### Planned Features

**1. Rate Limiting (Redis)**:
```yaml
- name: RequestRateLimiter
  args:
    redis-rate-limiter.replenishRate: 10
    redis-rate-limiter.burstCapacity: 20
```

**2. Circuit Breaker (Resilience4j)**:
```yaml
- name: CircuitBreaker
  args:
    name: userServiceCB
    fallbackUri: forward:/fallback/users
```

**3. Retry Logic**:
```yaml
- name: Retry
  args:
    retries: 3
    statuses: BAD_GATEWAY
```

**4. Redis Token Blacklist**:
- Faster revocation checks
- Remove dependency on Auth Service introspection
- Sub-millisecond lookup times

**5. Service Discovery**:
- Integrate with Consul/Eureka
- Dynamic service URL resolution
- Automatic failover

**6. Distributed Tracing**:
- OpenTelemetry integration
- Trace ID propagation
- Jaeger/Zipkin visualization

---

**Last Updated**: 2026-01-22  
**Version**: 0.1.0  
**Status**: Production Ready
