package com.daypulse.user_service.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProfileUpdateRequest {
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    String username;

    @Size(max = 100, message = "Name must be at most 100 characters")
    String name;

    @Size(max = 500, message = "Bio must be at most 500 characters")
    String bio;

    @Size(max = 500, message = "Avatar URL must be at most 500 characters")
    String avatarUrl;

    @Size(max = 50, message = "Timezone must be at most 50 characters")
    String timezone;

    @Size(max = 5, message = "Language must be at most 5 characters")
    String language;
}
