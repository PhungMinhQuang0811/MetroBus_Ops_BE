# Breakdown Chức Năng AFC Cấp 3 Và Cấp 4

Tài liệu này break nhỏ chức năng từ `document/Tổng hợp Chức năng Hệ thống AFC - Cấp 3 và Cấp 4 V2.md`.

Mục tiêu hiện tại là liệt kê chức năng ở mức có thể triển khai dần. Chưa chốt schema chi tiết.

## 1. Quy Ước Priority

| Priority | Ý nghĩa |
| --- | --- |
| P0 | Cần có để MVP chạy được luồng chính Cấp 3/Cấp 4 |
| P1 | Nên có để demo rõ nghiệp vụ vận hành |
| P2 | Có thể để sau nếu thiếu thời gian |

## 2. Actor

| Actor | Vai trò |
| --- | --- |
| OPERATOR_ADMIN | Admin nội bộ của đơn vị vận hành Cấp 4 |
| OPERATOR_MANAGER | Quản lý vận hành Cấp 4 |
| STATION_OPERATOR | Nhân viên/giám sát tại ga/trạm/tuyến Cấp 3 |
| DEVICE_CLIENT | Thiết bị hoặc hệ thống Cấp 2 gửi dữ liệu lên Cấp 3 |
| LEVEL5_SYSTEM | Hệ thống Cấp 5 hoặc mock dev/test |

## 3. Nhóm Chức Năng Cấp 3

### 3.1. Thu Thập Dữ Liệu Từ Thiết Bị Cấp 2

| Mã | Chức năng nhỏ | Actor | Priority |
| --- | --- | --- | --- |
| L3-ING-01 | Nhận heartbeat từ thiết bị | DEVICE_CLIENT | P0 |
| L3-ING-02 | Cập nhật trạng thái hiện tại của thiết bị | DEVICE_CLIENT | P0 |
| L3-ING-03 | Nhận tap/check event từ thiết bị | DEVICE_CLIENT | P0 |
| L3-ING-04 | Kiểm tra idempotency theo device/event | DEVICE_CLIENT | P0 |
| L3-ING-05 | Lưu raw payload event vào MongoDB | DEVICE_CLIENT | P1 |
| L3-ING-06 | Nhận incident/error event từ thiết bị | DEVICE_CLIENT | P1 |
| L3-ING-07 | Lưu incident log vào MongoDB | DEVICE_CLIENT | P1 |

### 3.2. Xử Lý Kỹ Thuật Tại Cấp 3

| Mã | Chức năng nhỏ | Actor | Priority |
| --- | --- | --- | --- |
| L3-VAL-01 | Validate device tồn tại và đang hoạt động | DEVICE_CLIENT | P0 |
| L3-VAL-02 | Validate station/route của device | DEVICE_CLIENT | P0 |
| L3-VAL-03 | Validate direction/tap type ở mức kỹ thuật | DEVICE_CLIENT | P0 |
| L3-VAL-04 | Áp dụng rule/config local đã đồng bộ | DEVICE_CLIENT | P1 |
| L3-VAL-05 | Trả decision `OPEN_GATE`/`DENY`/`ACCEPTED_FOR_FORWARDING` | DEVICE_CLIENT | P0 |
| L3-VAL-06 | Không gọi Cấp 4 realtime cho từng lượt tap | System rule | P0 |

### 3.3. Lưu Vết Và Chuyển Tiếp Dữ Liệu

| Mã | Chức năng nhỏ | Actor | Priority |
| --- | --- | --- | --- |
| L3-TXN-01 | Ghi transaction vận hành đã chuẩn hóa | DEVICE_CLIENT | P0 |
| L3-TXN-02 | Gắn transaction với device/station/route/operator | System | P0 |
| L3-TXN-03 | Đánh dấu trạng thái sync ban đầu | System | P0 |
| L3-TXN-04 | Forward/sync transaction lên phần Cấp 4 trong `afc-ops-service` | System | P0 |
| L3-TXN-05 | Retry khi sync lỗi | System | P1 |
| L3-TXN-06 | Ghi log request/response sync nếu cần trace | System | P2 |

### 3.4. Nhận Cấu Hình Từ Cấp 4

| Mã | Chức năng nhỏ | Actor | Priority |
| --- | --- | --- | --- |
| L3-CONF-01 | Station/Cấp 3 pull control package mới nhất | STATION_OPERATOR/System | P1 |
| L3-CONF-02 | Áp dụng control package local | System | P1 |
| L3-CONF-03 | Ack trạng thái applied/failed | System | P1 |
| L3-CONF-04 | Xem version config hiện tại tại station | STATION_OPERATOR | P2 |

### 3.5. Giám Sát Tại Ga/Trạm

| Mã | Chức năng nhỏ | Actor | Priority |
| --- | --- | --- | --- |
| L3-MON-01 | Xem danh sách thiết bị thuộc station | STATION_OPERATOR | P0 |
| L3-MON-02 | Xem thiết bị online/offline/maintenance | STATION_OPERATOR | P0 |
| L3-MON-03 | Xem transaction trong phạm vi station | STATION_OPERATOR | P0 |
| L3-MON-04 | Xem incident trong phạm vi station | STATION_OPERATOR | P1 |
| L3-MON-05 | Lọc dữ liệu theo thời gian/device/status | STATION_OPERATOR | P1 |

## 4. Nhóm Chức Năng Cấp 4

### 4.1. Quản Lý Hạ Tầng Mạng Lưới

| Mã | Chức năng nhỏ | Actor | Priority |
| --- | --- | --- | --- |
| L4-MD-01 | Khởi tạo hoặc cấu hình operator mặc định | OPERATOR_ADMIN | P0 |
| L4-MD-02 | Tạo/cập nhật route | OPERATOR_MANAGER | P0 |
| L4-MD-03 | Tạo/cập nhật station | OPERATOR_MANAGER | P0 |
| L4-MD-04 | Sắp xếp thứ tự station trong route | OPERATOR_MANAGER | P1 |
| L4-MD-05 | Tạo/cập nhật device | OPERATOR_MANAGER | P0 |
| L4-MD-06 | Disable/enable device | OPERATOR_MANAGER | P0 |
| L4-MD-07 | Không xóa cứng master data đã phát sinh transaction | System rule | P0 |

### 4.2. Quản Lý Tài Khoản Nội Bộ

| Mã | Chức năng nhỏ | Actor | Priority |
| --- | --- | --- | --- |
| L4-AUTH-01 | Init account `OPERATOR_ADMIN` đầu tiên | System | P0 |
| L4-AUTH-02 | Đăng nhập nội bộ | OPERATOR_ADMIN/OPERATOR_MANAGER/STATION_OPERATOR | P0 |
| L4-AUTH-03 | Tạo account nhân sự | OPERATOR_ADMIN | P0 |
| L4-AUTH-04 | Cập nhật account profile | OPERATOR_ADMIN | P1 |
| L4-AUTH-05 | Gán role cho account | OPERATOR_ADMIN | P0 |
| L4-AUTH-06 | Khóa/mở khóa account | OPERATOR_ADMIN | P0 |
| L4-AUTH-07 | Không tự khóa operator admin cuối cùng | System rule | P0 |

### 4.3. Giám Sát Transaction Và Thiết Bị Toàn Operator

| Mã | Chức năng nhỏ | Actor | Priority |
| --- | --- | --- | --- |
| L4-MON-01 | Xem tổng quan thiết bị toàn operator | OPERATOR_MANAGER | P0 |
| L4-MON-02 | Xem transaction toàn operator | OPERATOR_MANAGER | P0 |
| L4-MON-03 | Lọc transaction theo route/station/device/time/decision | OPERATOR_MANAGER | P0 |
| L4-MON-04 | Xem transaction bị deny | OPERATOR_MANAGER | P1 |
| L4-MON-05 | Xem incident toàn operator | OPERATOR_MANAGER | P1 |
| L4-MON-06 | Xem trạng thái sync Cấp 3 -> Cấp 4 | OPERATOR_MANAGER | P1 |

### 4.4. Phát Hành Control Package Xuống Cấp 3

| Mã | Chức năng nhỏ | Actor | Priority |
| --- | --- | --- | --- |
| L4-CONF-01 | Tạo control package | OPERATOR_MANAGER | P1 |
| L4-CONF-02 | Chọn package type: `DEVICE_CONFIG`, `MEDIA_ACCESS_RULES`, `TIME_SYNC`, `ALL` | OPERATOR_MANAGER | P1 |
| L4-CONF-03 | Lưu payload package vào MongoDB | System | P1 |
| L4-CONF-04 | Publish package cho station/Cấp 3 | OPERATOR_MANAGER | P1 |
| L4-CONF-05 | Theo dõi station đã applied/failed | OPERATOR_MANAGER | P1 |
| L4-CONF-06 | Không quản lý bộ quy tắc chung toàn mạng | System rule | P0 |

### 4.5. Dashboard Và Báo Cáo Vận Hành

| Mã | Chức năng nhỏ | Actor | Priority |
| --- | --- | --- | --- |
| L4-RPT-01 | Tổng hợp số transaction theo ngày | OPERATOR_MANAGER | P0 |
| L4-RPT-02 | Tổng hợp transaction theo route/station | OPERATOR_MANAGER | P0 |
| L4-RPT-03 | Tổng hợp decision `OPEN_GATE`/`DENY` | OPERATOR_MANAGER | P1 |
| L4-RPT-04 | Tổng hợp thiết bị online/offline | OPERATOR_MANAGER | P0 |
| L4-RPT-05 | Tổng hợp incident theo loại/mức độ | OPERATOR_MANAGER | P1 |
| L4-RPT-06 | Export báo cáo CSV/Excel | OPERATOR_MANAGER | P2 |

### 4.6. Tạo Batch Dữ Liệu Gửi Cấp 5

| Mã | Chức năng nhỏ | Actor | Priority |
| --- | --- | --- | --- |
| L4-BATCH-01 | Tạo batch theo khoảng thời gian | OPERATOR_MANAGER | P1 |
| L4-BATCH-02 | Chọn transaction đủ điều kiện đưa vào batch | System | P1 |
| L4-BATCH-03 | Gửi batch lên `LEVEL5_SYSTEM` hoặc mock dev/test | System | P1 |
| L4-BATCH-04 | Nhận ACK/REJECT | LEVEL5_SYSTEM | P1 |
| L4-BATCH-05 | Cập nhật trạng thái batch | System | P1 |
| L4-BATCH-06 | Lưu request/response batch vào MongoDB | System | P1 |
| L4-BATCH-07 | Không xử lý clearing/doanh thu/đối soát tài chính | System rule | P0 |

## 5. Nhóm Chức Năng MongoDB/Audit/Trace

| Mã | Chức năng nhỏ | Actor | Priority |
| --- | --- | --- | --- |
| LOG-01 | Ghi audit đăng nhập/đăng xuất | System | P1 |
| LOG-02 | Ghi audit thay đổi account/role | System | P1 |
| LOG-03 | Ghi audit thay đổi route/station/device | System | P1 |
| LOG-04 | Ghi raw device event | System | P1 |
| LOG-05 | Ghi heartbeat log lịch sử | System | P2 |
| LOG-06 | Ghi incident log thiết bị | System | P1 |
| LOG-07 | Ghi integration request/response | System | P1 |
| LOG-08 | Không lưu raw media token nhạy cảm | System rule | P0 |

## 6. MVP Theo Giai Đoạn

### Phase 1 - Nền Tảng Nội Bộ Và Master Data

P0:

- L4-AUTH-01 đến L4-AUTH-07.
- L4-MD-01, L4-MD-02, L4-MD-03, L4-MD-05, L4-MD-06.

Kết quả mong muốn:

- Có account nội bộ đăng nhập.
- Có route/station/device để thiết bị gửi dữ liệu.

### Phase 2 - Cấp 3 Nhận Dữ Liệu Thiết Bị

P0:

- L3-ING-01 đến L3-ING-04.
- L3-VAL-01 đến L3-VAL-06.
- L3-TXN-01 đến L3-TXN-04.

Kết quả mong muốn:

- Device gửi heartbeat được.
- Device gửi tap/check event được.
- Hệ thống ghi transaction và trả decision.

### Phase 3 - Giám Sát Và Tra Cứu

P0/P1:

- L3-MON-01 đến L3-MON-05.
- L4-MON-01 đến L4-MON-06.
- L4-RPT-01, L4-RPT-02, L4-RPT-04.

Kết quả mong muốn:

- Station operator xem được thiết bị/giao dịch tại station.
- Operator manager xem được dữ liệu toàn operator.

### Phase 4 - Control Package

P1:

- L4-CONF-01 đến L4-CONF-06.
- L3-CONF-01 đến L3-CONF-04.

Kết quả mong muốn:

- Cấp 4 phát hành config/rule.
- Cấp 3 nhận và ack package.

### Phase 5 - Batch Cấp 5 Và Audit

P1/P2:

- L4-BATCH-01 đến L4-BATCH-07.
- LOG-01 đến LOG-08.

Kết quả mong muốn:

- Cấp 4 gửi batch dữ liệu vận hành lên Cấp 5/mock.
- Có log/audit đủ để trace.

## 7. Out Of Scope Cần Giữ Chặt

- Passenger app.
- Đăng ký/đăng nhập hành khách.
- Bán vé, phát hành vé, mua vé online.
- Ví, payment, ngân hàng, trừ tiền realtime.
- Fare engine/tính giá vé.
- Clearing/settlement.
- Phân bổ doanh thu.
- Đối soát tài chính.
- Quản lý bộ quy tắc chung toàn mạng của Cấp 5.
- Quản lý an toàn thông tin liên thông toàn hệ thống AFC.
