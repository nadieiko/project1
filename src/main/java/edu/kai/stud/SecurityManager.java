package edu.kai.stud;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class SecurityManager {
    private static final Map<String, SecurityLevel> userLevels = new HashMap<>();
    private static final Map<String, Resource> resources = new HashMap<>();
    private static final Map<String, Map<String, DiscretionaryAccess>> discretionaryAccess = new HashMap<>();
    private static final Map<String, Map<String, RoleBasedAccess>> roleBasedAccess = new HashMap<>();
    private static final Map<String, Role> userRoles = new HashMap<>();
    private static final String DATA_DIR = "C:\\Users\\nadey\\TBD_Boyko\\Data";
    private static AccessType currentAccessType = null;
    private static boolean isWriteOperation = false;

    static {
        // Ініціалізація рівнів доступу користувачів
        userLevels.put("Boyko_1", SecurityLevel.NOT_SECRET);
        userLevels.put("Boyko_2", SecurityLevel.FOR_SERVICE_USE);
        userLevels.put("Boyko_3", SecurityLevel.SECRET);
        userLevels.put("Boyko_4", SecurityLevel.TOP_SECRET);
        userLevels.put("Boyko_5", SecurityLevel.SPECIAL_IMPORTANCE);

        // Ініціалізація ролей користувачів
        userRoles.put("Boyko_1", Role.VIEWER);
        userRoles.put("Boyko_2", Role.VIEWER);
        userRoles.put("Boyko_3", Role.EDITOR);
        userRoles.put("Boyko_4", Role.EDITOR);
        userRoles.put("Boyko_5", Role.ADMIN);

        // Ініціалізація ресурсів та їх рівнів безпеки
        initializeResources();
        initializeDiscretionaryAccess();
        initializeRoleBasedAccess();
    }

    private static void initializeResources() {
        resources.put("file1.txt", new Resource("file1.txt", 
            Paths.get(DATA_DIR, "file1.txt"), SecurityLevel.NOT_SECRET, Resource.ResourceType.TEXT_FILE));
        resources.put("file2.txt", new Resource("file2.txt", 
            Paths.get(DATA_DIR, "file2.txt"), SecurityLevel.FOR_SERVICE_USE, Resource.ResourceType.TEXT_FILE));
        resources.put("file3.txt", new Resource("file3.txt", 
            Paths.get(DATA_DIR, "file3.txt"), SecurityLevel.SECRET, Resource.ResourceType.TEXT_FILE));
        resources.put("file4.exe", new Resource("file4.exe", 
            Paths.get(DATA_DIR, "file4.exe"), SecurityLevel.TOP_SECRET, Resource.ResourceType.EXECUTABLE));
        resources.put("file5.jpg", new Resource("file5.jpg", 
            Paths.get(DATA_DIR, "file5.jpg"), SecurityLevel.SPECIAL_IMPORTANCE, Resource.ResourceType.IMAGE));
    }

    private static void initializeDiscretionaryAccess() {
        // Ініціалізація дискреційних прав доступу для користувачів
        for (String username : userLevels.keySet()) {
            discretionaryAccess.put(username, new HashMap<>());
            for (String resourceName : resources.keySet()) {
                DiscretionaryAccess access = new DiscretionaryAccess(username, resourceName);
                Resource resource = resources.get(resourceName);
                
                // Встановлення базових прав доступу
                if (resource.getType() == Resource.ResourceType.TEXT_FILE || 
                    resource.getType() == Resource.ResourceType.IMAGE) {
                    access.addPermission(DiscretionaryAccess.Permission.READ);
                    if (username.equals("Boyko_4") || username.equals("Boyko_5")) {
                        access.addPermission(DiscretionaryAccess.Permission.WRITE);
                    }
                } else if (resource.getType() == Resource.ResourceType.EXECUTABLE) {
                    if (username.equals("Boyko_3") || username.equals("Boyko_4") || 
                        username.equals("Boyko_5")) {
                        access.addPermission(DiscretionaryAccess.Permission.EXECUTE);
                    }
                }
                
                // Встановлення часових обмежень для деяких користувачів
                if (username.equals("Boyko_1") || username.equals("Boyko_2")) {
                    access.setTimeRestriction("09:00-17:00");
                }
                
                discretionaryAccess.get(username).put(resourceName, access);
            }
        }
    }

    private static void initializeRoleBasedAccess() {
        // Ініціалізація рольових прав доступу для користувачів
        for (Map.Entry<String, Role> entry : userRoles.entrySet()) {
            String username = entry.getKey();
            Role role = entry.getValue();
            
            Map<String, RoleBasedAccess> userAccess = new HashMap<>();
            for (String resourceName : resources.keySet()) {
                RoleBasedAccess access = new RoleBasedAccess(username, resourceName, role);
                userAccess.put(resourceName, access);
            }
            roleBasedAccess.put(username, userAccess);
        }
    }

    public static void setAccessType(AccessType type) {
        currentAccessType = type;
    }

    public static AccessType getCurrentAccessType() {
        return currentAccessType;
    }

    public static SecurityLevel getUserLevel(String username) {
        return userLevels.getOrDefault(username, SecurityLevel.NOT_SECRET);
    }

    public static boolean changeUserSecurityLevel(String username, SecurityLevel newLevel) {
        if (currentAccessType != AccessType.MANDATORY) {
            return false;
        }
        
        // Перевіряємо довжину пароля для високих рівнів доступу
        if (SecurityLevel.requiresLongPassword(newLevel)) {
            String password = UserDatabase.getUserPassword(username);
            if (password == null || password.length() <= 10) {
                return false;
            }
        }
        userLevels.put(username, newLevel);
        return true;
    }

    public static boolean canAccessResource(String username, String resourceName) {
        if (currentAccessType == null) {
            return false;
        }

        switch (currentAccessType) {
            case MANDATORY:
                return canAccessResourceMandatory(username, resourceName);
            case DISCRETIONARY:
                return canAccessResourceDiscretionary(username, resourceName);
            case ROLE_BASED:
                return canAccessResourceRoleBased(username, resourceName);
            default:
                return false;
        }
    }

    private static boolean canAccessResourceMandatory(String username, String resourceName) {
        SecurityLevel userLevel = getUserLevel(username);
        Resource resource = resources.get(resourceName);
        return resource != null && userLevel.canAccess(resource.getSecurityLevel());
    }

    private static boolean canAccessResourceDiscretionary(String username, String resourceName) {
        Map<String, DiscretionaryAccess> userAccess = discretionaryAccess.get(username);
        if (userAccess == null) return false;

        DiscretionaryAccess access = userAccess.get(resourceName);
        if (access == null) return false;

        // Перевірка часових обмежень
        if (access.getTimeRestriction() != null) {
            LocalTime currentTime = LocalTime.now();
            String timeStr = currentTime.format(DateTimeFormatter.ofPattern("HH:mm"));
            if (!access.isAccessAllowedAtTime(timeStr)) {
                return false;
            }
        }

        Resource resource = resources.get(resourceName);
        if (resource == null) return false;

        // Перевірка конкретних прав доступу в залежності від типу ресурсу
        switch (resource.getType()) {
            case TEXT_FILE:
            case IMAGE:
                return access.hasPermission(DiscretionaryAccess.Permission.READ);
            case EXECUTABLE:
                return access.hasPermission(DiscretionaryAccess.Permission.EXECUTE);
            default:
                return false;
        }
    }

    private static boolean canAccessResourceRoleBased(String username, String resourceName) {
        Map<String, RoleBasedAccess> userAccess = roleBasedAccess.get(username);
        if (userAccess == null) return false;

        RoleBasedAccess access = userAccess.get(resourceName);
        if (access == null) return false;

        Resource resource = resources.get(resourceName);
        if (resource == null) return false;

        return access.canAccessResource(resource);
    }

    public static void viewResource(String username, String resourceName, Component parent) {
        if (!canAccessResource(username, resourceName)) {
            JOptionPane.showMessageDialog(parent, 
                "Відмовлено в доступі до " + resourceName, 
                "Помилка доступу", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        Resource resource = resources.get(resourceName);
        if (resource == null || !resource.exists()) {
            JOptionPane.showMessageDialog(parent, 
                "Ресурс не знайдено: " + resourceName, 
                "Помилка", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            switch (resource.getType()) {
                case TEXT_FILE:
                    showTextFile(username, resource.getPath(), parent);
                    break;
                case IMAGE:
                    showImage(username, resource.getPath(), parent);
                    break;
                case EXECUTABLE:
                    executeFile(username, resource.getPath(), parent);
                    break;
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(parent, 
                "Помилка при відкритті файлу: " + e.getMessage(), 
                "Помилка", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void showTextFile(String username, Path path, Component parent) throws IOException {
        String content = Files.readString(path);
        JTextArea textArea = new JTextArea(content);
        
        // Встановлюємо можливість редагування в залежності від типу доступу
        boolean canEdit = false;
        
        switch (currentAccessType) {
            case DISCRETIONARY:
                Map<String, DiscretionaryAccess> discAccess = discretionaryAccess.get(username);
                if (discAccess != null) {
                    DiscretionaryAccess access = discAccess.get(path.getFileName().toString());
                    canEdit = access != null && access.hasPermission(DiscretionaryAccess.Permission.WRITE);
                }
                break;
            case ROLE_BASED:
                Map<String, RoleBasedAccess> roleAccess = roleBasedAccess.get(username);
                if (roleAccess != null) {
                    RoleBasedAccess access = roleAccess.get(path.getFileName().toString());
                    if (access != null) {
                        setWriteOperation(true);
                        canEdit = access.canAccessResource(resources.get(path.getFileName().toString()));
                        setWriteOperation(false);
                    }
                }
                break;
            case MANDATORY:
                canEdit = true;
                break;
        }
        
        textArea.setEditable(canEdit);
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(400, 300));

        int result = JOptionPane.showConfirmDialog(parent, scrollPane, 
            "Перегляд " + path.getFileName(), 
            JOptionPane.OK_CANCEL_OPTION, 
            JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION && textArea.isEditable()) {
            setWriteOperation(true);
            if (canAccessResource(username, path.getFileName().toString())) {
                Files.writeString(path, textArea.getText());
            } else {
                JOptionPane.showMessageDialog(parent,
                    "Немає прав на редагування файлу",
                    "Помилка",
                    JOptionPane.ERROR_MESSAGE);
            }
            setWriteOperation(false);
        }
    }

    private static void showImage(String username, Path path, Component parent) {
        ImageIcon icon = new ImageIcon(path.toString());
        JLabel label = new JLabel(icon);
        JScrollPane scrollPane = new JScrollPane(label);
        scrollPane.setPreferredSize(new Dimension(400, 300));

        JOptionPane.showMessageDialog(parent, scrollPane, 
            "Перегляд " + path.getFileName(), 
            JOptionPane.PLAIN_MESSAGE);
    }

    private static void executeFile(String username, Path path, Component parent) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(path.toString());
        processBuilder.start();
    }

    public static void addNewUser(String username) {
        // Для дискреційного доступу просто додаємо користувача без рівня доступу
        if (currentAccessType == AccessType.DISCRETIONARY) {
            // Ініціалізуємо права доступу для нового користувача
            Map<String, DiscretionaryAccess> userAccess = new HashMap<>();
            for (String resourceName : resources.keySet()) {
                DiscretionaryAccess access = new DiscretionaryAccess(username, resourceName);
                Resource resource = resources.get(resourceName);
                
                // За замовчуванням даємо права на читання для текстових файлів та зображень
                if (resource.getType() == Resource.ResourceType.TEXT_FILE || 
                    resource.getType() == Resource.ResourceType.IMAGE) {
                    access.addPermission(DiscretionaryAccess.Permission.READ);
                }
                
                userAccess.put(resourceName, access);
            }
            discretionaryAccess.put(username, userAccess);
        }
    }

    public static void addNewUser(String username, SecurityLevel level) {
        // Для мандатного доступу додаємо користувача з рівнем доступу
        if (currentAccessType == AccessType.MANDATORY) {
            userLevels.put(username, level);
        } else {
            addNewUser(username);
        }
    }

    public static void addNewUser(String username, Role role) {
        if (currentAccessType == AccessType.ROLE_BASED) {
            userRoles.put(username, role);
            
            // Ініціалізація прав доступу для нового користувача
            Map<String, RoleBasedAccess> userAccess = new HashMap<>();
            for (String resourceName : resources.keySet()) {
                RoleBasedAccess access = new RoleBasedAccess(username, resourceName, role);
                userAccess.put(resourceName, access);
            }
            roleBasedAccess.put(username, userAccess);
        }
    }

    public static void addNewResource(String name, Path path, SecurityLevel level, Resource.ResourceType type) {
        resources.put(name, new Resource(name, path, level, type));
    }

    public static Map<String, Resource> getResources() {
        return new HashMap<>(resources);
    }

    public static boolean changeResourceSecurityLevel(String resourceName, SecurityLevel newLevel) {
        if (currentAccessType != AccessType.MANDATORY) {
            return false;
        }
        
        Resource resource = resources.get(resourceName);
        if (resource == null) {
            return false;
        }
        
        resources.put(resourceName, new Resource(
            resource.getName(),
            resource.getPath(),
            newLevel,
            resource.getType()
        ));
        return true;
    }

    public static Map<String, DiscretionaryAccess> getUserDiscretionaryAccess(String username) {
        return discretionaryAccess.getOrDefault(username, new HashMap<>());
    }

    public static void updateDiscretionaryAccess(String username, String resourceName, 
                                               EnumSet<DiscretionaryAccess.Permission> permissions, 
                                               String timeRestriction) {
        Map<String, DiscretionaryAccess> userAccess = discretionaryAccess.computeIfAbsent(username, k -> new HashMap<>());
        DiscretionaryAccess access = userAccess.computeIfAbsent(resourceName, 
            k -> new DiscretionaryAccess(username, resourceName));
        
        access.getPermissions().clear();
        access.getPermissions().addAll(permissions);
        access.setTimeRestriction(timeRestriction);
    }

    public static Map<String, RoleBasedAccess> getUserRoleBasedAccess(String username) {
        return roleBasedAccess.getOrDefault(username, new HashMap<>());
    }

    public static Role getUserRole(String username) {
        return userRoles.getOrDefault(username, Role.VIEWER);
    }

    public static boolean isReadOperation() {
        return !isWriteOperation;
    }

    public static boolean isWriteOperation() {
        return isWriteOperation;
    }

    private static void setWriteOperation(boolean isWrite) {
        isWriteOperation = isWrite;
    }
} 