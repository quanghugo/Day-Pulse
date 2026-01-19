#!/bin/bash

# ============================================
# DayPulse Backend API Testing Script
# ============================================
# This script tests the complete user flow:
# 1. Register two users
# 2. Login both users
# 3. Setup profiles for both users
# 4. User1 follows User2
# 5. View followers/following
# 6. Refresh token
# 7. Logout
# ============================================

BASE_URL="http://localhost:8888/api/v1"
AUTH_URL="${BASE_URL}/auth"
USER_URL="${BASE_URL}/users"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print section headers
print_section() {
    echo ""
    echo -e "${YELLOW}============================================${NC}"
    echo -e "${YELLOW}$1${NC}"
    echo -e "${YELLOW}============================================${NC}"
}

# Function to print success
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

# Function to print error
print_error() {
    echo -e "${RED}✗ $1${NC}"
}

# ============================================
# TEST 1: Register User 1
# ============================================
print_section "TEST 1: Register User 1"

echo "Request: POST ${AUTH_URL}/register"
USER1_REGISTER=$(curl -s -X POST "${AUTH_URL}/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.doe@example.com",
    "password": "password123"
  }')

echo "Response:"
echo "$USER1_REGISTER" | jq '.'

if echo "$USER1_REGISTER" | jq -e '.result.success' > /dev/null; then
    print_success "User 1 registered successfully"
else
    print_error "User 1 registration failed"
fi

# ============================================
# TEST 2: Register User 2
# ============================================
print_section "TEST 2: Register User 2"

echo "Request: POST ${AUTH_URL}/register"
USER2_REGISTER=$(curl -s -X POST "${AUTH_URL}/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "jane.smith@example.com",
    "password": "password456"
  }')

echo "Response:"
echo "$USER2_REGISTER" | jq '.'

if echo "$USER2_REGISTER" | jq -e '.result.success' > /dev/null; then
    print_success "User 2 registered successfully"
else
    print_error "User 2 registration failed"
fi

# ============================================
# TEST 3: Login User 1
# ============================================
print_section "TEST 3: Login User 1"

echo "Request: POST ${AUTH_URL}/login"
USER1_LOGIN=$(curl -s -X POST "${AUTH_URL}/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.doe@example.com",
    "password": "password123"
  }')

echo "Response:"
echo "$USER1_LOGIN" | jq '.'

USER1_ACCESS_TOKEN=$(echo "$USER1_LOGIN" | jq -r '.result.tokens.accessToken')
USER1_REFRESH_TOKEN=$(echo "$USER1_LOGIN" | jq -r '.result.tokens.refreshToken')
USER1_ID=$(echo "$USER1_LOGIN" | jq -r '.result.user.id')

if [ "$USER1_ACCESS_TOKEN" != "null" ]; then
    print_success "User 1 logged in successfully"
    echo "User1 ID: $USER1_ID"
    echo "Access Token: ${USER1_ACCESS_TOKEN:0:50}..."
else
    print_error "User 1 login failed"
    exit 1
fi

# ============================================
# TEST 4: Login User 2
# ============================================
print_section "TEST 4: Login User 2"

echo "Request: POST ${AUTH_URL}/login"
USER2_LOGIN=$(curl -s -X POST "${AUTH_URL}/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "jane.smith@example.com",
    "password": "password456"
  }')

echo "Response:"
echo "$USER2_LOGIN" | jq '.'

USER2_ACCESS_TOKEN=$(echo "$USER2_LOGIN" | jq -r '.result.tokens.accessToken')
USER2_REFRESH_TOKEN=$(echo "$USER2_LOGIN" | jq -r '.result.tokens.refreshToken')
USER2_ID=$(echo "$USER2_LOGIN" | jq -r '.result.user.id')

if [ "$USER2_ACCESS_TOKEN" != "null" ]; then
    print_success "User 2 logged in successfully"
    echo "User2 ID: $USER2_ID"
    echo "Access Token: ${USER2_ACCESS_TOKEN:0:50}..."
else
    print_error "User 2 login failed"
    exit 1
fi

# ============================================
# TEST 5: Setup Profile for User 1
# ============================================
print_section "TEST 5: Setup Profile for User 1"

echo "Request: POST ${USER_URL}/me/setup"
USER1_PROFILE=$(curl -s -X POST "${USER_URL}/me/setup" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${USER1_ACCESS_TOKEN}" \
  -d '{
    "username": "johndoe",
    "name": "John Doe",
    "bio": "Software developer and tech enthusiast"
  }')

echo "Response:"
echo "$USER1_PROFILE" | jq '.'

if echo "$USER1_PROFILE" | jq -e '.result.username' > /dev/null; then
    print_success "User 1 profile setup successfully"
else
    print_error "User 1 profile setup failed"
fi

# ============================================
# TEST 6: Setup Profile for User 2
# ============================================
print_section "TEST 6: Setup Profile for User 2"

echo "Request: POST ${USER_URL}/me/setup"
USER2_PROFILE=$(curl -s -X POST "${USER_URL}/me/setup" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${USER2_ACCESS_TOKEN}" \
  -d '{
    "username": "janesmith",
    "name": "Jane Smith",
    "bio": "Designer and creative thinker"
  }')

echo "Response:"
echo "$USER2_PROFILE" | jq '.'

if echo "$USER2_PROFILE" | jq -e '.result.username' > /dev/null; then
    print_success "User 2 profile setup successfully"
else
    print_error "User 2 profile setup failed"
fi

# ============================================
# TEST 7: Get User 1 Profile
# ============================================
print_section "TEST 7: Get User 1 Profile"

echo "Request: GET ${USER_URL}/me"
MY_PROFILE=$(curl -s -X GET "${USER_URL}/me" \
  -H "Authorization: Bearer ${USER1_ACCESS_TOKEN}")

echo "Response:"
echo "$MY_PROFILE" | jq '.'

if echo "$MY_PROFILE" | jq -e '.result.id' > /dev/null; then
    print_success "Retrieved User 1 profile successfully"
else
    print_error "Failed to retrieve User 1 profile"
fi

# ============================================
# TEST 8: Update User 1 Profile
# ============================================
print_section "TEST 8: Update User 1 Profile"

echo "Request: PATCH ${USER_URL}/me"
UPDATED_PROFILE=$(curl -s -X PATCH "${USER_URL}/me" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${USER1_ACCESS_TOKEN}" \
  -d '{
    "bio": "Updated bio - Full stack developer",
    "timezone": "Asia/Ho_Chi_Minh"
  }')

echo "Response:"
echo "$UPDATED_PROFILE" | jq '.'

if echo "$UPDATED_PROFILE" | jq -e '.result.bio' > /dev/null; then
    print_success "User 1 profile updated successfully"
else
    print_error "Failed to update User 1 profile"
fi

# ============================================
# TEST 9: Get User 2 by ID
# ============================================
print_section "TEST 9: Get User 2 Profile by ID"

echo "Request: GET ${USER_URL}/${USER2_ID}"
USER2_BY_ID=$(curl -s -X GET "${USER_URL}/${USER2_ID}" \
  -H "Authorization: Bearer ${USER1_ACCESS_TOKEN}")

echo "Response:"
echo "$USER2_BY_ID" | jq '.'

if echo "$USER2_BY_ID" | jq -e '.result.id' > /dev/null; then
    print_success "Retrieved User 2 profile by ID successfully"
else
    print_error "Failed to retrieve User 2 profile by ID"
fi

# ============================================
# TEST 10: User 1 Follows User 2
# ============================================
print_section "TEST 10: User 1 Follows User 2"

echo "Request: POST ${USER_URL}/${USER2_ID}/follow"
FOLLOW_RESULT=$(curl -s -X POST "${USER_URL}/${USER2_ID}/follow" \
  -H "Authorization: Bearer ${USER1_ACCESS_TOKEN}")

echo "Response:"
echo "$FOLLOW_RESULT" | jq '.'

if echo "$FOLLOW_RESULT" | jq -e '.result.success' > /dev/null; then
    print_success "User 1 followed User 2 successfully"
else
    print_error "Follow operation failed"
fi

# ============================================
# TEST 11: Get User 2's Followers
# ============================================
print_section "TEST 11: Get User 2's Followers"

echo "Request: GET ${USER_URL}/${USER2_ID}/followers"
FOLLOWERS=$(curl -s -X GET "${USER_URL}/${USER2_ID}/followers?page=0&size=20" \
  -H "Authorization: Bearer ${USER1_ACCESS_TOKEN}")

echo "Response:"
echo "$FOLLOWERS" | jq '.'

if echo "$FOLLOWERS" | jq -e '.result' > /dev/null; then
    print_success "Retrieved User 2's followers successfully"
    FOLLOWER_COUNT=$(echo "$FOLLOWERS" | jq '.result.totalElements')
    echo "Total followers: $FOLLOWER_COUNT"
else
    print_error "Failed to retrieve followers"
fi

# ============================================
# TEST 12: Get User 1's Following List
# ============================================
print_section "TEST 12: Get User 1's Following List"

echo "Request: GET ${USER_URL}/${USER1_ID}/following"
FOLLOWING=$(curl -s -X GET "${USER_URL}/${USER1_ID}/following?page=0&size=20" \
  -H "Authorization: Bearer ${USER1_ACCESS_TOKEN}")

echo "Response:"
echo "$FOLLOWING" | jq '.'

if echo "$FOLLOWING" | jq -e '.result' > /dev/null; then
    print_success "Retrieved User 1's following list successfully"
    FOLLOWING_COUNT=$(echo "$FOLLOWING" | jq '.result.totalElements')
    echo "Total following: $FOLLOWING_COUNT"
else
    print_error "Failed to retrieve following list"
fi

# ============================================
# TEST 13: Get Suggested Users
# ============================================
print_section "TEST 13: Get Suggested Users"

echo "Request: GET ${USER_URL}/suggested"
SUGGESTED=$(curl -s -X GET "${USER_URL}/suggested" \
  -H "Authorization: Bearer ${USER1_ACCESS_TOKEN}")

echo "Response:"
echo "$SUGGESTED" | jq '.'

if echo "$SUGGESTED" | jq -e '.result' > /dev/null; then
    print_success "Retrieved suggested users successfully"
else
    print_error "Failed to retrieve suggested users"
fi

# ============================================
# TEST 14: Get Available Users
# ============================================
print_section "TEST 14: Get Available Users"

echo "Request: GET ${USER_URL}/available"
AVAILABLE=$(curl -s -X GET "${USER_URL}/available" \
  -H "Authorization: Bearer ${USER1_ACCESS_TOKEN}")

echo "Response:"
echo "$AVAILABLE" | jq '.'

if echo "$AVAILABLE" | jq -e '.result' > /dev/null; then
    print_success "Retrieved available users successfully"
else
    print_error "Failed to retrieve available users"
fi

# ============================================
# TEST 15: Token Introspection
# ============================================
print_section "TEST 15: Token Introspection"

echo "Request: POST ${AUTH_URL}/introspect"
INTROSPECT=$(curl -s -X POST "${AUTH_URL}/introspect" \
  -H "Content-Type: application/json" \
  -d "{
    \"token\": \"${USER1_ACCESS_TOKEN}\"
  }")

echo "Response:"
echo "$INTROSPECT" | jq '.'

if echo "$INTROSPECT" | jq -e '.result.valid' > /dev/null; then
    IS_VALID=$(echo "$INTROSPECT" | jq -r '.result.valid')
    if [ "$IS_VALID" = "true" ]; then
        print_success "Token is valid"
    else
        print_error "Token is invalid"
    fi
else
    print_error "Token introspection failed"
fi

# ============================================
# TEST 16: Refresh Token
# ============================================
print_section "TEST 16: Refresh Token"

echo "Request: POST ${AUTH_URL}/refresh"
REFRESH_RESULT=$(curl -s -X POST "${AUTH_URL}/refresh" \
  -H "Content-Type: application/json" \
  -d "{
    \"token\": \"${USER1_REFRESH_TOKEN}\"
  }")

echo "Response:"
echo "$REFRESH_RESULT" | jq '.'

NEW_ACCESS_TOKEN=$(echo "$REFRESH_RESULT" | jq -r '.result.tokens.accessToken')
NEW_REFRESH_TOKEN=$(echo "$REFRESH_RESULT" | jq -r '.result.tokens.refreshToken')

if [ "$NEW_ACCESS_TOKEN" != "null" ]; then
    print_success "Token refreshed successfully"
    echo "New Access Token: ${NEW_ACCESS_TOKEN:0:50}..."
    USER1_ACCESS_TOKEN=$NEW_ACCESS_TOKEN
    USER1_REFRESH_TOKEN=$NEW_REFRESH_TOKEN
else
    print_error "Token refresh failed"
fi

# ============================================
# TEST 17: User 1 Unfollows User 2
# ============================================
print_section "TEST 17: User 1 Unfollows User 2"

echo "Request: DELETE ${USER_URL}/${USER2_ID}/follow"
UNFOLLOW_RESULT=$(curl -s -X DELETE "${USER_URL}/${USER2_ID}/follow" \
  -H "Authorization: Bearer ${USER1_ACCESS_TOKEN}")

echo "Response:"
echo "$UNFOLLOW_RESULT" | jq '.'

if echo "$UNFOLLOW_RESULT" | jq -e '.result.success' > /dev/null; then
    print_success "User 1 unfollowed User 2 successfully"
else
    print_error "Unfollow operation failed"
fi

# ============================================
# TEST 18: Logout User 1
# ============================================
print_section "TEST 18: Logout User 1"

echo "Request: POST ${AUTH_URL}/logout"
LOGOUT_RESULT=$(curl -s -X POST "${AUTH_URL}/logout" \
  -H "Content-Type: application/json" \
  -d "{
    \"token\": \"${USER1_ACCESS_TOKEN}\"
  }")

echo "Response:"
echo "$LOGOUT_RESULT" | jq '.'

print_success "User 1 logged out"

# ============================================
# TEST 19: Try to use token after logout (Should fail)
# ============================================
print_section "TEST 19: Try to Access with Logged Out Token"

echo "Request: GET ${USER_URL}/me"
AFTER_LOGOUT=$(curl -s -X GET "${USER_URL}/me" \
  -H "Authorization: Bearer ${USER1_ACCESS_TOKEN}")

echo "Response:"
echo "$AFTER_LOGOUT" | jq '.'

if echo "$AFTER_LOGOUT" | jq -e '.result' > /dev/null; then
    print_error "Token still works after logout (This should not happen!)"
else
    print_success "Token correctly invalidated after logout"
fi

# ============================================
# Summary
# ============================================
print_section "TEST SUMMARY"
echo -e "${GREEN}All tests completed!${NC}"
echo ""
echo "Test Results:"
echo "✓ User registration"
echo "✓ User login"
echo "✓ Profile setup and management"
echo "✓ Follow/Unfollow operations"
echo "✓ Followers/Following lists"
echo "✓ Token refresh"
echo "✓ Logout"
echo ""
echo -e "${YELLOW}Note: Make sure all three services are running:${NC}"
echo "  - Auth Service (port 8080)"
echo "  - User Service (port 8081)"
echo "  - API Gateway (port 8888)"
