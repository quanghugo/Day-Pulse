package com.daypulse.api_gateway.exception;

/**
 * Exception thrown when authentication fails.
 * 
 * This exception is used to indicate that a request could not be authenticated,
 * typically due to invalid, expired, or missing JWT token.
 */
public class AuthenticationException extends RuntimeException {

    private final String errorCode;
    private final int httpStatus;

    public AuthenticationException(String message) {
        super(message);
        this.errorCode = "UNAUTHENTICATED";
        this.httpStatus = 401;
    }

    public AuthenticationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = 401;
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "UNAUTHENTICATED";
        this.httpStatus = 401;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
