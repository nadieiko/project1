package edu.kai.stud;

public enum AccessType {
    MANDATORY("Мандатне"),
    DISCRETIONARY("Дискреційне"),
    ROLE_BASED("Рольове");

    private final String description;

    AccessType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
} 