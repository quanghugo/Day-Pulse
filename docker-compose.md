# Docker Compose Configuration Documentation

## Overview

This Docker Compose configuration sets up a complete Keycloak authentication server with PostgreSQL database for local development. The setup includes health checks, proper service dependencies, and network isolation.

## Architecture Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    Docker Compose Stack                      │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────────┐         ┌──────────────────┐          │
│  │  keycloak-db     │◄────────┤    keycloak      │          │
│  │  (PostgreSQL)    │         │   (Keycloak)     │          │
│  │                  │         │                  │          │
│  │  Port: 5432      │         │  Port: 8888:8080 │          │
│  │  (internal)      │         │  (exposed)       │          │
│  └──────────────────┘         └──────────────────┘          │
│         │                              │                      │
│         └──────────┬───────────────────┘                      │
│                    │                                          │
│            keycloak-network (bridge)                          │
│                                                               │
│  Volumes:                                                     │
│  - keycloak-db-data (persistent storage)                     │
│  - ./keycloak/themes (custom themes)                         │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

## Service Dependencies Flow

1. **keycloak-db** starts first
   - Initializes PostgreSQL database
   - Waits for health check to pass
   
2. **keycloak** starts after keycloak-db is healthy
   - Connects to PostgreSQL database
   - Initializes Keycloak server
   - Exposes service on port 8888

## Services

### 1. keycloak-db (PostgreSQL Database)

**Purpose**: Provides persistent storage for Keycloak's configuration, realms, users, and sessions.

**Configuration**:
- **Image**: `postgres:15-alpine` (lightweight Alpine-based PostgreSQL 15)
- **Container Name**: `keycloak-db`
- **Database**: `keycloak`
- **Credentials**:
  - Username: `keycloak`
  - Password: `keycloak_password`
- **Data Persistence**: Volume `keycloak-db-data` mounted at `/var/lib/postgresql/data`
- **Network**: Connected to `keycloak-network` (internal communication only)

**Health Check**:
- **Test**: `pg_isready -U keycloak -d keycloak`
- **Interval**: Every 10 seconds
- **Timeout**: 5 seconds
- **Retries**: 5 attempts
- **Purpose**: Ensures database is ready before Keycloak starts

### 2. keycloak (Keycloak Server)

**Purpose**: Identity and Access Management (IAM) server providing authentication and authorization services.

**Configuration**:
- **Image**: `quay.io/keycloak/keycloak:23.0.0`
- **Container Name**: `keycloak`
- **Command**: `start-dev` (development mode)
- **Port Mapping**: `8888:8080` (host:container)
  - Access Keycloak at: `http://localhost:8888`

**Environment Variables**:

#### Database Configuration
- `KC_DB`: `postgres` - Database type
- `KC_DB_URL`: `jdbc:postgresql://keycloak-db:5432/keycloak` - Database connection URL
- `KC_DB_USERNAME`: `keycloak` - Database username
- `KC_DB_PASSWORD`: `keycloak_password` - Database password

#### Admin Credentials
- `KEYCLOAK_ADMIN`: `admin` - Admin username
- `KEYCLOAK_ADMIN_PASSWORD`: `admin` - Admin password
- **⚠️ Security Note**: Change these credentials in production!

#### Hostname/URL Configuration (Local Development)
- `KC_HTTP_ENABLED`: `true` - Enable HTTP (not HTTPS)
- `KC_HOSTNAME_STRICT`: `false` - Disable strict hostname checking (required for local dev)
- `KC_HOSTNAME_URL`: `http://localhost:8888` - Base URL for Keycloak

#### Health Endpoints
- `KC_HEALTH_ENABLED`: `"true"` - Enable health check endpoints

**Dependencies**:
- **depends_on**: `keycloak-db` with `condition: service_healthy`
  - Ensures Keycloak only starts after database is ready

**Volumes**:
- `./keycloak/themes:/opt/keycloak/themes` - Custom theme directory
- `./keycloak/realm-export.json:/opt/keycloak/data/import/realm.json` (commented out)
  - Uncomment to auto-import realm configuration on startup

**Health Check**:
- **Test**: `wget -qO- http://localhost:8080/health/ready`
- **Interval**: Every 30 seconds
- **Timeout**: 10 seconds
- **Retries**: 5 attempts
- **Start Period**: 60 seconds (grace period for initial startup)

## Networks

### keycloak-network
- **Type**: Bridge network
- **Purpose**: Isolated network for service-to-service communication
- **Services Connected**:
  - `keycloak-db`
  - `keycloak`

## Volumes

### keycloak-db-data
- **Type**: Local volume
- **Purpose**: Persistent storage for PostgreSQL data
- **Location**: Docker-managed volume
- **Data Persistence**: Survives container restarts and removals

### ./keycloak/themes (Bind Mount)
- **Type**: Bind mount
- **Purpose**: Custom Keycloak themes
- **Location**: `./keycloak/themes` → `/opt/keycloak/themes`
- **Note**: Create this directory structure if using custom themes

## Usage

### Starting Services

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f keycloak
docker-compose logs -f keycloak-db
```

### Accessing Keycloak

1. **Admin Console**: http://localhost:8888
2. **Admin Credentials**:
   - Username: `admin`
   - Password: `admin`

### Stopping Services

```bash
# Stop services (keeps volumes)
docker-compose down

# Stop and remove volumes (⚠️ deletes database data)
docker-compose down -v
```

### Health Check Status

```bash
# Check service health
docker-compose ps

# Check specific service health
docker inspect keycloak | grep -A 10 Health
```

## Important Notes

### Development vs Production

⚠️ **This configuration is for LOCAL DEVELOPMENT ONLY**

For production:
1. Change default admin credentials
2. Enable HTTPS (`KC_HTTP_ENABLED: false`)
3. Configure proper hostname (`KC_HOSTNAME_STRICT: true`)
4. Use production database with proper credentials
5. Set up reverse proxy if needed (`KC_PROXY: edge`)
6. Use `start` command instead of `start-dev`
7. Configure proper backup strategy for volumes

### Security Considerations

- Default passwords are for development only
- HTTP is enabled for local development (use HTTPS in production)
- Hostname strict checking is disabled (enable in production)
- Database credentials are in plain text (use secrets management in production)

### Custom Themes

To use custom themes:
1. Create `./keycloak/themes` directory
2. Place custom theme files in subdirectories
3. Restart Keycloak service

### Realm Import

To auto-import a realm on startup:
1. Uncomment line 58 in docker-compose.yml
2. Place `realm-export.json` in `./keycloak/` directory
3. Restart services

## Troubleshooting

### Keycloak won't start
- Check if database is healthy: `docker-compose ps`
- Check database logs: `docker-compose logs keycloak-db`
- Verify database connection string in environment variables

### Port 8888 already in use
- Change port mapping: `"8888:8080"` → `"8889:8080"`
- Update `KC_HOSTNAME_URL` accordingly

### Database connection errors
- Ensure `keycloak-db` service is healthy
- Verify database credentials match in both services
- Check network connectivity: `docker network inspect keycloak-network`

### Health check failures
- Increase `start_period` for Keycloak if startup takes longer
- Check service logs for errors
- Verify health endpoint is accessible: `docker exec keycloak wget -qO- http://localhost:8080/health/ready`
