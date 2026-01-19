package com.daypulse.user_service.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserSummaryResponse {
    UUID id;
    String username;
    String name;
    String avatarUrl;
    Boolean isOnline;
}
