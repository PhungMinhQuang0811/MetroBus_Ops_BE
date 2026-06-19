# Plan UC21 - Audit và truy vết

## 1. Mục tiêu

- Cho `OPERATOR_ADMIN` tra cứu audit và truy vết.
- Nếu wireframe đủ scope, hỗ trợ thêm tab integration.
- Audit là dữ liệu chỉ ghi append, không sửa/xóa bằng API nghiệp vụ.

## 2. Scope theo wireframe

### Tab 1 - Đăng nhập & tài khoản

- Search theo `from`, `to`, `username`, `action`, `result`.
- List hiển thị `time`, `username`, `action`, `result`, `ipAddress`, `userAgent`.
- Detail vẫn có `accountId` để tra kỹ thuật, nhưng FE không dùng nó làm filter chính.
- API: `GET /auth/search-audit-logs`

### Tab 2 - Thao tác vận hành

- Search theo `from`, `to`, `username`, `action`, `resourceType`, `resourceId`.
- List hiển thị `time`, `actor`, `action`, `result`, `module`, `resourceType`, `resourceId`.
- Detail hiển thị `module`, `resourceName`, `requestId`, `before`, `after`, `metadata`.
- API: `GET /audit/search-audit-logs`

### Tab 3 - Tích hợp hệ thống

Tab này hiện là **phần làm sau** vì phần tích hợp hệ thống chưa chốt chắc trong phase hiện tại.
Chỉ làm khi đã có tích hợp thật và cần theo dõi request/response kỹ thuật.

- Search theo `from`, `to`, `direction`, `targetSystem`, `resourceType`, `resourceId`, `correlationId`.
- List hiển thị `time`, `direction`, `targetSystem`, `status`, `correlationId`, `endpoint/action`.
- Detail hiển thị `requestSummary`, `responseSummary`, raw `request`, raw `response`.
- Data source: `integration_exchange_logs` hoặc log kỹ thuật nếu sau này chốt làm riêng

## 3. Collections cần có

- `afc_audit_logs`
- `auth_audit_logs`
- `integration_exchange_logs` nếu sau này làm tab integration

## 4. API cần có

### 4.1 Auth audit

`GET /auth/search-audit-logs`

Query params:

- `from` `ISO_DATE_TIME`, optional
- `to` `ISO_DATE_TIME`, optional
- `username` string, optional, contains search
- `action` string, optional
- `result` string, optional
- `page` int, default `0`
- `size` int, default `20`

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "items": [
      {
        "id": "audit-log-id",
        "accountId": "account-id",
        "username": "admin01",
        "action": "AUTH_LOGIN",
        "resourceType": "AUTH_SESSION",
        "resourceId": "account-id-or-username",
        "resourceName": "admin01",
        "result": "SUCCESS",
        "requestId": "req-001",
        "ipAddress": "127.0.0.1",
        "userAgent": "Mozilla/5.0",
        "httpMethod": "POST",
        "requestPath": "/auth/login",
        "requestData": "[...]",
        "responseData": "[...]",
        "errorMessage": null,
        "createdAt": "2026-06-19T10:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

`GET /auth/get-audit-log/{auditId}`

- Trả về 1 bản ghi audit chi tiết theo `auditId`.
- FE dùng khi mở drawer/modal detail từ list.

### 4.2 AFC audit

`GET /audit/search-audit-logs`

Query params:

- `from` `ISO_DATE_TIME`, optional
- `to` `ISO_DATE_TIME`, optional
- `username` string, optional, contains search
- `action` string, optional
- `resourceType` string, optional
- `resourceId` string, optional
- `page` int, default `0`
- `size` int, default `20`

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "items": [
      {
        "id": "audit-log-id",
        "operatorCode": "OP01",
        "accountId": "account-id",
        "username": "manager01",
        "action": "ROUTE_CREATED",
        "resourceType": "ROUTE",
        "resourceId": "1",
        "resourceName": "Route A",
        "result": "SUCCESS",
        "requestId": "req-002",
        "ipAddress": "127.0.0.1",
        "userAgent": "Mozilla/5.0",
        "httpMethod": "POST",
        "requestPath": "/route/create-route",
        "requestData": "[...]",
        "responseData": "[...]",
        "errorMessage": null,
        "createdAt": "2026-06-19T10:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

`GET /audit/get-audit-log/{auditId}`

- Trả về 1 bản ghi audit chi tiết theo `auditId`.
- FE dùng khi mở drawer/modal detail từ list.

### 4.3 Integration log

Tab integration hiện **chưa triển khai**.
Khi chốt scope sau này mới bổ sung endpoint và collection riêng.

## 5. Ghi log ở đâu

- create/update route
- create/update station
- create/update device
- create/publish control package
- create/submit batch
- login/logout/account change nếu scope auth được làm
- request/response integration nếu tab này được làm sau

## 6. Util / service cần thêm

- util lấy `ipAddress`, `userAgent`, `requestId`, `path`, `method`
- audit service để ghi log thống nhất
- repository/MongoTemplate query cho từng collection
- request context filter/interceptor nếu cần capture metadata từ HTTP request

## 7. Error / permission

- Permission: `AUDIT_READ`
- Query quá rộng trả `400 Bad Request`
- Không có quyền trả `403 Forbidden`
- Không đăng nhập trả `401 Unauthorized`
- Nếu `from > to` trả lỗi:
  - AFC: `2032 INVALID_AUDIT_TIME_RANGE`
  - Auth: `2010 INVALID_AUDIT_TIME_RANGE`
- Nếu `page/size` không hợp lệ:
  - AFC: `2001 INVALID_PAGE_REQUEST`
  - Auth: `2003 INVALID_PAGE_REQUEST`

## 8. Postman test scenarios

1. Search auth audit default.
2. Search AFC audit default.
3. Filter theo time range.
4. Filter theo action.
5. Filter theo resource.
6. Filter theo account.
7. Query quá rộng.
8. page/size invalid.
9. Unauthorized / forbidden.
10. Get audit detail by id.

## 9. Done when

- Có collection audit.
- Có API search audit và get audit detail cho cả auth và AFC.
- Có log ghi ở các nghiệp vụ chốt.
- Màn wireframe audit hiển thị đủ 2 tab bắt buộc.
- Tab integration vẫn là phần làm sau.
