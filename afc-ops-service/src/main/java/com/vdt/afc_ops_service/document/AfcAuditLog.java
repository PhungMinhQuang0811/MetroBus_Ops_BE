package com.vdt.afc_ops_service.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "afc_audit_logs")
public class AfcAuditLog {
    @Id
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
