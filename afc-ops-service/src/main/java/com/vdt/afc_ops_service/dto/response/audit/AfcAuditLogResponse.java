package com.vdt.afc_ops_service.dto.response.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AfcAuditLogResponse {
    String id;
    String operatorCode;
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
