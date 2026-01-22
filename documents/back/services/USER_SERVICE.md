# User Service

User profile and social graph management service for DayPulse.

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Configuration](#configuration)
- [Database Schema](#database-schema)
- [API Endpoints](#api-endpoints)
- [Security Model](#security-model)
- [Follow System](#follow-system)
- [Integration](#integration)
- [Development](#development)
- [Troubleshooting](#troubleshooting)

---

## Overview

### Purpose

The User Service manages all **user profile and social graph functionality** for DayPulse, providing:
- User profile creation and updates
- Social connections (follow/unfollow)
- User statistics tracking (followers, following, pulses)
- User discovery (suggested users, search)
- Internal APIs for service-to-service communication

### Basic Information

| Property | Value |
|----------|-------|
| **Service Name** | user-service |
| **Port** | 8181 |
| **Context Path** | `/user-service` |
| **Technology** | Spring Boot + JPA |
| **Framework** | Spring Boot 3.5.10 |
| **Database** | PostgreSQL (`user-service`) |
| **Base URL** | `http://localhost:8181/user-service` |

### Technology Stack

```yaml
Core:
  - Spring Boot 3.5.10
  - Spring Data JPA
  - Hibernate ORM
  - Spring Web

Database:
  - PostgreSQL 15+
  - JPA for data access

Utilities:
  - MapStruct 1.5.5 (DTO mapping)
  - Lombok (boilerplate reduction)
  - Jakarta Validation

Build:
  - Maven
  - Java 21
```

### Responsibilities

1. **Profile Management**
   - Initial profile setup after registration
   - Profile updates (name, bio, avatar, etc.)
   - View own profile
   - View other users' profiles

2. **Social Graph**
   - Follow/unfollow users
   - Get followers list (paginated)
   - Get following list (paginated)
   - Automatic stats updates

3. **User Discovery**
   - Suggested users algorithm
   - Available users listing
   - User search (future)

4. **Statistics Tracking**
   - Followers count
   - Following count
   - Pulses count (future)
   - Automatic updates on follow/unfollow

5. **Internal APIs**
   - User summary for denormalization (Feed Service)
   - Profile initialization (Auth Service)

---

## Architecture

### Component Structure

```
user-service/
├── src/main/java/com/daypulse/user_service/
│   ├── UserServiceApplication.java          # Main entry point
│   ├── controller/
│   │   ├── UserController.java              # Public endpoints
│   │   └── InternalUserController.java      # Internal service APIs
│   ├── service/
│   │   ├── UserProfileService.java          # Profile business logic
│   │   └── FollowService.java               # Follow system logic
│   ├── entity/
│   │   ├── UserProfile.java                 # User profile entity
│   │   ├── UserStats.java                   # User statistics entity
│   │   ├── Follow.java                      # Follow relationship entity
│   │   └── FollowId.java                    # Composite key for follows
│   ├── repository/
│   │   ├── UserProfileRepository.java       # Profile data access
│   │   ├── UserStatsRepository.java         # Stats data access
│   │   └── FollowRepository.java            # Follow data access
│   ├── dto/
│   │   ├── request/
│   │   │   ├── ProfileSetupRequest.java     # Setup profile DTO
│   │   │   └── ProfileUpdateRequest.java    # Update profile DTO
│   │   └── response/
│   │       ├── ApiResponse.java             # Standard response wrapper
│   │       ├── UserResponse.java            # Full user profile
│   │       ├── UserSummaryResponse.java     # Brief user info
│   │       └── FollowResponse.java          # Follow action result
│   └── mapper/
│       └── UserProfileMapper.java           # MapStruct DTO mapper
└── src/main/resources/
    └── application.yaml                      # Configuration
```

### Key Components

#### 1. UserController

Public user profile endpoints (via API Gateway):
- `POST /users/me/setup` - Initial profile setup
- `GET /users/me` - Get own profile
- `PATCH /users/me` - Update own profile
- `GET /users/{id}` - Get user by ID
- `POST /users/{id}/follow` - Follow user
- `DELETE /users/{id}/follow` - Unfollow user
- `GET /users/{id}/followers` - Get followers (paginated)
- `GET /users/{id}/following` - Get following (paginated)
- `GET /users/suggested` - Get suggested users
- `GET /users/available` - Get available users

#### 2. InternalUserController

Internal service-to-service endpoints:
- `GET /internal/users/{id}/summary` - Get user summary (for Feed Service)
- `POST /internal/users/{id}/init` - Initialize profile (for Auth Service)

**Security Note**: These endpoints are NOT exposed via API Gateway and should only be accessible from internal network.

#### 3. UserProfileService

Core profile management logic:
- Profile creation and initialization
- Profile updates with validation
- User lookup by ID
- User discovery algorithms
- Stats calculation

#### 4. FollowService

Social graph management:
- Follow user with validation
- Unfollow user
- Get followers/following with pagination
- Automatic stats updates (@Transactional)

#### 5. UserProfile Entity

User profile information:
- UUID primary key (same as Auth Service user ID)
- Username (unique, 50 chars)
- Name (100 chars)
- Bio, avatar URL, timezone, language
- Streak, last pulse timestamp
- Online status, last seen

#### 6. UserStats Entity

User statistics:
- Followers count
- Following count
- Pulses count (future)
- Automatically updated on follow/unfollow

#### 7. Follow Entity

Follow relationship:
- Composite primary key (followerId, followingId)
- Creation timestamp
- No additional data (simple relationship)

---

## Configuration

### application.yaml

```yaml
server:
  port: 8181
  servlet:
    context-path: /user-service

spring:
  application:
    name: user-service
  
  datasource:
    url: jdbc:postgresql://localhost:5432/user-service
    username: postgres
    password: 123456
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
    show-sql: true

logging:
  level:
    com.daypulse: INFO
    org.hibernate.SQL: DEBUG
```

### Environment Variables (Production)

```bash
# Server
SERVER_PORT=8181

# Database
DB_URL=jdbc:postgresql://postgres:5432/user-service
DB_USERNAME=user_service_user
DB_PASSWORD=<strong-password>

# JPA
JPA_DDL_AUTO=validate  # Never use 'update' in production

# Logging
LOG_LEVEL=INFO
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
│    user_profiles        │
├─────────────────────────┤
│ id (UUID, PK)           │────┐
│ username (VARCHAR, UQ)  │    │
│ name (VARCHAR)          │    │ 1:1
│ bio (TEXT)              │    │
│ avatar_url (VARCHAR)    │    ▼
│ timezone (VARCHAR)      │ ┌────────────────────┐
│ language (VARCHAR)      │ │   user_stats       │
│ streak (INTEGER)        │ ├────────────────────┤
│ last_pulse_at           │ │ user_id (UUID, PK) │
│ is_online (BOOLEAN)     │ │ followers_count    │
│ last_seen_at            │ │ following_count    │
│ created_at              │ │ pulses_count       │
│ updated_at              │ │ updated_at         │
└──────────┬──────────────┘ └────────────────────┘
           │ 1:N
           │
           ▼
┌─────────────────────────┐
│       follows           │
├─────────────────────────┤
│ follower_id (UUID, PK)  │──┐ FK → user_profiles.id
│ following_id (UUID, PK) │──┘ FK → user_profiles.id
│ created_at              │
└─────────────────────────┘
Composite PK: (follower_id, following_id)
```

### Table: user_profiles

User profile and settings information.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK | User identifier (same as auth_service.users_auth.id) |
| `username` | VARCHAR(50) | UNIQUE, NOT NULL | Unique username |
| `name` | VARCHAR(100) | NOT NULL | Display name |
| `bio` | TEXT | NULL | User bio/description |
| `avatar_url` | VARCHAR(500) | NULL | Profile picture URL |
| `timezone` | VARCHAR(50) | DEFAULT 'UTC' | User timezone |
| `language` | VARCHAR(5) | DEFAULT 'en' | User language preference |
| `streak` | INTEGER | DEFAULT 0 | Daily activity streak |
| `last_pulse_at` | TIMESTAMP | NULL | Last pulse/post timestamp |
| `is_online` | BOOLEAN | DEFAULT false | Online status |
| `last_seen_at` | TIMESTAMP | NULL | Last seen timestamp |
| `created_at` | TIMESTAMP | NOT NULL | Profile creation time |
| `updated_at` | TIMESTAMP | NOT NULL | Last update time |

**Indexes**:
```sql
CREATE UNIQUE INDEX idx_user_profiles_username ON user_profiles(username);
CREATE INDEX idx_user_profiles_created_at ON user_profiles(created_at);
CREATE INDEX idx_user_profiles_is_online ON user_profiles(is_online);
```

### Table: user_stats

User statistics (followers, following, pulses).

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `user_id` | UUID | PK, FK | Reference to user_profiles.id |
| `followers_count` | INTEGER | DEFAULT 0 | Number of followers |
| `following_count` | INTEGER | DEFAULT 0 | Number of users following |
| `pulses_count` | INTEGER | DEFAULT 0 | Number of pulses posted |
| `updated_at` | TIMESTAMP | NOT NULL | Last stats update time |

**Indexes**:
```sql
CREATE INDEX idx_user_stats_followers_count ON user_stats(followers_count);
CREATE INDEX idx_user_stats_following_count ON user_stats(following_count);
```

**Automatic Updates**: Stats are updated via @Transactional service methods when follow/unfollow actions occur.

### Table: follows

Follow relationships (many-to-many).

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `follower_id` | UUID | PK, FK | User who follows |
| `following_id` | UUID | PK, FK | User being followed |
| `created_at` | TIMESTAMP | NOT NULL | Follow timestamp |

**Composite Primary Key**: `(follower_id, following_id)`

**Indexes**:
```sql
CREATE INDEX idx_follows_follower_id ON follows(follower_id);
CREATE INDEX idx_follows_following_id ON follows(following_id);
CREATE INDEX idx_follows_created_at ON follows(created_at);
```

**Constraints**:
- Foreign key: `follower_id` → `user_profiles.id`
- Foreign key: `following_id` → `user_profiles.id`
- Cannot follow yourself: Enforced in service layer

---

## API Endpoints

### Public Endpoints (Via API Gateway)

All endpoints require JWT authentication. Gateway adds `X-User-Id` header.

#### 1. POST /users/me/setup

Initial profile setup after registration.

**Request**:
```bash
curl -X POST http://localhost:8188/api/v1/users/me/setup \
  -H "Authorization: Bearer <jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "name": "John Doe",
    "bio": "Software developer and tech enthusiast"
  }'
```

**Response** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "id": "abcd-1234-efgh-5678",
    "username": "johndoe",
    "name": "John Doe",
    "bio": "Software developer and tech enthusiast",
    "avatarUrl": null,
    "timezone": "UTC",
    "language": "en",
    "streak": 0,
    "isOnline": false,
    "createdAt": "2026-01-22T10:00:00Z",
    "stats": {
      "followersCount": 0,
      "followingCount": 0,
      "pulsesCount": 0
    }
  }
}
```

**Business Logic**:
1. Extract userId from `X-User-Id` header
2. Validate username is unique
3. Create user profile with provided data
4. Create user_stats record with zeros
5. Mark user as setup complete in Auth Service (future)

**Constraints**:
- Username must be unique
- Username: 3-50 characters, alphanumeric + underscore
- Name: 1-100 characters
- Bio: max 500 characters (optional)

---

#### 2. GET /users/me

Get authenticated user's profile.

**Request**:
```bash
curl -X GET http://localhost:8188/api/v1/users/me \
  -H "Authorization: Bearer <jwt-token>"
```

**Response** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "id": "abcd-1234-efgh-5678",
    "username": "johndoe",
    "name": "John Doe",
    "bio": "Software developer",
    "avatarUrl": "https://example.com/avatar.jpg",
    "timezone": "America/Los_Angeles",
    "language": "en",
    "streak": 5,
    "lastPulseAt": "2026-01-22T09:30:00Z",
    "isOnline": true,
    "lastSeenAt": "2026-01-22T10:30:00Z",
    "createdAt": "2026-01-15T08:00:00Z",
    "stats": {
      "followersCount": 150,
      "followingCount": 75,
      "pulsesCount": 42
    }
  }
}
```

**Business Logic**:
1. Extract userId from `X-User-Id` header
2. Fetch user profile from database
3. Join with user_stats for statistics
4. Return combined profile + stats

---

#### 3. PATCH /users/me

Update authenticated user's profile.

**Request**:
```bash
curl -X PATCH http://localhost:8188/api/v1/users/me \
  -H "Authorization: Bearer <jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe Updated",
    "bio": "Senior Software Engineer",
    "avatarUrl": "https://example.com/new-avatar.jpg",
    "timezone": "America/New_York"
  }'
```

**Response** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "id": "abcd-1234-efgh-5678",
    "username": "johndoe",
    "name": "John Doe Updated",
    "bio": "Senior Software Engineer",
    "avatarUrl": "https://example.com/new-avatar.jpg",
    "timezone": "America/New_York",
    "language": "en"
  }
}
```

**Updatable Fields**:
- `name` (1-100 chars)
- `bio` (max 500 chars)
- `avatarUrl` (valid URL)
- `timezone` (valid timezone string)
- `language` (2-5 char code)

**Non-Updatable Fields**:
- `username` (cannot change after setup)
- `id` (immutable)
- `stats` (automatically calculated)

---

#### 4. GET /users/{id}

Get any user's profile by ID.

**Request**:
```bash
curl -X GET http://localhost:8188/api/v1/users/xyz789 \
  -H "Authorization: Bearer <jwt-token>"
```

**Response** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "id": "xyz789",
    "username": "janedoe",
    "name": "Jane Doe",
    "bio": "Designer and artist",
    "avatarUrl": "https://example.com/jane.jpg",
    "stats": {
      "followersCount": 200,
      "followingCount": 100,
      "pulsesCount": 85
    }
  }
}
```

**Business Logic**:
1. Find user profile by ID
2. Join with user_stats
3. Return public profile info (no private fields like email)

---

#### 5. POST /users/{id}/follow

Follow a user.

**Request**:
```bash
curl -X POST http://localhost:8188/api/v1/users/xyz789/follow \
  -H "Authorization: Bearer <jwt-token>"
```

**Response** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "followerId": "abcd-1234-efgh-5678",
    "followingId": "xyz789",
    "createdAt": "2026-01-22T10:30:00Z"
  }
}
```

**Business Logic** (@Transactional):
1. Extract followerId from `X-User-Id` header
2. Validate followingId exists
3. Check not already following (idempotent - return 200 if already following)
4. Check not following self
5. Create follow record
6. **Increment follower's following_count**
7. **Increment following's followers_count**

**Constraints**:
- Cannot follow yourself
- Cannot follow same user twice (idempotent)
- Both users must exist

---

#### 6. DELETE /users/{id}/follow

Unfollow a user.

**Request**:
```bash
curl -X DELETE http://localhost:8188/api/v1/users/xyz789/follow \
  -H "Authorization: Bearer <jwt-token>"
```

**Response** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "message": "Unfollowed successfully"
  }
}
```

**Business Logic** (@Transactional):
1. Extract followerId from `X-User-Id` header
2. Find follow record
3. Delete follow record
4. **Decrement follower's following_count**
5. **Decrement following's followers_count**

**Idempotent**: Returns 200 even if not following.

---

#### 7. GET /users/{id}/followers

Get user's followers list (paginated).

**Request**:
```bash
curl -X GET "http://localhost:8188/api/v1/users/xyz789/followers?page=0&size=20" \
  -H "Authorization: Bearer <jwt-token>"
```

**Response** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "content": [
      {
        "id": "user1",
        "username": "user1",
        "name": "User One",
        "avatarUrl": "https://example.com/user1.jpg"
      },
      {
        "id": "user2",
        "username": "user2",
        "name": "User Two",
        "avatarUrl": "https://example.com/user2.jpg"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20,
      "offset": 0
    },
    "totalElements": 150,
    "totalPages": 8,
    "last": false,
    "first": true,
    "numberOfElements": 20
  }
}
```

**Query Parameters**:
- `page`: Page number (0-indexed, default: 0)
- `size`: Page size (default: 20, max: 100)

**SQL Query** (Conceptual):
```sql
SELECT up.* FROM user_profiles up
JOIN follows f ON f.follower_id = up.id
WHERE f.following_id = ?
ORDER BY f.created_at DESC
LIMIT ? OFFSET ?
```

---

#### 8. GET /users/{id}/following

Get user's following list (paginated).

**Request**:
```bash
curl -X GET "http://localhost:8188/api/v1/users/xyz789/following?page=0&size=20" \
  -H "Authorization: Bearer <jwt-token>"
```

**Response**: Same format as followers endpoint.

**SQL Query** (Conceptual):
```sql
SELECT up.* FROM user_profiles up
JOIN follows f ON f.following_id = up.id
WHERE f.follower_id = ?
ORDER BY f.created_at DESC
LIMIT ? OFFSET ?
```

---

#### 9. GET /users/suggested

Get suggested users to follow.

**Request**:
```bash
curl -X GET http://localhost:8188/api/v1/users/suggested \
  -H "Authorization: Bearer <jwt-token>"
```

**Response** (200 OK):
```json
{
  "code": 1000,
  "result": [
    {
      "id": "user1",
      "username": "suggested1",
      "name": "Suggested User One",
      "avatarUrl": "https://example.com/user1.jpg"
    }
  ]
}
```

**Current Algorithm**:
- Returns random users not followed by requester
- Limit: 10 users

**Future Enhancements**:
- Mutual friends algorithm
- Similar interests/topics
- Popular users in network
- Machine learning recommendations

---

#### 10. GET /users/available

Get all available users (for discovery).

**Request**:
```bash
curl -X GET http://localhost:8188/api/v1/users/available \
  -H "Authorization: Bearer <jwt-token>"
```

**Response**: Same format as suggested users.

**Business Logic**:
- Returns all users except requester
- Useful for user search/discovery
- Future: Add search/filter parameters

---

### Internal Endpoints (Service-to-Service)

**Security Warning**: These endpoints are NOT exposed via API Gateway. In production:
- Deploy User Service in private subnet/VPC
- Only allow access from internal services
- Implement service-to-service authentication (mTLS, service tokens)

#### 11. GET /internal/users/{id}/summary

Get brief user summary (for denormalization).

**Purpose**: Feed Service embeds user info when creating posts.

**Request** (Internal):
```http
GET /user-service/internal/users/xyz789/summary
X-Service-Token: <service-auth-token>
```

**Response** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "id": "xyz789",
    "username": "janedoe",
    "name": "Jane Doe",
    "avatarUrl": "https://example.com/jane.jpg"
  }
}
```

**Future**: Cache in Redis for 15 minutes.

---

#### 12. POST /internal/users/{id}/init

Initialize user profile after registration.

**Purpose**: Auth Service calls this after successful signup.

**Request** (Internal):
```http
POST /user-service/internal/users/abcd-1234/init
X-Service-Token: <service-auth-token>
```

**Response** (200 OK):
```json
{
  "code": 1000,
  "message": "User profile initialized successfully"
}
```

**Business Logic**:
1. Create empty user_profile record
2. Create user_stats record with zeros
3. Set defaults (timezone=UTC, language=en)

---

## Security Model

### Trust Boundary

User Service operates **behind the API Gateway** and trusts the security boundary established by the gateway.

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Client     │────▶│ API Gateway  │────▶│ User Service │
│              │     │              │     │              │
│ Sends JWT    │     │ Validates    │     │ Trusts       │
│ in Header    │     │ JWT & adds   │     │ X-User-Id    │
│              │     │ X-User-Id    │     │ header       │
└──────────────┘     └──────────────┘     └──────────────┘
```

### Why No JWT Validation?

**Advantages**:
1. **Performance**: No JWT signature verification overhead
2. **Simplicity**: No JWT dependencies or signing keys needed
3. **Single Point of Auth**: Centralized at gateway
4. **Loose Coupling**: Service doesn't know about auth mechanisms

**Requirements**:
1. **Network Isolation**: User Service NOT directly accessible from internet
2. **Gateway Trust**: Only accept requests from API Gateway
3. **Header Validation**: Always validate `X-User-Id` header presence

### Protecting Against Header Spoofing

**Problem**: What if someone sends fake `X-User-Id` headers?

**Solution Layers**:

**1. Network Level** (Primary Defense):
- Deploy User Service in private subnet/VPC
- Only allow ingress from API Gateway IP/security group
- Use firewall rules to block direct external access
- No public IP for User Service

**2. Application Level** (Future Enhancement):
```java
@Component
public class GatewayHeaderValidationFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        // Verify request came from gateway
        String gatewayToken = httpRequest.getHeader("X-Gateway-Token");
        if (!isValidGatewayToken(gatewayToken)) {
            throw new UnauthorizedException("Must come through gateway");
        }
        
        // Validate X-User-Id present
        String userId = httpRequest.getHeader("X-User-Id");
        if (userId == null || userId.isEmpty()) {
            throw new UnauthorizedException("Missing user context");
        }
        
        chain.doFilter(request, response);
    }
}
```

### Production Security Checklist

- [ ] User Service in private subnet/VPC
- [ ] Firewall rules: Only gateway can access
- [ ] Security groups: Restrict ingress to gateway IP
- [ ] Validate `X-User-Id` header in all endpoints
- [ ] Monitor for unauthorized direct access
- [ ] Implement service mesh (Istio/Linkerd) for mTLS (future)
- [ ] Add gateway signature verification (future)

---

## Follow System

### Follow/Unfollow Transaction

The follow system uses `@Transactional` to ensure data consistency:

**Follow Transaction**:
```java
@Transactional
public FollowResponse followUser(UUID followerId, UUID followingId) {
    // 1. Create follow record
    Follow follow = Follow.builder()
        .id(new FollowId(followerId, followingId))
        .build();
    followRepository.save(follow);
    
    // 2. Update follower's stats (+1 following)
    UserStats followerStats = userStatsRepository.findById(followerId)
        .orElseGet(() -> createDefaultStats(followerId));
    followerStats.setFollowingCount(followerStats.getFollowingCount() + 1);
    userStatsRepository.save(followerStats);
    
    // 3. Update following's stats (+1 followers)
    UserStats followingStats = userStatsRepository.findById(followingId)
        .orElseGet(() -> createDefaultStats(followingId));
    followingStats.setFollowersCount(followingStats.getFollowersCount() + 1);
    userStatsRepository.save(followingStats);
    
    return buildFollowResponse(follow);
}
```

**Atomicity**: All three operations succeed or all fail (transaction rollback).

### Stats Consistency

**Problem**: Race conditions in concurrent follow/unfollow

**Solution** (Current):
- `@Transactional` ensures database-level consistency
- Optimistic locking with `@Version` (future)

**Solution** (Future - Redis Counters):
```java
// Atomic increment with Redis
redisTemplate.opsForValue().increment("user:followers:" + followingId);
redisTemplate.opsForValue().increment("user:following:" + followerId);
```

### Pagination Performance

**Current Implementation**:
- Spring Data Pageable
- Offset-based pagination
- Index on created_at for sorting

**Future Optimization** (Cursor-based):
```java
@Query("SELECT u FROM UserProfile u JOIN Follow f ON f.followerId = u.id " +
       "WHERE f.followingId = :userId AND f.createdAt < :cursor " +
       "ORDER BY f.createdAt DESC")
List<UserProfile> findFollowersBefore(@Param("userId") UUID userId,
                                       @Param("cursor") LocalDateTime cursor,
                                       Pageable pageable);
```

Benefits:
- More efficient for large datasets
- Consistent results during pagination
- Works better with real-time updates

---

## Integration

### Service-to-Service Communication

#### Auth Service → User Service

**After Registration**:
```http
POST /user-service/internal/users/{userId}/init

Response:
{
  "code": 1000,
  "message": "User profile initialized successfully"
}
```

**Flow**:
1. User signs up via Auth Service
2. Auth Service creates user account
3. Auth Service calls User Service to initialize profile
4. User profile + stats created with defaults

#### Feed Service → User Service (Future)

**Get User Summary for Post**:
```http
GET /user-service/internal/users/{userId}/summary

Response:
{
  "code": 1000,
  "result": {
    "id": "xyz789",
    "username": "janedoe",
    "name": "Jane Doe",
    "avatarUrl": "https://example.com/jane.jpg"
  }
}
```

**Purpose**: Embed user info in posts for denormalization.

---

## Development

### Running Locally

```bash
# Navigate to user service directory
cd backEnd/user-service

# Run with Maven
mvn spring-boot:run

# Or build and run JAR
mvn clean package -DskipTests
java -jar target/user-service-0.0.1-SNAPSHOT.jar
```

### Database Setup

```bash
# Create database
psql -U postgres -c "CREATE DATABASE \"user-service\";"

# Verify tables created (JPA auto-creates on startup)
psql -U postgres -d user-service -c "\dt"
```

### Health Check

```bash
curl http://localhost:8181/user-service/actuator/health
```

Expected response:
```json
{
  "status": "UP"
}
```

### Testing Endpoints

**Setup Profile** (requires token from Auth Service):
```bash
# 1. Login to get token
TOKEN=$(curl -X POST http://localhost:8188/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "password": "password123"}' \
  | jq -r '.result.tokens.accessToken')

# 2. Setup profile
curl -X POST http://localhost:8188/api/v1/users/me/setup \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"username": "johndoe", "name": "John Doe", "bio": "Developer"}'

# 3. Get profile
curl -X GET http://localhost:8188/api/v1/users/me \
  -H "Authorization: Bearer $TOKEN"
```

**Follow User**:
```bash
# Get second user's ID
USER2_ID="xyz789"

# Follow user
curl -X POST "http://localhost:8188/api/v1/users/$USER2_ID/follow" \
  -H "Authorization: Bearer $TOKEN"

# Get followers
curl -X GET "http://localhost:8188/api/v1/users/$USER2_ID/followers" \
  -H "Authorization: Bearer $TOKEN"
```

### Common Development Tasks

**Add New Profile Field**:
```java
// 1. Add to UserProfile entity
@Column(length = 100)
private String location;

// 2. Add to ProfileUpdateRequest DTO
private String location;

// 3. Update MapStruct mapper (auto-generated)

// 4. Restart service (JPA updates schema)
```

**Change Default Values**:
```java
// In UserProfile entity
@Builder.Default
String timezone = "America/Los_Angeles";  // New default

@Builder.Default
String language = "es";  // Spanish default
```

**Debug SQL Queries**:
```yaml
# In application.yaml
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

---

## Troubleshooting

### Common Issues

#### 1. Profile Not Found After Registration

**Symptom**: 404 Not Found when accessing `/users/me`

**Cause**: Profile not initialized after registration

**Debug**:
```bash
# Check if profile exists
psql -U postgres -d user-service -c \
  "SELECT * FROM user_profiles WHERE id = 'user-uuid';"
```

**Fix**:
- Manually call `/internal/users/{id}/init`
- Or ensure Auth Service calls init endpoint after signup

---

#### 2. Duplicate Username Error

**Symptom**: 409 Conflict when setting up profile

**Cause**: Username already taken

**Debug**:
```bash
# Check existing usernames
psql -U postgres -d user-service -c \
  "SELECT username, id FROM user_profiles WHERE username = 'johndoe';"
```

**Fix**: Choose different username

---

#### 3. Stats Not Updating

**Symptom**: Follower/following counts incorrect

**Cause**: Transaction rollback or race condition

**Debug**:
```bash
# Check follow records
psql -U postgres -d user-service -c \
  "SELECT COUNT(*) FROM follows WHERE follower_id = 'user-uuid';"

# Check stats
psql -U postgres -d user-service -c \
  "SELECT * FROM user_stats WHERE user_id = 'user-uuid';"
```

**Fix**:
- Recalculate stats manually:
```sql
UPDATE user_stats SET
  following_count = (SELECT COUNT(*) FROM follows WHERE follower_id = user_id),
  followers_count = (SELECT COUNT(*) FROM follows WHERE following_id = user_id)
WHERE user_id = 'user-uuid';
```

---

#### 4. Cannot Follow Self

**Symptom**: Error when trying to follow own account

**Cause**: Business logic prevents self-following

**This is intentional behavior** - users cannot follow themselves.

---

#### 5. Pagination Returns Duplicate Results

**Symptom**: Same users appear on multiple pages

**Cause**: Offset pagination with concurrent updates

**Fix** (Future): Implement cursor-based pagination

---

### Performance Issues

**Slow Followers Query**:
- Add composite index: `(following_id, created_at)`
- Implement Redis caching for frequently accessed followers
- Use cursor-based pagination

**Stats Update Bottleneck**:
- Move to Redis counters for real-time updates
- Periodic sync from Redis to PostgreSQL
- Use Redis atomic operations (INCR/DECR)

---

## Future Enhancements

### Planned Features

**1. Redis Caching**:
- User profile caching (15 minutes TTL)
- User summary caching for Feed Service
- Stats counters for real-time updates

**2. User Search**:
- Full-text search on username and name
- Filter by location, interests
- Elasticsearch integration (optional)

**3. Relationship Types**:
- Mutual follows (friends)
- Blocked users
- Muted users
- Follow requests for private accounts

**4. Privacy Settings**:
- Private accounts (follow requests)
- Hidden follower/following lists
- Profile visibility controls

**5. Activity Tracking**:
- Last seen timestamp
- Online status (real-time with WebSocket)
- Activity feed

**6. User Recommendations**:
- Machine learning-based suggestions
- Mutual friends algorithm
- Interest-based recommendations
- Network analysis (graph algorithms)

**7. Kafka Event Publishing**:
- `user.profile.created` event
- `user.profile.updated` event
- `user.followed` event
- `user.unfollowed` event

**8. Batch Operations**:
- Bulk follow/unfollow
- Export followers/following list
- Import contacts

---

**Last Updated**: 2026-01-22  
**Version**: 0.1.0  
**Status**: Production Ready
