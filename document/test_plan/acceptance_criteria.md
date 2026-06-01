# TIÊU CHÍ NGHIỆM THU USE CASE (ACCEPTANCE CRITERIA)

**Dự án:** Hệ thống Quản lý Vé tháng Giao thông Công cộng tự động (PFC/AFC MVP)  
**Phạm vi:** 27 Use Case trong `use_case_specifications.md`  
**Tài liệu nguồn:** `SRS_MetroBusTicket.md`, `use_case_specifications.md`, `role_and_scope_analysis.md`

---

## 1. Nguyên tắc nghiệm thu

- Các tiêu chí dưới đây chỉ áp dụng cho phạm vi MVP phần mềm đã chốt, không mở rộng sang AFC Gate thật, RFID, IC Card, TVM/Kiosk, Handheld, BOS hoặc thanh toán thẻ ngân hàng open-loop tại cổng.
- Webcam Gate Simulator chỉ xác thực Dynamic QR của thẻ/vé điện tử đã mua trước và ghi nhận hành trình. Validator không nạp tiền hoặc trừ tiền ví tại thời điểm quét.
- Dữ liệu của `COMPANY_MANAGER` và `STAFF` phải bị giới hạn theo `operator_id`. Người dùng không được đọc hoặc sửa dữ liệu của đơn vị vận hành khác.
- Mọi giao dịch tài chính phải chống xử lý trùng, lưu được lịch sử và có trạng thái rõ ràng.
- Các thao tác tiền mặt tại quầy phải gắn với `staff_id`, `shift_id` đang hoạt động và lý do nghiệp vụ nếu có.
- Các tiêu chí được đánh dấu `[P0]` là bắt buộc để nghiệm thu MVP. `[P1]` là tiêu chí hoàn thiện trong phạm vi đã định nghĩa, có thể được triển khai sau khi luồng chính ổn định.

---

## 2. Module 1: Xác thực & Tài khoản

### UC01: Đăng ký & Đăng nhập bằng Số điện thoại

- [ ] **AC-UC01-01 [P0]** Given hành khách nhập số điện thoại hợp lệ chưa có tài khoản, when nhấn "Tiếp tục", then hệ thống sinh OTP 6 chữ số, gửi SMS và ghi nhận thời hạn hiệu lực 2 phút.
- [ ] **AC-UC01-02 [P0]** Given số điện thoại đã có tài khoản đang hoạt động, when nhấn "Tiếp tục", then hệ thống không gửi OTP và trả trạng thái để PWA chuyển sang màn hình nhập mật khẩu.
- [ ] **AC-UC01-03 [P0]** Given OTP đăng ký hợp lệ và còn hạn, when hành khách xác nhận, then hệ thống cho phép chuyển sang bước đặt mật khẩu, chưa đăng nhập nếu chưa có mật khẩu.
- [ ] **AC-UC01-04 [P0]** Given hành khách đặt mật khẩu thành công sau khi xác minh OTP đăng ký, then hệ thống tạo `accounts` với role `PASSENGER`, tạo ví `PASSENGER` có số dư `0`, cấp JWT và đăng nhập thành công.
- [ ] **AC-UC01-05 [P0]** Given tài khoản đã tồn tại và mật khẩu đúng, when hành khách đăng nhập, then hệ thống cấp JWT và đăng nhập thành công.
- [ ] **AC-UC01-06 [P0]** Given OTP đăng ký sai hoặc hết hạn, when hành khách xác nhận, then hệ thống từ chối xác minh và không tạo tài khoản.
- [ ] **AC-UC01-07 [P0]** Given cùng số điện thoại và `purpose = REGISTER`, when yêu cầu OTP lần thứ 6 trong ngày hoặc yêu cầu lại trước 60 giây, then hệ thống không gửi SMS và trả HTTP `429`.
- [ ] **AC-UC01-08 [P0]** Given cùng IP, when tổng số yêu cầu OTP đăng ký vượt 20 lần trong 1 giờ, then hệ thống không gửi SMS và trả HTTP `429`.
- [ ] **AC-UC01-09 [P0]** Given OTP đăng ký đã bị nhập sai 5 lần, when xác minh lại, then hệ thống vô hiệu hóa OTP hiện tại và yêu cầu hành khách gửi OTP mới.
- [ ] **AC-UC01-10 [P0]** Given OTP đăng ký mới được gửi cho cùng số điện thoại, then OTP cũ bị ghi đè và không còn hợp lệ.

### UC02: Đăng nhập tài khoản nội bộ

- [ ] **AC-UC02-01 [P0]** Given tài khoản nội bộ hợp lệ và đang hoạt động, when nhập đúng username/password, then hệ thống trả JWT chứa role/permission tương ứng.
- [ ] **AC-UC02-02 [P0]** Given sai username hoặc password, when đăng nhập, then hệ thống từ chối và không cấp JWT.
- [ ] **AC-UC02-03 [P0]** Given tài khoản bị khóa, when đăng nhập hoặc sử dụng token cũ, then hệ thống từ chối truy cập.
- [ ] **AC-UC02-04 [P0]** Given người dùng thuộc role khác nhau, when đăng nhập thành công, then Portal chỉ hiển thị và cho phép gọi các chức năng đúng quyền.

### UC03: Đăng xuất tài khoản

- [ ] **AC-UC03-01 [P0]** Given người dùng đang đăng nhập, when đăng xuất, then JWT hiện tại được đưa vào blacklist với TTL bằng thời hạn còn lại của token.
- [ ] **AC-UC03-02 [P0]** Given JWT đã bị blacklist, when gọi API bảo vệ, then backend từ chối token.
- [ ] **AC-UC03-03 [P0]** Given đăng xuất thành công, when frontend nhận phản hồi, then token cục bộ bị xóa và người dùng được chuyển về trang đăng nhập.

### UC04: Thay đổi mật khẩu nội bộ

- [ ] **AC-UC04-01 [P0]** Given người dùng nội bộ nhập đúng mật khẩu cũ và mật khẩu mới đạt chính sách bảo mật, when xác nhận, then mật khẩu mới được lưu dưới dạng BCrypt.
- [ ] **AC-UC04-02 [P0]** Given mật khẩu cũ sai, when xác nhận, then hệ thống từ chối cập nhật.
- [ ] **AC-UC04-03 [P0]** Given mật khẩu mới không đạt chính sách hoặc xác nhận không khớp, when gửi yêu cầu, then hệ thống trả lỗi validation.
- [ ] **AC-UC04-04 [P1]** Given đổi mật khẩu thành công, when thao tác hoàn tất, then phiên cũ bị vô hiệu hóa và người dùng phải đăng nhập lại.

### UC05: Khôi phục mật khẩu

- [ ] **AC-UC05-01 [P0]** Given email nội bộ tồn tại, when yêu cầu khôi phục, then hệ thống sinh token reset có thời hạn 15 phút và gửi liên kết qua email.
- [ ] **AC-UC05-02 [P0]** Given token reset hợp lệ và còn hạn, when người dùng nhập mật khẩu mới hợp lệ, then hệ thống cập nhật mật khẩu BCrypt và vô hiệu hóa token reset.
- [ ] **AC-UC05-03 [P0]** Given token reset sai, đã dùng hoặc hết hạn, when xác nhận, then hệ thống từ chối cập nhật.
- [ ] **AC-UC05-04 [P1]** Given email không tồn tại, when gửi yêu cầu, then hệ thống xử lý theo chính sách phản hồi đã chốt và không tạo token reset.
- [ ] **AC-UC05-05 [P0]** Given số điện thoại Passenger đã tồn tại và tài khoản đang hoạt động, when yêu cầu quên mật khẩu, then hệ thống gửi OTP reset password qua SMS.
- [ ] **AC-UC05-06 [P0]** Given OTP reset password hợp lệ và còn hạn, when hành khách xác nhận, then hệ thống trả reset token tạm thời để đặt mật khẩu mới.
- [ ] **AC-UC05-07 [P0]** Given reset token từ OTP hợp lệ, when hành khách đặt mật khẩu mới hợp lệ, then hệ thống cập nhật mật khẩu BCrypt và vô hiệu hóa reset token/OTP.
- [ ] **AC-UC05-08 [P0]** Given OTP reset password sai, hết hạn hoặc nhập sai quá 5 lần, when hành khách xác nhận, then hệ thống từ chối cấp reset token.
- [ ] **AC-UC05-09 [P0]** Given cùng số điện thoại và `purpose = RESET_PASSWORD`, when yêu cầu OTP lần thứ 6 trong ngày hoặc yêu cầu lại trước 60 giây, then hệ thống không gửi SMS và trả HTTP `429`.

### UC06: Cập nhật hồ sơ cá nhân

- [ ] **AC-UC06-01 [P0]** Given người dùng đã đăng nhập, when cập nhật các trường hồ sơ hợp lệ, then dữ liệu tương ứng trong `accounts` được lưu.
- [ ] **AC-UC06-02 [P0]** Given passenger xác thực email bằng OTP hợp lệ trong 5 phút, when hoàn thiện hồ sơ, then email được xác minh và trạng thái KYC được cập nhật.
- [ ] **AC-UC06-03 [P0]** Given OTP email sai hoặc hết hạn, when xác nhận, then hệ thống không lưu trạng thái xác minh email và không kích hoạt KYC.
- [ ] **AC-UC06-04 [P1]** Given ảnh eKYC không đọc được, when OCR thất bại, then hệ thống thông báo lỗi và cho phép nhập thủ công.
- [ ] **AC-UC06-05 [P0]** Given tài khoản không phải chủ sở hữu hồ sơ, when cố cập nhật hồ sơ người khác, then backend từ chối.

---

## 3. Module 2: Thẻ & Vé tháng

### UC07: Đăng ký mua Thẻ cứng trực tuyến

- [ ] **AC-UC07-01 [P0]** Given guest nhập đủ hồ sơ giao nhận hợp lệ, when tạo đơn, then hệ thống tạo order chờ thanh toán mà không yêu cầu đăng nhập.
- [ ] **AC-UC07-02 [P0]** Given thanh toán online thành công, when callback hợp lệ được xử lý, then order chuyển `PRINTING`, thẻ vật lý được tạo với `card_medium = 'PHYSICAL'`, `owner_id = NULL`, và mã `card_uid` được gửi cho khách.
- [ ] **AC-UC07-03 [P0]** Given thanh toán bị hủy hoặc quá hạn 15 phút, when hệ thống xử lý trạng thái thanh toán, then order chuyển `CANCELLED` và không ghi nhận thanh toán thành công.
- [ ] **AC-UC07-04 [P0]** Given callback thanh toán bị gửi lặp, when backend xử lý, then không tạo trùng order, card hoặc transaction.

### UC08: Đăng ký và Phát hành Thẻ ảo trực tiếp

- [ ] **AC-UC08-01 [P0]** Given passenger đã hoàn tất KYC và xác thực email, when đăng ký thẻ ảo kèm gói vé hợp lệ và thanh toán thành công, then hệ thống tạo card `VIRTUAL` trạng thái `ACTIVE` liên kết đúng chủ sở hữu.
- [ ] **AC-UC08-02 [P0]** Given phát hành thành công, when kiểm tra dữ liệu, then subscription tương ứng được tạo ở trạng thái `ACTIVE` và hiển thị thời hạn trên PWA.
- [ ] **AC-UC08-03 [P0]** Given tài khoản chưa KYC hoặc chưa xác thực email, when đăng ký, then backend từ chối phát hành.
- [ ] **AC-UC08-04 [P0]** Given passenger đã có thẻ ảo hoạt động, when đăng ký thêm ngoài giới hạn cho phép, then backend từ chối.
- [ ] **AC-UC08-05 [P0]** Given số dư ví không đủ, when thanh toán bằng ví, then không tạo card/subscription dở dang và số dư không thay đổi.

### UC09: Số hóa Thẻ cứng thành Thẻ điện tử ảo

- [ ] **AC-UC09-01 [P0]** Given thẻ vật lý hợp lệ, chưa số hóa và thông tin CCCD khớp đơn hàng, when passenger xác nhận số hóa, then card vật lý chuyển `VIRTUALIZED`.
- [ ] **AC-UC09-02 [P0]** Given số hóa thành công, when kiểm tra dữ liệu, then hệ thống tạo card `VIRTUAL` trạng thái `ACTIVE`, liên kết đúng passenger và di trú subscription còn hiệu lực.
- [ ] **AC-UC09-03 [P0]** Given CCCD không khớp, when số hóa, then backend từ chối và không thay đổi card/subscription.
- [ ] **AC-UC09-04 [P0]** Given card vật lý đã `VIRTUALIZED`, when yêu cầu số hóa lại, then backend từ chối.

### UC10: Gia hạn gói vé chu kỳ

- [ ] **AC-UC10-01 [P0]** Given passenger đăng nhập và đủ số dư ví, when gia hạn subscription, then ví bị trừ đúng số tiền và ghi nhận transaction `SUCCESS`.
- [ ] **AC-UC10-02 [P0]** Given guest tra cứu card hợp lệ và thanh toán online thành công, when callback được xử lý, then subscription được gia hạn và transaction được ghi nhận.
- [ ] **AC-UC10-03 [P0]** Given subscription còn hạn, when gia hạn, then `end_date` mới được tính tiếp từ hạn cũ; nếu đã hết hạn thì tính từ thời điểm hiện tại.
- [ ] **AC-UC10-04 [P0]** Given card không tồn tại, không hợp lệ hoặc ví không đủ tiền, when gia hạn, then backend từ chối và không thay đổi subscription.
- [ ] **AC-UC10-05 [P0]** Given callback thanh toán bị gửi lặp, when xử lý, then subscription chỉ được gia hạn một lần.

### UC11: Khởi tạo lô phôi Thẻ cứng vật lý

- [ ] **AC-UC11-01 [P0]** Given staff có quyền và ca trực phù hợp, when nhập một `card_uid` mới hợp lệ, then hệ thống tạo card `PHYSICAL`, `ACTIVE`, `owner_id = NULL`.
- [ ] **AC-UC11-02 [P0]** Given tệp CSV hợp lệ, when import, then hệ thống tạo toàn bộ card hợp lệ và trả kết quả tổng hợp.
- [ ] **AC-UC11-03 [P0]** Given lô import có mã trùng, when xử lý, then hệ thống bỏ qua mã trùng, vẫn nạp mã hợp lệ và trả danh sách cảnh báo.
- [ ] **AC-UC11-04 [P0]** Given user không có role `STAFF` phù hợp hoặc khác tenant, when khởi tạo card, then backend từ chối.

### UC12: Thu hồi và Vô hiệu hóa Thẻ vật lý hỏng/lỗi

- [ ] **AC-UC12-01 [P0]** Given card vật lý đang `ACTIVE`, when staff thu hồi với lý do hợp lệ, then card chuyển `EXPIRED` và lưu vết thao tác.
- [ ] **AC-UC12-02 [P0]** Given card có subscription còn hạn, when thu hồi, then subscription được đóng băng hoặc đánh dấu chờ xử lý theo rule đã định nghĩa.
- [ ] **AC-UC12-03 [P0]** Given card không ở trạng thái `ACTIVE`, when thu hồi, then backend từ chối.
- [ ] **AC-UC12-04 [P0]** Given staff khác tenant hoặc không có quyền, when thu hồi, then backend từ chối.

---

## 4. Module 3: Soát vé & Vận hành quầy ga

### UC13: Quét soát vé tự động qua Gate Validator

- [ ] **AC-UC13-01 [P0]** Given Dynamic QR hợp lệ của card/vé điện tử đang hoạt động, when quét check-in, then hệ thống tạo journey `IN_PROGRESS` với ga vào và mở cổng giả lập.
- [ ] **AC-UC13-02 [P0]** Given journey `IN_PROGRESS` hợp lệ, when quét check-out, then hệ thống ghi ga ra, tính cự ly/giá trị liên quan, chuyển journey `COMPLETED` và mở cổng giả lập.
- [ ] **AC-UC13-03 [P0]** Given QR sai, hết cửa sổ thời gian hoặc card/vé không hợp lệ, when quét, then validator từ chối mở cổng và không tạo journey sai.
- [ ] **AC-UC13-04 [P0]** Given cùng card quét lại tại cùng trạm trong vòng 60 giây, when validator nhận yêu cầu, then anti-passback từ chối.
- [ ] **AC-UC13-05 [P0]** Given vé lượt đi quá ga đã mua hoặc card bị `LOCKED`, when check-out/check-in, then validator từ chối và hướng dẫn đến PSC.
- [ ] **AC-UC13-06 [P0]** Given validator xử lý quét, when kiểm tra transaction ví, then không có thao tác nạp hoặc trừ ví tại cổng.

### UC14: PSC Xử lý sự cố và Giải khóa Thẻ kẹt ga

- [ ] **AC-UC14-01 [P0]** Given staff có ca `ACTIVE`, when tra cứu card/vé lỗi, then Portal hiển thị journey bị treo và lý do cần xử lý.
- [ ] **AC-UC14-02 [P0]** Given vé lượt đi quá chặng, when staff thu bù chênh lệch, then hệ thống tạo giao dịch `CASH_FARE_ADJUSTMENT` gắn `shift_id`, hoàn tất journey và cho phép khách ra ga.
- [ ] **AC-UC14-03 [P0]** Given lỗi quên check-out do vi phạm, when staff giải khóa có thu phạt, then hệ thống tạo `CASH_PENALTY`, hoàn tất journey cũ và chuyển card về `ACTIVE`.
- [ ] **AC-UC14-04 [P0]** Given lỗi khách quan được xác nhận, when staff giải khóa miễn phí, then hệ thống hoàn tất journey, chuyển card về `ACTIVE` và lưu audit lý do với số tiền `0`.
- [ ] **AC-UC14-05 [P0]** Given ca trực đã `CLOSED`, when staff xử lý giao dịch tài chính, then backend từ chối.

### UC15: In Thẻ cứng và Cập nhật trạng thái đơn hàng

- [ ] **AC-UC15-01 [P0]** Given order đã thanh toán và đang `PRINTING`, when staff cập nhật tiến độ, then order chỉ được chuyển sang `READY_FOR_PICKUP` hoặc `SHIPPED`.
- [ ] **AC-UC15-02 [P0]** Given order đã sẵn sàng nhận hoặc đã giao shipper, when bàn giao hoàn tất, then staff chuyển order sang `COMPLETED`.
- [ ] **AC-UC15-03 [P0]** Given thẻ in lỗi trước khi bàn giao, when staff yêu cầu in lại, then order quay về `PRINTING` và chưa được đánh dấu `COMPLETED`.
- [ ] **AC-UC15-04 [P0]** Given chuyển trạng thái sai thứ tự hoặc staff khác tenant, when cập nhật, then backend từ chối.

### UC16: Vận hành ca trực và Đối chiếu két tiền mặt

- [ ] **AC-UC16-01 [P0]** Given staff chưa có ca mở, when bắt đầu làm việc tại quầy, then hệ thống cho phép mở một ca `ACTIVE` gắn staff và trạm.
- [ ] **AC-UC16-02 [P0]** Given staff đã có ca `ACTIVE`, when cố mở thêm ca chồng lấn, then backend từ chối.
- [ ] **AC-UC16-03 [P0]** Given ca có các giao dịch tiền mặt, when kết ca, then hệ thống tổng hợp đúng doanh thu theo `shift_id`, lưu báo cáo và chuyển ca sang `CLOSED`.
- [ ] **AC-UC16-04 [P0]** Given tiền đếm thực tế lệch báo cáo, when kết ca, then staff phải nhập `actual_cash_counted` và `discrepancy_reason`; hệ thống lưu vết chênh lệch.
- [ ] **AC-UC16-05 [P0]** Given ca đã `CLOSED`, when phát sinh thao tác tiền mặt mới, then backend từ chối.

---

## 5. Module 4: Tài chính & Ví điện tử

### UC17: Quản lý ví điện tử và Nạp tiền

- [ ] **AC-UC17-01 [P0]** Given callback `VNPAY_SANDBOX` hợp lệ ở dev hoặc webhook Sepay hợp lệ ở production, when xử lý nạp tiền, then ví passenger được cộng đúng số tiền và tạo transaction `TOP_UP`, `SUCCESS`.
- [ ] **AC-UC17-02 [P0]** Given `provider_transaction_id` đã xử lý, when nhận callback lặp, then backend không cộng tiền lần hai.
- [ ] **AC-UC17-03 [P0]** Given thanh toán thất bại hoặc hết hạn, when cập nhật trạng thái, then transaction chuyển `FAILED` hoặc `EXPIRED` và số dư không đổi.
- [ ] **AC-UC17-04 [P1]** Given webhook Sepay không bóc tách được `account_id`, when xử lý, then transaction chuyển `MANUAL_REVIEW`, lưu log và cảnh báo Platform Manager.
- [ ] **AC-UC17-05 [P0]** Given passenger gọi chức năng rút tiền, when backend nhận yêu cầu, then từ chối vì passenger wallet không hỗ trợ withdrawal.

### UC18: Gửi và Duyệt yêu cầu giải ngân Ví doanh nghiệp

- [ ] **AC-UC18-01 [P0]** Given operator wallet `ACTIVE` và đủ số dư, when Company Manager gửi yêu cầu, then tạo `withdrawal_requests` trạng thái `PENDING`.
- [ ] **AC-UC18-02 [P0]** Given yêu cầu `PENDING` và số dư vẫn đủ, when Platform Manager duyệt, then yêu cầu chuyển `APPROVED`, ví doanh nghiệp bị trừ đúng số tiền và tạo transaction `OPERATOR_PAYOUT`.
- [ ] **AC-UC18-03 [P0]** Given yêu cầu `PENDING`, when Platform Manager từ chối, then yêu cầu chuyển `REJECTED` và số dư ví không đổi.
- [ ] **AC-UC18-04 [P0]** Given số dư không đủ hoặc wallet không `ACTIVE`, when gửi hoặc duyệt yêu cầu, then backend từ chối.
- [ ] **AC-UC18-05 [P0]** Given số dư thay đổi đồng thời lúc duyệt, when transaction khóa dòng và kiểm tra lại thất bại, then rollback toàn bộ và giữ yêu cầu `PENDING`.
- [ ] **AC-UC18-06 [P0]** Given yêu cầu được duyệt, when kiểm tra tích hợp ngân hàng, then hệ thống chỉ ghi nhận sổ sách; không tự động chuyển khoản ngân hàng.

### UC19: Chạy ngầm Đối soát & Phân chia doanh thu Clearing

- [ ] **AC-UC19-01 [P0]** Given dữ liệu ngày trước chưa xử lý, when scheduler chạy lúc `02:00` theo múi giờ `Asia/Ho_Chi_Minh`, then hệ thống tính phân bổ doanh thu theo operator.
- [ ] **AC-UC19-02 [P0]** Given clearing thành công, when commit, then ví `OPERATOR` được cộng đúng số tiền, tạo `clearing_settlements` và đánh dấu journey liên quan `PROCESSED`.
- [ ] **AC-UC19-03 [P0]** Given cùng kỳ clearing đã xử lý, when scheduler hoặc admin rerun, then hệ thống không cộng tiền trùng.
- [ ] **AC-UC19-04 [P0]** Given lỗi DB hoặc mạng trong quá trình clearing, when transaction thất bại, then rollback toàn bộ số dư, settlement và trạng thái journey.
- [ ] **AC-UC19-05 [P1]** Given clearing thất bại, when rollback hoàn tất, then hệ thống ghi log và gửi cảnh báo kỹ thuật để rerun thủ công.

---

## 6. Module 5: Quản trị vận hành đơn vị

### UC20: Quản lý Nhân viên ga & Phân lịch ca trực

- [ ] **AC-UC20-01 [P0]** Given Company Manager hợp lệ, when tạo staff trong tenant của mình, then hệ thống tạo tài khoản `STAFF` và lưu thông tin đơn vị.
- [ ] **AC-UC20-02 [P0]** Given staff và trạm hợp lệ, when phân lịch, then hệ thống lưu `staff_shifts` đúng staff, trạm, thời gian và operator.
- [ ] **AC-UC20-03 [P0]** Given username hoặc email đã tồn tại, when tạo staff, then backend từ chối.
- [ ] **AC-UC20-04 [P1]** Given file import sai cấu trúc hoặc trống, when tải lên, then backend từ chối và không tạo dữ liệu.
- [ ] **AC-UC20-05 [P1]** Given file import có lỗi logic, when xử lý, then rollback toàn bộ lô và trả báo cáo lỗi.
- [ ] **AC-UC20-06 [P0]** Given Company Manager cố thao tác staff của tenant khác, when gọi API, then backend từ chối.

### UC21: Quản trị Tuyến trạm và Lưới nhà ga

- [ ] **AC-UC21-01 [P0]** Given Company Manager hợp lệ, when thêm hoặc sửa tuyến/trạm thuộc tenant của mình, then dữ liệu được lưu và API danh mục phản ánh cấu hình mới.
- [ ] **AC-UC21-02 [P0]** Given `station_order` trùng hoặc không liên tục, when cập nhật, then backend từ chối.
- [ ] **AC-UC21-03 [P0]** Given Company Manager cố sửa tuyến/trạm tenant khác, when gọi API, then backend từ chối.
- [ ] **AC-UC21-04 [P1]** Given file import sai cấu trúc, rỗng hoặc có lỗi logic, when xử lý, then rollback toàn bộ và trả báo cáo lỗi.
- [ ] **AC-UC21-05 [P1]** Given cấu hình tuyến/trạm thay đổi hợp lệ, when cập nhật hoàn tất, then Gate Simulator nhận được dữ liệu mới qua cơ chế đồng bộ/cấu hình MVP.

### UC22: Thiết lập Cấu hình Biểu giá tuyến

- [ ] **AC-UC22-01 [P0]** Given Company Manager cấu hình giá hợp lệ cho tenant của mình, when lưu, then policy được cập nhật trong `fare_policies.json` và nạp lại vào cache RAM.
- [ ] **AC-UC22-02 [P0]** Given policy bus đồng giá hoặc Metro lũy tiến theo số ga, when Fare Engine tính giá, then kết quả đúng công thức trong SRS.
- [ ] **AC-UC22-03 [P0]** Given mức giá vượt khung trần, when lưu policy, then backend từ chối với lỗi nghiệp vụ.
- [ ] **AC-UC22-04 [P0]** Given policy mới có hiệu lực, when mua/gia hạn vé hoặc tính chênh lệch tại PSC, then hệ thống dùng policy mới mà không cần restart.
- [ ] **AC-UC22-05 [P0]** Given Company Manager cố sửa policy tenant khác, when gọi API, then backend từ chối.

---

## 7. Module 6: Quản trị nền tảng

### UC23: Khởi tạo Tenant & Cấp tài khoản Company Manager

- [ ] **AC-UC23-01 [P0]** Given Platform Manager nhập hồ sơ doanh nghiệp hợp lệ, when khởi tạo tenant, then tenant/operator được tạo ở trạng thái `ACTIVE`.
- [ ] **AC-UC23-02 [P0]** Given tenant tạo thành công, when kiểm tra dữ liệu, then tài khoản `COMPANY_MANAGER` và wallet `OPERATOR` số dư `0` được tạo.
- [ ] **AC-UC23-03 [P0]** Given MST hoặc email đại diện đã tồn tại, when khởi tạo, then backend từ chối và không tạo dữ liệu dở dang.
- [ ] **AC-UC23-04 [P0]** Given user không phải Platform Manager, when gọi API khởi tạo tenant, then backend từ chối.

### UC24: Cấu hình Khung giá trần toàn hệ thống

- [ ] **AC-UC24-01 [P0]** Given Platform Manager nhập mức trần dương hợp lệ, when lưu, then giá trị được cập nhật trong `system_configs.json` và nạp lại vào cache.
- [ ] **AC-UC24-02 [P0]** Given khung trần mới có hiệu lực, when Company Manager lưu policy vượt trần, then backend từ chối.
- [ ] **AC-UC24-03 [P0]** Given giá trị trần nhỏ hơn hoặc bằng `0`, when lưu, then backend từ chối.
- [ ] **AC-UC24-04 [P0]** Given user không phải Platform Manager, when sửa khung trần, then backend từ chối.

---

## 8. Module 7: Giám sát, Bảo mật & Phân quyền

### UC25: Khóa và Mở khóa tài khoản khẩn cấp

- [ ] **AC-UC25-01 [P0]** Given Admin khóa tài khoản hợp lệ, when thao tác hoàn tất, then `accounts.is_active = FALSE` và token đang hoạt động bị vô hiệu hóa.
- [ ] **AC-UC25-02 [P0]** Given tài khoản đã bị khóa, when đăng nhập, gọi API hoặc quét vé, then hệ thống từ chối.
- [ ] **AC-UC25-03 [P0]** Given Admin mở khóa tài khoản, when thao tác hoàn tất, then `accounts.is_active = TRUE` và người dùng có thể đăng nhập lại.
- [ ] **AC-UC25-04 [P0]** Given Admin cố tự khóa chính mình, when gửi yêu cầu, then backend từ chối.
- [ ] **AC-UC25-05 [P0]** Given khóa hoặc mở khóa, when kiểm tra audit, then hệ thống lưu admin thao tác, tài khoản mục tiêu, thời gian và lý do.

### UC26: Cấu hình Phân quyền động

- [ ] **AC-UC26-01 [P0]** Given Admin gán permission cho role, when lưu cấu hình, then quyền có hiệu lực runtime mà không restart server.
- [ ] **AC-UC26-02 [P0]** Given Admin tước permission khỏi role, when user thuộc role gọi API tương ứng, then backend từ chối ngay theo cấu hình mới.
- [ ] **AC-UC26-03 [P0]** Given Admin cố tước quyền cốt lõi như `CONFIG_RBAC` hoặc `MANAGE_USERS` khỏi role `ADMIN`, when lưu, then backend từ chối.
- [ ] **AC-UC26-04 [P0]** Given user không phải Admin, when sửa RBAC, then backend từ chối.
- [ ] **AC-UC26-05 [P0]** Given RBAC thay đổi, when kiểm tra audit, then hệ thống lưu cấu hình trước/sau và admin thao tác.

### UC27: Giám sát kỹ thuật & Tra cứu System Logs

- [ ] **AC-UC27-01 [P0]** Given Admin truy cập Portal giám sát, when lọc theo thời gian, loại log hoặc mức độ, then hệ thống trả đúng dữ liệu log trong phạm vi truy vấn.
- [ ] **AC-UC27-02 [P0]** Given log có dữ liệu chi tiết, when mở bản ghi, then Portal hiển thị cấu trúc JSON và metadata cần thiết.
- [ ] **AC-UC27-03 [P1]** Given kết quả truy vấn log, when Admin xuất báo cáo, then hệ thống tạo file CSV hoặc Excel tải được.
- [ ] **AC-UC27-04 [P1]** Given phát sinh `incident_logs` mức `CRITICAL`, when backend ghi nhận, then hệ thống hiển thị cảnh báo trên Portal và gửi webhook đến kênh kỹ thuật đã cấu hình.
- [ ] **AC-UC27-05 [P0]** Given user không phải Admin, when truy cập system logs toàn cục, then backend từ chối.

---

## 9. Tiêu chí nghiệm thu xuyên suốt

### 9.1. Phân quyền và cô lập tenant

- [ ] **AC-CROSS-01 [P0]** Mọi API nghiệp vụ kiểm tra role/permission ở backend, không chỉ ẩn nút trên frontend.
- [ ] **AC-CROSS-02 [P0]** `COMPANY_MANAGER` và `STAFF` không thể đọc hoặc sửa dữ liệu có `operator_id` khác tenant của mình.
- [ ] **AC-CROSS-03 [P0]** Các thao tác quản trị nền tảng chỉ dành cho `PLATFORM_MANAGER`; các thao tác bảo mật toàn cục chỉ dành cho `ADMIN`.

### 9.2. Tính nhất quán tài chính

- [ ] **AC-CROSS-04 [P0]** Callback/webhook thanh toán và clearing rerun có idempotency key để không cộng hoặc trừ tiền trùng.
- [ ] **AC-CROSS-05 [P0]** Mọi biến động ví tạo transaction tương ứng và không để số dư âm ngoài rule cho phép.
- [ ] **AC-CROSS-06 [P0]** Các thao tác thay đổi nhiều bảng liên quan tài chính chạy trong transaction và rollback toàn bộ khi lỗi.

### 9.3. Audit và vận hành

- [ ] **AC-CROSS-07 [P0]** Các thao tác nhạy cảm gồm ban/unban, RBAC, giải khóa miễn phí, thu phạt, bù chặng, kết ca, clearing và payout đều lưu audit.
- [ ] **AC-CROSS-08 [P0]** Toàn hệ thống dùng múi giờ `Asia/Ho_Chi_Minh` cho nghiệp vụ, log và scheduler.
- [ ] **AC-CROSS-09 [P0]** Các file cấu hình `fare_policies.json`, `system_configs.json`, `route_stations.json`, `tenants.json` được nạp vào cache RAM khi backend khởi động và có cơ chế cập nhật cache khi thay đổi.

---

## 10. Điều kiện hoàn tất nghiệm thu MVP

MVP được xem là đạt yêu cầu khi:

1. Toàn bộ tiêu chí `[P0]` đã pass.
2. Không còn lỗi blocker hoặc lỗi sai lệch số dư tài chính.
3. Các luồng demo xuyên suốt hoạt động:
   - Passenger đăng ký bằng SĐT/OTP hoặc đăng nhập bằng SĐT/mật khẩu, hoàn thiện hồ sơ, nạp ví, phát hành/mua vé và xem lịch sử.
   - Passenger quét Dynamic QR để check-in/check-out qua Webcam Gate Simulator.
   - Staff mở ca, xử lý lỗi tại PSC, kết ca và đối chiếu tiền mặt.
   - Company Manager quản lý staff, tuyến/trạm và biểu giá trong tenant.
   - Platform Manager khởi tạo tenant, quản lý khung giá và duyệt payout.
   - Scheduler chạy clearing và phân bổ doanh thu không trùng lặp.
   - Admin ban/unban, cấu hình RBAC và tra cứu logs.
