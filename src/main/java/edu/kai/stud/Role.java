package edu.kai.stud;

public enum Role {
    ADMIN("Адміністратор"),
    EDITOR("Редактор"),
    VIEWER("Переглядач");

    private final String description;

    Role(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
} 