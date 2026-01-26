package com.daypulse.user_service.security;

import com.daypulse.user_service.exception.AppException;
import com.daypulse.user_service.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Filter to validate gateway headers and set security context.
 * 
 * This filter validates that requests come from the trusted API Gateway by:
 * 1. Checking for required headers (X-User-Id, X-Gateway-Signature, X-Gateway-Timestamp)
 * 2. Verifying HMAC signature
 * 3. Checking timestamp to prevent replay attacks
 * 4. Setting authentication in security context
 * 
 * STANDARD: All requests to user-service must come through the API Gateway.
 * Direct access to user-service endpoints is not allowed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayHeaderValidationFilter extends OncePerRequestFilter {

    @Value("${jwt.signing-key}")
    private String serviceSecretKey;

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Skip validation for internal endpoints (they should use different auth mechanism)
        if (path.startsWith("/internal/")) {
            // TODO: Add service-to-service authentication for internal endpoints
            filterChain.doFilter(request, response);
            return;
        }

        // Extract headers
        String userId = request.getHeader(SecurityConstants.HEADER_USER_ID);
        String roles = request.getHeader(SecurityConstants.HEADER_USER_ROLES);
        String signature = request.getHeader(SecurityConstants.HEADER_GATEWAY_SIGNATURE);
        String timestampStr = request.getHeader(SecurityConstants.HEADER_GATEWAY_TIMESTAMP);

        // Validate required headers are present
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(signature) || !StringUtils.hasText(timestampStr)) {
            log.warn("Missing required gateway headers for path: {}", path);
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        // Validate timestamp
        try {
            long timestamp = Long.parseLong(timestampStr);
            long currentTime = System.currentTimeMillis() / 1000;
            long age = currentTime - timestamp;

            if (age > SecurityConstants.MAX_SIGNATURE_AGE_SECONDS || age < 0) {
                log.warn("Request timestamp is too old or invalid: age={}s, path={}", age, path);
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid timestamp format: {}", timestampStr);
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        // Validate signature
        if (!verifySignature(userId, roles, Long.parseLong(timestampStr), signature)) {
            log.warn("Invalid gateway signature for path: {}", path);
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        // Extract authorities from roles header
        List<SimpleGrantedAuthority> authorities;
        if (StringUtils.hasText(roles)) {
            authorities = Arrays.stream(roles.split(" "))
                    .filter(StringUtils::hasText)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        } else {
            authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }

        // Create authentication object
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userId, null, authorities);

        // Set in security context
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("Gateway headers validated for user: {} on path: {}", userId, path);

        filterChain.doFilter(request, response);
    }

    /**
     * Verify HMAC signature from gateway.
     */
    private boolean verifySignature(String userId, String roles, long timestamp, String signature) {
        try {
            String rolesValue = StringUtils.hasText(roles) ? roles : "ROLE_USER";
            String dataToSign = String.format("%s|%s|%d", userId, rolesValue, timestamp);

            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    Base64.getDecoder().decode(serviceSecretKey),
                    HMAC_ALGORITHM
            );
            mac.init(secretKeySpec);
            byte[] signatureBytes = mac.doFinal(dataToSign.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = Base64.getEncoder().encodeToString(signatureBytes);

            return expectedSignature.equals(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to verify gateway signature", e);
            return false;
        }
    }
}
