package com.garage.enums;

public enum Shift {
    MORNING("Ca Sáng (06:00 - 14:00)"),
    AFTERNOON("Ca Chiều (14:00 - 22:00)");

    private final String description;

    Shift(String description) { this.description = description; }
    public String getDescription() { return description; }
}