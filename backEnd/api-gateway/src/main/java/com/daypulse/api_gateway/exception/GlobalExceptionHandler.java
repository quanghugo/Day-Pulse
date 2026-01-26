package com.daypulse.api_gateway.exception;

import com.daypulse.api_gateway.dto.ApiBaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global exception handler for API Gateway.
 * 
 * Handles authentication and authorization exceptions, converting them
 * to appropriate HTTP responses.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public Mono<ResponseEntity<ApiBaseResponse<Void>>> handleAuthenticationException(
            AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        
        ApiBaseResponse<Void> response = ApiBaseResponse.<Void>builder()
                .code(401)
                .message(ex.getMessage())
                .build();
        
        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response));
    }

    @ExceptionHandler(AuthorizationException.class)
    public Mono<ResponseEntity<ApiBaseResponse<Void>>> handleAuthorizationException(
            AuthorizationException ex) {
        log.warn("Authorization failed: {}", ex.getMessage());
        
        ApiBaseResponse<Void> response = ApiBaseResponse.<Void>builder()
                .code(403)
                .message(ex.getMessage())
                .build();
        
        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(response));
    }
}
