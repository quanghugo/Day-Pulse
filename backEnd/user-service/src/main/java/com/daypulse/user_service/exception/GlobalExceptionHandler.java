package com.daypulse.user_service.exception;

import com.daypulse.user_service.dto.response.ApiResponse;
import jakarta.validation.ConstraintViolation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;
import java.util.Objects;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
    private static final String MIN_ATTRIBUTE = "min";

    @ExceptionHandler(value = RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handlingRuntimeException(RuntimeException exception) {
        log.error("RuntimeException occurred: {}", exception.getMessage(), exception);
        
        // Check if it's a known error message
        String message = exception.getMessage();
        ErrorCode errorCode;
        
        if (message != null) {
            if (message.contains(ErrorCode.PROFILE_NOT_FOUND.getMessage()) || 
                message.contains("Profile not found")) {
                errorCode = ErrorCode.PROFILE_NOT_FOUND;
            } else if (message.contains(ErrorCode.USER_NOT_FOUND.getMessage()) || 
                       message.contains("User not found")) {
                errorCode = ErrorCode.USER_NOT_FOUND;
            } else if (message.contains(ErrorCode.USERNAME_ALREADY_EXISTS.getMessage()) || 
                       message.contains("Username already exists")) {
                errorCode = ErrorCode.USERNAME_ALREADY_EXISTS;
            } else {
                errorCode = ErrorCode.UNCATEGORIZED_EXCEPTION;
            }
        } else {
            errorCode = ErrorCode.UNCATEGORIZED_EXCEPTION;
        }
        
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
        
        return ResponseEntity.status(errorCode.getStatusCode()).body(response);
    }

    @ExceptionHandler(value = AppException.class)
    public ResponseEntity<ApiResponse<Void>> handlingAppException(AppException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        log.error("AppException occurred: {}", errorCode.getMessage());
        
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
        
        return ResponseEntity.status(errorCode.getStatusCode()).body(response);
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handlingValidation(MethodArgumentNotValidException exception) {
        String enumKey = exception.getFieldError() != null 
                ? exception.getFieldError().getDefaultMessage() 
                : "INVALID_REQUEST";

        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;
        Map<String, Object> attributes = null;

        try {
            errorCode = ErrorCode.valueOf(enumKey);

            if (!exception.getBindingResult().getAllErrors().isEmpty()) {
                var constraintViolation = exception.getBindingResult()
                        .getAllErrors().getFirst().unwrap(ConstraintViolation.class);
                attributes = constraintViolation.getConstraintDescriptor().getAttributes();
            }
        } catch (IllegalArgumentException e) {
            log.error("Invalid key: {}", enumKey);
        }

        log.info("Attributes: {}", attributes);
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(errorCode.getCode())
                .message(Objects.nonNull(attributes)
                        ? mapAttributeToMessage(attributes, errorCode.getMessage())
                        : errorCode.getMessage())
                .build();

        return ResponseEntity.status(errorCode.getStatusCode()).body(response);
    }

    private String mapAttributeToMessage(Map<String, Object> attributes, String message) {
        String minValue = String.valueOf(attributes.get(MIN_ATTRIBUTE));
        return message.replace("{" + MIN_ATTRIBUTE + "}", minValue);
    }
}
