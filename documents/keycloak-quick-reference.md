# Keycloak Quick Reference - Port 8888

## Important URLs

**Keycloak Admin Console**: http://localhost:8888

- Username: `admin`
- Password: `admin`

**Realm Endpoints** (after creating `daypulse` realm):

- OpenID Configuration: http://localhost:8888/realms/daypulse/.well-known/openid-configuration
- Token Endpoint: http://localhost:8888/realms/daypulse/protocol/openid-connect/token
- Authorization Endpoint: http://localhost:8888/realms/daypulse/protocol/openid-connect/auth
- User Account Console: http://localhost:8888/realms/daypulse/account

## Next Steps

1. **Wait for Admin UI to load** (30-60 seconds on first startup)
2. **Login** with admin/admin
3. **Create Realm**:
   - Click dropdown next to "master" → "Create Realm"
   - Name: `daypulse`
4. **Create Clients**:
   - Backend: `daypulse-backend` (confidential)
   - Frontend: `daypulse-frontend` (public)
5. **Configure Google OAuth** (optional)
6. **Create test user**

## Docker Commands

**Check status**:

```bash
docker-compose -f docker-compose-keycloak.yml ps
```

**View logs**:

```bash
docker-compose -f docker-compose-keycloak.yml logs -f keycloak
```

**Stop Keycloak**:

```bash
docker-compose -f docker-compose-keycloak.yml down
```

**Restart Keycloak**:

```bash
docker-compose -f docker-compose-keycloak.yml restart
```

## Configuration Summary

All services configured to use **port 8888**:

- ✅ Docker Compose: `8888:8080`
- ✅ Backend `application.yaml`: `http://localhost:8888`
- ✅ Frontend `.env`: `VITE_KEYCLOAK_URL=http://localhost:8888`

## Troubleshooting

**If admin UI doesn't load**:

1. Check Docker logs: `docker-compose -f docker-compose-keycloak.yml logs keycloak`
2. Verify containers are running: `docker ps`
3. Try accessing: http://localhost:8888/health

**If you see "Loading the Admin UI"**:

- This is normal! Just wait 30-60 seconds for first startup
- Keycloak is initializing the database and admin console

For full setup instructions, see: [keycloak-setup.md](file:///c:/Users/vanqu/Desktop/DayPulse/documents/keycloak-setup.md)
