package com.vdt.afc_ops_service.service;

import com.vdt.afc_ops_service.dto.response.PageResponse;
import com.vdt.afc_ops_service.dto.response.audit.AfcAuditLogResponse;

import java.time.LocalDateTime;

public interface IAuditLogService {
    void record(String action, String resourceType, String resourceId, String resourceName,
                String result, String actorId, String actorName,
                Object requestPayload, Object responsePayload, String errorMessage);

    PageResponse<AfcAuditLogResponse> search(LocalDateTime from, LocalDateTime to,
                                             String username, String action,
                                             String resourceType, String resourceId,
                                             int page, int size);

    AfcAuditLogResponse getById(String id);
}
