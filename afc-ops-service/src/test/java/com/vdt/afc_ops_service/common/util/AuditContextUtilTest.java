package com.vdt.afc_ops_service.common.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditContextUtilTest {

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void testConstructor_isPrivate() throws Exception {
        Constructor<AuditContextUtil> constructor = AuditContextUtil.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
    }

    @Test
    void currentRequest_noRequestAttributes_returnsNull() {
        assertNull(AuditContextUtil.currentRequest());
    }

    @Test
    void getClientIp_noRequest_returnsNull() {
        assertNull(AuditContextUtil.getClientIp());
    }

    @Test
    void getClientIp_hasForwardedFor_returnsFirstIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", " 192.168.1.1, 10.0.0.1 ");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertEquals("192.168.1.1", AuditContextUtil.getClientIp());
    }

    @Test
    void getClientIp_noForwardedFor_returnsRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertEquals("127.0.0.1", AuditContextUtil.getClientIp());
    }

    @Test
    void getUserAgent_noRequest_returnsNull() {
        assertNull(AuditContextUtil.getUserAgent());
    }

    @Test
    void getUserAgent_hasUserAgent_returnsHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "Mozilla/5.0");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertEquals("Mozilla/5.0", AuditContextUtil.getUserAgent());
    }

    @Test
    void getRequestId_noRequest_returnsRandomUuid() {
        assertNotNull(AuditContextUtil.getRequestId());
    }

    @Test
    void getRequestId_hasXRequestId_returnsHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "req-123");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertEquals("req-123", AuditContextUtil.getRequestId());
    }

    @Test
    void getRequestId_noXRequestId_hasXCorrelationId_returnsHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-Id", "corr-123");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertEquals("corr-123", AuditContextUtil.getRequestId());
    }

    @Test
    void getRequestId_noHeaders_returnsRandomUuid() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertNotNull(AuditContextUtil.getRequestId());
    }

    @Test
    void getHttpMethod_returnsMethodOrNull() {
        assertNull(AuditContextUtil.getHttpMethod());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertEquals("POST", AuditContextUtil.getHttpMethod());
    }

    @Test
    void getRequestPath_returnsPathOrNull() {
        assertNull(AuditContextUtil.getRequestPath());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/test");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertEquals("/api/v1/test", AuditContextUtil.getRequestPath());
    }
}
