package com.chequeprint.controller;

import com.chequeprint.model.Cheque;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.util.Pair;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import com.chequeprint.service.ChequeService;
import com.chequeprint.service.ChequeOcrResult;
import com.chequeprint.service.ChequeOcrService;
import com.chequeprint.service.OcrException;
import javafx.concurrent.Task;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.application.Platform;
import javafx.scene.control.DatePicker;
import javafx.scene.Node;
import java.io.File;

public class ModernDashboardController {

    @FXML
    private Label lblTotal, lblPending, lblCleared, lblBounced;

    @FXML
    private TableView<Cheque> tblCheques;

    @FXML
    private TableColumn<Cheque, String> colNo;
    @FXML
    private TableColumn<Cheque, String> colPayee;
    @FXML
    private TableColumn<Cheque, BigDecimal> colAmount;
    @FXML
    private TableColumn<Cheque, LocalDate> colDate;
    @FXML
    private TableColumn<Cheque, Cheque.Status> colStatus;

    @FXML
    private ComboBox<Cheque.Status> cmbStatus;

    @FXML
    private Button btnUpdateStatus;

    private final ObservableList<Cheque> cheques = FXCollections.observableArrayList();
    private final ChequeService chequeService = new ChequeService();

    @FXML
    public void initialize() {
        // configure table columns
        colNo.setCellValueFactory(new PropertyValueFactory<>("chequeNo"));
        colPayee.setCellValueFactory(new PropertyValueFactory<>("payeeName"));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("issueDate"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        tblCheques.setItems(cheques);

        // status combo values
        cmbStatus.setItems(FXCollections.observableArrayList(Cheque.Status.values()));

        // color-coded status cell
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Cheque.Status status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                    return;
                }
                String label = status.name();
                setText(label);
                String color = switch (status) {
                    case Cleared, Approved, Printed -> "-fx-text-fill: #0a7a0a;"; // green
                    case Bounced, Rejected -> "-fx-text-fill: #b30000;"; // red
                    case Pending -> "-fx-text-fill: #b07a00;"; // yellow
                    case Deposited -> "-fx-text-fill: #0075b3;"; // blue
                    default -> "-fx-text-fill: -fx-text-base-color;";
                };
                setStyle(color + " -fx-font-weight: bold;");
            }
        });

        // load data from database
        loadFromDatabase();

        tblCheques.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    }
    private void loadFromDatabase() {
        Task<List<Cheque>> task = new Task<>() {
            @Override
            protected List<Cheque> call() throws Exception {
                return chequeService.getAll();
            }
        };
        task.setOnSucceeded(evt -> {
            cheques.clear();
            cheques.addAll(task.getValue());
            refreshSummary();
        });
        task.setOnFailed(evt -> {
            Throwable ex = task.getException();
            ex.printStackTrace();
            Platform.runLater(() -> {
                Alert a = new Alert(Alert.AlertType.ERROR, "Failed to load cheques: " + ex.getMessage(), ButtonType.OK);
                a.setHeaderText("Database Error"); a.showAndWait();
            });
        });
        Thread t = new Thread(task, "DB-Load"); t.setDaemon(true); t.start();
    }

    private Cheque make(String no, String payee, String status, double amount) {
        Cheque c = new Cheque();
        c.setChequeNo(no);
        c.setPayeeName(payee);
        c.setAmount(BigDecimal.valueOf(amount));
        c.setIssueDate(LocalDate.now().minusDays((int)(Math.random()*10)));
        try {
            c.setStatus(Cheque.Status.valueOf(status));
        } catch (IllegalArgumentException ex) {
            // map common labels
            if ("Pending".equalsIgnoreCase(status)) c.setStatus(Cheque.Status.Pending);
            else if ("Rejected".equalsIgnoreCase(status)) c.setStatus(Cheque.Status.Rejected);
            else if ("Printed".equalsIgnoreCase(status)) c.setStatus(Cheque.Status.Printed);
            else if ("Approved".equalsIgnoreCase(status)) c.setStatus(Cheque.Status.Approved);
            else c.setStatus(Cheque.Status.Draft);
        }
        return c;
    }

    private void refreshSummary() {
        int total = cheques.size();
        int pending = (int) cheques.stream().filter(c -> c.getStatus() == Cheque.Status.Pending).count();
        int cleared = (int) cheques.stream().filter(c -> {
            var s = c.getStatus();
            return s == Cheque.Status.Cleared || s == Cheque.Status.Approved || s == Cheque.Status.Printed;
        }).count();
        int bounced = (int) cheques.stream().filter(c -> c.getStatus() == Cheque.Status.Bounced || c.getStatus() == Cheque.Status.Rejected).count();

        lblTotal.setText(String.valueOf(total));
        lblPending.setText(String.valueOf(pending));
        lblCleared.setText(String.valueOf(cleared));
        lblBounced.setText(String.valueOf(bounced));
    }

    @FXML
    private void onUpdateStatus() {
        Cheque sel = tblCheques.getSelectionModel().getSelectedItem();
        Cheque.Status chosen = cmbStatus.getValue();
        if (sel == null) {
            Alert a = new Alert(Alert.AlertType.WARNING, "Please select a cheque to update.", ButtonType.OK);
            a.setHeaderText("No selection"); a.showAndWait(); return;
        }
        if (chosen == null) {
            Alert a = new Alert(Alert.AlertType.WARNING, "Please choose a status to set.", ButtonType.OK);
            a.setHeaderText("No status chosen"); a.showAndWait(); return;
        }

        try {
            boolean ok = chequeService.setStatus(sel, chosen);
            if (ok) {
                sel.setStatus(chosen);
                tblCheques.refresh();
                refreshSummary();
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Status updated.", ButtonType.OK);
                a.showAndWait();
            } else {
                Alert a = new Alert(Alert.AlertType.ERROR, "Failed to update status.", ButtonType.OK);
                a.showAndWait();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Alert a = new Alert(Alert.AlertType.ERROR, "Error updating status: " + ex.getMessage(), ButtonType.OK);
            a.showAndWait();
        }
    }

    @FXML
    private void onAddCheque() {
        showAddChequeDialog(null);
    }

    private void showAddChequeDialog(ChequeOcrResult ocr) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Add Cheque");
        ButtonType add = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(add, ButtonType.CANCEL);

        TextField no = new TextField(); no.setPromptText("Cheque No");
        TextField payee = new TextField(); payee.setPromptText("Payee Name");
        TextField amt = new TextField(); amt.setPromptText("Amount");
        DatePicker dp = new DatePicker();

        if (ocr != null) {
            if (ocr.getChequeNumber() != null) no.setText(ocr.getChequeNumber());
            if (ocr.getPayeeName() != null) payee.setText(ocr.getPayeeName());
            if (ocr.getAmount() != null) amt.setText(ocr.getAmount().toPlainString());
            if (ocr.getDate() != null) dp.setValue(ocr.getDate());
        }

        VBox box = new VBox(8, new Label("Cheque No"), no, new Label("Payee"), payee, new Label("Amount"), amt, new Label("Issue Date"), dp);
        dlg.getDialogPane().setContent(box);

        dlg.setResultConverter(button -> button);

        // validation: enable Add only when payee present, date present and amount looks numeric
        Node addBtn = dlg.getDialogPane().lookupButton(add);
        Runnable validate = () -> {
            boolean ok = true;
            String p = payee.getText();
            String a = amt.getText();
            if (p == null || p.isBlank()) ok = false;
            if (dp.getValue() == null) ok = false;
            try {
                if (a == null || a.isBlank()) ok = false;
                else {
                    double v = Double.parseDouble(a.replaceAll(",", ""));
                    if (v <= 0) ok = false;
                }
            } catch (Exception ex) { ok = false; }
            addBtn.setDisable(!ok);
        };
        payee.textProperty().addListener((s,o,n) -> validate.run());
        amt.textProperty().addListener((s,o,n) -> validate.run());
        dp.valueProperty().addListener((s,o,n) -> validate.run());
        validate.run();

        var res = dlg.showAndWait();
        if (res.isPresent() && res.get() == add) {
            String chequeNo = no.getText();
            String payeeName = payee.getText();
            double amount = 0;
            try { amount = Double.parseDouble(amt.getText().replaceAll(",", "")); } catch (Exception ignored) {}
            Cheque c = new Cheque();
            c.setChequeNo(chequeNo.isBlank() ? "CHQ-" + (1000 + cheques.size()+1) : chequeNo);
            c.setPayeeName(payeeName);
            c.setAmount(BigDecimal.valueOf(amount));
            c.setIssueDate(dp.getValue());
            c.setStatus(Cheque.Status.Pending);

            // persist using DAO in background
            Task<Boolean> saveTask = new Task<>() {
                @Override
                protected Boolean call() throws Exception {
                    return chequeService.save(c);
                }
            };
            saveTask.setOnSucceeded(evt -> {
                boolean ok = saveTask.getValue();
                if (ok) {
                    cheques.add(0, c);
                    refreshSummary();
                    Alert a = new Alert(Alert.AlertType.INFORMATION, "Cheque saved.", ButtonType.OK);
                    a.setHeaderText("Saved"); a.showAndWait();
                } else {
                    Alert a = new Alert(Alert.AlertType.ERROR, "Failed to save cheque.", ButtonType.OK);
                    a.setHeaderText("Save Error"); a.showAndWait();
                }
            });
            saveTask.setOnFailed(evt -> {
                Throwable ex = saveTask.getException();
                ex.printStackTrace();
                Platform.runLater(() -> {
                    Alert a = new Alert(Alert.AlertType.ERROR, "Error saving cheque: " + ex.getMessage(), ButtonType.OK);
                    a.setHeaderText("Database Error"); a.showAndWait();
                });
            });
            Thread tt = new Thread(saveTask, "DB-Save"); tt.setDaemon(true); tt.start();
        }
    }

    @FXML
    private void onScan() {
        Window w = tblCheques.getScene().getWindow();
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Cheque Image");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.tiff", "*.bmp")
        );
        File f = fc.showOpenDialog(w);
        if (f == null) return;

        // run OCR in background
        Task<ChequeOcrResult> task = new Task<>() {
            @Override
            protected ChequeOcrResult call() throws Exception {
                ChequeOcrService svc = new ChequeOcrService();
                return svc.extractFromFile(f);
            }
        };

        task.setOnSucceeded(evt -> {
            ChequeOcrResult res = task.getValue();
            showAddChequeDialog(res);
        });
        task.setOnFailed(evt -> {
            Throwable ex = task.getException();
            String msg = ex instanceof OcrException ? ex.getMessage() : "Failed to process image: " + ex.getMessage();
            Platform.runLater(() -> {
                Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
                a.setHeaderText("OCR Error"); a.showAndWait();
            });
        });

        Thread t = new Thread(task, "OCR-Thread");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void onViewSelected() {
        Cheque sel = tblCheques.getSelectionModel().getSelectedItem();
        if (sel == null) {
            Alert a = new Alert(Alert.AlertType.WARNING, "Please select a cheque to view.", ButtonType.OK);
            a.setHeaderText("No selection"); a.showAndWait(); return;
        }
        Alert a = new Alert(Alert.AlertType.INFORMATION, sel.toString(), ButtonType.OK);
        a.setTitle("Cheque Details"); a.setHeaderText(sel.getChequeNo()); a.showAndWait();
    }
}
