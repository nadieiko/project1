package edu.kai.stud;

public class AccessControlSettings {
    public enum AccessControlType {
        MANDATORY,
        DISCRETIONARY,
        ROLE_BASED
    }

    private AccessControlType currentType;

    public AccessControlSettings() {
        // Default to discretionary access control
        this.currentType = AccessControlType.DISCRETIONARY;
    }

    public AccessControlType getCurrentType() {
        return currentType;
    }

    public void setCurrentType(AccessControlType type) {
        this.currentType = type;
    }

    public boolean isMandatory() {
        return currentType == AccessControlType.MANDATORY;
    }

    public boolean isDiscretionary() {
        return currentType == AccessControlType.DISCRETIONARY;
    }

    public boolean isRoleBased() {
        return currentType == AccessControlType.ROLE_BASED;
    }
} 