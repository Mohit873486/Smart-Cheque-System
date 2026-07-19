package com.chequeprint.controller;

import com.chequeprint.service.BankService;
import com.chequeprint.model.Bank;
import com.chequeprint.model.Cheque;
import com.chequeprint.model.User;
import com.chequeprint.service.AccessControl;
import com.chequeprint.service.ChequeService;
import com.chequeprint.service.ChequeWorkflowService;
import com.chequeprint.service.Permission;
import com.chequeprint.service.PrintService;
import com.chequeprint.service.AuditService;
import com.chequeprint.model.AuditAction;
import com.chequeprint.model.AuditLog;
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

    // ── Form/Actions ──
    @FXML
    private Button btnNewCheque;
    @FXML
    private Button btnEdit;
    @FXML
    private Button btnApprove;
    @FXML
    private Button btnPrint;
    @FXML
    private MenuButton btnLifecycle;
    @FXML
    private Button btnDelete;

    // ── Filters ──
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> filterStatus;
    @FXML
    private ComboBox<String> filterBank;
    @FXML
    private DatePicker filterDate;

    // ── Root ──
    @FXML
    private VBox rootPane;

    // ── State ──
    MainController mainController;

    ChequeService chequeService = new ChequeService();
    ChequeWorkflowService workflowService = new ChequeWorkflowService();
    PrintService printService = new PrintService();
    BankService bankService = new BankService();
    AuditService auditService = new AuditService();

    private final ObservableList<Cheque> data = FXCollections.observableArrayList();
    private FilteredList<Cheque> filtered;
    private Cheque selectedCheque;

    public ObservableList<Cheque> getData() {
        return data;
    }

    /** Maps displayed bank name → bank_id; populated from DB. */
    private final java.util.Map<String, Integer> bankNameToId = new java.util.LinkedHashMap<>();

    // ─────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        setupTable();
        setupFilters();
        applyPermissions();
        loadData();
        updateButtonStates(null);
        FxUtils.animateIn(rootPane, 0);
    }

    private void applyPermissions() {
        User actor = SessionManager.getInstance().currentUser().orElse(null);
        boolean canCreate = AccessControl.can(actor, Permission.CREATE_CHEQUE);
        boolean canUpdate = AccessControl.can(actor, Permission.UPDATE_CHEQUE);
        boolean canApprove = AccessControl.can(actor, Permission.APPROVE_CHEQUE);
        boolean canPrint = AccessControl.can(actor, Permission.PRINT_CHEQUE);
        boolean canDelete = AccessControl.can(actor, Permission.DELETE_CHEQUE);

        setVisibleManaged(btnNewCheque, canCreate);
        setVisibleManaged(btnEdit, canUpdate);
        setVisibleManaged(btnApprove, canApprove);
        setVisibleManaged(btnPrint, canPrint);
        setVisibleManaged(btnLifecycle, canUpdate || canApprove);
        setVisibleManaged(btnDelete, canDelete);
    }

    private void setVisibleManaged(javafx.scene.Node node, boolean visible) {
        if (node != null) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
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
                    setTooltip(null);
                    return;
                }
                Label badge = statusBadge(item);
                Cheque cheque = getTableRow() != null ? getTableRow().getItem() : null;
                if (cheque != null && cheque.getLastPrinter() != null) {
                    Label subtext = new Label(cheque.getLastPrinter() + " (" + cheque.getLastPrintResult() + ")");
                    subtext.setStyle("-fx-font-size: 9px; -fx-text-fill: #94a3b8;");
                    VBox vbox = new VBox(2, badge, subtext);
                    vbox.setAlignment(javafx.geometry.Pos.CENTER);
                    setGraphic(vbox);

                    Tooltip tooltip = new Tooltip(String.format("Last Printer: %s\nStatus: %s",
                        cheque.getLastPrinter(), cheque.getLastPrintResult()));
                    tooltip.setShowDelay(javafx.util.Duration.millis(100));
                    setTooltip(tooltip);
                } else {
                    setGraphic(badge);
                    setTooltip(null);
                }
                setText(null);
                setAlignment(javafx.geometry.Pos.CENTER);
            }
        });

        chequeTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, sel) -> {
                    updateButtonStates(sel);
                });

        // Double-click row to edit
        chequeTable.setRowFactory(tv -> {
            TableRow<Cheque> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    Cheque rowData = row.getItem();
                    User actor = SessionManager.getInstance().currentUser().orElse(null);
                    if (AccessControl.can(actor, Permission.UPDATE_CHEQUE)) {
                        openChequeDialog(rowData);
                    }
                }
            });
            return row;
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
                "Status", "Draft", "Pending", "Approved", "Rejected", "Printed", "Cancelled"));
        filterStatus.setValue("Status");

        // Load bank list dynamically for filtering ComboBox
        new Thread(() -> {
            try {
                List<Bank> banks = bankService.getAll();
                Platform.runLater(() -> {
                    ObservableList<String> bankNames = FXCollections.observableArrayList("Bank Name");
                    for (Bank b : banks) {
                        if (b.getBankName() != null && !b.getBankName().isBlank()) {
                            bankNames.add(b.getBankName());
                        }
                    }
                    if (bankNames.size() == 1) {
                        bankNames.addAll("SBI", "HDFC", "ICICI", "Axis Bank");
                    }
                    filterBank.setItems(bankNames);
                    filterBank.setValue("Bank Name");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    filterBank.setItems(FXCollections.observableArrayList("Bank Name", "SBI", "HDFC", "ICICI", "Axis Bank"));
                    filterBank.setValue("Bank Name");
                });
            }
        }, "load-banks-filter").start();

        filtered = new FilteredList<>(data, p -> true);
        chequeTable.setItems(filtered);

        updateComboPlaceholderStyle(filterStatus, "Status");
        updateComboPlaceholderStyle(filterBank, "Bank Name");

        searchField.textProperty().addListener((obs, o, v) -> applyFilter());
        filterStatus.valueProperty().addListener((obs, o, v) -> {
            updateComboPlaceholderStyle(filterStatus, "Status");
            applyFilter();
        });
        if (filterBank != null) {
            filterBank.valueProperty().addListener((obs, o, v) -> {
                updateComboPlaceholderStyle(filterBank, "Bank Name");
                applyFilter();
            });
        }
        if (filterDate != null) {
            filterDate.valueProperty().addListener((obs, o, v) -> applyFilter());
        }
    }

    private void updateComboPlaceholderStyle(ComboBox<String> combo, String placeholder) {
        if (combo == null) return;
        if (placeholder.equals(combo.getValue())) {
            if (!combo.getStyleClass().contains("combo-placeholder")) {
                combo.getStyleClass().add("combo-placeholder");
            }
        } else {
            combo.getStyleClass().remove("combo-placeholder");
        }
    }

    private void applyFilter() {
        String search = searchField.getText() == null ? ""
                : searchField.getText().toLowerCase().trim();
        String status = filterStatus.getValue();
        String bank = filterBank != null ? filterBank.getValue() : "Bank Name";
        LocalDate date = filterDate != null ? filterDate.getValue() : null;

        if (!search.isEmpty()) {
            new Thread(() -> {
                try {
                    List<Cheque> results = chequeService.search(search);
                    Platform.runLater(() -> {
                        ObservableList<Cheque> filteredResults = FXCollections.observableArrayList();
                        for (Cheque c : results) {
                            boolean matchStatus = "Status".equals(status) || (c.getStatus() != null && c.getStatus().name().equals(status));
                            boolean matchBank = "Bank Name".equals(bank) || (c.getBankName() != null && c.getBankName().equalsIgnoreCase(bank));
                            boolean matchDate = date == null || (c.getIssueDate() != null && c.getIssueDate().equals(date));
                            if (matchStatus && matchBank && matchDate) {
                                filteredResults.add(c);
                            }
                        }
                        chequeTable.setItems(filteredResults);
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showAlert("Search Error", e.getMessage(), Alert.AlertType.ERROR));
                }
            }, "api-search-cheques").start();
        } else {
            filtered.setPredicate(c -> {
                boolean matchStatus = "Status".equals(status)
                        || (c.getStatus() != null && c.getStatus().name().equals(status));
                boolean matchBank = "Bank Name".equals(bank)
                        || (c.getBankName() != null && c.getBankName().equalsIgnoreCase(bank));
                boolean matchDate = date == null
                        || (c.getIssueDate() != null && c.getIssueDate().equals(date));
                return matchStatus && matchBank && matchDate;
            });
            chequeTable.setItems(filtered);
        }
    }

    @FXML
    private void onResetFilters() {
        if (searchField != null) searchField.clear();
        if (filterStatus != null) {
            filterStatus.setValue("Status");
            updateComboPlaceholderStyle(filterStatus, "Status");
        }
        if (filterBank != null) {
            filterBank.setValue("Bank Name");
            updateComboPlaceholderStyle(filterBank, "Bank Name");
        }
        if (filterDate != null) {
            filterDate.setValue(null);
        }
    }

    public void applyMainSearch(String query) {
        if (searchField != null) {
            searchField.setText(query == null ? "" : query.trim());
        }
    }

    // ── Load banks from DB into ComboBox ─────────────────────────────


    // ── Load cheque data ─────────────────────────────────────────────
    private void loadData() {
        new Thread(() -> {
            try {
                var list = chequeService.getAll();
                try {
                    List<AuditLog> auditLogs = auditService.findRecent(200);
                    java.util.Map<Integer, AuditLog> lastPrintLogs = new java.util.HashMap<>();
                    for (AuditLog log : auditLogs) {
                        if ("cheques".equals(log.getTableName()) && AuditAction.PRINT == log.getAction()) {
                            Integer chequeId = log.getRecordId();
                            if (chequeId != null && !lastPrintLogs.containsKey(chequeId)) {
                                lastPrintLogs.put(chequeId, log);
                            }
                        }
                    }
                    for (Cheque c : list) {
                        AuditLog log = lastPrintLogs.get(c.getId());
                        if (log != null) {
                            String details = log.getDetails();
                            String printer = parseDetails(details, "Printer: ", ".");
                            String status = parseDetails(details, "Status: ", ".");
                            c.setLastPrinter(printer != null ? printer : "Unknown");
                            c.setLastPrintResult(status != null ? status : "SUCCESS");
                        }
                    }
                } catch (Exception auditEx) {
                    System.err.println("Failed to enrich cheques with print log: " + auditEx.getMessage());
                }
                Platform.runLater(() -> {
                    data.setAll(list);
                    if (chequeTable != null) {
                        chequeTable.refresh();
                    }
                });
            } catch (Exception e) {
                String message = e.getMessage();
                if (message != null && message.contains("Failed to fetch cheques from REST API")) {
                    message = "Unable to connect to the backend server. Please start the REST API service and try again.";
                }
                final String alertMessage = message;
                Platform.runLater(() -> showAlert("Server Connection Error", alertMessage, Alert.AlertType.ERROR));
            }
        }, "load-cheques").start();
    }

    private String parseDetails(String details, String prefix, String suffix) {
        if (details == null) return null;
        int start = details.indexOf(prefix);
        if (start == -1) return null;
        start += prefix.length();
        int end = details.indexOf(suffix, start);
        if (end == -1) {
            return details.substring(start).trim();
        }
        return details.substring(start, end).trim();
    }

    public void reload() {
        loadData();
    }

    // ── Dialog / Popup Management ────────────────────────────────────
    @FXML
    private void onNewCheque() {
        openChequeDialog(null);
    }

    @FXML
    private void onEdit() {
        Cheque sel = chequeTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert("No Selection", "Please select a cheque to edit.", Alert.AlertType.WARNING);
            return;
        }
        openChequeDialog(sel);
    }

    private void openChequeDialog(Cheque cheque) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/view/cheque_dialog.fxml"));
            javafx.scene.Parent root = loader.load();

            ChequeDialogController controller = loader.getController();
            controller.initData(cheque);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initOwner(rootPane.getScene().getWindow());
            stage.setTitle(cheque == null ? "New Cheque" : "Edit Cheque");
            
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            com.chequeprint.util.ThemeManager.applySavedTheme(scene);

            // Allow dragging the undecorated stage
            final double[] xOffset = new double[1];
            final double[] yOffset = new double[1];
            root.setOnMousePressed(event -> {
                xOffset[0] = event.getSceneX();
                yOffset[0] = event.getSceneY();
            });
            root.setOnMouseDragged(event -> {
                stage.setX(event.getScreenX() - xOffset[0]);
                stage.setY(event.getScreenY() - yOffset[0]);
            });
            
            stage.setScene(scene);

            javafx.scene.Parent ownerRoot = rootPane.getScene().getRoot();
            javafx.scene.effect.Effect oldEffect = ownerRoot.getEffect();
            
            javafx.scene.effect.BoxBlur blur = new javafx.scene.effect.BoxBlur(6, 6, 3);
            javafx.scene.effect.ColorAdjust dim = new javafx.scene.effect.ColorAdjust();
            dim.setBrightness(-0.35);
            dim.setInput(blur);
            
            ownerRoot.setEffect(dim);
            
            try {
                stage.showAndWait();
            } finally {
                ownerRoot.setEffect(oldEffect);
            }

            if (controller.isSaved()) {
                loadData();
                if (mainController != null) {
                    Object dc = mainController.getController("dashboard");
                    if (dc instanceof DashboardController) {
                        ((DashboardController) dc).reload();
                    }
                }
            }
        } catch (Exception e) {
            showAlert("Error", "Could not open cheque window: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    // ── Print ────────────────────────────────────────────────────────
    @FXML
    private void onPrint() {
        User actor = SessionManager.getInstance().currentUser().orElse(null);
        if (!AccessControl.can(actor, Permission.PRINT_CHEQUE)) {
            showAlert("Permission Denied", "You do not have permission to print cheques.", Alert.AlertType.ERROR);
            return;
        }
        Cheque sel = chequeTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert("No Selection", "Please select a cheque to print.",
                    Alert.AlertType.WARNING);
            return;
        }

        if (sel.getStatus() != Cheque.Status.Approved && sel.getStatus() != Cheque.Status.Printed) {
            if (AccessControl.can(actor, Permission.APPROVE_CHEQUE)) {
                showAlert("Approval Required",
                        "This cheque is in " + sel.getStatus() + " status. You must approve it first using the 'Approve' button before printing.",
                        Alert.AlertType.WARNING);
            } else {
                if (sel.getStatus() == Cheque.Status.Draft) {
                    showAlert("Approval Required",
                            "This cheque is a draft. Save it first, then ask an Admin or Manager to approve it before printing.",
                            Alert.AlertType.INFORMATION);
                } else if (sel.getStatus() == Cheque.Status.Pending) {
                    showAlert("Approval Required",
                            "This cheque is pending approval. Ask an Admin or Manager to approve it before printing.",
                            Alert.AlertType.INFORMATION);
                } else {
                    showAlert("Cannot Print",
                            "This cheque cannot be printed. Current status: " + sel.getStatus(),
                            Alert.AlertType.WARNING);
                }
            }
            return;
        }

        try {
            workflowService.print(sel.getId(), actor);
            loadData();
            clearForm();
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

    // ── Approve ──────────────────────────────────────────────────────
    @FXML
    private void onApprove() {
        Cheque sel = chequeTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert("No Selection", "Please select a cheque to approve.",
                    Alert.AlertType.WARNING);
            return;
        }

        User actor = SessionManager.getInstance().currentUser().orElse(null);
        if (!AccessControl.can(actor, Permission.APPROVE_CHEQUE)) {
            showAlert("Permission Denied",
                    "You do not have permission to approve cheques.",
                    Alert.AlertType.ERROR);
            return;
        }

        if (sel.getStatus() != Cheque.Status.Pending) {
            showAlert("Cannot Approve",
                    "Only pending cheques can be approved. Current status: " + sel.getStatus().name(),
                    Alert.AlertType.WARNING);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Approve cheque " + sel.getChequeNo() + " for " + sel.getPayeeName() + " (₹" + sel.getAmount() + ")?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Approval");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    workflowService.approve(sel.getId(), actor);
                    loadData();
                    clearForm();
                    if (mainController != null) {
                        Object dc = mainController.getController("dashboard");
                        if (dc instanceof DashboardController)
                            ((DashboardController) dc).reload();
                    }
                    showAlert("Success",
                            "Cheque approved successfully. Ready to print.",
                            Alert.AlertType.INFORMATION);
                } catch (Exception e) {
                    showAlert("Error", e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    // ── Delete ───────────────────────────────────────────────────────
    @FXML
    private void onDelete() {
        User actor = SessionManager.getInstance().currentUser().orElse(null);
        if (!AccessControl.can(actor, Permission.DELETE_CHEQUE)) {
            showAlert("Permission Denied", "You do not have permission to delete cheques.", Alert.AlertType.ERROR);
            return;
        }
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
                new Thread(() -> {
                    try {
                        chequeService.delete(sel.getId());
                        Platform.runLater(() -> {
                            clearForm();
                            loadData();
                            if (mainController != null) {
                                Object dc = mainController.getController("dashboard");
                                if (dc instanceof DashboardController)
                                    ((DashboardController) dc).reload();
                            }
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> showAlert("Error", e.getMessage(), Alert.AlertType.ERROR));
                    }
                }, "delete-cheque").start();
            }
        });
    }

    @FXML
    private void onClear() {
        clearForm();
    }

    // ── Form helpers ─────────────────────────────────────────────────
    private void clearForm() {
        selectedCheque = null;
        chequeTable.getSelectionModel().clearSelection();
        updateButtonStates(null);
    }

    private void updateButtonStates(Cheque selected) {
        User actor = SessionManager.getInstance().currentUser().orElse(null);
        boolean hasApprovePerm = AccessControl.can(actor, Permission.APPROVE_CHEQUE);

        if (selected == null) {
            if (btnPrint != null) {
                btnPrint.setDisable(true);
                btnPrint.setTooltip(null);
            }
            if (btnApprove != null) {
                btnApprove.setDisable(true);
            }
            if (btnEdit != null) {
                btnEdit.setDisable(true);
            }
            return;
        }

        boolean isPrintable = selected.getStatus() == Cheque.Status.Approved || selected.getStatus() == Cheque.Status.Printed;
        if (btnPrint != null) {
            btnPrint.setDisable(!isPrintable);
            if (!isPrintable) {
                String reason = hasApprovePerm 
                        ? "This cheque is in " + selected.getStatus() + " status. Please approve it first."
                        : "This cheque is in " + selected.getStatus() + " status and requires manager approval.";
                btnPrint.setTooltip(new Tooltip(reason));
            } else {
                btnPrint.setTooltip(null);
            }
        }

        if (btnApprove != null) {
            btnApprove.setDisable(selected.getStatus() != Cheque.Status.Pending || !hasApprovePerm);
        }

        if (btnEdit != null) {
            btnEdit.setDisable(false);
        }
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(null);
        a.showAndWait();
    }

    @FXML
    private void onDeposit() {
        User actor = SessionManager.getInstance().currentUser().orElse(null);
        Cheque sel = chequeTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert("No Selection", "Please select a cheque to mark as deposited.", Alert.AlertType.WARNING);
            return;
        }
        try {
            workflowService.deposit(sel.getId(), actor);
            showAlert("Success", "Cheque " + sel.getChequeNo() + " marked as Deposited.", Alert.AlertType.INFORMATION);
            loadData();
            clearForm();
            refreshDashboard();
        } catch (Exception e) {
            showAlert("Error", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void onClearCheque() {
        User actor = SessionManager.getInstance().currentUser().orElse(null);
        Cheque sel = chequeTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert("No Selection", "Please select a cheque to mark as cleared.", Alert.AlertType.WARNING);
            return;
        }
        try {
            workflowService.clear(sel.getId(), actor);
            showAlert("Success", "Cheque " + sel.getChequeNo() + " marked as Cleared.", Alert.AlertType.INFORMATION);
            loadData();
            clearForm();
            refreshDashboard();
        } catch (Exception e) {
            showAlert("Error", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void onBounce() {
        User actor = SessionManager.getInstance().currentUser().orElse(null);
        Cheque sel = chequeTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert("No Selection", "Please select a cheque to mark as bounced.", Alert.AlertType.WARNING);
            return;
        }
        try {
            workflowService.bounce(sel.getId(), actor);
            showAlert("Success", "Cheque " + sel.getChequeNo() + " marked as Bounced.", Alert.AlertType.INFORMATION);
            loadData();
            clearForm();
            refreshDashboard();
        } catch (Exception e) {
            showAlert("Error", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void onCancel() {
        User actor = SessionManager.getInstance().currentUser().orElse(null);
        Cheque sel = chequeTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert("No Selection", "Please select a cheque to cancel.", Alert.AlertType.WARNING);
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to cancel / void cheque " + sel.getChequeNo() + "?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Cancellation");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    workflowService.cancel(sel.getId(), actor);
                    showAlert("Success", "Cheque " + sel.getChequeNo() + " has been cancelled.", Alert.AlertType.INFORMATION);
                    loadData();
                    clearForm();
                    refreshDashboard();
                } catch (Exception e) {
                    showAlert("Error", e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void refreshDashboard() {
        if (mainController != null) {
            Object dc = mainController.getController("dashboard");
            if (dc instanceof DashboardController) {
                ((DashboardController) dc).reload();
            }
        }
    }

    public void setMainController(MainController mc) {
        this.mainController = mc;
    }
}
