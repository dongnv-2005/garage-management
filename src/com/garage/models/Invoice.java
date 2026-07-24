package com.garage.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Invoice {
    private String invoiceId;
    private RepairOrder order;
    private LocalDateTime paymentDate;
    private double totalAmount;

    public Invoice(String invoiceId, RepairOrder order) {
        this.invoiceId = invoiceId;
        this.order = order;
        this.paymentDate = LocalDateTime.now();
        this.totalAmount = order.calculateTotalCost();
    }

    public String getInvoiceId() { return invoiceId; }
    public double getTotalAmount() { return totalAmount; }
    public LocalDateTime getPaymentDate() { return paymentDate; }
    public RepairOrder getOrder() { return order; }

    public void printInvoice() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        System.out.println("\n==================================================");
        System.out.println("            HÓA ĐƠN DỊCH VỤ GARA Ô TÔ            ");
        System.out.println("==================================================");
        System.out.println("Mã hóa đơn : " + invoiceId);
        System.out.println("Ngày xuất  : " + paymentDate.format(dtf));
        System.out.println("Khách hàng : " + order.getVehicle().getOwner().getName() + " | SĐT: " + order.getVehicle().getOwner().getPhone());
        System.out.println("Xe         : " + order.getVehicle().getLicensePlate() + " (" + order.getVehicle().getBrand() + " " + order.getVehicle().getModel() + ")");
        System.out.println("--------------------------------------------------");
        System.out.println("Danh sách dịch vụ đã sử dụng:");
        int idx = 1;
        for (BaseService service : order.getServices()) {
            System.out.printf("  %d. %-30s : %,12.0f VNĐ\n", idx++, service.getServiceName(), service.calculateCost());
        }
        System.out.println("--------------------------------------------------");
        System.out.printf("TỔNG CHI PHÍ THANH TOÁN            : %,12.0f VNĐ\n", totalAmount);
        System.out.println("==================================================\n");
    }
}