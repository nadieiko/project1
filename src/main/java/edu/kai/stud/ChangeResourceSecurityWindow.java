package edu.kai.stud;

import javax.swing.*;
import java.awt.*;

public class ChangeResourceSecurityWindow extends JFrame {
    private final String resourceName;
    private final JComboBox<SecurityLevel> securityLevelComboBox;

    public ChangeResourceSecurityWindow(String resourceName) {
        this.resourceName = resourceName;

        setTitle("Зміна мітки конфіденційності файлу");
        setSize(400, 150);
        setLayout(new GridLayout(3, 2, 10, 10));
        setLocationRelativeTo(null);

        add(new JLabel("Файл:"));
        add(new JLabel(resourceName));

        add(new JLabel("Нова мітка конфіденційності:"));
        securityLevelComboBox = new JComboBox<>(SecurityLevel.values());
        Resource resource = SecurityManager.getResources().get(resourceName);
        if (resource != null) {
            securityLevelComboBox.setSelectedItem(resource.getSecurityLevel());
        }
        add(securityLevelComboBox);

        JButton changeButton = new JButton("Змінити");
        changeButton.addActionListener(e -> changeSecurityLevel());
        add(changeButton);

        setVisible(true);
    }

    private void changeSecurityLevel() {
        SecurityLevel newLevel = (SecurityLevel) securityLevelComboBox.getSelectedItem();
        if (SecurityManager.changeResourceSecurityLevel(resourceName, newLevel)) {
            JOptionPane.showMessageDialog(this,
                    "Мітку конфіденційності файлу " + resourceName + " змінено на " + newLevel.getDescription());
            this.dispose();
        } else {
            JOptionPane.showMessageDialog(this,
                    "Помилка при зміні мітки конфіденційності файлу",
                    "Помилка",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
} 