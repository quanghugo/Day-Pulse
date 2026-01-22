# DayPulse Backend Changelog

All notable changes to the DayPulse backend services.

---

## v0.1.0 (2026-01-22) - RBAC Implementation

### Added

**Role-Based Access Control (RBAC)**:
- Enum-based role system (USER, MODERATOR, ADMIN)
- 12 chat-specific permissions with hierarchical inheritance
- Admin endpoints for role management (`/admin/**`)
- Default admin user auto-creation on startup
- Compile-time safety for roles and permissions

**New Files**:
- `enums/RoleEnum.java` - Three roles: USER, MODERATOR, ADMIN
- `enums/PermissionEnum.java` - 12 permissions
- `service/UserRoleService.java` - Role management logic
- `controller/AdminController.java` - Admin-only endpoints
- `config/DataInitializer.java` - Auto-create default admin
- `dto/request/UpdateRoleRequest.java` - Update role payload
- `dto/response/RoleInfoResponse.java` - Role info with permissions
- `db/migration/V2__add_role_enum_column.sql` - Database migration

**Admin Endpoints**:
- `PATCH /admin/users/{id}/role` - Update user role (admin only)
- `GET /admin/roles` - List all roles with permissions

### Changed

**Simplified RBAC Model**:
- **Before**: Database-driven RBAC with 4 tables (roles, permissions, user_roles, role_permissions)
- **After**: Enum-based system with single `role_enum` column
- **Benefit**: Simpler architecture, compile-time safety, no database overhead for role management

**Entity Updates**:
- `UserAuth.java`: Changed from `Set<Role> roles` to `RoleEnum role`
- Added `@Enumerated(EnumType.STRING)` for database storage
- Default role: `RoleEnum.USER`

**Service Updates**:
- `AuthenticationService.java`: Updated `buildScope()` to include all role permissions in JWT
- `AuthenticationService.register()`: Assigns `RoleEnum.USER` to new users
- Removed `RoleRepository` dependency

**Security Configuration**:
- Added admin endpoint protection: `.requestMatchers("/admin/**").hasRole("ADMIN")`
- `@PreAuthorize("hasRole('ADMIN')")` annotations on admin controller

**API Gateway**:
- Updated port from 8888 to 8188
- All public documentation updated to reflect new port

### Removed

**Deprecated RBAC Components**:
- Deleted `Role.java` entity
- Deleted `Permission.java` entity
- Deleted `RoleService.java`
- Deleted `PermissionService.java`
- Deleted `RoleController.java`
- Deleted `PermissionController.java`
- Deleted `RoleRepository.java`
- Deleted `PermissionRepository.java`
- Deleted `RoleMapper.java`
- Deleted `PermissionMapper.java`
- Deleted 4 DTOs (RoleRequest, RoleResponse, PermissionRequest, PermissionResponse)
- Marked `PredefinedRole.java` as `@Deprecated`

### Default Admin User

```
Email: admin@daypulse.com
Password: Admin@123
Role: ADMIN
```

Auto-created on first startup if no admin exists.

### Migration Notes for v0.1.0

**Database Migration**:
The `V2__add_role_enum_column.sql` migration automatically:
1. Adds `role` column to `users_auth` table
2. Migrates existing role data from old tables
3. Drops old `roles`, `permissions`, and junction tables
4. Sets default role to 'USER' for existing users

**API Changes**:
- Port changed from 8888 to 8188 (update all client configurations)
- Admin endpoints now require ADMIN role
- JWT tokens now include all role permissions in `scope` claim

**Testing**:
```bash
# Test admin login
curl -X POST http://localhost:8188/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@daypulse.com", "password": "Admin@123"}'

# Test role update (admin only)
curl -X PATCH http://localhost:8188/api/v1/admin/users/{userId}/role \
  -H "Authorization: Bearer <admin_token>" \
  -H "Content-Type: application/json" \
  -d '{"role": "MODERATOR"}'

# Test list roles
curl -X GET http://localhost:8188/api/v1/admin/roles \
  -H "Authorization: Bearer <admin_token>"
```

---

## v0.0.1 (2026-01-19) - Initial Release

### Added

**Core Services**:
- Auth Service (Port 8180) - Authentication and user account management
- User Service (Port 8181) - User profiles and social graph
- API Gateway (Port 8188) - Request routing and JWT validation

**Authentication Features**:
- User registration with email and password
- JWT-based authentication (OAuth 2.0 style)
- Access tokens (1 hour validity)
- Refresh tokens (10 hours validity)
- Token rotation on refresh
- Logout with token revocation
- Token introspection endpoint

**User Profile Features**:
- Profile setup after registration
- Profile updates (PATCH support)
- Get user by ID
- View own profile
- User statistics tracking

**Social Features**:
- Follow/unfollow users
- Get followers list (paginated)
- Get following list (paginated)
- Suggested users
- Available users discovery
- Automatic stats updates on follow/unfollow

**Security**:
- BCrypt password hashing (cost 10)
- HS512 JWT signing algorithm
- Token expiration enforcement
- Refresh token hashing (MD5 for lookup)
- CORS configuration
- Authorization header validation
- Internal header forwarding (X-User-Id, X-User-Roles)

**Database**:
- PostgreSQL for both services
- Flyway database migrations
- Separate databases per service (auth-service, user-service)

**API Standards**:
- RESTful API design
- Standard HTTP status codes
- Consistent response format
- Error code standardization
- OAuth 2.0-style token handling

### Database Schema

**Auth Service** (`auth-service`):
- `users_auth` - User accounts and credentials
- `refresh_tokens` - Refresh token storage with revocation
- `otp_codes` - Email verification codes (future)

**User Service** (`user-service`):
- `user_profiles` - User profile information
- `user_stats` - Follower/following/pulses counts
- `follows` - Follow relationships

### API Endpoints

**Auth Service**:
- `POST /auth/signup` - Create account (OAuth 2.0 standard)
- `POST /auth/register` - Register user (backward compatibility)
- `POST /auth/login` - Authenticate and get tokens
- `POST /auth/refresh` - Renew access token
- `POST /auth/logout` - Revoke tokens
- `POST /auth/introspect` - Validate token
- `GET /users/my-info` - Get authenticated user info

**User Service**:
- `POST /users/me/setup` - Complete profile setup
- `GET /users/me` - Get own profile
- `PATCH /users/me` - Update profile
- `GET /users/{id}` - Get user by ID
- `POST /users/{id}/follow` - Follow user
- `DELETE /users/{id}/follow` - Unfollow user
- `GET /users/{id}/followers` - Get followers (paginated)
- `GET /users/{id}/following` - Get following (paginated)
- `GET /users/suggested` - Get suggested users
- `GET /users/available` - Get available users
- `POST /internal/users/{id}/init` - Initialize profile (internal)
- `GET /internal/users/{id}/summary` - Get user summary (internal)

### Architecture Changes

**Microservices Architecture**:
- Single entry point via API Gateway
- Service independence (separate databases)
- Internal APIs for service-to-service communication
- Trust boundary at gateway

**Token Handling**:
- Access tokens in Authorization header (`Bearer <token>`)
- Refresh tokens in request body
- Token rotation on refresh (old token revoked)
- JWT claims: `sub`, `userId`, `scope`, `iss`, `exp`, `iat`, `jti`

**Security Model**:
- API Gateway validates all JWT tokens
- Downstream services trust X-User-Id header
- Network isolation for internal services
- No JWT validation in User Service (gateway handles it)

### Fixed Issues

**Performance Optimizations**:
- Fixed logout performance issue (was loading all tokens)
- Fixed follow stats object creation issue (orElse → orElseGet)
- Added null safety checks for stats updates
- Marked optimization points for future enhancements

**Code Quality**:
- Added TODO markers for future Redis integration
- Added TODO markers for future Kafka integration
- Documented future OAuth2 integration points
- Comprehensive JavaDoc for public methods

### Documentation

**Created Documentation**:
- README.md - Main documentation hub
- ARCHITECTURE.md - System architecture and diagrams
- API_REFERENCE.md - Complete API reference with examples
- DEVELOPMENT_GUIDE.md - Setup and development guide
- CHANGELOG.md - This file
- database_indexes.sql - Performance optimization indexes
- API_TEST.sh - Automated test script (19 test cases)

### Technology Stack

- Java 21
- Spring Boot 3.5.10
- Spring Cloud Gateway 2025.0.1
- Spring Security with OAuth2 Resource Server
- Spring Data JPA
- PostgreSQL 15+
- Flyway for database migrations
- MapStruct 1.5.5 for DTO mapping
- Lombok for boilerplate reduction
- BCrypt for password hashing
- JWT (HS512) for token generation

### Migration Notes for v0.0.1

**Database Setup**:
```sql
CREATE DATABASE "auth-service";
CREATE DATABASE "user-service";
```

**Configuration**:
- Update `application.yml` with database credentials
- Set JWT signing key (512 bits minimum)
- Configure CORS allowed origins

**First Run**:
1. Database migrations run automatically (Flyway)
2. Default tables created
3. Ready for first user registration

**Performance Indexes**:
Run `database_indexes.sql` for optimal performance:
```bash
psql -U postgres -d auth-service -f database_indexes.sql
psql -U postgres -d user-service -f database_indexes.sql
```

---

## Future Roadmap

### Planned v0.2.0 - Redis Integration

**Caching**:
- User profile caching
- Token blacklist for logout
- Session caching
- Rate limiting

**Performance**:
- 10x faster profile lookups
- Instant token revocation
- Reduced database load

### Planned v0.3.0 - Kafka Integration

**Event-Driven Architecture**:
- Async notifications
- Service decoupling
- Event sourcing
- Real-time updates

**Topics**:
- `auth.user.registered`
- `user.profile.updated`
- `user.follow.created`
- `user.follow.deleted`

### Planned v1.0.0 - Additional Services

**New Services**:
- Feed Service (MongoDB) - Status/pulse management
- Chat Service (MongoDB + WebSocket) - Real-time messaging
- Notification Service (MongoDB) - Push notifications
- Search Service (PostgreSQL + Redis) - User and tag search

**Infrastructure**:
- Redis cluster for caching
- Kafka cluster for events
- MongoDB for flexible schema data
- Elasticsearch for search (optional)

---

## Breaking Changes

### v0.1.0

**Port Change**:
- API Gateway port changed from 8888 to 8188
- **Action Required**: Update all client configurations

**RBAC Model**:
- Removed database-driven RBAC tables
- **Action Required**: None (migration handles automatically)
- **Note**: Old role management endpoints removed

**Admin Endpoints**:
- Now require ADMIN role (previously no authentication)
- **Action Required**: Use admin credentials for role management

### v0.0.1

Initial release - no breaking changes.

---

## Security Advisories

### v0.1.0

No security issues reported.

### v0.0.1

**Recommendations**:
- Change default admin password immediately in production
- Use strong JWT signing key (512 bits minimum)
- Enable HTTPS in production
- Implement rate limiting (future)
- Add token blacklist with Redis (future)

---

**Changelog Format**: Based on [Keep a Changelog](https://keepachangelog.com/)  
**Versioning**: Follows [Semantic Versioning](https://semver.org/)  
**Last Updated**: 2026-01-22
