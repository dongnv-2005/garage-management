package com.garage.gui;

import com.garage.enums.RepairStatus;
import com.garage.models.Invoice;
import com.garage.models.Vehicle;
import com.garage.services.BillingManager;
import com.garage.services.VehicleManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TableSummaryReportPanel extends BaseReportPanel {
    private DefaultTableModel reportModel;

    public TableSummaryReportPanel(BillingManager billingManager, VehicleManager vehicleManager) {
        super(billingManager, vehicleManager);

        reportModel = new DefaultTableModel(new String[]{"Hạng Mục Thống Kê", "Số Lượng / Giá Trị", "Ghi Chú"}, 0);
        JTable reportTable = new JTable(reportModel);
        reportTable.setRowHeight(35);

        contentContainer.add(new JScrollPane(reportTable), BorderLayout.CENTER);
    }

    @Override
    protected void renderReportContent(List<Invoice> invoices, List<Vehicle> vehicles, int selectedMonth, int selectedYear) {
        reportModel.setRowCount(0);

        double monthlyRevenue = 0;
        int doneVehCountInMonth = 0;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (Invoice inv : invoices) {
            if (inv.getCreatedAt() != null) {
                try {
                    LocalDateTime dt = LocalDateTime.parse(inv.getCreatedAt(), formatter);
                    if (dt.getMonthValue() == selectedMonth && dt.getYear() == selectedYear) {
                        monthlyRevenue += inv.getTotalAmount();
                        doneVehCountInMonth++;
                    }
                } catch (Exception e) {
                    try {
                        String[] parts = inv.getCreatedAt().split(" ")[0].split("-");
                        if (Integer.parseInt(parts[1]) == selectedMonth && Integer.parseInt(parts[0]) == selectedYear) {
                            monthlyRevenue += inv.getTotalAmount();
                            doneVehCountInMonth++;
                        }
                    } catch (Exception ignored) {}
                }
            }
        }

        int inProgressVehCount = (int) vehicles.stream().filter(v -> v.getStatus() != RepairStatus.COMPLETED).count();
        int totalVehInMonth = doneVehCountInMonth + (selectedMonth == LocalDate.now().getMonthValue() && selectedYear == LocalDate.now().getYear() ? inProgressVehCount : 0);
        String timeTitle = "Tháng " + selectedMonth + "/" + selectedYear;

        reportModel.addRow(new Object[]{"Tổng số xe tiếp nhận (" + timeTitle + ")", totalVehInMonth + " xe", "Bao gồm xe đã xuất HĐ trong tháng và xe đang xử lý"});
        reportModel.addRow(new Object[]{"Số xe đã hoàn thành (" + timeTitle + ")", doneVehCountInMonth + " xe", "Đã hoàn thành sửa chữa & xuất hóa đơn"});
        reportModel.addRow(new Object[]{"Số xe đang sửa / chờ xử lý hiện tại", inProgressVehCount + " xe", "Cần tiếp tục theo dõi tiến độ thi công"});
        reportModel.addRow(new Object[]{"Doanh thu " + timeTitle, String.format("%,.0f VNĐ", monthlyRevenue), "Tổng tiền thu từ hóa đơn dịch vụ trong tháng"});
    }
}