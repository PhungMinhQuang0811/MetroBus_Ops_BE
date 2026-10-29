# Kế hoạch: C2 gửi Batch Transaction lên C4

Tài liệu này mô tả luồng C2 gửi batch các tap events đã queue trong ngày lên C4 vào ban đêm.

**Kiến trúc: KHÔNG dùng qr_sessions table. ticketId được nhúng trực tiếp trong QR payload, ký bằng HMAC.**

---

## 1. QR Payload Format (chứa luôn ticketId)

C4 nhúng ticketId trực tiếp vào QR payload, ký bằng HMAC:

```
AFCQR:v1:{ticketId}:exp={epochSeconds}:hmac={base64url}
```

Ví dụ:
```
AFCQR:v1:TICKET-000001:exp=1765432100:hmac=abcXYZ123
```

Khi C2 scan, nó vẫn chỉ verify HMAC như cũ. ticketId là UUID — C2 không cần hiểu chúng.

## 2. Luồng Xử Lý

### C4 sinh QR
```java
String dataToSign = "AFCQR:v1:" + ticketId + ":exp=" + exp;
String hmac = CryptoHashUtil.hmacSha256Base64Url(hmacSecret, dataToSign);
String qrPayload = dataToSign + ":hmac=" + hmac;
```

**Không cần lưu qr_sessions. Không cần Redis mở rộng.**

### C2 scan (offline)
```
C2 parse payload → verify HMAC → check expiry → check blacklist → OPEN_GATE
C2 queue local: { qrPayload, deviceCode, stationCode, tapType, occurredAt }
```

### C4 nhận batch (ban đêm)
```
POST /transaction/submit-batch
C4 parse qrPayload → lấy trực tiếp { ticketId }
→ không cần lookup Redis hay PostgreSQL
→ tạo Transaction
→ mark ticket USED
```

---

## 3. Batch API

### Request/Response DTOs
```java
@Data
public class SubmitBatchRequest {
    @NotNull @Size(min = 1, max = 500) @Valid
    private List<BatchTransactionItem> transactions;
}

@Data
public class BatchTransactionItem {
    @NotBlank private String qrPayload;
    private String tapType;          // TAP_IN / TAP_OUT
    @NotNull private LocalDateTime occurredAt;
}

@Data
public class SubmitBatchResponse {
    private int total;
    private int success;
    private int failed;
    private List<String> errors;
}
```

### Service Logic (parse trực tiếp từ payload, không lookup)
```java
@Transactional
public SubmitBatchResponse processBatch(SubmitBatchRequest request) {
    List<String> errors = new ArrayList<>();
    int success = 0;
    for (BatchTransactionItem item : request.getTransactions()) {
        try {
            // Parse trực tiếp từ qrPayload — không cần DB lookup
            ParsedQrData parsed = dynamicQrSessionStore.parseQrPayload(item.getQrPayload(), hmacSecret);
            if (parsed == null) { errors.add("Invalid QR"); continue; }
            if (parsed.expired()) { errors.add("QR expired"); continue; }
            if (parsed.replayed()) { errors.add("QR replayed"); continue; }

            // ticketId có sẵn trong payload
            createTransaction(parsed.ticketId(), item);
            success++;
        } catch (Exception e) { errors.add(e.getMessage()); }
    }
    return new SubmitBatchResponse(request.getTransactions().size(), success, errors.size(), errors);
}
```

---

## 4. C4 Code Changes

### DynamicQrSessionStore.java — thêm 2 methods mới

```java
// 1. Build payload với ticketId
public String buildPayload(String ticketId, long expiresAt, String hmacSecret) {
    String dataToSign = QR_PAYLOAD_PREFIX + ticketId + ":exp=" + expiresAt;
    String hmac = CryptoHashUtil.hmacSha256Base64Url(hmacSecret, dataToSign);
    return dataToSign + ":hmac=" + hmac;
}

// 2. Parse và verify từ payload (dùng cho batch)
public ParsedQrData parseAndVerify(String qrPayload, String hmacSecret, long maxClockDriftSeconds) {
    if (qrPayload == null || !qrPayload.startsWith(QR_PAYLOAD_PREFIX)) return null;

    String[] parts = qrPayload.split(":");
    // parts = ["AFCQR", "v1", "{ticketId}", "exp={ts}", "hmac={sig}"]
    if (parts.length < 5) return null;

    String ticketId = parts[2];
    long exp = Long.parseLong(parts[3].replace("exp=", ""));
    String receivedHmac = parts[4].replace("hmac=", "");

    // Verify HMAC
    String dataToSign = QR_PAYLOAD_PREFIX + ticketId + ":exp=" + exp;
    String expectedHmac = CryptoHashUtil.hmacSha256Base64Url(hmacSecret, dataToSign);
    if (!expectedHmac.equals(receivedHmac)) return null;

    // Check expiry
    boolean expired = Instant.now().getEpochSecond() > exp + maxClockDriftSeconds;

    return new ParsedQrData(null, ticketId, exp, expired);
}
```

### DynamicQrService.java — dùng payload mới
```java
long exp = toEpochSecond(expiresAt);
String qrPayload = dynamicQrSessionStore.buildPayload(ticketId, exp, hmacSecret);
// Vẫn lưu Redis cho realtime tap event
dynamicQrSessionStore.cacheInRedis(qrId, null, ticketId, exp, ttlSeconds);
return dynamicQrMapper.toResponse(qrId, qrPayload, expiresAt, ttlSeconds);
```

---

## 5. File Change Summary

**New files (2):**
- `dto/request/transaction/SubmitBatchRequest.java`
- `dto/response/transaction/SubmitBatchResponse.java`

**Updated files (4):**
- `qr/DynamicQrSessionStore.java` — `buildPayload()` nhận ticketId, thêm `parseAndVerify()`
- `service/Impl/DynamicQrService.java` — truyền ticketId vào buildPayload()
- `service/Impl/TransactionService.java` — `processBatch()` parse trực tiếp từ payload
- `controller/TransactionController.java` — `POST /submit-batch`

**Không cần tạo:**
- ❌ qr_sessions table
- ❌ QrSession entity
- ❌ QrSessionRepository
- ❌ DB migration V4
- ❌ Dedup logic

---

## 6. C2 Cần Làm

| Task | Mô tả |
|------|-------|
| Queue local | Lưu tap events: `{ qrPayload, deviceCode, stationCode, tapType, occurredAt }` |
| Gọi API | `POST /transaction/submit-batch` với `X-Device-Code` + `X-Device-Secret` |
| Xóa queue | Sau khi nhận `{ success: N, failed: 0 }` |

**C2 KHÔNG cần thay đổi QR verify logic.** ticketId là UUID — C2 không cần parse chúng. HMAC verify vẫn trên toàn bộ payload như cũ.

---

## 7. Thứ Tự Triển Khai

| Phase | Tasks | Deps |
|-------|-------|------|
| B1 | Update `DynamicQrSessionStore.buildPayload()` — nhận ticketId | None |
| B2 | Thêm `parseAndVerify()` vào `DynamicQrSessionStore` | None |
| B3 | Update `DynamicQrService` — truyền ticketId | B1 |
| B4 | Tạo DTOs: `SubmitBatchRequest`, `SubmitBatchResponse` | None |
| B5 | Thêm `processBatch()` vào `TransactionService` — parse trực tiếp từ payload | B2, B4 |
| B6 | Thêm endpoint `POST /submit-batch` + SecurityConstants | B5 |