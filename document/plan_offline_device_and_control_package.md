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
| **DEVICE_CONFIG** | `qrVerificationKey` (HMAC secret), `qrMaxTtlSeconds`, `maxOfflineSeconds`, `allowOfflineValidation`, `heartbeatIntervalSeconds`, `deviceTypes` | OPERATOR_MANAGER (UC15) | UC16: operator chọn station → publish |
| **STATION_CONTEXT** | `stationCode`, `stationName`, `routeCode`, `direction`, `stationOrder`, `distance`, `operatorCode` | **System auto** | Station được tạo/cập nhật qua API hoặc import |
| **MEDIA_ACCESS_RULES** | `cardStatusRules[]` danh sách card bị blacklist/cancelled | **System auto** | C5 sync card status change → tạo package mới → publish xuống tất cả station |

### 1.3. RabbitMQ Routing (C4 → C2)

```
Exchange: afc.exchange (topic, đã tồn tại ở C4)

C4 publish (midnight cron):
  routing key: "device.{stationCode}"
  payload: 1 JSON tổng hợp cả 3 gói (lấy từ station_control_syncs)

C2 subscribe:
  queue: "c2.device.{deviceCode}"
  binding key: "device.{stationCode}"
```

C2 device chỉ cần biết `stationCode` (cấu hình khi lắp đặt). Mỗi đêm nhận 1 message duy nhất.

---

## 2. Chi Tiết Từng Package

### 2.1. DEVICE_CONFIG — Flow hiện tại (giữ nguyên)

```
UC15: OPERATOR_MANAGER tạo package → lưu control_packages + MongoDB
UC16: OPERATOR_MANAGER chọn station → publish → tạo station_control_syncs
UC17: C3 pull-pending → nhận package
```

**Cần thêm (P7):** Khi publish, C4 tự động enrich payload với `qrVerificationKey` lấy từ `app.security.qr-hmac-secret`.

### 2.2. STATION_CONTEXT — Flow mới (cần implement)

```
Khi station được tạo/cập nhật (qua API hoặc import):

  Step 1: StationService/StationImportService gọi StationControlPackageService
  Step 2: StationControlPackageService.createPackage(station):
          - Tạo control_package (type = STATION_CONTEXT, source = LEVEL4_CREATED)
          - Lưu payload { stationCode, stationName, routeCode, direction, ... } vào MongoDB
          - Tự động publish xuống station đó qua station_control_syncs
  Step 3: station_control_syncs có record → C3 pull được
```

**Cần tạo mới (P8):** `StationControlPackageService.java`
**Cần cập nhật (P9):** `StationService.java` + `StationImportService.java`

### 2.3. MEDIA_ACCESS_RULES — Flow mới (cần implement)

```
Khi C5 sync card status change (qua RabbitMQ Level5CardSyncListener):

  Step 1: Level5CardSyncListener gọi MediaAccessRulePackageService
  Step 2: MediaAccessRulePackageService.refreshAndPublish():
          - Query tất cả card BLACKLISTED/CANCELLED
          - Tạo control_package (type = MEDIA_ACCESS_RULES, source = LEVEL5_SYNCED)
          - Lưu payload { cardStatusRules: [...] } vào MongoDB
          - Tự động publish xuống tất cả ACTIVE station
  Step 3: station_control_syncs có record → C3 pull được
```

**Cần tạo mới (P10):** `MediaAccessRulePackageService.java`
**Cần cập nhật (P11):** `Level5CardSyncListener.java`

---

## 3. Combined JSON Format (C4 midnight cron → C2)

C4 cron job đọc dữ liệu từ `control_packages` + `station_control_syncs` + master data, build 1 JSON:

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
    "qrMaxTtlSeconds": 60,
    "maxClockDriftSeconds": 60,
    "heartbeatIntervalSeconds": 30
  },
  "stationContext": {
    "version": 1,
    "stationCode": "METRO-001-ST-001",
    "stationName": "Bến Thành",
    "routeCode": "METRO-001",
    "direction": "ENTRY",
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

### Internal fields (not exposed to C2, used by cron logic)
- `publishedAt`: timestamp when cron ran
- `deviceConfig.version`: version from control_packages
- `stationContext.version`: version from control_packages
- `mediaAccessRules.version`: version from control_packages

---

## 4. QR HMAC Signing (C4)

### 4.1. Format QR Payload — chứa luôn cardId, ticketId

**KHÔNG dùng qr_sessions table. KHÔNG dùng QR session lookup.**

C4 nhúng cardId và ticketId trực tiếp vào QR payload. Khi C2 gửi batch, C4 parse trực tiếp — không cần DB/Redis lookup.

```
Before: AFCQR:v1:QR-SESSION-000001
After:  AFCQR:v1:CARD-000001:TICKET-000001:exp=1765432100:hmac=base64url
```

### 4.2. C4 sinh QR (có HMAC + cardId + ticketId)

```java
long exp = toEpochSecond(expiresAt);
String dataToSign = "AFCQR:v1:" + cardId + ":" + ticketId + ":exp=" + exp;
String hmac = CryptoHashUtil.hmacSha256Base64Url(hmacSecret, dataToSign);
String qrPayload = dataToSign + ":hmac=" + hmac;
```

### 4.3. C4 vẫn verify HMAC khi nhận tap (online)

Khi C4 nhận tap event từ C2, C4 kiểm tra HMAC trước khi lookup Redis.

### 4.4. C4 verify batch từ C2 (offline)

Khi C2 gửi batch ban đêm, C4 parse trực tiếp từ qrPayload — không cần lookup:

```java
// Parse: AFCQR:v1:{cardId}:{ticketId}:exp={ts}:hmac={sig}
String[] parts = qrPayload.split(":");
String cardId = parts[2];
String ticketId = parts[3];
long exp = Long.parseLong(parts[4].replace("exp=", ""));
// Verify HMAC + check expiry → create Transaction
```

---

## 5. File Change Summary

### 5.1. Files MỚI (C4 phải tạo)

| File | Mục đích | Liên quan đến |
|------|---------|---------------|
| `messaging/ControlPackagePublisher.java` | Publish 1 combined JSON lên RabbitMQ tới từng C2 | Midnight cron → C2 |
| `integration/level5/scheduler/DeviceSyncScheduler.java` | Cron job midnight: đọc từ DB + build JSON + publish | Midnight cron → C2 |
| `service/impl/StationControlPackageService.java` | Auto-tạo STATION_CONTEXT + publish xuống station_control_syncs | STATION_CONTEXT → C3 |
| `service/impl/MediaAccessRulePackageService.java` | Auto-tạo MEDIA_ACCESS_RULES + publish xuống station_control_syncs | MEDIA_ACCESS_RULES → C3 |

### 5.2. Files CẬP NHẬT (C4 phải sửa)

| File | Thay đổi | Liên quan đến |
|------|----------|---------------|
| `constant/PredefinedControlPackageType.java` | Thêm `STATION_CONTEXT`, update `isValid()` | Package type |
| `constant/PredefinedControlPackageSourceType.java` | Thêm `AUTO_GENERATED` (optional) | Source type |
| `common/util/CryptoHashUtil.java` | Thêm method `hmacSha256Base64Url()` | QR signing |
| `qr/DynamicQrSessionStore.java` | Update `buildPayload()` ký HMAC, thêm `parseAndVerify()` | QR signing |
| `service/impl/DynamicQrService.java` | Inject HMAC secret, dùng signed payload | QR signing |
| `service/impl/TransactionService.java` | Verify HMAC trước khi lookup Redis | QR signing |
| `service/impl/StationService.java` | Gọi StationControlPackageService sau create/update | STATION_CONTEXT → C3 |
| `service/impl/StationImportService.java` | Gọi StationControlPackageService sau import | STATION_CONTEXT → C3 |
| `integration/level5/listener/Level5CardSyncListener.java` | Gọi MediaAccessRulePackageService sau card status change | MEDIA_ACCESS_RULES → C3 |
| `service/impl/ControlPackageService.java` | Enrich DEVICE_CONFIG payload với QR key khi publish | DEVICE_CONFIG → C3 |
| `resources/application.yaml` | Thêm `app.security.qr-hmac-secret` | Config |
| `config/SchedulerConfig.java` (new) | Enable scheduling (`@EnableScheduling`) | Midnight cron |

### 5.3. Files KHÔNG CẦN THAY ĐỔI

| File | Lý do |
|------|-------|
| `entity/ControlPackage.java` | Đã generic — dùng cho mọi package type |
| `entity/StationControlSync.java` | Đã generic — dùng cho mọi package |
| `repository/` | Query theo type + station đã support |

---

## 6. Midnight Cron: DeviceSyncScheduler Chi Tiết

### 6.1. Dữ liệu đầu vào (từ DB C4)

```
control_packages table:
  - WHERE package_type = 'DEVICE_CONFIG' AND status = 'PUBLISHED' → lấy version cao nhất
  - WHERE package_type = 'MEDIA_ACCESS_RULES' AND status = 'PUBLISHED' → lấy version cao nhất
  
stations table + routes table:
  - stationCode, stationName, routeCode, direction, stationOrder, distance, operatorCode

cards table:
  - WHERE status IN ('BLACKLISTED', 'CANCELLED')
```

### 6.2. Logic

```java
@Scheduled(cron = "0 0 0 * * ?") // midnight every day
public void syncAllDevices() {
    List<Station> stations = stationRepository.findAllActive();
    
    // 1. Lấy DEVICE_CONFIG mới nhất (toàn operator — 1 bản cho tất cả station)
    Map<String, Object> deviceConfig = getLatestDeviceConfig();
    
    // 2. Lấy MEDIA_ACCESS_RULES mới nhất (toàn operator — 1 bản cho tất cả station)
    Map<String, Object> mediaRules = getLatestMediaAccessRules();
    
    for (Station station : stations) {
        // 3. Lấy STATION_CONTEXT theo station (mỗi station khác nhau)
        Map<String, Object> stationContext = buildStationContext(station);
        
        // 4. Build combined JSON
        Map<String, Object> combined = new HashMap<>();
        combined.put("publishedAt", Instant.now().toString());
        combined.put("deviceConfig", deviceConfig);
        combined.put("stationContext", stationContext);
        combined.put("mediaAccessRules", mediaRules);
        
        // 5. Publish qua RabbitMQ tới C2
        publisher.publishToStation(station.getStationCode(), combined);
    }
}
```

---

## 7. DEVICE_CONFIG — Enriched Payload

Khi operator publish DEVICE_CONFIG qua UC16, payload tự động bao gồm QR key từ config:

```json
{
  "maxOfflineSeconds": 60,
  "allowOfflineValidation": true,
  "deviceTypes": ["QR_SCANNER_SIMULATOR"],
  "qrVerificationAlgorithm": "HMAC_SHA256",
  "qrVerificationKey": "<base64-url-encoded-hmac-secret>",
  "qrMaxTtlSeconds": 60,
  "maxClockDriftSeconds": 60,
  "heartbeatIntervalSeconds": 30
}
```

QR key được lấy từ `app.security.qr-hmac-secret`. Operator không cần nhập tay.

---

## 8. Thứ tự triển khai (C4 only)

| Phase | Tasks | Liên quan |
|-------|-------|-----------|
| **P1** | Thêm `STATION_CONTEXT` vào `PredefinedControlPackageType` | Package type |
| **P2** | Thêm HMAC method vào `CryptoHashUtil.java` | QR signing |
| **P3** | Cập nhật QR signing: `buildPayload()` ký HMAC + thêm `parseAndVerify()` | QR signing |
| **P4** | Cập nhật `DynamicQrService` — dùng signed payload | QR signing |
| **P5** | Cập nhật `TransactionService` — verify HMAC trước Redis | QR signing |
| **P6** | Cập nhật `ControlPackageService.publish()` — enrich DEVICE_CONFIG với QR key | DEVICE_CONFIG → C3 |
| **P7** | Tạo `ControlPackagePublisher.java` — publish RMQ tới C2 | Midnight cron → C2 |
| **P8** | Tạo `StationControlPackageService.java` — auto STATION_CONTEXT + publish xuống station_control_syncs | STATION_CONTEXT → C3 |
| **P9** | Wire STATION_CONTEXT vào StationService + StationImportService | STATION_CONTEXT → C3 |
| **P10** | Tạo `MediaAccessRulePackageService.java` — auto MEDIA_ACCESS_RULES + publish xuống station_control_syncs | MEDIA_ACCESS_RULES → C3 |
| **P11** | Wire MEDIA_ACCESS_RULES vào Level5CardSyncListener | MEDIA_ACCESS_RULES → C3 |
| **P12** | Tạo `DeviceSyncScheduler.java` — cron midnight: gom 3 gói + publish RMQ | Midnight cron → C2 |
| **P13** | Thêm `app.security.qr-hmac-secret` + `@EnableScheduling` | Config |

---

## ============================================
## PHẦN B: VIỆC C2 CẦN LÀM
## ============================================

## 9. B1 — C2 config

```properties
device.code=GATE-001
device.stationCode=METRO-001-ST-001
```

## 10. B2 — RabbitMQ Consumer

| Item | Chi tiết |
|------|---------|
| Exchange | `afc.exchange` (topic) |
| Queue | `c2.device.{deviceCode}` (auto-delete, exclusive) |
| Binding key | `device.{stationCode}` |
| Receive | **1 JSON duy nhất** mỗi đêm (combined format ở mục 3) |

## 11. B3 — C2 QR Verification

```java
String qrPayload = scanResult.getText(); // "AFCQR:v1:...:exp=...:hmac=..."

// Parse
if (!qrPayload.startsWith("AFCQR:v1:")) return DENY;
String[] parts = qrPayload.split(":");
String qrId = parts[2];
long exp = Long.parseLong(parts[3].replace("exp=", ""));
String receivedHmac = parts[4].replace("hmac=", "");

// Check expiry
if (System.currentTimeMillis() / 1000 > exp + maxClockDriftSeconds) return DENY;

// Recompute HMAC
String dataToSign = "AFCQR:v1:" + qrId + ":exp=" + exp;
String expectedHmac = hmacSha256Base64Url(qrVerificationKey, dataToSign);

// Compare
if (!expectedHmac.equals(receivedHmac)) return DENY;
return OPEN_GATE;
```

## 12. B4 — 6 tasks cho C2

| # | Task | Mô tả |
|---|------|-------|
| C2-1 | Config | `device.code`, `device.stationCode` |
| C2-2 | RabbitMQ consumer | Subscribe `device.{stationCode}` |
| C2-3 | Parse combined JSON | Lưu 3 phần: deviceConfig, stationContext, mediaAccessRules |
| C2-4 | Local storage | File hoặc embedded DB |
| C2-5 | QR verify | Parse + HMAC + expiry |
| C2-6 | Tap decision | OPEN_GATE / DENY |

---

## 13. Tổng kết

### C4 project — 13 phases

| Nhóm | Số file |
|------|---------|
| New files | 4 |
| Updated files | 9+ |

### C2 project — 6 tasks

| Nhóm | Số task |
|------|---------|
| RabbitMQ + storage | 4 |
| QR verify | 2 |

### Luồng chính

```
C4:
  DEVICE_CONFIG    → UC15-16 → station_control_syncs (C3)
  STATION_CONTEXT  → auto khi tạo station → station_control_syncs (C3)
  MEDIA_ACCESS_RULES → auto khi C5 sync → station_control_syncs (C3)
  
  Midnight cron: đọc từ station_control_syncs + master data → 1 JSON → RabbitMQ → C2