# DayPulse API Testing - Postman & Bruno Guide

This guide will help you import and use the DayPulse API collection in Postman or Bruno.

## 📦 Files Included

1. **DayPulse_API_Collection.postman_collection.json** - Complete API collection with all endpoints
2. **DayPulse_Environment.postman_environment.json** - Environment variables for local development

---

## 🚀 Import into Postman

### Step 1: Import Collection

1. Open Postman
2. Click **"Import"** button (top left)
3. Drag and drop `DayPulse_API_Collection.postman_collection.json` or click **"Upload Files"**
4. Click **"Import"**

### Step 2: Import Environment

1. Click **"Import"** button again
2. Drag and drop `DayPulse_Environment.postman_environment.json`
3. Click **"Import"**

### Step 3: Activate Environment

1. Click the environment dropdown (top right)
2. Select **"DayPulse - Local Development"**

### Step 4: Run Tests

1. Select the **"DayPulse API Collection"** in the left sidebar
2. Run requests in order or use the **Collection Runner** for automated testing

---

## 🎨 Import into Bruno

### Step 1: Import Collection

1. Open Bruno
2. Click **"Import Collection"**
3. Select **"Postman Collection"**
4. Choose `DayPulse_API_Collection.postman_collection.json`
5. Click **"Import"**

### Step 2: Setup Environment

1. Click on **"Environments"** in your collection
2. Create a new environment called **"Local"**
3. Add the following variables:
   ```
   base_url = http://localhost:8888/api/v1
   user1_access_token = (leave empty, auto-filled)
   user1_refresh_token = (leave empty, auto-filled)
   user1_id = (leave empty, auto-filled)
   user2_access_token = (leave empty, auto-filled)
   user2_refresh_token = (leave empty, auto-filled)
   user2_id = (leave empty, auto-filled)
   ```

### Step 3: Run Tests

1. Select requests from the collection
2. Execute them in order for the full user flow

---

## 📋 Collection Structure

The collection is organized into 3 main folders:

### 1. Authentication
- **Register User 1** - Create first test user
- **Register User 2** - Create second test user
- **Login User 1** - Login and get access token (auto-saves to environment)
- **Login User 2** - Login second user (auto-saves to environment)
- **Token Introspection** - Validate token
- **Refresh Token** - Get new access token (auto-updates environment)
- **Logout User 1** - Invalidate token
- **Try Access After Logout** - Verify token invalidation

### 2. User Management
- **Setup Profile User 1** - Initial profile setup
- **Setup Profile User 2** - Initial profile setup
- **Get My Profile** - Retrieve authenticated user's profile
- **Update My Profile** - Update profile information
- **Get User by ID** - Retrieve any user's profile
- **Get Suggested Users** - Get user suggestions
- **Get Available Users** - Get all available users

### 3. Follow System
- **User 1 Follows User 2** - Create follow relationship
- **Get User 2's Followers** - View followers list
- **Get User 1's Following List** - View following list
- **User 1 Unfollows User 2** - Remove follow relationship

---

## 🔄 Test Flow

The complete test flow simulates a real user journey:

1. **Register** two users (John Doe & Jane Smith)
2. **Login** both users → Tokens auto-saved to environment
3. **Setup profiles** for both users
4. **Get and update** User 1's profile
5. **User 1 follows User 2**
6. **View followers/following** lists
7. **Refresh token** → New token auto-saved
8. **User 1 unfollows User 2**
9. **Logout** User 1
10. **Verify** token invalidation after logout

---

## 🔐 Auto-Saved Variables

The collection automatically saves these variables after successful requests:

- `user1_access_token` - After User 1 login/refresh
- `user1_refresh_token` - After User 1 login/refresh
- `user1_id` - After User 1 login
- `user2_access_token` - After User 2 login
- `user2_refresh_token` - After User 2 login
- `user2_id` - After User 2 login

These are used automatically in subsequent requests via `{{variable_name}}`.

---

## ✅ Built-in Tests

Each request includes automatic tests that verify:

- ✓ Response status codes
- ✓ Response structure
- ✓ Data validity
- ✓ Token presence
- ✓ Successful operations

Test results appear in the **Test Results** tab after each request.

---

## 🛠️ Prerequisites

Make sure all services are running before testing:

```bash
# Auth Service
PORT: 8080

# User Service  
PORT: 8081

# API Gateway
PORT: 8888
```

---

## 🌍 Multiple Environments

You can create additional environments for different stages:

### Production Environment
```json
{
  "base_url": "https://api.daypulse.com/api/v1"
}
```

### Staging Environment
```json
{
  "base_url": "https://staging-api.daypulse.com/api/v1"
}
```

---

## 💡 Tips

1. **Run in Order**: Execute requests sequentially for the first time
2. **Collection Runner**: Use Postman's Collection Runner for automated testing
3. **Check Console**: View logged information (tokens, IDs) in the console
4. **Environment Variables**: All tokens and IDs are auto-managed
5. **Modify Data**: Feel free to change user data in request bodies

---

## 🐛 Troubleshooting

### Token Not Saved?
- Check if the login request succeeded
- Verify the response structure matches expected format
- Check the **Tests** tab for any errors

### Request Failed?
- Ensure all services are running
- Check the base_url in environment
- Verify previous requests succeeded (for dependent requests)

### 401 Unauthorized?
- Run the login request again to get a fresh token
- Check if the token is properly saved in environment

---

## 📚 Additional Resources

- [Postman Documentation](https://learning.postman.com/docs/getting-started/introduction/)
- [Bruno Documentation](https://docs.usebruno.com/)

---

## 🎯 Quick Start

1. Import both JSON files into Postman/Bruno
2. Activate the environment
3. Run **"Register User 1"** request
4. Run **"Login User 1"** request
5. Continue with other requests in order

That's it! You're ready to test the DayPulse API! 🚀
