package com.vdt.auth_ops_service.dto.response.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthAuditLogResponse {
    String id;
    String accountId;
    String username;
    String action;
    String resourceType;
    String resourceId;
    String resourceName;
    String result;
    String requestId;
    String ipAddress;
    String userAgent;
    String httpMethod;
    String requestPath;
    String requestData;
    String responseData;
    String errorMessage;
    LocalDateTime createdAt;
}
