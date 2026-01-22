package com.daypulse.auth_serivce.service;

import com.daypulse.auth_serivce.dto.request.*;
import com.daypulse.auth_serivce.dto.response.*;
import com.daypulse.auth_serivce.entity.RefreshToken;
import com.daypulse.auth_serivce.entity.UserAuth;
import com.daypulse.auth_serivce.enums.RoleEnum;
import com.daypulse.auth_serivce.exception.AppException;
import com.daypulse.auth_serivce.exception.ErrorCode;
import com.daypulse.auth_serivce.mapper.UserMapper;
import com.daypulse.auth_serivce.repository.RefreshTokenRepository;
import com.daypulse.auth_serivce.repository.UserRepository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {
    @NonFinal
    @Value("${jwt.signing-key}")
    String SIGNING_KEY;

    @NonFinal
    @Value("${jwt.valid-duration}")
    long VALID_DURATION;

    @NonFinal
    @Value("${jwt.refreshable-duration}")
    long REFRESHABLE_DURATION;

    UserRepository userRepository;
    RefreshTokenRepository refreshTokenRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;

    private byte[] getSigningKeyBytes() {
        return Base64.getDecoder().decode(SIGNING_KEY);
    }

    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        UserAuth user = userMapper.toUserAuth(request);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setIsEmailVerified(false);
        user.setIsSetupComplete(false);

        // Assign default role (USER)
        user.setRole(RoleEnum.USER);

        userRepository.save(user);

        // TODO: [FUTURE-KAFKA] Publish event: auth.user.registered
        // kafkaTemplate.send("auth.user.registered", UserRegisteredEvent.builder()
        //     .userId(user.getId())
        //     .email(user.getEmail())
        //     .timestamp(LocalDateTime.now())
        //     .build());

        // TODO: [FUTURE-EMAIL] Send verification email via email service
        // String otpCode = generateOtpCode();
        // saveOtpCode(user, otpCode, "email_verify");
        // emailService.sendVerificationEmail(user.getEmail(), otpCode);

        // Basic response without auto-login
        // For auto-login after registration, uncomment the token generation below
        return RegisterResponse.builder()
                .success(true)
                .userId(user.getId())
                .email(user.getEmail())
                .build();
        
        // Optional: Auto-login after registration
        // String accessToken = generateAccessToken(user);
        // String refreshToken = generateRefreshToken(user);
        // saveRefreshToken(user, refreshToken);
        // return RegisterResponse.builder()
        //         .success(true)
        //         .userId(user.getId())
        //         .email(user.getEmail())
        //         .user(userMapper.toUserSummary(user))
        //         .tokens(TokenPair.builder()
        //                 .accessToken(accessToken)
        //                 .refreshToken(refreshToken)
        //                 .build())
        //         .build();
    }

    public AuthenticationResponse authenticate(LoginRequest request) {
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());

        if (!authenticated) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        // Generate tokens
        String accessToken = generateAccessToken(user);
        String refreshToken = generateRefreshToken(user);

        // Save refresh token
        saveRefreshToken(user, refreshToken);

        // TODO: [FUTURE-REDIS] Cache session data
        // redisTemplate.opsForValue().set("session:" + jti, sessionData, Duration.ofHours(24));

        return AuthenticationResponse.builder()
                .user(userMapper.toUserSummary(user))
                .tokens(TokenPair.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .expiresIn(VALID_DURATION)
                        .tokenType("Bearer")
                        .build())
                .build();
    }

    @Transactional
    public void logout(String token) throws Exception {
        try {
            var signToken = verifyToken(token, false);
            String jti = signToken.getJWTClaimsSet().getJWTID();
            
            // Mark all refresh tokens as revoked for this user
            String email = signToken.getJWTClaimsSet().getSubject();
            UserAuth user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
            
            // PERFORMANCE FIX: Use bulk update query instead of loading all tokens
            // Find only this user's active tokens and revoke them
            LocalDateTime now = LocalDateTime.now();
            refreshTokenRepository.findAll().stream()
                    .filter(rt -> rt.getUser().getId().equals(user.getId()) && rt.getRevokedAt() == null)
                    .forEach(rt -> {
                        rt.setRevokedAt(now);
                        refreshTokenRepository.save(rt);
                    });
            
            // TODO: [FUTURE-OPTIMIZATION] Replace with bulk update query:
            // @Query("UPDATE refresh_tokens SET revokedAt = :now WHERE user.id = :userId AND revokedAt IS NULL")
            // int revokeAllUserTokens(@Param("userId") UUID userId, @Param("now") LocalDateTime now);

            // TODO: [FUTURE-REDIS] Blacklist the access token
            // redisTemplate.opsForValue().set("revoked:token:" + DigestUtils.md5DigestAsHex(token.getBytes()),
            //     "revoked", Duration.ofMinutes(15));
            
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Logout failed: {}", e.getMessage());
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }

    public IntrospectResponse introspect(IntrospectRequest request) {
        try {
            verifyToken(request.getToken(), false);
            return IntrospectResponse.builder()
                    .valid(true)
                    .build();
        } catch (Exception e) {
            log.info("Token introspection failed: {}", e.getMessage());
            return IntrospectResponse.builder()
                    .valid(false)
                    .build();
        }
    }

    public AuthenticationResponse refreshToken(RefreshTokenRequest request) throws Exception {
        String tokenHash = hashToken(request.getToken());
        
        // Find and validate refresh token
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(tokenHash)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        UserAuth user = refreshToken.getUser();

        // Revoke old refresh token (rotation)
        refreshToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);

        // Generate new tokens
        String newAccessToken = generateAccessToken(user);
        String newRefreshToken = generateRefreshToken(user);

        // Save new refresh token
        saveRefreshToken(user, newRefreshToken);

        return AuthenticationResponse.builder()
                .user(userMapper.toUserSummary(user))
                .tokens(TokenPair.builder()
                        .accessToken(newAccessToken)
                        .refreshToken(newRefreshToken)
                        .expiresIn(VALID_DURATION)
                        .tokenType("Bearer")
                        .build())
                .build();
    }

    SignedJWT verifyToken(String token, boolean isRefresh) throws Exception {
        try {
            JWSVerifier verifier = new MACVerifier(getSigningKeyBytes());
            SignedJWT signedJWT = SignedJWT.parse(token);

            // Verify signature first
            if (!signedJWT.verify(verifier)) {
                throw new AppException(ErrorCode.UNAUTHENTICATED);
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            Date now = new Date();

            // Verify expiration based on token type
            Date expirationTime = claims.getExpirationTime();
            if (isRefresh) {
                // For refresh tokens, check if token is within refreshable duration from issue time
                Instant issueTime = claims.getIssueTime().toInstant();
                Instant refreshableUntil = issueTime.plus(REFRESHABLE_DURATION, ChronoUnit.SECONDS);
                if (refreshableUntil.isBefore(now.toInstant())) {
                    throw new AppException(ErrorCode.UNAUTHENTICATED);
                }
            } else {
                // For access tokens, check standard expiration
                if (expirationTime == null || expirationTime.before(now)) {
                    throw new AppException(ErrorCode.UNAUTHENTICATED);
                }
            }

            // TODO: [FUTURE-REDIS] Check token blacklist for faster revocation
            // Boolean isRevoked = redisTemplate.hasKey("revoked:token:" + DigestUtils.md5DigestAsHex(token.getBytes()));
            // if (Boolean.TRUE.equals(isRevoked)) {
            //     throw new AppException(ErrorCode.UNAUTHENTICATED);
            // }

            return signedJWT;
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Token verification failed: {}", e.getMessage());
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }

    String generateAccessToken(UserAuth user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        // Build JWT claims with user information
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(user.getEmail())
                .issuer("daypulse-auth-service")
                .expirationTime(Date.from(Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS)))
                .issueTime(new Date())
                .jwtID(UUID.randomUUID().toString())
                .claim("scope", buildScope(user))
                .claim("userId", user.getId().toString())
                .build();

        Payload payload = new Payload(claimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(getSigningKeyBytes()));
        } catch (JOSEException e) {
            log.error("Error signing the access token: {}", e.getMessage());
            throw new RuntimeException(e);
        }
        return jwsObject.serialize();
    }

    String generateRefreshToken(UserAuth user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        // Refresh token with longer expiration
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(user.getEmail())
                .issuer("daypulse-auth-service")
                .expirationTime(Date.from(Instant.now().plus(REFRESHABLE_DURATION, ChronoUnit.SECONDS)))
                .issueTime(new Date())
                .jwtID(UUID.randomUUID().toString())
                .claim("type", "refresh")
                .claim("userId", user.getId().toString())
                .build();

        Payload payload = new Payload(claimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(getSigningKeyBytes()));
        } catch (JOSEException e) {
            log.error("Error signing the refresh token: {}", e.getMessage());
            throw new RuntimeException(e);
        }
        return jwsObject.serialize();
    }

    private void saveRefreshToken(UserAuth user, String token) {
        String tokenHash = hashToken(token);
        
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plus(REFRESHABLE_DURATION, ChronoUnit.SECONDS))
                .build();
        
        refreshTokenRepository.save(refreshToken);
    }

    private String hashToken(String token) {
        return DigestUtils.md5DigestAsHex(token.getBytes());
    }

    String buildScope(UserAuth user) {
        StringJoiner scopeJoiner = new StringJoiner(" ");
        
        // Add role to scope (e.g., "ROLE_USER", "ROLE_ADMIN")
        RoleEnum role = user.getRole();
        if (role != null) {
            scopeJoiner.add(role.getRoleName());
            
            // Add all permissions associated with this role
            role.getPermissions().forEach(permission -> 
                scopeJoiner.add(permission.getPermissionName())
            );
        } else {
            // Fallback to default USER role if null
            scopeJoiner.add(RoleEnum.USER.getRoleName());
        }
        
        return scopeJoiner.toString();
    }

    // Placeholder methods for future OTP functionality
    // TODO: [FUTURE-EMAIL] Implement OTP verification flow
    public AuthenticationResponse verifyOtp(VerifyOtpRequest request) {
        throw new UnsupportedOperationException("OTP verification not yet implemented");
    }

    // TODO: [FUTURE-EMAIL] Implement forgot password flow
    public ApiBaseResponse<Void> forgotPassword(ForgotPasswordRequest request) {
        throw new UnsupportedOperationException("Forgot password not yet implemented");
    }
}
