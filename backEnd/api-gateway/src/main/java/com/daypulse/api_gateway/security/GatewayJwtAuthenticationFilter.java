package com.daypulse.api_gateway.security;

import com.daypulse.api_gateway.client.AuthServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gateway JWT Authentication Filter
 * 
 * STANDARD: Validates JWT tokens from Authorization header following OAuth 2.0 Bearer Token Usage (RFC 6750)
 * 
 * Flow:
 * 1. Extract token from "Authorization: Bearer <token>" header
 * 2. Validate JWT signature and expiration locally
 * 3. Check token revocation via Auth Service introspection
 * 4. Extract user identity (userId, roles) from JWT claims
 * 5. Forward user context to downstream services via internal headers (X-User-Id, X-User-Roles)
 * 6. Set authentication in security context
 * 
 * Note: Public endpoints can pass through without tokens
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayJwtAuthenticationFilter implements WebFilter {

    private final ReactiveJwtDecoder jwtDecoder;
    private final AuthServiceClient authServiceClient;

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_ROLES = "X-User-Roles";

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String token = extractToken(exchange.getRequest());

        if (token == null) {
            // No token present, continue without authentication
            return chain.filter(exchange);
        }

        // Step 1: Decode JWT locally (validates signature and expiry)
        return jwtDecoder.decode(token)
                .flatMap(jwt -> {
                    // Step 2: Check token revocation via introspection
                    return authServiceClient.introspectToken(token)
                            .flatMap(introspectResponse -> {
                                if (!introspectResponse.isValid()) {
                                    log.warn("Token is invalid or revoked");
                                    return chain.filter(exchange);
                                }

                                // Step 3: Extract authorities from JWT scope claim
                                String scope = jwt.getClaimAsString("scope");
                                List<SimpleGrantedAuthority> authorities;
                                
                                if (StringUtils.hasText(scope)) {
                                    // Parse space-separated roles from scope claim
                                    authorities = Arrays.stream(scope.split(" "))
                                            .filter(StringUtils::hasText)  // Filter out empty strings
                                            .map(SimpleGrantedAuthority::new)
                                            .collect(Collectors.toList());
                                } else {
                                    // Default authority if scope is empty or null
                                    authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
                                    log.debug("No scope in JWT, using default ROLE_USER");
                                }

                                // Step 4: Create authentication object
                                String username = jwt.getSubject();
                                String userId = jwt.getClaimAsString("userId");
                                UsernamePasswordAuthenticationToken authentication =
                                        new UsernamePasswordAuthenticationToken(username, null, authorities);

                                // Step 5: Add user context headers to downstream requests
                                // STANDARD: Internal headers for service-to-service communication
                                // Downstream services trust these headers from the gateway
                                ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                                        .header(HEADER_USER_ID, userId)
                                        .header(HEADER_USER_ROLES, StringUtils.hasText(scope) ? scope : "ROLE_USER")
                                        .build();
                                
                                log.debug("Authenticated user: {} (userId: {}, roles: {})", username, userId, scope);

                                // TODO: [FUTURE-REDIS] Check token blacklist for faster revocation
                                // Boolean isRevoked = redisTemplate.hasKey("revoked:token:" + DigestUtils.md5DigestAsHex(token.getBytes()));
                                // if (Boolean.TRUE.equals(isRevoked)) {
                                //     log.warn("Token is blacklisted");
                                //     return chain.filter(exchange);
                                // }

                                // Step 6: Set authentication in security context and continue
                                return chain.filter(exchange.mutate().request(mutatedRequest).build())
                                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
                            });
                })
                .onErrorResume(error -> {
                    log.error("JWT validation failed: {}", error.getMessage());
                    return chain.filter(exchange);
                });
    }

    /**
     * Extract JWT token from Authorization header
     * 
     * STANDARD: Expects "Authorization: Bearer <token>" format (RFC 6750)
     * 
     * @param request HTTP request
     * @return JWT token string, or null if not present/invalid format
     */
    private String extractToken(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}