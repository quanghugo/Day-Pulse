package com.daypulse.user_service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(888, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    PROFILE_NOT_FOUND(201, "Profile not found. Please set up your profile first.", HttpStatus.NOT_FOUND),
    USER_NOT_FOUND(202, "User not found", HttpStatus.NOT_FOUND),
    USERNAME_ALREADY_EXISTS(203, "Username already exists", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST(204, "Invalid request", HttpStatus.BAD_REQUEST)
    ;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;
}
