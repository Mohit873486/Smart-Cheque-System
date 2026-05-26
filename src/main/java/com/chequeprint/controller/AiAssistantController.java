package com.chequeprint.controller;

import com.chequeprint.service.MultiAgentCoordinatorService;
import com.chequeprint.service.OpenAiChequeOcrService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class AiAssistantController {

    @FXML
    private TextField txtCommand;

    @FXML
    private TextArea txtOutput;

    @FXML
    private Button btnRun;

    @FXML
    private Button btnReminder;

    @FXML
    private Button btnSuggest;

    @FXML
    private Button btnOcr;

    private MainController mainController;

    @FXML
    public void initialize() {
        txtCommand.setOnAction(e -> onRun());
        appendOutput("AI Assistant ready.\nTry: Add cheque for Mohit 5000 tomorrow");
    }

    @FXML
    private void onRun() {
        String command = txtCommand.getText();
        if (command == null || command.isBlank()) {
            showAlert("AI Assistant", "Enter a command first.", Alert.AlertType.WARNING);
            return;
        }

        runBackground("Running agent...", () -> {
            MultiAgentCoordinatorService multiAgent = new MultiAgentCoordinatorService();
            var result = multiAgent.handle(command);
            return "User: " + command
                    + "\nAgent: " + result.getAgent()
                    + "\nSuccess: " + result.isSuccess()
                    + "\nCount: " + result.getCount()
                    + "\nMessage:\n" + result.getMessage()
                    + formatRows(result.getRows())
                    + (result.getRawOutput() == null || result.getRawOutput().isBlank()
                    ? ""
                    : "\n\nRaw Output:\n" + result.getRawOutput());
        });
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

        File file = chooser.showOpenDialog(txtOutput.getScene().getWindow());
        if (file == null) {
            return;
        }

        Path imagePath = file.toPath();
        runBackground("Reading cheque image...", () ->
                "OCR Image: " + imagePath
                        + "\nExtracted JSON:\n"
                        + new OpenAiChequeOcrService().extractChequeJson(imagePath));
    }

    @FXML
    private void onClear() {
        txtOutput.clear();
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    private void runBackground(String loadingMessage, Worker work) {
        setButtonsDisabled(true);
        appendOutput("\n" + loadingMessage);

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return work.run();
            }
        };

        task.setOnSucceeded(e -> {
            setButtonsDisabled(false);
            appendOutput("\n" + task.getValue());
        });

        task.setOnFailed(e -> {
            setButtonsDisabled(false);
            Throwable ex = task.getException();
            appendOutput("\nError: " + (ex != null ? ex.getMessage() : "Unknown error"));
        });

        Thread thread = new Thread(task, "ai-assistant-task");
        thread.setDaemon(true);
        thread.start();
    }

    private void appendOutput(String text) {
        Platform.runLater(() -> {
            if (!txtOutput.getText().isBlank()) {
                txtOutput.appendText("\n\n");
            }
            txtOutput.appendText(text);
        });
    }

    private void setButtonsDisabled(boolean disabled) {
        btnRun.setDisable(disabled);
        btnReminder.setDisable(disabled);
        btnSuggest.setDisable(disabled);
        btnOcr.setDisable(disabled);
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type, message);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private String formatRows(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return "";
        }

        StringBuilder text = new StringBuilder("\n\nRows:");
        int index = 1;
        for (Map<String, Object> row : rows) {
            text.append("\n").append(index++).append(". ");
            text.append(row);
        }
        return text.toString();
    }

    @FunctionalInterface
    private interface Worker {
        String run() throws Exception;
    }
}
