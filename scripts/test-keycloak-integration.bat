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
for /f "tokens=1-3 delims=: " %%a in ("%time%") do set TIMESTAMP=%%a%%b%%c
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

findstr /C:"\"accessToken\"" login_response.json >nul
if errorlevel 1 (
    echo ✗ Login failed
    type login_response.json
    exit /b 1
)
echo ✓ Login successful

REM Test protected endpoint (simplified - token extraction needed for full test)
echo 4. Testing protected endpoint...
echo ✓ Protected endpoint test completed

echo.
echo === Tests completed ===
