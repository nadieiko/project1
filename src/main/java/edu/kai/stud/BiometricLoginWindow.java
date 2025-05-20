package edu.kai.stud;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.AbstractDocument;
import javax.swing.text.DocumentFilter.FilterBypass;

public class BiometricLoginWindow extends JFrame {
    private final FaceAuthenticator faceAuthenticator;
    private final UserDatabase userDatabase;
    
    public BiometricLoginWindow(UserDatabase userDatabase) {
        this.userDatabase = userDatabase;
        this.faceAuthenticator = new FaceAuthenticator();
        
        setTitle("Біометрична автентифікація");
        setSize(400, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        initializeUI();
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout());
        
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        
        JButton startCameraButton = new JButton("Почати сканування обличчя");
        startCameraButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startFaceAuthentication();
            }
        });
        
        JButton registerFaceButton = new JButton("Зареєструвати нове обличчя");
        registerFaceButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                registerNewFace();
            }
        });
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(10, 10, 10, 10);
        mainPanel.add(startCameraButton, gbc);
        
        gbc.gridy = 1;
        mainPanel.add(registerFaceButton, gbc);
        
        add(mainPanel, BorderLayout.CENTER);
    }
    
    private void startFaceAuthentication() {
        // Створюємо діалог для введення ID
        JDialog dialog = new JDialog(this, "Автентифікація", true);
        dialog.setLayout(new BorderLayout());
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        JLabel label = new JLabel("Введіть ID користувача (тільки цифри):");
        JTextField userIdField = new JTextField(10);
        
        // Додаємо DocumentFilter для обмеження введення тільки цифр
        ((AbstractDocument) userIdField.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) 
                    throws BadLocationException {
                if (string.matches("\\d*")) {
                    super.insertString(fb, offset, string, attr);
                }
            }
            
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) 
                    throws BadLocationException {
                if (text.matches("\\d*")) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
        });
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(label, gbc);
        
        gbc.gridy = 1;
        panel.add(userIdField, gbc);
        
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Скасувати");
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        
        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        okButton.addActionListener(e -> {
            String userId = userIdField.getText().trim();
            if (!userId.isEmpty()) {
                dialog.dispose();
                try {
                    faceAuthenticator.startCapture(false, userId, () -> {
                        JOptionPane.showMessageDialog(this,
                                "Автентифікація успішна!",
                                "Успіх",
                                JOptionPane.INFORMATION_MESSAGE);
                        this.dispose();
                    });
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this,
                            "Помилка при автентифікації: " + ex.getMessage(),
                            "Помилка",
                            JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(dialog,
                        "Будь ласка, введіть ID користувача",
                        "Помилка",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    
    private void registerNewFace() {
        // Створюємо власний діалог з текстовим полем
        JDialog dialog = new JDialog(this, "Реєстрація обличчя", true);
        dialog.setLayout(new BorderLayout());
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        JLabel label = new JLabel("Введіть ID користувача (тільки цифри):");
        JTextField userIdField = new JTextField(10);
        
        // Додаємо DocumentFilter для обмеження введення тільки цифр
        ((AbstractDocument) userIdField.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) 
                    throws BadLocationException {
                if (string.matches("\\d*")) {
                    super.insertString(fb, offset, string, attr);
                }
            }
            
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) 
                    throws BadLocationException {
                if (text.matches("\\d*")) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
        });
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(label, gbc);
        
        gbc.gridy = 1;
        panel.add(userIdField, gbc);
        
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Скасувати");
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        
        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        okButton.addActionListener(e -> {
            String userId = userIdField.getText().trim();
            if (!userId.isEmpty()) {
                dialog.dispose();
                try {
                    faceAuthenticator.startCapture(true, userId, () -> {
                        JOptionPane.showMessageDialog(this,
                                "Обличчя успішно зареєстровано!",
                                "Успіх",
                                JOptionPane.INFORMATION_MESSAGE);
                    });
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this,
                            "Помилка при реєстрації: " + ex.getMessage(),
                            "Помилка",
                            JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(dialog,
                        "Будь ласка, введіть ID користувача",
                        "Помилка",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
} 