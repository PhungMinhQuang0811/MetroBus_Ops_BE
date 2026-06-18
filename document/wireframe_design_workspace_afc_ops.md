# Wireframe Design Workspace - AFC Cấp 3/Cấp 4

Nguồn: `document\Use Case Spec AFC Cấp 3 và Cấp 4.md`.

Mục tiêu của file này là phác thảo đủ giao diện hoặc điểm tương tác cho UC01-UC22 trước khi review tổng thể. Các wireframe chỉ thể hiện cấu trúc, dữ liệu và hành động; chưa chốt màu sắc hay visual design.

## 1. Quy ước

| Loại | Ý nghĩa |
| --- | --- |
| Màn hình nghiệp vụ | Actor con người trực tiếp thao tác trên FE |
| Modal | Tác vụ phụ mở từ màn hình chính |
| Màn hình giám sát | Xem dữ liệu sinh bởi API/System flow |
| Không có AFC Ops UI | Chỉ là API giữa hệ thống, thiết bị hoặc Passenger App |

Ba actor nội bộ sử dụng AFC Ops:

- `OPERATOR_ADMIN`: quản lý account và audit.
- `OPERATOR_MANAGER`: quản lý dữ liệu và vận hành toàn operator.
- `STATION_OPERATOR`: theo dõi vận hành trong station được phân quyền.

## 2. Khung giao diện chung

Sau đăng nhập, các màn hình nghiệp vụ dùng chung layout header ngang. Không dùng sidebar trong phạm vi wireframe hiện tại. Tìm kiếm/lọc đặt trong từng màn hình, không đặt ô tìm kiếm nhanh ở header.

```text
┌──────────────────────────────────────────────────────────────────────┐
│ [Logo]  Navigation theo actor                              [A]       │
├──────────────────────────────────────────────────────────────────────┤
│ Breadcrumb / Tên màn hình / Hành động chính                          │
├──────────────────────────────────────────────────────────────────────┤
│ Bộ lọc / Tabs của từng màn hình                                      │
├──────────────────────────────────────────────────────────────────────┤
│ Nội dung: bảng / form / biểu đồ / chi tiết                           │
└──────────────────────────────────────────────────────────────────────┘
```

Header chỉ chứa logo, navigation theo quyền actor và avatar user. Không đặt ô tìm kiếm nhanh, không đặt thông báo ở header.

Header cho `OPERATOR_ADMIN`:

```text
┌──────────────────────────────────────────────────────────────────────┐
│ [Logo]  Tài khoản nhân sự  Audit và truy vết              [A]       │
└──────────────────────────────────────────────────────────────────────┘
```

Admin tập trung quản lý account và audit. Không mặc định hiển thị các màn vận hành như thiết bị, giao dịch, cấu hình hay lô dữ liệu nếu chưa cấp quyền nghiệp vụ.

Header cho `OPERATOR_MANAGER`:

```text
┌────────────────────────────────────────────────────────────────────────────────────┐
│ [Logo] Tổng quan  Master data ▼  Thiết bị ▼  Giao dịch  Cấu hình vận hành ▼  Quản lý lô dữ liệu  [A] │
└────────────────────────────────────────────────────────────────────────────────────┘
```

Dropdown của manager:

```text
Master data ▼
├── Tuyến
└── Ga/Trạm

Thiết bị ▼
├── Quản lý thiết bị
├── Giám sát thiết bị
└── Sự cố thiết bị

Cấu hình vận hành ▼
├── Gói cấu hình
└── Trạng thái áp dụng
```

`Giao dịch`, `Quản lý lô dữ liệu` và `Tổng quan` là item trực tiếp trên header, không có dropdown trong sitemap hiện tại.

Header cho `STATION_OPERATOR`:

```text
┌──────────────────────────────────────────────────────────────────────┐
│ [Logo]  Thiết bị ▼  Giao dịch  Cấu hình vận hành ▼        [S]       │
└──────────────────────────────────────────────────────────────────────┘
```

Dropdown của station operator:

```text
Thiết bị ▼
├── Giám sát thiết bị
└── Sự cố thiết bị

Cấu hình vận hành ▼
└── Trạng thái áp dụng
```

Station operator chỉ thấy dữ liệu trong station được phân quyền. Không hiển thị master data, quản lý lô dữ liệu hoặc audit.

Avatar `[A]` là hình tròn nhỏ chứa chữ cái đầu của username. Khi bấm mở dropdown:

```text
┌──────────────────────┐
│ username             │
├──────────────────────┤
│ Đổi mật khẩu         │
│ Logout               │
└──────────────────────┘
```

Không có khu vực thông báo trong header ở wireframe hiện tại.

## 3. Quy ước message/error

Message không đặt trong header. Mỗi loại message đặt gần nơi phát sinh để user dễ hiểu.

| Loại message | Vị trí |
| --- | --- |
| Lỗi validate field | Ngay dưới field trong modal/form |
| Lỗi tổng của modal | Đầu modal, trên các field |
| Lỗi import theo dòng | Trong bảng preview/lỗi của import modal |
| Thành công sau create/update/delete/import | Toast nhỏ góc trên bên phải hoặc message dưới page header |
| Lỗi tải bảng/list | Vùng nội dung chính của màn hình |
| Empty state | Trong vùng bảng/list |
| Confirm nguy hiểm | Confirm dialog riêng |

Toast/message sau thao tác:

```text
┌────────────────────────────────────────────┐
│ Cập nhật thành công                        │
└────────────────────────────────────────────┘
```

Ví dụ lỗi trong modal:

```text
┌────────────────────────────────────────────┐
│ Tạo tuyến                                  │
├────────────────────────────────────────────┤
│ Không thể lưu tuyến. Vui lòng kiểm tra lỗi │
│ bên dưới.                                  │
│                                            │
│ Route code [R01_______________________]    │
│ Route code đã tồn tại.                     │
└────────────────────────────────────────────┘
```

## 4. Quy ước bảng quản lý

Các màn hình quản lý dạng danh sách dùng chung cấu trúc:

```text
┌──────────────────────────────────────────────────────────────────────┐
│ Tên màn hình                                      [Import] [Thêm mới]│
├──────────────────────────────────────────────────────────────────────┤
│ Tabs: [Tất cả] [Của tôi]                                             │
├──────────────────────────────────────────────────────────────────────┤
│ Filter theo nghiệp vụ của màn hình                                   │
├──────────────────────────────────────────────────────────────────────┤
│ Data table                                                           │
├──────────────────────────────────────────────────────────────────────┤
│ Rows per page [10]        1-10 of 120           < 1 2 3 ... 12 >     │
└──────────────────────────────────────────────────────────────────────┘
```

Ý nghĩa tabs:

| Tab | Ý nghĩa |
| --- | --- |
| `Tất cả` | Hiển thị toàn bộ dữ liệu user có quyền xem |
| `Của tôi` | Hiển thị dữ liệu do user hiện tại tạo/phụ trách nếu entity có trường owner/created_by |

Nếu một entity chưa có khái niệm owner/phụ trách, tab `Của tôi` có thể tạm ẩn hoặc disabled cho tới khi BE hỗ trợ dữ liệu đó.

## 5. Wireframe theo Use Case

### UC01 - Đăng nhập

**Actor:** `OPERATOR_ADMIN`, `OPERATOR_MANAGER`, `STATION_OPERATOR`  
**Loại:** Màn hình nghiệp vụ trước xác thực  
**Phục vụ:** Xác thực username/password và điều hướng theo role.

```text
┌────────────────────────────────────────────┐
│ AFC Ops                                    │
│ Đăng nhập hệ thống vận hành                │
├────────────────────────────────────────────┤
│ Username                                   │
│ [______________________________________]   │
│ Password                                   │
│ [____________________________________] [x] │
│                                            │
│ [ Đăng nhập ]                              │
│ Quên mật khẩu?                             │
├────────────────────────────────────────────┤
│ Vùng hiển thị lỗi đăng nhập                │
└────────────────────────────────────────────┘
```

Trạng thái cần có:

- Username/password không đúng.
- Account bị khóa.
- Account chưa có role.
- Đăng nhập bằng mật khẩu tạm: chuyển sang màn hình đổi mật khẩu bắt buộc.

---

### UC02 - Quản lý tài khoản nhân sự

**Actor:** `OPERATOR_ADMIN`  
**Loại:** Màn hình nghiệp vụ + modal + import modal  
**Phục vụ:** Tạo account, ban/unban account, reset password và import account.

```text
┌──────────────────────────────────────────────────────────────────────┐
│ Tài khoản nhân sự                         [Import] [Thêm tài khoản]  │
├──────────────────────────────────────────────────────────────────────┤
│ Tabs: [Tất cả] [Của tôi]                                             │
├──────────────────────────────────────────────────────────────────────┤
│ [Tìm username] [Role] [Trạng thái] [Password status] [Lọc] [Đặt lại] │
├──────────────────────────────────────────────────────────────────────┤
│ Username | Role | Status | Password status | Created at | Updated at | Actions │
│ admin01  | ADMIN| Active | Normal          | ...        | ...        | Ban | Reset │
├──────────────────────────────────────────────────────────────────────┤
│ Rows per page [10]        1-10 of 80              < 1 2 3 ... 8 >    │
└──────────────────────────────────────────────────────────────────────┘

Modal tạo:
┌────────────────────────────────────────────┐
│ Tạo tài khoản                              │
├────────────────────────────────────────────┤
│ Error summary nếu tạo thất bại             │
│                                            │
│ Username             [_________________]   │
│ Lỗi field username nếu có                  │
│ Role                 [ Chọn role       ]   │
│ Lỗi field role nếu có                      │
│ Status               [ Active          ]   │
│ [Hủy]                         [Lưu]         │
└────────────────────────────────────────────┘

Sau khi tạo thành công:
┌────────────────────────────────────────────┐
│ Tài khoản đã được tạo                      │
├────────────────────────────────────────────┤
│ Username: operator01                       │
│ Mật khẩu tạm: A7k#29Qp                     │
│                                            │
│ Mật khẩu tạm chỉ hiển thị một lần.         │
│ User phải đổi mật khẩu sau khi đăng nhập.  │
│                                            │
│ [Copy username/password]          [Đóng]   │
└────────────────────────────────────────────┘

Định dạng copy:
username: operator01
password: A7k#29Qp

Import modal:
┌────────────────────────────────────────────┐
│ Import tài khoản                           │
├────────────────────────────────────────────┤
│ [Chọn file] [Tải template]                 │
│ Preview: validate toàn bộ file             │
│ Row | Username | Role | Kết quả            │
│ [Hủy]                  [Xác nhận import]   │
└────────────────────────────────────────────┘

Nếu có bất kỳ dòng lỗi:
┌──────────────────────────────────────────────────────────────────────┐
│ Import tài khoản không thành công                                    │
├──────────────────────────────────────────────────────────────────────┤
│ File có lỗi nên chưa có account nào được tạo.                        │
│                                                                      │
│ Row | Username | Field | Lỗi                                         │
│ 3   | op03     | role  | Role không hợp lệ                           │
│ 7   | op07     | user  | Username trùng                              │
│                                                                      │
│ [Tải báo cáo lỗi] [Chọn file khác] [Đóng]                            │
└──────────────────────────────────────────────────────────────────────┘

Kết quả sau import thành công:
┌──────────────────────────────────────────────────────────────────────┐
│ Import tài khoản hoàn tất                                            │
├──────────────────────────────────────────────────────────────────────┤
│ Tổng dòng: 10 | Tạo thành công: 10 | Lỗi: 0                           │
│                                                                      │
│ Username   | Role              | Mật khẩu tạm | Kết quả              │
│ op01       | STATION_OPERATOR  | K8p#21Qa     | Created              │
│ op02       | OPERATOR_MANAGER  | P4x#72Lm     | Created              │
│                                                                      │
│ Mật khẩu tạm chỉ hiển thị một lần cho các account tạo thành công.    │
│ [Copy danh sách username/password] [Download CSV] [Đóng]             │
└──────────────────────────────────────────────────────────────────────┘

Định dạng copy danh sách:
username: op01
password: K8p#21Qa

username: op02
password: P4x#72Lm
```

Quy tắc UI:

- Không có popup cập nhật account trong phạm vi UC02 hiện tại.
- Account sau khi tạo không sửa thông tin định danh/role/status bằng form edit.
- Cột `Actions` hiển thị theo trạng thái:
  - Account `Active`: hiển thị `Ban` và `Reset password`.
  - Account `Banned/Disabled`: hiển thị `Unban` và `Reset password`.
- `Ban/Unban` gọi API đổi trạng thái account và cần confirm dialog.
- `Reset password` mở modal reset mật khẩu tạm ở UC04.
- Không xóa cứng account đã có transaction/audit; nếu cần ngừng sử dụng thì ban/disable.
- Hiển thị lỗi username trùng, role không hợp lệ, file import lỗi.
- Modal tạo account cần có vùng lỗi tổng và lỗi theo field.
- Popup tạo chỉ chứa các field account cần khi tạo mới: username, role và status ban đầu.
- Bảng account dùng `Created at`, `Updated at`; không cần `Last login` trong wireframe UC02.
- `Password status` giúp biết account có cần xử lý mật khẩu hay không:
  - `Normal`: đang dùng mật khẩu thật, không bị bắt đổi.
  - `Must change`: đang dùng mật khẩu tạm và phải đổi sau khi login.
  - `Reset required`: admin cần reset do user báo quên hoặc mất quyền truy cập.
- Mật khẩu tạm do hệ thống tự sinh, chỉ hiển thị một lần sau khi tạo account.
- Admin bàn giao username/password tạm qua kênh ngoài hệ thống, không gửi mail/noti tự động.
- Import account vẫn dùng nguyên tắc mật khẩu tạm tự sinh và chỉ hiển thị một lần.
- Import account là all-or-nothing: nếu có bất kỳ dòng lỗi thì không tạo account nào, chỉ hiển thị/tải báo cáo lỗi.

Confirm dialog ban account:

```text
┌────────────────────────────────────────────┐
│ Ban tài khoản operator01                   │
├────────────────────────────────────────────┤
│ Tài khoản này sẽ không thể đăng nhập hệ    │
│ thống sau khi bị ban.                      │
│                                            │
│ [Hủy]                         [Ban]        │
└────────────────────────────────────────────┘
```

Confirm dialog unban account:

```text
┌────────────────────────────────────────────┐
│ Unban tài khoản operator01                 │
├────────────────────────────────────────────┤
│ Tài khoản này sẽ được phép đăng nhập lại   │
│ nếu role và mật khẩu hợp lệ.               │
│                                            │
│ [Hủy]                         [Unban]      │
└────────────────────────────────────────────┘
```

---

### UC03 - Đổi mật khẩu

**Actor:** Tất cả user nội bộ đã đăng nhập  
**Loại:** Màn hình tài khoản cá nhân  
**Phục vụ:** User tự đổi mật khẩu hoặc bắt buộc đổi sau mật khẩu tạm.

```text
┌────────────────────────────────────────────┐
│ Đổi mật khẩu                               │
├────────────────────────────────────────────┤
│ Mật khẩu hiện tại   [__________________]   │
│ Mật khẩu mới        [__________________]   │
│ Xác nhận mật khẩu   [__________________]   │
│ Điều kiện password policy                  │
│ [Hủy]                         [Cập nhật]    │
└────────────────────────────────────────────┘
```

Trạng thái cần có:

- Mật khẩu hiện tại sai.
- Mật khẩu mới không đạt policy.
- Xác nhận mật khẩu không khớp.

---

### UC04 - Quên mật khẩu

**Actor chính:** `OPERATOR_ADMIN`  
**Người yêu cầu:** User nội bộ quên mật khẩu  
**Loại:** Hướng dẫn trước login + modal reset sau login  
**Phục vụ:** Admin cấp mật khẩu tạm và buộc user đổi mật khẩu.

```text
Trước login:
┌────────────────────────────────────────────┐
│ Quên mật khẩu                              │
├────────────────────────────────────────────┤
│ Vui lòng liên hệ quản trị viên hệ thống    │
│ để được cấp mật khẩu tạm.                  │
│ [Quay lại đăng nhập]                       │
└────────────────────────────────────────────┘

Modal mở từ UC02:
┌────────────────────────────────────────────┐
│ Reset mật khẩu: operator01                 │
├────────────────────────────────────────────┤
│ Hệ thống sẽ tự sinh mật khẩu tạm mới.      │
│ User sẽ bị buộc đổi mật khẩu sau login.    │
│ [Hủy]                    [Reset mật khẩu]  │
└────────────────────────────────────────────┘

Sau khi reset thành công:
┌────────────────────────────────────────────┐
│ Mật khẩu tạm mới                           │
├────────────────────────────────────────────┤
│ Username: operator01                       │
│ Mật khẩu tạm: M9t#41Lp                     │
│                                            │
│ Mật khẩu tạm chỉ hiển thị một lần.         │
│ [Copy username/password]          [Đóng]   │
└────────────────────────────────────────────┘
```

Không có reset qua email/OTP trong phạm vi hiện tại.

---

### UC05 - Quản lý tuyến

**Actor:** `OPERATOR_MANAGER`  
**Loại:** Màn hình nghiệp vụ + modal + import modal  
**Phục vụ:** Tạo, sửa, disable và import tuyến.

```text
┌──────────────────────────────────────────────────────────────────────┐
│ Quản lý tuyến                                  [Import] [Thêm tuyến] │
├──────────────────────────────────────────────────────────────────────┤
│ Tabs: [Tất cả] [Của tôi]                                             │
├──────────────────────────────────────────────────────────────────────┤
│ [Route code/name] [Transport type] [Status] [Lọc]                    │
├──────────────────────────────────────────────────────────────────────┤
│ Route code | Route name | Transport type | Status | Created at | Updated at | Actions │
│ R01        | Metro 1    | METRO          | Active | ...        | ...        | ...       │
├──────────────────────────────────────────────────────────────────────┤
│ Rows per page [10]        1-10 of 24              < 1 2 3 >          │
└──────────────────────────────────────────────────────────────────────┘

Modal tạo/cập nhật:
┌────────────────────────────────────────────┐
│ Tạo / cập nhật tuyến                       │
├────────────────────────────────────────────┤
│ Route code       [____________________]    │
│ Route name       [____________________]    │
│ Transport type   [Chọn loại          ]    │
│ Status           [Active             ]    │
│ [Hủy]                           [Lưu]      │
└────────────────────────────────────────────┘
```

Import tuyến:

```text
┌──────────────────────────────────────────────────────────────────────┐
│ Import tuyến                                                         │
├──────────────────────────────────────────────────────────────────────┤
│ [Chọn file] [Tải template]                                           │
│                                                                      │
│ Preview: validate toàn bộ file                                       │
│ Tổng dòng: 10 | Hợp lệ: 10 | Lỗi: 0                                  │
│                                                                      │
│ Row | Route code | Route name | Transport type | Kết quả             │
│ 1   | R01        | Metro 1    | METRO          | Hợp lệ              │
│ 2   | R02        | Bus 2      | BUS            | Hợp lệ              │
│                                                                      │
│ [Hủy]                                      [Xác nhận import]         │
└──────────────────────────────────────────────────────────────────────┘
```

Nếu có dòng lỗi:

```text
┌──────────────────────────────────────────────────────────────────────┐
│ Import tuyến không thành công                                        │
├──────────────────────────────────────────────────────────────────────┤
│ File có lỗi nên chưa có tuyến nào được tạo/cập nhật.                 │
│                                                                      │
│ Row | Route code | Field | Lỗi                                       │
│ 3   | R01        | code  | Route code trùng trong operator           │
│ 5   | -          | name  | Route name bắt buộc                       │
│                                                                      │
│ [Tải báo cáo lỗi] [Chọn file khác] [Đóng]                            │
└──────────────────────────────────────────────────────────────────────┘
```

Kết quả import thành công:

```text
┌──────────────────────────────────────────────────────────────────────┐
│ Import tuyến hoàn tất                                                │
├──────────────────────────────────────────────────────────────────────┤
│ Tổng dòng: 10 | Tạo mới: 7 | Cập nhật: 3 | Lỗi: 0                    │
│                                                                      │
│ Row | Route code | Route name | Kết quả                              │
│ 1   | R01        | Metro 1    | Created                              │
│ 2   | R02        | Bus 2      | Updated                              │
│                                                                      │
│ [Đóng]                                                               │
└──────────────────────────────────────────────────────────────────────┘
```

Menu actions `...`:

```text
┌──────────────┐
│ Xem          │
│ Sửa          │
│ Xóa          │
└──────────────┘
```

Màn hình xem chi tiết:

```text
┌──────────────────────────────────────────────────────────────────────┐
│ Chi tiết tuyến R01                                                   │
├──────────────────────────────────────────────────────────────────────┤
│ Route code          R01                                              │
│ Route name          Metro 1                                          │
│ Transport type      METRO                                            │
│ Status              Active                                           │
│ Created at          ...                                              │
│ Updated at          ...                                              │
├──────────────────────────────────────────────────────────────────────┤
│ Ga/trạm thuộc tuyến (5)                                               │
│ Order | Station code | Station name | Status                         │
│ 1     | ST01         | Bến Thành    | Active                         │
│ 2     | ST02         | Nhà hát      | Active                         │
├──────────────────────────────────────────────────────────────────────┤
│ [Đóng]                                                    [Sửa]      │
└──────────────────────────────────────────────────────────────────────┘
```

Nút `Sửa` trong màn hình chi tiết mở modal cập nhật tuyến.

Actions:

- `Xem`: mở màn hình chi tiết, có thêm danh sách ga/trạm thuộc tuyến.
- `Sửa`: mở modal cập nhật route.
- `Xóa`: soft delete route bằng cách set `deleted_at`, cần confirm dialog.

Không hard delete route trong UI.

Import tuyến là all-or-nothing: nếu có bất kỳ dòng lỗi thì không tạo/cập nhật tuyến nào.

Confirm dialog xóa tuyến:

```text
┌────────────────────────────────────────────┐
│ Xóa tuyến R01                              │
├────────────────────────────────────────────┤
│ Tuyến sẽ bị ẩn khỏi danh sách sử dụng.     │
│ Hệ thống chỉ set deleted_at, không xóa     │
│ dữ liệu khỏi database.                     │
│                                            │
│ [Hủy]                         [Xóa]        │
└────────────────────────────────────────────┘
```

---

### UC06 - Quản lý ga/trạm

**Actor:** `OPERATOR_MANAGER`  
**Loại:** Màn hình nghiệp vụ + modal + import modal  
**Phục vụ:** Quản lý station thuộc route.

```text
┌──────────────────────────────────────────────────────────────────────┐
│ Quản lý ga/trạm                              [Import] [Thêm ga/trạm] │
├──────────────────────────────────────────────────────────────────────┤
│ Tabs: [Tất cả] [Của tôi]                                             │
├──────────────────────────────────────────────────────────────────────┤
│ [Station code/name] [Status] [Lọc]                                   │
├──────────────────────────────────────────────────────────────────────┤
│ Order | Station code | Station name | Status | Created at | Updated at | Actions │
│ 1     | ST01         | Bến Thành    | Active | ...        | ...        | ...     │
├──────────────────────────────────────────────────────────────────────┤
│ Rows per page [10]        1-10 of 45              < 1 2 3 4 5 >      │
└──────────────────────────────────────────────────────────────────────┘

Modal:
┌────────────────────────────────────────────┐
│ Tạo / cập nhật ga/trạm                     │
├────────────────────────────────────────────┤
│ Route             [Chọn route         ]    │
│ Station code      [___________________]    │
│ Station name      [___________________]    │
│ Station order     [___________________]    │
│ Status            [Active             ]    │
│ [Hủy]                           [Lưu]      │
└────────────────────────────────────────────┘
```

Import ga/trạm:

```text
┌──────────────────────────────────────────────────────────────────────┐
│ Import ga/trạm                                                       │
├──────────────────────────────────────────────────────────────────────┤
│ [Chọn file] [Tải template]                                           │
│                                                                      │
│ Preview: validate toàn bộ file                                       │
│ Tổng dòng: 12 | Hợp lệ: 12 | Lỗi: 0                                  │
│                                                                      │
│ Row | Station code | Station name | Order | Kết quả                  │
│ 1   | ST01         | Bến Thành    | 1     | Hợp lệ                   │
│ 2   | ST02         | Nhà hát      | 2     | Hợp lệ                   │
│                                                                      │
│ [Hủy]                                      [Xác nhận import]         │
└──────────────────────────────────────────────────────────────────────┘
```

Nếu có dòng lỗi:

```text
┌──────────────────────────────────────────────────────────────────────┐
│ Import ga/trạm không thành công                                      │
├──────────────────────────────────────────────────────────────────────┤
│ File có lỗi nên chưa có ga/trạm nào được tạo/cập nhật.               │
│                                                                      │
│ Row | Station code | Field | Lỗi                                     │
│ 4   | ST01         | code  | Station code trùng trong route          │
│ 6   | ST06         | order | Station order trùng hoặc không hợp lệ   │
│                                                                      │
│ [Tải báo cáo lỗi] [Chọn file khác] [Đóng]                            │
└──────────────────────────────────────────────────────────────────────┘
```

Kết quả import thành công:

```text
┌──────────────────────────────────────────────────────────────────────┐
│ Import ga/trạm hoàn tất                                              │
├──────────────────────────────────────────────────────────────────────┤
│ Tổng dòng: 12 | Tạo mới: 9 | Cập nhật: 3 | Lỗi: 0                    │
│                                                                      │
│ Row | Station code | Station name | Kết quả                          │
│ 1   | ST01         | Bến Thành    | Created                          │
│ 2   | ST02         | Nhà hát      | Updated                          │
│                                                                      │
│ [Đóng]                                                               │
└──────────────────────────────────────────────────────────────────────┘
```

Menu actions `...`:

```text
┌──────────────┐
│ Xem          │
│ Sửa          │
│ Xóa          │
└──────────────┘
```

Màn hình xem chi tiết:

```text
┌──────────────────────────────────────────────────────────────────────┐
│ Chi tiết ga/trạm ST01                                                │
├──────────────────────────────────────────────────────────────────────┤
│ Route             R01 - Metro 1                                      │
│ Station code      ST01                                               │
│ Station name      Bến Thành                                          │
│ Station order     1                                                  │
│ Status            Active                                             │
│ Created at        ...                                                │
│ Updated at        ...                                                │
├──────────────────────────────────────────────────────────────────────┤
│ Thiết bị tại ga/trạm                                                  │
│ Tổng: 12 | Active: 10 | Maintenance: 1 | Disabled: 1                 │
│ Device code | Device type | Direction | Status                       │
│ GATE-001    | GATE        | ENTRY     | Active                       │
│ GATE-002    | GATE        | EXIT      | Maintenance                  │
├──────────────────────────────────────────────────────────────────────┤
│ [Đóng]                                                    [Sửa]      │
└──────────────────────────────────────────────────────────────────────┘
```

Nút `Sửa` trong màn hình chi tiết mở modal cập nhật ga/trạm với cùng dữ liệu.

Station order trùng/không hợp lệ cần hiển thị lỗi tại field. Station đã có device/transaction chỉ được soft delete bằng `deleted_at` nếu nghiệp vụ cho phép.

Actions:

- `Xem`: mở màn hình chi tiết, hiển thị route và thiết bị liên quan.
- `Sửa`: mở modal cập nhật station.
- `Xóa`: soft delete station bằng cách set `deleted_at`, cần confirm dialog.

Không hard delete station trong UI.

Import ga/trạm là all-or-nothing: nếu có bất kỳ dòng lỗi thì không tạo/cập nhật ga/trạm nào.

Confirm dialog xóa ga/trạm:

```text
┌────────────────────────────────────────────┐
│ Xóa ga/trạm ST01                           │
├────────────────────────────────────────────┤
│ Ga/trạm sẽ bị ẩn khỏi danh sách sử dụng.   │
│ Hệ thống chỉ set deleted_at, không xóa     │
│ dữ liệu khỏi database.                     │
│                                            │
│ [Hủy]                         [Xóa]        │
└────────────────────────────────────────────┘
```

---

### UC07 - Quản lý danh mục thiết bị AFC

**Actor:** `OPERATOR_MANAGER`  
**Loại:** Màn hình nghiệp vụ + modal + import modal  
**Phục vụ:** Khai báo và quản trị thiết bị Cấp 2 thuộc station.

```text
┌──────────────────────────────────────────────────────────────────────┐
│ Danh mục thiết bị AFC                     [Import] [Thêm thiết bị]   │
├──────────────────────────────────────────────────────────────────────┤
│ Tabs: [Tất cả] [Của tôi]                                             │
├──────────────────────────────────────────────────────────────────────┤
│ [Device code] [Type] [Status] [Lọc]                                  │
├──────────────────────────────────────────────────────────────────────┤
│ Device code | Type | Direction | Status | Created at | Updated at | Actions │
│ GATE-001    | GATE | ENTRY     | Active | ...        | ...        | ...     │
├──────────────────────────────────────────────────────────────────────┤
│ Rows per page [10]        1-10 of 120             < 1 2 3 ... 12 >   │
└──────────────────────────────────────────────────────────────────────┘

Modal tạo/cập nhật:
┌────────────────────────────────────────────┐
│ Tạo / cập nhật thiết bị                    │
├────────────────────────────────────────────┤
│ Device code      [____________________]    │
│ Device type      [Chọn loại          ]    │
│ Direction        [Chọn hướng         ]    │
│ Station          [Chọn station       ]    │
│ Status           [Active/Maintenance ]    │
│ [Hủy]                           [Lưu]      │
└────────────────────────────────────────────┘
```

Import thiết bị:

```text
┌──────────────────────────────────────────────────────────────────────┐
│ Import thiết bị AFC                                                  │
├──────────────────────────────────────────────────────────────────────┤
│ [Chọn file] [Tải template]                                           │
│                                                                      │
│ Preview: validate toàn bộ file                                       │
│ Tổng dòng: 20 | Hợp lệ: 20 | Lỗi: 0                                  │
│                                                                      │
│ Row | Device code | Type | Direction | Station | Kết quả             │
│ 1   | GATE-001    | GATE | ENTRY     | ST01    | Hợp lệ              │
│ 2   | GATE-002    | GATE | EXIT      | ST01    | Hợp lệ              │
│                                                                      │
│ [Hủy]                                      [Xác nhận import]         │
└──────────────────────────────────────────────────────────────────────┘
```

Nếu có dòng lỗi:

```text
┌──────────────────────────────────────────────────────────────────────┐
│ Import thiết bị không thành công                                     │
├──────────────────────────────────────────────────────────────────────┤
│ File có lỗi nên chưa có thiết bị nào được tạo/cập nhật.              │
│                                                                      │
│ Row | Device code | Field | Lỗi                                      │
│ 4   | GATE-001    | code  | Device code trùng                        │
│ 8   | GATE-008    | station | Station không tồn tại                  │
│                                                                      │
│ [Tải báo cáo lỗi] [Chọn file khác] [Đóng]                            │
└──────────────────────────────────────────────────────────────────────┘
```

Kết quả import thành công:

```text
┌──────────────────────────────────────────────────────────────────────┐
│ Import thiết bị hoàn tất                                             │
├──────────────────────────────────────────────────────────────────────┤
│ Tổng dòng: 20 | Tạo mới: 18 | Cập nhật: 2 | Lỗi: 0                   │
│                                                                      │
│ Row | Device code | Type | Kết quả                                   │
│ 1   | GATE-001    | GATE | Created                                   │
│ 2   | GATE-002    | GATE | Updated                                   │
│                                                                      │
│ [Đóng]                                                               │
└──────────────────────────────────────────────────────────────────────┘
```

Menu actions `...`:

```text
┌──────────────┐
│ Xem          │
│ Sửa          │
│ Xóa          │
└──────────────┘
```

Màn hình xem chi tiết:

```text
┌──────────────────────────────────────────────────────────────────────┐
│ Chi tiết thiết bị GATE-001                                           │
├──────────────────────────────────────────────────────────────────────┤
│ Device code       GATE-001                                           │
│ Device type       GATE                                               │
│ Direction         ENTRY                                              │
│ Status            Active                                             │
│ Created at        ...                                                │
│ Updated at        ...                                                │
├──────────────────────────────────────────────────────────────────────┤
│ Ga/trạm hiện tại                                                     │
│ Station code      ST01                                               │
│ Station name      Bến Thành                                          │
│ Route             R01 - Metro 1                                      │
├──────────────────────────────────────────────────────────────────────┤
│ Vận hành gần đây                                                     │
│ Last seen         ...                                                │
│ Firmware version  ...                                                │
│ Incident gần nhất ...                                                │
├──────────────────────────────────────────────────────────────────────┤
│ [Đóng]                                                    [Sửa]      │
└──────────────────────────────────────────────────────────────────────┘
```

Actions:

- `Xem`: mở màn hình chi tiết, hiển thị station/route và vận hành gần đây.
- `Sửa`: mở modal cập nhật thiết bị.
- `Xóa`: soft delete thiết bị bằng cách set `deleted_at`, cần confirm dialog.

Confirm dialog xóa thiết bị:

```text
┌────────────────────────────────────────────┐
│ Xóa thiết bị GATE-001                      │
├────────────────────────────────────────────┤
│ Thiết bị sẽ bị ẩn khỏi danh sách sử dụng.  │
│ Hệ thống chỉ set deleted_at, không xóa     │
│ dữ liệu khỏi database.                     │
│                                            │
│ [Hủy]                         [Xóa]        │
└────────────────────────────────────────────┘
```

Import thiết bị là all-or-nothing: nếu có bất kỳ dòng lỗi thì không tạo/cập nhật thiết bị nào.

Không hard delete thiết bị trong UI.

---

### UC08 - Thiết bị gửi tín hiệu trạng thái định kỳ

**Actor:** `DEVICE_CLIENT`  
**Loại:** Không có màn hình nhập liệu AFC Ops; dữ liệu được xem ở UC09  
**Phục vụ:** C3 nhận heartbeat và cập nhật `lastSeenAt`, status, firmware.

```text
DEVICE_CLIENT             Cấp 3/Cấp 4              Dữ liệu vận hành
     │ heartbeat payload       │                           │
     ├────────────────────────>│                           │
     │                         ├─ tìm device               │
     │                         ├─ cập nhật status          │
     │                         ├─ lưu heartbeat trace ────>│
     │       accepted          │                           │
     │<────────────────────────┤                           │
```

Điểm quan sát UI:

- UC09 hiển thị status, last seen, firmware.
- Chi tiết thiết bị có tab heartbeat log nếu bật trace.
- Unknown device/payload lỗi có thể xuất hiện trong technical/integration log, không cần màn hình riêng cho MVP.

---

### UC09 - Theo dõi trạng thái hoạt động thiết bị

**Actor:** `STATION_OPERATOR`, `OPERATOR_MANAGER`  
**Loại:** Màn hình giám sát + trang hoặc modal chi tiết  
**Phục vụ:** Xem online, offline, maintenance, disabled theo phạm vi quyền.

```text
┌──────────────────────────────────────────────────────────────────────┐
│ Giám sát thiết bị                                      [Làm mới]    │
├──────────────────────────────────────────────────────────────────────┤
│ [Route] [Station] [Status] [Device type] [Lọc] [Đặt lại]             │
├──────────────────────────────────────────────────────────────────────┤
│ Online: 120 | Offline: 4 | Maintenance: 3 | Disabled: 2              │
├──────────────────────────────────────────────────────────────────────┤
│ Device code | Type | Direction | Station | Status | Last seen | Firmware | Actions │
│ GATE-001    | QR_SCANNER_SIMULATOR | ENTRY | ST01 | ACTIVE  | 10:21:05 | 1.0.7 | Xem │
│ GATE-002    | QR_SCANNER_SIMULATOR | EXIT  | ST01 | OFFLINE | 09:58:10 | 1.0.5 | Xem │
└──────────────────────────────────────────────────────────────────────┘

Chi tiết thiết bị:
┌──────────────────────────────────────────────────────────────────────┐
│ Device AFC-GATE-001 | Online                                         │
├──────────────────────────────────────────────────────────────────────┤
│ Overview                                                             │
│ Device code | Type | Direction | Status                              │
│ Station | Route | Last seen | Firmware                               │
├──────────────────────────────────────────────────────────────────────┤
│ Tabs: Heartbeat log | Incident liên quan | Transaction gần đây       │
└──────────────────────────────────────────────────────────────────────┘
```

Thông tin hiển thị chính theo schema:

| Field trên UI | Nguồn dữ liệu | Ghi chú |
| --- | --- | --- |
| `Device code` | `devices.device_code` | Định danh thiết bị |
| `Type` | `devices.device_type` | MVP: `QR_SCANNER_SIMULATOR` |
| `Direction` | `devices.direction` | `ENTRY`, `EXIT`, `BOTH` |
| `Station` | `devices.station_id` join `stations` | Hiển thị station code/name nếu cần |
| `Route` | `stations.route_id` join `routes` | Chỉ cần ở filter hoặc chi tiết |
| `Status` | `devices.status` | `ACTIVE`, `OFFLINE`, `MAINTENANCE`, `DISABLED` |
| `Last seen` | `devices.last_seen_at` | Heartbeat mới nhất |
| `Firmware` | `devices.firmware_version` | Có thể null |

Không đưa `Signal age` thành cột bắt buộc vì schema không lưu field này. Nếu cần, FE/BE có thể tính từ `now - last_seen_at` và hiển thị phụ trong chi tiết.

Tab `Heartbeat log` trong chi tiết thiết bị chỉ hiển thị nếu hệ thống lưu MongoDB `device_heartbeats`:

```text
┌──────────────────────────────────────────────────────────────────────┐
│ Heartbeat log                                                        │
├──────────────────────────────────────────────────────────────────────┤
│ Received at | Sent at | Status | Firmware | Result                  │
│ 10:21:05    | ...     | ACTIVE | 1.0.7    | Accepted                │
│ 10:20:35    | ...     | ACTIVE | 1.0.7    | Accepted                │
└──────────────────────────────────────────────────────────────────────┘
```

Tab `Incident liên quan` lấy từ MongoDB `device_incidents` nếu có:

```text
┌──────────────────────────────────────────────────────────────────────┐
│ Incident liên quan                                                   │
├──────────────────────────────────────────────────────────────────────┤
│ Time | Incident type | Severity | Status | Message                   │
│ ...  | CONNECTION    | HIGH     | Open   | Device disconnected       │
└──────────────────────────────────────────────────────────────────────┘
```

Tab `Transaction gần đây` lấy từ RDBMS `afc_transactions` theo `device_id`:

```text
┌──────────────────────────────────────────────────────────────────────┐
│ Transaction gần đây                                                  │
├──────────────────────────────────────────────────────────────────────┤
│ Time | Event ID | Tap type | Decision | Reason                       │
│ ...  | EVT-001  | TAP_IN   | DENY     | QR_EXPIRED                   │
└──────────────────────────────────────────────────────────────────────┘
```

Nút `Xem` ở bảng giám sát thiết bị mở màn hình chi tiết thiết bị. Chi tiết thiết bị là màn hình riêng vì có thông tin join từ station/route và các tab log liên quan.

Nếu thiết bị không gửi heartbeat quá ngưỡng cấu hình, UC09 hiển thị `Offline` hoặc cảnh báo `Signal stale`. Ngưỡng cụ thể cần chốt ở phần nghiệp vụ/vận hành.

`STATION_OPERATOR` chỉ thấy station được phân quyền.

---

### UC10 - Ghi nhận lượt quét tại thiết bị soát vé

**Actor:** `DEVICE_CLIENT`  
**Loại:** Không có màn hình tạo transaction AFC Ops; kết quả xem ở UC11  
**Phục vụ:** Verify QR, ghi transaction và trả quyết định cho thiết bị.

```text
Passenger App QR     C2/C3 Device          C4 Verify              Store
      │                    │                    │                    │
      │<---- scan QR ------│                    │                    │
      │                    ├─ tap event ───────>│                    │
      │                    │                    ├─ verify QR/card    │
      │                    │                    ├─ verify ticket     │
      │                    │                    ├─ write transaction>│
      │                    │<─ OPEN_GATE/DENY ──┤                    │
```

Điểm quan sát UI:

- UC11 hiển thị transaction và decision.
- Chi tiết transaction hiển thị lý do `DENY`, idempotency/conflict và raw payload ref nếu có quyền.

---

### UC11 - Tra cứu giao dịch vé

**Actor:** `STATION_OPERATOR`, `OPERATOR_MANAGER`  
**Loại:** Màn hình nghiệp vụ + chi tiết  
**Phục vụ:** Tra cứu transaction do UC10 ghi nhận.

```text
┌──────────────────────────────────────────────────────────────────────┐
│ Giao dịch vé                                                        │
├──────────────────────────────────────────────────────────────────────┤
│ Tabs: [Tất cả] [Của tôi]                                             │
├──────────────────────────────────────────────────────────────────────┤
│ [Từ ngày] [Đến ngày] [Route] [Station] [Device]                      │
│ [Tap type] [Decision] [Reason] [Sync status] [Lọc] [Đặt lại]         │
├──────────────────────────────────────────────────────────────────────┤
│ Occurred at | Event ID | Station | Device | Tap | Decision | Reason | Sync | Actions │
│ 10:21:05    | EVT-001  | ST01    | GATE-1 | IN  | DENY     | QR_EXPIRED | PENDING | Xem │
├──────────────────────────────────────────────────────────────────────┤
│ Rows per page [10]        1-10 of 320             < 1 2 3 ... 32 >   │
└──────────────────────────────────────────────────────────────────────┘

Màn hình chi tiết giao dịch vé:
┌──────────────────────────────────────────────────────────────────────┐
│ Giao dịch vé TX-001 | DENY                                           │
├──────────────────────────────────────────────────────────────────────┤
│ Overview                                                             │
│ Event ID | Occurred at | Media type | Tap type                       │
│ Decision | Reason | Sync status | Ticket processing status           │
├──────────────────────────────────────────────────────────────────────┤
│ Thiết bị / vị trí                                                     │
│ Device code | Device type | Direction | Station | Route              │
├──────────────────────────────────────────────────────────────────────┤
│ Media / sản phẩm vé                                                   │
│ Card ref | Ticket ref | Entitlement ref                              │
│ Ticket usage status / Entitlement status nếu có                      │
├──────────────────────────────────────────────────────────────────────┤
│ Batch / đồng bộ C5                                                    │
│ Batch code/ref | Sync status | Ticket processing status | Updated at │
├──────────────────────────────────────────────────────────────────────┤
│ Tabs: Raw device event | Ticket usage result | Audit liên quan        │
└──────────────────────────────────────────────────────────────────────┘
```

`STATION_OPERATOR` chỉ tra cứu giao dịch vé thuộc station được giao.

Nguồn dữ liệu chính theo schema:

| Field trên UI | Nguồn dữ liệu |
| --- | --- |
| `Event ID` | `afc_transactions.event_id` |
| `Occurred at` | `afc_transactions.occurred_at` |
| `Media type` | `afc_transactions.media_type` |
| `Tap type` | `afc_transactions.tap_type` |
| `Decision` | `afc_transactions.decision` |
| `Reason` | `afc_transactions.reason` |
| `Sync status` | `afc_transactions.sync_status` |
| `Ticket processing status` | `afc_transactions.ticket_processing_status` |
| `Batch` | `afc_transactions.batch_id` join `batches` |
| `Device` | `afc_transactions.device_id` join `devices` |
| `Station` | `afc_transactions.station_id` join `stations` |
| `Card/Ticket/Entitlement` | FK tương ứng nếu transaction map được media/product |

Tab chi tiết:

```text
Raw device event
┌──────────────────────────────────────────────────────────────────────┐
│ Lấy từ MongoDB raw_device_events theo transaction_id/event_id         │
│ Received at | Device code | Event ID                                 │
│ Payload summary hoặc JSON viewer nếu có quyền                        │
└──────────────────────────────────────────────────────────────────────┘

Ticket usage result
┌──────────────────────────────────────────────────────────────────────┐
│ Chỉ hiển thị nếu transaction có ticket_id và C5 trả kết quả           │
│ Journey ref | Ticket processing status | Received at                  │
│ Payload summary                                                       │
└──────────────────────────────────────────────────────────────────────┘

Audit liên quan
┌──────────────────────────────────────────────────────────────────────┐
│ Các log liên quan batch/sync nếu hệ thống có ghi                     │
│ Time | Action | Result | Ref                                         │
└──────────────────────────────────────────────────────────────────────┘
```

Quy tắc hiển thị:

- `decision = OPEN_GATE` thì `reason = VALID`.
- `decision = DENY` thì `reason` là lý do từ chối cụ thể như `QR_EXPIRED`, `DEVICE_DISABLED`, `MEDIA_BLACKLISTED`.
- `ticket_processing_status` chỉ có ý nghĩa khi transaction có `ticket_id`.
- Transaction dùng vé tháng qua `entitlement_id` thì `ticket_processing_status` để trống/null.
- Nút `Xem` ở bảng mở màn hình chi tiết giao dịch vé.

---

### UC12 - Ghi nhận incident thiết bị

**Actor:** `DEVICE_CLIENT`  
**Loại:** Không có màn hình nhập liệu riêng; dữ liệu được xem ở UC13  
**Phục vụ:** Nhận error/incident event và cập nhật trạng thái thiết bị nếu cần.

```text
DEVICE_CLIENT             Cấp 3/Cấp 4                 Incident store
     │ incident payload        │                            │
     ├────────────────────────>│                            │
     │                         ├─ tìm device                │
     │                         ├─ lưu incident ────────────>│
     │                         ├─ cập nhật device status    │
     │       accepted          │                            │
     │<────────────────────────┤                            │
```

Unknown device hoặc payload lỗi chỉ cần ghi log kỹ thuật/incident phù hợp.

---

### UC13 - Theo dõi incident thiết bị

**Actor:** `STATION_OPERATOR`, `OPERATOR_MANAGER`  
**Loại:** Màn hình giám sát + chi tiết  
**Phục vụ:** Xem incident theo station hoặc toàn operator.

```text
┌──────────────────────────────────────────────────────────────────────┐
│ Sự cố thiết bị                                                      │
├──────────────────────────────────────────────────────────────────────┤
│ Tabs: [Tất cả] [Của tôi]                                             │
├──────────────────────────────────────────────────────────────────────┤
│ [Từ ngày] [Đến ngày] [Station] [Device] [Severity]                   │
│ [Incident type] [Trạng thái] [Lọc] [Đặt lại]                         │
├──────────────────────────────────────────────────────────────────────┤
│ Occurred at | Station | Device | Type | Severity | Status | Resolved at | Actions │
│ 10:21:05    | ST01    | GATE-1 | CONNECTION | HIGH | Open | - | Xem   │
├──────────────────────────────────────────────────────────────────────┤
│ Rows per page [10]        1-10 of 86              < 1 2 3 ... 9 >    │
└──────────────────────────────────────────────────────────────────────┘

Màn hình chi tiết sự cố thiết bị:
┌──────────────────────────────────────────────────────────────────────┐
│ Sự cố thiết bị INC-001 | HIGH                                        │
├──────────────────────────────────────────────────────────────────────┤
│ Overview                                                             │
│ Incident type | Severity | Status | Occurred at | Received at        │
│ Resolved at | Duration                                               │
├──────────────────────────────────────────────────────────────────────┤
│ Thiết bị / vị trí                                                     │
│ Device code | Device type | Device status                            │
│ Station | Route                                                      │
├──────────────────────────────────────────────────────────────────────┤
│ Nội dung sự cố                                                        │
│ Message / payload summary                                            │
│ Raw payload nếu có quyền                                             │
├──────────────────────────────────────────────────────────────────────┤
│ [Đóng]                                                               │
└──────────────────────────────────────────────────────────────────────┘
```

Nguồn dữ liệu:

| Field trên UI | Nguồn dữ liệu |
| --- | --- |
| `Incident type` | MongoDB `device_incidents.incident_type` |
| `Severity` | MongoDB `device_incidents.severity` |
| `Occurred at` | MongoDB `device_incidents.occurred_at` |
| `Received at` | MongoDB `device_incidents.received_at` |
| `Resolved at` | MongoDB `device_incidents.resolved_at` |
| `Payload` | MongoDB `device_incidents.payload` |
| `Device` | `device_incidents.device_id/device_code`, join `devices` nếu cần |
| `Station` | `device_incidents.station_id`, join `stations` |

Quy tắc hiển thị:

- `Status` trên UI có thể suy ra từ `resolved_at`: chưa có `resolved_at` là `Open`, có `resolved_at` là `Resolved`.
- UC13 chỉ để xem/tra cứu sự cố; không tạo sự cố thủ công trên AFC Ops.
- `STATION_OPERATOR` chỉ thấy sự cố thuộc station được phân quyền.
- Nút `Xem` mở màn hình chi tiết sự cố thiết bị.

---

### UC14 - Đồng bộ Card, Ticket, Entitlement từ Cấp 5

**Actor:** `LEVEL5_SYSTEM`, System  
**Loại:** System integration; không có màn hình quản lý AFC Ops  
**Phục vụ:** C4 nhận dữ liệu card/ticket/entitlement từ C5 để phục vụ cấp QR và verify tap.

```text
LEVEL5_SYSTEM           Cấp 4 Sync Service         Read model / Redis
      │ sync payload           │                           │
      ├───────────────────────>│                           │
      │                        ├─ auth/version/idempotency │
      │                        ├─ validate payload         │
      │                        ├─ upsert ─────────────────>│
      │        result          │                           │
      │<───────────────────────┤                           │
```

Không dựng các màn `Thẻ`, `Vé lượt`, `Gói chu kỳ` trong AFC Ops MVP. AFC Ops chỉ nhận dữ liệu từ C5 qua API/service integration và lưu read model để các luồng runtime sử dụng:

- UC22 dùng read model này để cấp QR động.
- UC10 dùng read model này để verify QR/card/ticket/entitlement khi thiết bị gửi tap event.
- UC11/UC19/UC20 có thể hiển thị mã tham chiếu card/ticket/entitlement trong transaction/batch nếu transaction đã map được.
- Nếu cần debug đồng bộ C5, dùng tab `Tích hợp hệ thống` trong UC21 hoặc log kỹ thuật, không tạo màn nghiệp vụ riêng cho UC14.

Sync type tối thiểu theo schema:

- `CARD_UPSERT`
- `CARD_STATUS_CHANGED`
- `TICKET_UPSERT`
- `TICKET_STATUS_CHANGED`
- `ENTITLEMENT_UPSERT`
- `ENTITLEMENT_STATUS_CHANGED`

Quy tắc hiển thị:

- Không có menu `Dữ liệu vé` trong AFC Ops MVP.
- Không có màn danh sách/chi tiết card, ticket hoặc entitlement ở UC14.
- Không có thao tác tạo/sửa dữ liệu từ UI; dữ liệu đến từ C5/System.
- Không có tab `Của tôi` vì dữ liệu này không do user AFC Ops tạo.
- Nếu payload là card status/blacklist cần phân phối xuống Cấp 3, backend có thể sinh package `MEDIA_ACCESS_RULES` để xử lý tiếp ở UC15/UC16.

---

### UC15 - Quản lý Control Package cấu hình vận hành

**Actor:** `OPERATOR_MANAGER`  
**Loại:** Màn hình nghiệp vụ  
**Phục vụ:** Tạo và sửa control package nháp do Cấp 4 tạo, MVP chủ yếu là `DEVICE_CONFIG`.

```text
┌──────────────────────────────────────────────────────────────────────┐
│ Control Package                                      [Tạo package]  │
├──────────────────────────────────────────────────────────────────────┤
│ Tabs: [Tất cả] [Của tôi]                                             │
├──────────────────────────────────────────────────────────────────────┤
│ [Package type] [Source type] [Status] [Version] [Lọc]                │
├──────────────────────────────────────────────────────────────────────┤
│ Version | Type | Source | Status | Created by | Updated at | Actions│
│ 12      | DEVICE_CONFIG | LEVEL4_CREATED | CREATED | manager01 | ... | ... │
├──────────────────────────────────────────────────────────────────────┤
│ Rows per page [10]        1-10 of 38              < 1 2 3 4 >        │
└──────────────────────────────────────────────────────────────────────┘
```

Menu actions `...`:

```text
┌──────────────┐
│ Xem          │
│ Sửa          │  Chỉ hiện khi package còn là nháp có thể sửa
│ Phát hành    │  Chuyển sang UC16
└──────────────┘
```

Màn hình tạo/sửa package:

```text
┌──────────────────────────────────────────────────────────────────────┐
│ Tạo / sửa Control Package                                            │
├──────────────────────────────────────────────────────────────────────┤
│ Lỗi tổng nếu validate/lưu thất bại                                   │
├──────────────────────────────────────────────────────────────────────┤
│ Thông tin package                                                     │
│ Package type       [DEVICE_CONFIG                              ]     │
│ Source type        LEVEL4_CREATED                                    │
│ Version            Hệ thống tự sinh theo operator                    │
│ Status             CREATED                                           │
├──────────────────────────────────────────────────────────────────────┤
│ Payload cấu hình                                                     │
│ Form field được hiển thị theo schema của package type đã chọn         │
│ [Field 1_______________________________________________]             │
│ [Field 2_______________________________________________]             │
│ [Field 3_______________________________________________]             │
├──────────────────────────────────────────────────────────────────────┤
│ Lỗi payload nếu BE trả về                                            │
│ Path                     | Error                                     │
├──────────────────────────────────────────────────────────────────────┤
│ [Hủy]                                                       [Lưu]    │
└──────────────────────────────────────────────────────────────────────┘
```

Field hệ thống không cho user nhập:

| Field | Cách xử lý |
| --- | --- |
| `version` | BE tự sinh tăng dần theo operator |
| `sourceType` | `LEVEL4_CREATED` |
| `status` | `CREATED` sau khi tạo |
| `createdBy` | Lấy từ user đăng nhập |
| `payloadRef` | BE lưu payload vào MongoDB rồi set ref |

Không cần nút `Validate` riêng trong wireframe hiện tại. User bấm `Lưu`, BE validate payload; nếu lỗi thì UI hiển thị lỗi tổng và lỗi theo field/path ngay trên màn hình tạo/sửa package.

Không nhập JSON thủ công ở UC15. Payload được nhập bằng form field theo schema của package type.

Kết quả sau khi tạo thành công:

```text
┌────────────────────────────────────────────┐
│ Tạo package thành công                     │
├────────────────────────────────────────────┤
│ Version: 12                                │
│ Package type: DEVICE_CONFIG                │
│ Status: CREATED                            │
│                                            │
│ Package đã được tạo nhưng chưa phát hành   │
│ xuống Cấp 3. Việc phát hành nằm ở UC16.    │
│                                            │
│ [Xem chi tiết]                  [Đóng]     │
└────────────────────────────────────────────┘
```

Kết quả sau khi sửa nháp thành công:

```text
┌────────────────────────────────────────────┐
│ Cập nhật package nháp thành công           │
├────────────────────────────────────────────┤
│ Version: 12                                │
│ Status: CREATED                            │
│ Updated at: ...                            │
│ [Xem chi tiết]                  [Đóng]     │
└────────────────────────────────────────────┘
```

Màn hình chi tiết package:

```text
┌──────────────────────────────────────────────────────────────────────┐
│ Control Package v12 | DEVICE_CONFIG | CREATED                        │
├──────────────────────────────────────────────────────────────────────┤
│ Overview                                                             │
│ Version | Package type | Source type | Status                        │
│ Created by | Created at | Updated at | Published at                  │
├──────────────────────────────────────────────────────────────────────┤
│ Payload                                                              │
│ Hiển thị readonly theo form/schema của package type                   │
├──────────────────────────────────────────────────────────────────────┤
│ Trạng thái phát hành                                                  │
│ Chưa phát hành xuống station nào                                     │
├──────────────────────────────────────────────────────────────────────┤
│ [Quay lại]                         [Sửa nếu còn nháp] [Phát hành]     │
└──────────────────────────────────────────────────────────────────────┘
```

Nguồn dữ liệu chính theo schema:

| Field trên UI | Nguồn dữ liệu |
| --- | --- |
| `Version` | `control_packages.version` |
| `Package type` | `control_packages.package_type` |
| `Source type` | `control_packages.source_type` |
| `External package code` | `control_packages.external_package_code`, chỉ có nếu `LEVEL5_SYNCED` |
| `Status` | `control_packages.status` |
| `Payload ref` | `control_packages.payload_ref` trỏ MongoDB `control_package_payloads` |
| `Created by` | `control_packages.created_by_account_id` |
| `Published at` | `control_packages.published_at` |
| `Created at` | `control_packages.created_at` |
| `Updated at` | `control_packages.updated_at` |

Quy tắc hiển thị:

- UC15 chỉ tạo/sửa package do Cấp 4/operator tạo, `source_type = LEVEL4_CREATED`.
- MVP chủ yếu tạo `package_type = DEVICE_CONFIG`.
- `version` do hệ thống tự sinh tăng dần theo operator, không cho user nhập tay.
- Sau khi tạo, package ở trạng thái `CREATED`.
- Nút `Sửa` chỉ hiển thị khi đồng thời:
  - `source_type = LEVEL4_CREATED`.
  - `status = CREATED`.
  - Package chưa từng publish.
  - Package chưa có bản ghi `station_control_syncs`.
- Khi sửa, BE validate lại payload, cập nhật metadata/payload và `updated_at`.
- Package đã publish, đã có sync hoặc nhận từ C5 không được sửa; khi cần thay đổi phải tạo package/version mới.
- Package nháp quá hạn sẽ được job hệ thống dọn tự động nếu chưa từng publish và chưa có sync; mặc định lưu nháp 30 ngày.
- Card status/blacklist không tạo thủ công ở UC15; nếu có thì là package `MEDIA_ACCESS_RULES` sinh từ dữ liệu C5 và xử lý ở UC16.
- Payload lớn lưu MongoDB qua `payload_ref`; UI hiển thị readonly theo form/schema của package type.

---

### UC16 - Phát hành Control Package xuống Cấp 3

**Actor:** `OPERATOR_MANAGER`, System  
**Loại:** Màn hình nghiệp vụ riêng + dialog xác nhận  
**Phục vụ:** Chọn package và các station đích, phát hành package và tạo các bản ghi đồng bộ `PENDING`.

UC16 được mở từ action **Phát hành** tại danh sách hoặc màn chi tiết Control Package của UC15. Không dùng modal cho toàn bộ nghiệp vụ vì danh sách station có thể dài và cần hiển thị trạng thái kiểm tra của từng station.

#### Màn hình phát hành Control Package

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│ Phát hành Control Package                                      [Quay lại]  │
├─────────────────────────────────────────────────────────────────────────────┤
│ PACKAGE ĐƯỢC CHỌN                                                          │
│ Version: 12              Type: MEDIA_ACCESS_RULES                           │
│ Source: LEVEL5_SYNCED     Status: CREATED                                   │
│ Created at: ...           Published at: --                                  │
│ Tóm tắt payload: 1.250 card status rules                         [Xem chi tiết]│
├─────────────────────────────────────────────────────────────────────────────┤
│ CHỌN STATION ĐÍCH                                                          │
│ [Tìm mã/tên station] [Tuyến ▼] [Trạng thái ▼] [Lọc]                        │
│ [ ] Chọn tất cả station hợp lệ trên trang                                  │
├────┬──────────────┬────────────────────┬──────────────┬────────┬────────────┤
│ Chọn│ Mã station   │ Tên station        │ Tuyến        │ Status │ Sync hiện tại│
├────┼──────────────┼────────────────────┼──────────────┼────────┼────────────┤
│ [x]│ ST-BT         │ Bến Thành          │ Metro 1      │ ACTIVE │ Chưa có     │
│ [x]│ ST-SG         │ Nhà hát Thành phố  │ Metro 1      │ ACTIVE │ FAILED      │
│ [ ]│ ST-OP         │ Ga Opera           │ Metro 2      │ DISABLED│ Chưa có    │
│ [ ]│ ST-TD         │ Thủ Đức            │ Metro 1      │ ACTIVE │ PENDING     │
├────┴──────────────┴────────────────────┴──────────────┴────────┴────────────┤
│                         < 1  2  3 ... >                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│ Đã chọn: 2 station     Có thể phát hành: 2     Không hợp lệ: 0              │
│                                               [Hủy] [Tiếp tục phát hành]    │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Quy tắc chọn station:**

- Chỉ station `ACTIVE` mới được chọn.
- Cột **Sync hiện tại** hiển thị trạng thái của chính package đang chọn tại station: `Chưa có`, `PENDING`, `APPLIED` hoặc `FAILED`.
- Station đã có sync `PENDING` hoặc `APPLIED` không được chọn để tạo bản ghi trùng.
- Station có sync `FAILED` vẫn không tạo bản ghi mới vì unique `(station_id, control_package_id)`; việc retry thuộc luồng xử lý sync, không phải publish lại.
- Có thể lọc theo tuyến để chọn một nhóm station, nhưng request cuối cùng vẫn gửi danh sách `stationIds`.
- Thông tin trên màn chọn giúp actor kiểm tra trước. Khi xác nhận, backend vẫn phải validate lại package và toàn bộ station vì trạng thái có thể đã thay đổi.

#### Dialog xác nhận phát hành

```text
┌──────────────────────────────────────────────────────────────┐
│ Xác nhận phát hành Control Package                           │
├──────────────────────────────────────────────────────────────┤
│ Package version 12 - MEDIA_ACCESS_RULES                      │
│ Phát hành tới 2 station hợp lệ.                              │
│                                                              │
│ Sau khi phát hành:                                           │
│ - Package chuyển sang PUBLISHED nếu đây là lần đầu publish.  │
│ - Mỗi station được tạo một sync có trạng thái PENDING.       │
│ - Payload package không thể sửa sau khi phát hành.           │
│                                                              │
│ [Hủy]                                      [Xác nhận phát hành]│
└──────────────────────────────────────────────────────────────┘
```

Nút **Xác nhận phát hành** gọi:

```text
POST /afc-ops/publish-control-package/{packageId}
Body: { "stationIds": [1, 2, 3] }
```

Trong lúc gọi API, khóa nút xác nhận để tránh gửi lặp. Không coi kiểm tra ở FE là kết quả cuối cùng.

#### Kết quả phát hành

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│ Đã phát hành Control Package                                                │
├─────────────────────────────────────────────────────────────────────────────┤
│ Package version 12     Status: PUBLISHED     Station đã tạo sync: 2         │
├──────────────┬────────────────────┬─────────────┬───────────────────────────┤
│ Mã station   │ Tên station        │ Sync status │ Kết quả                   │
├──────────────┼────────────────────┼─────────────┼───────────────────────────┤
│ ST-BT        │ Bến Thành          │ PENDING     │ Đã chờ station nhận       │
│ ST-SG        │ Nhà hát Thành phố  │ PENDING     │ Đã chờ station nhận       │
└──────────────┴────────────────────┴─────────────┴───────────────────────────┘
│ [Về danh sách package]                         [Xem trạng thái đồng bộ]     │
└─────────────────────────────────────────────────────────────────────────────┘
```

Nếu backend trả lỗi:

- `UC16-E01 - PACKAGE_NOT_READY`: hiển thị message lỗi trên đầu màn hình và không tạo sync.
- `UC16-E02 - Station không tồn tại hoặc disabled`: hiển thị station lỗi để actor bỏ khỏi lựa chọn rồi thử lại.
- `UC16-E03 - Đã có sync cùng package và station`: không tạo trùng; hiển thị trạng thái sync hiện tại.
- Với request gồm nhiều station, API contract cần chốt rõ xử lý **all-or-nothing** hay **thành công từng station**. Wireframe kết quả hỗ trợ hiển thị kết quả từng station; phương án này phù hợp hơn khi phát hành số lượng lớn.

#### Dữ liệu và trạng thái hiển thị

| Khu vực | Nguồn dữ liệu |
| --- | --- |
| Thông tin package | `control_packages`: `version`, `package_type`, `source_type`, `status`, `published_at`, `created_at` |
| Danh sách station | `stations`: mã, tên, tuyến và trạng thái active/disabled |
| Sync hiện tại/kết quả phát hành | `station_control_syncs`: `sync_status`, `retry_count`, `last_attempt_at`, `applied_at`, `error_message`, `updated_at` |

UC16 chỉ tạo sync `PENDING` và phát hành package. Việc station pull, apply, trả `APPLIED`/`FAILED` và theo dõi retry thuộc UC17.

---

### UC17 - Cấp 3 nhận và áp dụng Control Package

**Actor:** `STATION_OPERATOR`, System  
**Loại:** System/API flow + màn hình xem trạng thái áp dụng  
**Phục vụ:** Theo dõi station đã pull/apply package thành công hay thất bại.

UC17 không phải màn hình thao tác phát hành. Station/Cấp 3 tự pull package, apply local rồi gửi ack `APPLIED` hoặc `FAILED`. AFC Ops chỉ cần màn hình xem trạng thái để người vận hành biết gói cấu hình đã xuống station nào.

```text
Cấp 3 Station Agent        Cấp 4 Package Service          Sync status
        │ pull latest              │                           │
        ├─────────────────────────>│                           │
        │<──────── package ────────┤                           │
        ├─ apply local             │                           │
        ├──────── APPLIED/FAILED ─>│                           │
        │                          ├─ update status ──────────>│

Màn hình trạng thái:
┌────────────────────────────────────────────────────────────────────────────────────────────┐
│ Trạng thái áp dụng gói cấu hình                                                           │
├────────────────────────────────────────────────────────────────────────────────────────────┤
│ Tabs: [Tất cả] [Station của tôi]                                                          │
├────────────────────────────────────────────────────────────────────────────────────────────┤
│ [Package type] [Version] [Station] [Status] [Tuyến] [Lọc]                                 │
├──────────┬────────────────────┬─────────┬─────────┬───────┬──────────────┬──────────────┤
│ Station  │ Package type       │ Version │ Status  │ Retry │ Last attempt │ Applied at   │
├──────────┼────────────────────┼─────────┼─────────┼───────┼──────────────┼──────────────┤
│ ST-BT    │ DEVICE_CONFIG      │ 12      │ APPLIED │ 0     │ 2026-06-07   │ 2026-06-07   │
│ ST-SG    │ MEDIA_ACCESS_RULES │ 15      │ FAILED  │ 2     │ 2026-06-07   │ --           │
├──────────┴────────────────────┴─────────┴─────────┴───────┴──────────────┴──────────────┤
│ Updated at     │ Error summary                       │ Actions                            │
├────────────────┼─────────────────────────────────────┼────────────────────────────────────┤
│ 2026-06-07     │ --                                  │ [Xem chi tiết]                     │
│ 2026-06-07     │ Không đọc được payload_ref           │ [Xem chi tiết]                     │
├────────────────┴─────────────────────────────────────┴────────────────────────────────────┤
│ Rows per page [10]        1-10 of 120                         < 1 2 3 ... 12 >            │
└────────────────────────────────────────────────────────────────────────────────────────────┘
```

Action duy nhất trong bảng là `Xem chi tiết`. UC17 không có action sửa, publish hay retry thủ công.

Màn hình chi tiết trạng thái áp dụng:

```text
┌──────────────────────────────────────────────────────────────────────┐
│ Chi tiết áp dụng gói cấu hình                              [Quay lại]│
├──────────────────────────────────────────────────────────────────────┤
│ Thông tin station                                                    │
│ Mã station: ST-SG        Tên station: Nhà hát Thành phố              │
│ Tuyến: Metro 1           Trạng thái station: ACTIVE                  │
├──────────────────────────────────────────────────────────────────────┤
│ Thông tin gói cấu hình                                               │
│ Package ID: 100          Version: 15                                 │
│ Type: MEDIA_ACCESS_RULES Source: LEVEL5_SYNCED                       │
│ Package status: PUBLISHED Published at: 2026-06-07 09:00             │
├──────────────────────────────────────────────────────────────────────┤
│ Thông tin sync                                                       │
│ Sync status: FAILED      Retry count: 2                              │
│ Last attempt at: 2026-06-07 09:10                                    │
│ Applied at: --                                                       │
│ Created at: 2026-06-07 09:00                                         │
│ Updated at: 2026-06-07 09:10                                         │
├──────────────────────────────────────────────────────────────────────┤
│ Lỗi áp dụng gần nhất                                                 │
│ Không đọc được payload_ref từ MongoDB                                │
├──────────────────────────────────────────────────────────────────────┤
│ [Quay lại]                                      [Xem gói cấu hình]   │
└──────────────────────────────────────────────────────────────────────┘
```

Hiển thị trạng thái:

| Status | Ý nghĩa trên màn hình |
| --- | --- |
| `PENDING` | Station chưa pull, chưa apply hoặc đang chờ ack |
| `APPLIED` | Station đã áp dụng thành công package |
| `FAILED` | Station pull/apply lỗi, xem `error_message` để biết nguyên nhân |

Quyền xem:

- `OPERATOR_MANAGER`: xem toàn bộ station trong operator.
- `STATION_OPERATOR`: chỉ thấy tab `Station của tôi` hoặc dữ liệu station được phân quyền.

UC17 hiện chỉ là màn xem. Không có nút `Phát hành` vì phát hành thuộc UC16. Không có nút sửa payload vì package đã publish là bất biến. Nếu cần retry, sẽ chốt sau là system tự retry hay có thao tác thủ công; tạm thời không đưa action retry vào wireframe UC17.

---

### UC18 - Dashboard vận hành Cấp 4

**Actor:** `OPERATOR_MANAGER`  
**Loại:** Màn hình tổng quan  
**Phục vụ:** Tổng hợp transaction, decision, thiết bị, incident, batch và control sync.

UC18 là màn xem nhanh tình trạng vận hành. Không tạo/sửa dữ liệu tại dashboard; các nút trong từng khối chỉ điều hướng sang màn chi tiết đã có.

```text
┌────────────────────────────────────────────────────────────────────────────────────┐
│ Dashboard vận hành Cấp 4                                      [Làm mới]           │
├────────────────────────────────────────────────────────────────────────────────────┤
│ Bộ lọc                                                                             │
│ [Từ ngày/giờ] [Đến ngày/giờ] [Tuyến] [Station] [Áp dụng] [Đặt lại]                │
├────────────────────────────────────────────────────────────────────────────────────┤
│ Chỉ số tổng quan                                                                   │
│ Giao dịch vé: 24,530 | Mở cổng: 23,980 | Từ chối: 530 | Tỷ lệ từ chối: 2.1%       │
│ Thiết bị offline: 4 | Sự cố mở: 3 | Batch chờ gửi: 2 | Gói cấu hình lỗi: 1        │
├────────────────────────────────────────┬───────────────────────────────────────────┤
│ Giao dịch vé theo thời gian            │ Quyết định xử lý                         │
│ Biểu đồ line/bar theo giờ hoặc ngày    │ OPEN_GATE / DENY / ACCEPTED_FOR_FORWARDING│
│ [Xem giao dịch vé]                     │ [Xem giao dịch bị từ chối]               │
├────────────────────────────────────────┼───────────────────────────────────────────┤
│ Giao dịch theo tuyến/station           │ Thiết bị theo trạng thái                 │
│ Route/Station | Total | Open | Deny    │ Active | Maintenance | Disabled | Offline│
│ Metro 1/ST-BT | 8,120 | 7,980 | 140    │ [Xem giám sát thiết bị]                  │
├────────────────────────────────────────┼───────────────────────────────────────────┤
│ Sự cố thiết bị gần đây                 │ Cấu hình vận hành                        │
│ Time | Station | Device | Severity     │ Pending | Applied | Failed               │
│ [Xem sự cố thiết bị]                   │ [Xem trạng thái áp dụng]                 │
├────────────────────────────────────────┼───────────────────────────────────────────┤
│ Batch dữ liệu                          │ Cảnh báo cần chú ý                       │
│ Created | Submitted | Accepted | Failed│ Thiết bị offline lâu, batch failed,      │
│ [Xem batch]                            │ package apply failed                     │
└────────────────────────────────────────┴───────────────────────────────────────────┘
```

#### Thành phần trên dashboard

| Khu vực | Hiển thị | Nguồn dữ liệu chính | Link đi tiếp |
| --- | --- | --- | --- |
| Chỉ số tổng quan | Tổng giao dịch vé, tổng mở cổng, tổng từ chối, thiết bị offline, sự cố mở, batch chờ gửi, gói cấu hình lỗi | `afc_transactions`, `devices`, `device_incidents`, `batches`, `station_control_syncs` | Không bắt buộc |
| Giao dịch vé theo thời gian | Số transaction theo giờ/ngày trong khoảng lọc | `afc_transactions.occurred_at` | `Giao dịch vé` UC11 |
| Quyết định xử lý | Tỷ lệ `OPEN_GATE`, `DENY`, `ACCEPTED_FOR_FORWARDING` | `afc_transactions.decision` | UC11 với filter decision |
| Giao dịch theo tuyến/station | Bảng top route/station theo tổng lượt và số lượt bị từ chối | `afc_transactions.route_id`, `station_id`, `decision` | UC11 với filter route/station |
| Thiết bị theo trạng thái | Số thiết bị theo `status` và số thiết bị offline tính từ `last_seen_at` | `devices.status`, `devices.last_seen_at` | `Giám sát thiết bị` UC09 |
| Sự cố thiết bị gần đây | Incident mới nhất, severity, station/device, trạng thái mở/đã xử lý | Mongo `device_incidents` | `Sự cố thiết bị` UC13 |
| Cấu hình vận hành | Số sync `PENDING`, `APPLIED`, `FAILED` của gói cấu hình trong khoảng lọc | `station_control_syncs.sync_status` | `Trạng thái áp dụng` UC17 |
| Batch dữ liệu | Số batch theo `CREATED`, `SUBMITTED`, `ACCEPTED`, `REJECTED`, `FAILED` | `batches.status` | UC19/UC20 khi review xong nhóm batch |
| Cảnh báo cần chú ý | Danh sách rút gọn các vấn đề ưu tiên cao | Tổng hợp từ device, incident, batch, sync | Link theo từng cảnh báo |

#### Quy tắc hiển thị

- Khoảng thời gian mặc định: hôm nay hoặc 24 giờ gần nhất.
- Nếu khoảng thời gian quá rộng, yêu cầu user thu hẹp theo UC18-E02.
- Nếu không có dữ liệu trong khoảng lọc, hiển thị số 0 và empty state ngắn trong từng khối, không để màn hình trắng.
- Nút `Làm mới` tải lại dữ liệu dashboard, không tạo dữ liệu mới.
- `OPERATOR_MANAGER` xem toàn operator. Nếu sau này cấp dashboard cho `OPERATOR_ADMIN`, chỉ nên cho xem tổng quan/audit tùy quyền, không mặc định có quyền thao tác vận hành.
- `STATION_OPERATOR` không phải actor chính của UC18; nếu cần dashboard station riêng thì nên tách scope sau.

---

### UC19 - Tạo Batch dữ liệu

**Actor:** `OPERATOR_MANAGER`  
**Loại:** Màn hình nghiệp vụ + modal tạo batch  
**Phục vụ:** Gom transaction đủ điều kiện thành batch `CREATED`.

Tên hiển thị trên UI nên dùng `Lô dữ liệu đối soát` thay vì `Batch`. `Batch` chỉ giữ trong API/schema để map với bảng `batches`.

```text
┌────────────────────────────────────────────────────────────────────────────────────┐
│ Lô dữ liệu đối soát                                      [Tạo lô dữ liệu]          │
├────────────────────────────────────────────────────────────────────────────────────┤
│ Tabs: [Tất cả] [Của tôi]                                                          │
├────────────────────────────────────────────────────────────────────────────────────┤
│ [Từ ngày/giờ] [Đến ngày/giờ] [Trạng thái] [Mã lô] [Lọc] [Đặt lại]                │
├────────────────┬──────────────────────┬──────────────┬────────────┬──────────────┤
│ Mã lô          │ Khoảng dữ liệu        │ Số giao dịch │ Trạng thái │ Thời gian tạo│
├────────────────┼──────────────────────┼──────────────┼────────────┼──────────────┤
│ OP01-20260604-1│ 2026-06-04 00:00-23:59│ 1,500        │ CREATED    │ 2026-06-04   │
│ OP01-20260603-1│ 2026-06-03 00:00-23:59│ 1,240        │ ACCEPTED   │ 2026-06-03   │
├────────────────┴──────────────────────┴──────────────┴────────────┴──────────────┤
│ Submitted at │ Updated at  │ Actions                                               │
├──────────────┼─────────────┼───────────────────────────────────────────────────────┤
│ --           │ 2026-06-04  │ [Xem]                                                 │
│ 2026-06-03   │ 2026-06-03  │ [Xem]                                                 │
├──────────────┴─────────────┴───────────────────────────────────────────────────────┤
│ Rows per page [10]        1-10 of 65                         < 1 2 3 ... 7 >      │
└────────────────────────────────────────────────────────────────────────────────────┘
```

Actions:

- `Xem`: mở màn hình chi tiết lô dữ liệu.
- Không cho sửa/xóa lô đã tạo vì lô là snapshot nghiệp vụ để truy vết.

Vị trí nút trên UC19:

- `Tạo lô dữ liệu`: đặt ở góc phải page header của màn `Lô dữ liệu đối soát`; đây là primary action của UC19.
- `Tạo lô dữ liệu` trong modal: đặt ở footer bên phải, chỉ enable khi khoảng thời gian hợp lệ và nếu có bước kiểm tra trước thì phải có giao dịch đủ điều kiện.

#### Modal tạo lô dữ liệu

```text
┌──────────────────────────────────────────────────────────────────────┐
│ Tạo lô dữ liệu đối soát                                              │
├──────────────────────────────────────────────────────────────────────┤
│ Lỗi tổng nếu khoảng thời gian không hợp lệ hoặc tạo lô thất bại       │
├──────────────────────────────────────────────────────────────────────┤
│ Khoảng dữ liệu                                                       │
│ Từ ngày/giờ        [2026-06-04 00:00________________]                │
│ Đến ngày/giờ       [2026-06-04 23:59________________]                │
│                                                                      │
│ Điều kiện lấy giao dịch                                              │
│ sync_status = PENDING                                                │
│ occurred_at nằm trong khoảng đã chọn                                 │
│ chưa thuộc lô dữ liệu nào                                            │
├──────────────────────────────────────────────────────────────────────┤
│ Kiểm tra dữ liệu                                                     │
│ Giao dịch đủ điều kiện: 1,500                                        │
│ Giao dịch đã thuộc lô khác: 0                                        │
│ Cảnh báo: khoảng thời gian giao với lô đã tạo trước đó nếu có        │
├──────────────────────────────────────────────────────────────────────┤
│ [Hủy]                                      [Tạo lô dữ liệu]          │
└──────────────────────────────────────────────────────────────────────┘
```

Nếu không có giao dịch đủ điều kiện:

```text
┌──────────────────────────────────────────────────────────────┐
│ Không có giao dịch đủ điều kiện                              │
├──────────────────────────────────────────────────────────────┤
│ Trong khoảng đã chọn không có giao dịch PENDING nào để gom.  │
│ Hãy đổi khoảng thời gian hoặc kiểm tra màn Giao dịch vé.     │
│ [Đóng]                                                       │
└──────────────────────────────────────────────────────────────┘
```

Sau khi tạo thành công:

```text
┌──────────────────────────────────────────────────────────────┐
│ Đã tạo lô dữ liệu đối soát                                   │
├──────────────────────────────────────────────────────────────┤
│ Mã lô: OP01-20260604-0001                                    │
│ Khoảng dữ liệu: 2026-06-04 00:00 - 2026-06-04 23:59          │
│ Số giao dịch: 1,500                                          │
│ Trạng thái: CREATED                                          │
│                                                              │
│ [Về danh sách]                         [Xem chi tiết lô]    │
└──────────────────────────────────────────────────────────────┘
```

#### Màn hình chi tiết lô dữ liệu

```text
┌────────────────────────────────────────────────────────────────────────────────────┐
│ Chi tiết lô OP01-20260604-0001                                         [Quay lại]│
├────────────────────────────────────────────────────────────────────────────────────┤
│ Thông tin lô                                                                       │
│ Mã lô: OP01-20260604-0001    Trạng thái: CREATED                                  │
│ Khoảng dữ liệu: 2026-06-04 00:00 - 2026-06-04 23:59                                │
│ Số giao dịch: 1,500          Created at: 2026-06-04 23:00                          │
│ Submitted at: --                 Updated at: 2026-06-04 23:00                      │
├────────────────────────────────────────────────────────────────────────────────────┤
│ Tabs: [Giao dịch trong lô] [Lịch sử gửi Cấp 5]                                     │
├────────────────────────────────────────────────────────────────────────────────────┤
│ Giao dịch trong lô                                                                 │
│ Transaction ID | Event ID | Station | Device | Tap type | Decision | Occurred at   │
│ TX-001         | EVT-001  | ST-BT   | QR-001 | TAP_IN   | OPEN_GATE| 2026-06-04    │
├────────────────────────────────────────────────────────────────────────────────────┤
│ Rows per page [10]        1-10 of 1,500                     < 1 2 3 ... 150 >      │
├────────────────────────────────────────────────────────────────────────────────────┤
│ [Quay lại]                                                                      │
└────────────────────────────────────────────────────────────────────────────────────┘
```

Trong màn chi tiết lô của UC19, chỉ hiển thị trạng thái hiện tại (chẳng hạn `CREATED`, `SUBMITTED`, `ACCEPTED`, `REJECTED`, `FAILED`) để user theo dõi. Tiến trình gửi lên Cấp 5 (UC20) sẽ do hệ thống (Cron Job) tự động chạy ngầm.

#### Quy tắc nghiệp vụ UC19

- UC19 chỉ tạo lô ở trạng thái `CREATED`, chưa gửi dữ liệu ra Cấp 5.
- Backend tìm `afc_transactions.sync_status = PENDING` trong khoảng thời gian đã chọn.
- Backend gắn `batch_id` vào transaction đã được chọn để transaction đó không bị gom lại ở lô khác.
- Không chọn lại transaction đã thuộc batch/lô khác.
- Nếu không có transaction đủ điều kiện, không nên tạo lô rỗng trong MVP; hiển thị message để user chọn lại khoảng thời gian.
- Nếu khoảng thời gian quá rộng, yêu cầu thu hẹp để tránh query nặng.
- Phần `Kiểm tra dữ liệu` trong modal cần backend hỗ trợ count/preview trước khi tạo. Nếu chỉ giữ API hiện tại `POST /afc-ops/create-batch`, có thể bỏ bước kiểm tra trước và chỉ hiển thị số giao dịch sau khi tạo thành công.

API chính:

```text
POST /afc-ops/create-batch
Body: { "fromTime": "...", "toTime": "..." }
```

---

### UC20 - Gửi Batch dữ liệu lên Cấp 5

**Actor:** System, `LEVEL5_SYSTEM`  
**Loại:** System integration 
**Phục vụ:** Tự động gửi lô dữ liệu, theo dõi request/response, trạng thái gửi và kết quả C5.

Tên hiển thị UI tiếp tục dùng `Lô dữ liệu đối soát`. UC20 không có menu riêng; actor vào từ `Đối soát dữ liệu > Lô dữ liệu đối soát`, mở chi tiết lô để xem kết quả gửi ngầm từ hệ thống.

```text
Cấp 4 Batch Sender          LEVEL5_SYSTEM             Batch store
        │ build/send batch        │                        │
        ├────────────────────────>│                        │
        │<──── accepted/rejected ─┤                        │
        ├─ update status ─────────────────────────────────>│
        ├─ save request/response ─────────────────────────>│
        ├─ update transactions if accepted ───────────────>│
```

#### Màn hình chi tiết lô (chỉ xem kết quả gửi tự động)

```text
┌────────────────────────────────────────────────────────────────────────────────────┐
│ Chi tiết lô OP01-20260604-0001                                         [Quay lại]│
├────────────────────────────────────────────────────────────────────────────────────┤
│ Thông tin lô                                                                       │
│ Mã lô: OP01-20260604-0001    Trạng thái: ACCEPTED                                 │
│ Khoảng dữ liệu: 2026-06-04 00:00 - 2026-06-04 23:59                                │
│ Số giao dịch: 1,500          Submitted at: 2026-06-04 23:00                        │
├────────────────────────────────────────────────────────────────────────────────────┤
│ Tabs: [Giao dịch trong lô] [Lịch sử gửi Cấp 5] [Kết quả vé lượt]                  │
├────────────────────────────────────────────────────────────────────────────────────┤
│ Giao dịch trong lô                                                                 │
│ Transaction ID | Event ID | Station | Device | Tap type | Decision | Sync status   │
│ TX-001         | EVT-001  | ST-BT   | QR-001 | TAP_IN   | OPEN_GATE| SYNCED        │
└────────────────────────────────────────────────────────────────────────────────────┘
```

Trong hệ thống tích hợp, việc gửi lô dữ liệu được tự động hóa hoàn toàn bằng **Cron Job**. Không có nút `Gửi Cấp 5` trên UI.
User theo dõi tiến trình thông qua Trạng thái lô và Tab lịch sử gửi.

#### Kết quả gửi thành công

Khi hệ thống gửi Cấp 5 thành công (ACCEPTED):

- Cập nhật lô sang `ACCEPTED`.
- Cập nhật transaction trong lô sang `syncStatus = SYNCED` theo quy tắc API contract.
- Nếu Cấp 5 trả ticket/journey result cho vé lượt Metro, lưu kết quả để tra cứu ở tab `Kết quả vé lượt`.

Khi hệ thống gửi lỗi hoặc bị từ chối (FAILED/REJECTED):

- Cập nhật lô sang `REJECTED` hoặc `FAILED`.
- Lưu response reject vào `integration_exchange_logs`.
- Không tự sửa giao dịch trong lô trên UI; người vận hành xem chi tiết response để phân tích lỗi.

#### Tab lịch sử gửi Cấp 5

```text
┌────────────────────────────────────────────────────────────────────────────────────┐
│ Lịch sử gửi Cấp 5                                                                  │
├────────────────────┬──────────┬──────────────┬──────────────┬─────────────────────┤
│ Thời gian gửi      │ Hướng    │ Trạng thái   │ Correlation  │ Lỗi/Phản hồi        │
├────────────────────┼──────────┼──────────────┼──────────────┼─────────────────────┤
│ 2026-06-04 23:00   │ OUTBOUND │ SUCCESS      │ OP01-...     │ ACK - Accepted      │
│ 2026-06-04 22:55   │ OUTBOUND │ FAILED       │ OP01-...     │ LEVEL5_UNAVAILABLE  │
├────────────────────┴──────────┴──────────────┴──────────────┴─────────────────────┤
│ [Xem request/response]                                                             │
└────────────────────────────────────────────────────────────────────────────────────┘
```

Màn xem request/response:

```text
┌──────────────────────────────────────────────────────────────────────┐
│ Request/response Cấp 5                                      [Đóng]   │
├──────────────────────────────────────────────────────────────────────┤
│ Correlation ID: OP01-20260604-0001                                   │
│ Target system: LEVEL5_SYSTEM                                         │
│ Status: SUCCESS                                                      │
│ Created at: 2026-06-04 23:00                                         │
├──────────────────────────────────────────────────────────────────────┤
│ Request summary                                                      │
│ Batch code, số transaction, khoảng dữ liệu, endpoint gọi             │
├──────────────────────────────────────────────────────────────────────┤
│ Response summary                                                     │
│ ACK/REJECT, message, error nếu có                                    │
└──────────────────────────────────────────────────────────────────────┘
```

#### Tab kết quả vé lượt

```text
┌────────────────────────────────────────────────────────────────────────────────────┐
│ Kết quả vé lượt từ Cấp 5                                                           │
├────────────────┬──────────────┬─────────────┬──────────────┬──────────────────────┤
│ Transaction ID │ Ticket ID    │ Journey ref │ Kết quả      │ Updated at           │
├────────────────┼──────────────┼─────────────┼──────────────┼──────────────────────┤
│ TX-001         │ TICKET-001   │ JRN-001     │ CONFIRMED    │ 2026-06-04 23:05     │
│ TX-002         │ TICKET-002   │ --          │ FAILED       │ 2026-06-04 23:05     │
└────────────────┴──────────────┴─────────────┴──────────────┴──────────────────────┘
```

Tab này chỉ có dữ liệu nếu Cấp 5 trả kết quả xử lý vé lượt Metro. Nếu không có, hiển thị empty state.

#### Quy tắc trạng thái và action

| Trạng thái lô | Hiển thị trên UI | Action |
| --- | --- | --- |
| `CREATED` | Lô đã tạo, chờ hệ thống gửi Cấp 5 | Chỉ xem |
| `SUBMITTED` | Đã gửi ngầm, đang chờ hoặc đã ghi nhận gửi sơ bộ | Chỉ xem |
| `ACCEPTED` | Cấp 5 đã nhận hợp lệ | Chỉ xem |
| `REJECTED` | Cấp 5 từ chối nghiệp vụ | Chỉ xem response |
| `FAILED` | Lỗi kỹ thuật khi gửi tự động | Chỉ xem, hệ thống sẽ tự retry nếu cấu hình |

Hệ thống sẽ dựa vào cấu hình Cron Job để thực hiện retry tự động khi lô bị `FAILED`. Lô đã `ACCEPTED` không được gửi lại.

#### Nguồn dữ liệu

| Khu vực | Nguồn |
| --- | --- |
| Thông tin lô | `batches`: `batch_code`, `from_time`, `to_time`, `transaction_count`, `status`, `submitted_at`, `created_at`, `updated_at` |
| Giao dịch trong lô | `afc_transactions.batch_id`, `sync_status`, `ticket_processing_status` |
| Request/response Cấp 5 | Mongo `integration_exchange_logs` |
| Kết quả vé lượt | Mongo `ticket_usage_result_payloads` và fields `journey_ref`, `ticket_processing_status` trên transaction nếu đã cập nhật |

---

### UC21 - Audit và truy vết

**Actor:** `OPERATOR_ADMIN`  
**Loại:** Màn hình tra cứu + chi tiết  
**Phục vụ:** Tra cứu sự kiện đăng nhập, account/role, master data, package và integration log.

UC21 là màn phục vụ kiểm tra và truy vết, không phải màn chỉnh sửa dữ liệu. Màn này gom hai loại log chính: audit thao tác nội bộ và log tích hợp kỹ thuật.

#### Tab Đăng nhập & tài khoản

```text
┌────────────────────────────────────────────────────────────────────────────────────┐
│ Audit và truy vết                                                                  │
├────────────────────────────────────────────────────────────────────────────────────┤
│ Tabs: [Đăng nhập & tài khoản] [Thao tác vận hành] [Tích hợp hệ thống]             │
├────────────────────────────────────────────────────────────────────────────────────┤
│ Bộ lọc đăng nhập & tài khoản                                                       │
│ [Từ ngày/giờ] [Đến ngày/giờ] [Username] [Hành động] [Kết quả] [Lọc] [Đặt lại]     │
├────────────────────┬──────────────┬────────────────────┬──────────┬──────────────┤
│ Thời gian          │ Username     │ Hành động          │ Kết quả  │ IP address   │
├────────────────────┼──────────────┼────────────────────┼──────────┼──────────────┤
│ 2026-06-04 09:10   │ admin01      │ ACCOUNT_CREATED    │ SUCCESS │ 127.0.0.1    │
│ 2026-06-04 08:30   │ manager01    │ LOGIN_SUCCESS      │ SUCCESS │ 127.0.0.1    │
│ 2026-06-04 08:25   │ operator02   │ LOGIN_FAILED       │ FAILED  │ 127.0.0.1    │
├────────────────────┴──────────────┴────────────────────┴──────────┴──────────────┤
│ Tài nguyên    │ Mã tài nguyên │ User agent              │ Actions                 │
├───────────────┼───────────────┼─────────────────────────┼─────────────────────────┤
│ Tài khoản     │ account-uuid  │ Mozilla/5.0             │ [Xem chi tiết]          │
│ Session       │ session-id    │ Mozilla/5.0             │ [Xem chi tiết]          │
│ Account login │ operator02    │ Mozilla/5.0             │ [Xem chi tiết]          │
├───────────────┴───────────────┴─────────────────────────┴─────────────────────────┤
│ Rows per page [10]        1-10 of 200                         < 1 2 3 ... 20 >    │
└────────────────────────────────────────────────────────────────────────────────────┘
```

Tab này dùng `GET /auth/search-audit-logs`. Các action tiêu biểu: `LOGIN_SUCCESS`, `LOGIN_FAILED`, `LOGOUT`, `PASSWORD_CHANGED`, `ACCOUNT_CREATED`, `ACCOUNT_BANNED`, `ACCOUNT_UNBANNED`, `PASSWORD_RESET`.

#### Tab Thao tác vận hành

```text
┌────────────────────────────────────────────────────────────────────────────────────┐
│ Audit và truy vết                                                                  │
├────────────────────────────────────────────────────────────────────────────────────┤
│ Tabs: [Đăng nhập & tài khoản] [Thao tác vận hành] [Tích hợp hệ thống]             │
├────────────────────────────────────────────────────────────────────────────────────┤
│ Bộ lọc thao tác vận hành                                                           │
│ [Từ ngày/giờ] [Đến ngày/giờ] [Người thao tác] [Hành động] [Loại tài nguyên]       │
│ [Mã tài nguyên] [Kết quả] [Lọc] [Đặt lại]                                         │
├────────────────────┬──────────────┬────────────────────────────┬──────────┬───────┤
│ Thời gian          │ Người thao tác│ Hành động                  │ Kết quả  │ Module│
├────────────────────┼──────────────┼────────────────────────────┼──────────┼───────┤
│ 2026-06-04 10:30   │ manager01    │ CONTROL_PACKAGE_PUBLISHED  │ SUCCESS │ Cấu hình│
│ 2026-06-04 09:45   │ manager01    │ ROUTE_UPDATED              │ SUCCESS │ Master │
│ 2026-06-04 09:20   │ manager02    │ BATCH_CREATED              │ SUCCESS │ Đối soát│
├────────────────────┴──────────────┴────────────────────────────┴──────────┴───────┤
│ Loại tài nguyên │ Mã tài nguyên │ Request ID          │ Actions                    │
├─────────────────┼───────────────┼─────────────────────┼────────────────────────────┤
│ Gói cấu hình    │ 100           │ req-20260604-001    │ [Xem chi tiết]             │
│ Tuyến           │ R01           │ req-20260604-002    │ [Xem chi tiết]             │
│ Lô đối soát     │ OP01-20260604 │ req-20260604-003    │ [Xem chi tiết]             │
├─────────────────┴───────────────┴─────────────────────┴────────────────────────────┤
│ Rows per page [10]        1-10 of 350                         < 1 2 3 ... 35 >    │
└────────────────────────────────────────────────────────────────────────────────────┘
```

Tab này dùng `GET /afc-ops/search-audit-logs`. Các module/resource nên map theo sitemap: master data, thiết bị, cấu hình vận hành và đối soát dữ liệu. Log đồng bộ C5 nếu cần debug nằm ở tab `Tích hợp hệ thống`, không tạo module dữ liệu vé riêng.

#### Tab Tích hợp hệ thống

```text
┌────────────────────────────────────────────────────────────────────────────────────┐
│ Audit và truy vết                                                                  │
├────────────────────────────────────────────────────────────────────────────────────┤
│ Tabs: [Đăng nhập & tài khoản] [Thao tác vận hành] [Tích hợp hệ thống]             │
├────────────────────────────────────────────────────────────────────────────────────┤
│ Bộ lọc tích hợp hệ thống                                                           │
│ [Từ ngày/giờ] [Đến ngày/giờ] [Hệ thống đích] [Hướng] [Trạng thái] [Correlation]   │
│ [Lọc] [Đặt lại]                                                                    │
├────────────────────┬──────────┬────────────────┬──────────────┬──────────────────┤
│ Thời gian          │ Hướng    │ Hệ thống đích  │ Trạng thái   │ Correlation ID   │
├────────────────────┼──────────┼────────────────┼──────────────┼──────────────────┤
│ 2026-06-04 23:00   │ OUTBOUND │ LEVEL5_SYSTEM  │ SUCCESS      │ OP01-20260604    │
│ 2026-06-04 22:55   │ OUTBOUND │ LEVEL5_SYSTEM  │ FAILED       │ OP01-20260604    │
│ 2026-06-04 09:00   │ INBOUND  │ LEVEL5_SYSTEM  │ SUCCESS      │ L5-SYNC-001      │
├────────────────────┴──────────┴────────────────┴──────────────┴──────────────────┤
│ Resource ref   │ Error message              │ Actions                             │
├────────────────┼────────────────────────────┼─────────────────────────────────────┤
│ batch:OP01-... │ --                         │ [Xem request/response]             │
│ batch:OP01-... │ LEVEL5_UNAVAILABLE         │ [Xem request/response]             │
│ sync:L5-001    │ --                         │ [Xem request/response]             │
├────────────────┴────────────────────────────┴─────────────────────────────────────┤
│ Rows per page [10]        1-10 of 120                         < 1 2 3 ... 12 >    │
└────────────────────────────────────────────────────────────────────────────────────┘
```

Tab này đọc Mongo `integration_exchange_logs` hoặc API search integration log nếu BE tách riêng. Dùng để debug request/response với Cấp 5, không phải audit thao tác user.

#### Ý nghĩa từng tab

| Tab | Phục vụ ai | Nội dung |
| --- | --- | --- |
| `Đăng nhập & tài khoản` | `OPERATOR_ADMIN` | Login/logout, đổi mật khẩu, tạo account, ban/unban, reset password, gán role |
| `Thao tác vận hành` | `OPERATOR_ADMIN`, có thể cấp read-only cho `OPERATOR_MANAGER` nếu cần | Tạo/sửa/xóa tuyến, ga/trạm, thiết bị, tạo/phát hành gói cấu hình, tạo/gửi lô dữ liệu |
| `Tích hợp hệ thống` | `OPERATOR_ADMIN`, `OPERATOR_MANAGER` khi cần debug vận hành | Request/response với Cấp 5, publish/pull package, lỗi gửi lô dữ liệu |

#### Chi tiết tab Đăng nhập & tài khoản

```text
┌──────────────────────────────────────────────────────────────────────┐
│ Chi tiết audit tài khoản                                  [Quay lại]│
├──────────────────────────────────────────────────────────────────────┤
│ Thông tin sự kiện                                                    │
│ Thời gian: 2026-06-04 09:10                                         │
│ Username: admin01                                                    │
│ Account ID: account-uuid                                             │
│ Hành động: ACCOUNT_CREATED                                           │
│ Kết quả: SUCCESS                                                     │
├──────────────────────────────────────────────────────────────────────┤
│ Ngữ cảnh truy cập                                                    │
│ IP address: 127.0.0.1                                                │
│ Trình duyệt/thiết bị: Chrome on Windows                              │
│ User agent raw: Mozilla/5.0 ... Chrome/...                           │
│ Request ID: req-auth-20260604-001                                    │
├──────────────────────────────────────────────────────────────────────┤
│ Dữ liệu liên quan                                                    │
│ Target account: operator02                                           │
│ Role assigned: STATION_OPERATOR                                      │
│ mustChangePassword: true                                             │
├──────────────────────────────────────────────────────────────────────┤
│ Metadata kỹ thuật                                                    │
│ authMethod: PASSWORD                                                 │
│ resultReason: --                                                     │
│ sessionId: session-id nếu có                                         │
├──────────────────────────────────────────────────────────────────────┤
│ [Quay lại]                                      [Mở tài khoản liên quan]│
└──────────────────────────────────────────────────────────────────────┘
```

Frame này dùng cho `auth_audit_logs`. `Mở tài khoản liên quan` chỉ hiển thị nếu tài khoản còn tồn tại và user có quyền xem account.

#### Chi tiết tab Thao tác vận hành

```text
┌──────────────────────────────────────────────────────────────────────┐
│ Chi tiết audit vận hành                                   [Quay lại]│
├──────────────────────────────────────────────────────────────────────┤
│ Thông tin sự kiện                                                    │
│ Thời gian: 2026-06-04 10:30                                         │
│ Người thao tác: manager01                                           │
│ Account ID: account-uuid                                             │
│ Hành động: CONTROL_PACKAGE_PUBLISHED                                │
│ Kết quả: SUCCESS                                                     │
├──────────────────────────────────────────────────────────────────────┤
│ Tài nguyên nghiệp vụ                                                 │
│ Module: Cấu hình vận hành                                            │
│ Loại tài nguyên: Gói cấu hình                                       │
│ Mã tài nguyên: 100                                                   │
│ Tên/mô tả: Package version 12 - MEDIA_ACCESS_RULES                  │
├──────────────────────────────────────────────────────────────────────┤
│ Thay đổi chính                                                       │
│ Trước: status = CREATED                                              │
│ Sau: status = PUBLISHED                                              │
│ Phạm vi tác động: stationIds [1, 2, 3]                               │
├──────────────────────────────────────────────────────────────────────┤
│ Ngữ cảnh request                                                     │
│ IP address: 127.0.0.1                                                │
│ Request ID: req-afc-20260604-001                                     │
│ API/Action ref: publish-control-package                              │
├──────────────────────────────────────────────────────────────────────┤
│ Metadata                                                             │
│ stationCount: 3                                                      │
│ publishedAt: 2026-06-04 10:30                                       │
│ integrationRef: --                                                   │
├──────────────────────────────────────────────────────────────────────┤
│ [Quay lại]                                     [Mở tài nguyên liên quan]│
└──────────────────────────────────────────────────────────────────────┘
```

Frame này dùng cho `afc_audit_logs`. Nút `Mở tài nguyên liên quan` chỉ hiển thị khi UI có màn tương ứng và user có quyền xem. Ví dụ:

- Audit tạo/sửa tuyến: mở chi tiết tuyến.
- Audit tạo/sửa thiết bị: mở chi tiết thiết bị.
- Audit phát hành gói cấu hình: mở chi tiết gói cấu hình.
- Audit tạo/gửi lô dữ liệu: mở chi tiết lô dữ liệu đối soát.

#### Chi tiết tab Tích hợp hệ thống

```text
┌──────────────────────────────────────────────────────────────────────┐
│ Chi tiết tích hợp hệ thống                                [Quay lại]│
├──────────────────────────────────────────────────────────────────────┤
│ Thông tin giao tiếp                                                  │
│ Thời gian: 2026-06-04 23:00                                         │
│ Hướng: OUTBOUND                                                      │
│ Hệ thống đích: LEVEL5_SYSTEM                                         │
│ Trạng thái: SUCCESS                                                  │
│ Correlation ID: OP01-20260604-0001                                  │
├──────────────────────────────────────────────────────────────────────┤
│ Tài nguyên liên quan                                                 │
│ Resource ref: batch:OP01-20260604-0001                              │
│ Endpoint/Action: submit-batch-to-level5                              │
│ Request ID: req-level5-20260604-001                                  │
├──────────────────────────────────────────────────────────────────────┤
│ Request summary                                                      │
│ batchCode: OP01-20260604-0001                                       │
│ transactionCount: 1,500                                              │
│ fromTime/toTime: 2026-06-04 00:00 - 23:59                           │
├──────────────────────────────────────────────────────────────────────┤
│ Response summary                                                     │
│ ackCode: ACK                                                         │
│ message: Accepted                                                    │
│ errorMessage: --                                                     │
├──────────────────────────────────────────────────────────────────────┤
│ Raw payload readonly                                                 │
│ [Xem request raw] [Xem response raw]                                 │
├──────────────────────────────────────────────────────────────────────┤
│ [Quay lại]                                      [Mở lô dữ liệu liên quan]│
└──────────────────────────────────────────────────────────────────────┘
```

Frame này dùng cho `integration_exchange_logs`. Raw request/response nên mở trong modal readonly, có thể mask token/secret nếu log có dữ liệu nhạy cảm.

#### Quy tắc hiển thị

- Query quá rộng cần yêu cầu giới hạn thời gian theo UC21-E02.
- Không có dữ liệu thì hiển thị empty state trong bảng.
- Chỉ user có `AUDIT_READ` mới vào được màn này.
- `OPERATOR_ADMIN` là actor chính. `OPERATOR_MANAGER` chỉ nên được cấp một phần nếu cần xem log vận hành/tích hợp, không mặc định xem audit tài khoản.
- Không cho xóa/sửa audit log từ UI.
- Với metadata dài, hiển thị theo dạng key/value rút gọn; nếu cần xem raw payload thì mở modal readonly riêng.

Nguồn dữ liệu:

| Khu vực | API/Nguồn |
| --- | --- |
| Đăng nhập & tài khoản | `GET /auth/search-audit-logs` |
| Thao tác vận hành | `GET /afc-ops/search-audit-logs` |
| Tích hợp hệ thống | Mongo `integration_exchange_logs` hoặc API search integration log nếu BE tách riêng |

---

### UC22 - App lấy QR động từ Card

**Actor:** `PASSENGER_APP`, System  
**Loại:** Không có màn hình quản lý AFC Ops; Passenger App gọi API C4  
**Phục vụ:** C4 sinh QR ngắn hạn từ card/ticket/entitlement đã đồng bộ.

```text
Passenger App             C4 QR Service             Read model / Redis
      │ request QR              │                           │
      ├────────────────────────>│                           │
      │                         ├─ check active card ──────>│
      │                         ├─ check ticket/entitlement│
      │                         ├─ create signed QR/session│
      │<──── QR payload ────────┤                           │
      │ render QR               │                           │
```

Không cần wireframe AFC Ops riêng cho UC22. Nếu cần quan sát:

- UC11 cho biết kết quả sau khi QR được scan thành transaction.
- UC21 tab `Tích hợp hệ thống` hoặc log kỹ thuật cho biết lỗi đồng bộ C5/cấp QR nếu BE ghi log.
- UC14 chỉ là luồng nhận dữ liệu từ C5, không có màn AFC Ops riêng.

## 6. Sitemap tạm thời đã review/confirm

```text
AFC Ops
├── Tổng quan                              UC18 - OPERATOR_MANAGER
├── Master data                            OPERATOR_MANAGER
│   ├── Tuyến                              UC05
│   └── Ga/Trạm                            UC06
├── Thiết bị                               OPERATOR_MANAGER, STATION_OPERATOR
│   ├── Quản lý thiết bị                   UC07
│   ├── Giám sát thiết bị                  UC08, UC09
│   └── Sự cố thiết bị                     UC12, UC13
├── Giao dịch                              UC10, UC11 - OPERATOR_MANAGER, STATION_OPERATOR
├── Quản lý lô dữ liệu                     UC19, UC20 - OPERATOR_MANAGER
├── Cấu hình vận hành                      OPERATOR_MANAGER, STATION_OPERATOR
│   ├── Gói cấu hình                       UC15, UC16
│   └── Trạng thái áp dụng                 UC17
├── Audit và truy vết                      UC21 - OPERATOR_ADMIN
```

Các nhóm khác sẽ bổ sung vào sitemap sau khi review/confirm tới use case tương ứng.

Ghi chú nhóm `Cấu hình vận hành`:

- `Gói cấu hình` là một màn hình danh sách chung cho package do Cấp 4 tạo ở UC15 và package nhận từ Cấp 5 cần phát hành ở UC16.
- Action chính trong `Gói cấu hình`: tạo package nháp, xem chi tiết, sửa nháp nếu còn đủ điều kiện, phát hành xuống station.
- `Trạng thái áp dụng` là màn hình theo dõi `station_control_syncs` sau khi gói cấu hình đã được phát hành; `OPERATOR_MANAGER` xem toàn operator, `STATION_OPERATOR` chỉ xem station được phân quyền nếu cấp màn hình này cho Cấp 3.
- Không tách `Phát hành package` thành menu riêng vì đây là action theo từng package, mở từ danh sách hoặc chi tiết package.

Ghi chú mục `Giao dịch`:

- UC10 là API/device flow ghi nhận lượt quét và tạo transaction chuẩn hóa.
- UC11 là màn hình `Giao dịch` để người dùng tra cứu các giao dịch vé đã ghi nhận.

Ghi chú mục `Quản lý lô dữ liệu`:

- `Quản lý lô dữ liệu` là màn hình quản lý các lô gom giao dịch vé để gửi Cấp 5.
- UC19 tạo lô ở trạng thái `CREATED`.
- UC20 gửi lô lên Cấp 5/mock và theo dõi trạng thái `SUBMITTED`, `ACCEPTED`, `REJECTED`, `FAILED`.
- Không đặt tên menu là `Batch` vì đó là thuật ngữ kỹ thuật trong API/schema.

## 7. Ma trận màn hình theo actor

| Màn hình | ADMIN | MANAGER | STATION OPERATOR |
| --- | --- | --- | --- |
| Login / đổi mật khẩu | Có | Có | Có |
| Quản lý account / reset password | Có | Không | Không |
| Audit | Có | Không theo spec | Không |
| Master data: tuyến / ga-trạm | Không theo actor chính | Có | Không |
| Thiết bị: quản lý thiết bị | Không theo actor chính | Có | Không |
| Thiết bị: giám sát thiết bị | Không theo actor chính | Có toàn operator | Có trong station |
| Giám sát thiết bị | Không theo actor chính | Có toàn operator | Có trong station |
| Transaction | Không theo actor chính | Có toàn operator | Có trong station |
| Incident | Không theo actor chính | Có toàn operator | Có trong station |
| Cấu hình vận hành | Không theo actor chính | Có | Chỉ trạng thái áp dụng tại Cấp 3 nếu cần |
| Dashboard | Không theo actor chính | Có | Không |
| Đối soát dữ liệu | Không | Có | Không |

## 8. Điểm cần review chung

1. `OPERATOR_ADMIN` có cần xem Dashboard hay chỉ quản lý account/audit?
2. `STATION_OPERATOR` có cần xem trạng thái áp dụng package UC17 không?
3. Chi tiết device/transaction/incident dùng page riêng hay modal chi tiết?
4. Control package payload dùng JSON editor hay form sinh theo schema?
5. Retry batch UC20 có cho phép thao tác thủ công hay chỉ System tự retry?
6. Có cần màn hình integration log riêng hay gộp vào audit/chi tiết sync?
