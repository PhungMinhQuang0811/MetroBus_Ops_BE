# Kế hoạch triển khai UC15, UC16, UC17 (Control Package Management & Distribution)

Tài liệu này chi tiết hóa thiết kế cơ sở dữ liệu, đặc tả API và luồng xử lý nghiệp vụ nội bộ cho nhóm chức năng quản lý cấu hình vận hành (Control Package) từ Cấp 4 xuống Cấp 3.

---

## 1. Mô hình nghiệp vụ & Luồng đi dữ liệu

Hệ thống quản lý cấu hình hoạt động theo 3 bước tương ứng với 3 Use Case:
1. **UC15 (Tạo cấu hình):** Người quản lý (`OPERATOR_MANAGER`) tạo một gói cấu hình nháp (`status = CREATED`).
   * Metadata (phiên bản, loại gói, người tạo) lưu trong PostgreSQL.
   * Chi tiết cấu hình động (JSON payload) lưu trong MongoDB.
2. **UC16 (Phát hành cấu hình):** Người quản lý chọn danh sách Ga/Trạm (`stationIds`) và phát hành gói cấu hình (`status = PUBLISHED`).
   * Hệ thống tự động tạo các bản ghi đồng bộ trạng thái (`station_control_syncs`) ở trạng thái `PENDING` cho từng trạm.
3. **UC17 (Trạm nhận & Áp dụng):**
   * Định kỳ (hoặc khi khởi động), client Cấp 3 của Ga/Trạm gọi API để lấy các gói cấu hình chưa áp dụng (`PENDING`).
   * Sau khi áp dụng thành công (hoặc thất bại), client Cấp 3 gọi API phản hồi trạng thái (`APPLIED` hoặc `FAILED` kèm thông tin lỗi).

---

## 2. Thiết kế Cơ sở dữ liệu (Database Schema)

### 2.1. PostgreSQL (Bảng Master & Trạng thái đồng bộ)

#### Bảng `control_packages`
Lưu thông tin định danh và lịch sử phát hành của gói cấu hình.

| Tên trường | Kiểu dữ liệu | Ràng buộc | Mô tả |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, IDENTITY | Khóa chính tự tăng |
| `operator_id` | BIGINT | NOT NULL, FK `operators` | Đơn vị vận hành quản lý gói |
| `version` | BIGINT | NOT NULL | Số phiên bản cấu hình (tự tăng theo operator) |
| `package_type` | VARCHAR(50) | NOT NULL | Loại cấu hình (`DEVICE_CONFIG` hoặc `MEDIA_ACCESS_RULES`) |
| `source_type` | VARCHAR(30) | NOT NULL | Nguồn tạo (`LEVEL4_CREATED` hoặc `LEVEL5_SYNCED`) |
| `external_package_code`| VARCHAR(100) | NULL | Mã code từ C5 (nếu sync từ C5) |
| `status` | VARCHAR(30) | NOT NULL | Trạng thái gói (`CREATED`, `PUBLISHED`, `REVOKED`) |
| `payload_ref` | VARCHAR(100) | NULL | Tham chiếu tới `_id` trong MongoDB |
| `created_by_account_id`| VARCHAR(36) | NULL | UUID người tạo (tham chiếu mềm `auth_db.accounts`) |
| `published_at` | TIMESTAMP | NULL | Thời điểm phát hành |
| `created_at` | TIMESTAMP | NOT NULL | Thời điểm khởi tạo |
| `updated_at` | TIMESTAMP | NOT NULL | Thời điểm cập nhật cuối cùng |

* **Unique Constraint:** `(operator_id, version)` để đảm bảo tính tăng dần và duy nhất của phiên bản cấu hình theo từng đơn vị vận hành.

#### Bảng `station_control_syncs`
Lưu trạng thái đồng bộ và áp dụng gói cấu hình của từng Ga/Trạm Cấp 3.

| Tên trường | Kiểu dữ liệu | Ràng buộc | Mô tả |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, IDENTITY | Khóa chính tự tăng |
| `station_id` | BIGINT | NOT NULL, FK `stations` | Ga/Trạm tiếp nhận |
| `control_package_id` | BIGINT | NOT NULL, FK `control_packages` | Gói cấu hình tương ứng |
| `sync_status` | VARCHAR(30) | NOT NULL | Trạng thái đồng bộ (`PENDING`, `APPLIED`, `FAILED`) |
| `retry_count` | INT | NOT NULL, DEFAULT 0 | Số lần thử đồng bộ |
| `last_attempt_at` | TIMESTAMP | NULL | Lần thử đồng bộ cuối cùng |
| `applied_at` | TIMESTAMP | NULL | Thời điểm áp dụng thành công tại trạm |
| `error_message` | TEXT | NULL | Chi tiết lỗi nếu áp dụng thất bại |
| `created_at` | TIMESTAMP | NOT NULL | Thời điểm tạo lệnh đồng bộ |
| `updated_at` | TIMESTAMP | NOT NULL | Thời điểm cập nhật cuối cùng |

* **Unique Constraint:** `(station_id, control_package_id)` để đảm bảo mỗi trạm chỉ có duy nhất một trạng thái đồng bộ cho mỗi gói cấu hình.

---

### 2.2. MongoDB (Lưu Payload chi tiết)

#### Collection `control_package_payloads`
Lưu trữ cấu hình chi tiết dưới dạng JSON Document để hỗ trợ cấu trúc động linh hoạt.

```json
{
  "_id": "ObjectId / UUID String",
  "control_package_id": 101,
  "package_type": "DEVICE_CONFIG",
  "source_type": "LEVEL4_CREATED",
  "version": 13,
  "payload": {
    "maxOfflineSeconds": 60,
    "allowOfflineValidation": true,
    "deviceTypes": ["QR_SCANNER_SIMULATOR"]
  },
  "created_at": "ISODate"
}
```

---

## 3. Danh sách API endpoints

### 3.1. UC15 - Quản lý Control Package

#### API-AFC-022: Tạo gói cấu hình
* **URL:** `POST /control-package/create`
* **Quyền hạn:** `CONTROL_PACKAGE_WRITE`
* **Mô tả:** Tạo gói cấu hình ở trạng thái nháp (`CREATED`). Phiên bản (`version`) tự tăng dựa trên phiên bản lớn nhất hiện có của Operator đó.
* **Request Body:**
```json
{
  "packageType": "DEVICE_CONFIG",
  "payload": {
    "maxOfflineSeconds": 60,
    "allowOfflineValidation": true,
    "deviceTypes": ["QR_SCANNER_SIMULATOR"]
  }
}
```
* **Response Body:**
```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "id": 101,
    "version": 13,
    "packageType": "DEVICE_CONFIG",
    "sourceType": "LEVEL4_CREATED",
    "status": "CREATED",
    "createdAt": "2026-06-18T15:30:00"
  }
}
```
* **Các lỗi thường gặp:**
  * `INVALID_CONTROL_PACKAGE_TYPE` (Code 2022, HTTP 400): Loại gói cấu hình không hợp lệ.
  * `INVALID_CONTROL_PACKAGE_PAYLOAD` (Code 2025, HTTP 400): Payload rỗng hoặc không truyền.
  * `INVALID_DEVICE_TYPE` (Code 2014, HTTP 400): Có loại thiết bị trong `deviceTypes` của payload không hợp lệ (không phải `QR_SCANNER_SIMULATOR`).

#### API-AFC-022A: Cập nhật gói cấu hình nháp
* **URL:** `POST /control-package/update/{packageId}`
* **Quyền hạn:** `CONTROL_PACKAGE_WRITE`
* **Mô tả:** Cập nhật dữ liệu payload của gói cấu hình ở trạng thái nháp (`CREATED`). Chỉ áp dụng với gói do Cấp 4 tạo (`LEVEL4_CREATED`) và chưa có bản ghi phát hành (`station_control_syncs`).
* **Request Body:**
```json
{
  "payload": {
    "maxOfflineSeconds": 90,
    "allowOfflineValidation": true,
    "deviceTypes": ["QR_SCANNER_SIMULATOR"]
  }
}
```
* **Response Body:**
```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "id": 101,
    "version": 13,
    "packageType": "DEVICE_CONFIG",
    "sourceType": "LEVEL4_CREATED",
    "status": "CREATED",
    "updatedAt": "2026-06-18T16:00:00"
  }
}
```
* **Các lỗi thường gặp:**
  * `CONTROL_PACKAGE_NOT_FOUND` (Code 3028, HTTP 404): Không tìm thấy gói cấu hình tương ứng (hoặc thuộc đơn vị vận hành khác).
  * `CONTROL_PACKAGE_NOT_EDITABLE` (Code 3029, HTTP 400): Gói cấu hình đã được phát hành hoặc không phải do Cấp 4 tạo.
  * `CONTROL_PACKAGE_PAYLOAD_NOT_FOUND` (Code 3032, HTTP 404): Không tìm thấy payload tương ứng trong MongoDB.
  * `INVALID_CONTROL_PACKAGE_PAYLOAD` (Code 2025, HTTP 400): Payload rỗng hoặc không truyền.
  * `INVALID_DEVICE_TYPE` (Code 2014, HTTP 400): Có loại thiết bị trong `deviceTypes` của payload không hợp lệ.

#### API-AFC-022B: Chi tiết gói cấu hình
* **URL:** `GET /control-package/get-detail?packageId={packageId}`
* **Quyền hạn:** `CONTROL_PACKAGE_READ`
* **Mô tả:** Lấy thông tin chi tiết bao gồm siêu dữ liệu (metadata) của gói cấu hình và payload chi tiết được tải từ MongoDB.
* **Response Body:**
```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "id": 101,
    "version": 13,
    "packageType": "DEVICE_CONFIG",
    "sourceType": "LEVEL4_CREATED",
    "status": "CREATED",
    "payload": {
      "maxOfflineSeconds": 90,
      "allowOfflineValidation": true,
      "deviceTypes": ["QR_SCANNER_SIMULATOR"]
    },
    "createdByAccountId": "3c02cb1d-91b5-4b08-bdf4-f9ef6a575a7c",
    "createdAt": "2026-06-18T15:30:00",
    "updatedAt": "2026-06-18T16:00:00",
    "publishedAt": null
  }
}
```
* **Các lỗi thường gặp:**
  * `CONTROL_PACKAGE_NOT_FOUND` (Code 3028, HTTP 404): Không tìm thấy gói cấu hình tương ứng.
  * `CONTROL_PACKAGE_PAYLOAD_NOT_FOUND` (Code 3032, HTTP 404): Không tìm thấy payload tương ứng trong MongoDB.

#### API-AFC-023: Danh sách các gói cấu hình
* **URL:** `GET /control-package/list?packageType=&sourceType=&status=&page=0&size=20`
* **Quyền hạn:** `CONTROL_PACKAGE_READ`
* **Mô tả:** Trả về danh sách phân trang các gói cấu hình thuộc phạm vi quản lý của Operator hiện tại.
* **Response Body:**
```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "items": [
      {
        "id": 101,
        "version": 13,
        "packageType": "DEVICE_CONFIG",
        "sourceType": "LEVEL4_CREATED",
        "status": "CREATED",
        "createdAt": "2026-06-18T15:30:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```
* **Các lỗi thường gặp:**
  * `INVALID_PAGE_REQUEST` (Code 2001, HTTP 400): Phân trang không hợp lệ.

---

### 3.2. UC16 - Phát hành Control Package

#### API-AFC-024: Phát hành gói cấu hình xuống Cấp 3
* **URL:** `POST /control-package/publish/{packageId}`
* **Quyền hạn:** `CONTROL_PACKAGE_WRITE`
* **Mô tả:** Chuyển trạng thái gói cấu hình sang `PUBLISHED` và tạo các bản ghi trạng thái `PENDING` cho danh sách trạm được chọn.
* **Request Body:**
```json
{
  "stationIds": [1, 2, 3]
}
```
* **Response Body:**
```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "packageId": 101,
    "status": "PUBLISHED",
    "stationSyncs": [
      {
        "stationId": 1,
        "syncStatus": "PENDING"
      },
      {
        "stationId": 2,
        "syncStatus": "PENDING"
      }
    ]
  }
}
```
* **Các lỗi thường gặp:**
  * `CONTROL_PACKAGE_NOT_FOUND` (Code 3028, HTTP 404): Gói cấu hình không tồn tại.
  * `INVALID_STATION_LIST` (Code 2026, HTTP 400): `stationIds` null hoặc trống.
  * `CONTROL_PACKAGE_ALREADY_PUBLISHED` (Code 3030, HTTP 400): Gói cấu hình đã bị thu hồi (`REVOKED`), không thể phát hành lại.
  * `STATION_NOT_FOUND` (Code 3005, HTTP 404): Trạm không tồn tại hoặc thuộc Route Operator khác.
  * `STATION_ALREADY_DISABLED` (Code 3009, HTTP 400): Trạm được chọn đang bị vô hiệu hóa (`DISABLED`).

---

### 3.3. UC17 - Cấp 3 nhận và áp dụng Control Package

*Lưu ý: Hai API dưới đây được gọi bởi client Cấp 3 (tại ga/trạm) nên sẽ được cấu hình bỏ qua xác thực JWT của tài khoản nhân sự (nằm trong `SecurityConstants.ENDPOINT_THIRD_PARTY`), thay vào đó hệ thống sẽ nhận diện và lọc gói cấu hình dựa trên `stationCode` gửi lên.*

#### API-AFC-025: Ga/Trạm kéo các gói cấu hình chưa đồng bộ
* **URL:** `GET /control-package/pull-pending?stationCode=BEN-THANH&currentVersion=12`
* **Mô tả:** Client Cấp 3 gửi mã trạm và version hiện tại lên để lấy về danh sách các gói cấu hình mới hơn cần áp dụng.
* **Response Body:**
```json
{
  "code": 1000,
  "message": "Success",
  "result": [
    {
      "syncId": 500,
      "packageId": 101,
      "version": 13,
      "packageType": "DEVICE_CONFIG",
      "sourceType": "LEVEL4_CREATED",
      "payload": {
        "maxOfflineSeconds": 60,
        "allowOfflineValidation": true,
        "deviceTypes": ["QR_SCANNER_SIMULATOR"]
      }
    }
  ]
}
```
* **Các lỗi thường gặp:**
  * `FIELD_REQUIRED` (Code 2000, HTTP 400): Thiếu tham số `stationCode`.
  * `INVALID_CONTROL_PACKAGE_VERSION` (Code 2029, HTTP 400): `currentVersion` không hợp lệ hoặc thiếu.
  * `CONTROL_PACKAGE_PAYLOAD_NOT_FOUND` (Code 3032, HTTP 404): Payload của cấu hình pending không tìm thấy trong MongoDB.

#### API-AFC-026: ACK xác nhận trạng thái áp dụng cấu hình
* **URL:** `POST /control-package/ack-apply/{syncId}`
* **Mô tả:** Báo cáo kết quả cài đặt cấu hình tại Trạm (thành công -> `APPLIED` hoặc thất bại -> `FAILED`).
* **Request Body:**
```json
{
  "syncStatus": "APPLIED",
  "errorMessage": null
}
// Hoặc khi lỗi:
// {
//   "syncStatus": "FAILED",
//   "errorMessage": "Port 8080 binding error during device config reload."
// }
```
* **Response Body:**
```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "syncId": 500,
    "syncStatus": "APPLIED"
  }
}
```
* **Các lỗi thường gặp:**
  * `INVALID_CONTROL_SYNC_ID` (Code 2027, HTTP 400): `syncId` null.
  * `INVALID_CONTROL_SYNC_STATUS` (Code 2028, HTTP 400): `syncStatus` truyền lên khác `APPLIED` hoặc `FAILED`.
  * `CONTROL_PACKAGE_SYNC_NOT_FOUND` (Code 3031, HTTP 404): Không tìm thấy bản ghi đồng bộ `syncId`.

#### API-AFC-026A: Danh sách trạng thái đồng bộ tại các trạm
* **URL:** `GET /control-package/search-syncs?packageType=&version=&stationId=&status=&page=0&size=20`
* **Quyền hạn:** `CONTROL_PACKAGE_READ`
* **Mô tả:** Lấy danh sách phân trang trạng thái áp dụng cấu hình của các trạm Cấp 3 đối với các gói cấu hình.
* **Response Body:**
```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "items": [
      {
        "syncId": 500,
        "stationId": 1,
        "stationCode": "ST-BT",
        "stationName": "Bến Thành",
        "packageId": 101,
        "packageType": "DEVICE_CONFIG",
        "version": 13,
        "syncStatus": "FAILED",
        "retryCount": 2,
        "lastAttemptAt": "2026-06-18T16:10:00",
        "appliedAt": null,
        "updatedAt": "2026-06-18T16:10:00",
        "errorMessage": "Không đọc được payload_ref"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```
* **Các lỗi thường gặp:**
  * `INVALID_PAGE_REQUEST` (Code 2001, HTTP 400): Phân trang không hợp lệ.

#### API-AFC-026B: Chi tiết trạng thái đồng bộ tại trạm
* **URL:** `GET /control-package/get-sync-detail?syncId={syncId}`
* **Quyền hạn:** `CONTROL_PACKAGE_READ`
* **Mô tả:** Lấy thông tin chi tiết về trạng thái áp dụng của một gói cấu hình tại một trạm cụ thể, hữu ích để xem chi tiết log lỗi khi áp dụng thất bại.
* **Response Body:**
```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "syncId": 500,
    "stationId": 1,
    "stationCode": "ST-BT",
    "stationName": "Bến Thành",
    "routeId": 1,
    "routeName": "Metro Line 1",
    "packageId": 101,
    "version": 13,
    "packageType": "DEVICE_CONFIG",
    "sourceType": "LEVEL4_CREATED",
    "packageStatus": "PUBLISHED",
    "syncStatus": "FAILED",
    "retryCount": 2,
    "lastAttemptAt": "2026-06-18T16:10:00",
    "appliedAt": null,
    "errorMessage": "Không đọc được payload_ref",
    "createdAt": "2026-06-18T15:40:00",
    "updatedAt": "2026-06-18T16:10:00"
  }
}
```
* **Các lỗi thường gặp:**
  * `INVALID_CONTROL_SYNC_ID` (Code 2027, HTTP 400): `syncId` null.
  * `CONTROL_PACKAGE_SYNC_NOT_FOUND` (Code 3031, HTTP 404): Không tìm thấy bản ghi đồng bộ tương ứng.

---

## 4. Kế hoạch kiểm thử & Xác thực
1. **Automated Unit Tests (JUnit 5 + Mockito):** Viết unit test cho Service layer của cả 3 use cases để bao phủ:
   * Kiểm tra validation đầu vào (loại package, các tham số phân trang).
   * Kiểm tra tự động tăng phiên bản cấu hình (`version`).
   * Kiểm tra phân quyền dữ liệu theo `operatorId`.
   * Kiểm tra xử lý trường hợp lỗi (ví dụ: phát hành gói không tồn tại hoặc gói đã được phát hành trước đó).
2. **Kiểm thử tích hợp (Integration Test):** Chạy thử nghiệm các API trên môi trường dev local bằng cách dùng Postman/cURL để đi hết chu kỳ sống của một gói cấu hình từ lúc tạo -> phát hành -> kéo về trạm -> ACK xác nhận.
