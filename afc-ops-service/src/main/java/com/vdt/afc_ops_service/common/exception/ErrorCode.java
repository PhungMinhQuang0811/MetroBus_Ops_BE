package com.vdt.afc_ops_service.common.exception;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ErrorCode {
    /**
     * Range 1xxx: General / Success
     */
    SUCCESS(1000, "Success", HttpStatus.OK),

    /**
     * Range 2xxx: Validation errors
     */
    FIELD_REQUIRED(2000, "{fieldName} is required", HttpStatus.BAD_REQUEST),
    INVALID_PAGE_REQUEST(2001, "Page must be >= 0 and size must be between 1 and 100", HttpStatus.BAD_REQUEST),
    INVALID_SEARCH_KEYWORD(2002, "Search keyword is too long", HttpStatus.BAD_REQUEST),
    INVALID_OPERATOR_ID(2003, "Operator id is invalid", HttpStatus.BAD_REQUEST),
    INVALID_ROUTE_ID(2004, "Route id is invalid", HttpStatus.BAD_REQUEST),
    INVALID_TRANSPORT_TYPE(2005, "Invalid transport type", HttpStatus.BAD_REQUEST),
    INVALID_MASTER_DATA_STATUS(2006, "Invalid master data status", HttpStatus.BAD_REQUEST),
    INVALID_ROUTE_NAME_LENGTH(2007, "Route name must not exceed 255 characters", HttpStatus.BAD_REQUEST),

    /**
     * Range 3xxx: Business logic & Database errors
     */
    ROUTE_CODE_EXISTED(3000, "Route code already exists in operator", HttpStatus.BAD_REQUEST),
    ROUTE_ALREADY_ENABLED(3001, "Route is already active", HttpStatus.BAD_REQUEST),
    ROUTE_ALREADY_DISABLED(3002, "Route is already disabled", HttpStatus.BAD_REQUEST),
    OPERATOR_NOT_FOUND(3003, "Operator not found", HttpStatus.NOT_FOUND),
    ROUTE_NOT_FOUND(3004, "Route not found", HttpStatus.NOT_FOUND),
    IMPORT_FILE_INVALID(3014, "Import file is invalid", HttpStatus.BAD_REQUEST),
    IMPORT_FILE_HAS_ERRORS(3015, "Import file contains invalid rows", HttpStatus.BAD_REQUEST),

    /**
     * Range 4xxx: Security, Authentication & System errors
     */
    UNCATEGORIZED_EXCEPTION(4000, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    UNAUTHENTICATED(4002, "Unauthenticated access", HttpStatus.UNAUTHORIZED),
    ACCOUNT_DISABLED(4006, "Your account is currently disabled or inactive.", HttpStatus.FORBIDDEN),
    ACCESS_DENIED(4007, "You do not have permission to access this resource", HttpStatus.FORBIDDEN),
    INVALID_CSRF_TOKEN(4009, "Missing or invalid CSRF token", HttpStatus.FORBIDDEN),
    OPERATOR_SCOPE_REQUIRED(4012, "Operator scope is required", HttpStatus.FORBIDDEN),
    OPERATOR_ACCESS_DENIED(4013, "You do not have permission to access data from another operator", HttpStatus.FORBIDDEN);

    int code;
    String message;
    HttpStatusCode httpStatusCode;
}
