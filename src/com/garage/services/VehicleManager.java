package com.garage.services;

import com.garage.enums.RepairStatus;
import com.garage.models.Vehicle;
import com.garage.repository.VehicleRepository;

import java.sql.SQLException;
import java.util.List;

public class VehicleManager {
    private final VehicleRepository repository = new VehicleRepository();

    public List<Vehicle> getAllVehicles() {
        return repository.findAll();
    }

    public List<Vehicle> searchVehicles(String keyword) {
        return repository.search(keyword);
    }

    public void addVehicle(String plate, String brand, String model, String ownerId) throws SQLException {
        repository.save(new Vehicle(plate, brand, model, ownerId, null, RepairStatus.WAITING));
    }

    public void updateVehicleStatus(String plate, RepairStatus status) throws SQLException {
        repository.updateStatus(plate, status);
    }
}