package edu.kai.stud;

public enum SecurityLevel {
    NOT_SECRET(1, "Не таємно"),
    FOR_SERVICE_USE(2, "Для службового користування"),
    SECRET(3, "Таємно"),
    TOP_SECRET(4, "Цілком таємно"),
    SPECIAL_IMPORTANCE(5, "Особливої важливості");

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

    public static boolean requiresLongPassword(SecurityLevel level) {
        return level == TOP_SECRET || level == SPECIAL_IMPORTANCE;
    }
} 