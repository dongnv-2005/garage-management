package com.garage.models;

public class OilChangeService extends BaseService {
    private double oilVolumeLiters;
    private double pricePerLiter;

    public OilChangeService(String serviceId, double oilVolumeLiters, double pricePerLiter) {
        super(serviceId, "Thay dầu & Nhớt engine", 100000);
        this.oilVolumeLiters = oilVolumeLiters;
        this.pricePerLiter = pricePerLiter;
    }

    @Override
    public double calculateCost() {
        return basePrice + (oilVolumeLiters * pricePerLiter);
    }
}