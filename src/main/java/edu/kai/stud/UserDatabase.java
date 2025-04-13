package edu.kai.stud;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class UserDatabase {
    private static final String DB_URL = "jdbc:sqlite:users.db";

    public static void initializeDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 Statement stmt = conn.createStatement()) {

                String createUsersTableSQL = "CREATE TABLE IF NOT EXISTS users (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "username TEXT UNIQUE, " +
                        "password TEXT, " +
                        "is_strong_password TEXT)";
                stmt.execute(createUsersTableSQL);

                String createPasswordHistoryTableSQL = "CREATE TABLE IF NOT EXISTS password_history (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "user_id INTEGER, " +
                        "password TEXT, " +
                        "change_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "FOREIGN KEY (user_id) REFERENCES users(id))";
                stmt.execute(createPasswordHistoryTableSQL);

                // Додавання тестових користувачів
                addUser("Boyko_1", "default", "Слабкий");
                addUser("Boyko_2", "default", "Слабкий");
                addUser("Boyko_3", "default", "Слабкий");
                addUser("Boyko_4", "default", "Слабкий");
                addUser("Boyko_5", "default", "Слабкий");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean userExists(String username) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT 1 FROM users WHERE username = ?")) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void addUser(String username, String password) {
        addUser(username, password, "Слабкий");
    }

    public static void addUser(String username, String password, String passwordStrength) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT OR IGNORE INTO users (username, password, is_strong_password) VALUES (?, ?, ?)")) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, passwordStrength);
            pstmt.executeUpdate();
            
            // Додаємо поточний пароль в історію
            addPasswordToHistory(username, password);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void addPasswordToHistory(String username, String password) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO password_history (user_id, password) " +
                     "SELECT id, ? FROM users WHERE username = ?")) {
            pstmt.setString(1, password);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<String> getLastPasswords(String username, int count) {
        List<String> passwords = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT password FROM password_history " +
                     "WHERE user_id = (SELECT id FROM users WHERE username = ?) " +
                     "ORDER BY change_date DESC LIMIT ?")) {
            pstmt.setString(1, username);
            pstmt.setInt(2, count);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                passwords.add(rs.getString("password"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return passwords;
    }

    public static boolean updatePassword(String username, String newPassword, String passwordStrength) {
        // Перевіряємо, чи не використовувався цей пароль раніше
        List<String> lastPasswords = getLastPasswords(username, 3);
        if (lastPasswords.contains(newPassword)) {
            return false;
        }

        if (passwordStrength.equals("Складний")) {
            boolean isValid = validateStrongPassword(newPassword);
            if (!isValid) {
                return false;
            }
        }

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(
                     "UPDATE users SET password = ?, is_strong_password = ? WHERE username = ?")) {

            pstmt.setString(1, newPassword);
            pstmt.setString(2, passwordStrength);
            pstmt.setString(3, username);
            int updatedRows = pstmt.executeUpdate();
            
            if (updatedRows > 0) {
                // Додаємо новий пароль в історію
                addPasswordToHistory(username, newPassword);
                return true;
            }
            return false;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean authenticateUser(String username, String password) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT * FROM users WHERE username = ? AND password = ?")) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String getPasswordStrength(String username) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT is_strong_password FROM users WHERE username = ?")) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("is_strong_password");
            }
            return "Слабкий";
        } catch (SQLException e) {
            e.printStackTrace();
            return "Слабкий";
        }
    }

    static boolean validateStrongPassword(String password) {
        return Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\",.<>?/]).{8,40}$")
                .matcher(password.trim())
                .matches();
    }

    public static String getUserPassword(String username) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT password FROM users WHERE username = ?")) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("password");
            }
            return null;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
