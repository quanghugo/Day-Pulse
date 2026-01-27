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
TIMESTAMP=$(date +%s)
REGISTER_RESPONSE=$(curl -s -X POST http://localhost:8188/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"test-${TIMESTAMP}@example.com\",
    \"password\": \"TestPassword123!\"
  }")

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

# Test token introspection
echo "5. Testing token introspection..."
INTROSPECT_RESPONSE=$(curl -s -X POST http://localhost:8188/api/v1/auth/introspect \
  -H "Content-Type: application/json" \
  -d "{
    \"token\": \"$ACCESS_TOKEN\"
  }")

if echo "$INTROSPECT_RESPONSE" | grep -q '"valid":true'; then
    echo "✓ Token introspection successful"
else
    echo "✗ Token introspection failed: $INTROSPECT_RESPONSE"
    exit 1
fi

echo ""
echo "=== All tests passed! ==="
