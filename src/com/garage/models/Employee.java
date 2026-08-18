package com.garage.models;

public class Employee {
    private String id;
    private String name;
    private String phone;
    private String cccd;
    private String role;
    private String shift;
    private int shiftCount;
    private double totalSalary;
    private String notes;
    private String managedBy;
    private boolean isNotified;

    public Employee() {}

    public Employee(String id, String name, String phone, String cccd, String role, String shift, int shiftCount, double totalSalary) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.cccd = cccd;
        this.role = role;
        this.shift = shift;
        this.shiftCount = shiftCount;
        this.totalSalary = totalSalary;
        this.notes = "---";
        this.managedBy = null;
        this.isNotified = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getCccd() { return cccd; }
    public void setCccd(String cccd) { this.cccd = cccd; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getShift() { return shift; }
    public void setShift(String shift) { this.shift = shift; }

    public int getShiftCount() { return shiftCount; }
    public void setShiftCount(int shiftCount) { this.shiftCount = shiftCount; }

    public double getTotalSalary() { return totalSalary; }
    public void setTotalSalary(double totalSalary) { this.totalSalary = totalSalary; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getManagedBy() { return managedBy; }
    public void setManagedBy(String managedBy) { this.managedBy = managedBy; }

    public boolean isNotified() { return isNotified; }
    public void setNotified(boolean isNotified) { this.isNotified = isNotified; }
}