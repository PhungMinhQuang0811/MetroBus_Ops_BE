package com.vdt.afc_ops_service.service;

import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import com.vdt.afc_ops_service.document.AfcAuditLog;
import com.vdt.afc_ops_service.security.entity.AfcUserDetails;
import com.vdt.afc_ops_service.service.Impl.AuditLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    MongoTemplate mongoTemplate;

    AuditLogService service;

    @BeforeEach
    void setUp() {
        service = new AuditLogService(mongoTemplate, new ObjectMapper());
        AfcUserDetails principal = AfcUserDetails.builder()
                .id("account-1")
                .username("operator")
                .operatorCode("HCMC-METRO")
                .authorities(List.of())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void record_WithExplicitActorAndRequestContext_SavesMaskedAuditLog() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/device/create-device");
        request.addHeader("X-Forwarded-For", "10.0.0.1, 10.0.0.2");
        request.addHeader("X-Request-Id", "request-1");
        request.addHeader("User-Agent", "JUnit");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        service.record(
                "CREATE_DEVICE",
                "DEVICE",
                "100",
                "Gate 1",
                "SUCCESS",
                "actor-1",
                "auditor",
                Map.of("deviceCode", "GATE-001", "secret", "plain", "items", List.of(Map.of("newPassword", "abc"))),
                Map.of("token", "jwt", "ok", true),
                null
        );

        ArgumentCaptor<AfcAuditLog> captor = ArgumentCaptor.forClass(AfcAuditLog.class);
        verify(mongoTemplate).save(captor.capture());
        AfcAuditLog saved = captor.getValue();
        assertEquals("HCMC-METRO", saved.getOperatorCode());
        assertEquals("actor-1", saved.getAccountId());
        assertEquals("auditor", saved.getUsername());
        assertEquals("request-1", saved.getRequestId());
        assertEquals("10.0.0.1", saved.getIpAddress());
        assertEquals("JUnit", saved.getUserAgent());
        assertEquals("POST", saved.getHttpMethod());
        assertEquals("/device/create-device", saved.getRequestPath());
        assertEquals("CREATE_DEVICE", saved.getAction());
        assertEquals("SUCCESS", saved.getResult());
        assertNotNull(saved.getCreatedAt());
        assertTrue(saved.getRequestData().contains("\"deviceCode\":\"GATE-001\""));
        assertTrue(saved.getRequestData().contains("\"secret\":\"***\""));
        assertTrue(saved.getRequestData().contains("\"newPassword\":\"***\""));
        assertTrue(saved.getResponseData().contains("\"token\":\"***\""));
        assertTrue(saved.getResponseData().contains("\"ok\":true"));
    }

    @Test
    void record_WithoutActorOrPayload_UsesAuthenticatedActorAndAllowsNullPayloads() {
        service.record("DELETE_DEVICE", "DEVICE", "100", "Gate 1", "FAILED",
                null, null, null, null, "boom");

        ArgumentCaptor<AfcAuditLog> captor = ArgumentCaptor.forClass(AfcAuditLog.class);
        verify(mongoTemplate).save(captor.capture());
        AfcAuditLog saved = captor.getValue();
        assertEquals("HCMC-METRO", saved.getOperatorCode());
        assertEquals("account-1", saved.getAccountId());
        assertEquals("operator", saved.getUsername());
        assertNull(saved.getRequestData());
        assertNull(saved.getResponseData());
        assertEquals("boom", saved.getErrorMessage());
        assertNotNull(saved.getRequestId());
    }

    @Test
    void search_WithFilters_ReturnsMappedPageScopedByOperator() {
        LocalDateTime from = LocalDateTime.of(2026, 6, 20, 8, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 20, 9, 0);
        AfcAuditLog log = auditLog("audit-1", from.plusMinutes(5));
        when(mongoTemplate.find(any(Query.class), org.mockito.ArgumentMatchers.eq(AfcAuditLog.class)))
                .thenReturn(List.of(log));
        when(mongoTemplate.count(any(Query.class), org.mockito.ArgumentMatchers.eq(AfcAuditLog.class)))
                .thenReturn(11L);

        var response = service.search(from, to, "oper", "CREATE_DEVICE", "DEVICE", "100", 1, 10);

        assertEquals(1, response.getItems().size());
        assertEquals(1, response.getPage());
        assertEquals(10, response.getSize());
        assertEquals(11, response.getTotalElements());
        assertEquals(2, response.getTotalPages());
        assertEquals("audit-1", response.getItems().get(0).getId());
        assertEquals("HCMC-METRO", response.getItems().get(0).getOperatorCode());

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(queryCaptor.capture(), org.mockito.ArgumentMatchers.eq(AfcAuditLog.class));
        String query = queryCaptor.getValue().toString();
        assertEquals(true, query.contains("createdAt"));
        assertEquals(true, query.contains("operatorCode"));
        assertEquals(true, query.contains("username"));
        assertEquals(true, query.contains("resourceType"));
        assertEquals(true, query.contains("resourceId"));
    }

    @Test
    void search_WithoutOperatorCode_SkipsOperatorFilter() {
        setPrincipal("account-2", "global-admin", " ");
        when(mongoTemplate.find(any(Query.class), org.mockito.ArgumentMatchers.eq(AfcAuditLog.class)))
                .thenReturn(List.of());
        when(mongoTemplate.count(any(Query.class), org.mockito.ArgumentMatchers.eq(AfcAuditLog.class)))
                .thenReturn(0L);

        var response = service.search(null, null, " ", null, "", "", 0, 20);

        assertEquals(0, response.getTotalElements());
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(queryCaptor.capture(), org.mockito.ArgumentMatchers.eq(AfcAuditLog.class));
        assertEquals(false, queryCaptor.getValue().toString().contains("operatorCode"));
    }

    @Test
    void search_WithOnlyTo_UsesDerivedFromRange() {
        LocalDateTime to = LocalDateTime.of(2026, 6, 20, 9, 0);
        when(mongoTemplate.find(any(Query.class), org.mockito.ArgumentMatchers.eq(AfcAuditLog.class)))
                .thenReturn(List.of());
        when(mongoTemplate.count(any(Query.class), org.mockito.ArgumentMatchers.eq(AfcAuditLog.class)))
                .thenReturn(0L);

        var response = service.search(null, to, null, null, null, null, 0, 20);

        assertEquals(0, response.getTotalPages());
    }

    @Test
    void search_WithOnlyFrom_UsesNowAsTo() {
        LocalDateTime from = LocalDateTime.now().minusHours(1);
        when(mongoTemplate.find(any(Query.class), org.mockito.ArgumentMatchers.eq(AfcAuditLog.class)))
                .thenReturn(List.of());
        when(mongoTemplate.count(any(Query.class), org.mockito.ArgumentMatchers.eq(AfcAuditLog.class)))
                .thenReturn(0L);

        var response = service.search(from, null, null, null, null, null, 0, 20);

        assertEquals(0, response.getItems().size());
    }

    @Test
    void search_InvalidPage_ThrowsInvalidPageRequest() {
        AppException exception = assertThrows(AppException.class,
                () -> service.search(null, null, null, null, null, null, 0, 101));

        assertEquals(ErrorCode.INVALID_PAGE_REQUEST, exception.getErrorCode());
    }

    @Test
    void search_FromAfterTo_ThrowsInvalidAuditTimeRange() {
        LocalDateTime from = LocalDateTime.of(2026, 6, 20, 10, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 20, 9, 0);

        AppException exception = assertThrows(AppException.class,
                () -> service.search(from, to, null, null, null, null, 0, 20));

        assertEquals(ErrorCode.INVALID_AUDIT_TIME_RANGE, exception.getErrorCode());
    }

    @Test
    void getById_WhenFound_ReturnsMappedResponse() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 20, 8, 5);
        when(mongoTemplate.findById("audit-1", AfcAuditLog.class)).thenReturn(auditLog("audit-1", createdAt));

        var response = service.getById("audit-1");

        assertEquals("audit-1", response.getId());
        assertEquals("HCMC-METRO", response.getOperatorCode());
        assertEquals("account-1", response.getAccountId());
        assertEquals("operator", response.getUsername());
        assertEquals(createdAt, response.getCreatedAt());
    }

    @Test
    void getById_WhenMissing_ReturnsNull() {
        when(mongoTemplate.findById("missing", AfcAuditLog.class)).thenReturn(null);

        assertNull(service.getById("missing"));
    }

    private void setPrincipal(String accountId, String username, String operatorCode) {
        AfcUserDetails principal = AfcUserDetails.builder()
                .id(accountId)
                .username(username)
                .operatorCode(operatorCode)
                .authorities(List.of())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
    }

    private AfcAuditLog auditLog(String id, LocalDateTime createdAt) {
        return AfcAuditLog.builder()
                .id(id)
                .operatorCode("HCMC-METRO")
                .accountId("account-1")
                .username("operator")
                .action("CREATE_DEVICE")
                .resourceType("DEVICE")
                .resourceId("100")
                .resourceName("Gate 1")
                .result("SUCCESS")
                .requestId("request-1")
                .ipAddress("10.0.0.1")
                .userAgent("JUnit")
                .httpMethod("POST")
                .requestPath("/device/create-device")
                .requestData("{}")
                .responseData("{}")
                .errorMessage(null)
                .createdAt(createdAt)
                .build();
    }
}
