package edu.kai.stud;

import javax.swing.*;
import java.awt.*;

public class LoginWindow extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private final UserDatabase userDatabase;
    private final FaceAuthenticator faceAuthenticator;

    public LoginWindow() {
        this.userDatabase = new UserDatabase();
        this.faceAuthenticator = new FaceAuthenticator();
        
        setTitle("Вхід");
        setSize(300, 200);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Панель для традиційного входу
        JPanel loginPanel = new JPanel(new GridBagLayout());
        loginPanel.setBorder(BorderFactory.createTitledBorder("Вхід в систему"));

        gbc.gridx = 0;
        gbc.gridy = 0;
        loginPanel.add(new JLabel("Користувач:"), gbc);

        gbc.gridx = 1;
        usernameField = new JTextField(15);
        loginPanel.add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        loginPanel.add(new JLabel("Пароль:"), gbc);

        gbc.gridx = 1;
        passwordField = new JPasswordField(15);
        loginPanel.add(passwordField, gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        JButton loginButton = new JButton("Увійти");
        loginButton.addActionListener(e -> authenticateUser());
        loginPanel.add(loginButton, gbc);

        // Додаю кнопку для входу через розпізнавання обличчя
        gbc.gridx = 1;
        gbc.gridy = 3;
        JButton faceLoginButton = new JButton("Вхід через обличчя");
        faceLoginButton.addActionListener(e -> authenticateByFace());
        loginPanel.add(faceLoginButton, gbc);

        // Додаємо панель входу
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(loginPanel, gbc);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void authenticateUser() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        if (userDatabase.authenticateUser(username, password)) {
            // Одразу відкриваємо головне вікно без додаткової перевірки обличчя
            JOptionPane.showMessageDialog(this, 
                "Вітаємо, " + username + "!", 
                "Авторизація", 
                JOptionPane.INFORMATION_MESSAGE);
            new MainWindow(username).setVisible(true);
            this.dispose();
        } else {
            JOptionPane.showMessageDialog(this, 
                "Невірний логін або пароль!", 
                "Помилка", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    // Додаю новий метод для автентифікації через обличчя
    private void authenticateByFace() {
        String username = JOptionPane.showInputDialog(this, "Введіть ваш логін для розпізнавання обличчя:");
        if (username == null || username.trim().isEmpty()) {
            return;
        }
        Integer userId = UserDatabase.getUserIdByUsername(username);
        if (userId == null) {
            JOptionPane.showMessageDialog(this, "Користувача з таким логіном не знайдено!", "Помилка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            faceAuthenticator.startCapture(false, String.valueOf(userId), () -> {
                JOptionPane.showMessageDialog(this,
                    "Автентифікація за обличчям успішна! Вітаємо, " + username + "!",
                    "Авторизація",
                    JOptionPane.INFORMATION_MESSAGE);
                new MainWindow(username).setVisible(true);
                this.dispose();
            });
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Помилка при перевірці обличчя: " + e.getMessage(),
                "Помилка",
                JOptionPane.ERROR_MESSAGE);
        }
    }
}
