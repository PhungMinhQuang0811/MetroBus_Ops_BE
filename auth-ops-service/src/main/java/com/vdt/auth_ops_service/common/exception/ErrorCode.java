package com.vdt.auth_ops_service.common.exception;

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
    INVALID_PASSWORD(2001, "Password must be at least 9 characters and contain both letters and numbers", HttpStatus.BAD_REQUEST),
    INVALID_ROLE_SELECTION(2002, "Invalid operator role selection", HttpStatus.BAD_REQUEST),
    INVALID_PAGE_REQUEST(2003, "Page must be >= 0 and size must be between 1 and 100", HttpStatus.BAD_REQUEST),
    INVALID_ACCOUNT_ID(2004, "Account id is invalid", HttpStatus.BAD_REQUEST),
    INVALID_SEARCH_KEYWORD(2005, "Search keyword is too long", HttpStatus.BAD_REQUEST),
    PASSWORD_CONFIRMATION_MISMATCH(2006, "New password and confirm password do not match", HttpStatus.BAD_REQUEST),
    CURRENT_PASSWORD_INCORRECT(2007, "Current password is incorrect", HttpStatus.BAD_REQUEST),
    PASSWORD_RESET_NOT_REQUESTED(2008, "Password reset has not been requested for this account", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD_STATUS(2009, "Invalid password status", HttpStatus.BAD_REQUEST),

    /**
     * Range 3xxx: Business logic & Database errors
     */
    INVALID_CREDENTIALS(3000, "Username or password is incorrect", HttpStatus.BAD_REQUEST),
    USER_EXISTED(3002, "Username already exists", HttpStatus.BAD_REQUEST),
    TOKEN_BLACKLIST_FAILED(3003, "Failed to blacklist token", HttpStatus.INTERNAL_SERVER_ERROR),
    TOKEN_GENERATION_FAILED(3004, "Failed to generate token", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_TOKEN_FORMAT(3005, "Invalid token format", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(3007, "User not found", HttpStatus.NOT_FOUND),
    LOGOUT_FAILED(3008, "Failed to log out. Please try again.", HttpStatus.INTERNAL_SERVER_ERROR),
    ROLE_NOT_FOUND(3009, "Role not found", HttpStatus.NOT_FOUND),
    PERMISSION_NOT_FOUND(3010, "Permission not found", HttpStatus.NOT_FOUND),
    OPERATOR_ADMIN_STATUS_CHANGE_NOT_ALLOWED(3011, "Operator admin account status cannot be changed", HttpStatus.BAD_REQUEST),
    ACCOUNT_ALREADY_DISABLED(3012, "Account is already disabled", HttpStatus.BAD_REQUEST),
    ACCOUNT_ALREADY_ENABLED(3013, "Account is already enabled", HttpStatus.BAD_REQUEST),
    IMPORT_FILE_INVALID(3014, "Import file is invalid", HttpStatus.BAD_REQUEST),
    IMPORT_FILE_HAS_ERRORS(3015, "Import file contains invalid rows", HttpStatus.BAD_REQUEST),

    /**
     * Range 4xxx: Security, Authentication & System errors
     */
    UNCATEGORIZED_EXCEPTION(4000, "There was error happen during run time", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_ERROR_KEY(4001, "The error key could be misspelled", HttpStatus.INTERNAL_SERVER_ERROR),
    UNAUTHENTICATED(4002, "Unauthenticated access", HttpStatus.UNAUTHORIZED),
    INVALID_REFRESH_TOKEN(4003, "Invalid refresh token. Please try again.", HttpStatus.UNAUTHORIZED),
    INVALID_ONETIME_TOKEN(4004, "The token is invalid or this link has expired or has been used.", HttpStatus.BAD_REQUEST),
    ACCOUNT_DISABLED(4006, "Your account is currently disabled or inactive.", HttpStatus.FORBIDDEN),
    ACCESS_DENIED(4007, "You do not have permission to access this resource", HttpStatus.FORBIDDEN),
    TOKEN_EXPIRED_OR_INVALID(4008, "Token is already expired or invalid", HttpStatus.BAD_REQUEST),
    INVALID_CSRF_TOKEN(4009, "Missing or invalid CSRF token", HttpStatus.FORBIDDEN),
    PASSWORD_RESET_REQUIRED(4010, "Your account requires an administrator to reset the password.", HttpStatus.FORBIDDEN),
    PASSWORD_CHANGE_REQUIRED(4011, "You must change your password before using this account.", HttpStatus.FORBIDDEN),
    OPERATOR_SCOPE_REQUIRED(4012, "Operator scope is required", HttpStatus.FORBIDDEN),
    OPERATOR_ACCESS_DENIED(4013, "You do not have permission to access data from another operator", HttpStatus.FORBIDDEN),
    ;

    int code;
    String message;
    HttpStatusCode httpStatusCode;

}
