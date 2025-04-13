package edu.kai.stud;

import javax.swing.*;
import java.awt.*;

public class ChangeSecurityLevelWindow extends JFrame {
    private final JComboBox<SecurityLevel> securityLevelComboBox = new JComboBox<>(SecurityLevel.values());
    private final String username;

    public ChangeSecurityLevelWindow(String username) {
        this.username = username;

        // Перевіряємо тип розмежування
        if (SecurityManager.getCurrentAccessType() != AccessType.MANDATORY) {
            JOptionPane.showMessageDialog(null,
                "Зміна рівня доступу доступна тільки для мандатного типу розмежування",
                "Помилка",
                JOptionPane.ERROR_MESSAGE);
            dispose();
            return;
        }

        setTitle("Зміна рівня доступу");
        setSize(300, 150);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new FlowLayout());
        mainPanel.add(new JLabel("Новий рівень доступу:"));
        securityLevelComboBox.setSelectedItem(SecurityManager.getUserLevel(username));
        mainPanel.add(securityLevelComboBox);

        JButton saveButton = new JButton("Зберегти");
        saveButton.addActionListener(e -> changeLevel());

        add(mainPanel, BorderLayout.CENTER);
        add(saveButton, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void changeLevel() {
        SecurityLevel newLevel = (SecurityLevel) securityLevelComboBox.getSelectedItem();
        if (SecurityManager.changeUserSecurityLevel(username, newLevel)) {
            JOptionPane.showMessageDialog(this,
                "Рівень доступу успішно змінено",
                "Успіх",
                JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this,
                "Помилка при зміні рівня доступу.\nМожливо, пароль користувача недостатньо складний для цього рівня.",
                "Помилка",
                JOptionPane.ERROR_MESSAGE);
        }
    }
} 