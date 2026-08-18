package com.garage.enums;

public enum Shift {
    MORNING("Ca Sáng (06:00 - 14:00)"),
    AFTERNOON("Ca Chiều (14:00 - 22:00)");

    private final String description;

    Shift(String description) { this.description = description; }
    public String getDescription() { return description; }

    public static Shift fromDescription(String desc) {
        if (desc == null) return MORNING;
        for (Shift s : values()) {
            if (s.description.equalsIgnoreCase(desc) || desc.contains(s.name())) return s;
        }
        return desc.contains("2") || desc.toLowerCase().contains("chiều") ? AFTERNOON : MORNING;
    }
}