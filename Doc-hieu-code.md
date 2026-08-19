# TÀI LIỆU ĐỌC HIỂU MÃ NGUỒN & 4 NGUYÊN LÝ LẬP TRÌNH HƯỚNG ĐỐI TƯỢNG (OOP)
### DỰ ÁN: HỆ THỐNG QUẢN LÝ GARA Ô TÔ (GARAGE MANAGEMENT SYSTEM)

---

## MỤC LỤC
1. [Tổng quan Kiến trúc Hệ thống (Layered Architecture)](#1-tổng-quan-kiến-trúc-hệ-thống)
2. [Nguyên lý 1: Tính Đóng Gói (Encapsulation)](#2-nguyên-lý-1-tính-đóng-gói-encapsulation)
3. [Nguyên lý 2: Tính Trừu Tượng (Abstraction)](#3-nguyên-lý-2-tính-trừu-tượng-abstraction)
4. [Nguyên lý 3: Tính Kế Thừa (Inheritance)](#4-nguyên-lý-3-tính-kế-thừa-inheritance)
5. [Nguyên lý 4: Tính Đa Hình (Polymorphism)](#5-nguyên-lý-4-tính-đa-hình-polymorphism)
6. [Phân tích Luồng Nghiệp Vụ Cốt Lõi](#6-phân-tích-luồng-nghiệp-vụ-cốt-lõi)
7. [Bảng Tổng Hợp Đối Chiếu OOP Trong Dự Án](#7-bảng-tổng-hợp-đối-chiếu-oop-trong-dự-án)

---

## 1. TỔNG QUAN KIẾN TRÚC HỆ THỐNG

Dự án được xây dựng theo **Kiến trúc Phân tầng (Layered Architecture)** kết hợp với mô hình **MVC (Model - View - Controller/Service)**, giúp tách biệt rõ ràng giữa giao diện, nghiệp vụ và cơ sở dữ liệu:

```
┌─────────────────────────────────────────────────────────────┐
│  1. TẦNG GIAO DIỆN (GUI / View Layer)                       │
│     MainFrame, LoginFrame, Dialogs, Report Panels           │
└──────────────────────────────┬──────────────────────────────┘
                               │ Gọi nghiệp vụ
┌──────────────────────────────▼──────────────────────────────┐
│  2. TẦNG XỬ LÝ NGHIỆP VỤ (Service / Business Layer)         │
│     CustomerManager, VehicleManager, BillingManager, AuthService
└──────────────────────────────┬──────────────────────────────┘
                               │ Gọi truy xuất CSDL
┌──────────────────────────────▼──────────────────────────────┐
│  3. TẦNG TRUY XUẤT DỮ LIỆU (Repository / DAO Layer)         │
│     CustomerRepository, VehicleRepository, PartRepository,  │
│     InvoiceRepository, EmployeeRepository                   │
└──────────────────────────────┬──────────────────────────────┘
                               │ Ánh xạ đối tượng & Kết nối
┌──────────────────────────────▼──────────────────────────────┐
│  4. TẦNG THỰC THỂ & CẤU HÌNH (Models, Enums, Config)        │
│     Customer, Vehicle, Employee, Invoice, Part, BaseService │
│     DatabaseConfig, Role, RepairStatus, Shift               │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. NGUYÊN LÝ 1: TÍNH ĐÓNG GÓI (ENCAPSULATION)

### 💡 Khái niệm:
**Tính đóng gói** là việc che giấu dữ liệu bên trong đối tượng (thông qua phạm vi truy cập `private`), ngăn chặn sự can thiệp và truy cập trực tiếp từ bên ngoài. Mọi thao tác truy xuất hoặc sửa đổi dữ liệu bắt buộc phải thông qua các phương thức công khai (`getter`, `setter`) hoặc các phương thức nghiệp vụ hợp lệ.

---

### 🔍 Ứng dụng trong dự án:

#### 1. Đóng gói trạng thái đối tượng trong các lớp Entity (Domain Models)
Tất cả các thuộc tính của các lớp như `Customer`, `Vehicle`, `Employee`, `Part`, `Invoice`, `User` đều được khai báo là `private`.

* **Ví dụ trong [`Employee.java`](file:///e:/Code/OOP/garage-management/src/com/garage/models/Employee.java):**
```java
package com.garage.models;

public class Employee {
    // 1. Dữ liệu được che giấu với phạm vi truy cập private
    private String id;
    private String name;
    private String phone;
    private String cccd;
    private String role;
    private String shift;
    private int shiftCount;
    private double totalSalary;
    private String notes;
    private String managedBy;
    private boolean isNotified;

    // 2. Cung cấp getter/setter để kiểm soát truy xuất
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getShiftCount() { return shiftCount; }
    public void setShiftCount(int shiftCount) { this.shiftCount = shiftCount; }

    public double getTotalSalary() { return totalSalary; }
    public void setTotalSalary(double totalSalary) { this.totalSalary = totalSalary; }

    public String getManagedBy() { return managedBy; }
    public void setManagedBy(String managedBy) { this.managedBy = managedBy; }

    public boolean isNotified() { return isNotified; }
    public void setNotified(boolean isNotified) { this.isNotified = isNotified; }
}
```

#### 2. Đóng gói logic kết nối và quản trị CSDL trong [`DatabaseConfig.java`](file:///e:/Code/OOP/garage-management/src/com/garage/config/DatabaseConfig.java)
Các thông số máy chủ, mật khẩu và logic tự động thử nhiều mật khẩu dự phòng được che giấu bên trong `DatabaseConfig`. Các lớp khác chỉ cần gọi hàm công khai duy nhất `DatabaseConfig.getConnection()` mà không cần biết chi tiết mật khẩu là gì hay kết nối ra sao.

#### 3. Đóng gói quy tắc nghiệp vụ trong các lớp Service
Trong [`BillingManager.java`](file:///e:/Code/OOP/garage-management/src/com/garage/services/BillingManager.java), phương thức `createInvoice()` đóng gói toàn bộ quy trình: *sinh mã hóa đơn duy nhất $\rightarrow$ trừ tồn kho phụ tùng $\rightarrow$ ghi log thẻ kho $\rightarrow$ chuyển trạng thái xe sang COMPLETED $\rightarrow$ lưu hóa đơn vào CSDL*. Giao diện `MainFrame` chỉ cần gọi đúng 1 hàm mà không cần tự xử lý từng bước phức tạp.

---

## 3. NGUYÊN LÝ 2: TÍNH TRỪU TƯỢNG (ABSTRACTION)

### 💡 Khái niệm:
**Tính trừu tượng** tập trung vào việc định nghĩa đối tượng làm được gì (hành vi/chức năng) thay vì chi tiết cài đặt như thế nào. Tính trừu tượng giúp ẩn đi sự phức tạp và tạo ra các bộ khung tiêu chuẩn cho hệ thống thông qua `abstract class` hoặc `interface`.

---

### 🔍 Ứng dụng trong dự án:

#### 1. Lớp dịch vụ trừu tượng [`BaseService.java`](file:///e:/Code/OOP/garage-management/src/com/garage/models/BaseService.java)
Hệ thống gara có nhiều loại dịch vụ (Thay dầu, Bảo dưỡng định kỳ, Sửa động cơ...). Mỗi dịch vụ có công thức tính chi phí hoàn toàn khác nhau. Lớp trừu tượng `BaseService` định nghĩa khung chung cho mọi dịch vụ:

```java
package com.garage.models;

// Khai báo abstract class đại diện cho một dịch vụ trừu tượng
public abstract class BaseService {
    protected String serviceId;
    protected String serviceName;
    protected double basePrice;

    public BaseService(String serviceId, String serviceName, double basePrice) {
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.basePrice = basePrice;
    }

    public String getServiceId() { return serviceId; }
    public String getServiceName() { return serviceName; }
    public double getBasePrice() { return basePrice; }

    // PHƯƠNG THỨC TRỪU TƯỢNG: Bắt buộc mọi lớp con phải tự cài đặt cách tính phí
    public abstract double calculateCost();
}
```

#### 2. Lớp bảng điều khiển báo cáo trừu tượng [`BaseReportPanel.java`](file:///e:/Code/OOP/garage-management/src/com/garage/gui/BaseReportPanel.java)
Cung cấp bộ khung giao diện chung cho mọi loại báo cáo (bộ chọn Tháng, bộ chọn Năm, nút Làm mới, khu vực hiển thị). Phương thức trừu tượng `renderReportContent(...)` quy định cách hiển thị nội dung cụ thể:

```java
public abstract class BaseReportPanel extends JPanel {
    protected final BillingManager billingManager;
    protected final VehicleManager vehicleManager;

    // Khung phương thức dùng chung cho mọi panel báo cáo
    public void reloadReport() {
        int selectedMonth = Integer.parseInt((String) cbReportMonth.getSelectedItem());
        int selectedYear = Integer.parseInt((String) cbReportYear.getSelectedItem());
        List<Invoice> invoices = billingManager.getAllInvoices();
        List<Vehicle> vehicles = vehicleManager.getAllVehicles();

        // Gọi phương thức trừu tượng để lớp con tự vẽ nội dung
        renderReportContent(invoices, vehicles, selectedMonth, selectedYear);
    }

    // PHƯƠNG THỨC TRỪU TƯỢNG
    protected abstract void renderReportContent(List<Invoice> invoices, List<Vehicle> vehicles, int selectedMonth, int selectedYear);
}
```

#### 3. Trừu tượng hóa tầng CSDL qua Repository Pattern
Các lớp `CustomerRepository`, `InvoiceRepository`, `VehicleRepository` trừu tượng hóa toàn bộ câu lệnh SQL phức tạp (`SELECT`, `INSERT`, `UPDATE`, `JOIN`). Tầng Service và GUI chỉ làm việc với các phương thức thuần đối tượng như `save(Customer)`, `findAll()`, `findById()`.

---

## 4. NGUYÊN LÝ 3: TÍNH KẾ THỪA (INHERITANCE)

### 💡 Khái niệm:
**Tính kế thừa** cho phép một lớp con kế thừa lại các thuộc tính và phương thức từ lớp cha thông qua từ khóa `extends`. Tính kế thừa giúp tái sử dụng mã nguồn tối đa, tránh trùng lặp code và xây dựng mối quan hệ phân cấp **"is-a" (là một)**.

---

### 🔍 Ứng dụng trong dự án:

#### 1. Cây kế thừa Dịch vụ Sửa chữa (Service Hierarchy)
Các lớp dịch vụ cụ thể kế thừa từ `BaseService` và tái sử dụng các thuộc tính `serviceId`, `serviceName`, `basePrice`:

```
          ┌───────────────────────────────────┐
          │     BaseService (abstract)        │
          │   # serviceId, serviceName, price │
          │   + calculateCost()*              │
          └─────────────────┬─────────────────┘
                            │ extends
         ┌──────────────────┼──────────────────┐
         │                  │                  │
┌────────▼────────┐ ┌───────▼────────┐ ┌───────▼─────────┐
│ OilChangeService│ │MaintenanceServ.│ │EngineRepairServ.│
└─────────────────┘ └────────────────┘ └─────────────────┘
```

* **Lớp con [`OilChangeService.java`](file:///e:/Code/OOP/garage-management/src/com/garage/models/OilChangeService.java):**
```java
public class OilChangeService extends BaseService {
    private double oilVolumeLiters;
    private double pricePerLiter;

    public OilChangeService(String serviceId, double oilVolumeLiters, double pricePerLiter) {
        // Tái sử dụng constructor của lớp cha thông qua super()
        super(serviceId, "Thay dầu & Nhớt engine", 100000);
        this.oilVolumeLiters = oilVolumeLiters;
        this.pricePerLiter = pricePerLiter;
    }

    @Override
    public double calculateCost() {
        return basePrice + (oilVolumeLiters * pricePerLiter);
    }
}
```

* **Lớp con [`MaintenanceService.java`](file:///e:/Code/OOP/garage-management/src/com/garage/models/MaintenanceService.java):**
```java
public class MaintenanceService extends BaseService {
    private double laborHours;
    private double hourlyRate;

    public MaintenanceService(String serviceId, double laborHours, double hourlyRate) {
        super(serviceId, "Bảo trì định kỳ hệ thống", 200000);
        this.laborHours = laborHours;
        this.hourlyRate = hourlyRate;
    }

    @Override
    public double calculateCost() {
        return basePrice + (laborHours * hourlyRate);
    }
}
```

* **Lớp con [`EngineRepairService.java`](file:///e:/Code/OOP/garage-management/src/com/garage/models/EngineRepairService.java):**
```java
public class EngineRepairService extends BaseService {
    private double partsCost;
    private double complexityFactor;

    public EngineRepairService(String serviceId, double partsCost, double complexityFactor) {
        super(serviceId, "Sửa chữa Động cơ & Hộp số", 500000);
        this.partsCost = partsCost;
        this.complexityFactor = complexityFactor;
    }

    @Override
    public double calculateCost() {
        return (basePrice + partsCost) * complexityFactor;
    }
}
```

#### 2. Cây kế thừa Báo cáo Giao diện (Report Hierarchy)
* [`TableSummaryReportPanel`](file:///e:/Code/OOP/garage-management/src/com/garage/gui/TableSummaryReportPanel.java) `extends BaseReportPanel`
* [`ServiceChartReportPanel`](file:///e:/Code/OOP/garage-management/src/com/garage/gui/ServiceChartReportPanel.java) `extends BaseReportPanel`

#### 3. Kế thừa thư viện đồ họa chuẩn Java Swing
* `LoginFrame`, `MainFrame` `extends JFrame`
* `AccountManagementDialog`, `ChangePasswordDialog` `extends JDialog`
* `BaseReportPanel` `extends JPanel`

---

## 5. NGUYÊN LÝ 4: TÍNH ĐA HÌNH (POLYMORPHISM)

### 💡 Khái niệm:
**Tính đa hình** (Polymorphism) là khả năng các đối tượng khác nhau có thể phản ứng khác nhau đối với cùng một thông điệp (lời gọi phương thức).
- **Đa hình lúc chạy (Runtime Polymorphism / Overriding):** Gọi cùng một tên hàm trên đối tượng lớp cha nhưng mã thực thi sẽ tự động trỏ đến hàm của lớp con cụ thể.
- **Đa hình lúc biên dịch (Compile-time Polymorphism / Overloading):** Cùng tên hàm nhưng khác danh sách tham số.

---

### 🔍 Ứng dụng trong dự án:

#### 1. Đa hình động (Runtime Polymorphism) với [`RepairOrder.java`](file:///e:/Code/OOP/garage-management/src/com/garage/models/RepairOrder.java)
Lớp `RepairOrder` quản lý một danh sách các dịch vụ dạng tổng quát `List<BaseService>`. Khi tính tổng tiền, `RepairOrder` chỉ cần duyệt qua danh sách và gọi `s.calculateCost()`. Cơ chế đa hình của Java (Dynamic Method Dispatch) sẽ tự động chạy đúng công thức tính giá của từng loại dịch vụ tương ứng:

```java
package com.garage.models;

import java.util.ArrayList;
import java.util.List;

public class RepairOrder {
    private String orderId;
    private Vehicle vehicle;
    // Danh sách kiểu lớp cha BaseService chứa được mọi đối tượng con
    private List<BaseService> services;

    public RepairOrder(String orderId, Vehicle vehicle) {
        this.orderId = orderId;
        this.vehicle = vehicle;
        this.services = new ArrayList<>();
    }

    public void addService(BaseService service) {
        this.services.add(service);
    }

    // ĐA HÌNH Ở ĐÂY:
    public double calculateTotalCost() {
        double total = 0;
        for (BaseService s : services) {
            // Lời gọi hàm s.calculateCost() sẽ tự động gọi:
            // - OilChangeService.calculateCost() nếu s là dịch vụ thay dầu
            // - MaintenanceService.calculateCost() nếu s là dịch vụ bảo dưỡng
            // - EngineRepairService.calculateCost() nếu s là dịch vụ sửa động cơ
            total += s.calculateCost();
        }
        return total;
    }
}
```

#### 2. Đa hình động trong khởi tạo dịch vụ tại [`MainFrame.java`](file:///e:/Code/OOP/garage-management/src/com/garage/gui/MainFrame.java)
Khi Lễ tân chọn dịch vụ từ giao diện, hệ thống khai báo biến kiểu lớp cha `BaseService` nhưng gán đối tượng thực tế là một trong các lớp con:

```java
BaseService service;
if (serviceIndex == 0) {
    service = new OilChangeService("SRV01", 4.0, 50000);      // 100k + (4 * 50k) = 300,000
} else if (serviceIndex == 1) {
    service = new MaintenanceService("SRV02", 3.0, 100000);   // 200k + (3 * 100k) = 500,000
} else {
    service = new EngineRepairService("SRV03", 500000, 1.5);   // (500k + 500k) * 1.5 = 1,500,000
}

// Gọi phương thức đa hình
double servicePrice = service.calculateCost();
```

#### 3. Đa hình động trong Tải báo cáo
Trong [`BaseReportPanel.java`](file:///e:/Code/OOP/garage-management/src/com/garage/gui/BaseReportPanel.java), phương thức `reloadReport()` gọi hàm `renderReportContent(...)`:
* Nếu là đối tượng `TableSummaryReportPanel` $\rightarrow$ Nạp dữ liệu vào bảng `JTable`.
* Nếu là đối tượng `ServiceChartReportPanel` $\rightarrow$ Tính toán tọa độ và vẽ biểu đồ thanh (Bar Chart) trực quan với `Graphics2D`.

#### 4. Đa hình tĩnh (Method / Constructor Overloading)
Hầu hết các Entity Models (`Employee`, `Customer`, `Vehicle`, `Part`, `Invoice`, `User`) đều có **nạp chồng Constructor**:
- Constructor mặc định không tham số: phục vụ khởi tạo rỗng hoặc ánh xạ từ CSDL.
- Constructor đầy đủ tham số: phục vụ tạo nhanh đối tượng nghiệp vụ khi nhập từ giao diện Swing.

---

## 6. PHÂN TÍCH LUỒNG NGHIỆP VỤ CỐT LÕI

### 🚗 Luồng 1: Tiếp nhận xe $\rightarrow$ Sửa chữa $\rightarrow$ Xuất hóa đơn
1. **Tiếp nhận xe:** Lễ tân nhập biển số, dòng xe, mã khách hàng $\rightarrow$ `VehicleManager.addVehicle()` $\rightarrow$ `VehicleRepository.save()` $\rightarrow$ CSDL lưu trạng thái xe là `WAITING` (Chờ sửa).
2. **Cập nhật tiến độ:** KTV sửa xe $\rightarrow$ Lễ tân đổi trạng thái sang `IN_PROGRESS` (Đang sửa) $\rightarrow$ `VehicleManager.updateVehicleStatus()`.
3. **Xuất hóa đơn & Trừ kho:**
   - Chọn dịch vụ $\rightarrow$ `BaseService.calculateCost()` tính chi phí dịch vụ đa hình.
   - Chọn phụ tùng $\rightarrow$ `BillingManager.createInvoice()`:
     - Tạo mã hóa đơn Timestamp không trùng `INV-yyMMddHHmmssSSS`.
     - Gọi `PartRepository.reduceStock()` trừ số lượng tồn kho.
     - Gọi `PartRepository.logPartTransaction()` ghi nhật ký xuất kho.
     - Chuyển trạng thái xe thành `COMPLETED` (Đã hoàn thành).
     - Lưu hóa đơn vào bảng `invoices`.

---

### 👨‍🔧 Luồng 2: Thêm KTV $\rightarrow$ Phân công Lễ tân $\rightarrow$ Thông báo đăng nhập
1. **Chủ Garage thêm KTV mới:** Nhập thông tin KTV và chọn ca làm việc (`Ca 1` hoặc `Ca 2`).
2. **Thuật toán Cân bằng tải & Random ([`EmployeeRepository.java`](file:///e:/Code/OOP/garage-management/src/com/garage/repository/EmployeeRepository.java)):**
   - Lọc danh sách Lễ tân làm việc **cùng ca** với KTV mới.
   - Đếm số lượng KTV mà từng Lễ tân đang quản lý.
   - Ưu tiên gán KTV mới cho Lễ tân đang có **ít KTV nhất**.
   - Nếu các Lễ tân có số KTV bằng nhau $\rightarrow$ Dùng `new Random().nextInt()` để chọn ngẫu nhiên 1 người.
   - Lưu vào CSDL với `managed_by = [Mã Lễ Tân]` và `is_notified = FALSE`.
3. **Thông báo khi Lễ tân đăng nhập lần đầu:**
   - Lễ tân đăng nhập vào hệ thống $\rightarrow$ `MainFrame` kiểm tra `getUnnotifiedKtvsForReceptionist()`.
   - Hiển thị popup: *"Kỹ thuật viên: [Tên KTV] đã được thêm vào danh sách chấm công nhóm của bạn!"*.
   - Gọi `markKtvsAsNotified()` cập nhật `is_notified = TRUE` để không hiện lặp lại ở lần đăng nhập sau.
4. **Quản lý chấm công nhóm:**
   - Tab *"Lịch Sử Chấm Công Nhóm"* của Lễ tân chỉ hiển thị đúng các KTV do mình phụ trách để điểm danh hộ.

---

## 7. BẢNG TỔNG HỢP ĐỐI CHIẾU OOP TRONG DỰ ÁN

| Nguyên Lý OOP | Thành Phần / Lớp Triển Khai | Mã Nguồn Đại Diện | Lợi Ích & Mục Đích |
| :--- | :--- | :--- | :--- |
| **Đóng gói (Encapsulation)** | `Customer`, `Vehicle`, `Employee`, `Part`, `Invoice`, `User` | Thuộc tính `private`, truy xuất qua `getter`/`setter` | Bảo vệ toàn vẹn dữ liệu, kiểm soát truy cập hợp lệ |
| **Đóng gói (Encapsulation)** | `DatabaseConfig`, `BillingManager` | Che giấu kết nối CSDL, đóng gói giao dịch trừ kho | Giúp code bên ngoài gọn gàng, giảm phụ thuộc (loose coupling) |
| **Trừu tượng (Abstraction)** | `BaseService` | `public abstract double calculateCost();` | Định nghĩa khung phương thức tính giá chung cho mọi dịch vụ |
| **Trừu tượng (Abstraction)** | `BaseReportPanel` | `protected abstract void renderReportContent(...)` | Định nghĩa khung giao diện báo cáo, ẩn chi tiết hiển thị |
| **Trừu tượng (Abstraction)** | Tầng `Repository` (DAO) | `CustomerRepository`, `InvoiceRepository`... | Ẩn chi tiết câu lệnh SQL, cung cấp hàm thuần đối tượng |
| **Kế thừa (Inheritance)** | `OilChangeService`, `MaintenanceService`, `EngineRepairService` | `extends BaseService` | Tái sử dụng mã nguồn của `BaseService`, mở rộng công thức riêng |
| **Kế thừa (Inheritance)** | `TableSummaryReportPanel`, `ServiceChartReportPanel` | `extends BaseReportPanel` | Tái sử dụng khung lọc tháng/năm, làm mới báo cáo |
| **Đa hình (Polymorphism)** | `RepairOrder` chứa `List<BaseService>` | `s.calculateCost()` trong vòng lặp `for` | Tính tổng tiền linh hoạt cho bất kỳ tổ hợp dịch vụ nào |
| **Đa hình (Polymorphism)** | Nạp chồng Constructor | `Employee()`, `Employee(...)` | Cho phép tạo đối tượng linh hoạt lúc biên dịch (Overloading) |

---
*Tài liệu được biên soạn phục vụ cho việc học tập, bảo vệ đồ án và thuyết minh kiến trúc Hướng đối tượng của hệ thống Garage Management.*
