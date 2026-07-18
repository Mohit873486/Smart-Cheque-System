package com.chequeprint.controller;

import com.chequeprint.service.AIService;
import com.chequeprint.service.CommandDetectionService;
import com.chequeprint.service.CommandDetectionService.CommandType;
import com.chequeprint.service.VoiceService;
import com.chequeprint.util.ChatUiBuilder;
import com.chequeprint.util.ThemeManager;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;

public class AiAssistantController {

    @FXML private TextField txtCommand;
    @FXML private VBox chatContainer;
    @FXML private ScrollPane scrollChat;
    @FXML private Label lblStatus;
    @FXML private Button btnSend;
    @FXML private Button btnMic;
    @FXML private Button btnClear;
    @FXML private Button btnThemeToggle;
    @FXML private HBox typingIndicator;
    @FXML private StackPane dot1;
    @FXML private StackPane dot2;
    @FXML private StackPane dot3;
    
    // Kept to avoid FXML injection errors
    @FXML private VBox resultPanel;
    @FXML private Button btnAttach;
    @FXML private Button chipAddCheque;
    @FXML private Button chipReminder;
    @FXML private Button chipOcr;
    @FXML private Button chipInsights;

    private Timeline typingAnimation;
    private ChatUiBuilder uiBuilder;
    
    // We instantiate the Services
    private final AIService aiService = new AIService();
    private final CommandDetectionService commandService = new CommandDetectionService();
    private final VoiceService voiceService = new VoiceService();

    @FXML
    public void initialize() {
        // Initialize UI Builder for chat bubbles
        uiBuilder = new ChatUiBuilder(chatContainer, resultPanel);
        
        // Auto-scroll to bottom when new messages arrive
        chatContainer.heightProperty().addListener((obs, oldVal, newVal) -> {
            scrollChat.setVvalue(1.0);
        });

        // Event handlers for input
        txtCommand.setOnAction(e -> onRun());
        txtCommand.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                onRun();
                e.consume();
            }
        });

        initTypingAnimation();
        
        // Initial greeting
        Platform.runLater(() -> {
            chatContainer.getChildren().clear(); // Clear any placeholder bubbles from FXML
            uiBuilder.addAiBubble("Hello! 👋 I'm your AI Assistant.\nHow can I help you today?");
        });
    }

    @FXML
    private void onRun() {
        String message = txtCommand.getText();
        if (message == null || message.trim().isEmpty()) {
            return;
        }

        String userText = message.trim();
        txtCommand.clear();

        // 1. Show user message in UI
        uiBuilder.addUserBubble(userText);
        
        // 2. Prepare UI for processing
        setInputDisabled(true);
        showTypingIndicator(true);
        lblStatus.setText("Processing...");

        // 3. Smart Command Detection
        CommandType detectedCommand = commandService.detectCommand(userText);

        // 4. Execute on a background thread using Task
        Task<String> apiTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                if (detectedCommand == CommandType.AUTO_FILL_CHEQUE) {
                    // Extract data from the OCR result prefix
                    String prefix = "Auto-fill data:";
                    int idx = userText.toLowerCase().indexOf(prefix.toLowerCase());
                    String ocrText = idx >= 0 ? userText.substring(idx + prefix.length()).trim() : userText;
                    return "✅ Auto-filled Cheque Data:\n\n" + aiService.extractChequeData(ocrText);
                } else if (detectedCommand != CommandType.AI_FALLBACK) {
                    // Small delay to let the typing indicator show naturally
                    Thread.sleep(600); 
                    return commandService.executeCommand(detectedCommand);
                } else {
                    // Generate text using external AIService
                    return aiService.askAI(userText);
                }
            }
        };

        apiTask.setOnSucceeded(e -> {
            // 5. Show response in UI on the JavaFX Application Thread
            String responseText = apiTask.getValue();
            
            Platform.runLater(() -> {
                showTypingIndicator(false);
                setInputDisabled(false);
                lblStatus.setText("Online • Ready to help");
                
                if (detectedCommand != CommandType.AI_FALLBACK && responseText.startsWith("✅")) {
                    uiBuilder.addAiSuccessBubble(responseText);
                } else {
                    uiBuilder.addAiBubble(responseText);
                }
                
                txtCommand.requestFocus();
            });
        });

        apiTask.setOnFailed(e -> {
            Throwable ex = apiTask.getException();
            Platform.runLater(() -> {
                showTypingIndicator(false);
                setInputDisabled(false);
                lblStatus.setText("Online • Ready to help");
                uiBuilder.addAiErrorBubble("Error connecting to AI: " + ex.getMessage());
                txtCommand.requestFocus();
            });
        });

        // Start the background thread
        Thread thread = new Thread(apiTask, "ai-api-task");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void onClear() {
        chatContainer.getChildren().clear();
        uiBuilder.addAiBubble("Chat cleared. How can I help you?");
    }

    @FXML
    private void onThemeToggle() {
        if (txtCommand.getScene() == null) return;
        boolean isDark = false;
        for (String style : txtCommand.getScene().getStylesheets()) {
            if (style.contains("dark.css") || style.contains("ai_assistant.css")) {
                isDark = true;
                break;
            }
        }
        
        String newTheme = isDark ? "light" : "dark";
        ThemeManager.applyTheme(txtCommand.getScene(), newTheme);
        ThemeManager.saveTheme(newTheme);
    }

    // -- Dummy chip handlers to prevent FXML errors --
    @FXML private void onChipAddCheque() { txtCommand.setText("Add cheque for "); txtCommand.requestFocus(); }
    @FXML private void onReminder() { txtCommand.setText("Reminder check"); onRun(); }
    @FXML private void onSuggestions() { txtCommand.setText("Analyze cheque data"); onRun(); }
    @FXML private void onOcrImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Cheque Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        java.io.File selectedFile = fileChooser.showOpenDialog(txtCommand.getScene().getWindow());
        if (selectedFile != null) {
            uiBuilder.addUserBubble("Uploaded image: " + selectedFile.getName());
            setInputDisabled(true);
            showTypingIndicator(true);
            lblStatus.setText("Scanning image...");

            Task<String> ocrTask = new Task<>() {
                @Override
                protected String call() throws Exception {
                    com.chequeprint.service.GeminiApiClient geminiClient = new com.chequeprint.service.GeminiApiClient();
                    String mimeType = selectedFile.getName().toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";
                    return geminiClient.generateTextFromImage("gemini-1.5-flash", 
                        "Analyze this cheque image and extract all visible text. Return the raw text.", 
                        selectedFile.toPath(), 
                        mimeType, 
                        512);
                }
            };

            ocrTask.setOnSucceeded(e -> {
                String ocrResult = ocrTask.getValue();
                Platform.runLater(() -> {
                    showTypingIndicator(false);
                    setInputDisabled(false);
                    lblStatus.setText("Online • Ready to help");
                    uiBuilder.addAiBubble("OCR Result:\n" + ocrResult);
                    
                    // Optional: Setup for auto-fill in next step
                    txtCommand.setText("Auto-fill data: " + ocrResult);
                });
            });

            ocrTask.setOnFailed(e -> {
                Throwable ex = ocrTask.getException();
                Platform.runLater(() -> {
                    showTypingIndicator(false);
                    setInputDisabled(false);
                    lblStatus.setText("Online • Ready to help");
                    uiBuilder.addAiErrorBubble("OCR failed: " + ex.getMessage());
                });
            });

            Thread t = new Thread(ocrTask, "ocr-task");
            t.setDaemon(true);
            t.start();
        }
    }

    // ═══════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════

    private void initTypingAnimation() {
        if (dot1 == null || dot2 == null || dot3 == null) return;
        typingAnimation = new Timeline();
        typingAnimation.setCycleCount(Timeline.INDEFINITE);

        double bounceHeight = -5;
        Duration dotDuration = Duration.millis(400);

        typingAnimation.getKeyFrames().addAll(
                new KeyFrame(Duration.ZERO, new KeyValue(dot1.translateYProperty(), 0)),
                new KeyFrame(Duration.millis(200), new KeyValue(dot1.translateYProperty(), bounceHeight)),
                new KeyFrame(dotDuration, new KeyValue(dot1.translateYProperty(), 0)),
                new KeyFrame(Duration.millis(133), new KeyValue(dot2.translateYProperty(), 0)),
                new KeyFrame(Duration.millis(333), new KeyValue(dot2.translateYProperty(), bounceHeight)),
                new KeyFrame(Duration.millis(533), new KeyValue(dot2.translateYProperty(), 0)),
                new KeyFrame(Duration.millis(266), new KeyValue(dot3.translateYProperty(), 0)),
                new KeyFrame(Duration.millis(466), new KeyValue(dot3.translateYProperty(), bounceHeight)),
                new KeyFrame(Duration.millis(666), new KeyValue(dot3.translateYProperty(), 0))
        );
    }

    private void showTypingIndicator(boolean show) {
        if (typingIndicator == null) return;
        Platform.runLater(() -> {
            typingIndicator.setVisible(show);
            typingIndicator.setManaged(show);
            if (show && typingAnimation != null) {
                typingAnimation.play();
            } else if (typingAnimation != null) {
                typingAnimation.stop();
            }
        });
    }

    private void setInputDisabled(boolean disabled) {
        Platform.runLater(() -> {
            if (btnSend != null) btnSend.setDisable(disabled);
            if (txtCommand != null) txtCommand.setDisable(disabled);
            if (chipAddCheque != null) chipAddCheque.setDisable(disabled);
            if (chipReminder != null) chipReminder.setDisable(disabled);
            if (chipOcr != null) chipOcr.setDisable(disabled);
            if (chipInsights != null) chipInsights.setDisable(disabled);
            if (btnAttach != null) btnAttach.setDisable(disabled);
            if (btnMic != null) btnMic.setDisable(disabled);
        });
    }

    @FXML
    private void onMicToggle() {
        if (!voiceService.isRecording()) {
            voiceService.startRecording();
            btnMic.setText("🛑");
            btnMic.setStyle("-fx-text-fill: red;");
            lblStatus.setText("Recording... (Click stop when done)");
            
            // Disable other inputs but keep mic enabled
            if (btnSend != null) btnSend.setDisable(true);
            if (txtCommand != null) txtCommand.setDisable(true);
        } else {
            btnMic.setText("🎤");
            btnMic.setStyle("");
            lblStatus.setText("Transcribing audio...");
            setInputDisabled(true);
            showTypingIndicator(true);

            Task<String> transcribeTask = new Task<>() {
                @Override
                protected String call() throws Exception {
                    return voiceService.stopRecordingAndTranscribe();
                }
            };

            transcribeTask.setOnSucceeded(e -> {
                String transcribedText = transcribeTask.getValue();
                Platform.runLater(() -> {
                    showTypingIndicator(false);
                    setInputDisabled(false);
                    lblStatus.setText("Online • Ready to help");
                    txtCommand.setText(transcribedText);
                    txtCommand.requestFocus();
                });
            });

            transcribeTask.setOnFailed(e -> {
                Throwable ex = transcribeTask.getException();
                Platform.runLater(() -> {
                    showTypingIndicator(false);
                    setInputDisabled(false);
                    lblStatus.setText("Online • Ready to help");
                    uiBuilder.addAiErrorBubble("Transcription failed: " + ex.getMessage());
                });
            });

            Thread t = new Thread(transcribeTask, "transcription-task");
            t.setDaemon(true);
            t.start();
        }
    }
    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }
}
