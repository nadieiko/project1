package edu.kai.stud;

import javax.swing.*;
import java.awt.*;

public class AccessTypeWindow extends JFrame {
    private final MainWindow mainWindow;

    public AccessTypeWindow(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        
        setTitle("Вибір типу розмежування доступу");
        setSize(400, 200);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel panel = new JPanel(new GridLayout(4, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel label = new JLabel("Оберіть тип розмежування доступу:", SwingConstants.CENTER);
        panel.add(label);

        ButtonGroup group = new ButtonGroup();
        
        for (AccessType type : AccessType.values()) {
            JRadioButton button = new JRadioButton(type.getDescription());
            button.addActionListener(e -> {
                SecurityManager.setAccessType(type);
                mainWindow.updateAfterAccessTypeChange();
                dispose();
            });
            
            if (type == SecurityManager.getCurrentAccessType()) {
                button.setSelected(true);
            }
            
            group.add(button);
            panel.add(button);
        }

        // Якщо тип ще не вибрано, вікно не можна закрити
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                if (SecurityManager.getCurrentAccessType() != null) {
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(AccessTypeWindow.this,
                        "Будь ласка, оберіть тип розмежування доступу",
                        "Попередження",
                        JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        add(panel, BorderLayout.CENTER);
        setLocationRelativeTo(null);
        setModal(true);
        setVisible(true);
    }

    private void setModal(boolean modal) {
        if (modal) {
            // Блокуємо всі інші вікна
            setModalExclusionType(Dialog.ModalExclusionType.NO_EXCLUDE);
            toFront();
            requestFocus();
        }
    }
} 