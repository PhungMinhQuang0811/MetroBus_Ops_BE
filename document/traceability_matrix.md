# Traceability Matrix

**Dự án:** MetroBus Ticketing/AFC MVP  
**Mục đích:** Theo dõi liên kết giữa yêu cầu đề tài, use case, acceptance criteria và API test plan.  
**Tài liệu liên quan:** `SRS_MetroBusTicket.md`, `use_case_specifications.md`, `role_and_scope_analysis.md`, `test_plan/acceptance_criteria.md`, `test_plan/api_test_plan.md`

---

## 1. Quy Ước

| Ký hiệu | Ý nghĩa |
| :--- | :--- |
| `REQ-*` | Yêu cầu cấp đề tài/SRS |
| `UCxx` | Use case trong `use_case_specifications.md` |
| `AC-UCxx-*` | Acceptance criteria trong `test_plan/acceptance_criteria.md` |
| `API-UCxx-*` | API test case trong `test_plan/api_test_plan.md` |
| `P0` | Bắt buộc cho MVP |
| `P1` | Hoàn thiện trong scope, có thể làm sau P0 |

---

## 2. Traceability Theo Yêu Cầu Đề Tài

| ID | Yêu cầu | Use case cover | Acceptance criteria | API tests | Priority | Ghi chú |
| :--- | :--- | :--- | :--- | :--- | :---: | :--- |
| `REQ-01` | Quản lý tài khoản cá nhân hành khách | `UC01`, `UC03`, `UC05`, `UC06` | `AC-UC01-*`, `AC-UC03-*`, `AC-UC05-*`, `AC-UC06-*` | `API-UC01-*`, `API-UC03-*`, `API-UC05-*`, `API-UC06-*` | P0 | Phone auth, registration/reset OTP, profile, email/KYC |
| `REQ-02` | Quản lý người dùng nhân viên vận hành | `UC02`, `UC04`, `UC05`, `UC20` | `AC-UC02-*`, `AC-UC04-*`, `AC-UC05-*`, `AC-UC20-*` | `API-UC02-*`, `API-UC04-*`, `API-UC05-*`, `API-UC20-*` | P0 | Staff do Company Manager quản lý |
| `REQ-03` | Quản lý ca kíp | `UC16`, `UC20` | `AC-UC16-*`, `AC-UC20-*` | `API-UC16-*`, `API-UC20-*` | P0 | Mở ca, kết ca, phân lịch |
| `REQ-04` | Quản lý phát hành thẻ vé | `UC07`, `UC08`, `UC11`, `UC15` | `AC-UC07-*`, `AC-UC08-*`, `AC-UC11-*`, `AC-UC15-*` | `API-UC07-*`, `API-UC08-*`, `API-UC11-*`, `API-UC15-*` | P0 | Thẻ cứng, thẻ ảo, đơn hàng |
| `REQ-05` | Thu hồi, xác minh, loại bỏ vé/thẻ cũ | `UC09`, `UC12`, `UC14` | `AC-UC09-*`, `AC-UC12-*`, `AC-UC14-*` | `API-UC09-*`, `API-UC12-*`, `API-UC14-*` | P0 | Số hóa thẻ, thu hồi, giải khóa |
| `REQ-06` | Quản lý quy tắc tính tiền giá vé | `UC22`, `UC24` | `AC-UC22-*`, `AC-UC24-*` | `API-UC22-*`, `API-UC24-*` | P0 | Fare policy và khung giá trần |
| `REQ-07` | Tính toán số tiền dựa trên quãng đường/chặng | `UC13`, `UC14`, `UC22` | `AC-UC13-*`, `AC-UC14-*`, `AC-UC22-*` | `API-UC13-*`, `API-UC14-*`, `API-UC22-*` | P0 | Validator, over-riding, fare preview |
| `REQ-08` | Tính toán phân chia bù trừ doanh thu | `UC18`, `UC19` | `AC-UC18-*`, `AC-UC19-*` | `API-UC18-*`, `API-UC19-*` | P0 | Operator wallet, payout, clearing |
| `REQ-09` | Hành khách đăng ký và quản lý thẻ vé | `UC07`, `UC08`, `UC09`, `UC10` | `AC-UC07-*`, `AC-UC08-*`, `AC-UC09-*`, `AC-UC10-*` | `API-UC07-*`, `API-UC08-*`, `API-UC09-*`, `API-UC10-*` | P0 | Guest + Passenger |
| `REQ-10` | Lịch sử chuyến đi, lịch sử vé đã mua | `UC13`, `UC17`, `UC19`, `UC27` | `AC-UC13-*`, `AC-UC17-*`, `AC-UC19-*`, `AC-UC27-*` | `API-UC13-*`, `API-UC17-*`, `API-UC19-*`, `API-UC27-*` | P1 | Lịch sử qua journeys, transactions, logs |
| `REQ-11` | Quản trị tuyến, trạm, đơn vị vận hành | `UC21`, `UC23` | `AC-UC21-*`, `AC-UC23-*` | `API-UC21-*`, `API-UC23-*` | P0 | Multi-tenant route/station |
| `REQ-12` | Bảo mật, phân quyền, giám sát hệ thống | `UC25`, `UC26`, `UC27` | `AC-UC25-*`, `AC-UC26-*`, `AC-UC27-*` | `API-UC25-*`, `API-UC26-*`, `API-UC27-*` | P0 | Ban/unban, RBAC, logs |

---

## 3. Traceability Theo Use Case

| Use case | Nhóm nghiệp vụ | Yêu cầu liên quan | Acceptance criteria | API tests | E2E coverage đề xuất | Priority |
| :--- | :--- | :--- | :--- | :--- | :--- | :---: |
| `UC01` | Passenger Phone Auth | `REQ-01` | `AC-UC01-*` | `API-UC01-*` | Passenger onboarding | P0 |
| `UC02` | Internal Login | `REQ-02`, `REQ-12` | `AC-UC02-*` | `API-UC02-*` | Staff/Manager/Admin login | P0 |
| `UC03` | Logout | `REQ-01`, `REQ-02` | `AC-UC03-*` | `API-UC03-*` | Logout and token invalidation | P0 |
| `UC04` | Change Password | `REQ-02`, `REQ-12` | `AC-UC04-*` | `API-UC04-*` | Internal account security | P1 |
| `UC05` | Forgot/Reset Password | `REQ-01`, `REQ-02`, `REQ-12` | `AC-UC05-*` | `API-UC05-*` | Passenger/internal account recovery | P1 |
| `UC06` | Profile/KYC | `REQ-01` | `AC-UC06-*` | `API-UC06-*` | Passenger profile completion | P0 |
| `UC07` | Physical Card Guest Order | `REQ-04`, `REQ-09` | `AC-UC07-*` | `API-UC07-*` | Guest buys physical card | P0 |
| `UC08` | Virtual Card Issue | `REQ-04`, `REQ-09` | `AC-UC08-*` | `API-UC08-*` | Passenger issues virtual card | P0 |
| `UC09` | Physical-to-Virtual | `REQ-05`, `REQ-09` | `AC-UC09-*` | `API-UC09-*` | Passenger virtualizes physical card | P0 |
| `UC10` | Renew Subscription | `REQ-09` | `AC-UC10-*` | `API-UC10-*` | Passenger/guest renews pass | P0 |
| `UC11` | Physical Card Batch | `REQ-04` | `AC-UC11-*` | `API-UC11-*` | Staff imports card stock | P0 |
| `UC12` | Revoke Physical Card | `REQ-05` | `AC-UC12-*` | `API-UC12-*` | Staff revokes damaged card | P0 |
| `UC13` | Validator Scan | `REQ-07`, `REQ-10` | `AC-UC13-*` | `API-UC13-*` | Passenger check-in/check-out | P0 |
| `UC14` | PSC Incident Handling | `REQ-05`, `REQ-07` | `AC-UC14-*` | `API-UC14-*` | Staff resolves locked/over-riding card | P0 |
| `UC15` | Physical Card Order Processing | `REQ-04` | `AC-UC15-*` | `API-UC15-*` | Staff updates order status | P0 |
| `UC16` | Shift Reconciliation | `REQ-03` | `AC-UC16-*` | `API-UC16-*` | Staff opens/closes shift | P0 |
| `UC17` | Wallet Top-up | `REQ-10` | `AC-UC17-*` | `API-UC17-*` | Passenger top-up wallet | P0 |
| `UC18` | Operator Payout | `REQ-08` | `AC-UC18-*` | `API-UC18-*` | Company requests payout, Platform approves | P0 |
| `UC19` | Clearing Scheduler | `REQ-08`, `REQ-10` | `AC-UC19-*` | `API-UC19-*` | Platform runs clearing | P0 |
| `UC20` | Staff & Shift Scheduling | `REQ-02`, `REQ-03` | `AC-UC20-*` | `API-UC20-*` | Company creates staff and assigns shift | P0 |
| `UC21` | Route/Station Management | `REQ-11` | `AC-UC21-*` | `API-UC21-*` | Company configures route/station | P0 |
| `UC22` | Fare Policy | `REQ-06`, `REQ-07` | `AC-UC22-*` | `API-UC22-*` | Company configures fare policy | P0 |
| `UC23` | Tenant Creation | `REQ-11` | `AC-UC23-*` | `API-UC23-*` | Platform creates tenant | P0 |
| `UC24` | Fare Ceiling | `REQ-06` | `AC-UC24-*` | `API-UC24-*` | Platform sets fare ceiling | P0 |
| `UC25` | Ban/Unban | `REQ-12` | `AC-UC25-*` | `API-UC25-*` | Admin bans/unbans account | P0 |
| `UC26` | Dynamic RBAC | `REQ-12` | `AC-UC26-*` | `API-UC26-*` | Admin changes permission runtime | P0 |
| `UC27` | System Logs | `REQ-10`, `REQ-12` | `AC-UC27-*` | `API-UC27-*` | Admin searches/export logs | P1 |

---

## 4. E2E Flow Coverage

| Flow | Use cases | Acceptance criteria | API tests | Mục tiêu |
| :--- | :--- | :--- | :--- | :--- |
| `E2E-01 Passenger onboarding` | `UC01`, `UC06`, `UC17`, `UC08` | `AC-UC01-*`, `AC-UC06-*`, `AC-UC17-*`, `AC-UC08-*` | `API-UC01-*`, `API-UC06-*`, `API-UC17-*`, `API-UC08-*` | Passenger đăng ký bằng SĐT/OTP hoặc đăng nhập bằng mật khẩu, hoàn thiện hồ sơ, nạp ví, phát hành thẻ ảo |
| `E2E-02 Passenger travel` | `UC13`, `UC10` | `AC-UC13-*`, `AC-UC10-*` | `API-UC13-*`, `API-UC10-*` | Passenger gia hạn/mua vé và check-in/check-out |
| `E2E-03 PSC incident` | `UC16`, `UC14` | `AC-UC16-*`, `AC-UC14-*` | `API-UC16-*`, `API-UC14-*` | Staff mở ca, xử lý sự cố, ghi nhận tiền mặt |
| `E2E-04 Staff card operations` | `UC11`, `UC12`, `UC15` | `AC-UC11-*`, `AC-UC12-*`, `AC-UC15-*` | `API-UC11-*`, `API-UC12-*`, `API-UC15-*` | Staff quản lý phôi thẻ, thu hồi, xử lý order |
| `E2E-05 Company setup` | `UC20`, `UC21`, `UC22` | `AC-UC20-*`, `AC-UC21-*`, `AC-UC22-*` | `API-UC20-*`, `API-UC21-*`, `API-UC22-*` | Company Manager tạo staff, tuyến/trạm, biểu giá |
| `E2E-06 Platform settlement` | `UC23`, `UC24`, `UC19`, `UC18` | `AC-UC23-*`, `AC-UC24-*`, `AC-UC19-*`, `AC-UC18-*` | `API-UC23-*`, `API-UC24-*`, `API-UC19-*`, `API-UC18-*` | Platform tạo tenant, set trần giá, clearing, payout |
| `E2E-07 Admin security` | `UC25`, `UC26`, `UC27` | `AC-UC25-*`, `AC-UC26-*`, `AC-UC27-*` | `API-UC25-*`, `API-UC26-*`, `API-UC27-*` | Admin ban/unban, RBAC, logs |

---

## 5. Coverage Summary

| Nhóm | Số UC | P0/P1 chính | Ghi chú |
| :--- | :---: | :--- | :--- |
| Xác thực & tài khoản | 6 | P0/P1 | Phone auth, registration/reset OTP, profile, auth nội bộ |
| Thẻ & vé tháng | 6 | P0 | Thẻ cứng, thẻ ảo, subscription |
| Soát vé & quầy ga | 4 | P0 | Validator, PSC, shift |
| Tài chính & ví | 3 | P0 | Top-up, payout, clearing |
| Quản trị đơn vị | 3 | P0 | Staff, route/station, fare policy |
| Quản trị nền tảng | 2 | P0 | Tenant, fare ceiling |
| Bảo mật & logs | 3 | P0/P1 | Ban/unban, RBAC, logs |

---

## 6. Quy Tắc Cập Nhật

- Khi thêm hoặc sửa use case, phải cập nhật cả `acceptance_criteria.md`, `api_test_plan.md` và file traceability này.
- Khi đổi endpoint API, chỉ cần sửa cột `API tests` nếu mã test không đổi; nếu đổi mã test thì cập nhật toàn bộ dòng liên quan.
- Khi một yêu cầu chuyển khỏi scope MVP, đánh dấu rõ `Out of MVP` thay vì xóa mất dấu vết.
- Không kéo thêm scope từ tài liệu PDF tham khảo nếu không có quyết định chốt scope mới.
