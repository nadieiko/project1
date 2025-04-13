package edu.kai.stud;

import javax.swing.*;
import java.awt.*;
import java.util.*;

public class AddUserWindow extends JFrame {
    private final JTextField usernameField;
    private final JComboBox<SecurityLevel> securityLevelComboBox;
    private final JComboBox<Role> roleComboBox;
    private final JPasswordField passwordField;
    private final JCheckBox strongPasswordCheckbox;
    private final Map<String, Map<DiscretionaryAccess.Permission, JCheckBox>> resourcePermissions;
    private final Map<String, JTextField> resourceTimeRestrictions;

    public AddUserWindow() {
        resourcePermissions = new HashMap<>();
        resourceTimeRestrictions = new HashMap<>();

        setTitle("Додавання користувача");
        setSize(600, 500);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // Основна панель з прокруткою
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Панель для основної інформації
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(BorderFactory.createTitledBorder("Основна інформація"));

        // Поле для імені користувача
        JPanel usernamePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        usernamePanel.add(new JLabel("Ім'я користувача:"));
        usernameField = new JTextField(20);
        usernamePanel.add(usernameField);
        infoPanel.add(usernamePanel);

        // Поле для пароля
        JPanel passwordPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        passwordPanel.add(new JLabel("Пароль:"));
        passwordField = new JPasswordField(20);
        passwordPanel.add(passwordField);
        infoPanel.add(passwordPanel);

        // Чекбокс для складного пароля
        JPanel passwordStrengthPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        strongPasswordCheckbox = new JCheckBox("Складний пароль");
        passwordStrengthPanel.add(strongPasswordCheckbox);
        infoPanel.add(passwordStrengthPanel);

        // Комбобокс для вибору рівня доступу (тільки для мандатного типу)
        JPanel securityLevelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        securityLevelPanel.add(new JLabel("Рівень доступу:"));
        securityLevelComboBox = new JComboBox<>(SecurityLevel.values());
        securityLevelPanel.add(securityLevelComboBox);
        infoPanel.add(securityLevelPanel);

        // Комбобокс для вибору ролі (тільки для рольового типу)
        JPanel rolePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rolePanel.add(new JLabel("Роль:"));
        roleComboBox = new JComboBox<>(Role.values());
        rolePanel.add(roleComboBox);
        infoPanel.add(rolePanel);

        // Показуємо відповідні елементи в залежності від типу доступу
        securityLevelPanel.setVisible(SecurityManager.getCurrentAccessType() == AccessType.MANDATORY);
        rolePanel.setVisible(SecurityManager.getCurrentAccessType() == AccessType.ROLE_BASED);

        mainPanel.add(infoPanel);

        // Панель для налаштування прав доступу (тільки для дискреційного типу)
        if (SecurityManager.getCurrentAccessType() == AccessType.DISCRETIONARY) {
            JPanel permissionsPanel = new JPanel();
            permissionsPanel.setLayout(new BoxLayout(permissionsPanel, BoxLayout.Y_AXIS));
            permissionsPanel.setBorder(BorderFactory.createTitledBorder("Права доступу до файлів"));

            Map<String, Resource> resources = SecurityManager.getResources();
            for (Resource resource : resources.values()) {
                JPanel resourcePanel = new JPanel();
                resourcePanel.setLayout(new BoxLayout(resourcePanel, BoxLayout.Y_AXIS));
                resourcePanel.setBorder(BorderFactory.createTitledBorder(resource.getName()));

                // Панель для прав доступу
                JPanel checkboxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                Map<DiscretionaryAccess.Permission, JCheckBox> permissions = new HashMap<>();
                
                for (DiscretionaryAccess.Permission permission : DiscretionaryAccess.Permission.values()) {
                    JCheckBox checkbox = new JCheckBox(permission.getDescription());
                    // За замовчуванням встановлюємо права на читання для текстових файлів та зображень
                    if ((resource.getType() == Resource.ResourceType.TEXT_FILE || 
                         resource.getType() == Resource.ResourceType.IMAGE) &&
                        permission == DiscretionaryAccess.Permission.READ) {
                        checkbox.setSelected(true);
                    }
                    permissions.put(permission, checkbox);
                    checkboxPanel.add(checkbox);
                }
                resourcePermissions.put(resource.getName(), permissions);
                resourcePanel.add(checkboxPanel);

                // Поле для часового обмеження
                JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                timePanel.add(new JLabel("Часове обмеження (HH:mm-HH:mm):"));
                JTextField timeField = new JTextField(10);
                resourceTimeRestrictions.put(resource.getName(), timeField);
                timePanel.add(timeField);
                resourcePanel.add(timePanel);

                permissionsPanel.add(resourcePanel);
                permissionsPanel.add(Box.createVerticalStrut(10));
            }

            // Додаємо панель прав до прокручуваної області
            JScrollPane scrollPane = new JScrollPane(permissionsPanel);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            mainPanel.add(scrollPane);
        }

        // Кнопки
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addButton = new JButton("Додати");
        JButton cancelButton = new JButton("Скасувати");

        addButton.addActionListener(e -> addUser());
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(addButton);
        buttonPanel.add(cancelButton);

        // Додаємо все на головне вікно
        JScrollPane mainScrollPane = new JScrollPane(mainPanel);
        add(mainScrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void addUser() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        boolean isStrong = strongPasswordCheckbox.isSelected();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Ім'я користувача та пароль не можуть бути порожніми",
                "Помилка",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (UserDatabase.userExists(username)) {
            JOptionPane.showMessageDialog(this,
                "Користувач з таким іменем вже існує",
                "Помилка",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Перевірка складності пароля
        if (isStrong && !UserDatabase.validateStrongPassword(password)) {
            JOptionPane.showMessageDialog(this,
                "Складний пароль повинен містити:\n" +
                "- Мінімум 8 символів\n" +
                "- Великі та малі літери\n" +
                "- Цифри\n" +
                "- Спеціальні символи",
                "Помилка",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Додаємо користувача в залежності від типу розмежування
        if (SecurityManager.getCurrentAccessType() == AccessType.MANDATORY) {
            SecurityLevel level = (SecurityLevel) securityLevelComboBox.getSelectedItem();
            SecurityManager.addNewUser(username, level);
        } else if (SecurityManager.getCurrentAccessType() == AccessType.ROLE_BASED) {
            Role role = (Role) roleComboBox.getSelectedItem();
            SecurityManager.addNewUser(username, role);
        } else {
            SecurityManager.addNewUser(username);
            
            // Для дискреційного типу встановлюємо права доступу
            if (SecurityManager.getCurrentAccessType() == AccessType.DISCRETIONARY) {
                for (Map.Entry<String, Map<DiscretionaryAccess.Permission, JCheckBox>> entry : 
                     resourcePermissions.entrySet()) {
                    String resourceName = entry.getKey();
                    EnumSet<DiscretionaryAccess.Permission> permissions = EnumSet.noneOf(DiscretionaryAccess.Permission.class);
                    
                    for (Map.Entry<DiscretionaryAccess.Permission, JCheckBox> permEntry : 
                         entry.getValue().entrySet()) {
                        if (permEntry.getValue().isSelected()) {
                            permissions.add(permEntry.getKey());
                        }
                    }
                    
                    String timeRestriction = resourceTimeRestrictions.get(resourceName).getText().trim();
                    if (!timeRestriction.isEmpty() && 
                        !timeRestriction.matches("\\d{2}:\\d{2}-\\d{2}:\\d{2}")) {
                        JOptionPane.showMessageDialog(this,
                            "Неправильний формат часового обмеження для " + resourceName + 
                            "\nВикористовуйте формат HH:mm-HH:mm",
                            "Помилка",
                            JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    
                    SecurityManager.updateDiscretionaryAccess(username, resourceName, 
                        permissions, timeRestriction.isEmpty() ? null : timeRestriction);
                }
            }
        }

        // Додаємо користувача в базу даних
        UserDatabase.addUser(username, password, isStrong ? "Складний" : "Слабкий");

        JOptionPane.showMessageDialog(this,
            "Користувача успішно додано",
            "Успіх",
            JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }
}
