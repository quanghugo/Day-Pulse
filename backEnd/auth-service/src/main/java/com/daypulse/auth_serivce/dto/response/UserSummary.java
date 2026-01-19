package com.daypulse.auth_serivce.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserSummary {
    UUID id;
    String email;
    Boolean isEmailVerified;
    Boolean isSetupComplete;
}
