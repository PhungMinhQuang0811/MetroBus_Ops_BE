package com.vdt.auth_ops_service.common.exception;

import com.vdt.auth_ops_service.dto.response.ApiResponse;
import jakarta.validation.ConstraintViolation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    private final String[] VALIDATORS_ATTRIBUTES = {
            "fieldName"
    };

    @ExceptionHandler(value = Exception.class)
    ResponseEntity<ApiResponse<?>> handlingRuntimeException(Exception exception) {
        log.error("Exception: ", exception);
        ApiResponse<?> apiResponse = new ApiResponse<>();

        apiResponse.setCode(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode());
        apiResponse.setMessage(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage());

        return ResponseEntity.status(ErrorCode.UNCATEGORIZED_EXCEPTION.getHttpStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(value = AuthenticationException.class)
    ResponseEntity<ApiResponse<?>> handlingAuthenticationException(AuthenticationException exception) {
        ApiResponse<?> apiResponse = new ApiResponse<>();

        apiResponse.setCode(ErrorCode.INVALID_CREDENTIALS.getCode());
        apiResponse.setMessage(ErrorCode.INVALID_CREDENTIALS.getMessage());

        return ResponseEntity.status(ErrorCode.INVALID_CREDENTIALS.getHttpStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ApiResponse<?>> handlingAppException(AppException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        ApiResponse<?> apiResponse = new ApiResponse<>();

        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(exception.getMessage());

        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<?>> handlingValidation(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getFieldError();
        String enumKey = Objects.requireNonNull(fieldError).getDefaultMessage();

        ErrorCode errorCode = ErrorCode.INVALID_ERROR_KEY;
        try {
            errorCode = ErrorCode.valueOf(enumKey);
        } catch (IllegalArgumentException e) {
            // Log and keep default ErrorCode.INVALID_ERROR_KEY
        }

        Map<String, Object> attributes = new HashMap<>();
        String fieldName = "";

        try {
            ConstraintViolation<?> violation = fieldError.unwrap(ConstraintViolation.class);
            Map<String, Object> constraintAttributes = violation.getConstraintDescriptor().getAttributes();
            fieldName = constraintAttributes.getOrDefault("fieldName", "").toString();
        } catch (Exception e) {
            // Error unwrapping or getting attribute
        }

        attributes.put("fieldName", fieldName);

        ApiResponse<?> apiResponse = new ApiResponse<>();
        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(mapAttributeMessage(errorCode.getMessage(), attributes));

        return ResponseEntity.status(errorCode.getHttpStatusCode()).body(apiResponse);
    }

    private String mapAttributeMessage(String message, Map<String, Object> attributes) {
        for (String attribute : VALIDATORS_ATTRIBUTES) {
            String placeholder = "{" + attribute + "}";
            if (message.contains(placeholder)) {
                message = message.replace(placeholder, attributes.get(attribute).toString());
            }
        }
        return message;
    }
}
