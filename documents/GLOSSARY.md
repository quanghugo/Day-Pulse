# Glossary

Essential terminology for understanding DayPulse.

---

## DayPulse-Specific Terms

### Pulse

A short status update or post (max 280 characters) shared by users on the platform. Similar to a tweet or status update.

**Example:** "Just finished my morning workout! 💪 #HealthyLiving"

**Related:**

- See [Feed Service](SYSTEM_ARCHITECTURE.md#feed-service-port-8182) for implementation
- Pulses can include mood, tags, and visibility settings

---

### Streak

The number of consecutive days a user has posted a pulse. Encourages daily engagement.

**Example:** "15-day streak" means the user has posted every day for 15 days.

**Features:**

- Resets to 0 if user misses a day
- Displayed on user profiles
- Stored in `user_profiles.streak` column

**Related:**

- See [User Service](back/services/USER_SERVICE.md) for streak tracking
- Scheduler Service handles daily streak calculations

---

### Mood

An emotional state associated with a pulse. Users can optionally tag their mood when posting.

**Available Moods:**

- 😊 Happy
- 😢 Sad
- 😴 Tired
- 🎉 Excited
- 😰 Anxious
- 😌 Calm

**Related:** Stored in `pulses.mood` field

---

### Setup Profile

The one-time profile completion process after initial registration.

**Process:**

1. User registers (creates account)
2. `isSetupComplete = false`
3. User completes profile (username, display name, bio, etc.)
4. `isSetupComplete = true`
5. User redirected to main app

**Related:** See [Auth Flow](back/README.md#authentication--authorization)

---

## Authentication & Security

### JWT (JSON Web Token)

A compact, URL-safe token format used for authentication. Contains encoded user information and signature.

**Structure:**

```
eyJhbGci... (header)
.
eyJzdWIi... (payload)
.
3fXxKj2p... (signature)
```

**DayPulse Implementation:**

- **Algorithm:** HS512 (HMAC with SHA-512)
- **Access Token Expiry:** 1 hour
- **Refresh Token Expiry:** 10 hours

**Claims Used:**

- `sub` - User email
- `userId` - User UUID
- `scope` - User roles (e.g., "ROLE_USER")
- `exp` - Expiration timestamp
- `iss` - Issuer ("daypulse-auth")
- `jti` - Unique token ID

**Related:**

- [Auth Service](back/services/AUTH_SERVICE.md)
- [Security](back/SECURITY.md)

---

### Access Token

Short-lived JWT used to access protected API endpoints.

**Lifespan:** 1 hour (3600 seconds)

**Usage:**

```http
GET /api/v1/users/me
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

**Security Note:** Never stored in database, only validated

---

### Refresh Token

Long-lived token used to obtain new access tokens without re-login.

**Lifespan:** 10 hours (36000 seconds)

**Storage:** Hashed (MD5) in `refresh_tokens` table

**Flow:**

1. Access token expires
2. Client sends refresh token to `/api/v1/auth/refresh`
3. Server validates and issues new access + refresh tokens
4. Old refresh token is revoked

**Related:** [Token Refresh Flow](back/README.md#4-token-refresh-flow)

---

### Token Introspection

Process of validating a token to check if it's still valid and not revoked.

**Gateway Flow:**

1. Extract JWT from Authorization header
2. Validate signature and expiration
3. Call Auth Service `/auth/introspect` endpoint
4. Check if token is revoked in database
5. Proceed or reject request

**Related:** [API Gateway](back/services/API_GATEWAY.md)

---

### RBAC (Role-Based Access Control)

Access control mechanism where permissions are assigned to roles, and users are assigned roles.

**DayPulse Roles:**

- **USER** - Standard user (default)
- **MODERATOR** - Content moderation capabilities
- **ADMIN** - Full system access

**Role Hierarchy:**

```
ADMIN > MODERATOR > USER
```

**Implementation:** Stored in `users_auth.role_enum`

**Related:** [Auth Service](back/services/AUTH_SERVICE.md)

---

### OAuth 2.0

Industry-standard protocol for authorization. DayPulse uses OAuth-inspired patterns.

**DayPulse Usage:**

- Signup/login endpoints follow OAuth naming (e.g., `/signup` instead of `/register`)
- Token-based authentication flow
- Keycloak integration for social login (Google, Facebook)

**Related:** [Keycloak Setup](keycloak-setup.md)

---

### Keycloak

Open-source Identity and Access Management system used for authentication.

**Features Used:**

- OAuth/OIDC provider
- Social login (Google OAuth)
- User management
- SSO (Single Sign-On)

**Access:**

- **URL:** http://localhost:8888
- **Admin:** admin/admin

**Related:**

- [Keycloak Setup Guide](keycloak-setup.md)
- [Keycloak Integration](back/KEYCLOAK_INTEGRATION.md)

---

## Architecture Terms

### Microservices

Architectural style where application is composed of small, independent services.

**DayPulse Services:**

- API Gateway (8188)
- Auth Service (8180)
- User Service (8181)
- Feed Service (8182) - Planned
- Chat Service (8183) - Planned
- Notification Service (8184) - Planned
- Utility Service (8185) - Planned

**Benefits:**

- Independent deployment
- Technology diversity
- Fault isolation
- Scalability

**Related:** [System Architecture](SYSTEM_ARCHITECTURE.md)

---

### API Gateway

Single entry point for all client requests. Routes to appropriate microservices.

**Responsibilities:**

- Request routing
- JWT validation
- User identity extraction
- CORS handling
- Rate limiting (future)

**Technology:** Spring Cloud Gateway (Reactive)

**Related:** [API Gateway Documentation](back/services/API_GATEWAY.md)

---

### Service-to-Service Communication

Internal communication between backend microservices.

**Patterns Used:**

1. **Synchronous (HTTP/REST):**
   - Gateway → Services
   - Service → Service internal endpoints
   - Example: User Service `/internal/users/{id}/summary`

2. **Asynchronous (Kafka - Planned):**
   - Event-driven updates
   - Example: `pulse.created` event

**Security:** Internal endpoints not exposed via gateway

---

### Database-per-Service

Each microservice has its own database schema/instance.

**DayPulse Databases:**

- `auth-service` - User credentials, tokens, OTP
- `user-service` - User profiles, follows, stats
- `feed_db` (MongoDB) - Pulses, comments (planned)
- `chat_db` (MongoDB) - Messages, conversations (planned)

**Benefits:**

- Data isolation
- Independent scaling
- Technology choice per service

**Challenge:** Data consistency (handled via events)

---

## Frontend Terms

### React

JavaScript library for building user interfaces. DayPulse frontend is built with React.

**Version:** 18.x

**Key Concepts:**

- Components
- Hooks (useState, useEffect, custom hooks)
- Context (minimal use, Zustand preferred)

**Related:** [Frontend Architecture](front/ARCHITECTURE.md)

---

### TypeScript

Typed superset of JavaScript used in DayPulse frontend.

**Benefits:**

- Type safety
- Better IDE support
- Catch errors at compile time

---

### Zustand

Lightweight state management library used for client-side state.

**DayPulse Stores:**

- `useAuthStore` - Authentication state (user, tokens)
- `useUIStore` - UI preferences (theme, language)

**Features:**

- Persist to localStorage
- No boilerplate
- TypeScript support

**Example:**

```typescript
const { user, setAuth } = useAuthStore();
```

**Related:** [Frontend Architecture](front/ARCHITECTURE.md#pattern-2-zustand-for-client-state)

---

### React Query

Library for fetching, caching, and updating server data.

**DayPulse Usage:**

- All API calls go through React Query
- Automatic caching and refetching
- Optimistic updates

**Example:**

```typescript
const { data, isLoading } = useQuery({
  queryKey: ["feed"],
  queryFn: () => mockService.getFeed(),
});
```

**Related:** [Frontend Architecture](front/ARCHITECTURE.md#pattern-1-react-query-for-server-data)

---

### Vite

Fast build tool and development server for frontend.

**Features:**

- Hot Module Replacement (HMR)
- Fast cold start
- Optimized builds

**Command:** `npm run dev`

---

## Database Terms

### PostgreSQL

Open-source relational database used for structured data.

**DayPulse Usage:**

- User authentication data
- User profiles
- Follow relationships
- Refresh tokens

**Version:** 15+

---

### MongoDB

NoSQL document database (planned for feed and chat data).

**Planned Usage:**

- Pulses (posts)
- Comments
- Chat messages
- Notifications

**Benefits:**

- Flexible schema
- Fast reads
- Horizontal scaling

---

### Redis

In-memory data store (planned for caching and sessions).

**Planned Usage:**

- Session storage
- API response caching
- Rate limiting
- Real-time features

---

### ORM (Object-Relational Mapping)

Technique to interact with databases using objects instead of SQL.

**DayPulse:** Spring Data JPA

**Example:**

```java
// Instead of SQL:
// SELECT * FROM users_auth WHERE email = ?

// Use repository method:
userRepository.findByEmail("user@example.com");
```

---

### Flyway

Database migration tool for version control of database schemas.

**DayPulse Usage:**

- Migration files in `src/main/resources/db/migration/`
- Naming: `V1__Initial_Schema.sql`, `V2__Add_OTP_Table.sql`
- Automatically runs on application startup

**Related:** [Development Guide](back/DEVELOPMENT_GUIDE.md)

---

## Development Terms

### Maven

Build automation and dependency management tool for Java.

**Commands:**

- `mvn clean install` - Build project
- `mvn spring-boot:run` - Run application
- `mvn test` - Run tests

**File:** `pom.xml`

---

### Spring Boot

Framework for building production-ready Spring applications.

**Version:** 3.5.10

**Features Used:**

- Auto-configuration
- Embedded server (Tomcat)
- Actuator (health checks)
- Security
- Data JPA

---

### Spring Cloud Gateway

Reactive API gateway built on Spring Framework.

**Features:**

- Route requests
- Filter chains
- Load balancing
- Circuit breaker (planned)

**Related:** [API Gateway](back/services/API_GATEWAY.md)

---

### Docker

Containerization platform for packaging applications.

**DayPulse Usage:**

- Keycloak server
- PostgreSQL (optional)
- Future: All services

**Command:** `docker-compose up -d`

---

### Docker Compose

Tool for defining and running multi-container applications.

**File:** `docker-compose.yml`

**Services:**

- keycloak
- keycloak-db (PostgreSQL)

**Related:** [Docker Compose Guide](docker-compose.md)

---

## API Terms

### REST (REpresentational State Transfer)

Architectural style for designing networked applications.

**Principles:**

- Stateless
- Client-Server
- Cacheable
- Uniform Interface

**DayPulse:** All APIs follow REST principles

---

### Endpoint

A specific URL path that performs an action.

**Examples:**

- `POST /api/v1/auth/login` - Login
- `GET /api/v1/users/me` - Get profile
- `POST /api/v1/users/{id}/follow` - Follow user

---

### HTTP Methods

Actions that can be performed on resources.

**DayPulse Usage:**

- **GET** - Retrieve data (read)
- **POST** - Create new resource
- **PATCH** - Partial update
- **DELETE** - Remove resource
- **PUT** - Full replace (rarely used)

---

### Status Codes

HTTP response codes indicating request result.

**Common Codes:**

- `200 OK` - Success
- `201 Created` - Resource created
- `400 Bad Request` - Invalid input
- `401 Unauthorized` - Not authenticated
- `403 Forbidden` - Not authorized
- `404 Not Found` - Resource doesn't exist
- `500 Internal Server Error` - Server error

---

### CORS (Cross-Origin Resource Sharing)

Security feature that controls which domains can access your API.

**DayPulse Configuration:**

- Frontend: `http://localhost:5173`
- Configured in API Gateway

---

## Acronyms

| Acronym   | Full Name                                   | Description                                     |
| --------- | ------------------------------------------- | ----------------------------------------------- |
| **API**   | Application Programming Interface           | Interface for software communication            |
| **CDN**   | Content Delivery Network                    | Distributed server network for content delivery |
| **CRUD**  | Create, Read, Update, Delete                | Basic database operations                       |
| **DTO**   | Data Transfer Object                        | Object for transferring data between layers     |
| **ER**    | Entity-Relationship                         | Database design diagram                         |
| **FK**    | Foreign Key                                 | Database reference to another table             |
| **HTTP**  | HyperText Transfer Protocol                 | Web communication protocol                      |
| **HTTPS** | HTTP Secure                                 | Encrypted HTTP                                  |
| **IAM**   | Identity and Access Management              | Authentication and authorization                |
| **JPA**   | Java Persistence API                        | Java ORM specification                          |
| **JSON**  | JavaScript Object Notation                  | Data interchange format                         |
| **JWT**   | JSON Web Token                              | Token-based authentication format               |
| **OIDC**  | OpenID Connect                              | Identity layer on top of OAuth 2.0              |
| **ORM**   | Object-Relational Mapping                   | Database abstraction technique                  |
| **OTP**   | One-Time Password                           | Temporary verification code                     |
| **PK**    | Primary Key                                 | Unique database record identifier               |
| **RBAC**  | Role-Based Access Control                   | Permission system based on roles                |
| **REST**  | REpresentational State Transfer             | API architectural style                         |
| **SPA**   | Single Page Application                     | Web app that loads once                         |
| **SQL**   | Structured Query Language                   | Database query language                         |
| **SSO**   | Single Sign-On                              | One login for multiple services                 |
| **TLS**   | Transport Layer Security                    | Encryption protocol                             |
| **URL**   | Uniform Resource Locator                    | Web address                                     |
| **UUID**  | Universally Unique Identifier               | 128-bit unique ID                               |
| **VAPID** | Voluntary Application Server Identification | Web Push authentication                         |
| **WSS**   | WebSocket Secure                            | Encrypted WebSocket connection                  |

---

## Common Patterns

### Repository Pattern

Data access abstraction pattern.

**Example:**

```java
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
}
```

---

### DTO Pattern

Separate objects for data transfer vs domain models.

**Example:**

- `User` (entity) - Database model
- `UserDTO` (DTO) - API response
- MapStruct handles conversion

---

### Builder Pattern

Fluent API for constructing objects.

**Example:**

```java
User user = User.builder()
    .email("user@example.com")
    .role(Role.USER)
    .build();
```

---

## Need More Information?

- **System Overview:** [System Architecture](SYSTEM_ARCHITECTURE.md)
- **Backend Details:** [Backend README](back/README.md)
- **Frontend Details:** [Frontend Architecture](front/ARCHITECTURE.md)
- **API Reference:** [API Documentation](back/API_REFERENCE.md)

---

**Last Updated:** 2026-01-27  
**Total Terms:** 60+
