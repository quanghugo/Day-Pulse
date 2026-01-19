# DayPulse API Testing with cURL

Complete API test collection for first-time project startup.

## Prerequisites

1. All services must be running:
   - Auth Service: `http://localhost:8080`
   - User Service: `http://localhost:8081`
   - API Gateway: `http://localhost:8888`

2. PostgreSQL databases created:
   - `auth-service`
   - `user-service`

## Base URLs

```
Gateway: http://localhost:8888/api/v1
Auth: http://localhost:8888/api/v1/auth
Users: http://localhost:8888/api/v1/users
```

---

## 1. Register User 1

```bash
curl -X POST http://localhost:8888/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.doe@example.com",
    "password": "password123"
  }'
```

**Expected Response:**
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

## 2. Register User 2

```bash
curl -X POST http://localhost:8888/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "jane.smith@example.com",
    "password": "password456"
  }'
```

---

## 3. Login User 1

```bash
curl -X POST http://localhost:8888/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.doe@example.com",
    "password": "password123"
  }'
```

**Expected Response:**
```json
{
  "code": 1000,
  "result": {
    "user": {
      "id": "uuid-here",
      "email": "john.doe@example.com",
      "isEmailVerified": false,
      "isSetupComplete": false
    },
    "tokens": {
      "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
      "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
    }
  }
}
```

**Save these values:**
- `USER1_ACCESS_TOKEN` = accessToken
- `USER1_REFRESH_TOKEN` = refreshToken
- `USER1_ID` = user.id

---

## 4. Login User 2

```bash
curl -X POST http://localhost:8888/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "jane.smith@example.com",
    "password": "password456"
  }'
```

**Save these values:**
- `USER2_ACCESS_TOKEN` = accessToken
- `USER2_ID` = user.id

---

## 5. Setup Profile for User 1

```bash
curl -X POST http://localhost:8888/api/v1/users/me/setup \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_USER1_ACCESS_TOKEN" \
  -d '{
    "username": "johndoe",
    "name": "John Doe",
    "bio": "Software developer and tech enthusiast"
  }'
```

**Expected Response:**
```json
{
  "code": 1000,
  "result": {
    "id": "uuid",
    "username": "johndoe",
    "name": "John Doe",
    "bio": "Software developer and tech enthusiast",
    "streak": 0,
    "isOnline": false
  }
}
```

---

## 6. Setup Profile for User 2

```bash
curl -X POST http://localhost:8888/api/v1/users/me/setup \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_USER2_ACCESS_TOKEN" \
  -d '{
    "username": "janesmith",
    "name": "Jane Smith",
    "bio": "Designer and creative thinker"
  }'
```

---

## 7. Get My Profile (User 1)

```bash
curl -X GET http://localhost:8888/api/v1/users/me \
  -H "Authorization: Bearer YOUR_USER1_ACCESS_TOKEN"
```

---

## 8. Update My Profile (User 1)

```bash
curl -X PATCH http://localhost:8888/api/v1/users/me \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_USER1_ACCESS_TOKEN" \
  -d '{
    "bio": "Updated bio - Full stack developer",
    "timezone": "Asia/Ho_Chi_Minh",
    "language": "en"
  }'
```

---

## 9. Get User by ID (Get User 2's Profile)

```bash
curl -X GET http://localhost:8888/api/v1/users/YOUR_USER2_ID \
  -H "Authorization: Bearer YOUR_USER1_ACCESS_TOKEN"
```

---

## 10. User 1 Follows User 2

```bash
curl -X POST http://localhost:8888/api/v1/users/YOUR_USER2_ID/follow \
  -H "Authorization: Bearer YOUR_USER1_ACCESS_TOKEN"
```

**Expected Response:**
```json
{
  "code": 1000,
  "result": {
    "success": true,
    "message": "Successfully followed user"
  }
}
```

---

## 11. Get User 2's Followers

```bash
curl -X GET "http://localhost:8888/api/v1/users/YOUR_USER2_ID/followers?page=0&size=20" \
  -H "Authorization: Bearer YOUR_USER1_ACCESS_TOKEN"
```

**Expected Response:**
```json
{
  "code": 1000,
  "result": {
    "content": [
      {
        "id": "user1-uuid",
        "username": "johndoe",
        "name": "John Doe",
        "avatarUrl": null,
        "isOnline": false
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "size": 20,
    "number": 0
  }
}
```

---

## 12. Get User 1's Following List

```bash
curl -X GET "http://localhost:8888/api/v1/users/YOUR_USER1_ID/following?page=0&size=20" \
  -H "Authorization: Bearer YOUR_USER1_ACCESS_TOKEN"
```

---

## 13. Get Suggested Users

```bash
curl -X GET http://localhost:8888/api/v1/users/suggested \
  -H "Authorization: Bearer YOUR_USER1_ACCESS_TOKEN"
```

---

## 14. Get Available Users

```bash
curl -X GET http://localhost:8888/api/v1/users/available \
  -H "Authorization: Bearer YOUR_USER1_ACCESS_TOKEN"
```

---

## 15. Token Introspection

```bash
curl -X POST http://localhost:8888/api/v1/auth/introspect \
  -H "Content-Type: application/json" \
  -d '{
    "token": "YOUR_USER1_ACCESS_TOKEN"
  }'
```

**Expected Response:**
```json
{
  "code": 1000,
  "result": {
    "valid": true
  }
}
```

---

## 16. Refresh Token

```bash
curl -X POST http://localhost:8888/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "token": "YOUR_USER1_REFRESH_TOKEN"
  }'
```

**Expected Response:**
```json
{
  "code": 1000,
  "result": {
    "user": {
      "id": "uuid",
      "email": "john.doe@example.com"
    },
    "tokens": {
      "accessToken": "new-access-token",
      "refreshToken": "new-refresh-token"
    }
  }
}
```

**Note:** Old refresh token is revoked. Use new tokens going forward.

---

## 17. User 1 Unfollows User 2

```bash
curl -X DELETE http://localhost:8888/api/v1/users/YOUR_USER2_ID/follow \
  -H "Authorization: Bearer YOUR_USER1_ACCESS_TOKEN"
```

**Expected Response:**
```json
{
  "code": 1000,
  "result": {
    "success": true,
    "message": "Successfully unfollowed user"
  }
}
```

---

## 18. Logout

```bash
curl -X POST http://localhost:8888/api/v1/auth/logout \
  -H "Content-Type: application/json" \
  -d '{
    "token": "YOUR_USER1_ACCESS_TOKEN"
  }'
```

---

## 19. Try to Access After Logout (Should Fail)

```bash
curl -X GET http://localhost:8888/api/v1/users/me \
  -H "Authorization: Bearer YOUR_USER1_ACCESS_TOKEN"
```

**Expected:** Unauthorized response or empty result

---

## Error Responses

### 401 Unauthorized
```json
{
  "code": 106,
  "message": "Unauthenticated user"
}
```

### 400 Bad Request (User Already Exists)
```json
{
  "code": 102,
  "message": "User existed"
}
```

### 404 Not Found (User Not Found)
```json
{
  "code": 105,
  "message": "User not found"
}
```

---

## PowerShell Examples (Windows)

### Register
```powershell
$body = @{
    email = "john.doe@example.com"
    password = "password123"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8888/api/v1/auth/register" `
  -Method Post `
  -ContentType "application/json" `
  -Body $body
```

### Login
```powershell
$body = @{
    email = "john.doe@example.com"
    password = "password123"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://localhost:8888/api/v1/auth/login" `
  -Method Post `
  -ContentType "application/json" `
  -Body $body

$accessToken = $response.result.tokens.accessToken
$userId = $response.result.user.id
```

### Setup Profile
```powershell
$body = @{
    username = "johndoe"
    name = "John Doe"
    bio = "Software developer"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8888/api/v1/users/me/setup" `
  -Method Post `
  -ContentType "application/json" `
  -Headers @{Authorization="Bearer $accessToken"} `
  -Body $body
```

---

## Testing Checklist

- [ ] Both users can register
- [ ] Both users can login and receive tokens
- [ ] Both users can setup profiles
- [ ] Users can view each other's profiles
- [ ] Follow/unfollow works correctly
- [ ] Followers and following lists are updated
- [ ] Token refresh works
- [ ] Tokens are invalidated after logout
- [ ] Suggested users endpoint works
- [ ] Profile updates work correctly

---

## Common Issues

### 1. **Connection Refused**
- Check if all services are running
- Verify port numbers (8080, 8081, 8888)

### 2. **Database Connection Error**
- Ensure PostgreSQL is running
- Verify databases exist: `auth-service`, `user-service`
- Check credentials in application.yaml

### 3. **401 Unauthorized on Protected Endpoints**
- Verify token is included in Authorization header
- Check token format: `Bearer <token>`
- Token may have expired (default: 1 hour)

### 4. **Username Already Exists**
- Use different username or clear database
- Check if profile was already setup

### 5. **Cannot Follow Yourself**
- Ensure followerId and followingId are different users

---

## Quick Start Script (Save as test.sh)

```bash
#!/bin/bash

# Register and login user
REGISTER=$(curl -s -X POST http://localhost:8888/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test123"}')

LOGIN=$(curl -s -X POST http://localhost:8888/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test123"}')

TOKEN=$(echo $LOGIN | jq -r '.result.tokens.accessToken')

echo "Token: $TOKEN"

# Setup profile
curl -X POST http://localhost:8888/api/v1/users/me/setup \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"username":"testuser","name":"Test User","bio":"Test bio"}'

# Get profile
curl -X GET http://localhost:8888/api/v1/users/me \
  -H "Authorization: Bearer $TOKEN"
```

Make executable: `chmod +x test.sh`
Run: `./test.sh`
