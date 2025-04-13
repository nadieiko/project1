package edu.kai.stud;

import java.util.EnumSet;

public class DiscretionaryAccess {
    public enum Permission {
        READ("Читання"),
        WRITE("Запис"),
        EXECUTE("Виконання");

        private final String description;

        Permission(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private final String username;
    private final String resourceName;
    private final EnumSet<Permission> permissions;
    private String timeRestriction; // формат "HH:mm-HH:mm" або null якщо немає обмежень

    public DiscretionaryAccess(String username, String resourceName) {
        this.username = username;
        this.resourceName = resourceName;
        this.permissions = EnumSet.noneOf(Permission.class);
        this.timeRestriction = null;
    }

    public String getUsername() {
        return username;
    }

    public String getResourceName() {
        return resourceName;
    }

    public EnumSet<Permission> getPermissions() {
        return permissions;
    }

    public void addPermission(Permission permission) {
        permissions.add(permission);
    }

    public void removePermission(Permission permission) {
        permissions.remove(permission);
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
} 