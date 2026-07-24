package com.garage.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RepairOrder {
    private String orderId;
    private Vehicle vehicle;
    private List<BaseService> services;
    private LocalDateTime createdAt;

    public RepairOrder(String orderId, Vehicle vehicle) {
        this.orderId = orderId;
        this.vehicle = vehicle;
        this.services = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
    }

    public String getOrderId() { return orderId; }
    public Vehicle getVehicle() { return vehicle; }
    public List<BaseService> getServices() { return services; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void addService(BaseService service) {
        this.services.add(service);
    }

    public double calculateTotalCost() {
        double total = 0;
        for (BaseService s : services) {
            total += s.calculateCost();
        }
        return total;
    }
}