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

@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayJwtAuthenticationFilter implements WebFilter {

    private final ReactiveJwtDecoder jwtDecoder;
    private final AuthServiceClient authServiceClient;

    private static final String BEARER_PREFIX = "Bearer ";

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
                                List<SimpleGrantedAuthority> authorities = Arrays.stream(scope.split(" "))
                                        .map(SimpleGrantedAuthority::new)
                                        .collect(Collectors.toList());

                                // Step 4: Create authentication object
                                String username = jwt.getSubject();
                                String userId = jwt.getClaimAsString("userId");
                                UsernamePasswordAuthenticationToken authentication =
                                        new UsernamePasswordAuthenticationToken(username, null, authorities);

                                // Step 5: Add user context headers to downstream requests
                                ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                                        .header("X-User-Id", userId)
                                        .header("X-User-Roles", scope)
                                        .build();

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

    private String extractToken(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}