package com.garage.models;

public class EngineRepairService extends BaseService {
    private double partsCost;
    private double complexityFactor;

    public EngineRepairService(String serviceId, double partsCost, double complexityFactor) {
        super(serviceId, "Sửa chữa Động cơ & Hộp số", 500000);
        this.partsCost = partsCost;
        this.complexityFactor = complexityFactor;
    }

    @Override
    public double calculateCost() {
        return (basePrice + partsCost) * complexityFactor;
    }
}