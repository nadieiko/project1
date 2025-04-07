package edu.kai.stud;

public enum SecurityLevel {
    LOW(1, "Низький"),
    MEDIUM(2, "Середній"),
    HIGH(3, "Високий"),
    ADMIN(4, "Адміністратор");

    private final int level;
    private final String description;

    SecurityLevel(int level, String description) {
        this.level = level;
        this.description = description;
    }

    public int getLevel() {
        return level;
    }

    public String getDescription() {
        return description;
    }

    public boolean canAccess(SecurityLevel resourceLevel) {
        return this.level >= resourceLevel.level;
    }
} 