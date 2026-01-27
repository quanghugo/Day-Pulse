# Keycloak Setup Guide for DayPulse

Complete guide for setting up Keycloak as the authentication provider for DayPulse.

---

## Prerequisites

- Docker and Docker Compose installed
- Ports 8080 (Keycloak) available
- Google Cloud Console account (for Google OAuth)

---

## Step 1: Start Keycloak with Docker

### 1.1 Start Keycloak Services

From the project root directory:

```bash
docker-compose -f docker-compose-keycloak.yml up -d
```

This will start:

- PostgreSQL database (port 5432, internal)
- Keycloak server (port 8080)

### 1.2 Verify Services are Running

```bash
docker-compose -f docker-compose-keycloak.yml ps
```

Expected output:

```
NAME            STATUS          PORTS
keycloak        Up (healthy)    0.0.0.0:8080->8080/tcp
keycloak-db     Up (healthy)    5432/tcp
```

### 1.3 Wait for Keycloak to be Ready

Keycloak takes ~60 seconds to start. Check logs:

```bash
docker-compose -f docker-compose-keycloak.yml logs -f keycloak
```

Look for: `Keycloak 23.0.0 started`

---

## Step 2: Access Keycloak Admin Console

### 2.1 Open Admin Console

Navigate to: **http://localhost:8080**

### 2.2 Login with Admin Credentials

- **Username**: `admin`
- **Password**: `admin`

> ⚠️ **Production Note**: Change admin password immediately in production!

---

## Step 3: Create DayPulse Realm

### 3.1 Create New Realm

1. Click the **dropdown** next to "master" (top-left corner)
2. Click **"Create Realm"**
3. Enter **Realm name**: `daypulse`
4. Click **"Create"**

### 3.2 Configure Realm Settings

1. Go to **Realm Settings** (left sidebar)
2. On the **General** tab:
   - **Display name**: `DayPulse`
   - **Frontend URL**: `http://localhost:8080` (for development)
   - **Require SSL**: `none` (for development) or `external requests` (for production)
3. Click **"Save"**

### 3.3 Configure Email Settings (Optional)

Go to **Realm Settings** → **Email** tab:

For development, you can skip this or use a test SMTP service like MailHog.

For production:

- **From**: `noreply@daypulse.app`
- **Host**: Your SMTP server
- **Port**: 587 (or your SMTP port)
- **Username**: Your SMTP username
- **Password**: Your SMTP password
- **Enable StartTLS**: Yes

---

## Step 4: Create Backend API Client

This client is used by the Spring Boot backend service.

### 4.1 Create Client

1. Go to **Clients** (left sidebar)
2. Click **"Create client"**
3. Fill in:
   - **Client type**: `OpenID Connect`
   - **Client ID**: `daypulse-backend`
4. Click **"Next"**

### 4.2 Configure Client Settings

**Capability config**:

- ✅ Client authentication: **ON**
- ✅ Authorization: **ON**
- ✅ Standard flow: **ON**
- ✅ Direct access grants: **ON** (enables username/password flow)
- ✅ Service accounts roles: **ON**

Click **"Next"**

**Login settings**:

- **Root URL**: `http://localhost:8180` (auth-service port)
- **Home URL**: `http://localhost:8180`
- **Valid redirect URIs**: `http://localhost:8180/*`
- **Valid post logout redirect URIs**: `http://localhost:8180/*`
- **Web origins**: `http://localhost:8180`

Click **"Save"**

### 4.3 Get Client Secret

1. Go to **Clients** → `daypulse-backend`
2. Click **"Credentials"** tab
3. Copy the **Client secret** (you'll need this for backend configuration)

Example: `7fXxKj2pRq8Lm3Nt5Vw9Yz1Ac4Bd6Ce`

---

## Step 5: Create Frontend SPA Client

This client is used by the React frontend application.

### 5.1 Create Client

1. Go to **Clients** (left sidebar)
2. Click **"Create client"**
3. Fill in:
   - **Client type**: `OpenID Connect`
   - **Client ID**: `daypulse-frontend`
4. Click **"Next"**

### 5.2 Configure Client Settings

**Capability config**:

- ❌ Client authentication: **OFF** (public client for SPA)
- ❌ Authorization: **OFF**
- ✅ Standard flow: **ON**
- ✅ Implicit flow: **OFF** (not recommended for SPAs)
- ✅ Direct access grants: **ON**

Click **"Next"**

**Login settings**:

- **Root URL**: `http://localhost:5173` (Vite dev server)
- **Home URL**: `http://localhost:5173`
- **Valid redirect URIs**:
  - `http://localhost:5173/*`
  - `http://localhost:5173/callback`
- **Valid post logout redirect URIs**: `http://localhost:5173/*`
- **Web origins**: `http://localhost:5173`

Click **"Save"**

### 5.3 Configure PKCE (Recommended for SPAs)

1. Go to **Clients** → `daypulse-frontend`
2. Click **"Advanced"** tab
3. Scroll to **Advanced Settings**
4. Set **Proof Key for Code Exchange Code Challenge Method**: `S256`
5. Click **"Save"**

---

## Step 6: Configure Google OAuth Provider

### 6.1 Create Google OAuth Credentials

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing: **DayPulse**
3. Go to **APIs & Services** → **Credentials**
4. Click **"Create Credentials"** → **OAuth 2.0 Client ID**
5. Configure:
   - **Application type**: `Web application`
   - **Name**: `Keycloak - DayPulse`
   - **Authorized JavaScript origins**:
     - `http://localhost:8080`
   - **Authorized redirect URIs**:
     - `http://localhost:8080/realms/daypulse/broker/google/endpoint`
6. Click **"Create"**
7. Copy **Client ID** and **Client Secret**

### 6.2 Add Google Identity Provider in Keycloak

1. In Keycloak, go to **Identity Providers** (left sidebar)
2. Click **"Add provider"** → Select **"Google"**
3. Fill in:
   - **Alias**: `google` (lowercase, used in URLs)
   - **Display name**: `Google`
   - **Client ID**: [Your Google OAuth Client ID]
   - **Client Secret**: [Your Google OAuth Client Secret]
   - **Default scopes**: `openid profile email`
4. Click **"Add"**

### 6.3 Configure Google Identity Provider

1. Go to **Identity Providers** → `google`
2. **Settings** tab:
   - ✅ Enabled: **ON**
   - ✅ Trust Email: **ON**
   - ✅ Store Tokens: **ON** (optional, for accessing Google APIs)
   - **First Login Flow**: `first broker login`
3. Click **"Save"**

### 6.4 Test Google Provider

The redirect URI for Google should be:

```
http://localhost:8080/realms/daypulse/broker/google/endpoint
```

Ensure this EXACTLY matches the URI in Google Cloud Console.

---

## Step 7: Configure Roles

### 7.1 Create Realm Roles

1. Go to **Realm roles** (left sidebar)
2. Click **"Create role"**
3. Create the following roles:

**USER Role**:

- **Role name**: `USER`
- **Description**: `Standard user role`
- Click **"Save"**

**MODERATOR Role**:

- **Role name**: `MODERATOR`
- **Description**: `Content moderator role`
- Click **"Save"**

**ADMIN Role**:

- **Role name**: `ADMIN`
- **Description**: `Administrator role`
- Click **"Save"**

### 7.2 Set Default Role

1. Go to **Realm settings** → **User registration** tab
2. Click **"Default roles"** tab
3. Click **"Assign role"**
4. Select **`USER`** role
5. Click **"Assign"**

This ensures new users automatically get the USER role.

---

## Step 8: Create Test Users

### 8.1 Create Test User

1. Go to **Users** (left sidebar)
2. Click **"Create new user"**
3. Fill in:
   - **Username**: `testuser`
   - **Email**: `testuser@example.com`
   - **First name**: `Test`
   - **Last name**: `User`
   - ✅ Email verified: **ON**
   - ✅ Enabled: **ON**
4. Click **"Create"**

### 8.2 Set User Password

1. Click on the created user
2. Go to **Credentials** tab
3. Click **"Set password"**
4. Enter:
   - **Password**: `password123`
   - **Password confirmation**: `password123`
   - ❌ Temporary: **OFF** (so user doesn't need to change password on first login)
5. Click **"Save"**

### 8.3 Assign Role to User

1. Go to **Role mapping** tab
2. Click **"Assign role"**
3. Select **`USER`** role
4. Click **"Assign"**

### 8.4 Create Admin User (Optional)

Repeat steps 8.1-8.3 with:

- **Username**: `admin`
- **Email**: `admin@daypulse.com`
- **Password**: `admin123`
- **Role**: `ADMIN`

---

## Step 9: Verify Keycloak Configuration

### 9.1 Test OpenID Configuration

Open in browser:

```
http://localhost:8080/realms/daypulse/.well-known/openid-configuration
```

You should see JSON with Keycloak endpoints:

- `issuer`: `http://localhost:8080/realms/daypulse`
- `authorization_endpoint`
- `token_endpoint`
- `jwks_uri`
- etc.

### 9.2 Test Login Page

Open in browser:

```
http://localhost:8080/realms/daypulse/account
```

You should see:

- Keycloak login page
- "Sign in with Google" button (if configured)
- Username/password fields

Try logging in with the test user credentials.

---

## Step 10: Configure Backend Application

Update `backEnd/auth-service/src/main/resources/application.yml`:

```yaml
keycloak:
  realm: daypulse
  auth-server-url: http://localhost:8080
  resource: daypulse-backend
  credentials:
    secret: [YOUR_CLIENT_SECRET_FROM_STEP_4.3]

spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080/realms/daypulse
          jwk-set-uri: http://localhost:8080/realms/daypulse/protocol/openid-connect/certs
```

---

## Step 11: Configure Frontend Application

Create/update `frontEnd/.env`:

```env
VITE_KEYCLOAK_URL=http://localhost:8080
VITE_KEYCLOAK_REALM=daypulse
VITE_KEYCLOAK_CLIENT_ID=daypulse-frontend
```

---

## Troubleshooting

### Keycloak Won't Start

**Check logs**:

```bash
docker-compose -f docker-compose-keycloak.yml logs keycloak
```

**Common issues**:

- Port 8080 already in use → Stop other services or change port in docker-compose
- Database connection failed → Ensure keycloak-db is healthy

### "Invalid redirect URI" Error

- Ensure redirect URIs in Keycloak client match exactly
- Check for trailing slashes
- Verify protocol (http vs https)

### Google Login Not Working

- Verify Google OAuth redirect URI exactly matches:
  ```
  http://localhost:8080/realms/daypulse/broker/google/endpoint
  ```
- Check Google OAuth consent screen is configured
- Ensure Google Client ID and Secret are correct

### CORS Errors

- Add frontend URL to "Web origins" in both clients
- Check that `*` is not used in production

---

## Security Checklist for Production

Before deploying to production:

- [ ] Change admin password from default
- [ ] Enable SSL/TLS (change `KC_HOSTNAME_STRICT` to `true`)
- [ ] Use strong client secrets
- [ ] Configure proper email service
- [ ] Set up SSL certificates
- [ ] Review and configure password policies
- [ ] Enable brute force detection
- [ ] Configure session timeouts
- [ ] Review realm security settings
- [ ] Set up Keycloak clustering (for high availability)
- [ ] Configure proper backup strategy for Keycloak database

---

## Quick Reference

### Keycloak Admin Console

**URL**: http://localhost:8080  
**Username**: admin  
**Password**: admin

### Realm Configuration

**Realm**: daypulse

### Clients

- **Backend**: `daypulse-backend` (confidential)
- **Frontend**: `daypulse-frontend` (public)

### Test Users

- **Username**: `testuser`
- **Password**: `password123`

### Useful Commands

**Start Keycloak**:

```bash
docker-compose -f docker-compose-keycloak.yml up -d
```

**Stop Keycloak**:

```bash
docker-compose -f docker-compose-keycloak.yml down
```

**View Logs**:

```bash
docker-compose -f docker-compose-keycloak.yml logs -f
```

**Reset Keycloak (delete all data)**:

```bash
docker-compose -f docker-compose-keycloak.yml down -v
```

---

## Next Steps

After completing this setup:

1. **Backend Integration**: Update Spring Boot services to use Keycloak
2. **Frontend Integration**: Integrate Keycloak JS adapter in React app
3. **Testing**: Verify authentication flows work end-to-end
4. **User Migration**: Migrate existing users to Keycloak (if applicable)

Refer to the main implementation plan for detailed integration steps.
