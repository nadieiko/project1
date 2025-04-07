package edu.kai.stud;

import javax.swing.*;
import java.awt.*;

public class LoginWindow extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;

    public LoginWindow() {
        setTitle("Вхід");
        setSize(300, 200);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new GridLayout(3, 2));

        add(new JLabel("Користувач:"));
        usernameField = new JTextField();
        add(usernameField);

        add(new JLabel("Пароль:"));
        passwordField = new JPasswordField();
        add(passwordField);

        JButton loginButton = new JButton("Увійти");
        loginButton.addActionListener(e -> authenticateUser());
        add(loginButton);

        setVisible(true);
    }

    private void authenticateUser() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        if (UserDatabase.authenticateUser(username, password)) {
            JOptionPane.showMessageDialog(this, "Успішна авторизація! Вітаємо, " + username + "!", "Авторизація", JOptionPane.INFORMATION_MESSAGE);
            new MainWindow(username).setVisible(true);
            this.dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Невірний логін або пароль!", "Помилка", JOptionPane.ERROR_MESSAGE);
        }
    }
}
