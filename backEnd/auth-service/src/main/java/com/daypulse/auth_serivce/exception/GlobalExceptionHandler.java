package com.daypulse.auth_serivce.exception;

import com.daypulse.auth_serivce.dto.response.ApiBaseResponse;
import jakarta.validation.ConstraintViolation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;
import java.util.Objects;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
    private static final String MIN_ATTRIBUTE = "min";


    @ExceptionHandler(value = Exception.class)
    ResponseEntity<ApiBaseResponse> handlingRuntimeException(RuntimeException exception) {
        ApiBaseResponse ApiBaseResponse = new ApiBaseResponse();

        ApiBaseResponse.setCode(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode());
        ApiBaseResponse.setMessage(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage());

        return ResponseEntity.badRequest().body(ApiBaseResponse);
    }

    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ApiBaseResponse> handlingAppException(AppException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        ApiBaseResponse ApiBaseResponse = new ApiBaseResponse();

        ApiBaseResponse.setCode(errorCode.getCode());
        ApiBaseResponse.setMessage(errorCode.getMessage());

        return ResponseEntity.badRequest().body(ApiBaseResponse);
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<ApiBaseResponse> handlingValidation(MethodArgumentNotValidException exception) {
        String enumKey = exception.getFieldError().getDefaultMessage();

        ErrorCode errorCode = ErrorCode.INVALID_KEY;

        Map<String, Object> attributes = null;

        try {
            errorCode = ErrorCode.valueOf(enumKey);

            var constraintViolation = exception.getBindingResult()
                    .getAllErrors().getFirst().unwrap(ConstraintViolation.class);

            attributes = constraintViolation.getConstraintDescriptor().getAttributes();

        } catch (IllegalArgumentException e) {
            log.error("Invalid key: " + enumKey);
        }

        log.info("Attributes: " + attributes);
        ApiBaseResponse ApiBaseResponse = new ApiBaseResponse();

        ApiBaseResponse.setCode(errorCode.getCode());
        ApiBaseResponse.setMessage(Objects.nonNull(attributes)
                ? mapAttributeToMessage(attributes, errorCode.getMessage())
                : errorCode.getMessage());

        return ResponseEntity.badRequest().body(ApiBaseResponse);
    }

    private String mapAttributeToMessage(Map<String, Object> attributes, String message) {
        String minValue = String.valueOf(attributes.get(MIN_ATTRIBUTE));
        return message.replace("{" + MIN_ATTRIBUTE + "}", minValue);
    }

    @ExceptionHandler(value = AccessDeniedException.class)
    ResponseEntity<ApiBaseResponse> handlingAccessDenied(AccessDeniedException exception) {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;

        return ResponseEntity.status(errorCode.getStatusCode()).body(
                ApiBaseResponse.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    @ExceptionHandler(value = AuthorizationDeniedException.class)
    ResponseEntity<ApiBaseResponse> handlingAuthorizationDenied(AuthorizationDeniedException exception) {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;

        return ResponseEntity.status(errorCode.getStatusCode()).body(
                ApiBaseResponse.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

}
