# **TỔNG HỢP CHỨC NĂNG NGHIỆP VỤ HỆ THỐNG AFC: CẤP 3 VÀ CẤP 4**

Tài liệu này tổng hợp chi tiết cấu trúc chức năng, vai trò và luồng nghiệp vụ tinh gọn của Cấp 3 (Hệ thống tại Ga/Tuyến) và Cấp 4 (Hệ thống tại Công ty vận hành) trong hệ thống Thu vé tự động liên thông (AFC). Phiên bản V2 này đã được tinh chỉnh phân định rõ ranh giới trách nhiệm (Separation of Concerns) nhằm tối ưu hóa hiệu năng vận hành cho mô hình MVP (Minimum Viable Product), đảm bảo tính khả thi cao khi triển khai thực tế.

## **1\. Cấp 3: Hệ Thống Tại Ga / Đề-pô / Trạm / Tuyến (Station/Line Level)**

### **1.1. Vai trò tổng quan**

Cấp 3 đóng vai trò là trạm trung chuyển dữ liệu cục bộ và chịu trách nhiệm phản hồi kỹ thuật thời gian thực cho các thiết bị đầu cuối thuộc phạm vi ga hoặc tuyến quản lý. Cấp 3 áp dụng trực tiếp các quy tắc và cấu hình cục bộ được đồng bộ trước đó để xử lý nhanh, giảm thiểu tối đa độ trễ mạng và không can thiệp vào các logic xử lý tài chính chuyên sâu.

### **1.2. Các chức năng nghiệp vụ tinh gọn**

* **Thu thập dữ liệu kỹ thuật và sự kiện:** Tiếp nhận liên tục các thông tin về sự kiện (events), nhịp tim thiết bị (heartbeat) và các lỗi vận hành phát sinh (incident) từ thiết bị đầu cuối Cấp 2\.  
* **Lưu vết và chuyển tiếp dữ liệu:** Thực hiện lưu vết tạm thời tại local, gom gói các bản ghi sự kiện/giao dịch thô rồi chuyển tiếp đồng bộ lên hệ thống Cấp 4\.  
* **Nhận và đồng bộ cấu hình:** Tiếp nhận các gói dữ liệu cấu hình kỹ thuật, danh sách đen (blacklist) và bộ quy tắc vận hành (rules) do Cấp 4 phát hành xuống.  
* **Áp dụng bộ quy tắc cục bộ (Local Rules/Config):** Thực thi xử lý logic kỹ thuật ngay tại máy chủ ga dựa trên dữ liệu cấu hình đã đồng bộ để đưa ra phản hồi đóng/mở cửa lập tức cho thiết bị Cấp 2, đáp ứng yêu cầu xử lý real-time tốc độ cao.  
* **Giám sát thiết bị thuộc trạm:** Theo dõi trạng thái kết nối phần cứng và hiệu suất hoạt động của các thiết bị Cấp 2 trong phạm vi ga/tuyến như cổng soát vé, validator, TVM/TOM hoặc kiosk nếu được tích hợp. Phân hệ Cấp 3 chỉ giám sát và nhận dữ liệu thiết bị, không thực hiện nghiệp vụ bán vé.

## **2\. Cấp 4: Hệ Thống Tại Công Ty Vận Hành (Operator Back Office Level)**

### **2.1. Vai trò tổng quan**

Cấp 4 đóng vai trò là trung tâm quản lý hạ tầng kỹ thuật và điều phối dữ liệu vận hành tổng thể của một đơn vị khai thác tuyến (Operator Back Office). Hệ thống tập trung vào việc quản trị cấu hình mạng lưới, giám sát transaction/event vận hành và chuyển tiếp dữ liệu lên hệ thống Cấp 5/trung tâm liên thông thông qua API chung.

### **2.2. Các chức năng nghiệp vụ tinh gọn**

* **Quản lý cấu trúc mạng lưới hạ tầng:** Khai báo và quản trị danh mục hệ thống tuyến, danh sách các nhà ga, đề-pô và thông số kỹ thuật của toàn bộ thiết bị đầu cuối trực thuộc đơn vị vận hành.  
* **Giám sát và quản lý transaction vận hành:** Theo dõi toàn diện các giao dịch thô truyền lên từ các trạm, giám sát trạng thái hệ thống và ghi nhận nhật ký lỗi phần cứng/phần mềm toàn operator để phục vụ công tác điều độ kỹ thuật.  
* **Phát hành cấu hình vận hành (Control Package):** Tạo lập và đóng gói các tệp tham số cấu hình kỹ thuật, rule vận hành cục bộ hoặc danh sách chặn (blacklist) ở mức demo để phân phối tuần tự xuống Cấp 3\. Chức năng này không thay thế việc quản lý bộ quy tắc chung toàn mạng của Cấp 5\.  
* **Tổng hợp báo cáo số liệu vận hành:** Thực hiện tổng hợp sản lượng transaction qua trạm, tần suất sự kiện theo thời gian, trạng thái thiết bị và các số liệu kỹ thuật thô phục vụ báo cáo nội bộ của nhà vận hành. Không bao gồm báo cáo doanh thu, lịch chạy tàu hoặc vận hành đoàn tàu nếu không có hệ thống ngoài tích hợp.  
* **Tạo batch dữ liệu vận hành gửi Cấp 5:** Đóng gói dữ liệu transaction/event đã chuẩn hóa thành batch data và chuyển tiếp lên hệ thống Cấp 5/trung tâm liên thông. Cấp 5 hoặc hệ thống chuyên biệt bên ngoài chịu trách nhiệm xử lý clearing, doanh thu, đối soát tài chính và quy tắc chung nếu có.

## **3\. Ma Trận Phân Định Trách Nhiệm Tinh Gọn (Cấp 3 vs Cấp 4\)**

| Hạng mục nghiệp vụ | Cấp 3 (Hệ thống tại Ga) | Cấp 4 (Hệ thống Công ty vận hành)   |
| :---- | :---- | :---- |
| **Giao tiếp thiết bị & Phản hồi** | Trực tiếp đưa ra phản hồi đóng/mở cổng cho thiết bị Cấp 2 dựa trên bộ quy tắc (rule/config) local đã đồng bộ. | Không tham gia vào luồng ra lệnh mở cổng real-time; thực hiện giám sát và quản lý các transaction vận hành thô tổng thể. |
| **Quản lý quy tắc / Tham số** | Tiếp nhận và thực thi áp dụng rule/config local ở mức kỹ thuật trực tiếp tại trạm. | Tạo lập, đóng gói và chịu trách nhiệm phát hành cấu hình/rule vận hành xuống Cấp 3\. |
| **Xử lý dữ liệu chuyến đi** | Thu thập các sự kiện (events), dữ liệu heartbeat và incident từ Cấp 2, tiến hành lưu vết và gom gói dữ liệu thô. | Tiếp nhận dữ liệu từ Cấp 3, tổng hợp báo cáo số liệu vận hành và đóng gói thành các batch dữ liệu để gửi lên Cấp 5\. |
| **Tài chính & Quản lý người dùng** | Nằm ngoài phạm vi hệ thống (Out of scope). | Nằm ngoài phạm vi lõi hệ thống (Out of scope); các chức năng quản lý tài khoản hành khách, ví/thanh toán, clearing, doanh thu và đối soát tài chính được nhường cho các hệ thống chuyên biệt độc lập và Cấp 5\. |

Việc phân định ranh giới chức năng tinh gọn như trên giúp kiến trúc 5 cấp hoạt động tách biệt, đảm bảo các nút mạng local xử lý cực nhanh dưới ga, đồng thời giữ cho hệ thống Backoffice của nhà vận hành không bị quá tải bởi các logic phi vận hành.
