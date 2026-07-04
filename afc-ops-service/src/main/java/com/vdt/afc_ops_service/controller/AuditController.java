package com.vdt.afc_ops_service.controller;

import com.vdt.afc_ops_service.dto.response.ApiResponse;
import com.vdt.afc_ops_service.dto.response.PageResponse;
import com.vdt.afc_ops_service.dto.response.audit.AfcAuditLogResponse;
import com.vdt.afc_ops_service.dto.response.audit.IntegrationExchangeLogResponse;
import com.vdt.afc_ops_service.service.IAuditLogService;
import com.vdt.afc_ops_service.service.IIntegrationExchangeLogService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuditController {
    IAuditLogService auditLogService;
    IIntegrationExchangeLogService integrationExchangeLogService;

    @GetMapping("/search-audit-logs")
    public ApiResponse<PageResponse<AfcAuditLogResponse>> searchAuditLogs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.<PageResponse<AfcAuditLogResponse>>builder()
                .result(auditLogService.search(from, to, username, action, resourceType, resourceId, page, size))
                .build();
    }

    @GetMapping("/get-audit-log/{auditId}")
    public ApiResponse<AfcAuditLogResponse> getAuditLog(@PathVariable String auditId) {
        return ApiResponse.<AfcAuditLogResponse>builder()
                .result(auditLogService.getById(auditId))
                .build();
    }

    @GetMapping("/search-integration-logs")
    public ApiResponse<PageResponse<IntegrationExchangeLogResponse>> searchIntegrationLogs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String systemName,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.<PageResponse<IntegrationExchangeLogResponse>>builder()
                .result(integrationExchangeLogService.getLogs(page, size, systemName, direction, status, from, to))
                .build();
    }
}
