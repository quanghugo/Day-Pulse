# User Service Security Model

## Overview

User Service operates **behind the API Gateway** and does not implement JWT validation directly. Instead, it trusts the security boundary established by the gateway.

---

## Security Architecture

### Trust Model

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Client     │────▶│ API Gateway  │────▶│ User Service │
│              │     │              │     │              │
│ Sends JWT    │     │ Validates    │     │ Trusts       │
│ in Header    │     │ JWT & adds   │     │ X-User-Id    │
│              │     │ X-User-Id    │     │ header       │
└──────────────┘     └──────────────┘     └──────────────┘
```

**Key Principle**: User Service trusts that if a request reaches it with an `X-User-Id` header, the API Gateway has already authenticated the user.

---

## Endpoint Security

### Public Endpoints (via Gateway)

All user service endpoints are protected by the API Gateway. There are no truly "public" endpoints that bypass authentication.

| Endpoint | Access Control | Notes |
|----------|----------------|-------|
| `GET /users/{id}` | Authenticated users | View any user's profile |
| `GET /users/me` | Authenticated users | View own profile |
| `PATCH /users/me` | Authenticated users | Update own profile |
| `POST /users/me/setup` | Authenticated users | Initial profile setup |

### Internal Endpoints (Direct Service-to-Service)

These endpoints are **NOT exposed through the API Gateway** and should only be accessible from other services within the internal network.

| Endpoint | Purpose | Security Model |
|----------|---------|----------------|
| `GET /internal/users/{id}/summary` | Get user summary for denormalization | No auth (internal network) |
| `POST /internal/users/{id}/init` | Initialize user profile after registration | No auth (internal network) |

**Security Note**: In production, internal endpoints should be protected by:
- Network isolation (private VPC)
- Service-to-service authentication (mTLS or service tokens)
- IP whitelisting

---

## How Authentication Works

### 1. Client Request
```http
GET /api/v1/users/me HTTP/1.1
Host: api-gateway.daypulse.com
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

### 2. Gateway Validation
- Validates JWT signature and expiration
- Checks token revocation status
- Extracts `userId` from JWT claims

### 3. Gateway → User Service
```http
GET /user-service/users/me HTTP/1.1
Host: user-service.internal
X-User-Id: abcd-1234-efgh-5678
X-User-Roles: ROLE_USER
```

### 4. User Service Processing
- Reads `X-User-Id` header
- Fetches user profile using the user ID
- Returns profile data
- **Does NOT validate JWT** (gateway already did this)

---

## Security Considerations

### Why No JWT Validation in User Service?

**Advantages:**
1. **Performance**: No need to verify JWT signature on every request
2. **Simplicity**: User Service doesn't need JWT dependencies or signing keys
3. **Single Point of Auth**: Centralized authentication logic in the gateway
4. **Loose Coupling**: User Service doesn't need to know about auth mechanisms

**Requirements:**
1. **Network Isolation**: User Service should NOT be directly accessible from the internet
2. **Gateway Trust**: Only accept requests from the API Gateway
3. **Header Validation**: Always validate presence of `X-User-Id` header

### Protecting Against Header Spoofing

**Problem**: What if a malicious client sends fake `X-User-Id` headers?

**Solution**: User Service should ONLY be accessible from the API Gateway:

1. **Network Level**:
   - Deploy User Service in a private subnet
   - Only allow ingress from API Gateway's IP/security group
   - Use firewall rules to block direct access

2. **Application Level** (Future Enhancement):
   ```java
   @Component
   public class GatewayHeaderValidationFilter implements Filter {
       @Override
       public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
           HttpServletRequest httpRequest = (HttpServletRequest) request;
           
           // Verify request came from gateway (check internal header or signature)
           String gatewayToken = httpRequest.getHeader("X-Gateway-Token");
           if (!isValidGatewayToken(gatewayToken)) {
               throw new UnauthorizedException("Request must come through API Gateway");
           }
           
           // Validate X-User-Id is present
           String userId = httpRequest.getHeader("X-User-Id");
           if (userId == null || userId.isEmpty()) {
               throw new UnauthorizedException("Missing user context");
           }
           
           chain.doFilter(request, response);
       }
   }
   ```

---

## Configuration

### Current Setup (No Spring Security)

User Service currently has **no Spring Security dependency**. This is intentional because:
- All authentication is handled by the gateway
- Service operates in a trusted internal network
- Reduces complexity and dependencies

### Future Enhancements

If stricter security is needed, consider:

1. **Add Spring Security (Optional)**
   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-security</artifactId>
   </dependency>
   ```

2. **Configure Custom Security**
   ```java
   @Configuration
   @EnableWebSecurity
   public class SecurityConfig {
       @Bean
       public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
           http
               .csrf(csrf -> csrf.disable())
               .authorizeHttpRequests(auth -> auth
                   // Internal endpoints - verify gateway token
                   .requestMatchers("/internal/**").hasRole("GATEWAY")
                   // User endpoints - trust X-User-Id from gateway
                   .requestMatchers("/users/**").permitAll()
               );
           return http.build();
       }
   }
   ```

---

## Testing

### Unit Tests
Mock the `X-User-Id` header in controller tests:

```java
@Test
void testGetMyProfile() {
    mockMvc.perform(get("/users/me")
            .header("X-User-Id", "test-user-id"))
        .andExpect(status().isOk());
}
```

### Integration Tests
Ensure API Gateway properly forwards headers:

```bash
# Full flow test
curl -X GET http://localhost:8188/api/v1/users/me \
  -H "Authorization: Bearer <valid-token>"

# Should work (gateway adds X-User-Id)

# Direct access test (should fail in production)
curl -X GET http://localhost:8181/user-service/users/me \
  -H "X-User-Id: fake-id"

# Should be blocked by network firewall in production
```

---

## Summary

### Security Principles

✅ **DO:**
- Trust `X-User-Id` header from gateway
- Validate header presence before processing
- Deploy in private network
- Use network security controls
- Document internal vs external endpoints

❌ **DON'T:**
- Expose User Service directly to the internet
- Skip header validation
- Implement duplicate JWT validation (unless needed)
- Trust requests without proper network isolation

### Production Checklist

- [ ] User Service deployed in private subnet/VPC
- [ ] Firewall rules: Only allow traffic from API Gateway
- [ ] Security groups: Restrict ingress to gateway IP
- [ ] Validate `X-User-Id` header presence in all endpoints
- [ ] Monitor for unauthorized direct access attempts
- [ ] Consider adding gateway-to-service authentication
- [ ] Implement service mesh (Istio/Linkerd) for mTLS (future)
