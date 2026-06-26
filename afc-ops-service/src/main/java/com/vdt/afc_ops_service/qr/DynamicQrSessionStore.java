package com.vdt.afc_ops_service.qr;

import com.vdt.afc_ops_service.common.util.CryptoHashUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DynamicQrSessionStore {

    public static final String QR_PAYLOAD_PREFIX = "AFCQR:v1:";
    static final String QR_SESSION_KEY_PREFIX = "qr:session:";
    static final long USED_QR_TTL_SECONDS = 30L;

    RedisTemplate<String, String> redisTemplate;

    public String buildPayload(String qrId) {
        return QR_PAYLOAD_PREFIX + qrId;
    }

    public String buildHmacSignedPayload(String ticketId, long exp, String hmacSecret) {
        String dataToSign = QR_PAYLOAD_PREFIX + ticketId + ":exp=" + exp;
        String hmac = CryptoHashUtil.hmacSha256Base64Url(hmacSecret, dataToSign);
        return dataToSign + ":hmac=" + hmac;
    }

    public ParsedQrData parseHmacSignedPayload(String qrPayload, String hmacSecret, long maxClockDriftSeconds) {
        if (qrPayload == null || !qrPayload.startsWith(QR_PAYLOAD_PREFIX)) {
            return null;
        }

        // Format: AFCQR:v1:{ticketId}:exp={ts}:hmac={sig}
        String[] parts = qrPayload.split(":");
        if (parts.length < 5) {
            return null;
        }

        String ticketId = parts[2];
        long exp;
        try {
            exp = Long.parseLong(parts[3].replace("exp=", ""));
        } catch (NumberFormatException e) {
            return null;
        }
        String receivedHmac = parts[4].replace("hmac=", "");

        // Verify HMAC
        String dataToSign = QR_PAYLOAD_PREFIX + ticketId + ":exp=" + exp;
        String expectedHmac = CryptoHashUtil.hmacSha256Base64Url(hmacSecret, dataToSign);
        if (!expectedHmac.equals(receivedHmac)) {
            return null;
        }

        // Check expiry with clock drift tolerance
        long now = System.currentTimeMillis() / 1000;
        boolean expired = now > exp + maxClockDriftSeconds;

        return new ParsedQrData(ticketId, exp, expired);
    }

    public String parseQrId(String qrPayload) {
        if (qrPayload == null || !qrPayload.startsWith(QR_PAYLOAD_PREFIX)) {
            return null;
        }
        String qrId = qrPayload.substring(QR_PAYLOAD_PREFIX.length()).trim();
        return qrId.isBlank() ? null : qrId;
    }

    public void create(String qrId, DynamicQrSession session, long ttlSeconds) {
        redisTemplate.opsForValue().set(
                sessionKey(qrId),
                serialize(session),
                ttlSeconds,
                TimeUnit.SECONDS
        );
    }

    public DynamicQrSession find(String qrId) {
        String value = redisTemplate.opsForValue().get(sessionKey(qrId));
        return value == null ? null : deserialize(value);
    }

    public void markUsed(String qrId, DynamicQrSession session) {
        create(qrId, new DynamicQrSession(
                session.cardId(),
                session.ticketId(),
                session.entitlementId(),
                session.expiresAt(),
                true
        ), USED_QR_TTL_SECONDS);
    }

    public String sessionKey(String qrId) {
        return QR_SESSION_KEY_PREFIX + qrId;
    }

    String serialize(DynamicQrSession session) {
        return "{"
                + "\"cardId\":" + jsonStringOrNull(session.cardId()) + ","
                + "\"ticketId\":" + jsonStringOrNull(session.ticketId()) + ","
                + "\"entitlementId\":" + jsonStringOrNull(session.entitlementId()) + ","
                + "\"exp\":" + (session.expiresAt() == null ? "null" : session.expiresAt()) + ","
                + "\"used\":" + session.used()
                + "}";
    }

    DynamicQrSession deserialize(String value) {
        return new DynamicQrSession(
                jsonString(value, "cardId"),
                jsonString(value, "ticketId"),
                jsonString(value, "entitlementId"),
                jsonLong(value, "exp"),
                jsonBoolean(value, "used")
        );
    }

    private String jsonString(String json, String field) {
        String key = "\"" + field + "\":";
        int keyIndex = json.indexOf(key);
        if (keyIndex < 0) {
            return null;
        }
        int valueStart = keyIndex + key.length();
        if (json.startsWith("null", valueStart)) {
            return null;
        }
        if (valueStart >= json.length() || json.charAt(valueStart) != '"') {
            return null;
        }
        int valueEnd = json.indexOf('"', valueStart + 1);
        if (valueEnd < 0) {
            return null;
        }
        return json.substring(valueStart + 1, valueEnd)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private Long jsonLong(String json, String field) {
        String key = "\"" + field + "\":";
        int keyIndex = json.indexOf(key);
        if (keyIndex < 0) {
            return null;
        }
        int valueStart = keyIndex + key.length();
        int valueEnd = findJsonValueEnd(json, valueStart);
        if (valueEnd <= valueStart) {
            return null;
        }
        String raw = json.substring(valueStart, valueEnd).trim();
        if (raw.equals("null")) {
            return null;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private boolean jsonBoolean(String json, String field) {
        String key = "\"" + field + "\":";
        int keyIndex = json.indexOf(key);
        if (keyIndex < 0) {
            return false;
        }
        int valueStart = keyIndex + key.length();
        return json.startsWith("true", valueStart);
    }

    private int findJsonValueEnd(String json, int start) {
        int commaIndex = json.indexOf(',', start);
        int braceIndex = json.indexOf('}', start);
        if (commaIndex < 0) {
            return braceIndex;
        }
        if (braceIndex < 0) {
            return commaIndex;
        }
        return Math.min(commaIndex, braceIndex);
    }

    private String jsonStringOrNull(String value) {
        return value == null ? "null" : "\"" + escape(value) + "\"";
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
