# API Contract AFC Cấp 3 Và Cấp 4

Tài liệu này mô tả API contract tạm chốt cho scope AFC Cấp 3/Cấp 4, bám theo:

- `document/Use Case Spec AFC Cấp 3 và Cấp 4.md`
- `document/Draft Schema auth_db và afc_ops_db.md`

Mục tiêu là đủ rõ để triển khai `auth-ops-service`, `afc-ops-service`, FE vận hành, mock Cấp 2 và mock Cấp 5.

## 1. Quy Ước Chung

### 1.1. Service Và Base Path

| Service | Base path | Vai trò |
| --- | --- | --- |
| `auth-ops-service` | `/auth` | Đăng nhập, refresh token, account nội bộ, đổi/reset mật khẩu |
| `afc-ops-service` | `/afc-ops` | Master data, thiết bị, transaction, control package, batch, dashboard và dữ liệu card/ticket/entitlement đồng bộ từ Cấp 5 |

### 1.1.1. Quy Ước Đặt Tên API

API trong tài liệu này dùng kiểu **action-based endpoint** để dễ đọc và dễ ghép FE/mock, không cố ép RESTful resource path.

Ví dụ:

- Dùng `/account/create-account` thay vì `POST /auth/accounts`.
- Dùng `/afc-ops/create-route` thay vì `POST /afc-ops/routes`.
- Dùng `/afc-ops/submit-tap-event` thay vì `POST /afc-ops/device-api/tap-events`.

### 1.2. Response Chuẩn

```json
{
  "code": 1000,
  "message": "Success",
  "result": {}
}
```

Response lỗi:

```json
{
  "code": 4001,
  "message": "Invalid request",
  "result": null
}
```

### 1.3. Header Chung

| Header | Bắt buộc | Dùng cho | Ghi chú |
| --- | --- | --- | --- |
| `Authorization: Bearer <accessToken>` hoặc access token cookie | Có với API nội bộ | FE, staff API | Giữ theo cơ chế hiện tại của `auth-ops-service`; token không trả trong response body |
| `X-Device-Code` | Có với API thiết bị | Cấp 2/mock device | Mã thiết bị đã đăng ký |
| `X-Device-Secret` | Có với API thiết bị | Cấp 2/mock device | MVP có thể dùng shared secret |
| `X-Level5-Client-Id` | Có với API Cấp 5 inbound | Cấp 5/mock | Xác thực nguồn Cấp 5 |
| `X-Level5-Signature` | Có với API Cấp 5 inbound | Cấp 5/mock | MVP có thể mock signature |
| `X-External-User-Id` | Có với API Passenger App trong MVP | App/mock App | Định danh hành khách từ hệ ngoài/C5; không phải account trong `auth_db` |
| `X-Request-Id` | Khuyến nghị | Tất cả | Trace request |

### 1.4. Pagination

Request query:

| Query | Default | Ghi chú |
| --- | --- | --- |
| `page` | `0` | Zero-based |
| `size` | `20` | Max đề xuất `100` |
| `sort` | tùy API | Ví dụ `createdAt,desc` |

Response page:

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

### 1.5. Enum Chính

| Nhóm | Giá trị |
| --- | --- |
| Role | `OPERATOR_ADMIN`, `OPERATOR_MANAGER`, `STATION_OPERATOR` |
| Device type | `QR_SCANNER_SIMULATOR` |
| Device direction | `ENTRY`, `EXIT`, `BOTH` |
| Device status | `ACTIVE`, `OFFLINE`, `MAINTENANCE`, `DISABLED` |
| Master data status | `ACTIVE`, `DISABLED` |
| Transport type | `METRO`, `BUS` |
| Media type | `VIRTUAL_QR` |
| Card type | `VIRTUAL_QR`, `PHYSICAL` |
| Card status | `ACTIVE`, `INACTIVE`, `CANCELLED`, `BLACKLISTED` |
| Ticket type | `METRO_SINGLE_RIDE` |
| Ticket usage status | `UNUSED`, `IN_USE`, `USED`, `EXPIRED`, `CANCELLED` |
| Tap type | `TAP_IN`, `TAP_OUT`, `CHECK` |
| Transaction decision | `OPEN_GATE`, `DENY`, `ACCEPTED_FOR_FORWARDING` |
| Transaction reason | `VALID`, `DEVICE_DISABLED`, `INVALID_DIRECTION`, `MEDIA_BLACKLISTED`, `CARD_INACTIVE`, `CARD_CANCELLED`, `UNKNOWN_MEDIA`, `QR_EXPIRED`, `QR_INVALID_SIGNATURE`, `QR_REPLAYED`, `ENTITLEMENT_EXPIRED`, `ENTITLEMENT_INACTIVE`, `TICKET_INVALID`, `TICKET_EXPIRED`, `TICKET_ALREADY_USED`, `TICKET_SCOPE_INVALID`, `ACTIVE_PRODUCT_CONFLICT` |
| Transaction sync status | `PENDING`, `SYNCED`, `FAILED` |
| Ticket processing status | `PENDING`, `CONFIRMED`, `FAILED` |
| Package type | `DEVICE_CONFIG`, `MEDIA_ACCESS_RULES` |
| Package source type | `LEVEL4_CREATED`, `LEVEL5_SYNCED` |
| Package status | `CREATED`, `PUBLISHED`, `REVOKED` |
| Station sync status | `PENDING`, `APPLIED`, `FAILED` |
| Batch status | `CREATED`, `SUBMITTED`, `ACCEPTED`, `REJECTED`, `FAILED` |
| Entitlement status | `ACTIVE`, `INACTIVE`, `CANCELLED` |
| Pass period | `MONTH` |
| Pass scope | `SINGLE_ROUTE`, `INTERLINE` |

Ghi chú: `TIME_SYNC` không đưa vào package type chính của MVP. Đồng bộ thời gian xem là yêu cầu hạ tầng/NTP hoặc validate timestamp ở backend.

## 2. Auth-Ops Service APIs

### UC01 - Đăng Nhập

#### API-AUTH-001 - Login

`POST /auth/login`

Permission: Public.

Request:

```json
{
  "username": "manager01",
  "password": "Password@123"
}
```

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "id": "uuid",
    "username": "manager01",
    "operatorCode": "HCMC-METRO",
    "roles": ["OPERATOR_MANAGER"],
    "permissions": ["MASTER_DATA_READ", "DASHBOARD_READ"],
    "passwordStatus": "NORMAL"
  }
}
```

Response header:

```http
Set-Cookie: access_token=jwt-access-token; HttpOnly; Path=/...
Set-Cookie: refresh_token=jwt-refresh-token; HttpOnly; Path=/auth/refresh-token
Set-Cookie: refresh_token=jwt-refresh-token; HttpOnly; Path=/auth/logout
```

Luồng:

1. Kiểm tra username/password.
2. Kiểm tra `is_active = true`.
3. Load operator scope, roles/permissions.
4. Sinh access token có claim `operatorCode` và các permission name, sinh refresh token, set vào cookie theo cấu hình hiện tại.
5. Trả `passwordStatus` để FE biết mật khẩu đang `NORMAL`, cần đổi sau đăng nhập (`NEED_TO_CHANGE`) hoặc cần admin reset (`NEED_TO_RESET`).
6. Ghi login event/audit.

Lỗi chính:

| Code | Message | HTTP |
| --- | --- | --- |
| `AUTH_INVALID_CREDENTIALS` | Username or password is incorrect | 401 |
| `ACCOUNT_DISABLED` | Account is disabled | 403 |
| `ACCOUNT_ROLE_EMPTY` | Account has no assigned role | 403 |

#### API-AUTH-002 - Refresh Token

`POST /auth/refresh-token`

Permission: Public, cần refresh token cookie hợp lệ.

Request body: không có.

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": null
}
```

Response header:

```http
Set-Cookie: access_token=new-jwt-access-token; HttpOnly; Path=/...
Set-Cookie: refresh_token=new-jwt-refresh-token; HttpOnly; Path=/auth/refresh-token
Set-Cookie: refresh_token=new-jwt-refresh-token; HttpOnly; Path=/auth/logout
```

#### API-AUTH-003 - Logout

`POST /auth/logout`

Permission: Authenticated.

Request body: không có. System lấy access token/refresh token từ cookie để invalidate và clear cookie.

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": null
}
```

Response header:

```http
Set-Cookie: access_token=; HttpOnly; Max-Age=0; Path=/...
Set-Cookie: refresh_token=; HttpOnly; Max-Age=0; Path=/auth/refresh-token
Set-Cookie: refresh_token=; HttpOnly; Max-Age=0; Path=/auth/logout
```

### UC02 - Quản Lý Tài Khoản Nhân Sự

#### API-AUTH-004 - List Accounts

`GET /account/list-accounts?keyword=&role=&isActive=&passwordStatus=&page=0&size=20`

Permission: `ACCOUNT_READ`.

Scope dữ liệu: backend lấy `operatorCode` từ account `OPERATOR_ADMIN` đang đăng nhập và chỉ trả account thuộc operator đó.

Query params:

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `keyword` | string | No | Trim trước khi xử lý; nếu rỗng thì bỏ qua; tối đa 50 ký tự; tìm kiếm không phân biệt hoa thường theo `username` |
| `role` | string | No | Trim trước khi xử lý; nếu rỗng thì bỏ qua; nếu truyền vào phải là role đang tồn tại |
| `isActive` | boolean | No | Nếu không truyền thì bỏ qua điều kiện trạng thái |
| `passwordStatus` | string | No | Trim trước khi xử lý; nếu rỗng thì bỏ qua; nếu truyền vào phải thuộc `NORMAL`, `NEED_TO_CHANGE`, `NEED_TO_RESET` |
| `page` | integer | No | Mặc định `0`; phải `>= 0` |
| `size` | integer | No | Mặc định `20`; phải trong khoảng `1..100` |

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "items": [
      {
        "id": "uuid",
        "username": "station01",
        "operatorCode": "HCMC-METRO",
        "roles": ["STATION_OPERATOR"],
        "isActive": true,
        "passwordStatus": "NEED_TO_CHANGE",
        "createdAt": "2026-06-04T10:00:00+07:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

Lỗi chính:

| Code | Message | HTTP |
| --- | --- | --- |
| `INVALID_PAGE_REQUEST` | Page must be >= 0 and size must be between 1 and 100 | 400 |
| `INVALID_SEARCH_KEYWORD` | Search keyword is too long | 400 |
| `INVALID_ROLE_SELECTION` | Invalid operator role selection | 400 |
| `INVALID_PASSWORD_STATUS` | Invalid password status | 400 |
| `OPERATOR_SCOPE_REQUIRED` | Operator scope is required | 403 |

#### API-AUTH-005 - Create Account

`POST /account/create-account`

Permission: `ACCOUNT_WRITE`.

Content-Type: `application/json`

Request:

```json
{
  "username": "station01",
  "roleNames": ["STATION_OPERATOR"]
}
```

Validation:

| Field | Rule |
| --- | --- |
| `username` | Required; phải unique |
| `roleNames` | Required; chỉ được dùng `OPERATOR_MANAGER` hoặc `STATION_OPERATOR`; role phải tồn tại trong hệ thống |

Backend tự lấy `operatorCode` từ account `OPERATOR_ADMIN` đang đăng nhập và gán cho account mới. Client không truyền `operatorCode`.

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "id": "uuid",
    "username": "station01",
    "operatorCode": "HCMC-METRO",
    "roles": ["STATION_OPERATOR"],
    "isActive": true,
    "passwordStatus": "NEED_TO_CHANGE",
    "temporaryPassword": "A7xQp2Lm9"
  }
}
```

Luồng:

1. `OPERATOR_ADMIN` nhập username và role.
2. System validate username unique, role hợp lệ và account admin có operator scope.
3. System tự sinh mật khẩu tạm đạt password policy.
4. Lưu password hash.
5. Set `passwordStatus = NEED_TO_CHANGE`.
6. Trả `temporaryPassword` một lần trong response để admin bàn giao qua kênh ngoài hệ thống.
7. Ghi audit.

Lỗi chính:

| Code | Message | HTTP |
| --- | --- | --- |
| `USER_EXISTED` | Username already exists | 400 |
| `INVALID_ROLE_SELECTION` | Invalid operator role selection | 400 |

#### API-AUTH-006 - Disable Account

`POST /account/disable-account/{accountId}`

Permission: `ACCOUNT_WRITE`.

Path params:

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `accountId` | UUID | Yes | Phải đúng UUID format; nếu đúng format nhưng không tồn tại thì trả `USER_NOT_FOUND` |

Request body: không có.

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "id": "uuid",
    "username": "station01",
    "roles": ["STATION_OPERATOR"],
    "isActive": false,
    "passwordStatus": "NORMAL"
  }
}
```

Lỗi chính:

| Code | Message | HTTP |
| --- | --- | --- |
| `INVALID_ACCOUNT_ID` | Account id is invalid | 400 |
| `OPERATOR_ACCESS_DENIED` | You do not have permission to access data from another operator | 403 |
| `USER_NOT_FOUND` | User not found | 404 |
| `OPERATOR_ADMIN_STATUS_CHANGE_NOT_ALLOWED` | Operator admin account status cannot be changed | 400 |
| `ACCOUNT_ALREADY_DISABLED` | Account is already disabled | 400 |

#### API-AUTH-007 - Enable Account

`POST /account/enable-account/{accountId}`

Permission: `ACCOUNT_WRITE`.

Path params:

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `accountId` | UUID | Yes | Phải đúng UUID format; nếu đúng format nhưng không tồn tại thì trả `USER_NOT_FOUND` |

Request body: không có.

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "id": "uuid",
    "username": "station01",
    "roles": ["STATION_OPERATOR"],
    "isActive": true,
    "passwordStatus": "NORMAL"
  }
}
```

Lỗi chính:

| Code | Message | HTTP |
| --- | --- | --- |
| `INVALID_ACCOUNT_ID` | Account id is invalid | 400 |
| `OPERATOR_ACCESS_DENIED` | You do not have permission to access data from another operator | 403 |
| `USER_NOT_FOUND` | User not found | 404 |
| `OPERATOR_ADMIN_STATUS_CHANGE_NOT_ALLOWED` | Operator admin account status cannot be changed | 400 |
| `ACCOUNT_ALREADY_ENABLED` | Account is already enabled | 400 |

#### API-AUTH-008 - Preview Import Accounts

`POST /account/preview-import-accounts`

Permission: `ACCOUNT_WRITE`.

Content-Type: `multipart/form-data`

Form field: `file`.

Template: `auth-ops-service/src/main/resources/templates/account-import-template.xlsx`.

File columns:

| Column | Required | Validation |
| --- | --- | --- |
| `username` | Yes | Required; trim; unique trong file; chưa tồn tại trong DB |
| `roleName` | Yes | Chỉ được dùng `OPERATOR_MANAGER` hoặc `STATION_OPERATOR`; role phải tồn tại trong hệ thống |

Backend tự lấy `operatorCode` từ account `OPERATOR_ADMIN` đang đăng nhập và gán cho toàn bộ account import. File không có cột `operatorCode`.

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "totalRows": 10,
    "validRows": 10,
    "invalidRows": 0,
    "items": [
      {
        "row": 2,
        "username": "station01",
        "operatorCode": "HCMC-METRO",
        "roleName": "STATION_OPERATOR",
        "valid": true,
        "errors": []
      }
    ],
    "errors": []
  }
}
```

Nếu có dòng lỗi thì chỉ preview, chưa import:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "totalRows": 10,
    "validRows": 9,
    "invalidRows": 1,
    "items": [
      {
        "row": 3,
        "username": "station01",
        "roleName": "INVALID_ROLE",
        "valid": false,
        "errors": [
          {
            "field": "roleName",
            "message": "Invalid operator role selection"
          }
        ]
      }
    ],
    "errors": [
      {
        "row": 3,
        "field": "roleName",
        "message": "Invalid operator role selection"
      }
    ]
  }
}
```

Lỗi chính:

| Code | Message | HTTP |
| --- | --- | --- |
| `IMPORT_FILE_INVALID` | Import file is invalid | 400 |
| `INVALID_ROLE_SELECTION` | Invalid operator role selection | 400 |
| `USER_EXISTED` | Username already exists | 400 |

#### API-AUTH-009 - Confirm Import Accounts

`POST /account/confirm-import-accounts`

Permission: `ACCOUNT_WRITE`.

Content-Type: `multipart/form-data`

Form field: `file`.

Quy tắc:

- API confirm không dùng `previewId` và không lưu preview trên server.
- FE gọi preview trước và chỉ mở nút import khi preview không có lỗi.
- Khi confirm, FE upload lại cùng file đã preview.
- Backend validate lại toàn bộ file giống API preview trước khi ghi DB.
- Nếu có bất kỳ dòng lỗi nào ở thời điểm confirm thì không import dòng nào.
- Nếu file hợp lệ, system tạo account trong một transaction.
- Mật khẩu tạm do system tự sinh cho từng account, lưu password hash và trả plaintext một lần trong response.
- Account được tạo với `passwordStatus = NEED_TO_CHANGE`.

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "imported": 2,
    "items": [
      {
        "row": 2,
        "id": "uuid",
        "username": "station01",
        "roles": ["STATION_OPERATOR"],
        "isActive": true,
        "passwordStatus": "NEED_TO_CHANGE",
        "temporaryPassword": "A7xQp2Lm9"
      },
      {
        "row": 3,
        "id": "uuid",
        "username": "manager01",
        "roles": ["OPERATOR_MANAGER"],
        "isActive": true,
        "passwordStatus": "NEED_TO_CHANGE",
        "temporaryPassword": "K4mQp8Tx2"
      }
    ]
  }
}
```

Lỗi chính:

| Code | Message | HTTP |
| --- | --- | --- |
| `IMPORT_FILE_INVALID` | Import file is invalid | 400 |
| `IMPORT_FILE_HAS_ERRORS` | Import file contains invalid rows | 400 |
| `INVALID_ROLE_SELECTION` | Invalid operator role selection | 400 |
| `USER_EXISTED` | Username already exists | 400 |

### UC03 - Đổi Mật Khẩu

#### API-AUTH-010 - Change Password

`POST /account/change-password`

Permission: Authenticated.

Ghi chú: API đổi mật khẩu thuộc `auth-ops-service` nhưng không đặt dưới `/auth`. Backend lấy account hiện tại từ security context (`SecurityUtils.getCurrentAccountId()`), không nhận `accountId` từ request.

Request:

```json
{
  "currentPassword": "Temp@123456",
  "newPassword": "NewPassword@123",
  "confirmPassword": "NewPassword@123"
}
```

Validation:

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `currentPassword` | string | Yes | Không được rỗng |
| `newPassword` | string | Yes | Không được rỗng; tối thiểu 9 ký tự; có cả chữ và số |
| `confirmPassword` | string | Yes | Không được rỗng; phải trùng `newPassword` |

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "passwordStatus": "NORMAL"
  }
}
```

Luồng:

1. JWT filter xác thực access token/cookie và set security context.
2. Nếu account đang có `passwordStatus = NEED_TO_CHANGE`, endpoint `/account/change-password` vẫn được phép gọi để user tự đổi mật khẩu.
3. Service lấy `accountId` hiện tại từ `SecurityUtils.getCurrentAccountId()`.
4. Validate request body.
5. Load account, kiểm tra account còn active.
6. So khớp `currentPassword` với password hash hiện tại.
7. Hash `newPassword`, cập nhật password và set `passwordStatus = NORMAL`.
8. Trả trạng thái mật khẩu mới. API không phát hành lại access token/refresh token trong response.

Lỗi chính:

| Code | Message | HTTP |
| --- | --- | --- |
| `UNAUTHENTICATED` | Unauthenticated access | 401 |
| `FIELD_REQUIRED` | `{fieldName} is required` | 400 |
| `INVALID_PASSWORD` | Password must be at least 9 characters and contain both letters and numbers | 400 |
| `PASSWORD_CONFIRMATION_MISMATCH` | New password and confirm password do not match | 400 |
| `CURRENT_PASSWORD_INCORRECT` | Current password is incorrect | 400 |
| `ACCOUNT_DISABLED` | Your account is currently disabled or inactive. | 403 |
| `USER_NOT_FOUND` | User not found | 404 |

Response theo case để FE ghép:

**1. Đổi mật khẩu thành công**

HTTP `200`

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "passwordStatus": "NORMAL"
  }
}
```

FE action đề xuất: đóng form đổi mật khẩu, clear dữ liệu nhập, cho phép user tiếp tục dùng hệ thống. Nếu trước đó user có `passwordStatus = NEED_TO_CHANGE`, FE bỏ trạng thái bắt buộc đổi mật khẩu.

**2. Chưa đăng nhập, thiếu access token hoặc token không set được security context**

HTTP `401`

```json
{
  "code": 4002,
  "message": "Unauthenticated access",
  "result": null
}
```

FE action đề xuất: điều hướng về màn hình login hoặc gọi refresh token nếu flow hiện tại cho phép.

**3. Thiếu `currentPassword`**

HTTP `400`

```json
{
  "code": 2000,
  "message": "currentPassword is required",
  "result": null
}
```

FE action đề xuất: hiển thị lỗi tại field `currentPassword`.

**4. Thiếu `newPassword`**

HTTP `400`

```json
{
  "code": 2000,
  "message": "newPassword is required",
  "result": null
}
```

FE action đề xuất: hiển thị lỗi tại field `newPassword`.

**5. Thiếu `confirmPassword`**

HTTP `400`

```json
{
  "code": 2000,
  "message": "confirmPassword is required",
  "result": null
}
```

FE action đề xuất: hiển thị lỗi tại field `confirmPassword`.

**6. `newPassword` không đạt rule mật khẩu**

HTTP `400`

```json
{
  "code": 2001,
  "message": "Password must be at least 9 characters and contain both letters and numbers",
  "result": null
}
```

FE action đề xuất: hiển thị lỗi tại field `newPassword`. Rule hiện tại: tối thiểu 9 ký tự, có ít nhất 1 chữ và 1 số.

**7. `confirmPassword` không trùng `newPassword`**

HTTP `400`

```json
{
  "code": 2006,
  "message": "New password and confirm password do not match",
  "result": null
}
```

FE action đề xuất: hiển thị lỗi tại field `confirmPassword`.

**8. `currentPassword` không đúng**

HTTP `400`

```json
{
  "code": 2007,
  "message": "Current password is incorrect",
  "result": null
}
```

FE action đề xuất: hiển thị lỗi tại field `currentPassword`; không clear `newPassword`/`confirmPassword` nếu FE muốn giữ trải nghiệm nhập liệu.

**9. Account đã bị khóa hoặc inactive**

HTTP `403`

```json
{
  "code": 4006,
  "message": "Your account is currently disabled or inactive.",
  "result": null
}
```

FE action đề xuất: logout local state và báo user liên hệ quản trị viên.

**10. Account trong token không còn tồn tại**

HTTP `404`

```json
{
  "code": 3007,
  "message": "User not found",
  "result": null
}
```

FE action đề xuất: logout local state và yêu cầu đăng nhập lại.

**11. Thiếu hoặc sai CSRF token**

HTTP `403`

```json
{
  "code": 4009,
  "message": "Missing or invalid CSRF token",
  "result": null
}
```

FE action đề xuất: lấy lại CSRF token theo flow hiện tại rồi submit lại request.

### UC04 - Quên Mật Khẩu

#### API-AUTH-011 - Request Password Reset

`POST /auth/forgot-password`

Permission: Public.

Ghi chú: API cho nhân viên không đăng nhập được gửi yêu cầu reset mật khẩu. Không gửi email, không OTP. Backend đánh dấu account sang `passwordStatus = NEED_TO_RESET`; admin dùng API-AUTH-012 để cấp mật khẩu tạm.

Request:

```json
{
  "username": "station01"
}
```

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "username": "station01",
    "passwordStatus": "NEED_TO_RESET"
  }
}
```

Validation:

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `username` | string | Yes | Không được rỗng; phải là username account đang tồn tại |

Luồng:

1. Nhân viên nhập username và gửi yêu cầu quên mật khẩu.
2. Backend tìm account theo `username`.
3. Nếu account tồn tại và active, set `passwordStatus = NEED_TO_RESET`.
4. Account có `passwordStatus = NEED_TO_RESET` sẽ bị chặn login cho đến khi admin reset mật khẩu.

Lỗi chính:

| Code | Message | HTTP |
| --- | --- | --- |
| `FIELD_REQUIRED` | `username is required` | 400 |
| `USER_NOT_FOUND` | User not found | 404 |
| `ACCOUNT_DISABLED` | Your account is currently disabled or inactive. | 403 |

Response theo case để FE ghép:

**1. Gửi yêu cầu reset thành công**

HTTP `200`

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "username": "station01",
    "passwordStatus": "NEED_TO_RESET"
  }
}
```

FE action đề xuất: báo user liên hệ admin để nhận mật khẩu tạm.

**2. Thiếu username**

HTTP `400`

```json
{
  "code": 2000,
  "message": "username is required",
  "result": null
}
```

FE action đề xuất: hiển thị lỗi tại field `username`.

**3. Username không tồn tại**

HTTP `404`

```json
{
  "code": 3007,
  "message": "User not found",
  "result": null
}
```

FE action đề xuất: báo username không tồn tại hoặc yêu cầu kiểm tra lại username.

**4. Account bị khóa hoặc inactive**

HTTP `403`

```json
{
  "code": 4006,
  "message": "Your account is currently disabled or inactive.",
  "result": null
}
```

FE action đề xuất: báo user liên hệ quản trị viên.

#### API-AUTH-012 - Admin Reset Password

`POST /account/reset-password`

Permission: `ACCOUNT_WRITE`.

Ghi chú: API cho admin reset mật khẩu account. Admin truyền `username`. Backend tự sinh mật khẩu tạm, lưu password hash, set `passwordStatus = NEED_TO_CHANGE` và trả plaintext một lần trong response.

Request:

```json
{
  "username": "station01"
}
```

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "username": "station01",
    "passwordStatus": "NEED_TO_CHANGE",
    "temporaryPassword": "A7xQp2Lm9"
  }
}
```

Validation:

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `username` | string | Yes | Không được rỗng; phải là username account đang tồn tại |

Quy tắc:

- Chỉ truyền `username`.
- Account phải đang có `passwordStatus = NEED_TO_RESET`, tức là nhân viên đã gửi yêu cầu qua `POST /auth/forgot-password`.
- Không truyền `temporaryPassword`; backend tự sinh mật khẩu tạm.
- Mật khẩu tạm chỉ trả một lần trong response.
- Admin chuyển mật khẩu tạm cho nhân viên qua quy trình ngoài hệ thống.
- User bắt buộc đổi mật khẩu sau khi đăng nhập bằng mật khẩu tạm.

Lỗi chính:

| Code | Message | HTTP |
| --- | --- | --- |
| `UNAUTHENTICATED` | Unauthenticated access | 401 |
| `ACCESS_DENIED` | You do not have permission to access this resource | 403 |
| `INVALID_CSRF_TOKEN` | Missing or invalid CSRF token | 403 |
| `FIELD_REQUIRED` | `username is required` | 400 |
| `PASSWORD_RESET_NOT_REQUESTED` | Password reset has not been requested for this account | 400 |
| `OPERATOR_ACCESS_DENIED` | You do not have permission to access data from another operator | 403 |
| `USER_NOT_FOUND` | User not found | 404 |

Response theo case để FE ghép:

**1. Reset thành công**

HTTP `200`

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "username": "station01",
    "passwordStatus": "NEED_TO_CHANGE",
    "temporaryPassword": "A7xQp2Lm9"
  }
}
```

FE action đề xuất: hiển thị/copy `temporaryPassword` cho admin; cảnh báo mật khẩu chỉ hiển thị một lần.

**2. Chưa đăng nhập**

HTTP `401`

```json
{
  "code": 4002,
  "message": "Unauthenticated access",
  "result": null
}
```

FE action đề xuất: điều hướng về login hoặc refresh token theo flow hiện tại.

**3. Đã đăng nhập nhưng thiếu quyền `ACCOUNT_WRITE`**

HTTP `403`

```json
{
  "code": 4007,
  "message": "You do not have permission to access this resource",
  "result": null
}
```

FE action đề xuất: báo không đủ quyền và không retry.

**4. Thiếu/sai CSRF token khi đã authenticated và đủ quyền**

HTTP `403`

```json
{
  "code": 4009,
  "message": "Missing or invalid CSRF token",
  "result": null
}
```

FE action đề xuất: lấy lại CSRF token theo flow hiện tại rồi submit lại.

**5. Không truyền `username`**

HTTP `400`

```json
{
  "code": 2000,
  "message": "username is required",
  "result": null
}
```

FE action đề xuất: bắt admin chọn account trước khi gọi API.

**6. Account không tồn tại**

HTTP `404`

```json
{
  "code": 3007,
  "message": "User not found",
  "result": null
}
```

FE action đề xuất: reload danh sách account hoặc yêu cầu admin kiểm tra lại username.

**7. Account chưa gửi yêu cầu quên mật khẩu**

HTTP `400`

```json
{
  "code": 2008,
  "message": "Password reset has not been requested for this account",
  "result": null
}
```

FE action đề xuất: báo admin rằng account này chưa ở trạng thái cần reset; yêu cầu nhân viên gửi forgot-password trước hoặc kiểm tra lại account.

## 3. AFC-Ops Master Data APIs

### UC05 - Quản Lý Tuyến

#### API-AFC-001 - List Routes

`GET /afc-ops/list-routes?keyword=&transportType=&status=&page=0&size=20`

Permission: `MASTER_DATA_READ`.

Scope dữ liệu: backend lấy `operatorCode` từ JWT của account đang đăng nhập, tìm operator tương ứng và chỉ trả route thuộc operator đó. Client không truyền `operatorId`.

Query params:

| Field | Type | Required | Rule |
| --- | --- | --- | --- |
| `keyword` | string | No | Trim trước khi xử lý; tìm theo `routeCode` hoặc `routeName`; tối đa 50 ký tự |
| `transportType` | string | No | Nếu truyền phải thuộc `METRO`, `BUS` |
| `status` | string | No | Nếu truyền phải thuộc `ACTIVE`, `DISABLED` |
| `page` | number | No | Mặc định `0`, phải `>= 0` |
| `size` | number | No | Mặc định `20`, từ `1` đến `100` |

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "items": [
      {
        "id": 1,
        "operatorId": 1,
        "routeCode": "METRO-001",
        "routeName": "Metro Line 1",
        "transportType": "METRO",
        "status": "ACTIVE"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

#### API-AFC-002 - Create Route

`POST /afc-ops/create-route`

Permission: `MASTER_DATA_WRITE`.

Request:

```json
{
  "routeName": "Metro Line 1",
  "transportType": "METRO"
}
```

Create route luôn tạo tuyến trong operator của account đang đăng nhập theo claim `operatorCode` trong JWT, backend tự sinh `routeCode` theo `transportType` trong phạm vi operator và mặc định `status = ACTIVE`. Client không truyền `routeCode`, `operatorId` hoặc `status`.

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "id": 1,
    "operatorId": 1,
    "routeCode": "METRO-001",
    "routeName": "Metro Line 1",
    "transportType": "METRO",
    "status": "ACTIVE",
    "createdAt": "2026-06-04T10:00:00+07:00",
    "updatedAt": "2026-06-04T10:00:00+07:00"
  }
}
```

#### API-AFC-003 - Update Route

`POST /afc-ops/update-route/{routeId}`

Permission: `MASTER_DATA_WRITE`.

Request:

```json
{
  "routeName": "Metro Line 1 Updated",
  "transportType": "METRO"
}
```

Update route chỉ áp dụng trong operator của account đang đăng nhập theo claim `operatorCode` trong JWT và không đổi `routeCode`/`status`. Client không truyền `routeCode`, `operatorId` hoặc `status`; dùng API enable/disable riêng để đổi trạng thái.

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "id": 1,
    "operatorId": 1,
    "routeCode": "METRO-001",
    "routeName": "Metro Line 1 Updated",
    "transportType": "METRO",
    "status": "ACTIVE",
    "createdAt": "2026-06-04T10:00:00+07:00",
    "updatedAt": "2026-06-04T10:30:00+07:00"
  }
}
```

#### API-AFC-003A - Enable Route

`POST /afc-ops/enable-route/{routeId}`

Permission: `MASTER_DATA_WRITE`.

Response giống `RouteResponse`; `status` sau thao tác là `ACTIVE`.

#### API-AFC-003B - Disable Route

`POST /afc-ops/disable-route/{routeId}`

Permission: `MASTER_DATA_WRITE`.

Response giống `RouteResponse`; `status` sau thao tác là `DISABLED`.

#### UC05 Error Matrix

| Code key | Code | Áp dụng | Điều kiện | Message | HTTP |
| --- | ---: | --- | --- | --- | ---: |
| `UNAUTHENTICATED` | 4002 | Tất cả API UC05 | Thiếu access token, token sai chữ ký, hết hạn hoặc không đọc được | Unauthenticated access | 401 |
| `ACCOUNT_DISABLED` | 4006 | Tất cả API UC05 | Account đã bị disable trong Redis token-status | Your account is currently disabled or inactive. | 403 |
| `ACCESS_DENIED` | 4007 | Tất cả API UC05 | Không có `MASTER_DATA_READ` khi list hoặc không có `MASTER_DATA_WRITE` khi create/update/enable/disable | You do not have permission to access this resource | 403 |
| `INVALID_CSRF_TOKEN` | 4009 | Create, update, enable, disable | Thiếu hoặc sai CSRF token trên API thay đổi dữ liệu | Missing or invalid CSRF token | 403 |
| `FIELD_REQUIRED` | 2000 | Create, update | Thiếu/rỗng `routeName` hoặc `transportType` | `{fieldName} is required` | 400 |
| `FIELD_REQUIRED` | 2000 | Create, update | Thiếu request body hoặc JSON body sai cấu trúc/không đọc được | Request body is invalid | 400 |
| `INVALID_PAGE_REQUEST` | 2001 | List | `page < 0`, `size < 1` hoặc `size > 100` | Page must be >= 0 and size must be between 1 and 100 | 400 |
| `INVALID_SEARCH_KEYWORD` | 2002 | List | `keyword` sau trim dài hơn 50 ký tự | Search keyword is too long | 400 |
| `INVALID_TRANSPORT_TYPE` | 2005 | List, create, update | `transportType` không thuộc `METRO`, `BUS` | Invalid transport type | 400 |
| `INVALID_MASTER_DATA_STATUS` | 2006 | List | `status` không thuộc `ACTIVE`, `DISABLED` | Invalid master data status | 400 |
| `INVALID_ROUTE_NAME_LENGTH` | 2007 | Create, update | `routeName` dài hơn 255 ký tự | Route name must not exceed 255 characters | 400 |
| `INVALID_ROUTE_ID` | 2004 | Update, enable, disable | `routeId` không phải số nguyên dương | Route id is invalid | 400 |
| `OPERATOR_SCOPE_REQUIRED` | 4012 | Tất cả API UC05 | JWT/account không có `operatorCode` hợp lệ | Operator scope is required | 403 |
| `OPERATOR_ACCESS_DENIED` | 4013 | Update, enable, disable | Route tồn tại nhưng thuộc operator khác | You do not have permission to access data from another operator | 403 |
| `ROUTE_ALREADY_ENABLED` | 3001 | Enable | Route đang `ACTIVE` sẵn | Route is already active | 400 |
| `ROUTE_ALREADY_DISABLED` | 3002 | Disable | Route đang `DISABLED` sẵn | Route is already disabled | 400 |
| `OPERATOR_NOT_FOUND` | 3003 | Tất cả API UC05 | `operatorCode` của account không tồn tại trong bảng `operators` | Operator not found | 404 |
| `ROUTE_NOT_FOUND` | 3004 | Update, enable, disable | `routeId` không tồn tại | Route not found | 404 |
| `UNCATEGORIZED_EXCEPTION` | 4000 | Tất cả API UC05 | Lỗi hệ thống không được phân loại, lỗi DB hoặc lỗi phát sinh ngoài dự kiến | Uncategorized error | 500 |

Error response format:

```json
{
  "code": 4013,
  "message": "You do not have permission to access data from another operator",
  "result": null
}
```

FE và API test phải xác định case chính bằng numeric `code` và HTTP status. `message` dùng để hiển thị/log, không dùng làm khóa xử lý.

Ghi chú parsing query/path:

1. `page`, `size` không phải số hiện được Spring xử lý qua type-mismatch và trả `FIELD_REQUIRED` (`400`); message hiện tại có thể là placeholder chung.
2. `routeId` không phải số được map sang `INVALID_ROUTE_ID` (`400`).
3. Thiếu hẳn segment `{routeId}` không match endpoint và do framework xử lý như URL không tồn tại, không đi vào UC05 service.

#### UC05 Expected State Cases

| API | Trạng thái hiện tại | Kết quả |
| --- | --- | --- |
| Enable route | Route đang `ACTIVE` và thuộc đúng operator | Lỗi `ROUTE_ALREADY_ENABLED` |
| Disable route | Route đang `DISABLED` và thuộc đúng operator | Lỗi `ROUTE_ALREADY_DISABLED` |
| Update route | Route đang `ACTIVE` hoặc `DISABLED` và thuộc đúng operator | Thành công `200`, chỉ cập nhật `routeName`, `transportType`; giữ nguyên `routeCode`, `status`, `operator` |
| List routes | Operator chưa có route hoặc filter không có kết quả | Thành công `200`, `items = []`, `totalElements = 0` |
| List routes | `transportType`, `status` truyền chữ thường hoặc có khoảng trắng | Trim, uppercase và query bình thường |
| Create/update route | `routeName` có khoảng trắng đầu/cuối | Trim trước khi lưu |
| Create/update route | `transportType` là `metro`, `bus` hoặc có khoảng trắng | Trim, uppercase thành `METRO`, `BUS` |
| Create route | Operator chưa có code cùng `transportType` | Sinh code đầu tiên, ví dụ `METRO-001` hoặc `BUS-001` |
| Create route | Operator đã có code cùng `transportType` | Sinh sequence tiếp theo trong đúng operator |

Business notes:

1. `routeCode` do backend tự sinh và unique trong operator của account đang đăng nhập, ví dụ `METRO-001`, `BUS-001`.
2. Request create/update dùng Bean Validation ở DTO trước khi vào service: required field, route name length và transport type.
3. `routeName`, `transportType` được trim/normalize trước khi lưu; filter `status` được trim/normalize trước khi query.
4. Create route mặc định `status = ACTIVE`; enable/disable route là API riêng.
5. `status = DISABLED` dùng để ngừng route, không xóa cứng.
6. Nếu thao tác trực tiếp bằng `routeId` mà route tồn tại nhưng thuộc operator khác, backend trả `OPERATOR_ACCESS_DENIED`.
7. API chưa trả `createdByAccountId`; backend vẫn lưu để audit/truy vết.
8. FE phải xử lý `UNAUTHENTICATED` bằng luồng đăng nhập lại; `ACCESS_DENIED`, `OPERATOR_SCOPE_REQUIRED`, `OPERATOR_ACCESS_DENIED` là lỗi phân quyền và không retry tự động.
9. Với lỗi `500`, FE hiển thị thông báo lỗi hệ thống chung; không suy diễn thành lỗi validation.
10. Enable/disable không idempotent: nếu route đã ở đúng trạng thái thì trả lỗi riêng ở bảng lỗi UC05.

### UC06 - Quản Lý Ga/Trạm

#### API-AFC-005 - List Stations

`GET /afc-ops/list-stations?routeId=&keyword=&status=&page=0&size=20`

Permission: `MASTER_DATA_READ`.

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "items": [
      {
        "id": 1,
        "routeId": 1,
        "stationCode": "BEN-THANH",
        "stationName": "Bến Thành",
        "stationOrder": 1,
        "status": "ACTIVE",
        "createdAt": "2026-06-04T10:00:00+07:00",
        "updatedAt": "2026-06-04T10:00:00+07:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

#### API-AFC-006 - Create Station

`POST /afc-ops/create-station`

Permission: `MASTER_DATA_WRITE`.

Request:

```json
{
  "routeId": 1,
  "stationCode": "BEN-THANH",
  "stationName": "Bến Thành",
  "stationOrder": 1,
  "status": "ACTIVE"
}
```

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "id": 1,
    "routeId": 1,
    "stationCode": "BEN-THANH",
    "stationName": "Bến Thành",
    "stationOrder": 1,
    "status": "ACTIVE",
    "createdAt": "2026-06-04T10:00:00+07:00",
    "updatedAt": "2026-06-04T10:00:00+07:00"
  }
}
```

#### API-AFC-007 - Update Station

`POST /afc-ops/update-station/{stationId}`

Permission: `MASTER_DATA_WRITE`.

Request:

```json
{
  "routeId": 1,
  "stationCode": "BEN-THANH",
  "stationName": "Bến Thành Updated",
  "stationOrder": 1,
  "status": "ACTIVE"
}
```

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "id": 1,
    "routeId": 1,
    "stationCode": "BEN-THANH",
    "stationName": "Bến Thành Updated",
    "stationOrder": 1,
    "status": "ACTIVE",
    "createdAt": "2026-06-04T10:00:00+07:00",
    "updatedAt": "2026-06-04T10:30:00+07:00"
  }
}
```

#### API-AFC-008 - Import Stations

`POST /afc-ops/import-stations`

Permission: `MASTER_DATA_WRITE`.

Content-Type: `multipart/form-data`, field `file`.

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "imported": 10,
    "errors": []
  }
}
```

Nếu có dòng lỗi thì không import:

```json
{
  "code": 4001,
  "message": "Import file contains invalid rows",
  "result": {
    "imported": 0,
    "errors": [
      {
        "row": 3,
        "field": "stationCode",
        "message": "Station code already exists in route"
      }
    ]
  }
}
```

### UC07 - Quản Lý Danh Mục Thiết Bị AFC

#### API-AFC-009 - List Devices

`GET /afc-ops/list-devices?stationId=&deviceType=&status=&keyword=&page=0&size=20`

Permission: `MASTER_DATA_READ`.

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "items": [
      {
        "id": 10,
        "stationId": 1,
        "stationName": "Bến Thành",
        "deviceCode": "QR-BT-001",
        "deviceType": "QR_SCANNER_SIMULATOR",
        "direction": "ENTRY",
        "status": "ACTIVE",
        "firmwareVersion": "1.0.0",
        "lastSeenAt": "2026-06-04T10:00:00+07:00",
        "createdAt": "2026-06-04T10:00:00+07:00",
        "updatedAt": "2026-06-04T10:00:00+07:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

#### API-AFC-010 - Create Device

`POST /afc-ops/create-device`

Permission: `MASTER_DATA_WRITE`.

Request:

```json
{
  "stationId": 1,
  "deviceCode": "QR-BT-001",
  "deviceType": "QR_SCANNER_SIMULATOR",
  "direction": "ENTRY",
  "status": "ACTIVE",
  "firmwareVersion": "1.0.0",
  "deviceSecret": "mock-secret"
}
```

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "id": 10,
    "stationId": 1,
    "stationName": "Bến Thành",
    "deviceCode": "QR-BT-001",
    "deviceType": "QR_SCANNER_SIMULATOR",
    "direction": "ENTRY",
    "status": "ACTIVE",
    "firmwareVersion": "1.0.0",
    "lastSeenAt": null,
    "createdAt": "2026-06-04T10:00:00+07:00",
    "updatedAt": "2026-06-04T10:00:00+07:00"
  }
}
```

#### API-AFC-011 - Update Device

`POST /afc-ops/update-device/{deviceId}`

Permission: `MASTER_DATA_WRITE`.

Request:

```json
{
  "stationId": 1,
  "deviceType": "QR_SCANNER_SIMULATOR",
  "direction": "BOTH",
  "status": "MAINTENANCE",
  "firmwareVersion": "1.0.1"
}
```

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "id": 10,
    "stationId": 1,
    "stationName": "Bến Thành",
    "deviceCode": "QR-BT-001",
    "deviceType": "QR_SCANNER_SIMULATOR",
    "direction": "BOTH",
    "status": "MAINTENANCE",
    "firmwareVersion": "1.0.1",
    "lastSeenAt": "2026-06-04T10:00:00+07:00",
    "createdAt": "2026-06-04T10:00:00+07:00",
    "updatedAt": "2026-06-04T10:30:00+07:00"
  }
}
```

#### API-AFC-012 - Import Devices

`POST /afc-ops/import-devices`

Permission: `MASTER_DATA_WRITE`.

Content-Type: `multipart/form-data`, field `file`.

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "imported": 10,
    "errors": []
  }
}
```

Nếu có dòng lỗi thì không import:

```json
{
  "code": 4001,
  "message": "Import file contains invalid rows",
  "result": {
    "imported": 0,
    "errors": [
      {
        "row": 3,
        "field": "deviceCode",
        "message": "Device code already exists"
      }
    ]
  }
}
```

## 4. Device Integration APIs

Các API này dành cho Cấp 2/mock device gọi vào Cấp 3/Cấp 4 service.

### UC08 - Thiết Bị Gửi Tín Hiệu Trạng Thái Định Kỳ

#### API-AFC-013 - Submit Device Heartbeat

`POST /afc-ops/submit-heartbeat`

Auth: `X-Device-Code`, `X-Device-Secret`.

Request:

```json
{
  "sentAt": "2026-06-04T10:00:00+07:00",
  "status": "ACTIVE",
  "firmwareVersion": "1.0.1",
  "metrics": {
    "cpu": 0.31,
    "memory": 0.52,
    "network": "OK"
  }
}
```

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "deviceCode": "QR-BT-001",
    "accepted": true,
    "serverTime": "2026-06-04T10:00:05+07:00"
  }
}
```

Luồng:

1. Xác thực device.
2. Lưu heartbeat vào MongoDB.
3. Cập nhật `devices.last_seen_at`, `firmware_version`, trạng thái tổng hợp nếu cần.
4. Trả server time để device tự đối chiếu.

### UC10 - Ghi Nhận Lượt Quét Tại Thiết Bị Soát Vé

#### API-AFC-014 - Submit Tap Event

`POST /afc-ops/submit-tap-event`

Auth: `X-Device-Code`, `X-Device-Secret`.

Request:

```json
{
  "eventId": "QR-BT-001-20260604-000001",
  "mediaType": "VIRTUAL_QR",
  "qrPayload": "signed-dynamic-qr-payload",
  "tapType": "TAP_IN",
  "occurredAt": "2026-06-04T10:05:00+07:00",
  "direction": "ENTRY",
  "rawPayload": {
    "deviceSequence": 1001
  }
}
```

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "transactionId": "uuid",
    "eventId": "QR-BT-001-20260604-000001",
    "decision": "OPEN_GATE",
    "reason": "VALID",
    "serverTime": "2026-06-04T10:05:01+07:00"
  }
}
```

Luồng:

1. Xác thực device.
2. Kiểm tra idempotency theo `(deviceCode, eventId)`.
3. Kiểm tra device active và direction hợp lệ.
4. Verify chữ ký, TTL và replay của dynamic QR payload.
5. Resolve `cardId` và đúng một sản phẩm active từ QR session: `ticketId` hoặc `entitlementId`.
6. Kiểm tra card active/không blacklist, ticket hoặc entitlement active/còn hạn và scope hợp lệ.
7. Lưu raw payload MongoDB.
8. Lưu `afc_transactions` RDBMS.
9. Trả decision cho device.

Lỗi/idempotency:

| Điều kiện | Kết quả |
| --- | --- |
| Gửi lại cùng `eventId` và payload giống nhau | Trả lại decision cũ |
| Gửi lại cùng `eventId` nhưng payload khác | `409 TAP_EVENT_CONFLICT` |
| Device disabled | Trả `decision = DENY`, `reason = DEVICE_DISABLED` |
| Card trong blacklist | Trả `decision = DENY`, `reason = MEDIA_BLACKLISTED` |
| Card inactive/cancelled | Trả `decision = DENY`, `reason = CARD_INACTIVE` hoặc `CARD_CANCELLED` |
| QR hết hạn, sai chữ ký hoặc bị replay | Trả `decision = DENY` với reason QR tương ứng |
| Entitlement hết hạn hoặc inactive | Trả `decision = DENY` với reason entitlement tương ứng |
| Ticket không hợp lệ/hết hạn/đã dùng/sai scope | Trả `decision = DENY` với reason ticket tương ứng |
| Card có cả ticket và entitlement active trong read model | Trả `decision = DENY`, `reason = ACTIVE_PRODUCT_CONFLICT` |

### UC12 - Ghi Nhận Incident Thiết Bị

#### API-AFC-015 - Submit Device Incident

`POST /afc-ops/submit-device-incident`

Auth: `X-Device-Code`, `X-Device-Secret`.

Request:

```json
{
  "incidentType": "GATE_JAMMED",
  "severity": "HIGH",
  "occurredAt": "2026-06-04T10:10:00+07:00",
  "message": "Gate arm jammed",
  "payload": {
    "sensor": "ARM_LOCK"
  }
}
```

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "accepted": true,
    "incidentId": "mongo-id"
  }
}
```

## 5. Monitoring Và Tra Cứu APIs

### UC09 - Theo Dõi Trạng Thái Hoạt Động Thiết Bị

#### API-AFC-016 - Device Status Overview

`GET /afc-ops/get-device-status?routeId=&stationId=&status=&page=0&size=20`

Permission: `DEVICE_MONITOR_READ`.

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "items": [
      {
        "deviceId": 10,
        "deviceCode": "QR-BT-001",
        "stationId": 1,
        "stationName": "Bến Thành",
        "deviceType": "QR_SCANNER_SIMULATOR",
        "status": "ACTIVE",
        "lastSeenAt": "2026-06-04T10:00:00+07:00",
        "offlineSeconds": 5
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

### UC11 - Tra Cứu Transaction Vận Hành

#### API-AFC-017 - Search Transactions

`GET /afc-ops/search-transactions?from=&to=&routeId=&stationId=&deviceId=&cardId=&ticketId=&entitlementId=&decision=&syncStatus=&ticketProcessingStatus=&page=0&size=20`

Permission: `TRANSACTION_READ`.

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "items": [
      {
        "id": "uuid",
        "eventId": "QR-BT-001-20260604-000001",
        "routeId": 1,
        "stationId": 1,
        "deviceId": 10,
        "deviceCode": "QR-BT-001",
        "mediaType": "VIRTUAL_QR",
        "cardId": "CARD-000001",
        "ticketId": null,
        "entitlementId": "ENT-000001",
        "qrId": "QR-SESSION-000001",
        "tapType": "TAP_IN",
        "occurredAt": "2026-06-04T10:05:00+07:00",
        "decision": "OPEN_GATE",
        "reason": "VALID",
        "syncStatus": "PENDING",
        "ticketProcessingStatus": null,
        "batchId": null
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

#### API-AFC-018 - Get Transaction Detail

`GET /afc-ops/get-transaction-detail?transactionId={transactionId}`

Permission: `TRANSACTION_READ`.

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "id": "uuid",
    "eventId": "QR-BT-001-20260604-000001",
    "operatorId": 1,
    "routeId": 1,
    "stationId": 1,
    "deviceId": 10,
    "deviceCode": "QR-BT-001",
    "mediaType": "VIRTUAL_QR",
    "cardId": "CARD-000001",
    "ticketId": null,
    "entitlementId": "ENT-000001",
    "qrId": "QR-SESSION-000001",
    "qrPayloadHash": "sha256-qr-payload",
    "tapType": "TAP_IN",
    "occurredAt": "2026-06-04T10:05:00+07:00",
    "receivedAt": "2026-06-04T10:05:01+07:00",
    "decision": "OPEN_GATE",
    "reason": "VALID",
    "syncStatus": "PENDING",
    "ticketProcessingStatus": null,
    "batchId": null,
    "rawEvent": {
      "deviceSequence": 1001
    }
  }
}
```

### UC13 - Theo Dõi Incident Thiết Bị

#### API-AFC-019 - Search Incidents

`GET /afc-ops/search-incidents?from=&to=&stationId=&deviceId=&severity=&incidentType=&page=0&size=20`

Permission: `INCIDENT_READ`.

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "items": [
      {
        "id": "mongo-id",
        "deviceId": 10,
        "deviceCode": "QR-BT-001",
        "stationId": 1,
        "incidentType": "GATE_JAMMED",
        "severity": "HIGH",
        "message": "Gate arm jammed",
        "occurredAt": "2026-06-04T10:10:00+07:00",
        "receivedAt": "2026-06-04T10:10:02+07:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

## 6. Level 5 Và Control Package APIs

### UC14 - Đồng Bộ Dữ Liệu Quản Lý Từ Cấp 5

#### API-AFC-020 - Receive Level 5 Business Sync

`POST /afc-ops/receive-level5-business-sync`

Auth: `X-Level5-Client-Id`, `X-Level5-Signature`.

Request:

```json
{
  "syncBatchCode": "L5-BUSINESS-SYNC-20260604-001",
  "issuedAt": "2026-06-04T09:00:00+07:00",
  "items": [
    {
      "syncType": "CARD_STATUS_CHANGED",
      "externalId": "CARD-000001",
      "version": 12,
      "card": {
        "cardId": "CARD-000001",
        "externalUserId": null,
        "cardType": "VIRTUAL_QR",
        "status": "BLACKLISTED",
        "statusReason": "LOST_CARD",
        "sourceVersion": 12
      }
    },
    {
      "syncType": "ENTITLEMENT_UPSERT",
      "externalId": "ENT-000001",
      "version": 7,
      "entitlement": {
        "entitlementId": "ENT-000001",
        "cardId": "CARD-000001",
        "fareProductCode": "MONTHLY_PASS",
        "passPeriod": "MONTH",
        "passScope": "SINGLE_ROUTE",
        "operatorRef": "OP-01",
        "routeRef": "METRO-01",
        "transportType": "METRO",
        "passengerType": null,
        "status": "ACTIVE",
        "validFrom": "2026-06-05T00:00:00+07:00",
        "validTo": "2026-07-05T00:00:00+07:00",
        "sourceVersion": 7
      }
    },
    {
      "syncType": "TICKET_UPSERT",
      "externalId": "TICKET-000001",
      "version": 3,
      "ticket": {
        "ticketId": "TICKET-000001",
        "cardId": "CARD-000002",
        "ticketType": "METRO_SINGLE_RIDE",
        "routeScopeType": "SINGLE_ROUTE",
        "operatorRef": "OP-01",
        "routeRef": "METRO-01",
        "transportType": "METRO",
        "usageStatus": "UNUSED",
        "validFrom": "2026-06-05T00:00:00+07:00",
        "validTo": "2026-06-05T23:59:59+07:00",
        "firstTapAt": null,
        "usedAt": null,
        "sourceVersion": 3
      }
    }
  ]
}
```

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "syncBatchCode": "L5-BUSINESS-SYNC-20260604-001",
    "received": 3,
    "applied": 3,
    "ignored": 0,
    "failed": 0
  }
}
```

Luồng:

1. Xác thực nguồn Cấp 5.
2. Lưu payload gốc vào `level5_business_sync_payloads`.
3. Upsert `cards`, `tickets`, `entitlements` theo `syncType`.
4. Bỏ qua item có `sourceVersion` cũ hơn bản ghi hiện tại.
5. Nếu một card có nhiều hơn một active product, ghi nhận lỗi dữ liệu và không tự chọn sản phẩm ưu tiên.
6. Refresh Redis runtime cache sau khi commit RDBMS thành công.
7. Với card status/blacklist, có thể tạo control package `MEDIA_ACCESS_RULES` để phát hành xuống Cấp 3.

Quy ước `syncType` tối thiểu:

| Giá trị | Xử lý tại C4 |
| --- | --- |
| `CARD_UPSERT` | Tạo/cập nhật read model card |
| `CARD_STATUS_CHANGED` | Cập nhật trạng thái card, bao gồm `BLACKLISTED`, `CANCELLED`, `INACTIVE` |
| `TICKET_UPSERT` | Tạo/cập nhật vé lượt Metro prepaid |
| `TICKET_STATUS_CHANGED` | Cập nhật `usageStatus` của ticket |
| `ENTITLEMENT_UPSERT` | Tạo/cập nhật vé tháng |
| `ENTITLEMENT_STATUS_CHANGED` | Cập nhật trạng thái entitlement |

#### API-AFC-021 - Pull Level 5 Business Sync

`POST /afc-ops/pull-level5-business-sync`

Permission: `CONTROL_PACKAGE_WRITE` hoặc scheduled job nội bộ.

Request:

```json
{
  "syncTypes": ["CARD_UPSERT", "CARD_STATUS_CHANGED", "TICKET_UPSERT", "ENTITLEMENT_UPSERT"],
  "sinceVersion": 11
}
```

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "pulled": 2,
    "applied": 2,
    "items": [
      {
        "syncType": "CARD_STATUS_CHANGED",
        "externalId": "CARD-000001",
        "version": 12,
        "applied": true
      }
    ]
  }
}
```

Ghi chú: API này dùng cho dev/mock hoặc khi Cấp 4 chủ động pull Cấp 5.

### UC15 - Tạo Control Package Cấu Hình Vận Hành

#### API-AFC-022 - Create Control Package

`POST /afc-ops/create-control-package`

Permission: `CONTROL_PACKAGE_WRITE`.

Request:

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

Response:

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
    "createdAt": "2026-06-04T10:20:00+07:00"
  }
}
```

Ghi chú:

- Không tạo tay `MEDIA_ACCESS_RULES` toàn mạng ở API này.
- `MEDIA_ACCESS_RULES` đi từ UC14/API-AFC-020 hoặc API-AFC-021.

#### API-AFC-023 - List Control Packages

`GET /afc-ops/list-control-packages?packageType=&sourceType=&status=&page=0&size=20`

Permission: `CONTROL_PACKAGE_READ`.

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "items": [
      {
        "id": 100,
        "version": 12,
        "packageType": "MEDIA_ACCESS_RULES",
        "sourceType": "LEVEL5_SYNCED",
        "externalPackageCode": "L5-MEDIA-RULES-20260604-001",
        "status": "CREATED",
        "publishedAt": null,
        "createdAt": "2026-06-04T10:20:00+07:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

### UC16 - Phát Hành Control Package Xuống Cấp 3

#### API-AFC-024 - Publish Control Package

`POST /afc-ops/publish-control-package/{packageId}`

Permission: `CONTROL_PACKAGE_WRITE`.

Request:

```json
{
  "stationIds": [1, 2, 3]
}
```

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "packageId": 100,
    "status": "PUBLISHED",
    "stationSyncs": [
      {
        "stationId": 1,
        "syncStatus": "PENDING"
      }
    ]
  }
}
```

Luồng:

1. Kiểm tra package `CREATED` hoặc đã publish nhưng chưa có sync tới station đích.
2. Kiểm tra station active.
3. Tạo `station_control_syncs = PENDING`.
4. Cập nhật package `PUBLISHED`.
5. Ghi audit.

### UC17 - Cấp 3 Nhận Và Áp Dụng Control Package

#### API-AFC-025 - Station Pull Pending Packages

`GET /afc-ops/pull-pending-control-packages?stationCode=BEN-THANH&currentVersion=11`

Auth: Station/device integration credential hoặc service credential nội bộ.

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": [
    {
      "syncId": 500,
      "packageId": 100,
      "version": 12,
      "packageType": "MEDIA_ACCESS_RULES",
      "sourceType": "LEVEL5_SYNCED",
      "payload": {
        "cardStatusRules": [
          {
            "cardId": "CARD-000001",
            "cardType": "VIRTUAL_QR",
            "status": "BLACKLISTED",
            "statusReason": "LOST_CARD",
            "sourceVersion": 12
          }
        ]
      }
    }
  ]
}
```

#### API-AFC-026 - Ack Control Package Apply

`POST /afc-ops/ack-control-package-apply/{syncId}`

Auth: Station/device integration credential hoặc service credential nội bộ.

Request:

```json
{
  "syncStatus": "APPLIED",
  "appliedAt": "2026-06-04T10:30:00+07:00",
  "errorMessage": null
}
```

Response:

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

## 7. Dashboard APIs

### UC18 - Dashboard Vận Hành Cấp 4

#### API-AFC-027 - Get Operations Dashboard

`GET /afc-ops/get-operations-dashboard?from=&to=&routeId=&stationId=`

Permission: `DASHBOARD_READ`.

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "deviceSummary": {
      "active": 10,
      "offline": 2,
      "maintenance": 1,
      "disabled": 0
    },
    "transactionSummary": {
      "total": 1500,
      "openGate": 1400,
      "deny": 100
    },
    "incidentSummary": {
      "total": 5,
      "high": 1
    },
    "batchSummary": {
      "created": 1,
      "submitted": 2,
      "failed": 0
    },
    "controlSyncSummary": {
      "pending": 3,
      "applied": 20,
      "failed": 1
    }
  }
}
```

## 8. Batch Và Cấp 5 Outbound APIs

### UC19 - Tạo Batch Dữ Liệu

#### API-AFC-028 - Create Batch

`POST /afc-ops/create-batch`

Permission: `BATCH_WRITE`.

Request:

```json
{
  "fromTime": "2026-06-04T00:00:00+07:00",
  "toTime": "2026-06-04T23:59:59+07:00"
}
```

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "id": "uuid",
    "batchCode": "OP01-20260604-0001",
    "fromTime": "2026-06-04T00:00:00+07:00",
    "toTime": "2026-06-04T23:59:59+07:00",
    "transactionCount": 1500,
    "status": "CREATED"
  }
}
```

Luồng:

1. Tìm transaction `syncStatus = PENDING` trong khoảng thời gian.
2. Tạo batch.
3. Gắn `batch_id` vào transaction.
4. Không gửi Cấp 5 ở bước này.

#### API-AFC-029 - List Batches

`GET /afc-ops/list-batches?status=&from=&to=&page=0&size=20`

Permission: `BATCH_READ`.

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "items": [
      {
        "id": "uuid",
        "batchCode": "OP01-20260604-0001",
        "fromTime": "2026-06-04T00:00:00+07:00",
        "toTime": "2026-06-04T23:59:59+07:00",
        "transactionCount": 1500,
        "status": "CREATED",
        "submittedAt": null,
        "createdAt": "2026-06-04T23:00:00+07:00",
        "updatedAt": "2026-06-04T23:00:00+07:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

### UC20 - Gửi Batch Dữ Liệu Lên Cấp 5

#### API-AFC-030 - Submit Batch To Level 5

`POST /afc-ops/submit-batch-to-level5/{batchId}`

Permission: `BATCH_WRITE`.

Request:

```json
{
  "forceRetry": false
}
```

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "batchId": "uuid",
    "batchCode": "OP01-20260604-0001",
    "status": "ACCEPTED",
    "submittedAt": "2026-06-04T23:00:00+07:00",
    "level5Response": {
      "ackCode": "ACK",
      "message": "Accepted"
    }
  }
}
```

Luồng:

1. Load batch `CREATED` hoặc `FAILED` retryable.
2. Build payload gồm transaction đã gắn batch.
3. Gửi sang Cấp 5/mock.
4. Lưu request/response vào `integration_exchange_logs`.
5. Cập nhật batch status.
6. Cập nhật transaction `syncStatus = SYNCED` nếu Cấp 5 accepted.

Lỗi chính:

| Điều kiện | Kết quả |
| --- | --- |
| Cấp 5 unavailable | Batch `FAILED`, giữ transaction chưa `SYNCED` |
| Cấp 5 reject | Batch `REJECTED`, lưu response |
| Submit lại batch đã accepted | Trả trạng thái hiện tại hoặc yêu cầu `forceRetry` không được phép |

## 9. Audit APIs

### UC21 - Audit Và Truy Vết

#### API-AFC-031 - Search AFC Audit Logs

`GET /afc-ops/search-audit-logs?from=&to=&accountId=&action=&resourceType=&resourceId=&page=0&size=20`

Permission: `AUDIT_READ`.

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "items": [
      {
        "id": "mongo-id",
        "accountId": "uuid",
        "action": "CONTROL_PACKAGE_PUBLISHED",
        "resourceType": "CONTROL_PACKAGE",
        "resourceId": "100",
        "result": "SUCCESS",
        "createdAt": "2026-06-04T10:30:00+07:00",
        "metadata": {
          "stationIds": [1, 2, 3]
        }
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

#### API-AUTH-010 - Search Auth Audit Logs

`GET /auth/search-audit-logs?from=&to=&accountId=&action=&page=0&size=20`

Permission: `AUDIT_READ`.

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "items": [
      {
        "id": "mongo-id",
        "accountId": "uuid",
        "username": "manager01",
        "action": "LOGIN_SUCCESS",
        "resourceType": "ACCOUNT",
        "resourceId": "uuid",
        "result": "SUCCESS",
        "ipAddress": "127.0.0.1",
        "userAgent": "Mozilla/5.0",
        "createdAt": "2026-06-04T10:30:00+07:00",
        "metadata": {}
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

## 10. Passenger App APIs

Passenger App chỉ gọi trực tiếp `afc-ops-service` để lấy dynamic QR từ card đã được Cấp 5 cấp. Các nghiệp vụ mua vé, gia hạn vé, chuyển card vật lý sang card ảo vẫn thuộc App service/Cấp 5 tùy phân công nhóm.

### UC22 - App Lấy QR Động Từ Card

#### API-AFC-032 - Generate Dynamic QR

`POST /afc-ops/generate-dynamic-qr`

Auth: `X-External-User-Id` trong MVP, production có thể thay bằng token App/C5.

Request:

```json
{
  "cardId": "CARD-000001",
  "productType": "ENTITLEMENT",
  "ticketId": null,
  "entitlementId": "ENT-000001"
}
```

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "qrId": "QR-SESSION-000001",
    "cardId": "CARD-000001",
    "ticketId": null,
    "entitlementId": "ENT-000001",
    "qrPayload": "signed-dynamic-qr-payload",
    "expiresAt": "2026-06-04T10:05:30+07:00",
    "refreshAfterSeconds": 30
  }
}
```

Luồng:

1. Xác thực App/mock App.
2. Kiểm tra card tồn tại trong read model, `status = ACTIVE`, không blacklist/cancelled/inactive.
3. Nếu C5 có đồng bộ `externalUserId`, kiểm tra card thuộc user đang gọi.
4. Xác định đúng một active product của card: `ticket` vé lượt Metro hoặc `entitlement` vé tháng.
5. Nếu là ticket, kiểm tra `usageStatus` cho phép hiển thị QR, còn hạn và đúng phạm vi.
6. Nếu là entitlement, kiểm tra active, còn hạn và đúng phạm vi.
7. Sinh `qrId`, nonce, TTL 30-60 giây, ký payload và lưu `qr:session:{qrId}` trong Redis.
8. Trả QR payload để App render.

Lỗi chính:

| Điều kiện | Kết quả |
| --- | --- |
| Card không tồn tại hoặc không active | `CARD_NOT_FOUND` hoặc `CARD_INACTIVE` |
| Card bị blacklist/cancelled | `MEDIA_BLACKLISTED` hoặc `CARD_CANCELLED` |
| Ticket hết hạn/đã dùng/sai phạm vi | `TICKET_EXPIRED`, `TICKET_ALREADY_USED`, `TICKET_SCOPE_INVALID` |
| Entitlement hết hạn/inactive | `ENTITLEMENT_EXPIRED`, `ENTITLEMENT_INACTIVE` |
| Card có nhiều hơn một active product | `ACTIVE_PRODUCT_CONFLICT` |
| Chưa đồng bộ ticket/entitlement từ C5 | `ACTIVE_PRODUCT_NOT_FOUND` |

Các nghiệp vụ App khác không thuộc `afc-ops-service`. Cấp 3/Cấp 4 chỉ nhận read model từ Cấp 5 qua API-AFC-020/API-AFC-021 gồm:

- `cards`;
- `tickets`;
- `entitlements`;
- trạng thái card/blacklist hiện hành.

Mock App trong MVP gọi API-AFC-032 để lấy QR payload, sau đó mock C2 scan QR và gọi `POST /afc-ops/submit-tap-event`.

## 11. Luồng API Theo UC

### Luồng A - Login Và Vào Dashboard

1. FE gọi `POST /auth/login`.
2. Trình duyệt nhận access token và refresh token qua HttpOnly cookie.
3. FE gọi `GET /afc-ops/get-operations-dashboard`.
4. FE gọi thêm `GET /afc-ops/get-device-status` nếu cần bảng thiết bị.

### Luồng B - Thiết Bị Gửi Tap Event

1. Mock Cấp 2 gọi `POST /afc-ops/submit-tap-event`.
2. System xác thực device và ghi transaction.
3. System trả `OPEN_GATE` hoặc `DENY`.
4. Cấp 4 có thể tra cứu bằng `GET /afc-ops/search-transactions`.

### Luồng C - Card Status/Blacklist Từ Cấp 5 Xuống Thiết Bị

1. Cấp 5/mock gọi `POST /afc-ops/receive-level5-business-sync` với card status/blacklist hiện hành.
2. System cập nhật read model `cards` và có thể tạo control package `MEDIA_ACCESS_RULES`, `LEVEL5_SYNCED`, `CREATED`.
3. Manager gọi `POST /afc-ops/publish-control-package/{packageId}`.
4. Cấp 3/station gọi `GET /afc-ops/pull-pending-control-packages`.
5. Cấp 3/station apply rule local và gọi `POST /afc-ops/ack-control-package-apply/{syncId}`.
6. Tap event sau đó nếu card nằm blacklist thì API-AFC-014 trả `DENY` với `reason = MEDIA_BLACKLISTED`.

### Luồng D - Tạo Và Gửi Batch Lên Cấp 5

1. Manager gọi `POST /afc-ops/create-batch`.
2. System gom transaction `PENDING` thành batch `CREATED`.
3. Manager hoặc scheduler gọi `POST /afc-ops/submit-batch-to-level5/{batchId}`.
4. System gửi payload sang Cấp 5/mock.
5. Cập nhật batch `ACCEPTED`, `REJECTED` hoặc `FAILED`.

### Luồng E - Tạo Account Nhân Sự

1. `OPERATOR_ADMIN` gọi `POST /account/create-account`.
2. System tạo account với `passwordStatus = NEED_TO_CHANGE`.
3. Admin chuyển mật khẩu tạm ngoài hệ thống.
4. User login bằng mật khẩu tạm.
5. FE bắt user gọi `POST /auth/change-password`.

### Luồng F - Sử Dụng QR/Card Đã Đồng Bộ Từ Cấp 5

1. App/C5 tạo card, ticket hoặc entitlement ở hệ sở hữu nghiệp vụ.
2. C5 đồng bộ read model sang C4 qua `POST /afc-ops/receive-level5-business-sync`.
3. App gọi `POST /afc-ops/generate-dynamic-qr` để lấy QR payload ngắn hạn từ card đã đồng bộ.
4. Mock C2 scan QR và gọi `POST /afc-ops/submit-tap-event`.
5. C4 verify QR, card status, ticket/entitlement và blacklist rồi trả `OPEN_GATE` hoặc `DENY`.

## 12. Error Code Đề Xuất

| Error code | HTTP | Ý nghĩa |
| --- | --- | --- |
| `UNAUTHENTICATED` | 401 | Thiếu/sai token |
| `ACCESS_DENIED` | 403 | Không có permission |
| `VALIDATION_ERROR` | 400 | Request sai định dạng |
| `RESOURCE_NOT_FOUND` | 404 | Không tìm thấy resource |
| `DUPLICATE_RESOURCE` | 409 | Trùng unique key |
| `IDEMPOTENCY_CONFLICT` | 409 | Trùng idempotency key nhưng payload khác |
| `ACCOUNT_DISABLED` | 403 | Account bị khóa |
| `DEVICE_AUTH_FAILED` | 401 | Device credential sai |
| `DEVICE_DISABLED` | 403 | Device bị vô hiệu hóa |
| `LEVEL5_AUTH_FAILED` | 401 | Xác thực Cấp 5 thất bại |
| `LEVEL5_UNAVAILABLE` | 502 | Không gọi được Cấp 5 |
| `PACKAGE_NOT_READY` | 400 | Package chưa sẵn sàng phát hành |
| `BATCH_NOT_RETRYABLE` | 400 | Batch không được retry/gửi lại |
| `CARD_NOT_FOUND` | 404 | Không tìm thấy card trong read model C4 |
| `CARD_INACTIVE` | 400 | Card không active |
| `CARD_CANCELLED` | 400 | Card đã bị hủy |
| `MEDIA_BLACKLISTED` | 403 | Card đang nằm trong blacklist |
| `TICKET_NOT_FOUND` | 404 | Không tìm thấy ticket |
| `TICKET_INVALID` | 400 | Ticket vé lượt không hợp lệ |
| `TICKET_EXPIRED` | 400 | Ticket vé lượt đã hết hạn |
| `TICKET_ALREADY_USED` | 400 | Ticket vé lượt đã được dùng |
| `TICKET_SCOPE_INVALID` | 400 | Ticket vé lượt không hợp lệ với tuyến/ga hiện tại |
| `ACTIVE_PRODUCT_CONFLICT` | 409 | Card có nhiều hơn một sản phẩm active trong read model |
| `ACTIVE_PRODUCT_NOT_FOUND` | 404 | Không tìm thấy ticket/entitlement active cho card |
| `ENTITLEMENT_NOT_FOUND` | 404 | Không tìm thấy entitlement |
| `ENTITLEMENT_INACTIVE` | 400 | Entitlement không active |
| `ENTITLEMENT_EXPIRED` | 400 | Entitlement đã hết hạn |
| `QR_EXPIRED` | 400 | QR động đã hết hạn |
| `QR_INVALID_SIGNATURE` | 400 | Chữ ký QR không hợp lệ |
| `QR_REPLAYED` | 409 | QR/event bị phát hiện replay |
| `PASS_SCOPE_INVALID` | 400 | Phạm vi vé không hợp lệ |

## 13. Out Of Scope API

- API quản lý passenger identity/profile đầy đủ.
- API bán vé/mua vé/payment.
- API đăng ký/gia hạn ticket/entitlement cho Passenger App.
- API generate dynamic QR cho Passenger App.
- API chuyển card vật lý sang card ảo.
- API fare calculation.
- API clearing/settlement chi tiết.
- API quản lý source of truth và lịch sử blacklist toàn mạng ở Cấp 5.
- API whitelist.
- API QR tĩnh, EMV, biometric và thiết bị đọc card vật lý thật.
- API email/OTP.

