package com.daypulse.auth_serivce.entity;

import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity(name = "invalided_tokens")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InvalidedToken {
    @Id
    String id;
    Date expiredTime;
}

