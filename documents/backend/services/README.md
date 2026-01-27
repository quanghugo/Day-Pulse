# DayPulse Backend Services

Complete documentation for all microservices in the DayPulse backend system.

---

## Services Overview

### Implemented Services

| Service | Port | Context Path | Status | Documentation |
|---------|------|--------------|--------|---------------|
| **API Gateway** | 8188 | `/` | ✅ Implemented | [API_GATEWAY.md](API_GATEWAY.md) |
| **Auth Service** | 8180 | `/auth-service` | ✅ Implemented | [AUTH_SERVICE.md](AUTH_SERVICE.md) |
| **User Service** | 8181 | `/user-service` | ✅ Implemented | [USER_SERVICE.md](USER_SERVICE.md) |

### Planned Services

| Service | Port | Technology | Status | Purpose |
|---------|------|------------|--------|---------|
| **Feed Service** | 8182 | Spring Boot + MongoDB | 📋 Planned | Status/pulse management, likes, comments, trending |
| **Chat Service** | 8183 | Spring Boot + MongoDB + WebSocket | 📋 Planned | Real-time messaging, reminders |
| **Notification Service** | 8184 | Spring Boot + MongoDB | 📋 Planned | Push and in-app notifications |
| **Search Service** | 8185 | Spring Boot + PostgreSQL + Redis | 📋 Planned | User and tag search, suggestions |

---

## Service Responsibilities

### API Gateway (Port 8188)
**Single Entry Point for All Client Requests**

- Request routing to downstream services
- JWT token validation and user identity extraction
- CORS handling
- Rate limiting (future)
- Circuit breaker patterns (future)

**Technology Stack**:
- Spring Cloud Gateway (Reactive)
- Spring Security OAuth2 Resource Server
- WebFlux

**Routes**:
- `/api/v1/auth/**` → Auth Service
- `/api/v1/users/**` → User Service

---

### Auth Service (Port 8180)
**Authentication & Authorization**

- User registration and authentication
- JWT token generation (access + refresh)
- Token validation and revocation
- User credential management
- Role-Based Access Control (RBAC)
- Admin user management

**Technology Stack**:
- Spring Boot
- Spring Security
- PostgreSQL
- JWT (HS512)
- BCrypt password hashing

**Database**: `auth-service` (PostgreSQL)

---

### User Service (Port 8181)
**User Profiles & Social Graph**

- User profile management (CRUD)
- Follow/unfollow functionality
- User statistics tracking
- User discovery (suggested users)
- Service-to-service internal APIs

**Technology Stack**:
- Spring Boot
- Spring Data JPA
- PostgreSQL
- MapStruct for DTO mapping

**Database**: `user-service` (PostgreSQL)

---

## Architecture Patterns

### Microservices Communication

```
┌─────────────────────────────────────────────────────────────┐
│                         CLIENT                               │
│                    (Web/Mobile/Desktop)                      │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           │ HTTP/HTTPS + JWT
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                      API GATEWAY (8188)                      │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ • JWT Validation                                       │ │
│  │ • User Identity Extraction (userId, roles)            │ │
│  │ • Request Routing                                      │ │
│  │ • CORS Handling                                        │ │
│  └────────────────────────────────────────────────────────┘ │
└───────────┬─────────────────────┬───────────────────────────┘
            │                     │
            │ Internal Network    │
            │ (X-User-Id header)  │
            ▼                     ▼
┌───────────────────┐   ┌───────────────────┐
│  AUTH SERVICE     │   │  USER SERVICE     │
│  (8180)           │   │  (8181)           │
│                   │   │                   │
│  PostgreSQL       │   │  PostgreSQL       │
│  auth-service     │   │  user-service     │
└───────────────────┘   └───────────────────┘
```

### Trust Boundary Model

**API Gateway = Security Boundary**
- All external requests authenticated at gateway
- Downstream services trust `X-User-Id` header
- Services operate in private network
- No JWT validation in downstream services

### Database Per Service

Each service owns its database:
- **Auth Service**: `auth-service` database (user accounts, tokens, roles)
- **User Service**: `user-service` database (profiles, follows, stats)

**No cross-database queries** - services communicate via APIs.

---

## Common Patterns

### 1. Authentication Flow

```
Client → Gateway (JWT validation) → Service (X-User-Id header)
```

All services receive:
- `X-User-Id`: UUID of authenticated user
- `X-User-Roles`: Comma-separated roles (e.g., "ROLE_USER")

### 2. Error Handling

Standard error response format across all services:

```json
{
  "code": 1006,
  "message": "Unauthenticated"
}
```

### 3. API Response Format

Standard success response format:

```json
{
  "code": 1000,
  "result": {
    // response data
  }
}
```

### 4. Pagination

Services use Spring Data pagination:
- Query params: `?page=0&size=20`
- Default page size: 20
- Returns: `content`, `page` metadata

---

## Service Discovery

### Current (Static Configuration)

Services use hardcoded URLs in configuration:
```yaml
auth-service:
  url: http://localhost:8180
user-service:
  url: http://localhost:8181
```

### Future (Dynamic Discovery)

Planned integration with service discovery:
- **Consul** or **Eureka** for service registry
- **Kubernetes Service Discovery** for production
- Health checks and automatic failover

---

## Monitoring & Observability

### Current

Basic Spring Boot Actuator endpoints:
- `/actuator/health` - Health check
- `/actuator/info` - Service info

### Future (Planned)

**Distributed Tracing**:
- OpenTelemetry
- Jaeger or Zipkin
- Trace ID propagation across services

**Metrics**:
- Prometheus exporters
- Grafana dashboards
- Service-level metrics (latency, throughput, errors)

**Logging**:
- Structured JSON logging
- ELK Stack (Elasticsearch, Logstash, Kibana)
- Correlation IDs for request tracking

---

## Development Workflow

### Running All Services

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

### Health Check

```bash
curl http://localhost:8188/actuator/health  # Gateway
curl http://localhost:8180/auth-service/actuator/health  # Auth
curl http://localhost:8181/user-service/actuator/health  # User
```

### Testing Flow

1. **Register User**: `POST /api/v1/auth/signup`
2. **Login**: `POST /api/v1/auth/login`
3. **Setup Profile**: `POST /api/v1/users/me/setup`
4. **Use Services**: Access protected endpoints with JWT

---

## Technology Stack Summary

| Layer | Technology |
|-------|------------|
| **API Gateway** | Spring Cloud Gateway (Reactive) |
| **Services** | Spring Boot 3.5.10 |
| **Security** | Spring Security + OAuth2 |
| **Database** | PostgreSQL 15+ |
| **Migrations** | Flyway |
| **ORM** | Spring Data JPA / Hibernate |
| **Mapping** | MapStruct 1.5.5 |
| **Build** | Maven |
| **JDK** | Java 21 |

### Future Integrations

| Component | Technology | Purpose |
|-----------|------------|---------|
| **Caching** | Redis | Performance optimization |
| **Messaging** | Kafka | Event-driven architecture |
| **NoSQL** | MongoDB | Flexible schema data (feed, chat) |
| **Search** | Elasticsearch (optional) | Full-text search |
| **Tracing** | OpenTelemetry | Distributed tracing |
| **Metrics** | Prometheus + Grafana | Monitoring dashboards |

---

## Quick Links

### Main Documentation
- [Back to Main README](../README.md)
- [Architecture Overview](../ARCHITECTURE.md)
- [API Reference](../API_REFERENCE.md)
- [Development Guide](../DEVELOPMENT_GUIDE.md)
- [Changelog](../CHANGELOG.md)

### Service Documentation
- [API Gateway Documentation](API_GATEWAY.md)
- [Auth Service Documentation](AUTH_SERVICE.md)
- [User Service Documentation](USER_SERVICE.md)

---

**Last Updated**: 2026-01-22  
**Version**: 0.1.0
