package edu.kai.stud;

import javax.swing.*;
import java.awt.*;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class DiscretionaryAccessWindow extends JFrame {
    private final String username;
    private final String resourceName;
    private final Map<DiscretionaryAccess.Permission, JCheckBox> permissionCheckboxes;
    private final JTextField timeRestrictionField;

    public DiscretionaryAccessWindow(String username, String resourceName) {
        this.username = username;
        this.resourceName = resourceName;
        this.permissionCheckboxes = new HashMap<>();

        setTitle("Налаштування прав доступу");
        setSize(400, 300);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Інформація про користувача та ресурс
        mainPanel.add(new JLabel("Користувач: " + username));
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(new JLabel("Ресурс: " + resourceName));
        mainPanel.add(Box.createVerticalStrut(20));

        // Панель прав доступу
        JPanel permissionsPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        permissionsPanel.setBorder(BorderFactory.createTitledBorder("Права доступу"));

        Map<String, DiscretionaryAccess> userAccess = SecurityManager.getUserDiscretionaryAccess(username);
        DiscretionaryAccess currentAccess = userAccess.get(resourceName);

        for (DiscretionaryAccess.Permission permission : DiscretionaryAccess.Permission.values()) {
            JCheckBox checkbox = new JCheckBox(permission.getDescription());
            if (currentAccess != null) {
                checkbox.setSelected(currentAccess.hasPermission(permission));
            }
            permissionCheckboxes.put(permission, checkbox);
            permissionsPanel.add(checkbox);
        }

        mainPanel.add(permissionsPanel);
        mainPanel.add(Box.createVerticalStrut(20));

        // Панель часових обмежень
        JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        timePanel.add(new JLabel("Часове обмеження (HH:mm-HH:mm):"));
        timeRestrictionField = new JTextField(10);
        if (currentAccess != null && currentAccess.getTimeRestriction() != null) {
            timeRestrictionField.setText(currentAccess.getTimeRestriction());
        }
        timePanel.add(timeRestrictionField);
        mainPanel.add(timePanel);

        // Кнопки
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Зберегти");
        JButton cancelButton = new JButton("Скасувати");

        saveButton.addActionListener(e -> saveChanges());
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void saveChanges() {
        EnumSet<DiscretionaryAccess.Permission> permissions = EnumSet.noneOf(DiscretionaryAccess.Permission.class);
        
        for (Map.Entry<DiscretionaryAccess.Permission, JCheckBox> entry : permissionCheckboxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                permissions.add(entry.getKey());
            }
        }

        String timeRestriction = timeRestrictionField.getText().trim();
        if (timeRestriction.isEmpty()) {
            timeRestriction = null;
        } else if (!timeRestriction.matches("\\d{2}:\\d{2}-\\d{2}:\\d{2}")) {
            JOptionPane.showMessageDialog(this,
                "Неправильний формат часового обмеження.\nВикористовуйте формат HH:mm-HH:mm",
                "Помилка",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        SecurityManager.updateDiscretionaryAccess(username, resourceName, permissions, timeRestriction);
        dispose();
    }
} 