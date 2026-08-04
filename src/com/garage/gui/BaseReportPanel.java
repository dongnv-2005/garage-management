package com.garage.gui;

import com.garage.models.Invoice;
import com.garage.models.Vehicle;
import com.garage.services.BillingManager;
import com.garage.services.VehicleManager;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

public abstract class BaseReportPanel extends JPanel {
    protected final BillingManager billingManager;
    protected final VehicleManager vehicleManager;

    protected JComboBox<Integer> cbReportMonth;
    protected JComboBox<Integer> cbReportYear;
    protected JLabel lblTotalRevenue;
    protected JPanel contentContainer;

    public BaseReportPanel(BillingManager billingManager, VehicleManager vehicleManager) {
        this.billingManager = billingManager;
        this.vehicleManager = vehicleManager;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        topBar.setBorder(BorderFactory.createTitledBorder("Bộ Lọc Thống Kê Theo Thời Gian"));

        cbReportMonth = new JComboBox<>();
        for (int m = 1; m <= 12; m++) cbReportMonth.addItem(m);
        cbReportMonth.setSelectedItem(LocalDate.now().getMonthValue());

        cbReportYear = new JComboBox<>();
        int currentYear = LocalDate.now().getYear();
        for (int y = currentYear - 5; y <= currentYear; y++) cbReportYear.addItem(y);
        cbReportYear.setSelectedItem(currentYear);

        // --- SỬA NÚT "XEM THỐNG KÊ" DỄ NHÌN HƠN TẠI ĐÂY ---
        JButton btnFilter = new JButton("Xem Thống Kê");
        btnFilter.setBackground(new Color(142, 68, 173)); // Màu tím mộng mơ
        btnFilter.setForeground(Color.WHITE);             // Chữ trắng tương phản cao
        btnFilter.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnFilter.setFocusPainted(false);
        btnFilter.setOpaque(true);
        btnFilter.setContentAreaFilled(true);
        btnFilter.setBorderPainted(false);
        btnFilter.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnFilter.setPreferredSize(new Dimension(130, 30));

        topBar.add(new JLabel("Tháng:")); topBar.add(cbReportMonth);
        topBar.add(new JLabel("Năm:")); topBar.add(cbReportYear);
        topBar.add(btnFilter);

        contentContainer = new JPanel(new BorderLayout());

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBackground(new Color(236, 240, 241));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));

        lblTotalRevenue = new JLabel("TỔNG DOANH THU TOÀN HỆ THỐNG: 0 VNĐ");
        lblTotalRevenue.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblTotalRevenue.setForeground(new Color(192, 57, 43));
        bottomPanel.add(lblTotalRevenue);

        add(topBar, BorderLayout.NORTH);
        add(contentContainer, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        btnFilter.addActionListener(e -> reloadReport());
    }

    public void reloadReport() {
        int month = (int) cbReportMonth.getSelectedItem();
        int year = (int) cbReportYear.getSelectedItem();

        List<Invoice> allInvoices = billingManager.getAllInvoices();
        List<Vehicle> allVehicles = vehicleManager.getAllVehicles();

        double totalSystemRevenue = allInvoices.stream().mapToDouble(Invoice::getTotalAmount).sum();
        lblTotalRevenue.setText(String.format("TỔNG DOANH THU TOÀN HỆ THỐNG: %,.0f VNĐ", totalSystemRevenue));

        renderReportContent(allInvoices, allVehicles, month, year);
    }

    protected abstract void renderReportContent(List<Invoice> invoices, List<Vehicle> vehicles, int selectedMonth, int selectedYear);
}