package com.daypulse.auth_serivce.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Objects;

public class CustomJwtDecoder implements JwtDecoder {

    @Value("${jwt.signing-key}")
    private String SIGNING_KEY;

    private NimbusJwtDecoder nimbusJwtDecoder = null;

    @Override
    public Jwt decode(String token) throws JwtException {
        if (Objects.isNull(nimbusJwtDecoder)) {
            byte[] keyBytes = Base64.getDecoder().decode(SIGNING_KEY);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "HmacSHA512");
            nimbusJwtDecoder = NimbusJwtDecoder
                    .withSecretKey(secretKey)
                    .macAlgorithm(MacAlgorithm.HS512)
                    .build();
        }

        return nimbusJwtDecoder.decode(token);
    }
}
