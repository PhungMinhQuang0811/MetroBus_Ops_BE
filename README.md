# Hệ Thống Điều Hành & Trung Chuyển Dữ Liệu Thu Soát Vé Tự Động AFC (Cấp 3 & Cấp 4)

Dự án này là phân hệ **Cấp 3 (Hệ thống tại Ga)** và **Cấp 4 (Hệ thống tại Công ty vận hành)** thuộc giải pháp **Kiểm soát vé tự động liên thông 5 cấp (AFC)**, được xây dựng trong khuôn khổ Chương trình **Viettel Digital Talent 2026** (Mini-Project Giai đoạn 1).

Hệ thống đóng vai trò làm trục trung chuyển thông tin cốt lõi, tiếp nhận giao dịch soát vé thô từ cổng soát vé (Cấp 2) kết chuyển đối soát lên trung tâm bù trừ FMC (Cấp 5); đồng thời quản trị Master Data và đồng bộ các gói cấu hình (Control Package) xuống thiết bị ngoại vi tại ga.

---

## 🏗️ Kiến Trúc Hệ Thống (Architecture)

Hệ thống được thiết kế theo kiến trúc Microservices bóc tách độc lập nhằm tối ưu hóa tài nguyên và cô lập tầm ảnh hưởng:

1.  **`auth-ops-service` (Spring Boot):** Chuyên trách nghiệp vụ xác thực người dùng, phân quyền tác nghiệp dựa trên vai trò (RBAC).
2.  **`afc-ops-service` (Spring Boot):** Dịch vụ lõi xử lý nghiệp vụ quản trị Master Data (Tuyến, Ga, Thiết bị), cấp cấu hình thiết bị, tiếp nhận giao dịch thô từ ga và chạy tiến trình gom lô (Batch Job) gửi đối soát bù trừ tài chính.

### 🗄️ Mô Hình Lưu Trữ Hỗn Hợp (Data Storage)
*   **PostgreSQL (Quan hệ):** Quản trị Master Data và thông tin tài khoản cần ràng buộc toàn vẹn dữ liệu cao (`auth_ops_db` và `afc_ops_db`).
*   **MongoDB (Phi quan hệ):** Lưu trữ các loại nhật ký thô tần suất cao như *Audit Log* (truy vết người dùng), *System Log* (lỗi hệ thống) và *Integration Log* (lịch sử truyền nhận API/Message) dạng tài liệu JSON, giảm tải hoàn toàn cho cơ sở dữ liệu chính.
*   **Redis (Caching):** Lưu đệm phiên mã QR soát vé động (TTL 30 giây) phục vụ kiểm tra xác thực nhanh và chống tấn công phát lại (Replay Attack).
*   **RabbitMQ (Message Broker):** Trục trung chuyển dữ liệu bất đồng bộ qua giao thức AMQP để thực hiện luồng đồng bộ cấu hình xuống ga và gửi đối soát lên FMC Cấp 5, đảm bảo không mất mát thông tin khi mất kết nối mạng cục bộ.

---

## 🛠️ Yêu Cầu Hệ Thống (Prerequisites)

Để chạy dự án cục bộ, máy tính cần cài đặt sẵn các công cụ sau:
*   **Java Development Kit (JDK) 17** hoặc cao hơn.
*   **Apache Maven 3.8+**
*   **Docker & Docker Compose**
*   **Node.js** (Nếu chạy Portal quản trị Next.js)

---

## 🚀 Hướng Dẫn Cài Đặt & Khởi Chạy (Getting Started)

### Bước 1: Khởi động các dịch vụ hạ tầng qua Docker Compose
Hệ thống sử dụng các image chính thức cho Postgres, MongoDB, Redis, và RabbitMQ. Khởi chạy bằng lệnh sau tại thư mục gốc:

```bash
docker-compose -f docker-compose-dev.yml up -d
```

*Lưu ý: Các cổng dịch vụ mặc định được ánh xạ qua file `.env.dev`:*
*   *Postgres (Auth): `5432`*
*   *Postgres (AFC): `5433`*
*   *Redis: `6379`*
*   *RabbitMQ: `5672` (Management Console tại: `http://localhost:15672` tài khoản `guest/guest`)*
*   *MongoDB: `27017`*

### Bước 2: Build và Chạy Dịch Vụ Backend
Di chuyển vào thư mục của từng service và chạy Maven command:

1.  **Khởi chạy Dịch vụ Xác thực (`auth-ops-service`):**
    ```bash
    cd auth-ops-service
    mvn clean install
    mvn spring-boot:run
    ```
2.  **Khởi chạy Dịch vụ AFC (`afc-ops-service`):**
    ```bash
    cd ../afc-ops-service
    mvn clean install
    mvn spring-boot:run
    ```

---

## 🔄 Luồng Nghiệp Vụ Cốt Lõi (Core Workflows)

### 1. Đồng bộ cấu hình (Control Package) xuống thiết bị tại ga
*   Quy trình được thực thi tự động qua Scheduler hằng đêm (lúc 00:00) hoặc do Operator Manager kích hoạt trực tiếp từ Portal quản trị.
*   Hệ thống Cấp 4 (`afc-ops-service`) rà soát danh sách trạm hoạt động, kéo payload cấu hình từ MongoDB và kết hợp thành gói dữ liệu tổng hợp (Combined Payload) gồm cấu hình thiết bị, cấu hình ga và danh sách đen (Blacklist).
*   Gói tin này được đẩy qua RabbitMQ (AMQP) trực tiếp về hàng đợi của cổng soát vé Cấp 2 (`Gate Simulator`) tương ứng với mã ga để thiết bị áp dụng offline.

### 2. Kết chuyển giao dịch thô & Đối soát tài chính lên Cấp 5
*   Khi có sự kiện quẹt thẻ vé, thiết bị Cấp 2 gửi trực tiếp dữ liệu thô về Cấp 4 qua API để ghi nhận vào PostgreSQL với trạng thái `PENDING`.
*   Định kỳ hằng ngày (hoặc qua lệnh thủ công trên Portal), tiến trình Batch Job gom toàn bộ các giao dịch này thành một lô đối soát (Batch) và gửi qua RabbitMQ lên Trung tâm thanh toán bù trừ Cấp 5.
*   Khi Cấp 5 bù trừ xong và trả về thông điệp xác nhận quyết toán (`SettlementConfirmedEvent`), hệ thống Cấp 4 tự động cập nhật trạng thái lô thành công và ghi nhận kết quả phân chia doanh thu.

---

## 📂 Cơ Cấu Thư Mục Dự Án (Directory Structure)

```text
VDT2026_MetroBus_BE/
│
├── auth-ops-service/         # Dự án Spring Boot xác thực & phân quyền (RBAC)
├── afc-ops-service/          # Dự án Spring Boot quản lý điều hành & xử lý thẻ vé
├── document/                 # File đặc tả API, Use Case, Sequence Diagram, Báo cáo đề tài
├── data/                     # Thư mục volume mount của Docker để lưu trữ dữ liệu DB cục bộ
├── docker-compose-dev.yml    # File compose quản lý hạ tầng các DB & Broker
└── .env.dev                  # File chứa các biến môi trường cấu hình kết nối local
```
