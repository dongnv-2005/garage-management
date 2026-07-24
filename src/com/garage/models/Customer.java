package com.garage.models;

import java.util.ArrayList;
import java.util.List;

public class Customer {
    private String id;
    private String name;
    private String phone;
    private List<Vehicle> vehicles;

    public Customer(String id, String name, String phone) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.vehicles = new ArrayList<>();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public List<Vehicle> getVehicles() { return vehicles; }

    public void addVehicle(Vehicle vehicle) {
        this.vehicles.add(vehicle);
    }

    @Override
    public String toString() {
        return String.format("Mã KH: %s | Tên: %s | SĐT: %s | Số xe sở hữu: %d",
                id, name, phone, vehicles.size());
    }
}