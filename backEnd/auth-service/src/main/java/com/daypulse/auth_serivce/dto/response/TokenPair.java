package com.daypulse.auth_serivce.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Token pair response following OAuth 2.0 standard
 * STANDARD: Returns both access and refresh tokens
 * - accessToken: Short-lived JWT for API authentication (sent in Authorization header)
 * - refreshToken: Long-lived token for renewing access tokens
 * - expiresIn: Access token validity duration in seconds
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenPair {
    String accessToken;
    String refreshToken;
    Long expiresIn;  // Access token expiration time in seconds
    String tokenType;  // "Bearer"
}
