package com.garage.enums;

public enum Role {
    ADMIN("Chủ Garage"),
    RECEPTIONIST("Lễ tân");

    private final String description;

    Role(String description) { this.description = description; }
    public String getDescription() { return description; }
}