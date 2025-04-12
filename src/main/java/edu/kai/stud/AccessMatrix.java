package edu.kai.stud;

import java.util.HashMap;
import java.util.Map;

public class AccessMatrix {
    private Map<String, Map<String, AccessRights>> accessMatrix;

    public AccessMatrix() {
        accessMatrix = new HashMap<>();
    }

    public void addUser(String user) {
        accessMatrix.putIfAbsent(user, new HashMap<>());
    }

    public void addResource(String user, String resource, AccessRights rights) {
        accessMatrix.get(user).put(resource, rights);
    }

    public AccessRights getAccessRights(String user, String resource) {
        return accessMatrix.getOrDefault(user, new HashMap<>()).get(resource);
    }

    public void setAccessRights(String user, String resource, AccessRights rights) {
        if (accessMatrix.containsKey(user)) {
            accessMatrix.get(user).put(resource, rights);
        }
    }

    public void removeUser(String user) {
        accessMatrix.remove(user);
    }

    public void removeResource(String user, String resource) {
        if (accessMatrix.containsKey(user)) {
            accessMatrix.get(user).remove(resource);
        }
    }

    public void updateAccessRights(String user, String resource, AccessRights newRights) {
        if (accessMatrix.containsKey(user)) {
            accessMatrix.get(user).put(resource, newRights);
        }
    }

    public void addNewUserWithDefaultRights(String user, Map<String, AccessRights> defaultRights) {
        accessMatrix.put(user, new HashMap<>(defaultRights));
    }

    public void addNewResourceWithDefaultRights(String resource, AccessRights defaultRights) {
        for (Map<String, AccessRights> userRights : accessMatrix.values()) {
            userRights.put(resource, defaultRights);
        }
    }

    public static class AccessRights {
        private boolean canView;
        private boolean canEdit;
        private boolean canSave;
        private boolean canExecute;
        private String timeRestriction;

        public AccessRights(boolean canView, boolean canEdit, boolean canSave, boolean canExecute, String timeRestriction) {
            this.canView = canView;
            this.canEdit = canEdit;
            this.canSave = canSave;
            this.canExecute = canExecute;
            this.timeRestriction = timeRestriction;
        }

        // Getters and setters
        public boolean canView() { return canView; }
        public boolean canEdit() { return canEdit; }
        public boolean canSave() { return canSave; }
        public boolean canExecute() { return canExecute; }
        public String getTimeRestriction() { return timeRestriction; }
    }
} 