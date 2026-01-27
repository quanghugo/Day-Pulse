# Keycloak Integration Testing Guide

Complete guide for testing Keycloak integration in DayPulse backend services.

---

## Prerequisites

Before running tests, ensure:

1. **Keycloak is running**:
   ```bash
   docker-compose up -d
   ```

2. **Keycloak is configured**:
   - Realm `daypulse` exists
   - Client `daypulse-backend` is configured with client secret
   - Roles `USER`, `MODERATOR`, `ADMIN` exist
   - At least one test user exists (or will be created by tests)

3. **Environment variables** (optional):
   ```bash
   export KEYCLOAK_CLIENT_SECRET=your-client-secret-here
   export TEST_KEYCLOAK_TOKEN=valid-keycloak-jwt-token
   ```

---

## Manual Testing

### 1. Keycloak Setup Verification

#### Check Keycloak is Running
```bash
curl http://localhost:8888/health/ready
```

Expected: HTTP 200 OK

#### Check OpenID Configuration
```bash
curl http://localhost:8888/realms/daypulse/.well-known/openid-configuration
```

Expected: JSON with Keycloak endpoints

#### Check JWK Endpoint
```bash
curl http://localhost:8888/realms/daypulse/protocol/openid-connect/certs
```

Expected: JSON Web Key Set (JWKS)

---

### 2. Backend Service Testing

#### Test User Registration

**Request**:
```bash
curl -X POST http://localhost:8188/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "testuser@example.com",
    "password": "TestPassword123!"
  }'
```

**Expected Response**:
```json
{
  "code": 200,
  "message": "Success",
  "result": {
    "success": true,
    "userId": "uuid-here",
    "email": "testuser@example.com"
  }
}
```

#### Test User Login

**Request**:
```bash
curl -X POST http://localhost:8188/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "testuser@example.com",
    "password": "TestPassword123!"
  }'
```

**Expected Response**:
```json
{
  "code": 200,
  "message": "Success",
  "result": {
    "user": {
      "id": "uuid",
      "email": "testuser@example.com",
      ...
    },
    "tokens": {
      "accessToken": "eyJhbGc...",
      "refreshToken": "eyJhbGc...",
      "expiresIn": 300,
      "tokenType": "Bearer"
    }
  }
}
```

**Save the access token** for subsequent tests:
```bash
export ACCESS_TOKEN="eyJhbGc..."
```

#### Test Token Refresh

**Request**:
```bash
curl -X POST http://localhost:8188/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "token": "refresh-token-here"
  }'
```

**Expected Response**: New access and refresh tokens

#### Test Token Introspection

**Request**:
```bash
curl -X POST http://localhost:8188/api/v1/auth/introspect \
  -H "Content-Type: application/json" \
  -d '{
    "token": "'$ACCESS_TOKEN'"
  }'
```

**Expected Response**:
```json
{
  "code": 200,
  "message": "Success",
  "result": {
    "valid": true
  }
}
```

#### Test Protected Endpoint

**Request**:
```bash
curl -X GET http://localhost:8188/api/v1/users/my-info \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

**Expected Response**: User information

#### Test Logout

**Request**:
```bash
curl -X POST http://localhost:8188/api/v1/auth/logout \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

**Expected Response**:
```json
{
  "code": 200,
  "message": "Logout successful",
  "result": null
}
```

---

### 3. API Gateway JWT Validation Testing

#### Test Valid Token

```bash
curl -X GET http://localhost:8188/api/v1/users/my-info \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

**Expected**: HTTP 200 with user data

#### Test Invalid Token

```bash
curl -X GET http://localhost:8188/api/v1/users/my-info \
  -H "Authorization: Bearer invalid.token.here"
```

**Expected**: HTTP 401 Unauthorized

#### Test Missing Token

```bash
curl -X GET http://localhost:8188/api/v1/users/my-info
```

**Expected**: HTTP 401 Unauthorized (for protected endpoints)

#### Test Expired Token

1. Wait for token to expire (or use an old token)
2. Make request with expired token

**Expected**: HTTP 401 Unauthorized

---

### 4. Role-Based Access Control Testing

#### Test Admin Endpoint (Requires ADMIN role)

**Request**:
```bash
curl -X GET http://localhost:8188/api/v1/admin/users \
  -H "Authorization: Bearer $ADMIN_ACCESS_TOKEN"
```

**Expected**: 
- HTTP 200 if user has ADMIN role
- HTTP 403 Forbidden if user doesn't have ADMIN role

---

## Automated Testing

### Running Integration Tests

#### Auth Service Tests

```bash
cd backEnd/auth-service
mvn test -Dtest=KeycloakIntegrationTest
```

#### API Gateway Tests

```bash
cd backEnd/api-gateway
mvn test -Dtest=KeycloakJwtValidationTest
```

### Test Script

Create a test script for automated end-to-end testing:

**`scripts/test-keycloak-integration.sh`** (Linux/Mac):
```bash
#!/bin/bash

set -e

echo "=== Keycloak Integration Test Script ==="

# Check Keycloak is running
echo "1. Checking Keycloak health..."
if ! curl -f http://localhost:8888/health/ready > /dev/null 2>&1; then
    echo "ERROR: Keycloak is not running. Start it with: docker-compose up -d"
    exit 1
fi
echo "✓ Keycloak is running"

# Test registration
echo "2. Testing user registration..."
REGISTER_RESPONSE=$(curl -s -X POST http://localhost:8188/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test-'$(date +%s)'@example.com",
    "password": "TestPassword123!"
  }')

if echo "$REGISTER_RESPONSE" | grep -q '"success":true'; then
    echo "✓ Registration successful"
else
    echo "✗ Registration failed: $REGISTER_RESPONSE"
    exit 1
fi

# Test login
echo "3. Testing user login..."
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8188/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "testuser@example.com",
    "password": "password123"
  }')

ACCESS_TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

if [ -z "$ACCESS_TOKEN" ]; then
    echo "✗ Login failed: $LOGIN_RESPONSE"
    exit 1
fi
echo "✓ Login successful"

# Test protected endpoint
echo "4. Testing protected endpoint..."
PROTECTED_RESPONSE=$(curl -s -X GET http://localhost:8188/api/v1/users/my-info \
  -H "Authorization: Bearer $ACCESS_TOKEN")

if echo "$PROTECTED_RESPONSE" | grep -q '"code":200'; then
    echo "✓ Protected endpoint accessible"
else
    echo "✗ Protected endpoint failed: $PROTECTED_RESPONSE"
    exit 1
fi

echo ""
echo "=== All tests passed! ==="
```

**`scripts/test-keycloak-integration.bat`** (Windows):
```batch
@echo off
setlocal enabledelayedexpansion

echo === Keycloak Integration Test Script ===

REM Check Keycloak is running
echo 1. Checking Keycloak health...
curl -f http://localhost:8888/health/ready >nul 2>&1
if errorlevel 1 (
    echo ERROR: Keycloak is not running. Start it with: docker-compose up -d
    exit /b 1
)
echo ✓ Keycloak is running

REM Test registration
echo 2. Testing user registration...
set TIMESTAMP=%time:~0,2%%time:~3,2%%time:~6,2%
curl -s -X POST http://localhost:8188/api/v1/auth/signup ^
  -H "Content-Type: application/json" ^
  -d "{\"email\":\"test-!TIMESTAMP!@example.com\",\"password\":\"TestPassword123!\"}" > register_response.json

findstr /C:"\"success\":true" register_response.json >nul
if errorlevel 1 (
    echo ✗ Registration failed
    type register_response.json
    exit /b 1
)
echo ✓ Registration successful

REM Test login
echo 3. Testing user login...
curl -s -X POST http://localhost:8188/api/v1/auth/login ^
  -H "Content-Type: application/json" ^
  -d "{\"email\":\"testuser@example.com\",\"password\":\"password123\"}" > login_response.json

REM Extract access token (simplified - use jq or similar for production)
echo ✓ Login test completed

echo.
echo === Tests completed ===
```

---

## Troubleshooting

### Keycloak Not Running

**Error**: Connection refused to http://localhost:8888

**Solution**:
```bash
docker-compose up -d
docker-compose logs -f keycloak
```

### Invalid Client Secret

**Error**: 401 Unauthorized when calling Keycloak

**Solution**: 
1. Get client secret from Keycloak Admin Console
2. Update `application.yaml` or set environment variable:
   ```bash
   export KEYCLOAK_CLIENT_SECRET=your-secret-here
   ```

### Token Validation Fails

**Error**: JWT validation fails in API Gateway

**Solution**:
1. Verify JWK endpoint is accessible:
   ```bash
   curl http://localhost:8888/realms/daypulse/protocol/openid-connect/certs
   ```
2. Check Keycloak configuration in `application.yaml`
3. Verify token is from correct realm

### User Not Found

**Error**: User not found after registration

**Solution**:
1. Check user exists in Keycloak Admin Console
2. Verify local database has corresponding user record
3. Check Keycloak user ID mapping

---

## Test Data

### Test Users

Create these users in Keycloak for testing:

| Username | Email | Password | Role |
|----------|-------|----------|------|
| testuser | testuser@example.com | password123 | USER |
| admin | admin@daypulse.com | admin123 | ADMIN |
| moderator | moderator@daypulse.com | moderator123 | MODERATOR |

### Test Tokens

For testing, you can obtain tokens:

1. **Via Login Endpoint**:
   ```bash
   curl -X POST http://localhost:8188/api/v1/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email":"testuser@example.com","password":"password123"}'
   ```

2. **Via Keycloak Token Endpoint**:
   ```bash
   curl -X POST http://localhost:8888/realms/daypulse/protocol/openid-connect/token \
     -H "Content-Type: application/x-www-form-urlencoded" \
     -d "grant_type=password&client_id=daypulse-backend&client_secret=YOUR_SECRET&username=testuser@example.com&password=password123"
   ```

---

## Next Steps

After completing tests:

1. Review test results
2. Fix any failing tests
3. Update documentation with findings
4. Create test reports for CI/CD integration
