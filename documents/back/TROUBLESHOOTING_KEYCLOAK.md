# Keycloak Authentication Troubleshooting

## Common Issues and Solutions

### 401 Unauthorized on Login

If you get a `401 Unauthorized` error when calling `/api/v1/auth/login`, check the following:

#### 1. Client Secret Configuration

**Problem**: The client secret in `application.yaml` is still set to the placeholder value.

**Solution**:
1. Open Keycloak Admin Console: http://localhost:8888
2. Navigate to: **Clients** → `daypulse-backend` → **Credentials** tab
3. Copy the **Client secret**
4. Update `backEnd/auth-service/src/main/resources/application.yaml`:
   ```yaml
   keycloak:
     credentials:
       secret: ${KEYCLOAK_CLIENT_SECRET:YOUR_ACTUAL_SECRET_HERE}
   ```
   Or set environment variable:
   ```bash
   export KEYCLOAK_CLIENT_SECRET=your-actual-secret
   ```

#### 2. Direct Access Grant Not Enabled

**Problem**: The Keycloak client doesn't have Direct Access Grant enabled.

**Solution**:
1. Open Keycloak Admin Console: http://localhost:8888
2. Navigate to: **Clients** → `daypulse-backend`
3. Go to **Settings** tab
4. Under **Capability config**, ensure:
   - ✅ **Direct access grants**: **ON**
5. Click **Save**

#### 3. User Doesn't Exist in Keycloak

**Problem**: The user you're trying to login with doesn't exist in Keycloak.

**Solution**:
1. Register the user first via `/api/v1/auth/signup` endpoint
2. Or manually create the user in Keycloak:
   - Navigate to: **Users** → **Add user**
   - Set **Email** and **Username**
   - Go to **Credentials** tab → Set password
   - Set **Email verified**: ON (for testing)

#### 4. Check Keycloak Logs

View Keycloak logs to see detailed error messages:
```bash
docker-compose logs -f keycloak
```

#### 5. Verify Client Configuration

Run this diagnostic curl to test Keycloak directly:

```bash
curl -X POST http://localhost:8888/realms/daypulse/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=daypulse-backend" \
  -d "client_secret=YOUR_CLIENT_SECRET" \
  -d "username=test123@gmail.com" \
  -d "password=password123" \
  -d "scope=openid profile email"
```

Replace `YOUR_CLIENT_SECRET` with the actual secret from Keycloak.

If this works, the issue is in the application configuration.
If this fails, the issue is in Keycloak client/user configuration.

### Common Error Messages

#### "Invalid client credentials"
- **Cause**: Wrong client secret
- **Fix**: Update `keycloak.credentials.secret` in `application.yaml`

#### "Invalid user credentials"
- **Cause**: Wrong username/password or user doesn't exist
- **Fix**: Register user via `/api/v1/auth/signup` or create in Keycloak

#### "Direct access grants disabled"
- **Cause**: Direct Access Grant not enabled for client
- **Fix**: Enable in Keycloak client settings

#### "User is disabled"
- **Cause**: User account is disabled in Keycloak
- **Fix**: Enable user in Keycloak Admin Console

### Verification Checklist

- [ ] Keycloak is running: `docker-compose ps`
- [ ] Keycloak is accessible: http://localhost:8888
- [ ] Realm `daypulse` exists
- [ ] Client `daypulse-backend` exists
- [ ] Client secret is correct in `application.yaml`
- [ ] Direct Access Grant is enabled for client
- [ ] User exists in Keycloak realm
- [ ] User password is set correctly
- [ ] User account is enabled
