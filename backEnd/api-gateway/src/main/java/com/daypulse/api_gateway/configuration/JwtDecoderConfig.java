package com.daypulse.api_gateway.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
public class JwtDecoderConfig {

    @Value("${jwt.signing-key}")
    private String signingKey;

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        byte[] keyBytes = Base64.getDecoder().decode(signingKey);
        SecretKey secretKey = new SecretKeySpec(keyBytes, "HmacSHA512");
        return NimbusReactiveJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS512)
                .build();
    }
}
