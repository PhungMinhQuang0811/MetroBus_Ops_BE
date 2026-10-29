# Kế hoạch Refactor: Offline C2 Device & Control Package

Tài liệu này tổng hợp toàn bộ kế hoạch refactor cho hệ thống AFC, phân định rõ:
- **Phần C4 (project này)** — Java Spring Boot, `afc-ops-service`
- **Phần C3** — Station (bảng `station_control_syncs` trong cùng project này)
- **Phần C2 (project riêng)** — Thiết bị quét QR

---

## 1. Kiến trúc Tổng thể

### 1.1. Luồng Dữ Liệu

```
C5 ──RabbitMQ──→ C4
                     │
C4 tạo control packages, publish xuống C3 (station_control_syncs):
  ┌──────────────────────────────────────────────────────┐
  │ 1. DEVICE_CONFIG    → OPERATOR_MANAGER tạo (UC15-16) │
  │ 2. STATION_CONTEXT  → System auto khi tạo/sửa station│
  │ 3. MEDIA_ACCESS_RULES → System auto khi C5 sync card │
  └──────────────────────┬───────────────────────────────┘
                         ↓
                  C3 (station_control_syncs)
                         ↓
          C4 Midnight Cron (mỗi đêm 00:00):
          ┌──────────────────────────────────────────────┐
          │ Đọc 3 gói từ station_control_syncs per station│
          │ Fallback STATION_CONTEXT từ master data       │
          │ Gộp thành 1 JSON duy nhất                    │
          │ Publish qua RabbitMQ → C2 device             │
          └──────────────────────────────────────────────┘
                         ↓
           RabbitMQ routing key: "device.{stationCode}"
                         ↓
                  C2 device project (riêng)
```

### 1.2. 3 Package Types

| Package Type | Nội dung | Ai tạo | Publish xuống C3 (station_control_syncs) khi nào |
|-------------|----------|--------|--------------------------------------------------|
| **DEVICE_CONFIG** | `qrVerificationKey` (HMAC secret), `qrMaxTtlSeconds`, `maxClockDriftSeconds`, `heartbeatIntervalSeconds` + các field operator nhập | OPERATOR_MANAGER (UC15) | UC16: operator chọn station → publish |
| **STATION_CONTEXT** | `stationCode`, `stationName`, `routeCode`, `stationOrder`, `distance`, `operatorCode` | **System auto** | Station được tạo/cập nhật qua API |
| **MEDIA_ACCESS_RULES** | `cardStatusRules[]` danh sách card bị blacklist/cancelled | **System auto** | C5 sync card status/blacklist → tạo package → publish xuống tất cả ACTIVE station |

### 1.3. RabbitMQ Routing (C4 → C2)

```
Exchange: afc.exchange (topic, đã tồn tại ở C4)

C4 publish (midnight cron):
  routing key: "device.{stationCode}"
  payload: 1 JSON tổng hợp cả 3 gói (từ station_control_syncs + fallback master data)

C2 subscribe:
  queue: "c2.device.{deviceCode}"
  binding key: "device.{stationCode}"
```

C2 device chỉ cần biết `stationCode` (cấu hình khi lắp đặt). Mỗi đêm nhận 1 message duy nhất.

---

## 2. Chi Tiết Từng Package

### 2.1. DEVICE_CONFIG — Đã implement

```
UC15: OPERATOR_MANAGER tạo package → enrich QR key → lưu control_packages + MongoDB
UC16: OPERATOR_MANAGER chọn station → publish → xóa sync cũ cùng type → tạo station_control_syncs mới
UC17: C3 pull-pending → nhận package
```

**Enrich QR key:** Khi `create()`, backend tự động thêm `qrVerificationKey`, `qrMaxTtlSeconds`, `maxClockDriftSeconds`, `heartbeatIntervalSeconds` vào payload. Operator không cần nhập tay.

### 2.2. STATION_CONTEXT — Đã implement

```
Khi station được tạo/cập nhật (qua API):

  Step 1: StationService gọi StationControlPackageService
  Step 2: StationControlPackageService.createOrUpdateStationContext(station):
          - Tạo control_package (type = STATION_CONTEXT, source = LEVEL4_CREATED, status = PUBLISHED)
          - Lưu payload { stationCode, stationName, routeCode, stationOrder, distance, operatorCode } vào MongoDB
          - Xóa STATION_CONTEXT syncs cũ của station (tránh trùng lặp)
          - Tạo sync mới PENDING
  Step 3: station_control_syncs có record → C3 pull được
```

### 2.3. MEDIA_ACCESS_RULES — Đã implement

```
Khi C5 sync card status/blacklist (qua RabbitMQ):

  Step 1: Level5CardSyncListener.receiveCardSync() → Level5CardSyncService
  Step 2: upsert card thành công → publishRulesIfCardStatusChanged()
  Step 3: MediaAccessRulePackageService.refreshAndPublishForOperator():
          - Query tất cả card BLACKLISTED/CANCELLED
          - Tạo control_package (type = MEDIA_ACCESS_RULES, source = LEVEL5_SYNCED, status = PUBLISHED)
          - Lưu payload { cardStatusRules: [...] } vào MongoDB
          - Với mỗi ACTIVE station: xóa MEDIA_ACCESS_RULES syncs cũ → tạo sync mới PENDING
  Step 4: station_control_syncs có record → C3 pull được

Lưu ý: sync.blacklist.all (snapshot) gọi processBlacklistSnapshot() — chỉ upsert card, không tạo package.
```

---

## 3. Combined JSON Format (C4 midnight cron → C2)

C4 cron job đọc dữ liệu từ `station_control_syncs` + fallback master data, build 1 JSON:

```json
{
  "publishedAt": "2026-06-25T00:00:00Z",
  "deviceConfig": {
    "version": 13,
    "maxOfflineSeconds": 60,
    "allowOfflineValidation": true,
    "deviceTypes": ["QR_SCANNER_SIMULATOR"],
    "qrVerificationAlgorithm": "HMAC_SHA256",
    "qrVerificationKey": "base64url-encoded-secret",
    "qrMaxTtlSeconds": 30,
    "maxClockDriftSeconds": 30,
    "heartbeatIntervalSeconds": 30
  },
  "stationContext": {
    "stationCode": "METRO-001-ST-001",
    "stationName": "Bến Thành",
    "routeCode": "METRO-001",
    "stationOrder": 1,
    "distance": 0.00,
    "operatorCode": "HCMC-METRO"
  },
  "mediaAccessRules": {
    "version": 5,
    "cardStatusRules": [
      {"cardId": "uuid-1", "status": "BLACKLISTED", "statusReason": "LOST_CARD", "updatedAt": "2026-06-25T00:00:00"},
      {"cardId": "uuid-2", "status": "CANCELLED", "statusReason": "FRAUD", "updatedAt": "2026-06-25T00:00:00"}
    ]
  }
}
```

---

## 4. QR HMAC Signing (C4)

### 4.1. Format QR Payload

C4 ký QR payload bằng HMAC-SHA256, chứa `ticketId` (không chứa `cardId` — card là thẻ vật lý, ticket là vé ảo có thể không gắn card).

```
Format: AFCQR:v1:{ticketId}:exp={epochSeconds}:hmac={base64url}
Ví dụ:  AFCQR:v1:TICKET-001:exp=1765432100:hmac=abcXYZ123
```

### 4.2. C4 sinh QR

```java
long exp = toEpochSecond(expiresAt);
String qrPayload = dynamicQrSessionStore.buildHmacSignedPayload(ticket.getId(), exp, qrHmacSecret);
```

**`buildHmacSignedPayload` trong `DynamicQrSessionStore`:**
```java
public String buildHmacSignedPayload(String ticketId, long exp, String hmacSecret) {
    String dataToSign = QR_PAYLOAD_PREFIX + ticketId + ":exp=" + exp;
    String hmac = CryptoHashUtil.hmacSha256Base64Url(hmacSecret, dataToSign);
    return dataToSign + ":hmac=" + hmac;
}
```

### 4.3. C2 tự verify offline (có HMAC key từ midnight cron)

```java
// Parse: AFCQR:v1:{ticketId}:exp={ts}:hmac={sig}
// Verify HMAC → check expiry → check blacklist → OPEN_GATE/DENY
```
- `maxClockDriftSeconds` = `app.dynamic-qr.ttl-seconds` (mặc định 30s)
- C4 TransactionService giữ nguyên (C2 offline tự verify, C4 chỉ nhận batch)

### 4.4. Thay đổi trong DynamicQrService

- **`GenerateDynamicQrRequest.cardId`** → **`ticketId`** (App gửi `ticketId`)
- Lookup ticket trực tiếp bằng `ticketId`, không dùng `cardRepository`
- Nếu ticket có gắn card vật lý → validate card status
- Ký QR payload với HMAC

---

## 5. File Changes Actual

### 5.1. Files MỚI (đã tạo)

| File | Mục đích |
|------|---------|
| `qr/ParsedQrData.java` | Record `(ticketId, exp, expired)` cho parse HMAC-signed QR |
| `messaging/ControlPackagePublisher.java` | Publish combined JSON lên RabbitMQ tới C2 |
| `integration/level5/scheduler/DeviceSyncScheduler.java` | Cron midnight: đọc station_control_syncs → gom → publish RMQ |
| `service/IStationControlPackageService.java` | Interface cho STATION_CONTEXT |
| `service/impl/StationControlPackageService.java` | Auto-tạo STATION_CONTEXT + xóa sync cũ + tạo sync mới |
| `service/IMediaAccessRulePackageService.java` | Interface cho MEDIA_ACCESS_RULES |
| `service/impl/MediaAccessRulePackageService.java` | Query card BLACKLISTED/CANCELLED → tạo MEDIA_ACCESS_RULES → auto-publish |

### 5.2. Files CẬP NHẬT (đã sửa)

| File | Thay đổi |
|------|----------|
| `constant/PredefinedControlPackageType.java` | Thêm `STATION_CONTEXT` |
| `common/util/CryptoHashUtil.java` | Thêm `hmacSha256Base64Url()` |
| `qr/DynamicQrSessionStore.java` | Thêm `buildHmacSignedPayload()` + `parseHmacSignedPayload()` + `ParsedQrData` |
| `dto/request/qr/GenerateDynamicQrRequest.java` | `cardId` → `ticketId` |
| `service/impl/DynamicQrService.java` | Lookup bằng `ticketId`, ký HMAC payload |
| `service/impl/ControlPackageService.java` | `create()` enrich DEVICE_CONFIG với QR key; `publish()` xóa sync cũ cùng type + tạo mới |
| `service/impl/StationService.java` | Wire `StationControlPackageService` vào create/update/enable/disable |
| `service/impl/StationImportService.java` | Wire `StationControlPackageService` sau import station |
| `integration/level5/service/impl/Level5CardSyncService.java` | Wire `MediaAccessRulePackageService` sau blacklist/card status change; thêm `processBlacklistSnapshot()` |
| `integration/level5/listener/Level5CardSyncListener.java` | Thêm handler `sync.blacklist.all` dùng `processBlacklistSnapshot` |
| `repository/StationControlSyncRepository.java` | Thêm `deleteByStationIdAndPackageType` + `findByStationAndStatus` |
| `repository/ControlPackageRepository.java` | Thêm `findLatestByType` + `findPublishedByTypes` |
| `repository/StationRepository.java` | Thêm `findAllByStatus` + `findAllByStatusAndRouteOperatorId` |
| `repository/CardRepository.java` | Thêm `findByStatusIn` |
| `common/exception/ErrorCode.java` | Thêm `TICKET_NOT_FOUND`, `TICKET_EXPIRED`, `TICKET_ALREADY_USED`, `TICKET_INVALID` |
| `AfcOpsServiceApplication.java` | Thêm `@EnableScheduling` |
| `resources/application.yaml` | Thêm `app.security.qr-hmac-secret`, thêm routing key `sync.blacklist.all` |

### 5.3. Files KHÔNG THAY ĐỔI

| File | Lý do |
|------|-------|
| `entity/ControlPackage.java` | Đã generic |
| `entity/StationControlSync.java` | Đã generic |
| `TransactionService.java` | Giữ nguyên — C2 offline tự verify |
| `PredefinedControlPackageSourceType.java` | Không cần thêm `AUTO_GENERATED` |

---

## 6. Midnight Cron: DeviceSyncScheduler (Actual)

```java
@Scheduled(cron = "0 0 0 * * ?")
public void syncAllDevices() {
    List<Station> activeStations = stationRepository.findAllByStatus("ACTIVE");
    if (activeStations.isEmpty()) return;

    for (Station station : activeStations) {
        // Đọc syncs PENDING/APPLIED từ station_control_syncs
        List<StationControlSync> syncs = syncRepository
                .findByStationAndStatus(station.getStationCode(), List.of("PENDING", "APPLIED"));

        // Gom payload theo package type
        Map<String, Object> deviceConfig = null;
        Map<String, Object> stationContext = null;
        Map<String, Object> mediaAccessRules = null;
        for (StationControlSync sync : syncs) { ... }

        // Fallback STATION_CONTEXT từ master data
        if (stationContext == null) stationContext = buildStationContextFallback(station);

        // Build combined JSON → publish RabbitMQ
        controlPackagePublisher.publishToStation(station.getStationCode(), combined);
    }
}
```

---

## 7. DEVICE_CONFIG — Enriched Payload

Khi operator tạo DEVICE_CONFIG qua `ControlPackageService.create()`, payload tự động được enrich:

```json
{
  "maxOfflineSeconds": 60,
  "allowOfflineValidation": true,
  "deviceTypes": ["QR_SCANNER_SIMULATOR"],
  "qrVerificationAlgorithm": "HMAC_SHA256",
  "qrVerificationKey": "<base64-url-encoded-hmac-secret>",
  "qrMaxTtlSeconds": 30,
  "maxClockDriftSeconds": 30,
  "heartbeatIntervalSeconds": 30
}
```

QR key từ `app.security.qr-hmac-secret`, TTL từ `app.dynamic-qr.ttl-seconds`. Operator không cần nhập tay.

---

## 8. Tổng kết

### Luồng chính

```
C4:
  DEVICE_CONFIG      → UC15 create (auto-enrich QR key) → UC16 publish → station_control_syncs
  STATION_CONTEXT    → StationService gọi StationControlPackageService → delete sync cũ → tạo sync mới
  MEDIA_ACCESS_RULES → C5 sync → upsert card → MediaAccessRulePackageService
                       → query card BL/CANCELLED → tạo package → xóa sync cũ mỗi station → tạo sync mới

  DeviceSyncScheduler (midnight cron):
    Đọc station_control_syncs (PENDING/APPLIED) per station
    Fallback STATION_CONTEXT từ master data
    Build 1 combined JSON → RabbitMQ "device.{stationCode}" → C2

C2 (project riêng):
  Nhận combined JSON mỗi đêm
  → Lưu HMAC key, blacklist rules, station context
  → Verify QR offline: parse → HMAC → expiry → blacklist → OPEN_GATE/DENY
  → Queue local tap events → gửi batch lên C4 ban đêm