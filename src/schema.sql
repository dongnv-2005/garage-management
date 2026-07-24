-- =========================================================
-- 1. TẠO CƠ SỞ DỮ LIỆU VÀ CHỌN DATABASE
-- =========================================================
CREATE DATABASE IF NOT EXISTS garage_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE garage_db;

-- =========================================================
-- 2. ĐỊNH NGHĨA BẢNG DỮ LIỆU (SCHEMAS)
-- =========================================================

-- Bảng Tài khoản Người dùng
CREATE TABLE IF NOT EXISTS users (
    username VARCHAR(50) PRIMARY KEY,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL
);

-- Bảng Nhân viên & Ca làm việc
CREATE TABLE IF NOT EXISTS employees (
    id VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(15),
    role VARCHAR(50) NOT NULL,
    shift VARCHAR(50) NOT NULL
);

-- Bảng Khách hàng
CREATE TABLE IF NOT EXISTS customers (
    id VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(15) NOT NULL
);

-- Bảng Xe gửi Gara
CREATE TABLE IF NOT EXISTS vehicles (
    license_plate VARCHAR(20) PRIMARY KEY,
    brand VARCHAR(50) NOT NULL,
    model VARCHAR(50) NOT NULL,
    owner_id VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    FOREIGN KEY (owner_id) REFERENCES customers(id) ON DELETE CASCADE
);

-- Bảng Phụ tùng Kho
CREATE TABLE IF NOT EXISTS parts (
    id VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    import_price DOUBLE NOT NULL,
    export_price DOUBLE NOT NULL,
    stock_quantity INT NOT NULL
);

-- Bảng Hóa đơn Thanh toán
CREATE TABLE IF NOT EXISTS invoices (
    id VARCHAR(20) PRIMARY KEY,
    license_plate VARCHAR(20) NOT NULL,
    created_by VARCHAR(50) NOT NULL,
    total_amount DOUBLE NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (license_plate) REFERENCES vehicles(license_plate),
    FOREIGN KEY (created_by) REFERENCES users(username)
);

-- Bảng Lịch sử Chấm công Nhân viên
CREATE TABLE IF NOT EXISTS attendance_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id VARCHAR(20) NOT NULL,
    employee_name VARCHAR(100) NOT NULL,
    check_in_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    shift VARCHAR(50) NOT NULL,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
);

-- =========================================================
-- 3. BỘ DỮ LIỆU MẪU (SEED DATA)
-- =========================================================

-- A. Tài khoản Đăng nhập mẫu (Đã cập nhật theo yêu cầu)
INSERT IGNORE INTO users (username, password, full_name, role) VALUES 
('admin', '123456', 'Chủ Garage', 'ADMIN'),
('hoatt', '123456', 'Trần Thị Hòa', 'RECEPTIONIST'),
('namlv', '123456', 'Lê Văn Nam', 'RECEPTIONIST');

-- B. Danh sách Nhân viên & Ca làm việc (2 Ca: 06:00-14:00 và 14:00-22:00)
INSERT IGNORE INTO employees (id, name, phone, role, shift) VALUES 
('NV01', 'Trần Thị Hòa', '0912345678', 'Lễ Tân', 'Ca 1 (06:00 - 14:00)'),
('NV02', 'Lê Văn Nam', '0988776655', 'Lễ Tân', 'Ca 2 (14:00 - 22:00)'),
('NV03', 'Phạm Quốc Bảo', '0933445566', 'Kỹ Thuật Viên', 'Ca 1 (06:00 - 14:00)'),
('NV04', 'Hoàng Minh Tuấn', '0977112233', 'Kỹ Thuật Viên', 'Ca 1 (06:00 - 14:00)'),
('NV05', 'Đặng Văn Lâm', '0905554433', 'Kỹ Thuật Viên', 'Ca 2 (14:00 - 22:00)');

-- C. Danh sách Khách hàng mẫu
INSERT IGNORE INTO customers (id, name, phone) VALUES 
('KH01', 'Nguyễn Việt Đông', '0987654321'),
('KH02', 'Phạm Minh Anh', '0912888999'),
('KH03', 'Trần Đình Trọng', '0934111222');

-- D. Danh sách Xe tiếp nhận
INSERT IGNORE INTO vehicles (license_plate, brand, model, owner_id, status) VALUES 
('29A-12345', 'Toyota', 'Camry', 'KH01', 'COMPLETED'),
('30F-99999', 'BMW', 'X5', 'KH01', 'IN_PROGRESS'),
('30H-88888', 'Honda', 'CR-V', 'KH02', 'WAITING'),
('15B-67890', 'Mercedes-Benz', 'Glc 300', 'KH03', 'COMPLETED');

-- E. Phụ tùng Kho mẫu
INSERT IGNORE INTO parts (id, name, import_price, export_price, stock_quantity) VALUES 
('PT01', 'Dầu động cơ Castrol Edge 5W-30 (Lít)', 180000, 250000, 50),
('PT02', 'Lọc dầu động cơ Toyota/Honda', 80000, 150000, 30),
('PT03', 'Bộ má phanh trước Brembo', 850000, 1350000, 12),
('PT04', 'Bugi Iridium Denso', 120000, 220000, 40);

-- F. Lịch sử Hóa đơn mẫu (Đồng bộ tài khoản tạo)
INSERT IGNORE INTO invoices (id, license_plate, created_by, total_amount, created_at) VALUES 
('INV-1001', '29A-12345', 'hoatt', 1450000, '2026-07-20 09:30:00'),
('INV-1002', '15B-67890', 'namlv', 3200000, '2026-07-22 15:15:00');