package com.daypulse.api_gateway.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Utility for service-to-service authentication.
 * 
 * Generates HMAC signatures for gateway headers to ensure downstream services
 * can verify requests come from the trusted gateway.
 */
@Slf4j
@Component
public class ServiceAuthenticationUtil {

    private final String serviceSecretKey;
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    public ServiceAuthenticationUtil(@Value("${jwt.signing-key}") String serviceSecretKey) {
        this.serviceSecretKey = serviceSecretKey;
    }

    /**
     * Generate HMAC signature for gateway headers.
     * 
     * @param userId User ID to sign
     * @param roles User roles to sign
     * @param timestamp Request timestamp
     * @return Base64-encoded HMAC signature
     */
    public String generateSignature(String userId, String roles, long timestamp) {
        try {
            String dataToSign = String.format("%s|%s|%d", userId, roles, timestamp);
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    Base64.getDecoder().decode(serviceSecretKey),
                    HMAC_ALGORITHM
            );
            mac.init(secretKeySpec);
            byte[] signatureBytes = mac.doFinal(dataToSign.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signatureBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to generate service signature", e);
            throw new RuntimeException("Failed to generate service signature", e);
        }
    }

    /**
     * Verify HMAC signature.
     * 
     * @param userId User ID from header
     * @param roles User roles from header
     * @param timestamp Request timestamp
     * @param signature Signature to verify
     * @return true if signature is valid
     */
    public boolean verifySignature(String userId, String roles, long timestamp, String signature) {
        String expectedSignature = generateSignature(userId, roles, timestamp);
        return expectedSignature.equals(signature);
    }
}
