package com.vdt.auth_ops_service.service;

import com.vdt.auth_ops_service.dto.response.PageResponse;
import com.vdt.auth_ops_service.dto.response.audit.AuthAuditLogResponse;

import java.time.LocalDateTime;

public interface IAuditLogService {
    void record(String action, String resourceType, String resourceId, String resourceName,
                String result, String actorId, String actorName,
                Object requestPayload, Object responsePayload, String errorMessage);

    PageResponse<AuthAuditLogResponse> search(LocalDateTime from, LocalDateTime to,
                                              String username, String action, String result, int page, int size);

    AuthAuditLogResponse getById(String id);
}
