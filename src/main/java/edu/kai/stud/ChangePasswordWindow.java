package edu.kai.stud;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ChangePasswordWindow extends JFrame {
    private JPasswordField newPasswordField;
    private JCheckBox strongPasswordCheckbox;
    private String username;

    public ChangePasswordWindow(String username) {
        this.username = username;

        setTitle("Зміна пароля");
        setSize(300, 200);
        setLayout(new GridLayout(4, 2));

        add(new JLabel("Новий пароль:"));
        newPasswordField = new JPasswordField();
        add(newPasswordField);

        strongPasswordCheckbox = new JCheckBox("Складний пароль?");
        strongPasswordCheckbox.setSelected(UserDatabase.getPasswordStrength(username).equals("Складний"));
        add(strongPasswordCheckbox);

        JButton changeButton = new JButton("Змінити пароль");
        changeButton.addActionListener(e -> changePassword());
        add(changeButton);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void changePassword() {
        String newPassword = new String(newPasswordField.getPassword());
        String passwordStrength = strongPasswordCheckbox.isSelected() ? "Складний" : "Слабкий";

        if (UserDatabase.updatePassword(username, newPassword, passwordStrength)) {
            JOptionPane.showMessageDialog(this, "Пароль змінено успішно!");
            this.dispose();
        } else {
            List<String> lastPasswords = UserDatabase.getLastPasswords(username, 3);
            if (lastPasswords.contains(newPassword)) {
                JOptionPane.showMessageDialog(this, 
                    "Цей пароль вже використовувався раніше. Будь ласка, виберіть інший пароль.", 
                    "Помилка", 
                    JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Пароль має бути не менше 8 символів і містити символи хоча б трьох категорій (літери, цифри, спеціальні символи)", 
                    "Помилка", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
