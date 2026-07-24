package com.garage.gui;

import com.garage.config.DatabaseConfig;
import com.garage.enums.RepairStatus;
import com.garage.enums.Role;
import com.garage.services.AuthService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class MainFrame extends JFrame {
    private DefaultTableModel customerModel, vehicleModel, partModel, employeeModel, attendanceModel,
            invoiceHistoryModel;

    public MainFrame() {
        Role currentRole = AuthService.getCurrentUser().getRole();
        String currentUsername = AuthService.getCurrentUser().getUsername();
        String currentFullName = AuthService.getCurrentUser().getFullName();

        setTitle("HỆ THỐNG QUẢN LÝ GARA Ô TÔ - [" + currentFullName + " - " + currentRole.getDescription() + "]");
        setSize(1100, 720);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("SansSerif", Font.BOLD, 12));
        tabbedPane.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);

        tabbedPane.addTab("Quản lý Khách hàng", createCustomerPanel());
        tabbedPane.addTab("Tiếp nhận & Trạng thái Xe", createVehiclePanel());
        tabbedPane.addTab("Quản lý Kho Phụ tùng", createPartPanel());
        tabbedPane.addTab("Gán Dịch vụ & Xuất Hóa đơn", createBillingPanel());

        if (currentRole == Role.ADMIN) {
            tabbedPane.addTab("Quản lý Nhân sự & Chấm công", createEmployeePanel());
            tabbedPane.addTab("Báo cáo Doanh thu & Thống kê", createReportPanel());
        }

        // Dàn đều độ rộng các Tab
        int totalTabs = tabbedPane.getTabCount();
        for (int i = 0; i < totalTabs; i++) {
            JLabel tabLabel = new JLabel(tabbedPane.getTitleAt(i), SwingConstants.CENTER);
            tabLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
            tabLabel.setPreferredSize(new Dimension(1050 / totalTabs, 32));
            tabbedPane.setTabComponentAt(i, tabLabel);
        }

        // =========================================================
        // HEADER TOP BAR
        // =========================================================
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        topPanel.setBackground(new Color(236, 240, 241));

        JLabel lblUser = new JLabel("Xin chào: " + currentFullName + " (" + currentRole.getDescription() + ")");
        lblUser.setFont(new Font("SansSerif", Font.BOLD, 14));

        JPanel rightActionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightActionPanel.setOpaque(false);

        if (currentRole != Role.ADMIN) {
            JButton btnCheckinSelf = createCustomButton("TÍCH ĐI LÀM NGAY", new Color(46, 204, 113));
            btnCheckinSelf.addActionListener(e -> handleSelfCheckin(currentUsername, currentFullName));
            rightActionPanel.add(btnCheckinSelf);
        }

        JButton btnChangePass = createCustomButton("Đổi Mật Khẩu", new Color(52, 152, 219));
        btnChangePass.addActionListener(e -> new ChangePasswordDialog(this).setVisible(true));

        JButton btnLogout = createCustomButton("Đăng Xuất", new Color(231, 76, 60));
        btnLogout.addActionListener(e -> {
            AuthService.logout();
            this.dispose();
            new LoginFrame().setVisible(true);
        });

        rightActionPanel.add(btnChangePass);
        rightActionPanel.add(btnLogout);

        topPanel.add(lblUser, BorderLayout.WEST);
        topPanel.add(rightActionPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);

        loadCustomers();
        loadVehicles();
        loadParts();
        if (currentRole == Role.ADMIN) {
            loadEmployees();
            loadAttendanceLogs();
        }
    }

    private void handleSelfCheckin(String username, String fullName) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            String shift = "Ca 1 (06:00 - 14:00)";
            PreparedStatement p1 = conn.prepareStatement("SELECT shift FROM employees WHERE id = ? OR name LIKE ?");
            p1.setString(1, username);
            p1.setString(2, "%" + fullName + "%");
            ResultSet rs = p1.executeQuery();
            if (rs.next()) {
                shift = rs.getString("shift");
            }

            PreparedStatement p2 = conn.prepareStatement(
                    "INSERT INTO attendance_logs (employee_id, employee_name, shift) VALUES (?, ?, ?)");
            p2.setString(1, username);
            p2.setString(2, fullName);
            p2.setString(3, shift);
            p2.executeUpdate();

            JOptionPane.showMessageDialog(this,
                    "CHẤM CÔNG THÀNH CÔNG\nNhân viên: " + fullName + "\nThời gian: " + new java.util.Date()
                            + "\nCa làm: " + shift,
                    "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);

            loadAttendanceLogs();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Lỗi khi chấm công: " + ex.getMessage(), "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private JButton createCustomButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(button.getPreferredSize().width + 10, 32));
        return button;
    }

    // 1. TAB KHÁCH HÀNG
    private JPanel createCustomerPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        form.setBorder(BorderFactory.createTitledBorder("Thêm Khách Hàng Mới"));

        JTextField txtId = new JTextField(8);
        JTextField txtName = new JTextField(15);
        JTextField txtPhone = new JTextField(10);
        JButton btnAdd = createCustomButton("Thêm Khách Hàng", new Color(46, 204, 113));

        form.add(new JLabel("Mã KH:"));
        form.add(txtId);
        form.add(new JLabel("Họ tên:"));
        form.add(txtName);
        form.add(new JLabel("SĐT:"));
        form.add(txtPhone);
        form.add(btnAdd);

        customerModel = new DefaultTableModel(new String[] { "Mã KH", "Họ & Tên", "Số điện thoại" }, 0);
        JTable table = new JTable(customerModel);

        btnAdd.addActionListener(e -> {
            String id = txtId.getText().trim();
            String name = txtName.getText().trim();
            String phone = txtPhone.getText().trim();

            if (id.isEmpty() || name.isEmpty() || phone.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin!");
                return;
            }

            try (Connection conn = DatabaseConfig.getConnection();
                    PreparedStatement pstmt = conn
                            .prepareStatement("INSERT INTO customers (id, name, phone) VALUES (?, ?, ?)")) {
                pstmt.setString(1, id);
                pstmt.setString(2, name);
                pstmt.setString(3, phone);
                pstmt.executeUpdate();

                JOptionPane.showMessageDialog(this, "Đã thêm khách hàng thành công!");
                txtId.setText("");
                txtName.setText("");
                txtPhone.setText("");
                loadCustomers();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Lỗi thêm khách hàng: " + ex.getMessage());
            }
        });

        panel.add(form, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private void loadCustomers() {
        customerModel.setRowCount(0);
        try (Connection conn = DatabaseConfig.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM customers")) {
            while (rs.next()) {
                customerModel.addRow(new Object[] { rs.getString("id"), rs.getString("name"), rs.getString("phone") });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 2. TAB XE & TRẠNG THÁI
    private JPanel createVehiclePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        form.setBorder(BorderFactory.createTitledBorder("Tiếp Nhận Xe Về Gara"));

        JTextField txtPlate = new JTextField(8);
        JTextField txtBrand = new JTextField(10);
        JTextField txtModel = new JTextField(10);
        JTextField txtOwnerId = new JTextField(8);
        JButton btnAddVeh = createCustomButton("Tiếp Nhận Xe", new Color(26, 188, 156));

        form.add(new JLabel("Biển số:"));
        form.add(txtPlate);
        form.add(new JLabel("Hãng xe:"));
        form.add(txtBrand);
        form.add(new JLabel("Model:"));
        form.add(txtModel);
        form.add(new JLabel("Mã KH:"));
        form.add(txtOwnerId);
        form.add(btnAddVeh);

        vehicleModel = new DefaultTableModel(new String[] { "Biển số", "Hãng xe", "Model", "Mã Chủ Xe", "Trạng thái" },
                0);
        JTable table = new JTable(vehicleModel);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JComboBox<RepairStatus> cbStatus = new JComboBox<>(RepairStatus.values());
        JButton btnUpdateStatus = createCustomButton("Cập nhật Trạng thái", new Color(230, 126, 34));

        statusPanel.add(new JLabel("Trạng thái mới:"));
        statusPanel.add(cbStatus);
        statusPanel.add(btnUpdateStatus);

        btnAddVeh.addActionListener(e -> {
            String plate = txtPlate.getText().trim().toUpperCase();
            String brand = txtBrand.getText().trim();
            String model = txtModel.getText().trim();
            String ownerId = txtOwnerId.getText().trim();

            try (Connection conn = DatabaseConfig.getConnection();
                    PreparedStatement pstmt = conn.prepareStatement(
                            "INSERT INTO vehicles (license_plate, brand, model, owner_id, status) VALUES (?, ?, ?, ?, 'WAITING')")) {
                pstmt.setString(1, plate);
                pstmt.setString(2, brand);
                pstmt.setString(3, model);
                pstmt.setString(4, ownerId);
                pstmt.executeUpdate();

                JOptionPane.showMessageDialog(this, "Đã tiếp nhận xe thành công!");
                txtPlate.setText("");
                txtBrand.setText("");
                txtModel.setText("");
                txtOwnerId.setText("");
                loadVehicles();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Lỗi tiếp nhận xe: " + ex.getMessage());
            }
        });

        btnUpdateStatus.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn xe trong bảng!");
                return;
            }
            String plate = (String) vehicleModel.getValueAt(row, 0);
            RepairStatus st = (RepairStatus) cbStatus.getSelectedItem();

            try (Connection conn = DatabaseConfig.getConnection();
                    PreparedStatement pstmt = conn
                            .prepareStatement("UPDATE vehicles SET status = ? WHERE license_plate = ?")) {
                pstmt.setString(1, st.name());
                pstmt.setString(2, plate);
                pstmt.executeUpdate();

                JOptionPane.showMessageDialog(this, "Đã đổi trạng thái xe!");
                loadVehicles();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        panel.add(form, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(statusPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void loadVehicles() {
        vehicleModel.setRowCount(0);
        try (Connection conn = DatabaseConfig.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM vehicles")) {
            while (rs.next()) {
                RepairStatus st = RepairStatus.valueOf(rs.getString("status"));
                vehicleModel.addRow(new Object[] {
                        rs.getString("license_plate"), rs.getString("brand"),
                        rs.getString("model"), rs.getString("owner_id"), st.getDescription()
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 3. TAB PHỤ TÙNG KHO
    private JPanel createPartPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        form.setBorder(BorderFactory.createTitledBorder("Nhập Phụ Tùng Vào Kho"));

        JTextField txtId = new JTextField(6);
        JTextField txtName = new JTextField(12);
        JTextField txtImport = new JTextField(8);
        JTextField txtExport = new JTextField(8);
        JTextField txtQty = new JTextField(5);
        JButton btnAddPart = createCustomButton("Nhập Kho", new Color(241, 196, 15));

        form.add(new JLabel("Mã PT:"));
        form.add(txtId);
        form.add(new JLabel("Tên phụ tùng:"));
        form.add(txtName);
        form.add(new JLabel("Giá nhập:"));
        form.add(txtImport);
        form.add(new JLabel("Giá bán:"));
        form.add(txtExport);
        form.add(new JLabel("Số lượng:"));
        form.add(txtQty);
        form.add(btnAddPart);

        partModel = new DefaultTableModel(new String[] { "Mã PT", "Tên Phụ Tùng", "Giá Nhập", "Giá Bán", "Tồn Kho" },
                0);
        JTable table = new JTable(partModel);

        btnAddPart.addActionListener(e -> {
            try (Connection conn = DatabaseConfig.getConnection();
                    PreparedStatement pstmt = conn.prepareStatement(
                            "INSERT INTO parts (id, name, import_price, export_price, stock_quantity) VALUES (?, ?, ?, ?, ?)")) {
                pstmt.setString(1, txtId.getText().trim());
                pstmt.setString(2, txtName.getText().trim());
                pstmt.setDouble(3, Double.parseDouble(txtImport.getText().trim()));
                pstmt.setDouble(4, Double.parseDouble(txtExport.getText().trim()));
                pstmt.setInt(5, Integer.parseInt(txtQty.getText().trim()));
                pstmt.executeUpdate();

                JOptionPane.showMessageDialog(this, "Đã nhập phụ tùng vào kho!");
                loadParts();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi nhập kho: " + ex.getMessage());
            }
        });

        panel.add(form, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private void loadParts() {
        partModel.setRowCount(0);
        try (Connection conn = DatabaseConfig.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM parts")) {
            while (rs.next()) {
                partModel.addRow(new Object[] {
                        rs.getString("id"), rs.getString("name"),
                        String.format("%,.0f VNĐ", rs.getDouble("import_price")),
                        String.format("%,.0f VNĐ", rs.getDouble("export_price")),
                        rs.getInt("stock_quantity")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 4. TAB HÓA ĐƠN
    private JPanel createBillingPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField txtSearchPlate = new JTextField(10);
        JTextField txtAmount = new JTextField(10);
        JButton btnInvoice = createCustomButton("Tạo & In Hóa Đơn", new Color(52, 152, 219));
        JButton btnLoadHistory = createCustomButton("Xem Lịch Sử Hóa Đơn", new Color(142, 68, 173));

        top.add(new JLabel("Biển số xe:"));
        top.add(txtSearchPlate);
        top.add(new JLabel("Tổng tiền (VNĐ):"));
        top.add(txtAmount);
        top.add(btnInvoice);
        top.add(btnLoadHistory);

        invoiceHistoryModel = new DefaultTableModel(
                new String[] { "Mã Hóa Đơn", "Biển Số Xe", "Người Tạo", "Tổng Tiền", "Thời Gian Tạo" }, 0);
        JTable historyTable = new JTable(invoiceHistoryModel);

        btnInvoice.addActionListener(e -> {
            String plate = txtSearchPlate.getText().trim().toUpperCase();
            String amtStr = txtAmount.getText().trim();

            if (plate.isEmpty() || amtStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập biển số xe và tổng tiền!");
                return;
            }

            try (Connection conn = DatabaseConfig.getConnection();
                    PreparedStatement pstmt = conn.prepareStatement(
                            "INSERT INTO invoices (id, license_plate, created_by, total_amount) VALUES (?, ?, ?, ?)")) {
                String invId = "INV-" + System.currentTimeMillis() % 10000;
                double total = Double.parseDouble(amtStr);

                pstmt.setString(1, invId);
                pstmt.setString(2, plate);
                pstmt.setString(3, AuthService.getCurrentUser().getUsername());
                pstmt.setDouble(4, total);
                pstmt.executeUpdate();

                PreparedStatement p2 = conn
                        .prepareStatement("UPDATE vehicles SET status = 'COMPLETED' WHERE license_plate = ?");
                p2.setString(1, plate);
                p2.executeUpdate();

                JOptionPane.showMessageDialog(this, "Xuất hóa đơn " + invId + " thành công!");
                loadVehicles();
                loadInvoiceHistory();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi xuất hóa đơn: " + ex.getMessage());
            }
        });

        btnLoadHistory.addActionListener(e -> loadInvoiceHistory());

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(historyTable), BorderLayout.CENTER);

        loadInvoiceHistory();
        return panel;
    }

    private void loadInvoiceHistory() {
        if (invoiceHistoryModel == null)
            return;
        invoiceHistoryModel.setRowCount(0);
        try (Connection conn = DatabaseConfig.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM invoices ORDER BY created_at DESC")) {
            while (rs.next()) {
                invoiceHistoryModel.addRow(new Object[] {
                        rs.getString("id"),
                        rs.getString("license_plate"),
                        rs.getString("created_by"),
                        String.format("%,.0f VNĐ", rs.getDouble("total_amount")),
                        rs.getString("created_at")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 5. TAB NHÂN SỰ
    private JPanel createEmployeePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topForm = new JPanel(new GridLayout(2, 1, 5, 5));

        JPanel formAdd = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        formAdd.setBorder(BorderFactory.createTitledBorder("1. Thêm Nhân Viên Mới"));
        JTextField txtId = new JTextField(6);
        JTextField txtName = new JTextField(12);
        JTextField txtPhone = new JTextField(8);
        JComboBox<String> cbRole = new JComboBox<>(new String[] { "Lễ Tân", "Kỹ Thuật Viên" });
        JComboBox<String> cbShift = new JComboBox<>(new String[] { "Ca 1 (06:00 - 14:00)", "Ca 2 (14:00 - 22:00)" });
        JButton btnAddEmp = createCustomButton("Thêm Nhân Viên", new Color(155, 89, 182));

        formAdd.add(new JLabel("Mã NV:"));
        formAdd.add(txtId);
        formAdd.add(new JLabel("Tên:"));
        formAdd.add(txtName);
        formAdd.add(new JLabel("SĐT:"));
        formAdd.add(txtPhone);
        formAdd.add(new JLabel("Chức danh:"));
        formAdd.add(cbRole);
        formAdd.add(new JLabel("Ca làm:"));
        formAdd.add(cbShift);
        formAdd.add(btnAddEmp);

        JPanel formCheckinBackup = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        formCheckinBackup.setBorder(
                BorderFactory.createTitledBorder("2. Tích Chấm Công Hộ (Trường hợp hệ thống nhân viên bị lỗi)"));
        JTextField txtCheckinEmpId = new JTextField(8);
        JButton btnCheckinBackup = createCustomButton("Tích Chấm Công Hộ", new Color(230, 126, 34));

        formCheckinBackup.add(new JLabel("Nhập Mã NV / Tên NV cần chấm công:"));
        formCheckinBackup.add(txtCheckinEmpId);
        formCheckinBackup.add(btnCheckinBackup);

        topForm.add(formAdd);
        topForm.add(formCheckinBackup);

        employeeModel = new DefaultTableModel(new String[] { "Mã NV", "Họ & Tên", "SĐT", "Chức danh", "Ca Phân Công" },
                0);
        JTable empTable = new JTable(employeeModel);

        attendanceModel = new DefaultTableModel(
                new String[] { "STT", "Mã NV", "Họ & Tên", "Thời Gian Chấm Công", "Ca Làm" }, 0);
        JTable attTable = new JTable(attendanceModel);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(empTable), new JScrollPane(attTable));
        splitPane.setResizeWeight(0.5);

        btnAddEmp.addActionListener(e -> {
            try (Connection conn = DatabaseConfig.getConnection();
                    PreparedStatement pstmt = conn.prepareStatement(
                            "INSERT INTO employees (id, name, phone, role, shift) VALUES (?, ?, ?, ?, ?)")) {
                pstmt.setString(1, txtId.getText().trim());
                pstmt.setString(2, txtName.getText().trim());
                pstmt.setString(3, txtPhone.getText().trim());
                pstmt.setString(4, (String) cbRole.getSelectedItem());
                pstmt.setString(5, (String) cbShift.getSelectedItem());
                pstmt.executeUpdate();

                JOptionPane.showMessageDialog(this, "Thêm nhân viên thành công!");
                loadEmployees();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi thêm nhân viên: " + ex.getMessage());
            }
        });

        btnCheckinBackup.addActionListener(e -> {
            String empId = txtCheckinEmpId.getText().trim();
            if (empId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập mã hoặc tên nhân viên!");
                return;
            }

            try (Connection conn = DatabaseConfig.getConnection()) {
                PreparedStatement p1 = conn
                        .prepareStatement("SELECT name, shift FROM employees WHERE id = ? OR name LIKE ?");
                p1.setString(1, empId);
                p1.setString(2, "%" + empId + "%");
                ResultSet rs = p1.executeQuery();

                if (rs.next()) {
                    String empName = rs.getString("name");
                    String shift = rs.getString("shift");

                    PreparedStatement p2 = conn.prepareStatement(
                            "INSERT INTO attendance_logs (employee_id, employee_name, shift) VALUES (?, ?, ?)");
                    p2.setString(1, empId);
                    p2.setString(2, empName);
                    p2.setString(3, shift);
                    p2.executeUpdate();

                    JOptionPane.showMessageDialog(this,
                            "Chủ Garage đã tích chấm công hộ thành công cho nhân viên [" + empName + "]!");
                    txtCheckinEmpId.setText("");
                    loadAttendanceLogs();
                } else {
                    JOptionPane.showMessageDialog(this, "Không tìm thấy thông tin nhân viên này trong danh sách!",
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        panel.add(topForm, BorderLayout.NORTH);
        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    private void loadEmployees() {
        if (employeeModel == null)
            return;
        employeeModel.setRowCount(0);
        try (Connection conn = DatabaseConfig.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM employees")) {
            while (rs.next()) {
                employeeModel.addRow(new Object[] {
                        rs.getString("id"), rs.getString("name"), rs.getString("phone"),
                        rs.getString("role"), rs.getString("shift")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadAttendanceLogs() {
        if (attendanceModel == null)
            return;
        attendanceModel.setRowCount(0);
        try (Connection conn = DatabaseConfig.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM attendance_logs ORDER BY check_in_time DESC")) {
            while (rs.next()) {
                attendanceModel.addRow(new Object[] {
                        rs.getInt("id"),
                        rs.getString("employee_id"),
                        rs.getString("employee_name"),
                        rs.getString("check_in_time"),
                        rs.getString("shift")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 6. TAB BÁO CÁO
    private JPanel createReportPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextArea txtReport = new JTextArea();
        txtReport.setFont(new Font("Monospaced", Font.BOLD, 14));
        JButton btnRefresh = createCustomButton("Cập Nhật Báo Cáo Doanh Thu", new Color(142, 68, 173));

        btnRefresh.addActionListener(e -> {
            try (Connection conn = DatabaseConfig.getConnection();
                    Statement stmt = conn.createStatement()) {

                ResultSet r1 = stmt.executeQuery("SELECT COUNT(*) FROM vehicles");
                r1.next();
                int totalVeh = r1.getInt(1);

                ResultSet r2 = stmt.executeQuery("SELECT COUNT(*) FROM vehicles WHERE status = 'COMPLETED'");
                r2.next();
                int doneVeh = r2.getInt(1);

                ResultSet r3 = stmt.executeQuery("SELECT SUM(total_amount) FROM invoices");
                r3.next();
                double totalRev = r3.getDouble(1);

                StringBuilder sb = new StringBuilder();
                sb.append("==================================================\n");
                sb.append("          BÁO CÁO THỐNG KÊ DOANH THU GARA         \n");
                sb.append("==================================================\n\n");
                sb.append("- Tổng số xe đã tiếp nhận      : ").append(totalVeh).append(" xe\n");
                sb.append("- Số lượng xe đã sửa xong      : ").append(doneVeh).append(" xe\n");
                sb.append("- Xe đang chờ/đang sửa        : ").append(totalVeh - doneVeh).append(" xe\n\n");
                sb.append("--------------------------------------------------\n");
                sb.append(String.format("TỔNG DOANH THU TOÀN HỆ THỐNG    : %,.0f VNĐ\n", totalRev));
                sb.append("==================================================\n");

                txtReport.setText(sb.toString());
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        panel.add(btnRefresh, BorderLayout.NORTH);
        panel.add(new JScrollPane(txtReport), BorderLayout.CENTER);
        return panel;
    }
}