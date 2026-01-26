package com.daypulse.api_gateway.exception;

/**
 * Exception thrown when authorization fails.
 * 
 * This exception is used to indicate that an authenticated request
 * does not have sufficient permissions to access a resource.
 */
public class AuthorizationException extends RuntimeException {

    private final String errorCode;
    private final int httpStatus;

    public AuthorizationException(String message) {
        super(message);
        this.errorCode = "FORBIDDEN";
        this.httpStatus = 403;
    }

    public AuthorizationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = 403;
    }

    public AuthorizationException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "FORBIDDEN";
        this.httpStatus = 403;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
