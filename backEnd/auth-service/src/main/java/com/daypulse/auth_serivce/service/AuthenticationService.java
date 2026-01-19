package com.daypulse.auth_serivce.service;

import com.daypulse.auth_serivce.dto.request.AuthenticationRequest;
import com.daypulse.auth_serivce.dto.response.AuthenticationResponse;
import com.daypulse.auth_serivce.entity.User;
import com.daypulse.auth_serivce.exception.AppException;
import com.daypulse.auth_serivce.exception.ErrorCode;
import com.daypulse.auth_serivce.repository.InvalidedTokenRepository;
import com.daypulse.auth_serivce.repository.UserRepository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
            jwsObject.sign(new MACSigner(SIGNING_KEY));
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
