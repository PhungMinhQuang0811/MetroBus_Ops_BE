package com.vdt.afc_ops_service.common.exception;

import com.vdt.afc_ops_service.dto.response.ApiResponse;
import jakarta.validation.ConstraintViolation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String[] VALIDATOR_ATTRIBUTES = {"fieldName"};

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity.status(errorCode.getHttpStatusCode())
                .body(ApiResponse.<Void>builder()
                        .code(errorCode.getCode())
                        .message(exception.getMessage())
                        .build());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameter(
            MissingServletRequestParameterException exception
    ) {
        ErrorCode errorCode = ErrorCode.FIELD_REQUIRED;
        return ResponseEntity.status(errorCode.getHttpStatusCode())
                .body(ApiResponse.<Void>builder()
                        .code(errorCode.getCode())
                        .message(exception.getParameterName() + " is required")
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getFieldError();
        String enumKey = Objects.requireNonNull(fieldError).getDefaultMessage();
        ErrorCode errorCode = resolveErrorCode(enumKey);
        Map<String, Object> attributes = extractValidationAttributes(fieldError);

        return ResponseEntity.status(errorCode.getHttpStatusCode())
                .body(ApiResponse.<Void>builder()
                        .code(errorCode.getCode())
                        .message(mapAttributeMessage(errorCode.getMessage(), attributes))
                        .build());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception
    ) {
        ErrorCode errorCode = switch (exception.getName()) {
            case "operatorId" -> ErrorCode.INVALID_OPERATOR_ID;
            case "routeId" -> ErrorCode.INVALID_ROUTE_ID;
            case "stationId" -> ErrorCode.INVALID_STATION_ID;
            case "deviceId" -> ErrorCode.INVALID_DEVICE_ID;
            default -> ErrorCode.FIELD_REQUIRED;
        };
        return ResponseEntity.status(errorCode.getHttpStatusCode())
                .body(ApiResponse.<Void>builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable() {
        ErrorCode errorCode = ErrorCode.FIELD_REQUIRED;
        return ResponseEntity.status(errorCode.getHttpStatusCode())
                .body(ApiResponse.<Void>builder()
                        .code(errorCode.getCode())
                        .message("Request body is invalid")
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        log.error("Unexpected exception", exception);
        ErrorCode errorCode = ErrorCode.UNCATEGORIZED_EXCEPTION;
        return ResponseEntity.status(errorCode.getHttpStatusCode())
                .body(ApiResponse.<Void>builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    private ErrorCode resolveErrorCode(String enumKey) {
        try {
            return ErrorCode.valueOf(enumKey);
        } catch (IllegalArgumentException exception) {
            return ErrorCode.FIELD_REQUIRED;
        }
    }

    private Map<String, Object> extractValidationAttributes(FieldError fieldError) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("fieldName", fieldError.getField());

        try {
            ConstraintViolation<?> violation = fieldError.unwrap(ConstraintViolation.class);
            attributes.putAll(violation.getConstraintDescriptor().getAttributes());
        } catch (Exception exception) {
            // Keep default field name from FieldError.
        }

        return attributes;
    }

    private String mapAttributeMessage(String message, Map<String, Object> attributes) {
        for (String attribute : VALIDATOR_ATTRIBUTES) {
            String placeholder = "{" + attribute + "}";
            if (message.contains(placeholder)) {
                message = message.replace(placeholder, attributes.get(attribute).toString());
            }
        }
        return message;
    }
}
