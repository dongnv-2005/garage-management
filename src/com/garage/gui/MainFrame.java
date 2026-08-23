package com.garage.gui;

import com.garage.enums.RepairStatus;
import com.garage.enums.Role;
import com.garage.models.*;
import com.garage.repository.EmployeeRepository;
import com.garage.services.AuthService;
import com.garage.services.BillingManager;
import com.garage.services.CustomerManager;
import com.garage.services.VehicleManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MainFrame extends JFrame {
    private final CustomerManager customerManager = new CustomerManager();
    private final VehicleManager vehicleManager = new VehicleManager();
    private final BillingManager billingManager = new BillingManager();
    private final EmployeeRepository employeeRepository = new EmployeeRepository();

    private DefaultTableModel customerModel, vehicleModel, partModel, partUsageModel, employeeModel, attendanceModel, invoiceHistoryModel;
    private BaseReportPanel summaryReportPanel, chartReportPanel;

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

        employeeRepository.autoAssignUnassignedKtvs();

        if (currentRole == Role.ADMIN) {
            tabbedPane.addTab("Quản lý Nhân sự & Chấm công", createEmployeePanel());

            JTabbedPane reportSubTabs = new JTabbedPane();
            reportSubTabs.setFont(new Font("SansSerif", Font.BOLD, 12));
            
            summaryReportPanel = new TableSummaryReportPanel(billingManager, vehicleManager);
            chartReportPanel = new ServiceChartReportPanel(billingManager, vehicleManager);

            reportSubTabs.addTab("Thống Kê Tổng Quan (Bảng)", summaryReportPanel);
            reportSubTabs.addTab("Doanh Thu Theo Dịch Vụ (Biểu Đồ)", chartReportPanel);

            tabbedPane.addTab("Báo cáo Doanh thu & Thống kê", reportSubTabs);
        } else {
            tabbedPane.addTab("Lịch Sử Chấm Công Nhóm", createReceptionistAttendancePanel());

            String recEmpId = employeeRepository.getEmployeeIdByUsernameOrName(currentUsername, currentFullName);
            List<String> unnotifiedKtvs = employeeRepository.getUnnotifiedKtvsForReceptionist(recEmpId);
            if (!unnotifiedKtvs.isEmpty()) {
                SwingUtilities.invokeLater(() -> {
                    for (String ktvName : unnotifiedKtvs) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Kỹ thuật viên: " + ktvName + " đã được thêm vào danh sách chấm công nhóm của bạn!",
                                "Thông Báo Phân Công Kỹ Thuật Viên Mới",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    }
                    employeeRepository.markKtvsAsNotified(recEmpId);
                });
            }
        }

        int totalTabs = tabbedPane.getTabCount();
        for (int i = 0; i < totalTabs; i++) {
            JLabel tabLabel = new JLabel(tabbedPane.getTitleAt(i), SwingConstants.CENTER);
            tabLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
            tabLabel.setPreferredSize(new Dimension(1150 / totalTabs, 32));
            tabbedPane.setTabComponentAt(i, tabLabel);
        }

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

        JScrollPane custScrollPane = new JScrollPane(table);
        MouseAdapter custDeselect = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getSource() == table) {
                    if (table.rowAtPoint(e.getPoint()) == -1) {
                        table.clearSelection();
                        txtName.setText(""); txtPhone.setText("");
                    }
                } else {
                    table.clearSelection();
                    txtName.setText(""); txtPhone.setText("");
                }
            }
        };
        table.addMouseListener(custDeselect);
        custScrollPane.getViewport().addMouseListener(custDeselect);
        custScrollPane.addMouseListener(custDeselect);
        panel.addMouseListener(custDeselect);
        topBar.addMouseListener(custDeselect);

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(custScrollPane, BorderLayout.CENTER);
        return panel;
    }

    private void loadCustomers() {
        customerModel.setRowCount(0);
        for (Customer c : customerManager.getAllCustomers()) {
            customerModel.addRow(new Object[]{ c.getId(), c.getName(), c.getPhone() });
        }
    }

    private JPanel createVehiclePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        topBar.setBorder(BorderFactory.createTitledBorder("Thao Tác Quản Lý Xe"));

        JTextField txtSearchVeh = new JTextField(10);
        JButton btnSearchVeh = createCustomButton("Tìm Kiếm", new Color(52, 152, 219));
        JButton btnReloadVeh = createCustomButton("Làm Mới", new Color(142, 68, 173));

        JTextField txtPlate = new JTextField(8);
        JTextField txtBrand = new JTextField(8);
        JTextField txtModel = new JTextField(8);
        JTextField txtOwnerId = new JTextField(6);
        JButton btnAddVeh = createCustomButton("Tiếp Nhận Xe", new Color(26, 188, 156));

        topBar.add(new JLabel("Tìm Xe:")); topBar.add(txtSearchVeh); topBar.add(btnSearchVeh); topBar.add(btnReloadVeh);
        topBar.add(new JLabel(" | Biển số:")); topBar.add(txtPlate);
        topBar.add(new JLabel("Hãng:")); topBar.add(txtBrand);
        topBar.add(new JLabel("Model:")); topBar.add(txtModel);
        topBar.add(new JLabel("Mã KH:")); topBar.add(txtOwnerId);
        topBar.add(btnAddVeh);

        vehicleModel = new DefaultTableModel(new String[] { "Biển số", "Hãng xe", "Model", "Mã Chủ Xe", "Tên Chủ Xe", "Trạng thái" }, 0);
        JTable table = new JTable(vehicleModel);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JComboBox<RepairStatus> cbStatus = new JComboBox<>(RepairStatus.values());
        JButton btnUpdateStatus = createCustomButton("Cập nhật Trạng thái", new Color(230, 126, 34));

        statusPanel.add(new JLabel("Trạng thái mới:"));
        statusPanel.add(cbStatus);
        statusPanel.add(btnUpdateStatus);

        btnSearchVeh.addActionListener(e -> {
            String kw = txtSearchVeh.getText().trim();
            if (kw.isEmpty()) { loadVehicles(); return; }
            vehicleModel.setRowCount(0);
            for (Vehicle v : vehicleManager.searchVehicles(kw)) {
                vehicleModel.addRow(new Object[]{ v.getLicensePlate(), v.getBrand(), v.getModel(), v.getOwnerId(), v.getOwnerName(), v.getStatus().getDescription() });
            }
        });

        btnReloadVeh.addActionListener(e -> {
            txtSearchVeh.setText("");
            txtPlate.setText(""); txtBrand.setText(""); txtModel.setText(""); txtOwnerId.setText("");
            loadVehicles();
        });

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

        JScrollPane vehScrollPane = new JScrollPane(table);
        MouseAdapter vehDeselect = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getSource() == table) {
                    if (table.rowAtPoint(e.getPoint()) == -1) {
                        table.clearSelection();
                        txtPlate.setText(""); txtBrand.setText(""); txtModel.setText(""); txtOwnerId.setText("");
                    }
                } else {
                    table.clearSelection();
                    txtPlate.setText(""); txtBrand.setText(""); txtModel.setText(""); txtOwnerId.setText("");
                }
            }
        };
        table.addMouseListener(vehDeselect);
        vehScrollPane.getViewport().addMouseListener(vehDeselect);
        vehScrollPane.addMouseListener(vehDeselect);
        panel.addMouseListener(vehDeselect);
        topBar.addMouseListener(vehDeselect);
        statusPanel.addMouseListener(vehDeselect);

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(vehScrollPane, BorderLayout.CENTER);
        panel.add(statusPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void loadVehicles() {
        vehicleModel.setRowCount(0);
        for (Vehicle v : vehicleManager.getAllVehicles()) {
            vehicleModel.addRow(new Object[]{ v.getLicensePlate(), v.getBrand(), v.getModel(), v.getOwnerId(), v.getOwnerName(), v.getStatus().getDescription() });
        }
    }

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
        line2.setBorder(BorderFactory.createTitledBorder("2. Tìm Kiếm & Quản Lý Lịch Sử Hóa Đơn"));

        JTextField txtSearchInvoice = new JTextField(15);
        JButton btnSearchInv = createCustomButton("Tìm Theo Biển Số / Tên Người Tạo", new Color(142, 68, 173));
        JButton btnReloadInv = createCustomButton("Tải Lại Lịch Sử", new Color(46, 204, 113));
        JButton btnDeleteInv = createCustomButton("Xóa Hóa Đơn Chọn", new Color(231, 76, 60));

        line2.add(new JLabel("Từ khóa tìm kiếm:"));
        line2.add(txtSearchInvoice);
        line2.add(btnSearchInv);
        line2.add(btnReloadInv);
        line2.add(btnDeleteInv);

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
                        int qty = 1;
                        try {
                            String qStr = txtPartQty.getText().trim();
                            if (!qStr.isEmpty()) qty = Math.max(1, Integer.parseInt(qStr.replaceAll("[^0-9]", "")));
                        } catch (Exception ignored) {}
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
                if (selected.contains("Thay dầu")) {
                    BaseService service = new OilChangeService("SRV-OIL", 1.0, 50000);
                    txtAmount.setText(String.format("%.0f", service.calculateCost()));
                } else if (selected.contains("Bảo dưỡng")) {
                    BaseService service = new MaintenanceService("SRV-MAINT", 1.0, 50000);
                    txtAmount.setText(String.format("%.0f", service.calculateCost()));
                } else if (selected.contains("Sửa chữa động cơ")) {
                    BaseService service = new EngineRepairService("SRV-ENG", 1000000, 1.0);
                    txtAmount.setText(String.format("%.0f", service.calculateCost()));
                } else if (selected.contains("Sửa chữa hệ thống phanh")) {
                    BaseService service = new MaintenanceService("SRV-BRAKE", 2.0, 50000);
                    txtAmount.setText(String.format("%.0f", service.calculateCost()));
                } else if (selected.contains("Rửa xe")) {
                    txtAmount.setText("30000");
                }
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
            
            int partQty = 1;
            try {
                String qStr = txtPartQty.getText().trim();
                if (!qStr.isEmpty()) partQty = Math.max(1, Integer.parseInt(qStr.replaceAll("[^0-9]", "")));
            } catch (Exception ignored) {}

            double totalAmount = 0;
            try {
                totalAmount = Double.parseDouble(txtAmount.getText().trim());
            } catch (Exception ex) {
                totalAmount = 0;
            }

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

        btnDeleteInv.addActionListener(e -> {
            int selectedRow = historyTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 hóa đơn cần xóa!");
                return;
            }
            String invId = (String) invoiceHistoryModel.getValueAt(selectedRow, 0);
            int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc chắn muốn xóa hóa đơn " + invId + "? (Số lượng phụ tùng sẽ tự động hoàn lại vào kho)", "Xác nhận xóa", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    billingManager.deleteInvoice(invId);
                    JOptionPane.showMessageDialog(this, "Xóa hóa đơn thành công và đã hoàn kho phụ tùng!");
                    loadInvoiceHistory();
                    loadParts();
                    loadPartUsageLogs();
                    loadReportData();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Lỗi xóa hóa đơn: " + ex.getMessage());
                }
            }
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

    private JPanel createEmployeePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topForm = new JPanel(new GridLayout(2, 1, 5, 5));

        JPanel formAdd = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        formAdd.setBorder(BorderFactory.createTitledBorder("1. Thông Tin & Thao Tác Nhân Viên"));
        JTextField txtName = new JTextField(8);
        JTextField txtPhone = new JTextField(8);
        JTextField txtCccd = new JTextField(8);
        JTextField txtNotes = new JTextField(10);
        JComboBox<String> cbRole = new JComboBox<>(new String[] { "Lễ Tân", "Kỹ Thuật Viên" });
        JComboBox<String> cbShift = new JComboBox<>(new String[] { "Ca 1 (06:00 - 14:00)", "Ca 2 (14:00 - 22:00)" });

        JButton btnAddEmp = createCustomButton("Thêm NV", new Color(155, 89, 182));
        JButton btnUpdateEmp = createCustomButton("Cập Nhật NV", new Color(230, 126, 34));
        JButton btnDeleteEmp = createCustomButton("Xóa NV", new Color(231, 76, 60));
        JButton btnClearEmp = createCustomButton("Hủy Chọn / Nhập Mới", new Color(127, 140, 141));

        formAdd.add(new JLabel("Tên:")); formAdd.add(txtName);
        formAdd.add(new JLabel("SĐT:")); formAdd.add(txtPhone);
        formAdd.add(new JLabel("CCCD:")); formAdd.add(txtCccd);
        formAdd.add(new JLabel("Chức danh:")); formAdd.add(cbRole);
        formAdd.add(new JLabel("Ca làm:")); formAdd.add(cbShift);
        formAdd.add(new JLabel("Ghi chú:")); formAdd.add(txtNotes);
        formAdd.add(btnAddEmp);
        formAdd.add(btnUpdateEmp);
        formAdd.add(btnDeleteEmp);
        formAdd.add(btnClearEmp);

        JPanel formAction = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        formAction.setBorder(BorderFactory.createTitledBorder("2. Điểm Danh Hộ & Lịch Sử Chấm Công"));

        JComboBox<String> cbAllEmpSelect = new JComboBox<>();
        JButton btnAdminCheckin = createCustomButton("Điểm Danh Hộ Nhân Viên", new Color(46, 204, 113));
        JButton btnResetPassword = createCustomButton("Reset Mật Khẩu (123456)", new Color(52, 152, 219));
        JButton btnDeleteAttendance = createCustomButton("Xóa Lịch Sử Chấm Công Chọn", new Color(192, 57, 43));

        formAction.add(new JLabel("Chọn Nhân Viên:"));
        formAction.add(cbAllEmpSelect);
        formAction.add(btnAdminCheckin);
        formAction.add(btnResetPassword);
        formAction.add(btnDeleteAttendance);

        topForm.add(formAdd);
        topForm.add(formAction);

        employeeModel = new DefaultTableModel(new String[] { "Mã NV", "Họ & Tên", "SĐT", "CCCD", "Chức Danh", "Ca Phân Công", "Số Ca Làm", "Tổng Lương Tích Lũy", "Ghi Chú" }, 0);
        JTable empTable = new JTable(employeeModel);

        attendanceModel = new DefaultTableModel(new String[] { "Mã Log", "Mã NV", "Họ & Tên", "Thời Gian Chấm Công", "Ca Làm" }, 0);
        JTable attTable = new JTable(attendanceModel);

        JScrollPane empScrollPane = new JScrollPane(empTable);
        JScrollPane attScrollPane = new JScrollPane(attTable);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, empScrollPane, attScrollPane);
        splitPane.setResizeWeight(0.55);

        Runnable clearEmpInputs = () -> {
            empTable.clearSelection();
            txtName.setText("");
            txtPhone.setText("");
            txtCccd.setText("");
            txtNotes.setText("");
            cbRole.setSelectedIndex(0);
            cbShift.setSelectedIndex(0);
        };

        // Click ra ngoài bảng hoặc khu vực trống để hủy chọn nhân viên và xóa form
        MouseAdapter deselectMouseAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getSource() == empTable) {
                    if (empTable.rowAtPoint(e.getPoint()) == -1) {
                        clearEmpInputs.run();
                    }
                } else {
                    clearEmpInputs.run();
                }
            }
        };

        empTable.addMouseListener(deselectMouseAdapter);
        empScrollPane.getViewport().addMouseListener(deselectMouseAdapter);
        empScrollPane.addMouseListener(deselectMouseAdapter);
        panel.addMouseListener(deselectMouseAdapter);
        topForm.addMouseListener(deselectMouseAdapter);
        formAdd.addMouseListener(deselectMouseAdapter);
        formAction.addMouseListener(deselectMouseAdapter);

        btnClearEmp.addActionListener(e -> clearEmpInputs.run());

        Runnable reloadEmpCombo = () -> {
            cbAllEmpSelect.removeAllItems();
            for (Employee emp : employeeRepository.findAll()) {
                cbAllEmpSelect.addItem(emp.getId() + " - " + emp.getName() + " (" + emp.getRole() + ")");
            }
        };
        reloadEmpCombo.run();

        empTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int row = empTable.getSelectedRow();
            if (row != -1) {
                txtName.setText((String) employeeModel.getValueAt(row, 1));
                txtPhone.setText((String) employeeModel.getValueAt(row, 2));
                txtCccd.setText((String) employeeModel.getValueAt(row, 3));
                cbRole.setSelectedItem((String) employeeModel.getValueAt(row, 4));
                cbShift.setSelectedItem((String) employeeModel.getValueAt(row, 5));
                txtNotes.setText((String) employeeModel.getValueAt(row, 8));
            }
        });

        btnAddEmp.addActionListener(e -> {
            String name = txtName.getText().trim();
            if (name.isEmpty()) { JOptionPane.showMessageDialog(this, "Tên không được rỗng!", "Cảnh báo", JOptionPane.WARNING_MESSAGE); return; }
            try {
                String newId = employeeRepository.generateNextId();
                Employee emp = new Employee(newId, name, txtPhone.getText().trim(), txtCccd.getText().trim(), (String) cbRole.getSelectedItem(), (String) cbShift.getSelectedItem(), 0, 0);
                emp.setNotes(txtNotes.getText().trim());
                String createdUsername = employeeRepository.save(emp);

                String successMsg = "Thêm nhân viên thành công!";
                if (createdUsername != null) {
                    successMsg += "\nĐã tự động tạo tài khoản Lễ tân: " + createdUsername + " (Mật khẩu mặc định: 123456)";
                }
                JOptionPane.showMessageDialog(this, successMsg, "Thông Báo", JOptionPane.INFORMATION_MESSAGE);
                txtName.setText(""); txtPhone.setText(""); txtCccd.setText(""); txtNotes.setText("");
                loadEmployees();
                reloadEmpCombo.run();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnUpdateEmp.addActionListener(e -> {
            int row = empTable.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 nhân viên trong bảng!"); return; }
            String empId = (String) employeeModel.getValueAt(row, 0);
            try {
                Employee emp = new Employee(empId, txtName.getText().trim(), txtPhone.getText().trim(), txtCccd.getText().trim(), (String) cbRole.getSelectedItem(), (String) cbShift.getSelectedItem(), 0, 0);
                emp.setNotes(txtNotes.getText().trim());
                employeeRepository.update(emp);
                JOptionPane.showMessageDialog(this, "Cập nhật nhân viên thành công!");
                loadEmployees();
                reloadEmpCombo.run();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi cập nhật: " + ex.getMessage());
            }
        });

        btnDeleteEmp.addActionListener(e -> {
            int row = empTable.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 nhân viên!"); return; }
            String empId = (String) employeeModel.getValueAt(row, 0);
            if ("ADMIN".equalsIgnoreCase(empId)) {
                JOptionPane.showMessageDialog(this, "Không thể xóa tài khoản Chủ Garage!");
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn xóa nhân viên " + empId + "?", "Xác nhận xóa", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    employeeRepository.delete(empId);
                    JOptionPane.showMessageDialog(this, "Đã xóa nhân viên!");
                    loadEmployees();
                    reloadEmpCombo.run();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Lỗi xóa: " + ex.getMessage());
                }
            }
        });

        btnAdminCheckin.addActionListener(e -> {
            String selectedItem = (String) cbAllEmpSelect.getSelectedItem();
            if (selectedItem == null || selectedItem.isEmpty()) return;
            String targetEmpId = selectedItem.split(" - ")[0];
            String targetEmpName = selectedItem.split(" - ")[1].split(" \\(")[0];
            try {
                if (employeeRepository.addAttendance(targetEmpId, targetEmpName)) {
                    LocalDateTime now = LocalDateTime.now();
                    String dateStr = now.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                    String timeStr = now.format(DateTimeFormatter.ofPattern("HH:mm"));

                    JOptionPane.showMessageDialog(this, 
                            "TÍCH CHẤM CÔNG HỘ THÀNH CÔNG!\n" +
                            "Nhân viên: " + targetEmpName + "\n" +
                            "Ngày: " + dateStr + "\n" +
                            "Giờ: " + timeStr);
                    loadAttendanceLogs();
                    loadEmployees();
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi chấm công: " + ex.getMessage());
            }
        });

        btnResetPassword.addActionListener(e -> {
            String selectedItem = (String) cbAllEmpSelect.getSelectedItem();
            if (selectedItem == null || selectedItem.isEmpty()) return;
            String targetEmpId = selectedItem.split(" - ")[0];
            try {
                if (employeeRepository.resetPassword(targetEmpId)) {
                    JOptionPane.showMessageDialog(this, "Đã reset mật khẩu về 123456!");
                } else {
                    JOptionPane.showMessageDialog(this, "Không tìm thấy tài khoản đăng nhập tương ứng!");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
            }
        });

        btnDeleteAttendance.addActionListener(e -> {
            int row = attTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 dòng lịch sử chấm công cần xóa!");
                return;
            }
            int logId = (int) attendanceModel.getValueAt(row, 0);
            int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn xóa bản ghi chấm công mã " + logId + "?", "Xác nhận xóa", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    employeeRepository.deleteAttendanceLog(logId);
                    JOptionPane.showMessageDialog(this, "Đã xóa bản ghi chấm công!");
                    loadAttendanceLogs();
                    loadEmployees();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Lỗi xóa: " + ex.getMessage());
                }
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
                    "ADMIN".equals(emp.getId()) ? "---" : String.format("%,.0f VNĐ", emp.getTotalSalary()),
                    emp.getNotes()
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

    private JPanel createReceptionistAttendancePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        topBar.setBorder(BorderFactory.createTitledBorder("Điểm Danh / Chấm Công Hộ Kỹ Thuật Viên Làm Cùng Ca"));

        JComboBox<String> cbKtvSelect = new JComboBox<>();
        JButton btnCheckinKtv = createCustomButton("Điểm Danh KTV", new Color(46, 204, 113));

        topBar.add(new JLabel("Chọn KTV Cùng Ca:"));
        topBar.add(cbKtvSelect);
        topBar.add(btnCheckinKtv);

        DefaultTableModel groupKtvModel = new DefaultTableModel(new String[] { "STT", "Mã NV", "Họ & Tên KTV", "Chức Danh", "Ca Phân Công", "Lần Chấm Công Gần Nhất" }, 0);
        JTable ktvTable = new JTable(groupKtvModel);

        DefaultTableModel groupAttHistoryModel = new DefaultTableModel(new String[] { "STT", "Mã KTV", "Họ & Tên KTV", "Thời Gian Chấm Công", "Ca Làm" }, 0);
        JTable historyTable = new JTable(groupAttHistoryModel);

        DefaultTableModel selfAttHistoryModel = new DefaultTableModel(new String[] { "STT", "Mã NV", "Họ & Tên Lễ Tân", "Thời Gian Chấm Công", "Ca Làm" }, 0);
        JTable selfHistoryTable = new JTable(selfAttHistoryModel);

        JSplitPane splitKtvPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(ktvTable), new JScrollPane(historyTable));
        splitKtvPane.setResizeWeight(0.5);

        JTabbedPane attendanceSubTabs = new JTabbedPane();
        attendanceSubTabs.addTab("Chấm Công Nhóm KTV", splitKtvPane);
        attendanceSubTabs.addTab("Lịch Sử Tự Chấm Công Của Bạn", new JScrollPane(selfHistoryTable));

        Runnable loadGroupData = () -> {
            groupKtvModel.setRowCount(0);
            groupAttHistoryModel.setRowCount(0);
            selfAttHistoryModel.setRowCount(0);
            cbKtvSelect.removeAllItems();

            String username = AuthService.getCurrentUser().getUsername();
            String fullName = AuthService.getCurrentUser().getFullName();
            String myShift = employeeRepository.getEmployeeShift(username, fullName);
            String recEmpId = employeeRepository.getEmployeeIdByUsernameOrName(username, fullName);
            
            List<Object[]> ktvList = employeeRepository.findGroupKtvByReceptionist(recEmpId, myShift);
            for (Object[] row : ktvList) {
                groupKtvModel.addRow(row);
                cbKtvSelect.addItem(row[1] + " - " + row[2]);
            }

            for (Object[] row : employeeRepository.findKtvAttendanceHistoryByReceptionist(recEmpId, myShift)) {
                groupAttHistoryModel.addRow(row);
            }

            for (Object[] row : employeeRepository.findSelfAttendanceLogs(recEmpId, username)) {
                selfAttHistoryModel.addRow(row);
            }
        };

        btnCheckinKtv.addActionListener(e -> {
            String selected = (String) cbKtvSelect.getSelectedItem();
            if (selected == null || selected.isEmpty()) {
                int row = ktvTable.getSelectedRow();
                if (row != -1) {
                    selected = groupKtvModel.getValueAt(row, 1) + " - " + groupKtvModel.getValueAt(row, 2);
                }
            }

            if (selected == null || selected.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn KTV trong bảng hoặc danh sách!");
                return;
            }

            String targetKtvId = selected.split(" - ")[0];
            String targetKtvName = selected.split(" - ")[1];

            try {
                if (employeeRepository.addAttendance(targetKtvId, targetKtvName)) {
                    LocalDateTime now = LocalDateTime.now();
                    String dateStr = now.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                    String timeStr = now.format(DateTimeFormatter.ofPattern("HH:mm"));

                    JOptionPane.showMessageDialog(this, 
                            "ĐIỂM DANH KTV THÀNH CÔNG!\n" +
                            "Kỹ Thuật Viên: " + targetKtvName + "\n" +
                            "Người thực hiện: " + AuthService.getCurrentUser().getFullName() + "\n" +
                            "Ngày: " + dateStr + "\n" +
                            "Giờ: " + timeStr);
                    loadGroupData.run();
                } else {
                    JOptionPane.showMessageDialog(this, "Không tìm thấy KTV!");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
            }
        });

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(attendanceSubTabs, BorderLayout.CENTER);

        loadGroupData.run();
        return panel;
    }

    private void loadReportData() {
        if (summaryReportPanel != null) summaryReportPanel.reloadReport();
        if (chartReportPanel != null) chartReportPanel.reloadReport();
    }
}
