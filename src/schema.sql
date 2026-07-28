CREATE DATABASE IF NOT EXISTS garage_db;
USE garage_db;

-- 1. BẢNG NGƯỜI DÙNG HỆ THỐNG

CREATE TABLE IF NOT EXISTS users (
    username VARCHAR(50) PRIMARY KEY,
    password VARCHAR(100) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL
);

-- 2. BẢNG KHÁCH HÀNG

CREATE TABLE IF NOT EXISTS customers (
    id VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) DEFAULT '---'
);

-- 3. BẢNG XE & TRẠNG THÁI TIẾP NHẬN

CREATE TABLE IF NOT EXISTS vehicles (
    license_plate VARCHAR(20) PRIMARY KEY,
    brand VARCHAR(50) NOT NULL,
    model VARCHAR(50) DEFAULT '---',
    owner_id VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    FOREIGN KEY (owner_id) REFERENCES customers(id) ON DELETE CASCADE
);

-- 4. BẢNG KHO PHỤ TÙNG

CREATE TABLE IF NOT EXISTS parts (
    id VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    import_price DOUBLE NOT NULL,
    export_price DOUBLE NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0
);

-- 5. BẢNG NHÂN VIÊN

CREATE TABLE IF NOT EXISTS employees (
    id VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) DEFAULT '---',
    cccd VARCHAR(20) DEFAULT '---',
    role VARCHAR(50) NOT NULL,
    shift VARCHAR(50) NOT NULL
);


-- 6. BẢNG NHẬT KÝ CHẤM CÔNG

CREATE TABLE IF NOT EXISTS attendance_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id VARCHAR(20) NOT NULL,
    employee_name VARCHAR(100) NOT NULL,
    check_in_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    shift VARCHAR(50) NOT NULL,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
);


-- 7. BẢNG HÓA ĐƠN DỊCH VỤ

CREATE TABLE IF NOT EXISTS invoices (
    id VARCHAR(50) PRIMARY KEY,
    license_plate VARCHAR(20) NOT NULL,
    service_name VARCHAR(100) DEFAULT 'Dịch vụ gara',
    part_info VARCHAR(150) DEFAULT '---',
    notes VARCHAR(255) DEFAULT '---',
    created_by VARCHAR(50) NOT NULL,
    total_amount DOUBLE NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (license_plate) REFERENCES vehicles(license_plate) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(username) ON DELETE CASCADE
);

-- 8. BẢNG THẺ KHO / LỊCH SỬ NHẬP & XUẤT PHỤ TÙNG
CREATE TABLE IF NOT EXISTS part_usage_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    part_id VARCHAR(20) NOT NULL,
    part_name VARCHAR(100) NOT NULL,
    quantity INT NOT NULL,
    used_for_vehicle VARCHAR(50) NOT NULL,
    created_by VARCHAR(50) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (part_id) REFERENCES parts(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(username) ON DELETE CASCADE
);

-- SEED DATABASE

-- 1. Tài khoản hệ thống
INSERT IGNORE INTO users (username, password, full_name, role) VALUES 
('admin', '123456', 'Chủ Garage', 'ADMIN'),
('hoatt', '123456', 'Trần Thị Hòa', 'RECEPTIONIST'),
('namlv', '123456', 'Lê Văn Nam', 'RECEPTIONIST');

-- 2. Danh sách nhân sự đầy đủ các ca
INSERT IGNORE INTO employees (id, name, phone, cccd, role, shift) VALUES 
('ADMIN', 'Chủ Garage', '0901112233', '001099887766', 'Chủ Garage', 'Toàn thời gian'),
('NV01', 'Trần Thị Hòa', '0912345678', '001122334455', 'Lễ Tân', 'Ca 1 (06:00 - 14:00)'),
('NV02', 'Lê Văn Nam', '0988776655', '001122334466', 'Lễ Tân', 'Ca 2 (14:00 - 22:00)'),
('NV03', 'Nguyễn Văn Mạnh', '0977112233', '001122334477', 'Kỹ Thuật Viên', 'Ca 1 (06:00 - 14:00)'),
('NV04', 'Phạm Hoàng Long', '0966445566', '001122334488', 'Kỹ Thuật Viên', 'Ca 1 (06:00 - 14:00)'),
('NV05', 'Bùi Đức Anh', '0955332211', '001122334499', 'Kỹ Thuật Viên', 'Ca 2 (14:00 - 22:00)'),
('NV06', 'Vũ Thị Mai', '0944221100', '001122335500', 'Kỹ Thuật Viên', 'Ca 2 (14:00 - 22:00)');

-- 3. Khách hàng mẫu
INSERT IGNORE INTO customers (id, name, phone) VALUES 
('KH01', 'Nguyễn Việt Đông', '0987654321'),
('KH02', 'Phạm Minh Anh', '0912888999'),
('KH03', 'Hoàng Văn Thái', '0934567890'),
('KH04', 'Đỗ Thùy Linh', '0978111222'),
('KH05', 'Trịnh Quốc Bảo', '0905666777');

-- 4. Xe tiếp nhận mẫu (đủ các trạng thái)
INSERT IGNORE INTO vehicles (license_plate, brand, model, owner_id, status) VALUES 
('30A-88888', 'Toyota', 'Camry 2.5Q', 'KH01', 'COMPLETED'),
('15B-67890', 'Honda', 'CR-V Turbo', 'KH02', 'COMPLETED'),
('29A-12345', 'Mazda', 'Mazda3 Sedan', 'KH03', 'IN_PROGRESS'),
('30E-99999', 'Mercedes-Benz', 'GLC 300', 'KH04', 'WAITING'),
('14C-55555', 'Ford', 'Ranger XLS', 'KH05', 'COMPLETED');

-- 5. Phụ tùng trong kho mẫu
INSERT IGNORE INTO parts (id, name, import_price, export_price, stock_quantity) VALUES 
('PT01', 'Dầu động cơ Castrol Edge 5W-30 (Lít)', 180000, 250000, 45),
('PT02', 'Lọc dầu động cơ Toyota/Honda', 80000, 150000, 25),
('PT03', 'Bộ má phanh trước Brembo', 850000, 1350000, 10),
('PT04', 'Bugi Iridium Denso', 120000, 220000, 36),
('PT05', 'Lọc gió điều hòa Carking', 90000, 180000, 20),
('PT06', 'Gạt mưa silicone Bosch 22 inch', 150000, 280000, 15);

-- 6. Nhật ký chấm công mẫu
INSERT IGNORE INTO attendance_logs (id, employee_id, employee_name, check_in_time, shift) VALUES 
(1, 'NV01', 'Trần Thị Hòa', '2026-07-26 06:15:00', 'Ca 1 (06:00 - 14:00)'),
(2, 'NV03', 'Nguyễn Văn Mạnh', '2026-07-26 06:20:00', 'Ca 1 (06:00 - 14:00)'),
(3, 'NV04', 'Phạm Hoàng Long', '2026-07-26 06:22:00', 'Ca 1 (06:00 - 14:00)'),
(4, 'NV02', 'Lê Văn Nam', '2026-07-26 14:05:00', 'Ca 2 (14:00 - 22:00)'),
(5, 'NV05', 'Bùi Đức Anh', '2026-07-26 14:10:00', 'Ca 2 (14:00 - 22:00)'),
(6, 'NV01', 'Trần Thị Hòa', '2026-07-27 06:10:00', 'Ca 1 (06:00 - 14:00)'),
(7, 'NV03', 'Nguyễn Văn Mạnh', '2026-07-27 06:18:00', 'Ca 1 (06:00 - 14:00)'),
(8, 'ADMIN', 'Chủ Garage', '2026-07-27 08:00:00', 'Toàn thời gian');

-- 7. Nhật ký nhập/xuất kho mẫu
INSERT IGNORE INTO part_usage_logs (id, part_id, part_name, quantity, used_for_vehicle, created_by, created_at) VALUES 
(1, 'PT01', 'Dầu động cơ Castrol Edge 5W-30 (Lít)', 50, 'NHẬP KHO', 'admin', '2026-07-20 08:00:00'),
(2, 'PT02', 'Lọc dầu động cơ Toyota/Honda', 30, 'NHẬP KHO', 'admin', '2026-07-20 08:15:00'),
(3, 'PT03', 'Bộ má phanh trước Brembo', 12, 'NHẬP KHO', 'admin', '2026-07-20 08:30:00'),
(4, 'PT02', 'Lọc dầu động cơ Toyota/Honda', 1, '30A-88888', 'hoatt', '2026-07-22 10:15:00'),
(5, 'PT03', 'Bộ má phanh trước Brembo', 2, '15B-67890', 'namlv', '2026-07-24 15:30:00'),
(6, 'PT01', 'Dầu động cơ Castrol Edge 5W-30 (Lít)', 5, '14C-55555', 'hoatt', '2026-07-25 09:45:00');

-- 8. Hóa đơn mẫu
INSERT IGNORE INTO invoices (id, license_plate, service_name, part_info, notes, created_by, total_amount, created_at) VALUES 
('INV-1001', '30A-88888', 'Thay dầu & Lọc dầu (150.000đ)', 'Lọc dầu động cơ Toyota/Honda (SL: 1)', 'Khách yêu cầu kiểm tra áp suất lốp', 'hoatt', 300000, '2026-07-22 10:30:00'),
('INV-1002', '15B-67890', 'Thay thế phụ tùng kho', 'Bộ má phanh trước Brembo (SL: 2)', 'Thay má phanh trước 2 bên', 'namlv', 2700000, '2026-07-24 15:45:00'),
('INV-1003', '14C-55555', 'Bảo dưỡng định kỳ (250.000đ)', 'Dầu động cơ Castrol Edge 5W-30 (Lít) (SL: 5)', 'Khách hẹn tuần sau quay lại rửa xe', 'hoatt', 1500000, '2026-07-25 10:00:00');