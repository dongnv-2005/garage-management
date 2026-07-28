package com.garage.models;

public class Invoice {
    private String id;
    private String licensePlate;
    private String customerName;
    private String serviceName;
    private String partInfo;
    private String notes;
    private String createdBy;
    private double totalAmount;
    private String createdAt;

    public Invoice() {}

    public Invoice(String id, String licensePlate, String customerName, String serviceName, String partInfo, String notes, String createdBy, double totalAmount, String createdAt) {
        this.id = id;
        this.licensePlate = licensePlate;
        this.customerName = customerName;
        this.serviceName = serviceName;
        this.partInfo = partInfo;
        this.notes = notes;
        this.createdBy = createdBy;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getPartInfo() { return partInfo; }
    public void setPartInfo(String partInfo) { this.partInfo = partInfo; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}