package com.chequeprint.controller;

import com.chequeprint.dao.BankDAO;
import com.chequeprint.model.Bank;
import com.chequeprint.model.Cheque;
import com.chequeprint.model.User;
import com.chequeprint.service.ChequeService;
import com.chequeprint.service.ChequeWorkflowService;
import com.chequeprint.service.PrintService;
import com.chequeprint.util.FxUtils;
import com.chequeprint.util.SessionManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * ChequeController — manages Cheque CRUD, filtering, and printing.
 *
 * Fixes applied vs original:
 * • bankNameToId lookup now falls back correctly when a bank isn't in the map.
 * • onPrint() uses PrintService (not raw JasperPrintUtil) for cleaner
 * separation.
 * • loadBanksIntoCombo() properly handles empty DB — won't NPE on
 * getItems().get(0).
 * • applyFilter() is null-safe for bankName.
 * • clearForm() safely checks combo list before calling getValue().
 */
public class ChequeController {

    // ── Table ──
    @FXML
    private TableView<Cheque> chequeTable;
    @FXML
    private TableColumn<Cheque, String> colChequeNo;
    @FXML
    private TableColumn<Cheque, String> colPayee;
    @FXML
    private TableColumn<Cheque, String> colAmount;
    @FXML
    private TableColumn<Cheque, String> colBank;
    @FXML
    private TableColumn<Cheque, String> colDate;
    @FXML
    private TableColumn<Cheque, String> colStatus;

    // ── Form ──
    @FXML
    private TextField fldPayee;
    @FXML
    private TextField fldAmount;
    @FXML
    private ComboBox<String> cmbBank;
    @FXML
    private DatePicker datePicker;
    @FXML
    private Label lblFormTitle;
    @FXML
    private VBox formPanel;

    // ── Filters ──
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> filterStatus;

    // ── Root ──
    @FXML
    private VBox rootPane;

    // ── State ──
    private MainController mainController;

    private final ChequeService chequeService = new ChequeService();
    private final ChequeWorkflowService workflowService = new ChequeWorkflowService();
    private final PrintService printService = new PrintService();
    private final BankDAO bankDAO = new BankDAO();

    private final ObservableList<Cheque> data = FXCollections.observableArrayList();
    private FilteredList<Cheque> filtered;
    private Cheque selectedCheque;

    /** Maps displayed bank name → bank_id; populated from DB. */
    private final java.util.Map<String, Integer> bankNameToId = new java.util.LinkedHashMap<>();

    // ─────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        setupTable();
        setupFilters();
        loadBanksIntoCombo();
        datePicker.setValue(LocalDate.now());
        loadData();
        FxUtils.animateIn(rootPane, 0);
    }

    // ── Table setup ──────────────────────────────────────────────────
    private void setupTable() {
        colChequeNo.setCellValueFactory(new PropertyValueFactory<>("chequeNo"));
        colPayee.setCellValueFactory(new PropertyValueFactory<>("payeeName"));
        colBank.setCellValueFactory(new PropertyValueFactory<>("bankName"));

        colAmount.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getAmount() != null
                        ? "₹" + String.format("%,.2f", c.getValue().getAmount())
                        : "₹0.00"));

        colDate.setCellValueFactory(c -> {
            LocalDate d = c.getValue().getIssueDate();
            return new SimpleStringProperty(d != null ? d.toString() : "");
        });

        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name()));

        // Color-code status column
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setText(null);
                setGraphic(statusBadge(item));
            }
        });

        chequeTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, sel) -> {
                    if (sel != null)
                        populateForm(sel);
                });
    }

    // ── Filters ─────────────────────────────────────────────────────
    private Label statusBadge(String status) {
        Label badge = new Label(status);
        badge.getStyleClass().add("status-badge");
        switch (status) {
            case "Printed" -> badge.getStyleClass().add("status-printed");
            case "Pending" -> badge.getStyleClass().add("status-pending");
            case "Approved" -> badge.getStyleClass().add("status-approved");
            case "Rejected" -> badge.getStyleClass().add("status-rejected");
            case "Draft" -> badge.getStyleClass().add("status-draft");
            case "Cancelled" -> badge.getStyleClass().add("status-cancelled");
            default -> badge.getStyleClass().add("status-neutral");
        }
        return badge;
    }

    private void setupFilters() {
        filterStatus.setItems(FXCollections.observableArrayList(
                "All", "Draft", "Pending", "Approved", "Rejected", "Printed", "Cancelled"));
        filterStatus.setValue("All");

        filtered = new FilteredList<>(data, p -> true);
        chequeTable.setItems(filtered);

        searchField.textProperty().addListener((obs, o, v) -> applyFilter());
        filterStatus.valueProperty().addListener((obs, o, v) -> applyFilter());
    }

    private void applyFilter() {
        String search = searchField.getText() == null ? ""
                : searchField.getText().toLowerCase().trim();
        String status = filterStatus.getValue();

        filtered.setPredicate(c -> {
            boolean matchSearch = search.isEmpty()
                    || (c.getPayeeName() != null && c.getPayeeName().toLowerCase().contains(search))
                    || (c.getChequeNo() != null && c.getChequeNo().toLowerCase().contains(search))
                    || (c.getBankName() != null && c.getBankName().toLowerCase().contains(search));
            boolean matchStatus = "All".equals(status)
                    || (c.getStatus() != null && c.getStatus().name().equals(status));
            return matchSearch && matchStatus;
        });
    }

    public void applyMainSearch(String query) {
        if (searchField != null) {
            searchField.setText(query == null ? "" : query.trim());
        }
    }

    // ── Load banks from DB into ComboBox ─────────────────────────────
    private void loadBanksIntoCombo() {
        new Thread(() -> {
            try {
                List<Bank> banks = bankDAO.findAll();
                Platform.runLater(() -> {
                    bankNameToId.clear();
                    ObservableList<String> names = FXCollections.observableArrayList();
                    for (Bank b : banks) {
                        names.add(b.getBankName());
                        bankNameToId.put(b.getBankName(), b.getId());
                    }
                    if (names.isEmpty()) {
                        // Graceful fallback when DB is empty
                        names.addAll("SBI", "HDFC", "ICICI", "Axis Bank");
                    }
                    cmbBank.setItems(names);
                    cmbBank.setValue(names.get(0));
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    cmbBank.setItems(FXCollections.observableArrayList(
                            "SBI", "HDFC", "ICICI", "Axis Bank"));
                    cmbBank.setValue("SBI");
                });
            }
        }, "load-banks-combo").start();
    }

    // ── Load cheque data ─────────────────────────────────────────────
    private void loadData() {
        new Thread(() -> {
            try {
                var list = chequeService.getAll();
                Platform.runLater(() -> data.setAll(list));
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("DB Error", e.getMessage(), Alert.AlertType.ERROR));
            }
        }, "load-cheques").start();
    }

    public void reload() {
        loadData();
    }

    // ── Save / Update ────────────────────────────────────────────────
    @FXML
    private void onSave() {
        try {
            String payee = fldPayee.getText().trim();
            String amtStr = fldAmount.getText().trim();

            if (payee.isEmpty() || amtStr.isEmpty()) {
                FxUtils.shake(fldPayee.getParent());
                showAlert("Validation", "Payee name and amount are required.",
                        Alert.AlertType.WARNING);
                return;
            }

            BigDecimal amount;
            try {
                amount = new BigDecimal(amtStr);
            } catch (NumberFormatException nfe) {
                FxUtils.shake(fldAmount);
                showAlert("Validation", "Enter a valid numeric amount (e.g. 5000.00).",
                        Alert.AlertType.WARNING);
                return;
            }

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                FxUtils.shake(fldAmount);
                showAlert("Validation", "Amount must be greater than zero.",
                        Alert.AlertType.WARNING);
                return;
            }

            // Resolve bankId — prefer map lookup, fallback to combo index+1
            String selectedBankName = cmbBank.getValue();
            int bankId = bankNameToId.getOrDefault(selectedBankName,
                    Math.max(1, cmbBank.getSelectionModel().getSelectedIndex() + 1));

            User actor = SessionManager.currentUser().orElse(null);
            if (selectedCheque == null) {
                Cheque c = new Cheque(null, payee, amount, bankId, datePicker.getValue());
                workflowService.createPending(c, actor);
                showAlert("Success", "Cheque created and submitted for approval.", Alert.AlertType.INFORMATION);
            } else {
                if (!chequeService.update(selectedCheque)) {
                    throw new SQLException("Could not update cheque.");
                }
                showAlert("Success", "Cheque updated.", Alert.AlertType.INFORMATION);
            }
            clearForm();
            loadData();
            // Notify dashboard (if loaded) to reload its metrics and recent tables
            if (mainController != null) {
                Object dc = mainController.getController("dashboard");
                if (dc instanceof DashboardController)
                    ((DashboardController) dc).reload();
            }

        } catch (Exception e) {
            showAlert("Error", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // ── Save and Direct Print ────────────────────────────────────────
    @FXML
    private void onSaveAndPrint() {
        try {
            String payee = fldPayee.getText().trim();
            String amtStr = fldAmount.getText().trim();

            if (payee.isEmpty() || amtStr.isEmpty()) {
                FxUtils.shake(fldPayee.getParent());
                showAlert("Validation", "Payee name and amount are required.",
                        Alert.AlertType.WARNING);
                return;
            }

            BigDecimal amount;
            try {
                amount = new BigDecimal(amtStr);
            } catch (NumberFormatException nfe) {
                FxUtils.shake(fldAmount);
                showAlert("Validation", "Enter a valid numeric amount (e.g. 5000.00).",
                        Alert.AlertType.WARNING);
                return;
            }

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                FxUtils.shake(fldAmount);
                showAlert("Validation", "Amount must be greater than zero.",
                        Alert.AlertType.WARNING);
                return;
            }

            // Resolve bankId — prefer map lookup, fallback to combo index+1
            String selectedBankName = cmbBank.getValue();
            int bankId = bankNameToId.getOrDefault(selectedBankName,
                    Math.max(1, cmbBank.getSelectionModel().getSelectedIndex() + 1));

            User actor = SessionManager.currentUser().orElse(null);
            if (selectedCheque == null) {
                Cheque newCheque = new Cheque(null, payee, amount, bankId, datePicker.getValue());
                workflowService.createPending(newCheque, actor);
                showAlert("Success",
                        "Cheque created and submitted for approval. Print is available after manager approval.",
                        Alert.AlertType.INFORMATION);
            } else {
                selectedCheque.setPayeeName(payee);
                selectedCheque.setAmount(amount);
                selectedCheque.setBankId(bankId);
                selectedCheque.setIssueDate(datePicker.getValue());
                chequeService.update(selectedCheque);
                workflowService.print(selectedCheque.getId(), actor);
                showAlert("Success", "Cheque printed successfully.",
                        Alert.AlertType.INFORMATION);
            }

            loadData();
            clearForm();
            if (mainController != null) {
                Object dc = mainController.getController("dashboard");
                if (dc instanceof DashboardController) {
                    ((DashboardController) dc).reload();
                }
            }

        } catch (Exception e) {
            showAlert("Error", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // ── Print ────────────────────────────────────────────────────────
    @FXML
    private void onPrint() {
        Cheque sel = chequeTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert("No Selection", "Please select a cheque to print.",
                    Alert.AlertType.WARNING);
            return;
        }

        try {
            User actor = SessionManager.currentUser().orElse(null);
            workflowService.print(sel.getId(), actor);

            loadData();
            if (mainController != null) {
                Object dc = mainController.getController("dashboard");
                if (dc instanceof DashboardController) {
                    ((DashboardController) dc).reload();
                }
            }

            showAlert("Print Successful",
                    "Cheque printed successfully.",
                    Alert.AlertType.INFORMATION);

        } catch (Exception e) {
            showAlert("Print Error", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // ── Delete ───────────────────────────────────────────────────────
    @FXML
    private void onDelete() {
        Cheque sel = chequeTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert("No Selection", "Please select a cheque to delete.",
                    Alert.AlertType.WARNING);
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete cheque " + sel.getChequeNo() + "?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    chequeService.delete(sel.getId());
                    clearForm();
                    loadData();
                    if (mainController != null) {
                        Object dc = mainController.getController("dashboard");
                        if (dc instanceof DashboardController)
                            ((DashboardController) dc).reload();
                    }
                } catch (Exception e) {
                    showAlert("Error", e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    @FXML
    private void onClear() {
        clearForm();
    }

    // ── Form helpers ─────────────────────────────────────────────────
    private void populateForm(Cheque c) {
        selectedCheque = c;
        lblFormTitle.setText("Edit Cheque");
        fldPayee.setText(c.getPayeeName());
        fldAmount.setText(c.getAmount() != null ? c.getAmount().toPlainString() : "");
        datePicker.setValue(c.getIssueDate() != null ? c.getIssueDate() : LocalDate.now());
        if (c.getBankName() != null && cmbBank.getItems().contains(c.getBankName()))
            cmbBank.setValue(c.getBankName());
        FxUtils.animateIn(formPanel, 0);
    }

    private void clearForm() {
        selectedCheque = null;
        lblFormTitle.setText("New Cheque");
        fldPayee.clear();
        fldAmount.clear();
        if (!cmbBank.getItems().isEmpty())
            cmbBank.setValue(cmbBank.getItems().get(0));
        datePicker.setValue(LocalDate.now());
        chequeTable.getSelectionModel().clearSelection();
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(null);
        a.showAndWait();
    }

    public void setMainController(MainController mc) {
        this.mainController = mc;
    }
}
