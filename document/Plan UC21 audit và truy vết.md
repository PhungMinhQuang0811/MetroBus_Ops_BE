# Plan UC21 - Audit và truy vết

## 1. Mục tiêu

- Cho `OPERATOR_ADMIN` tra cứu audit và truy vết.
- Nếu wireframe đủ scope, hỗ trợ thêm tab integration.
- Audit là dữ liệu chỉ ghi append, không sửa/xóa bằng API nghiệp vụ.

## 2. Scope theo wireframe

### Tab 1 - Đăng nhập & tài khoản

- Search theo `from`, `to`, `username`, `action`, `result`.
- List hiển thị `time`, `username`, `action`, `result`, `ipAddress`, `userAgent`.
- Detail hiển thị `accountId`, `requestId`, `metadata`.
- API: `GET /auth/search-audit-logs`

### Tab 2 - Thao tác vận hành

- Search theo `from`, `to`, `accountId`, `action`, `resourceType`, `resourceId`.
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

- `GET /audit/search-audit-logs`
- `GET /auth/search-audit-logs`
- `GET /audit/search-integration-logs` nếu backend tách riêng và phần này đã được chốt làm sau

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

## 8. Postman test scenarios

1. Search audit default.
2. Filter theo time range.
3. Filter theo action.
4. Filter theo resource.
5. Filter theo account.
6. Query quá rộng.
7. Unauthorized / forbidden.
8. Search auth audit logs.
9. Search integration logs nếu tab này được làm sau.

## 9. Done when

- Có collection audit.
- Có API search audit.
- Có log ghi ở các nghiệp vụ chốt.
- Màn wireframe audit hiển thị đủ 2 tab bắt buộc, tab integration là phần làm sau.
