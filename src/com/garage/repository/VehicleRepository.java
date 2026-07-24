package com.garage.repository;

import com.garage.models.Vehicle;
import java.util.*;

public class VehicleRepository {
    private final Map<String, Vehicle> vehicles = new HashMap<>();

    public void save(Vehicle vehicle) {
        vehicles.put(vehicle.getLicensePlate(), vehicle);
    }

    public Vehicle findByLicensePlate(String licensePlate) {
        return vehicles.get(licensePlate);
    }

    public boolean existsByLicensePlate(String licensePlate) {
        return vehicles.containsKey(licensePlate);
    }

    public List<Vehicle> findAll() {
        return new ArrayList<>(vehicles.values());
    }
}