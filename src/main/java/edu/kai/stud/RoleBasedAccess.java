package edu.kai.stud;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class RoleBasedAccess {
    private final String username;
    private final String resourceName;
    private final Role role;
    private final EnumSet<Permission> permissions;
    private String timeRestriction;

    public enum Permission {
        READ("Читання"),
        WRITE("Редагування"),
        EXECUTE("Виконання");

        private final String description;

        Permission(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public RoleBasedAccess(String username, String resourceName, Role role) {
        this.username = username;
        this.resourceName = resourceName;
        this.role = role;
        this.permissions = EnumSet.noneOf(Permission.class);
        
        // Встановлення прав доступу в залежності від ролі
        switch (role) {
            case ADMIN:
                permissions.add(Permission.READ);
                permissions.add(Permission.WRITE);
                permissions.add(Permission.EXECUTE);
                break;
            case EDITOR:
                permissions.add(Permission.READ);
                permissions.add(Permission.WRITE);
                timeRestriction = "09:00-18:00";
                break;
            case VIEWER:
                permissions.add(Permission.READ);
                timeRestriction = "09:00-17:00";
                break;
        }
    }

    public String getUsername() {
        return username;
    }

    public String getResourceName() {
        return resourceName;
    }

    public Role getRole() {
        return role;
    }

    public EnumSet<Permission> getPermissions() {
        return permissions;
    }

    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }

    public String getTimeRestriction() {
        return timeRestriction;
    }

    public void setTimeRestriction(String timeRestriction) {
        this.timeRestriction = timeRestriction;
    }

    public boolean isAccessAllowedAtTime(String currentTime) {
        if (timeRestriction == null) return true;
        
        String[] restriction = timeRestriction.split("-");
        if (restriction.length != 2) return true;
        
        return currentTime.compareTo(restriction[0]) >= 0 && 
               currentTime.compareTo(restriction[1]) <= 0;
    }

    public boolean canAccessResource(Resource resource) {
        // Перевірка часових обмежень
        if (timeRestriction != null) {
            LocalTime currentTime = LocalTime.now();
            String timeStr = currentTime.format(DateTimeFormatter.ofPattern("HH:mm"));
            if (!isAccessAllowedAtTime(timeStr)) {
                return false;
            }
        }

        // Перевірка прав доступу в залежності від типу ресурсу та операції
        switch (resource.getType()) {
            case TEXT_FILE:
            case IMAGE:
                // Для читання потрібне право READ
                if (SecurityManager.isReadOperation()) {
                    return hasPermission(Permission.READ);
                }
                // Для запису потрібне право WRITE
                else if (SecurityManager.isWriteOperation()) {
                    return hasPermission(Permission.WRITE);
                }
                return false;
            case EXECUTABLE:
                return hasPermission(Permission.EXECUTE);
            default:
                return false;
        }
    }
} 