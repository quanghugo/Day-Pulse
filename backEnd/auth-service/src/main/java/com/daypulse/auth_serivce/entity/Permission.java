package com.daypulse.auth_serivce.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity(name = "permissions")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Permission {
    @Id
    String name;
    String description;
}

