package com.vdt.afc_ops_service.controller;

import com.vdt.afc_ops_service.dto.response.PageResponse;
import com.vdt.afc_ops_service.dto.response.audit.AfcAuditLogResponse;
import com.vdt.afc_ops_service.service.IAuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditControllerTest {

    @Mock
    IAuditLogService auditLogService;

    AuditController controller;

    @BeforeEach
    void setUp() {
        controller = new AuditController(auditLogService);
    }

    @Test
    void searchAuditLogs_DelegatesToService() {
        LocalDateTime from = LocalDateTime.of(2026, 6, 20, 8, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 20, 9, 0);
        PageResponse<AfcAuditLogResponse> expected = PageResponse.<AfcAuditLogResponse>builder()
                .items(List.of(AfcAuditLogResponse.builder().id("audit-1").build()))
                .page(1)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .build();
        when(auditLogService.search(from, to, "operator", "CREATE_DEVICE", "DEVICE", "100", 1, 10))
                .thenReturn(expected);

        var response = controller.searchAuditLogs(from, to, "operator", "CREATE_DEVICE", "DEVICE", "100", 1, 10);

        assertSame(expected, response.getResult());
        verify(auditLogService).search(from, to, "operator", "CREATE_DEVICE", "DEVICE", "100", 1, 10);
    }

    @Test
    void getAuditLog_DelegatesToService() {
        AfcAuditLogResponse expected = AfcAuditLogResponse.builder()
                .id("audit-1")
                .username("operator")
                .build();
        when(auditLogService.getById("audit-1")).thenReturn(expected);

        var response = controller.getAuditLog("audit-1");

        assertSame(expected, response.getResult());
        verify(auditLogService).getById("audit-1");
    }
}
