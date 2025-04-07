package edu.kai.stud;

import javax.swing.*;
import java.awt.*;

public class AddUserWindow extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JCheckBox strongPasswordCheckbox;
    private JComboBox<SecurityLevel> securityLevelComboBox;

    public AddUserWindow() {
        setTitle("Додати нового користувача");
        setSize(300, 250);
        setLayout(new GridLayout(5, 2));

        add(new JLabel("Ім'я користувача:"));
        usernameField = new JTextField();
        add(usernameField);

        add(new JLabel("Пароль:"));
        passwordField = new JPasswordField();
        add(passwordField);

        add(new JLabel("Рівень доступу:"));
        securityLevelComboBox = new JComboBox<>(SecurityLevel.values());
        add(securityLevelComboBox);

        strongPasswordCheckbox = new JCheckBox("Складний пароль?");
        add(strongPasswordCheckbox);

        JButton addButton = new JButton("Додати");
        addButton.addActionListener(e -> addUser());
        add(addButton);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void addUser() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        boolean isStrong = strongPasswordCheckbox.isSelected();
        SecurityLevel selectedLevel = (SecurityLevel) securityLevelComboBox.getSelectedItem();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Будь ласка, заповніть всі поля",
                    "Помилка",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (isStrong && !UserDatabase.validateStrongPassword(password)) {
            JOptionPane.showMessageDialog(this,
                    "Пароль має бути не менше 8 символів і містити символи хоча б трьох категорій (літери, цифри, спеціальні символи)",
                    "Помилка",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        String passwordStrength = isStrong ? "Складний" : "Слабкий";
        UserDatabase.addUser(username, password, passwordStrength);
        SecurityManager.addNewUser(username, selectedLevel);
        
        JOptionPane.showMessageDialog(this,
                "Користувач " + username + " успішно створений з рівнем доступу " + selectedLevel.getDescription());
        this.dispose();
    }
}
