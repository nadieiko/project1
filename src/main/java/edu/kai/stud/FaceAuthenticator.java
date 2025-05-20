package edu.kai.stud;

import org.bytedeco.javacv.*;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_face.*;
import org.bytedeco.opencv.opencv_objdetect.*;
import org.bytedeco.javacpp.*;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.bytedeco.opencv.global.opencv_objdetect.*;
import static org.bytedeco.opencv.global.opencv_face.*;

import java.nio.IntBuffer;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

public class FaceAuthenticator {
    private static final String MODEL_PATH = "src/main/resources/face_models/face_model.xml";
    private static final String USER_MAPPING_PATH = "src/main/resources/face_models/user_mapping.txt";
    private static final String CASCADE_FILE = "haarcascade_frontalface_default.xml";
    private static final String MODELS_DIR = "face_models";
    private static final int FACE_SAMPLES = 5;
    private static final double CONFIDENCE_THRESHOLD = 3000.0;
    
    private FaceRecognizer faceRecognizer;
    private CascadeClassifier faceDetector;
    private FrameGrabber grabber;
    private final Map<String, Integer> userIdMapping;
    private final List<Mat> faceSamples;
    private boolean isCapturing;
    private JFrame captureWindow;
    private JLabel statusLabel;
    private JLabel imageLabel;
    private JButton actionButton;
    private int currentSample;
    private boolean isRegistrationMode;
    private String currentUserId;
    private Runnable onSuccessCallback;
    private Mat capturedFace = null;
    private final Object faceLock = new Object();
    
    public FaceAuthenticator() {
        // Ініціалізуємо детектор обличчя
        faceDetector = new CascadeClassifier();
        try {
            InputStream is = getClass().getResourceAsStream("/" + CASCADE_FILE);
            if (is == null) {
                throw new RuntimeException("Не вдалося знайти файл класифікатора в ресурсах");
            }
            
            Path tempFile = Files.createTempFile("cascade", ".xml");
            Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
            is.close();
            
            if (!faceDetector.load(tempFile.toString())) {
                throw new RuntimeException("Не вдалося завантажити каскадний класифікатор");
            }
            
            Files.delete(tempFile);
        } catch (Exception e) {
            throw new RuntimeException("Помилка при ініціалізації детектора: " + e.getMessage(), e);
        }
        
        // Ініціалізуємо розпізнавач обличчя
        faceRecognizer = EigenFaceRecognizer.create();
        
        // Ініціалізуємо інші поля
        grabber = new OpenCVFrameGrabber(0);
        userIdMapping = new HashMap<>();
        faceSamples = new ArrayList<>();
        
        // Завантажуємо збережену модель та маппінг, якщо вони існують
        loadModel();
    }
    
    private void loadExistingModels() {
        try {
            Path modelsDirPath = Paths.get("src", "main", "resources", MODELS_DIR);
            if (!Files.exists(modelsDirPath)) {
                Files.createDirectories(modelsDirPath);
            }
            
            // Завантажуємо основну модель, якщо вона існує
            Path mainModelPath = modelsDirPath.resolve("face_model.xml");
            if (Files.exists(mainModelPath)) {
                faceRecognizer.read(mainModelPath.toString());
            }
        } catch (Exception e) {
            System.err.println("Помилка при завантаженні моделей: " + e.getMessage());
        }
    }
    
    public void startCapture(boolean isRegistration, String userId, Runnable onSuccess) throws Exception {
        this.isRegistrationMode = isRegistration;
        this.currentUserId = userId;
        this.onSuccessCallback = onSuccess;
        this.currentSample = 0;
        this.faceSamples.clear();
        
        grabber = new OpenCVFrameGrabber(0);
        grabber.start();
        
        captureWindow = new JFrame(isRegistration ? "Реєстрація обличчя" : "Автентифікація");
        captureWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        captureWindow.setLayout(new BorderLayout());
        
        // Змінна для зберігання поточного кадру
        final java.awt.image.BufferedImage[] currentFrame = new java.awt.image.BufferedImage[1];
        
        // Створюємо панель для відображення відео
        JPanel videoPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (currentFrame[0] != null) {
                    g.drawImage(currentFrame[0], 0, 0, getWidth(), getHeight(), null);
                }
            }
        };
        videoPanel.setPreferredSize(new Dimension(640, 480));
        
        // Створюємо панель керування
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout());
        
        final JButton actionButton = new JButton(isRegistration ? "Зберегти" : "Перевірити");
        actionButton.setEnabled(false);
        
        final JLabel statusLabel = new JLabel("Очікування виявлення обличчя...");
        statusLabel.setForeground(Color.RED);
        
        // Додаємо лічильник зразків для реєстрації
        final JLabel samplesLabel = new JLabel("");
        if (isRegistration) {
            samplesLabel.setText("Зразок 0/" + FACE_SAMPLES);
        }
        
        controlPanel.add(statusLabel);
        controlPanel.add(samplesLabel);
        controlPanel.add(actionButton);
        
        captureWindow.add(videoPanel, BorderLayout.CENTER);
        captureWindow.add(controlPanel, BorderLayout.SOUTH);
        
        captureWindow.pack();
        captureWindow.setLocationRelativeTo(null);
        captureWindow.setVisible(true);
        
        AtomicBoolean isRunning = new AtomicBoolean(true);
        AtomicBoolean faceDetected = new AtomicBoolean(false);
        
        actionButton.addActionListener(e -> {
            if (isRegistration && capturedFace != null) {
                faceSamples.add(capturedFace.clone());
                samplesLabel.setText("Зразок " + faceSamples.size() + "/" + FACE_SAMPLES);
                
                if (faceSamples.size() >= FACE_SAMPLES) {
                    registerNewFace(userId, faceSamples);
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                    isRunning.set(false);
                    captureWindow.dispose();
                }
            } else if (!isRegistration && capturedFace != null) {
                String recognizedUserId = recognizeFace(capturedFace);
                System.out.println("Очікуваний ID: " + userId + ", Розпізнаний ID: " + recognizedUserId);
                
                if (recognizedUserId != null) {
                    if (recognizedUserId.equals(userId)) {
                        statusLabel.setText("Автентифікація успішна!");
                        statusLabel.setForeground(Color.GREEN);
                        if (onSuccess != null) {
                            onSuccess.run();
                        }
                        isRunning.set(false);
                        captureWindow.dispose();
                    } else {
                        statusLabel.setText("Обличчя не співпадає з зареєстрованим! (ID: " + recognizedUserId + ")");
                        statusLabel.setForeground(Color.RED);
                    }
                } else {
                    statusLabel.setText("Обличчя не розпізнано!");
                    statusLabel.setForeground(Color.RED);
                }
            }
        });
        
        // Створюємо окремий потік для обробки відео
        Thread videoThread = new Thread(() -> {
            try {
                while (isRunning.get() && captureWindow.isVisible()) {
                    org.bytedeco.javacv.Frame grabbedFrame = grabber.grab();
                    if (grabbedFrame == null) continue;
                    
                    OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
                    Mat img = converter.convert(grabbedFrame);
                    if (img == null) continue;
                    
                    // Конвертуємо в сіре зображення перед обробкою
                    Mat grayImg = new Mat();
                    cvtColor(img, grayImg, COLOR_BGR2GRAY);
                    equalizeHist(grayImg, grayImg);
                    
                    RectVector faces = new RectVector();
                    faceDetector.detectMultiScale(grayImg, faces);
                    
                    boolean currentlyDetected = faces.size() > 0;
                    if (currentlyDetected != faceDetected.get()) {
                        faceDetected.set(currentlyDetected);
                        SwingUtilities.invokeLater(() -> {
                            actionButton.setEnabled(currentlyDetected);
                            statusLabel.setText(currentlyDetected ? "Обличчя виявлено!" : "Очікування виявлення обличчя...");
                            statusLabel.setForeground(currentlyDetected ? Color.GREEN : Color.RED);
                        });
                    }
                    
                    if (faces.size() > 0) {
                        Rect face = faces.get(0);
                        rectangle(img, face, new Scalar(0, 255, 0, 0), 2, LINE_8, 0);
                        
                        synchronized (faceLock) {
                            capturedFace = new Mat(grayImg, face);
                            resize(capturedFace, capturedFace, new Size(200, 200));
                            // Вже не потрібно конвертувати в сіре, бо воно вже сіре
                            // cvtColor(capturedFace, capturedFace, COLOR_BGR2GRAY);
                            equalizeHist(capturedFace, capturedFace);
                        }
                    }
                    
                    Java2DFrameConverter converter2D = new Java2DFrameConverter();
                    currentFrame[0] = converter2D.convert(converter.convert(img));
                    videoPanel.repaint();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    grabber.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        
        videoThread.start();
        
        captureWindow.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                isRunning.set(false);
                try {
                    videoThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    
    private void registerNewFace(String userId, List<Mat> faceSamples) {
        try {
            synchronized (faceLock) {
                if (!faceSamples.isEmpty()) {
                    // Тренуємо модель з усіма зразками
                    trainModel(userId, faceSamples);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Помилка при реєстрації обличчя: " + e.getMessage(), e);
        }
    }
    
    public void trainModel(String userId, List<Mat> faceImages) {
        MatVector images = new MatVector(faceImages.size());
        Mat labels = new Mat(faceImages.size(), 1, CV_32SC1);
        
        for (int i = 0; i < faceImages.size(); i++) {
            Mat processedImage = preprocessFace(faceImages.get(i));
            images.put(i, processedImage);
            IntBuffer labelBuffer = labels.createBuffer();
            labelBuffer.put(i, Integer.parseInt(userId));
        }
        
        faceRecognizer.train(images, labels);
        
        // Зберігаємо модель
        saveModel();
        
        // Зберігаємо маппінг
        userIdMapping.put(userId, Integer.parseInt(userId));
        
        if (onSuccessCallback != null) {
            onSuccessCallback.run();
        }
        if (captureWindow != null) {
            captureWindow.dispose();
        }
    }
    
    private Mat preprocessFace(Mat face) {
        Mat processed = new Mat();
        // Нормалізуємо розмір
        resize(face, processed, new Size(200, 200));
        // Нормалізуємо яскравість
        equalizeHist(processed, processed);
        return processed;
    }
    
    public String recognizeFace(Mat faceImage) {
        try {
            IntPointer label = new IntPointer(1);
            DoublePointer confidence = new DoublePointer(1);
            
            faceRecognizer.predict(faceImage, label, confidence);
            
            // Для Eigenfaces менше значення confidence означає краще співпадіння
            System.out.println("Розпізнавання: ID=" + label.get() + ", Confidence=" + confidence.get());
            
            if (confidence.get() < CONFIDENCE_THRESHOLD) {
                return String.valueOf(label.get());
            }
            return null;
        } catch (Exception e) {
            System.err.println("Помилка при розпізнаванні обличчя: " + e.getMessage());
            return null;
        }
    }

    private void loadModel() {
        File modelFile = new File(MODEL_PATH);
        File mappingFile = new File(USER_MAPPING_PATH);
        
        if (modelFile.exists() && mappingFile.exists()) {
            try {
                // Завантажуємо маппінг користувачів
                try (BufferedReader reader = new BufferedReader(new FileReader(mappingFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split(":");
                        if (parts.length == 2) {
                            userIdMapping.put(parts[0], Integer.parseInt(parts[1]));
                        }
                    }
                }
                
                // Завантажуємо модель
                faceRecognizer.read(MODEL_PATH);
                System.out.println("Модель розпізнавання обличчя успішно завантажена");
            } catch (Exception e) {
                System.err.println("Помилка при завантаженні моделі: " + e.getMessage());
            }
        }
    }

    private void saveModel() {
        try {
            // Створюємо директорію, якщо вона не існує
            new File("src/main/resources/face_models").mkdirs();
            
            // Зберігаємо модель
            faceRecognizer.write(MODEL_PATH);
            
            // Зберігаємо маппінг користувачів
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(USER_MAPPING_PATH))) {
                for (Map.Entry<String, Integer> entry : userIdMapping.entrySet()) {
                    writer.write(entry.getKey() + ":" + entry.getValue());
                    writer.newLine();
                }
            }
            
            System.out.println("Модель розпізнавання обличчя успішно збережена");
        } catch (Exception e) {
            System.err.println("Помилка при збереженні моделі: " + e.getMessage());
        }
    }
} 