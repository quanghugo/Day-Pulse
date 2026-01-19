package com.daypulse.auth_serivce.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiBaseResponse <T> {
    @Builder.Default
    int code = 1000;
    @Builder.Default
    String message = "Success";
    T result;
}

