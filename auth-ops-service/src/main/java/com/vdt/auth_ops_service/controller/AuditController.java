package com.vdt.auth_ops_service.controller;

import com.vdt.auth_ops_service.dto.response.ApiResponse;
import com.vdt.auth_ops_service.dto.response.PageResponse;
import com.vdt.auth_ops_service.dto.response.audit.AuthAuditLogResponse;
import com.vdt.auth_ops_service.service.IAuditLogService;
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
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuditController {
    IAuditLogService auditLogService;

    @GetMapping("/search-audit-logs")
    public ApiResponse<PageResponse<AuthAuditLogResponse>> searchAuditLogs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String result,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.<PageResponse<AuthAuditLogResponse>>builder()
                .result(auditLogService.search(from, to, username, action, result, page, size))
                .build();
    }

    @GetMapping("/get-audit-log/{auditId}")
    public ApiResponse<AuthAuditLogResponse> getAuditLog(@PathVariable String auditId) {
        return ApiResponse.<AuthAuditLogResponse>builder()
                .result(auditLogService.getById(auditId))
                .build();
    }
}
