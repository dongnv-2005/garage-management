package com.garage.enums;

public enum RepairStatus {
    WAITING("Chờ sửa"),
    IN_PROGRESS("Đang sửa"),
    COMPLETED("Đã hoàn tất");

    private final String description;

    RepairStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}