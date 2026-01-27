package com.daypulse.auth_serivce.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(888, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(101, "Uncategorized error", HttpStatus.BAD_REQUEST),
    USER_EXISTED(102, "User existed", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID(103, "Username must be at least {min} characters", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(104, "Password must be at least {min} characters", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(105, "User not found", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(106, "Unauthenticated user", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(107, "Unauthorized user", HttpStatus.FORBIDDEN),
    INVALID_DOB(108, "Date of birth must be at least {min} years old", HttpStatus.BAD_REQUEST),
    EMAIL_NOT_VERIFIED(109, "Email address is not verified", HttpStatus.FORBIDDEN),
    OTP_INVALID(110, "Verification code is invalid", HttpStatus.BAD_REQUEST),
    OTP_EXPIRED(111, "Verification code has expired", HttpStatus.BAD_REQUEST)
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
