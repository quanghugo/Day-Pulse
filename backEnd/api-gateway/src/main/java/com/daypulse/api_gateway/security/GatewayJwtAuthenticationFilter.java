package com.daypulse.api_gateway.security;

import com.daypulse.api_gateway.client.AuthServiceClient;
import com.daypulse.api_gateway.dto.IntrospectResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
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
 * 3. Check token revocation via Auth Service introspection (with caching)
 * 4. Extract user identity (userId, roles) from JWT claims
 * 5. Forward user context to downstream services via internal headers (X-User-Id, X-User-Roles)
 * 6. Add service-to-service authentication signature
 * 7. Set authentication in security context
 * 
 * Note: Public endpoints can pass through without tokens
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayJwtAuthenticationFilter implements WebFilter {

    private final ReactiveJwtDecoder jwtDecoder;
    private final AuthServiceClient authServiceClient;
    private final TokenIntrospectionCache introspectionCache;
    private final ServiceAuthenticationUtil serviceAuthUtil;

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String token = extractToken(exchange.getRequest());

        if (token == null) {
            // No token present, continue without authentication
            // Spring Security will handle authorization based on endpoint configuration
            return chain.filter(exchange);
        }

        // Step 1: Decode JWT locally (validates signature and expiry)
        return jwtDecoder.decode(token)
                .flatMap(jwt -> validateTokenAndSetContext(exchange, chain, jwt, token))
                .onErrorResume(error -> handleAuthenticationError(exchange, chain, error));
    }

    /**
     * Validate token via introspection and set authentication context.
     */
    private Mono<Void> validateTokenAndSetContext(
            ServerWebExchange exchange,
            WebFilterChain chain,
            Jwt jwt,
            String token) {
        
        // Step 2: Check token revocation via introspection (with caching)
        return checkTokenValidity(token)
                .flatMap(isValid -> {
                    if (!isValid) {
                        log.warn("Token is invalid or revoked: token={}", maskToken(token));
                        return createUnauthorizedResponse(exchange, "Token is invalid or revoked");
                    }

                    // Step 3: Extract user information from JWT
                    String userId = jwt.getClaimAsString(SecurityConstants.CLAIM_USER_ID);
                    String scope = jwt.getClaimAsString(SecurityConstants.CLAIM_SCOPE);
                    String username = jwt.getSubject();

                    if (!StringUtils.hasText(userId)) {
                        log.error("JWT missing userId claim");
                        return createUnauthorizedResponse(exchange, "Invalid token: missing user ID");
                    }

                    // Step 4: Extract authorities from JWT scope claim
                    List<SimpleGrantedAuthority> authorities = extractAuthorities(scope);

                    // Step 5: Create authentication object
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(username, null, authorities);

                    // Step 6: Add user context headers to downstream requests with signature
                    ServerHttpRequest mutatedRequest = addServiceHeaders(exchange.getRequest(), userId, scope);

                    log.debug("Authenticated user: {} (userId: {}, roles: {})", username, userId, scope);

                    // Step 7: Set authentication in security context and continue
                    return chain.filter(exchange.mutate().request(mutatedRequest).build())
                            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
                });
    }

    /**
     * Check token validity using cache and introspection.
     */
    private Mono<Boolean> checkTokenValidity(String token) {
        // Check cache first
        IntrospectResponse cached = introspectionCache.get(token);
        if (cached != null) {
            log.debug("Token validation result retrieved from cache");
            return Mono.just(cached.isValid());
        }

        // Call introspection service
        return authServiceClient.introspectToken(token)
                .doOnNext(response -> {
                    // Cache the result
                    introspectionCache.put(token, response);
                })
                .map(IntrospectResponse::isValid)
                .onErrorReturn(false);
    }

    /**
     * Extract authorities from scope claim.
     */
    private List<SimpleGrantedAuthority> extractAuthorities(String scope) {
        if (StringUtils.hasText(scope)) {
            return Arrays.stream(scope.split(" "))
                    .filter(StringUtils::hasText)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        } else {
            log.debug("No scope in JWT, using default {}", SecurityConstants.DEFAULT_ROLE);
            return List.of(new SimpleGrantedAuthority(SecurityConstants.DEFAULT_ROLE));
        }
    }

    /**
     * Add service-to-service authentication headers.
     */
    private ServerHttpRequest addServiceHeaders(ServerHttpRequest request, String userId, String roles) {
        long timestamp = System.currentTimeMillis() / 1000;
        String rolesValue = StringUtils.hasText(roles) ? roles : SecurityConstants.DEFAULT_ROLE;
        String signature = serviceAuthUtil.generateSignature(userId, rolesValue, timestamp);

        return request.mutate()
                .header(SecurityConstants.HEADER_USER_ID, userId)
                .header(SecurityConstants.HEADER_USER_ROLES, rolesValue)
                .header(SecurityConstants.HEADER_GATEWAY_SIGNATURE, signature)
                .header(SecurityConstants.HEADER_GATEWAY_TIMESTAMP, String.valueOf(timestamp))
                .build();
    }

    /**
     * Handle authentication errors and return proper HTTP response.
     */
    private Mono<Void> handleAuthenticationError(
            ServerWebExchange exchange,
            WebFilterChain chain,
            Throwable error) {
        
        log.error("JWT validation failed: {}", error.getMessage(), error);
        
        // Check if it's a JWT-specific error
        if (error instanceof org.springframework.security.oauth2.jwt.JwtException) {
            return createUnauthorizedResponse(exchange, "Invalid or expired token");
        }
        
        // For other errors, still return 401 to prevent information leakage
        return createUnauthorizedResponse(exchange, "Authentication failed");
    }

    /**
     * Create 401 Unauthorized response with JSON body.
     */
    private Mono<Void> createUnauthorizedResponse(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
        
        // Create JSON error response
        String jsonResponse = String.format(
            "{\"code\":401,\"message\":\"%s\",\"result\":null}",
            message.replace("\"", "\\\"")
        );
        
        byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse()
                .bufferFactory()
                .wrap(bytes);
        
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    /**
     * Extract JWT token from Authorization header.
     * 
     * STANDARD: Expects "Authorization: Bearer <token>" format (RFC 6750)
     * 
     * @param request HTTP request
     * @return JWT token string, or null if not present/invalid format
     */
    private String extractToken(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (bearerToken != null && StringUtils.hasText(bearerToken) 
                && bearerToken.startsWith(SecurityConstants.BEARER_PREFIX)) {
            return bearerToken.substring(SecurityConstants.BEARER_PREFIX.length());
        }
        return null;
    }

    /**
     * Mask token for logging (show only first and last few characters).
     */
    private String maskToken(String token) {
        if (token == null || token.length() <= 10) {
            return "***";
        }
        return token.substring(0, 5) + "..." + token.substring(token.length() - 5);
    }
}