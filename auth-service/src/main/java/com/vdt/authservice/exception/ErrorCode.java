package com.vdt.authservice.exception;

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
    INVALID_EMAIL(2001, "Invalid email format", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(2002, "Password must be at least 9 characters and contain both letters and numbers", HttpStatus.BAD_REQUEST),
    INVALID_ROLE_SELECTION(2003, "Invalid role selection. You can only choose external roles.", HttpStatus.BAD_REQUEST),

    /**
     * Range 3xxx: Business logic & Database errors
     */
    INVALID_CREDENTIALS(3000, "Invalid email or password. Please try again.", HttpStatus.BAD_REQUEST),
    EMAIL_NOT_USED_BY_ANY_ACCOUNT(3001, "The email address you’ve entered does not exist. Please try again.", HttpStatus.BAD_REQUEST),
    USER_EXISTED(3002, "The username or email was existed", HttpStatus.BAD_REQUEST),
    TOKEN_BLACKLIST_FAILED(3003, "Failed to blacklist token", HttpStatus.INTERNAL_SERVER_ERROR),
    TOKEN_GENERATION_FAILED(3004, "Failed to generate token", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_TOKEN_FORMAT(3005, "Invalid token format", HttpStatus.BAD_REQUEST),
    USER_ALREADY_VERIFIED(3006, "This account was already verified before", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(3007, "User not found", HttpStatus.NOT_FOUND),
    LOGOUT_FAILED(3008, "Failed to log out. Please try again.", HttpStatus.INTERNAL_SERVER_ERROR),
    ROLE_NOT_FOUND(3009, "Role not found", HttpStatus.NOT_FOUND),
    PERMISSION_NOT_FOUND(3010, "Permission not found", HttpStatus.NOT_FOUND),

    /**
     * Range 4xxx: Security, Authentication & System errors
     */
    UNCATEGORIZED_EXCEPTION(4000, "There was error happen during run time", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_ERROR_KEY(4001, "The error key could be misspelled", HttpStatus.INTERNAL_SERVER_ERROR),
    UNAUTHENTICATED(4002, "Unauthenticated access", HttpStatus.UNAUTHORIZED),
    INVALID_REFRESH_TOKEN(4003, "Invalid refresh token. Please try again.", HttpStatus.UNAUTHORIZED),
    INVALID_ONETIME_TOKEN(4004, "The token is invalid or this link has expired or has been used.", HttpStatus.BAD_REQUEST),
    EMAIL_NOT_VERIFIED(4005, "Your email has not been verified. Please check your inbox.", HttpStatus.FORBIDDEN),
    ACCOUNT_DISABLED(4006, "Your account is currently disabled or inactive.", HttpStatus.FORBIDDEN),
    ACCESS_DENIED(4007, "You do not have permission to access this resource", HttpStatus.FORBIDDEN),
    TOKEN_EXPIRED_OR_INVALID(4008, "Token is already expired or invalid", HttpStatus.BAD_REQUEST),
    INVALID_CSRF_TOKEN(4009, "Missing or invalid CSRF token", HttpStatus.FORBIDDEN),
    ;

    int code;
    String message;
    HttpStatusCode httpStatusCode;
}
