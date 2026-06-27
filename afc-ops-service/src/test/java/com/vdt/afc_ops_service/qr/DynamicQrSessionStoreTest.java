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
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    @Test
    void buildHmacSignedPayload_returnsCorrectFormat() {
        String payload = store.buildHmacSignedPayload("TICKET-001", 1765432100L, "my-secret");

        assertTrue(payload.startsWith("AFCQR:v1:TICKET-001:exp=1765432100:hmac="));
        // HMAC should be non-empty base64url
        String[] parts = payload.split(":");
        assertTrue(parts.length >= 5);
        assertTrue(parts[4].startsWith("hmac="));
        assertTrue(parts[4].length() > "hmac=".length());
    }

    @Test
    void parseHmacSignedPayload_validPayload_returnsParsedQrData() {
        String payload = store.buildHmacSignedPayload("TICKET-001", 1765432100L, "my-secret");

        ParsedQrData result = store.parseHmacSignedPayload(payload, "my-secret", 60);

        assertNotNull(result);
        assertEquals("TICKET-001", result.ticketId());
        assertEquals(1765432100L, result.exp());
        assertEquals(payload, "AFCQR:v1:" + "TICKET-001" + ":exp=" + 1765432100L + ":hmac="
                + payload.split(":hmac=")[1]); // just checking it's the same
    }

    @Test
    void parseHmacSignedPayload_invalidHmac_returnsNull() {
        String payload = store.buildHmacSignedPayload("TICKET-001", 1765432100L, "my-secret");

        ParsedQrData result = store.parseHmacSignedPayload(payload, "wrong-secret", 60);

        assertNull(result);
    }

    @Test
    void parseHmacSignedPayload_expiredPayload_returnsExpiredTrue() {
        long pastExp = System.currentTimeMillis() / 1000 - 100; // 100s in the past
        String payload = store.buildHmacSignedPayload("TICKET-001", pastExp, "my-secret");

        ParsedQrData result = store.parseHmacSignedPayload(payload, "my-secret", 60);

        assertNotNull(result);
        assertTrue(result.expired());
    }

    @Test
    void parseHmacSignedPayload_validNonExpiredPayload_returnsExpiredFalse() {
        long futureExp = System.currentTimeMillis() / 1000 + 3600; // 1h in the future
        String payload = store.buildHmacSignedPayload("TICKET-001", futureExp, "my-secret");

        ParsedQrData result = store.parseHmacSignedPayload(payload, "my-secret", 60);

        assertNotNull(result);
        assertEquals(false, result.expired());
    }

    @Test
    void parseHmacSignedPayload_nullOrInvalidFormat_returnsNull() {
        assertNull(store.parseHmacSignedPayload(null, "secret", 60));
        assertNull(store.parseHmacSignedPayload("", "secret", 60));
        assertNull(store.parseHmacSignedPayload("bad-format", "secret", 60));
        assertNull(store.parseHmacSignedPayload("AFCQR:v1:short", "secret", 60));
        assertNull(store.parseHmacSignedPayload("AFCQR:v1:TICKET-001:exp=notanumber:hmac=abc", "secret", 60));
    }

    @Test
    void testJsonParsingEdgeCases() {
        // jsonString keyIndex < 0
        DynamicQrSession session1 = store.deserialize("{\"otherKey\":\"CARD-001\"}");
        assertNull(session1.cardId());

        // jsonString value starts with null
        DynamicQrSession session2 = store.deserialize("{\"cardId\":null}");
        assertNull(session2.cardId());

        // jsonString charAt != '\"' (missing start quote)
        DynamicQrSession session3 = store.deserialize("{\"cardId\":123}");
        assertNull(session3.cardId());

        // jsonString valueEnd < 0 (missing end quote)
        DynamicQrSession session4 = store.deserialize("{\"cardId\":\"CARD-001}");
        assertNull(session4.cardId());

        // jsonLong keyIndex < 0
        DynamicQrSession session5 = store.deserialize("{\"otherKey\":100}");
        assertNull(session5.expiresAt());

        // jsonLong raw equals null
        DynamicQrSession session6 = store.deserialize("{\"exp\":null}");
        assertNull(session6.expiresAt());

        // jsonLong NumberFormatException
        DynamicQrSession session7 = store.deserialize("{\"exp\":notanumber}");
        assertNull(session7.expiresAt());

        // jsonBoolean keyIndex < 0
        DynamicQrSession session8 = store.deserialize("{\"otherKey\":true}");
        assertEquals(false, session8.used());
    }

    @Test
    void serialize_EscapesSpecialCharacters() {
        DynamicQrSession session = new DynamicQrSession(
                "CARD-\"\\",
                null,
                null,
                null,
                false
        );
        String serialized = store.serialize(session);
        // CARD-\"\\ should be escaped to CARD-\\\"\\\\
        assertTrue(serialized.contains("\"cardId\":\"CARD-\\\"\\\\\""));
    }

    @Test
    void parseQrId_InvalidInputVariants() {
        assertNull(store.parseQrId(null));
        assertNull(store.parseQrId("AFCQR:v2:QR-001"));
        assertNull(store.parseQrId("AFCQR:v1:"));
        assertNull(store.parseQrId("AFCQR:v1:   "));
    }
}
