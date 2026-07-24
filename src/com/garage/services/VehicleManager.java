package com.garage.services;

import com.garage.enums.RepairStatus;
import com.garage.models.Customer;
import com.garage.models.Vehicle;
import com.garage.repository.CustomerRepository;
import com.garage.repository.VehicleRepository;
import java.util.List;

public class VehicleManager {
    private final VehicleRepository vehicleRepo;
    private final CustomerRepository customerRepo;

    public VehicleManager(VehicleRepository vehicleRepo, CustomerRepository customerRepo) {
        this.vehicleRepo = vehicleRepo;
        this.customerRepo = customerRepo;
    }

    public boolean registerVehicle(String licensePlate, String brand, String model, String customerId) {
        Customer owner = customerRepo.findById(customerId);
        if (owner == null) {
            System.out.println("Khách hàng không tồn tại. Vui lòng tạo khách hàng trước!");
            return false;
        }
        if (vehicleRepo.existsByLicensePlate(licensePlate)) {
            System.out.println("Biển số xe này đã có trong hệ thống!");
            return false;
        }
        Vehicle vehicle = new Vehicle(licensePlate, brand, model, owner);
        vehicleRepo.save(vehicle);
        owner.addVehicle(vehicle);
        System.out.println("Đã tiếp nhận xe " + licensePlate + " cho khách hàng " + owner.getName());
        return true;
    }

    public Vehicle findVehicle(String licensePlate) {
        return vehicleRepo.findByLicensePlate(licensePlate);
    }

    public void updateVehicleStatus(String licensePlate, RepairStatus status) {
        Vehicle vehicle = vehicleRepo.findByLicensePlate(licensePlate);
        if (vehicle == null) {
            System.out.println("Không tìm thấy xe với biển số: " + licensePlate);
            return;
        }
        vehicle.setStatus(status);
        System.out.println("Đã cập nhật trạng thái xe " + licensePlate + " thành: " + status.getDescription());
    }

    public List<Vehicle> getAllVehicles() {
        return vehicleRepo.findAll();
    }
}