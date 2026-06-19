package com.vdt.auth_ops_service.service;

import com.vdt.auth_ops_service.common.exception.AppException;
import com.vdt.auth_ops_service.common.exception.ErrorCode;
import com.vdt.auth_ops_service.document.AuthAuditLog;
import com.vdt.auth_ops_service.security.entity.CustomUserDetails;
import com.vdt.auth_ops_service.service.Impl.AuditLogService;
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
        CustomUserDetails principal = CustomUserDetails.builder()
                .id("account-1")
                .username("admin")
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
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.addHeader("X-Forwarded-For", "10.0.0.1, 10.0.0.2");
        request.addHeader("X-Request-Id", "request-1");
        request.addHeader("User-Agent", "JUnit");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        service.record(
                "LOGIN",
                "ACCOUNT",
                "account-2",
                "operator",
                "SUCCESS",
                "actor-1",
                "auditor",
                Map.of("username", "admin", "password", "secret", "items", List.of(Map.of("token", "abc"))),
                Map.of("refreshToken", "rt", "ok", true),
                null
        );

        ArgumentCaptor<AuthAuditLog> captor = ArgumentCaptor.forClass(AuthAuditLog.class);
        verify(mongoTemplate).save(captor.capture());
        AuthAuditLog saved = captor.getValue();
        assertEquals("actor-1", saved.getAccountId());
        assertEquals("auditor", saved.getUsername());
        assertEquals("request-1", saved.getRequestId());
        assertEquals("10.0.0.1", saved.getIpAddress());
        assertEquals("JUnit", saved.getUserAgent());
        assertEquals("POST", saved.getHttpMethod());
        assertEquals("/auth/login", saved.getRequestPath());
        assertEquals("LOGIN", saved.getAction());
        assertEquals("SUCCESS", saved.getResult());
        assertNotNull(saved.getCreatedAt());
        assertTrue(saved.getRequestData().contains("\"username\":\"admin\""));
        assertTrue(saved.getRequestData().contains("\"password\":\"***\""));
        assertTrue(saved.getRequestData().contains("\"token\":\"***\""));
        assertTrue(saved.getResponseData().contains("\"refreshToken\":\"***\""));
        assertTrue(saved.getResponseData().contains("\"ok\":true"));
    }

    @Test
    void record_WithoutActorOrPayload_UsesAuthenticatedActorAndAllowsNullPayloads() {
        service.record("LOGOUT", "ACCOUNT", "account-1", "admin", "SUCCESS",
                null, null, null, null, "boom");

        ArgumentCaptor<AuthAuditLog> captor = ArgumentCaptor.forClass(AuthAuditLog.class);
        verify(mongoTemplate).save(captor.capture());
        AuthAuditLog saved = captor.getValue();
        assertEquals("account-1", saved.getAccountId());
        assertEquals("admin", saved.getUsername());
        assertNull(saved.getRequestData());
        assertNull(saved.getResponseData());
        assertEquals("boom", saved.getErrorMessage());
        assertNotNull(saved.getRequestId());
    }

    @Test
    void search_WithFilters_ReturnsMappedPage() {
        LocalDateTime from = LocalDateTime.of(2026, 6, 20, 8, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 20, 9, 0);
        AuthAuditLog log = auditLog("audit-1", from.plusMinutes(5));
        when(mongoTemplate.find(any(Query.class), org.mockito.ArgumentMatchers.eq(AuthAuditLog.class)))
                .thenReturn(List.of(log));
        when(mongoTemplate.count(any(Query.class), org.mockito.ArgumentMatchers.eq(AuthAuditLog.class)))
                .thenReturn(11L);

        var response = service.search(from, to, "adm", "LOGIN", "SUCCESS", 1, 10);

        assertEquals(1, response.getItems().size());
        assertEquals(1, response.getPage());
        assertEquals(10, response.getSize());
        assertEquals(11, response.getTotalElements());
        assertEquals(2, response.getTotalPages());
        assertEquals("audit-1", response.getItems().get(0).getId());
        assertEquals("admin", response.getItems().get(0).getUsername());

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(queryCaptor.capture(), org.mockito.ArgumentMatchers.eq(AuthAuditLog.class));
        String query = queryCaptor.getValue().toString();
        assertEquals(true, query.contains("createdAt"));
        assertEquals(true, query.contains("username"));
        assertEquals(true, query.contains("action"));
        assertEquals(true, query.contains("result"));
    }

    @Test
    void search_WithOnlyTo_UsesDerivedFromRange() {
        LocalDateTime to = LocalDateTime.of(2026, 6, 20, 9, 0);
        when(mongoTemplate.find(any(Query.class), org.mockito.ArgumentMatchers.eq(AuthAuditLog.class)))
                .thenReturn(List.of());
        when(mongoTemplate.count(any(Query.class), org.mockito.ArgumentMatchers.eq(AuthAuditLog.class)))
                .thenReturn(0L);

        var response = service.search(null, to, " ", null, "", 0, 20);

        assertEquals(0, response.getTotalElements());
        assertEquals(0, response.getTotalPages());
    }

    @Test
    void search_WithOnlyFrom_UsesNowAsTo() {
        LocalDateTime from = LocalDateTime.now().minusHours(1);
        when(mongoTemplate.find(any(Query.class), org.mockito.ArgumentMatchers.eq(AuthAuditLog.class)))
                .thenReturn(List.of());
        when(mongoTemplate.count(any(Query.class), org.mockito.ArgumentMatchers.eq(AuthAuditLog.class)))
                .thenReturn(0L);

        var response = service.search(from, null, null, null, null, 0, 20);

        assertEquals(0, response.getItems().size());
    }

    @Test
    void search_InvalidPage_ThrowsInvalidPageRequest() {
        AppException exception = assertThrows(AppException.class,
                () -> service.search(null, null, null, null, null, -1, 20));

        assertEquals(ErrorCode.INVALID_PAGE_REQUEST, exception.getErrorCode());
    }

    @Test
    void search_FromAfterTo_ThrowsInvalidAuditTimeRange() {
        LocalDateTime from = LocalDateTime.of(2026, 6, 20, 10, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 20, 9, 0);

        AppException exception = assertThrows(AppException.class,
                () -> service.search(from, to, null, null, null, 0, 20));

        assertEquals(ErrorCode.INVALID_AUDIT_TIME_RANGE, exception.getErrorCode());
    }

    @Test
    void getById_WhenFound_ReturnsMappedResponse() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 20, 8, 5);
        when(mongoTemplate.findById("audit-1", AuthAuditLog.class)).thenReturn(auditLog("audit-1", createdAt));

        var response = service.getById("audit-1");

        assertEquals("audit-1", response.getId());
        assertEquals("account-1", response.getAccountId());
        assertEquals("admin", response.getUsername());
        assertEquals(createdAt, response.getCreatedAt());
    }

    @Test
    void getById_WhenMissing_ReturnsNull() {
        when(mongoTemplate.findById("missing", AuthAuditLog.class)).thenReturn(null);

        assertNull(service.getById("missing"));
    }

    private AuthAuditLog auditLog(String id, LocalDateTime createdAt) {
        return AuthAuditLog.builder()
                .id(id)
                .accountId("account-1")
                .username("admin")
                .action("LOGIN")
                .resourceType("ACCOUNT")
                .resourceId("account-1")
                .resourceName("admin")
                .result("SUCCESS")
                .requestId("request-1")
                .ipAddress("10.0.0.1")
                .userAgent("JUnit")
                .httpMethod("POST")
                .requestPath("/auth/login")
                .requestData("{}")
                .responseData("{}")
                .errorMessage(null)
                .createdAt(createdAt)
                .build();
    }
}
