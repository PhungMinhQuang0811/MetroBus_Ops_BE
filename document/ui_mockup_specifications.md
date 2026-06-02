# UI Mockup Specifications

**Dự án:** MetroBus Ticketing/AFC MVP  
**Mục đích:** Mô tả nhanh các màn hình cần có để generate UI frontend bằng v0/dev tool, bám theo 27 use case đã chốt.  
**Phạm vi:** PWA hành khách, Web Portal vận hành, Gate Validator Simulator, Admin/Platform/Company Portal.  
**Tài liệu nguồn:** `use_case_specifications.md`, `role_and_scope_analysis.md`, `SRS_MetroBusTicket.md`, `test_plan/api_test_plan.md`

---

## 1. Nguyên Tắc UI Chung

- UI phục vụ MVP phần mềm, không mô phỏng thiết bị AFC thật như TVM/Kiosk/IC Card/Bank Card/Handheld/BOS.
- Ưu tiên màn hình thao tác nghiệp vụ thật, không làm landing page marketing.
- Các màn hình quản trị cần gọn, dạng dashboard/table/form/modal, dễ scan và thao tác nhanh.
- Các màn hình passenger PWA cần mobile-first, rõ trạng thái vé/thẻ, ví, QR, lịch sử.
- Mọi action tài chính hoặc thay đổi trạng thái quan trọng phải có confirmation modal.
- Các màn hình có role khác nhau phải thể hiện rõ quyền truy cập, không hiển thị chức năng ngoài scope role.
- Multi-tenant: Company Manager và Staff chỉ nhìn thấy dữ liệu thuộc đơn vị vận hành của mình.
- Các text lỗi trong UI nên khớp với API/business error message ở mức ý nghĩa, không cần giống từng chữ.

---

## 2. Frontend Tech Stack & Design Tokens

### 2.1. Tech Stack FE

- Framework: **Next.js**.
- Language: **TypeScript**.
- Styling khuyến nghị: **Tailwind CSS**.
- Component style: dashboard/form/table/card theo hướng operational UI.
- Icon set khuyến nghị: **lucide-react**.
- Data fetching: tùy project setup, ưu tiên service layer rõ ràng gọi API backend.
- Auth FE:
  - Access token lưu bằng HTTP-only cookie do backend set.
  - FE không đọc token trực tiếp từ JavaScript.
  - UI gọi API với cookie credentials enabled.
- Form handling:
  - Có thể dùng `react-hook-form` + schema validation nếu project đã setup.
  - Nếu cần nhanh cho MVP, form state local cũng được nhưng phải có validation message rõ.

### 2.2. Visual Direction

Phong cách UI: hiện đại, sạch, vận hành đô thị, dễ dùng trong dashboard nghiệp vụ. Không dùng phong cách marketing/landing page.

Từ khóa visual:

- Urban transit
- Clean operations
- High readability
- Calm authority
- Mobile-first for passenger
- Dense but organized for admin/portal

### 2.3. Chủ Đạo Màu Sắc

Palette đề xuất:

| Token | Hex | Dùng cho |
| :--- | :--- | :--- |
| `primary` | `#0066B3` | CTA chính, active nav, link quan trọng |
| `primaryDark` | `#004F8A` | Hover/pressed state của primary |
| `secondary` | `#00A878` | Success, valid ticket, active card |
| `accent` | `#F5A524` | Warning, pending, attention |
| `danger` | `#D92D20` | Error, rejected, locked, destructive action |
| `background` | `#F6F8FB` | Page background |
| `surface` | `#FFFFFF` | Card/table/form surface |
| `border` | `#D9E2EC` | Divider, input border, table border |
| `textPrimary` | `#102A43` | Heading/body chính |
| `textSecondary` | `#52606D` | Metadata, helper text |
| `muted` | `#9FB3C8` | Placeholder, disabled |

Không dùng giao diện một màu đơn điệu. Màu xanh transit (`primary`) là chủ đạo, xanh lá dùng cho trạng thái hợp lệ, vàng cho trạng thái chờ/cảnh báo, đỏ cho lỗi/khóa/từ chối.

### 2.4. Status Color Mapping

| Status | Color |
| :--- | :--- |
| `ACTIVE`, `SUCCESS`, `COMPLETED`, `APPROVED`, `CHECK_IN`, `CHECK_OUT` | `secondary` |
| `PENDING`, `PRINTING`, `READY_FOR_PICKUP`, `SHIPPED`, `MANUAL_REVIEW` | `accent` |
| `LOCKED`, `EXPIRED`, `CANCELLED`, `FAILED`, `REJECTED`, `INVALID` | `danger` |
| `IN_PROGRESS` | `primary` |
| Disabled/empty state | `muted` |

### 2.5. Layout Tokens

- Border radius:
  - Cards/panels: `8px`
  - Buttons/inputs: `6px`
  - Modal: `8px`
- Spacing:
  - Page container: `24px` desktop, `16px` mobile
  - Section gap: `24px`
  - Form row gap: `12px`
- Table density:
  - Row height around `44px` to `52px`
  - Sticky header nếu table dài
- Typography:
  - Avoid viewport-scaled font sizes.
  - Dashboard headings compact, not hero-scale.
  - Use clear label + helper/error text for forms.

### 2.6. Next.js Page Group Gợi Ý

Gợi ý route FE:

```text
/login
/forgot-password
/reset-password

/app
/app/cards
/app/history
/app/profile
/app/qr

/guest/physical-card-order
/guest/renew-subscription

/validator

/staff
/staff/shift
/staff/psc
/staff/cards
/staff/orders

/company
/company/staff
/company/routes
/company/fare-policies
/company/payouts

/platform
/platform/tenants
/platform/clearing
/platform/payouts

/admin
/admin/accounts
/admin/rbac
/admin/logs
```

---

## 3. Actor & App Surface

| Actor | App surface | Mục tiêu UI |
| :--- | :--- | :--- |
| `GUEST` | Public Web Portal | Mua thẻ cứng, gia hạn vé bằng mã thẻ, không đăng nhập |
| `PASSENGER` | Mobile PWA | Đăng ký bằng SĐT/OTP, đăng nhập bằng SĐT/mật khẩu, quản lý hồ sơ, thẻ ảo, QR, lịch sử |
| `STAFF` | Staff Portal | Mở/kết ca, xử lý PSC, phôi thẻ, order thẻ cứng |
| `COMPANY_MANAGER` | Company Portal | Quản lý staff, ca trực, tuyến/trạm, biểu giá, payout |
| `PLATFORM_MANAGER` | Platform Portal | Tenant, clearing, payout approval |
| `ADMIN` | Admin Portal | Ban/unban, RBAC, system logs |
| `Validator` | Gate Simulator Web | Quét QR, hiển thị kết quả vào/ra, lỗi soát vé |

---

## 4. Layout & Navigation

### 3.1. Passenger PWA

- Bottom navigation:
  - `Home`
  - `Cards`
  - `History`
  - `Profile`
- Header:
  - Greeting/user name
- Notification icon
- Primary card:
  - Active virtual card
  - Subscription status
  - QR button

### 3.2. Web Portal Nội Bộ

- Sidebar navigation theo role.
- Top bar:
  - Current role
  - Operator/tenant name nếu có
  - Current shift indicator nếu là Staff
  - Account menu
- Table pages:
  - Search
  - Filter
  - Pagination
  - Row actions
- Form pages:
  - Left/main form
  - Right summary/status panel nếu cần

### 3.3. Gate Validator Simulator

- Full-screen operational layout.
- Camera/QR scan panel ở trung tâm.
- Status panel rõ màu:
  - Accepted/check-in/check-out
  - Rejected
  - PSC required
- Last scan history list.

---

## 5. Passenger PWA Screens

### PWA-01: Phone Login / Registration

**Use cases:** `UC01`

Mục tiêu: Passenger bắt đầu bằng số điện thoại; tài khoản đã tồn tại đăng nhập bằng mật khẩu, tài khoản mới xác minh OTP rồi đặt mật khẩu. OTP không được gửi cho đăng nhập thường. Link quên mật khẩu điều hướng sang luồng UC05.

UI elements:

- Phone number input: `phoneNumber`
- Button: `Continue`
- Password input for existing account
- Button: `Login`
- Link: `Forgot password`
- OTP input 6 digits for registration only
- Button: `Verify OTP`
- Password setup input
- Button: `Set password`
- Countdown timer 120s
- Error area for invalid/expired OTP

States:

- Initial phone input
- Existing account password input
- Login loading
- Registration OTP sent
- OTP verifying/loading
- OTP expired
- OTP invalid
- OTP request rate limited
- Set password
- Login success

API mapping:

- `POST /auth/phone/check`
- `POST /auth/login`
- `POST /auth/register/verify-otp`
- `POST /auth/register/set-password`

Acceptance focus:

- Existing account phone check does not send OTP and routes to password login.
- New account phone check sends registration OTP and routes to OTP verification.
- Phone check success stores `result.phoneNumber` as the normalized phone value for login identifier and OTP verification.
- Registration completes only after OTP verification and password setup.
- Forgot password link routes to UC05 and is not part of UC01 API mapping.
- OTP request rate limit displays a retry-later message and keeps the submit action disabled during the 60-second cooldown.

### PWA-02: Passenger Home Dashboard

**Use cases:** `UC06`, `UC08`, `UC10`, `UC13`

Mục tiêu: Màn hình chính cho passenger sau đăng nhập.

UI elements:

- Active card/subscription summary
- Button: `Show QR`
- Button: `Renew pass`
- Recent journeys list
- Recent transactions list
- Profile completion/KYC banner nếu chưa hoàn tất

States:

- No card yet
- Has active virtual card
- Subscription expired/expiring soon
- KYC incomplete

### PWA-03: Profile & KYC

**Use cases:** `UC04`, `UC06`

UI elements:

- Form fields:
  - Full name
  - Email
  - Date of birth
  - Citizen ID
  - Address
- Button: `Save profile`
- Email OTP verification section:
  - Email input
  - OTP input
  - Button: `Verify email`
- KYC status badge: `PENDING`, `VERIFIED`, `REJECTED`
- Account actions:
  - Change password
  - Logout

API mapping:

- `GET /user/me`
- `PUT /user/me`
- `POST /user/verify-email`
- `POST /user/change-password`

### PWA-04: Virtual Card Issue

**Use cases:** `UC08`

UI elements:

- Plan selector
- Fare/package summary
- Payment provider selector
- Button: `Issue virtual card`
- Confirmation modal
- Success state showing card UID and subscription period

States:

- KYC incomplete: disabled CTA with warning
- Payment failed/expired
- Already has active virtual card
- Success

API mapping:

- `POST /cards/create-virtual-card`

### PWA-05: Virtualize Physical Card

**Use cases:** `UC09`

UI elements:

- Card UID input
- Citizen ID input
- Button: `Virtualize card`
- Confirmation modal explaining physical card becomes unusable for conversion again
- Result card showing new virtual card

States:

- CCCD mismatch
- Already virtualized
- Success

API mapping:

- `POST /cards/virtualize-card`

### PWA-06: Renew Subscription

**Use cases:** `UC10`

UI elements:

- Current subscription card
- Plan selector
- Payment method:
  - `VNPAY_SANDBOX`
  - `SEPAY`
- Price summary
- Button: `Renew`
- Success summary with new `endDate`

States:

- Payment failed/expired
- Expired subscription
- Active subscription extended from old end date

API mapping:

- `POST /subscriptions/renew-subscription`

### PWA-08: Dynamic QR

**Use cases:** `UC13`

UI elements:

- Large QR display
- Countdown ring/progress for QR refresh
- Card status badge
- Subscription status
- Offline-ready note/status

States:

- Active QR
- Card locked
- Subscription expired
- QR refresh loading

Notes:

- UI does not expose QR algorithm/time-step config.
- QR config is backend/card data, all MVP cards use default config.

### PWA-09: Travel & Purchase History

**Use cases:** `UC10`, `UC13`, `UC18`

UI elements:

- Tabs:
  - Journeys
  - Subscriptions
  - Payment transactions
- Filters:
  - Date range
  - Status
  - Type
- Detail drawer for one item

---

## 6. Guest Web Portal Screens

### GUEST-01: Physical Card Order

**Use cases:** `UC07`

UI elements:

- Guest checkout form:
  - Full name
  - Phone
  - Email
  - Citizen ID
  - Delivery method
  - Pickup station or shipping address
- Payment provider
- Price/order summary
- Button: `Create order`

API mapping:

- `POST /orders/physical-card`
- `POST /payments/callback` for payment simulation

States:

- Pending payment
- Payment success -> `PRINTING`
- Cancelled/expired payment

### GUEST-02: Guest Renew Subscription

**Use cases:** `UC10`

UI elements:

- Card UID input
- Plan selector
- Payment provider
- Payment summary
- Button: `Renew subscription`

API mapping:

- `POST /subscriptions/guest-renew-subscription`
- `POST /payments/callback`

---

## 7. Gate Validator Simulator Screens

### VAL-01: Gate Scanner

**Use cases:** `UC13`

UI elements:

- Camera/QR upload simulator
- Station selector
- Gate ID selector
- Button: `Scan card`
- Result panel:
  - Accepted
  - Rejected
  - PSC required
- Last scans list

API mapping:

- `POST /validator/scan-ticket`

States:

- Check-in success
- Check-out success
- Invalid QR
- Anti-passback
- Locked card

Result UI:

- Green: gate open
- Red: rejected
- Amber: PSC required

---

## 8. Staff Portal Screens

### STAFF-01: Staff Login

**Use cases:** `UC02`, `UC03`, `UC04`, `UC05`

UI elements:

- Login form:
  - Username
  - Password
- Forgot password link
- Account menu:
  - Change password
  - Logout

API mapping:

- `POST /auth/login`
- `POST /auth/logout`
- `POST /user/change-password`
- `POST /auth/forgot-password`
- `POST /auth/forgot-password/request-otp`
- `POST /auth/forgot-password/verify-otp`
- `POST /auth/reset-password`

### STAFF-02: Shift Console

**Use cases:** `UC16`

UI elements:

- Current shift status
- Station selector
- Button: `Open shift`
- Button: `Close shift`
- Shift summary preview

API mapping:

- `POST /shifts/open-shift`
- `POST /shifts/close-shift`

States:

- No active shift
- Active shift
- Shift closed

### STAFF-03: PSC Incident Handling

**Use cases:** `UC14`

UI elements:

- Card UID/QR search input
- Incident detail panel:
  - Card status
  - Journey status
  - Entry station
  - Missing checkout reason
- Reason selector
- Confirmation modal

API mapping:

- `GET /psc/incidents`
- `POST /psc/unlock`

States:

- Shift not active: actions disabled
- Card locked
- Unlock requires reason

### STAFF-04: Physical Card Inventory

**Use cases:** `UC11`, `UC12`

UI elements:

- Create single physical card form
- Import batch textarea/file-like input
- Card table:
  - Card UID
  - Medium
  - Status
  - Created by
- Row action: `Revoke card`
- Revoke reason modal

API mapping:

- `POST /cards/create-physical-card`
- `POST /cards/import-physical-cards`
- `POST /cards/revoke-card`

### STAFF-05: Physical Card Order Processing

**Use cases:** `UC15`

UI elements:

- Orders table:
  - Order ID
  - Customer
  - Card UID
  - Status
  - Delivery method
- Filter by status
- Row actions:
  - Mark `READY_FOR_PICKUP`
  - Mark `SHIPPED`
  - Mark `COMPLETED`
  - Send back to `PRINTING` if print error

API mapping:

- `GET /orders/physical-card`
- `POST /orders/update-status`

---

## 9. Company Manager Portal Screens

### COMPANY-01: Staff Management

**Use cases:** `UC19`

UI elements:

- Staff table
- Create staff modal
- Import staff batch modal
- Staff detail drawer
- Shift assignment form:
  - Staff
  - Station
  - Start time
  - End time

API mapping:

- `POST /staff`
- `POST /staff/import-staff`
- `POST /staff/assign-shift`
- `GET /staff`

### COMPANY-02: Route & Station Management

**Use cases:** `UC20`

UI elements:

- Route list
- Route form
- Station table per route
- Station create/edit modal
- Reorder station screen/table
- Import route/station modal

API mapping:

- `POST /routes`
- `POST /stations`
- `POST /stations/reorder-stations`
- `POST /routes/import-routes`

### COMPANY-03: Fare Policy Management

**Use cases:** `UC21`

UI elements:

- Fare policy list
- Policy form:
  - Package code
  - Package name
  - Transport type
  - Route selector optional
  - Duration days
  - Price
  - Currency
  - Status
  - Effective from
- Package preview panel:
  - Package name
  - Duration
  - Price
- Warning if exceeds ceiling

API mapping:

- `POST /fare-policies`
- `GET /fare-policies`

### COMPANY-04: Payout Request

**Use cases:** `UC17`

UI elements:

- Operator wallet balance card
- Payout request form:
  - Amount
  - Bank account no
  - Bank name
  - Note
- Payout history table
- Status badges:
  - `PENDING`
  - `APPROVED`
  - `REJECTED`

API mapping:

- `POST /payouts`
- `GET /clearing/reports`

---

## 10. Platform Manager Portal Screens

### PLATFORM-01: Tenant Management

**Use cases:** `UC22`

UI elements:

- Tenant table
- Create tenant form:
  - Company code
  - Company name
  - Tax code
  - Manager email
  - Manager username
- Operator wallet status

API mapping:

- `POST /tenants`
- `GET /tenants`

### PLATFORM-02: Clearing Dashboard

**Use cases:** `UC18`

UI elements:

- Settlement date picker
- Button: `Run clearing`
- Summary cards:
  - Processed journeys
  - Total amount
  - Operators affected
- Settlement table
- Rerun warning modal

API mapping:

- `POST /clearing/run-clearing`
- `GET /clearing/reports`

### PLATFORM-04: Payout Approval

**Use cases:** `UC17`

UI elements:

- Payout request queue
- Request detail drawer:
  - Operator
  - Amount
  - Bank info
  - Wallet balance
- Actions:
  - Approve
  - Reject
- Approval/rejection note modal

API mapping:

- `POST /payouts/approve-payout`
- `POST /payouts/reject-payout`

---

## 11. Admin Portal Screens

### ADMIN-01: Account Ban/Unban

**Use cases:** `UC23`

UI elements:

- Account search
- Account detail card
- Status badge
- Button:
  - Ban account
  - Unban account
- Reason modal
- Audit summary

API mapping:

- `POST /admin/ban-account`
- `POST /admin/unban-account`

### ADMIN-02: Dynamic RBAC

**Use cases:** `UC24`

UI elements:

- Role selector
- Permission matrix table
- Add/remove permission controls
- Save changes button
- Warning modal for core admin permissions
- Before/after summary

API mapping:

- `POST /admin/rbac`

### ADMIN-03: System Logs

**Use cases:** `UC25`

UI elements:

- Filter bar:
  - From/to
  - Severity
  - Type
  - Keyword
- Logs table
- JSON detail drawer
- Button: `Export CSV`
- Critical alert banner
- Simulate incident button for dev/test

API mapping:

- `GET /admin/logs`
- `GET /admin/logs/export`
- `POST /admin/simulate-incident`

---

## 12. E2E Demo Screens By Flow

### Flow A: Passenger Onboarding

Screens:

1. `PWA-01 Phone Login / Registration`
2. `PWA-03 Profile & KYC`
3. `PWA-04 Virtual Card Issue`
5. `PWA-02 Home Dashboard`

### Flow B: Passenger Travel

Screens:

1. `PWA-08 Dynamic QR`
2. `VAL-01 Gate Scanner`
3. `PWA-09 Travel & Purchase History`

### Flow C: Staff PSC

Screens:

1. `STAFF-01 Staff Login`
2. `STAFF-02 Shift Console`
3. `STAFF-03 PSC Incident Handling`
4. `STAFF-02 Shift Console`

### Flow D: Company Setup

Screens:

1. `COMPANY-01 Staff Management`
2. `COMPANY-02 Route & Station Management`
3. `COMPANY-03 Fare Policy Management`

### Flow E: Platform Settlement

Screens:

1. `PLATFORM-01 Tenant Management`
2. `PLATFORM-02 Clearing Dashboard`
3. `PLATFORM-03 Payout Approval`

### Flow F: Admin Security

Screens:

1. `ADMIN-01 Account Ban/Unban`
2. `ADMIN-02 Dynamic RBAC`
3. `ADMIN-03 System Logs`

---

## 13. Suggested v0 Prompt Template

Dùng template này để generate từng screen nhanh:

```text
Build a [mobile PWA/admin dashboard] screen for MetroBus AFC MVP.
Frontend stack: Next.js + TypeScript + Tailwind CSS.
Screen ID: [SCREEN-ID]
Actor: [ACTOR]
Use cases: [UCxx]
Purpose: [one sentence]
Required UI elements:
- ...
States:
- ...
Primary actions:
- ...
Style:
- Clean operational UI
- Dense but readable forms/tables for portal screens
- Mobile-first for passenger PWA
- Primary color #0066B3, success #00A878, warning #F5A524, danger #D92D20
- Use badges for statuses and confirmation modals for destructive/financial actions
Do not add marketing landing page content.
```

---

## 14. Out Of Scope For UI MVP

- Real AFC hardware control UI.
- TVM/Kiosk UI.
- IC Card/Bank Card open-loop payment UI.
- Handheld inspector app.
- BOS enterprise console.
- Train operation planning UI.
- Public marketing landing page.
