package edu.kai.stud;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class MainWindow extends JFrame {
    private final String currentUser;
    private final JLabel statusLabel;
    private final JPanel resourcesPanel;

    public MainWindow(String username) {
        this.currentUser = username;

        setTitle("TBD_Boyko - Мандатне розмежування доступу");
        setSize(800, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Створення меню
        createMenuBar();

        // Статус користувача
        SecurityLevel userLevel = SecurityManager.getUserLevel(username);
        String passwordStatus = UserDatabase.getPasswordStrength(username);
        
        statusLabel = new JLabel(String.format("Користувач: %s (Рівень доступу: %s, Пароль: %s)", 
            username, userLevel.getDescription(), passwordStatus), SwingConstants.CENTER);
        add(statusLabel, BorderLayout.NORTH);

        // Панель ресурсів
        resourcesPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        resourcesPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        updateResourcesList();

        JScrollPane scrollPane = new JScrollPane(resourcesPanel);
        add(scrollPane, BorderLayout.CENTER);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // Меню користувача
        JMenu userMenu = new JMenu("Користувач");
        JMenuItem changePasswordItem = new JMenuItem("Змінити пароль");
        JMenuItem logoutItem = new JMenuItem("Вийти");

        changePasswordItem.addActionListener(e -> new ChangePasswordWindow(currentUser));
        logoutItem.addActionListener(e -> logout());

        userMenu.add(changePasswordItem);
        userMenu.add(logoutItem);

        // Меню адміністрування
        JMenu adminMenu = new JMenu("Адміністрування");
        JMenuItem addUserItem = new JMenuItem("Додати користувача");
        JMenuItem changeUserLevelItem = new JMenuItem("Змінити рівень доступу користувача");
        JMenuItem authorInfo = new JMenuItem("Про автора");

        addUserItem.addActionListener(e -> new AddUserWindow());
        changeUserLevelItem.addActionListener(e -> new ChangeSecurityLevelWindow(currentUser));
        authorInfo.addActionListener(e -> 
            JOptionPane.showMessageDialog(this, "Розробила: Boyko, група БІ-125-21-4-БІ"));

        adminMenu.add(addUserItem);
        adminMenu.add(changeUserLevelItem);
        adminMenu.add(authorInfo);

        // Додавання меню до панелі меню
        menuBar.add(userMenu);
        menuBar.add(adminMenu);
        
        setJMenuBar(menuBar);
    }

    private void updateResourcesList() {
        resourcesPanel.removeAll();
        
        // Заголовок таблиці
        JPanel headerPanel = new JPanel(new GridLayout(1, 4));
        headerPanel.add(new JLabel("Назва файлу"));
        headerPanel.add(new JLabel("Тип"));
        headerPanel.add(new JLabel("Рівень доступу"));
        headerPanel.add(new JLabel("Дії"));
        resourcesPanel.add(headerPanel);

        // Додавання ресурсів
        Map<String, Resource> resources = SecurityManager.getResources();
        for (Resource resource : resources.values()) {
            JPanel resourcePanel = new JPanel(new GridLayout(1, 4));
            
            resourcePanel.add(new JLabel(resource.getName()));
            resourcePanel.add(new JLabel(getResourceTypeDescription(resource.getType())));
            resourcePanel.add(new JLabel(resource.getSecurityLevel().getDescription()));
            
            JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            
            JButton accessButton = new JButton("Відкрити");
            accessButton.addActionListener(e -> 
                SecurityManager.viewResource(currentUser, resource.getName(), this));
            
            JButton changeLevelButton = new JButton("Змінити рівень");
            changeLevelButton.addActionListener(e -> 
                new ChangeResourceSecurityWindow(resource.getName()));
            
            if (!SecurityManager.canAccessResource(currentUser, resource.getName())) {
                accessButton.setEnabled(false);
                accessButton.setToolTipText("Недостатньо прав для доступу");
            }
            
            buttonsPanel.add(accessButton);
            buttonsPanel.add(changeLevelButton);
            resourcePanel.add(buttonsPanel);
            
            resourcesPanel.add(resourcePanel);
        }

        resourcesPanel.revalidate();
        resourcesPanel.repaint();
    }

    private String getResourceTypeDescription(Resource.ResourceType type) {
        switch (type) {
            case TEXT_FILE:
                return "Текстовий файл";
            case EXECUTABLE:
                return "Виконуваний файл";
            case IMAGE:
                return "Зображення";
            default:
                return "Невідомий тип";
        }
    }

    private void logout() {
        this.dispose();
        new LoginWindow();
    }

    public static void main(String[] args) {
        UserDatabase.initializeDatabase();
        SwingUtilities.invokeLater(LoginWindow::new);
    }
}
