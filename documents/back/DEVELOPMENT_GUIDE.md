# DayPulse Development Guide

Complete guide for setting up, developing, and maintaining the DayPulse backend services.

---

## Table of Contents

- [Quick Start](#quick-start-5-minutes)
- [Detailed Setup](#detailed-setup)
- [Code Standards](#code-standards)
- [Performance Guidelines](#performance-guidelines)
- [Troubleshooting](#troubleshooting)

---

## Quick Start (5 Minutes)

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

## Detailed Setup

### Prerequisites

#### Required Software

1. **Java Development Kit 21**
   - Download: https://adoptium.net/
   - Verify: `java -version`
   - Should show: Java 21.x.x

2. **Maven 3.8+**
   - Verify: `mvn -version`
   - Should show: Apache Maven 3.8.x or higher

3. **PostgreSQL 15+**
   - Download: https://www.postgresql.org/download/
   - Verify: `psql --version`

4. **Git**
   - Verify: `git --version`

#### Optional (Recommended)

- **pgAdmin** - PostgreSQL GUI tool
- **Postman** or **Bruno** - API testing
- **IntelliJ IDEA** - IDE for development

### Database Setup

#### Step 1: Start PostgreSQL Service

**Windows**:
```powershell
# Check if PostgreSQL is running
Get-Service -Name postgresql*

# Start if not running
Start-Service -Name postgresql-x64-15
```

**macOS**:
```bash
brew services start postgresql@15
```

**Linux**:
```bash
sudo systemctl start postgresql
sudo systemctl enable postgresql  # Start on boot
```

#### Step 2: Create Databases

Open PostgreSQL command line:

```bash
# Connect to PostgreSQL
psql -U postgres

# Or on Windows:
psql -U postgres -W
```

Create the databases:

```sql
-- Create databases
CREATE DATABASE "auth-service";
CREATE DATABASE "user-service";

-- Verify databases were created
\l

-- Exit
\q
```

#### Step 3: Verify Database Connection

Test connection to each database:

```bash
# Test auth-service database
psql -U postgres -d auth-service -c "SELECT version();"

# Test user-service database
psql -U postgres -d user-service -c "SELECT version();"
```

**Expected Output**: Should show PostgreSQL version information.

#### Step 4: Update Database Credentials (If Needed)

If you're not using the default `postgres` user or need to change passwords:

**Auth Service**: `backEnd/auth-service/src/main/resources/application.yml`
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/auth-service
    username: postgres
    password: your_password
```

**User Service**: `backEnd/user-service/src/main/resources/application.yml`
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/user-service
    username: postgres
    password: your_password
```

### Build Services

#### Option 1: Build All Services

```bash
cd backEnd

# Auth Service
cd auth-service
mvn clean install -DskipTests
cd ..

# User Service
cd user-service
mvn clean install -DskipTests
cd ..

# API Gateway
cd api-gateway
mvn clean install -DskipTests
cd ..
```

#### Option 2: Build with Tests

```bash
cd backEnd/auth-service
mvn clean install
```

### Running Services

#### Option 1: Using Maven (Development)

Open three separate terminal windows:

**Terminal 1 - Auth Service**:
```bash
cd backEnd/auth-service
mvn spring-boot:run
```

**Terminal 2 - User Service**:
```bash
cd backEnd/user-service
mvn spring-boot:run
```

**Terminal 3 - API Gateway**:
```bash
cd backEnd/api-gateway
mvn spring-boot:run
```

#### Option 2: Using JAR Files (Production-like)

```bash
# Build JARs
cd backEnd/auth-service
mvn clean package -DskipTests

cd ../user-service
mvn clean package -DskipTests

cd ../api-gateway
mvn clean package -DskipTests

# Run JARs
cd ../auth-service/target
java -jar auth-service-0.0.1-SNAPSHOT.jar &

cd ../../user-service/target
java -jar user-service-0.0.1-SNAPSHOT.jar &

cd ../../api-gateway/target
java -jar api-gateway-0.0.1-SNAPSHOT.jar &
```

#### Option 3: Using IDE (IntelliJ IDEA)

1. Open IntelliJ IDEA
2. File → Open → Select `backEnd` folder
3. Wait for Maven import
4. Right-click on `AuthServiceApplication.java` → Run
5. Repeat for `UserServiceApplication.java` and `ApiGatewayApplication.java`

### Verify Services Running

Check that all services are running:

```bash
# Auth Service
curl http://localhost:8180/auth-service/actuator/health

# User Service
curl http://localhost:8181/user-service/actuator/health

# API Gateway
curl http://localhost:8188/actuator/health
```

All should return: `{"status":"UP"}`

### Database Migrations

Flyway automatically runs migrations on startup. Check migration status:

```sql
-- Connect to auth-service database
psql -U postgres -d auth-service

-- Check Flyway history
SELECT * FROM flyway_schema_history;

-- Exit
\q
```

### Apply Performance Indexes

For optimal performance, run the index creation script:

```bash
psql -U postgres -d auth-service -f documents/back/database_indexes.sql
psql -U postgres -d user-service -f documents/back/database_indexes.sql
```

---

## Code Standards

### Architecture Principles

1. **Separation of Concerns**
   - Controller: Handle HTTP requests/responses
   - Service: Business logic
   - Repository: Data access
   - DTO: Data transfer between layers

2. **Microservice Independence**
   - Each service has its own database
   - No direct database access between services
   - Use internal APIs for service-to-service communication

3. **API Gateway Pattern**
   - Single entry point for all client requests
   - Centralized authentication
   - Request routing to appropriate services

### Java Code Style

#### Use Lombok for Boilerplate Reduction

**Good**:
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private UUID id;
    private String username;
    private String name;
}
```

**Avoid**:
```java
public class UserResponse {
    private UUID id;
    private String username;
    private String name;
    
    // 50+ lines of getters/setters/constructors...
}
```

#### Use MapStruct for DTO Mapping

**Good**:
```java
@Mapper(componentModel = "spring")
public interface UserProfileMapper {
    UserResponse toResponse(UserProfile profile);
    UserSummaryResponse toSummary(UserProfile profile);
}
```

**Avoid**:
```java
public UserResponse toResponse(UserProfile profile) {
    UserResponse response = new UserResponse();
    response.setId(profile.getId());
    response.setUsername(profile.getUsername());
    // ... manual mapping for every field
    return response;
}
```

#### Use @Transactional for Data Modifications

**Good**:
```java
@Service
public class FollowService {
    @Transactional
    public void followUser(UUID followerId, UUID followingId) {
        // Multiple database operations in one transaction
        followRepository.save(follow);
        userStatsRepository.updateFollowerCount(followingId);
        userStatsRepository.updateFollowingCount(followerId);
    }
}
```

#### Add JavaDoc for Public Methods

**Good**:
```java
/**
 * Follows a user and updates statistics.
 * 
 * @param followerId The ID of the user who is following
 * @param followingId The ID of the user being followed
 * @throws IllegalArgumentException if user tries to follow themselves
 * @throws UserNotFoundException if either user doesn't exist
 */
@Transactional
public void followUser(UUID followerId, UUID followingId) {
    // Implementation
}
```

#### Use Meaningful Variable Names

**Good**:
```java
UserProfile currentUserProfile = userProfileRepository.findById(userId)
    .orElseThrow(() -> new UserNotFoundException(userId));
```

**Avoid**:
```java
UserProfile up = userProfileRepository.findById(userId)
    .orElseThrow(() -> new UserNotFoundException(userId));
```

### API Design Standards

#### RESTful Endpoints

**Good**:
```
GET    /users/{id}      - Get user
POST   /users           - Create user
PATCH  /users/{id}      - Update user (partial)
DELETE /users/{id}      - Delete user
POST   /users/{id}/follow   - Follow action
DELETE /users/{id}/follow   - Unfollow action
```

**Avoid**:
```
GET    /getUser/{id}
POST   /createUser
POST   /updateUser
POST   /followUser
```

#### Use Standard HTTP Status Codes

- **200 OK**: Success
- **201 Created**: Resource created
- **400 Bad Request**: Invalid input
- **401 Unauthorized**: Authentication required
- **403 Forbidden**: Insufficient permissions
- **404 Not Found**: Resource doesn't exist
- **409 Conflict**: Duplicate resource
- **500 Internal Server Error**: Server error

#### Consistent Response Format

**Good**:
```json
{
  "code": 1000,
  "result": {
    "user": {...}
  }
}
```

**For Errors**:
```json
{
  "code": 1006,
  "message": "Unauthenticated"
}
```

### Error Handling

#### Use Custom Exceptions

```java
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(UUID userId) {
        super("User not found: " + userId);
    }
}
```

#### Global Exception Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(1003, ex.getMessage()));
    }
}
```

### Database Best Practices

#### Use Prepared Statements (JPA does this automatically)

**Good** (JPA):
```java
@Query("SELECT u FROM UserProfile u WHERE u.username = :username")
Optional<UserProfile> findByUsername(@Param("username") String username);
```

#### Index Important Columns

```sql
-- Add index for frequently queried columns
CREATE INDEX idx_user_profiles_username ON user_profiles(username);
CREATE INDEX idx_follows_follower_id ON follows(follower_id);
```

#### Use Transactions for Multiple Operations

```java
@Transactional
public void transferFollowers(UUID oldUserId, UUID newUserId) {
    List<Follow> follows = followRepository.findByFollowingId(oldUserId);
    follows.forEach(follow -> follow.setFollowingId(newUserId));
    followRepository.saveAll(follows);
}
```

---

## Performance Guidelines

### Issues Fixed

#### 1. Logout Performance Issue (FIXED)

**Problem**: Loading all refresh tokens into memory

```java
// BEFORE - Bad Performance (loads all tokens)
List<RefreshToken> tokens = refreshTokenRepository.findAll();
tokens.stream()
    .filter(rt -> rt.getUser().getId().equals(user.getId()) && rt.getRevokedAt() == null)
    .forEach(rt -> { ... });
```

**Solution**: Use bulk update query

```java
// AFTER - Better (marked for future optimization)
@Modifying
@Query("UPDATE RefreshToken rt SET rt.revokedAt = :now " +
       "WHERE rt.user.id = :userId AND rt.revokedAt IS NULL")
int revokeAllUserTokens(@Param("userId") UUID userId, @Param("now") LocalDateTime now);
```

#### 2. Follow Stats Performance Issue (FIXED)

**Problem**: Always creating objects even when not needed

```java
// BEFORE - Bad (always evaluates)
UserStats stats = userStatsRepository.findById(userId)
    .orElse(UserStats.builder().userId(userId).build());
```

**Solution**: Use `orElseGet()` for lazy evaluation

```java
// AFTER - Better (only evaluates when needed)
UserStats stats = userStatsRepository.findById(userId)
    .orElseGet(() -> UserStats.builder()
        .userId(userId)
        .followersCount(0)
        .followingCount(0)
        .build());
```

#### 3. Null Safety in Stats Updates (FIXED)

**Problem**: NullPointerException when count is null

```java
// BEFORE - Unsafe
followerStats.setFollowersCount(followerStats.getFollowersCount() + 1);
```

**Solution**: Add null checks

```java
// AFTER - Safe
int currentCount = followerStats.getFollowersCount() != null 
    ? followerStats.getFollowersCount() : 0;
followerStats.setFollowersCount(currentCount + 1);
```

### Performance Best Practices

1. **Use Pagination for Large Datasets**
   ```java
   Page<UserProfile> users = userProfileRepository.findAll(
       PageRequest.of(page, size)
   );
   ```

2. **Add Database Indexes**
   - Index foreign keys
   - Index frequently queried columns
   - Index columns used in WHERE clauses

3. **Use Connection Pooling**
   ```yaml
   spring:
     datasource:
       hikari:
         maximum-pool-size: 20
         minimum-idle: 5
         connection-timeout: 30000
   ```

4. **Cache Frequently Accessed Data** (Future with Redis)
   ```java
   @Cacheable(value = "users", key = "#userId")
   public UserProfile getUserProfile(UUID userId) {
       return userProfileRepository.findById(userId).orElseThrow();
   }
   ```

5. **Use Bulk Operations**
   ```java
   // Good - Single query
   followRepository.saveAll(follows);
   
   // Avoid - Multiple queries
   follows.forEach(follow -> followRepository.save(follow));
   ```

### Future Optimizations Marked in Code

Look for `TODO` comments in the codebase:

```bash
# Find all TODO comments
grep -r "TODO:" backEnd/

# By category
grep -r "TODO: \[FUTURE-REDIS\]" backEnd/
grep -r "TODO: \[FUTURE-OPTIMIZATION\]" backEnd/
```

---

## Troubleshooting

### Common Issues

#### Services Won't Start

**Symptom**: Service fails to start with error

**Solutions**:
- ✅ Check PostgreSQL is running
- ✅ Verify databases exist
- ✅ Check port availability (8180, 8181, 8188)
- ✅ Verify Java 21 is active: `java -version`

**Check Port Availability**:
```bash
# Windows
netstat -ano | findstr "8188"

# macOS/Linux
lsof -i :8188
```

#### Database Connection Errors

**Symptom**: `Connection refused` or `Database not found`

**Solutions**:
- ✅ Check PostgreSQL service is running
- ✅ Verify database name matches application.yml
- ✅ Check database credentials
- ✅ Test connection: `psql -U postgres -d auth-service`

**Common Error Messages**:
```
FATAL: database "auth-service" does not exist
Solution: CREATE DATABASE "auth-service";

FATAL: password authentication failed
Solution: Update password in application.yml
```

#### JWT Token Errors

**Symptom**: `Unauthenticated` error on protected endpoints

**Solutions**:
- ✅ Token may be expired (default: 1 hour)
- ✅ Check token format: `Bearer <token>`
- ✅ Verify JWT signing key matches across services
- ✅ Use refresh token to get new access token

**Test Token**:
```bash
# Introspect token
curl -X POST http://localhost:8188/api/v1/auth/introspect \
  -H "Content-Type: application/json" \
  -d '{"token": "your_token_here"}'
```

#### Build Errors

**Symptom**: Maven build fails

**Solutions**:
- ✅ Clean Maven cache: `rm -rf ~/.m2/repository`
- ✅ Update dependencies: `mvn clean install -U`
- ✅ Check Java version: `java -version`
- ✅ Check Maven version: `mvn -version`

**Force Clean Build**:
```bash
mvn clean install -U -DskipTests
```

#### Port Already in Use

**Symptom**: `Port 8188 already in use`

**Solution**:
```bash
# Windows - Find and kill process
netstat -ano | findstr "8188"
taskkill /PID <process_id> /F

# macOS/Linux - Find and kill process
lsof -ti:8188 | xargs kill -9
```

#### Migration Failures

**Symptom**: Flyway migration fails

**Solutions**:
- ✅ Check Flyway history: `SELECT * FROM flyway_schema_history;`
- ✅ Manually fix failed migration
- ✅ Drop database and recreate (development only)

**Reset Database** (development only):
```sql
DROP DATABASE "auth-service";
CREATE DATABASE "auth-service";
```

### Debug Mode

Run services in debug mode to see detailed logs:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--logging.level.com.daypulse=DEBUG"
```

Or update `application.yml`:
```yaml
logging:
  level:
    com.daypulse: DEBUG
    org.springframework.security: DEBUG
```

### Health Check Endpoints

Check service health:

```bash
# Auth Service
curl http://localhost:8180/auth-service/actuator/health

# User Service
curl http://localhost:8181/user-service/actuator/health

# API Gateway
curl http://localhost:8188/actuator/health
```

### Database Verification

Check that migrations ran successfully:

```bash
# Connect to database
psql -U postgres -d auth-service

# Check tables exist
\dt

# Check Flyway history
SELECT * FROM flyway_schema_history;

# Check user count
SELECT COUNT(*) FROM users_auth;
```

### Common SQL Queries for Debugging

```sql
-- Check if user exists
SELECT * FROM users_auth WHERE email = 'user@example.com';

-- Check refresh tokens
SELECT * FROM refresh_tokens WHERE user_id = 'user-uuid';

-- Check user profile
SELECT * FROM user_profiles WHERE id = 'user-uuid';

-- Check follows
SELECT * FROM follows WHERE follower_id = 'user-uuid';

-- Check user stats
SELECT * FROM user_stats WHERE user_id = 'user-uuid';
```

### Getting Help

1. **Check Logs**: Review service logs for error messages
2. **Check Documentation**: See [API_REFERENCE.md](API_REFERENCE.md) for API details
3. **Run Tests**: `mvn test` to verify functionality
4. **Clean Build**: `mvn clean install` to rebuild from scratch

---

**Last Updated**: 2026-01-22  
**Version**: 0.1.0  
**Status**: Production Ready
