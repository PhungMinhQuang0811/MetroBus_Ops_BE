# Use Case Spec AFC Cấp 3 Và Cấp 4

Tài liệu này đặc tả use case theo từng nghiệp vụ cụ thể cho MVP Cấp 3/Cấp 4.

Nguồn đầu vào:

- `document/Tổng hợp Chức năng Hệ thống AFC - Cấp 3 và Cấp 4 V2.md`
- `document/Breakdown Chức năng AFC Cấp 3 và Cấp 4.md`

## 1. Actor

| Actor | Mô tả |
| --- | --- |
| `OPERATOR_ADMIN` | Admin của đơn vị vận hành Cấp 4, init/quản lý account và role |
| `OPERATOR_MANAGER` | Quản lý vận hành Cấp 4 của một operator |
| `STATION_OPERATOR` | Nhân viên/giám sát tại ga/trạm/tuyến Cấp 3 |
| `DEVICE_CLIENT` | Thiết bị hoặc hệ thống Cấp 2 gửi dữ liệu lên Cấp 3 |
| `LEVEL5_SYSTEM` | Hệ thống Cấp 5 thật hoặc mock dev/test nhận batch dữ liệu vận hành và đồng bộ card, ticket, entitlement, card status xuống Cấp 4 |
| `PASSENGER_APP` | App/FE hành khách gọi Cấp 4 để lấy QR động; các nghiệp vụ mua vé/gia hạn/chuyển card gọi Cấp 5 và không thuộc use case C3/C4 |

### Thuật Ngữ Dễ Nhầm

| Thuật ngữ | Cách hiểu trong tài liệu |
| --- | --- |
| Quản lý thiết bị | Khai báo danh mục thiết bị: mã thiết bị, loại thiết bị, ga/trạm, hướng hoạt động, trạng thái quản trị |
| Heartbeat | Tín hiệu trạng thái định kỳ do thiết bị gửi để xác nhận thiết bị vẫn đang kết nối và hoạt động |
| Giám sát trạng thái thiết bị | Chức năng cho nhân sự xem thiết bị nào online, offline, maintenance hoặc disabled dựa trên dữ liệu heartbeat |
| Tap/Check event | Sự kiện phát sinh khi hành khách chạm thẻ hoặc quét mã tại thiết bị soát vé; trong tài liệu gọi là lượt quét |
| Batch | Gói dữ liệu gồm nhiều transaction được gom lại; tạo batch và gửi batch là hai use case riêng |
| App | Client/FE hành khách; App không phải C1 |
| C1 | Media hành khách sử dụng; trong MVP là QR động đang hiển thị trên App |
| Card | Media hành khách dùng để đi lại; trong MVP là card ảo hiển thị bằng QR động trên App |
| Ticket | Vé lượt prepaid do Cấp 5 phát hành, dùng một lần trong phạm vi/thời hạn cho phép |
| Entitlement | Quyền sử dụng vé tháng/vé chu kỳ do Cấp 5 cấp cho user/card trong một phạm vi và thời hạn |
| Dynamic QR payload | Payload ngắn hạn do Cấp 4 sinh từ card/ticket/entitlement để App render thành QR, TTL 30-60 giây |
| Card status | Trạng thái hiện hành của card do Cấp 5 đồng bộ xuống Cấp 4; blacklist là một trạng thái của card |
| Vé lượt Metro | Vé prepaid một lượt, chỉ áp dụng cho Metro trong MVP; C5 phát hành ticket, C3/C4 verify QR và ghi nhận tap event |

## 2. Danh Sách Use Case

| UC | Tên use case | Actor chính | Priority |
| --- | --- | --- | --- |
| UC01 | Đăng nhập | OPERATOR_ADMIN, OPERATOR_MANAGER, STATION_OPERATOR | P0 |
| UC02 | Quản lý tài khoản nhân sự | OPERATOR_ADMIN | P0 |
| UC03 | Đổi mật khẩu | OPERATOR_ADMIN, OPERATOR_MANAGER, STATION_OPERATOR | P2 |
| UC04 | Quên mật khẩu | OPERATOR_ADMIN | P2 |
| UC05 | Quản lý tuyến | OPERATOR_ADMIN, OPERATOR_MANAGER | P1 |
| UC06 | Quản lý ga/trạm | OPERATOR_ADMIN, OPERATOR_MANAGER | P1 |
| UC07 | Quản lý danh mục thiết bị AFC | OPERATOR_ADMIN, OPERATOR_MANAGER | P1 |
| UC08 | Thiết bị gửi tín hiệu trạng thái định kỳ | DEVICE_GATEWAY, System | P0 |
| UC09 | Theo dõi trạng thái hoạt động thiết bị | OPERATOR_MANAGER, STATION_OPERATOR | P0 |
| UC10 | Ghi nhận lượt quét tại thiết bị soát vé | DEVICE_GATEWAY, System | P0 |
| UC11 | Tra cứu transaction vận hành | OPERATOR_MANAGER, STATION_OPERATOR | P0 |
| UC12 | Ghi nhận incident thiết bị | DEVICE_GATEWAY, STATION_OPERATOR | P1 |
| UC13 | Theo dõi incident thiết bị | OPERATOR_MANAGER, STATION_OPERATOR | P1 |
| UC14 | Đồng bộ card, ticket, entitlement từ Cấp 5 | LEVEL5_SYSTEM, System | P0 |
| UC15 | Tạo control package cấu hình vận hành | OPERATOR_ADMIN, OPERATOR_MANAGER | P0 |
| UC16 | Phát hành control package xuống Cấp 3 | OPERATOR_ADMIN, OPERATOR_MANAGER, System | P0 |
| UC17 | Cấp 3 nhận và áp dụng control package | STATION_AGENT, System | P0 |
| UC18 | Dashboard vận hành Cấp 4 | OPERATOR_ADMIN, OPERATOR_MANAGER | P1 |
| UC19 | Tạo batch dữ liệu | System, OPERATOR_MANAGER | P0 |
| UC20 | Gửi batch dữ liệu lên Cấp 5 | System, OPERATOR_MANAGER | P0 |
| UC21 | Audit và truy vết | OPERATOR_ADMIN, OPERATOR_MANAGER | P1 |
| UC22 | App lấy QR động từ card | PASSENGER_APP, System | P0 |

### Scope Sản Phẩm Vé Trong MVP

| Loại vận tải | Sản phẩm trong scope | Ghi chú |
| --- | --- | --- |
| BUS | Vé tháng | Không làm vé lượt bus |
| METRO | Vé tháng, vé lượt prepaid | Vé lượt Metro dùng bảng `tickets`, không dùng wallet/balance trong MVP C3/C4 |

Không làm vé ngày trong MVP. Cắt vé ngày để giữ scope gọn sau khi bổ sung vé lượt Metro.

## UC01 - Đăng Nhập

### Bảng Mô Tả

| Thuộc tính | Nội dung |
| --- | --- |
| Mục tiêu | Cho phép nhân sự nội bộ đăng nhập hệ thống Cấp 3/Cấp 4 |
| Actor chính | `OPERATOR_ADMIN`, `OPERATOR_MANAGER`, `STATION_OPERATOR` |
| Priority | P0 |
| Tần suất | Mỗi phiên làm việc |
| Dữ liệu chính | Account, role, token/session |

### Tiền Điều Kiện

- Account đã tồn tại.
- Account đang active.
- Account đã được gán role hợp lệ.

### Luồng Chính

| Bước | Actor/System | Mô tả |
| --- | --- | --- |
| 1 | Actor | Nhập username và password |
| 2 | System | Kiểm tra account tồn tại |
| 3 | System | Kiểm tra account đang active |
| 4 | System | Xác thực password |
| 5 | System | Sinh token/cookie đăng nhập |
| 6 | System | Trả thông tin user và role |

### Luồng Thay Thế/Lỗi

| Mã | Điều kiện | Kết quả |
| --- | --- | --- |
| UC01-E01 | Account không tồn tại | Từ chối đăng nhập |
| UC01-E02 | Password sai | Từ chối đăng nhập, ghi audit nếu bật |
| UC01-E03 | Account bị khóa | Từ chối đăng nhập |
| UC01-E04 | Account chưa có role | Đăng nhập thất bại hoặc không cho truy cập API nghiệp vụ |

### Hậu Điều Kiện

- User có token/session hợp lệ.
- Backend dùng role/permission để bảo vệ API.

## UC02 - Quản Lý Tài Khoản Nhân Sự

### Bảng Mô Tả

| Thuộc tính | Nội dung |
| --- | --- |
| Mục tiêu | `OPERATOR_ADMIN` tạo và quản lý tài khoản nhân sự trong phạm vi operator |
| Actor chính | `OPERATOR_ADMIN` |
| Priority | P0 |
| Tần suất | Khi onboarding/offboarding nhân sự |
| Dữ liệu chính | Account, role |

### Tiền Điều Kiện

- `OPERATOR_ADMIN` đã đăng nhập.
- Operator đã được init.

### Luồng Chính

| Bước | Actor/System | Mô tả |
| --- | --- | --- |
| 1 | OPERATOR_ADMIN | Mở màn hình quản lý account |
| 2 | OPERATOR_ADMIN | Nhập username và mật khẩu tạm |
| 3 | OPERATOR_ADMIN | Chọn role cho account |
| 4 | System | Kiểm tra username không trùng |
| 5 | System | Tạo account |
| 6 | System | Gán role |
| 7 | System | Đánh dấu account bắt buộc đổi mật khẩu ở lần đăng nhập đầu tiên |
| 8 | OPERATOR_ADMIN | Chuyển mật khẩu tạm cho user qua kênh ngoài hệ thống |
| 9 | System | Ghi audit thay đổi |

### Luồng Thay Thế - Import Tài Khoản Hàng Loạt

| Bước | Actor/System | Mô tả |
| --- | --- | --- |
| A1 | OPERATOR_ADMIN | Tải lên file CSV/Excel theo mẫu |
| A2 | System | Kiểm tra username, mật khẩu tạm và role của từng dòng |
| A3 | System | Validate toàn bộ username không trùng và role đã tồn tại |
| A4 | System | Hiển thị preview hoặc danh sách lỗi |
| A5 | OPERATOR_ADMIN | Xác nhận import |
| A6 | System | Tạo account, gán role và đánh dấu bắt buộc đổi mật khẩu trong một transaction |

### Luồng Thay Thế/Lỗi

| Mã | Điều kiện | Kết quả |
| --- | --- | --- |
| UC02-E01 | Username trùng | Từ chối tạo/cập nhật |
| UC02-E02 | Role không hợp lệ | Từ chối gán role |
| UC02-E03 | Khóa operator admin cuối cùng | Từ chối thao tác |
| UC02-E04 | Account đã có transaction/audit | Không xóa cứng, chỉ disable |
| UC02-E05 | File import sai cấu trúc hoặc có dòng lỗi | Không import, trả báo cáo lỗi |

### Hậu Điều Kiện

- Account được tạo/cập nhật/khóa đúng trạng thái.
- Role được áp dụng cho phân quyền API.
- User phải đổi mật khẩu ngay lần đăng nhập đầu tiên.
- `OPERATOR_ADMIN` chỉ biết mật khẩu tạm, không biết mật khẩu thật sau khi user đổi.

## UC03 - Đổi Mật Khẩu

### Bảng Mô Tả

| Thuộc tính | Nội dung |
| --- | --- |
| Mục tiêu | Cho phép user nội bộ đổi mật khẩu khi đang đăng nhập |
| Actor chính | `OPERATOR_ADMIN`, `OPERATOR_MANAGER`, `STATION_OPERATOR` |
| Priority | P2 |
| Tần suất | Không thường xuyên |
| Dữ liệu chính | Account password |

### Tiền Điều Kiện

- User đã đăng nhập.
- Account đang active.

### Luồng Chính

| Bước | Actor/System | Mô tả |
| --- | --- | --- |
| 1 | Actor | Mở chức năng đổi mật khẩu |
| 2 | Actor | Nhập mật khẩu hiện tại |
| 3 | Actor | Nhập mật khẩu mới và xác nhận mật khẩu mới |
| 4 | System | Kiểm tra mật khẩu hiện tại |
| 5 | System | Validate strength của mật khẩu mới |
| 6 | System | Cập nhật password hash |
| 7 | System | Ghi audit đổi mật khẩu |

### Luồng Thay Thế/Lỗi

| Mã | Điều kiện | Kết quả |
| --- | --- | --- |
| UC03-E01 | Mật khẩu hiện tại sai | Từ chối đổi mật khẩu |
| UC03-E02 | Mật khẩu mới không đạt policy | Từ chối đổi mật khẩu |
| UC03-E03 | Xác nhận mật khẩu không khớp | Từ chối đổi mật khẩu |

### Hậu Điều Kiện

- Password mới có hiệu lực.
- User dùng mật khẩu mới cho lần đăng nhập tiếp theo.

## UC04 - Quên Mật Khẩu

### Bảng Mô Tả

| Thuộc tính | Nội dung |
| --- | --- |
| Mục tiêu | Cho phép user nội bộ khôi phục quyền truy cập khi quên mật khẩu |
| Actor chính | `OPERATOR_ADMIN` |
| Priority | P2 |
| Tần suất | Không thường xuyên |
| Dữ liệu chính | Account, admin reset, must change password |

### Tiền Điều Kiện

- Account tồn tại.
- Account đang active.
- Hệ thống sử dụng cơ chế `OPERATOR_ADMIN` reset mật khẩu.

### Luồng Chính

| Bước | Actor/System | Mô tả |
| --- | --- | --- |
| 1 | Actor | Báo quên mật khẩu cho `OPERATOR_ADMIN` |
| 2 | OPERATOR_ADMIN | Tìm account |
| 3 | OPERATOR_ADMIN | Nhập mật khẩu tạm mới |
| 4 | System | Kiểm tra mật khẩu tạm đạt password policy |
| 5 | System | Cập nhật password hash |
| 6 | System | Đánh dấu account bắt buộc đổi mật khẩu ở lần đăng nhập tiếp theo |
| 7 | OPERATOR_ADMIN | Chuyển mật khẩu tạm cho user qua kênh ngoài hệ thống như nói trực tiếp, điện thoại hoặc chat nội bộ |
| 8 | System | Ghi audit reset mật khẩu |

### Luồng Thay Thế/Lỗi

| Mã | Điều kiện | Kết quả |
| --- | --- | --- |
| UC04-E01 | Account không tồn tại | Từ chối reset |
| UC04-E02 | Account bị khóa | Từ chối reset hoặc yêu cầu mở khóa trước |
| UC04-E03 | OPERATOR_ADMIN không có quyền quản lý account đó | Từ chối |
| UC04-E04 | Mật khẩu tạm không đạt policy | Từ chối cập nhật |

### Hậu Điều Kiện

- User có thể đăng nhập lại bằng mật khẩu mới.
- Audit reset mật khẩu được ghi.

### Ghi Chú

- Đây là chức năng P2, không phải luồng chính AFC.
- Không dùng OTP trong auth nội bộ C3/C4.
- Không dùng email reset trong MVP vì hệ thống không có nghiệp vụ bắt buộc gửi mail.
- Email reset/link reset chỉ đưa vào future scope nếu sau này bổ sung mail service thật.
- Hệ thống không gửi mật khẩu tạm. Việc chuyển mật khẩu tạm là quy trình nội bộ ngoài hệ thống.
- User phải đổi mật khẩu sau khi đăng nhập bằng mật khẩu tạm.
- `OPERATOR_ADMIN` chỉ biết mật khẩu tạm trong lúc bàn giao/reset, không biết mật khẩu thật sau khi user đổi.

## UC05 - Quản Lý Tuyến

### Bảng Mô Tả

| Thuộc tính | Nội dung |
| --- | --- |
| Mục tiêu | Quản lý danh mục tuyến thuộc một operator |
| Actor chính | `OPERATOR_MANAGER` |
| Priority | P0 |
| Tần suất | Khi cấu hình hoặc thay đổi mạng lưới |
| Dữ liệu chính | Operator, route |

### Tiền Điều Kiện

- `OPERATOR_MANAGER` đã đăng nhập.
- Operator đã tồn tại.

### Luồng Chính

| Bước | Actor/System | Mô tả |
| --- | --- | --- |
| 1 | OPERATOR_MANAGER | Mở danh sách tuyến |
| 2 | OPERATOR_MANAGER | Tạo tuyến mới hoặc chọn tuyến cần sửa |
| 3 | OPERATOR_MANAGER | Nhập route code, route name, transport type |
| 4 | System | Validate route code trong operator |
| 5 | System | Lưu route |
| 6 | System | Ghi audit thay đổi master data |

### Luồng Thay Thế - Import Tuyến Hàng Loạt

| Bước | Actor/System | Mô tả |
| --- | --- | --- |
| A1 | OPERATOR_MANAGER | Tải lên file CSV/Excel theo mẫu |
| A2 | System | Kiểm tra cấu trúc file và các cột bắt buộc |
| A3 | System | Validate toàn bộ route code và dữ liệu từng dòng |
| A4 | System | Hiển thị kết quả preview hoặc danh sách lỗi |
| A5 | OPERATOR_MANAGER | Xác nhận import |
| A6 | System | Tạo/cập nhật các tuyến hợp lệ trong một transaction |

### Luồng Thay Thế/Lỗi

| Mã | Điều kiện | Kết quả |
| --- | --- | --- |
| UC05-E01 | Route code trùng trong operator | Từ chối lưu |
| UC05-E02 | Dữ liệu bắt buộc thiếu | Từ chối lưu |
| UC05-E03 | Route đã có station/transaction | Không xóa cứng, chỉ disable |
| UC05-E04 | File import sai cấu trúc hoặc có dòng lỗi | Không import, trả báo cáo lỗi |

### Hậu Điều Kiện

- Route sẵn sàng để gán station và thiết bị.

## UC06 - Quản Lý Ga/Trạm

### Bảng Mô Tả

| Thuộc tính | Nội dung |
| --- | --- |
| Mục tiêu | Quản lý danh mục ga/trạm thuộc tuyến |
| Actor chính | `OPERATOR_MANAGER` |
| Priority | P0 |
| Tần suất | Khi cấu hình hoặc thay đổi mạng lưới |
| Dữ liệu chính | Route, station |

### Tiền Điều Kiện

- `OPERATOR_MANAGER` đã đăng nhập.
- Route đã tồn tại.

### Luồng Chính

| Bước | Actor/System | Mô tả |
| --- | --- | --- |
| 1 | OPERATOR_MANAGER | Mở danh sách station của route |
| 2 | OPERATOR_MANAGER | Tạo station mới hoặc chọn station cần sửa |
| 3 | OPERATOR_MANAGER | Nhập station code, station name, station order |
| 4 | System | Validate station code trong route |
| 5 | System | Validate station order nếu có cấu hình thứ tự |
| 6 | System | Lưu station |

### Luồng Thay Thế - Import Ga/Trạm Hàng Loạt

| Bước | Actor/System | Mô tả |
| --- | --- | --- |
| A1 | OPERATOR_MANAGER | Tải lên file CSV/Excel theo mẫu |
| A2 | System | Kiểm tra route code, station code, station name và station order |
| A3 | System | Validate toàn bộ dữ liệu và thứ tự ga/trạm |
| A4 | System | Hiển thị preview hoặc danh sách lỗi |
| A5 | OPERATOR_MANAGER | Xác nhận import |
| A6 | System | Tạo/cập nhật ga/trạm trong một transaction |

### Luồng Thay Thế/Lỗi

| Mã | Điều kiện | Kết quả |
| --- | --- | --- |
| UC06-E01 | Station code trùng trong route | Từ chối lưu |
| UC06-E02 | Station order trùng hoặc không hợp lệ | Từ chối lưu |
| UC06-E03 | Station đã có device/transaction | Không xóa cứng, chỉ disable |
| UC06-E04 | File import sai cấu trúc hoặc có dòng lỗi | Không import, trả báo cáo lỗi |

### Hậu Điều Kiện

- Station sẵn sàng để gán device và nhận dữ liệu từ Cấp 2.

## UC07 - Quản Lý Danh Mục Thiết Bị AFC

### Bảng Mô Tả

| Thuộc tính | Nội dung |
| --- | --- |
| Mục tiêu | Khai báo danh mục và thông tin quản trị của thiết bị Cấp 2 thuộc station |
| Actor chính | `OPERATOR_MANAGER` |
| Priority | P0 |
| Tần suất | Khi lắp đặt, bảo trì hoặc thay đổi thiết bị |
| Dữ liệu chính | Device, station |

### Tiền Điều Kiện

- `OPERATOR_MANAGER` đã đăng nhập.
- Station đã tồn tại.

### Luồng Chính

| Bước | Actor/System | Mô tả |
| --- | --- | --- |
| 1 | OPERATOR_MANAGER | Mở danh sách thiết bị |
| 2 | OPERATOR_MANAGER | Tạo hoặc cập nhật device |
| 3 | OPERATOR_MANAGER | Nhập device code, device type, direction, station |
| 4 | System | Validate device code unique |
| 5 | System | Lưu device |
| 6 | OPERATOR_MANAGER | Chuyển trạng thái device nếu cần |

### Luồng Thay Thế - Import Thiết Bị Hàng Loạt

| Bước | Actor/System | Mô tả |
| --- | --- | --- |
| A1 | OPERATOR_MANAGER | Tải lên file CSV/Excel theo mẫu |
| A2 | System | Kiểm tra device code, device type, direction và station code |
| A3 | System | Validate toàn bộ thiết bị trong file |
| A4 | System | Hiển thị preview hoặc danh sách lỗi |
| A5 | OPERATOR_MANAGER | Xác nhận import |
| A6 | System | Tạo/cập nhật thiết bị trong một transaction |

### Luồng Thay Thế/Lỗi

| Mã | Điều kiện | Kết quả |
| --- | --- | --- |
| UC07-E01 | Device code trùng | Từ chối lưu |
| UC07-E02 | Station không tồn tại | Từ chối lưu |
| UC07-E03 | Device đã có transaction | Không xóa cứng, chỉ disable/maintenance |
| UC07-E04 | File import sai cấu trúc hoặc có dòng lỗi | Không import, trả báo cáo lỗi |

### Hậu Điều Kiện

- Device có thể gửi tín hiệu trạng thái định kỳ và lượt quét.

## UC08 - Thiết Bị Gửi Tín Hiệu Trạng Thái Định Kỳ

### Bảng Mô Tả

| Thuộc tính | Nội dung |
| --- | --- |
| Mục tiêu | Cấp 3 nhận tín hiệu trạng thái định kỳ để biết thiết bị Cấp 2 vẫn đang kết nối và hoạt động |
| Actor chính | `DEVICE_CLIENT` |
| Priority | P0 |
| Tần suất | Định kỳ theo thiết bị |
| Dữ liệu chính | Device status, heartbeat payload |

### Tiền Điều Kiện

- Device đã được khai báo.

### Luồng Chính

| Bước | Actor/System | Mô tả |
| --- | --- | --- |
| 1 | DEVICE_CLIENT | Gửi heartbeat (tín hiệu trạng thái định kỳ) gồm `deviceCode`, status, firmware version, sentAt |
| 2 | System | Tìm device theo `deviceCode` |
| 3 | System | Cập nhật `lastSeenAt`, status, firmware version |
| 4 | System | Lưu heartbeat log vào MongoDB nếu bật trace |
| 5 | System | Trả kết quả accepted |

### Luồng Thay Thế/Lỗi

| Mã | Điều kiện | Kết quả |
| --- | --- | --- |
| UC08-E01 | Device không tồn tại | Từ chối hoặc ghi unknown-device log |
| UC08-E02 | Payload sai định dạng | Từ chối |
| UC08-E03 | Device disabled | Ghi nhận tín hiệu nhưng không cho xử lý lượt quét |

### Hậu Điều Kiện

- Trạng thái hiện tại của thiết bị được cập nhật.

## UC09 - Theo Dõi Trạng Thái Hoạt Động Thiết Bị

### Bảng Mô Tả

| Thuộc tính | Nội dung |
| --- | --- |
| Mục tiêu | Nhân sự xem thiết bị nào đang online, offline, maintenance hoặc disabled theo station hoặc toàn operator |
| Actor chính | `STATION_OPERATOR`, `OPERATOR_MANAGER` |
| Priority | P0 |
| Tần suất | Thường xuyên khi vận hành |
| Dữ liệu chính | Device, status, lastSeenAt |

### Tiền Điều Kiện

- User đã đăng nhập.
- Có dữ liệu device.

### Luồng Chính

| Bước | Actor/System | Mô tả |
| --- | --- | --- |
| 1 | Actor | Mở màn hình giám sát thiết bị |
| 2 | System | Xác định phạm vi quyền của actor |
| 3 | Actor | Lọc theo route/station/status/device type |
| 4 | System | Trả danh sách thiết bị |
| 5 | Actor | Xem chi tiết trạng thái và thời điểm nhận tín hiệu trạng thái gần nhất |

### Luồng Thay Thế/Lỗi

| Mã | Điều kiện | Kết quả |
| --- | --- | --- |
| UC09-E01 | STATION_OPERATOR truy cập station không có quyền | Từ chối |
| UC09-E02 | Không có dữ liệu phù hợp bộ lọc | Trả danh sách rỗng |

### Hậu Điều Kiện

- Nhân sự nắm được trạng thái vận hành hiện tại của thiết bị.

## UC10 - Ghi Nhận Lượt Quét Tại Thiết Bị Soát Vé

### Bảng Mô Tả

| Thuộc tính | Nội dung |
| --- | --- |
| Mục tiêu | Cấp 3 nhận sự kiện khi C2 giả lập bằng webcam scan QR động trên App, chuyển lên Cấp 4 verify, ghi transaction và trả decision |
| Actor chính | `DEVICE_CLIENT` |
| Priority | P0 |
| Tần suất | Mỗi lượt thiết bị gửi event |
| Dữ liệu chính | QR payload, lượt quét, card, ticket/entitlement, transaction, raw payload |

### Tiền Điều Kiện

- Device/webcam giả lập active.
- Station/route hợp lệ.
- QR payload do C4 sinh từ card/ticket/entitlement hiện hành.

### Luồng Chính

| Bước | Actor/System | Mô tả |
| --- | --- | --- |
| 1 | DEVICE_CLIENT | Webcam/app giả lập C2 scan QR động đang hiển thị trên App |
| 2 | DEVICE_CLIENT | Gửi lượt quét lên C3 gồm `eventId`, `deviceCode`, `stationCode`, `qrPayload`, `tapType`, `occurredAt` |
| 3 | System | C3 kiểm tra station/device context và forward event lên C4 |
| 4 | System | C4 kiểm tra idempotency theo `deviceCode + eventId` |
| 5 | System | C4 verify QR payload: chữ ký, expiry, nonce/replay |
| 6 | System | C4 map QR payload về card/ticket/entitlement trong read model hoặc Redis |
| 7 | System | C4 kiểm tra device active, station/direction/tap type |
| 8 | System | C4 kiểm tra card active và không bị blacklist/cancelled |
| 9 | System | Nếu là vé tháng, C4 kiểm tra entitlement active/còn hạn và phạm vi tuyến |
| 10 | System | Nếu là vé lượt Metro và `tapType = TAP_IN`, C4 kiểm tra ticket `UNUSED`, còn hạn và đúng phạm vi tuyến |
| 11 | System | Nếu là vé lượt Metro và `tapType = TAP_OUT`, C4 kiểm tra ticket `IN_USE` và card không bị chặn |
| 12 | System | C4 ghi transaction vận hành |
| 13 | System | C4 lưu raw payload vào MongoDB nếu bật trace |
| 14 | System | C4 trả decision về C3, C3 trả về C2 |

### Luồng Thay Thế/Lỗi

| Mã | Điều kiện | Kết quả |
| --- | --- | --- |
| UC10-E01 | Gửi lại cùng event và cùng payload | Trả lại kết quả cũ |
| UC10-E02 | Gửi lại cùng event nhưng payload khác | Từ chối conflict |
| UC10-E03 | Device không active | Trả `DENY` |
| UC10-E04 | Station/direction không hợp lệ | Trả `DENY` |
| UC10-E05 | QR payload hết hạn/sai chữ ký/replay | Trả `DENY` |
| UC10-E06 | Không map được QR về card/ticket/entitlement | Trả `DENY` |
| UC10-E07 | Entitlement hết hạn hoặc không active | Trả `DENY` |
| UC10-E08 | Card bị blacklist/cancelled/inactive | Trả `DENY` |
| UC10-E09 | Vé lượt Metro `TAP_IN` nhưng ticket không `UNUSED`, hết hạn, đã dùng hoặc sai phạm vi tuyến | Trả `DENY` |
| UC10-E10 | Vé lượt bus | Trả `DENY` vì MVP không hỗ trợ vé lượt bus |

### Hậu Điều Kiện

- Transaction được ghi nhận với đủ dữ liệu để C5 xác nhận trạng thái sử dụng ticket và phục vụ clearing nếu là vé lượt Metro.
- Thiết bị nhận `OPEN_GATE`, `DENY` hoặc `ACCEPTED_FOR_FORWARDING`.

## UC11 - Tra Cứu Transaction Vận Hành

### Bảng Mô Tả

| Thuộc tính | Nội dung |
| --- | --- |
| Mục tiêu | Nhân sự tra cứu transaction đã ghi nhận |
| Actor chính | `STATION_OPERATOR`, `OPERATOR_MANAGER` |
| Priority | P0 |
| Tần suất | Khi giám sát hoặc xử lý vận hành |
| Dữ liệu chính | Transaction |

### Tiền Điều Kiện

- User đã đăng nhập.
- Có transaction trong hệ thống.

### Luồng Chính

| Bước | Actor/System | Mô tả |
| --- | --- | --- |
| 1 | Actor | Mở màn hình transaction |
| 2 | Actor | Nhập bộ lọc thời gian, route, station, device, decision |
| 3 | System | Kiểm tra phạm vi quyền |
| 4 | System | Trả danh sách transaction |
| 5 | Actor | Chọn một transaction để xem chi tiết |
| 6 | System | Hiển thị chi tiết và raw payload ref nếu có quyền |

### Luồng Thay Thế/Lỗi

| Mã | Điều kiện | Kết quả |
| --- | --- | --- |
| UC11-E01 | STATION_OPERATOR xem ngoài phạm vi | Từ chối |
| UC11-E02 | Không có transaction phù hợp | Trả danh sách rỗng |

### Hậu Điều Kiện

- Actor xem được transaction đúng phạm vi.

## UC12 - Ghi Nhận Incident Thiết Bị

### Bảng Mô Tả

| Thuộc tính | Nội dung |
| --- | --- |
| Mục tiêu | Cấp 3 nhận incident/error event từ thiết bị |
| Actor chính | `DEVICE_CLIENT` |
| Priority | P1 |
| Tần suất | Khi thiết bị phát sinh lỗi |
| Dữ liệu chính | Incident log |

### Tiền Điều Kiện

- Device đã được khai báo.

### Luồng Chính

| Bước | Actor/System | Mô tả |
| --- | --- | --- |
| 1 | DEVICE_CLIENT | Gửi incident gồm deviceCode, incidentType, severity, message, occurredAt |
| 2 | System | Tìm device theo deviceCode |
| 3 | System | Lưu incident log vào MongoDB |
| 4 | System | Cập nhật trạng thái device nếu incident ảnh hưởng trạng thái hiện tại |
| 5 | System | Trả kết quả accepted |

### Luồng Thay Thế/Lỗi

| Mã | Điều kiện | Kết quả |
| --- | --- | --- |
| UC12-E01 | Device không tồn tại | Ghi unknown-device incident hoặc từ chối |
| UC12-E02 | Payload sai định dạng | Từ chối |

### Hậu Điều Kiện

- Incident được lưu để trace.
- Device status được cập nhật nếu cần.

## UC13 - Theo Dõi Incident Thiết Bị

### Bảng Mô Tả

| Thuộc tính | Nội dung |
| --- | --- |
| Mục tiêu | Nhân sự xem incident theo station hoặc toàn operator |
| Actor chính | `STATION_OPERATOR`, `OPERATOR_MANAGER` |
| Priority | P1 |
| Tần suất | Khi giám sát thiết bị |
| Dữ liệu chính | Incident log |

### Tiền Điều Kiện

- User đã đăng nhập.
- Có incident log trong MongoDB.

### Luồng Chính

| Bước | Actor/System | Mô tả |
| --- | --- | --- |
| 1 | Actor | Mở màn hình incident |
| 2 | Actor | Lọc theo thời gian, station, device, severity, incident type |
| 3 | System | Kiểm tra phạm vi quyền |
| 4 | System | Trả danh sách incident |
| 5 | Actor | Xem chi tiết incident |

### Luồng Thay Thế/Lỗi

| Mã | Điều kiện | Kết quả |
| --- | --- | --- |
| UC13-E01 | STATION_OPERATOR xem ngoài phạm vi | Từ chối |
| UC13-E02 | Không có dữ liệu phù hợp | Trả danh sách rỗng |

### Hậu Điều Kiện

- Incident được truy vết phục vụ vận hành.

## UC14 - Đồng Bộ Card, Ticket, Entitlement Từ Cấp 5

### Bảng Mô Tả

| Thuộc tính | Nội dung |
| --- | --- |
| Mục tiêu | Cấp 4 nhận dữ liệu card, ticket, entitlement và card status từ Cấp 5 để lưu read model, cập nhật Redis runtime và phục vụ cấp QR/verify lượt quét |
| Actor chính | `LEVEL5_SYSTEM`, System |
| Priority | P1 |
| Tần suất | Khi Cấp 5 phát hành dữ liệu mới hoặc theo lịch đồng bộ |
| Dữ liệu chính | Card, ticket, entitlement, card status/blacklist, payload đồng bộ |

### Tiền Điều Kiện

- Endpoint/mock Cấp 5 đã được cấu hình.
- Cấp 4 có thông tin xác thực để gọi hoặc nhận dữ liệu từ Cấp 5.

### Luồng Chính

| Bước | Actor/System | Mô tả |
| --- | --- | --- |
| 1 | LEVEL5_SYSTEM/System | Cấp 5 push dữ liệu hoặc Cấp 4 pull dữ liệu theo lịch |
| 2 | System | Kiểm tra xác thực nguồn gửi và version dữ liệu |
| 3 | System | Kiểm tra idempotency để tránh xử lý lại cùng version |
| 4 | System | Validate payload theo loại dữ liệu nhận: card, ticket, entitlement hoặc card status |
| 5 | System | Lưu payload lớn vào MongoDB nếu cần |
| 6 | System | Upsert read model tương ứng trong C4 và cập nhật Redis runtime |
| 7 | System | Nếu là card status/blacklist cần phân phối local, đánh dấu dữ liệu sẵn sàng để phát hành xuống Cấp 3 qua UC16 |

### Luồng Thay Thế/Lỗi

| Mã | Điều kiện | Kết quả |
| --- | --- | --- |
| UC14-E01 | Nguồn gửi không hợp lệ hoặc sai chữ ký/token | Từ chối nhận, ghi integration log |
| UC14-E02 | Version đã nhận trước đó | Không xử lý lại, trả kết quả idempotent |
| UC14-E03 | Payload sai định dạng | Từ chối nhận, ghi lỗi đồng bộ |
| UC14-E04 | Cấp 5 unavailable khi pull | Ghi lỗi và chờ retry |

### Hậu Điều Kiện

- Cấp 4 có read model mới nhất từ Cấp 5 để verify QR/tap event.
- Redis runtime được cập nhật theo dữ liệu mới nếu cần.
- Dữ liệu card status/blacklist chưa tự động áp dụng xuống Cấp 3 cho tới khi được phát hành ở UC16.

### Ghi Chú

- UC14 là system integration UC, không phải nghiệp vụ thao tác bởi người dùng.
- `cards` tối thiểu gồm `cardId`, `cardType`, `status`, `statusReason`, `sourceVersion`.
- `tickets` tối thiểu gồm `ticketId`, `cardId`, `ticketType`, `usageStatus`, `validFrom`, `validTo`, `routeScope`.
- `entitlements` tối thiểu gồm `entitlementId`, `cardId`, `fareProductCode`, `status`, `validFrom`, `validTo`, `passScope`.
- Card status/blacklist được biểu diễn trong `cards.status` và `cards.statusReason`; C4 không lưu lịch sử blacklist chính thức.
- Whitelist không triển khai trong MVP vì kéo theo hồ sơ hành khách, độ tuổi/nghề nghiệp/đối tượng ưu đãi và xác minh điều kiện.
- Cấp 4 không tự định nghĩa trạng thái chặn toàn mạng trong phạm vi MVP; Cấp 4 chỉ nhận, lưu, giám sát và phân phối xuống Cấp 3.
- Cấp 4 không lưu wallet/card balance trong MVP.

## UC15 - Tạo Control Package Cấu Hình Vận Hành

### Bảng Mô Tả

| Thuộc tính | Nội dung |
| --- | --- |
| Mục tiêu | Cấp 4 tạo control package cho các cấu hình vận hành nội bộ của operator |
| Actor chính | `OPERATOR_MANAGER` |
| Priority | P1 |
| Tần suất | Khi thay đổi cấu hình vận hành |
| Dữ liệu chính | Control package, payload |

### Tiền Điều Kiện

- `OPERATOR_MANAGER` đã đăng nhập.
- Package type thuộc phạm vi Cấp 4 được phép tạo.

### Luồng Chính

| Bước | Actor/System | Mô tả |
| --- | --- | --- |
| 1 | OPERATOR_MANAGER | Mở màn hình tạo control package |
| 2 | OPERATOR_MANAGER | Chọn package type trong phạm vi Cấp 4, MVP chủ yếu là `DEVICE_CONFIG` |
| 3 | OPERATOR_MANAGER | Nhập payload cấu hình |
| 4 | System | Validate payload theo schema của package type |
| 5 | System | Lưu metadata package ở trạng thái `CREATED` |
| 6 | System | Lưu payload lớn vào MongoDB nếu cần |
| 7 | System | Ghi audit tạo control package |

### Luồng Thay Thế/Lỗi

| Mã | Điều kiện | Kết quả |
| --- | --- | --- |
| UC15-E01 | Package type không hợp lệ | Từ chối tạo |
| UC15-E02 | Payload sai định dạng | Từ chối tạo |
| UC15-E03 | User không có quyền tạo loại package đó | Từ chối |

### Hậu Điều Kiện

- Control package được tạo nhưng chưa được gửi xuống Cấp 3.
- Việc phát hành xuống Cấp 3 được thực hiện ở UC16.

### Ghi Chú

- UC15 chỉ dành cho package do Cấp 4 tạo.
- Card status/blacklist không được tạo thủ công ở UC15. Khi cần gửi xuống Cấp 3, UC16 chọn dữ liệu đã đồng bộ từ Cấp 5 ở UC14.

## UC16 - Phát Hành Control Package Xuống Cấp 3

### Bảng Mô Tả

| Thuộc tính | Nội dung |
| --- | --- |
| Mục tiêu | Cấp 4 chọn control package vận hành hoặc card status/blacklist đã đồng bộ từ Cấp 5 để phát hành xuống một hoặc nhiều station Cấp 3 |
| Actor chính | `OPERATOR_MANAGER`, System |
| Priority | P1 |
| Tần suất | Khi có package cần áp dụng xuống Cấp 3 |
| Dữ liệu chính | Control package, card status/blacklist package, station control sync |

### Tiền Điều Kiện

- `OPERATOR_MANAGER` đã đăng nhập nếu phát hành thủ công.
- Control package Cấp 4 đã được tạo ở UC15 hoặc card status/blacklist từ Cấp 5 đã được nhận ở UC14.
- Station đích tồn tại và active.

### Luồng Chính

| Bước | Actor/System | Mô tả |
| --- | --- | --- |
| 1 | OPERATOR_MANAGER/System | Chọn package cần phát hành |
| 2 | OPERATOR_MANAGER/System | Chọn station hoặc nhóm station áp dụng |
| 3 | System | Kiểm tra package đã sẵn sàng phát hành |
| 4 | System | Kiểm tra station đích hợp lệ |
| 5 | System | Tạo bản ghi `station_control_syncs` ở trạng thái `PENDING` |
| 6 | System | Cập nhật trạng thái package thành `PUBLISHED` nếu cần |
| 7 | System | Ghi audit/integration log phát hành |

### Luồng Thay Thế/Lỗi

| Mã | Điều kiện | Kết quả |
| --- | --- | --- |
| UC16-E01 | Package chưa sẵn sàng | Từ chối phát hành |
| UC16-E02 | Station không tồn tại hoặc disabled | Từ chối phát hành tới station đó |
| UC16-E03 | Đã có sync pending cùng package và station | Không tạo trùng, trả trạng thái hiện tại |

### Hậu Điều Kiện

- Cấp 3 có package mới để pull/receive ở UC17.
- Cấp 4 theo dõi được station nào đã nhận, đang chờ hoặc failed.

## UC17 - Cấp 3 Nhận Và Áp Dụng Control Package

### Bảng Mô Tả

| Thuộc tính | Nội dung |
| --- | --- |
| Mục tiêu | Cấp 3 nhận control package từ Cấp 4 và áp dụng local |
| Actor chính | `STATION_OPERATOR`, System |
| Priority | P1 |
| Tần suất | Khi có package mới |
| Dữ liệu chính | Control package, station sync status |

### Tiền Điều Kiện

- Control package đã được publish.
- Station thuộc phạm vi package.

### Luồng Chính

| Bước | Actor/System | Mô tả |
| --- | --- | --- |
| 1 | System/STATION_OPERATOR | Cấp 3 pull package mới nhất |
| 2 | System | Kiểm tra version package |
| 3 | System | Tải payload từ MongoDB nếu cần |
| 4 | System | Áp dụng config/rule local |
| 5 | System | Gửi ack `APPLIED` |
| 6 | System | Cập nhật sync status |

### Luồng Thay Thế/Lỗi

| Mã | Điều kiện | Kết quả |
| --- | --- | --- |
| UC17-E01 | Không có package mới | Trả version hiện tại |
| UC17-E02 | Payload không đọc được | Sync status `FAILED` |
| UC17-E03 | Apply local lỗi | Sync status `FAILED`, lưu error message |

### Hậu Điều Kiện

- Station/Cấp 3 có config/rule local mới nhất.
- Cấp 4 xem được station đã applied hay failed.

## UC18 - Dashboard Vận Hành Cấp 4

### Bảng Mô Tả

| Thuộc tính | Nội dung |
| --- | --- |
| Mục tiêu | Cấp 4 xem tổng quan vận hành AFC trực thuộc operator |
| Actor chính | `OPERATOR_MANAGER` |
| Priority | P0/P1 |
| Tần suất | Thường xuyên |
| Dữ liệu chính | Device, transaction, incident, batch |

### Tiền Điều Kiện

- `OPERATOR_MANAGER` đã đăng nhập.
- Hệ thống có dữ liệu vận hành.

### Luồng Chính

| Bước | Actor/System | Mô tả |
| --- | --- | --- |
| 1 | OPERATOR_MANAGER | Mở dashboard |
| 2 | System | Tổng hợp transaction theo ngày |
| 3 | System | Tổng hợp transaction theo route/station |
| 4 | System | Tổng hợp decision `OPEN_GATE`/`DENY` |
| 5 | System | Tổng hợp thiết bị theo status |
| 6 | System | Tổng hợp incident nếu có |
| 7 | System | Hiển thị trạng thái batch/control sync nếu có |

### Luồng Thay Thế/Lỗi

| Mã | Điều kiện | Kết quả |
| --- | --- | --- |
| UC18-E01 | Không có dữ liệu trong khoảng thời gian | Hiển thị số liệu 0 |
| UC18-E02 | Query quá rộng | Giới hạn khoảng thời gian hoặc yêu cầu lọc |

### Hậu Điều Kiện

- Operator manager nắm được tình trạng vận hành.

## UC19 - Tạo Batch Dữ Liệu

### Bảng Mô Tả

| Thuộc tính | Nội dung |
| --- | --- |
| Mục tiêu | Cấp 4 gom các transaction vận hành đủ điều kiện thành một batch ở trạng thái `CREATED` |
| Actor chính | `OPERATOR_MANAGER` |
| Priority | P1 |
| Tần suất | Theo ca/ngày hoặc khi cần gửi dữ liệu |
| Dữ liệu chính | Batch, transaction |

### Tiền Điều Kiện

- Có transaction hợp lệ.
- `OPERATOR_MANAGER` đã đăng nhập.

### Luồng Chính

| Bước | Actor/System | Mô tả |
| --- | --- | --- |
| 1 | OPERATOR_MANAGER | Chọn khoảng thời gian tạo batch |
| 2 | System | Tìm transaction đủ điều kiện |
| 3 | System | Tạo batch metadata |
| 4 | System | Gắn transaction vào batch |
| 5 | System | Trả batch ở trạng thái `CREATED` |

### Luồng Thay Thế/Lỗi

| Mã | Điều kiện | Kết quả |
| --- | --- | --- |
| UC19-E01 | Không có transaction đủ điều kiện | Không tạo batch hoặc tạo batch rỗng theo cấu hình |
| UC19-E02 | Transaction đã thuộc batch khác | Không chọn lại |

### Hậu Điều Kiện

- Batch đã được tạo và sẵn sàng cho UC20.
- UC19 không thực hiện gửi dữ liệu ra hệ thống Cấp 5.

## UC20 - Gửi Batch Dữ Liệu Lên Cấp 5

### Bảng Mô Tả

| Thuộc tính | Nội dung |
| --- | --- |
| Mục tiêu | Gửi batch dữ liệu vận hành lên Cấp 5/trung tâm liên thông để C5 xác nhận ticket usage, ghi nhận journey và phục vụ clearing |
| Actor chính | System, `LEVEL5_SYSTEM` |
| Priority | P1 |
| Tần suất | Sau khi batch được tạo |
| Dữ liệu chính | Batch, transaction, integration log, ticket/journey result nếu C5 trả về |

### Tiền Điều Kiện

- Batch ở trạng thái `CREATED` hoặc retryable.
- Endpoint Cấp 5/mock đã cấu hình.

### Luồng Chính

| Bước | Actor/System | Mô tả |
| --- | --- | --- |
| 1 | System | Lấy batch cần gửi |
| 2 | System | Build payload gửi Cấp 5 |
| 3 | System | Gửi request tới `LEVEL5_SYSTEM` |
| 4 | LEVEL5_SYSTEM | Trả ACK/REJECT |
| 5 | LEVEL5_SYSTEM | Với vé lượt Metro, C5 dùng batch để xác nhận `TAP_IN/TAP_OUT`, cập nhật ticket usage và phục vụ clearing |
| 6 | System | Cập nhật batch status |
| 7 | System | Nếu C5 trả ticket/journey result, C4 lưu kết quả tham chiếu để tra cứu |
| 8 | System | Lưu request/response vào MongoDB |

### Luồng Thay Thế/Lỗi

| Mã | Điều kiện | Kết quả |
| --- | --- | --- |
| UC20-E01 | Cấp 5 unavailable | Batch `FAILED` hoặc chờ retry |
| UC20-E02 | Cấp 5 reject | Batch `REJECTED`, lưu response |
| UC20-E03 | Gửi trùng batch | Idempotent theo batch code nếu có |

### Hậu Điều Kiện

- Batch có trạng thái gửi rõ ràng.
- Với vé lượt Metro, transaction đã được chuyển đủ dữ liệu để C5 xử lý trạng thái sử dụng ticket.
- Request/response được trace.

## UC21 - Audit Và Truy Vết

### Bảng Mô Tả

| Thuộc tính | Nội dung |
| --- | --- |
| Mục tiêu | Ghi nhận và tra cứu sự kiện quan trọng trong hệ thống nội bộ operator |
| Actor chính | `OPERATOR_ADMIN` |
| Priority | P1 |
| Tần suất | Theo hành động hệ thống |
| Dữ liệu chính | Audit log |

### Tiền Điều Kiện

- Audit logging được bật.

### Luồng Chính

| Bước | Actor/System | Mô tả |
| --- | --- | --- |
| 1 | System | Ghi audit đăng nhập/đăng xuất |
| 2 | System | Ghi audit thay đổi account/role |
| 3 | System | Ghi audit thay đổi route/station/device |
| 4 | System | Ghi audit phát hành control package |
| 5 | System | Ghi integration log nếu cần |
| 6 | OPERATOR_ADMIN | Tra cứu audit theo thời gian, actor, action, resource |

### Luồng Thay Thế/Lỗi

| Mã | Điều kiện | Kết quả |
| --- | --- | --- |
| UC21-E01 | User không có quyền xem audit | Từ chối |
| UC21-E02 | Query quá rộng | Yêu cầu giới hạn thời gian |

### Hậu Điều Kiện

- Có log phục vụ bảo mật và truy vết.

## UC22 - App Lấy QR Động Từ Card

### Bảng Mô Tả

| Thuộc tính | Nội dung |
| --- | --- |
| Mục tiêu | C4 sinh dynamic QR payload ngắn hạn từ card và quyền hiện hành để App render thành QR |
| Actor chính | `PASSENGER_APP`, System |
| Priority | P0 |
| Tần suất | Mỗi 30-60 giây khi App đang hiển thị QR |
| Dữ liệu chính | Dynamic QR payload, card/ticket/entitlement read model, Redis QR session |

### Tiền Điều Kiện

- App đã có `cardId` từ C5 sau khi hành khách mua vé/đăng ký card.
- C4 đã có card và ticket/entitlement mới nhất từ C5 qua UC14.
- Card không bị blacklist/cancelled/inactive.

### Luồng Chính

| Bước | Actor/System | Mô tả |
| --- | --- | --- |
| 1 | PASSENGER_APP | Gửi request lấy QR payload bằng `cardId` và loại quyền muốn dùng nếu cần |
| 2 | System | C4 kiểm tra card tồn tại và active |
| 3 | System | Nếu là vé tháng, C4 kiểm tra entitlement active, còn hạn và đúng phạm vi |
| 4 | System | Nếu là vé lượt Metro, C4 kiểm tra ticket còn hạn và `usageStatus` cho phép hiển thị QR |
| 5 | System | C4 sinh `qrId`, `nonce`, `expiresAt` và reference mã hóa tới card/ticket/entitlement |
| 6 | System | C4 ký QR payload bằng secret/private key của C4 |
| 7 | System | C4 lưu QR session trong Redis để verify khi C2 scan |
| 8 | System | C4 trả QR payload cho App |
| 9 | PASSENGER_APP | App render QR và tự refresh định kỳ |

### Luồng Thay Thế/Lỗi

| Mã | Điều kiện | Kết quả |
| --- | --- | --- |
| UC22-E01 | Không có card active | Từ chối cấp QR |
| UC22-E02 | Entitlement hết hạn hoặc inactive | Từ chối cấp QR |
| UC22-E03 | Card bị blacklist/cancelled/inactive | Từ chối cấp QR |
| UC22-E04 | Ticket/entitlement chưa đồng bộ | Từ chối hoặc yêu cầu sync lại |
| UC22-E05 | Vé lượt Metro hết hạn, đã dùng hoặc không thuộc phạm vi hợp lệ | Từ chối cấp QR |

### Hậu Điều Kiện

- App có QR động để hiển thị.
- QR payload hết hạn nhanh và không thay thế ticket/entitlement gốc.
- Operator manager không tham gia cấp QR; đây là luồng system runtime tự động.

## 3. Out Of Scope

- Đăng ký/đăng nhập hành khách đầy đủ như một passenger identity platform riêng.
- Passenger profile sâu, xác minh giấy tờ, nghề nghiệp, độ tuổi, đối tượng ưu đãi.
- Whitelist/card ưu đãi theo hồ sơ hành khách.
- QR tĩnh.
- EMV, biometric.
- Thiết bị đọc card vật lý thật ở C2.
- Cổng soát vé/TVM/TOM thật.
- Payment thật, ngân hàng thật, trừ tiền realtime.
- Wallet/card balance cho vé lượt; vé lượt MVP dùng ticket prepaid.
- Fare engine tại C3/C4; C4 chỉ lưu read model card/ticket/entitlement để vận hành QR và tap event.
- Clearing/settlement production-grade tại C3/C4.
- Tính toán phân bổ doanh thu tại C3/C4; nghiệp vụ này thuộc Cấp 5/FMC.
- Đối soát tài chính tại C3/C4.
- Quản lý bộ quy tắc chung toàn mạng của Cấp 5.
- Quản lý an toàn thông tin liên thông toàn hệ thống AFC.

