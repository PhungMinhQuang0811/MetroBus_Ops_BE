# Tổng Hợp Hệ Thống AFC Cấp 3 & Cấp 4 - Báo Cáo Tiến Độ

## 1. Tổng Quan Kiến Trúc

Hệ thống AFC (Automatic Fare Collection) gồm 5 tầng, trong phạm vi đồ án triển khai **Cấp 3 (Station Level)** và **Cấp 4 (Operator Back Office)**.

### Kiến trúc tổng thể

```
C5 (Clearing House) ──── RabbitMQ ────→ C4 (Operator Back Office)
                                           ↑
C3 (Station/Line Level) ──── REST API ────┘
    ↑
C2 (Device/Gate - giả lập bằng webcam laptop)
    ↑
C1 (QR Code trên App hành khách)
```

### Hai Service chính

| Service | Base Path | Công nghệ | Vai trò |
|---------|-----------|-----------|---------|
| `auth-ops-service` | `/auth` | Java Spring Boot | Xác thực, JWT, account nội bộ, role/permission |
| `afc-ops-service` | Theo controller | Java Spring Boot | Master data, thiết bị, transaction, control package, batch, dashboard |

### Cơ sở dữ liệu

| Database | Loại | Mục đích |
|----------|------|----------|
| `auth_db` | PostgreSQL | Account, role, permission nội bộ |
| `afc_ops_db` | PostgreSQL | Master data, transaction, batch, control package, settlement |
| `afc_ops_mongo` | MongoDB | Audit logs, raw device events, heartbeats, incidents, control package payloads |
| Redis | Cache | QR session (TTL 30-60s), card/ticket/entitlement runtime, device status |

---

## 2. Danh Sách Actor

| Actor | Vai trò |
|-------|---------|
| `OPERATOR_ADMIN` | Admin nội bộ, quản lý account + audit |
| `OPERATOR_MANAGER` | Quản lý vận hành Cấp 4 |
| `STATION_OPERATOR` | Nhân viên/giám sát tại ga/trạm Cấp 3 |
| `DEVICE_CLIENT` | Thiết bị Cấp 2 (webcam giả lập) gửi heartbeat/tap event |
| `LEVEL5_SYSTEM` | Hệ thống Cấp 5 đồng bộ dữ liệu qua RabbitMQ |
| `PASSENGER_APP` | App hành khách lấy QR động |

---

## 3. Chi Tiết Các Phân Hệ Đã Triển Khai

### 3.1. Xác Thực & Phân Quyền (auth-ops-service)

| Tính năng | API | Trạng thái |
|-----------|-----|-----------|
| Đăng nhập | `POST /auth/login` | ✅ Hoàn thành |
| Refresh token | `POST /auth/refresh-token` | ✅ Hoàn thành |
| Đăng xuất | `POST /auth/logout` | ✅ Hoàn thành |
| Danh sách account | `GET /account/list-accounts` | ✅ Hoàn thành |
| Tạo account | `POST /account/create-account` | ✅ Hoàn thành |
| Disable account | `POST /account/disable-account/{id}` | ✅ Hoàn thành |
| Enable account | `POST /account/enable-account/{id}` | ✅ Hoàn thành |
| Preview import account | `POST /account/preview-import-accounts` | ✅ Hoàn thành |
| Confirm import account | `POST /account/confirm-import-accounts` | ✅ Hoàn thành |
| Đổi mật khẩu | `POST /account/change-password` | ✅ Hoàn thành |
| Quên mật khẩu | `POST /auth/forgot-password` | ✅ Hoàn thành |
| Admin reset password | `POST /account/reset-password` | ✅ Hoàn thành |

**Entity chính:** `Account`, `Role`, `Permission`  
**Audit:** `AuthAuditLog` (MongoDB)

### 3.2. Master Data (afc-ops-service)

#### Tuyến (Route)

| Tính năng | API | Trạng thái |
|-----------|-----|-----------|
| Danh sách tuyến | `GET /route/list-routes` | ✅ Hoàn thành |
| Chi tiết tuyến | `GET /route/get-route/{id}` | ✅ Hoàn thành |
| Tạo tuyến | `POST /route/create-route` | ✅ Hoàn thành |
| Cập nhật tuyến | `POST /route/update-route/{id}` | ✅ Hoàn thành |
| Enable tuyến | `POST /route/enable-route/{id}` | ✅ Hoàn thành |
| Disable tuyến | `POST /route/disable-route/{id}` | ✅ Hoàn thành |
| Preview import | `POST /route/preview-import-routes` | ✅ Hoàn thành |
| Confirm import | `POST /route/confirm-import-routes` | ✅ Hoàn thành |

#### Ga/Trạm (Station)

| Tính năng | API | Trạng thái |
|-----------|-----|-----------|
| Danh sách ga | `GET /station/list-stations` | ✅ Hoàn thành |
| Chi tiết ga | `GET /station/get-station/{id}` | ✅ Hoàn thành |
| Tạo ga | `POST /station/create-station` | ✅ Hoàn thành |
| Cập nhật ga | `POST /station/update-station/{id}` | ✅ Hoàn thành |
| Enable ga | `POST /station/enable-station/{id}` | ✅ Hoàn thành |
| Disable ga | `POST /station/disable-station/{id}` | ✅ Hoàn thành |
| Preview import | `POST /station/preview-import-stations` | ✅ Hoàn thành |
| Confirm import | `POST /station/confirm-import-stations` | ✅ Hoàn thành |

**Cự ly ga:** Đã thêm cột `distance` vào entity `Station`, DTO và import Excel

#### Thiết bị (Device)

| Tính năng | API | Trạng thái |
|-----------|-----|-----------|
| Danh sách thiết bị | `GET /device/list-devices` | ✅ Hoàn thành |
| Chi tiết thiết bị | `GET /device/get-device/{id}` | ✅ Hoàn thành |
| Tạo thiết bị | `POST /device/create-device` | ✅ Hoàn thành |
| Cập nhật thiết bị | `POST /device/update-device/{id}` | ✅ Hoàn thành |
| Enable thiết bị | `POST /device/enable-device/{id}` | ✅ Hoàn thành |
| Disable thiết bị | `POST /device/disable-device/{id}` | ✅ Hoàn thành |
| Preview import | `POST /device/preview-import-devices` | ✅ Hoàn thành |
| Confirm import | `POST /device/confirm-import-devices` | ✅ Hoàn thành |

### 3.3. Device Integration

| Tính năng | API | Trạng thái |
|-----------|-----|-----------|
| Heartbeat thiết bị | `POST /afc-ops/submit-heartbeat` | ✅ Hoàn thành |
| Tap event (quét QR) | `POST /transaction/submit-tap-event` | ✅ Hoàn thành |
| Incident thiết bị | `POST /afc-ops/submit-device-incident` | ✅ Hoàn thành |

**Cơ chế QR:**
- Format: `AFCQR:v1:{ticketId}:exp={epochSeconds}:hmac={base64url}`
- HMAC-SHA256 với secret key
- TTL mặc định 30 giây
- Cache Redis: `qr:session:{qrId}`

### 3.4. Giám Sát & Tra Cứu

| Tính năng | API | Trạng thái |
|-----------|-----|-----------|
| Trạng thái thiết bị | `GET /afc-ops/get-device-status` | ✅ Hoàn thành |
| Lịch sử heartbeat | `GET /afc-ops/get-device-heartbeats` | ✅ Hoàn thành |
| Tra cứu transaction | `GET /transaction/search-transactions` | ✅ Hoàn thành |
| Chi tiết transaction | `GET /transaction/get-transaction-detail` | ✅ Hoàn thành |
| Tra cứu incident | `GET /afc-ops/search-incidents` | ✅ Hoàn thành |
| Chi tiết incident | `GET /afc-ops/get-incident/{id}` | ✅ Hoàn thành |

### 3.5. Dashboard Vận Hành (UC18 - 5 endpoints)

| API | Mô tả |
|-----|-------|
| `GET /dashboard/summary` | Tổng quan: thiết bị, transaction, incident, batch, control sync |
| `GET /dashboard/transaction-timeline` | Biểu đồ transaction theo giờ/ngày |
| `GET /dashboard/route-station-summaries` | Transaction theo tuyến/ga |
| `GET /dashboard/recent-incidents` | Incident gần đây |
| `GET /dashboard/alerts` | Cảnh báo vận hành |

**Quyền:** `DASHBOARD_READ`  
**Khoảng thời gian mặc định:** 24 giờ gần nhất  
**Đã có:** Unit test (DashboardServiceTest, DashboardControllerTest)

### 3.6. Audit & Truy Vết (UC21)

| Tính năng | API | Trạng thái |
|-----------|-----|-----------|
| Audit auth (login/account) | `GET /auth/search-audit-logs` | ✅ Hoàn thành |
| Chi tiết audit auth | `GET /auth/get-audit-log/{id}` | ✅ Hoàn thành |
| Audit AFC (vận hành) | `GET /audit/search-audit-logs` | ✅ Hoàn thành |
| Chi tiết audit AFC | `GET /audit/get-audit-log/{id}` | ✅ Hoàn thành |
| Integration logs | Chưa có collection MongoDB | ⏳ Chưa làm |

**Ghi log tự động tại:** create/update route, station, device, control package, batch, login/logout

### 3.7. Control Package (UC15-UC17)

| Tính năng | API | Trạng thái |
|-----------|-----|-----------|
| Tạo package | `POST /control-package/create` | ✅ Hoàn thành |
| Cập nhật draft | `POST /control-package/update/{id}` | ✅ Hoàn thành |
| Chi tiết package | `GET /control-package/get-detail` | ✅ Hoàn thành |
| Danh sách package | `GET /control-package/list` | ✅ Hoàn thành |
| Phát hành package | `POST /control-package/publish/{id}` | ✅ Hoàn thành |
| Pull pending (C3) | `GET /control-package/pull-pending` | ✅ Hoàn thành |
| Ack apply (C3) | `POST /control-package/ack-apply/{syncId}` | ✅ Hoàn thành |
| Danh sách sync | `GET /control-package/search-syncs` | ✅ Hoàn thành |
| Chi tiết sync | `GET /control-package/get-sync-detail` | ✅ Hoàn thành |

**3 loại package:**
1. `DEVICE_CONFIG` - Cấu hình thiết bị (do OPERATOR_MANAGER tạo)
2. `STATION_CONTEXT` - Context ga (system auto khi tạo/sửa station)
3. `MEDIA_ACCESS_RULES` - Rules blacklist/card (system auto khi C5 sync)

**Cron midnight:** `DeviceSyncScheduler` gộp 3 gói → RabbitMQ → C2 device

### 3.8. Đồng Bộ Dữ Liệu Cấp 5 (UC14)

| Tính năng | Cơ chế | Trạng thái |
|-----------|--------|-----------|
| Card sync | RabbitMQ: `card.status.changed`, `sync.card.all` | ✅ Hoàn thành |
| Ticket sync | RabbitMQ: `ticket.created`, `ticket.unlinked`, `sync.ticket.all` | ✅ Hoàn thành |
| Entitlement sync | Đã merge vào Ticket (xóa Entitlement entity) | ✅ Hoàn thành |
| Operator sync | RabbitMQ: `sync.operator.all` | ✅ Hoàn thành |
| Đối soát C5 | `SettlementConfirmedEvent` + `Level5SettlementSyncListener` | ✅ Hoàn thành |

### 3.9. Batch & Gửi Cấp 5 (UC19-UC20)

| Tính năng | API | Trạng thái |
|-----------|-----|-----------|
| Tạo batch | `POST /batch/create-batch` | ✅ Hoàn thành |
| Danh sách batch | `GET /batch/list-batches` | ✅ Hoàn thành |
| Gửi batch lên C5 | `POST /batch/submit-batch-to-level5/{id}` | ✅ Hoàn thành |
| Batch C2 về đêm | `POST /transaction/submit-batch` (HMAC-signed QR) | ✅ Hoàn thành |
| Đối soát doanh thu | `GET /reconciliation/settlements` | ✅ Hoàn thành |

### 3.10. App QR (UC22)

| Tính năng | API | Trạng thái |
|-----------|-----|-----------|
| Generate dynamic QR | `POST /qr/generate-dynamic-qr` | ✅ Hoàn thành |

**Cơ chế:**
- App gửi `ticketId` → C4 lookup ticket → kiểm tra card status → ký HMAC → trả QR payload
- QR có TTL 30-60 giây, tự refresh
- Redis lưu session `qr:session:{qrId}` với TTL ngắn

---

## 4. Các Phần Chưa Triển Khai

| Phân hệ | Mức độ ưu tiên | Ghi chú |
|---------|---------------|---------|
| **Ca kíp (Shift Management)** | Trung bình | Entity/Service/Controller check-in/check-out. Phục vụ demo quy trình vận hành nhân viên ga |
| **UC21 - Tab Tích hợp hệ thống** | Thấp | Collection MongoDB `integration_exchange_logs` + API search. Debug request/response với C5 |
| **Offline Mode** | ❌ Không làm | C2 chỉ webcam laptop giả lập, không có thiết bị thật ngoại tuyến |

---

## 5. Danh Sách Entity Đã Triển Khai

### auth_db

| Entity | Table | Ghi chú |
|--------|-------|---------|
| `Account` | `accounts` | PK UUID, có operator_code, password_status |
| `Role` | `roles` | Seed: OPERATOR_ADMIN, OPERATOR_MANAGER, STATION_OPERATOR |
| `Permission` | `permissions` | 13 permissions (ACCOUNT_READ/WRITE, MASTER_DATA_READ/WRITE, ...) |

### afc_ops_db

| Entity | Table | Ghi chú |
|--------|-------|---------|
| `Operator` | `operators` | operator_code UK |
| `Route` | `routes` | Unique (operator_id, route_code) |
| `Station` | `stations` | Có cột distance (cự ly ga) |
| `Device` | `devices` | device_code UK |
| `Card` | `cards` | cardId từ C5, status + status_reason |
| `Ticket` | `tickets` | Đã merge Entitlement (cả SINGLE_TRIP và MONTHLY_PASS) |
| `Transaction` | `afc_transactions` | Unique (device_id, event_id) |
| `ControlPackage` | `control_packages` | Unique (operator_id, version) |
| `StationControlSync` | `station_control_syncs` | Unique (station_id, control_package_id) |
| `Batch` | `batches` | batch_code UK |
| `OperatorSettlement` | `operator_settlements` | settlement_id từ C5 |

### MongoDB Collections

| Collection | Mục đích |
|------------|----------|
| `auth_audit_logs` | Audit đăng nhập, tài khoản |
| `afc_audit_logs` | Audit vận hành (master data, package, batch) |
| `raw_device_events` | Payload gốc từ C2 |
| `device_heartbeats` | Lịch sử heartbeat thiết bị |
| `device_incidents` | Lịch sử incident thiết bị |
| `control_package_payloads` | Payload chi tiết control package |
| `ticket_usage_result_payloads` | Kết quả xác nhận ticket từ C5 |
| `integration_exchange_logs` | ⏳ Chưa tạo |

---

## 6. API Tổng Hợp (60+ endpoints)

### auth-ops-service (12 APIs)

| # | Method | Endpoint | Mô tả |
|---|--------|----------|-------|
| 1 | POST | `/auth/login` | Đăng nhập |
| 2 | POST | `/auth/refresh-token` | Refresh token |
| 3 | POST | `/auth/logout` | Đăng xuất |
| 4 | GET | `/account/list-accounts` | DS account |
| 5 | POST | `/account/create-account` | Tạo account |
| 6 | POST | `/account/disable-account/{id}` | Khóa account |
| 7 | POST | `/account/enable-account/{id}` | Mở khóa account |
| 8 | POST | `/account/preview-import-accounts` | Preview import |
| 9 | POST | `/account/confirm-import-accounts` | Confirm import |
| 10 | POST | `/account/change-password` | Đổi mật khẩu |
| 11 | POST | `/auth/forgot-password` | Quên mật khẩu |
| 12 | POST | `/account/reset-password` | Admin reset password |

### afc-ops-service Master Data (21 APIs)

| # | Method | Endpoint | Mô tả |
|---|--------|----------|-------|
| 13 | GET | `/route/list-routes` | DS tuyến |
| 14 | GET | `/route/get-route/{id}` | Chi tiết tuyến |
| 15 | POST | `/route/create-route` | Tạo tuyến |
| 16 | POST | `/route/update-route/{id}` | Sửa tuyến |
| 17 | POST | `/route/enable-route/{id}` | Enable tuyến |
| 18 | POST | `/route/disable-route/{id}` | Disable tuyến |
| 19 | POST | `/route/preview-import-routes` | Preview import tuyến |
| 20 | POST | `/route/confirm-import-routes` | Confirm import tuyến |
| 21 | GET | `/station/list-stations` | DS ga |
| 22 | GET | `/station/get-station/{id}` | Chi tiết ga |
| 23 | POST | `/station/create-station` | Tạo ga |
| 24 | POST | `/station/update-station/{id}` | Sửa ga |
| 25 | POST | `/station/enable-station/{id}` | Enable ga |
| 26 | POST | `/station/disable-station/{id}` | Disable ga |
| 27 | POST | `/station/preview-import-stations` | Preview import ga |
| 28 | POST | `/station/confirm-import-stations` | Confirm import ga |
| 29 | GET | `/device/list-devices` | DS thiết bị |
| 30 | GET | `/device/get-device/{id}` | Chi tiết thiết bị |
| 31 | POST | `/device/create-device` | Tạo thiết bị |
| 32 | POST | `/device/update-device/{id}` | Sửa thiết bị |
| 33 | POST | `/device/enable-device/{id}` | Enable thiết bị |
| 34 | POST | `/device/disable-device/{id}` | Disable thiết bị |
| 35 | POST | `/device/preview-import-devices` | Preview import TB |
| 36 | POST | `/device/confirm-import-devices` | Confirm import TB |

### afc-ops-service Device Integration (3 APIs)

| # | Method | Endpoint | Mô tả |
|---|--------|----------|-------|
| 37 | POST | `/afc-ops/submit-heartbeat` | Heartbeat |
| 38 | POST | `/transaction/submit-tap-event` | Tap event |
| 39 | POST | `/afc-ops/submit-device-incident` | Incident |

### afc-ops-service Monitoring (7 APIs)

| # | Method | Endpoint | Mô tả |
|---|--------|----------|-------|
| 40 | GET | `/afc-ops/get-device-status` | Trạng thái TB |
| 41 | GET | `/afc-ops/get-device-heartbeats` | Lịch sử heartbeat |
| 42 | GET | `/transaction/search-transactions` | Tra cứu transaction |
| 43 | GET | `/transaction/get-transaction-detail` | Chi tiết transaction |
| 44 | GET | `/afc-ops/search-incidents` | Tra cứu incident |
| 45 | GET | `/afc-ops/get-incident/{id}` | Chi tiết incident |

### afc-ops-service Dashboard (5 APIs)

| # | Method | Endpoint | Mô tả |
|---|--------|----------|-------|
| 46 | GET | `/dashboard/summary` | Tổng quan |
| 47 | GET | `/dashboard/transaction-timeline` | Transaction timeline |
| 48 | GET | `/dashboard/route-station-summaries` | Theo tuyến/ga |
| 49 | GET | `/dashboard/recent-incidents` | Incident gần đây |
| 50 | GET | `/dashboard/alerts` | Cảnh báo |

### afc-ops-service Control Package (9 APIs)

| # | Method | Endpoint | Mô tả |
|---|--------|----------|-------|
| 51 | POST | `/control-package/create` | Tạo package |
| 52 | POST | `/control-package/update/{id}` | Sửa draft |
| 53 | GET | `/control-package/get-detail` | Chi tiết package |
| 54 | GET | `/control-package/list` | DS package |
| 55 | POST | `/control-package/publish/{id}` | Phát hành |
| 56 | GET | `/control-package/pull-pending` | C3 pull |
| 57 | POST | `/control-package/ack-apply/{syncId}` | C3 ack |
| 58 | GET | `/control-package/search-syncs` | DS sync |
| 59 | GET | `/control-package/get-sync-detail` | Chi tiết sync |

### afc-ops-service Batch (3 APIs)

| # | Method | Endpoint | Mô tả |
|---|--------|----------|-------|
| 60 | POST | `/batch/create-batch` | Tạo batch |
| 61 | GET | `/batch/list-batches` | DS batch |
| 62 | POST | `/batch/submit-batch-to-level5/{id}` | Gửi C5 |

### afc-ops-service QR & Khác (3 APIs)

| # | Method | Endpoint | Mô tả |
|---|--------|----------|-------|
| 63 | POST | `/qr/generate-dynamic-qr` | Lấy QR động |
| 64 | GET | `/reconciliation/settlements` | Đối soát C5 |
| 65 | POST | `/transaction/submit-batch` | C2 batch về đêm |

### Audit (4 APIs)

| # | Method | Endpoint | Mô tả |
|---|--------|----------|-------|
| 66 | GET | `/auth/search-audit-logs` | Audit auth |
| 67 | GET | `/auth/get-audit-log/{id}` | Chi tiết audit auth |
| 68 | GET | `/audit/search-audit-logs` | Audit AFC |
| 69 | GET | `/audit/get-audit-log/{id}` | Chi tiết audit AFC |

---

## 7. Công Nghệ Sử Dụng

| Thành phần | Công nghệ |
|------------|-----------|
| Backend framework | Java Spring Boot 3.x |
| Database chính | PostgreSQL 16 |
| NoSQL | MongoDB 7.x |
| Cache | Redis 7.x |
| Message broker | RabbitMQ |
| ORM | Spring Data JPA (Hibernate) |
| Build tool | Maven |
| CI/CD (dự kiến) | Docker, Docker Compose |
| QR crypto | HMAC-SHA256 |

---

## 8. Wireframe & Giao Diện

Đã thiết kế wireframe đầy đủ cho tất cả màn hình:

| Màn hình | Actor | File tham khảo |
|----------|-------|---------------|
| Đăng nhập | Tất cả | UC01 |
| Quản lý tài khoản | OPERATOR_ADMIN | UC02 |
| Đổi mật khẩu | Tất cả | UC03 |
| Quản lý tuyến | OPERATOR_MANAGER | UC05 |
| Quản lý ga/trạm | OPERATOR_MANAGER | UC06 |
| Quản lý thiết bị | OPERATOR_MANAGER | UC07 |
| Giám sát thiết bị | MANAGER + STATION | UC09 |
| Giao dịch vé | MANAGER + STATION | UC11 |
| Sự cố thiết bị | MANAGER + STATION | UC13 |
| Dashboard | OPERATOR_MANAGER | UC18 |
| Control Package | OPERATOR_MANAGER | UC15-16 |
| Trạng thái áp dụng | MANAGER + STATION | UC17 |
| Lô dữ liệu đối soát | OPERATOR_MANAGER | UC19-20 |
| Audit | OPERATOR_ADMIN | UC21 |

Chi tiết: `document/wireframe_design_workspace_afc_ops.md`

---

## 9. Danh Sách Tài Liệu Dự Án

| File | Nội dung |
|------|----------|
| `API Contract AFC Cấp 3 và Cấp 4.md` | API contract chi tiết cho 69 endpoints |
| `Use Case Spec AFC Cấp 3 và Cấp 4.md` | Đặc tả 22 use case |
| `Draft Schema auth_db và afc_ops_db.md` | Schema DB chi tiết |
| `Draft_Flow.md` | 10 luồng dữ liệu MVP |
| `Tổng hợp Chức năng Hệ thống AFC - Cấp 3 và Cấp 4 V2.md` | Tổng hợp chức năng V2 |
| `Breakdown Chức năng AFC Cấp 3 và Cấp 4.md` | Breakdown theo phase P0-P2 |
| `wireframe_design_workspace_afc_ops.md` | Thiết kế wireframe đầy đủ |
| `Plan triển khai UC18 và UC21.md` | Plan Dashboard (đã xong) + Audit (đã xong) |
| `plan_uc15_uc16_uc17.md` | Plan Control Package (đã xong) |
| `plan_offline_device_and_control_package.md` | Plan Offline C2 + Control Package (đã xong) |
| `plan_c2_batch_transaction.md` | Plan C2 batch transaction (HMAC QR - đã xong) |
| `plan_refactor_c4_card_ticket_to_c5.md` | Plan merge Entitlement vào Ticket (đã xong) |
| `plan_ca_kip_offline_mode.md` | Plan Ca kíp + Cự ly ga (Cự ly ga đã xong, Ca kíp chưa) |
| `Plan UC21 audit và truy vết.md` | Plan chi tiết Audit |
| `Tổng hợp Hệ thống AFC Cấp 3 và Cấp 4 - Báo cáo tiến độ.md` | **File này** |