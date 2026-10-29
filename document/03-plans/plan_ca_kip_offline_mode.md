# Kế hoạch triển khai Phân hệ Ca kíp & Cự ly ga

Tài liệu này chi tiết hóa thiết kế cơ sở dữ liệu, đặc tả API và luồng xử lý nghiệp vụ bổ sung cho tính năng Ca kíp nhân viên và cập nhật trường cự ly ga phục vụ tính giá vé.

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

## 4. Kế hoạch triển khai Ca kíp

### Các bước triển khai

| Bước | Nội dung | File liên quan |
|------|----------|---------------|
| 1 | Tạo bảng `station_shifts` (DB migration) | `resources/db/migration/` |
| 2 | Tạo Entity `StationShift` | `entity/StationShift.java` |
| 3 | Tạo Repository `StationShiftRepository` | `repository/StationShiftRepository.java` |
| 4 | Tạo DTO request/response cho check-in, check-out | `dto/request/shift/`, `dto/response/shift/` |
| 5 | Tạo Service `ShiftService` | `service/impl/ShiftService.java` |
| 6 | Tạo Controller `ShiftController` với 2 endpoints | `controller/ShiftController.java` |
| 7 | Đăng ký permission `SHIFT_WRITE` | Seed/RBAC config |
| 8 | Cập nhật `SecurityConstants` nếu cần (STATION_OPERATOR permission) | `config/SecurityConstants.java` |
| 9 | Unit test Service + Controller | Test files |

### Chi tiết

#### Entity `StationShift`
```java
@Entity
@Table(name = "station_shifts")
public class StationShift {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false, length = 36)
    private String accountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @Column(name = "status", nullable = false, length = 30)
    private String status; // CHECKED_IN, CHECKED_OUT

    @Column(name = "total_transactions", nullable = false)
    private Integer totalTransactions = 0;

    @Column(name = "checked_in_at", nullable = false)
    private LocalDateTime checkedInAt;

    @Column(name = "checked_out_at")
    private LocalDateTime checkedOutAt;
}
```

#### Service logic
- **Check-in**: Kiểm tra station active, account không có shift CHECKED_IN khác cùng station → tạo shift mới
- **Check-out**: Tìm shift CHECKED_IN theo account_id, đếm `afc_transactions` trong khoảng `checked_in_at → now()` → cập nhật total_transactions, set CHECKED_OUT + checked_out_at

### Ghi chú
- Không cần tích hợp Ca kíp vào luồng soát vé UC10 trong MVP vì C2 chỉ là webcam giả lập
- API check-in/check-out đủ để demo quy trình vận hành Ca kíp