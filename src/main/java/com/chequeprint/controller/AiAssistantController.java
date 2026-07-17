package com.chequeprint.controller;

import com.chequeprint.model.Cheque;
import com.chequeprint.service.MultiAgentCoordinatorService;
import com.chequeprint.service.OpenAiChequeOcrService;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AiAssistantController {

    @FXML private TextField txtCommand;
    @FXML private VBox chatContainer;
    @FXML private ScrollPane scrollChat;
    @FXML private Label lblStatus;
    @FXML private Button btnSend;
    @FXML private Button btnClear;
    @FXML private Button btnThemeToggle;
    @FXML private Button btnAttach;
    @FXML private Button chipAddCheque;
    @FXML private Button chipReminder;
    @FXML private Button chipOcr;
    @FXML private Button chipInsights;
    @FXML private HBox typingIndicator;
    @FXML private StackPane dot1;
    @FXML private StackPane dot2;
    @FXML private StackPane dot3;
    @FXML private HBox chipsBar;
    @FXML private VBox resultPanel;

    private MainController mainController;
    private Timeline typingAnimation;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");

    private final com.chequeprint.service.CommandParser commandParser = new com.chequeprint.service.CommandParser();
    private final com.chequeprint.service.ChequeApiService chequeApiService = new com.chequeprint.service.ChequeApiService();
    private final com.chequeprint.service.ChatHistoryService chatHistoryService = new com.chequeprint.service.ChatHistoryService();
    private com.chequeprint.util.ChatUiBuilder uiBuilder;

    @FXML
    public void initialize() {
        uiBuilder = new com.chequeprint.util.ChatUiBuilder(chatContainer, resultPanel);
        
        txtCommand.setOnAction(e -> onRun());
        txtCommand.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                onRun();
                e.consume();
            }
        });

        // Auto-scroll when new children are added
        chatContainer.heightProperty().addListener((obs, oldVal, newVal) -> {
            scrollChat.setVvalue(1.0);
        });

        initTypingAnimation();
        
        // Load history
        java.util.List<java.util.Map<String, String>> history = chatHistoryService.loadHistory();
        if (history.isEmpty()) {
            uiBuilder.addAiBubble("Hello! 👋 I'm your AI Assistant.\n\nI can help you with cheque management, " +
                    "reminders, OCR scanning, and data insights.\n\nTry one of the quick actions below, " +
                    "or type a command like:\n• \"Add cheque for Mohit ₹5000 tomorrow\"\n" +
                    "• \"Show all pending cheques\"\n• \"Reminder check\"");
        } else {
            for (java.util.Map<String, String> msg : history) {
                if ("USER".equals(msg.get("role"))) {
                    uiBuilder.addUserBubble(msg.get("text"));
                } else {
                    uiBuilder.addAiBubble(msg.get("text"));
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════
    //  SEND / RUN
    // ═══════════════════════════════════════════════════

    @FXML
    private void onRun() {
        String command = txtCommand.getText();
        if (command == null || command.isBlank()) {
            return;
        }

        uiBuilder.addUserBubble(command);
        chatHistoryService.saveMessage("USER", command);
        txtCommand.clear();
        txtCommand.requestFocus();

        setInputDisabled(true);
        showTypingIndicator(true);
        lblStatus.setText("Processing...");

        com.chequeprint.service.CommandParser.ParsedCommand parsed = commandParser.parse(command);
        
        if (parsed.getAction() != com.chequeprint.service.CommandParser.CommandAction.UNKNOWN) {
            Task<com.chequeprint.service.ChequeApiResponse> apiTask = new Task<>() {
                @Override
                protected com.chequeprint.service.ChequeApiResponse call() {
                    switch (parsed.getAction()) {
                        case ADD_CHEQUE:
                            String name = (String) parsed.getData().get("name");
                            double amount = (Double) parsed.getData().get("amount");
                            String date = (String) parsed.getData().get("date");
                            return chequeApiService.createCheque(name, amount, date);
                        case SHOW_ALL_CHEQUES:
                            return chequeApiService.getAllCheques();
                        case SHOW_PENDING_CHEQUES:
                            return chequeApiService.getPendingCheques();
                        case DELETE_CHEQUE:
                            int id = (Integer) parsed.getData().get("id");
                            return chequeApiService.deleteCheque(id);
                        default:
                            return com.chequeprint.service.ChequeApiResponse.error("Unknown action.");
                    }
                }
            };

            apiTask.setOnSucceeded(e -> {
                showTypingIndicator(false);
                setInputDisabled(false);
                lblStatus.setText("Online • Ready to help");
                
                com.chequeprint.service.ChequeApiResponse res = apiTask.getValue();
                if (!res.isSuccess()) {
                    uiBuilder.addAiErrorBubble(res.getMessage());
                } else if (res.getData() == null || res.getData().isEmpty()) {
                    uiBuilder.addAiSuccessBubble(res.getMessage());
                } else if (res.getData().size() == 1) {
                    uiBuilder.addAiBubble(res.getMessage());
                    uiBuilder.addSingleChequeCard(res.getData().get(0));
                } else {
                    uiBuilder.addAiBubble(res.getMessage());
                    uiBuilder.addTableBubble(res.getData());
                }
                chatHistoryService.saveMessage("AI", res.getMessage());
                
                refreshCachedPages();
            });

            apiTask.setOnFailed(e -> {
                showTypingIndicator(false);
                setInputDisabled(false);
                lblStatus.setText("Online • Ready to help");
                uiBuilder.addAiErrorBubble("API Error: " + getDetailedErrorMessage(apiTask.getException()));
                chatHistoryService.saveMessage("AI", "API Error: " + getDetailedErrorMessage(apiTask.getException()));
            });

            Thread thread = new Thread(apiTask, "api-task");
            thread.setDaemon(true);
            thread.start();
            return;
        }

        final MultiAgentCoordinatorService multiAgent = new MultiAgentCoordinatorService();
        final java.util.concurrent.atomic.AtomicReference<MultiAgentCoordinatorService.MultiAgentResult> resultRef =
                new java.util.concurrent.atomic.AtomicReference<>();

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                var result = multiAgent.handle(command);
                resultRef.set(result);
                return buildResponseText(result);
            }
        };

        task.setOnSucceeded(e -> {
            showTypingIndicator(false);
            setInputDisabled(false);
            lblStatus.setText("Online • Ready to help");

            String out = task.getValue();
            uiBuilder.addAiBubble(out);
            chatHistoryService.saveMessage("AI", out);

            var res = resultRef.get();
            if (res != null && res.isSuccess() && "Action Agent".equals(res.getAgent())) {
                refreshCachedPages();
            }

            if (res != null && res.getRows() != null && !res.getRows().isEmpty()) {
                uiBuilder.addTableBubble(res.getRows());
            }
        });

        task.setOnFailed(e -> {
            showTypingIndicator(false);
            setInputDisabled(false);
            lblStatus.setText("Online • Ready to help");
            Throwable ex = task.getException();
            uiBuilder.addAiBubble("❌ Error: " + getDetailedErrorMessage(ex));
            chatHistoryService.saveMessage("AI", "❌ Error: " + getDetailedErrorMessage(ex));
        });

        Thread thread = new Thread(task, "ai-assistant-task");
        thread.setDaemon(true);
        thread.start();
    }

    // ═══════════════════════════════════════════════════
    //  QUICK ACTION CHIPS
    // ═══════════════════════════════════════════════════

    @FXML
    private void onChipAddCheque() {
        txtCommand.setText("Add cheque for ");
        txtCommand.requestFocus();
        txtCommand.positionCaret(txtCommand.getText().length());
    }

    @FXML
    private void onReminder() {
        txtCommand.setText("Reminder check");
        onRun();
    }

    @FXML
    private void onSuggestions() {
        txtCommand.setText("Analyze cheque data and suggest insights");
        onRun();
    }

    @FXML
    private void onOcrImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Cheque Image");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.webp"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));

        File file = chooser.showOpenDialog(scrollChat.getScene().getWindow());
        if (file == null) {
            return;
        }

        uiBuilder.addUserImageBubble(file);

        setInputDisabled(true);
        showTypingIndicator(true);
        lblStatus.setText("Processing OCR with Tesseract...");

        Task<com.chequeprint.service.ChequeOcrResult> task = new Task<>() {
            @Override
            protected com.chequeprint.service.ChequeOcrResult call() throws Exception {
                return new com.chequeprint.service.ChequeOcrService().extractFromFile(file);
            }
        };

        task.setOnSucceeded(e -> {
            showTypingIndicator(false);
            setInputDisabled(false);
            lblStatus.setText("Online • Ready to help");
            
            com.chequeprint.service.ChequeOcrResult result = task.getValue();
            uiBuilder.addAiBubble("Extracted Text:\n\n" + result.getRawText());
            chatHistoryService.saveMessage("AI", "Extracted Text:\n\n" + result.getRawText());
            
            // Auto-fill form (command bar)
            String name = result.getPayeeName() != null ? result.getPayeeName() : "[Name]";
            String amount = result.getAmount() != null ? result.getAmount().toString() : "[Amount]";
            String date = result.getDate() != null ? result.getDate().toString() : "[Date]";
            
            String cmd = String.format("add cheque for %s %s %s", name, amount, date);
            txtCommand.setText(cmd);
            txtCommand.requestFocus();
            txtCommand.positionCaret(cmd.length());
        });

        task.setOnFailed(e -> {
            showTypingIndicator(false);
            setInputDisabled(false);
            lblStatus.setText("Online • Ready to help");
            Throwable ex = task.getException();
            uiBuilder.addAiErrorBubble("OCR Error: " + getDetailedErrorMessage(ex));
            chatHistoryService.saveMessage("AI", "OCR Error: " + getDetailedErrorMessage(ex));
        });

        Thread thread = new Thread(task, "ai-ocr-task");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void onClear() {
        chatContainer.getChildren().clear();
        chatHistoryService.clearHistory();
        uiBuilder.addAiBubble("Chat cleared. How can I help you?");
    }
    
    @FXML
    private void onThemeToggle() {
        if (txtCommand.getScene() == null) return;
        
        boolean isDark = false;
        for (String style : txtCommand.getScene().getStylesheets()) {
            if (style.contains("dark.css")) {
                isDark = true;
                break;
            }
        }
        
        String newTheme = isDark ? "light" : "dark";
        com.chequeprint.util.ThemeManager.applyTheme(txtCommand.getScene(), newTheme);
        com.chequeprint.util.ThemeManager.saveTheme(newTheme);
        
        btnThemeToggle.setText(newTheme.equals("dark") ? "☀️" : "🌙");
    }

    // ═══════════════════════════════════════════════════
    //  TYPING INDICATOR
    // ═══════════════════════════════════════════════════

    private void initTypingAnimation() {
        typingAnimation = new Timeline();
        typingAnimation.setCycleCount(Timeline.INDEFINITE);

        // Create bouncing dots animation
        double bounceHeight = -5;
        Duration dotDuration = Duration.millis(400);

        typingAnimation.getKeyFrames().addAll(
                // Dot 1
                new KeyFrame(Duration.ZERO, new KeyValue(dot1.translateYProperty(), 0)),
                new KeyFrame(Duration.millis(200), new KeyValue(dot1.translateYProperty(), bounceHeight)),
                new KeyFrame(dotDuration, new KeyValue(dot1.translateYProperty(), 0)),
                // Dot 2
                new KeyFrame(Duration.millis(133), new KeyValue(dot2.translateYProperty(), 0)),
                new KeyFrame(Duration.millis(333), new KeyValue(dot2.translateYProperty(), bounceHeight)),
                new KeyFrame(Duration.millis(533), new KeyValue(dot2.translateYProperty(), 0)),
                // Dot 3
                new KeyFrame(Duration.millis(266), new KeyValue(dot3.translateYProperty(), 0)),
                new KeyFrame(Duration.millis(466), new KeyValue(dot3.translateYProperty(), bounceHeight)),
                new KeyFrame(Duration.millis(666), new KeyValue(dot3.translateYProperty(), 0))
        );
    }

    private void showTypingIndicator(boolean show) {
        Platform.runLater(() -> {
            typingIndicator.setVisible(show);
            typingIndicator.setManaged(show);
            if (show) {
                typingAnimation.play();
                
                resultPanel.getChildren().clear();
                javafx.scene.control.ProgressIndicator spinner = new javafx.scene.control.ProgressIndicator();
                spinner.setMaxSize(60, 60);
                spinner.setStyle("-fx-progress-color: #3b82f6;");
                Label lbl = new Label("Processing...");
                lbl.setStyle("-fx-font-size: 16px; -fx-text-fill: #64748b; -fx-padding: 10 0 0 0;");
                resultPanel.getChildren().addAll(spinner, lbl);
            } else {
                typingAnimation.stop();
            }
        });
    }

    // ═══════════════════════════════════════════════════
    //  STATE HELPERS
    // ═══════════════════════════════════════════════════

    private void setInputDisabled(boolean disabled) {
        Platform.runLater(() -> {
            btnSend.setDisable(disabled);
            txtCommand.setDisable(disabled);
            chipAddCheque.setDisable(disabled);
            chipReminder.setDisable(disabled);
            chipOcr.setDisable(disabled);
            chipInsights.setDisable(disabled);
            btnAttach.setDisable(disabled);
        });
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    // ═══════════════════════════════════════════════════
    //  RESPONSE FORMATTING
    // ═══════════════════════════════════════════════════

    private String buildResponseText(MultiAgentCoordinatorService.MultiAgentResult result) {
        StringBuilder sb = new StringBuilder();

        if (result.isSuccess()) {
            sb.append("✅ ");
        } else {
            sb.append("⚠️ ");
        }

        if (result.getMessage() != null && !result.getMessage().isBlank()) {
            sb.append(result.getMessage());
        }

        if (result.getCount() > 0) {
            sb.append("\n\n📋 ").append(result.getCount()).append(" result(s) found.");
        }

        return sb.toString().trim();
    }

    private String getDetailedErrorMessage(Throwable ex) {
        if (ex == null) return "Unknown error occurred";
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage() != null ? root.getMessage() : root.getClass().getSimpleName();
    }

    // ═══════════════════════════════════════════════════
    //  CHEQUE HELPERS
    // ═══════════════════════════════════════════════════

    private void refreshCachedPages() {
        if (mainController == null) {
            return;
        }

        Object chequeController = mainController.getController("cheques");
        if (chequeController instanceof ChequeController cc) {
            cc.reload();
        }

        Object dashboardController = mainController.getController("dashboard");
        if (dashboardController instanceof DashboardController dc) {
            dc.reload();
        }
    }

    // ═══════════════════════════════════════════════════
    //  PUBLIC CHAT API
    // ═══════════════════════════════════════════════════

    public void addUserMessage(String msg) {
        uiBuilder.addUserBubble(msg);
        chatHistoryService.saveMessage("USER", msg);
    }

    public void addAIMessage(String msg) {
        uiBuilder.addAiBubble(msg);
        chatHistoryService.saveMessage("AI", msg);
    }
}
