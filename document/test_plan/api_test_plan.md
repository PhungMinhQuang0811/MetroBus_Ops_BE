# API Test Plan Theo Use Case

**Dự án:** MetroBus Ticketing/AFC MVP  
**Mục đích:** Code xong use case nào thì test API use case đó, không phải đối chiếu qua nhiều phần tài liệu.  
**Tài liệu nguồn:** `use_case_specifications.md`, `acceptance_criteria.md`, `SRS_MetroBusTicket.md`

---

## 1. Quy Ước Chung

### 1.1. Path

Path chỉ ghi phần nghiệp vụ ngắn gọn:

- Đúng: `/auth/login`, `/cards/create-virtual-card`, `/validator/scan-ticket`
- Không cần ghi prefix: `/api/v1`

Khi tạo Postman collection, đặt `{{baseUrl}} = http://localhost:{port}/vdt`, request path chỉ nối phần còn lại.

### 1.2. Response Wrapper

Tất cả API trả về theo `ApiResponse<T>`:

```java
public class ApiResponse<T> {
    @Builder.Default
    private int code = 1000;

    @Builder.Default
    private String message = "Success";

    private T result;
}
```

Success mặc định:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {}
}
```

### 1.3. Code Range

| Range | Nhóm | Ý nghĩa |
| :--- | :--- | :--- |
| `1xxx` | General / Success | Thành công hoặc trạng thái xử lý hợp lệ |
| `2xxx` | Validation errors | Sai format, thiếu field, request không hợp lệ |
| `3xxx` | Business logic & Database errors | Vi phạm nghiệp vụ, conflict, không đủ số dư, sai trạng thái |
| `4xxx` | Security, Authentication & System errors | Chưa đăng nhập, token sai/hết hạn, không có quyền, lỗi hệ thống |

### 1.4. Assertion Bắt Buộc

Mỗi API test phải kiểm tra:

- HTTP status.
- Response có `code`, `message`, `result`.
- Success dùng `code = 1000` nếu không có success code đặc thù.
- Error code nằm đúng range.
- API protected có test `401`.
- API theo role có test `403`.
- API theo tenant có test tenant isolation.
- API tài chính có test idempotency và không thay đổi số dư khi lỗi.

### 1.5. Environment Variables

| Variable | Ý nghĩa |
| :--- | :--- |
| `baseUrl` | Backend base URL, ví dụ `http://localhost:8080/vdt` |
| `passengerToken` | JWT passenger |
| `staffToken` | JWT staff |
| `companyManagerToken` | JWT company manager |
| `platformManagerToken` | JWT platform manager |
| `adminToken` | JWT admin |
| `operatorId` | Tenant/operator test |
| `otherOperatorId` | Tenant khác để test isolation |
| `cardUid` | Mã thẻ test |
| `virtualCardId` | ID thẻ ảo |
| `physicalCardId` | ID thẻ vật lý |
| `subscriptionId` | ID subscription |
| `orderId` | ID order |
| `walletId` | ID ví |
| `shiftId` | ID ca trực active |
| `journeyId` | ID journey |
| `policyId` | ID fare policy |
| `payoutId` | ID payout request |

---

## 2. Common API Tests

### API-COMMON-001: Missing Token

- Request:

```http
GET /user/me
Authorization: none
```

- Expected:

```json
{
  "code": 4001,
  "message": "Unauthenticated",
  "result": null
}
```

- HTTP: `401`

### API-COMMON-002: Invalid Or Expired Token

- Request:

```http
GET /user/me
Cookie: accessToken=invalid_or_expired_token
```

- Expected:

```json
{
  "code": 4002,
  "message": "Token is invalid or expired",
  "result": null
}
```

- HTTP: `401`

### API-COMMON-003: Forbidden Role

- Request:

```http
POST /admin/rbac
Cookie: accessToken={{passengerAccessToken}}
```

- Expected:

```json
{
  "code": 4003,
  "message": "Access denied",
  "result": null
}
```

- HTTP: `403`

### API-COMMON-004: Tenant Isolation

- Request:

```http
GET /staff?operatorId={{otherOperatorId}}
Cookie: accessToken={{companyManagerAccessToken}}
```

- Expected:

```json
{
  "code": 4003,
  "message": "Access denied",
  "result": null
}
```

- HTTP: `403` hoặc `404`

---

## 3. API Tests Theo Use Case

## Module 1: Xác Thực & Tài Khoản

### UC01: Đăng ký & Đăng nhập bằng Số điện thoại Passenger

#### API-UC01-001: Check phone number - existing account

- Request:

```http
POST /auth/phone/check
```

```json
{
  "phoneNumber": "0900000001"
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "exists": true,
    "nextStep": "PASSWORD_LOGIN",
    "phoneNumber": "0900000001"
  }
}
```

- HTTP: `200`
- Side effects: Không gửi OTP cho số điện thoại đã có tài khoản.
- FE usage: Lưu `result.phoneNumber` làm `identifier` khi gọi `POST /auth/login`.

#### API-UC01-002: Check phone number - new account triggers registration OTP

- Request:

```http
POST /auth/phone/check
```

```json
{
  "phoneNumber": "0900000002"
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "exists": false,
    "nextStep": "REGISTER_OTP",
    "phoneNumber": "0900000002"
  }
}
```

- HTTP: `200`
- Notes: Production phải gửi OTP thật qua SMS Gateway/Firebase SMS và không trả OTP trong response. Dev/test có thể dùng fake SMS hoặc log OTP để kiểm thử. Backend lưu OTP đăng ký tạm theo `phoneNumber` và `purpose` với TTL 2 phút.
- FE usage: Lưu `result.phoneNumber` để truyền sang bước verify OTP đăng ký.

#### API-UC01-003: Login existing passenger with password

- Request:

```http
POST /auth/login
```

```json
{
  "identifier": "0900000001",
  "password": "P@ssword123"
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "id": "account_id",
    "phoneNumber": "0900000001",
    "roles": ["PASSENGER"],
    "permissions": []
  }
}
```

- HTTP: `200`
- Header/Cookie:
  - Response set cookie chứa access token và refresh token theo cấu hình backend.
- Side effects: Không gửi OTP.

#### API-UC01-004: Verify registration OTP success

- Request:

```http
POST /auth/register/verify-otp
```

```json
{
  "phoneNumber": "0900000002",
  "otp": "123456"
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "registrationToken": "temporary-registration-token",
    "nextStep": "SET_PASSWORD"
  }
}
```

- HTTP: `200`
- Side effects: Chưa tạo account hoàn chỉnh và chưa cấp JWT nếu người dùng chưa đặt mật khẩu.

#### API-UC01-005: Complete registration by setting password

- Request:

```http
POST /auth/register/set-password
```

```json
{
  "registrationToken": "temporary-registration-token",
  "password": "P@ssword123"
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "id": "account_id",
    "phoneNumber": "0900000002",
    "roles": ["PASSENGER"],
    "permissions": []
  }
}
```

- HTTP: `200`
- Header/Cookie:
  - Response set cookie chứa access token và refresh token theo cấu hình backend.
- Side effects: Tạo account `PASSENGER`, set `isPhoneVerified = true`, lưu password đã mã hóa và tạo wallet `PASSENGER` số dư `0`.

#### API-UC01-006: Registration OTP invalid/expired

- Request:

```http
POST /auth/register/verify-otp
```

```json
{
  "phoneNumber": "0900000002",
  "otp": "000000"
}
```

- Expected response:

```json
{
  "code": 3001,
  "message": "OTP is invalid or expired",
  "result": null
}
```

- HTTP: `400` hoặc `422`

#### API-UC01-007: Request registration OTP again replaces previous OTP

- Request:

```http
POST /auth/phone/check
```

```json
{
  "phoneNumber": "0900000002"
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "exists": false,
    "nextStep": "REGISTER_OTP",
    "phoneNumber": "0900000002"
  }
}
```

- HTTP: `200`
- Side effects: Nếu số điện thoại chưa có tài khoản, OTP cũ bị ghi đè bằng OTP mới theo `phoneNumber`; backend không cần endpoint resend riêng.

#### API-UC01-008: Invalid phone number

- Request:

```http
POST /auth/phone/check
```

```json
{
  "phoneNumber": "09000000001"
}
```

- Expected response:

```json
{
  "code": 2004,
  "message": "Invalid phone number format",
  "result": null
}
```

- HTTP: `400`

#### API-UC01-009: Request registration OTP during cooldown

- Request:

```http
POST /auth/phone/check
```

```json
{
  "phoneNumber": "0900000002"
}
```

- Precondition: Cùng số điện thoại và `purpose = REGISTER` vừa được gửi OTP chưa đủ 60 giây.
- Expected response:

```json
{
  "code": 3002,
  "message": "OTP request limit exceeded. Please try again later.",
  "result": null
}
```

- HTTP: `429`
- Side effects: Không gửi SMS và không thay đổi OTP đang còn hiệu lực.

#### API-UC01-010: Daily registration OTP limit by phone number and purpose

- Request: Gọi yêu cầu OTP lần thứ 6 trong ngày với cùng số điện thoại và cùng `purpose`.
- Expected response:

```json
{
  "code": 3002,
  "message": "OTP request limit exceeded. Please try again later.",
  "result": null
}
```

- HTTP: `429`
- Side effects: Không gửi SMS.

#### API-UC01-011: Hourly registration OTP limit by IP

- Request: Gọi yêu cầu OTP lần thứ 21 trong 1 giờ từ cùng IP.
- Expected response:

```json
{
  "code": 3002,
  "message": "OTP request limit exceeded. Please try again later.",
  "result": null
}
```

- HTTP: `429`
- Side effects: Không gửi SMS.

#### API-UC01-012: Registration OTP invalid after 5 failed verification attempts

- Request: Xác minh sai OTP lần thứ 5 rồi thử xác minh lại OTP cũ.
- Expected response:

```json
{
  "code": 3001,
  "message": "OTP is invalid or expired",
  "result": null
}
```

- HTTP: `400` hoặc `422`
- Side effects: OTP hiện tại bị vô hiệu hóa; hành khách phải yêu cầu OTP mới.

#### Cấu hình OTP MVP

Các giới hạn phải nằm trong application config và counter được lưu tạm trong Redis theo `phoneNumber`, IP và `purpose`:

```yaml
app:
  otp:
    expiration-ms: 120000
    resend-cooldown-seconds: 60
    max-requests-per-phone-per-day: 5
    max-requests-per-ip-per-hour: 20
    max-verification-attempts: 5
```

### UC02: Đăng nhập tài khoản nội bộ

#### API-UC02-001: Internal login success

- Request:

```http
POST /auth/login
```

```json
{
  "identifier": "staff01",
  "password": "Password@123"
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "id": "account_id",
    "username": "staff01",
    "roles": ["STAFF"],
    "permissions": ["CARD_CREATE", "SHIFT_OPEN"]
  }
}
```

- HTTP: `200`
- Header/Cookie:
  - Response set cookie chứa access token theo cấu hình backend, ví dụ `Set-Cookie: accessToken=...; HttpOnly; Path=/; SameSite=Lax`

#### API-UC02-002: Wrong credential

- Request:

```http
POST /auth/login
```

```json
{
  "identifier": "staff01",
  "password": "wrong"
}
```

- Expected response:

```json
{
  "code": 4001,
  "message": "Invalid username or password",
  "result": null
}
```

- HTTP: `401`

#### API-UC02-003: Locked account cannot login

- Request:

```http
POST /auth/login
```

```json
{
  "identifier": "locked_staff",
  "password": "Password@123"
}
```

- Expected response:

```json
{
  "code": 4004,
  "message": "Account is locked",
  "result": null
}
```

- HTTP: `401` hoặc `403`

### UC03: Đăng xuất

#### API-UC03-001: Logout success

- Request:

```http
POST /auth/logout
Cookie: accessToken={{staffAccessToken}}
```

```json
{}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": null
}
```

- HTTP: `200`
- Side effects: Token hiện tại bị blacklist.

#### API-UC03-002: Reuse logged-out token

- Request:

```http
GET /user/me
Cookie: accessToken=logged_out_token
```

- Expected response:

```json
{
  "code": 4002,
  "message": "Token is invalid or expired",
  "result": null
}
```

- HTTP: `401`

### UC04: Đổi mật khẩu nội bộ

#### API-UC04-001: Change password success

- Request:

```http
POST /user/change-password
Cookie: accessToken={{staffAccessToken}}
```

```json
{
  "oldPassword": "OldPassword@123",
  "newPassword": "NewPassword@123",
  "confirmPassword": "NewPassword@123"
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": null
}
```

- HTTP: `200`
- Side effects: Login bằng password cũ thất bại, password mới thành công.

#### API-UC04-002: Wrong old password

- Request:

```http
POST /user/change-password
Cookie: accessToken={{staffAccessToken}}
```

```json
{
  "oldPassword": "WrongPassword@123",
  "newPassword": "NewPassword@123",
  "confirmPassword": "NewPassword@123"
}
```

- Expected response:

```json
{
  "code": 3005,
  "message": "Old password is incorrect",
  "result": null
}
```

- HTTP: `422`

#### API-UC04-003: Weak or mismatched new password

- Request:

```http
POST /user/change-password
Cookie: accessToken={{staffAccessToken}}
```

```json
{
  "oldPassword": "OldPassword@123",
  "newPassword": "123",
  "confirmPassword": "456"
}
```

- Expected response:

```json
{
  "code": 2001,
  "message": "Password is invalid",
  "result": null
}
```

- HTTP: `400`

### UC05: Khôi phục mật khẩu

#### API-UC05-001: Internal forgot password request by email

- Request:

```http
POST /auth/forgot-password
```

```json
{
  "email": "staff@example.com"
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": null
}
```

- HTTP: `200`

#### API-UC05-002: Reset password success

- Request:

```http
POST /auth/reset-password
```

```json
{
  "resetToken": "reset_token",
  "newPassword": "NewPassword@123",
  "confirmPassword": "NewPassword@123"
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": null
}
```

- HTTP: `200`

#### API-UC05-003: Reset token expired/invalid

- Request:

```http
POST /auth/reset-password
```

```json
{
  "resetToken": "expired_token",
  "newPassword": "NewPassword@123",
  "confirmPassword": "NewPassword@123"
}
```

- Expected response:

```json
{
  "code": 3006,
  "message": "Reset token is invalid or expired",
  "result": null
}
```

- HTTP: `400` hoặc `422`

#### API-UC05-004: Passenger forgot password request OTP

- Request:

```http
POST /auth/forgot-password/request-otp
```

```json
{
  "phoneNumber": "0900000001"
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": null
}
```

- HTTP: `200`
- Notes: Chỉ gửi OTP nếu số điện thoại đã tồn tại và tài khoản đang hoạt động. OTP reset password được lưu tách biệt theo `purpose = RESET_PASSWORD`.

#### API-UC05-005: Passenger verify forgot password OTP

- Request:

```http
POST /auth/forgot-password/verify-otp
```

```json
{
  "phoneNumber": "0900000001",
  "otp": "123456"
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "resetToken": "temporary-reset-token",
    "nextStep": "RESET_PASSWORD"
  }
}
```

- HTTP: `200`

#### API-UC05-006: Passenger reset password by phone OTP token

- Request:

```http
POST /auth/reset-password
```

```json
{
  "token": "temporary-reset-token",
  "newPassword": "NewP@ssword123"
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": null
}
```

- HTTP: `200`
- Side effects: Lưu mật khẩu mới đã mã hóa, xóa reset token và vô hiệu hóa OTP reset password.

#### API-UC05-007: Passenger forgot password OTP invalid/expired

- Request:

```http
POST /auth/forgot-password/verify-otp
```

```json
{
  "phoneNumber": "0900000001",
  "otp": "000000"
}
```

- Expected response:

```json
{
  "code": 3001,
  "message": "OTP is invalid or expired",
  "result": null
}
```

- HTTP: `400` hoặc `422`

#### API-UC05-008: Passenger forgot password OTP rate limited

- Request: Gọi yêu cầu OTP reset password lần thứ 6 trong ngày với cùng số điện thoại và `purpose = RESET_PASSWORD`, hoặc yêu cầu lại trước 60 giây.
- Expected response:

```json
{
  "code": 3002,
  "message": "OTP request limit exceeded. Please try again later.",
  "result": null
}
```

- HTTP: `429`
- Side effects: Không gửi SMS.

### UC06: Cập nhật hồ sơ cá nhân

#### API-UC06-001: Get my profile

- Request:

```http
GET /user/me
Cookie: accessToken={{passengerAccessToken}}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "accountId": "account_id",
    "phone": "0900000001",
    "email": "user@example.com",
    "fullName": "Nguyen Van A",
    "roles": ["PASSENGER"],
    "kycStatus": "VERIFIED"
  }
}
```

- HTTP: `200`

#### API-UC06-002: Update my profile

- Request:

```http
PUT /user/me
Cookie: accessToken={{passengerAccessToken}}
```

```json
{
  "fullName": "Nguyen Van A",
  "email": "user@example.com",
  "dateOfBirth": "1999-01-01",
  "citizenId": "079099000001",
  "address": "Ho Chi Minh City"
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "accountId": "account_id",
    "updated": true
  }
}
```

- HTTP: `200`

#### API-UC06-003: Verify email OTP

- Request:

```http
POST /user/verify-email
Cookie: accessToken={{passengerAccessToken}}
```

```json
{
  "email": "user@example.com",
  "otp": "123456"
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": null
}
```

- HTTP: `200`

#### API-UC06-004: Email OTP invalid/expired

- Request:

```http
POST /user/verify-email
Cookie: accessToken={{passengerAccessToken}}
```

```json
{
  "email": "user@example.com",
  "otp": "000000"
}
```

- Expected response:

```json
{
  "code": 3001,
  "message": "OTP is invalid or expired",
  "result": null
}
```

- HTTP: `400` hoặc `422`

## Module 2: Thẻ & Vé Tháng

### UC07: Đăng ký mua thẻ cứng trực tuyến

#### API-UC07-001: Guest create physical card order

- Request:

```http
POST /orders/physical-card
```

```json
{
  "fullName": "Nguyen Van A",
  "phone": "0900000001",
  "email": "user@example.com",
  "citizenId": "079099000001",
  "deliveryMethod": "PICKUP",
  "pickupStationId": 1001,
  "shippingAddress": null,
  "paymentProvider": "VNPAY_SANDBOX"
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "orderId": "order_id",
    "orderStatus": "PENDING_PAYMENT",
    "paymentUrl": "https://payment-url"
  }
}
```

- HTTP: `200` hoặc `201`

#### API-UC07-002: Physical card payment success callback

- Request:

```http
POST /payments/callback
```

```json
{
  "provider": "VNPAY_SANDBOX",
  "providerTransactionId": "provider_txn_id",
  "orderId": "order_id",
  "amount": 100000,
  "status": "SUCCESS",
  "signature": "signature"
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "processed": true,
    "orderId": "order_id",
    "orderStatus": "PRINTING",
    "cardMedium": "PHYSICAL",
    "cardStatus": "ACTIVE"
  }
}
```

- HTTP: `200`

#### API-UC07-003: Payment cancelled/expired

- Request:

```http
POST /payments/callback
```

```json
{
  "provider": "VNPAY_SANDBOX",
  "providerTransactionId": "provider_txn_id",
  "orderId": "order_id",
  "amount": 100000,
  "status": "CANCELLED",
  "signature": "signature"
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "processed": true,
    "orderId": "order_id",
    "orderStatus": "CANCELLED"
  }
}
```

- HTTP: `200`

#### API-UC07-004: Payment callback idempotency

- Request: gửi lại request success của `API-UC07-002` với cùng `providerTransactionId`.
- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "processed": false,
    "duplicate": true
  }
}
```

- HTTP: `200` hoặc `409`
- Side effects: Không tạo card/order/transaction trùng.

### UC08: Đăng ký và phát hành thẻ ảo

#### API-UC08-001: Issue virtual card success

- Request:

```http
POST /cards/create-virtual-card
Cookie: accessToken={{passengerAccessToken}}
```

```json
{
  "subscriptionPlanId": "MONTHLY_METRO_01",
  "paymentMethod": "WALLET"
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "cardId": "card_id",
    "cardUid": "card_uid",
    "cardMedium": "VIRTUAL",
    "status": "ACTIVE",
    "subscriptionId": "subscription_id"
  }
}
```

- HTTP: `200` hoặc `201`

#### API-UC08-002: Reject when KYC incomplete

- Request: giống `API-UC08-001`, dùng passenger chưa KYC.
- Expected response:

```json
{
  "code": 3007,
  "message": "KYC is required",
  "result": null
}
```

- HTTP: `422`

#### API-UC08-003: Reject when wallet insufficient

- Request: giống `API-UC08-001`, dùng passenger không đủ số dư.
- Expected response:

```json
{
  "code": 3008,
  "message": "Wallet balance is insufficient",
  "result": null
}
```

- HTTP: `422`

### UC09: Số hóa thẻ cứng thành thẻ ảo

#### API-UC09-001: Virtualize physical card success

- Request:

```http
POST /cards/create-virtual-cardize-card
Cookie: accessToken={{passengerAccessToken}}
```

```json
{
  "cardUid": "physical_card_uid",
  "citizenId": "079099000001"
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "physicalCardId": "physical_card_id",
    "physicalCardStatus": "VIRTUALIZED",
    "virtualCardId": "virtual_card_id",
    "virtualCardStatus": "ACTIVE"
  }
}
```

- HTTP: `200`

#### API-UC09-002: CCCD mismatch rejected

- Request: giống `API-UC09-001`, truyền `citizenId` không khớp.
- Expected response:

```json
{
  "code": 3009,
  "message": "Citizen ID does not match the physical card order",
  "result": null
}
```

- HTTP: `422`

#### API-UC09-003: Already virtualized rejected

- Request: giống `API-UC09-001`, dùng card đã `VIRTUALIZED`.
- Expected response:

```json
{
  "code": 3010,
  "message": "Physical card has already been virtualized",
  "result": null
}
```

- HTTP: `409` hoặc `422`

### UC10: Gia hạn gói vé chu kỳ

#### API-UC10-001: Renew by passenger wallet

- Request:

```http
POST /subscriptions/renew-subscription
Cookie: accessToken={{passengerAccessToken}}
```

```json
{
  "subscriptionId": "subscription_id",
  "planId": "MONTHLY_METRO_01",
  "paymentMethod": "WALLET"
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "subscriptionId": "subscription_id",
    "startDate": "2026-06-01",
    "endDate": "2026-06-30",
    "transactionId": "transaction_id"
  }
}
```

- HTTP: `200`

#### API-UC10-002: Renew by guest online payment

- Request:

```http
POST /subscriptions/guest-renew-subscription
```

```json
{
  "cardUid": "physical_card_uid",
  "planId": "MONTHLY_METRO_01",
  "paymentProvider": "VNPAY_SANDBOX"
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "paymentUrl": "https://payment-url",
    "orderId": "renew_order_id"
  }
}
```

- HTTP: `200`

#### API-UC10-003: Renew rejected when wallet insufficient

- Request: giống `API-UC10-001`, dùng ví không đủ tiền.
- Expected response:

```json
{
  "code": 3008,
  "message": "Wallet balance is insufficient",
  "result": null
}
```

- HTTP: `422`

#### API-UC10-004: Renew payment callback idempotency

- Request: gửi lại callback gia hạn với cùng `providerTransactionId`.
- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "processed": false,
    "duplicate": true
  }
}
```

- HTTP: `200` hoặc `409`
- Side effects: Subscription chỉ gia hạn một lần.

### UC11: Khởi tạo lô phôi thẻ cứng

#### API-UC11-001: Create physical card

- Request:

```http
POST /cards/create-physical-card
Cookie: accessToken={{staffAccessToken}}
```

```json
{
  "cardUid": "physical_card_uid"
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "cardId": "card_id",
    "cardUid": "physical_card_uid",
    "cardMedium": "PHYSICAL",
    "status": "ACTIVE"
  }
}
```

- HTTP: `200` hoặc `201`

#### API-UC11-002: Import physical card batch

- Request:

```http
POST /cards/import-physical-cards
Cookie: accessToken={{staffAccessToken}}
```

```json
{
  "cardUids": ["card_uid_1", "card_uid_2"]
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "createdCount": 2,
    "duplicateCount": 0,
    "duplicates": []
  }
}
```

- HTTP: `200`

#### API-UC11-003: Import forbidden for non-staff

- Request: giống `API-UC11-002`, dùng `passengerToken`.
- Expected response:

```json
{
  "code": 4003,
  "message": "Access denied",
  "result": null
}
```

- HTTP: `403`

### UC12: Thu hồi và vô hiệu hóa thẻ vật lý

#### API-UC12-001: Revoke physical card success

- Request:

```http
POST /cards/revoke-card
Cookie: accessToken={{staffAccessToken}}
```

```json
{
  "cardUid": "physical_card_uid",
  "reason": "DAMAGED"
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "cardId": "card_id",
    "status": "EXPIRED"
  }
}
```

- HTTP: `200`

#### API-UC12-002: Revoke non-active card rejected

- Request: giống `API-UC12-001`, dùng card không `ACTIVE`.
- Expected response:

```json
{
  "code": 3011,
  "message": "Card is not active",
  "result": null
}
```

- HTTP: `409` hoặc `422`

#### API-UC12-003: Revoke tenant isolation

- Request: giống `API-UC12-001`, dùng card thuộc tenant khác.
- Expected response:

```json
{
  "code": 4003,
  "message": "Access denied",
  "result": null
}
```

- HTTP: `403` hoặc `404`

## Module 3: Soát Vé & Vận Hành Quầy Ga

### UC13: Quét soát vé tự động qua Validator

#### API-UC13-001: Check-in success

- Request:

```http
POST /validator/scan-ticket
```

```json
{
  "qrPayload": "dynamic_qr_payload",
  "stationId": 1001,
  "gateId": "GATE_01",
  "scanTime": "2026-06-01T08:00:00+07:00"
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "accepted": true,
    "action": "CHECK_IN",
    "journeyId": "journey_id",
    "gateOpen": true
  }
}
```

- HTTP: `200`
- Side effects: Journey `IN_PROGRESS`, không trừ ví tại cổng.

#### API-UC13-002: Check-out success

- Request: giống `API-UC13-001`, dùng QR/card đang có journey `IN_PROGRESS` tại ga ra.
- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "accepted": true,
    "action": "CHECK_OUT",
    "journeyId": "journey_id",
    "journeyStatus": "COMPLETED",
    "gateOpen": true
  }
}
```

- HTTP: `200`

#### API-UC13-003: Invalid QR rejected

- Request: giống `API-UC13-001`, dùng `qrPayload` sai/hết hạn.
- Expected response:

```json
{
  "code": 3012,
  "message": "QR is invalid or expired",
  "result": {
    "accepted": false,
    "gateOpen": false,
    "reason": "INVALID_QR"
  }
}
```

- HTTP: `422`

#### API-UC13-004: Anti-passback rejected

- Request: quét cùng card tại cùng trạm dưới 60 giây.
- Expected response:

```json
{
  "code": 3013,
  "message": "Scan too fast. Please wait before scanning again",
  "result": {
    "accepted": false,
    "gateOpen": false,
    "reason": "ANTI_PASSBACK"
  }
}
```

- HTTP: `409` hoặc `422`

#### API-UC13-005: Locked card or over-riding rejected

- Request: quét card `LOCKED` hoặc vé lượt đi quá ga đã mua.
- Expected response:

```json
{
  "code": 3014,
  "message": "Card is locked or ticket is invalid for this exit station",
  "result": {
    "accepted": false,
    "gateOpen": false,
    "reason": "PSC_REQUIRED"
  }
}
```

- HTTP: `422`

### UC14: PSC xử lý sự cố và giải khóa thẻ kẹt ga

#### API-UC14-001: Lookup incident

- Request:

```http
GET /psc/incidents?cardUid={{cardUid}}
Cookie: accessToken={{staffAccessToken}}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "cardUid": "card_uid",
    "status": "LOCKED",
    "journeyId": "journey_id",
    "incidentType": "MISSING_CHECKOUT"
  }
}
```

- HTTP: `200`

#### API-UC14-002: Fare adjustment cash

- Request:

```http
POST /psc/fare-adjustment
Cookie: accessToken={{staffAccessToken}}
```

```json
{
  "cardUid": "card_uid",
  "journeyId": "journey_id",
  "actualExitStationId": 1005,
  "amount": 5000,
  "paymentMethod": "CASH",
  "shiftId": "shift_id",
  "reason": "OVER_RIDING"
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "journeyStatus": "COMPLETED",
    "transactionId": "transaction_id",
    "transactionType": "CASH_FARE_ADJUSTMENT"
  }
}
```

- HTTP: `200`

#### API-UC14-003: Penalty unlock

- Request:

```http
POST /psc/penalty-unlock
Cookie: accessToken={{staffAccessToken}}
```

```json
{
  "cardUid": "card_uid",
  "journeyId": "journey_id",
  "amount": 10000,
  "paymentMethod": "CASH",
  "shiftId": "shift_id",
  "reason": "TAILGATING"
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "cardStatus": "ACTIVE",
    "journeyStatus": "COMPLETED",
    "transactionType": "CASH_PENALTY"
  }
}
```

- HTTP: `200`

#### API-UC14-004: Free override unlock

- Request:

```http
POST /psc/free-unlock
Cookie: accessToken={{staffAccessToken}}
```

```json
{
  "cardUid": "card_uid",
  "journeyId": "journey_id",
  "shiftId": "shift_id",
  "reason": "SYSTEM_INCIDENT"
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "cardStatus": "ACTIVE",
    "journeyStatus": "COMPLETED",
    "amount": 0
  }
}
```

- HTTP: `200`

#### API-UC14-005: PSC action rejected when shift closed

- Request: dùng một trong các API `/psc/*`, truyền `shiftId` đã `CLOSED`.
- Expected response:

```json
{
  "code": 3015,
  "message": "Shift is closed",
  "result": null
}
```

- HTTP: `409` hoặc `422`

### UC15: In thẻ cứng và cập nhật đơn hàng

#### API-UC15-001: List printable orders

- Request:

```http
GET /orders/physical-card?status=PRINTING
Cookie: accessToken={{staffAccessToken}}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "items": [
      {
        "orderId": "order_id",
        "orderStatus": "PRINTING",
        "cardUid": "card_uid"
      }
    ]
  }
}
```

- HTTP: `200`

#### API-UC15-002: Update order status

- Request:

```http
POST /orders/update-status
Cookie: accessToken={{staffAccessToken}}
```

```json
{
  "orderId": "order_id",
  "targetStatus": "READY_FOR_PICKUP",
  "note": "Printed successfully"
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "orderId": "order_id",
    "orderStatus": "READY_FOR_PICKUP"
  }
}
```

- HTTP: `200`

#### API-UC15-003: Invalid status transition rejected

- Request: cập nhật trạng thái sai thứ tự, ví dụ `PENDING_PAYMENT` -> `COMPLETED`.
- Expected response:

```json
{
  "code": 3016,
  "message": "Invalid order status transition",
  "result": null
}
```

- HTTP: `409` hoặc `422`

### UC16: Vận hành ca trực và đối chiếu két tiền mặt

#### API-UC16-001: Open shift

- Request:

```http
POST /shifts/open-shift
Cookie: accessToken={{staffAccessToken}}
```

```json
{
  "stationId": 1001,
  "openingCashAmount": 0
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "shiftId": "shift_id",
    "status": "ACTIVE"
  }
}
```

- HTTP: `200` hoặc `201`

#### API-UC16-002: Overlapping shift rejected

- Request: mở shift mới khi staff đã có shift `ACTIVE`.
- Expected response:

```json
{
  "code": 3017,
  "message": "Staff already has an active shift",
  "result": null
}
```

- HTTP: `409`

#### API-UC16-003: Close shift

- Request:

```http
POST /shifts/close-shift
Cookie: accessToken={{staffAccessToken}}
```

```json
{
  "shiftId": "shift_id",
  "actualCashCounted": 150000,
  "discrepancyReason": "No discrepancy"
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "shiftId": "shift_id",
    "status": "CLOSED",
    "systemCashAmount": 150000,
    "actualCashCounted": 150000
  }
}
```

- HTTP: `200`

#### API-UC16-004: Close shift with discrepancy

- Request: giống `API-UC16-003`, `actualCashCounted` khác `systemCashAmount` và có `discrepancyReason`.
- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "status": "CLOSED",
    "hasDiscrepancy": true
  }
}
```

- HTTP: `200`

## Module 4: Tài Chính & Ví Điện Tử

### UC17: Quản lý ví và nạp tiền

#### API-UC17-001: Get passenger wallet

- Request:

```http
GET /wallets/me
Cookie: accessToken={{passengerAccessToken}}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "walletId": "wallet_id",
    "walletType": "PASSENGER",
    "balance": 100000,
    "status": "ACTIVE"
  }
}
```

- HTTP: `200`

#### API-UC17-002: Create top-up request

- Request:

```http
POST /wallets/create-top-up
Cookie: accessToken={{passengerAccessToken}}
```

```json
{
  "amount": 100000,
  "provider": "VNPAY_SANDBOX"
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "paymentUrl": "https://payment-url",
    "paymentRequestId": "payment_request_id"
  }
}
```

- HTTP: `200`

#### API-UC17-003: Top-up callback success

- Request:

```http
POST /wallets/top-up-callback
```

```json
{
  "provider": "VNPAY_SANDBOX",
  "providerTransactionId": "provider_txn_id",
  "walletId": "wallet_id",
  "amount": 100000,
  "status": "SUCCESS",
  "signature": "signature"
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "processed": true,
    "walletId": "wallet_id",
    "balance": 200000,
    "transactionId": "transaction_id"
  }
}
```

- HTTP: `200`

#### API-UC17-004: Top-up callback duplicate

- Request: gửi lại callback với cùng `providerTransactionId`.
- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "processed": false,
    "duplicate": true
  }
}
```

- HTTP: `200` hoặc `409`
- Side effects: Không cộng tiền lần hai.

#### API-UC17-005: Passenger withdraw rejected

- Request:

```http
POST /wallets/withdraw-wallet
Cookie: accessToken={{passengerAccessToken}}
```

```json
{
  "amount": 100000
}
```

- Expected response:

```json
{
  "code": 3004,
  "message": "Passenger wallet does not support withdrawal",
  "result": null
}
```

- HTTP: `403` hoặc `422`

### UC18: Gửi và duyệt yêu cầu giải ngân ví doanh nghiệp

#### API-UC18-001: Create payout request

- Request:

```http
POST /payouts
Cookie: accessToken={{companyManagerAccessToken}}
```

```json
{
  "operatorWalletId": "wallet_id",
  "amount": 1000000,
  "bankAccountNo": "0123456789",
  "bankName": "VCB",
  "note": "Monthly payout"
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "payoutId": "payout_id",
    "status": "PENDING"
  }
}
```

- HTTP: `200` hoặc `201`

#### API-UC18-002: Insufficient operator wallet rejected

- Request: giống `API-UC18-001`, amount lớn hơn số dư.
- Expected response:

```json
{
  "code": 3018,
  "message": "Operator wallet balance is insufficient",
  "result": null
}
```

- HTTP: `422`

#### API-UC18-003: Approve payout

- Request:

```http
POST /payouts/approve-payout
Cookie: accessToken={{platformManagerAccessToken}}
```

```json
{
  "payoutId": "payout_id",
  "note": "Approved after manual bank transfer"
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "payoutId": "payout_id",
    "status": "APPROVED",
    "transactionId": "transaction_id"
  }
}
```

- HTTP: `200`
- Notes: Không tự động chuyển khoản ngân hàng.

#### API-UC18-004: Reject payout

- Request:

```http
POST /payouts/reject-payout
Cookie: accessToken={{platformManagerAccessToken}}
```

```json
{
  "payoutId": "payout_id",
  "reason": "Invalid bank account"
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "payoutId": "payout_id",
    "status": "REJECTED"
  }
}
```

- HTTP: `200`

#### API-UC18-005: Company Manager cannot approve payout

- Request: gọi `/payouts/approve-payout` bằng `companyManagerToken`.
- Expected response:

```json
{
  "code": 4003,
  "message": "Access denied",
  "result": null
}
```

- HTTP: `403`

### UC19: Chạy đối soát và phân chia doanh thu

#### API-UC19-001: Run clearing manually

- Request:

```http
POST /clearing/run-clearing
Cookie: accessToken={{platformManagerAccessToken}}
```

```json
{
  "settlementDate": "2026-06-01",
  "rerun": false
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "settlementId": "settlement_id",
    "settlementDate": "2026-06-01",
    "processedJourneyCount": 120,
    "totalAmount": 2500000
  }
}
```

- HTTP: `200`

#### API-UC19-002: Clearing rerun idempotent

- Request: chạy lại cùng `settlementDate`.
- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "processed": false,
    "duplicate": true
  }
}
```

- HTTP: `200` hoặc `409`
- Side effects: Không cộng ví operator lần hai.

#### API-UC19-003: Get clearing reports

- Request:

```http
GET /clearing/reports?fromDate=2026-06-01&toDate=2026-06-30&operatorId=1
Cookie: accessToken={{platformManagerAccessToken}}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "items": [
      {
        "settlementId": "settlement_id",
        "operatorId": 1,
        "amount": 2500000,
        "status": "COMPLETED"
      }
    ]
  }
}
```

- HTTP: `200`

## Module 5: Quản Trị Vận Hành Đơn Vị

### UC20: Quản lý nhân viên và phân lịch ca trực

#### API-UC20-001: Create staff

- Request:

```http
POST /staff
Cookie: accessToken={{companyManagerAccessToken}}
```

```json
{
  "username": "staff01",
  "email": "staff01@example.com",
  "fullName": "Tran Van B",
  "stationId": 1001
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "accountId": "staff_account_id",
    "role": "STAFF",
    "operatorId": 1
  }
}
```

- HTTP: `200` hoặc `201`

#### API-UC20-002: Duplicate staff rejected

- Request: tạo staff trùng username/email.
- Expected response:

```json
{
  "code": 3019,
  "message": "Username or email already exists",
  "result": null
}
```

- HTTP: `409`

#### API-UC20-003: Import staff batch

- Request:

```http
POST /staff/import-staff
Cookie: accessToken={{companyManagerAccessToken}}
```

```json
{
  "items": [
    {
      "username": "staff01",
      "email": "staff01@example.com",
      "fullName": "Tran Van B",
      "stationId": 1001
    }
  ]
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "importedCount": 1,
    "failedCount": 0,
    "errors": []
  }
}
```

- HTTP: `200`

#### API-UC20-004: Assign staff shift

- Request:

```http
POST /staff/assign-shift
Cookie: accessToken={{companyManagerAccessToken}}
```

```json
{
  "staffId": "staff_account_id",
  "stationId": 1001,
  "shiftStart": "2026-06-01T08:00:00+07:00",
  "shiftEnd": "2026-06-01T16:00:00+07:00"
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "shiftId": "shift_id",
    "staffId": "staff_account_id"
  }
}
```

- HTTP: `200` hoặc `201`

#### API-UC20-005: Tenant isolation for staff management

- Request:

```http
GET /staff?operatorId={{otherOperatorId}}
Cookie: accessToken={{companyManagerAccessToken}}
```

- Expected response: không trả staff tenant khác hoặc trả `403/404`.

### UC21: Quản trị tuyến trạm và lưới nhà ga

#### API-UC21-001: Create/update route

- Request:

```http
POST /routes
Cookie: accessToken={{companyManagerAccessToken}}
```

```json
{
  "routeCode": "METRO_01",
  "routeName": "Metro Line 1",
  "transportType": "METRO"
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "routeId": 101,
    "operatorId": 1
  }
}
```

- HTTP: `200` hoặc `201`

#### API-UC21-002: Create/update station

- Request:

```http
POST /stations
Cookie: accessToken={{companyManagerAccessToken}}
```

```json
{
  "routeId": 101,
  "stationCode": "BEN_THANH",
  "stationName": "Ben Thanh",
  "stationOrder": 1
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "stationId": 1001,
    "routeId": 101
  }
}
```

- HTTP: `200` hoặc `201`

#### API-UC21-003: Invalid station order rejected

- Request:

```http
POST /stations/reorder-stations
Cookie: accessToken={{companyManagerAccessToken}}
```

```json
{
  "routeId": 101,
  "stationOrders": [
    { "stationId": 1001, "stationOrder": 1 },
    { "stationId": 1002, "stationOrder": 1 }
  ]
}
```

- Expected response:

```json
{
  "code": 3020,
  "message": "Station order is invalid",
  "result": null
}
```

- HTTP: `422`

#### API-UC21-004: Route/station import

- Request:

```http
POST /routes/import-routes
Cookie: accessToken={{companyManagerAccessToken}}
```

```json
{
  "items": [
    {
      "routeCode": "METRO_01",
      "stationCode": "BEN_THANH",
      "stationOrder": 1
    }
  ]
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "importedCount": 1,
    "failedCount": 0,
    "errors": []
  }
}
```

- HTTP: `200`

### UC22: Thiết lập cấu hình biểu giá tuyến

#### API-UC22-001: Create/update fare policy

- Request:

```http
POST /fare-policies
Cookie: accessToken={{companyManagerAccessToken}}
```

```json
{
  "policyId": "FARE_HCM_METRO_2026",
  "transportType": "METRO",
  "calculationModel": "STATION_COUNT",
  "baseFare": 8000,
  "stepFare": 1000,
  "maxFare": 20000,
  "effectiveFrom": "2026-06-01"
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "policyId": "FARE_HCM_METRO_2026",
    "cacheUpdated": true
  }
}
```

- HTTP: `200`

#### API-UC22-002: Fare policy exceeds ceiling rejected

- Request: giống `API-UC22-001`, truyền `maxFare` vượt khung trần.
- Expected response:

```json
{
  "code": 3021,
  "message": "Fare policy exceeds system ceiling",
  "result": null
}
```

- HTTP: `422`

#### API-UC22-003: Fare preview

- Request:

```http
POST /fare-policies/preview-fare
Cookie: accessToken={{companyManagerAccessToken}}
```

```json
{
  "policyId": "FARE_HCM_METRO_2026",
  "entryStationId": 1001,
  "exitStationId": 1005
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "fare": 12000,
    "currency": "VND"
  }
}
```

- HTTP: `200`

## Module 6: Quản Trị Nền Tảng

### UC23: Khởi tạo tenant và cấp tài khoản Company Manager

#### API-UC23-001: Create tenant

- Request:

```http
POST /tenants
Cookie: accessToken={{platformManagerAccessToken}}
```

```json
{
  "companyCode": "HCM_METRO",
  "companyName": "HCM Metro",
  "taxCode": "0312345678",
  "managerEmail": "manager@example.com",
  "managerUsername": "hcm_manager"
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "tenantId": "hcm-metro",
    "operatorId": 1,
    "companyManagerAccountId": "account_id",
    "operatorWalletId": "wallet_id"
  }
}
```

- HTTP: `200` hoặc `201`

#### API-UC23-002: Duplicate tenant rejected

- Request: giống `API-UC23-001`, dùng `taxCode` hoặc `managerEmail` đã tồn tại.
- Expected response:

```json
{
  "code": 3022,
  "message": "Tenant already exists",
  "result": null
}
```

- HTTP: `409`

#### API-UC23-003: List tenants

- Request:

```http
GET /tenants
Cookie: accessToken={{platformManagerAccessToken}}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "items": [
      {
        "tenantId": "hcm-metro",
        "operatorId": 1,
        "companyName": "HCM Metro",
        "status": "ACTIVE"
      }
    ]
  }
}
```

- HTTP: `200`

### UC24: Cấu hình khung giá trần toàn hệ thống

#### API-UC24-001: Update fare ceiling

- Request:

```http
POST /configs/fare-ceiling
Cookie: accessToken={{platformManagerAccessToken}}
```

```json
{
  "transportType": "METRO",
  "maxSingleJourneyFare": 20000,
  "maxMonthlyPassFare": 300000,
  "effectiveFrom": "2026-06-01"
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "configKey": "fare.ceiling.metro",
    "cacheUpdated": true
  }
}
```

- HTTP: `200`

#### API-UC24-002: Invalid fare ceiling rejected

- Request: giống `API-UC24-001`, truyền giá trị âm hoặc `0`.
- Expected response:

```json
{
  "code": 2001,
  "message": "Fare ceiling must be greater than zero",
  "result": null
}
```

- HTTP: `400`

#### API-UC24-003: Company Manager cannot update fare ceiling

- Request: gọi `/configs/fare-ceiling` bằng `companyManagerToken`.
- Expected response:

```json
{
  "code": 4003,
  "message": "Access denied",
  "result": null
}
```

- HTTP: `403`

## Module 7: Giám Sát, Bảo Mật & Phân Quyền

### UC25: Khóa và mở khóa tài khoản khẩn cấp

#### API-UC25-001: Ban account

- Request:

```http
POST /admin/ban-account
Cookie: accessToken={{adminAccessToken}}
```

```json
{
  "accountId": "account_id",
  "reason": "Fraud violation"
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "accountId": "account_id",
    "isActive": false
  }
}
```

- HTTP: `200`

#### API-UC25-002: Banned account blocked

- Request: login hoặc gọi API bằng account đã bị ban.
- Expected response:

```json
{
  "code": 4004,
  "message": "Account is locked",
  "result": null
}
```

- HTTP: `401` hoặc `403`

#### API-UC25-003: Unban account

- Request:

```http
POST /admin/unban-account
Cookie: accessToken={{adminAccessToken}}
```

```json
{
  "accountId": "account_id",
  "reason": "Resolved"
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "accountId": "account_id",
    "isActive": true
  }
}
```

- HTTP: `200`

#### API-UC25-004: Admin self-ban rejected

- Request: gọi `/admin/ban-account` với `accountId` của chính admin đang đăng nhập.
- Expected response:

```json
{
  "code": 3023,
  "message": "Admin cannot ban itself",
  "result": null
}
```

- HTTP: `422`

### UC26: Cấu hình phân quyền động

#### API-UC26-001: Update role permissions

- Request:

```http
POST /admin/rbac
Cookie: accessToken={{adminAccessToken}}
```

```json
{
  "role": "STAFF",
  "permissionsToAdd": ["CARD_REVOKE"],
  "permissionsToRemove": []
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "role": "STAFF",
    "permissions": ["CARD_REVOKE"],
    "effectiveImmediately": true
  }
}
```

- HTTP: `200`

#### API-UC26-002: Revoke permission effect

- Request: remove permission khỏi role rồi gọi API tương ứng bằng user thuộc role đó.
- Expected response:

```json
{
  "code": 4003,
  "message": "Access denied",
  "result": null
}
```

- HTTP: `403`

#### API-UC26-003: Revoke core admin permission rejected

- Request:

```http
POST /admin/rbac
Cookie: accessToken={{adminAccessToken}}
```

```json
{
  "role": "ADMIN",
  "permissionsToAdd": [],
  "permissionsToRemove": ["CONFIG_RBAC"]
}
```

- Expected response:

```json
{
  "code": 3024,
  "message": "Cannot revoke core admin permission",
  "result": null
}
```

- HTTP: `422`

### UC27: Giám sát kỹ thuật và tra cứu system logs

#### API-UC27-001: Search system logs

- Request:

```http
GET /admin/logs?from=2026-06-01T00:00:00+07:00&to=2026-06-01T23:59:59+07:00&severity=ERROR&type=SYSTEM
Cookie: accessToken={{adminAccessToken}}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "items": [
      {
        "logId": "log_id",
        "severity": "ERROR",
        "type": "SYSTEM",
        "message": "Error message"
      }
    ]
  }
}
```

- HTTP: `200`

#### API-UC27-002: Export system logs

- Request:

```http
GET /admin/export-logs?from=2026-06-01T00:00:00+07:00&to=2026-06-01T23:59:59+07:00&format=CSV
Cookie: accessToken={{adminAccessToken}}
```

- Expected response:

```text
Content-Type: text/csv hoặc application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
```

- HTTP: `200`

#### API-UC27-003: Critical incident alert

- Request:

```http
POST /admin/simulate-incident
Cookie: accessToken={{adminAccessToken}}
```

```json
{
  "severity": "CRITICAL",
  "stationId": 1001,
  "message": "Validator disconnected for more than 15 minutes"
}
```

- Expected response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "incidentId": "incident_id",
    "alertTriggered": true
  }
}
```

- HTTP: `200`

#### API-UC27-004: Non-admin cannot access system logs

- Request: gọi `/admin/logs` bằng `companyManagerToken`, `staffToken` hoặc `passengerToken`.
- Expected response:

```json
{
  "code": 4003,
  "message": "Access denied",
  "result": null
}
```

- HTTP: `403`

---

## 4. Postman Collection Order Theo Tiến Độ Code

Nên tạo folder trong Postman theo đúng thứ tự:

1. `UC01 - Passenger Phone Auth`
2. `UC02 - Internal Login`
3. `UC03 - Logout`
4. `UC04 - Change Password`
5. `UC05 - Reset Password`
6. `UC06 - Profile`
7. `UC07 - Physical Card Order`
8. `UC08 - Virtual Card`
9. `UC09 - Virtualize Card`
10. `UC10 - Renew Subscription`
11. `UC11 - Physical Card Batch`
12. `UC12 - Revoke Card`
13. `UC13 - Validator`
14. `UC14 - PSC`
15. `UC15 - Physical Card Order Processing`
16. `UC16 - Shift`
17. `UC17 - Wallet Top-up`
18. `UC18 - Payout`
19. `UC19 - Clearing`
20. `UC20 - Staff & Shift Scheduling`
21. `UC21 - Route & Station`
22. `UC22 - Fare Policy`
23. `UC23 - Tenant`
24. `UC24 - Fare Ceiling`
25. `UC25 - Ban/Unban`
26. `UC26 - RBAC`
27. `UC27 - Logs`

---

## 5. Done Criteria Cho API Test MVP

API test đạt yêu cầu khi:

1. Mỗi UC có ít nhất happy path API pass.
2. Mỗi UC có role protected đã test `401` và `403` phù hợp.
3. Mỗi UC có tenant scope đã test không truy cập dữ liệu tenant khác.
4. Mỗi UC có giao dịch tiền đã test idempotency và không đổi số dư khi lỗi.
5. Tất cả response đúng `ApiResponse<T>`.
6. Tất cả success trả `code = 1000`.
7. Tất cả error code nằm đúng range `2xxx`, `3xxx`, `4xxx`.
