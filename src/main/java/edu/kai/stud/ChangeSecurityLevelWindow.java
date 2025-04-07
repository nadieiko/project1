package edu.kai.stud;

import javax.swing.*;
import java.awt.*;

public class ChangeSecurityLevelWindow extends JFrame {
    private final String username;
    private final JComboBox<SecurityLevel> securityLevelComboBox;

    public ChangeSecurityLevelWindow(String username) {
        this.username = username;

        setTitle("Зміна рівня доступу користувача");
        setSize(400, 150);
        setLayout(new GridLayout(3, 2, 10, 10));
        setLocationRelativeTo(null);

        add(new JLabel("Користувач:"));
        add(new JLabel(username));

        add(new JLabel("Новий рівень доступу:"));
        securityLevelComboBox = new JComboBox<>(SecurityLevel.values());
        securityLevelComboBox.setSelectedItem(SecurityManager.getUserLevel(username));
        add(securityLevelComboBox);

        JButton changeButton = new JButton("Змінити");
        changeButton.addActionListener(e -> changeSecurityLevel());
        add(changeButton);

        setVisible(true);
    }

    private void changeSecurityLevel() {
        SecurityLevel newLevel = (SecurityLevel) securityLevelComboBox.getSelectedItem();
        if (SecurityManager.changeUserSecurityLevel(username, newLevel)) {
            JOptionPane.showMessageDialog(this,
                    "Рівень доступу користувача " + username + " змінено на " + newLevel.getDescription());
            this.dispose();
        } else {
            JOptionPane.showMessageDialog(this,
                    "Неможливо встановити рівень доступу " + newLevel.getDescription() + 
                    ". Для цього рівня потрібен пароль довжиною більше 10 символів.",
                    "Помилка",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
} 