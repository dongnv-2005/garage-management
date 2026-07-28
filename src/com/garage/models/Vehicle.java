package com.garage.models;

import com.garage.enums.RepairStatus;

public class Vehicle {
    private String licensePlate;
    private String brand;
    private String model;
    private String ownerId;
    private String ownerName;
    private RepairStatus status;

    public Vehicle() {}

    public Vehicle(String licensePlate, String brand, String model, String ownerId, String ownerName, RepairStatus status) {
        this.licensePlate = licensePlate;
        this.brand = brand;
        this.model = model;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.status = status;
    }

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public RepairStatus getStatus() { return status; }
    public void setStatus(RepairStatus status) { this.status = status; }
}