package edu.kai.stud;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class MainWindow extends JFrame {
    private final String currentUser;
    private JLabel statusLabel;
    private final JPanel resourcesPanel;

    public MainWindow(String username) {
        this.currentUser = username;

        setTitle("TBD_Boyko - Система розмежування доступу");
        setSize(800, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Створення меню
        createMenuBar();

        // Статус користувача
        statusLabel = new JLabel();
        add(statusLabel, BorderLayout.NORTH);
        
        // Панель ресурсів
        resourcesPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        resourcesPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(resourcesPanel);
        add(scrollPane, BorderLayout.CENTER);

        setLocationRelativeTo(null);
        setVisible(true);

        // Якщо тип розмежування не вибрано, показуємо вікно вибору
        if (SecurityManager.getCurrentAccessType() == null) {
            SwingUtilities.invokeLater(() -> new AccessTypeWindow(this));
        } else {
            updateAfterAccessTypeChange();
        }
    }

    public void updateAfterAccessTypeChange() {
        updateStatusLabel();
        updateResourcesList();
    }

    private void updateStatusLabel() {
        String status = String.format("Користувач: %s", currentUser);
        
        AccessType currentType = SecurityManager.getCurrentAccessType();
        if (currentType != null) {
            status += String.format(" | Тип розмежування: %s", currentType.getDescription());
            
            if (currentType == AccessType.MANDATORY) {
                SecurityLevel userLevel = SecurityManager.getUserLevel(currentUser);
                String passwordStatus = UserDatabase.getPasswordStrength(currentUser);
                status += String.format(" | Рівень доступу: %s | Пароль: %s", 
                    userLevel.getDescription(), passwordStatus);
            }
        } else {
            status += " | Тип розмежування не вибрано";
        }
        
        statusLabel.setText(status);
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Меню користувача
        JMenu userMenu = new JMenu("Користувач");
        JMenuItem changePasswordItem = new JMenuItem("Змінити пароль");
        JMenuItem changeAccessTypeItem = new JMenuItem("Змінити тип розмежування");
        JMenuItem logoutItem = new JMenuItem("Вийти");

        changePasswordItem.addActionListener(e -> new ChangePasswordWindow(currentUser));
        changeAccessTypeItem.addActionListener(e -> {
            new AccessTypeWindow(this);
        });
        logoutItem.addActionListener(e -> logout());

        userMenu.add(changePasswordItem);
        userMenu.add(changeAccessTypeItem);
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

        // Якщо тип розмежування не вибрано, показуємо повідомлення
        if (SecurityManager.getCurrentAccessType() == null) {
            resourcesPanel.add(new JLabel("Оберіть тип розмежування доступу для перегляду ресурсів", 
                SwingConstants.CENTER));
            resourcesPanel.revalidate();
            resourcesPanel.repaint();
            return;
        }

        // Заголовок таблиці
        JPanel headerPanel = new JPanel(new GridLayout(1, 4));
        headerPanel.add(new JLabel("Назва файлу"));
        headerPanel.add(new JLabel("Тип"));
        
        if (SecurityManager.getCurrentAccessType() == AccessType.MANDATORY) {
            headerPanel.add(new JLabel("Рівень доступу"));
        } else if (SecurityManager.getCurrentAccessType() == AccessType.DISCRETIONARY) {
            headerPanel.add(new JLabel("Права доступу"));
        } else if (SecurityManager.getCurrentAccessType() == AccessType.ROLE_BASED) {
            headerPanel.add(new JLabel("Роль та права"));
        }
        
        headerPanel.add(new JLabel("Дії"));
        resourcesPanel.add(headerPanel);

        // Додавання ресурсів
        Map<String, Resource> resources = SecurityManager.getResources();
        for (Resource resource : resources.values()) {
            JPanel resourcePanel = new JPanel(new GridLayout(1, 4));

            resourcePanel.add(new JLabel(resource.getName()));
            resourcePanel.add(new JLabel(getResourceTypeDescription(resource.getType())));

            // Відображення прав доступу в залежності від типу розмежування
            if (SecurityManager.getCurrentAccessType() == AccessType.MANDATORY) {
                resourcePanel.add(new JLabel(resource.getSecurityLevel().getDescription()));
            } else if (SecurityManager.getCurrentAccessType() == AccessType.DISCRETIONARY) {
                Map<String, DiscretionaryAccess> userAccess = SecurityManager.getUserDiscretionaryAccess(currentUser);
                DiscretionaryAccess access = userAccess.get(resource.getName());
                
                StringBuilder rights = new StringBuilder();
                if (access != null) {
                    for (DiscretionaryAccess.Permission permission : access.getPermissions()) {
                        if (rights.length() > 0) rights.append(", ");
                        rights.append(permission.getDescription());
                    }
                    if (access.getTimeRestriction() != null) {
                        rights.append(" (").append(access.getTimeRestriction()).append(")");
                    }
                }
                resourcePanel.add(new JLabel(rights.toString()));
            } else if (SecurityManager.getCurrentAccessType() == AccessType.ROLE_BASED) {
                Map<String, RoleBasedAccess> userAccess = SecurityManager.getUserRoleBasedAccess(currentUser);
                RoleBasedAccess access = userAccess.get(resource.getName());
                
                StringBuilder rights = new StringBuilder();
                if (access != null) {
                    rights.append(access.getRole().getDescription()).append(": ");
                    for (RoleBasedAccess.Permission permission : access.getPermissions()) {
                        if (rights.length() > access.getRole().getDescription().length() + 2) 
                            rights.append(", ");
                        rights.append(permission.getDescription());
                    }
                    if (access.getTimeRestriction() != null) {
                        rights.append(" (").append(access.getTimeRestriction()).append(")");
                    }
                }
                resourcePanel.add(new JLabel(rights.toString()));
            }

            JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

            JButton accessButton = new JButton("Відкрити");
            accessButton.addActionListener(e ->
                    SecurityManager.viewResource(currentUser, resource.getName(), this));

            if (!SecurityManager.canAccessResource(currentUser, resource.getName())) {
                accessButton.setEnabled(false);
                accessButton.setToolTipText("Недостатньо прав для доступу");
            }

            buttonsPanel.add(accessButton);

            // Додавання кнопки налаштування прав для дискреційного доступу
            if (SecurityManager.getCurrentAccessType() == AccessType.DISCRETIONARY) {
                JButton rightsButton = new JButton("Права");
                rightsButton.addActionListener(e -> {
                    new DiscretionaryAccessWindow(currentUser, resource.getName());
                    updateResourcesList();
                });
                buttonsPanel.add(rightsButton);
            } else if (SecurityManager.getCurrentAccessType() == AccessType.MANDATORY) {
                // Кнопка зміни рівня доступу для мандатного типу
                JButton changeLevelButton = new JButton("Змінити рівень");
                changeLevelButton.addActionListener(e ->
                        new ChangeResourceSecurityWindow(resource.getName()));
                buttonsPanel.add(changeLevelButton);
            }

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
