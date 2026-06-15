package com.vdt.afc_ops_service.qr;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DynamicQrSessionStoreTest {

    @Mock
    RedisTemplate<String, String> redisTemplate;

    @Mock
    ValueOperations<String, String> valueOperations;

    DynamicQrSessionStore store;

    @BeforeEach
    void setUp() {
        store = new DynamicQrSessionStore(redisTemplate);
    }

    @Test
    void buildPayloadAndParseQrId_ReturnExpectedValues() {
        assertEquals("AFCQR:v1:QR-000001", store.buildPayload("QR-000001"));
        assertEquals("QR-000001", store.parseQrId("AFCQR:v1:QR-000001"));
        assertNull(store.parseQrId("bad-payload"));
        assertNull(store.parseQrId("AFCQR:v1:   "));
    }

    @Test
    void create_SerializesSessionWithTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        store.create("QR-000001", new DynamicQrSession(
                "CARD-000001",
                "TICKET-000001",
                null,
                1000L,
                false
        ), 30);

        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq("qr:session:QR-000001"), valueCaptor.capture(), eq(30L), eq(TimeUnit.SECONDS));
        assertTrue(valueCaptor.getValue().contains("\"cardId\":\"CARD-000001\""));
        assertTrue(valueCaptor.getValue().contains("\"ticketId\":\"TICKET-000001\""));
        assertTrue(valueCaptor.getValue().contains("\"entitlementId\":null"));
        assertTrue(valueCaptor.getValue().contains("\"used\":false"));
    }

    @Test
    void find_DeserializesSession() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("qr:session:QR-000001")).thenReturn("""
                {"cardId":"CARD-000001","ticketId":null,"entitlementId":"ENT-000001","exp":1000,"used":true}
                """);

        DynamicQrSession session = store.find("QR-000001");

        assertEquals("CARD-000001", session.cardId());
        assertNull(session.ticketId());
        assertEquals("ENT-000001", session.entitlementId());
        assertEquals(1000L, session.expiresAt());
        assertEquals(true, session.used());
    }

    @Test
    void find_MissingSession_ReturnsNull() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("qr:session:QR-000001")).thenReturn(null);

        assertNull(store.find("QR-000001"));
    }

    @Test
    void markUsed_SerializesUsedSessionWithShortTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        store.markUsed("QR-000001", new DynamicQrSession(
                "CARD-000001",
                null,
                "ENT-000001",
                1000L,
                false
        ));

        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq("qr:session:QR-000001"), valueCaptor.capture(), eq(30L), eq(TimeUnit.SECONDS));
        assertTrue(valueCaptor.getValue().contains("\"entitlementId\":\"ENT-000001\""));
        assertTrue(valueCaptor.getValue().contains("\"used\":true"));
    }
}
