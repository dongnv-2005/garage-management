package com.garage.models;

import com.garage.enums.RepairStatus;

public class Vehicle {
    private String licensePlate;
    private String brand;
    private String model;
    private RepairStatus status;
    private Customer owner;

    public Vehicle(String licensePlate, String brand, String model, Customer owner) {
        this.licensePlate = licensePlate;
        this.brand = brand;
        this.model = model;
        this.owner = owner;
        this.status = RepairStatus.WAITING;
    }

    public String getLicensePlate() { return licensePlate; }
    public String getBrand() { return brand; }
    public String getModel() { return model; }
    public RepairStatus getStatus() { return status; }
    public void setStatus(RepairStatus status) { this.status = status; }
    public Customer getOwner() { return owner; }

    @Override
    public String toString() {
        return String.format("Biển số: %s | Hãng: %s %s | Trạng thái: %s | Chủ xe: %s",
                licensePlate, brand, model, status.getDescription(), owner.getName());
    }
}