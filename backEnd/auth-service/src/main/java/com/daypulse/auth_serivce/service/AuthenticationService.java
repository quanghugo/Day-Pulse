package com.daypulse.auth_serivce.service;

import com.daypulse.auth_serivce.dto.request.AuthenticationRequest;
import com.daypulse.auth_serivce.dto.request.IntrospectRequest;
import com.daypulse.auth_serivce.dto.request.RefreshTokenRequest;
import com.daypulse.auth_serivce.dto.response.AuthenticationResponse;
import com.daypulse.auth_serivce.dto.response.IntrospectResponse;
import com.daypulse.auth_serivce.entity.InvalidedToken;
import com.daypulse.auth_serivce.entity.User;
import com.daypulse.auth_serivce.exception.AppException;
import com.daypulse.auth_serivce.exception.ErrorCode;
import com.daypulse.auth_serivce.repository.InvalidedTokenRepository;
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
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.StringJoiner;
import java.util.UUID;

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
    InvalidedTokenRepository invalidedTokenRepository;

    private byte[] getSigningKeyBytes() {
        return Base64.getDecoder().decode(SIGNING_KEY);
    }

    public AuthenticationResponse authenticate(AuthenticationRequest authenticationRequest) {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        var userOptional = userRepository.findByUsername(authenticationRequest.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        boolean authenticated = passwordEncoder.matches(authenticationRequest.getPassword(),
                userOptional.getPassword());

        if (!authenticated) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        var token = generateToken(userOptional);

        return AuthenticationResponse.builder()
                .authenticated(true)
                .token(token)
                .build();
    }

    public void logout(String token) throws Exception {
        try {
            var signToken = verifyToken(token, true);
            String jit = signToken.getJWTClaimsSet().getJWTID();
            Date expiredTime = signToken.getJWTClaimsSet().getExpirationTime();
            invalidedTokenRepository.save(InvalidedToken.builder()
                    .id(jit)
                    .expiredTime(expiredTime)
                    .build());
        } catch (Exception e) {
            log.error("Token ready expired : {}", e.getMessage());
            throw new RuntimeException(e);
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

    public AuthenticationResponse refreshToken(RefreshTokenRequest request) throws Exception{
        var signnedJWT = verifyToken(request.getToken(), true);

        String jit = signnedJWT.getJWTClaimsSet().getJWTID();
        Date expiredTime = signnedJWT.getJWTClaimsSet().getExpirationTime();
        invalidedTokenRepository.save(InvalidedToken.builder()
                .id(jit)
                .expiredTime(expiredTime)
                .build());

        var userName = signnedJWT.getJWTClaimsSet().getSubject();
        User user = userRepository.findByUsername(userName)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        String token = generateToken(user);    // new token

        return AuthenticationResponse.builder()
                .authenticated(true)
                .token(token)
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

            // Check if token has been invalidated (logged out)
            if (invalidedTokenRepository.existsById(claims.getJWTID())) {
                throw new AppException(ErrorCode.UNAUTHENTICATED);
            }

            return signedJWT;
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Token verification failed: {}", e.getMessage());
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }

    String generateToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        // Build JWT claims with user information
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issuer("qnit18.com")
                .expirationTime(Date.from(Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS)))
                .issueTime(new Date())
                .jwtID(UUID.randomUUID().toString())
                .claim("scope", buildScope(user))
                .claim("userId", user.getId())
                .build();

        Payload payload = new Payload(claimsSet.toJSONObject());

        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(getSigningKeyBytes()));
        } catch (JOSEException e) {
            log.info("Error signing the token: {}", e.getMessage());
            throw new RuntimeException(e);
        }
        return jwsObject.serialize();
    }

    String buildScope(User user) {
        StringJoiner scopeJoiner = new StringJoiner(" ");
        if (!CollectionUtils.isEmpty(user.getRoles())) {
            user.getRoles().forEach(role -> {
                scopeJoiner.add("ROLE_" + role.getName());
                if (!CollectionUtils.isEmpty(role.getPermissions())) {
                    role.getPermissions().forEach(permission -> scopeJoiner.add(permission.getName()));
                }
            });
            return scopeJoiner.toString();
        } else {
            return "";
        }
    }
}
