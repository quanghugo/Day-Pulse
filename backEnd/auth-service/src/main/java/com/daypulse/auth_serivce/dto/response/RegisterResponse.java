package com.daypulse.auth_serivce.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

/**
 * Response for user registration/signup
 * STANDARD: Can optionally include tokens for auto-login after signup
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegisterResponse {
    @Builder.Default
    Boolean success = true;
    UUID userId;
    String email;
    
    // Optional: for auto-login after registration
    TokenPair tokens;
    UserSummary user;
}
