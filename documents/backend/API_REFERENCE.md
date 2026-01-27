# DayPulse API Reference

Complete API reference for DayPulse backend services with testing examples.

---

## Table of Contents

- [Authentication Standards](#authentication-standards)
- [Auth Service Endpoints](#auth-service-endpoints)
- [User Service Endpoints](#user-service-endpoints)
- [Error Codes](#error-codes)
- [Testing Guide](#testing-guide)

---

## Authentication Standards

### Token Handling

Day-Pulse follows industry-standard OAuth 2.0-style JWT authentication:
- **Access Tokens**: Short-lived JWTs (1 hour) for API authentication
- **Refresh Tokens**: Long-lived tokens (10 hours) for renewing access tokens
- **Token Transport**: Authorization header for access tokens, body for refresh tokens

### How to Send Tokens (Client → Server)

#### For Protected Endpoints (Authenticated Requests)

**ALWAYS use the `Authorization` header with Bearer scheme:**

```http
GET /api/v1/users/me
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIi...
```

**NEVER send access tokens in:**
- Request body
- Query parameters (vulnerable to logging and leakage)
- Custom headers (use standard `Authorization` header)

#### For Token Refresh

**Send refresh token in request body:**

```http
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "token": "refresh_token_here"
}
```

### How to Receive Tokens (Server → Client)

#### Login/Signup Response

Tokens are returned in the **response body**:

```json
{
  "code": 200,
  "result": {
    "user": {
      "id": "abcd-1234-efgh-5678",
      "email": "user@example.com",
      "isEmailVerified": false,
      "isSetupComplete": false
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

**Client Responsibilities:**
1. Extract `accessToken` and `refreshToken` from response
2. Store securely (sessionStorage, localStorage, or memory)
3. Include `accessToken` in `Authorization: Bearer <token>` header for all protected API calls
4. Use `refreshToken` to get new access token before expiration

### Token Standards

**Access Token (JWT)**:
- Algorithm: HS512 (HMAC with SHA-512)
- Expiration: 3600 seconds (1 hour)
- Transport: `Authorization: Bearer <token>` header
- Claims: `sub` (email), `userId`, `scope` (roles), `iss`, `exp`, `iat`, `jti`

**Refresh Token**:
- Algorithm: HS512
- Expiration: 36000 seconds (10 hours)
- Storage: MD5 hash in database
- Transport: Request body
- Rotation: Automatic on refresh (old token revoked)

---

## Auth Service Endpoints

Base URL: `http://localhost:8188/api/v1/auth`

### 1. Signup (Create Account)

**Endpoint**: `POST /auth/signup`  
**Authentication**: Not required  
**Standard**: OAuth 2.0 compliant

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
  "message": "Success",
  "result": {
    "success": true,
    "userId": "abcd-1234-efgh-5678",
    "email": "user@example.com"
  }
}
```

**Notes**:
- Password must be at least 8 characters
- Email must be unique
- User assigned ROLE_USER by default
- Auto-login tokens can be returned (implementation dependent)

---

### 2. Register (Alternative)

**Endpoint**: `POST /auth/register`  
**Authentication**: Not required  
**Status**: Deprecated (use `/auth/signup` instead)

**Request**:
```bash
curl -X POST http://localhost:8188/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.doe@example.com",
    "password": "password123"
  }'
```

**Response** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "success": true,
    "email": "john.doe@example.com"
  }
}
```

---

### 3. Login

**Endpoint**: `POST /auth/login`  
**Authentication**: Not required

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

**Error Responses**:
- 401 Unauthorized: Invalid email or password
- 1003: User not exists
- 1004: Unauthenticated

---

### 4. Refresh Token

**Endpoint**: `POST /auth/refresh`  
**Authentication**: Not required (refresh token in body)

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

**Notes**:
- Old refresh token is automatically revoked
- New access and refresh tokens are issued
- Token rotation enhances security

---

### 5. Token Introspection

**Endpoint**: `POST /auth/introspect`  
**Authentication**: Not required  
**Purpose**: Validate token (used by API Gateway)

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

**Invalid Token Response**:
```json
{
  "code": 1000,
  "result": {
    "valid": false
  }
}
```

---

### 6. Logout

**Endpoint**: `POST /auth/logout`  
**Authentication**: Required (Bearer token)

**Request**:
```bash
curl -X POST http://localhost:8188/api/v1/auth/logout \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
```

**Response** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "message": "Logout successful"
  }
}
```

**Notes**:
- Revokes all user's refresh tokens
- Access token remains valid until expiration (consider implementing token blacklist with Redis)
- User must re-login to get new tokens

---

### 7. Get My Info

**Endpoint**: `GET /users/my-info`  
**Authentication**: Required (Bearer token)  
**Note**: Part of auth-service (not user-service)

**Request**:
```bash
curl -X GET http://localhost:8188/api/v1/auth/users/my-info \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
```

**Response** (200 OK):
```json
{
  "code": 1000,
  "result": {
    "id": "abcd-1234-efgh-5678",
    "email": "user@example.com",
    "role": "USER",
    "isEmailVerified": false,
    "isSetupComplete": false
  }
}
```

---

## User Service Endpoints

Base URL: `http://localhost:8188/api/v1/users`

### 1. Get My Profile

**Endpoint**: `GET /users/me`  
**Authentication**: Required (Bearer token)

**Request**:
```bash
curl -X GET http://localhost:8188/api/v1/users/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
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
    "location": "San Francisco",
    "website": "https://johndoe.com",
    "timezone": "America/Los_Angeles",
    "language": "en",
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

---

### 2. Setup Profile (First Time)

**Endpoint**: `POST /users/me/setup`  
**Authentication**: Required (Bearer token)  
**Purpose**: Complete initial profile setup after registration

**Request**:
```bash
curl -X POST http://localhost:8188/api/v1/users/me/setup \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..." \
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
    "bio": "Software developer and tech enthusiast"
  }
}
```

**Notes**:
- Username must be unique
- Only allowed once per user
- Updates `isSetupComplete` flag in auth service

---

### 3. Update My Profile

**Endpoint**: `PATCH /users/me`  
**Authentication**: Required (Bearer token)  
**Note**: Supports partial updates

**Request**:
```bash
curl -X PATCH http://localhost:8188/api/v1/users/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe Updated",
    "bio": "Senior Software Engineer",
    "location": "New York"
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
    "location": "New York"
  }
}
```

**Updatable Fields**:
- `name`, `bio`, `avatarUrl`, `coverImageUrl`
- `location`, `website`, `birthDate`
- `timezone`, `language`

---

### 4. Get User by ID

**Endpoint**: `GET /users/{id}`  
**Authentication**: Required (Bearer token)

**Request**:
```bash
curl -X GET http://localhost:8188/api/v1/users/xyz789 \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
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

---

### 5. Follow User

**Endpoint**: `POST /users/{id}/follow`  
**Authentication**: Required (Bearer token)

**Request**:
```bash
curl -X POST http://localhost:8188/api/v1/users/xyz789/follow \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
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

**Notes**:
- Automatically updates follower and following counts
- Idempotent (multiple follows return 200 OK)
- Cannot follow yourself (returns error)

---

### 6. Unfollow User

**Endpoint**: `DELETE /users/{id}/follow`  
**Authentication**: Required (Bearer token)

**Request**:
```bash
curl -X DELETE http://localhost:8188/api/v1/users/xyz789/follow \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
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

---

### 7. Get User's Followers

**Endpoint**: `GET /users/{id}/followers`  
**Authentication**: Required (Bearer token)  
**Pagination**: Supported

**Request**:
```bash
curl -X GET "http://localhost:8188/api/v1/users/xyz789/followers?page=0&size=20" \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
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
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 150,
      "totalPages": 8
    }
  }
}
```

---

### 8. Get User's Following

**Endpoint**: `GET /users/{id}/following`  
**Authentication**: Required (Bearer token)  
**Pagination**: Supported

**Request**:
```bash
curl -X GET "http://localhost:8188/api/v1/users/xyz789/following?page=0&size=20" \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
```

**Response**: Same format as followers endpoint

---

### 9. Get Suggested Users

**Endpoint**: `GET /users/suggested`  
**Authentication**: Required (Bearer token)

**Request**:
```bash
curl -X GET http://localhost:8188/api/v1/users/suggested \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
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
    },
    {
      "id": "user2",
      "username": "suggested2",
      "name": "Suggested User Two",
      "avatarUrl": "https://example.com/user2.jpg"
    }
  ]
}
```

**Notes**:
- Currently returns random users not followed by requester
- Future: Implement recommendation algorithm

---

### 10. Get Available Users

**Endpoint**: `GET /users/available`  
**Authentication**: Required (Bearer token)

**Request**:
```bash
curl -X GET http://localhost:8188/api/v1/users/available \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
```

**Response**: Same format as suggested users

**Notes**:
- Returns all users except the requester
- Useful for user discovery

---

## Error Codes

### Standard HTTP Status Codes

| Status | Code | Meaning |
|--------|------|---------|
| 200 | 1000 | Success |
| 201 | 1000 | Created (successful signup) |
| 400 | - | Bad Request (invalid input) |
| 401 | 1006 | Unauthenticated (invalid/expired token) |
| 403 | 1007 | Forbidden (insufficient permissions) |
| 404 | 1003 | Not Found (user/resource doesn't exist) |
| 409 | 1002 | Conflict (email/username already exists) |
| 500 | 9999 | Internal Server Error |

### Custom Error Codes

| Code | Message | Description |
|------|---------|-------------|
| 1000 | Success | Request completed successfully |
| 1002 | User existed | Email or username already registered |
| 1003 | User not exists | User not found in database |
| 1004 | Unauthenticated | Invalid credentials |
| 1006 | Unauthenticated | Invalid or expired token |
| 1007 | You do not have permission | Insufficient permissions for action |
| 9999 | Uncategorized error | Internal server error |

### Error Response Format

```json
{
  "code": 1006,
  "message": "Unauthenticated"
}
```

With details (optional):
```json
{
  "code": 1002,
  "message": "User existed",
  "details": {
    "field": "email",
    "value": "user@example.com"
  }
}
```

---

## Testing Guide

### Complete Test Flow

This section provides a complete end-to-end test sequence for all API endpoints.

#### Prerequisites

1. All services running:
   - Auth Service: `http://localhost:8180`
   - User Service: `http://localhost:8181`
   - API Gateway: `http://localhost:8188`

2. PostgreSQL databases created:
   - `auth-service`
   - `user-service`

#### Test Sequence

**1. Register User 1**:
```bash
curl -X POST http://localhost:8188/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email": "john.doe@example.com", "password": "password123"}'
```

**2. Register User 2**:
```bash
curl -X POST http://localhost:8188/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email": "jane.smith@example.com", "password": "password456"}'
```

**3. Login User 1** (save the accessToken):
```bash
curl -X POST http://localhost:8188/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "john.doe@example.com", "password": "password123"}'
```

**4. Login User 2** (save the accessToken):
```bash
curl -X POST http://localhost:8188/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "jane.smith@example.com", "password": "password456"}'
```

**5. Setup Profile User 1**:
```bash
curl -X POST http://localhost:8188/api/v1/users/me/setup \
  -H "Authorization: Bearer <USER1_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"username": "johndoe", "name": "John Doe", "bio": "Developer"}'
```

**6. Setup Profile User 2**:
```bash
curl -X POST http://localhost:8188/api/v1/users/me/setup \
  -H "Authorization: Bearer <USER2_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"username": "janesmith", "name": "Jane Smith", "bio": "Designer"}'
```

**7. Get My Profile (User 1)**:
```bash
curl -X GET http://localhost:8188/api/v1/users/me \
  -H "Authorization: Bearer <USER1_TOKEN>"
```

**8. Update My Profile (User 1)**:
```bash
curl -X PATCH http://localhost:8188/api/v1/users/me \
  -H "Authorization: Bearer <USER1_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"bio": "Senior Developer", "location": "San Francisco"}'
```

**9. Get User 2 by ID** (save USER2_ID from login response):
```bash
curl -X GET http://localhost:8188/api/v1/users/<USER2_ID> \
  -H "Authorization: Bearer <USER1_TOKEN>"
```

**10. User 1 Follows User 2**:
```bash
curl -X POST http://localhost:8188/api/v1/users/<USER2_ID>/follow \
  -H "Authorization: Bearer <USER1_TOKEN>"
```

**11. Get User 2's Followers**:
```bash
curl -X GET "http://localhost:8188/api/v1/users/<USER2_ID>/followers?page=0&size=20" \
  -H "Authorization: Bearer <USER1_TOKEN>"
```

**12. Get User 1's Following List**:
```bash
curl -X GET "http://localhost:8188/api/v1/users/<USER1_ID>/following?page=0&size=20" \
  -H "Authorization: Bearer <USER1_TOKEN>"
```

**13. Get Suggested Users**:
```bash
curl -X GET http://localhost:8188/api/v1/users/suggested \
  -H "Authorization: Bearer <USER1_TOKEN>"
```

**14. Get Available Users**:
```bash
curl -X GET http://localhost:8188/api/v1/users/available \
  -H "Authorization: Bearer <USER1_TOKEN>"
```

**15. Token Introspection**:
```bash
curl -X POST http://localhost:8188/api/v1/auth/introspect \
  -H "Content-Type: application/json" \
  -d '{"token": "<USER1_TOKEN>"}'
```

**16. Refresh Token**:
```bash
curl -X POST http://localhost:8188/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"token": "<USER1_REFRESH_TOKEN>"}'
```

**17. User 1 Unfollows User 2**:
```bash
curl -X DELETE http://localhost:8188/api/v1/users/<USER2_ID>/follow \
  -H "Authorization: Bearer <USER1_TOKEN>"
```

**18. Logout User 1**:
```bash
curl -X POST http://localhost:8188/api/v1/auth/logout \
  -H "Authorization: Bearer <USER1_TOKEN>"
```

**19. Try Access After Logout** (should fail):
```bash
curl -X GET http://localhost:8188/api/v1/users/me \
  -H "Authorization: Bearer <USER1_TOKEN>"
```

### Testing Checklist

- [ ] User registration works
- [ ] User login returns tokens
- [ ] Profile setup creates profile
- [ ] Profile update modifies fields
- [ ] Get profile returns correct data
- [ ] Get user by ID returns other users
- [ ] Follow creates relationship
- [ ] Unfollow removes relationship
- [ ] Followers list shows correct users
- [ ] Following list shows correct users
- [ ] Suggested users returns recommendations
- [ ] Token refresh generates new tokens
- [ ] Token introspection validates tokens
- [ ] Logout revokes refresh tokens
- [ ] Access after logout fails

### Automated Testing

Use the provided test script:

```bash
# Make script executable
chmod +x API_TEST.sh

# Run all tests
./API_TEST.sh
```

The script tests all 19 scenarios automatically and shows color-coded results.

---

**Last Updated**: 2026-01-22  
**Version**: 0.1.0  
**Base URL**: `http://localhost:8188/api/v1`
