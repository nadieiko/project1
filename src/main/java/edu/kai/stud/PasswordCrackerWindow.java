package edu.kai.stud;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.text.NumberFormat;
import java.util.concurrent.TimeUnit;
import java.util.List;

public class PasswordCrackerWindow extends JFrame {
    private final SecurityManager securityManager;
    private final PasswordCracker passwordCracker;
    private final JTextField usernameField;
    private final JComboBox<PasswordCracker.CrackingScenario> scenarioCombo;
    private final JSpinner lengthSpinner;
    private final JCheckBox lowercaseCheck;
    private final JCheckBox uppercaseCheck;
    private final JCheckBox digitsCheck;
    private final JCheckBox specialCheck;
    private final JCheckBox cyrillicLowerCheck;
    private final JCheckBox cyrillicUpperCheck;
    private final JTextArea resultArea;
    private final JButton startButton;
    private final JButton stopButton;
    private SwingWorker<PasswordCracker.CrackingResult, Void> currentWorker;
    private Timer statsTimer;
    private long lastUpdateTime;
    private long lastCombinationsTried;
    private long startTime;
    private String foundPassword;
    private boolean shouldStop;

    public PasswordCrackerWindow(SecurityManager securityManager) {
        this.securityManager = securityManager;
        this.passwordCracker = new PasswordCracker(securityManager);

        setTitle("Password Cracker");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setSize(800, 600);

        // Панель налаштувань
        JPanel settingsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Поле для імені користувача
        gbc.gridx = 0;
        gbc.gridy = 0;
        settingsPanel.add(new JLabel("Ім'я користувача:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        usernameField = new JTextField(20);
        settingsPanel.add(usernameField, gbc);

        // Комбобокс для вибору сценарію
        gbc.gridx = 0;
        gbc.gridy = 1;
        settingsPanel.add(new JLabel("Сценарій:"), gbc);

        gbc.gridx = 1;
        scenarioCombo = new JComboBox<>(PasswordCracker.CrackingScenario.values());
        scenarioCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, 
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof PasswordCracker.CrackingScenario) {
                    setText(((PasswordCracker.CrackingScenario) value).getDescription());
                }
                return this;
            }
        });
        scenarioCombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateUIForScenario((PasswordCracker.CrackingScenario) e.getItem());
            }
        });
        settingsPanel.add(scenarioCombo, gbc);

        // Спіннер для довжини пароля
        gbc.gridx = 0;
        gbc.gridy = 2;
        settingsPanel.add(new JLabel("Довжина пароля:"), gbc);

        gbc.gridx = 1;
        lengthSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 20, 1));
        settingsPanel.add(lengthSpinner, gbc);

        // Чекбокси для наборів символів
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        JPanel charsetPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        charsetPanel.setBorder(BorderFactory.createTitledBorder("Набори символів"));
        
        lowercaseCheck = new JCheckBox("Малі латинські (a-z)", true);
        uppercaseCheck = new JCheckBox("Великі латинські (A-Z)", true);
        digitsCheck = new JCheckBox("Цифри (0-9)", true);
        specialCheck = new JCheckBox("Спеціальні (!@#$...)", false);
        cyrillicLowerCheck = new JCheckBox("Малі кириличні (а-я)", false);
        cyrillicUpperCheck = new JCheckBox("Великі кириличні (А-Я)", false);

        charsetPanel.add(lowercaseCheck);
        charsetPanel.add(uppercaseCheck);
        charsetPanel.add(digitsCheck);
        charsetPanel.add(specialCheck);
        charsetPanel.add(cyrillicLowerCheck);
        charsetPanel.add(cyrillicUpperCheck);
        settingsPanel.add(charsetPanel, gbc);

        // Панель кнопок
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        startButton = new JButton("Почати підбір");
        startButton.addActionListener(this::startCracking);
        buttonPanel.add(startButton);

        stopButton = new JButton("Зупинити");
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> stopCracking());
        buttonPanel.add(stopButton);

        settingsPanel.add(buttonPanel, gbc);

        add(settingsPanel, BorderLayout.NORTH);

        // Область результатів
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(resultArea);
        add(scrollPane, BorderLayout.CENTER);

        // Початкове оновлення UI
        updateUIForScenario(PasswordCracker.CrackingScenario.NO_INFO);
    }

    private void updateUIForScenario(PasswordCracker.CrackingScenario scenario) {
        boolean showCharsets = scenario == PasswordCracker.CrackingScenario.EXACT_LENGTH_AND_CHARSET ||
                             scenario == PasswordCracker.CrackingScenario.APPROXIMATE_LENGTH_AND_CHARSET;
        
        boolean showLength = scenario != PasswordCracker.CrackingScenario.NO_INFO;

        lowercaseCheck.setEnabled(showCharsets);
        uppercaseCheck.setEnabled(showCharsets);
        digitsCheck.setEnabled(showCharsets);
        specialCheck.setEnabled(showCharsets);
        cyrillicLowerCheck.setEnabled(showCharsets);
        cyrillicUpperCheck.setEnabled(showCharsets);
        lengthSpinner.setEnabled(showLength);

        if (!showCharsets) {
            lowercaseCheck.setSelected(true);
            uppercaseCheck.setSelected(true);
            digitsCheck.setSelected(true);
            specialCheck.setSelected(true);
            cyrillicLowerCheck.setSelected(true);
            cyrillicUpperCheck.setSelected(true);
        }
    }

    private void startCracking(ActionEvent e) {
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Будь ласка, введіть ім'я користувача", 
                "Помилка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!UserDatabase.userExists(username)) {
            JOptionPane.showMessageDialog(this, "Користувач не існує", 
                "Помилка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        PasswordCracker.CrackingScenario scenario = 
            (PasswordCracker.CrackingScenario) scenarioCombo.getSelectedItem();
            
        int length = (scenario == PasswordCracker.CrackingScenario.NO_INFO) ? 
            1 : (int) lengthSpinner.getValue();

        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        scenarioCombo.setEnabled(false);
        lengthSpinner.setEnabled(false);
        resultArea.setText("Починаємо підбір пароля...\n");

        startTime = System.currentTimeMillis();
        lastUpdateTime = startTime;
        lastCombinationsTried = 0;

        // Створюємо таймер для оновлення статистики
        statsTimer = new Timer(1000, evt -> {
            if (currentWorker != null && !currentWorker.isDone()) {
                long currentTime = System.currentTimeMillis();
                long currentCombinations = passwordCracker.getCombinationsTried();
                long timeDiff = currentTime - lastUpdateTime;
                long combinationsDiff = currentCombinations - lastCombinationsTried;
                
                long speed = (timeDiff > 0) ? combinationsDiff * 1000 / timeDiff : 0;
                
                SwingUtilities.invokeLater(() -> {
                    resultArea.append(String.format(
                        "\rПеребрано: %s | Швидкість: %s паролів/сек | Час: %s\n",
                        NumberFormat.getInstance().format(currentCombinations),
                        NumberFormat.getInstance().format(speed),
                        formatDuration(currentTime - startTime)
                    ));
                });
                
                lastUpdateTime = currentTime;
                lastCombinationsTried = currentCombinations;
            }
        });
        statsTimer.start();

        currentWorker = new SwingWorker<PasswordCracker.CrackingResult, Void>() {
            @Override
            protected PasswordCracker.CrackingResult doInBackground() {
                PasswordCracker.PasswordConstraints constraints = 
                    new PasswordCracker.PasswordConstraints(
                        scenario, length,
                        lowercaseCheck.isSelected(),
                        uppercaseCheck.isSelected(),
                        digitsCheck.isSelected(),
                        specialCheck.isSelected(),
                        cyrillicLowerCheck.isSelected(),
                        cyrillicUpperCheck.isSelected()
                    );
                return passwordCracker.crackPassword(username, constraints);
            }

            @Override
            protected void done() {
                if (statsTimer != null) {
                    statsTimer.stop();
                }
                try {
                    PasswordCracker.CrackingResult result = get();
                    String password = result.getPassword();
                    long duration = result.getDuration();
                    long combinationsTried = result.getCombinationsTried();
                    long totalCombinations = result.getTotalCombinations();

                    StringBuilder sb = new StringBuilder();
                    sb.append("\nПідбір завершено!\n");
                    sb.append("Загальна кількість можливих комбінацій: ")
                      .append(NumberFormat.getInstance().format(totalCombinations)).append("\n");
                    sb.append("Перевірено комбінацій: ")
                      .append(NumberFormat.getInstance().format(combinationsTried)).append("\n");
                    sb.append("Середня швидкість підбору: ")
                      .append(NumberFormat.getInstance().format(combinationsTried * 1000L / duration))
                      .append(" паролів/сек\n");
                    sb.append("Витрачений час: ").append(formatDuration(duration)).append("\n");
                    
                    if (password != null) {
                        foundPassword = password;
                        sb.append("Знайдений пароль: ").append(password).append("\n");
                    } else if (shouldStop) {
                        sb.append("Підбір було зупинено користувачем\n");
                    } else {
                        sb.append("Пароль не знайдено\n");
                    }

                    resultArea.append(sb.toString());
                } catch (Exception ex) {
                    resultArea.append("\nПомилка: " + ex.getMessage() + "\n");
                } finally {
                    startButton.setEnabled(true);
                    stopButton.setEnabled(false);
                    scenarioCombo.setEnabled(true);
                    lengthSpinner.setEnabled(true);
                    currentWorker = null;
                }
            }
        };

        currentWorker.execute();
    }

    private void stopCracking() {
        if (currentWorker != null && !currentWorker.isDone()) {
            passwordCracker.stopCracking();
            resultArea.append("\nЗупинка підбору...\n");
        }
    }

    private String formatDuration(long millis) {
        return String.format("%02d:%02d:%02d.%03d",
                TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1),
                millis % 1000
        );
    }
} 