package com.garage.models;

public class MaintenanceService extends BaseService {
    private double laborHours;
    private double hourlyRate;

    public MaintenanceService(String serviceId, double laborHours, double hourlyRate) {
        super(serviceId, "Bảo trì định kỳ hệ thống", 200000);
        this.laborHours = laborHours;
        this.hourlyRate = hourlyRate;
    }

    @Override
    public double calculateCost() {
        return basePrice + (laborHours * hourlyRate);
    }
}