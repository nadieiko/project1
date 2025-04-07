package edu.kai.stud;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class SecurityManager {
    private static final Map<String, SecurityLevel> userLevels = new HashMap<>();
    private static final Map<String, Resource> resources = new HashMap<>();
    private static final String DATA_DIR = "C:\\Users\\nadey\\TBD_Boyko\\Data";

    static {
        // Ініціалізація рівнів доступу користувачів
        userLevels.put("Boyko_1", SecurityLevel.LOW);
        userLevels.put("Boyko_2", SecurityLevel.MEDIUM);
        userLevels.put("Boyko_3", SecurityLevel.MEDIUM);
        userLevels.put("Boyko_4", SecurityLevel.HIGH);
        userLevels.put("Boyko_5", SecurityLevel.ADMIN);

        // Ініціалізація ресурсів та їх рівнів безпеки
        initializeResources();
    }

    private static void initializeResources() {
        resources.put("file1.txt", new Resource("file1.txt", 
            Paths.get(DATA_DIR, "file1.txt"), SecurityLevel.LOW, Resource.ResourceType.TEXT_FILE));
        resources.put("file2.txt", new Resource("file2.txt", 
            Paths.get(DATA_DIR, "file2.txt"), SecurityLevel.MEDIUM, Resource.ResourceType.TEXT_FILE));
        resources.put("file3.txt", new Resource("file3.txt", 
            Paths.get(DATA_DIR, "file3.txt"), SecurityLevel.HIGH, Resource.ResourceType.TEXT_FILE));
        resources.put("file4.exe", new Resource("file4.exe", 
            Paths.get(DATA_DIR, "file4.exe"), SecurityLevel.HIGH, Resource.ResourceType.EXECUTABLE));
        resources.put("file5.jpg", new Resource("file5.jpg", 
            Paths.get(DATA_DIR, "file5.jpg"), SecurityLevel.MEDIUM, Resource.ResourceType.IMAGE));
    }

    public static SecurityLevel getUserLevel(String username) {
        return userLevels.getOrDefault(username, SecurityLevel.LOW);
    }

    public static boolean canAccessResource(String username, String resourceName) {
        SecurityLevel userLevel = getUserLevel(username);
        Resource resource = resources.get(resourceName);
        return resource != null && userLevel.canAccess(resource.getSecurityLevel());
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
                    showTextFile(resource.getPath(), parent);
                    break;
                case IMAGE:
                    showImage(resource.getPath(), parent);
                    break;
                case EXECUTABLE:
                    executeFile(resource.getPath(), parent);
                    break;
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(parent, 
                "Помилка при відкритті файлу: " + e.getMessage(), 
                "Помилка", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void showTextFile(Path path, Component parent) throws IOException {
        String content = Files.readString(path);
        JTextArea textArea = new JTextArea(content);
        textArea.setEditable(true);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(400, 300));

        int result = JOptionPane.showConfirmDialog(parent, scrollPane, 
            "Перегляд " + path.getFileName(), 
            JOptionPane.OK_CANCEL_OPTION, 
            JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            Files.writeString(path, textArea.getText());
        }
    }

    private static void showImage(Path path, Component parent) {
        ImageIcon icon = new ImageIcon(path.toString());
        JLabel label = new JLabel(icon);
        JScrollPane scrollPane = new JScrollPane(label);
        scrollPane.setPreferredSize(new Dimension(400, 300));

        JOptionPane.showMessageDialog(parent, scrollPane, 
            "Перегляд " + path.getFileName(), 
            JOptionPane.PLAIN_MESSAGE);
    }

    private static void executeFile(Path path, Component parent) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(path.toString());
        processBuilder.start();
    }

    public static void addNewUser(String username, SecurityLevel level) {
        userLevels.put(username, level);
    }

    public static void addNewResource(String name, Path path, SecurityLevel level, Resource.ResourceType type) {
        resources.put(name, new Resource(name, path, level, type));
    }

    public static Map<String, Resource> getResources() {
        return new HashMap<>(resources);
    }
} 