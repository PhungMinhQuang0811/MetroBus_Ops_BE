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
    SUCCESS(1000, "Success", HttpStatus.OK),

    UNAUTHENTICATED(4002, "Unauthenticated access", HttpStatus.UNAUTHORIZED),
    ACCOUNT_DISABLED(4006, "Your account is currently disabled or inactive.", HttpStatus.FORBIDDEN),
    ACCESS_DENIED(4007, "You do not have permission to access this resource", HttpStatus.FORBIDDEN),
    INVALID_CSRF_TOKEN(4009, "Missing or invalid CSRF token", HttpStatus.FORBIDDEN);

    int code;
    String message;
    HttpStatusCode httpStatusCode;
}
