package com.chequeprint.controller;

import com.chequeprint.model.Bank;
import com.chequeprint.model.Cheque;
import com.chequeprint.model.User;
import com.chequeprint.service.*;
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
import java.time.LocalDate;
import java.util.List;

public class ChequeHistoryController {

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

    @FXML
    private TextField searchField;
    @FXML
    private DatePicker filterDate;
    @FXML
    private ComboBox<String> filterStatus;
    @FXML
    private ComboBox<String> filterBank;

    @FXML
    private Label lblTotalCount;
    @FXML
    private Label lblTotalAmount;
    @FXML
    private Button btnPrint;
    @FXML
    private VBox rootPane;

    private final ChequeService chequeService = new ChequeService();
    private final ChequeWorkflowService workflowService = new ChequeWorkflowService();
    private final BankService bankService = new BankService();

    private final ObservableList<Cheque> data = FXCollections.observableArrayList();
    private FilteredList<Cheque> filtered;

    @FXML
    public void initialize() {
        setupTable();
        setupFilters();
        loadBanksFilter();
        loadChequeData();
        applyPermissions();
        FxUtils.animateIn(rootPane, 0);
    }

    private void applyPermissions() {
        User actor = SessionManager.getInstance().currentUser().orElse(null);
        boolean canPrint = AccessControl.can(actor, Permission.PRINT_CHEQUE);
        if (btnPrint != null) {
            btnPrint.setVisible(canPrint);
            btnPrint.setManaged(canPrint);
        }
    }

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
                setAlignment(javafx.geometry.Pos.CENTER);
            }
        });
    }

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
                "All", "Draft", "Pending", "Approved", "Rejected", "Printed", "Cancelled", "Deposited", "Cleared", "Bounced"));
        filterStatus.setValue("All");

        filtered = new FilteredList<>(data, p -> true);
        chequeTable.setItems(filtered);

        searchField.textProperty().addListener((obs, o, v) -> applyFilters());
        filterDate.valueProperty().addListener((obs, o, v) -> applyFilters());
        filterStatus.valueProperty().addListener((obs, o, v) -> applyFilters());
        filterBank.valueProperty().addListener((obs, o, v) -> applyFilters());
    }

    private void applyFilters() {
        String search = searchField.getText() == null ? ""
                : searchField.getText().toLowerCase().trim();
        LocalDate pickedDate = filterDate.getValue();
        String status = filterStatus.getValue();
        String bank = filterBank.getValue();

        if (!search.isEmpty()) {
            new Thread(() -> {
                try {
                    List<Cheque> results = chequeService.search(search);
                    Platform.runLater(() -> {
                        ObservableList<Cheque> filteredResults = FXCollections.observableArrayList();
                        for (Cheque c : results) {
                            boolean matchDate = pickedDate == null
                                    || (c.getIssueDate() != null && c.getIssueDate().equals(pickedDate));
                            boolean matchStatus = status == null || "All".equals(status)
                                    || (c.getStatus() != null && c.getStatus().name().equals(status));
                            boolean matchBank = bank == null || "All".equals(bank)
                                    || (c.getBankName() != null && c.getBankName().equals(bank));
                            if (matchDate && matchStatus && matchBank) {
                                filteredResults.add(c);
                            }
                        }
                        chequeTable.setItems(filteredResults);
                        
                        // Recalculate summary based on current table items
                        int count = filteredResults.size();
                        BigDecimal total = BigDecimal.ZERO;
                        for (Cheque c : filteredResults) {
                            if (c.getAmount() != null) {
                                total = total.add(c.getAmount());
                            }
                        }
                        lblTotalCount.setText(String.valueOf(count));
                        lblTotalAmount.setText("₹" + String.format("%,.2f", total));
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showAlert("Search Error", e.getMessage(), Alert.AlertType.ERROR));
                }
            }, "api-search-history").start();
        } else {
            filtered.setPredicate(c -> {
                boolean matchDate = pickedDate == null
                        || (c.getIssueDate() != null && c.getIssueDate().equals(pickedDate));

                boolean matchStatus = status == null || "All".equals(status)
                        || (c.getStatus() != null && c.getStatus().name().equals(status));

                boolean matchBank = bank == null || "All".equals(bank)
                        || (c.getBankName() != null && c.getBankName().equals(bank));

                return matchDate && matchStatus && matchBank;
            });
            chequeTable.setItems(filtered);
            recalculateSummary();
        }
    }

    private void recalculateSummary() {
        int count = filtered.size();
        BigDecimal total = BigDecimal.ZERO;
        for (Cheque c : filtered) {
            if (c.getAmount() != null) {
                total = total.add(c.getAmount());
            }
        }
        lblTotalCount.setText(String.valueOf(count));
        lblTotalAmount.setText("₹" + String.format("%,.2f", total));
    }

    private void loadBanksFilter() {
        new Thread(() -> {
            try {
                List<Bank> banks = bankService.getAll();
                Platform.runLater(() -> {
                    ObservableList<String> names = FXCollections.observableArrayList();
                    names.add("All");
                    for (Bank b : banks) {
                        names.add(b.getBankName());
                    }
                    filterBank.setItems(names);
                    filterBank.setValue("All");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    filterBank.setItems(FXCollections.observableArrayList("All", "SBI", "HDFC", "ICICI", "Axis Bank"));
                    filterBank.setValue("All");
                });
            }
        }, "history-load-banks").start();
    }

    private void loadChequeData() {
        new Thread(() -> {
            try {
                List<Cheque> list = chequeService.getAll();
                Platform.runLater(() -> {
                    data.setAll(list);
                    if (chequeTable != null) {
                        chequeTable.refresh();
                    }
                    recalculateSummary();
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Load Failure", "Failed to load cheque history: " + e.getMessage(), Alert.AlertType.ERROR));
            }
        }, "history-load-cheques").start();
    }

    @FXML
    private void onResetFilters() {
        searchField.clear();
        filterDate.setValue(null);
        filterStatus.setValue("All");
        filterBank.setValue("All");
        applyFilters();
    }

    @FXML
    private void onPrint() {
        User actor = SessionManager.getInstance().currentUser().orElse(null);
        if (!AccessControl.can(actor, Permission.PRINT_CHEQUE)) {
            showAlert("Permission Denied", "You do not have permission to print cheques.", Alert.AlertType.ERROR);
            return;
        }

        Cheque sel = chequeTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert("No Selection", "Please select a cheque from the table to print.", Alert.AlertType.WARNING);
            return;
        }

        try {
            // Draft status workflow
            if (sel.getStatus() == Cheque.Status.Draft) {
                if (AccessControl.can(actor, Permission.APPROVE_CHEQUE)) {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                            "This cheque is a draft. Submit it for approval and proceed to print preview?",
                            ButtonType.YES, ButtonType.NO);
                    confirm.setTitle("Submit & Approve for Printing");
                    confirm.setHeaderText(null);
                    var choice = confirm.showAndWait();
                    if (choice.isEmpty() || choice.get() != ButtonType.YES) {
                        return;
                    }
                    chequeService.setStatus(sel, Cheque.Status.Pending);
                    workflowService.approve(sel.getId(), actor);
                } else {
                    showAlert("Approval Required",
                            "This cheque is a draft. Ask an Admin or Manager to approve it before printing.",
                            Alert.AlertType.INFORMATION);
                    return;
                }
            }
            // Pending status workflow
            else if (sel.getStatus() == Cheque.Status.Pending) {
                if (AccessControl.can(actor, Permission.APPROVE_CHEQUE)) {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                            "This cheque is pending approval. Approve it and continue to print preview?",
                            ButtonType.YES, ButtonType.NO);
                    confirm.setTitle("Approve Before Printing");
                    confirm.setHeaderText(null);
                    var choice = confirm.showAndWait();
                    if (choice.isEmpty() || choice.get() != ButtonType.YES) {
                        return;
                    }
                    workflowService.approve(sel.getId(), actor);
                } else {
                    showAlert("Approval Required",
                            "This cheque is pending approval. Ask an Admin or Manager to approve it before printing.",
                            Alert.AlertType.INFORMATION);
                    return;
                }
            }
            // Rejected / Cancelled cannot be printed
            else if (sel.getStatus() == Cheque.Status.Rejected || sel.getStatus() == Cheque.Status.Cancelled) {
                showAlert("Cannot Print", "Rejected or cancelled cheques cannot be printed.", Alert.AlertType.WARNING);
                return;
            }

            // Execute print
            workflowService.print(sel.getId(), actor);

            // Reload data
            loadChequeData();
            
            // Reload parent dashboard if mainController available
            showAlert("Print Successful", "Cheque sent to print preview successfully.", Alert.AlertType.INFORMATION);

        } catch (Exception e) {
            showAlert("Printing Error", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
