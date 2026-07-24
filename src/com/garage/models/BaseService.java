package com.garage.models;

public abstract class BaseService {
    protected String serviceId;
    protected String serviceName;
    protected double basePrice;

    public BaseService(String serviceId, String serviceName, double basePrice) {
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.basePrice = basePrice;
    }

    public String getServiceId() { return serviceId; }
    public String getServiceName() { return serviceName; }
    public double getBasePrice() { return basePrice; }

    public abstract double calculateCost();
}