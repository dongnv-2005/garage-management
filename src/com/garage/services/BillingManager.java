package com.garage.services;

import com.garage.enums.RepairStatus;
import com.garage.models.BaseService;
import com.garage.models.Invoice;
import com.garage.models.RepairOrder;
import com.garage.models.Vehicle;
import com.garage.repository.InvoiceRepository;
import com.garage.repository.VehicleRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BillingManager {
    private final InvoiceRepository invoiceRepo;
    private final VehicleRepository vehicleRepo;
    private final List<RepairOrder> orders = new ArrayList<>();

    public BillingManager(InvoiceRepository invoiceRepo, VehicleRepository vehicleRepo) {
        this.invoiceRepo = invoiceRepo;
        this.vehicleRepo = vehicleRepo;
    }

    public RepairOrder createOrder(String orderId, String licensePlate) {
        Vehicle vehicle = vehicleRepo.findByLicensePlate(licensePlate);
        if (vehicle == null) {
            System.out.println("❌ Xe chưa đăng ký trong gara!");
            return null;
        }
        RepairOrder order = new RepairOrder(orderId, vehicle);
        orders.add(order);
        System.out.println("✅ Đã tạo phiếu dịch vụ " + orderId + " cho xe " + licensePlate);
        return order;
    }

    public Invoice generateInvoice(String invoiceId, RepairOrder order) {
        if (order == null || order.getServices().isEmpty()) {
            System.out.println("❌ Phiếu dịch vụ rỗng, không thể xuất hóa đơn!");
            return null;
        }
        Invoice invoice = new Invoice(invoiceId, order);
        invoiceRepo.save(invoice);
        order.getVehicle().setStatus(RepairStatus.COMPLETED);
        return invoice;
    }

    public void printReports() {
        List<Vehicle> allVehicles = vehicleRepo.findAll();
        List<Invoice> allInvoices = invoiceRepo.findAll();

        System.out.println("\n==================================================");
        System.out.println("             BÁO CÁO THỐNG KÊ GARA                ");
        System.out.println("==================================================");
        System.out.println("1. Tổng số xe đã tiếp nhận    : " + allVehicles.size() + " xe");
        long completedVehicles = allVehicles.stream()
                .filter(v -> v.getStatus() == RepairStatus.COMPLETED).count();
        System.out.println("2. Số lượng xe đã sửa xong    : " + completedVehicles + " xe");

        double totalRevenue = allInvoices.stream().mapToDouble(Invoice::getTotalAmount).sum();
        System.out.printf("3. Tổng doanh thu hệ thống    : %,12.0f VNĐ\n", totalRevenue);

        System.out.println("4. Thống kê dịch vụ được sử dụng:");
        Map<String, Long> serviceCount = new HashMap<>();
        for (RepairOrder order : orders) {
            for (BaseService s : order.getServices()) {
                serviceCount.put(s.getServiceName(), serviceCount.getOrDefault(s.getServiceName(), 0L) + 1);
            }
        }
        serviceCount.forEach((name, count) ->
                System.out.println("   - " + name + ": " + count + " lần")
        );
        System.out.println("==================================================\n");
    }
}