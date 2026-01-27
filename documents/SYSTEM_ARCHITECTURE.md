# DayPulse System Architecture

> High-level overview of the DayPulse platform architecture

**Quick Links:** [Backend Details](back/README.md) | [Frontend Details](front/ARCHITECTURE.md) | [Quick Start](QUICK_START.md) | [Glossary](GLOSSARY.md)

---

## What is DayPulse?

DayPulse is a modern social networking platform designed to help users:

- 📝 Share daily moments through short posts ("pulses")
- 👥 Connect with friends and follow their activities
- 🔥 Maintain healthy habits through daily streaks
- 💬 Communicate via real-time chat
- 📊 Discover trending content and users

**Technology:** Microservices architecture with React frontend and Spring Boot backend

---

## System Overview

### Architecture at a Glance

```mermaid
graph TB
    subgraph "Client Layer"
        Web[Web App<br/>React + TypeScript<br/>Port: 5173]
    end

    subgraph "API Layer"
        Gateway[API Gateway<br/>Spring Cloud Gateway<br/>Port: 8188]
    end

    subgraph "Service Layer"
        Auth[Auth Service<br/>Port: 8180<br/>✅ Implemented]
        User[User Service<br/>Port: 8181<br/>✅ Implemented]
        Feed[Feed Service<br/>Port: 8182<br/>🔄 Planned]
        Chat[Chat Service<br/>Port: 8183<br/>🔄 Planned]
        Notif[Notification Service<br/>Port: 8184<br/>🔄 Planned]
        Util[Utility Service<br/>Port: 8185<br/>🔄 Planned]
    end

    subgraph "Data Layer"
        PG[(PostgreSQL<br/>Auth + User Data)]
        Mongo[(MongoDB<br/>Feed + Chat<br/>🔄 Planned)]
        Redis[(Redis<br/>Cache<br/>🔄 Planned)]
    end

    Web -->|HTTPS| Gateway
    Gateway --> Auth
    Gateway --> User
    Gateway -.-> Feed
    Gateway -.-> Chat
    Gateway -.-> Notif
    Gateway -.-> Util

    Auth --> PG
    User --> PG
    Feed -.-> Mongo
    Chat -.-> Mongo
    Auth -.-> Redis
    User -.-> Redis

    style Feed fill:#f9f,stroke:#333,stroke-dasharray: 5 5
    style Chat fill:#f9f,stroke:#333,stroke-dasharray: 5 5
    style Notif fill:#f9f,stroke:#333,stroke-dasharray: 5 5
    style Util fill:#f9f,stroke:#333,stroke-dasharray: 5 5
    style Mongo fill:#f9f,stroke:#333,stroke-dasharray: 5 5
    style Redis fill:#f9f,stroke:#333,stroke-dasharray: 5 5
```

**Legend:**

- ✅ **Solid lines** = Implemented
- 🔄 **Dashed lines** = Planned

---

## Core Components

### 1. Web Application (Frontend)

**Technology:** React 18 + TypeScript + Vite  
**Port:** 5173

**Features:**

- User authentication (login/register)
- Profile management
- Social feed
- Real-time chat
- Notifications

**📖 Details:** [Frontend Architecture](front/ARCHITECTURE.md)

---

### 2. API Gateway

**Technology:** Spring Cloud Gateway (Reactive)  
**Port:** 8188  
**Status:** ✅ Implemented

**Responsibilities:**

- Route requests to microservices
- JWT token validation
- User identity extraction
- CORS handling
- Rate limiting (future)

**Request Flow:**

```
Client → API Gateway → Service
        JWT Validation
        Add Headers:
         - X-User-Id
         - X-User-Roles
```

**📖 Details:** [API Gateway Documentation](back/services/API_GATEWAY.md)

---

### 3. Auth Service

**Technology:** Spring Boot + Spring Security  
**Port:** 8180  
**Database:** PostgreSQL (`auth-service`)  
**Status:** ✅ Implemented

**Features:**

- User registration & authentication
- JWT token generation (access + refresh)
- Token validation & revocation
- Role-Based Access Control (RBAC)
- Password reset with OTP

**Key Endpoints:**

- `POST /auth/signup` - Create account
- `POST /auth/login` - Login
- `POST /auth/refresh` - Refresh tokens
- `POST /auth/logout` - Logout

**Token Expiration:**

- Access Token: 1 hour
- Refresh Token: 10 hours

**📖 Details:** [Auth Service Documentation](back/services/AUTH_SERVICE.md)

---

### 4. User Service

**Technology:** Spring Boot + JPA  
**Port:** 8181  
**Database:** PostgreSQL (`user-service`)  
**Status:** ✅ Implemented

**Features:**

- User profile management
- Follow/unfollow system
- User discovery & suggestions
- Social graph management
- User statistics (followers, following, streak)

**Key Endpoints:**

- `GET /users/me` - Get my profile
- `PATCH /users/me` - Update profile
- `POST /users/{id}/follow` - Follow user
- `GET /users/suggested` - Get suggested users

**📖 Details:** [User Service Documentation](back/services/USER_SERVICE.md)

---

### 5. Feed Service (Planned)

**Technology:** Spring Boot + MongoDB  
**Port:** 8182  
**Database:** MongoDB (`feed_db`)  
**Status:** 🔄 Planned

**Features:**

- Create/read/update/delete pulses (posts)
- Like/unlike pulses
- Comment on pulses
- Personalized feed generation
- Trending tags

**Design:** See [Feed Service Design](SYSTEM_ARCHITECTURE.md#feed-service-port-8182) in full docs

---

### 6. Chat Service (Planned)

**Technology:** Spring Boot + MongoDB + WebSocket  
**Port:** 8183  
**Database:** MongoDB (`chat_db`)  
**Status:** 🔄 Planned

**Features:**

- Real-time 1:1 messaging
- Chat rooms
- Message delivery & read receipts
- Typing indicators
- Scheduled reminders

**Design:** See [Chat Service Design](SYSTEM_ARCHITECTURE.md#chat-service-port-8183) in full docs

---

### 7. Notification Service (Planned)

**Technology:** Spring Boot + MongoDB + Web Push API  
**Port:** 8184  
**Database:** MongoDB (`notification_db`)  
**Status:** 🔄 Planned

**Features:**

- Browser push notifications
- In-app notifications
- Notification preferences
- Email notifications

**Design:** See [Notification Service Design](SYSTEM_ARCHITECTURE.md#notification-service-port-8184) in full docs

---

### 8. Utility Service (Planned)

**Technology:** Spring Boot + PostgreSQL + Redis + MinIO  
**Port:** 8185  
**Databases:** PostgreSQL + Redis + MinIO  
**Status:** 🔄 Planned

**Features:**

- Full-text search (users, tags, pulses)
- Media upload & storage
- Image processing
- Scheduled tasks & reminders
- Background jobs

**Design:** See [Utility Service Design](SYSTEM_ARCHITECTURE.md#utility-service-port-8185) in full docs

---

## Technology Stack

### Backend

| Category    | Technology            | Version  |
| ----------- | --------------------- | -------- |
| Language    | Java                  | 21       |
| Framework   | Spring Boot           | 3.5.10   |
| API Gateway | Spring Cloud Gateway  | 2025.0.1 |
| Security    | Spring Security + JWT | Latest   |
| Database    | PostgreSQL            | 15+      |
| ORM         | Spring Data JPA       | Latest   |
| Build Tool  | Maven                 | 3.8+     |

### Frontend

| Category         | Technology                   | Version |
| ---------------- | ---------------------------- | ------- |
| Language         | TypeScript                   | Latest  |
| Framework        | React                        | 18      |
| Build Tool       | Vite                         | Latest  |
| State Management | Zustand                      | Latest  |
| Data Fetching    | React Query (TanStack Query) | Latest  |
| Routing          | React Router                 | Latest  |

### Infrastructure (Planned)

| Category         | Technology            | Purpose                      |
| ---------------- | --------------------- | ---------------------------- |
| Database (NoSQL) | MongoDB               | Feed, chat, notifications    |
| Cache            | Redis                 | Session storage, API caching |
| Message Broker   | Apache Kafka          | Event streaming              |
| Object Storage   | MinIO (S3-compatible) | Media files                  |
| Authentication   | Keycloak (optional)   | OAuth/OIDC provider          |

---

## Data Architecture

### Databases

**PostgreSQL Databases:**

1. **auth-service**
   - `users_auth` - User credentials
   - `refresh_tokens` - JWT refresh tokens
   - `otp_codes` - One-time passwords

2. **user-service**
   - `user_profiles` - User information
   - `user_stats` - Follower/following counts
   - `follows` - Follow relationships

**MongoDB Collections (Planned):** 3. **feed_db**

- `pulses` - User posts
- `comments` - Pulse comments
- `reactions` - Emoji reactions

4. **chat_db**
   - `conversations` - Chat metadata
   - `messages` - Chat messages
   - `reminders` - Scheduled reminders

5. **notification_db**
   - `notifications` - User notifications
   - `devices` - Push notification subscriptions

**📖 Full Schemas:** See [Database Schema](SYSTEM_ARCHITECTURE.md#data-architecture) in full docs

---

## Security Architecture

### Authentication Flow

```mermaid
sequenceDiagram
    participant Client
    participant Gateway as API Gateway
    participant Auth as Auth Service
    participant User as User Service
    participant DB as Database

    Client->>Gateway: POST /api/v1/auth/login<br/>{email, password}
    Gateway->>Auth: POST /auth/login
    Auth->>DB: Verify credentials
    DB-->>Auth: User data
    Auth->>Auth: Generate JWT tokens
    Auth-->>Gateway: {user, tokens}
    Gateway-->>Client: {user, tokens}<br/>accessToken (1h)<br/>refreshToken (10h)

    Note over Client: Store tokens

    Client->>Gateway: GET /api/v1/users/me<br/>Authorization: Bearer <token>
    Gateway->>Gateway: Validate JWT<br/>Extract userId
    Gateway->>Auth: POST /auth/introspect<br/>{token}
    Auth-->>Gateway: {valid: true}
    Gateway->>User: GET /users/me<br/>X-User-Id: abc-123
    User->>DB: Fetch profile
    DB-->>User: User data
    User-->>Gateway: Profile response
    Gateway-->>Client: Profile data
```

### Security Features

**Implemented:**

- ✅ JWT-based authentication (HS512)
- ✅ Refresh token rotation
- ✅ Token revocation
- ✅ Password hashing (BCrypt)
- ✅ RBAC (Role-Based Access Control)
- ✅ CORS configuration

**Planned:**

- 🔄 Rate limiting
- 🔄 OAuth 2.0 (via Keycloak)
- 🔄 API key authentication for service-to-service
- 🔄 Audit logging

**Roles:**

- `USER` - Standard user (default)
- `MODERATOR` - Content moderation
- `ADMIN` - Full system access

**📖 Details:** [Security Documentation](back/SECURITY.md)

---

## Communication Patterns

### Synchronous Communication (HTTP/REST)

**Current:** All service-to-service communication is synchronous

- Client → API Gateway → Services
- Service → Service (internal endpoints)

**Example:**

```
Gateway → Auth Service: POST /auth/introspect
Gateway → User Service: GET /internal/users/{id}/summary
```

### Asynchronous Communication (Planned)

**Future:** Kafka for event-driven updates

**Event Examples:**

- `user.registered` - New user created
- `pulse.created` - New post published
- `user.followed` - User followed another user
- `message.sent` - New chat message

**Benefits:**

- Loose coupling
- Better scalability
- Event sourcing capabilities

---

## API Design Standards

### RESTful Principles

- Resource-based URLs
- HTTP methods (GET, POST, PATCH, DELETE)
- Stateless requests
- JSON payloads

### Endpoint Patterns

```
GET    /resource         - List resources
GET    /resource/{id}    - Get single resource
POST   /resource         - Create resource
PATCH  /resource/{id}    - Update resource
DELETE /resource/{id}    - Delete resource
```

### Response Format

**Success:**

```json
{
  "code": 200,
  "message": "Success",
  "result": { ... }
}
```

**Error:**

```json
{
  "code": 1006,
  "message": "Unauthenticated"
}
```

### Authentication

All protected endpoints require JWT:

```http
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

**📖 Full API Reference:** [API Documentation](back/API_REFERENCE.md)

---

## Deployment Strategy

### Local Development

```bash
# Start PostgreSQL
psql -U postgres

# Start services (3 terminals)
cd backEnd/auth-service && mvn spring-boot:run
cd backEnd/user-service && mvn spring-boot:run
cd backEnd/api-gateway && mvn spring-boot:run

# Start frontend
cd frontEnd && npm run dev
```

### Docker Compose (Recommended)

```bash
docker-compose up -d
```

**Services:**

- Keycloak (port 8888)
- PostgreSQL (port 5432)
- Future: All microservices

### Production (Planned)

- **Container Orchestration:** Kubernetes
- **Database:** Managed PostgreSQL (AWS RDS, Google Cloud SQL)
- **Caching:** Redis cluster
- **Storage:** S3 or compatible (MinIO)
- **Monitoring:** Prometheus + Grafana

**📖 Deployment Guide:** [Deployment Documentation](back/DEPLOYMENT_GUIDE.md)

---

## Scalability & Performance

### Current Performance

| Operation   | Response Time | Notes                     |
| ----------- | ------------- | ------------------------- |
| Register    | 200-300ms     | Password hashing overhead |
| Login       | 200-300ms     | JWT generation            |
| Get Profile | 50-100ms      | Simple database query     |
| Follow User | 100-150ms     | Transactional update      |

### Scalability Strategies

**Horizontal Scaling:**

- Stateless services can run multiple instances
- API Gateway load balances requests
- Database read replicas for heavy read operations

**Caching** (Planned):

- Redis for:
  - User sessions
  - Frequent queries (user profiles)
  - API response caching
  - Rate limiting counters

**Database Optimization:**

- Indexes on frequently queried columns
- Connection pooling
- Query optimization

**CDN** (Planned):

- Static asset delivery
- Media file caching
- Edge locations for global access

---

## Monitoring & Observability

### Health Checks

All services expose actuator endpoints:

```bash
curl http://localhost:8188/actuator/health  # API Gateway
curl http://localhost:8180/actuator/health  # Auth Service
curl http://localhost:8181/actuator/health  # User Service
```

### Logging

- Structured logging (JSON format)
- Log levels: ERROR, WARN, INFO, DEBUG
- Request/response logging at gateway

### Metrics (Planned)

- **Prometheus:** Metrics collection
- **Grafana:** Visualization dashboards
- **Metrics:**
  - Request count & latency
  - Error rates
  - Database connection pool
  - JWT validation time

### Tracing (Planned)

- **Distributed Tracing:** Spring Cloud Sleuth + Zipkin
- Trace requests across services
- Identify performance bottlenecks

---

## Design Principles

### 1. Microservices Architecture

**Benefits:**

- Independent deployment
- Technology diversity
- Fault isolation
- Team autonomy

**Trade-offs:**

- Increased complexity
- Network latency
- Data consistency challenges

### 2. Domain-Driven Design

Services organized around business domains:

- **Auth Domain:** Authentication & authorization
- **User Domain:** Profiles & social graph
- **Feed Domain:** Content sharing
- **Chat Domain:** Real-time messaging

### 3. API-First Design

- Define APIs before implementation
- Clear contracts between services
- Documentation as code
- Client-server independence

### 4. Security by Design

- Defense in depth
- Zero-trust architecture
- Least privilege access
- Secure by default

### 5. Cloud-Native

- Containerized services
- Stateless where possible
- Configuration externalization
- Resilience patterns

---

## Development Workflow

### Getting Started

1. **Prerequisites:** Java 21, Maven, PostgreSQL, Node.js
2. **Setup:** See [Quick Start Guide](QUICK_START.md)
3. **Documentation:** Read [Development Guide](back/DEVELOPMENT_GUIDE.md)

### Code Organization

```
DayPulse/
├── backEnd/
│   ├── api-gateway/      # Spring Cloud Gateway
│   ├── auth-service/     # Authentication service
│   └── user-service/     # User management service
├── frontEnd/             # React application
└── documents/            # Documentation (you are here)
```

### Development Tools

- **IDE:** IntelliJ IDEA / VS Code
- **API Testing:** Postman / cURL
- **Database:** pgAdmin / psql
- **Version Control:** Git

### Testing

```bash
# Backend unit tests
cd backEnd/auth-service && mvn test

# API integration tests
./back/API_TEST.sh
```

---

## Next Steps

### For New Developers

1. ✅ Read this architecture overview
2. 📖 Follow [Quick Start Guide](QUICK_START.md)
3. 📚 Review [Glossary](GLOSSARY.md) for terminology
4. 🔍 Explore detailed docs:
   - [Backend Documentation](back/README.md)
   - [Frontend Documentation](front/ARCHITECTURE.md)
   - [API Reference](back/API_REFERENCE.md)

### For Backend Developers

- [Auth Service Details](back/services/AUTH_SERVICE.md)
- [User Service Details](back/services/USER_SERVICE.md)
- [Development Guide](back/DEVELOPMENT_GUIDE.md)
- [Security Model](back/SECURITY.md)

### For Frontend Developers

- [Frontend Architecture](front/ARCHITECTURE.md)
- [Feature Flows](front/FEATURE_FLOWS.md)
- [Getting Started](front/GETTING_STARTED.md)

### For DevOps

- [Deployment Guide](back/DEPLOYMENT_GUIDE.md)
- [Docker Compose Guide](docker-compose.md)
- [Keycloak Setup](keycloak-setup.md)

---

## Detailed Documentation

This is a **simplified overview**. For comprehensive details, see:

- **[Full Architecture Documentation](SYSTEM_ARCHITECTURE_DETAILED.md)** - 2699 lines of detailed specs
- **[Service Implementations](back/services/README.md)** - Service-specific deep dives
- **[Database Schemas](SYSTEM_ARCHITECTURE_DETAILED.md#data-architecture)** - Complete schema definitions
- **[Communication Patterns](SYSTEM_ARCHITECTURE_DETAILED.md#communication-patterns)** - Detailed flow diagrams

---

## Support & Resources

- **Quick Reference:** [Glossary](GLOSSARY.md)
- **Getting Started:** [Quick Start](QUICK_START.md)
- **API Testing:** [API Testing Guide](API_TESTING.md)
- **Troubleshooting:** [Development Guide](back/DEVELOPMENT_GUIDE.md#troubleshooting)
- **Contributing:** [Contributing Guide](CONTRIBUTING.md)

---

**Documentation Version:** 2.0 (Simplified)  
**Last Updated:** 2026-01-27  
**Full Version:** Available as [SYSTEM_ARCHITECTURE_DETAILED.md](SYSTEM_ARCHITECTURE_DETAILED.md)
