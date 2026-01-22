package com.daypulse.auth_serivce.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

/**
 * Response DTO for role information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoleInfoResponse {
    String name;
    String displayName;
    Set<String> permissions;
}
