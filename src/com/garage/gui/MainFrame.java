package com.garage.gui;

import com.garage.config.DatabaseConfig;
import com.garage.enums.RepairStatus;
import com.garage.enums.Role;
import com.garage.services.AuthService;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class MainFrame extends JFrame {
    private DefaultTableModel customerModel, vehicleModel, partModel, partUsageModel, employeeModel, attendanceModel,
            invoiceHistoryModel, reportModel;
    private JLabel lblTotalRevenue;

    public MainFrame() {
        Role currentRole = AuthService.getCurrentUser().getRole();
        String currentUsername = AuthService.getCurrentUser().getUsername();
        String currentFullName = AuthService.getCurrentUser().getFullName();

        setTitle("HỆ THỐNG QUẢN LÝ GARA Ô TÔ - [" + currentFullName + " - " + currentRole.getDescription() + "]");
        setSize(1150, 750);
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

        int totalTabs = tabbedPane.getTabCount();
        for (int i = 0; i < totalTabs; i++) {
            JLabel tabLabel = new JLabel(tabbedPane.getTitleAt(i), SwingConstants.CENTER);
            tabLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
            tabLabel.setPreferredSize(new Dimension(1100 / totalTabs, 32));
            tabbedPane.setTabComponentAt(i, tabLabel);
        }

        // HEADER TOP BAR

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        topPanel.setBackground(new Color(236, 240, 241));

        JLabel lblUser = new JLabel("Xin chào: " + currentFullName + " (" + currentRole.getDescription() + ")");
        lblUser.setFont(new Font("SansSerif", Font.BOLD, 14));

        JPanel rightActionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightActionPanel.setOpaque(false);

            JButton btnCheckinSelf = createCustomButton("TÍCH ĐI LÀM NGAY", new Color(46, 204, 113));
            btnCheckinSelf.addActionListener(e -> handleSelfCheckin(currentUsername, currentFullName));
            rightActionPanel.add(btnCheckinSelf);

        JButton btnAccount = createCustomButton("Tài Khoản", new Color(52, 152, 219));
        btnAccount.addActionListener(e -> new AccountManagementDialog(this).setVisible(true));

        JButton btnLogout = createCustomButton("Đăng Xuất", new Color(231, 76, 60));
        btnLogout.addActionListener(e -> {
            AuthService.logout();
            this.dispose();
            new LoginFrame().setVisible(true);
        });

        rightActionPanel.add(btnAccount);
        rightActionPanel.add(btnLogout);

        topPanel.add(lblUser, BorderLayout.WEST);
        topPanel.add(rightActionPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);

        loadCustomers();
        loadVehicles();
        loadParts();
        loadPartUsageLogs();
        if (currentRole == Role.ADMIN) {
            loadEmployees();
            loadAttendanceLogs();
            loadReportData();
        }
    }

    private String generateCustomerId() {
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id FROM customers ORDER BY CAST(SUBSTRING(id, 3) AS UNSIGNED) DESC LIMIT 1")) {
            if (rs.next()) {
                int lastNum = Integer.parseInt(rs.getString("id").replaceAll("[^0-9]", ""));
                return String.format("KH%02d", lastNum + 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "KH01";
    }

    private String generatePartId() {
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id FROM parts ORDER BY CAST(SUBSTRING(id, 3) AS UNSIGNED) DESC LIMIT 1")) {
            if (rs.next()) {
                int lastNum = Integer.parseInt(rs.getString("id").replaceAll("[^0-9]", ""));
                return String.format("PT%02d", lastNum + 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "PT01";
    }

    private String generateEmployeeId() {
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id FROM employees WHERE id LIKE 'NV%' ORDER BY CAST(SUBSTRING(id, 3) AS UNSIGNED) DESC LIMIT 1")) {
            if (rs.next()) {
                int lastNum = Integer.parseInt(rs.getString("id").replaceAll("[^0-9]", ""));
                return String.format("NV%02d", lastNum + 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "NV01";
    }

    private void handleSelfCheckin(String username, String fullName) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            String empId = null;
            String shift = "Ca 1 (06:00 - 14:00)";
            PreparedStatement p1 = conn.prepareStatement("SELECT id, shift FROM employees WHERE id = ? OR name LIKE ?");
            p1.setString(1, username);
            p1.setString(2, "%" + fullName + "%");
            ResultSet rs = p1.executeQuery();
            if (rs.next()) {
                empId = rs.getString("id");
                shift = rs.getString("shift");
            } 
            
            else if ("admin".equalsIgnoreCase(username) || AuthService.getCurrentUser().getRole() == Role.ADMIN) {
            empId = "ADMIN";
            shift = "Toàn thời gian";
            } else {
                JOptionPane.showMessageDialog(this,
                        "Không tìm thấy thông tin nhân viên [" + fullName + "] trong danh sách nhân sự!\nVui lòng nhờ Chủ Garage thêm thông tin nhân viên trước.",
                        "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            PreparedStatement p2 = conn.prepareStatement(
                    "INSERT INTO attendance_logs (employee_id, employee_name, shift) VALUES (?, ?, ?)");
            p2.setString(1, empId);
            p2.setString(2, fullName);
            p2.setString(3, shift);
            p2.executeUpdate();

            JOptionPane.showMessageDialog(this,
                    "CHẤM CÔNG THÀNH CÔNG\nNhân viên: " + fullName + "\nThời gian: " + new java.util.Date()
                            + "\nCa làm: " + shift,
                    "Thông báo", JOptionPane.INFORMATION_MESSAGE);

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

        JTextField txtName = new JTextField(15);
        JTextField txtPhone = new JTextField(10);
        JButton btnAdd = createCustomButton("Thêm Khách Hàng", new Color(46, 204, 113));

        form.add(new JLabel("Họ tên:"));
        form.add(txtName);
        form.add(new JLabel("SĐT:"));
        form.add(txtPhone);
        form.add(btnAdd);

        customerModel = new DefaultTableModel(new String[] { "Mã KH", "Họ & Tên", "Số điện thoại" }, 0);
        JTable table = new JTable(customerModel);

        btnAdd.addActionListener(e -> {
            String name = txtName.getText().trim();
            String phone = txtPhone.getText().trim();

            if (name.isEmpty() || phone.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin!");
                return;
            }

            String autoId = generateCustomerId();

            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("INSERT INTO customers (id, name, phone) VALUES (?, ?, ?)")) {
                pstmt.setString(1, autoId);
                pstmt.setString(2, name);
                pstmt.setString(3, phone.isEmpty() ? "---" : phone);
                pstmt.executeUpdate();

                JOptionPane.showMessageDialog(this, "Đã thêm khách hàng thành công với Mã: " + autoId);
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
                String phone = rs.getString("phone");
                customerModel.addRow(new Object[] { rs.getString("id"), rs.getString("name"), (phone == null || phone.isEmpty()) ? "---" : phone });
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

        vehicleModel = new DefaultTableModel(new String[] { "Biển số", "Hãng xe", "Model", "Mã Chủ Xe", "Trạng thái" }, 0);
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

            if (plate.isEmpty() || brand.isEmpty() || ownerId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ biển số, hãng xe và mã chủ xe!");
                return;
            }

            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "INSERT INTO vehicles (license_plate, brand, model, owner_id, status) VALUES (?, ?, ?, ?, 'WAITING')")) {
                pstmt.setString(1, plate);
                pstmt.setString(2, brand);
                pstmt.setString(3, model.isEmpty() ? "---" : model);
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
                 PreparedStatement pstmt = conn.prepareStatement("UPDATE vehicles SET status = ? WHERE license_plate = ?")) {
                pstmt.setString(1, st.name());
                pstmt.setString(2, plate);
                pstmt.executeUpdate();

                JOptionPane.showMessageDialog(this, "Đã đổi trạng thái xe!");
                loadVehicles();
                loadReportData();
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
                String model = rs.getString("model");
                vehicleModel.addRow(new Object[] {
                        rs.getString("license_plate"), rs.getString("brand"),
                        (model == null || model.isEmpty()) ? "---" : model, rs.getString("owner_id"), st.getDescription()
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 3. TAB QUẢN LÝ KHO PHỤ TÙNG

    private JPanel createPartPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        form.setBorder(BorderFactory.createTitledBorder("Nhập Phụ Tùng Vào Kho"));

        JTextField txtName = new JTextField(12);
        JTextField txtImport = new JTextField(8);
        JTextField txtExport = new JTextField(8);
        JTextField txtQty = new JTextField(5);
        JButton btnAddPart = createCustomButton("Nhập Kho", new Color(241, 196, 15));

        form.add(new JLabel("Tên phụ tùng:"));
        form.add(txtName);
        form.add(new JLabel("Giá nhập:"));
        form.add(txtImport);
        form.add(new JLabel("Giá bán:"));
        form.add(txtExport);
        form.add(new JLabel("Số lượng:"));
        form.add(txtQty);
        form.add(btnAddPart);

        partModel = new DefaultTableModel(new String[] { "Mã PT", "Tên Phụ Tùng", "Giá Nhập", "Giá Bán", "Tồn Kho" }, 0);
        JTable partTable = new JTable(partModel);

        partUsageModel = new DefaultTableModel(
                new String[] { "STT", "Mã PT", "Tên PT", "Loại Giao Dịch", "Số Lượng", "Ghi Chú / Xe", "Người Thực Hiện", "Thời Gian" }, 0);
        JTable partUsageTable = new JTable(partUsageModel);

        partUsageTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        partUsageTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        partUsageTable.getColumnModel().getColumn(1).setPreferredWidth(60);
        partUsageTable.getColumnModel().getColumn(2).setPreferredWidth(180);
        partUsageTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        partUsageTable.getColumnModel().getColumn(4).setPreferredWidth(70);
        partUsageTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        partUsageTable.getColumnModel().getColumn(6).setPreferredWidth(120);
        partUsageTable.getColumnModel().getColumn(7).setPreferredWidth(140);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(partTable), new JScrollPane(partUsageTable));
        splitPane.setResizeWeight(0.5);

        btnAddPart.addActionListener(e -> {
            String name = txtName.getText().trim();
            String importPriceStr = txtImport.getText().trim();
            String exportPriceStr = txtExport.getText().trim();
            String qtyStr = txtQty.getText().trim();

            if (name.isEmpty() || importPriceStr.isEmpty() || exportPriceStr.isEmpty() || qtyStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin phụ tùng!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try (Connection conn = DatabaseConfig.getConnection()) {
                double importPrice = Double.parseDouble(importPriceStr);
                double exportPrice = Double.parseDouble(exportPriceStr);
                int inputQty = Integer.parseInt(qtyStr);
                String currentUsername = AuthService.getCurrentUser().getUsername();

                PreparedStatement checkStmt = conn.prepareStatement(
                        "SELECT id, stock_quantity FROM parts WHERE LOWER(name) = LOWER(?) AND import_price = ?"
                );
                checkStmt.setString(1, name);
                checkStmt.setDouble(2, importPrice);
                ResultSet rs = checkStmt.executeQuery();

                String finalPartId;

                if (rs.next()) {
                    finalPartId = rs.getString("id");

                    PreparedStatement updateStmt = conn.prepareStatement(
                            "UPDATE parts SET stock_quantity = stock_quantity + ?, export_price = ? WHERE id = ?"
                    );
                    updateStmt.setInt(1, inputQty);
                    updateStmt.setDouble(2, exportPrice);
                    updateStmt.setString(3, finalPartId);
                    updateStmt.executeUpdate();

                    JOptionPane.showMessageDialog(this,
                            "Phụ tùng đã tồn tại trong kho (Mã: " + finalPartId + ").\nĐã tự động cộng thêm " + inputQty + " vào số lượng tồn kho!",
                            "Thành công", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    finalPartId = generatePartId();

                    PreparedStatement insertStmt = conn.prepareStatement(
                            "INSERT INTO parts (id, name, import_price, export_price, stock_quantity) VALUES (?, ?, ?, ?, ?)"
                    );
                    insertStmt.setString(1, finalPartId);
                    insertStmt.setString(2, name);
                    insertStmt.setDouble(3, importPrice);
                    insertStmt.setDouble(4, exportPrice);
                    insertStmt.setInt(5, inputQty);
                    insertStmt.executeUpdate();

                    JOptionPane.showMessageDialog(this,
                            "Đã nhập phụ tùng mới thành công với Mã: " + finalPartId,
                            "Thành công", JOptionPane.INFORMATION_MESSAGE);
                }

                PreparedStatement logStmt = conn.prepareStatement(
                        "INSERT INTO part_usage_logs (part_id, part_name, quantity, used_for_vehicle, created_by) VALUES (?, ?, ?, ?, ?)"
                );
                logStmt.setString(1, finalPartId);
                logStmt.setString(2, name);
                logStmt.setInt(3, inputQty);
                logStmt.setString(4, "NHẬP KHO");
                logStmt.setString(5, currentUsername);
                logStmt.executeUpdate();

                txtName.setText("");
                txtImport.setText("");
                txtExport.setText("");
                txtQty.setText("");
                loadParts();
                loadPartUsageLogs();

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Giá nhập, giá bán và số lượng phải là số hợp lệ!", "Lỗi nhập liệu", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi nhập kho: " + ex.getMessage(), "Lỗi CSDL", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });

        panel.add(form, BorderLayout.NORTH);
        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    private void loadParts() {
        if (partModel == null) return;
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

    private void loadPartUsageLogs() {
        if (partUsageModel == null) return;
        partUsageModel.setRowCount(0);
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT p.id, p.part_id, p.part_name, p.quantity, p.used_for_vehicle, COALESCE(u.full_name, p.created_by) AS creator_name, p.created_at " +
                     "FROM part_usage_logs p LEFT JOIN users u ON p.created_by = u.username " +
                     "ORDER BY p.created_at DESC")) {
            while (rs.next()) {
                String usedFor = rs.getString("used_for_vehicle");
                boolean isImport = "NHẬP KHO".equalsIgnoreCase(usedFor);

                String transactionType = isImport ? "NHẬP KHO" : "XUẤT KHO";
                String note = isImport ? "---" : usedFor;

                partUsageModel.addRow(new Object[] {
                        rs.getInt("id"),
                        rs.getString("part_id"),
                        rs.getString("part_name"),
                        transactionType,
                        rs.getInt("quantity"),
                        note,
                        rs.getString("creator_name"),
                        rs.getString("created_at")
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

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        top.setBorder(BorderFactory.createTitledBorder("Tạo Hóa Đơn Dịch Vụ / Thay Phụ Tùng"));

        JComboBox<String> cbLicensePlate = new JComboBox<>();
        
        JComboBox<String> cbServiceType = new JComboBox<>(new String[]{
                "Rửa xe & Dọn nội thất (30.000đ)",
                "Thay dầu & Lọc dầu (150.000đ)",
                "Bảo dưỡng định kỳ (250.000đ)",
                "Sửa chữa động cơ (1.500.000đ)",
                "Sửa chữa hệ thống phanh (300.000đ)",
                "Thay thế phụ tùng kho"
        });

        JComboBox<String> cbPartSelect = new JComboBox<>();
        JTextField txtPartQty = new JTextField("1", 4);
        JTextField txtAmount = new JTextField(10);

        txtAmount.setEditable(false);
        txtAmount.setText("30000");

        cbPartSelect.setEnabled(false);
        txtPartQty.setEnabled(false);

        JButton btnInvoice = createCustomButton("Tạo & In Hóa Đơn", new Color(52, 152, 219));
        JButton btnLoadHistory = createCustomButton("Xem Lịch Sử Hóa Đơn", new Color(142, 68, 173));

        top.add(new JLabel("Biển số xe:"));
        top.add(cbLicensePlate);
        top.add(new JLabel("Dịch vụ:"));
        top.add(cbServiceType);
        top.add(new JLabel("Mã PT Kho:"));
        top.add(cbPartSelect);
        top.add(new JLabel("SL:"));
        top.add(txtPartQty);
        top.add(new JLabel("Tổng tiền (VNĐ):"));
        top.add(txtAmount);
        top.add(btnInvoice);
        top.add(btnLoadHistory);

        Runnable reloadPlateCombo = () -> {
            cbLicensePlate.removeAllItems();
            try (Connection conn = DatabaseConfig.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT license_plate, brand FROM vehicles")) {
                while (rs.next()) {
                    cbLicensePlate.addItem(rs.getString("license_plate") + " (" + rs.getString("brand") + ")");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        };
        reloadPlateCombo.run();

        Runnable reloadPartCombo = () -> {
            cbPartSelect.removeAllItems();
            try (Connection conn = DatabaseConfig.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, name, export_price, stock_quantity FROM parts")) {
                while (rs.next()) {
                    cbPartSelect.addItem(rs.getString("id") + " - " + rs.getString("name") + " (" + String.format("%,.0f", rs.getDouble("export_price")) + "đ/Kho:" + rs.getInt("stock_quantity") + ")");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        };
        reloadPartCombo.run();

        Runnable autoCalculatePartPrice = () -> {
            if ("Thay thế phụ tùng kho".equals(cbServiceType.getSelectedItem())) {
                String selectedPart = (String) cbPartSelect.getSelectedItem();
                if (selectedPart != null && !selectedPart.isEmpty()) {
                    String partId = selectedPart.split(" - ")[0];
                    try (Connection conn = DatabaseConfig.getConnection();
                         PreparedStatement stmt = conn.prepareStatement("SELECT export_price FROM parts WHERE id = ?")) {
                        stmt.setString(1, partId);
                        ResultSet rs = stmt.executeQuery();
                        if (rs.next()) {
                            double price = rs.getDouble("export_price");
                            int qty = Integer.parseInt(txtPartQty.getText().trim().replaceAll("[^0-9]", "1"));
                            txtAmount.setText(String.format("%.0f", price * qty));
                        }
                    } catch (Exception ex) {
                        txtAmount.setText("0");
                    }
                }
            }
        };

        cbServiceType.addActionListener(e -> {
            String selected = (String) cbServiceType.getSelectedItem();
            if (selected == null) return;

            boolean isPart = "Thay thế phụ tùng kho".equals(selected);
            cbPartSelect.setEnabled(isPart);
            txtPartQty.setEnabled(isPart);

            if (isPart) {
                reloadPartCombo.run();
                autoCalculatePartPrice.run();
            } else {
                if (selected.contains("Rửa xe")) txtAmount.setText("30000");
                else if (selected.contains("Thay dầu")) txtAmount.setText("150000");
                else if (selected.contains("Bảo dưỡng")) txtAmount.setText("250000");
                else if (selected.contains("Sửa chữa động cơ")) txtAmount.setText("1500000");
                else if (selected.contains("Sửa chữa hệ thống phanh")) txtAmount.setText("300000");
            }
        });

        cbPartSelect.addActionListener(e -> autoCalculatePartPrice.run());
        txtPartQty.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                autoCalculatePartPrice.run();
            }
        });

        invoiceHistoryModel = new DefaultTableModel(
                new String[] { "Mã Hóa Đơn", "Biển Số Xe", "Người Tạo", "Tổng Tiền", "Thời Gian Tạo" }, 0);
        JTable historyTable = new JTable(invoiceHistoryModel);

        btnInvoice.addActionListener(e -> {
            String selectedPlateItem = (String) cbLicensePlate.getSelectedItem();
            if (selectedPlateItem == null || selectedPlateItem.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Không có xe nào trong Gara! Vui lòng tiếp nhận xe trước.", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String plate = selectedPlateItem.split(" ")[0];
            String serviceType = (String) cbServiceType.getSelectedItem();
            boolean isPartService = "Thay thế phụ tùng kho".equals(serviceType);

            try (Connection conn = DatabaseConfig.getConnection()) {
                String invId = "INV-" + System.currentTimeMillis() % 10000;
                double totalAmount = Double.parseDouble(txtAmount.getText().trim());
                String currentUsername = AuthService.getCurrentUser().getUsername();

                if (isPartService) {
                    String selectedPart = (String) cbPartSelect.getSelectedItem();
                    if (selectedPart == null) {
                        JOptionPane.showMessageDialog(this, "Chưa chọn phụ tùng!");
                        return;
                    }

                    String partId = selectedPart.split(" - ")[0];
                    int qty = Integer.parseInt(txtPartQty.getText().trim());

                    PreparedStatement checkPartStmt = conn.prepareStatement("SELECT name, stock_quantity FROM parts WHERE id = ?");
                    checkPartStmt.setString(1, partId);
                    ResultSet rsPart = checkPartStmt.executeQuery();

                    if (rsPart.next()) {
                        String partName = rsPart.getString("name");
                        int currentStock = rsPart.getInt("stock_quantity");

                        if (currentStock < qty) {
                            JOptionPane.showMessageDialog(this, "Số lượng trong kho không đủ! Hiện tại còn: " + currentStock, "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                            return;
                        }

                        PreparedStatement updateStockStmt = conn.prepareStatement("UPDATE parts SET stock_quantity = stock_quantity - ? WHERE id = ?");
                        updateStockStmt.setInt(1, qty);
                        updateStockStmt.setString(2, partId);
                        updateStockStmt.executeUpdate();

                        PreparedStatement logPartStmt = conn.prepareStatement(
                                "INSERT INTO part_usage_logs (part_id, part_name, quantity, used_for_vehicle, created_by) VALUES (?, ?, ?, ?, ?)"
                        );
                        logPartStmt.setString(1, partId);
                        logPartStmt.setString(2, partName);
                        logPartStmt.setInt(3, qty);
                        logPartStmt.setString(4, plate);
                        logPartStmt.setString(5, currentUsername);
                        logPartStmt.executeUpdate();
                    }
                }

                PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT INTO invoices (id, license_plate, created_by, total_amount) VALUES (?, ?, ?, ?)");
                pstmt.setString(1, invId);
                pstmt.setString(2, plate);
                pstmt.setString(3, currentUsername);
                pstmt.setDouble(4, totalAmount);
                pstmt.executeUpdate();

                PreparedStatement p2 = conn.prepareStatement("UPDATE vehicles SET status = 'COMPLETED' WHERE license_plate = ?");
                p2.setString(1, plate);
                p2.executeUpdate();

                JOptionPane.showMessageDialog(this, "Xuất hóa đơn " + invId + " cho xe " + plate + " thành công!");

                loadVehicles();
                loadParts();
                loadPartUsageLogs();
                loadInvoiceHistory();
                reloadPartCombo.run();
                reloadPlateCombo.run();
                loadReportData();

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi xuất hóa đơn: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });

        btnLoadHistory.addActionListener(e -> {
            loadInvoiceHistory();
            reloadPlateCombo.run();
        });

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(historyTable), BorderLayout.CENTER);

        loadInvoiceHistory();
        return panel;
    }

    private void loadInvoiceHistory() {
        if (invoiceHistoryModel == null) return;
        invoiceHistoryModel.setRowCount(0);
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT i.id, i.license_plate, COALESCE(u.full_name, i.created_by) AS creator_name, i.total_amount, i.created_at " +
                     "FROM invoices i LEFT JOIN users u ON i.created_by = u.username " +
                     "ORDER BY i.created_at DESC")) {
            while (rs.next()) {
                invoiceHistoryModel.addRow(new Object[] {
                        rs.getString("id"),
                        rs.getString("license_plate"),
                        rs.getString("creator_name"),
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
        JTextField txtName = new JTextField(10);
        JTextField txtPhone = new JTextField(8);
        JTextField txtCccd = new JTextField(10);
        JComboBox<String> cbRole = new JComboBox<>(new String[] { "Lễ Tân", "Kỹ Thuật Viên" });
        JComboBox<String> cbShift = new JComboBox<>(new String[] { "Ca 1 (06:00 - 14:00)", "Ca 2 (14:00 - 22:00)" });
        JButton btnAddEmp = createCustomButton("Thêm Nhân Viên", new Color(155, 89, 182));

        formAdd.add(new JLabel("Tên:"));
        formAdd.add(txtName);
        formAdd.add(new JLabel("SĐT:"));
        formAdd.add(txtPhone);
        formAdd.add(new JLabel("CCCD:"));
        formAdd.add(txtCccd);
        formAdd.add(new JLabel("Chức danh:"));
        formAdd.add(cbRole);
        formAdd.add(new JLabel("Ca làm:"));
        formAdd.add(cbShift);
        formAdd.add(btnAddEmp);

        JPanel formAction = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        formAction.setBorder(BorderFactory.createTitledBorder("2. Quản Lý & Chấm Công Hộ"));
        JTextField txtCheckinEmpId = new JTextField(8);
        JButton btnCheckinBackup = createCustomButton("Tích Chấm Công Hộ", new Color(230, 126, 34));
        JButton btnResetPassword = createCustomButton("Reset Mật Khẩu", new Color(231, 76, 60));

        formAction.add(new JLabel("Nhập Mã NV / Tên NV:"));
        formAction.add(txtCheckinEmpId);
        formAction.add(btnCheckinBackup);
        formAction.add(btnResetPassword);

        topForm.add(formAdd);
        topForm.add(formAction);

        employeeModel = new DefaultTableModel(new String[] { "Mã NV", "Họ & Tên", "SĐT", "CCCD", "Chức danh", "Ca Phân Công" }, 0);
        JTable empTable = new JTable(employeeModel);

        attendanceModel = new DefaultTableModel(
                new String[] { "STT", "Mã NV", "Họ & Tên", "Thời Gian Chấm Công", "Ca Làm" }, 0);
        JTable attTable = new JTable(attendanceModel);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(empTable), new JScrollPane(attTable));
        splitPane.setResizeWeight(0.5);

        btnAddEmp.addActionListener(e -> {
            String autoId = generateEmployeeId();
            String name = txtName.getText().trim();
            String phone = txtPhone.getText().trim();
            String cccd = txtCccd.getText().trim();

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Tên nhân viên không được để trống!");
                return;
            }

            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "INSERT INTO employees (id, name, phone, cccd, role, shift) VALUES (?, ?, ?, ?, ?, ?)")) {
                pstmt.setString(1, autoId);
                pstmt.setString(2, name);
                pstmt.setString(3, phone.isEmpty() ? "---" : phone);
                pstmt.setString(4, cccd.isEmpty() ? "---" : cccd);
                pstmt.setString(5, (String) cbRole.getSelectedItem());
                pstmt.setString(6, (String) cbShift.getSelectedItem());
                pstmt.executeUpdate();

                JOptionPane.showMessageDialog(this, "Thêm nhân viên thành công với Mã: " + autoId);
                txtName.setText("");
                txtPhone.setText("");
                txtCccd.setText("");
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
                PreparedStatement p1 = conn.prepareStatement("SELECT id, name, shift FROM employees WHERE id = ? OR name LIKE ?");
                p1.setString(1, empId);
                p1.setString(2, "%" + empId + "%");
                ResultSet rs = p1.executeQuery();

                if (rs.next()) {
                    String foundId = rs.getString("id");
                    String empName = rs.getString("name");
                    String shift = rs.getString("shift");

                    PreparedStatement p2 = conn.prepareStatement(
                            "INSERT INTO attendance_logs (employee_id, employee_name, shift) VALUES (?, ?, ?)");
                    p2.setString(1, foundId);
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

        btnResetPassword.addActionListener(e -> {
            int selectedRow = empTable.getSelectedRow();
            String empId = txtCheckinEmpId.getText().trim();
            String empName = "";

            if (selectedRow != -1) {
                empId = (String) employeeModel.getValueAt(selectedRow, 0);
                empName = (String) employeeModel.getValueAt(selectedRow, 1);
            }

            if (empId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 nhân viên trong bảng hoặc nhập mã/tên nhân viên để reset mật khẩu!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc chắn muốn reset mật khẩu tài khoản nhân viên [" + empId + "] về 123456?", "Xác nhận Reset", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;

            try (Connection conn = DatabaseConfig.getConnection()) {

                PreparedStatement p1 = conn.prepareStatement("UPDATE users SET password = '123456' WHERE username = ? OR full_name LIKE ?");
                p1.setString(1, empId);
                p1.setString(2, "%" + empName + "%");
                int updated = p1.executeUpdate();

                if (updated > 0) {
                    JOptionPane.showMessageDialog(this, "Đã reset mật khẩu tài khoản [" + empId + "] về '123456' thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Không tìm thấy tài khoản đăng nhập tương ứng với nhân viên này!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi reset mật khẩu: " + ex.getMessage());
            }
        });

        panel.add(topForm, BorderLayout.NORTH);
        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    private void loadEmployees() {
        if (employeeModel == null) return;
        employeeModel.setRowCount(0);
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {

            ResultSet r1 = stmt.executeQuery("SELECT full_name FROM users WHERE role = 'ADMIN' LIMIT 1");
            if (r1.next()) {
                employeeModel.addRow(new Object[] {
                        "ADMIN", r1.getString("full_name"), "---", "---", "Chủ Garage", "Toàn thời gian"
                });
            }

            ResultSet rs = stmt.executeQuery("SELECT * FROM employees ORDER BY id ASC");
            while (rs.next()) {
                String phone = rs.getString("phone");
                String cccd = rs.getString("cccd");
                employeeModel.addRow(new Object[] {
                        rs.getString("id"),
                        rs.getString("name"),
                        (phone == null || phone.isEmpty()) ? "---" : phone,
                        (cccd == null || cccd.isEmpty()) ? "---" : cccd,
                        rs.getString("role"),
                        rs.getString("shift")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadAttendanceLogs() {
        if (attendanceModel == null) return;
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

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnRefresh = createCustomButton("Cập Nhật Báo Cáo Doanh Thu", new Color(142, 68, 173));
        topBar.add(btnRefresh);

        reportModel = new DefaultTableModel(new String[] { "Hạng Mục Thống Kê", "Số Lượng / Giá Trị", "Ghi Chú" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable reportTable = new JTable(reportModel);
        reportTable.setRowHeight(32);
        reportTable.setFont(new Font("SansSerif", Font.PLAIN, 13));
        reportTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        reportTable.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBackground(new Color(236, 240, 241));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));

        lblTotalRevenue = new JLabel("TỔNG DOANH THU TOÀN HỆ THỐNG: 0 VNĐ");
        lblTotalRevenue.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblTotalRevenue.setForeground(new Color(192, 57, 43));
        bottomPanel.add(lblTotalRevenue);

        btnRefresh.addActionListener(e -> loadReportData());

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(new JScrollPane(reportTable), BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void loadReportData() {
        if (reportModel == null) return;
        reportModel.setRowCount(0);

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {

            ResultSet r1 = stmt.executeQuery("SELECT COUNT(*) FROM vehicles");
            r1.next();
            int totalVeh = r1.getInt(1);

            ResultSet r2 = stmt.executeQuery("SELECT COUNT(*) FROM vehicles WHERE status = 'COMPLETED'");
            r2.next();
            int doneVeh = r2.getInt(1);

            int inProgressVeh = totalVeh - doneVeh;

            ResultSet r3 = stmt.executeQuery("SELECT SUM(stock_quantity) FROM parts");
            r3.next();
            int totalParts = r3.getInt(1);

            ResultSet r4 = stmt.executeQuery("SELECT SUM(total_amount) FROM invoices");
            r4.next();
            double totalRev = r4.getDouble(1);

            reportModel.addRow(new Object[] { "Tổng số xe tiếp nhận", totalVeh + " xe", "Toàn bộ xe đã đưa vào gara" });
            reportModel.addRow(new Object[] { "Số xe đã sửa xong", doneVeh + " xe", "Đã hoàn thành & đủ điều kiện xuất HĐ" });
            reportModel.addRow(new Object[] { "Số xe đang sửa / chờ xử lý", inProgressVeh + " xe", "Cần tiếp tục theo dõi tiến độ" });
            reportModel.addRow(new Object[] { "Tổng số phụ tùng tồn kho", totalParts + " món", "Số lượng linh kiện sẵn có trong kho" });
            reportModel.addRow(new Object[] { "Tổng số hóa đơn đã xuất", getInvoiceCount(conn) + " hóa đơn", "Tất cả giao dịch đã thanh toán" });

            lblTotalRevenue.setText(String.format("TỔNG DOANH THU TOÀN HỆ THỐNG: %,.0f VNĐ", totalRev));

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private int getInvoiceCount(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM invoices")) {
            rs.next();
            return rs.getInt(1);
        }
    }
}