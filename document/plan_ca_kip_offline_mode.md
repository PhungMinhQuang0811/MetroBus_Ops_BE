# Kế hoạch triển khai Phân hệ Ca kíp, Chế độ ngoại tuyến (Offline Mode) & Cự ly ga

Tài liệu này chi tiết hóa thiết kế cơ sở dữ liệu, đặc tả API và luồng xử lý nghiệp vụ bổ sung cho hai tính năng bắt buộc của đề tài VTS Viettel (Ca kíp nhân viên, Hàng đợi ngoại tuyến Cấp 3) và cập nhật trường cự ly ga phục vụ tính giá vé.

---

## 1. Cấu hình Cự ly Ga (Station Distance)

### 1.1. Nghiệp vụ tính giá vé theo cự ly
Áp dụng Quyết định 3680/QĐ-UBND của UBND TP Hà Nội về phương pháp tính giá vé lượt theo khoảng cách:
$$\text{Giá vé lượt} = \text{Giá mở cửa} + (\text{Cự ly di chuyển} \times \text{Đơn giá 1km})$$

Trong đó:
* **Đối với đường sắt đô thị (Metro)**: Giá mở cửa = `8.000 đồng`, đơn giá 1km = `850 đồng`.
* **Đối với xe buýt**: Giá mở cửa = `3.000 đồng`, đơn giá 1km = `450 đồng` (chỉ áp dụng nếu sau này mở rộng vé lượt bus).
* **Cự ly di chuyển**: Khoảng cách tuyệt đối giữa ga đi và ga đến:
$$\text{Cự ly} = | \text{distance (Ga Đến)} - \text{distance (Ga Đi)} |$$

### 1.2. Cập nhật bảng `stations` (PostgreSQL)

Thêm cột `distance` kiểu số thực thập phân để lưu trữ khoảng cách lũy kế (km) từ ga đầu tiên của tuyến đến ga hiện tại.

```sql
ALTER TABLE stations ADD COLUMN distance DECIMAL(5, 2) NOT NULL DEFAULT 0.00;
```

#### Ví dụ dữ liệu mẫu của Tuyến Metro Cát Linh - Hà Đông:
* Ga Cát Linh (Ga đầu): `distance = 0.00`
* Ga La Thành: `distance = 0.96`
* Ga Thái Hà: `distance = 1.98`
* Ga Láng: `distance = 2.97`
* Ga Yên Nghĩa (Ga cuối): `distance = 13.05`

*Ví dụ khách đi từ Thái Hà (1.98) đến Yên Nghĩa (13.05):*
* $\text{Cự ly} = |13.05 - 1.98| = 11.07\text{ km}$
* $\text{Giá vé Metro} = 8.000 + (11.07 \times 850) = 17.409,5 \approx 17.500\text{ đồng}$.

---

## 2. Đối soát & Nhận phân chia doanh thu từ Cấp 5

### 2.1. Nghiệp vụ đối soát phân chia doanh thu
Sau khi Cấp 5 (Clearing House) nhận các lô giao dịch (batch) do Cấp 4 đẩy lên và thực hiện đối soát/bù trừ tài chính thành công, Cấp 5 sẽ gửi một sự kiện `SettlementConfirmedEvent` qua Message Broker (RabbitMQ) để Cấp 4 nhận diện và ghi nhận doanh thu thực tế được phân chia.

* **Cấu trúc Java Record của sự kiện nhận về từ Cấp 5 (`SettlementConfirmedEvent`)**:
```java
public record SettlementConfirmedEvent(
    UUID settlementId,
    String period,
    List<CompanyShareMessage> shares
) {}
```

* **Cấu trúc Java Record chi tiết của thông tin phân chia doanh thu (`CompanyShareMessage`)**:
```java
public record CompanyShareMessage(
    UUID operatorId,
    String operatorCode,
    BigDecimal allocatedAmount,
    BigDecimal totalKm,
    Integer totalTrips,
    BigDecimal kmRatio
) {}
```

* **Cách Cấp 4 xử lý**:
  * Lọc phần tử trong `shares` có `operatorCode` trùng với mã đơn vị của Cấp 4.
  * Lưu trữ thông tin đối soát vào cơ sở dữ liệu `afc_ops_db`.

### 2.2. Thiết kế Cơ sở dữ liệu (PostgreSQL - `afc_ops_db`)

#### Bảng `operator_settlements`
Lưu trữ thông tin doanh thu phân chia thực tế nhận được từ Cấp 5.

| Tên trường | Kiểu dữ liệu | Ràng buộc | Mô tả |
| :--- | :--- | :--- | :--- |
| `id` | BIGSERIAL | PK | Khóa chính tự tăng |
| `settlement_id` | VARCHAR(36) | NOT NULL | Định danh kỳ đối soát từ Cấp 5 |
| `period` | VARCHAR(30) | NOT NULL | Chu kỳ đối soát (Ví dụ: "2026-06-24") |
| `allocated_amount` | DECIMAL(15, 2) | NOT NULL | Số tiền doanh thu được phân chia |
| `total_km` | DECIMAL(10, 2) | NOT NULL | Tổng km di chuyển thực tế trên tuyến |
| `total_trips` | INT | NOT NULL | Tổng số chuyến đi (trips) thực hiện thành công |
| `km_ratio` | DECIMAL(5, 4) | NOT NULL | Tỷ lệ phân chia km (dùng làm cơ sở tính tiền) |
| `created_at` | TIMESTAMP | NOT NULL | Thời điểm lưu bản ghi |

### 2.3. API tra cứu lịch sử phân chia doanh thu

#### API-AFC-033: Tra cứu lịch sử đối soát và phân chia doanh thu
* **URL:** `GET /reconciliation/settlements?page=0&size=20`
* **Quyền hạn:** `OPERATOR_MANAGER`
* **Response Body:**
```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "items": [
      {
        "id": 1,
        "settlementId": "d3b07384-d113-495f-9e7b-c9e29f0322d1",
        "period": "2026-06-24",
        "allocatedAmount": 12545000.00,
        "totalKm": 1420.50,
        "totalTrips": 2450,
        "kmRatio": 0.3542,
        "createdAt": "2026-06-24T18:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

---

## 3. Phân hệ Quản lý Ca kíp Tối giản (Shift Management)

### 3.1. Luồng nghiệp vụ
Hệ thống ca trực được thiết kế tối giản để tối ưu hóa thời gian triển khai nhưng vẫn đảm bảo chặt chẽ về mặt nghiệp vụ:
1. **Nhận ca (Check-in)**: Nhân viên ga (`STATION_OPERATOR`) đăng nhập vào hệ thống Cấp 3 và thực hiện nhận ca trực tại ga được phân quyền. 
   * Trạng thái ca trực đổi thành `CHECKED_IN`.
   * Hệ thống kích hoạt trạng thái của tất cả các thiết bị Cấp 2 thuộc ga này thành `ACTIVE` (sẵn sàng quét thẻ).
2. **Soát vé**: Thiết bị soát vé chỉ chấp nhận lượt quét thẻ nếu ga đó có ít nhất một ca trực đang hoạt động (`CHECKED_IN`).
3. **Kết ca (Check-out & Cash-up)**: Nhân viên ga kết ca trước khi bàn giao.
   * Trạng thái ca trực đổi thành `CHECKED_OUT`.
   * Hệ thống tự động đếm tổng số giao dịch (`total_transactions`) phát sinh trong thời gian ca trực diễn ra để hiển thị báo cáo kết toán nhanh.
   * Hệ thống tự động vô hiệu hóa các thiết bị soát vé tại ga về trạng thái `DISABLED`/`OFFLINE` (trừ khi có nhân viên ca tiếp theo check-in).

### 3.2. Thiết kế Cơ sở dữ liệu (PostgreSQL - `afc_ops_db`)

#### Bảng `station_shifts`
Lưu trữ thông tin ca trực của nhân viên vận hành ga.

| Tên trường | Kiểu dữ liệu | Ràng buộc | Mô tả |
| :--- | :--- | :--- | :--- |
| `id` | BIGSERIAL | PK | Khóa chính tự tăng |
| `account_id` | VARCHAR(36) | NOT NULL | UUID nhân viên (tham chiếu mềm `auth_db.accounts`) |
| `station_id` | BIGINT | NOT NULL, FK `stations` | Ga trực tiếp nhận |
| `status` | VARCHAR(30) | NOT NULL | Trạng thái ca (`CHECKED_IN`, `CHECKED_OUT`) |
| `total_transactions` | INT | NOT NULL DEFAULT 0 | Tổng số giao dịch kết toán trong ca trực |
| `checked_in_at` | TIMESTAMP | NOT NULL | Thời điểm bắt đầu nhận ca |
| `checked_out_at` | TIMESTAMP | NULL | Thời điểm kết thúc ca |

### 3.3. Đặc tả API endpoints

#### API-AFC-030: Đăng nhập nhận ca (Check-in Shift)
* **URL:** `POST /shifts/check-in`
* **Quyền hạn:** `STATION_OPERATOR`
* **Request Body:**
```json
{
  "stationId": 1
}
```
* **Response Body:**
```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "shiftId": 5001,
    "accountId": "3c02cb1d-91b5-4b08-bdf4-f9ef6a575a7c",
    "stationId": 1,
    "status": "CHECKED_IN",
    "checkedInAt": "2026-06-24T06:00:00"
  }
}
```

#### API-AFC-031: Kết ca và Bàn giao (Check-out Shift)
* **URL:** `POST /shifts/check-out`
* **Quyền hạn:** `STATION_OPERATOR`
* **Response Body:**
```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "shiftId": 5001,
    "status": "CHECKED_OUT",
    "totalTransactions": 142,
    "checkedInAt": "2026-06-24T06:00:00",
    "checkedOutAt": "2026-06-24T14:00:00"
  }
}
```

---

## 4. Quản lý Bộ nhớ đệm Ngoại tuyến (Offline Mode) tại Cấp 3

### 4.1. Kiến trúc lưu đệm & đồng bộ ngầm
Để đảm bảo tính sẵn sàng cao khi mạng kết nối giữa Cấp 3 lên Cấp 4/5 bị gián đoạn, Cấp 3 sẽ lưu trữ tạm thời giao dịch soát vé vào hàng đợi cục bộ (Local Queue) bằng cấu trúc bảng trong Database của Cấp 3:

```text
[Cấp 2 Device] ---> (TAP_IN/TAP_OUT) ---> [Cấp 3 Ga Server]
                                              |
                                     (Ghi PENDING & Phản hồi)
                                              |
                                      [(offline_transactions)]
                                              |
                                     (Background Scheduler)
                                              |
                                     (REST API Batch Sync)
                                              v
                                     [Cấp 4 Central Cloud]
```

1. **Ghi nhận ngoại tuyến**: Khi nhận sự kiện soát vé từ Cấp 2, Cấp 3 thực hiện kiểm tra chữ ký của QR Code ngoại tuyến (bằng Public Key). Sau đó lưu thông tin sự kiện vào bảng tạm với trạng thái `PENDING` và lập tức mở cổng cho khách.
2. **Đồng bộ ngầm (Background Worker)**: Một tác vụ chạy ngầm định kỳ (mỗi 5-10 giây) sẽ quét các giao dịch `PENDING` và đẩy theo lô (Batch) lên API của Cấp 4. Đẩy thành công sẽ cập nhật trạng thái giao dịch cục bộ thành `SYNCED`.

### 4.2. Thiết kế Cơ sở dữ liệu (PostgreSQL/H2 Local - Cấp 3)

#### Bảng `offline_transactions`
Hàng đợi lưu trữ giao dịch tạm thời tại máy chủ Ga.

| Tên trường | Kiểu dữ liệu | Ràng buộc | Mô tả |
| :--- | :--- | :--- | :--- |
| `id` | VARCHAR(36) | PK | Mã giao dịch UUID |
| `event_id` | VARCHAR(100) | NOT NULL | Idempotency Key gửi từ thiết bị Cấp 2 |
| `device_code` | VARCHAR(100) | NOT NULL | Mã thiết bị soát vé |
| `station_code` | VARCHAR(50) | NOT NULL | Mã nhà ga phát sinh giao dịch |
| `qr_payload` | TEXT | NOT NULL | Payload chuỗi QR động đã quét |
| `tap_type` | VARCHAR(30) | NOT NULL | Loại quét (`TAP_IN`, `TAP_OUT`) |
| `occurred_at` | TIMESTAMP | NOT NULL | Thời gian quét thực tế tại thiết bị |
| `sync_status` | VARCHAR(30) | NOT NULL | Trạng thái đồng bộ (`PENDING`, `SYNCED`, `FAILED`) |
| `retry_count` | INT | NOT NULL DEFAULT 0 | Số lần thử đồng bộ lại |
| `error_message` | TEXT | NULL | Thông tin lỗi kỹ thuật khi đẩy dữ liệu thất bại |

### 4.3. Đặc tả API đồng bộ dữ liệu

#### API-AFC-032: Đồng bộ lô giao dịch ngoại tuyến (Batch Sync Transactions)
* **URL:** `POST /transactions/sync-batch`
* **Quyền hạn:** Client Cấp 3 (Third-party client xác thực qua mã ga)
* **Request Body:**
```json
{
  "stationCode": "BEN-THANH",
  "transactions": [
    {
      "id": "tx-9901-abcd",
      "eventId": "event-101",
      "deviceCode": "GATE-BT-01",
      "qrPayload": "signed-qr-string-here...",
      "tapType": "TAP_IN",
      "occurredAt": "2026-06-24T08:30:15"
    }
  ]
}
```
* **Response Body:**
```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "syncedCount": 1,
    "status": "COMPLETED"
  }
}
```

---

## 5. Kế hoạch triển khai & Kiểm thử (2 Tuần)

### Tuần 1: Cập nhật Cự ly Ga & Đối soát Quyết toán Cấp 5 (Làm trước)
1. **Ngày 1-2**: Thực hiện chạy migration SQL cập nhật bảng `stations` (thêm cột `distance` DECIMAL(5, 2)). Chỉnh sửa Java Entity `Station`, DTO, REST API tương ứng và cập nhật logic Import Excel Master Data để hỗ trợ lưu cự ly ga.
2. **Ngày 3-4**: Tạo bảng `operator_settlements`. Định nghĩa cấu trúc Java Record `SettlementConfirmedEvent` và `CompanyShareMessage`. Khai báo cấu hình RabbitMQ (Queue, Exchange, Routing Key) trong `application.yaml` và `MessageBrokerConfig`. Viết `Level5SettlementSyncListener` để lắng nghe sự kiện đối soát từ Cấp 5, lọc theo `operatorCode` và lưu thông tin vào DB.
3. **Ngày 5**: Viết API tra cứu lịch sử quyết toán doanh thu `/reconciliation/settlements` và bổ sung Unit Test bao phủ logic đối soát đạt > 80% coverage.

### Tuần 2: Hàng đợi Ngoại tuyến & Phân hệ Ca trực (Làm sau)
1. **Ngày 6-7**: Tạo bảng `offline_transactions`. Xây dựng Scheduler ngầm tại Cấp 3 để gửi lô giao dịch và API đồng bộ lô `/transactions/sync-batch` tại Cấp 4.
2. **Ngày 8-9**: Tạo bảng `station_shifts`. Viết logic API nhận ca, kết ca (`/shifts/check-in`, `/shifts/check-out`) và cấu hình trạng thái hoạt động của các thiết bị soát vé tại ga.
3. **Ngày 10-11**: Tích hợp kiểm tra ca trực hoạt động và offline sync vào API xử lý soát vé `UC10`. Kiểm thử tích hợp E2E giả lập quét QR.
4. **Ngày 12-14**: Sửa lỗi tích hợp, tối ưu hiệu năng và hoàn thiện tài liệu báo cáo hội đồng.
