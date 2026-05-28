package com.chequeprint.controller;

import com.chequeprint.model.Cheque;
import com.chequeprint.service.MultiAgentCoordinatorService;
import com.chequeprint.service.OpenAiChequeOcrService;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AiAssistantController {

    @FXML
    private TextField txtCommand;

    @FXML
    private TextArea txtOutput;

    @FXML
    private TableView<Map<String, Object>> tblResults;

    @FXML
    private Button btnRun;

    @FXML
    private Button btnReminder;

    @FXML
    private Button btnSuggest;

    @FXML
    private Button btnOcr;

    private MainController mainController;
    private final ObjectMapper mapper = new ObjectMapper();

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

        setButtonsDisabled(true);
        appendOutput("\nRunning agent...");

        final MultiAgentCoordinatorService multiAgent = new MultiAgentCoordinatorService();
        final java.util.concurrent.atomic.AtomicReference<com.chequeprint.service.MultiAgentCoordinatorService.MultiAgentResult> resultRef = new java.util.concurrent.atomic.AtomicReference<>();

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                var result = multiAgent.handle(command);
                resultRef.set(result);
                // update table rows on UI thread
                Platform.runLater(() -> setResultRows(result.getRows()));
                return "User: " + command
                        + "\nAgent: " + result.getAgent()
                        + "\nSuccess: " + result.isSuccess()
                        + "\nCount: " + result.getCount()
                        + "\nMessage:\n" + result.getMessage()
                        + formatRows(result.getRows())
                        + (result.getRawOutput() == null || result.getRawOutput().isBlank()
                                ? ""
                                : "\n\nRaw Output:\n" + result.getRawOutput());
            }
        };

        task.setOnSucceeded(e -> {
            setButtonsDisabled(false);
            String out = task.getValue();
            appendOutput("\n" + out);

            var res = resultRef.get();
            if (res != null && res.isSuccess() && "Action Agent".equals(res.getAgent())) {
                if (mainController != null) {
                    Object ctrl = mainController.getController("cheques");
                    if (ctrl instanceof com.chequeprint.controller.ChequeController cc) {
                        cc.reload();
                        mainController.showCheques();
                    }
                    Object dc = mainController.getController("dashboard");
                    if (dc instanceof com.chequeprint.controller.DashboardController dbc) {
                        dbc.reload();
                    }
                }
            }
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

    @FXML
    private void onReminder() {
        // Provide a sensible default reminder command and run it
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
        runBackground("Reading cheque image...", () -> {
            OpenAiChequeOcrService.OcrSaveResult result = new OpenAiChequeOcrService().extractAndSaveCheque(imagePath);
            setResultRows(result.getCheque() == null ? List.of() : List.of(toRow(result.getCheque())));
            if (result.isSaved()) {
                refreshCachedPages();
            }
            return "OCR Image: " + imagePath
                    + "\n" + result.getMessage()
                    + "\nSaved: " + result.isSaved()
                    + "\nExtracted JSON:\n"
                    + mapper.writeValueAsString(result.getOcrResult());
        });
    }

    @FXML
    private void onClear() {
        txtOutput.clear();
        clearResultRows();
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
            appendOutput("\nError: " + getDetailedErrorMessage(ex));
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

    private String getDetailedErrorMessage(Throwable ex) {
        if (ex == null) {
            return "Unknown error";
        }

        StringBuilder message = new StringBuilder(
                ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
        Throwable cause = ex.getCause();
        while (cause != null) {
            String causeMessage = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
            if (!message.toString().contains(causeMessage)) {
                message.append("\nCause: ").append(causeMessage);
            }
            cause = cause.getCause();
        }
        return message.toString();
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

    private void setResultRows(List<Map<String, Object>> rows) {
        Platform.runLater(() -> {
            tblResults.getColumns().clear();

            if (rows == null || rows.isEmpty()) {
                tblResults.setItems(FXCollections.observableArrayList());
                return;
            }

            for (String key : rows.get(0).keySet()) {
                TableColumn<Map<String, Object>, String> column = new TableColumn<>(formatColumnTitle(key));
                column.setMinWidth(110);
                column.setCellValueFactory(data -> {
                    Object value = data.getValue().get(key);
                    return new SimpleStringProperty(value == null ? "" : value.toString());
                });
                tblResults.getColumns().add(column);
            }

            tblResults.setItems(FXCollections.observableArrayList(rows));
        });
    }

    private void clearResultRows() {
        tblResults.getColumns().clear();
        tblResults.setItems(FXCollections.observableArrayList());
    }

    private Map<String, Object> toRow(Cheque cheque) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("cheque_no", cheque.getChequeNo());
        row.put("payee_name", cheque.getPayeeName());
        row.put("amount", cheque.getAmount());
        row.put("bank_id", cheque.getBankId());
        row.put("issue_date", cheque.getIssueDate());
        row.put("status", cheque.getStatus());
        return row;
    }

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

    private String formatColumnTitle(String key) {
        if (key == null || key.isBlank()) {
            return "";
        }
        String[] words = key.replace('_', ' ').split("\\s+");
        StringBuilder title = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (!title.isEmpty()) {
                title.append(' ');
            }
            title.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                title.append(word.substring(1));
            }
        }
        return title.toString();
    }

    @FunctionalInterface
    private interface Worker {
        String run() throws Exception;
    }
}
