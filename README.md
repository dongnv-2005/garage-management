# HỆ THỐNG QUẢN LÝ GARA Ô TÔ (GARAGE MANAGEMENT SYSTEM)

Dự án ứng dụng Java Swing kết hợp với Cơ sở dữ liệu MySQL theo mô hình kiến trúc phân tầng (Layered Architecture) và nguyên lý Hướng đối tượng (OOP) mẫu mực, giúp quản lý toàn bộ quy trình vận hành gara ô tô: tiếp nhận xe, theo dõi trạng thái sửa chữa, quản lý kho phụ tùng, chấm công nhân viên, phân công kỹ thuật viên thông minh và báo cáo doanh thu trực quan.

---

## 1. TỔNG QUAN HỆ THỐNG

### Phân quyền người dùng (Role-based Access Control)

Hệ thống được thiết kế phân quyền trực quan và rõ ràng theo vai trò người dùng (**Chủ Garage / Admin** và **Lễ tân / Receptionist**):

#### 1. Chức năng nghiệp vụ dùng chung (Cả Admin & Lễ tân đều thao tác được):
* **Quản lý Khách hàng:** Thêm mới, cập nhật thông tin khách hàng (Họ và tên, Số điện thoại), tra cứu và tìm kiếm khách hàng nhanh chóng.
* **Tiếp nhận & Quản lý Xe:** Tiếp nhận xe vào gara (Biển số xe, Hãng xe, Model, Mã chủ xe), tra cứu tìm kiếm xe, cập nhật trạng thái sửa chữa (`Chờ sửa - WAITING`, `Đang sửa - IN_PROGRESS`, `Đã sửa xong - COMPLETED`).
* **Quản lý Kho Phụ tùng:** Tra cứu danh mục phụ tùng, số lượng tồn kho, giá nhập và giá bán; nhập thêm phụ tùng vào kho; theo dõi thẻ kho / lịch sử biến động phụ tùng (Nhập kho / Dùng cho xe / Tự động hoàn kho khi xóa hóa đơn).
* **Gán Dịch vụ & Xuất Hóa đơn:** Tạo hóa đơn dịch vụ & phụ tùng, tính tiền tự động theo cơ chế OOP, tự động trừ số lượng tồn kho và tự động chuyển trạng thái xe sang `Đã sửa xong`; tra cứu lịch sử hóa đơn theo biển số hoặc người tạo; xóa hóa đơn (tự động hoàn lại số lượng phụ tùng tương ứng vào kho).
* **Tự Điểm danh Cá nhân:** Nút bấm **"TÍCH ĐI LÀM NGAY"** ngay trên thanh tác vụ trên cùng giúp người dùng tự ghi nhận chấm công ca làm việc thực tế.
* **Quản lý Tài khoản Cá nhân:** Xem và cập nhật thông tin cá nhân (Họ tên, SĐT) và Đổi mật khẩu tài khoản đăng nhập.

#### 2. Chức năng DÀNH RIÊNG cho Chủ Garage (Admin):
* **Quản lý Nhân sự & Tiền lương:** Xem danh sách toàn bộ nhân sự, thông tin CCCD, SĐT, ca trực phân công, tự động tính tổng số ca làm việc thực tế và tổng lương tích lũy (Lễ tân: 400.000đ/ca, KTV: 360.000đ/ca); thêm mới, cập nhật và xóa nhân viên.
* **Tự động Phân công Kỹ thuật viên (KTV):** Khi thêm KTV mới hoặc hệ thống khởi chạy, tự động gán KTV cho 1 Lễ tân cùng ca làm việc theo thuật toán cân bằng tải (ưu tiên Lễ tân quản lý ít KTV nhất, chọn ngẫu nhiên nếu bằng nhau).
* **Quản lý Chấm công Toàn gara:** Điểm danh hộ cho bất kỳ nhân viên nào trong gara; xem toàn bộ lịch sử chấm công; xóa các bản ghi chấm công khi cần.
* **Reset Mật khẩu:** Đặt lại mật khẩu tài khoản đăng nhập của nhân viên về mặc định (`123456`).
* **Báo cáo Doanh thu & Thống kê:**
  * *Thống kê Tổng quan (Bảng):* Tổng doanh thu, tổng chi phí phụ tùng, lợi nhuận ròng, số lượng xe đã hoàn thành, phụ tùng bán chạy nhất.
  * *Doanh thu theo Dịch vụ (Biểu đồ cột tương tác trực quan):* Vẽ trực quan tỷ trọng doanh thu giữa các nhóm dịch vụ bằng Graphics2D.

#### 3. Chức năng DÀNH RIÊNG cho Lễ tân (Receptionist):
* **Lịch Sử Chấm Công Nhóm:** 
  * *Chấm Công Nhóm KTV:* Điểm danh hộ và theo dõi lần chấm công gần nhất của nhóm Kỹ thuật viên được Chủ Garage phân công quản lý cùng ca.
  * *Lịch Sử Chấm Công Nhóm:* Xem toàn bộ lịch sử chấm công của các KTV thuộc nhóm mình phụ trách.
  * *Lịch Sử Tự Chấm Công Của Bạn:* Theo dõi riêng lịch sử các lần tự chấm công đi làm của bản thân.
* **Thông báo Phân công KTV mới:** Tự động hiển thị popup thông báo ngay khi đăng nhập nếu có KTV mới được phân công vào nhóm quản lý của mình.

---

## 2. CẤU TRÚC THƯ MỤC DỰ ÁN

```text
garage-management/
│
├── bin/                          # Thư mục chứa các file .class sau khi biên dịch
├── lib/                          # Thư mục chứa thư viện ngoại vi (.jar)
│   └── mysql-connector-j-8.3.0.jar
├── src/                          # Mã nguồn Java
│   ├── com/
│   │   └── garage/
│   │       ├── config/           # Cấu hình kết nối & tự động khởi tạo CSDL
│   │       │   └── DatabaseConfig.java
│   │       ├── enums/            # Các hằng số Enum
│   │       │   ├── RepairStatus.java
│   │       │   ├── Role.java
│   │       │   └── Shift.java
│   │       ├── gui/              # Giao diện người dùng Swing (UI)
│   │       │   ├── AccountManagementDialog.java
│   │       │   ├── BaseReportPanel.java
│   │       │   ├── ChangePasswordDialog.java
│   │       │   ├── LoginFrame.java
│   │       │   ├── MainFrame.java
│   │       │   ├── ServiceChartReportPanel.java
│   │       │   └── TableSummaryReportPanel.java
│   │       ├── models/           # Các lớp Entity & OOP Service Models
│   │       │   ├── BaseService.java          # Lớp trừu tượng dịch vụ
│   │       │   ├── Customer.java
│   │       │   ├── Employee.java
│   │       │   ├── EngineRepairService.java   # Đa hình dịch vụ sửa động cơ
│   │       │   ├── Invoice.java
│   │       │   ├── MaintenanceService.java    # Đa hình dịch vụ bảo dưỡng
│   │       │   ├── OilChangeService.java      # Đa hình dịch vụ thay dầu
│   │       │   ├── Part.java
│   │       │   ├── RepairOrder.java           # Đơn sửa chữa chứa danh sách dịch vụ
│   │       │   ├── User.java
│   │       │   └── Vehicle.java
│   │       ├── repository/       # Tầng truy xuất dữ liệu CSDL (DAO / Repository)
│   │       │   ├── CustomerRepository.java
│   │       │   ├── EmployeeRepository.java
│   │       │   ├── InvoiceRepository.java
│   │       │   ├── PartRepository.java
│   │       │   └── VehicleRepository.java
│   │       ├── services/         # Tầng xử lý nghiệp vụ chính (Business Logic)
│   │       │   ├── AuthService.java
│   │       │   ├── BillingManager.java
│   │       │   ├── CustomerManager.java
│   │       │   └── VehicleManager.java
│   │       └── Main.java         # Class khởi chạy ứng dụng chính
│   └── schema.sql                # Script khởi tạo CSDL & nạp dữ liệu mẫu
└── README.md                     # Tài liệu hướng dẫn dự án
```

---

## 3. NGUYÊN LÝ THIẾT KẾ & ĐẶC TÍNH NỔI BẬT

1. **Nguyên lý Hướng đối tượng (OOP):**
   * **Tính Trừu tượng (Abstraction) & Kế thừa (Inheritance):** Lớp trừu tượng `BaseService` làm cha cho `OilChangeService`, `MaintenanceService`, `EngineRepairService` với phương thức `calculateCost()`.
   * **Tính Đa hình (Polymorphism):** `RepairOrder` chứa danh sách `List<BaseService>` và gọi phương thức `calculateCost()` đa hình để tính toán tổng chi phí sửa chữa động.
   * **Tính Đóng gói (Encapsulation):** Toàn bộ thuộc tính trong các Entity Model được đóng gói với getter/setter và kiểm tra tính hợp lệ.

2. **Cơ chế Phân bổ Kỹ thuật viên Thông minh:**
   * Tự động cân bằng tải (Load Balancing) KTV cho các Lễ tân cùng ca làm.
   * Chọn ngẫu nhiên (Random) công bằng khi số lượng KTV quản lý bằng nhau.
   * Thông báo tức thời dạng Popup Dialog khi Lễ tân đăng nhập lần đầu.

3. **Tính ổn định & Chống lỗi CSDL:**
   * Tự động thử các mật khẩu MySQL phổ biến (`123456`, rỗng `""`, `root`, `admin`) hoặc biến môi trường `DB_PASS`.
   * Khóa chính hóa đơn sinh theo Timestamp `INV-yyMMddHHmmssSSS` đảm bảo không bao giờ bị trùng lặp.
   * Tự động hoàn kho khi xóa hóa đơn và cập nhật trạng thái xe.

---

## 4. YÊU CẦU MÔI TRƯỜNG & CÀI ĐẶT

* **Java Development Kit (JDK):** Version 17 hoặc 21+ (Đã được thiết lập `JAVA_HOME`).
* **Database:** MySQL Server (Version 8.0 trở lên, chạy qua XAMPP, MySQL Installer hoặc Docker).
* **Thư viện Driver:** `mysql-connector-j-8.3.0.jar` (đặt trong thư mục `lib/`).
* **Công cụ hỗ trợ:** VS Code, IntelliJ IDEA, Eclipse hoặc PowerShell/Command Prompt.

---

## 5. HƯỚNG DẪN BIÊN DỊCH & CHẠY ỨNG DỤNG

### Bước 1: Chuẩn bị CSDL MySQL

1. Đảm bảo dịch vụ MySQL Server đang chạy (Port mặc định: 3306).
2. Ứng dụng tích hợp cơ chế tự động đọc file `schema.sql` để tạo Database `garage_db` và nạp dữ liệu mẫu ngay lần chạy đầu tiên.

---

### Bước 2: Biên dịch và Khởi chạy bằng PowerShell / Terminal

Mở Terminal tại thư mục gốc của dự án (`garage-management/`):

#### 1. Biên dịch toàn bộ mã nguồn Java:

```powershell
javac -encoding UTF-8 -cp "lib/*;src" -d bin src/com/garage/Main.java (Get-ChildItem -Recurse -Filter *.java -Path src | ForEach-Object { $_.FullName })
```

#### 2. Khởi chạy ứng dụng:

```powershell
java "-Dfile.encoding=UTF-8" -cp "bin;lib/*" com.garage.Main
```

---

## 6. TÀI KHOẢN ĐĂNG NHẬP MẶC ĐỊNH

Hệ thống đã nạp sẵn các tài khoản kiểm thử mặc định (Mật khẩu chung: `123456`):

| Tên Tài Khoản (Username) | Mật Khẩu | Họ và Tên | Vai Trò (Role) | Chức Năng Chính |
| :--- | :--- | :--- | :--- | :--- |
| `admin` | `123456` | Chủ Garage | `ADMIN` | Toàn quyền quản lý gara, nhân sự, chấm công, xem báo cáo |
| `hoatt` | `123456` | Trần Thị Hòa | `RECEPTIONIST` | Lễ tân Ca 1 (Quản lý tiếp nhận xe, hóa đơn, nhóm KTV Ca 1) |
| `namlv` | `123456` | Lê Văn Nam | `RECEPTIONIST` | Lễ tân Ca 2 (Quản lý tiếp nhận xe, hóa đơn, nhóm KTV Ca 2) |

---

## 7. CÁCH LÀM SẠCH & RESET LẠI CƠ SỞ DỮ LIỆU

Nếu muốn xóa toàn bộ dữ liệu kiểm thử để làm mới lại từ đầu:

1. Mở PowerShell / Command Prompt và thực thi:
```powershell
mysql -u root -p123456 -e "DROP DATABASE IF EXISTS garage_db;"
```

2. Khởi chạy lại ứng dụng bằng lệnh:
```powershell
java "-Dfile.encoding=UTF-8" -cp "bin;lib/*" com.garage.Main
```
*Hệ thống sẽ tự động quét file `schema.sql`, tạo mới lại toàn bộ bảng và nạp dữ liệu chuẩn.*