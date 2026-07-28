package com.garage.gui;

import com.garage.enums.RepairStatus;
import com.garage.enums.Role;
import com.garage.models.Customer;
import com.garage.models.Employee;
import com.garage.models.Invoice;
import com.garage.models.Part;
import com.garage.models.Vehicle;
import com.garage.repository.EmployeeRepository;
import com.garage.services.AuthService;
import com.garage.services.BillingManager;
import com.garage.services.CustomerManager;
import com.garage.services.VehicleManager;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MainFrame extends JFrame {
    private final CustomerManager customerManager = new CustomerManager();
    private final VehicleManager vehicleManager = new VehicleManager();
    private final BillingManager billingManager = new BillingManager();
    private final EmployeeRepository employeeRepository = new EmployeeRepository();

    private DefaultTableModel customerModel, vehicleModel, partModel, partUsageModel, employeeModel, attendanceModel, invoiceHistoryModel, reportModel;
    private JLabel lblTotalRevenue;

    public MainFrame() {
        Role currentRole = AuthService.getCurrentUser().getRole();
        String currentUsername = AuthService.getCurrentUser().getUsername();
        String currentFullName = AuthService.getCurrentUser().getFullName();

        setTitle("HỆ THỐNG QUẢN LÝ GARA Ô TÔ - [" + currentFullName + " - " + currentRole.getDescription() + "]");
        setSize(1200, 780);
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
        } else {
            tabbedPane.addTab("Chấm Công Nhóm", createReceptionistAttendancePanel());
        }

        int totalTabs = tabbedPane.getTabCount();
        for (int i = 0; i < totalTabs; i++) {
            JLabel tabLabel = new JLabel(tabbedPane.getTitleAt(i), SwingConstants.CENTER);
            tabLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
            tabLabel.setPreferredSize(new Dimension(1150 / totalTabs, 32));
            tabbedPane.setTabComponentAt(i, tabLabel);
        }

        // =========================================================
        // 1. THANH TIÊU ĐỀ DÙNG CHUNG (HEADER TOP BAR)
        // =========================================================
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        topPanel.setBackground(new Color(236, 240, 241));

        JLabel lblUser = new JLabel("Xin chào: " + currentFullName + " (" + currentRole.getDescription() + ")");
        lblUser.setFont(new Font("SansSerif", Font.BOLD, 14));

        JPanel rightActionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightActionPanel.setOpaque(false);

        JButton btnCheckinSelf = createCustomButton("TÍCH ĐI LÀM NGAY", new Color(46, 204, 113));
        btnCheckinSelf.addActionListener(e -> {
            try {
                if (employeeRepository.addAttendance(currentUsername, currentFullName)) {
                    LocalDateTime now = LocalDateTime.now();
                    String dateStr = now.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                    String timeStr = now.format(DateTimeFormatter.ofPattern("HH:mm"));
                    
                    JOptionPane.showMessageDialog(this, 
                            "CHẤM CÔNG THÀNH CÔNG!\n" +
                            "Nhân viên: " + currentFullName + "\n" +
                            "Ngày: " + dateStr + "\n" +
                            "Giờ: " + timeStr);
                    loadAttendanceLogs();
                } else {
                    JOptionPane.showMessageDialog(this, "Không tìm thấy nhân viên!");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
            }
        });

        JButton btnAccount = createCustomButton("Tài Khoản", new Color(52, 152, 219));
        btnAccount.addActionListener(e -> new AccountManagementDialog(this).setVisible(true));

        JButton btnLogout = createCustomButton("Đăng Xuất", new Color(231, 76, 60));
        btnLogout.addActionListener(e -> {
            AuthService.logout();
            this.dispose();
            new LoginFrame().setVisible(true);
        });

        rightActionPanel.add(btnCheckinSelf);
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

    private JButton createCustomButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(button.getPreferredSize().width + 10, 32));
        return button;
    }

    // =========================================================
    // 2. TAB QUẢN LÝ KHÁCH HÀNG
    // =========================================================
    private JPanel createCustomerPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        topBar.setBorder(BorderFactory.createTitledBorder("Thao Tác Khách Hàng"));

        JTextField txtSearch = new JTextField(12);
        JButton btnSearch = createCustomButton("Tìm Kiếm", new Color(52, 152, 219));
        JButton btnReload = createCustomButton("Làm Mới", new Color(142, 68, 173));

        JTextField txtName = new JTextField(12);
        JTextField txtPhone = new JTextField(10);
        JButton btnAdd = createCustomButton("Thêm Khách Hàng", new Color(46, 204, 113));
        JButton btnUpdate = createCustomButton("Cập Nhật KH", new Color(230, 126, 34));

        topBar.add(new JLabel("Tìm KH:")); topBar.add(txtSearch); topBar.add(btnSearch); topBar.add(btnReload);
        topBar.add(new JLabel(" | Họ tên:")); topBar.add(txtName);
        topBar.add(new JLabel("SĐT:")); topBar.add(txtPhone);
        topBar.add(btnAdd); topBar.add(btnUpdate);

        customerModel = new DefaultTableModel(new String[] { "Mã KH", "Họ & Tên", "Số điện thoại" }, 0);
        JTable table = new JTable(customerModel);

        table.getSelectionModel().addListSelectionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow != -1) {
                txtName.setText((String) customerModel.getValueAt(selectedRow, 1));
                txtPhone.setText((String) customerModel.getValueAt(selectedRow, 2));
            }
        });

        btnSearch.addActionListener(e -> {
            String kw = txtSearch.getText().trim();
            if (kw.isEmpty()) { loadCustomers(); return; }
            customerModel.setRowCount(0);
            for (Customer c : customerManager.searchCustomers(kw)) {
                customerModel.addRow(new Object[]{ c.getId(), c.getName(), c.getPhone() });
            }
        });

        btnReload.addActionListener(e -> {
            txtSearch.setText(""); txtName.setText(""); txtPhone.setText("");
            loadCustomers();
        });

        btnAdd.addActionListener(e -> {
            String name = txtName.getText().trim();
            String phone = txtPhone.getText().trim();
            if (name.isEmpty()) { JOptionPane.showMessageDialog(this, "Họ tên không được rỗng!"); return; }
            try {
                customerManager.addCustomer(name, phone);
                JOptionPane.showMessageDialog(this, "Thêm khách hàng thành công!");
                txtName.setText(""); txtPhone.setText("");
                loadCustomers();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
            }
        });

        btnUpdate.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) { JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 khách hàng!"); return; }
            String id = (String) customerModel.getValueAt(selectedRow, 0);
            try {
                customerManager.updateCustomer(id, txtName.getText().trim(), txtPhone.getText().trim());
                JOptionPane.showMessageDialog(this, "Cập nhật thành công!");
                loadCustomers();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
            }
        });

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private void loadCustomers() {
        customerModel.setRowCount(0);
        for (Customer c : customerManager.getAllCustomers()) {
            customerModel.addRow(new Object[]{ c.getId(), c.getName(), c.getPhone() });
        }
    }

    // =========================================================
    // 3. TAB QUẢN LÝ XE & TRẠNG THÁI TIẾP NHẬN
    // =========================================================
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

        form.add(new JLabel("Biển số:")); form.add(txtPlate);
        form.add(new JLabel("Hãng xe:")); form.add(txtBrand);
        form.add(new JLabel("Model:")); form.add(txtModel);
        form.add(new JLabel("Mã KH:")); form.add(txtOwnerId);
        form.add(btnAddVeh);

        vehicleModel = new DefaultTableModel(new String[] { "Biển số", "Hãng xe", "Model", "Mã Chủ Xe", "Tên Chủ Xe", "Trạng thái" }, 0);
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
            String ownerId = txtOwnerId.getText().trim();
            if (plate.isEmpty() || brand.isEmpty() || ownerId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ biển số, hãng xe và mã chủ xe!");
                return;
            }
            try {
                vehicleManager.addVehicle(plate, brand, txtModel.getText().trim(), ownerId);
                JOptionPane.showMessageDialog(this, "Đã tiếp nhận xe!");
                txtPlate.setText(""); txtBrand.setText(""); txtModel.setText(""); txtOwnerId.setText("");
                loadVehicles();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
            }
        });

        btnUpdateStatus.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Vui lòng chọn xe!"); return; }
            String plate = (String) vehicleModel.getValueAt(row, 0);
            RepairStatus st = (RepairStatus) cbStatus.getSelectedItem();
            try {
                vehicleManager.updateVehicleStatus(plate, st);
                JOptionPane.showMessageDialog(this, "Đã cập nhật trạng thái!");
                loadVehicles();
                loadReportData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
            }
        });

        panel.add(form, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(statusPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void loadVehicles() {
        vehicleModel.setRowCount(0);
        for (Vehicle v : vehicleManager.getAllVehicles()) {
            vehicleModel.addRow(new Object[]{ v.getLicensePlate(), v.getBrand(), v.getModel(), v.getOwnerId(), v.getOwnerName(), v.getStatus().getDescription() });
        }
    }

    // =========================================================
    // 4. TAB QUẢN LÝ KHO PHỤ TÙNG
    // =========================================================
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

        form.add(new JLabel("Tên phụ tùng:")); form.add(txtName);
        form.add(new JLabel("Giá nhập:")); form.add(txtImport);
        form.add(new JLabel("Giá bán:")); form.add(txtExport);
        form.add(new JLabel("Số lượng:")); form.add(txtQty);
        form.add(btnAddPart);

        partModel = new DefaultTableModel(new String[] { "Mã PT", "Tên Phụ Tùng", "Giá Nhập", "Giá Bán", "Tồn Kho" }, 0);
        JTable partTable = new JTable(partModel);

        partUsageModel = new DefaultTableModel(new String[] { "STT", "Mã PT", "Tên PT", "Loại Giao Dịch", "Số Lượng", "Ghi Chú / Xe", "Người Thực Hiện", "Thời Gian" }, 0);
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

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(partTable), new JScrollPane(partUsageTable));
        splitPane.setResizeWeight(0.5);

        btnAddPart.addActionListener(e -> {
            try {
                String name = txtName.getText().trim();
                double importPrice = Double.parseDouble(txtImport.getText().trim());
                double exportPrice = Double.parseDouble(txtExport.getText().trim());
                int qty = Integer.parseInt(txtQty.getText().trim());
                billingManager.importPart(name, importPrice, exportPrice, qty, AuthService.getCurrentUser().getUsername());
                JOptionPane.showMessageDialog(this, "Đã nhập kho phụ tùng!");
                txtName.setText(""); txtImport.setText(""); txtExport.setText(""); txtQty.setText("");
                loadParts();
                loadPartUsageLogs();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi nhập kho: " + ex.getMessage());
            }
        });

        panel.add(form, BorderLayout.NORTH);
        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    private void loadParts() {
        if (partModel == null) return;
        partModel.setRowCount(0);
        for (Part p : billingManager.getAllParts()) {
            partModel.addRow(new Object[]{
                    p.getId(), p.getName(),
                    String.format("%,.0f VNĐ", p.getImportPrice()),
                    String.format("%,.0f VNĐ", p.getExportPrice()),
                    p.getStockQuantity()
            });
        }
    }

    private void loadPartUsageLogs() {
        if (partUsageModel == null) return;
        partUsageModel.setRowCount(0);
        for (Object[] row : billingManager.getPartUsageLogs()) {
            partUsageModel.addRow(row);
        }
    }

    // =========================================================
    // 5. TAB GÁN DỊCH VỤ & XUẤT HÓA ĐƠN
    // =========================================================
    private JPanel createBillingPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topForm = new JPanel(new GridLayout(2, 1, 5, 5));

        JPanel line1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        line1.setBorder(BorderFactory.createTitledBorder("1. Tạo Hóa Đơn Mới"));

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
        JTextField txtPartQty = new JTextField("1", 3);
        JTextField txtNotes = new JTextField(12);
        JTextField txtAmount = new JTextField(8);

        txtAmount.setEditable(false);
        txtAmount.setText("30000");

        cbPartSelect.setEnabled(false);
        txtPartQty.setEnabled(false);

        JButton btnInvoice = createCustomButton("Tạo & In Hóa Đơn", new Color(52, 152, 219));

        line1.add(new JLabel("Biển số xe:")); line1.add(cbLicensePlate);
        line1.add(new JLabel("Dịch vụ:")); line1.add(cbServiceType);
        line1.add(new JLabel("Phụ tùng Kho:")); line1.add(cbPartSelect);
        line1.add(new JLabel("SL:")); line1.add(txtPartQty);
        line1.add(new JLabel("Ghi chú:")); line1.add(txtNotes);
        line1.add(new JLabel("Tổng tiền:")); line1.add(txtAmount);
        line1.add(btnInvoice);

        JPanel line2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        line2.setBorder(BorderFactory.createTitledBorder("2. Tìm Kiếm Lịch Sử Hóa Đơn"));

        JTextField txtSearchInvoice = new JTextField(15);
        JButton btnSearchInv = createCustomButton("Tìm Hoá đơn (Biển số/ Tên KH)", new Color(142, 68, 173));
        JButton btnReloadInv = createCustomButton("Tải Lại", new Color(46, 204, 113));

        line2.add(new JLabel("Từ khóa tìm kiếm:"));
        line2.add(txtSearchInvoice);
        line2.add(btnSearchInv);
        line2.add(btnReloadInv);

        topForm.add(line1);
        topForm.add(line2);

        invoiceHistoryModel = new DefaultTableModel(new String[] { "Mã HĐ", "Biển Số Xe", "Tên Khách Hàng", "Loại Dịch Vụ", "Thông Tin Phụ Tùng", "Ghi Chú", "Người Tạo", "Tổng Tiền", "Thời Gian" }, 0);
        JTable historyTable = new JTable(invoiceHistoryModel);

        Runnable reloadPlateCombo = () -> {
            cbLicensePlate.removeAllItems();
            for (Vehicle v : vehicleManager.getAllVehicles()) {
                cbLicensePlate.addItem(v.getLicensePlate() + " (" + v.getOwnerName() + ")");
            }
        };
        reloadPlateCombo.run();

        Runnable reloadPartCombo = () -> {
            cbPartSelect.removeAllItems();
            for (Part p : billingManager.getAllParts()) {
                cbPartSelect.addItem(p.getId() + " - " + p.getName() + " (" + String.format("%,.0f", p.getExportPrice()) + "đ/Kho:" + p.getStockQuantity() + ")");
            }
        };
        reloadPartCombo.run();

        Runnable autoCalculatePartPrice = () -> {
            if ("Thay thế phụ tùng kho".equals(cbServiceType.getSelectedItem())) {
                String selectedPart = (String) cbPartSelect.getSelectedItem();
                if (selectedPart != null && !selectedPart.isEmpty()) {
                    String partId = selectedPart.split(" - ")[0];
                    Part p = billingManager.getAllParts().stream().filter(item -> item.getId().equals(partId)).findFirst().orElse(null);
                    if (p != null) {
                        int qty = Integer.parseInt(txtPartQty.getText().trim().replaceAll("[^0-9]", "1"));
                        txtAmount.setText(String.format("%.0f", p.getExportPrice() * qty));
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
            public void keyReleased(java.awt.event.KeyEvent evt) { autoCalculatePartPrice.run(); }
        });

        btnInvoice.addActionListener(e -> {
            String selectedPlateItem = (String) cbLicensePlate.getSelectedItem();
            if (selectedPlateItem == null || selectedPlateItem.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Chưa chọn xe!"); return;
            }
            String plate = selectedPlateItem.split(" ")[0];
            String serviceType = (String) cbServiceType.getSelectedItem();
            String selectedPartRaw = (String) cbPartSelect.getSelectedItem();
            int partQty = Integer.parseInt(txtPartQty.getText().trim().replaceAll("[^0-9]", "1"));
            double totalAmount = Double.parseDouble(txtAmount.getText().trim());

            try {
                billingManager.createInvoice(plate, serviceType, selectedPartRaw, partQty, txtNotes.getText().trim(), totalAmount, AuthService.getCurrentUser().getUsername());
                JOptionPane.showMessageDialog(this, "Tạo hóa đơn thành công!");
                txtNotes.setText("");
                loadVehicles();
                loadParts();
                loadPartUsageLogs();
                loadInvoiceHistory();
                reloadPartCombo.run();
                reloadPlateCombo.run();
                loadReportData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
            }
        });

        btnSearchInv.addActionListener(e -> {
            String kw = txtSearchInvoice.getText().trim();
            if (kw.isEmpty()) { loadInvoiceHistory(); return; }
            invoiceHistoryModel.setRowCount(0);
            for (Invoice inv : billingManager.searchInvoices(kw)) {
                invoiceHistoryModel.addRow(new Object[]{ inv.getId(), inv.getLicensePlate(), inv.getCustomerName(), inv.getServiceName(), inv.getPartInfo(), inv.getNotes(), inv.getCreatedBy(), String.format("%,.0f VNĐ", inv.getTotalAmount()), inv.getCreatedAt() });
            }
        });

        btnReloadInv.addActionListener(e -> {
            txtSearchInvoice.setText("");
            loadInvoiceHistory();
            reloadPlateCombo.run();
        });

        panel.add(topForm, BorderLayout.NORTH);
        panel.add(new JScrollPane(historyTable), BorderLayout.CENTER);

        loadInvoiceHistory();
        return panel;
    }

    private void loadInvoiceHistory() {
        if (invoiceHistoryModel == null) return;
        invoiceHistoryModel.setRowCount(0);
        for (Invoice inv : billingManager.getAllInvoices()) {
            invoiceHistoryModel.addRow(new Object[]{ inv.getId(), inv.getLicensePlate(), inv.getCustomerName(), inv.getServiceName(), inv.getPartInfo(), inv.getNotes(), inv.getCreatedBy(), String.format("%,.0f VNĐ", inv.getTotalAmount()), inv.getCreatedAt() });
        }
    }

    // =========================================================
    // 6. TAB QUẢN LÝ NHÂN SỰ & CHẤM CÔNG (QUẢN TRỊ VIÊN)
    // =========================================================
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

        formAdd.add(new JLabel("Tên:")); formAdd.add(txtName);
        formAdd.add(new JLabel("SĐT:")); formAdd.add(txtPhone);
        formAdd.add(new JLabel("CCCD:")); formAdd.add(txtCccd);
        formAdd.add(new JLabel("Chức danh:")); formAdd.add(cbRole);
        formAdd.add(new JLabel("Ca làm:")); formAdd.add(cbShift);
        formAdd.add(btnAddEmp);

        JPanel formAction = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        formAction.setBorder(BorderFactory.createTitledBorder("2. Quản Lý & Chấm Công Hộ"));
        JTextField txtCheckinEmpId = new JTextField(8);
        JButton btnCheckinBackup = createCustomButton("Tích Chấm Công Hộ", new Color(230, 126, 34));
        JButton btnResetPassword = createCustomButton("Reset Mật Khẩu (123456)", new Color(231, 76, 60));

        formAction.add(new JLabel("Nhập Mã NV / Tên NV:"));
        formAction.add(txtCheckinEmpId);
        formAction.add(btnCheckinBackup);
        formAction.add(btnResetPassword);

        topForm.add(formAdd);
        topForm.add(formAction);

        employeeModel = new DefaultTableModel(new String[] { "Mã NV", "Họ & Tên", "SĐT", "CCCD", "Chức Danh", "Ca Phân Công", "Số Ca Làm", "Tổng Lương Tích Lũy" }, 0);
        JTable empTable = new JTable(employeeModel);

        attendanceModel = new DefaultTableModel(new String[] { "STT", "Mã NV", "Họ & Tên", "Thời Gian Chấm Công", "Ca Làm" }, 0);
        JTable attTable = new JTable(attendanceModel);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(empTable), new JScrollPane(attTable));
        splitPane.setResizeWeight(0.55);

        btnAddEmp.addActionListener(e -> {
            String name = txtName.getText().trim();
            if (name.isEmpty()) { JOptionPane.showMessageDialog(this, "Tên không được rỗng!"); return; }
            try {
                String newId = employeeRepository.generateNextId();
                Employee emp = new Employee(newId, name, txtPhone.getText().trim(), txtCccd.getText().trim(), (String) cbRole.getSelectedItem(), (String) cbShift.getSelectedItem(), 0, 0);
                employeeRepository.save(emp);
                JOptionPane.showMessageDialog(this, "Thêm nhân viên thành công!");
                txtName.setText(""); txtPhone.setText(""); txtCccd.setText("");
                loadEmployees();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
            }
        });

        btnCheckinBackup.addActionListener(e -> {
            String input = txtCheckinEmpId.getText().trim();
            if (input.isEmpty()) { JOptionPane.showMessageDialog(this, "Vui lòng nhập mã/tên!"); return; }
            try {
                if (employeeRepository.addAttendance(input, AuthService.getCurrentUser().getFullName())) {
                    LocalDateTime now = LocalDateTime.now();
                    String dateStr = now.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                    String timeStr = now.format(DateTimeFormatter.ofPattern("HH:mm"));
                    
                    JOptionPane.showMessageDialog(this, 
                            "TÍCH CHẤM CÔNG HỘ THÀNH CÔNG!\n" +
                            "Người thực hiện: " + AuthService.getCurrentUser().getFullName() + "\n" +
                            "Ngày: " + dateStr + "\n" +
                            "Giờ: " + timeStr);
                    txtCheckinEmpId.setText("");
                    loadAttendanceLogs();
                    loadEmployees();
                } else {
                    JOptionPane.showMessageDialog(this, "Không tìm thấy nhân viên!");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
            }
        });

        btnResetPassword.addActionListener(e -> {
            int selectedRow = empTable.getSelectedRow();
            String input = txtCheckinEmpId.getText().trim();
            if (selectedRow != -1) {
                input = (String) employeeModel.getValueAt(selectedRow, 0);
            }
            if (input.isEmpty()) { JOptionPane.showMessageDialog(this, "Vui lòng chọn nhân viên!"); return; }
            try {
                if (employeeRepository.resetPassword(input)) {
                    JOptionPane.showMessageDialog(this, "Đã reset mật khẩu về 123456!");
                } else {
                    JOptionPane.showMessageDialog(this, "Không tìm thấy tài khoản!");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
            }
        });

        panel.add(topForm, BorderLayout.NORTH);
        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    private void loadEmployees() {
        if (employeeModel == null) return;
        employeeModel.setRowCount(0);
        for (Employee emp : employeeRepository.findAll()) {
            employeeModel.addRow(new Object[]{
                    emp.getId(), emp.getName(), emp.getPhone(), emp.getCccd(), emp.getRole(), emp.getShift(),
                    "ADMIN".equals(emp.getId()) ? "---" : emp.getShiftCount() + " ca",
                    "ADMIN".equals(emp.getId()) ? "---" : String.format("%,.0f VNĐ", emp.getTotalSalary())
            });
        }
    }

    private void loadAttendanceLogs() {
        if (attendanceModel == null) return;
        attendanceModel.setRowCount(0);
        for (Object[] row : employeeRepository.findAttendanceLogs()) {
            attendanceModel.addRow(row);
        }
    }

    // =========================================================
    // 7. TAB LỊCH SỬ CHẤM CÔNG NHÓM (DÀNH CHO LỄ TÂN)
    // =========================================================
    private JPanel createReceptionistAttendancePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        topBar.setBorder(BorderFactory.createTitledBorder("Điểm Danh / Chấm Công Hộ Kỹ Thuật Viên Làm Cùng Ca"));

        JTextField txtKtvName = new JTextField(12);
        JButton btnCheckinKtv = createCustomButton("Điểm Danh KTV", new Color(46, 204, 113));

        topBar.add(new JLabel("Mã / Tên KTV cần chấm công:"));
        topBar.add(txtKtvName);
        topBar.add(btnCheckinKtv);

        DefaultTableModel groupKtvModel = new DefaultTableModel(new String[] { "STT", "Mã NV", "Họ & Tên KTV", "Chức Danh", "Ca Phân Công", "Lần Chấm Công Gần Nhất" }, 0);
        JTable ktvTable = new JTable(groupKtvModel);

        DefaultTableModel groupAttHistoryModel = new DefaultTableModel(new String[] { "STT", "Mã KTV", "Họ & Tên KTV", "Thời Gian Chấm Công", "Ca Làm" }, 0);
        JTable historyTable = new JTable(groupAttHistoryModel);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(ktvTable), new JScrollPane(historyTable));
        splitPane.setResizeWeight(0.5);

        Runnable loadGroupData = () -> {
            groupKtvModel.setRowCount(0);
            groupAttHistoryModel.setRowCount(0);
            String myShift = employeeRepository.getEmployeeShift(AuthService.getCurrentUser().getUsername(), AuthService.getCurrentUser().getFullName());
            
            for (Object[] row : employeeRepository.findGroupKtvByShift(myShift)) {
                groupKtvModel.addRow(row);
            }

            for (Object[] row : employeeRepository.findKtvAttendanceHistoryByShift(myShift)) {
                groupAttHistoryModel.addRow(row);
            }
        };

        btnCheckinKtv.addActionListener(e -> {
            String input = txtKtvName.getText().trim();
            if (input.isEmpty()) {
                int row = ktvTable.getSelectedRow();
                if (row != -1) input = (String) groupKtvModel.getValueAt(row, 1);
            }
            if (input == null || input.isEmpty()) { JOptionPane.showMessageDialog(this, "Vui lòng chọn hoặc nhập tên KTV!"); return; }
            try {
                if (employeeRepository.addAttendance(input, AuthService.getCurrentUser().getFullName())) {
                    LocalDateTime now = LocalDateTime.now();
                    String dateStr = now.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                    String timeStr = now.format(DateTimeFormatter.ofPattern("HH:mm"));

                    JOptionPane.showMessageDialog(this, 
                            "ĐIỂM DANH KTV THÀNH CÔNG!\n" +
                            "Người thực hiện: " + AuthService.getCurrentUser().getFullName() + "\n" +
                            "Ngày: " + dateStr + "\n" +
                            "Giờ: " + timeStr);
                    txtKtvName.setText("");
                    loadGroupData.run();
                } else {
                    JOptionPane.showMessageDialog(this, "Không tìm thấy KTV!");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
            }
        });

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(splitPane, BorderLayout.CENTER);

        loadGroupData.run();
        return panel;
    }

    // =========================================================
    // 8. TAB BÁO CÁO DOANH THU & THỐNG KÊ
    // =========================================================
    private JPanel createReportPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnRefresh = createCustomButton("Cập Nhật Báo Cáo Doanh Thu", new Color(142, 68, 173));
        topBar.add(btnRefresh);

        reportModel = new DefaultTableModel(new String[] { "Hạng Mục Thống Kê", "Số Lượng / Giá Trị", "Ghi Chú" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
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

        List<Vehicle> vehicles = vehicleManager.getAllVehicles();
        int totalVeh = vehicles.size();
        int doneVeh = (int) vehicles.stream().filter(v -> v.getStatus() == RepairStatus.COMPLETED).count();
        int inProgressVeh = totalVeh - doneVeh;

        List<Part> parts = billingManager.getAllParts();
        int totalParts = parts.stream().mapToInt(Part::getStockQuantity).sum();

        List<Invoice> invoices = billingManager.getAllInvoices();
        double totalRev = invoices.stream().mapToDouble(Invoice::getTotalAmount).sum();

        reportModel.addRow(new Object[] { "Tổng số xe tiếp nhận", totalVeh + " xe", "Toàn bộ xe đã đưa vào gara" });
        reportModel.addRow(new Object[] { "Số xe đã sửa xong", doneVeh + " xe", "Đã hoàn thành & đủ điều kiện xuất HĐ" });
        reportModel.addRow(new Object[] { "Số xe đang sửa / chờ xử lý", inProgressVeh + " xe", "Cần tiếp tục theo dõi tiến độ" });
        reportModel.addRow(new Object[] { "Tổng số phụ tùng tồn kho", totalParts + " món", "Số lượng linh kiện sẵn có trong kho" });
        reportModel.addRow(new Object[] { "Tổng số hóa đơn đã xuất", invoices.size() + " hóa đơn", "Tất cả giao dịch đã thanh toán" });

        lblTotalRevenue.setText(String.format("TỔNG DOANH THU TOÀN HỆ THỐNG: %,.0f VNĐ", totalRev));
    }
}