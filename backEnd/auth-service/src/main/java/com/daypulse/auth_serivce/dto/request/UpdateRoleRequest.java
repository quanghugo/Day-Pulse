package com.daypulse.auth_serivce.dto.request;

import com.daypulse.auth_serivce.enums.RoleEnum;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Request DTO for updating a user's role
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateRoleRequest {
    
    @NotNull(message = "Role is required")
    RoleEnum role;
}
