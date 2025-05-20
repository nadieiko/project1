package edu.kai.stud;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class UserDatabase {
    private static final String DB_URL = "jdbc:sqlite:users.db";
    private static final Map<String, String> passwordCache = new ConcurrentHashMap<>();
    private static Connection permanentConnection;
    private static PreparedStatement cachedAuthStatement;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
            permanentConnection = DriverManager.getConnection(DB_URL);
            initializeDatabase();
            cachedAuthStatement = permanentConnection.prepareStatement(
                "SELECT password FROM users WHERE username = ?");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void initializeDatabase() {
        try {
            try (Statement stmt = permanentConnection.createStatement()) {
                // Починаємо транзакцію
                permanentConnection.setAutoCommit(false);
                
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

                // Створюємо індекс для оптимізації пошуку
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_users_username ON users(username)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_password_history_user_id ON password_history(user_id)");

                // Перевіряємо, чи таблиця користувачів порожня
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
                if (rs.next() && rs.getInt(1) == 0) {
                    // Додаємо тестових користувачів тільки якщо таблиця порожня
                    try (PreparedStatement pstmt = permanentConnection.prepareStatement(
                            "INSERT INTO users (username, password, is_strong_password) VALUES (?, ?, ?)")) {
                        
                        String[][] testUsers = {
                            {"Boyko_1", "default", "Слабкий"},
                            {"Boyko_2", "default", "Слабкий"},
                            {"Boyko_3", "default", "Слабкий"},
                            {"Boyko_4", "default", "Слабкий"},
                            {"Boyko_5", "default", "Слабкий"}
                        };

                        for (String[] user : testUsers) {
                            pstmt.setString(1, user[0]);
                            pstmt.setString(2, user[1]);
                            pstmt.setString(3, user[2]);
                            pstmt.executeUpdate();
                            
                            // Додаємо пароль в історію
                            try (PreparedStatement histStmt = permanentConnection.prepareStatement(
                                    "INSERT INTO password_history (user_id, password) " +
                                    "SELECT id, ? FROM users WHERE username = ?")) {
                                histStmt.setString(1, user[1]);
                                histStmt.setString(2, user[0]);
                                histStmt.executeUpdate();
                            }
                        }
                    }
                }
                
                // Завершуємо транзакцію
                permanentConnection.commit();
                permanentConnection.setAutoCommit(true);
            }
        } catch (Exception e) {
            try {
                permanentConnection.rollback();
                permanentConnection.setAutoCommit(true);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
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
                // Очищаємо кеш для цього користувача
                passwordCache.remove(username);
                return true;
            }
            return false;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static synchronized boolean authenticateUser(String username, String password) {
        try {
            // Спочатку перевіряємо кеш
            String cachedPassword = passwordCache.get(username);
            if (cachedPassword != null) {
                return cachedPassword.equals(password);
            }

            // Якщо в кеші немає, перевіряємо в базі
            cachedAuthStatement.setString(1, username);
            try (ResultSet rs = cachedAuthStatement.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString("password");
                    if (storedPassword != null) {
                        // Зберігаємо в кеш
                        passwordCache.put(username, storedPassword);
                        return storedPassword.equals(password);
                    }
                }
            }
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void clearPasswordCache() {
        passwordCache.clear();
    }

    public static void clearUserPasswordCache(String username) {
        passwordCache.remove(username);
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

    public static Integer getUserIdByUsername(String username) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT id FROM users WHERE username = ?")) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
            return null;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            if (cachedAuthStatement != null) {
                cachedAuthStatement.close();
            }
            if (permanentConnection != null) {
                permanentConnection.close();
            }
        } finally {
            super.finalize();
        }
    }
}
